package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * SimilarityComputer
 * ------------------
 * Builds a feature-by-feature similarity matrix for a single cohort (FeatureCollection A).
 *
 * Metrics supported:
 *  - NMI:      Normalized Mutual Information in [0,1] via equal-width binning + add-one smoothing
 *  - PEARSON:  |Pearson r| in [0,1]
 *  - DIST_CORR:Distance correlation (biased estimator) in [0,1]
 *
 * Inputs:
 *  - Feature vectors (List<FeatureVector>) assumed to share dimensionality.
 *  - A JpdfRecipe for binning/guards (minAvgCountPerCell) and to pull component index range.
 *    Only component-based features are supported here (COMPONENT_AT_DIMENSION).
 *
 * Outputs:
 *  - SimilarityResult: matrix (double[F][F]), labels, selected component indices, metric, and simple quality stats.
 *
 * Notes:
 *  - This class does NOT build Joint PDFs and does not require GridSpec; it works on raw columns.
 *  - Bounds for NMI are per-pair (min..max per each variable).
 *  - Computation is parallelized over the upper triangle.
 *
 * @author Sean Phillips 
 */
public final class SimilarityComputer {

    public enum Metric {
        /** Normalized mutual information in [0,1] (symmetric, robust to monotone transforms roughly). */
        NMI,
        /** Absolute Pearson correlation |r| in [0,1]. */
        PEARSON_ABS,
        /** Distance correlation (biased), in [0,1]. */
        DIST_CORR
    }

    /** Output bundle for a single similarity computation. */
    public static final class SimilarityResult implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        /** Symmetric matrix (F x F). Diagonal is 1.0 when defined. */
        public final double[][] matrix;

        /** Optional quality matrix (F x F), e.g., fraction of finite pairs used. In [0,1]. */
        public final double[][] quality;

        /** Chosen metric. */
        public final Metric metric;

        /** Selected component indices (length F). */
        public final int[] componentIndices;

        /** Human-readable labels per feature (length F), e.g., "Comp 0", "Comp 1". */
        public final List<String> labels;

        /** Number of sample rows used per pair (ideally equals N unless NaNs encountered). */
        public final int nSamples;

        /** Effective bin count used by NMI (null when metric != NMI). */
        public final Integer nmiBins;

        /** Guard threshold from recipe (for FYI / UI badges). */
        public final double minAvgCountPerCell;

        public SimilarityResult(double[][] matrix,
                                double[][] quality,
                                Metric metric,
                                int[] componentIndices,
                                List<String> labels,
                                int nSamples,
                                Integer nmiBins,
                                double minAvgCountPerCell) {
            this.matrix = matrix;
            this.quality = quality;
            this.metric = metric;
            this.componentIndices = componentIndices;
            this.labels = labels;
            this.nSamples = nSamples;
            this.nmiBins = nmiBins;
            this.minAvgCountPerCell = minAvgCountPerCell;
        }
    }

    private SimilarityComputer() {}

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * Compute a similarity matrix across a component index range defined by the recipe.
     * Uses all rows in {@code vectors}.
     *
     * @param vectors feature vectors (rows). Must be non-null; empty => empty result.
     * @param recipe  provides component index range and bins (for NMI) + guard knobs.
     * @param metric  similarity metric to compute.
     * @return SimilarityResult containing the matrix and metadata.
     */
    public static SimilarityResult computeForComponentRange(
            List<FeatureVector> vectors,
            JpdfRecipe recipe,
            Metric metric
    ) {
        Objects.requireNonNull(vectors, "vectors");
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(metric, "metric");

        // Resolve component index range from recipe
        int start = Math.max(0, recipe.getComponentIndexStart());
        int end = Math.max(start, recipe.getComponentIndexEnd());
        // Clamp by dimensionality if possible
        int dim = (vectors.isEmpty() || vectors.get(0).getData() == null) ? -1 : vectors.get(0).getData().size();
        if (dim >= 0) end = Math.min(end, Math.max(0, dim - 1));

        List<Integer> indices = new ArrayList<>();
        for (int i = start; i <= end; i++) indices.add(i);

        return computeForComponents(vectors, indices, recipe, metric);
    }

    /**
     * Compute a similarity matrix for an explicit list of component indices.
     *
     * @param vectors feature vectors (rows)
     * @param componentIndices list of dimensions to include (0-based)
     * @param recipe recipe for bins/guards (NMI), ignored bounds policy here
     * @param metric metric to compute
     * @return SimilarityResult
     */
    public static SimilarityResult computeForComponents(
            List<FeatureVector> vectors,
            List<Integer> componentIndices,
            JpdfRecipe recipe,
            Metric metric
    ) {
        Objects.requireNonNull(vectors, "vectors");
        Objects.requireNonNull(componentIndices, "componentIndices");
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(metric, "metric");

        // Early outs
        if (vectors.isEmpty() || componentIndices.isEmpty()) {
            return new SimilarityResult(new double[0][0], new double[0][0], metric,
                    new int[0], List.of(), 0,
                    metric == Metric.NMI ? Math.min(recipe.getBinsX(), recipe.getBinsY()) : null,
                    recipe.getMinAvgCountPerCell());
        }

        // Extract selected columns into dense arrays: cols[k][row]
        int[] comps = componentIndices.stream().mapToInt(Integer::intValue).toArray();
        double[][] cols = extractColumns(vectors, comps);
        int F = cols.length;
        int N = (F == 0) ? 0 : cols[0].length;

        // Prepare outputs
        double[][] M = new double[F][F];
        double[][] Q = new double[F][F]; // quality (fraction finite pairs used)

        // Diagonal
        for (int i = 0; i < F; i++) {
            M[i][i] = 1.0;
            Q[i][i] = 1.0;
        }

        // NMI bins: choose something sane. Use recipe bins, but not too huge for 1D discretization.
        final int nmiBins = (metric == Metric.NMI)
                ? Math.max(4, Math.min(128, Math.min(recipe.getBinsX(), recipe.getBinsY())))
                : -1;

        // Parallel upper-triangle computation
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < F; i++) {
            final int ii = i;
            futures.add(pool.submit(() -> {
                double[] x = cols[ii];
                for (int j = ii + 1; j < F; j++) {
                    double[] y = cols[j];

                    // Filter finite pairs once per edge
                    PairXY xy = finitePairs(x, y);
                    double s;
                    switch (metric) {
                        case NMI -> s = nmiLite(xy.x, xy.y, nmiBins);
                        case PEARSON_ABS -> s = Math.abs(pearson(xy.x, xy.y));
                        case DIST_CORR -> s = distCorr(xy.x, xy.y);
                        default -> s = 0.0;
                    }
                    double q = (xy.n == 0) ? 0.0 : (xy.n / (double) N); // fraction of usable rows

                    M[ii][j] = s;
                    M[j][ii] = s;
                    Q[ii][j] = q;
                    Q[j][ii] = q;
                }
            }));
        }

        // Wait
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception e) {
                pool.shutdownNow();
                throw new RuntimeException("SimilarityComputer: task failed", e);
            }
        }
        pool.shutdown();

        // Labels
        List<String> labels = new ArrayList<>(F);
        for (int c : comps) labels.add("Comp " + c);

        // Return
        return new SimilarityResult(
                M,
                Q,
                metric,
                comps,
                labels,
                N,
                (metric == Metric.NMI ? nmiBins : null),
                recipe.getMinAvgCountPerCell()
        );
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    /** Extracts an array per selected component index: cols[k][row]. */
    private static double[][] extractColumns(List<FeatureVector> vectors, int[] comps) {
        int F = comps.length;
        int N = vectors.size();
        double[][] cols = new double[F][N];
        for (int r = 0; r < N; r++) {
            List<Double> row = vectors.get(r).getData();
            for (int k = 0; k < F; k++) {
                int idx = comps[k];
                double v = (row != null && idx >= 0 && idx < row.size() && row.get(idx) != null)
                        ? row.get(idx)
                        : Double.NaN;
                cols[k][r] = v;
            }
        }
        return cols;
    }

    /** Holder for filtered finite pairs. */
    private static final class PairXY {
        final double[] x, y;
        final int n;
        PairXY(double[] x, double[] y) { this.x = x; this.y = y; this.n = x.length; }
    }

    /** Remove any rows with non-finite x or y. */
    private static PairXY finitePairs(double[] x, double[] y) {
        int n = Math.min(x.length, y.length);
        double[] xx = new double[n];
        double[] yy = new double[n];
        int m = 0;
        for (int i = 0; i < n; i++) {
            double a = x[i], b = y[i];
            if (Double.isFinite(a) && Double.isFinite(b)) {
                xx[m] = a; yy[m] = b; m++;
            }
        }
        if (m == n) return new PairXY(xx, yy);
        double[] x2 = new double[m], y2 = new double[m];
        System.arraycopy(xx, 0, x2, 0, m);
        System.arraycopy(yy, 0, y2, 0, m);
        return new PairXY(x2, y2);
    }

    // ---------------- Pearson r ----------------
    private static double pearson(double[] x, double[] y) {
        int n = Math.min(x.length, y.length);
        if (n < 3) return 0.0;
        double sx = 0, sy = 0, sxx = 0, syy = 0, sxy = 0;
        for (int i = 0; i < n; i++) {
            double xi = x[i], yi = y[i];
            sx += xi; sy += yi;
            sxx += xi * xi; syy += yi * yi;
            sxy += xi * yi;
        }
        double num = n * sxy - sx * sy;
        double den = Math.sqrt(Math.max(0, n * sxx - sx * sx)) * Math.sqrt(Math.max(0, n * syy - sy * sy));
        if (den == 0) return 0.0;
        double r = num / den;
        if (Double.isNaN(r) || Double.isInfinite(r)) return 0.0;
        return r;
    }

    // ---------------- Distance correlation (biased) ----------------
    private static double distCorr(double[] x, double[] y) {
        int n = Math.min(x.length, y.length);
        if (n < 3) return 0.0;

        double[][] ax = distanceMatrix(x);
        double[][] ay = distanceMatrix(y);

        double[][] axc = doubleCenter(ax);
        double[][] ayc = doubleCenter(ay);

        double dcov2 = meanElementwiseProduct(axc, ayc);
        double dvarx = meanElementwiseProduct(axc, axc);
        double dvary = meanElementwiseProduct(ayc, ayc);

        if (dvarx <= 0 || dvary <= 0) return 0.0;
        double dcor = Math.sqrt(Math.max(0, dcov2)) / Math.sqrt(dvarx * dvary);
        if (Double.isNaN(dcor) || Double.isInfinite(dcor)) return 0.0;
        if (dcor < 0) dcor = 0; else if (dcor > 1) dcor = 1;
        return dcor;
    }

    private static double[][] distanceMatrix(double[] v) {
        int n = v.length;
        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++) {
            d[i][i] = 0;
            for (int j = i + 1; j < n; j++) {
                double dij = Math.abs(v[i] - v[j]);
                d[i][j] = dij; d[j][i] = dij;
            }
        }
        return d;
    }

    private static double[][] doubleCenter(double[][] a) {
        int n = a.length;
        double[] rowMean = new double[n];
        double[] colMean = new double[n];
        double grand = 0.0;

        for (int i = 0; i < n; i++) {
            double rs = 0;
            for (int j = 0; j < n; j++) rs += a[i][j];
            rowMean[i] = rs / n;
            grand += rs;
        }
        grand /= (n * n);

        for (int j = 0; j < n; j++) {
            double cs = 0;
            for (int i = 0; i < n; i++) cs += a[i][j];
            colMean[j] = cs / n;
        }

        double[][] out = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                out[i][j] = a[i][j] - rowMean[i] - colMean[j] + grand;
            }
        }
        return out;
    }

    private static double meanElementwiseProduct(double[][] a, double[][] b) {
        int n = a.length;
        double s = 0.0;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                s += a[i][j] * b[i][j];
        return s / (n * n);
    }

    // ---------------- NMI (equal-width + add-one smoothing) ----------------
    private static double nmiLite(double[] x, double[] y, int bins) {
        int n = Math.min(x.length, y.length);
        if (n == 0) return 0.0;

        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double xi = x[i], yi = y[i];
            if (xi < minX) minX = xi; if (xi > maxX) maxX = xi;
            if (yi < minY) minY = yi; if (yi > maxY) maxY = yi;
        }
        if (!(maxX > minX)) maxX = minX + 1e-8;
        if (!(maxY > minY)) maxY = minY + 1e-8;

        double dx = (maxX - minX) / bins;
        double dy = (maxY - minY) / bins;

        // add-one smoothing
        double[][] joint = new double[bins][bins];
        double[] px = new double[bins];
        double[] py = new double[bins];
        for (int bx = 0; bx < bins; bx++) { px[bx] = 1.0; }
        for (int by = 0; by < bins; by++) { py[by] = 1.0; for (int bx = 0; bx < bins; bx++) joint[by][bx] = 1.0; }

        double nEff = n + bins + bins + bins * bins;

        for (int i = 0; i < n; i++) {
            int bx = (int) Math.floor((x[i] - minX) / dx);
            int by = (int) Math.floor((y[i] - minY) / dy);
            if (bx < 0) bx = 0; if (bx >= bins) bx = bins - 1;
            if (by < 0) by = 0; if (by >= bins) by = bins - 1;
            joint[by][bx] += 1.0;
            px[bx] += 1.0;
            py[by] += 1.0;
        }

        for (int bx = 0; bx < bins; bx++) px[bx] /= nEff;
        for (int by = 0; by < bins; by++) py[by] /= nEff;
        for (int by = 0; by < bins; by++)
            for (int bx = 0; bx < bins; bx++)
                joint[by][bx] /= nEff;

        double hx = entropy(px);
        double hy = entropy(py);
        double mi = 0.0;
        for (int by = 0; by < bins; by++) {
            for (int bx = 0; bx < bins; bx++) {
                double pxy = joint[by][bx];
                double den = px[bx] * py[by];
                if (pxy > 0 && den > 0) mi += pxy * Math.log(pxy / den);
            }
        }
        double denom = Math.sqrt(Math.max(hx, 1e-12) * Math.max(hy, 1e-12));
        double nmi = (denom > 0) ? (mi / denom) : 0.0;
        if (Double.isNaN(nmi) || Double.isInfinite(nmi)) nmi = 0.0;
        if (nmi < 0) nmi = 0; else if (nmi > 1) nmi = 1;
        return nmi;
    }

    private static double entropy(double[] p) {
        double h = 0.0;
        for (double v : p) if (v > 0) h -= v * Math.log(v);
        return h;
    }
}
