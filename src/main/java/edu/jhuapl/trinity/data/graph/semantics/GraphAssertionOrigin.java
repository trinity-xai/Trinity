package edu.jhuapl.trinity.data.graph.semantics;

/** How a statement entered a graph, NOT a statement of its factual truth. */
public enum GraphAssertionOrigin {
    SOURCE_ASSERTED,
    STRUCTURAL_IMPORTED,
    ALGORITHM_DERIVED,
    USER_CURATED
}
