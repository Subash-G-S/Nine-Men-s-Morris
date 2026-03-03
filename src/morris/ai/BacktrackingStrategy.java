package morris.ai;

import java.util.ArrayList;
import java.util.List;
import morris.model.Board;
import morris.model.Move;
import morris.model.Player;
import morris.util.Constants;

//-------------------------BacktrackingImplementation-------------------------------------


// LOGIC : 
/*
--------1 ) Choose the piece : picks legal move (placing a piece or moving a piece or flying a piece)
--------2 ) Execute : Update the board state and check whether a mill is formed or not.
--------3 ) Recursion : Call the function again and again to simulate the oponents best move .
--------4 ) Backtrack : Revert the board to its previous state to check the alternative path . 
 * 
 * 
 * Kinda trial and error method to win the game against a human. Not actually a brute force .
 * 
 * Time Complexity ::  O(b^d)
 * 
 * here b is avaerage numbe of legal moves available . 
 * and d is depth (no of turns the algorithm looks ahead)
 */
public class BacktrackingStrategy implements CpuStrategy {

    private int maxDepth = 4;
    private int cpuCode;
    private int humanCode;
    private int nodesVisited;

    private static final int WIN_SCORE = 100_000;
    private static final int MILL_VALUE = 80;
    private static final int PIECE_VALUE = 120;
    private static final int MOBILITY_WEIGHT = 8;
    private static final int MILL_POTENTIAL = 35;
    private static final int BLOCK_THREAT_VALUE = 50;
    private static final int CLUSTER_VALUE = 6;

    private static class TraceContext {
        private final StringBuilder out = new StringBuilder();
        private final int maxNodes;
        private int visited;
        private boolean capped;

        private TraceContext(int maxNodes) {
            this.maxNodes = Math.max(1, maxNodes);
        }

        private boolean allowNode() {
            if (visited >= maxNodes) {
                capped = true;
                return false;
            }
            visited++;
            return true;
        }
    }


    @Override
    public Move getBestMove(Board board, Player cpu, Player human) {
        this.cpuCode = cpu.code();
        this.humanCode = human.code();
        this.nodesVisited = 0;

        List<Move> legalMoves = board.generateLegalMoves(cpuCode);
        if (legalMoves.isEmpty()) return null;

        // Tactical fast path: immediate mill wins are preferred before deeper search.
        Move immediateMill = findImmediateMillMove(board, legalMoves, cpuCode, humanCode);
        if (immediateMill != null) return immediateMill;

        // Tactical defense: if human has an instant threat, prioritize a direct block.
        Move blockingMove = findImmediateBlockMove(board, legalMoves, humanCode);
        if (blockingMove != null) return blockingMove;

        // Lower depth in placement phase where branching factor is very high.
        int searchDepth = isPlacementPhase(board) ? 2 : maxDepth;
        List<Move> orderedMoves = orderMoves(board, legalMoves, cpuCode, humanCode, true);

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (Move move : orderedMoves) {
            List<Board> nextStates = applyMoveAndResolveMill(board, move, cpuCode, humanCode);
            int moveScore = Integer.MIN_VALUE;

            for (Board next : nextStates) {
                int score = minimax(next, searchDepth - 1, false, alpha, beta);
                moveScore = Math.max(moveScore, score);
                alpha = Math.max(alpha, moveScore);
                if (alpha >= beta) break;
            }

            if (moveScore > bestScore) {
                bestScore = moveScore;
                bestMove = move;
            }
        }

        return bestMove;
    }
   private int minimax(Board state, int depth, boolean isMaximizing, int alpha, int beta) {
        nodesVisited++;
        if (depth == 0 || isTerminal(state)) return evaluate(state);

        int currentCode = isMaximizing ? cpuCode : humanCode;
        int opponentCode = isMaximizing ? humanCode : cpuCode;
        List<Move> moves = orderMoves(
                state,
                state.generateLegalMoves(currentCode),
                currentCode,
                opponentCode,
                isMaximizing
        );

        if (moves.isEmpty()) {
            return isMaximizing ? -WIN_SCORE + depth : WIN_SCORE - depth;
        }

        if (isMaximizing) {
            int best = Integer.MIN_VALUE;
            for (Move move : moves) {
                for (Board next : applyMoveAndResolveMill(state, move, currentCode, opponentCode)) {
                    int score = minimax(next, depth - 1, false, alpha, beta);
                    best = Math.max(best, score);
                    alpha = Math.max(alpha, best);
                    if (alpha >= beta) return best;
                }
            }
            return best;
        }

        int best = Integer.MAX_VALUE;
        for (Move move : moves) {
            for (Board next : applyMoveAndResolveMill(state, move, currentCode, opponentCode)) {
                int score = minimax(next, depth - 1, true, alpha, beta);
                best = Math.min(best, score);
                beta = Math.min(beta, best);
                if (alpha >= beta) return best;
            }
        }
        return best;
    }

    private List<Board> applyMoveAndResolveMill(Board state, Move move, int moverCode, int opponentCode) {
        Board applied = state.clone();
        applied.applyMove(move, moverCode);

        if (!applied.formsMill(moverCode, move.to)) {
            return List.of(applied);
        }

        List<Integer> removals = applied.candidateRemovals(opponentCode);
        if (removals.isEmpty()) {
            return List.of(applied);
        }

        List<Board> outcomes = new ArrayList<>(removals.size());
        List<Integer> orderedRemovals = orderRemovals(applied, removals, moverCode, opponentCode);
        for (int removeAt : orderedRemovals) {
            Board afterCapture = applied.clone();
            afterCapture.getCells()[removeAt] = Constants.EMPTY;
            outcomes.add(afterCapture);
        }
        return outcomes;
    }

    private boolean isTerminal(Board board) {
        if (isPlacementPhase(board)) return false;

        if (board.countPieces(cpuCode) <= 2 || board.countPieces(humanCode) <= 2) return true;
        return board.generateLegalMoves(cpuCode).isEmpty() || board.generateLegalMoves(humanCode).isEmpty();
    }

    private boolean isPlacementPhase(Board board) {
        return board.generateLegalMoves(cpuCode).stream().anyMatch(m -> m.from == -1);
    }

    private int evaluate(Board state) {
        if (!isPlacementPhase(state)) {
            if (state.countPieces(cpuCode) <= 2 || state.generateLegalMoves(cpuCode).isEmpty()) return -WIN_SCORE;
            if (state.countPieces(humanCode) <= 2 || state.generateLegalMoves(humanCode).isEmpty()) return WIN_SCORE;
        }

        int score = 0;

        int cpuPieces = state.countPieces(cpuCode);
        int humanPieces = state.countPieces(humanCode);
        int cpuMobility = state.generateLegalMoves(cpuCode).size();
        int humanMobility = state.generateLegalMoves(humanCode).size();

        score += (cpuPieces - humanPieces) * PIECE_VALUE;
        score += (cpuMobility - humanMobility) * MOBILITY_WEIGHT;
        score += (state.countMills(cpuCode) - state.countMills(humanCode)) * MILL_VALUE;
        score += (countNearMills(state, cpuCode) - countNearMills(state, humanCode)) * MILL_POTENTIAL;
        score += (countBlockedPieces(state, humanCode) - countBlockedPieces(state, cpuCode)) * BLOCK_THREAT_VALUE;
        score += (largestCluster(state, cpuCode) - largestCluster(state, humanCode)) * CLUSTER_VALUE;

        score += state.countPiecesInList(cpuCode, Constants.INNER_RING) * 8;
        score += state.countPiecesInList(cpuCode, Constants.MIDDLE_RING) * 5;
        score -= state.countPiecesInList(humanCode, Constants.INNER_RING) * 8;
        score -= state.countPiecesInList(humanCode, Constants.MIDDLE_RING) * 5;

        return score;
    }

    private int countNearMills(Board board, int playerCode) {
        int near = 0;
        for (int[] mill : Constants.MILLS) {
            int own = 0;
            int empty = 0;
            for (int idx : mill) {
                if (board.getCells()[idx] == playerCode) own++;
                else if (board.getCells()[idx] == Constants.EMPTY) empty++;
            }
            if (own == 2 && empty == 1) near++;
        }
        return near;
    }

    private Move findImmediateMillMove(Board board, List<Move> moves, int playerCode, int opponentCode) {
        Move best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Move move : moves) {
            Board next = board.clone();
            next.applyMove(move, playerCode);
            if (!next.formsMill(playerCode, move.to)) continue;

            // Approximate impact after best available capture.
            int score = Integer.MIN_VALUE;
            List<Integer> removals = next.candidateRemovals(opponentCode);
            if (removals.isEmpty()) {
                score = evaluate(next);
            } else {
                for (int rem : removals) {
                    Board c = next.clone();
                    c.getCells()[rem] = Constants.EMPTY;
                    score = Math.max(score, evaluate(c));
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = move;
            }
        }
        return best;
    }

    private Move findImmediateBlockMove(Board board, List<Move> cpuMoves, int opponentCode) {
        List<Move> threatMoves = board.generateLegalMoves(opponentCode);
        List<Integer> threatTargets = new ArrayList<>();
        for (Move threat : threatMoves) {
            Board next = board.clone();
            next.applyMove(threat, opponentCode);
            if (next.formsMill(opponentCode, threat.to)) threatTargets.add(threat.to);
        }

        if (threatTargets.isEmpty()) return null;
        for (Move cpuMove : cpuMoves) {
            if (threatTargets.contains(cpuMove.to)) return cpuMove;
        }
        return null;
    }

    private List<Move> orderMoves(
            Board state,
            List<Move> moves,
            int moverCode,
            int opponentCode,
            boolean descending
    ) {
        List<ScoredMove> scored = new ArrayList<>(moves.size());
        for (Move move : moves) {
            Board c = state.clone();
            c.applyMove(move, moverCode);
            int score = 0;
            if (c.formsMill(moverCode, move.to)) score += 400;
            score += (countNearMills(c, moverCode) - countNearMills(c, opponentCode)) * 20;
            score += (c.generateLegalMoves(moverCode).size() - c.generateLegalMoves(opponentCode).size()) * 3;
            if (Constants.INNER_RING.contains(move.to)) score += 20;
            if (Constants.MIDDLE_RING.contains(move.to)) score += 12;
            scored.add(new ScoredMove(move, score));
        }

        scored.sort((a, b) -> descending
                ? Integer.compare(b.score, a.score)
                : Integer.compare(a.score, b.score));

        List<Move> ordered = new ArrayList<>(moves.size());
        for (ScoredMove sm : scored) ordered.add(sm.move);
        return ordered;
    }

    private List<Integer> orderRemovals(Board state, List<Integer> removals, int moverCode, int opponentCode) {
        List<ScoredRemoval> scored = new ArrayList<>(removals.size());
        for (int rem : removals) {
            Board c = state.clone();
            c.getCells()[rem] = Constants.EMPTY;
            int score = evaluate(c);
            scored.add(new ScoredRemoval(rem, score));
        }
        // Higher board score for mover is better, so try these first.
        scored.sort((a, b) -> Integer.compare(b.score, a.score));

        List<Integer> ordered = new ArrayList<>(removals.size());
        for (ScoredRemoval sr : scored) ordered.add(sr.index);
        return ordered;
    }

    private int countBlockedPieces(Board board, int playerCode) {
        if (isPlacementPhase(board)) return 0;
        int blocked = 0;
        boolean flying = board.countPieces(playerCode) == 3;
        if (flying) return 0;
        for (int i = 0; i < 24; i++) {
            if (board.getCells()[i] != playerCode) continue;
            boolean hasEmptyNeighbor = false;
            for (int nb : Constants.ADJ.get(i)) {
                if (board.getCells()[nb] == Constants.EMPTY) {
                    hasEmptyNeighbor = true;
                    break;
                }
            }
            if (!hasEmptyNeighbor) blocked++;
        }
        return blocked;
    }

    private int largestCluster(Board board, int playerCode) {
        boolean[] visited = new boolean[24];
        int best = 0;
        for (int i = 0; i < 24; i++) {
            if (visited[i] || board.getCells()[i] != playerCode) continue;
            int cluster = dfsCluster(board, i, playerCode, visited);
            if (cluster > best) best = cluster;
        }
        return best;
    }

    private int dfsCluster(Board board, int index, int playerCode, boolean[] visited) {
        visited[index] = true;
        int size = 1;
        for (int nb : Constants.ADJ.get(index)) {
            if (!visited[nb] && board.getCells()[nb] == playerCode) {
                size += dfsCluster(board, nb, playerCode, visited);
            }
        }
        return size;
    }
    public String buildSearchTrace(Board board, Player cpu, Player human, int depthLimit, int nodeLimit) {
        this.cpuCode = cpu.code();
        this.humanCode = human.code();

        int effectiveDepth = Math.max(1, depthLimit);
        List<Move> legalMoves = board.generateLegalMoves(cpuCode);
        TraceContext trace = new TraceContext(nodeLimit);

        trace.out.append("Backtracking Visualizer\n");
        trace.out.append("Depth limit: ").append(effectiveDepth)
                .append(", node cap: ").append(nodeLimit).append("\n");
        trace.out.append("Legal root moves: ").append(legalMoves.size()).append("\n\n");

        if (legalMoves.isEmpty()) {
            trace.out.append("No legal move for CPU.\n");
            return trace.out.toString();
        }

        Move immediateMill = findImmediateMillMove(board, legalMoves, cpuCode, humanCode);
        if (immediateMill != null) {
            trace.out.append("Fast-path immediate mill detected -> ")
                    .append(immediateMill)
                    .append("\n");
        }
        Move immediateBlock = findImmediateBlockMove(board, legalMoves, humanCode);
        if (immediateBlock != null) {
            trace.out.append("Fast-path block threat detected -> ")
                    .append(immediateBlock)
                    .append("\n");
        }
        trace.out.append("\nRoot search:\n");

        List<Move> orderedMoves = orderMoves(board, legalMoves, cpuCode, humanCode, true);
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move move : orderedMoves) {
            trace.out.append("- Try ").append(move).append("\n");
            List<Board> nextStates = applyMoveAndResolveMill(board, move, cpuCode, humanCode);
            int moveScore = Integer.MIN_VALUE;

            for (Board next : nextStates) {
                int score = minimaxTrace(next, effectiveDepth - 1, false, alpha, beta, 1, trace);
                moveScore = Math.max(moveScore, score);
                alpha = Math.max(alpha, moveScore);
                if (alpha >= beta) {
                    trace.out.append("  prune at root (alpha >= beta)\n");
                    break;
                }
            }

            trace.out.append("  score(").append(move).append(") = ").append(moveScore).append("\n");
            if (moveScore > bestScore) {
                bestScore = moveScore;
                bestMove = move;
            }

            if (trace.capped) break;
        }

        trace.out.append("\nChosen move: ").append(bestMove)
                .append(" with score ").append(bestScore).append("\n");
        trace.out.append("Visited nodes: ").append(trace.visited).append("\n");
        if (trace.capped) trace.out.append("(Trace capped by node limit)\n");

        return trace.out.toString();
    }

    private int minimaxTrace(
            Board state,
            int depth,
            boolean isMaximizing,
            int alpha,
            int beta,
            int level,
            TraceContext trace
    ) {
        if (!trace.allowNode()) {
            return evaluate(state);
        }

        String indent = "  ".repeat(Math.max(0, level));
        if (depth == 0 || isTerminal(state)) {
            int eval = evaluate(state);
            trace.out.append(indent)
                    .append("leaf eval=")
                    .append(eval)
                    .append("\n");
            return eval;
        }

        int currentCode = isMaximizing ? cpuCode : humanCode;
        int opponentCode = isMaximizing ? humanCode : cpuCode;
        List<Move> moves = orderMoves(
                state,
                state.generateLegalMoves(currentCode),
                currentCode,
                opponentCode,
                isMaximizing
        );

        if (moves.isEmpty()) {
            int score = isMaximizing ? -WIN_SCORE + depth : WIN_SCORE - depth;
            trace.out.append(indent).append("no moves -> ").append(score).append("\n");
            return score;
        }

        if (isMaximizing) {
            int best = Integer.MIN_VALUE;
            trace.out.append(indent).append("MAX depth=").append(depth).append("\n");
            for (Move move : moves) {
                trace.out.append(indent).append("try ").append(move).append("\n");
                for (Board next : applyMoveAndResolveMill(state, move, currentCode, opponentCode)) {
                    int score = minimaxTrace(next, depth - 1, false, alpha, beta, level + 1, trace);
                    best = Math.max(best, score);
                    alpha = Math.max(alpha, best);
                    if (alpha >= beta) {
                        trace.out.append(indent).append("prune (alpha >= beta)\n");
                        return best;
                    }
                    if (trace.capped) return best;
                }
            }
            return best;
        }

        int best = Integer.MAX_VALUE;
        trace.out.append(indent).append("MIN depth=").append(depth).append("\n");
        for (Move move : moves) {
            trace.out.append(indent).append("try ").append(move).append("\n");
            for (Board next : applyMoveAndResolveMill(state, move, currentCode, opponentCode)) {
                int score = minimaxTrace(next, depth - 1, true, alpha, beta, level + 1, trace);
                best = Math.min(best, score);
                beta = Math.min(beta, best);
                if (alpha >= beta) {
                    trace.out.append(indent).append("prune (alpha >= beta)\n");
                    return best;
                }
                if (trace.capped) return best;
            }
        }
        return best;
    }


    private static class ScoredMove {
        private final Move move;
        private final int score;

        private ScoredMove(Move move, int score) {
            this.move = move;
            this.score = score;
        }
    }

    private static class ScoredRemoval {
        private final int index;
        private final int score;

        private ScoredRemoval(int index, int score) {
            this.index = index;
            this.score = score;
        }
    }
}
 