package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

/** Identity of a derived community, scoped to one hierarchy level, not a graph entity. */
public record ContextGraphCommunityRef(int level, int id) {
    public ContextGraphCommunityRef {
        if (level < 0 || id < 0) throw new IllegalArgumentException("Negative community identity");
    }
}
