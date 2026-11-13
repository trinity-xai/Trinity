package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

/**
 * Independent source-node visibility policy.
 *
 * Aggregating or disclosing graph relationships never causes original nodes to
 * be replaced by community centroids. The only supported restrictions here are
 * the existing FactorLabel visibility and explicit neighborhood isolation.
 * This policy is kept separate from the edge disclosure implementation to
 * prevent accidental re-coupling of node and edge display modes.
 */
public final class ContextGraphCommunityDisclosurePolicy {
    private ContextGraphCommunityDisclosurePolicy() { }

    public static boolean originalVisible(boolean labelVisible, boolean isolateSelection,
                                          boolean neighborhoodEmpty, boolean inNeighborhood) {
        return labelVisible && (!isolateSelection || neighborhoodEmpty || inNeighborhood);
    }
}
