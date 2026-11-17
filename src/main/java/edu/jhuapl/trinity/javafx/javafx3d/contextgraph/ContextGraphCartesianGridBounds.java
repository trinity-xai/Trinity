package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

/**
 * Value-only Euclidean grid framing computed from actual displayed node positions.
 * Does not inspect JavaFX scene bounds (which include the grid itself).
 */
public record ContextGraphCartesianGridBounds(
        double centerX, double floorY, double centerZ, double width, double length) {
    private static final double STARTUP_SIZE = 200.0;
    private static final double MIN_SIZE = 20.0;
    private static final double PADDING = 0.12;

    public static ContextGraphCartesianGridBounds empty() {
        return new ContextGraphCartesianGridBounds(0, 0, 0, STARTUP_SIZE, STARTUP_SIZE);
    }

    public static ContextGraphCartesianGridBounds fromExtent(
            double minX, double maxX, double minY, double maxY, double minZ, double maxZ) {
        if (!Double.isFinite(minX) || !Double.isFinite(maxX)
                || !Double.isFinite(minY) || !Double.isFinite(maxY)
                || !Double.isFinite(minZ) || !Double.isFinite(maxZ)
                || maxX < minX || maxY < minY || maxZ < minZ) return empty();
        double spanX = maxX - minX;
        double spanY = maxY - minY;
        double spanZ = maxZ - minZ;
        if (!Double.isFinite(spanX) || !Double.isFinite(spanY)
                || !Double.isFinite(spanZ)) return empty();
        double horizontalSpan = Math.max(spanX, spanZ);
        // Avoid an almost zero-width grid for a line or a nearly planar dataset.
        double minimumAxis = Math.max(MIN_SIZE, horizontalSpan * 0.25);
        double width = Math.max(minimumAxis, spanX * (1 + 2 * PADDING));
        double length = Math.max(minimumAxis, spanZ * (1 + 2 * PADDING));
        // JavaFX positive Y points downward. Put the grid slightly below the nodes.
        double depth = Math.max(MIN_SIZE, Math.max(horizontalSpan, spanY));
        return new ContextGraphCartesianGridBounds(
            minX + spanX / 2, maxY + depth * 0.06, minZ + spanZ / 2, width, length);
    }
}
