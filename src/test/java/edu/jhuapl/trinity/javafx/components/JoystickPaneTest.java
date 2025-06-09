package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.javafx.components.panes.JoystickPane;
import edu.jhuapl.trinity.javafx.javafx3d.animated.FastScatter3D;
import edu.jhuapl.trinity.utils.volumetric.Octree;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import org.fxyz3d.utils.CameraTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JoystickPaneTest extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(JoystickPaneTest.class);
    PerspectiveCamera camera;
    CameraTransformer cameraTransform;
    Group sceneRoot;
    SubScene subScene;
    StackPane centerStack;
    Pane pathPane;
    JoystickPane joystickPane;
    ContextMenu cm = new ContextMenu();
    double cameraDistance = -500;
    double sceneWidth = 4000;
    double sceneHeight = 4000;
    double numericCenter = 0.5;
    double scale = 100;
    long hyperspaceRefreshRate = 15; //milliseconds
    int totalPoints = 10000;
    double point3dSize = 0.5;
    int divisions = 8;
    double mousePosX;
    double mousePosY;
    double mouseOldX;
    double mouseOldY;
    double mouseDeltaX;
    double mouseDeltaY;
    FastScatter3D fastScatter3D;
    ArrayList<Point3D> positions;
    Random rando = new Random();
    Octree octree;
    int currentIndex = 0;
    double currentRadius = 10.0;
    Color defaultColor = Color.BLACK;
    Color currentIndexColor = Color.RED;
    Color neighborColor = Color.CYAN;

    /**
     * Transform Management
     */
    public Affine affineTransform;

    @Override
    public void start(Stage stage) {
        fastScatter3D = new FastScatter3D(totalPoints, point3dSize, divisions);
        fastScatter3D.setAllVisible(true);
        int pointCount = fastScatter3D.getPointCount();
        positions = new ArrayList<>(pointCount);
        //generate some random positions
        for (int i = 0; i < pointCount; i++) {
            positions.add(new Point3D(
                rando.nextGaussian(numericCenter, numericCenter * 0.5) * scale,
                rando.nextGaussian(numericCenter, numericCenter * 0.5) * -scale,
                rando.nextGaussian(numericCenter, numericCenter * 0.5) * scale));
        }
        fastScatter3D.updatePositionsList(positions);

        octree = new Octree();
        octree.setMaxPointsPerNode(totalPoints + 1);
        refreshOctree();

        //make transparent so it doesn't interfere with subnode transparency effects
        Background transBack = new Background(new BackgroundFill(
            Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
        pathPane = new Pane(); //transparent layer that just holds floating panes
        pathPane.setPickOnBounds(false);

        sceneRoot = new Group();
        camera = new PerspectiveCamera(true);
        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.DARKSLATEGRAY);
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
        subScene.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> subScene.requestFocus());

        camera = new PerspectiveCamera(true);
        //setup camera transform for rotational support
        cameraTransform = new CameraTransformer();
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
        cameraTransform.ry.setAngle(-45.0);
        cameraTransform.rx.setAngle(-10.0);
        subScene.setCamera(camera);

        centerStack = new StackPane(subScene, pathPane);
        centerStack.setBackground(transBack);
        subScene.widthProperty().bind(centerStack.widthProperty());
        subScene.heightProperty().bind(centerStack.heightProperty());

        Sphere sphereX = new Sphere(5);
        sphereX.setTranslateX(scale);
        sphereX.setMaterial(new PhongMaterial(Color.RED));

        Sphere sphereY = new Sphere(5);
        sphereY.setTranslateY(-scale);
        sphereY.setMaterial(new PhongMaterial(Color.GREEN));

        Sphere sphereZ = new Sphere(5);
        sphereZ.setTranslateZ(scale);
        sphereZ.setMaterial(new PhongMaterial(Color.BLUE));

        AmbientLight ambientLight = new AmbientLight(Color.WHITE);

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

            //point size and scaling
            if (keycode == KeyCode.O || (keycode == KeyCode.P && event.isControlDown())) {
                point3dSize -= 0.1;
                if (point3dSize < 0.1)
                    point3dSize = 0.1;
                fastScatter3D.setRadius(point3dSize);
            }
            if (keycode == KeyCode.P) {
                point3dSize += 0.1;
                fastScatter3D.setRadius(point3dSize);
            }
        });

        BorderPane bpOilSpill = new BorderPane(centerStack);
        bpOilSpill.setBackground(transBack);

        CheckMenuItem toggleJoystickMenuItem = new CheckMenuItem("Enable Joystick Pane");
        toggleJoystickMenuItem.setOnAction(e -> {
            if (toggleJoystickMenuItem.isSelected()) {
                pathPane.getChildren().add(joystickPane);
                joystickPane.slideInPane();
            } else
                pathPane.getChildren().remove(joystickPane);
        });
        CheckMenuItem toggleTopControlsMenuItem = new CheckMenuItem("Joystick Controls");
        toggleTopControlsMenuItem.setOnAction(e -> {
            joystickPane.toggleTopControls(toggleTopControlsMenuItem.isSelected());
        });
        CheckMenuItem toggleButtonsMenuItem = new CheckMenuItem("Enable Fire and Thrust Buttons");
        toggleButtonsMenuItem.setOnAction(e -> {
            joystickPane.toggleBottomControls(toggleButtonsMenuItem.isSelected());
        });

        cm.getItems().addAll(
            toggleJoystickMenuItem,
            toggleTopControlsMenuItem,
            toggleButtonsMenuItem
        );
        cm.setAutoFix(true);
        cm.setAutoHide(true);
        cm.setHideOnEscape(true);

        centerStack.setOnMouseClicked((MouseEvent e) -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (null != cm)
                    if (!cm.isShowing())
                        cm.show(centerStack.getParent(), e.getScreenX(), e.getScreenY());
                    else
                        cm.hide();
                e.consume();
            }
        });

        Scene scene = new Scene(bpOilSpill, Color.BLACK);
        joystickPane = new JoystickPane(scene, centerStack);
        joystickPane.fireButton.setOnAction(e -> LOG.info("fire"));
        joystickPane.thrustButton.setOnAction(e -> LOG.info("thrust"));
        joystickPane.angleproperty.subscribe(c -> {
//            projectileSystem.playerShip.mouseDragged(null,
//                joystickPane.mouseDeltaX,
//                joystickPane.mouseDeltaY);
        });

        joystickPane.valueproperty.subscribe(c -> {
            currentRadius = c.doubleValue() * scale;
//            Platform.runLater(() -> {
            selectByRadius(currentRadius);
//            });
        });
        joystickPane.fireButton.setOnAction(e -> refreshOctree());
        joystickPane.thrustButton.setOnAction(e -> selectRandom());

        affineTransform = new Affine();

        //Make everything pretty
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        CSS = StyleResourceProvider.getResource("covalent.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        scene.setOnMouseEntered(event -> subScene.requestFocus());

        stage.setTitle("Joystick tester");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public void joystickDragged(boolean controlDown, boolean shiftDown, double mouseDeltaX, double mouseDeltaY) {
        double modifier = 1.0;
        double modifierFactor = 0.1;  //@TODO SMP connect to sensitivity property

        if (controlDown) {
            modifier = 0.1;
        }
        if (shiftDown) {
            modifier = 25.0;
        }
        double yChange = (((mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
        double xChange = (((-mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);

        addRotation(yChange, Rotate.Y_AXIS);
        addRotation(xChange, Rotate.X_AXIS);
    }

    /**
     * Accumulate rotation about specified axis
     *
     * @param angle
     * @param axis
     */
    public void addRotation(double angle, Point3D axis) {
        Rotate r = new Rotate(angle, axis);
        /**
         * This is the important bit and thanks to bronkowitz in this post
         * https://stackoverflow.com/questions/31382634/javafx-3d-rotations for
         * getting me to the solution that the rotations need accumulated in
         * this way
         */
        affineTransform = (Affine) r.createConcatenation(affineTransform);
    }

    private void selectByRadius(double radius) {
        fastScatter3D.setColorAll(defaultColor);

        if (radius > 0.0) {
            Point3D centroid = octree.getCurrentPoints().get(currentIndex);
            List<Integer> indices = octree.searchAllNeighborsWithinDistance(centroid, radius);
            LOG.info("{} {}", indices.size(), radius);
            for (int i : indices) {
                fastScatter3D.setColorByIndex(i, neighborColor);
            }
        }
        fastScatter3D.setColorByIndex(currentIndex, currentIndexColor);
    }

    private void selectRandom() {
        fastScatter3D.setColorAll(defaultColor);
        currentIndex = rando.nextInt(positions.size());
        int[] indices = octree.searchNearestNeighbors(20, currentIndex);
        fastScatter3D.setColorByIndex(currentIndex, currentIndexColor);
        for (int i : indices)
            fastScatter3D.setColorByIndex(i, neighborColor);
    }

    private void selectCentroid() {
        fastScatter3D.setColorAll(defaultColor);
        Point3D centroid = new Point3D(
            numericCenter * scale, numericCenter * -scale, numericCenter * scale);
        int[] indices = octree.searchNearestNeighbors(1, centroid);
        currentIndex = indices[0];
        indices = octree.searchNearestNeighbors(20, currentIndex);
        for (int i : indices)
            fastScatter3D.setColorByIndex(i, neighborColor);
        fastScatter3D.setColorByIndex(currentIndex, currentIndexColor);
    }

    private void refreshOctree() {
        octree.buildIndex(positions);
        selectCentroid();
        LOG.info("Octree stuff done.");
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
        launch(args);
    }
}
