
package morris.controller;

import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.Duration;

import morris.ai.CpuStrategy;
import morris.ai.DivideAndConquerStrategy;
import morris.ai.DpStrategy;
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
    private final TextArea commentaryArea;

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
    private Timeline bgTimeline;
    private boolean gameOver = false;
    private static final double CANVAS_SIZE = 620;

    public GameController() {
        board = new Board();

        root = new BorderPane();
        canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        g = canvas.getGraphicsContext2D();

        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f3efe6, #e9decf, #e3d6c3);");

        Label title = new Label("Nine Men Morris");
        title.setStyle("-fx-font-family: 'Cambria'; -fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #342313;");

        status = new Label("Placement phase. Human plays first.");
        status.setStyle("-fx-font-family: 'Cambria'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4a3721; -fx-padding: 8 12 8 12; -fx-background-color: rgba(255, 247, 230, 0.75); -fx-background-radius: 10;");

        humanCoinsLabel = new Label("Your Coins: 9");
        cpuCoinsLabel   = new Label("CPU Coins: 9");
        humanCoinsLabel.setStyle("-fx-font-family: 'Cambria'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1f5b8a;");
        cpuCoinsLabel.setStyle("-fx-font-family: 'Cambria'; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #9a3f2c;");

        algoSelect = new ComboBox<>();
        algoSelect.getItems().addAll("Greedy", "Divide & Conquer", "DP");
        algoSelect.setValue("DP");
        algoSelect.setStyle("-fx-font-family: 'Cambria'; -fx-font-size: 14px; -fx-background-color: #fff9ef; -fx-border-color: #9a7b57; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label algoTitle = new Label("CPU Strategy");
        algoTitle.setStyle("-fx-font-family: 'Cambria'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #5a4028;");

        commentaryArea = new TextArea();
        commentaryArea.setEditable(false);
        commentaryArea.setWrapText(true);
        commentaryArea.setPrefRowCount(6);
        commentaryArea.setStyle("-fx-font-family: 'Cambria'; -fx-font-size: 13px; -fx-control-inner-background: #fffaf1; -fx-text-fill: #3d2f22; -fx-border-color: #ccb38f; -fx-border-radius: 10; -fx-background-radius: 10;");

        algoSelect.setOnAction(e -> {
            updateCpuStrategy();
            addCommentary("CPU strategy set to " + algoSelect.getValue() + ".");
        });
        updateCpuStrategy();

        BorderPane top = new BorderPane();
        top.setPadding(new Insets(12, 16, 12, 16));

        VBox titleBox = new VBox(4, title, status);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        VBox leftBox = new VBox(8, algoTitle, algoSelect, humanCoinsLabel, cpuCoinsLabel);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        HBox topContent = new HBox(24, leftBox, titleBox);
        topContent.setAlignment(Pos.CENTER_LEFT);
        topContent.setStyle("-fx-background-color: rgba(255,250,240,0.70); -fx-background-radius: 14; -fx-padding: 10 14 10 14; -fx-border-color: rgba(140,95,58,0.35); -fx-border-radius: 14;");

        top.setCenter(topContent);

        root.setTop(top);
        StackPane boardContainer = new StackPane(canvas);
        boardContainer.setAlignment(Pos.CENTER);
        boardContainer.setPadding(new Insets(12, 12, 12, 12));
        boardContainer.setStyle("-fx-background-color: rgba(90, 64, 39, 0.20); -fx-background-radius: 16; -fx-border-color: rgba(100, 72, 46, 0.28); -fx-border-radius: 16;");
        root.setCenter(boardContainer);
        Label commentaryTitle = new Label("Move Commentary");
        commentaryTitle.setStyle("-fx-font-family: 'Cambria'; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #503721;");
        VBox commentaryPanel = new VBox(6, commentaryTitle, commentaryArea);
        commentaryPanel.setPadding(new Insets(8, 14, 12, 14));
        commentaryPanel.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(255,249,236,0.92), rgba(244,231,205,0.92)); -fx-border-color: #b8986f; -fx-border-width: 2 0 0 0;");
        root.setBottom(commentaryPanel);

        startBackgroundAnimation();

        setupNodePositions();
        drawBoard();
        addCommentary("Game started. Placement phase begins.");

        Platform.runLater(this::showStartDialog);

        canvas.setOnMouseClicked(e -> {
            if (animating || gameOver) return; // ignore clicks during animations or after game end
            if (currentPlayer != Player.HUMAN) return;
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
            case "DP":
                cpuStrategy = new DpStrategy();
                break;
        }
    }

    // ========================= BOARD LAYOUT =========================

    private void setupNodePositions() {
        double[][] base = {
                {50, 50}, {350, 50}, {650, 50},
                {150, 150}, {350, 150}, {550, 150},
                {250, 250}, {350, 250}, {450, 250},
                {50, 350}, {150, 350}, {250, 350}, {450, 350}, {550, 350}, {650, 350},
                {250, 450}, {350, 450}, {450, 450},
                {150, 550}, {350, 550}, {550, 550},
                {50, 650}, {350, 650}, {650, 650}
        };

        double margin = 58;
        double scale = (canvas.getWidth() - margin * 2) / 600.0;
        for (int i = 0; i < 24; i++) {
            nodePos[i][0] = margin + (base[i][0] - 50) * scale;
            nodePos[i][1] = margin + (base[i][1] - 50) * scale;
        }
    }

    // ========================= DRAWING (WOODEN BOARD + PIECES) =========================

    private void drawBoard() {
        drawBoard(null);
    }

    /**
     * @param hideIndex if non-null, that index is not drawn (for move animation).
     */
    private void drawBoard(Integer hideIndex) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        for (int i = 0; i < 24; i++) {
            minX = Math.min(minX, nodePos[i][0]);
            minY = Math.min(minY, nodePos[i][1]);
            maxX = Math.max(maxX, nodePos[i][0]);
            maxY = Math.max(maxY, nodePos[i][1]);
        }
        double outerPad = 24;
        double innerPad = 8;
        double outerX = minX - outerPad;
        double outerY = minY - outerPad;
        double outerW = (maxX - minX) + outerPad * 2;
        double outerH = (maxY - minY) + outerPad * 2;

        // Table background
        RadialGradient tableBg = new RadialGradient(
                0, 0, 0.5, 0.45, 0.85, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(242, 233, 213)),
                new Stop(0.6, Color.rgb(214, 192, 154)),
                new Stop(1, Color.rgb(172, 145, 108))
        );
        g.setFill(tableBg);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Board base
        LinearGradient boardBg = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(167, 117, 63)),
                new Stop(0.55, Color.rgb(139, 91, 48)),
                new Stop(1, Color.rgb(103, 68, 39))
        );
        g.setFill(boardBg);
        g.fillRoundRect(outerX, outerY, outerW, outerH, 26, 26);

        g.setStroke(Color.rgb(42, 96, 74));
        g.setLineWidth(8);
        g.strokeRoundRect(outerX, outerY, outerW, outerH, 26, 26);

        g.setStroke(Color.rgb(219, 191, 134));
        g.setLineWidth(3);
        g.strokeRoundRect(outerX + innerPad, outerY + innerPad, outerW - innerPad * 2, outerH - innerPad * 2, 20, 20);

        // Board lines
        g.setStroke(Color.rgb(236, 209, 160));
        g.setLineWidth(3.2);
        for (int i = 0; i < 24; i++) {
            for (int nb : Constants.ADJ.get(i)) {
                if (nb > i) {
                    g.strokeLine(nodePos[i][0], nodePos[i][1], nodePos[nb][0], nodePos[nb][1]);
                }
            }
        }

        // Highlight valid moves
        g.setFill(Color.color(0.96, 0.94, 0.74, 0.95));
        for (int d : validDestinations) {
            double x = nodePos[d][0];
            double y = nodePos[d][1];
            g.fillOval(x - 9, y - 9, 18, 18);
        }

        // Highlight removable CPU pieces when human formed a mill
        if (waitingForRemoval) {
            g.setStroke(Color.rgb(255, 210, 125));
            g.setLineWidth(4.0);
            for (int d : removalCandidates) {
                double x = nodePos[d][0];
                double y = nodePos[d][1];
                g.strokeOval(x - 18, y - 18, 36, 36);
            }
        }

        // Highlight selected source
        if (selectedSource != -1) {
            g.setStroke(Color.rgb(247, 204, 96));
            g.setLineWidth(4.5);
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

            // Point base
            g.setFill(Color.rgb(78, 48, 28));
            g.fillOval(x - 8, y - 8, 16, 16);

            // Piece
            if (v == 1) { // HUMAN
                g.setFill(new RadialGradient(
                        0, 0, x - 4, y - 6, 16, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(218, 239, 255)),
                        new Stop(1, Color.rgb(24, 99, 187))
                ));
                g.fillOval(x - 14, y - 14, 28, 28);
                g.setStroke(Color.rgb(8, 44, 90));
                g.setLineWidth(2.5);
                g.strokeOval(x - 14, y - 14, 28, 28);
                g.setStroke(Color.rgb(245, 250, 255));
                g.setLineWidth(1.2);
                g.strokeOval(x - 11.5, y - 11.5, 8, 8);
            } else if (v == 2) { // CPU
                g.setFill(new RadialGradient(
                        0, 0, x - 4, y - 6, 16, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 223, 206)),
                        new Stop(1, Color.rgb(184, 43, 27))
                ));
                g.fillOval(x - 14, y - 14, 28, 28);
                g.setStroke(Color.rgb(96, 17, 10));
                g.setLineWidth(2.5);
                g.strokeOval(x - 14, y - 14, 28, 28);
                g.setStroke(Color.rgb(255, 245, 241));
                g.setLineWidth(1.2);
                g.strokeOval(x - 11.5, y - 11.5, 8, 8);
            }
        }

        // CPU last move highlight
        if (lastCpuTo != null) {
            double x = nodePos[lastCpuTo][0];
            double y = nodePos[lastCpuTo][1];
            g.setStroke(Color.rgb(252, 228, 144));
            g.setLineWidth(4.5);
            g.strokeOval(x - 20, y - 20, 40, 40);
        }

        updateCoinCounters();
    }

    private void drawMovingPiece(double x, double y, int playerCode) {
        g.setFill(playerCode == Player.HUMAN.code()
                ? new RadialGradient(
                        0, 0, x - 4, y - 6, 16, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(218, 239, 255)),
                        new Stop(1, Color.rgb(24, 99, 187))
                )
                : new RadialGradient(
                        0, 0, x - 4, y - 6, 16, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(255, 223, 206)),
                        new Stop(1, Color.rgb(184, 43, 27))
                ));
        g.fillOval(x - 14, y - 14, 28, 28);
        g.setStroke(playerCode == Player.HUMAN.code()
                ? Color.rgb(8, 44, 90)
                : Color.rgb(96, 17, 10));
        g.setLineWidth(2.5);
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
        if (animating || gameOver) return;
        if (currentPlayer != Player.HUMAN) return;

        if (!hasAnyLegalMove(Player.HUMAN)) {
            endGame("Oops! You lost. No legal moves.", false);
            return;
        }

        // REMOVAL MODE
        if (waitingForRemoval) {
            if (removalCandidates.contains(pos)) {
                board.getCells()[pos] = 0;
                addCommentary("Human removed CPU piece at " + nodeName(pos) + ".");
                waitingForRemoval = false;
                removalCandidates.clear();
                drawBoard();
                if (checkCpuDefeatAfterHumanTurn()) return;
                handOverToCpu();
            } else if (board.getCells()[pos] == Player.CPU.code()) {
                status.setText("That CPU piece is protected in a mill. Choose a highlighted piece.");
            } else {
                status.setText("Choose a highlighted CPU piece to remove.");
            }
            return;
        }

        // PLACEMENT PHASE
        boolean placement = isPlacementPhase();

        if (placement) {
            if (!board.isEmpty(pos)) {
                status.setText("Select an empty point.");
                return;
            }

            board.applyMove(Move.placement(pos), Player.HUMAN.code());
            addCommentary("Human placed at " + nodeName(pos) + ".");

            if (board.formsMill(Player.HUMAN.code(), pos)) {
                waitingForRemoval = true;
                removalCandidates = board.candidateRemovals(Player.CPU.code());
                if (removalCandidates.isEmpty()) {
                    waitingForRemoval = false;
                    status.setText("Mill formed, but no removable CPU pieces.");
                    addCommentary("Human formed a mill, but no CPU piece could be removed.");
                    drawBoard();
                    if (checkCpuDefeatAfterHumanTurn()) return;
                    handOverToCpu();
                    return;
                }
                status.setText("Nice! Mill formed. Remove a CPU piece.");
                addCommentary("Human formed a mill and must remove one CPU piece.");
                drawBoard();
                return;
            }

            drawBoard();
            if (checkCpuDefeatAfterHumanTurn()) return;
            handOverToCpu();
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
            addCommentary("Human cancelled piece selection.");
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
                addCommentary("Human moved " + nodeName(m.from) + " -> " + nodeName(m.to) + ".");

                if (board.formsMill(Player.HUMAN.code(), m.to)) {
                    waitingForRemoval = true;
                    removalCandidates = board.candidateRemovals(Player.CPU.code());
                    if (removalCandidates.isEmpty()) {
                    waitingForRemoval = false;
                    status.setText("Mill formed, but no removable CPU pieces.");
                    addCommentary("Human formed a mill, but no CPU piece could be removed.");
                    drawBoard();
                    if (checkCpuDefeatAfterHumanTurn()) return;
                    handOverToCpu();
                } else {
                    status.setText("Nice! Mill formed. Remove a CPU piece.");
                    addCommentary("Human formed a mill and must remove one CPU piece.");
                    drawBoard();
                }
            } else {
                drawBoard();
                if (checkCpuDefeatAfterHumanTurn()) return;
                handOverToCpu();
                }
            });
            return;
        }

        status.setText("Invalid move.");
        addCommentary("Invalid move attempted at " + nodeName(pos) + ".");
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
        if (animating || gameOver) return;
        if (currentPlayer != Player.CPU) return;

        List<Move> cpuMoves = board.generateLegalMoves(Player.CPU.code());
        if (cpuMoves.isEmpty()) {
            endGame("Hurray! You won! CPU has no moves.", true);
            return;
        }

        Move best = cpuStrategy.getBestMove(board, Player.CPU, Player.HUMAN);
        if (best == null) {
            endGame("Hurray! You won! CPU is stuck.", true);
            return;
        }

        lastCpuTo = best.to;

        // PLACEMENT move
        if (best.from == -1) {
            board.applyMove(best, Player.CPU.code());
            addCommentary("CPU placed at " + nodeName(best.to) + ".");

            if (board.formsMill(Player.CPU.code(), best.to)) {
                List<Integer> rem = board.candidateRemovals(Player.HUMAN.code());
                if (!rem.isEmpty()) {
                    int r = rem.get(0);
                    board.getCells()[r] = 0;
                    addCommentary("CPU formed a mill and removed your piece at " + nodeName(r) + ".");
                }
            }

            if (!isPlacementPhase() && board.countPieces(Player.HUMAN.code()) <= 2) {
                drawBoard();
                endGame("Oops! You lost. Only 2 pieces left.", false);
                return;
            }

            drawBoard();
            if (!isPlacementPhase() && !hasAnyLegalMove(Player.HUMAN)) {
                endGame("Oops! You lost. No legal moves.", false);
                return;
            }
            currentPlayer = Player.HUMAN;
            status.setText("Your turn.");
            return;
        }

        // MOVEMENT move → animate
        animateMove(best, Player.CPU.code(), () -> {
            board.applyMove(best, Player.CPU.code());
            addCommentary("CPU moved " + nodeName(best.from) + " -> " + nodeName(best.to) + ".");

            if (board.formsMill(Player.CPU.code(), best.to)) {
                List<Integer> rem = board.candidateRemovals(Player.HUMAN.code());
                if (!rem.isEmpty()) {
                    int r = rem.get(0);
                    board.getCells()[r] = 0;
                    addCommentary("CPU formed a mill and removed your piece at " + nodeName(r) + ".");
                }
            }

            if (!isPlacementPhase() && board.countPieces(Player.HUMAN.code()) <= 2) {
                drawBoard();
                endGame("Oops! You lost. Only 2 pieces left.", false);
                return;
            }

            drawBoard();
            if (!isPlacementPhase() && !hasAnyLegalMove(Player.HUMAN)) {
                endGame("Oops! You lost. No legal moves.", false);
                return;
            }
            currentPlayer = Player.HUMAN;
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

    private boolean hasAnyLegalMove(Player player) {
        List<Move> moves = board.generateLegalMoves(player.code());
        return moves != null && !moves.isEmpty();
    }

    private boolean isPlacementPhase() {
        List<Move> moves = board.generateLegalMoves(Player.HUMAN.code());
        return moves.stream().anyMatch(m -> m.from == -1);
    }

    private boolean checkCpuDefeatAfterHumanTurn() {
        if (!isPlacementPhase() && board.countPieces(Player.CPU.code()) <= 2) {
            endGame("Hurray! You won! CPU has only 2 pieces left.", true);
            return true;
        }
        if (!isPlacementPhase() && !hasAnyLegalMove(Player.CPU)) {
            endGame("Hurray! You won! CPU has no legal moves.", true);
            return true;
        }
        return false;
    }

    private String nodeName(int idx) {
        return "P" + (idx + 1);
    }

    private void addCommentary(String line) {
        if (line == null || line.isBlank()) return;
        if (!commentaryArea.getText().isEmpty()) commentaryArea.appendText("\n");
        commentaryArea.appendText(line);
        commentaryArea.positionCaret(commentaryArea.getText().length());
    }

    private void handOverToCpu() {
        currentPlayer = Player.CPU;
        cpuTurn();
    }

    private void startBackgroundAnimation() {
        if (bgTimeline != null) bgTimeline.stop();
        bgTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f3efe6, #e9decf, #e3d6c3);");
                }),
                new KeyFrame(Duration.seconds(3), e -> {
                    root.setStyle("-fx-background-color: linear-gradient(to bottom right, #efe7d8, #e8dbc6, #ddd0ba);");
                }),
                new KeyFrame(Duration.seconds(6), e -> {
                    root.setStyle("-fx-background-color: linear-gradient(to bottom right, #f3efe6, #e9decf, #e3d6c3);");
                })
        );
        bgTimeline.setCycleCount(Timeline.INDEFINITE);
        bgTimeline.play();
    }

    private void showStartDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
        alert.setTitle("Welcome");
        alert.setHeaderText("Nine Men Morris");
        alert.setContentText("Game start! Placement phase begins.\nYou play first. Good luck!");
        alert.showAndWait();
    }

    private void endGame(String message, boolean humanWon) {
        if (gameOver) return;
        gameOver = true;
        status.setText(message);
        addCommentary(message);
        canvas.setDisable(true);
        algoSelect.setDisable(true);
        if (bgTimeline != null) bgTimeline.stop();
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.OK);
            alert.setTitle("Game Over");
            alert.setHeaderText(humanWon ? "Hurray! You won!" : "Oops! You lost.");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
