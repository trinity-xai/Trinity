package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Topology-only, undirected exploration index for a directed graph.
 * Direction and edge identity remain intact in the underlying graph; only
 * neighborhood traversal is bidirectional. Parallel edges are preserved.
 * This class holds no JavaFX nodes or scene references.
 */
final class ContextGraphNeighborhoodIndex {
    private final Map<String, GraphNode> nodesById = new HashMap<>();
    private final Map<String, Set<String>> neighborsById = new HashMap<>();
    private final List<GraphEdge> validEdges = new ArrayList<>();

    ContextGraphNeighborhoodIndex(GraphDirectedCollection graph) {
        if (graph == null) return;
        if (graph.getNodes() != null) {
            for (GraphNode node : graph.getNodes()) {
                if (node == null || node.getEntityID() == null) continue;
                nodesById.put(node.getEntityID(), node);
                neighborsById.computeIfAbsent(node.getEntityID(), ignored -> new LinkedHashSet<>());
            }
        }
        if (graph.getEdges() != null) {
            for (GraphEdge edge : graph.getEdges()) {
                if (edge == null || !nodesById.containsKey(edge.getStartID())
                    || !nodesById.containsKey(edge.getEndID())) continue;
                validEdges.add(edge);
                neighborsById.get(edge.getStartID()).add(edge.getEndID());
                neighborsById.get(edge.getEndID()).add(edge.getStartID());
            }
        }
    }

    GraphNode node(String id) { return nodesById.get(id); }

    Neighborhood traverse(Set<String> roots, int depth) {
        if (roots == null || roots.isEmpty()) return Neighborhood.empty();
        Set<String> reached = new HashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        ArrayDeque<Integer> depths = new ArrayDeque<>();
        for (String root : roots) {
            if (root != null && nodesById.containsKey(root) && reached.add(root)) {
                queue.addLast(root);
                depths.addLast(0);
            }
        }
        while (!queue.isEmpty()) {
            String id = queue.removeFirst();
            int currentDepth = depths.removeFirst();
            if (currentDepth >= depth) continue;
            for (String other : neighborsById.getOrDefault(id, Collections.emptySet())) {
                if (reached.add(other)) {
                    queue.addLast(other);
                    depths.addLast(currentDepth + 1);
                }
            }
        }
        Set<GraphEdge> edges = Collections.newSetFromMap(new IdentityHashMap<>());
        for (GraphEdge edge : validEdges) {
            if (reached.contains(edge.getStartID()) && reached.contains(edge.getEndID())) edges.add(edge);
        }
        return new Neighborhood(reached, edges);
    }

    static final class Neighborhood {
        private final Set<String> nodes;
        private final Set<GraphEdge> edges;

        private Neighborhood(Set<String> nodes, Set<GraphEdge> edges) {
            this.nodes = nodes;
            this.edges = edges;
        }

        static Neighborhood empty() {
            return new Neighborhood(Collections.emptySet(), Collections.emptySet());
        }
        boolean containsNode(String id) { return nodes.contains(id); }
        boolean containsEdge(GraphEdge edge) { return edges.contains(edge); }
        boolean isEmpty() { return nodes.isEmpty(); }
        Set<String> nodeIds() { return Collections.unmodifiableSet(nodes); }
        int nodeCount() { return nodes.size(); }
        int edgeCount() { return edges.size(); }
    }
}
