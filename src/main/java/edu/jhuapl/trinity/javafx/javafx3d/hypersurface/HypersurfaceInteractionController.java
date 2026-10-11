package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FactorAnalysisEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.javafx.events.HypersurfaceEvent;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.fxyz3d.geometry.Point3D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates transient interaction with a Hypersurface without owning surface
 * rendering or source-data processing.
 *
 * <p>The controller resolves picked surface positions into source/render indices,
 * dispatches hover-derived analysis events, coordinates crosshair requests, publishes
 * FeatureVector selections for Trinity's shared inspection tools, and interprets graph
 * hover/click events that map back onto surface rows.</p>
 *
 * @author Sean Phillips
 */
public final class HypersurfaceInteractionController {

    /**
     * Resolved semantic meaning of a picked surface position. Render row/column refer
     * to the currently active HeightField while source row/column remain stable across
     * LOD changes.
     */
    public record SurfaceInteractionContext(
        Point3D surfacePoint,
        int renderRow,
        int renderColumn,
        int sourceRow,
        int sourceColumn,
        double value
    ) {
    }

    private final Hypersurface3DPane pane;
    private final Scene scene;
    private final SurfaceCoordinateMapper coordinateMapper;
    private final SurfaceCrosshairOverlay surfaceCrosshairOverlay;
    private final HypersurfaceOverlayManager overlayManager;

    private boolean hoverEnabled;
    private boolean surfaceChartsEnabled;
    private boolean crosshairsEnabled;

    private FeatureVector lastPublishedFeatureVector;
    private SurfaceInteractionContext lastInteractionContext;
    private SurfaceInspection lastSurfaceInspection;

    private int lastStatsRenderRow = -1;
    private int lastStatsRenderColumn = -1;
    private double lastRowMinimum = Double.NaN;
    private double lastRowMaximum = Double.NaN;
    private double lastColumnMinimum = Double.NaN;
    private double lastColumnMaximum = Double.NaN;

    private GraphDirectedCollection currentGraph;
    private final IdentityHashMap<GraphNode, Integer> graphNodeRowIndex = new IdentityHashMap<>();
    private final HashMap<String, Integer> graphEntityIdToIndex = new HashMap<>();
    private int[][] graphAdjacencyIndices = new int[0][];

    public HypersurfaceInteractionController(Hypersurface3DPane pane,
                                             Scene scene,
                                             SurfaceCoordinateMapper coordinateMapper,
                                             SurfaceCrosshairOverlay surfaceCrosshairOverlay,
                                             HypersurfaceOverlayManager overlayManager) {
        this.pane = pane;
        this.scene = scene;
        this.coordinateMapper = coordinateMapper;
        this.surfaceCrosshairOverlay = surfaceCrosshairOverlay;
        this.overlayManager = overlayManager;
        wireGraphInteractionHandlers();
    }

    /**
     * Installs the common hover-picking path on both renderers. The legacy mesh reports
     * points directly in surface coordinates; tiled MeshViews are first converted back
     * through the tiled renderer's local coordinate system.
     */
    public void installSurfacePicking(HyperSurfacePlotMesh surfPlot,
                                      TiledSurfaceRenderer tiledSurfaceRenderer) {
        if (tiledSurfaceRenderer != null) {
            tiledSurfaceRenderer.setHoverPickingEnabled(hoverEnabled);
            tiledSurfaceRenderer.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
                if (!hoverEnabled) return;
                Node picked = e.getPickResult().getIntersectedNode();
                javafx.geometry.Point3D pickedPoint = e.getPickResult().getIntersectedPoint();
                if (picked == null || pickedPoint == null) return;
                javafx.geometry.Point3D scenePoint = picked.localToScene(pickedPoint);
                javafx.geometry.Point3D surfacePoint = tiledSurfaceRenderer.sceneToLocal(scenePoint);
                handleSurfaceHover(Point3D.convertFromJavaFXPoint3D(surfacePoint));
                e.consume();
            });
        }

        if (surfPlot != null) {
            surfPlot.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
                if (!hoverEnabled) return;
                javafx.geometry.Point3D pickedPoint = e.getPickResult().getIntersectedPoint();
                if (pickedPoint == null) return;
                handleSurfaceHover(Point3D.convertFromJavaFXPoint3D(pickedPoint));
                e.consume();
            });
        }
    }

    public boolean isHoverEnabled() {
        return hoverEnabled;
    }

    public void setHoverEnabled(boolean hoverEnabled) {
        this.hoverEnabled = hoverEnabled;
        TiledSurfaceRenderer tiledSurfaceRenderer = pane.getTiledSurfaceRendererForInteraction();
        if (tiledSurfaceRenderer != null) {
            tiledSurfaceRenderer.setHoverPickingEnabled(hoverEnabled);
        }
    }

    public boolean isSurfaceChartsEnabled() {
        return surfaceChartsEnabled;
    }

    public void setSurfaceChartsEnabled(boolean surfaceChartsEnabled) {
        this.surfaceChartsEnabled = surfaceChartsEnabled;
        if (surfaceChartsEnabled) pane.showSurfaceChartsPane();
    }

    public boolean isCrosshairsEnabled() {
        return crosshairsEnabled;
    }

    public void setCrosshairsEnabled(boolean crosshairsEnabled) {
        this.crosshairsEnabled = crosshairsEnabled;
        if (!crosshairsEnabled) surfaceCrosshairOverlay.hide();
    }

    /**
     * Opens Trinity's existing Content Navigator. Image-backed surfaces can provide
     * their source image directly. Feature-backed surfaces republish the last hovered
     * FeatureVector after the navigator has been shown so its existing event listener
     * receives the current selection immediately.
     */
    public void showContentNavigator(Image sourceImage) {
        if (sourceImage != null) {
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_NAVIGATOR_PANE, sourceImage));
        } else {
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_NAVIGATOR_PANE));
        }

        if (lastPublishedFeatureVector != null) {
            publishFeatureVector(lastPublishedFeatureVector, true);
        }
        if (lastSurfaceInspection != null) {
            publishSurfaceInspection(lastSurfaceInspection);
        }
    }

    /** Publishes an accepted Hypersurface source image without forcing Navigator open. */
    public void publishSourceImageChanged(Image sourceImage) {
        fireHypersurfaceEvent(HypersurfaceEvent.sourceImageChanged(sourceImage));
    }

    public SurfaceInteractionContext getLastInteractionContext() {
        return lastInteractionContext;
    }

    public void resetSelectionState() {
        lastPublishedFeatureVector = null;
        lastInteractionContext = null;
        lastSurfaceInspection = null;
        lastStatsRenderRow = -1;
        lastStatsRenderColumn = -1;
        lastRowMinimum = Double.NaN;
        lastRowMaximum = Double.NaN;
        lastColumnMinimum = Double.NaN;
        lastColumnMaximum = Double.NaN;
        surfaceCrosshairOverlay.hide();
        fireHypersurfaceEvent(HypersurfaceEvent.surfaceInspection(null));
    }

    /**
     * Rebuilds the graph-to-surface interaction cache when graph content changes.
     * Rendering/styling of the graph remains owned by Hypersurface3DPane.
     */
    public void setGraph(GraphDirectedCollection graph) {
        currentGraph = graph;
        graphNodeRowIndex.clear();
        graphEntityIdToIndex.clear();
        graphAdjacencyIndices = new int[0][];
        if (graph == null || graph.getNodes() == null) return;

        List<GraphNode> nodes = graph.getNodes();
        int n = nodes.size();
        int sourceHeight = pane.getSourceHeight();

        HashMap<Long, GraphNode> validIdentities = new HashMap<>();
        boolean uniqueIdentityMapping = sourceHeight > 0;
        for (int i = 0; i < n; i++) {
            GraphNode node = nodes.get(i);
            if (node == null) {
                uniqueIdentityMapping = false;
                continue;
            }
            graphEntityIdToIndex.put(node.getEntityID(), i);
            long identity = node.getIdentity();
            if (identity < 0 || identity >= sourceHeight
                || validIdentities.put(identity, node) != null) {
                uniqueIdentityMapping = false;
            }
        }

        if (uniqueIdentityMapping && validIdentities.size() == n) {
            for (GraphNode node : nodes) {
                graphNodeRowIndex.put(node, (int) node.getIdentity());
            }
        } else if (n == sourceHeight) {
            for (int i = 0; i < n; i++) {
                GraphNode node = nodes.get(i);
                if (node != null) graphNodeRowIndex.put(node, i);
            }
        }

        ArrayList<ArrayList<Integer>> adjacency = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adjacency.add(new ArrayList<>());
        if (graph.getEdges() != null) {
            for (GraphEdge edge : graph.getEdges()) {
                if (edge == null) continue;
                Integer startIndex = graphEntityIdToIndex.get(edge.getStartID());
                Integer endIndex = graphEntityIdToIndex.get(edge.getEndID());
                if (startIndex == null || endIndex == null || startIndex.equals(endIndex)) continue;
                adjacency.get(startIndex).add(endIndex);
                adjacency.get(endIndex).add(startIndex);
            }
        }

        graphAdjacencyIndices = new int[n][];
        for (int i = 0; i < n; i++) {
            ArrayList<Integer> neighbors = adjacency.get(i);
            int[] indices = new int[neighbors.size()];
            for (int j = 0; j < neighbors.size(); j++) indices[j] = neighbors.get(j);
            graphAdjacencyIndices[i] = indices;
        }
    }

    private void handleSurfaceHover(Point3D surfacePoint) {
        // Keep the 3D marker responsive at raw mouse-event frequency. Rich inspection
        // state below is coalesced by source/render cell changes.
        overlayManager.updateHoverMarker(surfacePoint);

        int renderRow = coordinateMapper.surfaceZToRenderRow(surfacePoint.getZ());
        int renderColumn = coordinateMapper.surfaceXToRenderColumn(surfacePoint.getX());
        int sourceRow = coordinateMapper.surfaceZToSourceRow(surfacePoint.getZ());
        int sourceColumn = coordinateMapper.surfaceXToSourceColumn(surfacePoint.getX());
        HeightField activeHeightField = pane.getActiveHeightFieldForInteraction();
        double value = activeHeightField != null
            ? pane.getActiveRenderValue(renderRow, renderColumn)
            : Double.NaN;

        SurfaceInteractionContext previous = lastInteractionContext;
        SurfaceInteractionContext context = new SurfaceInteractionContext(
            surfacePoint, renderRow, renderColumn, sourceRow, sourceColumn, value);
        lastInteractionContext = context;

        publishFeatureSelection(sourceRow);

        if (crosshairsEnabled && activeHeightField != null) {
            surfaceCrosshairOverlay.requestRenderPosition(renderRow, renderColumn);
        }

        boolean renderCellChanged = previous == null
            || previous.renderRow() != renderRow
            || previous.renderColumn() != renderColumn;
        boolean sourcePixelChanged = previous == null
            || previous.sourceRow() != sourceRow
            || previous.sourceColumn() != sourceColumn;

        if (activeHeightField != null && (sourcePixelChanged || renderCellChanged
            || tileOrLodChanged(renderRow, renderColumn))) {
            publishSurfaceInspection(buildSurfaceInspection(context));
        }

        if (surfaceChartsEnabled && activeHeightField != null && renderCellChanged) {
            publishSurfaceChartData(context);
        }
    }

    private void publishFeatureSelection(int sourceRow) {
        List<FeatureVector> featureVectors = pane.getFeatureVectorsForInteraction();
        if (sourceRow < 0 || sourceRow >= featureVectors.size()) {
            lastPublishedFeatureVector = null;
            return;
        }

        FeatureVector featureVector = featureVectors.get(sourceRow);
        if (featureVector == null || featureVector == lastPublishedFeatureVector) return;
        lastPublishedFeatureVector = featureVector;
        publishFeatureVector(featureVector, false);
    }

    private void publishFeatureVector(FeatureVector featureVector, boolean force) {
        if (featureVector == null) return;
        if (!force && featureVector != lastPublishedFeatureVector) {
            lastPublishedFeatureVector = featureVector;
        }
        scene.getRoot().fireEvent(new FeatureVectorEvent(
            FeatureVectorEvent.SELECT_FEATURE_VECTOR,
            featureVector,
            pane.getFeatureLabelsForInteraction()));
    }

    private SurfaceInspection buildSurfaceInspection(SurfaceInteractionContext context) {
        refreshStatistics(context.renderRow(), context.renderColumn());

        boolean imageBacked = pane.isImageBackedSurface();
        int imageColumn = context.sourceColumn();
        int imageRow = context.sourceRow();
        int red = -1;
        int green = -1;
        int blue = -1;
        double intensity = Double.NaN;

        Image sourceImage = pane.getSourceImageForInteraction();
        if (imageBacked && sourceImage != null) {
            imageColumn += pane.getImageSourceStartXForInteraction();
            imageRow += pane.getImageSourceStartYForInteraction();
            PixelReader pixelReader = sourceImage.getPixelReader();
            if (pixelReader != null) {
                int imageWidth = (int) sourceImage.getWidth();
                int imageHeight = (int) sourceImage.getHeight();
                int px = Math.max(0, Math.min(imageColumn, Math.max(0, imageWidth - 1)));
                int py = Math.max(0, Math.min(imageRow, Math.max(0, imageHeight - 1)));
                int argb = pixelReader.getArgb(px, py);
                red = (argb >> 16) & 0xFF;
                green = (argb >> 8) & 0xFF;
                blue = argb & 0xFF;
                intensity = ((red + green + blue) / 3.0) / 255.0;
            }
        }

        int tileId = -1;
        int activeLod = -1;
        int tileImageMinX = -1;
        int tileImageMinY = -1;
        int tileImageMaxXExclusive = -1;
        int tileImageMaxYExclusive = -1;

        TiledSurfaceRenderer tiledRenderer = pane.getTiledSurfaceRendererForInteraction();
        if (imageBacked && pane.isTiledHeightFieldRendererActive() && tiledRenderer != null) {
            Optional<TiledSurfaceRenderer.TileInspection> tile =
                tiledRenderer.getTileInspectionAt(context.renderRow(), context.renderColumn());
            if (tile.isPresent()) {
                TiledSurfaceRenderer.TileInspection t = tile.get();
                tileId = t.tileId();
                activeLod = t.activeLod();

                int sx1 = coordinateMapper.renderColumnBoundaryToSourceBoundary(t.startColumn());
                int sx2 = coordinateMapper.renderColumnBoundaryToSourceBoundary(t.endColumnExclusive());
                int sy1 = coordinateMapper.renderRowBoundaryToSourceBoundary(t.startRow());
                int sy2 = coordinateMapper.renderRowBoundaryToSourceBoundary(t.endRowExclusive());
                int sourceOffsetX = pane.getImageSourceStartXForInteraction();
                int sourceOffsetY = pane.getImageSourceStartYForInteraction();
                tileImageMinX = Math.min(sx1, sx2) + sourceOffsetX;
                tileImageMaxXExclusive = Math.max(sx1, sx2) + sourceOffsetX;
                tileImageMinY = Math.min(sy1, sy2) + sourceOffsetY;
                tileImageMaxYExclusive = Math.max(sy1, sy2) + sourceOffsetY;
            }
        }

        double worldX = context.surfacePoint().getX() - pane.getWorldWidth() / 2.0;
        double worldY = context.surfacePoint().getY();
        double worldZ = context.surfacePoint().getZ() - pane.getWorldDepth() / 2.0;

        return new SurfaceInspection(
            imageBacked,
            pane.getSourceWidth(),
            pane.getSourceHeight(),
            coordinateMapper.renderWidth(),
            coordinateMapper.renderHeight(),
            context.renderColumn(),
            context.renderRow(),
            context.sourceColumn(),
            context.sourceRow(),
            imageColumn,
            imageRow,
            context.value(),
            worldX,
            worldY,
            worldZ,
            lastRowMinimum,
            lastRowMaximum,
            lastColumnMinimum,
            lastColumnMaximum,
            red,
            green,
            blue,
            intensity,
            tileId,
            activeLod,
            tileImageMinX,
            tileImageMinY,
            tileImageMaxXExclusive,
            tileImageMaxYExclusive);
    }

    private void refreshStatistics(int renderRow, int renderColumn) {
        HeightField field = pane.getActiveHeightFieldForInteraction();
        if (field == null) return;

        if (renderRow != lastStatsRenderRow) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int column = 0; column < field.width(); column++) {
                double value = pane.getActiveRenderValue(renderRow, column);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            lastRowMinimum = Double.isFinite(min) ? min : Double.NaN;
            lastRowMaximum = Double.isFinite(max) ? max : Double.NaN;
            lastStatsRenderRow = renderRow;
        }

        if (renderColumn != lastStatsRenderColumn) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int row = 0; row < field.height(); row++) {
                double value = pane.getActiveRenderValue(row, renderColumn);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            lastColumnMinimum = Double.isFinite(min) ? min : Double.NaN;
            lastColumnMaximum = Double.isFinite(max) ? max : Double.NaN;
            lastStatsRenderColumn = renderColumn;
        }
    }

    private boolean tileOrLodChanged(int renderRow, int renderColumn) {
        if (!pane.isImageBackedSurface() || !pane.isTiledHeightFieldRendererActive()) return false;
        TiledSurfaceRenderer renderer = pane.getTiledSurfaceRendererForInteraction();
        if (renderer == null) return false;
        Optional<TiledSurfaceRenderer.TileInspection> tile =
            renderer.getTileInspectionAt(renderRow, renderColumn);
        if (tile.isEmpty()) return lastSurfaceInspection != null && lastSurfaceInspection.hasTile();
        if (lastSurfaceInspection == null) return true;
        return tile.get().tileId() != lastSurfaceInspection.tileId()
            || tile.get().activeLod() != lastSurfaceInspection.activeLod();
    }

    private void publishSurfaceInspection(SurfaceInspection inspection) {
        lastSurfaceInspection = inspection;
        fireHypersurfaceEvent(HypersurfaceEvent.surfaceInspection(inspection));
    }

    private void fireHypersurfaceEvent(HypersurfaceEvent event) {
        if (Platform.isFxApplicationThread()) {
            scene.getRoot().fireEvent(event);
        } else {
            Platform.runLater(() -> scene.getRoot().fireEvent(event));
        }
    }

    private void publishSurfaceChartData(SurfaceInteractionContext context) {
        List<Double> xList = pane.getActiveRenderRow(context.renderRow());
        Double[] xRay = xList.toArray(Double[]::new);
        Double[] zRay = pane.getActiveRenderColumn(context.renderColumn());

        scene.getRoot().fireEvent(new FactorAnalysisEvent(
            FactorAnalysisEvent.SURFACE_XFACTOR_VECTOR, xRay));
        scene.getRoot().fireEvent(new FactorAnalysisEvent(
            FactorAnalysisEvent.SURFACE_ZFACTOR_VECTOR, zRay));
    }

    private void wireGraphInteractionHandlers() {
        scene.addEventHandler(GraphEvent.GRAPH_NODE_HOVER, e -> {
            if (!(e.object instanceof GraphNode graphNode)) return;
            if (surfaceChartsEnabled) {
                Double[] row = buildAdjacencyRowFromGraph(graphNode);
                scene.getRoot().fireEvent(new FactorAnalysisEvent(
                    FactorAnalysisEvent.ANALYSIS_DATA_VECTOR,
                    "Graph Adjacency Row (hover): " + graphNode,
                    row));
            }
            highlightSurfaceRowIfPossible(graphNode);
        });

        scene.addEventHandler(GraphEvent.GRAPH_NODE_CLICK, e -> {
            if (!(e.object instanceof GraphNode graphNode)) return;
            Double[] row = buildAdjacencyRowFromGraph(graphNode);
            scene.getRoot().fireEvent(new FactorAnalysisEvent(
                FactorAnalysisEvent.ANALYSIS_DATA_VECTOR,
                "Graph Adjacency Row (click): " + graphNode,
                row));
            highlightSurfaceRowIfPossible(graphNode);
        });

        scene.addEventHandler(GraphEvent.GRAPH_EDGE_HOVER, e -> {
            if (!(e.object instanceof GraphEdge graphEdge)) return;
            Optional<GraphNode> start = currentGraph != null
                ? currentGraph.findNodeById(graphEdge.getStartID())
                : Optional.empty();
            Optional<GraphNode> end = currentGraph != null
                ? currentGraph.findNodeById(graphEdge.getEndID())
                : Optional.empty();
            double weight = getEdgeWeightSafe(graphEdge);
            scene.getRoot().fireEvent(new CommandTerminalEvent(
                "Edge hover: " + start.map(Object::toString).orElse("?") + " → "
                    + end.map(Object::toString).orElse("?") + " | weight = " + weight,
                new Font("Consolas", 16), Color.ALICEBLUE));
        });

        scene.addEventHandler(GraphEvent.GRAPH_EDGE_CLICK, e -> {
            if (!(e.object instanceof GraphEdge graphEdge)) return;
            double weight = getEdgeWeightSafe(graphEdge);
            scene.getRoot().fireEvent(new FactorAnalysisEvent(
                FactorAnalysisEvent.ANALYSIS_DATA_VECTOR,
                "Graph Edge Weight (click): " + graphEdge.getStartID() + " → "
                    + graphEdge.getEndID(),
                new Double[]{weight}));
        });
    }

    private Double[] buildAdjacencyRowFromGraph(GraphNode node) {
        if (currentGraph == null || node == null) return new Double[0];
        Integer nodeIndex = graphEntityIdToIndex.get(node.getEntityID());
        if (nodeIndex == null || nodeIndex < 0 || nodeIndex >= graphAdjacencyIndices.length) {
            return new Double[0];
        }

        int n = currentGraph.getNodes().size();
        Double[] out = new Double[n];
        Arrays.fill(out, 0.0);
        for (int neighbor : graphAdjacencyIndices[nodeIndex]) {
            if (neighbor >= 0 && neighbor < n) out[neighbor] = 1.0;
        }
        return out;
    }

    private Optional<Integer> getGraphSourceRowIndex(GraphNode node) {
        if (node == null || currentGraph == null) return Optional.empty();
        Integer row = graphNodeRowIndex.get(node);
        if (row == null || row < 0 || row >= pane.getSourceHeight()) return Optional.empty();
        return Optional.of(row);
    }

    private void highlightSurfaceRowIfPossible(GraphNode node) {
        if (!crosshairsEnabled || pane.getActiveHeightFieldForInteraction() == null) {
            surfaceCrosshairOverlay.hide();
            return;
        }
        getGraphSourceRowIndex(node).ifPresent(surfaceCrosshairOverlay::requestSourceRow);
    }

    private static double getEdgeWeightSafe(GraphEdge edge) {
        try {
            return (double) GraphEdge.class.getMethod("getWeight").invoke(edge);
        } catch (Throwable t) {
            return 1.0;
        }
    }
}
