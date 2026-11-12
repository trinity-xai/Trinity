package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Native weighted undirected Louvain: modularity local moves followed by graph coarsening.
 * Every level remains a partition of original entities (not just supernodes).
 * A[i][i] is twice the sum of internal edges in a coarsened vertex.
 */
final class ContextGraphLouvain {
    private static final double GAIN_EPS = 1e-10;
    private static final int MAX_LOCAL_PASSES = 100;
    private ContextGraphLouvain() { }

    static List<int[]> run(List<Map<Integer, Double>> original, double gamma, int maximumLevels) {
        int n = original.size();
        if (n == 0) return List.of();
        List<int[]> levels = new ArrayList<>();
        List<Map<Integer, Double>> current = original;
        int[] originalToCurrent = new int[n];
        for (int i = 0; i < n; i++) originalToCurrent[i] = i;
        double previousQuality = Double.NEGATIVE_INFINITY;
        for (int level = 0; level < maximumLevels; level++) {
            checkCancelled();
            int[] local = localMove(current, gamma);
            int[] projected = new int[n];
            for (int i = 0; i < n; i++) projected[i] = local[originalToCurrent[i]];
            // Stable community numbering: ascending minimum original entity index.
            int count = canonicalize(projected);
            double quality = modularity(original, projected, gamma);
            if (!levels.isEmpty() && (count >= current.size()
                || quality <= previousQuality + GAIN_EPS)) break;
            levels.add(projected);
            previousQuality = quality;
            if (count <= 1 || count == current.size()) break;
            // Canonical IDs in projected can differ from local, so align the coarse
            // vertex index to the IDs stored in the original-entity partition.
            int[] canonicalCurrent = new int[current.size()];
            Arrays.fill(canonicalCurrent, -1);
            for (int i = 0; i < n; i++) canonicalCurrent[originalToCurrent[i]] = projected[i];
            current = aggregate(current, canonicalCurrent, count);
            originalToCurrent = projected;
        }
        return levels;
    }

    private static int[] localMove(List<Map<Integer, Double>> adjacency, double gamma) {
        int n = adjacency.size();
        int[] membership = new int[n];
        double[] degree = new double[n];
        double[] totals = new double[n];
        double totalDegree = 0;
        for (int i = 0; i < n; i++) {
            membership[i] = i;
            for (double weight : adjacency.get(i).values()) degree[i] += weight;
            totals[i] = degree[i];
            totalDegree += degree[i];
        }
        if (totalDegree == 0) return membership;
        for (int pass = 0; pass < MAX_LOCAL_PASSES; pass++) {
            checkCancelled();
            int moves = 0;
            for (int i = 0; i < n; i++) {
                if ((i & 255) == 0) checkCancelled();
                if (degree[i] == 0) continue;
                int previous = membership[i];
                Map<Integer, Double> neighborCommunities = new TreeMap<>();
                for (Map.Entry<Integer, Double> neighbor : adjacency.get(i).entrySet()) {
                    if (neighbor.getKey() == i) continue; // constant self-loop contribution
                    neighborCommunities.merge(membership[neighbor.getKey()],
                        neighbor.getValue(), Double::sum);
                }
                totals[previous] -= degree[i];
                int chosen = previous;
                double best = neighborCommunities.getOrDefault(previous, 0.0)
                    - gamma * degree[i] * totals[previous] / totalDegree;
                for (Map.Entry<Integer, Double> candidate : neighborCommunities.entrySet()) {
                    int target = candidate.getKey();
                    if (target == previous) continue;
                    double score = candidate.getValue()
                        - gamma * degree[i] * totals[target] / totalDegree;
                    if (score > best + GAIN_EPS) {
                        best = score;
                        chosen = target;
                    }
                }
                membership[i] = chosen;
                totals[chosen] += degree[i];
                if (chosen != previous) moves++;
            }
            if (moves == 0) break;
        }
        return membership;
    }

    /** Remap any community IDs by first original member in ascending entity-ID order. */
    static int canonicalize(int[] membership) {
        Map<Integer, Integer> renamed = new HashMap<>();
        int next = 0;
        for (int i = 0; i < membership.length; i++) {
            Integer id = renamed.get(membership[i]);
            if (id == null) {
                id = next++;
                renamed.put(membership[i], id);
            }
            membership[i] = id;
        }
        return next;
    }

    private static List<Map<Integer, Double>> aggregate(List<Map<Integer, Double>> graph,
                                                         int[] membership, int count) {
        List<Map<Integer, Double>> result = new ArrayList<>(count);
        for (int c = 0; c < count; c++) result.add(new TreeMap<>());
        for (int vertex = 0; vertex < graph.size(); vertex++) {
            if ((vertex & 255) == 0) checkCancelled();
            int group = membership[vertex];
            for (Map.Entry<Integer, Double> entry : graph.get(vertex).entrySet()) {
                result.get(group).merge(membership[entry.getKey()], entry.getValue(), Double::sum);
            }
        }
        return result;
    }

    static double modularity(List<Map<Integer, Double>> adjacency, int[] community,
                             double gamma) {
        if (community.length == 0) return 0.0;
        double totalDegree = 0.0, internal = 0.0;
        Map<Integer, Double> groupTotals = new TreeMap<>();
        for (int i = 0; i < adjacency.size(); i++) {
            double degree = 0.0;
            for (Map.Entry<Integer, Double> edge : adjacency.get(i).entrySet()) {
                degree += edge.getValue();
                if (community[i] == community[edge.getKey()]) internal += edge.getValue();
            }
            totalDegree += degree;
            groupTotals.merge(community[i], degree, Double::sum);
        }
        if (totalDegree == 0) return 0;
        double expected = 0;
        for (double degree : groupTotals.values()) expected += degree * degree;
        return internal / totalDegree - gamma * expected / (totalDegree * totalDegree);
    }

    private static void checkCancelled() {
        if (Thread.currentThread().isInterrupted())
            throw new java.util.concurrent.CancellationException("Community analysis cancelled");
    }
}
