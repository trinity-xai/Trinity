package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * Lightweight reusable crosshair overlay for Hypersurface height fields.
 *
 * <p>The overlay owns the full crosshair rendering lifecycle: active surface
 * context, pending target, fixed-rate JavaFX pulse coalescing, dirty-state
 * tracking, and the two reusable surface-following ribbon meshes. Callers
 * provide semantic position requests; rapid requests are collapsed to the
 * newest target and mesh updates are capped at 30 Hz.</p>
 *
 * <p>Row and column ribbons are tracked independently. Moving only the row,
 * which is the common graph-hover case, updates only the row mesh point buffer
 * while the unchanged column mesh is left untouched.</p>
 *
 * @author Sean Phillips
 */
public final class SurfaceCrosshairOverlay extends Group {

    private static final double MIN_WORLD_THICKNESS = 0.35;
    private static final double THICKNESS_CELL_FRACTION = 0.35;
    private static final double MIN_LIFT = 0.20;

    /**
     * Maximum crosshair mesh-update rate. Mouse/graph position requests may
     * arrive much faster; only the newest pending target is consumed when this
     * interval has elapsed. This bounds TriangleMesh point-buffer churn while
     * retaining latest-position-wins behavior.
     */
    private static final int CROSSHAIR_UPDATE_HZ = 30;
    private static final long CROSSHAIR_UPDATE_INTERVAL_NANOS =
        1_000_000_000L / CROSSHAIR_UPDATE_HZ;

    private enum RequestSpace {
        NONE,
        RENDER,
        SOURCE
    }

    private final TriangleMesh rowMesh = new TriangleMesh();
    private final TriangleMesh columnMesh = new TriangleMesh();
    private final MeshView rowView = new MeshView(rowMesh);
    private final MeshView columnView = new MeshView(columnMesh);
    private final PhongMaterial material = new PhongMaterial(Color.WHITE);

    private final AnimationTimer updateTimer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (!updatePending) {
                stop();
                return;
            }

            if (lastUpdateNanos != 0L
                && now - lastUpdateNanos < CROSSHAIR_UPDATE_INTERVAL_NANOS) {
                return;
            }

            lastUpdateNanos = now;
            applyPendingUpdate();

            if (!updatePending) stop();
        }
    };

    private HeightField heightField;
    private SurfaceCoordinateMapper coordinateMapper;
    private double worldWidth = 1.0;
    private double worldDepth = 1.0;
    private double yScale = 1.0;
    private SurfaceRowOrientation rowOrientation = SurfaceRowOrientation.FIRST_ROW_NEAR;
    private SurfaceHeightOrientation heightOrientation = SurfaceHeightOrientation.HIGH_VALUES_UP;

    private int topologyWidth = -1;
    private int topologyHeight = -1;
    private float[] rowPoints = new float[0];
    private float[] columnPoints = new float[0];

    private RequestSpace requestSpace = RequestSpace.NONE;
    private int requestedRow = -1;
    private int requestedColumn = -1;
    private boolean updatePending = false;
    private long lastUpdateNanos = 0L;

    private int renderedVisualRow = -1;
    private int renderedVisualColumn = -1;
    private boolean rowGeometryDirty = true;
    private boolean columnGeometryDirty = true;

    public SurfaceCrosshairOverlay() {
        material.setDiffuseColor(Color.WHITE);
        material.setSpecularColor(Color.TRANSPARENT);

        configureView(rowView);
        configureView(columnView);
        getChildren().addAll(rowView, columnView);
        setMouseTransparent(true);
        setVisible(false);
    }

    private void configureView(MeshView view) {
        view.setMaterial(material);
        view.setCullFace(CullFace.NONE);
        view.setMouseTransparent(true);
    }

    public void setColor(Color color) {
        Color resolved = color != null ? color : Color.WHITE;
        material.setDiffuseColor(resolved);
    }

    /**
     * Configures the surface context used by future crosshair requests.
     *
     * <p>If the geometry context changes while a crosshair request is active,
     * the current semantic request is reapplied on the next permitted crosshair update. This
     * keeps source-space requests correct across orientation, scale, and LOD
     * changes without exposing that bookkeeping to Hypersurface3DPane.</p>
     */
    public void configureSurface(HeightField heightField,
                                 SurfaceCoordinateMapper coordinateMapper,
                                 double yScale,
                                 SurfaceRowOrientation rowOrientation,
                                 SurfaceHeightOrientation heightOrientation) {
        if (heightField == null || coordinateMapper == null
            || heightField.width() <= 0 || heightField.height() <= 0) {
            clearSurface();
            return;
        }

        double newWorldWidth = coordinateMapper.worldWidth();
        double newWorldDepth = coordinateMapper.worldDepth();
        SurfaceRowOrientation newRowOrientation = rowOrientation != null
            ? rowOrientation
            : SurfaceRowOrientation.FIRST_ROW_NEAR;
        SurfaceHeightOrientation newHeightOrientation = heightOrientation != null
            ? heightOrientation
            : SurfaceHeightOrientation.HIGH_VALUES_UP;

        boolean geometryChanged = this.heightField != heightField
            || this.coordinateMapper != coordinateMapper
            || Double.compare(this.worldWidth, newWorldWidth) != 0
            || Double.compare(this.worldDepth, newWorldDepth) != 0
            || Double.compare(this.yScale, yScale) != 0
            || this.rowOrientation != newRowOrientation
            || this.heightOrientation != newHeightOrientation;

        this.heightField = heightField;
        this.coordinateMapper = coordinateMapper;
        this.worldWidth = sanitizeExtent(newWorldWidth);
        this.worldDepth = sanitizeExtent(newWorldDepth);
        this.yScale = Double.isFinite(yScale) ? yScale : 1.0;
        this.rowOrientation = newRowOrientation;
        this.heightOrientation = newHeightOrientation;

        if (geometryChanged) {
            rowGeometryDirty = true;
            columnGeometryDirty = true;
            if (requestSpace != RequestSpace.NONE) scheduleUpdate();
        }
    }

    /**
     * Requests a crosshair using visual render-grid indices.
     * Intermediate requests are coalesced to the newest target. Mesh updates
     * are rate-limited by {@link #CROSSHAIR_UPDATE_HZ}.
     */
    public void requestRenderPosition(int visualRow, int visualColumn) {
        if (!hasSurface()) {
            hide();
            return;
        }
        requestSpace = RequestSpace.RENDER;
        requestedRow = visualRow;
        requestedColumn = visualColumn;
        scheduleUpdate();
    }

    /**
     * Requests a crosshair using source-data row/column coordinates.
     */
    public void requestSourcePosition(int sourceRow, int sourceColumn) {
        if (!hasSurface()) {
            hide();
            return;
        }
        requestSpace = RequestSpace.SOURCE;
        requestedRow = sourceRow;
        requestedColumn = sourceColumn;
        scheduleUpdate();
    }

    /**
     * Requests a source row with the perpendicular crosshair at the source center.
     * This is the graph-node highlighting path.
     */
    public void requestSourceRow(int sourceRow) {
        if (!hasSurface()) {
            hide();
            return;
        }
        requestSourcePosition(sourceRow, coordinateMapper.sourceWidth() / 2);
    }

    /**
     * Cancels any pending update and hides the overlay. Surface configuration is
     * retained so a later position request can be serviced without reconfiguration.
     */
    public void hide() {
        updatePending = false;
        requestSpace = RequestSpace.NONE;
        requestedRow = -1;
        requestedColumn = -1;
        updateTimer.stop();
        lastUpdateNanos = 0L;
        setVisible(false);
    }

    /**
     * Releases the active surface context and invalidates rendered geometry.
     */
    public void clearSurface() {
        hide();
        heightField = null;
        coordinateMapper = null;
        renderedVisualRow = -1;
        renderedVisualColumn = -1;
        rowGeometryDirty = true;
        columnGeometryDirty = true;
    }

    private void scheduleUpdate() {
        updatePending = true;
        updateTimer.start();
    }

    private void applyPendingUpdate() {
        if (!updatePending) return;
        updatePending = false;

        if (!hasSurface() || requestSpace == RequestSpace.NONE) {
            setVisible(false);
            return;
        }

        int visualRow;
        int visualColumn;
        if (requestSpace == RequestSpace.SOURCE) {
            double surfaceZ = coordinateMapper.sourceRowToSurfaceZ(requestedRow);
            double surfaceX = coordinateMapper.sourceColumnToSurfaceX(requestedColumn);
            visualRow = coordinateMapper.surfaceZToRenderRow(surfaceZ);
            visualColumn = coordinateMapper.surfaceXToRenderColumn(surfaceX);
        } else {
            visualRow = requestedRow;
            visualColumn = requestedColumn;
        }

        final int width = heightField.width();
        final int height = heightField.height();
        ensureTopology(width, height);

        final int row = clamp(visualRow, height);
        final int column = clamp(visualColumn, width);
        final double scaleX = worldWidth / width;
        final double scaleZ = worldDepth / height;
        final double halfRowThickness = Math.max(
            MIN_WORLD_THICKNESS,
            scaleZ * THICKNESS_CELL_FRACTION) * 0.5;
        final double halfColumnThickness = Math.max(
            MIN_WORLD_THICKNESS,
            scaleX * THICKNESS_CELL_FRACTION) * 0.5;
        final double lift = Math.max(MIN_LIFT, Math.abs(yScale) * 0.02);

        if (rowGeometryDirty || row != renderedVisualRow) {
            updateRowPoints(heightField, row, scaleX, scaleZ,
                halfRowThickness, lift);
            rowMesh.getPoints().set(0, rowPoints, 0, rowPoints.length);
            renderedVisualRow = row;
            rowGeometryDirty = false;
        }

        if (columnGeometryDirty || column != renderedVisualColumn) {
            updateColumnPoints(heightField, column, scaleX, scaleZ,
                halfColumnThickness, lift);
            columnMesh.getPoints().set(0, columnPoints, 0, columnPoints.length);
            renderedVisualColumn = column;
            columnGeometryDirty = false;
        }

        setVisible(true);
    }

    private void updateRowPoints(HeightField heightField,
                                 int visualRow,
                                 double scaleX,
                                 double scaleZ,
                                 double halfThickness,
                                 double lift) {
        final int width = heightField.width();
        final int sourceRow = orientVisualRow(visualRow, heightField.height(), rowOrientation);
        final int rowOffset = sourceRow * width;
        final float[] data = heightField.data();
        final double zCenter = visualRow * scaleZ - worldDepth / 2.0;

        int out = 0;
        for (int vx = 0; vx <= width; vx++) {
            int sampleX = Math.min(vx, width - 1);
            double x = vx * scaleX - worldWidth / 2.0;
            double y = heightOrientation.toWorldY(data[rowOffset + sampleX], yScale) - lift;

            rowPoints[out++] = (float) x;
            rowPoints[out++] = (float) y;
            rowPoints[out++] = (float) (zCenter - halfThickness);

            rowPoints[out++] = (float) x;
            rowPoints[out++] = (float) y;
            rowPoints[out++] = (float) (zCenter + halfThickness);
        }
    }

    private void updateColumnPoints(HeightField heightField,
                                    int visualColumn,
                                    double scaleX,
                                    double scaleZ,
                                    double halfThickness,
                                    double lift) {
        final int width = heightField.width();
        final int height = heightField.height();
        final float[] data = heightField.data();
        final double xCenter = visualColumn * scaleX - worldWidth / 2.0;

        int out = 0;
        for (int vz = 0; vz <= height; vz++) {
            int visualSampleRow = Math.min(vz, height - 1);
            int sourceRow = orientVisualRow(visualSampleRow, height, rowOrientation);
            double z = vz * scaleZ - worldDepth / 2.0;
            double y = heightOrientation.toWorldY(
                data[sourceRow * width + visualColumn], yScale) - lift;

            columnPoints[out++] = (float) (xCenter - halfThickness);
            columnPoints[out++] = (float) y;
            columnPoints[out++] = (float) z;

            columnPoints[out++] = (float) (xCenter + halfThickness);
            columnPoints[out++] = (float) y;
            columnPoints[out++] = (float) z;
        }
    }

    private void ensureTopology(int width, int height) {
        if (width == topologyWidth && height == topologyHeight) return;

        topologyWidth = width;
        topologyHeight = height;
        rowPoints = new float[(width + 1) * 2 * 3];
        columnPoints = new float[(height + 1) * 2 * 3];

        rowMesh.getPoints().setAll(rowPoints);
        columnMesh.getPoints().setAll(columnPoints);
        rowMesh.getTexCoords().setAll(0.0f, 0.0f);
        columnMesh.getTexCoords().setAll(0.0f, 0.0f);
        rowMesh.getFaces().setAll(buildRibbonFaces(width));
        columnMesh.getFaces().setAll(buildRibbonFaces(height));

        renderedVisualRow = -1;
        renderedVisualColumn = -1;
        rowGeometryDirty = true;
        columnGeometryDirty = true;
    }

    private boolean hasSurface() {
        return heightField != null
            && coordinateMapper != null
            && heightField.width() > 0
            && heightField.height() > 0
            && worldWidth > 0.0
            && worldDepth > 0.0;
    }

    private static int[] buildRibbonFaces(int segments) {
        int[] faces = new int[segments * 2 * 6];
        int out = 0;
        for (int i = 0; i < segments; i++) {
            int a = i * 2;
            int b = a + 1;
            int c = a + 2;
            int d = a + 3;

            faces[out++] = a; faces[out++] = 0;
            faces[out++] = c; faces[out++] = 0;
            faces[out++] = d; faces[out++] = 0;

            faces[out++] = d; faces[out++] = 0;
            faces[out++] = b; faces[out++] = 0;
            faces[out++] = a; faces[out++] = 0;
        }
        return faces;
    }

    private static int orientVisualRow(int visualRow,
                                       int height,
                                       SurfaceRowOrientation orientation) {
        int clamped = clamp(visualRow, height);
        return orientation == SurfaceRowOrientation.FIRST_ROW_FAR
            ? height - 1 - clamped
            : clamped;
    }

    private static int clamp(int value, int size) {
        return Math.max(0, Math.min(value, Math.max(1, size) - 1));
    }

    private static double sanitizeExtent(double extent) {
        return Double.isFinite(extent) && extent > 0.0 ? extent : 1.0;
    }
}
