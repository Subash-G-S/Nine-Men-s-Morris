package morris.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import morris.model.Board;
import morris.model.Move;
import morris.model.Player;
import morris.util.Constants;

public class DpStrategy implements CpuStrategy {

    private final Map<String, Integer> dpCache = new HashMap<>();

    @Override
    public Move getBestMove(Board board, Player cpu, Player human) {
        List<Move> moves = board.generateLegalMoves(cpu.code());
        if (moves.isEmpty()) return null;

        // Placement phase
        if (moves.get(0).from == -1) {
            return choosePlacementMove(board, moves, cpu, human);
        }

        dpCache.clear();

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move m : moves) {
            Board afterCpu = board.clone();
            afterCpu.applyMove(m, cpu.code());

            int score = scoreMoveWithShallowDP(afterCpu, m, cpu, human);

            if (score > bestScore) {
                bestScore = score;
                bestMove = m;
            }
        }

        return bestMove;
    }

    private Move choosePlacementMove(Board board, List<Move> moves, Player cpu, Player human) {
        // 1) Immediate mill
        for (Move m : moves) {
            Board c = board.clone();
            c.applyMove(m, cpu.code());
            if (c.formsMill(cpu.code(), m.to)) return m;
        }
        // 2) Block human immediate mill
        for (Move hm : board.generateLegalMoves(human.code())) {
            Board hc = board.clone();
            hc.applyMove(hm, human.code());
            if (hc.formsMill(human.code(), hm.to)) {
                for (Move m : moves) if (m.to == hm.to) return m;
            }
        }
        // 3) Prefer inner ring, then middle ring
        for (Move m : moves) if (Constants.INNER_RING.contains(m.to)) return m;
        for (Move m : moves) if (Constants.MIDDLE_RING.contains(m.to)) return m;
        return moves.get(0);
    }
    private int scoreMoveWithShallowDP(Board afterCpu, Move cpuMove, Player cpu, Player human) {
        if (afterCpu.formsMill(cpu.code(), cpuMove.to)) {
            applyBestRemoval(afterCpu, cpu, human, true, cpu, human);
        }

        int myEval = evaluateWithCache(afterCpu, cpu, human);

        List<Move> oppMoves = afterCpu.generateLegalMoves(human.code());
        if (oppMoves.isEmpty()) {
            return myEval + 50_000;
        }

        int worstForCpu = Integer.MAX_VALUE;
        for (Move om : oppMoves) {
            Board afterOpp = afterCpu.clone();
            afterOpp.applyMove(om, human.code());

            if (afterOpp.formsMill(human.code(), om.to)) {
                applyBestRemoval(afterOpp, human, cpu, false, cpu, human);
            }

            int v = evaluateWithCache(afterOpp, cpu, human);
            if (v < worstForCpu) worstForCpu = v;
        }

        return worstForCpu;
    }

    private int evaluateWithCache(Board b, Player cpu, Player human) {
        String key = encode(b);
        Integer cached = dpCache.get(key);
        if (cached != null) return cached;
        int value = evaluate(b, cpu, human);
        dpCache.put(key, value);
        return value;
    }

    private boolean blocksOpponentMill(Board board, Move m, Player opponent) {
        for (Move om : board.generateLegalMoves(opponent.code())) {
            Board c = board.clone();
            c.applyMove(om, opponent.code());
            if (c.hasAnyMill(opponent.code()) && m.to == om.to) return true;
        }
        return false;
    }

    private void applyBestRemoval(Board board, Player attacker, Player defender, boolean maximizeCpuEval, Player cpu, Player human) {
        List<Integer> candidates = board.candidateRemovals(defender.code());
        if (candidates.isEmpty()) return;

        Integer bestRemoval = null;
        int bestScore = maximizeCpuEval ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (int idx : candidates) {
            Board clone = board.clone();
            clone.getCells()[idx] = Constants.EMPTY;
            int score = evaluateWithCache(clone, cpu, human);

            if (maximizeCpuEval) {
                if (score > bestScore) {
                    bestScore = score;
                    bestRemoval = idx;
                }
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestRemoval = idx;
                }
            }
        }

        if (bestRemoval != null) board.getCells()[bestRemoval] = Constants.EMPTY;
    }

    // ------------------------ Encoding / Utilities ------------------------
    private String encode(Board b) {
        // compact string of 24 chars '0','1','2' representing cells
        char[] arr = new char[24];
        int[] cells = b.getCells();
        for (int i = 0; i < 24; i++) arr[i] = (char) ('0' + cells[i]);
        return new String(arr);
    }

    // ------------------------ Evaluation ------------------------
    private int evaluate(Board b, Player cpu, Player human) {
        int cpuCount = b.countPieces(cpu.code());
        int humanCount = b.countPieces(human.code());
        boolean placement = b.generateLegalMoves(cpu.code()).stream().anyMatch(m -> m.from == -1);
        if (!placement && cpuCount <= 2) return -100000;
        if (!placement && humanCount <= 2) return 100000;

        int score = 0;
        score += (cpuCount - humanCount) * 120;
        score += (b.generateLegalMoves(cpu.code()).size() - b.generateLegalMoves(human.code()).size()) * 10;
        score += millPotential(b, cpu) * 40;
        score -= millPotential(b, human) * 45;
        score += twoInRow(b, cpu) * 6;
        score -= twoInRow(b, human) * 8;
        score += b.countPiecesInList(cpu.code(), Constants.MIDDLE_RING) * 5;
        score += b.countPiecesInList(cpu.code(), Constants.INNER_RING) * 8;
        score -= b.countPiecesInList(human.code(), Constants.MIDDLE_RING) * 5;
        score -= b.countPiecesInList(human.code(), Constants.INNER_RING) * 8;
        return score;
    }

    private int millPotential(Board b, Player p) {
        int c = 0;
        for (int[] mill : Constants.MILLS) {
            int own = 0, empty = 0;
            for (int idx : mill) {
                if (b.getCells()[idx] == p.code()) own++;
                else if (b.getCells()[idx] == 0) empty++;
            }
            if (own == 2 && empty == 1) c++;
        }
        return c;
    }

    private int twoInRow(Board b, Player p) {
        int count = 0;
        for (int[] mill : Constants.MILLS) {
            int own = 0;
            for (int idx : mill) if (b.getCells()[idx] == p.code()) own++;
            if (own == 2) count++;
        }
        return count;
    }
}
