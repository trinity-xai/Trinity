package edu.jhuapl.trinity.javafx.javafx3d;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import edu.jhuapl.trinity.data.HyperspaceSeed;
import edu.jhuapl.trinity.data.Trial;
import edu.jhuapl.trinity.javafx.renderers.TrajectoryRenderer;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import org.fxyz3d.geometry.MathUtils;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.scene.CubeWorld;
import org.fxyz3d.utils.CameraTransformer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */

public class FactorAnalysis3DPane extends StackPane implements TrajectoryRenderer {
    public static double DEFAULT_INTRO_DISTANCE = -60000.0;
    public static double DEFAULT_ZOOM_TIME_MS = 500.0;
    Path lastPath = null;
    public PerspectiveCamera camera;
    public CameraTransformer cameraTransform = new CameraTransformer();
    private double cameraDistance = -4000;
    private final double sceneWidth = 4000;
    private final double sceneHeight = 4000;
    private final double cubeSize = sceneWidth / 2.0;

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    public Group sceneRoot = new Group();
    Group trajectoryGroup = new Group();
    Group trajectoryPointGroup = new Group();
    public Group extrasGroup = new Group();

    public SubScene subScene;
    public double point3dSize = 20.00; //size of 3d tetrahedra
    public double pointScale = 10;

    DirectedScatterMesh rgb3D;
    DirectedScatterDataModel scatterModel;

    public Color sceneColor = Color.BLACK;
    public ContextMenu cm = new ContextMenu();
    //    public LabeledProgressVBox theSpinner = new LabeledProgressVBox();
    public Text dataHudText = new Text("Waiting...");
    boolean isDirty = false;
    int pointId;
    Sphere highlightedPoint = new Sphere(1);

    ArrayList<Trial> trials = new ArrayList<>();

    ArrayList<Point3D> data;
    ArrayList<Point3D> endPoints;
    //    //This maps each seed to a Point3D object which represents its transfromed screen coordinates.
    HashMap<Point3D, HyperspaceSeed> seedToDataMap = new HashMap<>();
    //    //This maps each archiveItem to a Point3D object which represents its end point transfromed to screen coordinates.
    HashMap<Point3D, HyperspaceSeed> seedToEndMap = new HashMap<>();
    //    //This maps each periapsis to a perspective node
    HashMap<HyperspaceSeed, Perspective3DNode> seedToPNodeMap = new HashMap<>();

    List<Integer> visibleIndices = new ArrayList<>();
    Function<Point3D, Number> colorByRGB = p -> {
        Color color = new Color(p.x, p.y, p.z, 1.0);
        return color.getHue();
    };

    ArrayList<Perspective3DNode> pNodes = new ArrayList<>();

    private CubeWorld cubeWorld;
    // initial rotation
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotateZ = new Rotate(0, Rotate.Z_AXIS);

    Group nodeGroup = new Group();
    Group labelGroup = new Group();

    BorderPane bp;
    HashMap<Shape3D, Label> shape3DToLabel = new HashMap<>();
    //For each label you'll need some Shape3D to derive a point3d from.
    //For this we will use simple spheres.  These can be optionally invisible.
    private Sphere xSphere = new Sphere(5);
    private Sphere ySphere = new Sphere(5);
    private Sphere zSphere = new Sphere(5);
    private Label xLabel = new Label("X Axis");
    private Label yLabel = new Label("Y Axis");
    private Label zLabel = new Label("Z Axis");

    public Scene scene;

    public FactorAnalysis3DPane(Scene scene) {
        this.scene = scene;
        setBackground(Background.EMPTY);
        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        subScene.setFill(sceneColor);

        cubeWorld = new CubeWorld(cubeSize, 100, true);
        //add our nodes to the group that will later be added to the 3D scene
        nodeGroup.getChildren().addAll(xSphere, ySphere, zSphere);
        //attach our custom rotation transforms so we can update the labels dynamically
        nodeGroup.getTransforms().addAll(rotateX, rotateY, rotateZ);
        //add our labels to the group that will be added to the StackPane
        labelGroup.getChildren().addAll(xLabel, yLabel, zLabel);
        //Add to hashmap so updateLabels() can manage the label position
        shape3DToLabel.put(xSphere, xLabel);
        shape3DToLabel.put(ySphere, yLabel);
        shape3DToLabel.put(zSphere, zLabel);
        //Customize the 3D nodes a bit
        xSphere.setTranslateX(cubeSize);
        xSphere.setTranslateZ(cubeSize / 2.0);
        xSphere.setMaterial(new PhongMaterial(Color.RED));

        ySphere.setTranslateY(cubeSize);
        ySphere.setTranslateX(-cubeSize / 2.0);
        ySphere.setTranslateZ(cubeSize / 2.0);
        ySphere.setMaterial(new PhongMaterial(Color.GREEN));

        zSphere.setTranslateZ(cubeSize);
        zSphere.setTranslateY(-cubeSize / 2.0);
        zSphere.setMaterial(new PhongMaterial(Color.BLUE));

        //customize the labels to match
        xLabel.setTextFill(Color.YELLOW);
        Font font = new Font("calibri", 20);
        xLabel.setFont(font);
        yLabel.setTextFill(Color.SKYBLUE);
        yLabel.setFont(new Font("calibri", 20));
        zLabel.setTextFill(Color.LIGHTGREEN);
        zLabel.setFont(new Font("calibri", 20));

        camera = new PerspectiveCamera(true);
        //setup camera transform for rotational support
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(30000.0);
        camera.setTranslateZ(cameraDistance);
        cameraTransform.ry.setAngle(-45.0);
        cameraTransform.rx.setAngle(-10.0);

        //Add 3D subscene stuff to 3D scene root object
        sceneRoot.getChildren().addAll(cubeWorld, cameraTransform,
            highlightedPoint, nodeGroup, trajectoryGroup, trajectoryPointGroup, extrasGroup);

        highlightedPoint.setMaterial(new PhongMaterial(Color.ALICEBLUE));
        subScene.setCamera(camera);
        //add a Point Light for better viewing of the grid coordinate system
        PointLight light = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(light);
        light.setTranslateX(camera.getTranslateX());
        light.setTranslateY(camera.getTranslateY());
        light.setTranslateZ(camera.getTranslateZ() + 500.0);

        //Some camera controls...
        subScene.setOnMouseEntered(event -> subScene.requestFocus());
        setOnMouseEntered(event -> subScene.requestFocus());
        subScene.setOnKeyPressed(event -> {
            double change = 10.0;
            //Add shift modifier to simulate "Running Speed"
            if (event.isShiftDown()) {
                change = 50.0;
            }
            //What key did the user press?
            KeyCode keycode = event.getCode();
            //Step 2c: Add Zoom controls
            if (keycode == KeyCode.W) {
                camera.setTranslateZ(camera.getTranslateZ() + change);
            }
            if (keycode == KeyCode.S) {
                camera.setTranslateZ(camera.getTranslateZ() - change);
            }
            //Step 2d:  Add Strafe controls
            if (keycode == KeyCode.A) {
                camera.setTranslateX(camera.getTranslateX() - change);
            }
            if (keycode == KeyCode.D) {
                camera.setTranslateX(camera.getTranslateX() + change);
            }
            updateLabels();
        });

        subScene.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
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
            updateLabels();
        });

        //Start Tracking mouse movements only when a button is pressed
        subScene.setOnMouseDragged((MouseEvent me) -> mouseDragCamera(me));

//        if(null != subScene) {
//            //Set up an easy drag and drop for loading a play back file
//            subScene.setOnDragOver((DragEvent event) -> {
//                Dragboard db = event.getDragboard();
//            });
//
//            // Dropping over surface
//            subScene.setOnDragDropped((DragEvent event) -> {
//                Dragboard db = event.getDragboard();
//                boolean success = false;
//                event.setDropCompleted(success);
//                event.consume();
//            });
//        }

//        theSpinner.setVisible(false);
//        //Setup Text overlays
//        //HUD Text
//        DropShadow ds = new DropShadow();
//        ds.setOffsetY(5.0f);
//        ds.setColor(Color.DARKSLATEGRAY);
//        ds.setBlurType(BlurType.THREE_PASS_BOX);
//        ds.setSpread(0.25);
//        dataHudText.setEffect(ds);
//        dataHudText.setFont(Font.font ("Verdana", 20));
//        dataHudText.setFill(Color.LIGHTSKYBLUE);
//        dataHudText.setMouseTransparent(true);
//        StackPane.setMargin(dataHudText, new Insets(10));
//        StackPane.setAlignment(dataHudText, Pos.BOTTOM_LEFT);

        bp = new BorderPane(subScene);

        getChildren().clear();
        getChildren().addAll(bp, labelGroup);//, theSpinner, dataHudText);

        CheckMenuItem cmShowSystemHUDItem = new CheckMenuItem("Data HUD");
        cmShowSystemHUDItem.setSelected(true);
        dataHudText.visibleProperty().bind(cmShowSystemHUDItem.selectedProperty());

        MenuItem copyMapAsImageItem = new MenuItem("Copy Scene to Clipboard");
        copyMapAsImageItem.setOnAction((ActionEvent e) -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putImage(this.snapshot(new SnapshotParameters(), null));
            clipboard.setContent(content);
        });
//        MenuItem saveSnapshotItem = new MenuItem("Save Scene as Image");
//        saveSnapshotItem.setOnAction((ActionEvent e) -> {
//            final FileChooser fileChooser = new FileChooser();
//            fileChooser.setTitle("Save scene as...");
//            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
//            File file = fileChooser.showSaveDialog(null);
//            if (file != null) {
//                WritableImage image = this.snapshot(new SnapshotParameters(), null);
//                try {
//                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
//                } catch (IOException ioe) {
//                    // TODO: handle exception here
//                }
//            }
//        });
        MenuItem updateAllItem = new MenuItem("Update Render");
        updateAllItem.setOnAction(e -> updateAll());
        MenuItem clearDataItem = new MenuItem("Clear Data");
        clearDataItem.setDisable(true);
        clearDataItem.setOnAction(e -> {
//Disabled directed scatter mesh calls
//            clearAll();
//            updateView(true);
        });

        cm.getItems().addAll(cmShowSystemHUDItem, copyMapAsImageItem, //saveSnapshotItem,
            updateAllItem, clearDataItem);
        cm.setAutoFix(true);
        cm.setAutoHide(true);
        cm.setHideOnEscape(true);
        cm.setOpacity(0.85);

        subScene.setOnMouseClicked((MouseEvent e) -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (null != cm)
                    if (!cm.isShowing())
                        cm.show(this.getParent(), e.getScreenX(), e.getScreenY());
                    else
                        cm.hide();
                e.consume();
            }
        });

    }

    public void intro(double milliseconds) {
        camera.setTranslateZ(DEFAULT_INTRO_DISTANCE);
        JavaFX3DUtils.zoomTransition(milliseconds, camera, cameraDistance);
    }

    public void outtro(double milliseconds) {
        JavaFX3DUtils.zoomTransition(milliseconds, camera, DEFAULT_INTRO_DISTANCE);
    }

    @Override
    public void addTrajectory(Group trajGroup, Group pointGroup, Trajectory3D trajectory3D) {
        //Move the trajectory objects into a dedicated Group
        trajGroup.getChildren().add(trajectory3D);
        for (Point3D point : trajectory3D.points) {
            TrajectoryState sphere = new TrajectoryState(trajectory3D.width / 4.0,
                trajectory3D.dayNumber, trajectory3D.trialNumber);
            sphere.setTranslateX(point.x);
            sphere.setTranslateY(point.y);
            sphere.setTranslateZ(point.z);
            sphere.setMaterial(new PhongMaterial(Color.ALICEBLUE));
            pointGroup.getChildren().add(sphere);
        }
    }

    @Override
    public void addTrajectoryGroup(Group newTrajGroup, Group newTrajPointGroup) {
        trajectoryGroup.getChildren().add(newTrajGroup);
        trajectoryPointGroup.getChildren().add(newTrajPointGroup);
    }

    public void addTrajectory3D(Group trajGroup, Group pointGroup, Trajectory3D trajectory3D) {
        addTrajectory(trajGroup, pointGroup, trajectory3D);
    }

    @Override
    public void clearTrajectories() {
        trajectoryGroup.getChildren().clear();
        trajectoryPointGroup.getChildren().clear();
    }

    @Override
    protected void layoutChildren() {
        final int top = (int) snappedTopInset();
        final int right = (int) snappedRightInset();
        final int bottom = (int) snappedBottomInset();
        final int left = (int) snappedLeftInset();
        final int w = (int) getWidth() - left - right;
        final int h = (int) getHeight() - top - bottom;
//        theSpinner.setLayoutX(w/2);
//        theSpinner.setLayoutY(h/2);
        dataHudText.setLayoutX((w / 2) - dataHudText.getBoundsInLocal().getWidth() / 2.0);
        dataHudText.setLayoutY(top + 50);
    }

    public void updateAll() {
//        if(null != perturbationMap)
//            Platform.runLater(()-> {
////                theSpinner.setVisible(true);
//                highlightedPoint.setRadius(point3dSize/2.0);
////                theSpinner.fadeBusy(false);
////                updatePNodes();
//                updateView(true);
//            });
    }

    private void mouseDragCamera(MouseEvent me) {
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);
        double modifier = 10.0;
        double modifierFactor = 0.1;

        if (me.isControlDown()) {
            modifier = 1;
        }
        if (me.isShiftDown()) {
            modifier = 50.0;
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
                cubeWorld.adjustPanelsByPos(cameraTransform.rx.getAngle(), cameraTransform.ry.getAngle(), cameraTransform.rz.getAngle());
            }
        } else if (me.isMiddleButtonDown()) {
            cameraTransform.t.setX(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3); // -
            cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3); // -
        }
        updateLabels();
    }

    private void updateLabels() {
        shape3DToLabel.forEach((node, label) -> {
            javafx.geometry.Point3D coordinates = node.localToScene(javafx.geometry.Point3D.ZERO, true);
            //@DEBUG SMP  useful debugging print
            //System.out.println("subSceneToScene Coordinates: " + coordinates.toString());
            //Clipping Logic
            //if coordinates are outside of the scene it could
            //stretch the screen so don't transform them
            double x = coordinates.getX();
            double y = coordinates.getY();
            //is it left of the view?
            if (x < 0) {
                x = 0;
            }
            //is it right of the view?
            if ((x + label.getWidth() + 5) > subScene.getWidth()) {
                x = subScene.getWidth() - (label.getWidth() + 5);
            }
            //is it above the view?
            if (y < 0) {
                y = 0;
            }
            //is it below the view
            if ((y + label.getHeight()) > subScene.getHeight())
                y = subScene.getHeight() - (label.getHeight() + 5);
            //@DEBUG SMP  useful debugging print
            //System.out.println("clipping Coordinates: " + x + ", " + y);
            //update the local transform of the label.
            label.getTransforms().setAll(new Translate(x, y));
        });
    }

    public void hideFA3D() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(0.1), new KeyValue(opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.2), e -> outtro(1000)),
            new KeyFrame(Duration.seconds(2.0), new KeyValue(opacityProperty(), 0.0)),
            new KeyFrame(Duration.seconds(2.0), e -> setVisible(false))
        );
        timeline.play();
    }

    public void showFA3D() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(0.1), e ->
                camera.setTranslateZ(FactorAnalysis3DPane.DEFAULT_INTRO_DISTANCE)),
            new KeyFrame(Duration.seconds(0.1), new KeyValue(opacityProperty(), 0.0)),
            new KeyFrame(Duration.seconds(0.3), e -> setVisible(true)),
            new KeyFrame(Duration.seconds(0.3), new KeyValue(opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.6), e -> intro(1000))
        );
        timeline.play();
    }
}
