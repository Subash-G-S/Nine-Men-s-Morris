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

    private int maxDepth;
    private int cpuCode;
    private int humanCode;
    private final int MILL_VALUE = 100;
    private final int PIECE_VALUE = 20;
    private final int MOBILITY_WEIGHT = 5;
    private final int BLOCK_VALUE = 150;
    private Stack<Move> moveHistory; 
    // METHODS (Declarations Only)
    @Override
    public Move getBestMove(Board board, Player cpu, Player human) {
        return null;
    }
    private int minimax(Board state, int depth, boolean isMaximizing) {
        return 0;
    }
    private int evaluate(Board state) {
        return 0;
    }
}
