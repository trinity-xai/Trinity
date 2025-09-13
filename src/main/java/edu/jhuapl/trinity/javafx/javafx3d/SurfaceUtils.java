package edu.jhuapl.trinity.javafx.javafx3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility helpers for sampling, smoothing, and tone-mapping height grids
 * used by Hypersurface rendering.
 */
public final class SurfaceUtils {
    private SurfaceUtils() {}

    // ============================== Enums ===============================

    public enum Interpolation { NEAREST, BILINEAR, BICUBIC }

    public enum Smoothing { NONE, BOX, GAUSSIAN, MEDIAN }

    public enum ToneMap { NONE, CLAMP_01, NORMALIZE_01, LOG1P, GAMMA, ZSCORE }

    // ============================ Sampling ==============================

    public static double sample(List<List<Double>> grid, double x, double y, Interpolation mode) {
        if (isEmpty(grid)) return 0.0;
        final int h = height(grid), w = width(grid);
        x = clamp(x, 0.0, w - 1.0);
        y = clamp(y, 0.0, h - 1.0);
        return switch (mode) {
            case NEAREST  -> sampleNearest(grid, x, y);
            case BILINEAR -> sampleBilinear(grid, x, y);
            case BICUBIC  -> sampleBicubicSafe(grid, x, y); // edge-safe w/ fallback
        };
    }

    public static double sample(List<List<Double>> grid, double x, double y) {
        return sample(grid, x, y, Interpolation.BILINEAR);
    }

    /** Kept for compatibility with older call sites. */
    public static double sampleBicubic(List<List<Double>> grid, double x, double y) {
        if (isEmpty(grid)) return 0.0;
        final int h = height(grid), w = width(grid);
        x = clamp(x, 0.0, w - 1.0);
        y = clamp(y, 0.0, h - 1.0);
        return sampleBicubicSafe(grid, x, y);
    }

    private static double sampleNearest(List<List<Double>> g, double x, double y) {
        final int w = width(g), h = height(g);
        final int ix = (int) Math.round(clamp(x, 0, w - 1));
        final int iy = (int) Math.round(clamp(y, 0, h - 1));
        return g.get(iy).get(ix);
    }

public static double sampleBilinear(List<List<Double>> g, double x, double y) {
    final int w = width(g), h = height(g);

    // Compute base indices and fractions from the *original* x,y
    int x0 = (int) Math.floor(x);
    int y0 = (int) Math.floor(y);
    int x1 = x0 + 1;
    int y1 = y0 + 1;

    double tx = x - x0;
    double ty = y - y0;

    // Clamp indices to valid range; if we collapsed a side, zero the fraction
    x0 = clamp(x0, 0, w - 1);
    x1 = clamp(x1, 0, w - 1);
    y0 = clamp(y0, 0, h - 1);
    y1 = clamp(y1, 0, h - 1);
    if (x0 == x1) tx = 0.0;
    if (y0 == y1) ty = 0.0;

    double c00 = g.get(y0).get(x0);
    double c10 = g.get(y0).get(x1);
    double c01 = g.get(y1).get(x0);
    double c11 = g.get(y1).get(x1);

    double cx0 = lerp(c00, c10, tx);
    double cx1 = lerp(c01, c11, tx);
    return lerp(cx0, cx1, ty);
}

public static double sampleBicubicSafe(List<List<Double>> g, double x, double y) {
    final int w = width(g), h = height(g);
    final int ix = (int) Math.floor(x);
    final int iy = (int) Math.floor(y);
    // Need a 1-cell border; otherwise fall back to bilinear
    if (ix < 1 || ix > w - 3 || iy < 1 || iy > h - 3) {
        return sampleBilinear(g, x, y);
    }
    final double tx = x - ix, ty = y - iy;
    final double[][] p = new double[4][4];
    for (int m = -1; m <= 2; m++) {
        for (int n = -1; n <= 2; n++) {
            p[m + 1][n + 1] = g.get(iy + m).get(ix + n);
        }
    }
    final double[] col = new double[4];
    for (int row = 0; row < 4; row++) {
        col[row] = cubicCatmullRom(p[row][0], p[row][1], p[row][2], p[row][3], tx);
    }
    return cubicCatmullRom(col[0], col[1], col[2], col[3], ty);
}

    // =========================== Smoothing ==============================

    /**
     * ***New:*** Convenience wrapper to match your call site
     * {@code SurfaceUtils.smooth(g, sm, sigma, radius)}.
     * One iteration; GAUSSIAN uses {@code sigma}; other methods ignore it.
     */
    public static List<List<Double>> smooth(
            List<List<Double>> grid,
            Smoothing method,
            double sigma,
            int radius
    ) {
        return smoothCopy(grid, method, radius, 1, sigma);
    }

    /** Overload: BOX/MEDIAN/GAUSSIAN with default sigma heuristic (one iteration). */
    public static List<List<Double>> smooth(
            List<List<Double>> grid,
            Smoothing method,
            int radius
    ) {
        return smoothCopy(grid, method, radius, 1, -1.0);
    }

    /** Overload with explicit iterations (sigma used only for GAUSSIAN). */
    public static List<List<Double>> smooth(
            List<List<Double>> grid,
            Smoothing method,
            int radius,
            int iterations,
            double sigma
    ) {
        return smoothCopy(grid, method, radius, iterations, sigma);
    }

    /**
     * Create a smoothed copy of the grid.
     *
     * @param radius kernel radius (>=0), size = 2r+1
     * @param iterations number of passes (>=1)
     * @param sigma standard deviation for GAUSSIAN; if <=0 a heuristic is used
     */
    public static List<List<Double>> smoothCopy(
            List<List<Double>> grid,
            Smoothing method,
            int radius,
            int iterations,
            double sigma
    ) {
        if (isEmpty(grid) || method == Smoothing.NONE || radius <= 0 || iterations <= 0) {
            return deepCopy(grid);
        }
        List<List<Double>> src = deepCopy(grid);
        List<List<Double>> dst = makeZeroGrid(height(grid), width(grid));
        switch (method) {
            case BOX -> {
                for (int i = 0; i < iterations; i++) {
                    boxBlurSeparable(src, dst, radius);
                    List<List<Double>> tmp = src; src = dst; dst = tmp;
                }
            }
            case GAUSSIAN -> {
                double[] k = gaussianKernel(radius, sigma);
                for (int i = 0; i < iterations; i++) {
                    convolveSeparable(src, dst, k);
                    List<List<Double>> tmp = src; src = dst; dst = tmp;
                }
            }
            case MEDIAN -> {
                for (int i = 0; i < iterations; i++) {
                    medianFilter(src, dst, radius);
                    List<List<Double>> tmp = src; src = dst; dst = tmp;
                }
            }
            default -> { /* NONE already returned above */ }
        }
        return src;
    }

    /** In-place smoothing (replaces {@code grid}). */
    public static void smoothInPlace(
            List<List<Double>> grid,
            Smoothing method,
            int radius,
            int iterations,
            double sigma
    ) {
        List<List<Double>> out = smoothCopy(grid, method, radius, iterations, sigma);
        replaceContents(grid, out);
    }

    // =========================== Tone mapping ===========================

    /**
     * ***New:*** Alias to match your call site {@code toneMapGrid(g, tm, param)}.
     */
    public static List<List<Double>> toneMapGrid(
            List<List<Double>> grid,
            ToneMap mode,
            double param
    ) {
        return toneMapCopy(grid, mode, param);
    }

    /** Create a tone-mapped copy of the grid. */
    public static List<List<Double>> toneMapCopy(List<List<Double>> grid, ToneMap mode, double param) {
        if (isEmpty(grid) || mode == ToneMap.NONE) return deepCopy(grid);

        final int h = height(grid), w = width(grid);
        List<List<Double>> out = makeZeroGrid(h, w);

        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        double mean = 0.0, m2 = 0.0; int n = 0;

        if (mode == ToneMap.NORMALIZE_01 || mode == ToneMap.LOG1P || mode == ToneMap.GAMMA) {
            for (int y = 0; y < h; y++) {
                List<Double> row = grid.get(y);
                for (int x = 0; x < w; x++) {
                    double v = row.get(x);
                    if (v < min) min = v;
                    if (v > max) max = v;
                }
            }
        } else if (mode == ToneMap.ZSCORE) {
            for (int y = 0; y < h; y++) {
                List<Double> row = grid.get(y);
                for (int x = 0; x < w; x++) {
                    double v = row.get(x);
                    n++; double d = v - mean; mean += d / n; double d2 = v - mean; m2 += d * d2;
                }
            }
        }

        final double range = (max - min);
        final double gamma = (mode == ToneMap.GAMMA && param > 0) ? param : 2.2;

        for (int y = 0; y < h; y++) {
            List<Double> inRow = grid.get(y), outRow = out.get(y);
            for (int x = 0; x < w; x++) {
                double v = inRow.get(x), r;
                switch (mode) {
                    case CLAMP_01 -> r = clamp(v, 0.0, 1.0);
                    case NORMALIZE_01 -> {
                        r = (range <= 0) ? 0.0 : (v - min) / range;
                        r = clamp(r, 0.0, 1.0);
                    }
                    case LOG1P -> {
                        double shift = (min < 0) ? -min : 0.0;
                        double lv = Math.log1p(v + shift);
                        double lmax = Math.log1p(max + shift);
                        r = (lmax <= 0) ? 0.0 : (lv / lmax);
                        r = clamp(r, 0.0, 1.0);
                    }
                    case GAMMA -> {
                        double t = (range <= 0) ? 0.0 : (v - min) / range;
                        t = clamp(t, 0.0, 1.0);
                        r = Math.pow(t, 1.0 / gamma);
                    }
                    case ZSCORE -> {
                        double variance = (n > 1) ? (m2 / (n - 1)) : 0.0;
                        double std = (variance > 0) ? Math.sqrt(variance) : 1.0;
                        r = (v - mean) / std;
                    }
                    default -> r = v;
                }
                outRow.set(x, r);
            }
        }
        return out;
    }

    public static void toneMapInPlace(List<List<Double>> grid, ToneMap mode, double param) {
        replaceContents(grid, toneMapCopy(grid, mode, param));
    }

    // ====================== Internal: smoothing ops =====================

    private static void boxBlurSeparable(List<List<Double>> src, List<List<Double>> dst, int radius) {
        double[] k = boxKernel(radius);
        convolveSeparable(src, dst, k);
    }

    private static void convolveSeparable(List<List<Double>> src, List<List<Double>> dst, double[] kernel) {
        final int h = height(src), w = width(src), r = kernel.length / 2;
        List<List<Double>> tmp = makeZeroGrid(h, w);
        // horizontal
        for (int y = 0; y < h; y++) {
            List<Double> srow = src.get(y), trow = tmp.get(y);
            for (int x = 0; x < w; x++) {
                double acc = 0.0;
                for (int i = -r; i <= r; i++) acc += srow.get(clamp(x + i, 0, w - 1)) * kernel[i + r];
                trow.set(x, acc);
            }
        }
        // vertical
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double acc = 0.0;
                for (int i = -r; i <= r; i++) acc += tmp.get(clamp(y + i, 0, h - 1)).get(x) * kernel[i + r];
                dst.get(y).set(x, acc);
            }
        }
    }

    private static void medianFilter(List<List<Double>> src, List<List<Double>> dst, int radius) {
        final int h = height(src), w = width(src);
        final int size = (2 * radius + 1) * (2 * radius + 1);
        double[] win = new double[size];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    int yy = clamp(y + dy, 0, h - 1);
                    List<Double> row = src.get(yy);
                    for (int dx = -radius; dx <= radius; dx++) {
                        int xx = clamp(x + dx, 0, w - 1);
                        win[idx++] = row.get(xx);
                    }
                }
                java.util.Arrays.sort(win);
                double med = (size % 2 == 1) ? win[size / 2] : 0.5 * (win[size / 2 - 1] + win[size / 2]);
                dst.get(y).set(x, med);
            }
        }
    }

    private static double[] boxKernel(int radius) {
        int n = 2 * radius + 1;
        double[] k = new double[n];
        double v = 1.0 / n;
        for (int i = 0; i < n; i++) k[i] = v;
        return k;
    }

    private static double[] gaussianKernel(int radius, double sigma) {
        if (radius <= 0) return new double[]{1.0};
        if (sigma <= 0) sigma = Math.max(1e-6, (radius + 1) / 2.0); // heuristic
        int n = 2 * radius + 1;
        double[] k = new double[n];
        double sum = 0.0, twoSigma2 = 2.0 * sigma * sigma;
        for (int i = -radius; i <= radius; i++) {
            double val = Math.exp(-(i * i) / twoSigma2);
            k[i + radius] = val; sum += val;
        }
        for (int i = 0; i < n; i++) k[i] /= sum;
        return k;
    }

    // ============================ Utilities =============================

    private static double cubicCatmullRom(double p0, double p1, double p2, double p3, double t) {
        final double t2 = t * t, t3 = t2 * t;
        return 0.5 * (2.0 * p1
                + (-p0 + p2) * t
                + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2
                + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static int width(List<List<Double>> g) { return g.get(0).size(); }

    private static int height(List<List<Double>> g) { return g.size(); }

    private static boolean isEmpty(List<List<Double>> g) {
        return g == null || g.isEmpty() || g.get(0) == null || g.get(0).isEmpty();
    }

    private static int clamp(int v, int lo, int hi) { return (v < lo) ? lo : (v > hi) ? hi : v; }

    private static double clamp(double v, double lo, double hi) {
        return (v < lo) ? lo : (v > hi) ? hi : v;
    }

    private static List<List<Double>> deepCopy(List<List<Double>> g) {
        if (isEmpty(g)) return g;
        List<List<Double>> out = new ArrayList<>(g.size());
        for (List<Double> row : g) out.add(new ArrayList<>(row));
        return out;
    }

    private static List<List<Double>> makeZeroGrid(int h, int w) {
        List<List<Double>> out = new ArrayList<>(h);
        for (int y = 0; y < h; y++) out.add(new ArrayList<>(Collections.nCopies(w, 0.0)));
        return out;
    }

    private static void replaceContents(List<List<Double>> target, List<List<Double>> src) {
        target.clear();
        target.addAll(src);
    }
}
