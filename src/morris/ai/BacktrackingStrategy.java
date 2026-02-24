//-------------------------BacktrackingImplementation-------------------------------------


// LOGIC : 
/*
--------1 ) Choose the piece : picka legal move (placing a piece or moving a piece or flying a piece)
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