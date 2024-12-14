/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.graph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.javafx.javafx3d.animated.AnimatedSphere;
import edu.jhuapl.trinity.javafx.javafx3d.animated.Tracer;
import static edu.jhuapl.trinity.utils.JavaFX3DUtils.getGraphNodePoint3D;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.AnimationTimer;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxyz3d.geometry.MathUtils;
import org.fxyz3d.utils.CameraTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.PointLight;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import org.fxyz3d.geometry.Point3D;

public class GraphDirectedTest extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(GraphDirectedTest.class);
    PerspectiveCamera camera = new PerspectiveCamera(true);
    public Group sceneRoot = new Group();
    public SubScene subScene;
    public CameraTransformer cameraTransform = new CameraTransformer();
    private double cameraDistance = -2000;
    private final double sceneWidth = 10000;
    private final double sceneHeight = 4000;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    GraphDirectedCollection graphDirectedCollection = null;

    ArrayList<AnimatedSphere> nodes;
    ArrayList<Tracer> edges;

    List<ParallelTransition> transitionList = new ArrayList<>();
    double transitionXOffset = -2000;
    double transitionYOffset = -2000;
    double transitionZOffset = 0;
    double originRadius = 2300;
    double positionScalar = 1.0;
    double defaultRadius = 20;
    float defaultEdgeWidth = 10;
    int defaultDivisions = 64;    
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        //Start Tracking mouse movements only when a button is pressed
        subScene.setOnMousePressed((MouseEvent me) -> {
            if (me.isSynthesized())
                LOG.info("isSynthesized");
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
        subScene.setOnMouseDragged((MouseEvent me) -> mouseDragCamera(me));
        subScene.setOnScroll((ScrollEvent event) -> {
            double modifier = 50.0;
            double modifierFactor = 0.1;

            if (event.isControlDown()) {
                modifier = 1;
            }
            if (event.isShiftDown()) {
                modifier = 100.0;
            }
            double z = camera.getTranslateZ();
            double newZ = z + event.getDeltaY() * modifierFactor * modifier;
            camera.setTranslateZ(newZ);
        });
        subScene.setOnKeyPressed(event -> {
            //What key did the user press?
            KeyCode keycode = event.getCode();

            double change = 10.0;
            //Add shift modifier to simulate "Running Speed"
            if (event.isShiftDown()) {
                change = 100.0;
            }
            //Zoom controls
            if (keycode == KeyCode.W) {
                camera.setTranslateZ(camera.getTranslateZ() + change);
            }
            if (keycode == KeyCode.S) {
                camera.setTranslateZ(camera.getTranslateZ() - change);
            }
            //Strafe controls
            if (keycode == KeyCode.A) {
                camera.setTranslateX(camera.getTranslateX() - change);
            }
            if (keycode == KeyCode.D) {
                camera.setTranslateX(camera.getTranslateX() + change);
            }
            //jump controls
            if (keycode == KeyCode.SPACE) {
                camera.setTranslateY(camera.getTranslateY() - change);
            }
            if (keycode == KeyCode.C) {
                camera.setTranslateY(camera.getTranslateY() + change);
            }

        });
        StackPane stackPane = new StackPane(subScene);
        subScene.widthProperty().bind(stackPane.widthProperty());
        subScene.heightProperty().bind(stackPane.heightProperty());
        subScene.setFill(Color.BLACK);

        camera = new PerspectiveCamera(true);
        //setup camera transform for rotational support
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        //add a Point Light for better viewing of the grid coordinate system
        PointLight light = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(light);
        light.setTranslateX(camera.getTranslateX());
        light.setTranslateY(camera.getTranslateY());
        light.setTranslateZ(camera.getTranslateZ());
        light.translateXProperty().bind(camera.translateXProperty());
        light.translateYProperty().bind(camera.translateYProperty());
        light.translateZProperty().bind(camera.translateZProperty());
//        light.setMaxRange(100);
               
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
//        cameraTransform.ry.setAngle(-45.0);
//        cameraTransform.rx.setAngle(-10.0);
        subScene.setCamera(camera);
        sceneRoot.getChildren().addAll(cameraTransform);
        ContextMenu cm = new ContextMenu();
        MenuItem animateItem = new MenuItem("Animate");
        animateItem.setOnAction(e -> animate());

        cm.getItems().addAll(animateItem);
        cm.setAutoFix(true);
        cm.setAutoHide(true);
        cm.setHideOnEscape(true);
        cm.setOpacity(0.85);
        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (!cm.isShowing())
                    cm.show(subScene.getParent(), e.getScreenX(), e.getScreenY());
                else
                    cm.hide();
                e.consume();
            }
        });

        BorderPane bpOilSpill = new BorderPane(stackPane);
        bpOilSpill.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

        bpOilSpill.setLeft(makeGraphControls());
        Scene scene = new Scene(bpOilSpill, 1024, 768, Color.BLACK);
        
        scene.setOnMouseEntered(event -> subScene.requestFocus());
        scene.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event))
                event.acceptTransferModes(TransferMode.COPY);
            else
                event.consume();
        });
        scene.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            ResourceUtils.onDragDropped(event, scene);
        });
        scene.getRoot().addEventHandler(GraphEvent.NEW_GRAPHDIRECTED_COLLECTION, e-> {
            graphDirectedCollection = (GraphDirectedCollection)e.object;
            clearAll();
            generateGraph(graphDirectedCollection);
            //animate();
        });        
        
        String CSS = this.getClass().getResource("/edu/jhuapl/trinity/css/styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        
        primaryStage.setTitle("3D Graph Demonstration");
        primaryStage.setScene(scene);
        primaryStage.show();

        Sphere origin = new Sphere(10.0);
        Sphere northPole = new Sphere(20.0);
        PhongMaterial northPhong = new PhongMaterial(Color.CYAN);
        northPole.setMaterial(northPhong);
        northPole.setTranslateY(-originRadius);

        Sphere southPole = new Sphere(20.0);
        PhongMaterial southPhong = new PhongMaterial(Color.TOMATO);
        southPole.setMaterial(southPhong);
        southPole.setTranslateY(originRadius);

        sceneRoot.getChildren().addAll(origin, northPole, southPole);
    }
    private VBox makeGraphControls() {
        Spinner<Double> scalarSpinner = new Spinner(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(-200.0, 200.0, 1.0, 10.0));
        scalarSpinner.setEditable(true);
        //whenever the spinner value is changed...
        scalarSpinner.valueProperty().addListener(e -> {
            positionScalar = scalarSpinner.getValue();
            clearAll();
            generateGraph(graphDirectedCollection);
        });
        scalarSpinner.setRepeatDelay(Duration.millis(64));
        scalarSpinner.setMinWidth(100);        

        Spinner<Double> radiusSpinner = new Spinner(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5, 50.0, defaultRadius, 0.5));
        radiusSpinner.setEditable(true);
        radiusSpinner.valueProperty().addListener(e -> {
            defaultRadius = radiusSpinner.getValue();
            nodes.forEach(n -> n.setRadius(defaultRadius));
        });
        radiusSpinner.setRepeatDelay(Duration.millis(64));
        radiusSpinner.setMinWidth(100);

        // Edge Width /////////////////////////////////
        Spinner<Double> edgeWidthSpinner = new Spinner(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5, 50.0, defaultEdgeWidth, 0.5));
        edgeWidthSpinner.setEditable(true);
        edgeWidthSpinner.valueProperty().addListener(e -> {
            defaultEdgeWidth = edgeWidthSpinner.getValue().floatValue();
            if(null != graphDirectedCollection) {
                sceneRoot.getChildren().removeIf(c -> c instanceof Tracer);
                edges = generateEdges(graphDirectedCollection);
                sceneRoot.getChildren().addAll(edges);            
            }
       });
        edgeWidthSpinner.setRepeatDelay(Duration.millis(64));
        edgeWidthSpinner.setMinWidth(100);

        
        VBox vbox = new VBox(10,
            new VBox(new Label("Node Radius"), radiusSpinner),
            new VBox(new Label("Edge Width"), edgeWidthSpinner),
            new VBox(new Label("Position Scaling"), scalarSpinner)
        );
        return vbox;        
    }
    private void clearAll() {
        sceneRoot.getChildren().removeIf(c -> c instanceof AnimatedSphere);
        sceneRoot.getChildren().removeIf(c -> c instanceof Tracer);
        transitionList.clear();        
    }
    private void generateGraph(GraphDirectedCollection graph) {
        if(null == graph) return;
        nodes = generateNodes(graph);
        sceneRoot.getChildren().addAll(nodes);

        edges = generateEdges(graph);
        sceneRoot.getChildren().addAll(edges);

        nodes.forEach(n -> transitionList.add(createTransition(n)));
        edges.forEach(n -> transitionList.add(createVisibilityTransition(n)));
        
    }
    private ArrayList<Tracer> generateEdges(GraphDirectedCollection graph) {
        ArrayList<Tracer> newEdges = new ArrayList<>();
        Color defaultDiffuseColor = Color.ALICEBLUE;
        if(null != graph.getDefaultEdgeColor()) {
            defaultDiffuseColor = Color.valueOf(graph.getDefaultEdgeColor());
        }
        final Color defaultColor = defaultDiffuseColor;
        
        graph.getEdges().forEach(edge -> {
            Optional<GraphNode> startNodeOpt = graph.findNodeById(edge.getStartID());
            Optional<GraphNode> endNodeOpt = graph.findNodeById(edge.getEndID());
            if(startNodeOpt.isPresent() && endNodeOpt.isPresent()) {
                Point3D startP3D = getGraphNodePoint3D(startNodeOpt.get(), positionScalar);
                Point3D endP3D = getGraphNodePoint3D(endNodeOpt.get(), positionScalar);
                Tracer tracer = new Tracer(startP3D, endP3D, defaultEdgeWidth, 
                    null != edge.getColor() ? Color.valueOf(edge.getColor()) : defaultColor);
                newEdges.add(tracer);
            }
        });
        return newEdges;
    }

    private ArrayList<AnimatedSphere> generateNodes(GraphDirectedCollection graph) {
        ArrayList<AnimatedSphere> newNodes = new ArrayList<>();
        Color defaultDiffuseColor = Color.CYAN;
        if(null != graph.getDefaultNodeColor()) {
            defaultDiffuseColor = Color.valueOf(graph.getDefaultNodeColor());
        }
        final Color defaultColor = defaultDiffuseColor;
        
        graph.getNodes().forEach(gN -> {
            PhongMaterial material;
            if(null != gN.getColor()) {
                material = new PhongMaterial(Color.valueOf(gN.getColor()));
            } else {
                material = new PhongMaterial(defaultColor);
            }
            AnimatedSphere sphere = new AnimatedSphere(
                material, defaultRadius, defaultDivisions, true);
            Point3D p3d = getGraphNodePoint3D(gN, positionScalar);
            sphere.setTranslateX(p3d.getX());
            sphere.setTranslateY(p3d.getY());
            sphere.setTranslateZ(p3d.getZ());  
            newNodes.add(sphere);
        });
        return newNodes;
    }

    public void animate() {
        nodes.forEach(n -> n.setVisible(false));
        edges.forEach(n -> n.setVisible(false));
        AnimationTimer timer = createAnimation();
        timer.start();
    }

    private AnimationTimer createAnimation() {
        AnimationTimer timer = new AnimationTimer() {
            long last = 0;
            int transitionIndex = 0;

            @Override
            public void handle(long now) {
                if ((now - last) > 16_000_000) {
                    if (transitionIndex < transitionList.size()) {
                        ParallelTransition t = transitionList.get(transitionIndex);
                        t.getNode().setVisible(true);
                        t.play();
                        transitionIndex++;
                    }
                    last = now;
                }
                if (transitionIndex >= transitionList.size()) {
                    stop();
                }
            }
        };

        return timer;
    }

    private ParallelTransition createVisibilityTransition(final Node node) {
        Duration duration = Duration.millis(100);
        FadeTransition ft  = new FadeTransition(duration, node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ScaleTransition st = new ScaleTransition(duration, node);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setFromZ(1.0);
        st.setToX(2.0);
        st.setToY(2.0);
        st.setToZ(2.0);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        RotateTransition rt = new RotateTransition(duration, node);
        rt.setByAngle(720);
        rt.setAutoReverse(true);
        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.setNode(node);
        parallelTransition.getChildren().addAll(st);
        return parallelTransition;
    }

    private ParallelTransition createTransition(final Node node) {
        Path path = new Path();
        path.getElements().add(new MoveToAbs(node,
            node.getTranslateX() + transitionXOffset,
            node.getTranslateY() + transitionYOffset));
        path.getElements().add(new LineToAbs(node, 
            node.getTranslateX(), 
            node.getTranslateY()));

        Duration duration = Duration.millis(500);
        PathTransition pt = new PathTransition(duration, path, node);
        RotateTransition rt = new RotateTransition(duration, node);
        rt.setByAngle(720);
        rt.setAutoReverse(true);
        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.setNode(node);
        parallelTransition.getChildren().addAll(pt); //, rt);
        return parallelTransition;
    }

    private void mouseDragCamera(MouseEvent me) {
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);
        double modifier = 1.0;
        double modifierFactor = 0.1;

        if (me.isControlDown()) {
            modifier = 0.1;
        }
        if (me.isShiftDown()) {
            modifier = 10.0;
        }
        if (me.isPrimaryButtonDown()) {
            if (me.isAltDown()) { //roll
                cameraTransform.rz.setAngle(((cameraTransform.rz.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
            } else {
                cameraTransform.ry.setAngle(((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
                cameraTransform.rx.setAngle(
                    MathUtils.clamp(-60,
                        (((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180),
                        60)); // -
            }
        } else if (me.isMiddleButtonDown()) {
            cameraTransform.t.setX(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3); // -
            cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3); // -
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static class MoveToAbs extends MoveTo {

        public MoveToAbs(Node node, double x, double y) {
            super(x - node.getLayoutX() + node.getLayoutBounds().getWidth() / 2,
                y - node.getLayoutY() + node.getLayoutBounds().getHeight() / 2);
        }
    }

    public static class LineToAbs extends LineTo {
        public LineToAbs(Node node, double x, double y) {
            super(x,y);
        }
    }
}
