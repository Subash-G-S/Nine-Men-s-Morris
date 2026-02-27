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

 