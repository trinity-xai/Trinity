package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Alternative, deterministic angular community placements. Computation is restricted
 * to derived community links. The source graph and its original edges are never edited.
 * All outputs are unit directions; the existing hyperbolic radial-step machinery
 * places them inside the open Poincare ball.
 */
final class ContextGraphAdditionalPlacement {
    private static final double GOLDEN = Math.PI * (3.0 - Math.sqrt(5.0));

    private ContextGraphAdditionalPlacement() { }

    /**
     * Three nontrivial components of the normalized weighted adjacency operator.
     * Iteration uses a positive semidefinite lazy walk and removes the stationary
     * component, avoiding a dense eigen-decomposition or external dependencies.
     * Sparse/disconnected graphs retain a deterministic fraction of their seed layout.
     */
    static double[][] spectral(List<Map<Integer, Double>> links, List<Integer> ids,
                               double[][] seed, double[] axis, double maxAngle) {
        int n = ids.size();
        if (n < 3) return copy(seed);
        LocalGraph graph = local(links, ids);
        if (graph.totalWeight == 0) return copy(seed);
        double[] stationary = new double[n];
        for (int i = 0; i < n; i++) stationary[i] = Math.sqrt(graph.degree[i]);
        normalizeVector(stationary);
        double[][] eigen = new double[3][n];
        int components = Math.min(3, n - 1);
        for (int k = 0; k < components; k++) {
            // The deterministic trigonometric seed is stable under source reordering.
            double[] x = new double[n];
            for (int i = 0; i < n; i++)
                x[i] = Math.sin((i + 1) * (k + 1.13) * GOLDEN)
                    + 0.31 * Math.cos((i + 1) * (k + 2.37));
            orthogonalize(x, stationary, eigen, k);
            if (normalizeVector(x) < 1.0e-10) continue;
            double[] next = new double[n];
            for (int iteration = 0; iteration < 144; iteration++) {
                cancelled();
                for (int i = 0; i < n; i++) {
                    double sum = 0;
                    if (graph.degree[i] > 0) {
                        for (Map.Entry<Integer, Double> e : graph.edges.get(i).entrySet()) {
                            int j = e.getKey();
                            sum += e.getValue() * x[j]
                                / Math.sqrt(graph.degree[i] * graph.degree[j]);
                        }
                    }
                    next[i] = 0.5 * (x[i] + sum);
                }
                orthogonalize(next, stationary, eigen, k);
                if (normalizeVector(next) < 1.0e-10) break;
                double[] tmp = x; x = next; next = tmp;
            }
            // Resolve the eigenvector sign using the stable seed direction.
            double alignment = 0;
            for (int i = 0; i < n; i++) alignment += x[i] * seed[i][k];
            if (alignment < 0) for (int i = 0; i < n; i++) x[i] = -x[i];
            eigen[k] = x;
        }
        double[][] result = new double[n][3];
        for (int i = 0; i < n; i++) {
            double[] direction = new double[] {
                eigen[0][i] + 0.12 * seed[i][0],
                eigen[1][i] + 0.12 * seed[i][1],
                eigen[2][i] + 0.12 * seed[i][2]
            };
            normalize3(direction);
            limitCap(direction, axis, maxAngle);
            result[i] = direction;
        }
        return result;
    }

    /**
     * Place a high-weight hub along the sector axis, with community directions
     * increasingly far around it as weighted shortest-path distance increases.
     * Disconnected communities occupy the sector periphery. The layout uses a
     * deterministic golden-angle sweep to distinguish equally distant groups.
     */
    static double[][] graphDistanceRadial(List<Map<Integer, Double>> links,
                                          List<Integer> ids, double[][] seed,
                                          double[] axis, double maxAngle) {
        int n = ids.size();
        if (n < 2) return copy(seed);
        LocalGraph graph = local(links, ids);
        if (graph.totalWeight == 0) return copy(seed);
        int hub = 0;
        for (int i = 1; i < n; i++)
            if (graph.degree[i] > graph.degree[hub] + 1.0e-10) hub = i;
        double[] distance = new double[n];
        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        distance[hub] = 0;
        boolean[] visited = new boolean[n];
        double meanWeight = graph.totalWeight / Math.max(1, graph.edgeCount);
        for (int step = 0; step < n; step++) {
            cancelled();
            int nearest = -1;
            for (int i = 0; i < n; i++)
                if (!visited[i] && (nearest < 0 || distance[i] < distance[nearest])) nearest = i;
            if (nearest < 0 || !Double.isFinite(distance[nearest])) break;
            visited[nearest] = true;
            for (Map.Entry<Integer, Double> e : graph.edges.get(nearest).entrySet()) {
                int next = e.getKey();
                double cost = Math.sqrt(meanWeight / Math.max(1.0e-10, e.getValue()));
                distance[next] = Math.min(distance[next], distance[nearest] + cost);
            }
        }
        double largestFinite = 0;
        for (double d : distance) if (Double.isFinite(d)) largestFinite = Math.max(largestFinite, d);
        for (int i = 0; i < n; i++)
            if (!Double.isFinite(distance[i])) distance[i] = largestFinite + 1.5;
        double max = 0;
        for (double d : distance) max = Math.max(max, d);
        double[] north = axis == null ? new double[]{0, 1, 0} : unit(axis);
        double[] auxiliary = Math.abs(north[1]) < 0.9
            ? new double[]{0, 1, 0} : new double[]{1, 0, 0};
        double[] east = unit(cross(north, auxiliary));
        double[] northEast = unit(cross(north, east));
        List<Integer> ordering = new ArrayList<>();
        for (int i = 0; i < n; i++) ordering.add(i);
        ordering.sort(Comparator.comparingDouble((Integer i) -> distance[i])
            .thenComparingInt(i -> ids.get(i)));
        double[][] result = new double[n][3];
        double aperture = axis == null ? Math.PI * 0.88 : Math.min(maxAngle, Math.PI * 0.88);
        for (int rank = 0; rank < n; rank++) {
            int i = ordering.get(rank);
            double spread = Math.max(distance[i] / Math.max(max, 1.0e-9),
                0.12 * Math.sqrt((double) rank / Math.max(1, n - 1)));
            double theta = aperture * Math.sqrt(spread);
            double az = GOLDEN * rank;
            result[i] = unit(new double[] {
                north[0] * Math.cos(theta) + Math.sin(theta)
                    * (east[0] * Math.cos(az) + northEast[0] * Math.sin(az)),
                north[1] * Math.cos(theta) + Math.sin(theta)
                    * (east[1] * Math.cos(az) + northEast[1] * Math.sin(az)),
                north[2] * Math.cos(theta) + Math.sin(theta)
                    * (east[2] * Math.cos(az) + northEast[2] * Math.sin(az))
            });
        }
        return result;
    }

    private static LocalGraph local(List<Map<Integer, Double>> links, List<Integer> ids) {
        Map<Integer, Integer> index = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) index.put(ids.get(i), i);
        List<Map<Integer, Double>> edges = new ArrayList<>();
        double[] degree = new double[ids.size()];
        double totalWeight = 0;
        int edgeCount = 0;
        for (int i = 0; i < ids.size(); i++) {
            cancelled();
            Map<Integer, Double> adjacent = new java.util.TreeMap<>();
            for (Map.Entry<Integer, Double> e : links.get(ids.get(i)).entrySet()) {
                Integer j = index.get(e.getKey());
                if (j == null || j == i || e.getValue() <= 0) continue;
                adjacent.put(j, e.getValue());
                degree[i] += e.getValue();
                if (j > i) { totalWeight += e.getValue(); edgeCount++; }
            }
            edges.add(adjacent);
        }
        return new LocalGraph(edges, degree, totalWeight, edgeCount);
    }

    private record LocalGraph(List<Map<Integer, Double>> edges, double[] degree,
                              double totalWeight, int edgeCount) { }

    private static void orthogonalize(double[] x, double[] stationary,
                                      double[][] previous, int count) {
        subtractProjection(x, stationary);
        for (int i = 0; i < count; i++) subtractProjection(x, previous[i]);
    }
    private static void subtractProjection(double[] x, double[] basis) {
        double dot = 0;
        for (int i = 0; i < x.length; i++) dot += x[i] * basis[i];
        for (int i = 0; i < x.length; i++) x[i] -= dot * basis[i];
    }
    private static double normalizeVector(double[] x) {
        double norm = 0;
        for (double value : x) norm += value * value;
        norm = Math.sqrt(norm);
        if (norm > 1e-12) for (int i = 0; i < x.length; i++) x[i] /= norm;
        return norm;
    }
    private static double[][] copy(double[][] source) {
        double[][] result = new double[source.length][3];
        for (int i = 0; i < source.length; i++) result[i] = source[i].clone();
        return result;
    }
    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }
    private static double[] unit(double[] a) { double[] v = a.clone(); normalize3(v); return v; }
    private static void normalize3(double[] a) {
        double magnitude = Math.sqrt(a[0]*a[0] + a[1]*a[1] + a[2]*a[2]);
        if (magnitude < 1.0e-12) { a[0] = 0; a[1] = 1; a[2] = 0; }
        else for (int i = 0; i < 3; i++) a[i] /= magnitude;
    }
    private static void limitCap(double[] dir, double[] axis, double maxAngle) {
        if (axis == null || maxAngle >= Math.PI) return;
        double[] north = unit(axis);
        double dot = Math.max(-1, Math.min(1,
            dir[0]*north[0] + dir[1]*north[1] + dir[2]*north[2]));
        if (dot >= Math.cos(maxAngle)) return;
        double[] tangent = new double[]{dir[0]-dot*north[0], dir[1]-dot*north[1],
            dir[2]-dot*north[2]};
        normalize3(tangent);
        for (int i = 0; i < 3; i++)
            dir[i] = north[i]*Math.cos(maxAngle) + tangent[i]*Math.sin(maxAngle);
    }
    private static void cancelled() {
        if (Thread.currentThread().isInterrupted())
            throw new java.util.concurrent.CancellationException("Community placement cancelled");
    }
}
