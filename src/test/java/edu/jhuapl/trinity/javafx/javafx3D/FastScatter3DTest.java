/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.javafx3d;

import edu.jhuapl.trinity.javafx.javafx3d.animated.FastScatter3D;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import org.fxyz3d.utils.CameraTransformer;

import java.util.ArrayList;
import java.util.Random;

public class FastScatter3DTest extends Application {
    PerspectiveCamera camera = new PerspectiveCamera(true);
    public Group sceneRoot = new Group();
    public SubScene subScene;
    public CameraTransformer cameraTransform = new CameraTransformer();
    private double cameraDistance = -500;
    private final double sceneWidth = 4000;
    private final double sceneHeight = 4000;

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    FastScatter3D fastScatter3D;
    ArrayList<Point3D> positions;
    Random rando = new Random();
    double scale = 100;
    long hyperspaceRefreshRate = 15; //milliseconds
    int totalPoints = 25000;
    double radius = 0.5;
    int divisions = 8;

    @Override
    public void start(Stage primaryStage) throws Exception {

        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        //Start Tracking mouse movements only when a button is pressed
        subScene.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
        subScene.setOnMouseDragged((MouseEvent me) -> mouseDragCamera(me));
        subScene.setOnScroll((ScrollEvent event) -> {
            double modifier = 2.0;
            double modifierFactor = 0.1;

            if (event.isControlDown()) {
                modifier = 1;
            }
            if (event.isShiftDown()) {
                modifier = 50.0;
            }
            double z = camera.getTranslateZ();
            double newZ = z + event.getDeltaY() * modifierFactor * modifier;
            camera.setTranslateZ(newZ);
        });
        StackPane stackPane = new StackPane(subScene);
        subScene.widthProperty().bind(stackPane.widthProperty());
        subScene.heightProperty().bind(stackPane.heightProperty());
        subScene.setFill(Color.BLACK);

        camera = new PerspectiveCamera(true);
        //setup camera transform for rotational support
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
        cameraTransform.ry.setAngle(-45.0);
        cameraTransform.rx.setAngle(-10.0);
        subScene.setCamera(camera);

        AmbientLight ambientLight = new AmbientLight(Color.WHITE);

        fastScatter3D = new FastScatter3D(totalPoints, radius, divisions);
        fastScatter3D.setAllVisible(true);
        int pointCount = fastScatter3D.getPointCount();
        positions = new ArrayList<>(pointCount);
        //generate some random positions
        for (int i = 0; i < pointCount; i++) {
            positions.add(new Point3D(
                rando.nextDouble() * scale,
                rando.nextDouble() * scale,
                rando.nextDouble() * scale));
        }
        fastScatter3D.updatePositionsList(positions);

        Sphere sphereX = new Sphere(5);
        sphereX.setTranslateX(scale);
        sphereX.setMaterial(new PhongMaterial(Color.RED));

        Sphere sphereY = new Sphere(5);
        sphereY.setTranslateY(-scale);
        sphereY.setMaterial(new PhongMaterial(Color.GREEN));

        Sphere sphereZ = new Sphere(5);
        sphereZ.setTranslateZ(scale);
        sphereZ.setMaterial(new PhongMaterial(Color.BLUE));

        sceneRoot.getChildren().addAll(cameraTransform, ambientLight,
            sphereX, sphereY, sphereZ, fastScatter3D);

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

            if (keycode == KeyCode.SPACE) {
                camera.setTranslateY(camera.getTranslateY() - change);
            }
            if (keycode == KeyCode.C) {
                camera.setTranslateY(camera.getTranslateY() + change);
            }

            change = event.isShiftDown() ? 10.0 : 1.0;
            Point3D p3D = fastScatter3D.getCenterPoint();
            double x = p3D.getX();
            double y = p3D.getY();
            double z = p3D.getZ();
            boolean moved = false;
            if (keycode == KeyCode.UP && event.isAltDown()) {
                z += change;
                moved = true;
            }
            if (keycode == KeyCode.DOWN && event.isAltDown()) {
                z -= change;
                moved = true;
            }

            if (keycode == KeyCode.LEFT) {
                x += change;
                moved = true;
            }
            if (keycode == KeyCode.RIGHT) {
                x -= change;
                moved = true;
            }

            if (keycode == KeyCode.UP && !event.isAltDown()) {
                y -= change;
                moved = true;
            }
            if (keycode == KeyCode.DOWN && !event.isAltDown()) {
                y += change;
                moved = true;
            }
            if (moved)
                fastScatter3D.setCenterPoint(new Point3D(x, y, z));

        });

        BorderPane bpOilSpill = new BorderPane(subScene);
        stackPane.getChildren().clear();
        stackPane.getChildren().addAll(bpOilSpill);
        stackPane.setPadding(new Insets(10));
        stackPane.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255), CornerRadii.EMPTY, Insets.EMPTY)));
        Scene scene = new Scene(stackPane, 1000, 1000);
        scene.setOnMouseEntered(event -> subScene.requestFocus());

        primaryStage.setTitle("FastScatter3D Test");
        primaryStage.setScene(scene);
        primaryStage.show();

        AnimationTimer animationTimer = new AnimationTimer() {
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_MILLI = 1_000_000;

            @Override
            public void handle(long now) {
                sleepNs = hyperspaceRefreshRate * NANOS_IN_MILLI;
                if ((now - prevTime) < sleepNs) return;
                prevTime = now;
                updatePositions();
            }

            ;
        };
        animationTimer.start();
    }

    //generate some random positions
    private void updatePositions() {
        int pointCount = fastScatter3D.getPointCount();
        for (int i = 0; i < pointCount; i++) {
            positions.set(i, new Point3D(
                rando.nextDouble() * scale,
                rando.nextDouble() * scale,
                rando.nextDouble() * scale));
        }
        fastScatter3D.updatePositionsList(positions);
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
                cameraTransform.rz.setAngle(
                    ((cameraTransform.rz.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
            } else {
                cameraTransform.ry.setAngle(
                    ((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
                cameraTransform.rx.setAngle(
                    ((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            }
        } else if (me.isMiddleButtonDown()) {
            cameraTransform.t.setX(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3); // -
            cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3); // -
        }
    }

    public static void main(String[] args) {
        Application.launch(FastScatter3DTest.class, args);
    }
}
