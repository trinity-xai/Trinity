package edu.jhuapl.trinity.javafx.javafx3D;

import edu.jhuapl.trinity.javafx.javafx3d.animated.RadialGrid;
import edu.jhuapl.trinity.utils.Utils;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * @author Sean Phillips
 */
public class RadialGridTest extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(RadialGridTest.class);
    private final double sceneWidth = 800;
    private final double sceneHeight = 800;
    Scene scene;
    private double mouseOldX, mouseOldY;
    private final Rotate rotateX = new Rotate(-30, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(-30, Rotate.Y_AXIS);
    private final Translate cameraTranslate = new Translate(0, -50, -500);

    private static final int NUM_CIRCLES = 10;
    private static final int NUM_RADIAL_LINES = 24;
    private static final double MAX_RADIUS = 200;
    private static final double LINE_RADIUS = 0.2;
    private static final double CIRCLE_SEGMENTS = 90;
    RadialGrid radialGrid;
    Group sceneRoot;
    Spinner<Integer> numCirclesSpinner;
    Spinner<Integer> numLinesSpinner;
    Spinner<Double> maxRadiusSpinner;
    Spinner<Double> lineRadiusSpinner;
    Spinner<Double> circleSegmentsSpinner;    
    
    @Override
    public void start(Stage primaryStage) {
         sceneRoot = new Group();
         radialGrid = new RadialGrid(NUM_CIRCLES, NUM_RADIAL_LINES, 
            MAX_RADIUS, LINE_RADIUS, CIRCLE_SEGMENTS);
         sceneRoot.getChildren().add(radialGrid);
         // Camera
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.getTransforms().addAll(rotateX, rotateY, cameraTranslate);
        
        SubScene subScene = new SubScene(sceneRoot, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setWidth(sceneWidth);
        subScene.setHeight(sceneHeight);
        subScene.setFill(Color.BLACK);
        subScene.setCamera(camera);
        Pane subScenePane = new Pane(subScene);
        subScene.widthProperty().bind(subScenePane.widthProperty());
        subScene.heightProperty().bind(subScenePane.heightProperty());
        
        // Mouse controls
        handleMouse(subScene);
        ToggleButton animateToggle = new ToggleButton("Animation");
        animateToggle.setOnAction(e -> 
            radialGrid.setEnableAnimation(animateToggle.isSelected()));
        ToggleButton pulseToggle = new ToggleButton("Pulse");
        pulseToggle.setOnAction(e -> 
            radialGrid.setEnablePulsation(pulseToggle.isSelected()));
        ToggleButton rotateToggle = new ToggleButton("Rotate");
        rotateToggle.setOnAction(e -> 
            radialGrid.setEnableRotation(rotateToggle.isSelected()));

        numCirclesSpinner = new Spinner(2, 200, NUM_CIRCLES, 1);
        numCirclesSpinner.valueProperty().addListener(e->regenerate());
        numLinesSpinner = new Spinner(2, 200, NUM_RADIAL_LINES, 1);
        numLinesSpinner.valueProperty().addListener(e->regenerate());
        maxRadiusSpinner = new Spinner(1, 10000, MAX_RADIUS, 100);
        maxRadiusSpinner.valueProperty().addListener(e->regenerate());
        lineRadiusSpinner = new Spinner(0.1, 100, LINE_RADIUS, 0.1);
        lineRadiusSpinner.valueProperty().addListener(e->regenerate());
        circleSegmentsSpinner = new Spinner(1, 360, CIRCLE_SEGMENTS, 1);
        circleSegmentsSpinner.valueProperty().addListener(e->regenerate());
        
        Button generateButton = new Button("Regenerate");
        generateButton.setOnAction(e -> regenerate());
        
        ColorPicker diffuseColorPicker = new ColorPicker(Color.DEEPSKYBLUE);
        diffuseColorPicker.valueProperty().bindBidirectional(radialGrid.gridMaterial.diffuseColorProperty());
        ColorPicker specularColorPicker = new ColorPicker(Color.LIGHTCYAN);
        specularColorPicker.valueProperty().bindBidirectional(radialGrid.gridMaterial.specularColorProperty());
        
        VBox vbox = new VBox(10, 
            animateToggle,
            pulseToggle,
            rotateToggle,
            new Label("Number of Circles"),
            numCirclesSpinner,
            new Label("Number of Lines"),
            numLinesSpinner,
            new Label("Max Radius"),
            maxRadiusSpinner,
            new Label("Line Radius"),
            lineRadiusSpinner,
            new Label("Circle Segment Arc"),
            circleSegmentsSpinner,
            generateButton,
            new Label("Diffuse Color"),
            diffuseColorPicker,
            new Label("Specular Color"),
            specularColorPicker
        );
        BorderPane root = new BorderPane(subScenePane);
        root.setLeft(vbox);
        root.setMinSize(800,800);
        Scene scene = new Scene(root);

        primaryStage.setTitle("3D Radial Grid with Camera Controls");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
private void regenerate() {
    long startTime = System.nanoTime();
    radialGrid.regenerate(numCirclesSpinner.getValue(), 
        numLinesSpinner.getValue(), maxRadiusSpinner.getValue(), 
        lineRadiusSpinner.getValue(), circleSegmentsSpinner.getValue());
    Utils.printTotalTime(startTime);
}

    private void handleMouse(SubScene scene) {
        scene.setOnMousePressed((MouseEvent event) -> {
            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();
        });

        scene.setOnMouseDragged((MouseEvent event) -> {
            double dx = event.getSceneX() - mouseOldX;
            double dy = event.getSceneY() - mouseOldY;

            rotateY.setAngle(rotateY.getAngle() + dx * 0.3);
            rotateX.setAngle(rotateX.getAngle() - dy * 0.3);

            mouseOldX = event.getSceneX();
            mouseOldY = event.getSceneY();
        });

        scene.addEventHandler(ScrollEvent.SCROLL, event -> {
            double delta = event.getDeltaY();
            cameraTranslate.setZ(cameraTranslate.getZ() + delta * 0.2);
        });
    }
}
