package edu.jhuapl.trinity.utils;

import javafx.scene.paint.Color;

/**
 * MatrixViewUtil
 * --------------
 * Small helper utilities shared by heatmap/matrix views.
 *
 * @author Sean Phillips
 */
public final class MatrixViewUtil {

    private MatrixViewUtil() {
    }

    // ---- math helpers ----
    public static double[] minMax(double[][] m) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < m.length; i++) {
            double[] row = m[i];
            for (int j = 0; j < row.length; j++) {
                double v = row[j];
                if (Double.isNaN(v) || Double.isInfinite(v)) continue;
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }
        if (min == Double.POSITIVE_INFINITY) {
            min = 0.0;
            max = 1.0;
        }
        if (max - min < 1e-12) max = min + 1e-12;
        return new double[]{min, max};
    }

    public static double norm01(double v, double min, double max) {
        double t = (v - min) / (max - min);
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        return t;
    }

    public static double sanitize(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0;
        return v;
    }

    // ---- palettes ----
    public static Color sequentialColor(double t) {
        // dark blue → teal → yellow (simple perceptual-ish)
        double r = clamp01(-0.5 + 2.2 * t);
        double g = clamp01(0.1 + 1.9 * t);
        double b = clamp01(1.0 - 0.9 * t);
        double gamma = 0.95;
        return Color.color(Math.pow(r, gamma), Math.pow(g, gamma), Math.pow(b, gamma));
    }

    public static Color divergingColor(double v, double min, double max, double center) {
        // Map to [-1, 1] where 0 = center
        double t = (v - center) / (Math.max(Math.abs(max - center), Math.abs(center - min)) + 1e-12);
        if (t < -1) t = -1;
        if (t > 1) t = 1;
        if (t >= 0) {
            double a = t;
            double r = 1.0;
            double g = 1.0 - 0.6 * a;
            double b = 1.0 - 0.9 * a;
            return Color.color(r, g, b);
        } else {
            double a = -t;
            double r = 1.0 - 0.9 * a;
            double g = 1.0 - 0.8 * a;
            double b = 1.0;
            return Color.color(r, g, b);
        }
    }

    private static double clamp01(double x) {
        if (x < 0) return 0;
        if (x > 1) return 1;
        return x;
    }

    // ---- labels ----

    /**
     * Replace "Comp 7" → "7"; "Comp7" → "7"; otherwise returns original.
     */
    public static String compactCompLabel(String s) {
        if (s == null) return null;
        String x = s.trim();
        if (x.startsWith("Comp ")) {
            String tail = x.substring(5).trim();
            return tail.isEmpty() ? x : tail;
        }
        if (x.regionMatches(true, 0, "Comp", 0, 4) && x.length() > 4) {
            String tail = x.substring(4).trim();
            return tail.isEmpty() ? x : tail;
        }
        return x;
    }
}
