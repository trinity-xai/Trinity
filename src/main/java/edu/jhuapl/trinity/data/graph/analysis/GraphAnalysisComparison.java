package edu.jhuapl.trinity.data.graph.analysis;

import edu.jhuapl.trinity.data.graph.GraphEdge;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Method-comparison COUNTS on two views of the SAME source graph. This is
 * descriptive edge/node overlap, not independent evidence of semantic truth.
 * Cross-graph matching requires explicit source identity reconciliation and
 * belongs to a later document/analysis comparison workflow.
 */
public record GraphAnalysisComparison(int leftNodes, int rightNodes, int commonNodes,
                                      int leftEdges, int rightEdges, int commonEdges) {
    public static GraphAnalysisComparison compare(GraphAnalysisProjection left,
                                                  GraphAnalysisProjection right) {
        if (left == null || right == null || left.sourceGraph() != right.sourceGraph())
            throw new IllegalArgumentException("Compare projections of the SAME source graph only");
        Set<String> ids = new HashSet<>();
        for (GraphAnalysisProjection.Node node : left.nodes()) ids.add(node.entityId());
        int commonNodes = 0;
        for (GraphAnalysisProjection.Node node : right.nodes())
            if (ids.contains(node.entityId())) commonNodes++;
        // Compare actual source edge identity, not list indices: a source graph's
        // edge order can change between independently constructed snapshots.
        Set<GraphEdge> originals = Collections.newSetFromMap(new IdentityHashMap<>());
        originals.addAll(left.selectedOriginalEdges());
        int commonEdges = 0;
        for (GraphEdge edge : right.selectedOriginalEdges())
            if (originals.contains(edge)) commonEdges++;
        return new GraphAnalysisComparison(left.nodes().size(), right.nodes().size(), commonNodes,
            left.links().size(), right.links().size(), commonEdges);
    }

    public double edgeJaccard() {
        int union = leftEdges + rightEdges - commonEdges;
        return union == 0 ? 1.0 : (double) commonEdges / union;
    }
}
