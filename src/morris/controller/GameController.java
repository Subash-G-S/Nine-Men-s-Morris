package morris.controller;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

import morris.ai.CpuStrategy;
import morris.ai.DivideAndConquerStrategy;
import morris.ai.DpBacktrackingStrategy;
import morris.ai.GreedyStrategy;
import morris.model.Board;
import morris.model.Move;
import morris.model.Player;
import morris.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class GameController {

    // --- UI root + canvas ---
    private final BorderPane root;
    private final Canvas canvas;
    private final GraphicsContext g;

    // --- top + side UI ---
    private final Label status;
    private final Label humanCoinsLabel;
    private final Label cpuCoinsLabel;
    private final ComboBox<String> algoSelect;
    private final TextArea algoInfo;

    // --- game state ---
    private final Board board;
    private Player currentPlayer = Player.HUMAN;
    private CpuStrategy cpuStrategy;
    private final double[][] nodePos = new double[24][2];

    // --- movement UI state ---
    private int selectedSource = -1;
    private List<Integer> validDestinations = new ArrayList<>();
    private boolean waitingForRemoval = false;
    private List<Integer> removalCandidates = new ArrayList<>();

    // --- animation state ---
    private boolean animating = false;
    private Timeline moveTimeline;
    private Integer lastCpuTo = null;

    public GameController() {
        board = new Board();

        root = new BorderPane();
        canvas = new Canvas(900, 700);
        g = canvas.getGraphicsContext2D();

        status = new Label("Placement phase. Human plays first.");
        humanCoinsLabel = new Label("Your Coins: 9");
        cpuCoinsLabel   = new Label("CPU Coins: 9");

        algoSelect = new ComboBox<>();
        algoSelect.getItems().addAll("Greedy", "Divide & Conquer", "DP + Backtracking");
        algoSelect.setValue("Greedy");

        algoInfo = new TextArea();
        algoInfo.setEditable(false);
        algoInfo.setWrapText(true);
        algoInfo.setPrefWidth(260);
        algoInfo.setStyle("-fx-font-size: 14px; -fx-control-inner-background: #f4f4f4;");

        algoSelect.setOnAction(e -> {
            updateCpuStrategy();
            updateAlgorithmExplanation();
        });
        updateCpuStrategy();
        updateAlgorithmExplanation();

        BorderPane top = new BorderPane();
        VBox leftBox = new VBox(5, algoSelect, humanCoinsLabel, cpuCoinsLabel);
        top.setLeft(leftBox);
        top.setRight(status);

        root.setTop(top);
        root.setCenter(canvas);
        root.setRight(algoInfo);

        setupNodePositions();
        drawBoard();

        canvas.setOnMouseClicked(e -> {
            if (animating) return; // ignore clicks during animations
            int clicked = findNearestNode(e.getX(), e.getY());
            if (clicked != -1) {
                handleHumanClick(clicked);
            }
        });
    }

    public BorderPane getRoot() {
        return root;
    }

    // ========================= CPU STRATEGY + EXPLANATION =========================

    private void updateCpuStrategy() {
        switch (algoSelect.getValue()) {
            case "Greedy":
                cpuStrategy = new GreedyStrategy();
                break;
            case "Divide & Conquer":
                cpuStrategy = new DivideAndConquerStrategy();
                break;
            case "DP + Backtracking":
                cpuStrategy = new DpBacktrackingStrategy();
                break;
        }
    }

    private void updateAlgorithmExplanation() {
        String algo = algoSelect.getValue();

        switch (algo) {
            case "Greedy":
                algoInfo.setText(
                    "Algorithm: Graph-Based Greedy\n\n" +
                    "Strategy:\n" +
                    "- Uses adjacency graph (Constants.ADJ)\n" +
                    "- Evaluates each move based on local gain\n" +
                    "- Prioritizes:\n" +
                    "   • Mill formation\n" +
                    "   • Blocking opponent’s mill\n" +
                    "   • Node degree (graph centrality)\n" +
                    "   • Opponent mobility reduction\n" +
                    "   • CPU cluster connectivity\n\n" +
                    "Time Complexity: O(E)\n" +
                    "(E = number of legal moves)\n"
                );
                break;

            case "Divide & Conquer":
                algoInfo.setText(
                    "Algorithm: Sorting + Divide & Conquer\n\n" +
                    "Strategy:\n" +
                    "- DIVIDE the board into 3 subgraphs:\n" +
                    "   • Outer Ring\n" +
                    "   • Middle Ring\n" +
                    "   • Inner Ring\n" +
                    "- CONQUER each region by evaluating moves\n" +
                    "- SORT using recursive Merge Sort\n" +
                    "- MERGE regions into a global ranking\n\n" +
                    "Time Complexity: O(N log N)\n"
                );
                break;

            case "DP + Backtracking":
                algoInfo.setText(
                    "Algorithm: Minimax + DP + Backtracking\n\n" +
                    "Strategy:\n" +
                    "- Uses Minimax game tree search\n" +
                    "- Alpha-Beta Pruning\n" +
                    "- Memoization (DP) for repeated board states\n" +
                    "- Backtracking through move tree\n\n" +
                    "Time Complexity: O(b^d) (reduced by pruning)\n"
                );
                break;
        }
    }

    // ========================= BOARD LAYOUT =========================

    private void setupNodePositions() {
        nodePos[0] = new double[]{50, 50};
        nodePos[1] = new double[]{350, 50};
        nodePos[2] = new double[]{650, 50};

        nodePos[3] = new double[]{150, 150};
        nodePos[4] = new double[]{350, 150};
        nodePos[5] = new double[]{550, 150};

        nodePos[6] = new double[]{250, 250};
        nodePos[7] = new double[]{350, 250};
        nodePos[8] = new double[]{450, 250};

        nodePos[9] = new double[]{50, 350};
        nodePos[10] = new double[]{150, 350};
        nodePos[11] = new double[]{250, 350};
        nodePos[12] = new double[]{450, 350};
        nodePos[13] = new double[]{550, 350};
        nodePos[14] = new double[]{650, 350};

        nodePos[15] = new double[]{250, 450};
        nodePos[16] = new double[]{350, 450};
        nodePos[17] = new double[]{450, 450};

        nodePos[18] = new double[]{150, 550};
        nodePos[19] = new double[]{350, 550};
        nodePos[20] = new double[]{550, 550};

        nodePos[21] = new double[]{50, 650};
        nodePos[22] = new double[]{350, 650};
        nodePos[23] = new double[]{650, 650};
    }

    // ========================= DRAWING (WOODEN BOARD + PIECES) =========================

    private void drawBoard() {
        drawBoard(null);
    }

    /**
     * @param hideIndex if non-null, that index is not drawn (for move animation).
     */
    private void drawBoard(Integer hideIndex) {
        // Wooden background
        LinearGradient woodBg = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(90, 60, 40)),
                new Stop(0.5, Color.rgb(140, 90, 50)),
                new Stop(1, Color.rgb(70, 45, 30))
        );
        g.setFill(woodBg);
        g.fillRect(0, 0, 900, 700);

        // Inner board area with lighter wood
        g.setFill(Color.rgb(190, 140, 90));
        g.fillRoundRect(40, 40, 620, 640, 20, 20);

        // Outer border
        g.setStroke(Color.rgb(60, 35, 20));
        g.setLineWidth(4);
        g.strokeRoundRect(40, 40, 620, 640, 20, 20);

        // Board lines (dark wood)
        g.setStroke(Color.rgb(80, 50, 30));
        g.setLineWidth(3);
        for (int i = 0; i < 24; i++) {
            for (int nb : Constants.ADJ.get(i)) {
                if (nb > i) {
                    g.strokeLine(nodePos[i][0], nodePos[i][1], nodePos[nb][0], nodePos[nb][1]);
                }
            }
        }

        // Highlight valid moves (soft green glow)
        g.setFill(Color.color(0.5, 0.9, 0.5, 0.9));
        for (int d : validDestinations) {
            double x = nodePos[d][0];
            double y = nodePos[d][1];
            g.fillOval(x - 8, y - 8, 16, 16);
        }

        // Highlight selected source
        if (selectedSource != -1) {
            g.setStroke(Color.CORNFLOWERBLUE);
            g.setLineWidth(3);
            double x = nodePos[selectedSource][0];
            double y = nodePos[selectedSource][1];
            g.strokeOval(x - 16, y - 16, 32, 32);
        }

        // Draw board points and pieces
        for (int i = 0; i < 24; i++) {
            if (hideIndex != null && i == hideIndex) {
                continue; // hide this point's piece for animation
            }

            double x = nodePos[i][0];
            double y = nodePos[i][1];
            int v = board.getCells()[i];

            // Point base: peg hole
            g.setFill(Color.rgb(90, 60, 40));
            g.fillOval(x - 10, y - 10, 20, 20);

            // Piece
            if (v == 1) { // HUMAN
                g.setFill(Color.rgb(30, 130, 220));
                g.fillOval(x - 14, y - 14, 28, 28);
                g.setStroke(Color.BLACK);
                g.strokeOval(x - 14, y - 14, 28, 28);
            } else if (v == 2) { // CPU
                g.setFill(Color.rgb(200, 30, 50));
                g.fillOval(x - 14, y - 14, 28, 28);
                g.setStroke(Color.BLACK);
                g.strokeOval(x - 14, y - 14, 28, 28);
            }
        }

        // CPU last move highlight
        if (lastCpuTo != null) {
            double x = nodePos[lastCpuTo][0];
            double y = nodePos[lastCpuTo][1];
            g.setStroke(Color.GOLD);
            g.setLineWidth(4);
            g.strokeOval(x - 20, y - 20, 40, 40);
        }

        updateCoinCounters();
    }

    private void drawMovingPiece(double x, double y, int playerCode) {
        g.setFill(playerCode == Player.HUMAN.code()
                ? Color.rgb(30, 130, 220)
                : Color.rgb(200, 30, 50));
        g.fillOval(x - 14, y - 14, 28, 28);
        g.setStroke(Color.BLACK);
        g.strokeOval(x - 14, y - 14, 28, 28);
    }

    private void updateCoinCounters() {
        int humanCount = board.countPieces(Player.HUMAN.code());
        int cpuCount   = board.countPieces(Player.CPU.code());

        humanCoinsLabel.setText("Your Coins: " + humanCount);
        cpuCoinsLabel.setText("CPU Coins: " + cpuCount);
    }

    // ========================= CLICK LOGIC =========================

    private void handleHumanClick(int pos) {
        if (animating) return;

        // REMOVAL MODE
        if (waitingForRemoval) {
            if (removalCandidates.contains(pos)) {
                board.getCells()[pos] = 0;
                waitingForRemoval = false;
                removalCandidates.clear();
                drawBoard();
                cpuTurn();
            }
            return;
        }

        // PLACEMENT PHASE
        List<Move> moves = board.generateLegalMoves(Player.HUMAN.code());
        boolean placement = moves.stream().anyMatch(m -> m.from == -1);

        if (placement) {
            if (!board.isEmpty(pos)) {
                status.setText("Select an empty point.");
                return;
            }

            board.applyMove(Move.placement(pos), Player.HUMAN.code());

            if (board.formsMill(Player.HUMAN.code(), pos)) {
                waitingForRemoval = true;
                removalCandidates = board.candidateRemovals(Player.CPU.code());
                status.setText("Mill! Remove a CPU piece.");
                drawBoard();
                return;
            }

            drawBoard();
            cpuTurn();
            return;
        }

        // MOVEMENT PHASE

        // No piece selected yet
        if (selectedSource == -1) {
            if (board.getCells()[pos] != Player.HUMAN.code()) {
                status.setText("Select your piece.");
                return;
            }
            selectedSource = pos;
            validDestinations = computeValidMoves(pos);
            status.setText("Choose destination.");
            drawBoard();
            return;
        }

        // Unselect
        if (pos == selectedSource) {
            selectedSource = -1;
            validDestinations.clear();
            drawBoard();
            status.setText("Selection cancelled.");
            return;
        }

        // Valid move
        if (validDestinations.contains(pos)) {
            Move m = Move.normal(selectedSource, pos);
            selectedSource = -1;
            validDestinations.clear();

            // animate HUMAN move, then apply logic
            animateMove(m, Player.HUMAN.code(), () -> {
                board.applyMove(m, Player.HUMAN.code());

                if (board.formsMill(Player.HUMAN.code(), m.to)) {
                    waitingForRemoval = true;
                    removalCandidates = board.candidateRemovals(Player.CPU.code());
                    status.setText("Mill! Remove a CPU piece.");
                    drawBoard();
                } else {
                    drawBoard();
                    cpuTurn();
                }
            });
            return;
        }

        status.setText("Invalid move.");
    }

    // ========================= MOVE OPTIONS =========================

    private List<Integer> computeValidMoves(int pos) {
        List<Integer> list = new ArrayList<>();

        int count = board.countPieces(Player.HUMAN.code());
        boolean flying = (count == 3);

        if (flying) {
            for (int i = 0; i < 24; i++) if (board.isEmpty(i)) list.add(i);
            return list;
        }

        for (int nb : Constants.ADJ.get(pos)) {
            if (board.isEmpty(nb)) list.add(nb);
        }
        return list;
    }

    // ========================= CPU TURN =========================

    private void cpuTurn() {
        if (animating) return;

        List<Move> cpuMoves = board.generateLegalMoves(Player.CPU.code());
        if (cpuMoves.isEmpty()) {
            status.setText("CPU has no moves left. YOU WIN!");
            return;
        }

        Move best = cpuStrategy.getBestMove(board, Player.CPU, Player.HUMAN);
        if (best == null) {
            status.setText("CPU is stuck. YOU WIN!");
            return;
        }

        lastCpuTo = best.to;

        // PLACEMENT move
        if (best.from == -1) {
            board.applyMove(best, Player.CPU.code());

            if (board.formsMill(Player.CPU.code(), best.to)) {
                List<Integer> rem = board.candidateRemovals(Player.HUMAN.code());
                if (!rem.isEmpty()) {
                    int r = rem.get(0);
                    board.getCells()[r] = 0;
                }
            }

            if (board.countPieces(Player.HUMAN.code()) <= 2) {
                drawBoard();
                status.setText("CPU wins. You have only 2 pieces.");
                return;
            }

            drawBoard();
            status.setText("Your turn.");
            return;
        }

        // MOVEMENT move → animate
        animateMove(best, Player.CPU.code(), () -> {
            board.applyMove(best, Player.CPU.code());

            if (board.formsMill(Player.CPU.code(), best.to)) {
                List<Integer> rem = board.candidateRemovals(Player.HUMAN.code());
                if (!rem.isEmpty()) {
                    int r = rem.get(0);
                    board.getCells()[r] = 0;
                }
            }

            if (board.countPieces(Player.HUMAN.code()) <= 2) {
                drawBoard();
                status.setText("CPU wins. You have only 2 pieces.");
                return;
            }

            drawBoard();
            status.setText("Your turn.");
        });
    }

    // ========================= ANIMATION =========================

    private void animateMove(Move m, int playerCode, Runnable afterLogic) {
        if (m.from < 0 || m.to < 0) {
            if (afterLogic != null) afterLogic.run();
            return;
        }

        animating = true;

        double startX = nodePos[m.from][0];
        double startY = nodePos[m.from][1];
        double endX   = nodePos[m.to][0];
        double endY   = nodePos[m.to][1];

        DoubleProperty t = new SimpleDoubleProperty(0.0);

        t.addListener((obs, oldVal, newVal) -> {
            double tt = newVal.doubleValue();
            double x = startX + (endX - startX) * tt;
            double y = startY + (endY - startY) * tt;
            drawBoard(m.from); // hide original piece during animation
            drawMovingPiece(x, y, playerCode);
        });

        if (moveTimeline != null) moveTimeline.stop();

        moveTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,   new KeyValue(t, 0.0)),
                new KeyFrame(Duration.millis(250), new KeyValue(t, 1.0))
        );
        moveTimeline.setOnFinished(e -> {
            animating = false;
            if (afterLogic != null) afterLogic.run();
        });
        moveTimeline.play();
    }

    // ========================= UTIL =========================

    private int findNearestNode(double x, double y) {
        for (int i = 0; i < 24; i++) {
            double dx = x - nodePos[i][0];
            double dy = y - nodePos[i][1];
            if (dx * dx + dy * dy <= 18 * 18) return i;
        }
        return -1;
    }
}
