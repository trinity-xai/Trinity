package edu.jhuapl.trinity.utils.graph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MatrixToGraphAdapter
 * --------------------
 * Converts a similarity or divergence matrix into a GraphDirectedCollection,
 * and computes node positions using a chosen layout (Static, MDS-3D, Force-FR).
 *
 * - STATIC layouts (CIRCLE_XZ, CIRCLE_XY, SPHERE) ignore the matrix for positions.
 * - MDS_3D: uses a provided embedding function (plug your existing MDS).
 * - FORCE_FR: uses simple 3D Fruchterman–Reingold with edge weights
 *   derived from the matrix (see WeightMode).
 *
 * @author Sean Phillips
 */
public final class MatrixToGraphAdapter {

    public enum MatrixKind { SIMILARITY, DIVERGENCE }

    /**
     * How to turn matrix entries into edge weights for the graph.
     * SIMILARITY: higher means stronger attraction; DIVERGENCE: lower means stronger attraction.
     */
    public enum WeightMode {
        /** Use raw entries (optionally normalized 0..1). */
        DIRECT,
        /** For divergence: w = 1 / (eps + d). For similarity: same as DIRECT. */
        INVERSE_FOR_DIVERGENCE
    }

    /**
     * Pluggable MDS-3D embedding. Return double[n][3] positions.
     * Bring your own implementation and pass it into build().
     */
    public interface MdsEmbedding3D {
        double[][] embed(double[][] distances, List<String> labels);
    }

    public static GraphDirectedCollection build(double[][] matrix,
                                                List<String> labels,
                                                MatrixKind kind,
                                                GraphLayoutParams layoutParams,
                                                WeightMode weightMode,
                                                MdsEmbedding3D mds3d) {
        int n = (matrix == null) ? 0 : matrix.length;
        if (n == 0) return emptyGraph();

        // Defensive checks
        for (int i = 0; i < n; i++) {
            if (matrix[i] == null || matrix[i].length != n) {
                throw new IllegalArgumentException("Matrix must be square and non-null.");
            }
        }
        if (labels == null || labels.size() != n) labels = defaultLabels(n);

        // 1) Prepare a weight matrix for edges (symmetric, non-negative).
        double[][] weights = toEdgeWeights(matrix, kind, weightMode, layoutParams.normalizeWeights01);

        // 2) Build edges (sparsify by global top-K with per-node degree cap).
        List<EdgeRec> edgeList = selectEdges(weights, layoutParams.maxEdges, layoutParams.maxDegreePerNode, layoutParams.minEdgeWeight);

        // 3) Choose / run layout
        double[][] pos;
        switch (layoutParams.kind) {
            case CIRCLE_XZ -> pos = StaticLayouts.circleXZ(n, layoutParams.radius);
            case CIRCLE_XY -> pos = StaticLayouts.circleXY(n, layoutParams.radius);
            case SPHERE -> pos = StaticLayouts.sphere(n, layoutParams.radius);
            case MDS_3D -> {
                // Need distances for MDS
                double[][] distances = toDistances(matrix, kind);
                if (mds3d == null) {
                    // Fallback to sphere if no MDS provided
                    pos = StaticLayouts.sphere(n, layoutParams.radius);
                } else {
                    pos = mds3d.embed(distances, labels);
                    // Normalize/scale to radius
                    pos = normalizeToRadius(pos, layoutParams.radius);
                }
            }
            case FORCE_FR -> {
                GraphLayoutEngine fr = new ForceFrLayout3D();
                pos = fr.layout(n, labels, null, weights, layoutParams);
                pos = normalizeToRadius(pos, layoutParams.radius); // optional post-scale
            }
            default -> pos = StaticLayouts.sphere(n, layoutParams.radius);
        }

        // 4) Emit GraphDirectedCollection
        GraphDirectedCollection gc = new GraphDirectedCollection();
        gc.setGraphId((kind == MatrixKind.SIMILARITY ? "similarity" : "divergence") + "_N" + n);
        gc.setDefaultNodeColor("#33B5E5FF"); // cyan-ish default
        gc.setDefaultEdgeColor("#A0A0A0FF"); // grey default

        // Nodes
        List<GraphNode> nodes = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            GraphNode gn = new GraphNode();
            gn.setEntityID("n" + i);
            ArrayList<Double> vec = new ArrayList<>(3);
            vec.add(pos[i][0]);
            vec.add(pos[i][1]);
            vec.add(pos[i][2]);
            gn.setVector(vec);

            ArrayList<String> lbls = new ArrayList<>(1);
            lbls.add(labels.get(i));
            gn.setLabels(lbls);

            nodes.add(gn);
        }

        // Edges
        List<GraphEdge> edges = new ArrayList<>(edgeList.size());
        for (EdgeRec e : edgeList) {
            GraphEdge ge = new GraphEdge();
            ge.setStartID("n" + e.i);
            ge.setEndID("n" + e.j);
            ge.setColor(null); // use default or color by weight in renderer
            edges.add(ge);
        }

        gc.setNodes(nodes);
        gc.setEdges(edges);
        return gc;
    }

    // ----- Helpers -----

    private static GraphDirectedCollection emptyGraph() {
        GraphDirectedCollection gc = new GraphDirectedCollection();
        gc.setGraphId("empty");
        gc.setNodes(new ArrayList<>());
        gc.setEdges(new ArrayList<>());
        return gc;
    }

    private static List<String> defaultLabels(int n) {
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add("F" + i);
        return out;
    }

    /** Build symmetric weight matrix for edges from similarity/divergence. */
    private static double[][] toEdgeWeights(double[][] m, MatrixKind kind, WeightMode wmode, boolean normalize01) {
        final int n = m.length;
        double[][] w = new double[n][n];

        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        if (normalize01) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double v = m[i][j];
                    if (Double.isFinite(v)) {
                        if (v < min) min = v;
                        if (v > max) max = v;
                    }
                }
            }
            if (!(max > min)) { max = min + 1e-9; }
        }

        for (int i = 0; i < n; i++) {
            w[i][i] = 0.0;
            for (int j = i + 1; j < n; j++) {
                double v = m[i][j];
                if (normalize01) {
                    v = (v - min) / (max - min);
                    v = Math.max(0.0, Math.min(1.0, v));
                }

                double wij;
                if (kind == MatrixKind.SIMILARITY) {
                    // Similarity: larger value → stronger edge
                    wij = v;
                } else {
                    // Divergence: interpret smaller value as "closer" → stronger edge
                    if (wmode == WeightMode.INVERSE_FOR_DIVERGENCE) {
                        wij = 1.0 / (1e-9 + v);  // map small d -> big weight
                    } else {
                        wij = 1.0 - v;           // map d in [0,1] -> weight (invert)
                    }
                    if (wij < 0) wij = 0;
                }
                w[i][j] = w[j][i] = wij;
            }
        }
        return w;
    }

    /** Distances for MDS: for similarity convert to distance; for divergence use as-is. */
    private static double[][] toDistances(double[][] m, MatrixKind kind) {
        final int n = m.length;
        double[][] d = new double[n][n];

        // Find min/max for similarity normalize
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        if (kind == MatrixKind.SIMILARITY) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double v = m[i][j];
                    if (Double.isFinite(v)) {
                        if (v < min) min = v;
                        if (v > max) max = v;
                    }
                }
            }
            if (!(max > min)) { max = min + 1e-9; }
        }

        for (int i = 0; i < n; i++) {
            d[i][i] = 0.0;
            for (int j = i + 1; j < n; j++) {
                double dij;
                if (kind == MatrixKind.SIMILARITY) {
                    double s = (m[i][j] - min) / (max - min);
                    s = Math.max(0.0, Math.min(1.0, s));
                    // Correlation-like → distance; sqrt(2(1-s)) preserves correlations to Euclidean
                    dij = Math.sqrt(Math.max(0.0, 2.0 * (1.0 - s)));
                } else {
                    // Divergence in [0,1] already acts like a distance
                    dij = Math.max(0.0, m[i][j]);
                }
                d[i][j] = d[j][i] = dij;
            }
        }
        return d;
    }

    /** Pick a sparse edge set: global top-K by weight with per-node degree cap. */
    private static List<EdgeRec> selectEdges(double[][] weights, int maxEdges, int maxDeg, double minW) {
        final int n = weights.length;
        List<EdgeRec> all = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double w = weights[i][j];
                if (w > minW) all.add(new EdgeRec(i, j, w));
            }
        }
        all.sort(Comparator.comparingDouble((EdgeRec e) -> e.w).reversed());

        int[] deg = new int[n];
        List<EdgeRec> out = new ArrayList<>();
        for (EdgeRec e : all) {
            if (maxEdges > 0 && out.size() >= maxEdges) break;
            if (maxDeg > 0 && (deg[e.i] >= maxDeg || deg[e.j] >= maxDeg)) continue;
            out.add(e);
            deg[e.i]++; deg[e.j]++;
        }
        return out;
    }

    private record EdgeRec(int i, int j, double w) {}

    // ---------- Static layouts & helpers ----------

    private static final class StaticLayouts {
        static double[][] circleXZ(int n, double r) {
            double[][] p = new double[n][3];
            for (int i = 0; i < n; i++) {
                double a = (2 * Math.PI * i) / Math.max(1, n);
                p[i][0] = r * Math.cos(a);
                p[i][1] = 0.0;
                p[i][2] = r * Math.sin(a);
            }
            return p;
        }
        static double[][] circleXY(int n, double r) {
            double[][] p = new double[n][3];
            for (int i = 0; i < n; i++) {
                double a = (2 * Math.PI * i) / Math.max(1, n);
                p[i][0] = r * Math.cos(a);
                p[i][1] = r * Math.sin(a);
                p[i][2] = 0.0;
            }
            return p;
        }
        static double[][] sphere(int n, double r) {
            double[][] p = new double[n][3];
            final double phi = Math.PI * (3.0 - Math.sqrt(5.0));
            for (int i = 0; i < n; i++) {
                double y = 1.0 - (i / (double)(n - 1)) * 2.0;
                double radius = Math.sqrt(1.0 - y*y);
                double theta = phi * i;
                double x = Math.cos(theta) * radius;
                double z = Math.sin(theta) * radius;
                p[i][0] = r * x; p[i][1] = r * y; p[i][2] = r * z;
            }
            return p;
        }
    }

    private static double[][] normalizeToRadius(double[][] pos, double radius) {
        // Normalize RMS radius then scale to target radius (keeps aspect, avoids huge spread)
        double s2 = 0.0; int n = pos.length;
        for (int i = 0; i < n; i++) s2 += pos[i][0]*pos[i][0] + pos[i][1]*pos[i][1] + pos[i][2]*pos[i][2];
        double rms = Math.sqrt(s2 / Math.max(1, n));
        double scale = (rms > 1e-9) ? (radius / rms) : 1.0;
        if (scale != 1.0) {
            for (int i = 0; i < n; i++) {
                pos[i][0] *= scale; pos[i][1] *= scale; pos[i][2] *= scale;
            }
        }
        return pos;
    }
}
