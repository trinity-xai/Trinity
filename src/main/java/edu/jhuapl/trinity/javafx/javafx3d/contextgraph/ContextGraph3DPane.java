package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.javafx.components.panes.ContextGraphControlsPane;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.CheckMenuItem;

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
    private final CameraTransformer cameraTransform = new CameraTransformer();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene subScene;
    private double fitGraphPercentage = 85.0;
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
        interactionController = new ContextGraphInteractionController(
            scene, graphController);
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
        getChildren().add(subScene);
        ContextMenu graphMenu = new ContextMenu();
        MenuItem controlsItem = new MenuItem("Context Graph Controls");
        controlsItem.setOnAction(e -> showGraphControls(scene));
        MenuItem fitItem = new MenuItem("Fit Graph");
        fitItem.setOnAction(e -> frameGraph(graphController.getCurrentGraph()));
        MenuItem navigatorItem = new MenuItem("Content Navigator");
        navigatorItem.setOnAction(e -> scene.getRoot().fireEvent(
            new ApplicationEvent(ApplicationEvent.SHOW_NAVIGATOR_PANE)));
        CheckMenuItem diffuseTestItem = new CheckMenuItem("Diagnostic: Opaque Diffuse Materials");
        diffuseTestItem.setOnAction(e -> graphController.setDiagnosticDiffuseMaterials(diffuseTestItem.isSelected()));
        graphMenu.getItems().addAll(controlsItem, navigatorItem, fitItem, diffuseTestItem);
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
                scene.getRoot().fireEvent(new GraphEvent(GraphEvent.GRAPH_NODE_CLICK, node));
            } else if (edge != null) {
                graphController.toggleEdgeSelection(edge);
                scene.getRoot().fireEvent(new GraphEvent(GraphEvent.GRAPH_EDGE_CLICK, edge));
            } else {
                graphController.clearSelections();
            }
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
                graphController.loadGraph(graph);
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

    /** Percentage of viewport width and height available to the fitted graph. */
    public double getFitGraphPercentage() {
        return fitGraphPercentage;
    }

    public void setFitGraphPercentage(double percentage) {
        if (!Double.isFinite(percentage) || percentage < 10.0 || percentage > 100.0) return;
        fitGraphPercentage = percentage;
    }

    private void frameGraph(edu.jhuapl.trinity.data.graph.GraphDirectedCollection graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) return;

        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (GraphNode node : graph.getNodes()) {
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
