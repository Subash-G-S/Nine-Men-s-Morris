package morris.util;

import java.util.*;

public class Constants {
    // 0 = empty, 1 = human, 2 = cpu
    public static final int EMPTY = 0;
    public static final int HUMAN = 1;
    public static final int CPU = 2;

    // adjacency for 24 nodes (0..23) for standard Nine Men's Morris
    public static final List<List<Integer>> ADJ = createAdj();

    private static List<List<Integer>> createAdj() {
        List<List<Integer>> a = new ArrayList<>();
        for (int i = 0; i < 24; i++) a.add(new ArrayList<>());

        // connections (standard indexing). Source: common Morris layout mapping
        a.get(0).addAll(Arrays.asList(1, 9));
        a.get(1).addAll(Arrays.asList(0, 2, 4));
        a.get(2).addAll(Arrays.asList(1, 14));
        a.get(3).addAll(Arrays.asList(4, 10));
        a.get(4).addAll(Arrays.asList(1, 3, 5, 7));
        a.get(5).addAll(Arrays.asList(4, 13));
        a.get(6).addAll(Arrays.asList(7, 11));
        a.get(7).addAll(Arrays.asList(4, 6, 8));
        a.get(8).addAll(Arrays.asList(7, 12));
        a.get(9).addAll(Arrays.asList(0, 10, 21));
        a.get(10).addAll(Arrays.asList(3, 9, 11, 18));
        a.get(11).addAll(Arrays.asList(6, 10, 15));
        a.get(12).addAll(Arrays.asList(8, 13, 17));
        a.get(13).addAll(Arrays.asList(5, 12, 14, 20));
        a.get(14).addAll(Arrays.asList(2, 13, 23));
        a.get(15).addAll(Arrays.asList(11, 16));
        a.get(16).addAll(Arrays.asList(15, 17, 19));
        a.get(17).addAll(Arrays.asList(12, 16));
        a.get(18).addAll(Arrays.asList(10, 19));
        a.get(19).addAll(Arrays.asList(16, 18, 20, 22));
        a.get(20).addAll(Arrays.asList(13, 19));
        a.get(21).addAll(Arrays.asList(9, 22));
        a.get(22).addAll(Arrays.asList(21, 19, 23));
        a.get(23).addAll(Arrays.asList(14, 22));
        return a;
    }

    // All mill triplets
    public static final int[][] MILLS = {
        {0,1,2},{3,4,5},{6,7,8},{15,16,17},{18,19,20},{21,22,23},
        {0,9,21},{3,10,18},{6,11,15},{2,14,23},{5,13,20},{8,12,17},
        {1,4,7},{16,19,22},{9,10,11},{12,13,14} // last two are center row/col like mills
    };

    // rings used for D&C evaluation (outer, middle, inner)
    public static final List<Integer> OUTER_RING = Arrays.asList(0,1,2,14,23,22,21,9);
    public static final List<Integer> MIDDLE_RING = Arrays.asList(3,4,5,13,20,19,18,10);
    public static final List<Integer> INNER_RING = Arrays.asList(6,7,8,12,17,16,15,11);
}
