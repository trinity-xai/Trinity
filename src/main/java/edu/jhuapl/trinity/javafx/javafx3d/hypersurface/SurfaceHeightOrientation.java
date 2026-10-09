package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

/**
 * Controls how scalar surface height values map onto the JavaFX Y axis.
 *
 * <p>JavaFX uses a screen-oriented coordinate convention where positive Y points
 * downward. {@link #HIGH_VALUES_UP} therefore maps increasing scalar values toward
 * Y-, which is the intuitive default for elevations, intensity surfaces, and most
 * ordinary height maps.</p>
 *
 * <p>{@link #HIGH_VALUES_DOWN} preserves the historical Trinity mapping where
 * increasing scalar values move toward Y+.</p>
 */
public enum SurfaceHeightOrientation {
    HIGH_VALUES_UP("High Values Up", -1.0),
    HIGH_VALUES_DOWN("High Values Down", 1.0);

    private final String displayName;
    private final double yDirection;

    SurfaceHeightOrientation(String displayName, double yDirection) {
        this.displayName = displayName;
        this.yDirection = yDirection;
    }

    /**
     * Converts a source scalar value into its world-space Y coordinate using the
     * supplied magnitude scale.
     */
    public double toWorldY(double value, double yScale) {
        return value * yScale * yDirection;
    }

    /**
     * Converts an already-scaled world Y coordinate back to an orientation-neutral
     * signed height. This is useful for color mappings that should not reverse when
     * the visual height orientation changes.
     */
    public double toLogicalHeight(double worldY) {
        return worldY * yDirection;
    }

    public double yDirection() {
        return yDirection;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
