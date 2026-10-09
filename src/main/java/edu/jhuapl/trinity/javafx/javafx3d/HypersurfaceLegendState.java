package edu.jhuapl.trinity.javafx.javafx3d;

/**
 * Immutable presentation state for the Hypersurface value/color legend.
 */
public record HypersurfaceLegendState(
    String heightLabel,
    double heightMinimum,
    double heightMaximum,
    double heightScale,
    SurfaceHeightOrientation heightOrientation,
    Hypersurface3DPane.COLORATION coloration,
    String colorLabel,
    boolean numericColorRange,
    double colorMinimum,
    double colorMaximum
) {
}
