package morris.model;

/**
 * Move representation:
 * - placement: from = -1, to = index where piece placed
 * - normal move: from = orig index, to = dest index
 * - removal (capture): use removed field set to index removed (or -1 if none)
 */
public class Move {
    public final int from;
    public final int to;
    public final int removed; // -1 if none

    public Move(int from, int to, int removed) {
        this.from = from;
        this.to = to;
        this.removed = removed;
    }

    public static Move placement(int to) {
        return new Move(-1, to, -1);
    }

    public static Move normal(int from, int to) {
        return new Move(from, to, -1);
    }

    public Move withRemoval(int rem) {
        return new Move(this.from, this.to, rem);
    }

    @Override
    public String toString() {
        return "Move(" + from + "->" + to + ", remove=" + removed + ")";
    }
}
