package morris.ai;

import morris.model.Board;
import morris.model.Move;
import morris.model.Player;

public interface CpuStrategy {
    Move getBestMove(Board board, Player cpu, Player human);
}
