package morris.model;

import java.util.*;
import morris.util.Constants;

public class Board implements Cloneable {
    private final int[] cells = new int[24];
    // counts for placement phase tracking
    private int humanPlaced = 0, cpuPlaced = 0;

    // phase: 0 = placement, 1 = movement, 2 = flying (handled when pieceCount==3)
    private int phase = 0;

    public Board() {
        Arrays.fill(cells, Constants.EMPTY);
    }

    public int[] getCells() { return cells; }

    public Board clone() {
        Board b = new Board();
        System.arraycopy(this.cells, 0, b.cells, 0, 24);
        b.humanPlaced = this.humanPlaced;
        b.cpuPlaced = this.cpuPlaced;
        b.phase = this.phase;
        return b;
    }

    // count pieces for player
    public int countPieces(int player) {
        int c = 0;
        for (int v : cells) if (v == player) c++;
        return c;
    }

    public int countPiecesInList(int player, List<Integer> list) {
        int c = 0;
        for (int idx : list) if (cells[idx] == player) c++;
        return c;
    }

    public boolean isEmpty(int idx) { return cells[idx] == Constants.EMPTY; }
    public void setCell(int idx, int player) { cells[idx] = player; }

    // apply move (assumes legal)
    public void applyMove(Move m, int playerCode) {
        if (m.from == -1) {
            // placement
            cells[m.to] = playerCode;
            if (playerCode == Player.HUMAN.code()) humanPlaced++;
            else cpuPlaced++;
            if (humanPlaced + cpuPlaced >= 18) phase = 1; // finished placement
        } else {
            // normal move
            cells[m.from] = Constants.EMPTY;
            cells[m.to] = playerCode;
        }
        if (m.removed != -1) {
            cells[m.removed] = Constants.EMPTY;
        }
        // flying handled by move generation: if piece count becomes 3, generation allows flying
    }

    public void undoMove(Move m, int prevFromValue, int prevRemovedValue, int playerCode) {
        // revert: this is only used inside minimax clone context; simpler approach is to use clone instead of undo.
    }

    // generate legal moves for player (handles phases)
    public List<Move> generateLegalMoves(int playerCode) {
        List<Move> moves = new ArrayList<>();
        int placed = (playerCode == Player.HUMAN.code()) ? humanPlaced : cpuPlaced;
        int pieces = countPieces(playerCode);
        boolean flying = (pieces == 3);
        if (phase == 0) {
            // placement: any empty cell
            for (int i = 0; i < 24; i++) {
                if (cells[i] == Constants.EMPTY) {
                    // if placing creates a mill, CPU must also choose a removal - but removal is modeled separately when applying
                    moves.add(Move.placement(i));
                }
            }
        } else {
            
            for (int i = 0; i < 24; i++) {
                if (cells[i] == playerCode) {
                    if (flying) {
                        // move to any empty
                        for (int j = 0; j < 24; j++) if (cells[j] == Constants.EMPTY) moves.add(Move.normal(i, j));
                    } else {
                        // move along adjacency
                        for (int nb : Constants.ADJ.get(i)) {
                            if (cells[nb] == Constants.EMPTY) moves.add(Move.normal(i, nb));
                        }
                    }
                }
            }
        }
        return moves;
    }

    // check if placing/moving to 'pos' by 'player' forms a mill
    public boolean formsMill(int playerCode, int pos) {
        for (int[] mill : Constants.MILLS) {
            boolean contains = false;
            for (int m : mill) if (m == pos) { contains = true; break; }
            if (!contains) continue;
            boolean all = true;
            for (int m : mill) {
                if (cells[m] != playerCode) {
                    all = false;
                    break;
                }
            }
            // if pos not yet set in board when simulating, caller should use clone+apply before calling formsMill
            if (all) return true;
        }
        return false;
    }

    // check if any mill exists for player (current board)
    public boolean hasAnyMill(int playerCode) {
        for (int[] mill : Constants.MILLS) {
            boolean all = true;
            for (int m : mill) if (cells[m] != playerCode) { all = false; break; }
            if (all) return true;
        }
        return false;
    }

    // find candidate removal indices when a mill is formed (prefer to remove pieces not in mills)
    public List<Integer> candidateRemovals(int opponentCode) {
        List<Integer> res = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            if (cells[i] == opponentCode && !isPartOfMill(i, opponentCode)) res.add(i);
        }
        if (res.isEmpty()) {
            for (int i = 0; i < 24; i++) if (cells[i] == opponentCode) res.add(i);
        }
        return res;
    }

    public boolean isPartOfMill(int pos, int playerCode) {
        for (int[] mill : Constants.MILLS) {
            boolean contains = false;
            for (int m : mill) if (m == pos) { contains = true; break; }
            if (!contains) continue;
            boolean all = true;
            for (int m : mill) if (cells[m] != playerCode) { all = false; break; }
            if (all) return true;
        }
        return false;
    }

    public boolean isGameOver() {
        // game over if opponent has <=2 pieces or no moves
        int humanPieces = countPieces(Player.HUMAN.code());
        int cpuPieces = countPieces(Player.CPU.code());

        if (humanPieces <= 2 || cpuPieces <= 2) return true;

        // no legal moves for someone
        boolean humanHas = !generateLegalMoves(Player.HUMAN.code()).isEmpty();
        boolean cpuHas = !generateLegalMoves(Player.CPU.code()).isEmpty();
        return !(humanHas && cpuHas);
    }

    // Evaluate heuristic (higher -> better for CPU)
    public int evaluate(int cpuCode, int oppCode) {
        int cpuPieces = countPieces(cpuCode);
        int oppPieces = countPieces(oppCode);
        int mills = countMills(cpuCode) - countMills(oppCode);
        int mobility = generateLegalMoves(cpuCode).size() - generateLegalMoves(oppCode).size();

        return (cpuPieces - oppPieces) * 10 + mills * 8 + mobility * 2;
    }

    public int countMills(int playerCode) {
        int c = 0;
        for (int[] mill : Constants.MILLS) {
            boolean all = true;
            for (int m : mill) if (cells[m] != playerCode) { all = false; break; }
            if (all) c++;
        }
        return c;
    }
    public int[] getMillIndices(int player, int pos) {
        for (int[] mill : Constants.MILLS) {
            boolean contains = false;
            for (int x : mill) if (x == pos) contains = true;

            if (!contains) continue;

            boolean full = true;
            for (int x : mill) if (cells[x] != player) full = false;

            if (full) return mill;
        }
        return null;
    }


    // Utility: get ring lists (used by D&C evaluation)
    public List<Integer> outerRing() { return Constants.OUTER_RING; }
    public List<Integer> middleRing() { return Constants.MIDDLE_RING; }
    public List<Integer> innerRing() { return Constants.INNER_RING; }
}
