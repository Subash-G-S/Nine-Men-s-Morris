package morris.ai;

import morris.model.Board;
import morris.model.Move;
import morris.model.Player;
import morris.util.Constants;

import java.util.*;

public class GreedyStrategy implements CpuStrategy {

    @Override
    public Move getBestMove(Board board, Player cpu, Player human) {
        List<Move> moves = board.generateLegalMoves(cpu.code());
        if (moves.isEmpty()) return null;

        Move best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move m : moves) {
            Board clone = board.clone();
            clone.applyMove(m, cpu.code());

            int score = evaluate(clone, board, m, cpu, human);

            if (score > bestScore) {
                bestScore = score;
                best = m;
            }
        }

        return best;
    }

    // ---------------------------------------------------------
    // GRAPH-BASED GREEDY EVALUATION
    // ---------------------------------------------------------
    private int evaluate(Board newState, Board oldState, Move move,
                         Player cpu, Player human) {

        int score = 0;
        int cpuCode = cpu.code();
        int humanCode = human.code();

        // -------------------------
        // 1. Mill formation
        // -------------------------
        if (newState.formsMill(cpuCode, move.to))
            score += 100;

        // -------------------------
        // 2. Block opponent mill
        // -------------------------
        if (threateningMill(oldState, human))
            score += 80;

        // -------------------------
        // 3. Graph-based: Node centrality (degree)
        // -------------------------
        int degree = Constants.ADJ.get(move.to).size();
        score += degree * 10;

        // -------------------------
        // 4. Graph-based: Opponent mobility reduction
        // -------------------------
        int before = oldState.generateLegalMoves(humanCode).size();
        int after  = newState.generateLegalMoves(humanCode).size();
        score += (before - after) * 12;

        // -------------------------
        // 5. Graph-based: CPU mobility improvement
        // -------------------------
        score += newState.generateLegalMoves(cpuCode).size() * 3;

        // -------------------------
        // 6. Graph-based: Connected component advantage
        // -------------------------
        score += largestCluster(newState, cpuCode) * 7;
        score -= largestCluster(newState, humanCode) * 8;

        // -------------------------
        // 7. Ring control (outer → middle → inner)
        // -------------------------
        score += newState.countPiecesInList(cpuCode, Constants.INNER_RING) * 5;
        score += newState.countPiecesInList(cpuCode, Constants.MIDDLE_RING) * 3;

        score -= newState.countPiecesInList(humanCode, Constants.INNER_RING) * 5;
        score -= newState.countPiecesInList(humanCode, Constants.MIDDLE_RING) * 3;

        return score;
    }

    // ---------------------------------------------------------
    // opponent threatens a mill
    // ---------------------------------------------------------
    private boolean threateningMill(Board b, Player human) {
        for (Move m : b.generateLegalMoves(human.code())) {
            Board c = b.clone();
            c.applyMove(m, human.code());
            if (c.hasAnyMill(human.code())) return true;
        }
        return false;
    }

    // ---------------------------------------------------------
    // BFS for largest connected component of player's stones
    // ---------------------------------------------------------
    private int largestCluster(Board b, int player) {
        boolean[] visited = new boolean[24];
        int maxSize = 0;

        for (int i = 0; i < 24; i++) {
            if (!visited[i] && b.getCells()[i] == player) {
                int size = bfsCluster(b, i, player, visited);
                if (size > maxSize) maxSize = size;
            }
        }
        return maxSize;
    }

    private int bfsCluster(Board b, int start, int player, boolean[] visited) {
        Queue<Integer> q = new LinkedList<>();
        q.add(start);
        visited[start] = true;

        int size = 1;

        while (!q.isEmpty()) {
            int u = q.poll();
            for (int nb : Constants.ADJ.get(u)) {
                if (!visited[nb] && b.getCells()[nb] == player) {
                    visited[nb] = true;
                    size++;
                    q.add(nb);
                }
            }
        }
        return size;
    }
}
