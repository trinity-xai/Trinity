package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

/** Stable unordered pair of distinct community IDs at one hierarchy level. */
public record ContextGraphCommunityPair(int first, int second)
        implements Comparable<ContextGraphCommunityPair> {
    public ContextGraphCommunityPair {
        if (first < 0 || second < 0 || first >= second)
            throw new IllegalArgumentException("Expected distinct ordered community IDs");
    }
    @Override public int compareTo(ContextGraphCommunityPair other) {
        int comparison = Integer.compare(first, other.first);
        return comparison != 0 ? comparison : Integer.compare(second, other.second);
    }
}
