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

 