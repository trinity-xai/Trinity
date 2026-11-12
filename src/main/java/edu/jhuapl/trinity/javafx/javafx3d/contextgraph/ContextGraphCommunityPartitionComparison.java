package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Compares two flat selections from computed hierarchies without changing either. */
public final class ContextGraphCommunityPartitionComparison {
    private ContextGraphCommunityPartitionComparison() { }

    /** 1: identical partitions (labels may differ); ~0: chance similarity. */
    public static double adjustedRandIndex(int[] a, int[] b) {
        if (a == null || b == null || a.length != b.length)
            throw new IllegalArgumentException("Membership arrays must have equal length");
        if (a.length < 2) return 1.0;
        Map<Integer, Integer> row = new HashMap<>();
        Map<Integer, Integer> column = new HashMap<>();
        Map<Long, Integer> intersection = new HashMap<>();
        for (int i = 0; i < a.length; i++) {
            row.merge(a[i], 1, Integer::sum);
            column.merge(b[i], 1, Integer::sum);
            long key = ((long) a[i] << 32) | (b[i] & 0xffffffffL);
            intersection.merge(key, 1, Integer::sum);
        }
        double same = 0, left = 0, right = 0;
        for (int count : intersection.values()) same += pairs(count);
        for (int count : row.values()) left += pairs(count);
        for (int count : column.values()) right += pairs(count);
        double expected = left * right / pairs(a.length);
        double denominator = 0.5 * (left + right) - expected;
        if (Math.abs(denominator) < 1e-12) return Math.abs(same - expected) < 1e-12 ? 1.0 : 0.0;
        return (same - expected) / denominator;
    }

    public static String compare(ContextGraphCommunityHierarchy previous,
                                 ContextGraphCommunityHierarchy current) {
        if (previous == null || current == null || previous.levels().isEmpty()
            || current.levels().isEmpty() || !previous.entityIds().equals(current.entityIds())) return "";
        ContextGraphCommunityLevel before = previous.levels().get(previous.selectedLayoutLevel());
        ContextGraphCommunityLevel after = current.levels().get(current.selectedLayoutLevel());
        return String.format(Locale.ROOT,
            "%nPrevious -> current partition: %s / %d groups -> %s / %d groups; "
                + "adjusted Rand index = %.4f (1=identical). "
                + "Q before %.4f, now %.4f (Q is comparable only when relationship weights and resolution match).",
            previous.params().effectiveAlgorithm(), before.count(),
            current.params().effectiveAlgorithm(), after.count(),
            adjustedRandIndex(before.membership(), after.membership()),
            before.modularity(), after.modularity());
    }

    private static double pairs(long count) {
        return count * (count - 1.0) * 0.5;
    }
}
