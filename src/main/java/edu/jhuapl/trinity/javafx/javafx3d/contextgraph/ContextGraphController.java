package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphFeatureCollectionAdapter;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.utils.graph.GraphLayoutParams;
import edu.jhuapl.trinity.utils.graph.ForceFrLayout3D;
import edu.jhuapl.trinity.utils.graph.GraphTopologyLayouts3D;
import edu.jhuapl.trinity.utils.graph.GraphNetworkLayouts3D;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.concurrent.Task;
import javafx.geometry.Point3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.javafx.javafx3d.animated.AnimatedSphere;

import edu.jhuapl.trinity.javafx.renderers.Graph3DRenderer;
import edu.jhuapl.trinity.utils.graph.GraphStyleParams;
import edu.jhuapl.trinity.utils.Utils;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;
import javafx.scene.image.WritableImage;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.text.Font;


/**
 * Owns the 3D graph visual subsystem hosted by the Context Graph view.
 *
 * The controller owns graph rendering and style state independently of other views.
 *
 * @author Sean Phillips
 */
public final class ContextGraphController {

    private final Scene scene;
    private final Group graphLayer = new Group();
    // These groups stay attached to the SubScene for the lifetime of the viewer.
    private final Group nodeLayer = new Group();
    private final Group edgeLayer = new Group();
    private boolean diagnosticDiffuseMaterials;
    private final Map<Color, PhongMaterial> diffuseMaterials = new HashMap<>();
    // Test 2: all edges use one shared opaque material, independent of style.
    private static final PhongMaterial TEST_EDGE_MATERIAL = new PhongMaterial(Color.CORNFLOWERBLUE);

    public boolean isDiagnosticDiffuseMaterials() { return diagnosticDiffuseMaterials; }

    /** Diagnostic only: compare the ordinary diffuse pipeline with RGBA emissive textures. */
    public void setDiagnosticDiffuseMaterials(boolean enabled) {
        if (diagnosticDiffuseMaterials == enabled) return;
        diagnosticDiffuseMaterials = enabled;
        applyNodeMaterials(styleParams);
        applyEdgeMaterials(styleParams);
    }

    private PhongMaterial displayMaterial(Color color, double opacity) {
        if (!diagnosticDiffuseMaterials) return emissiveMaterial(color, opacity);
        // Deliberately opaque for the rendering A/B test.
        Color opaque = new Color(color.getRed(), color.getGreen(), color.getBlue(), 1.0);
        return diffuseMaterials.computeIfAbsent(opaque, c -> {
            PhongMaterial material = new PhongMaterial(c);
            material.setSpecularColor(c);
            return material;
        });
    }

    private boolean graphVisible = true;
    private GraphDirectedCollection currentGraph;
    private final Graph3DRenderer.Params graphParams = new Graph3DRenderer.Params()
        .withNodeRadius(1.0)
        .withNodeDivisions(16)
        .withEdgeWidth(0.5f)
        .withPositionScalar(1.0);
    private GraphStyleParams styleParams = new GraphStyleParams();
    private boolean eventHandlersInstalled;
    private final Map<Color, PhongMaterial> emissiveMaterials = new HashMap<>();
    private double coordinateScale = 1.0;

    // Coordinates are independent of the GraphNode's source vector.
    private final Map<String, double[]> layoutPositions = new HashMap<>();
    private final Map<String, AnimatedSphere> nodeViews = new HashMap<>();
    private final Map<GraphEdge, ContextGraphEdge3D> edgeViews = new java.util.IdentityHashMap<>();


    private GraphLayoutParams layoutParams = new GraphLayoutParams();
    private long layoutGeneration;
    private volatile long renderGeneration;
    private int edgeBatchSize = 100;

    public int getEdgeBatchSize() { return edgeBatchSize; }

    /** Changes the number of edges attached on the next animation pulse. FX thread only. */
    public void setEdgeBatchSize(int value) {
        if (value < 1 || value > 1000) throw new IllegalArgumentException("Edge batch size must be between 1 and 1000");
        edgeBatchSize = value;
    }
    private AnimationTimer edgeAttachmentTimer;

    public double getCoordinateScale() { return coordinateScale; }

    public void setCoordinateScale(double value) {
        if (!Double.isFinite(value) || value <= 0 || value == coordinateScale) return;
        coordinateScale = value;
        updateNodePositions();
        updateEdgeEndpoints();
    }

    public void finishCoordinateScale() {
        updateEdgeEndpoints();
    }

    private double[] sourcePosition(GraphNode node) {
        double[] position = layoutPositions.get(node.getEntityID());
        if (position != null) return position;
        if (node.getVector() == null || node.getVector().size() < 3) return new double[]{0,0,0};
        return new double[]{node.getVector().get(0), node.getVector().get(1), node.getVector().get(2)};
    }

    private double[] scaledPosition(GraphNode node) {
        double[] p = sourcePosition(node);
        return new double[]{p[0]*coordinateScale, p[1]*coordinateScale, p[2]*coordinateScale};
    }

    private void updateNodePositions() {
        if (currentGraph == null) return;
        for (GraphNode node : currentGraph.getNodes()) {
            AnimatedSphere sphere = nodeViews.get(node.getEntityID());
            if (sphere == null) continue;
            double[] p = scaledPosition(node);
            sphere.setTranslateX(p[0]);
            sphere.setTranslateY(p[1]);
            sphere.setTranslateZ(p[2]);
        }
    }

    private void updateEdgeEndpoints() {
        if (currentGraph == null) return;
        for (Map.Entry<GraphEdge, ContextGraphEdge3D> entry : edgeViews.entrySet()) {
            GraphEdge edge = entry.getKey();
            Optional<GraphNode> a = currentGraph.findNodeById(edge.getStartID());
            Optional<GraphNode> b = currentGraph.findNodeById(edge.getEndID());
            if (a.isEmpty() || b.isEmpty()) continue;
            double[] ap=scaledPosition(a.get()), bp=scaledPosition(b.get());
            entry.getValue().setEndpoints(new Point3D(ap[0],ap[1],ap[2]),
                new Point3D(bp[0],bp[1],bp[2]));
        }
    }

    private void refreshEdges() {
        long startTime = System.nanoTime();
        if (currentGraph == null || edgeLayer == null) return;
        edgeLayer.getChildren().clear();
        edgeViews.clear();
        for (GraphEdge edge : currentGraph.getEdges()) {
            Optional<GraphNode> a = currentGraph.findNodeById(edge.getStartID());
            Optional<GraphNode> b = currentGraph.findNodeById(edge.getEndID());
            if (a.isEmpty() || b.isEmpty()) continue;
            double[] ap = scaledPosition(a.get());
            double[] bp = scaledPosition(b.get());
            ContextGraphEdge3D edgeView = new ContextGraphEdge3D(
                (float) Math.max(0.001, styleParams.edgeWidth), 6, 1);
            edgeView.setEndpoints(new Point3D(ap[0], ap[1], ap[2]),
                new Point3D(bp[0], bp[1], bp[2]));
            edgeViews.put(edge, edgeView);
            edgeLayer.getChildren().add(edgeView);
        }
        System.out.printf("[CG-TIMING] edge construction + attachment %.3f ms edges=%d%n", (System.nanoTime()-startTime)/1e6, edgeLayer.getChildren().size());
        applyEdgeMaterials(styleParams);
        System.out.print("ContextGraph refreshEdges: ");
        Utils.printTotalTime(startTime);
    }

    private void applyLayout(GraphLayoutParams params) {
        if (currentGraph == null || currentGraph.getNodes().isEmpty()) return;
        layoutParams = params;
        final GraphDirectedCollection graph = currentGraph;
        final List<GraphNode> nodes = new ArrayList<>(graph.getNodes());
        final long generation = ++layoutGeneration;
        final int n = nodes.size();
        final double[][] weights = new double[n][n];
        final Map<String,Integer> ids = new HashMap<>();
        for (int i=0;i<n;i++) ids.put(nodes.get(i).getEntityID(),i);
        final List<int[]> topologyEdges = new ArrayList<>();
        for (GraphEdge edge:graph.getEdges()) {
            Integer a=ids.get(edge.getStartID()), b=ids.get(edge.getEndID());
            if(a!=null && b!=null) { weights[a][b]=1; weights[b][a]=1; topologyEdges.add(new int[]{a,b}); }
        }
        Thread worker = new Thread(() -> {
            double[][] positions = new double[n][3];
            if (params.kind == GraphLayoutParams.LayoutKind.FORCE_FR) {
                positions = new ForceFrLayout3D().layout(n, null, null, weights, params);
            } else if (params.kind == GraphLayoutParams.LayoutKind.FORCE_ATLAS_3D
                || params.kind == GraphLayoutParams.LayoutKind.KAMADA_KAWAI_3D
                || params.kind == GraphLayoutParams.LayoutKind.STRESS_MAJOR_3D
                || params.kind == GraphLayoutParams.LayoutKind.SPECTRAL_3D) {
                positions = GraphNetworkLayouts3D.layout(n, topologyEdges, params);
            } else if (params.kind == GraphLayoutParams.LayoutKind.RADIAL_TREE_3D
                || params.kind == GraphLayoutParams.LayoutKind.LAYERED_HIERARCHY_3D
                || params.kind == GraphLayoutParams.LayoutKind.HYPERBOLIC_HIERARCHY_3D) {
                positions = GraphTopologyLayouts3D.layout(n, topologyEdges, params);
            } else {
                for(int i=0;i<n;i++) {
                    double angle=2*Math.PI*i/n;
                    if(params.kind == GraphLayoutParams.LayoutKind.CIRCLE_XY) {
                        positions[i]=new double[]{params.radius*Math.cos(angle),params.radius*Math.sin(angle),0};
                    } else if(params.kind == GraphLayoutParams.LayoutKind.CIRCLE_XZ) {
                        positions[i]=new double[]{params.radius*Math.cos(angle),0,params.radius*Math.sin(angle)};
                    } else {
                        double y=1-2*(i+0.5)/n;
                        double r=Math.sqrt(1-y*y);
                        double theta=i*Math.PI*(3-Math.sqrt(5));
                        positions[i]=new double[]{params.radius*r*Math.cos(theta),params.radius*y,params.radius*r*Math.sin(theta)};
                    }
                }
            }
            final double[][] result=positions;
            Platform.runLater(() -> {
                if(generation!=layoutGeneration || graph!=currentGraph) return;
                layoutPositions.clear();
                for(int i=0;i<n;i++) layoutPositions.put(nodes.get(i).getEntityID(),result[i]);
                updateNodePositions();
                updateEdgeEndpoints();
                report("Applied graph layout: " + params.kind);
            });
        }, "context-graph-layout");
        worker.setDaemon(true);
        worker.start();
    }

    private void report(String message) {
        if(scene.getRoot()!=null) scene.getRoot().fireEvent(new CommandTerminalEvent(
            message, new Font("Consolas", 16), Color.ALICEBLUE));
    }

    public ContextGraphController(Scene scene) {
        this.scene = scene;
        graphLayer.setVisible(graphVisible);
        graphLayer.getChildren().addAll(edgeLayer, nodeLayer);
    }


    /**
     * Synchronizes graph-owned state back to the existing graph controls.
     */
    public void syncVisibilityGui() {
        fireOnRoot(new GraphEvent(GraphEvent.SET_GRAPH_VISIBILITY_GUI, graphVisible));
    }

    private boolean nodesDisplayed = true;
    private boolean edgesDisplayed = true;

    public boolean isNodesDisplayed() { return nodesDisplayed; }
    public boolean isEdgesDisplayed() { return edgesDisplayed; }

    /** Detach or reattach the existing layer without recreating geometry. */
    public void setNodesDisplayed(boolean displayed) {
        if (nodesDisplayed == displayed) return;
        nodesDisplayed = displayed;
        long start = System.nanoTime();
        if (displayed) {
            if (!graphLayer.getChildren().contains(nodeLayer)) graphLayer.getChildren().add(nodeLayer);
        } else {
            graphLayer.getChildren().remove(nodeLayer);
        }
        System.out.printf("[CG-TIMING] nodes %s layer switch %.3f ms objects=%d%n",
            displayed ? "attached" : "detached", (System.nanoTime()-start)/1e6, nodeLayer.getChildren().size());
    }

    public void setEdgesDisplayed(boolean displayed) {
        if (edgesDisplayed == displayed) return;
        edgesDisplayed = displayed;
        long start = System.nanoTime();
        if (displayed) {
            if (!graphLayer.getChildren().contains(edgeLayer)) graphLayer.getChildren().add(0, edgeLayer);
        } else {
            graphLayer.getChildren().remove(edgeLayer);
        }
        System.out.printf("[CG-TIMING] edges %s layer switch %.3f ms objects=%d%n",
            displayed ? "attached" : "detached", (System.nanoTime()-start)/1e6, edgeLayer.getChildren().size());
    }

    public Group getGraphLayer() {
        return graphLayer;
    }

    public boolean isGraphVisible() {
        return graphVisible;
    }

    public GraphDirectedCollection getCurrentGraph() {
        return currentGraph;
    }

    public GraphStyleParams getStyleParams() {
        return styleParams;
    }

    /**
     * Installs GraphEvent handlers once. Kept separate from construction so the host
     * pane controls when the graph subsystem becomes event-active.
     */
    public void installEventHandlers() {
        if (scene == null || eventHandlersInstalled) return;
        eventHandlersInstalled = true;

        scene.addEventHandler(GraphEvent.GRAPH_REBUILD_PARAMS, e -> {
            if (e.object instanceof GraphLayoutParams p) applyLayout(p);
        });
        scene.addEventHandler(GraphEvent.GRAPH_RESET_PARAMS, e -> {
            layoutParams = new GraphLayoutParams();
            layoutPositions.clear();
            updateNodePositions();
            updateEdgeEndpoints();
        });
        scene.addEventHandler(GraphEvent.GRAPH_STYLE_PARAMS_CHANGED, e -> {
            if (!(e.object instanceof GraphStyleParams p)) return;

            styleParams.nodeColor = p.nodeColor;
            styleParams.nodeRadius = p.nodeRadius;
            styleParams.nodeOpacity = clamp01(p.nodeOpacity);
            styleParams.edgeColor = p.edgeColor;
            styleParams.edgeWidth = p.edgeWidth;
            styleParams.edgeOpacity = clamp01(p.edgeOpacity);

            applyGraphStyle(styleParams, true);
        });

        scene.addEventHandler(GraphEvent.GRAPH_STYLE_RESET_DEFAULTS, e -> {
            styleParams = new GraphStyleParams();

            graphParams.withNodeRadius(styleParams.nodeRadius)
                .withEdgeWidth((float) styleParams.edgeWidth);

            if (currentGraph != null) {
                rebuildGraph();
            }
            applyGraphStyle(styleParams, false);
            fireOnRoot(new GraphEvent(GraphEvent.SET_STYLE_GUI, styleParams));
        });


        scene.addEventHandler(GraphEvent.GRAPH_VISIBILITY_CHANGED, e -> {
            if (!(e.object instanceof Boolean visible)) return;
            setGraphVisible(visible);
        });
    }

    private GraphFeatureCollectionAdapter featureAdapter;

    public GraphFeatureCollectionAdapter getFeatureAdapter() { return featureAdapter; }

    /** Loads a graph without routing through a generic callback. */
    public void loadGraph(GraphDirectedCollection graph) {
        if (graph == null) return;
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> loadGraph(graph));
            return;
        }
        currentGraph = graph;
        ++layoutGeneration;
        layoutPositions.clear();
        rebuildGraph();
        fireOnRoot(new GraphEvent(GraphEvent.SET_STYLE_GUI, styleParams));
        fireOnRoot(new GraphEvent(GraphEvent.SET_GRAPH_VISIBILITY_GUI, graphVisible));
    }

    private void setGraphVisible(boolean visible) {
        graphVisible = visible;
        graphLayer.setVisible(visible);
    }

    /** Prepare detached geometry on a worker Task; attach edges across FX pulses. */
    private void rebuildGraph() {
        // Only the layer reset below requires FX-thread ownership; the Task
        // itself and all geometry preparation are independent of FX scheduling.
        final long generation = ++renderGeneration;
        if (edgeAttachmentTimer != null) {
            edgeAttachmentTimer.stop();
            edgeAttachmentTimer = null;
        }
        nodeLayer.getChildren().clear();
        edgeLayer.getChildren().clear();
        nodeViews.clear();
        edgeViews.clear();
        final GraphDirectedCollection graph = currentGraph;
        if (graph == null) return;
        final double scale = coordinateScale;
        final double nodeRadius = styleParams.nodeRadius;
        final double edgeRadius = Math.max(0.001, styleParams.edgeWidth);
        final int nodeDivisions = graphParams.nodeDivisions;
        final PhongMaterial nodeMaterial = displayMaterial(
            styleParams.nodeColor == null ? Color.CYAN : styleParams.nodeColor,
            styleParams.nodeOpacity);
        final PhongMaterial edgeMaterial = displayMaterial(
            styleParams.edgeColor == null ? Color.CORNFLOWERBLUE : styleParams.edgeColor,
            styleParams.edgeOpacity);
        final long started = System.nanoTime();
        final Map<String, double[]> layoutSnapshot = new HashMap<>(layoutPositions);
        // Snapshot ordinary graph data on the worker, not the FX thread.
        // This Task is started directly; it is not rescheduled onto FX.
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                long prepareStart = System.nanoTime();
                final List<GraphNode> graphNodes = new ArrayList<>(graph.getNodes());
                final List<GraphEdge> graphEdges = new ArrayList<>(graph.getEdges());
                final Map<String, double[]> coordinates = new HashMap<>();
                for (GraphNode node : graphNodes) {
                    if (isCancelled() || generation != renderGeneration) return null;
                    double[] pos = layoutSnapshot.get(node.getEntityID());
                    if (pos == null) {
                        if (node.getVector() == null || node.getVector().size() < 3) {
                            pos = new double[]{0, 0, 0};
                        } else {
                            pos = new double[]{node.getVector().get(0),
                                node.getVector().get(1), node.getVector().get(2)};
                        }
                    }
                    coordinates.put(node.getEntityID(), new double[]{
                        pos[0] * scale, pos[1] * scale, pos[2] * scale});
                }
                for (GraphNode node : graphNodes) {
                    if (isCancelled() || generation != renderGeneration) return null;
                    double[] pos = coordinates.get(node.getEntityID());
                    AnimatedSphere sphere = new AnimatedSphere(nodeMaterial, nodeRadius,
                        nodeDivisions, false);
                    sphere.setExpansionScale(1.10);
                    sphere.setMouseTransparent(true);
                    sphere.setOnMouseMoved(null);
                    sphere.setOnMouseClicked(null);
                    sphere.setTranslateX(pos[0]);
                    sphere.setTranslateY(pos[1]);
                    sphere.setTranslateZ(pos[2]);
                    Platform.runLater(() -> {
                        if (generation != renderGeneration) return;
                        nodeViews.put(node.getEntityID(), sphere);
                        nodeLayer.getChildren().add(sphere);
                    });
                }
                long edgeStart = System.nanoTime();
                final List<ContextGraphEdge3D> preparedEdges = new ArrayList<>();
                final List<GraphEdge> preparedEdgeKeys = new ArrayList<>();
                for (GraphEdge edge : graphEdges) {
                    if (isCancelled() || generation != renderGeneration) return null;
                    double[] a = coordinates.get(edge.getStartID());
                    double[] b = coordinates.get(edge.getEndID());
                    if (a == null || b == null) continue;
                    ContextGraphEdge3D view = new ContextGraphEdge3D((float) edgeRadius, 3, 1);
                    view.setEndpoints(new Point3D(a[0], a[1], a[2]),
                        new Point3D(b[0], b[1], b[2]));
                    view.setMaterial(edgeMaterial);
                    view.setMouseTransparent(true);
                    preparedEdges.add(view);
                    preparedEdgeKeys.add(edge);
                }
                System.out.printf("[CG-TIMING] background geometry preparation %.3f ms; edge phase %.3f ms%n",
                    (System.nanoTime() - prepareStart) / 1e6,
                    (System.nanoTime() - edgeStart) / 1e6);
                Platform.runLater(() -> {
                    if (generation != renderGeneration) return;
                    final long progressiveStart = System.nanoTime();
                    edgeAttachmentTimer = new AnimationTimer() {
                        private int nextIndex;
                        private int batchCount;
                        private long previousPulse = -1L;
                        private double maxPulseGapMs;
                        private double maxAttachMs;

                        @Override public void handle(long now) {
                            if (generation != renderGeneration) {
                                stop();
                                if (edgeAttachmentTimer == this) edgeAttachmentTimer = null;
                                return;
                            }
                            if (previousPulse >= 0) {
                                double gapMs = (now - previousPulse) / 1e6;
                                maxPulseGapMs = Math.max(maxPulseGapMs, gapMs);
                                if (gapMs >= 250.0) {
                                    System.out.printf("[CG-TIMING] progressive FX pulse gap %.3f ms edges=%d/%d%n",
                                        gapMs, nextIndex, preparedEdges.size());
                                }
                            }
                            previousPulse = now;
                            int end = Math.min(nextIndex + edgeBatchSize, preparedEdges.size());
                            long attachStart = System.nanoTime();
                            if (end > nextIndex) {
                                // Honor style changes made while progressive loading is active.
                                PhongMaterial activeMaterial = displayMaterial(
                                    styleParams.edgeColor == null ? Color.CORNFLOWERBLUE : styleParams.edgeColor,
                                    styleParams.edgeOpacity);
                                float activeWidth = (float) Math.max(0.001, styleParams.edgeWidth);
                                for (int i = nextIndex; i < end; i++) {
                                    ContextGraphEdge3D pending = preparedEdges.get(i);
                                    pending.setMaterial(activeMaterial);
                                    if (pending.getRadius() != activeWidth) pending.setRadius(activeWidth);
                                }
                                edgeLayer.getChildren().addAll(
                                    preparedEdges.subList(nextIndex, end));
                                for (int i = nextIndex; i < end; i++) {
                                    edgeViews.put(preparedEdgeKeys.get(i), preparedEdges.get(i));
                                }
                            }
                            double attachMs = (System.nanoTime() - attachStart) / 1e6;
                            maxAttachMs = Math.max(maxAttachMs, attachMs);
                            nextIndex = end;
                            batchCount++;
                            if (nextIndex == preparedEdges.size()) {
                                stop();
                                if (edgeAttachmentTimer == this) edgeAttachmentTimer = null;
                                System.out.printf("[CG-TIMING] progressive edges complete %.3f ms batches=%d batchSize=%d maxAttach=%.3f ms maxPulseGap=%.3f ms nodes=%d edges=%d%n",
                                    (System.nanoTime() - progressiveStart) / 1e6,
                                    batchCount, edgeBatchSize, maxAttachMs, maxPulseGapMs,
                                    nodeLayer.getChildren().size(), edgeLayer.getChildren().size());
                                System.out.printf("[CG-TIMING] all FX attachments completed %.3f ms nodes=%d edges=%d%n",
                                    (System.nanoTime() - started) / 1e6,
                                    nodeLayer.getChildren().size(), edgeLayer.getChildren().size());
                                report("Rendered 3D graph: nodes=" + nodeViews.size()
                                    + ", edges=" + edgeViews.size());
                            }
                        }
                    };
                    edgeAttachmentTimer.start();
                });
                return null;
            }
        };
        Thread worker = new Thread(task, "context-graph-geometry");
        worker.setDaemon(true);
        worker.start();
    }

    /** Styles only the indexed graph objects. No scene-graph recursion. */
    private void applyGraphStyle(GraphStyleParams style, boolean rebuildIfNeeded) {
        if (style == null) return;
        long startTime = System.nanoTime();
        boolean edgeWidthChanged = Math.abs(style.edgeWidth - graphParams.edgeWidth) > 1.0e-6;
        graphParams.withNodeRadius(style.nodeRadius);
        if (edgeWidthChanged && rebuildIfNeeded && currentGraph != null) {
            graphParams.withEdgeWidth((float) style.edgeWidth);
            for (ContextGraphEdge3D edge : edgeViews.values()) {
                edge.setRadius((float) Math.max(0.001, style.edgeWidth));
            }
        }
        applyNodeMaterials(style);
        applyEdgeMaterials(style);
        System.out.print("ContextGraph applyGraphStyle: ");
        Utils.printTotalTime(startTime);
    }

    private void applyNodeMaterials(GraphStyleParams style) {
        Color color = style.nodeColor == null ? Color.CYAN : style.nodeColor;
        PhongMaterial material = displayMaterial(color, style.nodeOpacity);
        for (AnimatedSphere sphere : nodeViews.values()) {
            if (sphere.getRadius() != style.nodeRadius) sphere.setSphereRadius(style.nodeRadius);
            if (sphere.getMaterial() != material) sphere.setMaterial(material);
        }
    }

    private void applyEdgeMaterials(GraphStyleParams style) {
        PhongMaterial material = displayMaterial(
            style.edgeColor == null ? Color.CORNFLOWERBLUE : style.edgeColor,
            style.edgeOpacity);
        for (ContextGraphEdge3D edge : edgeViews.values()) {
            if (edge.getMaterial() != material) edge.setMaterial(material);
        }
    }

    /** Current scaled coordinates, including any active graph layout. */
    public double[] getDisplayedPosition(GraphNode node) {
        return scaledPosition(node);
    }

    private PhongMaterial emissiveMaterial(Color color, double opacity) {
        Color rgba = new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp01(opacity));
        return emissiveMaterials.computeIfAbsent(rgba, key -> {
            WritableImage image = new WritableImage(1, 1);
            image.getPixelWriter().setColor(0, 0, key);
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseColor(null);
            material.setDiffuseMap(null);
            material.setSelfIlluminationMap(image);
            return material;
        });
    }

    private void fireOnRoot(Event event) {
        if (scene != null && scene.getRoot() != null) {
            scene.getRoot().fireEvent(event);
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
