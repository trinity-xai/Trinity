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
 * <p>Tiles are defined in the finest rendered level (L0) and keep constant world
 * footprints. Each visible tile independently selects a pyramid level through
 * {@link TiledLodManager}. Meshes are created lazily and cached while culled.</p>
 */
public final class TiledSurfaceRenderer extends Group {

    public enum ColorMode {
        IMAGE,
        HEIGHT
    }

    private static final int DEFAULT_TILE_CELLS_L0 = 256;

    private final PerspectiveCamera camera;
    private final SubScene subScene;
    private final TiledLodManager lodManager;
    private final Map<Integer, SurfaceTile> tilesById = new HashMap<>();

    private List<HeightField> levels = List.of();
    private List<TiledLodManager.TileSpec> tileSpecs = List.of();
    private double baseWorldWidth = 1.0;
    private double baseWorldDepth = 1.0;
    private double yScale = 1.0;
    private int tileCellsL0 = DEFAULT_TILE_CELLS_L0;

    private DrawMode drawMode = DrawMode.FILL;
    private CullFace cullFace = CullFace.NONE;
    private ColorMode colorMode = ColorMode.HEIGHT;
    private Image image;
    private int paletteColors = 1530;
    private double colorMin = 0.0;
    private double colorMax = 1.0;
    private Color specularColor = Color.WHITE;

    private int visibleTileCount;
    private int lastRebuildCount;

    public TiledSurfaceRenderer(PerspectiveCamera camera, SubScene subScene) {
        this.camera = Objects.requireNonNull(camera, "camera");
        this.subScene = Objects.requireNonNull(subScene, "subScene");
        this.lodManager = new TiledLodManager(camera, subScene, this);
        this.lodManager.setOnDecisions(this::applyDecisions);
        setPickOnBounds(false);
    }

    public void setTileCellsL0(int tileCellsL0) {
        if (tileCellsL0 < 16) throw new IllegalArgumentException("tileCellsL0 must be >= 16");
        if (this.tileCellsL0 == tileCellsL0) return;
        this.tileCellsL0 = tileCellsL0;
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
        if (levels.isEmpty()) throw new IllegalArgumentException("levels must not be empty");
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
        tilesById.clear();
        tileSpecs = List.of();
        levels = List.of();
        getChildren().clear();
    }

    public void requestUpdate(LodManager.UpdateReason reason) {
        if (!levels.isEmpty()) lodManager.requestUpdate(reason);
    }

    public void forceUpdate() {
        if (!levels.isEmpty()) lodManager.forceUpdate();
    }

    public void dispose() {
        lodManager.dispose();
        clearSurface();
    }

    public void setYScale(double yScale) {
        if (Double.compare(this.yScale, yScale) == 0) return;
        this.yScale = yScale;
        for (SurfaceTile tile : tilesById.values()) tile.currentLod = -1;
        forceUpdate();
    }

    public void setDrawMode(DrawMode drawMode) {
        this.drawMode = Objects.requireNonNull(drawMode, "drawMode");
        for (SurfaceTile tile : tilesById.values()) {
            if (tile.mesh != null) tile.mesh.setDrawMode(drawMode);
        }
    }

    public void setCullFace(CullFace cullFace) {
        this.cullFace = Objects.requireNonNull(cullFace, "cullFace");
        for (SurfaceTile tile : tilesById.values()) {
            if (tile.mesh != null) tile.mesh.setCullFace(cullFace);
        }
    }

    public void setSpecularColor(Color specularColor) {
        this.specularColor = Objects.requireNonNull(specularColor, "specularColor");
        for (SurfaceTile tile : tilesById.values()) applySpecular(tile);
    }

    public void setColorByImage(Image image) {
        this.image = image;
        this.colorMode = ColorMode.IMAGE;
        for (SurfaceTile tile : tilesById.values()) applyColoration(tile);
    }

    public void setColorByHeight(int colors, double min, double max) {
        if (colors < 2) throw new IllegalArgumentException("colors must be >= 2");
        if (!(max > min)) throw new IllegalArgumentException("max must be > min");
        this.paletteColors = colors;
        this.colorMin = min;
        this.colorMax = max;
        this.colorMode = ColorMode.HEIGHT;
        for (SurfaceTile tile : tilesById.values()) applyColoration(tile);
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
                tilesById.put(id, new SurfaceTile(spec, startX, endX, startZ, endZ));
                id++;
            }
        }
        tileSpecs = List.copyOf(specs);

        float[] minMax = l0.minMax();
        double maxAbsHeight = Math.max(Math.abs(minMax[0] * yScale), Math.abs(minMax[1] * yScale));
        lodManager.setSurface(
            LodManager.fromHeightFields(levels),
            tileSpecs,
            baseWorldWidth,
            baseWorldDepth,
            maxAbsHeight
        );

        System.out.println("Tiled Hypersurface configured: tiles=" + columns + "x" + rows
            + " (" + tileSpecs.size() + "), tileCellsL0=" + tileCellsL0
            + ", world=" + baseWorldWidth + "x" + baseWorldDepth);
    }

    private void applyDecisions(List<TiledLodManager.TileDecision> decisions) {
        int visible = 0;
        int rebuilt = 0;
        int[] lodCounts = new int[levels.size()];

        for (TiledLodManager.TileDecision decision : decisions) {
            SurfaceTile tile = tilesById.get(decision.tileId());
            if (tile == null) continue;

            if (!decision.visible()) {
                if (tile.mesh != null) tile.mesh.setVisible(false);
                continue;
            }

            visible++;
            int lod = Math.max(0, Math.min(decision.lodIndex(), levels.size() - 1));
            lodCounts[lod]++;
            if (tile.mesh == null || tile.currentLod != lod) {
                rebuildTile(tile, lod);
                rebuilt++;
            }
            tile.mesh.setVisible(isVisible());
        }

        visibleTileCount = visible;
        lastRebuildCount = rebuilt;
        if (rebuilt > 0) {
            StringBuilder sb = new StringBuilder("Tiled Hypersurface LOD: visible=")
                .append(visible).append('/').append(tileSpecs.size())
                .append(", rebuilt=").append(rebuilt).append(", ");
            for (int i = 0; i < lodCounts.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append("L").append(i).append('=').append(lodCounts[i]);
            }
            System.out.println(sb);
        }
    }

    private void rebuildTile(SurfaceTile tile, int lod) {
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

        if (tile.mesh == null) {
            tile.mesh = new HyperSurfacePlotMesh(1, 1, 1, 1,
                yScale, 1.0, p -> 0.0);
            tile.mesh.setDrawMode(drawMode);
            tile.mesh.setCullFace(cullFace);
            getChildren().add(tile.mesh);
        }

        tile.mesh.updateMeshHeightField(field, startX, startZ,
            cellsX, cellsZ, scaleX, yScale, scaleZ);
        tile.mesh.setTranslateX(startX * scaleX);
        tile.mesh.setTranslateZ(startZ * scaleZ);
        tile.currentLod = lod;
        applyColoration(tile);
        applySpecular(tile);
    }

    private void applyColoration(SurfaceTile tile) {
        if (tile.mesh == null || !tile.mesh.isDirectHeightFieldMesh()) return;
        if (colorMode == ColorMode.IMAGE) {
            if (image != null) tile.mesh.setDirectTextureModeImage(image);
        } else {
            tile.mesh.setDirectTextureModeByHeight(paletteColors, colorMin, colorMax);
        }
    }

    private void applySpecular(SurfaceTile tile) {
        if (tile.mesh == null) return;
        if (tile.mesh.getMaterial() instanceof PhongMaterial material) {
            material.setSpecularColor(specularColor);
        }
    }

    private static int mapBoundary(int sourceBoundary, int sourceSize, int targetSize) {
        if (sourceBoundary <= 0) return 0;
        if (sourceBoundary >= sourceSize) return targetSize;
        return (int) Math.round(sourceBoundary * targetSize / (double) sourceSize);
    }

    private static final class SurfaceTile {
        final TiledLodManager.TileSpec spec;
        final int startX0;
        final int endX0;
        final int startZ0;
        final int endZ0;
        HyperSurfacePlotMesh mesh;
        int currentLod = -1;

        SurfaceTile(TiledLodManager.TileSpec spec,
                    int startX0, int endX0,
                    int startZ0, int endZ0) {
            this.spec = spec;
            this.startX0 = startX0;
            this.endX0 = endX0;
            this.startZ0 = startZ0;
            this.endZ0 = endZ0;
        }
    }
}
