package morris.model;

public enum Player {
    HUMAN(1), CPU(2);

    private final int code;
    Player(int c) { this.code = c; }
    public int code() { return code; }
}
