package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Display-only edge disclosure calculations. Does not mutate graph data or
 * change adjacency used by neighborhood exploration.
 */
final class ContextGraphEdgeDisclosure {
    private ContextGraphEdgeDisclosure() { }

    static boolean shown(ContextGraphEdgeDisplayMode mode, GraphEdge edge,
                         boolean ready, Set<GraphEdge> backbone, Set<GraphEdge> focus) {
        if (mode == ContextGraphEdgeDisplayMode.ALL || !ready) return true;
        return backbone.contains(edge)
            || (mode == ContextGraphEdgeDisplayMode.BACKBONE_WITH_FOCUS && focus.contains(edge));
    }

    static Set<GraphEdge> focused(GraphDirectedCollection graph, boolean ready,
                                  Set<GraphEdge> backbone, String selectedNodeId,
                                  GraphEdge selectedEdge, int limit,
                                  Set<String> disabledTypes) {
        if (graph == null || !ready || limit <= 0) return Set.of();
        Set<String> selected = new HashSet<>();
        if (selectedNodeId != null) selected.add(selectedNodeId);
        if (selectedEdge != null) {
            selected.add(selectedEdge.getStartID());
            selected.add(selectedEdge.getEndID());
        }
        if (selected.isEmpty()) return Set.of();
        List<GraphEdge> candidates = new ArrayList<>();
        for (GraphEdge edge : graph.getEdges()) {
            if (!backbone.contains(edge)
                && !disabledTypes.contains(relationshipType(edge))
                && (selected.contains(edge.getStartID()) || selected.contains(edge.getEndID()))) {
                candidates.add(edge);
            }
        }
        candidates.sort(Comparator.comparing(ContextGraphEdgeDisclosure::relationshipType)
            .thenComparing(GraphEdge::getStartID)
            .thenComparing(GraphEdge::getEndID)
            .thenComparingLong(edge -> edge.getRelationship() == null ? 0L
                : edge.getRelationship().getIdentity()));
        Set<GraphEdge> chosen = Collections.newSetFromMap(new IdentityHashMap<>());
        if (selectedEdge != null && !backbone.contains(selectedEdge)
            && !disabledTypes.contains(relationshipType(selectedEdge))) chosen.add(selectedEdge);
        for (GraphEdge candidate : candidates) {
            if (chosen.size() >= limit) break;
            chosen.add(candidate);
        }
        return chosen;
    }

    static String relationshipType(GraphEdge edge) {
        if (edge.getRelationship() == null || edge.getRelationship().getLabels() == null
            || edge.getRelationship().getLabels().isEmpty()) return "Uncategorized";
        String joined = String.join(", ", edge.getRelationship().getLabels()).trim();
        return joined.isEmpty() ? "Uncategorized" : joined;
    }
}
