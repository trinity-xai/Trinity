package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A derived, weighted community-to-community graph at each semantic hierarchy level.
 * Every weight is an aggregation of ORIGINAL graph relationships. No model objects
 * or synthetic relationships are created or attached to the JavaFX scene.
 *
 * The placement solver minimizes a deterministic spherical force objective:
 * weighted-neighbor attraction, short-range angular repulsion, and a weak anchor
 * to the original evenly-spaced starting directions. Children stay inside their
 * parent's angular cap. This is a visualization heuristic, not a hyperbolic
 * geodesic embedding or a claim that every semantic link can be made short.
 */
final class ContextGraphCommunityConnectivity {
    private static final int RELAXATION_STEPS = 96;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
    private static final double ATTRACTION = 1.85;
    private static final double REPULSION = 4.0;
    private static final double ANCHOR = 0.10;

    private final List<List<Map<Integer, Double>>> linksByLevel;

    private ContextGraphCommunityConnectivity(List<List<Map<Integer, Double>>> linksByLevel) {
        this.linksByLevel = linksByLevel;
    }

    static ContextGraphCommunityConnectivity from(GraphDirectedCollection graph,
                                                   ContextGraphCommunityHierarchy hierarchy) {
        ContextGraphCommunityParams weights = hierarchy.params().effectiveAlgorithm()
            == ContextGraphCommunityParams.Algorithm.LEGACY
                ? ContextGraphCommunityParams.defaults() : hierarchy.params();
        ContextGraphWeightedTopology topology = ContextGraphWeightedTopology.from(graph, weights);
        if (!topology.entityIds.equals(hierarchy.entityIds()))
            throw new IllegalArgumentException("Community hierarchy does not match graph identities");
        List<List<Map<Integer, Double>>> levels = new ArrayList<>();
        for (ContextGraphCommunityLevel level : hierarchy.levels()) {
            int count = level.count();
            int[] membership = level.membership();
            List<Map<Integer, Double>> links = new ArrayList<>(count);
            for (int i = 0; i < count; i++) links.add(new TreeMap<>());
            for (int i = 0; i < topology.adjacency.size(); i++) {
                checkCancelled();
                int a = membership[i];
                for (Map.Entry<Integer, Double> edge : topology.adjacency.get(i).entrySet()) {
                    int j = edge.getKey();
                    if (j <= i) continue; // The input adjacency is undirected.
                    int b = membership[j];
                    if (a == b) continue;
                    double w = edge.getValue();
                    links.get(a).merge(b, w, Double::sum);
                    links.get(b).merge(a, w, Double::sum);
                }
            }
            levels.add(List.copyOf(links));
        }
        return new ContextGraphCommunityConnectivity(List.copyOf(levels));
    }

    /**
     * Return optimized unit directions in caller-supplied child order.
     * For top-level groups axis is null and the full sphere is available.
     */
    double[][] arrange(int level, List<Integer> childIds, double[][] initial,
                       double[] axis, double maxAngle) {
        int n = childIds.size();
        if (n <= 1) return copy(initial);
        List<Map<Integer, Double>> links = linksByLevel.get(level);
        Map<Integer, Integer> index = new TreeMap<>();
        for (int i = 0; i < n; i++) index.put(childIds.get(i), i);
        List<Map<Integer, Double>> local = new ArrayList<>(n);
        double[] degree = new double[n];
        boolean connected = false;
        for (int i = 0; i < n; i++) {
            Map<Integer, Double> neighbors = new TreeMap<>();
            for (Map.Entry<Integer, Double> edge : links.get(childIds.get(i)).entrySet()) {
                Integer j = index.get(edge.getKey());
                if (j != null && j != i && edge.getValue() > 0) {
                    neighbors.put(j, edge.getValue());
                    degree[i] += edge.getValue();
                    connected = true;
                }
            }
            local.add(neighbors);
        }
        if (!connected) return copy(initial);

        double[][] p = copy(initial);
        double[][] next = new double[n][3];
        double cosCap = axis == null ? -1 : Math.cos(maxAngle);
        // Each pairwise repulsive term is normalized by group count, so the
        // solver remains stable with large groups. Sampled repulsion avoids
        // quadratic work when a parent has many children.
        int samples = Math.min(n - 1, 32);
        for (int pass = 0; pass < RELAXATION_STEPS; pass++) {
            checkCancelled();
            double step = 0.13 * (1.0 - 0.73 * pass / (RELAXATION_STEPS - 1.0));
            for (int i = 0; i < n; i++) {
                double[] force = new double[3];
                if (degree[i] > 0) {
                    for (Map.Entry<Integer, Double> edge : local.get(i).entrySet()) {
                        double normalizedWeight = edge.getValue() / degree[i];
                        double[] neighbor = p[edge.getKey()];
                        for (int d = 0; d < 3; d++)
                            force[d] += ATTRACTION * normalizedWeight * (neighbor[d] - p[i][d]);
                    }
                }
                // Deterministic circulant sampling: no per-node random seeds or
                // dependence on the original graph input order.
                for (int s = 1; s <= samples; s++) {
                    int j = (i + (int) ((long) s * (n - 1) / samples)) % n;
                    if (i == j) continue;
                    double dx = p[i][0] - p[j][0];
                    double dy = p[i][1] - p[j][1];
                    double dz = p[i][2] - p[j][2];
                    double distance2 = dx * dx + dy * dy + dz * dz;
                    double strength = REPULSION / samples
                        / Math.pow(0.075 + distance2, 1.5);
                    force[0] += dx * strength;
                    force[1] += dy * strength;
                    force[2] += dz * strength;
                }
                for (int d = 0; d < 3; d++)
                    force[d] += ANCHOR * (initial[i][d] - p[i][d]);
                // Tangent projection: integrate on the sphere, not through its interior.
                double dot = force[0] * p[i][0] + force[1] * p[i][1] + force[2] * p[i][2];
                double[] moved = new double[3];
                for (int d = 0; d < 3; d++)
                    moved[d] = p[i][d] + step * (force[d] - dot * p[i][d]);
                normalize(moved);
                if (axis != null) limitToCap(moved, axis, cosCap, maxAngle);
                next[i] = moved;
            }
            double[][] temp = p;
            p = next;
            next = temp;
        }
        return p;
    }

    private static void limitToCap(double[] direction, double[] axis,
                                   double cosine, double angle) {
        double dot = direction[0] * axis[0] + direction[1] * axis[1]
            + direction[2] * axis[2];
        if (dot >= cosine) return;
        double[] tangent = new double[] {direction[0] - axis[0] * dot,
            direction[1] - axis[1] * dot, direction[2] - axis[2] * dot};
        double tangentialLength = Math.sqrt(tangent[0] * tangent[0]
            + tangent[1] * tangent[1] + tangent[2] * tangent[2]);
        if (tangentialLength < 1e-10) {
            double[] other = Math.abs(axis[1]) < 0.9
                ? new double[]{0, 1, 0} : new double[]{1, 0, 0};
            tangent = new double[]{axis[1] * other[2] - axis[2] * other[1],
                axis[2] * other[0] - axis[0] * other[2],
                axis[0] * other[1] - axis[1] * other[0]};
        }
        normalize(tangent);
        for (int d = 0; d < 3; d++)
            direction[d] = axis[d] * cosine + tangent[d] * Math.sin(angle);
    }

    static double weightedAngularDistance(double[][] directions, List<Integer> ids,
                                          List<Map<Integer, Double>> adjacency) {
        double distance = 0, mass = 0;
        Map<Integer, Integer> index = new TreeMap<>();
        for (int i = 0; i < ids.size(); i++) index.put(ids.get(i), i);
        for (int i = 0; i < ids.size(); i++) {
            for (Map.Entry<Integer, Double> edge : adjacency.get(ids.get(i)).entrySet()) {
                Integer j = index.get(edge.getKey());
                if (j == null || j <= i) continue;
                double[] a = directions[i], b = directions[j];
                double dot = Math.max(-1, Math.min(1,
                    a[0] * b[0] + a[1] * b[1] + a[2] * b[2]));
                distance += edge.getValue() * Math.acos(dot);
                mass += edge.getValue();
            }
        }
        return mass > 0 ? distance / mass : Double.NaN;
    }

    List<Map<Integer, Double>> linksAt(int level) {
        return linksByLevel.get(level);
    }

    private static double[][] copy(double[][] source) {
        double[][] copy = new double[source.length][3];
        for (int i = 0; i < source.length; i++) copy[i] = Arrays.copyOf(source[i], 3);
        return copy;
    }

    private static void normalize(double[] value) {
        double norm = Math.sqrt(value[0] * value[0] + value[1] * value[1]
            + value[2] * value[2]);
        if (norm < 1e-12) {
            value[0] = Math.cos(GOLDEN_ANGLE);
            value[1] = 0;
            value[2] = Math.sin(GOLDEN_ANGLE);
        } else {
            for (int d = 0; d < 3; d++) value[d] /= norm;
        }
    }

    private static void checkCancelled() {
        if (Thread.currentThread().isInterrupted())
            throw new java.util.concurrent.CancellationException("Community placement cancelled");
    }
}
