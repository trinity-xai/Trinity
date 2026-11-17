package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

/** Internal computed projection choices. Graph data is never modified. */
public enum ContextGraphProjectionMode {
    EUCLIDEAN("Euclidean / original layout"),
    POINCARE_COMPUTED("Hyperbolic / computed from graph topology");

    private final String displayName;

    ContextGraphProjectionMode(String displayName) { this.displayName = displayName; }

    public boolean isHyperbolic() { return this == POINCARE_COMPUTED; }

    @Override public String toString() { return displayName; }
}
