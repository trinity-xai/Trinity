package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

/** The displayed edges are a view filter; all source relationships remain in the model. */
public enum ContextGraphEdgeDisplayMode {
    ALL("All relationships"),
    BACKBONE("Spanning hierarchy only"),
    BACKBONE_WITH_FOCUS("Hierarchy + focused cross-links"),
    COMMUNITY_AGGREGATE("Summarized edges (show all nodes)");

    private final String label;

    ContextGraphEdgeDisplayMode(String label) { this.label = label; }

    @Override public String toString() { return label; }
}
