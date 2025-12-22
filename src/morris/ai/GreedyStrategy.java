package morris.ai;



import morris.model.Board;

import morris.model.Move;

import morris.model.Player;

import morris.util.Constants;



import java.util.*;



public class GreedyStrategy implements CpuStrategy {



    private final Random rnd = new Random();



    @Override

    public Move getBestMove(Board board, Player cpu, Player human) {



        List<Move> moves = board.generateLegalMoves(cpu.code());

        if (moves.isEmpty()) return null;



        Move bestMove = null;

        int bestScore = Integer.MIN_VALUE;



        for (Move m : moves) {

            Board afterMyMove = board.clone();

            afterMyMove.applyMove(m, cpu.code());



            int score = evaluateMyMove(board, afterMyMove, m, cpu, human);



            // tie-break randomness to avoid same pattern

            score += rnd.nextInt(3);



            if (score > bestScore) {

                bestScore = score;

                bestMove = m;

            }

        }



        return bestMove;

    }



    // =========================================================

    // MAIN GREEDY EVALUATION

    // =========================================================

    private int evaluateMyMove(Board before, Board after,

                               Move move, Player cpu, Player human) {



        int score = 0;

        int C = cpu.code();

        int H = human.code();



        // 1️⃣ WIN IMMEDIATELY

        if (after.formsMill(C, move.to))

            score += 1000;



        // 2️⃣ BLOCK OPPONENT IMMEDIATE MILL

        if (opponentHasImmediateMill(before, human))

            score += 800;



        // 3️⃣ PUNISH OPPONENT BEST RESPONSE (KEY FIX)

        int opponentReplyPenalty = worstOpponentReply(after, cpu, human);

        score -= opponentReplyPenalty;



        // 4️⃣ MOBILITY ADVANTAGE

        score += (after.generateLegalMoves(C).size()

                - after.generateLegalMoves(H).size()) * 15;



        // 5️⃣ DOUBLE-MILL POTENTIAL

        score += countTwoInRow(after, cpu) * 60;

        score -= countTwoInRow(after, human) * 80;



        // 6️⃣ GRAPH CENTRALITY

        score += Constants.ADJ.get(move.to).size() * 10;



        // 7️⃣ CONNECTED CLUSTERS

        score += largestCluster(after, C) * 12;

        score -= largestCluster(after, H) * 14;



        // 8️⃣ RING CONTROL

        score += after.countPiecesInList(C, Constants.INNER_RING) * 20;

        score += after.countPiecesInList(C, Constants.MIDDLE_RING) * 10;



        return score;

    }



    // =========================================================

    // OPPONENT BEST REPLY (1-PLY LOOKAHEAD, STILL GREEDY)

    // =========================================================

    private int worstOpponentReply(Board state, Player cpu, Player human) {



        int worst = 0;



        for (Move m : state.generateLegalMoves(human.code())) {

            Board c = state.clone();

            c.applyMove(m, human.code());



            int replyScore = 0;



            if (c.formsMill(human.code(), m.to))

                replyScore += 600;



            replyScore += countTwoInRow(c, human) * 50;

            replyScore += c.generateLegalMoves(human.code()).size() * 5;



            worst = Math.max(worst, replyScore);

        }



        return worst;

    }



    // =========================================================

    // IMMEDIATE MILL CHECK

    // =========================================================

    private boolean opponentHasImmediateMill(Board b, Player human) {

        for (Move m : b.generateLegalMoves(human.code())) {

            Board c = b.clone();

            c.applyMove(m, human.code());

            if (c.hasAnyMill(human.code())) return true;

        }

        return false;

    }



    // =========================================================

    // TWO-IN-A-ROW (FUTURE MILL)

    // =========================================================

    private int countTwoInRow(Board b, Player p) {

        int count = 0;

        for (int[] mill : Constants.MILLS) {

            int own = 0, empty = 0;

            for (int i : mill) {

                if (b.getCells()[i] == p.code()) own++;

                else if (b.getCells()[i] == 0) empty++;

            }

            if (own == 2 && empty == 1) count++;

        }

        return count;

    }



    // =========================================================

    // LARGEST CONNECTED COMPONENT (GRAPH BFS)

    // =========================================================

    private int largestCluster(Board b, int player) {

        boolean[] visited = new boolean[24];

        int max = 0;



        for (int i = 0; i < 24; i++) {

            if (!visited[i] && b.getCells()[i] == player) {

                max = Math.max(max, bfs(b, i, player, visited));

            }

        }

        return max;

    }



    private int bfs(Board b, int start, int p, boolean[] visited) {

        Queue<Integer> q = new ArrayDeque<>();

        q.add(start);

        visited[start] = true;

        int size = 1;



        while (!q.isEmpty()) {

            int u = q.poll();

            for (int nb : Constants.ADJ.get(u)) {

                if (!visited[nb] && b.getCells()[nb] == p) {

                    visited[nb] = true;

                    size++;

                    q.add(nb);

                }

            }

        }

        return size;

    }

}
