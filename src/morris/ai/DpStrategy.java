package morris.ai;

import morris.model.Board;
import morris.model.Move;
import morris.model.Player;
import morris.util.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

            int score = scoreMoveWithShallowDP(afterCpu, cpu, human);

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
