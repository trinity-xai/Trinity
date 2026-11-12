package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.javafx.components.panes.ContextGraphControlsPane;
import edu.jhuapl.trinity.javafx.components.panes.RadialEntityOverlayPane;
import edu.jhuapl.trinity.javafx.components.callouts.Callout;
import edu.jhuapl.trinity.javafx.components.callouts.CalloutBuilder;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import java.util.Map;
import java.util.Objects;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.CullFace;
import javafx.scene.paint.PhongMaterial;
import java.util.List;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;

import javafx.scene.layout.Pane;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.Node;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.PointLight;
import org.fxyz3d.utils.CameraTransformer;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.geometry.Point3D;
import javafx.scene.transform.NonInvertibleTransformException;

/** Independent 3D graph viewport. Graph data remains in the shared graph model. */
public class ContextGraph3DPane extends StackPane {
    private final Group world = new Group();
    // Visual boundary of the computed Poincare chart, not a graph entity.
    private final Sphere poincareBoundary;
    private boolean showPoincareBoundary = true;
    private final CameraTransformer cameraTransform = new CameraTransformer();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene subScene;
    private final RadialEntityOverlayPane selectionOverlay;
    private final AnimationTimer calloutTimer;
    private Callout nodeCallout;
    private Callout edgeCallout;
    private ContextGraphNode3D nodeCalloutAnchor;
    private ContextGraphEdge3D edgeCalloutAnchor;
    // A dismissed callout stays closed until that entity's selection changes.
    private String observedNodeSelectionId;
    private String dismissedNodeSelectionId;
    private GraphEdge observedEdgeSelection;
    private GraphEdge dismissedEdgeSelection;
    private boolean selectionCalloutsEnabled = true;
    private double fitGraphPercentage = 85.0;
    private ContextGraphProjectionMode displayedProjectionMode = ContextGraphProjectionMode.EUCLIDEAN;
    private final ContextGraphController graphController;
    private final ContextGraphInteractionController interactionController;
    private ContextGraphControlsPane controlsPane;
    private double anchorX;
    private double anchorY;
    private double dragDistance;
    private String lastHoveredNodeId;
    private GraphEdge lastHoveredEdge;

    public ContextGraph3DPane(Scene scene) {
        graphController = new ContextGraphController(scene);
        interactionController = new ContextGraphInteractionController(scene);
        poincareBoundary = new Sphere(graphController.getHyperbolicDisplayRadius(), 36);
        poincareBoundary.setDrawMode(DrawMode.LINE);
        poincareBoundary.setCullFace(CullFace.NONE);
        poincareBoundary.setMaterial(new PhongMaterial(Color.color(0.42, 0.65, 0.95, 0.30)));
        poincareBoundary.setMouseTransparent(true);
        poincareBoundary.setVisible(false);
        graphController.getGraphLayer().getChildren().add(0, poincareBoundary);
        camera.setNearClip(0.1);
        camera.setFarClip(100000);
        camera.setTranslateZ(-80);
        cameraTransform.getChildren().add(camera);
        cameraTransform.ry.setAngle(-45.0);
        cameraTransform.rx.setAngle(-10.0);
        PointLight pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateZ(-900);
        cameraTransform.getChildren().add(pointLight);
        world.getChildren().addAll(cameraTransform, new AmbientLight(Color.color(0.65, 0.65, 0.65)),
            graphController.getGraphLayer());
        subScene = new SubScene(world, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.rgb(8, 12, 22));
        subScene.setCamera(camera);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        selectionOverlay = new RadialEntityOverlayPane(scene, new java.util.ArrayList<>());
        selectionOverlay.prefWidthProperty().bind(widthProperty());
        selectionOverlay.prefHeightProperty().bind(heightProperty());
        selectionOverlay.setPickOnBounds(false);
        getChildren().addAll(subScene, selectionOverlay);
        calloutTimer = new AnimationTimer() {
            @Override public void handle(long now) { synchronizeSelectionCallouts(); }
        };
        ContextMenu graphMenu = new ContextMenu();
        MenuItem controlsItem = new MenuItem("Context Graph Controls");
        controlsItem.setOnAction(e -> showGraphControls(scene));
        MenuItem fitItem = new MenuItem("Fit Graph");
        fitItem.setOnAction(e -> frameGraph(graphController.getCurrentGraph()));
        MenuItem focusSelectedItem = new MenuItem("Focus Selection");
        focusSelectedItem.setOnAction(e -> focusSelection());
        MenuItem focusNeighborhoodItem = new MenuItem("Focus Neighborhood");
        focusNeighborhoodItem.setOnAction(e -> focusNeighborhood());
        Menu projections = new Menu("Projection");
        ToggleGroup projectionToggles = new ToggleGroup();
        for (ContextGraphProjectionMode mode : ContextGraphProjectionMode.values()) {
            RadioMenuItem option = new RadioMenuItem(mode.toString());
            option.setToggleGroup(projectionToggles);
            option.setSelected(mode == graphController.getProjectionMode());
            option.setOnAction(e -> graphController.setProjectionMode(mode));
            projections.getItems().add(option);
        }
        MenuItem resetFocusItem = new MenuItem("Reset Hyperbolic Focus");
        resetFocusItem.setOnAction(e -> graphController.resetHyperbolicFocus());
        projections.getItems().add(resetFocusItem);
        scene.addEventHandler(GraphEvent.GRAPH_PROJECTION_APPLIED, e -> {
            ContextGraphProjectionMode mode = graphController.getProjectionMode();
            for (int i = 0; i < ContextGraphProjectionMode.values().length; i++) {
                ((RadioMenuItem) projections.getItems().get(i)).setSelected(
                    ContextGraphProjectionMode.values()[i] == mode);
            }
            if (mode != displayedProjectionMode) {
                displayedProjectionMode = mode;
                frameGraph(graphController.getCurrentGraph());
            }
            refreshPoincareBoundary();
            if (mode.isHyperbolic()) {
                // Orbital center is the Poincare origin after Mobius recentering.
                cameraTransform.setTranslate(0.0, 0.0, 0.0);
                camera.setTranslateX(0.0);
                camera.setTranslateY(0.0);
            }
        });
        scene.addEventHandler(GraphEvent.GRAPH_PROJECTION_FAILED, e -> {
            ContextGraphProjectionMode mode = graphController.getProjectionMode();
            for (int i = 0; i < ContextGraphProjectionMode.values().length; i++) {
                ((RadioMenuItem) projections.getItems().get(i)).setSelected(
                    ContextGraphProjectionMode.values()[i] == mode);
            }
        });
        MenuItem showAllItem = new MenuItem("Show All (Category Filters Retained)");
        showAllItem.setOnAction(e -> graphController.showAll());
        MenuItem navigatorItem = new MenuItem("Content Navigator");
        navigatorItem.setOnAction(e -> scene.getRoot().fireEvent(
            new ApplicationEvent(ApplicationEvent.SHOW_NAVIGATOR_PANE)));
        CheckMenuItem diffuseTestItem = new CheckMenuItem("Diagnostic: Opaque Diffuse Materials");
        diffuseTestItem.setOnAction(e -> graphController.setDiagnosticDiffuseMaterials(diffuseTestItem.isSelected()));
        graphMenu.getItems().addAll(controlsItem, navigatorItem, projections, fitItem,
            focusSelectedItem, focusNeighborhoodItem, showAllItem, diffuseTestItem);
        subScene.setOnContextMenuRequested(e -> {
            graphMenu.show(subScene, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        // Pick against the concrete mesh and resolve via typed controller registries.
        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
            dragDistance = 0;
        });
        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double dx = e.getSceneX() - anchorX;
            double dy = e.getSceneY() - anchorY;
            dragDistance += Math.hypot(dx, dy);
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
            double modifier = e.isControlDown() ? 0.1 : e.isShiftDown() ? 25.0 : 1.0;
            double factor = 0.1 * modifier;
            if (e.isPrimaryButtonDown()) {
                if (e.isAltDown()) {
                    cameraTransform.rz.setAngle(cameraTransform.rz.getAngle() + dx * factor * 2.0);
                } else {
                    cameraTransform.ry.setAngle(cameraTransform.ry.getAngle() + dx * factor * 2.0);
                    cameraTransform.rx.setAngle(cameraTransform.rx.getAngle() - dy * factor * 2.0);
                }
            } else if (e.isMiddleButtonDown()) {
                cameraTransform.t.setX(cameraTransform.t.getX() + dx * factor * 0.3);
                cameraTransform.t.setY(cameraTransform.t.getY() + dy * factor * 0.3);
            }
        });
        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() != MouseButton.PRIMARY || !e.isStillSincePress()
                || dragDistance > 3.0) return;
            Node picked = e.getPickResult() == null ? null : e.getPickResult().getIntersectedNode();
            GraphNode node = graphController.resolveNode(picked);
            GraphEdge edge = graphController.resolveEdge(picked);
            if (node != null) {
                graphController.toggleNodeSelection(node);
                scene.getRoot().fireEvent(new GraphEvent(GraphEvent.GRAPH_NODE_CLICK,
                    node, graphController.getSelectedNodeId() != null));
            } else if (edge != null) {
                graphController.toggleEdgeSelection(edge);
                scene.getRoot().fireEvent(new GraphEvent(GraphEvent.GRAPH_EDGE_CLICK,
                    edge, graphController.getSelectedEdge() != null));
            } else {
                graphController.clearSelections();
            }
            updateSelectionCallouts();
        });
        // Shift+wheel is delivered as horizontal scrolling on some platforms.
        // Use deltaX as a fallback rather than losing the zoom input.
        subScene.setOnScroll(e -> {
            double delta = e.getDeltaY();
            if (Math.abs(delta) < 1.0e-9 && e.isShiftDown()) {
                delta = e.getDeltaX();
            }
            if (Math.abs(delta) < 1.0e-9) return;
            double modifier = e.isShiftDown() ? 100.0 : e.isControlDown() ? 1.0 : 50.0;
            double nextZ = camera.getTranslateZ() + delta * 0.1 * modifier;
            // Keep the camera on the negative-Z side of its orbital center.
            camera.setTranslateZ(Math.min(-0.1, nextZ));
            e.consume();
        });
        // Match Projections3DPane: camera-local translation, not pivot movement.
        subScene.setOnKeyPressed(e -> {
            double step = e.isShiftDown() ? 100.0 : 10.0;
            switch (e.getCode()) {
                case W -> camera.setTranslateZ(Math.min(-0.1, camera.getTranslateZ() + step));
                case S -> camera.setTranslateZ(camera.getTranslateZ() - step);
                case A -> camera.setTranslateX(camera.getTranslateX() - step);
                case D -> camera.setTranslateX(camera.getTranslateX() + step);
                case SPACE -> camera.setTranslateY(camera.getTranslateY() + step);
                case X -> camera.setTranslateY(camera.getTranslateY() - step);
                default -> { return; }
            }
            e.consume();
        });
        subScene.setFocusTraversable(true);
        subScene.setOnMouseEntered(e -> subScene.requestFocus());
        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> subScene.requestFocus());
        graphController.installEventHandlers();
        scene.addEventHandler(GraphEvent.NEW_GRAPHDIRECTED_COLLECTION, e -> {
            if (e.object instanceof edu.jhuapl.trinity.data.graph.GraphDirectedCollection graph) {
                long eventStart = System.nanoTime();
                System.out.printf("[CG-TIMING] graph event entered thread=%s%n", Thread.currentThread().getName());
                clearSelectionCallouts();
                // Graph loading temporarily starts from source coordinates while the
                // hyperbolic chart is prepared; the completed event will re-fit the chart.
                displayedProjectionMode = ContextGraphProjectionMode.EUCLIDEAN;
                graphController.loadGraph(graph);
                updateSelectionCallouts();
                long frameStart = System.nanoTime();
                frameGraph(graph);
                System.out.printf("[CG-TIMING] frameGraph %.3f ms; graph event total %.3f ms%n", (System.nanoTime()-frameStart)/1e6, (System.nanoTime()-eventStart)/1e6);
                // Sample FX pulse callbacks for 15 seconds; these are NOT GPU presentation timestamps.
                new javafx.animation.AnimationTimer() {
                    private long previousPulse = -1;
                    private long nextReport = eventStart + 1_000_000_000L;
                    private long maxGapNanos;
                    private int pulseCount;
                    @Override public void handle(long now) {
                        long current = System.nanoTime();
                        pulseCount++;
                        if (previousPulse > 0) {
                            long gap = current - previousPulse;
                            if (gap > maxGapNanos) maxGapNanos = gap;
                            if (gap >= 250_000_000L) {
                                System.out.printf("[CG-TIMING] FX pulse gap %.3f ms at elapsed %.3f ms%n",
                                    gap / 1e6, (current - eventStart) / 1e6);
                            }
                        }
                        previousPulse = current;
                        if (current >= nextReport) {
                            System.out.printf("[CG-TIMING] FX pulse sample elapsed=%.3f ms pulses=%d maxGap=%.3f ms%n",
                                (current - eventStart) / 1e6, pulseCount, maxGapNanos / 1e6);
                            nextReport = current + 1_000_000_000L;
                        }
                        if (current - eventStart >= 15_000_000_000L) {
                            System.out.printf("[CG-TIMING] FX pulse sampling finished elapsed=%.3f ms pulses=%d maxGap=%.3f ms%n",
                                (current - eventStart) / 1e6, pulseCount, maxGapNanos / 1e6);
                            stop();
                        }
                    }
                }.start();
            }
        });
        interactionController.installEventHandlers();
    }

    private void showGraphControls(Scene scene) {
        Pane overlay = App.getAppPathPaneStack();
        if (overlay == null) return;
        if (controlsPane == null) {
            controlsPane = new ContextGraphControlsPane(scene, overlay, graphController, this);
        }
        if (!overlay.getChildren().contains(controlsPane)) {
            overlay.getChildren().add(controlsPane);
        }
        controlsPane.show();
        scene.getRoot().fireEvent(new GraphEvent(GraphEvent.SET_STYLE_GUI,
            edu.jhuapl.trinity.utils.graph.GraphStyleParams.copyOf(graphController.getStyleParams())));
        graphController.syncVisibilityGui();
    }

    /** Boundary is a visual reference only; it never participates in graph picking. */
    public boolean isPoincareBoundaryVisible() { return showPoincareBoundary; }

    public void setPoincareBoundaryVisible(boolean visible) {
        showPoincareBoundary = visible;
        refreshPoincareBoundary();
    }

    public void refreshPoincareBoundary() {
        double radius = graphController.getHyperbolicDisplayRadius();
        if (Double.isFinite(radius) && radius > 0.0 && poincareBoundary.getRadius() != radius) {
            poincareBoundary.setRadius(radius);
        }
        poincareBoundary.setVisible(showPoincareBoundary
            && graphController.getProjectionMode().isHyperbolic());
    }

    public boolean isSelectionCalloutsEnabled() { return selectionCalloutsEnabled; }

    public void setSelectionCalloutsEnabled(boolean enabled) {
        selectionCalloutsEnabled = enabled;
        updateSelectionCallouts();
    }

    /**
     * Search-panel selection uses the same graph state and events as 3D picking.
     * Focus uses Euclidean camera framing or hyperbolic recentering as appropriate.
     */
    public boolean selectEntity(String entityId, boolean focus) {
        if (!graphController.selectNodeById(entityId)) return false;
        GraphNode node = graphController.getSelectedNode();
        getScene().getRoot().fireEvent(new GraphEvent(GraphEvent.GRAPH_NODE_CLICK, node, true));
        updateSelectionCallouts();
        if (focus) focusSelection();
        return true;
    }

    /** Focus preserves the current orbital camera rotation. */
    public void focusSelection() {
        if (graphController.getProjectionMode().isHyperbolic())
            graphController.focusHyperbolicSelection();
        else frameNodes(graphController.getSelectedEntityNodes());
    }

    public void focusNeighborhood() {
        if (graphController.getProjectionMode().isHyperbolic())
            graphController.focusHyperbolicNeighborhood();
        else frameNodes(graphController.getNeighborhoodNodes());
    }

    private void clearSelectionCallouts() {
        if (nodeCalloutAnchor != null) selectionOverlay.removeCallout(nodeCalloutAnchor);
        if (edgeCalloutAnchor != null) selectionOverlay.removeCallout(edgeCalloutAnchor);
        nodeCalloutAnchor = null;
        edgeCalloutAnchor = null;
        nodeCallout = null;
        edgeCallout = null;
        calloutTimer.stop();
        observedNodeSelectionId = null;
        dismissedNodeSelectionId = null;
        observedEdgeSelection = null;
        dismissedEdgeSelection = null;
    }

    private void updateSelectionCallouts() {
        if (!selectionCalloutsEnabled) {
            clearSelectionCallouts();
            return;
        }
        if (graphController.getSelectedNodeId() == null && graphController.getSelectedEdge() == null) {
            clearSelectionCallouts();
            return;
        }
        synchronizeSelectionCallouts();
        calloutTimer.start();
    }

    private void synchronizeSelectionCallouts() {
        if (!selectionCalloutsEnabled) return;
        String selectedNodeId = graphController.getSelectedNodeId();
        GraphEdge selectedEdge = graphController.getSelectedEdge();
        if (!Objects.equals(observedNodeSelectionId, selectedNodeId)) {
            observedNodeSelectionId = selectedNodeId;
            dismissedNodeSelectionId = null;
        }
        if (observedEdgeSelection != selectedEdge) {
            observedEdgeSelection = selectedEdge;
            dismissedEdgeSelection = null;
        }

        ContextGraphNode3D currentNode = graphController.getSelectedNodeView();
        ContextGraphEdge3D currentEdge = graphController.getSelectedEdgeView();
        if (nodeCalloutAnchor != currentNode) {
            if (nodeCalloutAnchor != null) selectionOverlay.removeCallout(nodeCalloutAnchor);
            nodeCalloutAnchor = null;
            nodeCallout = null;
            GraphNode model = graphController.getSelectedNode();
            if (currentNode != null && model != null
                && !Objects.equals(dismissedNodeSelectionId, selectedNodeId)) {
                String name = displayName(model);
                nodeCallout = makeCallout(currentNode, name,
                    graphNodeDetails(model), this::dismissNodeCallout);
                nodeCalloutAnchor = currentNode;
            }
        }
        if (edgeCalloutAnchor != currentEdge) {
            if (edgeCalloutAnchor != null) selectionOverlay.removeCallout(edgeCalloutAnchor);
            edgeCalloutAnchor = null;
            edgeCallout = null;
            GraphEdge model = graphController.getSelectedEdge();
            if (currentEdge != null && model != null && dismissedEdgeSelection != selectedEdge) {
                String title = graphEdgeTitle(model);
                edgeCallout = makeCallout(currentEdge, title,
                    graphEdgeDetails(model), this::dismissEdgeCallout);
                edgeCalloutAnchor = currentEdge;
            }
        }
        if (nodeCallout != null && nodeCalloutAnchor != null) {
            boolean nodeShown = nodeCalloutAnchor.isVisible()
                && graphController.isNodesDisplayed() && graphController.isGraphVisible();
            nodeCallout.setVisible(nodeShown);
            if (nodeShown) {
                selectionOverlay.updateCalloutHeadPoint(nodeCalloutAnchor, nodeCallout, subScene);
            }
        }
        if (edgeCallout != null && edgeCalloutAnchor != null) {
            boolean edgeShown = edgeCalloutAnchor.isVisible()
                && graphController.isEdgesDisplayed() && graphController.isGraphVisible();
            edgeCallout.setVisible(edgeShown);
            if (edgeShown) {
                selectionOverlay.updateCalloutHeadPoint(edgeCalloutAnchor, edgeCallout, subScene);
            }
        }
    }

    private void dismissNodeCallout() {
        dismissedNodeSelectionId = graphController.getSelectedNodeId();
        if (nodeCallout != null) nodeCallout.hide();
        if (nodeCalloutAnchor != null) selectionOverlay.removeCallout(nodeCalloutAnchor);
        nodeCalloutAnchor = null;
        nodeCallout = null;
    }

    private void dismissEdgeCallout() {
        dismissedEdgeSelection = graphController.getSelectedEdge();
        if (edgeCallout != null) edgeCallout.hide();
        if (edgeCalloutAnchor != null) selectionOverlay.removeCallout(edgeCalloutAnchor);
        edgeCalloutAnchor = null;
        edgeCallout = null;
    }

    private static String displayName(GraphNode node) {
        if (node.getProperties() != null && node.getProperties().get("name") != null) {
            return String.valueOf(node.getProperties().get("name"));
        }
        return node.getEntityID();
    }

    private String graphEdgeTitle(GraphEdge edge) {
        String type = edge.getRelationship() == null || edge.getRelationship().getLabels() == null
            || edge.getRelationship().getLabels().isEmpty()
            ? "Relationship" : String.join(", ", edge.getRelationship().getLabels());
        return type;
    }

    /** Content mirrors the graph-specific fields presented by NavigatorPane on hover. */
    private static String graphNodeDetails(GraphNode node) {
        StringBuilder text = new StringBuilder();
        detail(text, "Entity ID", node.getEntityID());
        detail(text, "Identity", node.getIdentity());
        detail(text, "Labels", node.getLabels());
        if (node.getColor() != null) detail(text, "Color", node.getColor());
        appendProperties(text, node.getProperties());
        if (node.getVector() != null) detail(text, "Vector", node.getVector());
        return text.toString().stripTrailing();
    }

    private String graphEdgeDetails(GraphEdge edge) {
        StringBuilder text = new StringBuilder();
        detail(text, "Source", endpointDescription(edge.getStartID()));
        detail(text, "Target", endpointDescription(edge.getEndID()));
        double distance = graphController.hyperbolicDistance(edge.getStartID(), edge.getEndID());
        if (Double.isFinite(distance)) detail(text, "Hyperbolic distance",
            String.format(java.util.Locale.ROOT, "%.5f", distance));
        if (edge.getColor() != null) detail(text, "Color", edge.getColor());
        if (edge.getRelationship() != null) {
            detail(text, "Identity", edge.getRelationship().getIdentity());
            detail(text, "Labels", edge.getRelationship().getLabels());
            appendProperties(text, edge.getRelationship().getProperties());
        }
        return text.toString().stripTrailing();
    }

    private String endpointDescription(String entityId) {
        var graph = graphController.getCurrentGraph();
        if (graph == null || entityId == null) return String.valueOf(entityId);
        return graph.findNodeById(entityId)
            .map(node -> displayName(node) + " (" + entityId + ")")
            .orElse(entityId);
    }

    private static void detail(StringBuilder text, String key, Object value) {
        text.append(key).append(": ").append(String.valueOf(value)).append('\n');
    }

    private static void appendProperties(StringBuilder text, Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) return;
        text.append("\nMetadata\n");
        properties.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> detail(text, entry.getKey(), entry.getValue()));
    }

    private Callout makeCallout(Shape3D shape, String title, String details, Runnable onClose) {
        Point2D point = JavaFX3DUtils.getTransformedP2D(shape, subScene, Callout.DEFAULT_HEAD_RADIUS + 5);
        TextArea detailArea = new TextArea(details);
        detailArea.setEditable(false);
        detailArea.setWrapText(true);
        detailArea.setPrefColumnCount(40);
        detailArea.setPrefRowCount(11);
        detailArea.setPrefWidth(360);
        detailArea.setPrefHeight(240);
        detailArea.setMaxWidth(360);
        detailArea.setMaxHeight(240);
        Label note = new Label("Double-click the title to close. Details can be selected and copied.");
        note.setWrapText(true);
        note.setMaxWidth(360);
        VBox content = new VBox(6, detailArea, note);
        Callout result = CalloutBuilder.create()
            .headPoint(point.getX(), point.getY())
            .leaderLineToPoint(point.getX() - 95, point.getY() - 120)
            .endLeaderLineRight()
            .mainTitle(title, content)
            .pause(10)
            .build();
        // Do not close when the analyst double-clicks inside the editable-looking
        // read-only metadata field to select words. Only the callout title closes it.
        result.mainTitleHBox.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            boolean titleHit = event.getTarget() == result.mainTitleTextNode
                || event.getTarget() == result.mainTitleHBox;
            if (titleHit && event.getButton() == MouseButton.PRIMARY
                && event.getClickCount() >= 2) {
                onClose.run();
                event.consume();
            }
        });
        // The generic Callout 'Close This' only calls hide(). Our animation timer
        // would make it visible on the next pulse, so unregister it instead.
        for (MenuItem item : result.contextMenu.getItems()) {
            if ("Close This".equals(item.getText())) {
                item.setOnAction(event -> onClose.run());
            }
        }
        result.setPickOnBounds(false);
        result.setManaged(false);
        selectionOverlay.addCallout(result, shape);
        result.play();
        return result;
    }

    /** Percentage of viewport width and height available to the fitted graph. */
    public double getFitGraphPercentage() {
        return fitGraphPercentage;
    }

    public void setFitGraphPercentage(double percentage) {
        if (!Double.isFinite(percentage) || percentage < 10.0 || percentage > 100.0) return;
        fitGraphPercentage = percentage;
    }

    private void frameGraph(edu.jhuapl.trinity.data.graph.GraphDirectedCollection graph) {
        if (graph == null) return;
        if (graphController.getProjectionMode().isHyperbolic()) {
            frameHyperbolicBall();
        } else frameNodes(graph.getNodes());
    }

    /** Fit the complete Poincare chart without changing its geometric origin. */
    private void frameHyperbolicBall() {
        double radius = graphController.getHyperbolicDisplayRadius();
        double width = Math.max(1.0, subScene.getWidth());
        double height = Math.max(1.0, subScene.getHeight());
        double aspect = width / height;
        double tangent = Math.tan(Math.toRadians(camera.getFieldOfView()) / 2.0);
        double verticalTangent = camera.isVerticalFieldOfView() ? tangent : tangent / aspect;
        double horizontalTangent = camera.isVerticalFieldOfView() ? tangent * aspect : tangent;
        double limitingTangent = Math.max(1.0e-3, Math.min(verticalTangent, horizontalTangent));
        double distance = radius + radius / (limitingTangent * fitGraphPercentage / 100.0)
            + camera.getNearClip() + 1.0;
        cameraTransform.setTranslate(0.0, 0.0, 0.0);
        camera.setTranslateX(0.0);
        camera.setTranslateY(0.0);
        camera.setTranslateZ(-distance);
    }

    private void frameNodes(List<GraphNode> nodes) {
        if (nodes == null || nodes.isEmpty()) return;

        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (GraphNode node : nodes) {
            if (node == null) continue;
            double[] p = graphController.getDisplayedPosition(node);
            if (p == null || p.length < 3 || !Double.isFinite(p[0])
                || !Double.isFinite(p[1]) || !Double.isFinite(p[2])) continue;
            minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
            minY = Math.min(minY, p[1]); maxY = Math.max(maxY, p[1]);
            minZ = Math.min(minZ, p[2]); maxZ = Math.max(maxZ, p[2]);
        }
        if (!Double.isFinite(minX)) return;

        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;
        double cz = (minZ + maxZ) / 2.0;
        cameraTransform.setTranslate(cx, cy, cz);
        camera.setTranslateX(0);
        camera.setTranslateY(0);

        double width = Math.max(1.0, subScene.getWidth());
        double height = Math.max(1.0, subScene.getHeight());
        double aspect = width / height;
        double halfFov = Math.toRadians(camera.getFieldOfView()) / 2.0;
        double tanVertical = camera.isVerticalFieldOfView()
            ? Math.tan(halfFov) : Math.tan(halfFov) / aspect;
        double tanHorizontal = camera.isVerticalFieldOfView()
            ? Math.tan(halfFov) * aspect : Math.tan(halfFov);
        double occupancy = fitGraphPercentage / 100.0;
        double requiredDistance = camera.getNearClip() + 1.0;

        // Rotate each bounding-box corner into camera-local coordinates.
        // Camera local +Z is forward; its position is at local Z = -distance.
        // For a corner at (x,y,z), depth is distance + z.
        try {
            var inverse = cameraTransform.getLocalToParentTransform().createInverse();
            for (double x : new double[]{minX, maxX}) {
                for (double y : new double[]{minY, maxY}) {
                    for (double z : new double[]{minZ, maxZ}) {
                        Point3D local = inverse.deltaTransform(x - cx, y - cy, z - cz);
                        requiredDistance = Math.max(requiredDistance,
                            Math.abs(local.getX()) / (tanHorizontal * occupancy) - local.getZ());
                        requiredDistance = Math.max(requiredDistance,
                            Math.abs(local.getY()) / (tanVertical * occupancy) - local.getZ());
                        requiredDistance = Math.max(requiredDistance,
                            camera.getNearClip() + 1.0 - local.getZ());
                    }
                }
            }
        } catch (NonInvertibleTransformException ex) {
            double diagonal = Math.sqrt(Math.pow(maxX - minX, 2)
                + Math.pow(maxY - minY, 2) + Math.pow(maxZ - minZ, 2));
            requiredDistance = Math.max(requiredDistance, diagonal * 2.5);
        }
        camera.setTranslateZ(-requiredDistance);
    }

    public ContextGraphController getGraphController() {
        return graphController;
    }
}
