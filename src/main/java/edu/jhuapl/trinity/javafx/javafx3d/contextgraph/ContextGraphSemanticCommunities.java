package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Topology-only deterministic weighted modularity grouping for layout hints. */
public final class ContextGraphSemanticCommunities {
    private static final int MAX_PASSES = 16;
    private static final double EPSILON = 1.0e-9;
    private ContextGraphSemanticCommunities() { }
    public record Result(int[] membership, int count) { }

    /** Nodes are sorted by entity ID by the calling layout. */
    public static Result detect(List<GraphNode> nodes, List<GraphEdge> edges,
                                Map<String, Integer> index) {
        int n = nodes.size();
        int[] community = new int[n];
        for (int i = 0; i < n; i++) community[i] = i;
        if (n == 0 || edges == null || edges.isEmpty()) return new Result(community, n);
        List<Map<Integer, Double>> adjacency = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adjacency.add(new TreeMap<>());
        for (GraphEdge edge : edges) {
            if (edge == null) continue;
            Integer a = index.get(edge.getStartID());
            Integer b = index.get(edge.getEndID());
            if (a == null || b == null || a.equals(b)) continue;
            double weight = weight(edge.getRelationship());
            adjacency.get(a).merge(b, weight, Double::sum);
            adjacency.get(b).merge(a, weight, Double::sum);
        }
        double[] strength = new double[n];
        double totalStrength = 0.0;
        for (int i = 0; i < n; i++) {
            for (double weight : adjacency.get(i).values()) strength[i] += weight;
            totalStrength += strength[i];
        }
        if (totalStrength == 0.0) return new Result(community, n);
        double[] communityStrength = strength.clone();
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            int moved = 0;
            for (int i = 0; i < n; i++) {
                if (strength[i] == 0.0) continue;
                int old = community[i];
                Map<Integer, Double> neighbors = new TreeMap<>();
                for (Map.Entry<Integer, Double> neighbor : adjacency.get(i).entrySet()) {
                    neighbors.merge(community[neighbor.getKey()], neighbor.getValue(), Double::sum);
                }
                communityStrength[old] -= strength[i];
                int best = old;
                double bestScore = neighbors.getOrDefault(old, 0.0)
                    - strength[i] * communityStrength[old] / totalStrength;
                for (Map.Entry<Integer, Double> candidate : neighbors.entrySet()) {
                    int target = candidate.getKey();
                    if (target == old) continue;
                    double score = candidate.getValue()
                        - strength[i] * communityStrength[target] / totalStrength;
                    if (score > bestScore + EPSILON) {
                        bestScore = score;
                        best = target;
                    }
                }
                community[i] = best;
                communityStrength[best] += strength[i];
                if (best != old) moved++;
            }
            if (moved == 0) break;
        }
        Map<Integer, Integer> populations = new HashMap<>();
        Map<Integer, Integer> firstMember = new HashMap<>();
        for (int i = 0; i < n; i++) {
            populations.merge(community[i], 1, Integer::sum);
            firstMember.merge(community[i], i, Math::min);
        }
        List<Integer> groups = new ArrayList<>(populations.keySet());
        groups.sort(Comparator.comparingInt((Integer c) -> -populations.get(c))
            .thenComparingInt(firstMember::get));
        Map<Integer, Integer> renamed = new HashMap<>();
        for (int i = 0; i < groups.size(); i++) renamed.put(groups.get(i), i);
        for (int i = 0; i < n; i++) community[i] = renamed.get(community[i]);
        return new Result(community, groups.size());
    }

    /** Downgrade administrative links so semantic connectivity guides clustering. */
    private static double weight(GraphRelationship relationship) {
        if (relationship == null || relationship.getLabels() == null) return 1.0;
        for (String raw : relationship.getLabels()) {
            if (raw == null) continue;
            String label = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            if (label.startsWith("INDEXES_") || label.equals("DATASET_SECTION")
                || label.equals("CATALOG_VENDOR") || label.equals("LISTS_PRODUCT")
                || label.equals("HAS_CWE_INDEX")) return 0.08;
            if (label.equals("SUBTECHNIQUE_OF") || label.equals("SUBCLASS_OF")
                || label.equals("HAS_TACTIC") || label.equals("CLASSIFIES_TECHNIQUE")
                || label.equals("HAS_SECTION") || label.equals("HAS_PARAGRAPH")
                || label.equals("HAS_SENTENCE") || label.equals("HAS_DOCUMENT")
                || label.equals("CONTAINS") || label.equals("PART_OF")) return 0.65;
        }
        return 1.0;
    }
}
