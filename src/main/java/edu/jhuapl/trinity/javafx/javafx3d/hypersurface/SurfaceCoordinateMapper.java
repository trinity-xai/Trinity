package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

/**
 * Centralizes coordinate conversion between source data, the active render grid,
 * and the centered world coordinates used by the Hypersurface scene.
 *
 * <p>The mapper deliberately keeps source dimensions independent from render
 * dimensions. This matters for image-backed surfaces where an 8K source may be
 * rendered from a capped 2K L0 HeightField while preserving the original source
 * row/column semantics.</p>
 *
 * @author Sean Phillips
 */
public final class SurfaceCoordinateMapper {

    private int sourceWidth = 1;
    private int sourceHeight = 1;
    private int renderWidth = 1;
    private int renderHeight = 1;
    private double worldWidth = 1.0;
    private double worldDepth = 1.0;
    private SurfaceRowOrientation rowOrientation = SurfaceRowOrientation.FIRST_ROW_NEAR;

    public void configure(int sourceWidth,
                          int sourceHeight,
                          int renderWidth,
                          int renderHeight,
                          double worldWidth,
                          double worldDepth,
                          SurfaceRowOrientation rowOrientation) {
        this.sourceWidth = Math.max(1, sourceWidth);
        this.sourceHeight = Math.max(1, sourceHeight);
        this.renderWidth = Math.max(1, renderWidth);
        this.renderHeight = Math.max(1, renderHeight);
        this.worldWidth = sanitizeExtent(worldWidth);
        this.worldDepth = sanitizeExtent(worldDepth);
        this.rowOrientation = rowOrientation != null
            ? rowOrientation
            : SurfaceRowOrientation.FIRST_ROW_NEAR;
    }

    public double sourceColumnToWorldX(int sourceColumn) {
        return sourceColumnToSurfaceX(sourceColumn) - worldWidth / 2.0;
    }

    public int worldXToSourceColumn(double worldX) {
        return surfaceXToSourceColumn(worldX + worldWidth / 2.0);
    }

    public double sourceRowToWorldZ(int sourceRow) {
        return sourceRowToSurfaceZ(sourceRow) - worldDepth / 2.0;
    }

    public int worldZToSourceRow(double worldZ) {
        return surfaceZToSourceRow(worldZ + worldDepth / 2.0);
    }

    /**
     * Converts a source column to the uncentered surface-local X coordinate used
     * by HyperSurfacePlotMesh and TiledSurfaceRenderer before their root translation.
     */
    public double sourceColumnToSurfaceX(int sourceColumn) {
        int clamped = clampIndex(sourceColumn, sourceWidth);
        return clamped * sourceCellWidth();
    }

    public int surfaceXToSourceColumn(double surfaceX) {
        int visualColumn = coordinateToIndex(surfaceX, worldWidth, sourceWidth);
        return clampIndex(visualColumn, sourceWidth);
    }

    /**
     * Converts a source row to uncentered surface-local Z. Row orientation is
     * applied here, so source row zero maps to either the near or far edge.
     */
    public double sourceRowToSurfaceZ(int sourceRow) {
        int clamped = clampIndex(sourceRow, sourceHeight);
        int visualRow = orientSourceRowToVisualRow(clamped);
        return visualRow * sourceCellDepth();
    }

    public int surfaceZToSourceRow(double surfaceZ) {
        int visualRow = coordinateToIndex(surfaceZ, worldDepth, sourceHeight);
        return orientVisualRowToSourceRow(visualRow);
    }

    /** Render-grid indices are visual indices and therefore are not row-flipped. */
    public int surfaceXToRenderColumn(double surfaceX) {
        return coordinateToIndex(surfaceX, worldWidth, renderWidth);
    }

    /** Render-grid indices are visual indices and therefore are not row-flipped. */
    public int surfaceZToRenderRow(double surfaceZ) {
        return coordinateToIndex(surfaceZ, worldDepth, renderHeight);
    }

    public int renderColumnToSourceColumn(int renderColumn) {
        return mapIndex(clampIndex(renderColumn, renderWidth), renderWidth, sourceWidth);
    }

    public int renderRowToSourceRow(int renderRow) {
        int visualRenderRow = clampIndex(renderRow, renderHeight);
        int visualSourceRow = mapIndex(visualRenderRow, renderHeight, sourceHeight);
        return orientVisualRowToSourceRow(visualSourceRow);
    }

    public int sourceWidth() {
        return sourceWidth;
    }

    public int sourceHeight() {
        return sourceHeight;
    }

    public int renderWidth() {
        return renderWidth;
    }

    public int renderHeight() {
        return renderHeight;
    }

    public double worldWidth() {
        return worldWidth;
    }

    public double worldDepth() {
        return worldDepth;
    }

    private double sourceCellWidth() {
        return worldWidth / sourceWidth;
    }

    private double sourceCellDepth() {
        return worldDepth / sourceHeight;
    }

    private int orientSourceRowToVisualRow(int sourceRow) {
        return rowOrientation == SurfaceRowOrientation.FIRST_ROW_FAR
            ? sourceHeight - 1 - sourceRow
            : sourceRow;
    }

    private int orientVisualRowToSourceRow(int visualRow) {
        int clamped = clampIndex(visualRow, sourceHeight);
        return rowOrientation == SurfaceRowOrientation.FIRST_ROW_FAR
            ? sourceHeight - 1 - clamped
            : clamped;
    }

    private static int coordinateToIndex(double coordinate, double extent, int size) {
        if (size <= 1 || !(extent > 0.0) || !Double.isFinite(coordinate)) return 0;
        double clamped = Math.max(0.0, Math.min(coordinate, Math.nextDown(extent)));
        int index = (int) Math.floor(clamped * size / extent);
        return clampIndex(index, size);
    }

    private static int mapIndex(int index, int fromSize, int toSize) {
        if (toSize <= 1 || fromSize <= 1) return 0;
        double targetCoordinate = ((index + 0.5) * toSize / (double) fromSize) - 0.5;
        return clampIndex((int) Math.round(targetCoordinate), toSize);
    }

    private static int clampIndex(int index, int size) {
        return Math.max(0, Math.min(index, Math.max(1, size) - 1));
    }

    private static double sanitizeExtent(double extent) {
        return Double.isFinite(extent) && extent > 0.0 ? extent : 1.0;
    }
}
