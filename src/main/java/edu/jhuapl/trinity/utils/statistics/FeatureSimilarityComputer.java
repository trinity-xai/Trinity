package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * FeatureSimilarityComputer
 * -------------------------
 * Computes an N x N feature similarity matrix for a single cohort (one FeatureCollection),
 * where N = (endIndex - startIndex + 1). Each entry S[i,j] encodes dependence between
 * component @i and component @j using one of:
 * <p>
 * - PEARSON   : |r|
 * - KENDALL   : |tau_b| (approx; stride-sampled for large N)
 * - MI_LITE   : Normalized Mutual Information in [0,1] using fixed equal-width binning
 * - DIST_CORR : Distance correlation in [0,1] (biased estimator)
 * <p>
 * Data sufficiency guard:
 * sufficient <=> (#samples) / (binsX * binsY) >= minAvgCountPerCell
 * If insufficient, S[i,j] is set to Double.NaN and mask[i][j] = false.
 * <p>
 * Diagonal:
 * S[i,i] = 1.0 when sufficient; else NaN.
 * <p>
 * Output includes labels ("Comp <k>") for convenience and a metadata block.
 *
 * @author Sean Phillips
 */
public final class FeatureSimilarityComputer {

    /**
     * Similarity metric options (aligned with JpdfRecipe.ScoreMetric names).
     */
    public enum SimilarityMetric {
        PEARSON,
        KENDALL,
        MI_LITE,
        DIST_CORR
    }

    /**
     * Result bundle for a single cohort.
     *
     * @param sim        Similarity matrix, size N x N. May contain NaN where insufficient.
     * @param sufficient True where the corresponding sim[i][j] passed the sufficiency guard.
     * @param labels     Display labels for each component (length N), e.g., "Comp 7".
     * @param meta       Metadata for auditing and UI badges.
     */
    public record Result(double[][] sim, boolean[][] sufficient, List<String> labels, Meta meta) implements Serializable {


    }

    /**
     * Metadata captured for the matrix computation.
     *
     * @param miBins      for MI_LITE
     * @param kendallMaxN for KENDALL sampling
     * @param binsX       for sufficiency guard
     * @param binsY       for sufficiency guard
     */
    public record Meta(int startIndex, int endIndex, long nSamples, SimilarityMetric metric, int miBins, int kendallMaxN, int binsX, int binsY,
                       double minAvgCountPerCell) implements Serializable {

    }

    private FeatureSimilarityComputer() {
        // utility class
    }

    /**
     * Compute an N x N similarity matrix across components [startIndex..endIndex] for one cohort.
     *
     * @param vectors            cohort feature vectors (rows); required
     * @param startIndex         inclusive component start (clamped to [0, D-1])
     * @param endIndex           inclusive component end (clamped to [start, D-1])
     * @param metric             similarity metric to use
     * @param miBins             MI-lite bin count (>=4) used only when metric == MI_LITE
     * @param kendallMaxN        stride-sampling cap for Kendall tau (>=50)
     * @param binsX              guard param for avg count per cell (>=2)
     * @param binsY              guard param for avg count per cell (>=2)
     * @param minAvgCountPerCell sufficiency threshold; if <=0 no pairs are blocked
     */
    public static Result compute(List<FeatureVector> vectors,
                                 int startIndex,
                                 int endIndex,
                                 SimilarityMetric metric,
                                 int miBins,
                                 int kendallMaxN,
                                 int binsX,
                                 int binsY,
                                 double minAvgCountPerCell) {
        Objects.requireNonNull(vectors, "vectors");
        if (vectors.isEmpty()) {
            return emptyResult(0, 0, metric, miBins, kendallMaxN, binsX, binsY, minAvgCountPerCell);
        }

        // Clamp index range against dimensionality
        int dims = (vectors.get(0).getData() == null) ? 0 : vectors.get(0).getData().size();
        int s = Math.max(0, startIndex);
        int e = Math.max(s, Math.min(endIndex, Math.max(0, dims - 1)));
        int N = (e >= s) ? (e - s + 1) : 0;
        if (N == 0) {
            return emptyResult(s, e, metric, miBins, kendallMaxN, binsX, binsY, minAvgCountPerCell);
        }

        // Extract columns as double[N][nSamples]
        double[][] cols = extractComponentColumns(vectors, s, e);
        int nSamples = (cols.length == 0) ? 0 : cols[0].length;

        // Prepare output
        double[][] S = new double[N][N];
        boolean[][] ok = new boolean[N][N];

        // Sufficiency (same n for all pairs since same cohort)
        boolean sufficient = sufficiency(nSamples, binsX, binsY, minAvgCountPerCell);

        // Fill matrix (symmetric; compute upper triangle and mirror)
        for (int i = 0; i < N; i++) {
            S[i][i] = sufficient ? 1.0 : Double.NaN;
            ok[i][i] = sufficient;
        }

        for (int i = 0; i < N; i++) {
            double[] xi = cols[i];
            for (int j = i + 1; j < N; j++) {
                double[] xj = cols[j];
                double val;
                if (!sufficient) {
                    val = Double.NaN;
                } else {
                    switch (metric) {
                        case PEARSON -> val = Math.abs(pearson(xi, xj));
                        case KENDALL -> val = Math.abs(kendallTauApprox(xi, xj, Math.max(50, kendallMaxN)));
                        case MI_LITE -> val = nmiLite(xi, xj, Math.max(4, miBins));
                        case DIST_CORR -> val = distCorr(xi, xj);
                        default -> val = Double.NaN;
                    }
                }
                S[i][j] = val;
                S[j][i] = val;
                ok[i][j] = sufficient && !Double.isNaN(val);
                ok[j][i] = ok[i][j];
            }
        }

        // Labels "Comp <index>"
        List<String> labels = new ArrayList<>(N);
        for (int k = 0; k < N; k++) labels.add("Comp " + (s + k));

        Meta meta = new Meta(s, e, nSamples, metric,
            Math.max(4, miBins), Math.max(50, kendallMaxN),
            Math.max(2, binsX), Math.max(2, binsY),
            Math.max(0.0, minAvgCountPerCell));

        return new Result(S, ok, labels, meta);
    }

    // ------------------------------------------------------------------------------------
    // Internals: column extraction, sufficiency, metrics
    // ------------------------------------------------------------------------------------

    private static Result emptyResult(int s, int e,
                                      SimilarityMetric metric,
                                      int miBins,
                                      int kendallMaxN,
                                      int binsX,
                                      int binsY,
                                      double minAvgCountPerCell) {
        double[][] S = new double[0][0];
        boolean[][] ok = new boolean[0][0];
        List<String> labels = new ArrayList<>(0);
        Meta meta = new Meta(s, e, 0L, metric,
            Math.max(4, miBins), Math.max(50, kendallMaxN),
            Math.max(2, binsX), Math.max(2, binsY),
            Math.max(0.0, minAvgCountPerCell));
        return new Result(S, ok, labels, meta);
    }

    private static double[][] extractComponentColumns(List<FeatureVector> vectors, int start, int end) {
        int N = end - start + 1;
        int n = vectors.size();
        double[][] cols = new double[N][n];
        for (int row = 0; row < n; row++) {
            List<Double> data = vectors.get(row).getData();
            for (int d = 0; d < N; d++) {
                int idx = start + d;
                double val = (data != null && idx >= 0 && idx < data.size()) ? data.get(idx) : 0.0;
                cols[d][row] = val;
            }
        }
        return cols;
    }

    private static boolean sufficiency(long n, int bx, int by, double minAvgPerCell) {
        if (bx <= 0 || by <= 0) return true;
        if (minAvgPerCell <= 0.0) return true;
        double avg = (bx * (double) by) == 0 ? 0.0 : (n / (bx * (double) by));
        return avg >= minAvgPerCell;
    }

    // ----- Metrics (duplicated here to keep this class self-contained) -----

    private static double pearson(double[] x, double[] y) {
        int n = Math.min(x.length, y.length);
        if (n < 3) return 0.0;
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
        double den = Math.sqrt(Math.max(0.0, n * sxx - sx * sx)) *
            Math.sqrt(Math.max(0.0, n * syy - sy * sy));
        if (den == 0.0) return 0.0;
        double r = num / den;
        if (Double.isNaN(r) || Double.isInfinite(r)) return 0.0;
        return r;
    }

    /**
     * Approximate Kendall's tau_b using stride-sampling for large n.
     */
    private static double kendallTauApprox(double[] x, double[] y, int maxN) {
        int n = Math.min(x.length, y.length);
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

                if (dx == 0.0 && dy == 0.0) continue; // joint tie
                if (dx == 0.0) {
                    tiesX++;
                    continue;
                }
                if (dy == 0.0) {
                    tiesY++;
                    continue;
                }

                double prod = dx * dy;
                if (prod > 0) concordant++;
                else if (prod < 0) discordant++;
            }
        }
        double denom = Math.sqrt((concordant + discordant + tiesX) * 1.0) *
            Math.sqrt((concordant + discordant + tiesY) * 1.0);
        if (denom == 0.0) return 0.0;
        return (concordant - discordant) / denom;
    }

    private static int[] sampledIndices(int n, int maxN) {
        if (n <= maxN) {
            int[] idx = new int[n];
            for (int i = 0; i < n; i++) idx[i] = i;
            return idx;
        }
        int[] idx = new int[maxN];
        double step = (n - 1.0) / (maxN - 1.0);
        for (int k = 0; k < maxN; k++) idx[k] = (int) Math.round(k * step);
        return idx;
    }

    /**
     * NMI in [0,1] using equal-width binning with add-one smoothing.
     */
    private static double nmiLite(double[] x, double[] y, int bins) {
        int n = Math.min(x.length, y.length);
        if (n == 0) return 0.0;

        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            double xi = x[i], yi = y[i];
            if (xi < minX) minX = xi;
            if (xi > maxX) maxX = xi;
            if (yi < minY) minY = yi;
            if (yi > maxY) maxY = yi;
        }
        if (!(maxX > minX)) maxX = minX + 1e-8;
        if (!(maxY > minY)) maxY = minY + 1e-8;

        double dx = (maxX - minX) / bins;
        double dy = (maxY - minY) / bins;

        double[][] joint = new double[bins][bins];
        double[] px = new double[bins];
        double[] py = new double[bins];

        Arrays.fill(px, 1.0);
        Arrays.fill(py, 1.0);
        for (int by = 0; by < bins; by++) Arrays.fill(joint[by], 1.0);

        double nEff = n + bins + bins + bins * bins;

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

        for (int b = 0; b < bins; b++) {
            px[b] /= nEff;
            py[b] /= nEff;
        }
        for (int by = 0; by < bins; by++) {
            for (int bx = 0; bx < bins; bx++) {
                joint[by][bx] /= nEff;
            }
        }

        double hx = entropy(px);
        double hy = entropy(py);
        double mi = 0.0;
        for (int by = 0; by < bins; by++) {
            for (int bx = 0; bx < bins; bx++) {
                double pxy = joint[by][bx];
                double denom = px[bx] * py[by];
                if (pxy > 0.0 && denom > 0.0) {
                    mi += pxy * Math.log(pxy / denom);
                }
            }
        }
        double denom = Math.sqrt(Math.max(hx, 1e-12) * Math.max(hy, 1e-12));
        double nmi = (denom > 0.0) ? (mi / denom) : 0.0;
        if (Double.isNaN(nmi) || Double.isInfinite(nmi)) nmi = 0.0;
        if (nmi < 0.0) nmi = 0.0;
        else if (nmi > 1.0) nmi = 1.0;
        return nmi;
    }

    private static double entropy(double[] p) {
        double h = 0.0;
        for (double v : p) if (v > 0.0) h -= v * Math.log(v);
        return h;
    }

    /**
     * Distance correlation (biased), returned in [0,1].
     */
    private static double distCorr(double[] x, double[] y) {
        int n = Math.min(x.length, y.length);
        if (n < 3) return 0.0;

        double[][] ax = distanceMatrix(x, n);
        double[][] ay = distanceMatrix(y, n);

        double[][] axc = doubleCenter(ax);
        double[][] ayc = doubleCenter(ay);

        double dcov2 = meanElementwiseProduct(axc, ayc);
        double dvarx = meanElementwiseProduct(axc, axc);
        double dvary = meanElementwiseProduct(ayc, ayc);

        if (dvarx <= 0.0 || dvary <= 0.0) return 0.0;
        double dcor = Math.sqrt(Math.max(0.0, dcov2)) / Math.sqrt(dvarx * dvary);
        if (Double.isNaN(dcor) || Double.isInfinite(dcor)) return 0.0;
        if (dcor < 0.0) dcor = 0.0;
        else if (dcor > 1.0) dcor = 1.0;
        return dcor;
    }

    private static double[][] distanceMatrix(double[] v, int n) {
        double[][] d = new double[n][n];
        for (int i = 0; i < n; i++) {
            d[i][i] = 0.0;
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
            double rs = 0.0;
            for (int j = 0; j < n; j++) rs += a[i][j];
            rowMean[i] = rs / n;
            grand += rs;
        }
        grand /= (n * n);

        for (int j = 0; j < n; j++) {
            double cs = 0.0;
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
}
