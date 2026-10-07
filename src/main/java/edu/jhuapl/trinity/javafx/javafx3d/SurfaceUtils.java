package edu.jhuapl.trinity.javafx.javafx3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Utility helpers for sampling, smoothing, and tone-mapping height grids
 * used by Hypersurface rendering.
 *
 * <p>This class preserves the existing List<List<Double>> API for compatibility,
 * and adds a parallel primitive API using {@link HeightField} to eliminate boxing,
 * deep-copy overhead, and excessive GC for large grids.</p>
 */
public final class SurfaceUtils {
    private SurfaceUtils() {
    }

    // ============================== Enums ===============================

    public enum Interpolation {NEAREST, BILINEAR, BICUBIC}

    public enum Smoothing {NONE, BOX, GAUSSIAN, MEDIAN}

    public enum ToneMap {NONE, CLAMP_01, NORMALIZE_01, LOG1P, GAMMA, ZSCORE}

    // ===================================================================
    //  List<List<Double>> API (existing behavior preserved)
    // ===================================================================

    // ============================ Sampling ==============================

    public static double sample(List<List<Double>> grid, double x, double y, Interpolation mode) {
        if (isEmpty(grid)) return 0.0;
        final int h = height(grid), w = width(grid);
        x = clamp(x, 0.0, w - 1.0);
        y = clamp(y, 0.0, h - 1.0);
        return switch (mode) {
            case NEAREST -> sampleNearest(grid, x, y);
            case BILINEAR -> sampleBilinear(grid, x, y);
            case BICUBIC -> sampleBicubicSafe(grid, x, y); // edge-safe w/ fallback
        };
    }

    public static double sample(List<List<Double>> grid, double x, double y) {
        return sample(grid, x, y, Interpolation.BILINEAR);
    }

    /**
     * Kept for compatibility with older call sites.
     */
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
     * Convenience wrapper to match call site {@code smooth(g, sm, sigma, radius)}.
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

    /**
     * Overload: BOX/MEDIAN/GAUSSIAN with default sigma heuristic (one iteration).
     */
    public static List<List<Double>> smooth(
        List<List<Double>> grid,
        Smoothing method,
        int radius
    ) {
        return smoothCopy(grid, method, radius, 1, -1.0);
    }

    /**
     * Overload with explicit iterations (sigma used only for GAUSSIAN).
     */
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
     * @param radius     kernel radius (>=0), size = 2r+1
     * @param iterations number of passes (>=1)
     * @param sigma      standard deviation for GAUSSIAN; if <=0 a heuristic is used
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
                    List<List<Double>> tmp = src;
                    src = dst;
                    dst = tmp;
                }
            }
            case GAUSSIAN -> {
                double[] k = gaussianKernel(radius, sigma);
                for (int i = 0; i < iterations; i++) {
                    convolveSeparable(src, dst, k);
                    List<List<Double>> tmp = src;
                    src = dst;
                    dst = tmp;
                }
            }
            case MEDIAN -> {
                for (int i = 0; i < iterations; i++) {
                    medianFilter(src, dst, radius);
                    List<List<Double>> tmp = src;
                    src = dst;
                    dst = tmp;
                }
            }
            default -> { /* NONE already returned above */ }
        }
        return src;
    }

    /**
     * In-place smoothing (replaces {@code grid}).
     */
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
     * Alias to match call site {@code toneMapGrid(g, tm, param)}.
     */
    public static List<List<Double>> toneMapGrid(
        List<List<Double>> grid,
        ToneMap mode,
        double param
    ) {
        return toneMapCopy(grid, mode, param);
    }

    /**
     * Create a tone-mapped copy of the grid.
     */
    public static List<List<Double>> toneMapCopy(List<List<Double>> grid, ToneMap mode, double param) {
        if (isEmpty(grid) || mode == ToneMap.NONE) return deepCopy(grid);

        final int h = height(grid), w = width(grid);
        List<List<Double>> out = makeZeroGrid(h, w);

        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        double mean = 0.0, m2 = 0.0;
        int n = 0;

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
                    n++;
                    double d = v - mean;
                    mean += d / n;
                    double d2 = v - mean;
                    m2 += d * d2;
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
            k[i + radius] = val;
            sum += val;
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

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int width(List<List<Double>> g) {
        return g.get(0).size();
    }

    private static int height(List<List<Double>> g) {
        return g.size();
    }

    private static boolean isEmpty(List<List<Double>> g) {
        return g == null || g.isEmpty() || g.get(0) == null || g.get(0).isEmpty();
    }

    private static int clamp(int v, int lo, int hi) {
        return (v < lo) ? lo : (v > hi) ? hi : v;
    }

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

    // ===================================================================
    //  HeightField (primitive) API (new functionality)
    // ===================================================================

    // -------------------------
    // Dimension helpers
    // -------------------------

    /**
     * Compute L0 dimensions (width,height) from full-res source while preserving aspect ratio
     * and clamping the larger side to maxRender.
     *
     * @param fullW     source width (samples)
     * @param fullH     source height (samples)
     * @param maxRender maximum allowed dimension (e.g. 2048)
     * @return int[2] {l0Width, l0Height}
     */
    public static int[] computeL0Dimensions(int fullW, int fullH, int maxRender) {
        if (fullW <= 0 || fullH <= 0 || maxRender <= 0) throw new IllegalArgumentException("Invalid dimensions");
        if (fullW <= maxRender && fullH <= maxRender) return new int[]{fullW, fullH};
        double scale = Math.min((double) maxRender / fullW, (double) maxRender / fullH);
        int w = Math.max(1, (int) Math.round(fullW * scale));
        int h = Math.max(1, (int) Math.round(fullH * scale));
        return new int[]{w, h};
    }

    // -------------------------
    // Conversion helpers
    // -------------------------

    /**
     * Convert a boxed grid to a primitive HeightField (float-backed).
     * Values are cast to float.
     */
    public static HeightField toHeightField(List<List<Double>> grid) {
        if (isEmpty(grid)) return HeightField.allocate(1, 1);
        int h = height(grid), w = width(grid);
        float[] out = new float[Math.multiplyExact(w, h)];
        int idx = 0;
        for (int y = 0; y < h; y++) {
            List<Double> row = grid.get(y);
            for (int x = 0; x < w; x++) out[idx++] = row.get(x).floatValue();
        }
        return new HeightField(w, h, out);
    }

    /**
     * Convert a primitive HeightField to a boxed grid.
     */
    public static List<List<Double>> toGrid(HeightField hf) {
        Objects.requireNonNull(hf, "hf");
        int w = hf.width(), h = hf.height();
        float[] data = hf.data();
        List<List<Double>> out = new ArrayList<>(h);
        int idx = 0;
        for (int y = 0; y < h; y++) {
            ArrayList<Double> row = new ArrayList<>(w);
            for (int x = 0; x < w; x++) row.add((double) data[idx++]);
            out.add(row);
        }
        return out;
    }

    // -------------------------
    // Sampling (HeightField)
    // -------------------------

    /**
     * Sample a HeightField at (x,y) in grid coordinates where integer coordinates
     * correspond to exact samples.
     *
     * @return sampled value as double (to match existing List-grid sample API)
     */
    public static double sample(HeightField hf, double x, double y, Interpolation mode) {
        Objects.requireNonNull(hf, "hf");
        int w = hf.width(), h = hf.height();
        x = clamp(x, 0.0, w - 1.0);
        y = clamp(y, 0.0, h - 1.0);
        return switch (mode) {
            case NEAREST -> sampleNearest(hf, x, y);
            case BILINEAR -> sampleBilinear(hf, x, y);
            case BICUBIC -> sampleBicubicSafe(hf, x, y);
        };
    }

    /**
     * Default HeightField sample: bilinear.
     */
    public static double sample(HeightField hf, double x, double y) {
        return sample(hf, x, y, Interpolation.BILINEAR);
    }

    private static double sampleNearest(HeightField hf, double x, double y) {
        int w = hf.width(), h = hf.height();
        int ix = (int) Math.round(clamp(x, 0, w - 1));
        int iy = (int) Math.round(clamp(y, 0, h - 1));
        return hf.data()[iy * w + ix];
    }

    public static double sampleBilinear(HeightField hf, double x, double y) {
        int w = hf.width(), h = hf.height();

        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        double tx = x - x0;
        double ty = y - y0;

        x0 = clamp(x0, 0, w - 1);
        x1 = clamp(x1, 0, w - 1);
        y0 = clamp(y0, 0, h - 1);
        y1 = clamp(y1, 0, h - 1);
        if (x0 == x1) tx = 0.0;
        if (y0 == y1) ty = 0.0;

        float[] d = hf.data();
        double c00 = d[y0 * w + x0];
        double c10 = d[y0 * w + x1];
        double c01 = d[y1 * w + x0];
        double c11 = d[y1 * w + x1];

        double cx0 = lerp(c00, c10, tx);
        double cx1 = lerp(c01, c11, tx);
        return lerp(cx0, cx1, ty);
    }

    public static double sampleBicubicSafe(HeightField hf, double x, double y) {
        int w = hf.width(), h = hf.height();
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        if (ix < 1 || ix > w - 3 || iy < 1 || iy > h - 3) {
            return sampleBilinear(hf, x, y);
        }
        double tx = x - ix, ty = y - iy;

        float[] d = hf.data();
        double[] col = new double[4];
        for (int row = -1; row <= 2; row++) {
            int yy = (iy + row) * w;
            double p0 = d[yy + (ix - 1)];
            double p1 = d[yy + (ix)];
            double p2 = d[yy + (ix + 1)];
            double p3 = d[yy + (ix + 2)];
            col[row + 1] = cubicCatmullRom(p0, p1, p2, p3, tx);
        }
        return cubicCatmullRom(col[0], col[1], col[2], col[3], ty);
    }

    // -------------------------
    // Resampling / Pyramid
    // -------------------------

    /**
     * Resample the source heightfield to target dimensions using bilinear filtering.
     * Destination samples are aligned by mapping destination centers to source coordinates.
     */
    public static HeightField resample(HeightField src, int targetW, int targetH) {
        Objects.requireNonNull(src, "src");
        if (targetW <= 0 || targetH <= 0) throw new IllegalArgumentException("Invalid target dims");
        final int sw = src.width();
        final int sh = src.height();
        final float[] sdata = src.data();
        final float[] out = new float[Math.multiplyExact(targetW, targetH)];

        // Map dst pixel centers to src grid coords.
        // gx = (x + 0.5) * (sw/targetW) - 0.5
        final double sxRatio = (double) sw / targetW;
        final double syRatio = (double) sh / targetH;

        for (int y = 0; y < targetH; y++) {
            double gy = (y + 0.5) * syRatio - 0.5;
            gy = clamp(gy, 0.0, sh - 1.0);
            int y0 = (int) Math.floor(gy);
            int y1 = clamp(y0 + 1, 0, sh - 1);
            double ty = gy - y0;
            if (y0 == y1) ty = 0.0;

            int row0 = y0 * sw;
            int row1 = y1 * sw;

            for (int x = 0; x < targetW; x++) {
                double gx = (x + 0.5) * sxRatio - 0.5;
                gx = clamp(gx, 0.0, sw - 1.0);
                int x0 = (int) Math.floor(gx);
                int x1 = clamp(x0 + 1, 0, sw - 1);
                double tx = gx - x0;
                if (x0 == x1) tx = 0.0;

                double c00 = sdata[row0 + x0];
                double c10 = sdata[row0 + x1];
                double c01 = sdata[row1 + x0];
                double c11 = sdata[row1 + x1];

                double a = lerp(c00, c10, tx);
                double b = lerp(c01, c11, tx);
                out[y * targetW + x] = (float) lerp(a, b, ty);
            }
        }

        return new HeightField(targetW, targetH, out);
    }

    /**
     * Downsample by ~2x. Uses bilinear resample; safe for odd sizes.
     */
    public static HeightField downsample2x(HeightField src) {
        Objects.requireNonNull(src, "src");
        int tw = Math.max(1, src.width() / 2);
        int th = Math.max(1, src.height() / 2);
        return resample(src, tw, th);
    }

    /**
     * Build L0 raw by resampling full-res to aspect-preserving dimensions clamped by maxRender.
     */
    public static HeightField buildL0Raw(HeightField fullRes, int maxRender) {
        Objects.requireNonNull(fullRes, "fullRes");
        int[] dims = computeL0Dimensions(fullRes.width(), fullRes.height(), maxRender);
        if (dims[0] == fullRes.width() && dims[1] == fullRes.height()) {
            // processL0() does not mutate its input, so avoid an unnecessary same-size copy.
            return fullRes;
        }
        return resample(fullRes, dims[0], dims[1]);
    }

    /**
     * Build a pyramid of processed levels from processed L0.
     * Each subsequent level is downsampled by 2x until the next would fall below minDim
     * on either axis.
     *
     * @return levels [L0, L1, L2, ...]
     */
    public static List<HeightField> buildPyramid(HeightField l0Processed, int minDim) {
        Objects.requireNonNull(l0Processed, "l0Processed");
        if (minDim <= 0) throw new IllegalArgumentException("minDim must be > 0");
        ArrayList<HeightField> levels = new ArrayList<>();
        levels.add(l0Processed);
        HeightField prev = l0Processed;
        while (true) {
            int nextW = Math.max(1, prev.width() / 2);
            int nextH = Math.max(1, prev.height() / 2);
            if (Math.min(nextW, nextH) < minDim) break;
            HeightField next = resample(prev, nextW, nextH);
            levels.add(next);
            prev = next;
        }
        return levels;
    }

    /**
     * Build exactly up to {@code maxLevels} power-of-two LOD levels from processed L0.
     * This is intended for tiled rendering where the useful stopping criterion is the
     * number of tile-local LOD steps rather than a global image dimension.
     *
     * <p>The method stops early only if both dimensions can no longer be reduced.</p>
     *
     * @return levels [L0, L1, ...] with at most {@code maxLevels} entries
     */
    public static List<HeightField> buildPyramidLevels(HeightField l0Processed, int maxLevels) {
        Objects.requireNonNull(l0Processed, "l0Processed");
        if (maxLevels < 1) {
            throw new IllegalArgumentException("maxLevels must be >= 1");
        }

        ArrayList<HeightField> levels = new ArrayList<>(maxLevels);
        levels.add(l0Processed);
        HeightField prev = l0Processed;

        while (levels.size() < maxLevels) {
            int nextW = Math.max(1, prev.width() / 2);
            int nextH = Math.max(1, prev.height() / 2);
            if (nextW == prev.width() && nextH == prev.height()) break;

            HeightField next = resample(prev, nextW, nextH);
            levels.add(next);
            prev = next;
        }
        return levels;
    }

    // -------------------------
    // Smoothing (HeightField)
    // -------------------------

    /**
     * Smooth a HeightField and return a new HeightField.
     *
     * <p>Behavior mirrors the List-grid version:
     * <ul>
     *   <li>NONE or radius<=0 or iterations<=0 returns a copy</li>
     *   <li>GAUSSIAN uses sigma if >0, else heuristic</li>
     * </ul>
     */
    public static HeightField smoothCopy(
        HeightField hf,
        Smoothing method,
        int radius,
        int iterations,
        double sigma
    ) {
        Objects.requireNonNull(hf, "hf");
        if (method == null) method = Smoothing.NONE;
        if (method == Smoothing.NONE || radius <= 0 || iterations <= 0) return hf.copy();

        HeightField src = hf.copy();
        HeightField dst = HeightField.allocate(hf.width(), hf.height());

        switch (method) {
            case BOX -> {
                for (int i = 0; i < iterations; i++) {
                    boxBlurSeparable(src, dst, radius);
                    HeightField tmp = src;
                    src = dst;
                    dst = tmp;
                }
            }
            case GAUSSIAN -> {
                double[] k = gaussianKernel(radius, sigma);
                for (int i = 0; i < iterations; i++) {
                    convolveSeparable(src, dst, k);
                    HeightField tmp = src;
                    src = dst;
                    dst = tmp;
                }
            }
            case MEDIAN -> {
                for (int i = 0; i < iterations; i++) {
                    medianFilter(src, dst, radius);
                    HeightField tmp = src;
                    src = dst;
                    dst = tmp;
                }
            }
            default -> { /* NONE handled above */ }
        }
        return src;
    }

    /**
     * Convenience overload: one iteration, sigma applied only for GAUSSIAN.
     */
    public static HeightField smooth(HeightField hf, Smoothing method, double sigma, int radius) {
        return smoothCopy(hf, method, radius, 1, sigma);
    }

    /**
     * Convenience overload: one iteration, heuristic sigma for GAUSSIAN.
     */
    public static HeightField smooth(HeightField hf, Smoothing method, int radius) {
        return smoothCopy(hf, method, radius, 1, -1.0);
    }

    // HeightField smoothing internals

    private static void boxBlurSeparable(HeightField src, HeightField dst, int radius) {
        double[] k = boxKernel(radius);
        convolveSeparable(src, dst, k);
    }

    private static void convolveSeparable(HeightField src, HeightField dst, double[] kernel) {
        final int w = src.width();
        final int h = src.height();
        final int r = kernel.length / 2;

        float[] s = src.data();
        float[] tmp = new float[Math.multiplyExact(w, h)];
        float[] out = dst.data();

        // horizontal
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                double acc = 0.0;
                for (int i = -r; i <= r; i++) {
                    int xx = clamp(x + i, 0, w - 1);
                    acc += s[row + xx] * kernel[i + r];
                }
                tmp[row + x] = (float) acc;
            }
        }

        // vertical
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double acc = 0.0;
                for (int i = -r; i <= r; i++) {
                    int yy = clamp(y + i, 0, h - 1);
                    acc += tmp[yy * w + x] * kernel[i + r];
                }
                out[y * w + x] = (float) acc;
            }
        }
    }

    private static void medianFilter(HeightField src, HeightField dst, int radius) {
        final int w = src.width();
        final int h = src.height();
        final int size = (2 * radius + 1) * (2 * radius + 1);

        float[] s = src.data();
        float[] out = dst.data();
        float[] win = new float[size];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = 0;
                for (int dy = -radius; dy <= radius; dy++) {
                    int yy = clamp(y + dy, 0, h - 1);
                    int row = yy * w;
                    for (int dx = -radius; dx <= radius; dx++) {
                        int xx = clamp(x + dx, 0, w - 1);
                        win[idx++] = s[row + xx];
                    }
                }
                java.util.Arrays.sort(win);
                float med = (size % 2 == 1) ? win[size / 2] : (0.5f * (win[size / 2 - 1] + win[size / 2]));
                out[y * w + x] = med;
            }
        }
    }

    // -------------------------
    // Tone mapping (HeightField)
    // -------------------------

    /**
     * Tone-map a HeightField and return a new HeightField.
     * Semantics match {@link #toneMapCopy(List, ToneMap, double)}.
     */
    public static HeightField toneMapCopy(HeightField hf, ToneMap mode, double param) {
        Objects.requireNonNull(hf, "hf");
        if (mode == null) mode = ToneMap.NONE;
        if (mode == ToneMap.NONE) return hf.copy();

        final int w = hf.width();
        final int h = hf.height();
        final float[] in = hf.data();
        final float[] out = new float[Math.multiplyExact(w, h)];

        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        double mean = 0.0, m2 = 0.0;
        int n = 0;

        if (mode == ToneMap.NORMALIZE_01 || mode == ToneMap.LOG1P || mode == ToneMap.GAMMA) {
            for (float v : in) {
                if (v < min) min = v;
                if (v > max) max = v;
            }
        } else if (mode == ToneMap.ZSCORE) {
            for (float vf : in) {
                double v = vf;
                n++;
                double d = v - mean;
                mean += d / n;
                double d2 = v - mean;
                m2 += d * d2;
            }
        }

        final double range = (max - min);
        final double gamma = (mode == ToneMap.GAMMA && param > 0) ? param : 2.2;

        for (int i = 0; i < in.length; i++) {
            double v = in[i];
            double r;
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
            out[i] = (float) r;
        }

        return new HeightField(w, h, out);
    }

    /**
     * Alias mirroring List-grid {@link #toneMapGrid(List, ToneMap, double)}.
     */
    public static HeightField toneMapGrid(HeightField hf, ToneMap mode, double param) {
        return toneMapCopy(hf, mode, param);
    }

    // -------------------------
    // Convenience pipeline helper (Option B)
    // -------------------------

    /**
     * Process L0: smoothing then tone map. Intended for "Option B" pipeline where
     * L0 is processed once then downsampled into lower LODs.
     *
     * @param l0Raw      resampled raw L0
     * @param smoothing smoothing mode
     * @param radius    smoothing radius
     * @param iterations smoothing iterations
     * @param sigma     gaussian sigma (only for GAUSSIAN; <=0 uses heuristic)
     * @param toneMap   tone-map mode
     * @param toneParam tone-map parameter (GAMMA uses this if >0)
     * @return processed L0
     */
    public static HeightField processL0(
        HeightField l0Raw,
        Smoothing smoothing,
        int radius,
        int iterations,
        double sigma,
        ToneMap toneMap,
        double toneParam
    ) {
        Objects.requireNonNull(l0Raw, "l0Raw");
        Smoothing effectiveSmoothing = smoothing != null ? smoothing : Smoothing.NONE;
        ToneMap effectiveToneMap = toneMap != null ? toneMap : ToneMap.NONE;
        boolean smoothActive = effectiveSmoothing != Smoothing.NONE && radius > 0 && iterations > 0;
        boolean toneActive = effectiveToneMap != ToneMap.NONE;

        if (!smoothActive && !toneActive) {
            return l0Raw.copy();
        }
        if (!smoothActive) {
            return toneMapCopy(l0Raw, effectiveToneMap, toneParam);
        }

        HeightField smoothed = smoothCopy(l0Raw, effectiveSmoothing, radius, iterations, sigma);
        return toneActive ? toneMapCopy(smoothed, effectiveToneMap, toneParam) : smoothed;
    }
}