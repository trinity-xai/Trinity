package edu.jhuapl.trinity.javafx.javafx3d.animated;

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
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
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
    RadialGridControlBox controls;
    Group sceneRoot;
    
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
        controls = new RadialGridControlBox(radialGrid);
        BorderPane root = new BorderPane(subScenePane);
        root.setLeft(controls);
        root.setMinSize(800,800);
        Scene scene = new Scene(root);

        primaryStage.setTitle("3D Radial Grid with Camera Controls");
        primaryStage.setScene(scene);
        primaryStage.show();
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
