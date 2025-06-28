package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
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
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxyz3d.utils.CameraTransformer;

public class LookAtTest extends Application {
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
        subScene.setOnKeyPressed(me -> {
            if (me.isControlDown() && me.getCode() == KeyCode.R) {
                resetCamera(me.isShiftDown());
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
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
        cameraTransform.ry.setAngle(-45.0);
        cameraTransform.rx.setAngle(-10.0);
        subScene.setCamera(camera);

        AmbientLight ambientLight = new AmbientLight(Color.WHITE);

        Box center = new Box(2, 2, 2);
        center.setMaterial(new PhongMaterial(Color.AQUA));

        Sphere body = new Sphere(10);
        Box pointer = new Box(2, 2, 20);
        pointer.setMaterial(new PhongMaterial(Color.TOMATO));
        pointer.setTranslateZ(10);
        Group pointyGroup = new Group(body, pointer);

        pointyGroup.setTranslateX(-100);
        //@SMP IF we translate the group we have to inform lookAt() to not
        //apply the translation coordinates in the resulting Affine transform

        Sphere sphereX = new Sphere(5);
        sphereX.setTranslateX(100);
        sphereX.setMaterial(new PhongMaterial(Color.RED));
        sphereX.setOnMouseClicked(e -> {
            Point3D currentPoint3D = new Point3D(
                pointyGroup.getTranslateX(),
                pointyGroup.getTranslateY(),
                pointyGroup.getTranslateZ());
            Point3D lookAtPoint3D = new Point3D(
                sphereX.getTranslateX(),
                sphereX.getTranslateY(),
                sphereX.getTranslateZ());
            if (e.isControlDown()) {
                orbitAt(camera, cameraTransform, lookAtPoint3D, e.isShiftDown());
            } else
                lookAt(pointyGroup, currentPoint3D, lookAtPoint3D, false);
        });

        Sphere sphereY = new Sphere(5);
        sphereY.setTranslateY(-100);
        sphereY.setMaterial(new PhongMaterial(Color.GREEN));
        sphereY.setOnMouseClicked(e -> {
            Point3D currentPoint3D = new Point3D(
                pointyGroup.getTranslateX(),
                pointyGroup.getTranslateY(),
                pointyGroup.getTranslateZ());
            Point3D lookAtPoint3D = new Point3D(
                sphereY.getTranslateX(),
                sphereY.getTranslateY(),
                sphereY.getTranslateZ());
            if (e.isControlDown()) {
                orbitAt(camera, cameraTransform, lookAtPoint3D, e.isShiftDown());
            } else
                lookAt(pointyGroup, currentPoint3D, lookAtPoint3D, false);
        });


        Sphere sphereZ = new Sphere(5);
        sphereZ.setTranslateZ(100);
        sphereZ.setMaterial(new PhongMaterial(Color.BLUE));
        sphereZ.setOnMouseClicked(e -> {
            Point3D currentPoint3D = new Point3D(
                pointyGroup.getTranslateX(),
                pointyGroup.getTranslateY(),
                pointyGroup.getTranslateZ());
            Point3D lookAtPoint3D = new Point3D(
                sphereZ.getTranslateX(),
                sphereZ.getTranslateY(),
                sphereZ.getTranslateZ());
            if (e.isControlDown()) {
                orbitAt(camera, cameraTransform, lookAtPoint3D, e.isShiftDown());
            } else
                lookAt(pointyGroup, currentPoint3D, lookAtPoint3D, false);
        });

        sceneRoot.getChildren().addAll(cameraTransform, ambientLight,
            center, sphereX, sphereY, sphereZ, pointyGroup);

        BorderPane bpOilSpill = new BorderPane(subScene);
        stackPane.getChildren().clear();
        stackPane.getChildren().addAll(bpOilSpill);
        stackPane.setPadding(new Insets(10));
        stackPane.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255), CornerRadii.EMPTY, Insets.EMPTY)));
        Scene scene = new Scene(stackPane, 1000, 1000);
        scene.setOnMouseEntered(event -> subScene.requestFocus());

        primaryStage.setTitle("Multiple SpotLight Test");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public void resetCamera(boolean animate) {
//        cameraTransform.setPivot(0, 0, 0);
        cameraTransform.setTranslate(0, 0, 0);
        orbitAt(camera, cameraTransform, Point3D.ZERO, animate);
    }

    public static void orbitAt(Camera camera, CameraTransformer cameraTransform, Point3D at, boolean animate) {
        double currentZ = camera.getTranslateZ();

        if (!animate) {
            cameraTransform.setPivot(at.getX(), at.getY(), at.getZ());
            cameraTransform.setTranslate(at.getX(), at.getY(), at.getZ());
            cameraTransform.setPivot(at.getX(), at.getY(), at.getZ());
            camera.setTranslateZ(currentZ);
        } else {
            double time = 1.0;

            Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(time),
                    new KeyValue(cameraTransform.translateXProperty(), at.getX()),
                    new KeyValue(cameraTransform.translateYProperty(), at.getY()),
                    new KeyValue(cameraTransform.translateZProperty(), at.getZ())
                )
//                ,new KeyFrame(Duration.seconds(time),
//                    new KeyValue(camera.translateZProperty(), currentZ)
//                )
            );
            cameraTransform.setPivot(at.getX(), at.getY(), at.getZ());
            timeline.play();
        }
    }

    public static Affine lookAt(Node node, Point3D from, Point3D to, boolean applyTranslate) {
        //zVec is "forward"
        Point3D zVec = to.subtract(from).normalize();
        //ydir is "up"
        Point3D ydir = Rotate.Y_AXIS;
        Point3D tangent0 = zVec.crossProduct(ydir);
        //handle edge case where to location is precisely the "up" direction
        if (tangent0.magnitude() < 0.001) {
            //pick a different axis to use
            ydir = Rotate.X_AXIS;
            tangent0 = zVec.crossProduct(ydir);
        }
        tangent0.normalize();
        ydir = zVec.crossProduct(tangent0);

        Point3D xVec = ydir.normalize().crossProduct(zVec).normalize();
        Point3D yVec = zVec.crossProduct(xVec).normalize();

        Affine affine = new Affine(
            xVec.getX(), yVec.getX(), zVec.getX(), 0,
            xVec.getY(), yVec.getY(), zVec.getY(), 0,
            xVec.getZ(), yVec.getZ(), zVec.getZ(), 0);
        if (applyTranslate) {
            affine.setTx(from.getX());
            affine.setTy(from.getY());
            affine.setTz(from.getZ());
        }
        node.getTransforms().setAll(affine);
        return affine;
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
        Application.launch(LookAtTest.class, args);
    }
}
