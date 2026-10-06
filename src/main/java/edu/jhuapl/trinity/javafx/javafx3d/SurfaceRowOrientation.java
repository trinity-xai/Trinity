package edu.jhuapl.trinity.javafx.javafx3d;

/**
 * Controls how source rows map onto the Hypersurface Z axis.
 *
 * <p>{@link #FIRST_ROW_NEAR} preserves the historical Trinity convention: source
 * row zero is placed at the Z- / near edge of the surface. This is useful for
 * ordered samples where the first/oldest sample should appear closest to the
 * beginning of the surface.</p>
 *
 * <p>{@link #FIRST_ROW_FAR} reverses the row-to-Z mapping: source row zero is
 * placed at the Z+ / far edge. This is the intuitive default for ordinary 2D
 * imagery viewed in perspective because the bottom of the image is then closest
 * to the viewer.</p>
 */
public enum SurfaceRowOrientation {
    FIRST_ROW_NEAR("First Row Near"),
    FIRST_ROW_FAR("First Row Far");

    private final String displayName;

    SurfaceRowOrientation(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
