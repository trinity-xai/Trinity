package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

/** Poincare ball mathematics in three dimensions (curvature -1). No JavaFX dependencies. */
public final class ContextGraphHyperbolicMath {
    private static final double EPSILON = 1.0e-12;
    private static final double MAX_NORM = 1.0 - 1.0e-7;

    private ContextGraphHyperbolicMath() { }

    public static boolean insideBall(double[] p) {
        return p != null && p.length == 3 && Double.isFinite(p[0])
            && Double.isFinite(p[1]) && Double.isFinite(p[2]) && dot(p, p) < 1.0;
    }

    public static double[] validate(double[] p) {
        if (!insideBall(p)) throw new IllegalArgumentException("Point must be inside the open unit ball");
        return p.clone();
    }

    public static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /** Mobius addition: u (+) v; maps the open unit ball to itself. */
    public static double[] add(double[] u, double[] v) {
        validate(u);
        validate(v);
        double uu = dot(u, u), vv = dot(v, v), uv = dot(u, v);
        double denominator = 1.0 + 2.0 * uv + uu * vv;
        if (!(denominator > EPSILON)) {
            throw new IllegalArgumentException("Numerically unstable Mobius translation");
        }
        double a = 1.0 + 2.0 * uv + vv;
        double b = 1.0 - uu;
        return clamp(new double[]{(a * u[0] + b * v[0]) / denominator,
            (a * u[1] + b * v[1]) / denominator,
            (a * u[2] + b * v[2]) / denominator});
    }

    /** Moves the specified focus point to the center of the ball. */
    public static double[] recenter(double[] point, double[] focus) {
        return add(new double[]{-focus[0], -focus[1], -focus[2]}, point);
    }

    /** Geodesic midpoint in the Poincare ball; not the Euclidean chart midpoint. */
    public static double[] midpoint(double[] a, double[] b) {
        validate(a);
        validate(b);
        double[] displaced = recenter(b, a);
        double normSquared = dot(displaced, displaced);
        if (normSquared < EPSILON) return a.clone();
        double factor = 1.0 / (1.0 + Math.sqrt(Math.max(0.0, 1.0 - normSquared)));
        double[] half = new double[]{factor * displaced[0],
            factor * displaced[1], factor * displaced[2]};
        return add(a, half);
    }

    /** Exact hyperbolic distance on the unit Poincare ball. */
    public static double distance(double[] a, double[] b) {
        validate(a);
        validate(b);
        double[] delta = {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
        double q = 2.0 * dot(delta, delta) / ((1.0 - dot(a, a)) * (1.0 - dot(b, b)));
        return Math.log1p(q + Math.sqrt(q * (q + 2.0)));
    }

    /** Boundary guard is only for floating-point round-off after valid transforms. */
    private static double[] clamp(double[] p) {
        double norm = Math.sqrt(dot(p, p));
        if (!Double.isFinite(norm)) throw new IllegalArgumentException("Non-finite Mobius result");
        if (norm >= MAX_NORM) {
            double scale = MAX_NORM / norm;
            for (int i = 0; i < 3; i++) p[i] *= scale;
        }
        return p;
    }
}
