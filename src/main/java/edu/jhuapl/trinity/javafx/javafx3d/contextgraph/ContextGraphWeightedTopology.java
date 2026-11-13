package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Stable, undirected, weighted analysis snapshot built only from original graph topology. */
final class ContextGraphWeightedTopology {
    final List<GraphNode> nodes;
    final List<String> entityIds;
    final List<Map<Integer, Double>> adjacency;
    final String[] sourceKeys;
    final int inputEdgeCount;
    final int semanticEdges;
    final int structuralEdges;
    final int administrativeEdges;
    final int components;
    final double totalEdgeWeight;

    private ContextGraphWeightedTopology(List<GraphNode> nodes, List<String> entityIds,
                                         List<Map<Integer, Double>> adjacency, String[] sourceKeys,
                                         int inputEdgeCount, int semanticEdges, int structuralEdges,
                                         int administrativeEdges, int components, double totalEdgeWeight) {
        this.nodes = nodes;
        this.entityIds = entityIds;
        this.adjacency = adjacency;
        this.sourceKeys = sourceKeys;
        this.inputEdgeCount = inputEdgeCount;
        this.semanticEdges = semanticEdges;
        this.structuralEdges = structuralEdges;
        this.administrativeEdges = administrativeEdges;
        this.components = components;
        this.totalEdgeWeight = totalEdgeWeight;
    }

    static ContextGraphWeightedTopology from(GraphDirectedCollection graph,
                                              ContextGraphCommunityParams params) {
        if (graph == null || graph.getNodes() == null)
            throw new IllegalArgumentException("Graph nodes are required");
        List<GraphNode> nodes = new ArrayList<>(graph.getNodes());
        for (GraphNode node : nodes) {
            if (node == null || node.getEntityID() == null || node.getEntityID().isBlank())
                throw new IllegalArgumentException("Every graph node must have a nonblank entity ID");
        }
        nodes.sort(Comparator.comparing(GraphNode::getEntityID));
        int n = nodes.size();
        Map<String, Integer> indices = new HashMap<>();
        List<String> ids = new ArrayList<>(n);
        String[] sources = new String[n];
        for (int i = 0; i < n; i++) {
            String id = nodes.get(i).getEntityID();
            if (indices.putIfAbsent(id, i) != null)
                throw new IllegalArgumentException("Duplicate graph entity ID: " + id);
            ids.add(id);
            sources[i] = sourceKey(nodes.get(i));
        }
        List<ContextGraphWeightedLink> links = new ArrayList<>();
        List<GraphEdge> rawEdges = graph.getEdges();
        if (rawEdges != null) {
            for (GraphEdge edge : rawEdges) {
                if (Thread.currentThread().isInterrupted())
                    throw new java.util.concurrent.CancellationException("Topology analysis cancelled");
                if (edge == null) continue;
                Integer a = indices.get(edge.getStartID());
                Integer b = indices.get(edge.getEndID());
                if (a == null || b == null || a.equals(b)) continue;
                int type = relationshipType(edge.getRelationship());
                double weight = switch (type) {
                    case 0 -> params.administrativeWeight();
                    case 1 -> params.structuralWeight();
                    default -> params.semanticWeight();
                };
                links.add(new ContextGraphWeightedLink(Math.min(a, b), Math.max(a, b),
                    weight, type));
            }
        }
        // Canonical accumulation also makes floating-point summation independent of edge order.
        links.sort(Comparator.comparingInt(ContextGraphWeightedLink::a)
            .thenComparingInt(ContextGraphWeightedLink::b)
            .thenComparingDouble(ContextGraphWeightedLink::weight)
            .thenComparingInt(ContextGraphWeightedLink::type));
        List<Map<Integer, Double>> adjacency = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adjacency.add(new TreeMap<>());
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        int admin = 0, structural = 0, semantic = 0;
        double sum = 0.0;
        for (ContextGraphWeightedLink link : links) {
            if (Thread.currentThread().isInterrupted())
                throw new java.util.concurrent.CancellationException("Topology analysis cancelled");
            adjacency.get(link.a()).merge(link.b(), link.weight(), Double::sum);
            adjacency.get(link.b()).merge(link.a(), link.weight(), Double::sum);
            sum += link.weight();
            if (link.type() == 0) admin++;
            else if (link.type() == 1) structural++;
            else semantic++;
            union(parent, link.a(), link.b());
        }
        int components = 0;
        for (int i = 0; i < n; i++) if (find(parent, i) == i) components++;
        return new ContextGraphWeightedTopology(List.copyOf(nodes), List.copyOf(ids),
            List.copyOf(adjacency), sources, rawEdges == null ? 0 : rawEdges.size(),
            semantic, structural, admin, components, sum);
    }

    static int relationshipType(GraphRelationship relationship) {
        if (relationship == null || relationship.getLabels() == null) return 2;
        for (String raw : relationship.getLabels()) {
            if (raw == null) continue;
            String label = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            if (label.startsWith("INDEXES_") || label.equals("DATASET_SECTION")
                || label.equals("CATALOG_VENDOR") || label.equals("LISTS_PRODUCT")
                || label.equals("HAS_CWE_INDEX")) return 0;
            if (label.equals("SUBTECHNIQUE_OF") || label.equals("SUBCLASS_OF")
                || label.equals("HAS_TACTIC") || label.equals("CLASSIFIES_TECHNIQUE")
                || label.equals("HAS_SECTION") || label.equals("HAS_PARAGRAPH")
                || label.equals("HAS_SENTENCE") || label.equals("HAS_DOCUMENT")
                || label.equals("CONTAINS") || label.equals("PART_OF")) return 1;
        }
        return 2;
    }

    /** Unknown provenance is kept unknown, not guessed from category or tree position. */
    private static String sourceKey(GraphNode node) {
        if (node.getProperties() == null) return null;
        for (String name : List.of("sourceDataset", "source_dataset", "dataset", "source")) {
            Object value = node.getProperties().get(name);
            if (value instanceof String text && !text.isBlank()) return text.trim();
        }
        return null;
    }

    private static int find(int[] parents, int index) {
        int root = index;
        while (parents[root] != root) root = parents[root];
        while (parents[index] != index) {
            int next = parents[index];
            parents[index] = root;
            index = next;
        }
        return root;
    }

    private static void union(int[] parents, int a, int b) {
        int ra = find(parents, a), rb = find(parents, b);
        if (ra != rb) parents[Math.max(ra, rb)] = Math.min(ra, rb);
    }
}

record ContextGraphWeightedLink(int a, int b, double weight, int type) { }
