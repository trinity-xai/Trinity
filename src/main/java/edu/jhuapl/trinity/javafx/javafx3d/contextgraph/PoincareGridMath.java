package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

/**
 * Pure mathematical helpers for a Poincare disk in the unit ball of H^3,
 * with constant sectional curvature -1.
 *
 * A disk through the center of the ball is a totally geodesic H^2 plane.
 * No JavaFX dependency: usable from deterministic headless tests.
 */
public final class PoincareGridMath {
    private PoincareGridMath() { }

    /** Euclidean radius fraction corresponding to hyperbolic distance d. */
    public static double euclideanFraction(double hyperbolicDistance) {
        requireFiniteNonnegative(hyperbolicDistance, "hyperbolicDistance");
        return Math.tanh(hyperbolicDistance / 2.0);
    }

    /** Hyperbolic distance from origin at Euclidean radius fraction r in [0,1). */
    public static double hyperbolicDistance(double euclideanFraction) {
        if (!Double.isFinite(euclideanFraction) || euclideanFraction < 0.0
                || euclideanFraction >= 1.0) {
            throw new IllegalArgumentException("euclideanFraction must be finite and in [0,1)");
        }
        return 2.0 * atanh(euclideanFraction);
    }

    /** Physical/model radius for a disk of specified Euclidean display radius. */
    public static double ringRadius(double displayBallRadius, double hyperbolicDistance) {
        if (!Double.isFinite(displayBallRadius) || displayBallRadius <= 0.0) {
            throw new IllegalArgumentException("displayBallRadius must be finite and > 0");
        }
        return displayBallRadius * euclideanFraction(hyperbolicDistance);
    }

    private static double atanh(double x) {
        return 0.5 * (Math.log1p(x) - Math.log1p(-x));
    }

    private static void requireFiniteNonnegative(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and >= 0");
        }
    }
}
