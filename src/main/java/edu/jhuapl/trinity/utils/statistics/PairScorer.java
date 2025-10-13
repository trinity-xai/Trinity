package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import edu.jhuapl.trinity.utils.metric.Metric;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * PairScorer
 * ----------
 * Pre-ranks 2D axis pairs for Joint PDF/CDF generation.
 * <p>
 * Primary path: COMPONENT_AT_DIMENSION pairs over an index range (fast, cached columns).
 * Also includes a convenience scorer for an arbitrary AxisParams pair (slower; extracts scalars).
 * <p>
 * Score metrics (normalized to ~[0,1] where possible):
 * - PEARSON       : |r| (abs Pearson correlation), robust to sign
 * - KENDALL       : |tau_b| (approx; down-samples if very large N)
 * - MI_LITE       : NMI = I(X;Y) / sqrt(H(X) * H(Y)) using fixed equal-width binning + add-one smoothing
 * - DIST_CORR     : distance correlation (biased estimator), ∈ [0,1]
 * <p>
 * Data sufficiency flag: sufficient iff N / (binsX*binsY) >= minAvgCountPerCell (when bins params provided).
 * <p>
 * Notes:
 * - This class does not mutate input data and is dependency-free.
 * - MI-lite assumes finite values; if inputs are near-constant, entropy guards avoid divide-by-zero.
 * - Kendall tau computation is O(N^2); for N>MAX_KENDALL_N we stride-sample to MAX_KENDALL_N.
 *
 * @author Sean Phillips
 */
public final class PairScorer {

    // ----------- Result type -----------

    public static final class PairScore implements Serializable, Comparable<PairScore> {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * For component-mode: i and j are component indices. For axis-mode: both are -1.
         */
        public final int i;
        public final int j;
        public final double score;           // normalized score used for ranking (>=0)
        public final long nSamples;
        public final boolean sufficient;
        public final String reason;          // optional note (e.g., "low variance", "insufficient N")

        public PairScore(int i, int j, double score, long nSamples, boolean sufficient, String reason) {
            this.i = i;
            this.j = j;
            this.score = score;
            this.nSamples = nSamples;
            this.sufficient = sufficient;
            this.reason = reason;
        }

        @Override
        public int compareTo(PairScore o) {
            return -Double.compare(this.score, o.score);
        } // desc

        @Override
        public String toString() {
            return "PairScore{" +
                "i=" + i + ", j=" + j +
                ", score=" + score +
                ", n=" + nSamples +
                ", sufficient=" + sufficient +
                (reason != null ? (", reason='" + reason + '\'') : "") +
                '}';
        }
    }

    // ----------- Config -----------

    public static final class Config {
        public final JpdfRecipe.ScoreMetric metric;
        public final int miBins;                 // for MI-lite (equal-width)
        public final int maxKendallN;            // downsample limit for Kendall
        public final Integer binsX;              // for sufficiency flag (nullable)
        public final Integer binsY;              // for sufficiency flag (nullable)
        public final double minAvgCountPerCell;  // sufficiency threshold

        public Config(JpdfRecipe.ScoreMetric metric,
                      int miBins,
                      int maxKendallN,
                      Integer binsX,
                      Integer binsY,
                      double minAvgCountPerCell) {
            this.metric = Objects.requireNonNull(metric);
            this.miBins = Math.max(4, miBins);
            this.maxKendallN = Math.max(50, maxKendallN);
            this.binsX = binsX;
            this.binsY = binsY;
            this.minAvgCountPerCell = Math.max(0.0, minAvgCountPerCell);
        }

        public static Config defaultFor(JpdfRecipe.ScoreMetric metric, Integer binsX, Integer binsY, double minAvgPerCell) {
            return new Config(metric, 16, 2000, binsX, binsY, minAvgPerCell);
        }
    }

    private PairScorer() {
    }

    // ====================================================================================
    // COMPONENT MODE: fast scoring over (start..end) component indices with caching
    // ====================================================================================

    /**
     * Score all valid component pairs in [start..end], respecting includeSelf/ordered flags.
     */
    public static List<PairScore> scoreComponentPairs(
        List<FeatureVector> vectors,
        int startInclusive,
        int endInclusive,
        boolean includeSelfPairs,
        boolean orderedPairs,
        Config cfg
    ) {
        Objects.requireNonNull(vectors, "vectors");
        if (vectors.isEmpty()) return Collections.emptyList();

        int dStart = Math.max(0, startInclusive);
        int dEnd = Math.max(dStart, endInclusive);
        int nDims = (vectors.get(0).getData() != null) ? vectors.get(0).getData().size() : 0;
        dEnd = Math.min(dEnd, Math.max(0, nDims - 1));

        // Extract columns (dimension -> array over samples)
        double[][] cols = extractComponentColumns(vectors, dStart, dEnd);
        int nSamples = cols.length == 0 ? 0 : cols[0].length;

        boolean suff = sufficiency(nSamples, cfg.binsX, cfg.binsY, cfg.minAvgCountPerCell);
        String insuffReason = suff ? null : "insufficient N per cell";

        List<PairScore> out = new ArrayList<>();
        for (int i = dStart; i <= dEnd; i++) {
            int ii = i - dStart;
            for (int j = dStart; j <= dEnd; j++) {
                if (!includeSelfPairs && i == j) continue;
                if (!orderedPairs && j <= i) continue;

                int jj = j - dStart;
                double[] x = cols[ii];
                double[] y = cols[jj];

                PairScore ps = new PairScore(i, j,
                    scorePair(x, y, cfg),
                    nSamples,
                    suff,
                    suff ? null : insuffReason);
                out.add(ps);
            }
        }
        Collections.sort(out);
        return out;
    }

    // ====================================================================================
    // AXIS MODE: convenience scoring for an arbitrary AxisParams pair (slower; on demand)
    // ====================================================================================

    public static PairScore scoreAxisPair(
        List<FeatureVector> vectors,
        AxisParams xAxis,
        AxisParams yAxis,
        Config cfg
    ) {
        Objects.requireNonNull(vectors, "vectors");
        if (vectors.isEmpty()) return new PairScore(-1, -1, 0.0, 0, false, "no data");

        double[] x = extractAxisScalars(vectors, xAxis);
        double[] y = extractAxisScalars(vectors, yAxis);

        boolean suff = sufficiency(x.length, cfg.binsX, cfg.binsY, cfg.minAvgCountPerCell);
        String reason = suff ? null : "insufficient N per cell";

        double score = scorePair(x, y, cfg);
        return new PairScore(-1, -1, score, x.length, suff, reason);
    }

    // ====================================================================================
    // Core scoring
    // ====================================================================================

    private static double scorePair(double[] x, double[] y, Config cfg) {
        if (x.length != y.length || x.length < 3) return 0.0;

        switch (cfg.metric) {
            case PEARSON:
                return Math.abs(pearson(x, y));
            case KENDALL:
                return Math.abs(kendallTauApprox(x, y, cfg.maxKendallN));
            case MI_LITE:
                return nmiLite(x, y, cfg.miBins);
            case DIST_CORR:
                return distCorr(x, y);
            default:
                return 0.0;
        }
    }

    // ----------- Pearson r -----------

    private static double pearson(double[] x, double[] y) {
        int n = x.length;
        double sx = 0, sy = 0, sxx = 0, syy = 0, sxy = 0;
        for (int i = 0; i < n; i++) {
            double xi = x[i], yi = y[i];
            sx += xi;
            sy += yi;
            sxx += xi * xi;
            syy += yi * yi;
            sxy += xi * yi;
        }
        double num = n * sxy - sx * sy;
        double den = Math.sqrt(Math.max(0, n * sxx - sx * sx)) * Math.sqrt(Math.max(0, n * syy - sy * sy));
        if (den == 0) return 0.0;
        double r = num / den;
        if (Double.isNaN(r) || Double.isInfinite(r)) return 0.0;
        return r;
    }

    // ----------- Kendall's tau-b (approx; stride sampling for large N) -----------

    private static double kendallTauApprox(double[] x, double[] y, int maxN) {
        int n = x.length;
        if (n <= 1) return 0.0;

        int[] idx = sampledIndices(n, maxN);
        int m = idx.length;

        long concordant = 0, discordant = 0;
        long tiesX = 0, tiesY = 0;

        for (int a = 0; a < m; a++) {
            int i = idx[a];
            for (int b = a + 1; b < m; b++) {
                int j = idx[b];
                double dx = x[i] - x[j];
                double dy = y[i] - y[j];

                if (dx == 0 && dy == 0) continue; // joint tie: ignore
                if (dx == 0) {
                    tiesX++;
                    continue;
                }
                if (dy == 0) {
                    tiesY++;
                    continue;
                }

                double prod = dx * dy;
                if (prod > 0) concordant++;
                else if (prod < 0) discordant++;
                // prod == 0 cases already handled above
            }
        }
        double denom = Math.sqrt((concordant + discordant + tiesX) * 1.0) *
            Math.sqrt((concordant + discordant + tiesY) * 1.0);
        if (denom == 0) return 0.0;
        return (concordant - discordant) / denom;
    }

    private static int[] sampledIndices(int n, int maxN) {
        if (n <= maxN) {
            int[] idx = new int[n];
            for (int i = 0; i < n; i++) idx[i] = i;
            return idx;
        }
        // stride sampling (deterministic, stable)
        int[] idx = new int[maxN];
        double step = (n - 1.0) / (maxN - 1.0);
        for (int k = 0; k < maxN; k++) idx[k] = (int) Math.round(k * step);
        return idx;
    }

    // ----------- MI-lite -> NMI in [0,1] -----------

    private static double nmiLite(double[] x, double[] y, int bins) {
        int n = x.length;
        if (n == 0) return 0.0;

        // Bounds: robust to constant signals
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double xi = x[i];
            double yi = y[i];
            if (xi < minX) minX = xi;
            if (xi > maxX) maxX = xi;
            if (yi < minY) minY = yi;
            if (yi > maxY) maxY = yi;
        }
        if (!(maxX > minX)) maxX = minX + 1e-8;
        if (!(maxY > minY)) maxY = minY + 1e-8;

        double dx = (maxX - minX) / bins;
        double dy = (maxY - minY) / bins;

        // add-one smoothing
        double[][] joint = new double[bins][bins];
        double[] px = new double[bins];
        double[] py = new double[bins];

        Arrays.fill(px, 1.0);
        Arrays.fill(py, 1.0);
        for (int by = 0; by < bins; by++) Arrays.fill(joint[by], 1.0);

        double nEff = n + bins + bins + bins * bins; // pseudo counts included

        for (int i = 0; i < n; i++) {
            int bx = (int) Math.floor((x[i] - minX) / dx);
            int by = (int) Math.floor((y[i] - minY) / dy);
            if (bx < 0) bx = 0;
            if (bx >= bins) bx = bins - 1;
            if (by < 0) by = 0;
            if (by >= bins) by = bins - 1;

            joint[by][bx] += 1.0;
            px[bx] += 1.0;
            py[by] += 1.0;
        }

        // Normalize to probabilities
        for (int bx = 0; bx < bins; bx++) px[bx] /= nEff;
        for (int by = 0; by < bins; by++) py[by] /= nEff;
        for (int by = 0; by < bins; by++)
            for (int bx = 0; bx < bins; bx++)
                joint[by][bx] /= nEff;

        // Entropies, MI
        double hx = entropy(px);
        double hy = entropy(py);
        double mi = 0.0;
        for (int by = 0; by < bins; by++) {
            for (int bx = 0; bx < bins; bx++) {
                double pxy = joint[by][bx];
                double denom = px[bx] * py[by];
                if (pxy > 0 && denom > 0) mi += pxy * Math.log(pxy / denom);
            }
        }

        // Normalized MI: divide by sqrt(Hx*Hy); guard if entropies near zero
        double denom = Math.sqrt(Math.max(hx, 1e-12) * Math.max(hy, 1e-12));
        double nmi = (denom > 0) ? (mi / denom) : 0.0;
        // Clamp to [0,1] range for numeric stability
        if (Double.isNaN(nmi) || Double.isInfinite(nmi)) nmi = 0.0;
        if (nmi < 0) nmi = 0;
        else if (nmi > 1) nmi = 1;
        return nmi;
    }

    private static double entropy(double[] p) {
        double h = 0.0;
        for (double v : p) if (v > 0) h -= v * Math.log(v);
        return h;
    }

    // ----------- Distance correlation (biased) -----------

    private static double distCorr(double[] x, double[] y) {
        int n = x.length;
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
        // Clamp
        if (dcor < 0) dcor = 0;
        else if (dcor > 1) dcor = 1;
        return dcor;
    }

    private static double[][] distanceMatrix(double[] v) {
        int n = v.length;
        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++) {
            d[i][i] = 0;
            for (int j = i + 1; j < n; j++) {
                double dij = Math.abs(v[i] - v[j]);
                d[i][j] = dij;
                d[j][i] = dij;
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

    // ----------- Sufficiency -----------

    private static boolean sufficiency(int n, Integer bx, Integer by, double minAvgPerCell) {
        if (bx == null || by == null) return true; // unknown bins → don't block
        if (bx <= 0 || by <= 0) return true;
        double avg = (bx * (double) by) == 0 ? 0 : (n / (bx * (double) by));
        return avg >= minAvgPerCell;
    }

    // ----------- Extraction helpers -----------

    private static double[][] extractComponentColumns(List<FeatureVector> vectors, int start, int end) {
        int nDims = end - start + 1;
        int n = vectors.size();
        double[][] cols = new double[nDims][n];
        for (int row = 0; row < n; row++) {
            List<Double> data = vectors.get(row).getData();
            for (int d = 0; d < nDims; d++) {
                int idx = start + d;
                double val = (idx >= 0 && idx < data.size()) ? data.get(idx) : 0.0;
                cols[d][row] = val;
            }
        }
        return cols;
    }

    private static double[] extractAxisScalars(List<FeatureVector> vectors, AxisParams axis) {
        // Precompute mean if needed
        List<Double> meanVector = null;
        Set<StatisticEngine.ScalarType> needMean = Set.of(
            StatisticEngine.ScalarType.DIST_TO_MEAN,
            StatisticEngine.ScalarType.COSINE_TO_MEAN
        );
        if (needMean.contains(axis.getType())) {
            meanVector = FeatureVector.getMeanVector(vectors);
        }

        Metric metric = null;
        if (axis.getType() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN
            && axis.getMetricName() != null
            && axis.getReferenceVec() != null) {
            metric = Metric.getMetric(axis.getMetricName());
        }

        double[] out = new double[vectors.size()];
        for (int i = 0; i < vectors.size(); i++) {
            FeatureVector fv = vectors.get(i);
            out[i] = scalarValue(fv, axis.getType(), meanVector, metric, axis.getReferenceVec(), axis.getComponentIndex());
        }
        return out;
    }

    private static double scalarValue(FeatureVector fv,
                                      StatisticEngine.ScalarType type,
                                      List<Double> meanVec,
                                      Metric metric,
                                      List<Double> refVec,
                                      Integer componentIndex) {
        switch (type) {
            case L1_NORM:
                return fv.getData().stream().mapToDouble(Math::abs).sum();
            case LINF_NORM:
                return fv.getData().stream().mapToDouble(Math::abs).max().orElse(0.0);
            case MEAN:
                return fv.getData().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            case MAX:
                return fv.getMax();
            case MIN:
                return fv.getMin();
            case DIST_TO_MEAN:
                if (meanVec == null) return 0.0;
                return AnalysisUtils.l2Norm(StatisticEngine.diffList(fv.getData(), meanVec));
            case COSINE_TO_MEAN:
                if (meanVec == null) return 0.0;
                return AnalysisUtils.cosineSimilarity(fv.getData(), meanVec);
            case METRIC_DISTANCE_TO_MEAN:
                if (metric == null || refVec == null) return 0.0;
                double[] a = fv.getData().stream().mapToDouble(Double::doubleValue).toArray();
                double[] b = refVec.stream().mapToDouble(Double::doubleValue).toArray();
                return metric.distance(a, b);
            case COMPONENT_AT_DIMENSION:
                if (componentIndex == null) return 0.0;
                List<Double> d = fv.getData();
                if (componentIndex >= 0 && componentIndex < d.size()) return d.get(componentIndex);
                return 0.0;
            case PC1_PROJECTION:
                return 0.0; // not computed here
            default:
                return 0.0;
        }
    }
}
