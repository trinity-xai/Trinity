package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

/**
 * Presentation-ready inspection state for a hovered Hypersurface location.
 *
 * <p>All source/image coordinates are resolved before publication so consumers such
 * as Content Navigator do not need to understand LOD, row orientation, or tile
 * layout. Tile maximum coordinates are exclusive bounds.</p>
 *
 * @author Sean Phillips
 */
public record SurfaceInspection(
    boolean imageBacked,
    int sourceWidth,
    int sourceHeight,
    int renderWidth,
    int renderHeight,
    int renderColumn,
    int renderRow,
    int sourceColumn,
    int sourceRow,
    int imageColumn,
    int imageRow,
    double surfaceValue,
    double worldX,
    double worldY,
    double worldZ,
    double rowMinimum,
    double rowMaximum,
    double columnMinimum,
    double columnMaximum,
    int red,
    int green,
    int blue,
    double imageIntensity,
    int tileId,
    int activeLod,
    int tileImageMinX,
    int tileImageMinY,
    int tileImageMaxXExclusive,
    int tileImageMaxYExclusive
) {
    public boolean hasImageSample() {
        return imageBacked && red >= 0 && green >= 0 && blue >= 0;
    }

    public boolean hasTile() {
        return tileId >= 0
            && tileImageMaxXExclusive > tileImageMinX
            && tileImageMaxYExclusive > tileImageMinY;
    }
}
