package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

import edu.jhuapl.trinity.data.messages.xai.ShapleyVector;
import edu.jhuapl.trinity.javafx.javafx3d.Vert3D;
import edu.jhuapl.trinity.javafx.javafx3d.animated.TessellationTube;
import edu.jhuapl.trinity.utils.Utils;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import org.fxyz3d.geometry.Point3D;

import java.util.List;
import java.util.function.Function;

/**
 * Owns Hypersurface surface-renderer construction, renderer selection, appearance,
 * and LOD activation/orchestration.
 *
 * <p>The controller deliberately does not process source data, interpret user
 * interaction, manage overlays, or own application controls. It consumes processed
 * HeightFields from {@link HypersurfaceSourceModel} and coordinates the legacy/global
 * and tiled rendering paths.</p>
 *
 * @author Sean Phillips
 */
public final class HypersurfaceRenderController {

    private static final int DEFAULT_TILE_CELLS_L0 = 256;
    private static final int TOTAL_COLORS = 1530;

    private final Hypersurface3DPane owner;
    private final HypersurfaceSourceModel sourceModel;
    private final HypersurfaceProcessingController processingController;
    private final SurfaceCrosshairOverlay surfaceCrosshairOverlay;
    private final Runnable coordinateMapperRefresh;

    private final Function<Vert3D, Number> heightLookup;
    private final Function<Point3D, Number> colorByHeight;
    private final Function<Point3D, Number> colorByShapley = p -> p.f;

    private HyperSurfacePlotMesh surfaceMesh;
    private LodManager lodManager;
    private TiledSurfaceRenderer tiledSurfaceRenderer;
    private boolean tiledHeightFieldRenderingEnabled = true;
    private int tileCellsL0 = DEFAULT_TILE_CELLS_L0;

    public HypersurfaceRenderController(Hypersurface3DPane owner,
                                        HypersurfaceSourceModel sourceModel,
                                        HypersurfaceProcessingController processingController,
                                        SurfaceCrosshairOverlay surfaceCrosshairOverlay,
                                        Runnable coordinateMapperRefresh) {
        if (owner == null) throw new IllegalArgumentException("owner cannot be null");
        if (sourceModel == null) throw new IllegalArgumentException("sourceModel cannot be null");
        if (processingController == null) {
            throw new IllegalArgumentException("processingController cannot be null");
        }
        if (surfaceCrosshairOverlay == null) {
            throw new IllegalArgumentException("surfaceCrosshairOverlay cannot be null");
        }
        if (coordinateMapperRefresh == null) {
            throw new IllegalArgumentException("coordinateMapperRefresh cannot be null");
        }
        this.owner = owner;
        this.sourceModel = sourceModel;
        this.processingController = processingController;
        this.surfaceCrosshairOverlay = surfaceCrosshairOverlay;
        this.coordinateMapperRefresh = coordinateMapperRefresh;
        this.heightLookup = p -> processingController.sampleHeight(
            p, owner.surfaceRender, owner.surfScale, owner.yScale);
        this.colorByHeight = p -> sourceModel.getHeightOrientation().toLogicalHeight(p.y);
    }

    /**
     * Create the legacy/global surface mesh and both LOD render paths. This method is
     * idempotent so renderer ownership remains centralized here.
     */
    public HyperSurfacePlotMesh initializeRenderers() {
        if (surfaceMesh == null) {
            surfaceMesh = new HyperSurfacePlotMesh(
                owner.xWidth, owner.zWidth, 1, 1, owner.yScale, owner.surfScale, heightLookup);
            surfaceMesh.setRowOrientation(sourceModel.getRowOrientation());
            surfaceMesh.setHeightOrientation(sourceModel.getHeightOrientation());
            surfaceMesh.setTextureModeVertices3D(
                TOTAL_COLORS, colorByHeight, 0.0, 360.0);
            surfaceMesh.setDrawMode(DrawMode.FILL);
            surfaceMesh.setCullFace(CullFace.BACK);
            surfaceMesh.setTranslateX(-(owner.xWidth * owner.surfScale) / 2.0);
            surfaceMesh.setTranslateZ(-(owner.zWidth * owner.surfScale) / 2.0);
            owner.sceneRoot.getChildren().add(surfaceMesh);
        }

        if (lodManager == null) {
            lodManager = new LodManager(owner.camera, owner.subScene, surfaceMesh);
            LodManager.LodConfig cfg = new LodManager.LodConfig();
            cfg.targetPixelsPerCell = 1.5;
            cfg.lowThreshold = 1.0;
            cfg.highThreshold = 2.0;
            cfg.throttleMs = 75;
            cfg.debounceMs = 75;
            lodManager.setConfig(cfg);
            lodManager.setOnLodSelected(idx -> {
                if (!shouldUseTiledHeightFieldRenderer()) applyActiveLodIndex(idx);
            });
        }

        if (tiledSurfaceRenderer == null) {
            tiledSurfaceRenderer = new TiledSurfaceRenderer(owner.camera, owner.subScene);
            tiledSurfaceRenderer.setRowOrientation(sourceModel.getRowOrientation());
            tiledSurfaceRenderer.setHeightOrientation(sourceModel.getHeightOrientation());
            tiledSurfaceRenderer.setTileCellsL0(tileCellsL0);
            tiledSurfaceRenderer.setVisible(false);
            owner.sceneRoot.getChildren().add(tiledSurfaceRenderer);
        }
        return surfaceMesh;
    }

    public HyperSurfacePlotMesh getSurfaceMesh() {
        return surfaceMesh;
    }

    public TiledSurfaceRenderer getTiledSurfaceRenderer() {
        return tiledSurfaceRenderer;
    }

    public double getSurfaceMaxAbsY() {
        return surfaceMesh != null ? Math.abs(surfaceMesh.getMaxAbsY()) : 0.0;
    }

    public void updateMesh() {
        if (surfaceMesh == null) return;

        surfaceCrosshairOverlay.hide();
        owner.sceneRoot.getChildren().removeIf(n -> n instanceof TessellationTube);

        final boolean useTiledRenderer = owner.surfaceRender && shouldUseTiledHeightFieldRenderer();
        if (tiledSurfaceRenderer != null) {
            tiledSurfaceRenderer.setVisible(useTiledRenderer);
        }
        if (useTiledRenderer) {
            surfaceMesh.setVisible(false);
            configureTiledAppearance();
            tiledSurfaceRenderer.forceUpdate();
            return;
        }

        // An image-backed surface with no active HeightField is between source
        // replacement and LOD-pyramid activation. Never build a raw full-resolution
        // legacy mesh in that transient state.
        if (sourceModel.isImageBackedSurface() && sourceModel.getActiveHeightField() == null) {
            surfaceMesh.setVisible(false);
            return;
        }

        surfaceMesh.setVisible(owner.surfaceRender);

        final int renderWidth = sourceModel.getRenderWidth(owner.xWidth);
        final int renderHeight = sourceModel.getRenderHeight(owner.zWidth);
        final double renderScaleX = sourceModel.getRenderScaleX(owner.surfScale);
        final double renderScaleZ = sourceModel.getRenderScaleZ(owner.surfScale);

        if (owner.surfaceRender) {
            surfaceMesh.setRowOrientation(sourceModel.getRowOrientation());
            surfaceMesh.setHeightOrientation(sourceModel.getHeightOrientation());
            boolean useDirectHeightFieldMesh = sourceModel.getActiveHeightField() != null
                && owner.colorationMethod != Hypersurface3DPane.COLORATION.COLOR_BY_SHAPLEY;

            long meshStart = System.nanoTime();
            if (useDirectHeightFieldMesh) {
                surfaceMesh.updateMeshHeightField(
                    sourceModel.getActiveHeightField(), renderScaleX, owner.yScale, renderScaleZ);
                System.out.println("Direct HeightField mesh built: "
                    + renderWidth + "x" + renderHeight + " in "
                    + Utils.totalTimeString(meshStart));
            } else {
                surfaceMesh.updateMeshRaw(
                    renderWidth, renderHeight, renderScaleX, owner.yScale, renderScaleZ);
            }
            applyCurrentColoration();
        } else {
            List<List<Double>> renderGrid = sourceModel.getActiveHeightField() != null
                ? SurfaceUtils.toGrid(sourceModel.getActiveHeightField())
                : owner.dataGrid;
            TessellationTube tube = new TessellationTube(
                renderGrid, Color.WHITE, owner.yScale * 10, renderScaleZ, owner.yScale);
            tube.setMouseTransparent(true);
            if (sourceModel.getSourceImage() != null) {
                tube.meshView.setDrawMode(DrawMode.FILL);
                tube.colorByImage = owner.colorationMethod
                    == Hypersurface3DPane.COLORATION.COLOR_BY_IMAGE;
                tube.updateMaterial(sourceModel.getSourceImage());
            }
            Platform.runLater(() -> owner.sceneRoot.getChildren().add(tube));
        }
    }

    /** Apply only coloration/material state without rebuilding surface geometry. */
    public void applyCurrentColoration() {
        if (surfaceMesh == null || owner.colorationMethod == null) return;
        if (shouldUseTiledHeightFieldRenderer()) {
            configureTiledAppearance();
            return;
        }

        switch (owner.colorationMethod) {
            case COLOR_BY_IMAGE -> {
                if (sourceModel.getSourceImage() == null) return;
                if (surfaceMesh.isDirectHeightFieldMesh()) {
                    surfaceMesh.setDirectTextureModeImage(sourceModel.getSourceImage());
                } else {
                    PhongMaterial material;
                    if (surfaceMesh.getMaterial() instanceof PhongMaterial existing) {
                        material = existing;
                    } else {
                        material = new PhongMaterial(Color.WHITE);
                    }
                    material.setDiffuseColor(Color.WHITE);
                    material.setDiffuseMap(sourceModel.getSourceImage());
                    surfaceMesh.setMaterial(material);
                }
            }
            case COLOR_BY_FEATURE -> {
                if (surfaceMesh.isDirectHeightFieldMesh()) {
                    HeightField colorRangeField = !sourceModel.getLodProcessedLevels().isEmpty()
                        ? sourceModel.getLodProcessedLevels().get(0)
                        : sourceModel.getActiveHeightField();
                    if (colorRangeField != null) {
                        float[] minMax = colorRangeField.minMax();
                        double minColorValue = minMax[0] * owner.yScale;
                        double maxColorValue = minMax[1] * owner.yScale;
                        if (!(maxColorValue > minColorValue)) {
                            double pad = Math.max(1.0e-9, Math.abs(minColorValue) * 1.0e-9);
                            minColorValue -= pad;
                            maxColorValue += pad;
                        }
                        surfaceMesh.setDirectTextureModeByHeight(
                            TOTAL_COLORS, minColorValue, maxColorValue);
                    }
                } else {
                    surfaceMesh.setTextureModeVertices3D(
                        TOTAL_COLORS, colorByHeight, 0.0, 360.0);
                }
            }
            case COLOR_BY_SHAPLEY -> surfaceMesh.setTextureModeVertices3D(
                TOTAL_COLORS, colorByShapley, 0.0, 360.0);
        }
    }

    /**
     * Apply a coloration-mode transition, including the legacy Shapley geometry path.
     */
    public void handleColorationChanged(Hypersurface3DPane.COLORATION previous,
                                        Hypersurface3DPane.COLORATION next) {
        boolean shapleyPathChanged = (previous == Hypersurface3DPane.COLORATION.COLOR_BY_SHAPLEY)
            != (next == Hypersurface3DPane.COLORATION.COLOR_BY_SHAPLEY);
        if (!shapleyPathChanged) {
            applyCurrentColoration();
            return;
        }

        if (next == Hypersurface3DPane.COLORATION.COLOR_BY_SHAPLEY
            && lodManager != null
            && sourceModel.getLodProcessedLevels() != null
            && !sourceModel.getLodProcessedLevels().isEmpty()) {
            sourceModel.setActiveLod(-1, null);
            lodManager.setPyramid(
                LodManager.fromHeightFields(sourceModel.getLodProcessedLevels()),
                sourceModel.getBaseWorldWidth(), sourceModel.getBaseWorldDepth(), -1);
            lodManager.forceUpdate();
        } else if (shouldUseTiledHeightFieldRenderer()) {
            configureTiledPyramid();
        }
        updateMesh();
    }

    public void activateProcessedPyramid() {
        if (shouldUseTiledHeightFieldRenderer()) {
            configureTiledPyramid();
            updateMesh();
        } else if (lodManager != null) {
            lodManager.setPyramid(
                LodManager.fromHeightFields(sourceModel.getLodProcessedLevels()),
                sourceModel.getBaseWorldWidth(), sourceModel.getBaseWorldDepth(), -1);
            lodManager.forceUpdate();
        } else {
            applyActiveLodIndex(0);
        }
    }

    public void refreshWorldExtentsAndLodMetadata() {
        sourceModel.recomputeBaseWorldExtents(owner.xWidth, owner.zWidth, owner.surfScale);
        coordinateMapperRefresh.run();

        HeightField activeField = sourceModel.getActiveHeightField();
        if (activeField != null) {
            sourceModel.setCurrentLodSurfScale(
                sourceModel.getBaseWorldWidth() / activeField.width(),
                sourceModel.getBaseWorldDepth() / activeField.height());
        }
        centerLegacySurfaceOnWorldExtents();

        if (shouldUseTiledHeightFieldRenderer()) {
            configureTiledPyramid();
            tiledSurfaceRenderer.forceUpdate();
        } else if (lodManager != null && !sourceModel.getLodProcessedLevels().isEmpty()) {
            lodManager.setPyramid(
                LodManager.fromHeightFields(sourceModel.getLodProcessedLevels()),
                sourceModel.getBaseWorldWidth(),
                sourceModel.getBaseWorldDepth(),
                sourceModel.getActiveLodIndex());
            lodManager.forceUpdate();
        }
    }

    public void updateRawSurfaceTranslation() {
        if (surfaceMesh == null || sourceModel.getActiveHeightField() != null) return;
        surfaceMesh.setTranslateX(-(owner.xWidth * owner.surfScale) / 2.0);
        surfaceMesh.setTranslateZ(-(owner.zWidth * owner.surfScale) / 2.0);
    }

    public void centerLegacySurfaceOnWorldExtents() {
        if (surfaceMesh == null) return;
        surfaceMesh.setTranslateX(-sourceModel.getBaseWorldWidth() / 2.0);
        surfaceMesh.setTranslateZ(-sourceModel.getBaseWorldDepth() / 2.0);
    }

    public void setFunctionScale(double yScale) {
        if (surfaceMesh != null) surfaceMesh.setFunctionScale(yScale);
    }

    public void scaleHeight(double factor) {
        if (surfaceMesh != null) surfaceMesh.scaleHeight((float) factor);
    }

    public void setDrawMode(DrawMode drawMode) {
        if (surfaceMesh != null) surfaceMesh.setDrawMode(drawMode);
        if (tiledSurfaceRenderer != null) tiledSurfaceRenderer.setDrawMode(drawMode);
    }

    public void setCullFace(CullFace cullFace) {
        if (surfaceMesh != null) surfaceMesh.setCullFace(cullFace);
        if (tiledSurfaceRenderer != null) tiledSurfaceRenderer.setCullFace(cullFace);
    }

    public void setSpecularColor(Color color) {
        if (surfaceMesh != null && surfaceMesh.getMaterial() instanceof PhongMaterial material) {
            material.setSpecularColor(color);
        }
        if (tiledSurfaceRenderer != null) tiledSurfaceRenderer.setSpecularColor(color);
    }

    public void setRowOrientation(SurfaceRowOrientation orientation) {
        if (surfaceMesh != null) surfaceMesh.setRowOrientation(orientation);
        if (tiledSurfaceRenderer != null) tiledSurfaceRenderer.setRowOrientation(orientation);
    }

    public void setHeightOrientation(SurfaceHeightOrientation orientation) {
        if (surfaceMesh != null) surfaceMesh.setHeightOrientation(orientation);
        if (tiledSurfaceRenderer != null) tiledSurfaceRenderer.setHeightOrientation(orientation);
    }

    public void updateShapleyFunctionValues(List<ShapleyVector> shapleyVectors, double yScale) {
        if (surfaceMesh == null) return;
        surfaceMesh.functionValues.clear();
        for (ShapleyVector vector : shapleyVectors) {
            surfaceMesh.functionValues.add(vector.getData().get(0) * yScale);
        }
    }

    public void releaseImageBackedRenderStateForReplacement() {
        if (tiledSurfaceRenderer != null) {
            tiledSurfaceRenderer.clearSurface();
            tiledSurfaceRenderer.setVisible(false);
        }
        if (surfaceMesh != null) surfaceMesh.setVisible(false);
        surfaceCrosshairOverlay.hide();
    }

    public void resetLodRenderState() {
        if (tiledSurfaceRenderer != null) {
            tiledSurfaceRenderer.clearSurface();
            tiledSurfaceRenderer.setVisible(false);
        }
    }

    public boolean isTiledHeightFieldRenderingEnabled() {
        return tiledHeightFieldRenderingEnabled;
    }

    public void setTiledHeightFieldRenderingEnabled(boolean enabled) {
        if (tiledHeightFieldRenderingEnabled == enabled) return;
        tiledHeightFieldRenderingEnabled = enabled;
        if (sourceModel.getLodProcessedLevels() == null
            || sourceModel.getLodProcessedLevels().isEmpty()) {
            return;
        }

        if (enabled && shouldUseTiledHeightFieldRenderer()) {
            configureTiledPyramid();
        } else if (lodManager != null) {
            sourceModel.setActiveLod(-1, null);
            lodManager.setPyramid(
                LodManager.fromHeightFields(sourceModel.getLodProcessedLevels()),
                sourceModel.getBaseWorldWidth(), sourceModel.getBaseWorldDepth(), -1);
            lodManager.forceUpdate();
        }
        updateMesh();
    }

    public int getTileCellsL0() {
        return tileCellsL0;
    }

    public void setTileCellsL0(int tileCellsL0) {
        if (tileCellsL0 < 16) {
            throw new IllegalArgumentException("tileCellsL0 must be >= 16");
        }
        if (this.tileCellsL0 == tileCellsL0) return;
        this.tileCellsL0 = tileCellsL0;
        if (shouldUseTiledHeightFieldRenderer()) {
            configureTiledPyramid();
            updateMesh();
        }
    }

    public TiledLodManager.Config getTiledLodConfigCopy() {
        return tiledSurfaceRenderer != null
            ? tiledSurfaceRenderer.getLodConfigCopy()
            : new TiledLodManager.Config();
    }

    public void setTiledLodConfig(TiledLodManager.Config config) {
        if (tiledSurfaceRenderer == null || config == null) return;
        tiledSurfaceRenderer.setLodConfig(config);
    }

    public TiledSurfaceRenderer.LodStatistics getTiledLodStatistics() {
        return tiledSurfaceRenderer != null
            ? tiledSurfaceRenderer.getLodStatistics()
            : new TiledSurfaceRenderer.LodStatistics(
                0, 0, 0, 0, 0, 0, 0, 0L, 0L, 0L, 0, 0);
    }

    public boolean shouldUseTiledHeightFieldRenderer() {
        return tiledHeightFieldRenderingEnabled
            && sourceModel.isImageBackedSurface()
            && tiledSurfaceRenderer != null
            && sourceModel.getLodProcessedLevels() != null
            && !sourceModel.getLodProcessedLevels().isEmpty()
            && owner.colorationMethod != Hypersurface3DPane.COLORATION.COLOR_BY_SHAPLEY;
    }

    public boolean isTiledHeightFieldRendererActive() {
        return shouldUseTiledHeightFieldRenderer()
            && owner.surfaceRender
            && tiledSurfaceRenderer.isVisible();
    }

    public void requestLodUpdate(LodManager.UpdateReason reason) {
        if (shouldUseTiledHeightFieldRenderer()) {
            tiledSurfaceRenderer.requestUpdate(reason);
        } else if (lodManager != null) {
            lodManager.requestUpdate(reason);
        }
    }

    public void forceLodUpdate() {
        if (shouldUseTiledHeightFieldRenderer()) {
            tiledSurfaceRenderer.forceUpdate();
        } else if (lodManager != null) {
            lodManager.forceUpdate();
        }
    }

    private void configureTiledAppearance() {
        if (tiledSurfaceRenderer == null) return;
        if (surfaceMesh != null) {
            tiledSurfaceRenderer.setDrawMode(surfaceMesh.getDrawMode());
            tiledSurfaceRenderer.setCullFace(surfaceMesh.getCullFace());
            if (surfaceMesh.getMaterial() instanceof PhongMaterial material) {
                tiledSurfaceRenderer.setSpecularColor(material.getSpecularColor());
            }
        }
        tiledSurfaceRenderer.setYScale(owner.yScale);
        tiledSurfaceRenderer.setRowOrientation(sourceModel.getRowOrientation());
        tiledSurfaceRenderer.setHeightOrientation(sourceModel.getHeightOrientation());

        if (owner.colorationMethod == Hypersurface3DPane.COLORATION.COLOR_BY_IMAGE
            && sourceModel.getSourceImage() != null) {
            tiledSurfaceRenderer.setColorByImage(sourceModel.getSourceImage());
        } else if (owner.colorationMethod == Hypersurface3DPane.COLORATION.COLOR_BY_FEATURE
            && !sourceModel.getLodProcessedLevels().isEmpty()) {
            float[] minMax = sourceModel.getLodProcessedLevels().get(0).minMax();
            double minColorValue = minMax[0] * owner.yScale;
            double maxColorValue = minMax[1] * owner.yScale;
            if (!(maxColorValue > minColorValue)) {
                double pad = Math.max(1.0e-9, Math.abs(minColorValue) * 1.0e-9);
                minColorValue -= pad;
                maxColorValue += pad;
            }
            tiledSurfaceRenderer.setColorByHeight(
                TOTAL_COLORS, minColorValue, maxColorValue);
        }
    }

    private void configureTiledPyramid() {
        if (!shouldUseTiledHeightFieldRenderer()) return;
        sourceModel.setActiveLod(0, sourceModel.getLodProcessedLevels().get(0));
        owner.xWidth = sourceModel.getActiveHeightField().width();
        owner.zWidth = sourceModel.getActiveHeightField().height();
        sourceModel.setCurrentLodSurfScale(
            sourceModel.getBaseWorldWidth() / owner.xWidth,
            sourceModel.getBaseWorldDepth() / owner.zWidth);
        coordinateMapperRefresh.run();

        tiledSurfaceRenderer.setTileCellsL0(tileCellsL0);
        tiledSurfaceRenderer.setPyramid(
            sourceModel.getLodProcessedLevels(),
            sourceModel.getBaseWorldWidth(),
            sourceModel.getBaseWorldDepth(),
            owner.yScale);
        configureTiledAppearance();
        tiledSurfaceRenderer.forceUpdate();
        owner.syncGuiControls();
    }

    /** Apply the selected global LOD level from the processed pyramid. */
    private void applyActiveLodIndex(int idx) {
        if (sourceModel.getLodProcessedLevels() == null
            || sourceModel.getLodProcessedLevels().isEmpty()) return;
        if (idx < 0) idx = 0;
        if (idx >= sourceModel.getLodProcessedLevels().size()) {
            idx = sourceModel.getLodProcessedLevels().size() - 1;
        }

        HeightField selected = sourceModel.getLodProcessedLevels().get(idx);
        if (idx == sourceModel.getActiveLodIndex()
            && sourceModel.getActiveHeightField() == selected) return;

        sourceModel.setActiveLod(idx, selected);
        owner.xWidth = selected.width();
        owner.zWidth = selected.height();
        sourceModel.setCurrentLodSurfScale(
            sourceModel.getBaseWorldWidth() / owner.xWidth,
            sourceModel.getBaseWorldDepth() / owner.zWidth);
        coordinateMapperRefresh.run();

        System.out.println("Applying Hypersurface LOD L" + sourceModel.getActiveLodIndex()
            + ": " + owner.xWidth + "x" + owner.zWidth
            + ", cellScale=" + sourceModel.getCurrentLodSurfScaleX()
            + "x" + sourceModel.getCurrentLodSurfScaleZ());

        centerLegacySurfaceOnWorldExtents();
        owner.syncGuiControls();
        updateMesh();
    }
}
