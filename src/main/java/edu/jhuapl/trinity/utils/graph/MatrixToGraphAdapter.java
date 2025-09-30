package edu.jhuapl.trinity.utils.graph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * MatrixToGraphAdapter
 * --------------------
 * Converts a similarity or divergence matrix into a GraphDirectedCollection,
 * and computes node positions using a chosen layout (Static, MDS-3D, Force-FR).
 *
 * New: supports edge sparsification via GraphLayoutParams.EdgePolicy:
 *   - ALL: fully connected (except diagonal)
 *   - KNN: k-nearest neighbors per node (with optional symmetrization)
 *   - EPSILON: keep edges with weight >= epsilon (uses transformed weights)
 *   - MST_PLUS_K: minimum spanning tree (built on 1/weight distances) plus KNN edges
 *
 * Notes:
 *  • We convert the input matrix to a symmetric, non-negative "weight" matrix first
 *    using {@link WeightMode} and {@link MatrixKind}. Policies operate on these weights.
 *  • For MDS, we build a distance matrix via {@link #toDistances(double[][], MatrixKind)}.
 *  • For MST, we use distances derived from the weight matrix: dist = 1/(eps + w).
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

        // 2) Build edges using selected policy and post-constraints
        List<EdgeRec> edgeList = buildEdgesByPolicy(weights, layoutParams, kind);

        // 3) Choose / run layout
        double[][] pos;
        switch (layoutParams.kind) {
            case CIRCLE_XZ -> pos = StaticLayouts.circleXZ(n, layoutParams.radius);
            case CIRCLE_XY -> pos = StaticLayouts.circleXY(n, layoutParams.radius);
            case SPHERE   -> pos = StaticLayouts.sphere(n, layoutParams.radius);
            case MDS_3D   -> {
                // Need distances for MDS
                double[][] distances = toDistances(matrix, kind);
                if (mds3d == null) {
                    // Fallback to sphere if no MDS provided
                    pos = StaticLayouts.sphere(n, layoutParams.radius);
                } else {
                    pos = mds3d.embed(distances, labels);
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

    // ===== Helpers =====

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

    // ===== Edge policies =====

    private static List<EdgeRec> buildEdgesByPolicy(double[][] weights,
                                                    GraphLayoutParams lp,
                                                    MatrixKind kind) {
        return switch (lp.edgePolicy) {
            case ALL -> selectAll(weights, lp);
            case KNN -> selectKnn(weights, lp);
            case EPSILON -> selectByEpsilon(weights, lp);
            case MST_PLUS_K -> selectMstPlusK(weights, lp);
        };
    }

    /** ALL: keep all i<j edges above minEdgeWeight, then enforce caps. */
    private static List<EdgeRec> selectAll(double[][] w, GraphLayoutParams lp) {
        int n = w.length;
        List<EdgeRec> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double wij = w[i][j];
                if (wij > lp.minEdgeWeight) edges.add(new EdgeRec(i, j, wij));
            }
        }
        // Sort by weight desc and enforce caps
        edges.sort(Comparator.comparingDouble((EdgeRec e) -> e.w).reversed());
        return applyGlobalAndDegreeCaps(edges, n, lp.maxEdges, lp.maxDegreePerNode);
        // (Note: this still can be dense; caps limit total/degree.)
    }

    /** KNN: per node, take top-k neighbors by weight; union; optional symmetrization; then caps. */
    private static List<EdgeRec> selectKnn(double[][] w, GraphLayoutParams lp) {
        int n = w.length;
        int k = Math.max(1, lp.knnK);
        // Collect neighbors
        Set<Long> edgeSet = new HashSet<>();  // packed (min,max) -> key
        for (int i = 0; i < n; i++) {
            // top-k for row i
            PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[1]));
            for (int j = 0; j < n; j++) {
                if (j == i) continue;
                double wij = w[i][j];
                if (wij <= lp.minEdgeWeight) continue;
                if (pq.size() < k) {
                    pq.offer(new int[]{j, (int)Double.doubleToLongBits(wij)});
                } else {
                    double smallest = Double.longBitsToDouble(pq.peek()[1]);
                    if (wij > smallest) {
                        pq.poll();
                        pq.offer(new int[]{j, (int)Double.doubleToLongBits(wij)});
                    }
                }
            }
            // add edges
            while (!pq.isEmpty()) {
                int[] entry = pq.poll();
                int j = entry[0];
                double wij = Double.longBitsToDouble(entry[1]);
                addUndirectedEdgeKey(edgeSet, i, j);
            }
        }

        if (lp.knnSymmetrize) {
            // ensure i∈N_k(j) OR j∈N_k(i) → keep both directions already covered by union.
            // (The union above already covers both endpoints’ selections.)
        }

        List<EdgeRec> edges = unpackEdgeSet(edgeSet, w);
        // Sort by weight desc & apply degree/global caps
        edges.sort(Comparator.comparingDouble((EdgeRec e) -> e.w).reversed());
        return applyGlobalAndDegreeCaps(edges, n, lp.maxEdges, lp.maxDegreePerNode);
    }

    /** EPSILON: keep all edges with weight >= epsilon (after transforms), then caps. */
    private static List<EdgeRec> selectByEpsilon(double[][] w, GraphLayoutParams lp) {
        int n = w.length;
        double thr = lp.epsilon;
        List<EdgeRec> edges = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double wij = w[i][j];
                if (wij >= thr && wij > lp.minEdgeWeight)
                    edges.add(new EdgeRec(i, j, wij));
            }
        }
        edges.sort(Comparator.comparingDouble((EdgeRec e) -> e.w).reversed());
        return applyGlobalAndDegreeCaps(edges, n, lp.maxEdges, lp.maxDegreePerNode);
    }

    /** MST_PLUS_K: build MST on distances (1/(eps+w)), then union with KNN(k), then caps. */
    private static List<EdgeRec> selectMstPlusK(double[][] w, GraphLayoutParams lp) {
        int n = w.length;
        // Distances from weights (avoid div by 0)
        final double EPS = 1e-9;
        double[][] dist = new double[n][n];
        for (int i = 0; i < n; i++) {
            dist[i][i] = 0.0;
            for (int j = i + 1; j < n; j++) {
                double wij = w[i][j];
                double dij = 1.0 / (EPS + wij); // large when weight small
                dist[i][j] = dist[j][i] = dij;
            }
        }

        // Build MST via Kruskal on (i,j,dist) ascending
        List<EdgeRec> mst = kruskalMstFromDistances(dist, w);

        // Union with KNN edges
        List<EdgeRec> knn = selectKnn(w, lp); // this already applies caps; we want raw KNN edges first
        // Make a set to union without duplicates
        Set<Long> edgeSet = new HashSet<>();
        for (EdgeRec e : mst) addUndirectedEdgeKey(edgeSet, e.i, e.j);
        for (EdgeRec e : knn) addUndirectedEdgeKey(edgeSet, e.i, e.j);

        List<EdgeRec> union = unpackEdgeSet(edgeSet, w);
        // Sort by weight desc & then apply final caps
        union.sort(Comparator.comparingDouble((EdgeRec e) -> e.w).reversed());
        return applyGlobalAndDegreeCaps(union, n, lp.maxEdges, lp.maxDegreePerNode);
    }

    // ===== Edge utilities =====

    private static List<EdgeRec> kruskalMstFromDistances(double[][] dist, double[][] weights) {
        int n = dist.length;
        List<MstEdge> all = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                all.add(new MstEdge(i, j, dist[i][j]));
            }
        }
        all.sort(Comparator.comparingDouble(e -> e.d));

        UnionFind uf = new UnionFind(n);
        List<EdgeRec> out = new ArrayList<>(n - 1);
        for (MstEdge e : all) {
            if (uf.union(e.i, e.j)) {
                out.add(new EdgeRec(e.i, e.j, weights[e.i][e.j]));
                if (out.size() == n - 1) break;
            }
        }
        return out;
    }

    private static void addUndirectedEdgeKey(Set<Long> set, int i, int j) {
        int a = Math.min(i, j), b = Math.max(i, j);
        long key = (((long) a) << 32) ^ (long) b;
        set.add(key);
    }

    private static List<EdgeRec> unpackEdgeSet(Set<Long> set, double[][] w) {
        List<EdgeRec> list = new ArrayList<>(set.size());
        for (long key : set) {
            int i = (int) (key >> 32);
            int j = (int) (key & 0xFFFFFFFFL);
            list.add(new EdgeRec(i, j, w[i][j]));
        }
        return list;
    }

    private static List<EdgeRec> applyGlobalAndDegreeCaps(List<EdgeRec> edges, int n, int maxEdges, int maxDeg) {
        if (edges.isEmpty()) return edges;
        int[] deg = new int[n];
        List<EdgeRec> out = new ArrayList<>(Math.min(edges.size(), Math.max(1, maxEdges)));
        for (EdgeRec e : edges) {
            if (maxEdges > 0 && out.size() >= maxEdges) break;
            if (maxDeg > 0 && (deg[e.i] >= maxDeg || deg[e.j] >= maxDeg)) continue;
            out.add(e);
            deg[e.i]++; deg[e.j]++;
        }
        return out;
    }

    private record EdgeRec(int i, int j, double w) {}
    private record MstEdge(int i, int j, double d) {}

    private static final class UnionFind {
        private final int[] p, r;
        UnionFind(int n) { p = new int[n]; r = new int[n]; for (int i = 0; i < n; i++) p[i] = i; }
        int find(int x) { return p[x] == x ? x : (p[x] = find(p[x])); }
        boolean union(int a, int b) {
            int pa = find(a), pb = find(b);
            if (pa == pb) return false;
            if (r[pa] < r[pb]) { p[pa] = pb; }
            else if (r[pa] > r[pb]) { p[pb] = pa; }
            else { p[pb] = pa; r[pa]++; }
            return true;
        }
    }

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
                double y = 1.0 - (i / (double)(Math.max(1, n - 1))) * 2.0;
                double radius = Math.sqrt(Math.max(0.0, 1.0 - y*y));
                double theta = phi * i;
                double x = Math.cos(theta) * radius;
                double z = Math.sin(theta) * radius;
                p[i][0] = r * x; p[i][1] = r * y; p[i][2] = r * z;
            }
            return p;
        }
    }

    private static double[][] normalizeToRadius(double[][] pos, double radius) {
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
