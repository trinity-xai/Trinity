package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.graph.MatrixToGraphAdapter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static edu.jhuapl.trinity.utils.AnalysisUtils.clamp01;

/**
 * SyntheticMatrixFactory
 * ----------------------
 * Utilities for producing small, controllable synthetic matrices and cohorts
 * that are visually easy to validate with the 3D graph layouts.
 * <p>
 * Provided generators:
 * 1) twoClustersSimilarity / kClustersSimilarity  (block-diagonal similarity)
 * 2) ringSimilarity                               (circular 1D manifold)
 * 3) threeClustersDivergence                      (triangular separation)
 * 4) gridSimilarity                               (2D lattice with RBF kernel)
 * 5) makeCohorts_GaussianVsUniform                (cohorts; useful for divergence workflows)
 * <p>
 * All matrix builders return a SyntheticMatrix bundle containing:
 * - double[][] matrix
 * - List<String> labels (one per feature)
 * - List<Integer> clusterIds (optional ground-truth: cluster index, -1 if not applicable)
 * - MatrixKind (SIMILARITY or DIVERGENCE)
 * - title (brief description)
 * - OPTIONAL: cohortA / cohortB (if you attach them)
 * <p>
 * Notes:
 * - Similarity matrices are normalized to [0,1], symmetric, and have 1.0 on the diagonal.
 * - Divergence matrices are normalized to [0,1], symmetric, and have 0.0 on the diagonal.
 * - Jitter adds small random noise to off-diagonals to break ties / make layouts less rigid.
 */
public final class SyntheticMatrixFactory {

    private SyntheticMatrixFactory() {
    }

    // ---------------------------------------------------------------------
    // Result bundle
    // ---------------------------------------------------------------------

    public static final class SyntheticMatrix implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public final double[][] matrix;
        public final List<String> labels;
        public final List<Integer> clusterIds; // may be null or size N (useful for coloring)
        public final MatrixToGraphAdapter.MatrixKind kind;
        public final String title;

        /**
         * Optional cohorts (may be null). If present, PairwiseMatrixView can fall back to them for JPDF/ΔPDF.
         */
        public final List<FeatureVector> cohortA;
        public final List<FeatureVector> cohortB;

        public SyntheticMatrix(double[][] matrix,
                               List<String> labels,
                               List<Integer> clusterIds,
                               MatrixToGraphAdapter.MatrixKind kind,
                               String title) {
            this(matrix, labels, clusterIds, kind, title, null, null);
        }

        public SyntheticMatrix(double[][] matrix,
                               List<String> labels,
                               List<Integer> clusterIds,
                               MatrixToGraphAdapter.MatrixKind kind,
                               String title,
                               List<FeatureVector> cohortA,
                               List<FeatureVector> cohortB) {
            this.matrix = matrix;
            this.labels = labels;
            this.clusterIds = clusterIds;
            this.kind = kind;
            this.title = title;
            this.cohortA = cohortA;
            this.cohortB = cohortB;
        }

        /**
         * Return a copy of this SyntheticMatrix with cohorts attached (immutably).
         */
        public SyntheticMatrix withCohorts(List<FeatureVector> a, List<FeatureVector> b) {
            return new SyntheticMatrix(matrix, labels, clusterIds, kind, title,
                a != null ? new ArrayList<>(a) : null,
                b != null ? new ArrayList<>(b) : null);
        }

        /**
         * Return a copy with a modified title (e.g., to annotate how cohorts were produced).
         */
        public SyntheticMatrix withTitle(String extra) {
            String t = (extra == null || extra.isBlank()) ? title : (title + " — " + extra);
            return new SyntheticMatrix(matrix, labels, clusterIds, kind, t, cohortA, cohortB);
        }
    }

    // ---------------------------------------------------------------------
    // 1) Block-diagonal similarity (2 clusters and K clusters)
    // ---------------------------------------------------------------------

    /**
     * Two clusters: N = n1 + n2. Within-cluster similarity ≈ withinSim, across ≈ betweenSim.
     *
     * @param n1         size of cluster 0
     * @param n2         size of cluster 1
     * @param withinSim  e.g., 0.85
     * @param betweenSim e.g., 0.10
     * @param jitter     small noise added to off-diagonals (e.g., 0.02)
     * @param seed       RNG seed
     */
    public static SyntheticMatrix twoClustersSimilarity(int n1, int n2,
                                                        double withinSim,
                                                        double betweenSim,
                                                        double jitter,
                                                        long seed) {
        return kClustersSimilarity(new int[]{n1, n2}, withinSim, betweenSim, jitter, seed);
    }

    /**
     * K clusters with sizes[]; uses a block-diagonal similarity pattern.
     */
    public static SyntheticMatrix kClustersSimilarity(int[] sizes,
                                                      double withinSim,
                                                      double betweenSim,
                                                      double jitter,
                                                      long seed) {
        int n = 0;
        for (int s : sizes) n += s;
        double[][] S = new double[n][n];
        List<Integer> clusterIds = new ArrayList<>(n);
        List<String> labels = defaultLabels(n);

        // Assign cluster ids per index
        int idx = 0;
        for (int c = 0; c < sizes.length; c++) {
            for (int k = 0; k < sizes[c]; k++) {
                clusterIds.add(c);
                idx++;
            }
        }

        Random rng = new Random(seed);

        // Fill similarities
        int start = 0;
        for (int c = 0; c < sizes.length; c++) {
            int end = start + sizes[c] - 1;

            // within cluster block
            for (int i = start; i <= end; i++) {
                for (int j = start; j <= end; j++) {
                    if (i == j) S[i][j] = 1.0;
                    else S[i][j] = clamp01(withinSim + noise(rng, jitter));
                }
            }

            // across clusters
            int crossStart = end + 1;
            for (int i = start; i <= end; i++) {
                for (int j = crossStart; j < n; j++) {
                    double val = clamp01(betweenSim + noise(rng, jitter));
                    S[i][j] = val;
                    S[j][i] = val;
                }
            }

            start = end + 1;
        }

        symmetrize(S, true);       // force symmetry + diag=1
        normalize01(S, true);      // re-normalize to [0,1], keep diag=1
        String title = "Similarity: " + sizes.length + " clusters (K="
            + sizes.length + ", within≈" + withinSim + ", between≈" + betweenSim + ")";
        return new SyntheticMatrix(S, labels, clusterIds, MatrixToGraphAdapter.MatrixKind.SIMILARITY, title);
    }

    // ---------------------------------------------------------------------
    // 2) Ring similarity
    // ---------------------------------------------------------------------

    /**
     * A 1D ring (cycle) whose pairwise similarity decays with circular distance (Gaussian kernel).
     *
     * @param n      number of features on the ring
     * @param sigma  kernel width in ring steps (e.g., 3.0)
     * @param jitter noise on off-diagonal (e.g., 0.01)
     * @param seed   RNG seed
     */
    public static SyntheticMatrix ringSimilarity(int n, double sigma, double jitter, long seed) {
        double[][] S = new double[n][n];
        List<String> labels = defaultLabels(n);
        Random rng = new Random(seed);

        for (int i = 0; i < n; i++) {
            S[i][i] = 1.0;
            for (int j = i + 1; j < n; j++) {
                int d = ringDistance(i, j, n);
                double s = Math.exp(-(d * d) / (sigma * sigma));
                s = clamp01(s + noise(rng, jitter));
                S[i][j] = S[j][i] = s;
            }
        }
        symmetrize(S, true);
        normalize01(S, true);
        return new SyntheticMatrix(S, labels, null, MatrixToGraphAdapter.MatrixKind.SIMILARITY,
            "Similarity: Ring (n=" + n + ", σ=" + sigma + ")");
    }

    // ---------------------------------------------------------------------
    // 3) Three clusters divergence
    // ---------------------------------------------------------------------

    /**
     * Three groups with small within divergence and large across divergence.
     *
     * @param sizes    array of 3 sizes (e.g., {10,10,10})
     * @param withinD  e.g., 0.1
     * @param betweenD e.g., 0.9
     * @param jitter   noise added to off-diagonals (e.g., 0.02)
     * @param seed     RNG seed
     */
    public static SyntheticMatrix threeClustersDivergence(int[] sizes,
                                                          double withinD,
                                                          double betweenD,
                                                          double jitter,
                                                          long seed) {
        if (sizes == null || sizes.length != 3) {
            throw new IllegalArgumentException("threeClustersDivergence expects sizes length == 3");
        }
        int n = sizes[0] + sizes[1] + sizes[2];
        double[][] D = new double[n][n];
        List<Integer> clusterIds = new ArrayList<>(n);
        List<String> labels = defaultLabels(n);
        Random rng = new Random(seed);

        // assign ids
        int idx = 0;
        for (int c = 0; c < 3; c++) {
            for (int k = 0; k < sizes[c]; k++) {
                clusterIds.add(c);
                idx++;
            }
        }

        // Fill
        int[] starts = new int[]{0, sizes[0], sizes[0] + sizes[1]};
        int[] ends = new int[]{sizes[0] - 1, sizes[0] + sizes[1] - 1, n - 1};

        for (int c = 0; c < 3; c++) {
            for (int i = starts[c]; i <= ends[c]; i++) {
                D[i][i] = 0.0;
                for (int j = i + 1; j <= ends[c]; j++) {
                    double val = clamp01(withinD + noise(rng, jitter));
                    D[i][j] = D[j][i] = val;
                }
            }
        }
        // across clusters
        for (int c1 = 0; c1 < 3; c1++) {
            for (int c2 = c1 + 1; c2 < 3; c2++) {
                for (int i = starts[c1]; i <= ends[c1]; i++) {
                    for (int j = starts[c2]; j <= ends[c2]; j++) {
                        double val = clamp01(betweenD + noise(rng, jitter));
                        D[i][j] = D[j][i] = val;
                    }
                }
            }
        }

        symmetrize(D, false);      // diag=0
        normalize01(D, false);     // keep 0 on diag
        String title = "Divergence: 3 clusters (within≈" + withinD + ", between≈" + betweenD + ")";
        return new SyntheticMatrix(D, labels, clusterIds, MatrixToGraphAdapter.MatrixKind.DIVERGENCE, title);
    }

    // ---------------------------------------------------------------------
    // 4) Grid similarity (2D lattice + RBF kernel)
    // ---------------------------------------------------------------------

    /**
     * Features positioned on a rows×cols lattice; similarity uses an RBF kernel on the 2D grid distance.
     *
     * @param rows   grid rows (e.g., 6)
     * @param cols   grid cols (e.g., 6)
     * @param sigma  kernel width in grid steps (e.g., 1.5)
     * @param jitter off-diagonal noise
     * @param seed   RNG seed
     */
    public static SyntheticMatrix gridSimilarity(int rows, int cols, double sigma, double jitter, long seed) {
        int n = Math.max(1, rows) * Math.max(1, cols);
        double[][] S = new double[n][n];
        List<String> labels = defaultLabels(n);
        Random rng = new Random(seed);

        // map index -> (r,c)
        int[][] coords = new int[n][2];
        int t = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                coords[t][0] = r;
                coords[t][1] = c;
                t++;
            }
        }

        for (int i = 0; i < n; i++) {
            S[i][i] = 1.0;
            for (int j = i + 1; j < n; j++) {
                int dr = coords[i][0] - coords[j][0];
                int dc = coords[i][1] - coords[j][1];
                double dist2 = dr * dr + dc * dc;
                double s = Math.exp(-(dist2) / (sigma * sigma));
                s = clamp01(s + noise(rng, jitter));
                S[i][j] = S[j][i] = s;
            }
        }
        symmetrize(S, true);
        normalize01(S, true);
        return new SyntheticMatrix(S, labels, null, MatrixToGraphAdapter.MatrixKind.SIMILARITY,
            "Similarity: Grid " + rows + "×" + cols + " (σ=" + sigma + ")");
    }

    // ---------------------------------------------------------------------
    // 5) Cohorts for divergence workflows (Gaussian vs Uniform)
    // ---------------------------------------------------------------------

    /**
     * Build two cohorts of FeatureVectors:
     * - A: Gaussian(μ, σ) per component
     * - B: Uniform[min,max] per component
     * Both with N samples and D dimensions (matching shapes).
     */
    public static Cohorts makeCohorts_GaussianVsUniform(int N, int D,
                                                        double mu, double sigma,
                                                        double uniMin, double uniMax,
                                                        long seedA, long seedB) {
        Random rngA = new Random(seedA);
        Random rngB = new Random(seedB);

        List<FeatureVector> a = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            ArrayList<Double> row = new ArrayList<>(D);
            for (int j = 0; j < D; j++) {
                row.add(mu + rngA.nextGaussian() * sigma);
            }
            a.add(new FeatureVector(row));
        }

        double span = uniMax - uniMin;
        List<FeatureVector> b = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            ArrayList<Double> row = new ArrayList<>(D);
            for (int j = 0; j < D; j++) {
                row.add(uniMin + rngB.nextDouble() * span);
            }
            b.add(new FeatureVector(row));
        }
        return new Cohorts(a, b);
    }

    public static final class Cohorts {
        public final List<FeatureVector> cohortA;
        public final List<FeatureVector> cohortB;

        public Cohorts(List<FeatureVector> a, List<FeatureVector> b) {
            this.cohortA = a;
            this.cohortB = b;
        }
    }

    // ---------------------------------------------------------------------
    // Utilities
    // ---------------------------------------------------------------------

    private static List<String> defaultLabels(int n) {
        List<String> L = new ArrayList<>(n);
        for (int i = 0; i < n; i++) L.add("F" + i);
        return L;
    }

    private static int ringDistance(int i, int j, int n) {
        int d = Math.abs(i - j);
        return Math.min(d, n - d);
        // neighbors are ringDistance==1, next-neighbors==2, etc.
    }

    private static double noise(Random rng, double amp) {
        if (amp <= 0) return 0.0;
        // small zero-mean noise in [-amp, +amp]
        return (rng.nextDouble() * 2.0 - 1.0) * amp;
    }

    /**
     * Make symmetric, set diagonal to 1 (similarity) or 0 (divergence).
     */
    private static void symmetrize(double[][] M, boolean similarity) {
        int n = M.length;
        for (int i = 0; i < n; i++) {
            M[i][i] = similarity ? 1.0 : 0.0;
            for (int j = i + 1; j < n; j++) {
                double v = 0.5 * (safe(M[i][j]) + safe(M[j][i]));
                M[i][j] = M[j][i] = v;
            }
        }
    }

    /**
     * Normalize to [0,1] preserving diagonal convention (1 for similarity, 0 for divergence).
     */
    private static void normalize01(double[][] M, boolean similarity) {
        int n = M.length;
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) continue; // keep diag convention
                double v = safe(M[i][j]);
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }
        if (!(max > min)) return; // already flat / degenerate

        double span = max - min;
        for (int i = 0; i < n; i++) {
            M[i][i] = similarity ? 1.0 : 0.0;
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                double v = (M[i][j] - min) / span;
                M[i][j] = clamp01(v);
            }
        }
    }

    private static double safe(double v) {
        return (Double.isFinite(v) ? v : 0.0);
    }
}
