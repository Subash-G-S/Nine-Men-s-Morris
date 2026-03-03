package morris;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import morris.ai.BacktrackingStrategy;
import morris.ai.CpuStrategy;
import morris.ai.DivideAndConquerStrategy;
import morris.ai.DpStrategy;
import morris.ai.GreedyStrategy;
import morris.model.Board;
import morris.model.Move;
import morris.model.Player;
import morris.util.Constants;

public class ConsoleMain {
    private final Scanner in = new Scanner(System.in);
    private final Board board = new Board();
    private CpuStrategy cpuStrategy;

    public static void main(String[] args) {
        new ConsoleMain().run();
    }

    private void run() {
        chooseStrategy();
        println("Nine Men's Morris (Console)");
        println("You are H, CPU is C. Nodes are numbered 1..24.");
        printBoard();

        Player current = Player.HUMAN;

        while (true) {
            if (current == Player.HUMAN) {
                if (!hasAnyLegalMove(Player.HUMAN)) {
                    println("You lose: no legal moves.");
                    break;
                }
                humanTurn();
                if (isCpuDefeated()) {
                    println("You win!");
                    break;
                }
                current = Player.CPU;
            } else {
                if (!hasAnyLegalMove(Player.CPU)) {
                    println("You win: CPU has no legal moves.");
                    break;
                }
                cpuTurn();
                if (isHumanDefeated()) {
                    println("You lose.");
                    break;
                }
                current = Player.HUMAN;
            }

            printBoard();
        }
    }

    private void chooseStrategy() {
        println("Choose CPU strategy:");
        println("1) Greedy");
        println("2) Divide & Conquer");
        println("3) DP (recommended)");
        println("4) Backtracking");
        int ch = readIntInRange("Enter 1-4: ", 1, 4);
        switch (ch) {
            case 1:
                cpuStrategy = new GreedyStrategy();
                println("Using Greedy.");
                break;
            case 2:
                cpuStrategy = new DivideAndConquerStrategy();
                println("Using Divide & Conquer.");
                break;
            case 3:
                cpuStrategy = new DpStrategy();
                println("Using DP.");
                break;
            case 4:
            default:
                cpuStrategy = new BacktrackingStrategy();
                println("Using Backtracking.");
                break;
        }
    }

    private void humanTurn() {
        boolean placement = isPlacementPhase();
        if (placement) {
            int to = readPlacement();
            Move m = Move.placement(to);
            board.applyMove(m, Player.HUMAN.code());
            println("You placed at " + nodeName(to) + ".");
            if (board.formsMill(Player.HUMAN.code(), to)) {
                int rem = readRemoval(Player.CPU.code());
                board.getCells()[rem] = Constants.EMPTY;
                println("You removed CPU piece at " + nodeName(rem) + ".");
            }
            return;
        }

        boolean flying = board.countPieces(Player.HUMAN.code()) == 3;
        int from = readSource(flying);
        int to = readDestination(from, flying);

        Move m = Move.normal(from, to);
        board.applyMove(m, Player.HUMAN.code());
        println("You moved " + nodeName(from) + " -> " + nodeName(to) + ".");

        if (board.formsMill(Player.HUMAN.code(), to)) {
            int rem = readRemoval(Player.CPU.code());
            board.getCells()[rem] = Constants.EMPTY;
            println("You removed CPU piece at " + nodeName(rem) + ".");
        }
    }

    private void cpuTurn() {
        Move best = cpuStrategy.getBestMove(board, Player.CPU, Player.HUMAN);
        if (best == null) {
            println("CPU has no move.");
            return;
        }

        board.applyMove(best, Player.CPU.code());
        if (best.from == -1) {
            println("CPU placed at " + nodeName(best.to) + ".");
        } else {
            println("CPU moved " + nodeName(best.from) + " -> " + nodeName(best.to) + ".");
        }

        if (board.formsMill(Player.CPU.code(), best.to)) {
            List<Integer> removals = board.candidateRemovals(Player.HUMAN.code());
            if (!removals.isEmpty()) {
                int rem = removals.get(0);
                board.getCells()[rem] = Constants.EMPTY;
                println("CPU formed a mill and removed your piece at " + nodeName(rem) + ".");
            }
        }
    }

    private int readPlacement() {
        while (true) {
            int idx = readIntInRange("Place at node (1-24): ", 1, 24) - 1;
            if (board.isEmpty(idx)) return idx;
            println("That node is not empty.");
        }
    }

    private int readSource(boolean flying) {
        while (true) {
            int from = readIntInRange("Move from node (1-24): ", 1, 24) - 1;
            if (board.getCells()[from] != Player.HUMAN.code()) {
                println("That is not your piece.");
                continue;
            }
            List<Integer> moves = legalDestinations(from, flying, Player.HUMAN.code());
            if (moves.isEmpty()) {
                println("Selected piece has no legal destination.");
                continue;
            }
            return from;
        }
    }

    private int readDestination(int from, boolean flying) {
        List<Integer> allowed = legalDestinations(from, flying, Player.HUMAN.code());
        println("Allowed destinations: " + formatNodeList(allowed));
        while (true) {
            int to = readIntInRange("Move to node (1-24): ", 1, 24) - 1;
            if (allowed.contains(to)) return to;
            println("Invalid destination.");
        }
    }

    private int readRemoval(int opponentCode) {
        List<Integer> removable = board.candidateRemovals(opponentCode);
        if (removable.isEmpty()) return -1;
        println("Removable CPU nodes: " + formatNodeList(removable));
        while (true) {
            int rem = readIntInRange("Remove node (1-24): ", 1, 24) - 1;
            if (removable.contains(rem)) return rem;
            println("That piece cannot be removed now.");
        }
    }

    private List<Integer> legalDestinations(int from, boolean flying, int playerCode) {
        List<Integer> result = new ArrayList<>();
        if (flying) {
            for (int i = 0; i < 24; i++) {
                if (board.isEmpty(i)) result.add(i);
            }
            return result;
        }
        for (int nb : Constants.ADJ.get(from)) {
            if (board.isEmpty(nb)) result.add(nb);
        }
        return result;
    }

    private boolean isPlacementPhase() {
        List<Move> moves = board.generateLegalMoves(Player.HUMAN.code());
        return moves.stream().anyMatch(m -> m.from == -1);
    }

    private boolean hasAnyLegalMove(Player p) {
        return !board.generateLegalMoves(p.code()).isEmpty();
    }

    private boolean isCpuDefeated() {
        if (isPlacementPhase()) return false;
        return board.countPieces(Player.CPU.code()) <= 2 || !hasAnyLegalMove(Player.CPU);
    }

    private boolean isHumanDefeated() {
        if (isPlacementPhase()) return false;
        return board.countPieces(Player.HUMAN.code()) <= 2 || !hasAnyLegalMove(Player.HUMAN);
    }

    private void printBoard() {
        println("");
        println("Board snapshot (node:symbol)");
        for (int i = 0; i < 24; i++) {
            String symbol = switch (board.getCells()[i]) {
                case 1 -> "H";
                case 2 -> "C";
                default -> ".";
            };
            System.out.printf("%2d:%s  ", i + 1, symbol);
            if ((i + 1) % 6 == 0) System.out.println();
        }
        println("");
        println("Pieces -> You: " + board.countPieces(Player.HUMAN.code()) + " | CPU: " + board.countPieces(Player.CPU.code()));
    }

    private String formatNodeList(List<Integer> zeroBased) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < zeroBased.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(zeroBased.get(i) + 1);
        }
        return sb.toString();
    }

    private int readIntInRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = in.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) {
            }
            println("Please enter a number from " + min + " to " + max + ".");
        }
    }

    private static void println(String s) {
        System.out.println(s);
    }

    private static String nodeName(int idx) {
        return "P" + (idx + 1);
    }
}
