package morris.ai;

import java.util.*;
import morris.model.Board;
import morris.model.Move;
import morris.model.Player;
import morris.util.Constants;

public class DivideAndConquerStrategy implements CpuStrategy {

    @Override
    public Move getBestMove(Board board, Player cpu, Player human) {

        List<Move> moves = board.generateLegalMoves(cpu.code());
        if (moves.isEmpty()) return null;

        // ---- DIVIDE ----
        List<Move> outer = new ArrayList<>();
        List<Move> middle = new ArrayList<>();
        List<Move> inner = new ArrayList<>();

        for (Move m : moves) {
            if (Constants.OUTER_RING.contains(m.to)) outer.add(m);
            else if (Constants.MIDDLE_RING.contains(m.to)) middle.add(m);
            else inner.add(m);
        }

        // ---- CONQUER ----
        List<ScoredMove> outerScored = scoreRegion(board, cpu, human, outer);
        List<ScoredMove> midScored   = scoreRegion(board, cpu, human, middle);
        List<ScoredMove> innerScored = scoreRegion(board, cpu, human, inner);

        // ---- MERGE ----
        List<ScoredMove> merged = mergeSorted(
                sortRegion(outerScored),
                sortRegion(midScored),
                sortRegion(innerScored)
        );

        return merged.isEmpty() ? null : merged.get(0).move;
    }

    // ===============================================================
    // REGION SCORING
    // ===============================================================
    private List<ScoredMove> scoreRegion(Board board, Player cpu, Player human, List<Move> regionMoves) {
        List<ScoredMove> result = new ArrayList<>();

        for (Move m : regionMoves) {
            Board clone = board.clone();
            clone.applyMove(m, cpu.code());

            int score = evaluate(clone, board, m, cpu, human);
            result.add(new ScoredMove(m, score));
        }
        return result;
    }

    // ===============================================================
    // LOCAL EVALUATION FUNCTION
    // ===============================================================
    private int evaluate(Board newState, Board oldState, Move m, Player cpu, Player human) {
        int score = 0;

        int cpuCode = cpu.code();
        int humanCode = human.code();

        // Mill formation
        if (newState.formsMill(cpuCode, m.to)) score += 80;

        // Block opponent near mill
        boolean hadThreat = hasMillThreat(oldState, human);
        if (hadThreat) {
            if (blocksOpponentMill(oldState, m, human)) {
                score += 1000; // huge bonus for blocking
            } else {
                score -= 200;  // penalize ignoring the threat
            }
        }

        // Mobility difference
        score += newState.generateLegalMoves(cpuCode).size() * 4;
        score -= newState.generateLegalMoves(humanCode).size() * 4;

        // Ring control
        if (Constants.INNER_RING.contains(m.to)) score += 12;
        if (Constants.MIDDLE_RING.contains(m.to)) score += 6;

        return score;
    }

    // ===============================================================
    // SORT REGION
    // ===============================================================
    private List<ScoredMove> sortRegion(List<ScoredMove> moves) {
        if (moves.size() <= 1) return moves;

        int mid = moves.size() / 2;
        List<ScoredMove> left = sortRegion(moves.subList(0, mid));
        List<ScoredMove> right = sortRegion(moves.subList(mid, moves.size()));

        return mergeSorted(left, right);
    }

    // ===============================================================
    // MERGE
    // ===============================================================
    private List<ScoredMove> mergeSorted(List<ScoredMove> a, List<ScoredMove> b) {
        List<ScoredMove> merged = new ArrayList<>();
        int i = 0, j = 0;
        while (i < a.size() && j < b.size()) {
            if (a.get(i).score >= b.get(j).score) merged.add(a.get(i++));
            else merged.add(b.get(j++));
        }
        while (i < a.size()) merged.add(a.get(i++));
        while (j < b.size()) merged.add(b.get(j++));
        return merged;
    }

    private List<ScoredMove> mergeSorted(List<ScoredMove> a, List<ScoredMove> b, List<ScoredMove> c) {
        return mergeSorted(mergeSorted(a, b), c);
    }

    // ===============================================================
    // CHECK IF HUMAN ABOUT TO FORM MILL
    // ===============================================================
    private boolean hasMillThreat(Board b, Player human) {
        for (Move m : b.generateLegalMoves(human.code())) {
            Board c = b.clone();
            c.applyMove(m, human.code());
            if (c.hasAnyMill(human.code())) {
                return true;
            }
        }
        return false;
    }

    private boolean blocksOpponentMill(Board b, Move cpuMove, Player human) {
        for (Move m : b.generateLegalMoves(human.code())) {
            Board c = b.clone();
            c.applyMove(m, human.code());
            if (c.hasAnyMill(human.code())) {
                if (cpuMove.to == m.to) return true;
            }
        }
        return false;
    }

    // ===============================================================
    // HELPER CLASS
    // ===============================================================
    private static class ScoredMove {
        Move move;
        int score;

        ScoredMove(Move m, int s) {
            this.move = m;
            this.score = s;
        }
    }
}
