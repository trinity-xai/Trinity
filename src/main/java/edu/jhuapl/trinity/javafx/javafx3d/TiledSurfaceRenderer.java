package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tiled renderer for primitive HeightField LOD pyramids.
 *
 * <p>Tile scene-graph state is persistent. Each tile/LOD pair receives its own
 * {@link HyperSurfacePlotMesh} the first time it is needed, and that MeshView
 * remains permanently associated with its TriangleMesh. Subsequent LOD changes
 * only toggle which cached MeshView is visible.</p>
 */
public final class TiledSurfaceRenderer extends Group {

    public record LodStatistics(
        int visibleTiles,
        int totalTiles,
        int lod0Tiles,
        int lod1Tiles,
        int lod2Tiles,
        int lod3Tiles,
        int lod4Tiles,
        long visibleTriangles,
        int pendingTransitions
    ) { }

    public enum ColorMode {
        IMAGE,
        HEIGHT
    }

    private static final int DEFAULT_TILE_CELLS_L0 = 256;

    private final PerspectiveCamera camera;
    private final SubScene subScene;
    private final TiledLodManager lodManager;
    private final Map<Integer, TileRenderState> tilesById = new HashMap<>();

    private List<HeightField> levels = List.of();
    private List<TiledLodManager.TileSpec> tileSpecs = List.of();
    private double baseWorldWidth = 1.0;
    private double baseWorldDepth = 1.0;
    private double yScale = 1.0;
    private int tileCellsL0 = DEFAULT_TILE_CELLS_L0;

    private DrawMode drawMode = DrawMode.FILL;
    private CullFace cullFace = CullFace.BACK;
    private ColorMode colorMode = ColorMode.HEIGHT;
    private Image image;
    private int paletteColors = 1530;
    private double colorMin = 0.0;
    private double colorMax = 1.0;
    private Color specularColor = Color.WHITE;

    private int visibleTileCount;
    private int lastRebuildCount;
    private int lastCacheHitCount;
    private int lastLodChangeCount;

    public TiledSurfaceRenderer(PerspectiveCamera camera, SubScene subScene) {
        this.camera = Objects.requireNonNull(camera, "camera");
        this.subScene = Objects.requireNonNull(subScene, "subScene");
        this.lodManager = new TiledLodManager(camera, subScene, this);
        this.lodManager.setOnDecisions(this::applyVisibilityDecisions);
        this.lodManager.setTransitionApplier(this::applyTransition);
        setPickOnBounds(false);
    }

    public void setTileCellsL0(int tileCellsL0) {
        if (tileCellsL0 < 16) {
            throw new IllegalArgumentException("tileCellsL0 must be >= 16");
        }
        if (this.tileCellsL0 == tileCellsL0) return;
        this.tileCellsL0 = tileCellsL0;
        System.out.println("Tiled Hypersurface tile size updated: tileCellsL0=" + tileCellsL0);
        if (!levels.isEmpty()) {
            rebuildTileLayout();
            forceUpdate();
        }
    }

    public int getTileCellsL0() {
        return tileCellsL0;
    }

    public void setLodConfig(TiledLodManager.Config config) {
        lodManager.setConfig(config);
    }

    public TiledLodManager.Config getLodConfigCopy() {
        return lodManager.getConfigCopy();
    }

    public void setPyramid(List<HeightField> levels,
                           double baseWorldWidth,
                           double baseWorldDepth,
                           double yScale) {
        Objects.requireNonNull(levels, "levels");
        if (levels.isEmpty()) {
            throw new IllegalArgumentException("levels must not be empty");
        }
        if (!(baseWorldWidth > 0.0) || !(baseWorldDepth > 0.0)) {
            throw new IllegalArgumentException("base world dimensions must be > 0");
        }
        this.levels = List.copyOf(levels);
        this.baseWorldWidth = baseWorldWidth;
        this.baseWorldDepth = baseWorldDepth;
        this.yScale = yScale;
        setTranslateX(-baseWorldWidth * 0.5);
        setTranslateZ(-baseWorldDepth * 0.5);
        rebuildTileLayout();
    }

    public void clearSurface() {
        lodManager.clearSurface();
        tilesById.clear();
        tileSpecs = List.of();
        levels = List.of();
        getChildren().clear();
        visibleTileCount = 0;
    }

    public void requestUpdate(LodManager.UpdateReason reason) {
        if (!levels.isEmpty()) lodManager.requestUpdate(reason);
    }

    public void forceUpdate() {
        if (!levels.isEmpty()) lodManager.forceUpdate();
    }

    public void dispose() {
        lodManager.dispose();
        tilesById.clear();
        tileSpecs = List.of();
        levels = List.of();
        getChildren().clear();
    }

    public void setYScale(double yScale) {
        if (Double.compare(this.yScale, yScale) == 0) return;
        this.yScale = yScale;

        // Geometry Y coordinates changed, so all cached per-LOD MeshViews are invalid.
        getChildren().clear();
        for (TileRenderState tile : tilesById.values()) {
            tile.clearViews();
        }
        lodManager.invalidateRenderedState();
        forceUpdate();
    }

    public void setDrawMode(DrawMode drawMode) {
        this.drawMode = Objects.requireNonNull(drawMode, "drawMode");
        forEachBuiltView(view -> view.setDrawMode(drawMode));
    }

    public void setCullFace(CullFace cullFace) {
        this.cullFace = Objects.requireNonNull(cullFace, "cullFace");
        forEachBuiltView(view -> view.setCullFace(cullFace));
    }

    public void setSpecularColor(Color specularColor) {
        this.specularColor = Objects.requireNonNull(specularColor, "specularColor");
        forEachBuiltView(this::applySpecular);
    }

    public void setColorByImage(Image image) {
        this.image = image;
        this.colorMode = ColorMode.IMAGE;
        forEachBuiltView(this::applyColoration);
    }

    public void setColorByHeight(int colors, double min, double max) {
        if (colors < 2) throw new IllegalArgumentException("colors must be >= 2");
        if (!(max > min)) throw new IllegalArgumentException("max must be > min");
        this.paletteColors = colors;
        this.colorMin = min;
        this.colorMax = max;
        this.colorMode = ColorMode.HEIGHT;
        forEachBuiltView(this::applyColoration);
    }

    public int getVisibleTileCount() {
        return visibleTileCount;
    }

    public int getTotalTileCount() {
        return tileSpecs.size();
    }

    public int getLastRebuildCount() {
        return lastRebuildCount;
    }

    public int getLastCacheHitCount() {
        return lastCacheHitCount;
    }

    public int getLastLodChangeCount() {
        return lastLodChangeCount;
    }

    public int getPendingTransitionCount() {
        return lodManager.getPendingTransitionCount();
    }

    public LodStatistics getLodStatistics() {
        int visible = 0;
        int lod0 = 0;
        int lod1 = 0;
        int lod2 = 0;
        int lod3 = 0;
        int lod4 = 0;
        long triangles = 0L;

        for (TileRenderState tile : tilesById.values()) {
            if (!tile.isTileVisible()) continue;
            visible++;
            int lod = tile.getActiveLod();
            if (lod == 0) lod0++;
            else if (lod == 1) lod1++;
            else if (lod == 2) lod2++;
            else if (lod == 3) lod3++;
            else if (lod >= 4) lod4++;
            if (lod >= 0 && lod < levels.size()) {
                triangles += triangleCountFor(tile, lod);
            }
        }

        return new LodStatistics(
            visible, tileSpecs.size(), lod0, lod1, lod2, lod3, lod4, triangles,
            lodManager.getPendingTransitionCount());
    }

    private long triangleCountFor(TileRenderState tile, int lod) {
        if (levels.isEmpty() || lod < 0 || lod >= levels.size()) return 0L;
        HeightField field = levels.get(lod);
        HeightField l0 = levels.get(0);

        int startX = mapBoundary(tile.startX0, l0.width(), field.width());
        int endX = mapBoundary(tile.endX0, l0.width(), field.width());
        int startZ = mapBoundary(tile.startZ0, l0.height(), field.height());
        int endZ = mapBoundary(tile.endZ0, l0.height(), field.height());
        endX = Math.max(startX + 1, Math.min(field.width(), endX));
        endZ = Math.max(startZ + 1, Math.min(field.height(), endZ));
        startX = Math.max(0, Math.min(startX, endX - 1));
        startZ = Math.max(0, Math.min(startZ, endZ - 1));

        long cellsX = endX - startX;
        long cellsZ = endZ - startZ;
        return cellsX * cellsZ * 2L;
    }

    private void rebuildTileLayout() {
        getChildren().clear();
        tilesById.clear();

        HeightField l0 = levels.get(0);
        int l0Width = l0.width();
        int l0Height = l0.height();
        int columns = Math.max(1, (int) Math.ceil(l0Width / (double) tileCellsL0));
        int rows = Math.max(1, (int) Math.ceil(l0Height / (double) tileCellsL0));

        ArrayList<TiledLodManager.TileSpec> specs = new ArrayList<>(columns * rows);
        int id = 0;
        for (int row = 0; row < rows; row++) {
            int startZ = row * tileCellsL0;
            int endZ = Math.min(l0Height, startZ + tileCellsL0);
            double minZ = startZ * baseWorldDepth / l0Height;
            double maxZ = endZ * baseWorldDepth / l0Height;

            for (int column = 0; column < columns; column++) {
                int startX = column * tileCellsL0;
                int endX = Math.min(l0Width, startX + tileCellsL0);
                double minX = startX * baseWorldWidth / l0Width;
                double maxX = endX * baseWorldWidth / l0Width;

                TiledLodManager.TileSpec spec = new TiledLodManager.TileSpec(
                    id, column, row, minX, maxX, minZ, maxZ);
                specs.add(spec);
                tilesById.put(id, new TileRenderState(
                    spec, startX, endX, startZ, endZ, levels.size()));
                id++;
            }
        }
        tileSpecs = List.copyOf(specs);

        float[] minMax = l0.minMax();
        double maxAbsHeight = Math.max(
            Math.abs(minMax[0] * yScale), Math.abs(minMax[1] * yScale));
        lodManager.setSurface(
            LodManager.fromHeightFields(levels),
            tileSpecs,
            baseWorldWidth,
            baseWorldDepth,
            maxAbsHeight
        );

        System.out.println("Tiled Hypersurface configured: tiles=" + columns + "x" + rows
            + " (" + tileSpecs.size() + "), tileCellsL0=" + tileCellsL0
            + ", world=" + baseWorldWidth + "x" + baseWorldDepth
            + ", persistentLodViews=true");
    }

    /**
     * Visibility/frustum changes are applied immediately. LOD changes are not;
     * they are queued and budgeted by TiledLodManager.
     */
    private void applyVisibilityDecisions(List<TiledLodManager.TileDecision> decisions) {
        int visible = 0;
        for (TiledLodManager.TileDecision decision : decisions) {
            TileRenderState tile = tilesById.get(decision.tileId());
            if (tile == null) continue;

            tile.setTileVisible(decision.visible());
            if (decision.visible()) visible++;

            HyperSurfacePlotMesh activeView = tile.getLodView(tile.getActiveLod());
            if (activeView != null) {
                activeView.setVisible(decision.visible() && isVisible());
            }
        }
        visibleTileCount = visible;
    }

    /**
     * Applies one budgeted LOD transition. Existing tile/LOD MeshViews are only
     * shown/hidden; no MeshView.setMesh(...) operation occurs after construction.
     */
    private TiledLodManager.TransitionResult applyTransition(LodTransition transition) {
        TileRenderState tile = tilesById.get(transition.tileId());
        if (tile == null) return TiledLodManager.TransitionResult.notApplied();

        int lod = Math.max(0, Math.min(transition.toLod(), levels.size() - 1));
        HyperSurfacePlotMesh targetView = tile.getLodView(lod);
        boolean built = false;

        if (targetView == null) {
            targetView = buildTileLodView(tile, lod);
            tile.setLodView(lod, targetView);
            built = true;
        }

        HyperSurfacePlotMesh previousView = tile.getLodView(tile.getActiveLod());
        if (previousView != null && previousView != targetView) {
            previousView.setVisible(false);
        }

        tile.setActiveLod(lod);
        targetView.setVisible(tile.isTileVisible() && isVisible());

        lastLodChangeCount = 1;
        lastRebuildCount = built ? 1 : 0;
        lastCacheHitCount = built ? 0 : 1;
        return built
            ? TiledLodManager.TransitionResult.builtResult()
            : TiledLodManager.TransitionResult.cacheHitResult();
    }

    private HyperSurfacePlotMesh buildTileLodView(TileRenderState tile, int lod) {
        HeightField field = levels.get(lod);
        HeightField l0 = levels.get(0);

        // Map the constant L0 tile footprint onto the selected pyramid level.
        int startX = mapBoundary(tile.startX0, l0.width(), field.width());
        int endX = mapBoundary(tile.endX0, l0.width(), field.width());
        int startZ = mapBoundary(tile.startZ0, l0.height(), field.height());
        int endZ = mapBoundary(tile.endZ0, l0.height(), field.height());
        endX = Math.max(startX + 1, Math.min(field.width(), endX));
        endZ = Math.max(startZ + 1, Math.min(field.height(), endZ));
        startX = Math.max(0, Math.min(startX, endX - 1));
        startZ = Math.max(0, Math.min(startZ, endZ - 1));

        int cellsX = endX - startX;
        int cellsZ = endZ - startZ;
        double scaleX = baseWorldWidth / field.width();
        double scaleZ = baseWorldDepth / field.height();
        double translateX = startX * scaleX;
        double translateZ = startZ * scaleZ;

        HyperSurfacePlotMesh view = new HyperSurfacePlotMesh(
            1, 1, 1, 1, yScale, 1.0, p -> 0.0);
        view.setDrawMode(drawMode);
        view.setCullFace(cullFace);
        view.updateMeshHeightField(field, startX, startZ,
            cellsX, cellsZ, scaleX, yScale, scaleZ);
        view.setTranslateX(translateX);
        view.setTranslateZ(translateZ);
        view.setVisible(false);
        applyColoration(view);
        applySpecular(view);
        getChildren().add(view);
        return view;
    }

    private void applyColoration(HyperSurfacePlotMesh view) {
        if (view == null || !view.isDirectHeightFieldMesh()) return;
        if (colorMode == ColorMode.IMAGE) {
            if (image != null) view.setDirectTextureModeImage(image);
        } else {
            view.setDirectTextureModeByHeight(paletteColors, colorMin, colorMax);
        }
    }

    private void applySpecular(HyperSurfacePlotMesh view) {
        if (view == null) return;
        if (view.getMaterial() instanceof PhongMaterial material) {
            material.setSpecularColor(specularColor);
        }
    }

    private void forEachBuiltView(java.util.function.Consumer<HyperSurfacePlotMesh> consumer) {
        for (TileRenderState tile : tilesById.values()) {
            for (int lod = 0; lod < tile.getLodCount(); lod++) {
                HyperSurfacePlotMesh view = tile.getLodView(lod);
                if (view != null) consumer.accept(view);
            }
        }
    }

    private static int mapBoundary(int sourceBoundary, int sourceSize, int targetSize) {
        if (sourceBoundary <= 0) return 0;
        if (sourceBoundary >= sourceSize) return targetSize;
        return (int) Math.round(sourceBoundary * targetSize / (double) sourceSize);
    }
}
