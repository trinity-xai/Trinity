package edu.jhuapl.trinity.data.graph.construction.source;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Deterministic radial initial coordinates for a newly constructed source hierarchy.
 * The source graph remains authoritative: GraphNode.vector is only an initial
 * Euclidean display position, not an ontology or embedding coordinate.
 *
 * Passages are ordered around an outer ring in source order. Structural parents
 * are placed at smaller radii at the mid-angle of their descendant passages,
 * producing visually separated page/section groups even before a 3D layout runs.
 */
public final class SourceHierarchyInitialLayout {
    private SourceHierarchyInitialLayout() { }

    public static void assign(GraphDirectedCollection graph) {
        assign(graph, () -> false);
    }

    public static void assign(GraphDirectedCollection graph, BooleanSupplier cancelled) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(cancelled, "cancelled");
        Map<String, GraphNode> byId = new HashMap<>();
        List<GraphNode> passages = new ArrayList<>();
        String rootId = null;
        for (GraphNode node : graph.getNodes()) {
            check(cancelled);
            byId.put(node.getEntityID(), node);
            if ("DOCUMENT".equals(node.getNodeKind())) rootId = node.getEntityID();
            if ("PASSAGE".equals(node.getNodeKind())) passages.add(node);
        }
        if (rootId == null || passages.isEmpty()) return;
        Map<String, String> parents = new HashMap<>();
        Map<String, List<String>> children = new HashMap<>();
        for (GraphEdge edge : graph.getEdges()) {
            check(cancelled);
            if (byId.containsKey(edge.getStartID()) && byId.containsKey(edge.getEndID())) {
                parents.put(edge.getEndID(), edge.getStartID());
                children.computeIfAbsent(edge.getStartID(), ignored -> new ArrayList<>())
                    .add(edge.getEndID());
            }
        }
        Map<String, Integer> depth = new HashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        depth.put(rootId, 0);
        queue.addLast(rootId);
        int maximumDepth = 1;
        while (!queue.isEmpty()) {
            check(cancelled);
            String id = queue.removeFirst();
            int nextDepth = depth.get(id) + 1;
            for (String child : children.getOrDefault(id, List.of())) {
                if (depth.putIfAbsent(child, nextDepth) == null) {
                    maximumDepth = Math.max(maximumDepth, nextDepth);
                    queue.addLast(child);
                }
            }
        }
        record Range(int first, int last) { }
        Map<String, Range> range = new HashMap<>();
        for (int i = 0; i < passages.size(); i++) {
            check(cancelled);
            String id = passages.get(i).getEntityID();
            int guard = 0;
            while (id != null && guard++ <= byId.size()) {
                Range old = range.get(id);
                range.put(id, old == null ? new Range(i, i)
                    : new Range(Math.min(old.first(), i), Math.max(old.last(), i)));
                id = parents.get(id);
            }
        }
        int count = passages.size();
        // Keep individual passages distinguishable at higher counts without
        // changing their mesh radii or forcing the user's coordinate scale.
        double outerRadius = Math.max(160.0, count * 5.0 / (2.0 * Math.PI));
        int fallback = 0;
        for (GraphNode node : graph.getNodes()) {
            check(cancelled);
            int nodeDepth = depth.getOrDefault(node.getEntityID(), 1);
            Range span = range.get(node.getEntityID());
            double midpoint = span == null ? fallback++ + 0.5
                : (span.first() + span.last() + 1.0) / 2.0;
            double angle = 2.0 * Math.PI * midpoint / Math.max(1, count);
            double fraction = Math.min(1.0, (double) nodeDepth / maximumDepth);
            double radius = outerRadius * fraction;
            double x = nodeDepth == 0 ? 0.0 : radius * Math.cos(angle);
            double y = nodeDepth == 0 ? 0.0 : radius * Math.sin(angle);
            double z = nodeDepth == 0 ? 0.0 : radius * 0.08 * Math.sin(angle * 2.0);
            node.setVector(new ArrayList<>(List.of(x, y, z)));
        }
    }

    private static void check(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
            throw new java.util.concurrent.CancellationException("Initial source layout cancelled");
        }
    }
}
