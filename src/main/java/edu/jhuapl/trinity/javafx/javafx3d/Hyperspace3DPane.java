package edu.jhuapl.trinity.javafx.javafx3d;

/*-
 * #%L
 * trinity
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

import edu.jhuapl.trinity.data.CoordinateSet;
import edu.jhuapl.trinity.data.Dimension;
import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.FeatureLayer;
import edu.jhuapl.trinity.data.HyperspaceSeed;
import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.data.messages.GaussianMixture;
import edu.jhuapl.trinity.data.messages.GaussianMixtureCollection;
import edu.jhuapl.trinity.data.messages.GaussianMixtureData;
import edu.jhuapl.trinity.data.messages.PointCluster;
import edu.jhuapl.trinity.javafx.components.callouts.Callout;
import edu.jhuapl.trinity.javafx.components.panes.RadialEntityOverlayPane;
import edu.jhuapl.trinity.javafx.components.radial.HyperspaceMenu;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.ColorMapEvent;
import edu.jhuapl.trinity.javafx.events.ColorMapEvent.COLOR_MAP;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent.COLOR_MODE;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.events.ShadowEvent;
import edu.jhuapl.trinity.javafx.events.TimelineEvent;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
import edu.jhuapl.trinity.javafx.javafx3d.ShadowCubeWorld.PROJECTION_TYPE;
import edu.jhuapl.trinity.javafx.renderers.FeatureVectorRenderer;
import edu.jhuapl.trinity.javafx.renderers.GaussianMixtureRenderer;
import edu.jhuapl.trinity.javafx.renderers.ManifoldRenderer;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import edu.jhuapl.trinity.utils.VisibilityMap;
import javafx.animation.AnimationTimer;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.StageStyle;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.scene.Skybox;
import org.fxyz3d.utils.CameraTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */

public class Hyperspace3DPane extends StackPane implements
    FeatureVectorRenderer, GaussianMixtureRenderer, ManifoldRenderer {
    public static double ICON_FIT_HEIGHT = 64;
    public static double ICON_FIT_WIDTH = 64;
    public static double DEFAULT_INTRO_DISTANCE = -60000.0;
    public static double DEFAULT_ZOOM_TIME_MS = 500.0;
    private double cameraDistance = -4000;
    private final double sceneWidth = 4000;
    private final double sceneHeight = 4000;
    private final double cubeSize = sceneWidth / 2.0;
    public PerspectiveCamera camera;
    public CameraTransformer cameraTransform = new CameraTransformer();
    public XFormGroup dataXForm = new XFormGroup();
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;
    RadialEntityOverlayPane radialOverlayPane;

    public Group sceneRoot = new Group();
    public Group extrasGroup = new Group();
    public Group debugGroup = new Group();
    public Group ellipsoidGroup = new Group();
    public SubScene subScene;
    public ShadowCubeWorld cubeWorld;
    public PROJECTION_TYPE projectionType = ShadowCubeWorld.PROJECTION_TYPE.FIXED_ORTHOGRAPHIC;

    public double point3dSize = 10.0; //size of 3d tetrahedra
    public double pointScale = 1.0; //scales parameter value in transform
    public double scatterBuffScaling = 1.0; //scales domain range in transform
    public long hyperspaceRefreshRate = 500; //milliseconds
    public int queueLimit = 50000;

    //feature vector indices for 3D coordinates
    private boolean directionEnabled = false;
    private int xFactorIndex = 0;
    private int yFactorIndex = 1;
    private int zFactorIndex = 2;
    private int xDirFactorIndex = 3;
    private int yDirFactorIndex = 4;
    private int zDirFactorIndex = 5;
    private int factorMaxIndex = 512;
    private int anchorIndex = 0;
    DirectedScatterMesh scatterMesh3D;
    DirectedScatterDataModel scatterModel;

    public Color sceneColor = Color.BLACK;
    HyperspaceMenu hyperspaceMenu;
    boolean isDirty = false;
    boolean heightChanged = false;
    boolean reflectY = true;
    Sphere highlightedPoint = new Sphere(1, 8);

    Callout anchorCallout;
    TriaxialSpheroidMesh anchorTSM;
    Trajectory anchorTrajectory;
    Trajectory3D anchorTraj3D;
    Group trajectorySphereGroup;
    Group trajectoryGroup;
    HashMap<Trajectory, Trajectory3D> trajToTraj3DMap = new HashMap<>();
    double trajectoryScale = 1.0;
    int trajectoryTailSize = 5;

    ArrayList<Point3D> data;
    ArrayList<Point3D> endPoints;
    //This maps each seed to a Point3D object which represents its transfromed screen coordinates.
    HashMap<Point3D, HyperspaceSeed> seedToDataMap = new HashMap<>();
    //This maps each seed to a Point3D object which represents its end point transfromed to screen coordinates.
    HashMap<Point3D, HyperspaceSeed> seedToEndMap = new HashMap<>();
    //This maps each ellipsoid to a GMM
    HashMap<TriaxialSpheroidMesh, GaussianMixture> ellipsoidToGMMessageMap = new HashMap<>();
    //This maps each ellipsoid to its specific GaussianMixtureData
    HashMap<TriaxialSpheroidMesh, GaussianMixtureData> ellipsoidToGMDataMap = new HashMap<>();
    //allows 2D labels to track their 3D counterparts
    HashMap<Shape3D, Label> shape3DToLabel = new HashMap<>();

    public List<FeatureVector> featureVectors = new ArrayList<>();
    public boolean meanCentered = true;
    public boolean autoScaling = true;
    public boolean updatingTrajectories = false;
    public COLOR_MODE colorMode = COLOR_MODE.COLOR_BY_LABEL;
    public COLOR_MAP colorMap = COLOR_MAP.ONE_COLOR_SPECTRUM;
    public List<Double> meanVector = new ArrayList<>();
    public double maxAbsValue = 1.0;
    public double meanCenteredMaxAbsValue = 1.0;

    public ConcurrentLinkedQueue<HyperspaceSeed> hyperspaceSeeds = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Perspective3DNode> pNodes = new ConcurrentLinkedQueue<>();
    int TOTAL_COLORS = 1530; //colors used by map function
    Function<Point3D, Number> colorByLabelFunction = p -> p.f; //Color mapping function

    // initial rotation
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotateZ = new Rotate(0, Rotate.Z_AXIS);

    private Skybox skybox;
    private Group nodeGroup = new Group();
    private Group labelGroup = new Group();
    private Group manifoldGroup = new Group();
    private ArrayList<Manifold3D> manifolds = new ArrayList<>();

    BorderPane bp;
    //For each label you'll need some Shape3D to derive a point3d from.
    //For this we will use simple spheres.  These can be optionally invisible.
    private Sphere xSphere = new Sphere(10);
    private Sphere ySphere = new Sphere(10);
    private Sphere zSphere = new Sphere(10);
    private Label xLabel = new Label("X Axis");
    private Label yLabel = new Label("Y Axis");
    private Label zLabel = new Label("Z Axis");
    public List<String> featureLabels = new ArrayList<>();
    public Scene scene;

    public Hyperspace3DPane(Scene scene) {
        this.scene = scene;
        cubeWorld = new ShadowCubeWorld(cubeSize, 100, false, featureVectors);
        cubeWorld.setScene(this.scene);
        setBackground(Background.EMPTY);
        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        subScene.setFill(sceneColor);

        //add our nodes to the group that will later be added to the 3D scene
        nodeGroup.getChildren().addAll(xSphere, ySphere, zSphere);
        //attach our custom rotation transforms so we can update the labels dynamically
        nodeGroup.getTransforms().addAll(rotateX, rotateY, rotateZ);
        //Customize the 3D nodes a bit
        xSphere.setTranslateX(cubeSize / 2.0);
        xSphere.setMaterial(new PhongMaterial(Color.RED));

        ySphere.setTranslateY(-cubeSize / 2.0);
        ySphere.setMaterial(new PhongMaterial(Color.GREEN));

        zSphere.setTranslateZ(cubeSize / 2.0);
        zSphere.setMaterial(new PhongMaterial(Color.BLUE));

        //customize the labels to match
        xLabel.setFont(new Font("Consolas", 20));
        xLabel.setTextFill(Color.YELLOW);
        yLabel.setFont(new Font("Consolas", 20));
        yLabel.setTextFill(Color.SKYBLUE);
        zLabel.setFont(new Font("Consolas", 20));
        zLabel.setTextFill(Color.LIGHTGREEN);

        //add our labels to the group that will be added to the StackPane
        labelGroup.getChildren().addAll(xLabel, yLabel, zLabel);
        labelGroup.setManaged(false);
        //Add to hashmap so updateLabels() can manage the label position
        shape3DToLabel.put(xSphere, xLabel);
        shape3DToLabel.put(ySphere, yLabel);
        shape3DToLabel.put(zSphere, zLabel);

        camera = new PerspectiveCamera(true);

        //setup camera transform for rotational support
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
        cameraTransform.ry.setAngle(-45.0);
        cameraTransform.rx.setAngle(-10.0);

        setupSkyBox();
        debugGroup.setVisible(false);

        //Anchor and event trajectory setup
        anchorTSM = createEllipsoid(10, 10, 10, Color.STEELBLUE);
        anchorTrajectory = new Trajectory("Anchor History");
        trajectoryScale = 1.0;
        anchorTraj3D = JavaFX3DUtils.buildPolyLineFromTrajectory(1, 1,
            anchorTrajectory, Color.CYAN, trajectoryScale, sceneWidth, sceneHeight);
        extrasGroup.getChildren().add(anchorTraj3D);
        trajectoryGroup = new Group();
        trajectorySphereGroup = new Group();
        double ellipsoidWidth = anchorTraj3D.width / 2.0;
        for (Point3D point : anchorTraj3D.points) {
            TriaxialSpheroidMesh tsm = createEllipsoid(ellipsoidWidth,
                ellipsoidWidth, ellipsoidWidth, Color.LIGHTBLUE);
            tsm.setTranslateX(point.x);
            tsm.setTranslateY(point.y);
            tsm.setTranslateZ(point.z);
            trajectorySphereGroup.getChildren().add(tsm);
        }
        extrasGroup.getChildren().add(0, trajectorySphereGroup);
        extrasGroup.getChildren().add(0, trajectoryGroup);
        this.scene.addEventHandler(TrajectoryEvent.REFRESH_3D_TRAJECTORIES, e -> {
            updateTrajectory3D(true);
        });
        this.scene.addEventHandler(TrajectoryEvent.AUTO_UDPATE_TRAJECTORIES, e -> {
            updatingTrajectories = (boolean) e.eventObject;
            if (updatingTrajectories)
                updateTrajectory3D(false);
        });
        this.scene.addEventHandler(TrajectoryEvent.TIMELINE_SHOW_TRAJECTORY, e -> {
            anchorTraj3D.setVisible((boolean) e.eventObject);
            trajectorySphereGroup.setVisible((boolean) e.eventObject);
        });
        this.scene.addEventHandler(TrajectoryEvent.TRAJECTORY_TAIL_SIZE, e -> {
            trajectoryTailSize = (int) e.eventObject;
            updateTrajectory3D(false);
        });
        this.scene.addEventHandler(TrajectoryEvent.TIMELINE_SHOW_CALLOUT, e -> {
            anchorCallout.setVisible((boolean) e.eventObject);
        });
        this.scene.addEventHandler(TrajectoryEvent.NEW_TRAJECTORY_OBJECT, e -> {
            Platform.runLater(() -> updateTrajectory3D(false));
        });
        this.scene.addEventHandler(TrajectoryEvent.CLEAR_ALL_TRAJECTORIES, e -> {
            Platform.runLater(() -> updateTrajectory3D(true));
        });
        this.scene.addEventHandler(TrajectoryEvent.TRAJECTORY_VISIBILITY_CHANGED, e -> {
            Trajectory trajectory = (Trajectory) e.eventObject;
            Trajectory3D traj3D = trajToTraj3DMap.get(trajectory);
            if (null != traj3D) {
                traj3D.setVisible(trajectory.getVisible());
            }
        });
        this.scene.addEventHandler(TrajectoryEvent.TRAJECTORY_COLOR_CHANGED, e -> {
            Platform.runLater(() -> updateTrajectory3D(false));
        });
        //Add 3D subscene stuff to 3D scene root object
        sceneRoot.getChildren().addAll(cameraTransform, highlightedPoint, nodeGroup,
            manifoldGroup, debugGroup, cubeWorld, dataXForm, extrasGroup, anchorTSM);

        highlightedPoint.setMaterial(new PhongMaterial(Color.ALICEBLUE));
        highlightedPoint.setDrawMode(DrawMode.LINE);
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
        subScene.setOnZoom(event -> {
            double modifier = 50.0;
            double modifierFactor = 0.1;
            double z = camera.getTranslateZ();
            double newZ = z + event.getZoomFactor() * modifierFactor * modifier;
            camera.setTranslateZ(newZ);
            updateLabels();
        });
        subScene.setOnSwipeUp(e -> {
            e.consume();
            Platform.runLater(() -> {
                cubeWorld.animateOut(1000, 20000);
            });
        });
        subScene.setOnSwipeDown(e -> {
            e.consume();
            Platform.runLater(() -> {
                cubeWorld.animateIn(1000, cubeSize);
            });
        });

        subScene.setOnKeyPressed(event -> {
            //What key did the user press?
            KeyCode keycode = event.getCode();
            if (keycode == KeyCode.OPEN_BRACKET && event.isControlDown()) {
                Platform.runLater(() -> {
                    cubeWorld.animateOut(1000, 20000);
                });
            } else if (keycode == KeyCode.CLOSE_BRACKET && event.isControlDown()) {
                Platform.runLater(() -> {
                    cubeWorld.animateIn(1000, cubeSize);
                });
            }
            if ((keycode == KeyCode.NUMPAD0 && event.isControlDown())
                || (keycode == KeyCode.DIGIT0 && event.isControlDown())) {
                resetView(1000, false);
            } else if ((keycode == KeyCode.NUMPAD0 && event.isShiftDown())
                || (keycode == KeyCode.DIGIT0 && event.isShiftDown())) {
                resetView(0, true);
            }
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
            if (keycode == KeyCode.PLUS && event.isShortcutDown()) {
                camera.setTranslateZ(camera.getTranslateZ() + change);
            }
            if (keycode == KeyCode.MINUS && event.isShortcutDown()) {
                camera.setTranslateZ(camera.getTranslateZ() - change);
            }

            //Strafe controls
            if (keycode == KeyCode.A) {
                camera.setTranslateX(camera.getTranslateX() - change);
            }
            if (keycode == KeyCode.D) {
                camera.setTranslateX(camera.getTranslateX() + change);
            }
            //rotate controls  use less sensitive modifiers
            change = event.isShiftDown() ? 10.0 : 1.0;

            if (keycode == KeyCode.NUMPAD7 || (keycode == KeyCode.DIGIT8)) //yaw positive
                cameraTransform.ry.setAngle(cameraTransform.ry.getAngle() + change);
            if (keycode == KeyCode.NUMPAD9 || (keycode == KeyCode.DIGIT8 && event.isControlDown())) //yaw negative
                cameraTransform.ry.setAngle(cameraTransform.ry.getAngle() - change);

            if (keycode == KeyCode.NUMPAD4 || (keycode == KeyCode.DIGIT9)) //pitch positive
                cameraTransform.rx.setAngle(cameraTransform.rx.getAngle() + change);
            if (keycode == KeyCode.NUMPAD6 || (keycode == KeyCode.DIGIT9 && event.isControlDown())) //pitch negative
                cameraTransform.rx.setAngle(cameraTransform.rx.getAngle() - change);

            if (keycode == KeyCode.NUMPAD1 || (keycode == KeyCode.DIGIT0)) //roll positive
                cameraTransform.rz.setAngle(cameraTransform.rz.getAngle() + change);
            if (keycode == KeyCode.NUMPAD3 || (keycode == KeyCode.DIGIT0 && event.isControlDown())) //roll negative
                cameraTransform.rz.setAngle(cameraTransform.rz.getAngle() - change);

            //Coordinate shifts
            if (keycode == KeyCode.COMMA) {
                //shift coordinates to the left
                if (xFactorIndex > 0 && yFactorIndex > 0 && zFactorIndex > 0) {
                    xFactorIndex -= 1;
                    yFactorIndex -= 1;
                    zFactorIndex -= 1;
                    Platform.runLater(() -> scene.getRoot().fireEvent(
                        new HyperspaceEvent(HyperspaceEvent.FACTOR_COORDINATES_KEYPRESS,
                            new CoordinateSet(xFactorIndex, yFactorIndex, zFactorIndex))));
                    boolean redraw = false;
                    try {
                        updatePNodeIndices(xFactorIndex, yFactorIndex, zFactorIndex,
                            xDirFactorIndex, yDirFactorIndex, zDirFactorIndex);
                        redraw = true;
                    } catch (Exception ex) {
                        scene.getRoot().fireEvent(
                            new CommandTerminalEvent("Feature Indexing Error: ("
                                + xFactorIndex + ", " + yFactorIndex + ", " + zFactorIndex + ")",
                                new Font("Consolas", 20), Color.RED));
                    }
                    if (redraw) {
                        updateView(false);
                        updateEllipsoids();
                        notifyIndexChange();
                    }
                    updateLabels();
                    setSpheroidAnchor(true, anchorIndex);
                }
            }
            if (keycode == KeyCode.PERIOD) {
                if (featureVectors.isEmpty()) return;
                //shift coordinates to the right
                int featureSize = featureVectors.get(0).getData().size();
                if (xFactorIndex < factorMaxIndex - 1 && yFactorIndex < factorMaxIndex - 1
                    && zFactorIndex < factorMaxIndex - 1 && xFactorIndex < featureSize - 1
                    && yFactorIndex < featureSize - 1 && zFactorIndex < featureSize - 1) {
                    xFactorIndex += 1;
                    yFactorIndex += 1;
                    zFactorIndex += 1;
                    Platform.runLater(() -> scene.getRoot().fireEvent(
                        new HyperspaceEvent(HyperspaceEvent.FACTOR_COORDINATES_KEYPRESS,
                            new CoordinateSet(xFactorIndex, yFactorIndex, zFactorIndex))));
                    boolean redraw = false;
                    try {
                        updatePNodeIndices(xFactorIndex, yFactorIndex, zFactorIndex,
                            xDirFactorIndex, yDirFactorIndex, zDirFactorIndex);
                        redraw = true;
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        scene.getRoot().fireEvent(
                            new CommandTerminalEvent("Feature Indexing Error: ("
                                + xFactorIndex + ", " + yFactorIndex + ", " + zFactorIndex + ")",
                                new Font("Consolas", 20), Color.RED));
                    }
                    if (redraw) {
                        updateView(false);
                        updateEllipsoids();
                        notifyIndexChange();
                    }
                    updateLabels();
                    setSpheroidAnchor(true, anchorIndex);
                } else {
                    scene.getRoot().fireEvent(
                        new CommandTerminalEvent("Feature Index Max Reached: ("
                            + featureSize + ")", new Font("Consolas", 20), Color.YELLOW));
                }
            }
            if (keycode == KeyCode.SLASH && event.isControlDown()) {
                debugGroup.setVisible(!debugGroup.isVisible());
            }

            //point size and scaling
            if (keycode == KeyCode.T) {
                scatterBuffScaling -= 0.1;
                Platform.runLater(() -> scene.getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.SCATTERBUFF_SCALING_KEYPRESS, scatterBuffScaling)));
                updateScatterLimits(scatterBuffScaling, true);
                notifyScaleChange();
                updateView(false);
                updateEllipsoids();
            }
            if (keycode == KeyCode.Y) {
                scatterBuffScaling += 0.1;
                Platform.runLater(() -> scene.getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.SCATTERBUFF_SCALING_KEYPRESS, scatterBuffScaling)));
                updateScatterLimits(scatterBuffScaling, true);
                notifyScaleChange();
                updateView(false);
                updateEllipsoids();
            }

            //point size and scaling
            if (keycode == KeyCode.O || (keycode == KeyCode.P && event.isControlDown())) {
                point3dSize -= 5;
                Platform.runLater(() -> scene.getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.POINT3D_SIZE_KEYPRESS, point3dSize)));
                heightChanged = true;
                updateView(false);
            }
            if (keycode == KeyCode.P) {
                point3dSize += 5;
                Platform.runLater(() -> scene.getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.POINT3D_SIZE_KEYPRESS, point3dSize)));
                heightChanged = true;
                updateView(false);
            }
            if (keycode == KeyCode.U) {
                pointScale -= 0.1;
                Platform.runLater(() -> scene.getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.POINT_SCALE_KEYPRESS, pointScale)));
                scatterModel.pointScale = pointScale;
                notifyScaleChange();
                updateView(false);
            }
            if (keycode == KeyCode.I) {
                pointScale += 0.1;
                Platform.runLater(() -> scene.getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.POINT_SCALE_KEYPRESS, pointScale)));
                scatterModel.pointScale = pointScale;
                notifyScaleChange();
                updateView(false);
            }

            if (event.isAltDown() && keycode == KeyCode.H) {
                makeHull(false, null, null);
            }
            if (keycode == KeyCode.F) {
                anchorIndex--;
                if (anchorIndex < 0) anchorIndex = 0;
                setSpheroidAnchor(true, anchorIndex);
            }
            if (keycode == KeyCode.G) {
                anchorIndex++;
                if (anchorIndex > scatterModel.data.size()) anchorIndex = scatterModel.data.size();
                setSpheroidAnchor(true, anchorIndex);
            }

            updateLabels();
            radialOverlayPane.updateCalloutHeadPoints(subScene);
        });

        subScene.setOnMousePressed((MouseEvent me) -> {
            //@DEBUG SMP useful for debugging touchscreens
            //if(me.isSynthesized())
            //    System.out.println("isSynthesized");
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
        subScene.setOnZoom(e -> {
            double zoom = e.getZoomFactor();
            if (zoom > 1) {
                camera.setTranslateZ(camera.getTranslateZ() + 50.0);
            } else {
                camera.setTranslateZ(camera.getTranslateZ() - 50.0);
            }
            updateLabels();
            radialOverlayPane.updateCalloutHeadPoints(subScene);
            e.consume();
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
            radialOverlayPane.updateCalloutHeadPoints(subScene);
        });

        //Start Tracking mouse movements only when a button is pressed
        subScene.setOnMouseDragged((MouseEvent me) -> mouseDragCamera(me));

        bp = new BorderPane(subScene);
        //RadialOverlayPane will hold all those nifty callouts and radial entities
        radialOverlayPane = new RadialEntityOverlayPane(this.scene, featureVectors);
        radialOverlayPane.prefWidthProperty().bind(widthProperty());
        radialOverlayPane.prefHeightProperty().bind(heightProperty());
        radialOverlayPane.minWidthProperty().bind(widthProperty());
        radialOverlayPane.minHeightProperty().bind(heightProperty());
        radialOverlayPane.maxWidthProperty().bind(widthProperty());
        radialOverlayPane.maxHeightProperty().bind(heightProperty());

        getChildren().clear();
        getChildren().addAll(bp, labelGroup, radialOverlayPane);

        hyperspaceMenu = new HyperspaceMenu(this);
        hyperspaceMenu.hideRadialMenu();
        hyperspaceMenu.setVisible(false);
        radialOverlayPane.addEntity(hyperspaceMenu);
        subScene.setOnMouseClicked((MouseEvent e) -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (hyperspaceMenu.isVisible()) {
                    Platform.runLater(() -> {
                        hyperspaceMenu.hideRadialMenu();
                        hyperspaceMenu.setVisible(false);
                    });
                } else {
                    Platform.runLater(() -> {
                        hyperspaceMenu.setVisible(true);
                        hyperspaceMenu.setTranslateX(e.getSceneX());
                        hyperspaceMenu.setTranslateY(e.getSceneY());
                        hyperspaceMenu.itemReticuleAnimation();
                    });
                }
            }
        });
        //Create dummy seed and pnode for mesh to form around
        double[] features = new double[factorMaxIndex];
        Arrays.fill(features, 0);
        HyperspaceSeed seed = new HyperspaceSeed(0, 1, 2, 3, 4, 5, features);
        hyperspaceSeeds.add(seed);
        addPNodeFromSeed(seed);
        //load empty mesh (except single 0'ed point.
        loadDirectedMesh();

        this.scene.addEventHandler(HyperspaceEvent.ADDED_FEATURE_LAYER, e ->
            changeFeatureLayer((FeatureLayer) e.object));
        this.scene.addEventHandler(HyperspaceEvent.ADDEDALL_FEATURE_LAYER, e ->
            changeFeatureLayer(null));
        this.scene.addEventHandler(HyperspaceEvent.UPDATED_FEATURE_LAYER, e ->
            changeFeatureLayer((FeatureLayer) e.object));
        this.scene.addEventHandler(HyperspaceEvent.REMOVED_FEATURE_LAYER, e -> {
            Integer index = ((FeatureLayer) e.object).getIndex();
            if (null != FeatureLayer.removeFeatureLayer(index)) {
                updateView(true);
            }
        });
        this.scene.addEventHandler(HyperspaceEvent.ADDED_FACTOR_LABEL, e ->
            changeFactorLabels((FactorLabel) e.object));
        this.scene.addEventHandler(HyperspaceEvent.ADDEDALL_FACTOR_LABELS, e -> {
            List<FactorLabel> labels = (List<FactorLabel>) e.object;
            updateOnLabelChange(labels);
        });
        this.scene.addEventHandler(HyperspaceEvent.UPDATEDALL_FACTOR_LABELS, e -> {
            List<FactorLabel> labels = (List<FactorLabel>) e.object;
            updateOnLabelChange(labels);
        });
        this.scene.addEventHandler(HyperspaceEvent.UPDATED_FACTOR_LABEL, e ->
            changeFactorLabels((FactorLabel) e.object));
        this.scene.addEventHandler(HyperspaceEvent.REMOVED_FACTOR_LABEL, e -> {
            String label = ((FactorLabel) e.object).getLabel();
            if (null != FactorLabel.removeFactorLabel(label)) {
                updateView(true);
                updateEllipsoids();
            }
        });
        this.scene.addEventHandler(HyperspaceEvent.CLEAR_HYPERSPACE_NOW, e -> {
            clearAll();
            updateView(true);
            cubeWorld.clearAll();
        });
        this.scene.addEventHandler(HyperspaceEvent.CLEARED_FACTOR_LABELS, e -> {
            refresh();
        });
        this.scene.addEventHandler(HyperspaceEvent.CLEARED_FEATURE_LAYERS, e -> {
            refresh();
        });
        this.scene.addEventHandler(HyperspaceEvent.HYPERSPACE_BACKGROUND_COLOR, e -> {
            Color color = (Color) e.object;
            subScene.setFill(color);
        });
        this.scene.addEventHandler(HyperspaceEvent.ENABLE_HYPERSPACE_SKYBOX, e -> {
            skybox.setVisible((Boolean) e.object);
        });
        this.scene.addEventHandler(HyperspaceEvent.ENABLE_FEATURE_DATA, e -> {
            scatterMesh3D.setVisible((Boolean) e.object);
        });
        this.scene.addEventHandler(HyperspaceEvent.ENABLE_DIRECTION_COORDINATES, e -> {
            directionEnabled = (boolean) e.object;
            updateView(true);
        });
        scene.addEventHandler(HyperspaceEvent.DIMENSION_LABEL_REMOVED, e -> {
            Dimension d = (Dimension) e.object;
            if (d.index < featureLabels.size()) {
                featureLabels.remove(d.index);
                featureLabels.add(d.index, "No Label");
            }
            updateLabels();
        });
        scene.addEventHandler(HyperspaceEvent.DIMENSION_LABEL_UPDATE, e -> {
            Dimension d = (Dimension) e.object;
            //artificially fill in the label list if necessary
            if (d.index >= featureLabels.size()) {
                for (int i = featureLabels.size(); i <= d.index; i++) {
                    featureLabels.add("No Label");
                }
            }
            featureLabels.remove(d.index);
            featureLabels.add(d.index, d.labelString);
            updateLabels();
        });
        scene.addEventHandler(HyperspaceEvent.CLEARED_DIMENSION_LABELS, e -> {
            featureLabels.clear();
            updateLabels();
        });
        this.scene.addEventHandler(HyperspaceEvent.FACTOR_COORDINATES_GUI, e -> {
            CoordinateSet coords = (CoordinateSet) e.object;
            xFactorIndex = coords.coordinateIndices.get(0);
            yFactorIndex = coords.coordinateIndices.get(1);
            zFactorIndex = coords.coordinateIndices.get(2);
            xDirFactorIndex = coords.coordinateIndices.get(3);
            yDirFactorIndex = coords.coordinateIndices.get(4);
            zDirFactorIndex = coords.coordinateIndices.get(5);
            updateLabels();
            updateView(true);
            updateEllipsoids();
            notifyIndexChange();
        });

        scene.addEventHandler(HyperspaceEvent.FACTOR_VECTORMAX_GUI, e -> {
            int newFactorMaxIndex = (int) e.object;
            if (newFactorMaxIndex < factorMaxIndex) {
                factorMaxIndex = newFactorMaxIndex;
                boolean update = false;
                if (xFactorIndex > factorMaxIndex) {
                    xFactorIndex = factorMaxIndex;
                    update = true;
                }
                if (yFactorIndex > factorMaxIndex) {
                    yFactorIndex = factorMaxIndex;
                    update = true;
                }
                if (zFactorIndex > factorMaxIndex) {
                    zFactorIndex = factorMaxIndex;
                    update = true;
                }
                if (update) {
                    updateView(true);
                    updateEllipsoids();
                    notifyIndexChange();
                }
            } else
                factorMaxIndex = newFactorMaxIndex;
        });
        //Color Mode Events
        scene.addEventHandler(HyperspaceEvent.COLOR_BY_LABEL, e -> {
            colorMode = COLOR_MODE.COLOR_BY_LABEL;
            updateView(true);
        });
        scene.addEventHandler(HyperspaceEvent.COLOR_BY_LAYER, e -> {
            colorMode = COLOR_MODE.COLOR_BY_LAYER;
            updateView(true);
        });
        scene.addEventHandler(HyperspaceEvent.COLOR_BY_GRADIENT, e -> {
            colorMode = COLOR_MODE.COLOR_BY_GRADIENT;
            updateView(true);
        });
        scene.addEventHandler(HyperspaceEvent.COLOR_BY_SCORE, e -> {
            colorMode = COLOR_MODE.COLOR_BY_SCORE;
            updateView(true);
        });
        scene.addEventHandler(HyperspaceEvent.COLOR_BY_PFA, e -> {
            colorMode = COLOR_MODE.COLOR_BY_PFA;
            updateView(true);
        });
        //Color Map Events
        scene.addEventHandler(ColorMapEvent.ONE_COLOR_SPECTRUM, e -> {
            colorMap = COLOR_MAP.ONE_COLOR_SPECTRUM;
            updateView(true);
        });
        scene.addEventHandler(ColorMapEvent.TWO_COLOR_SPECTRUM, e -> {
            colorMap = COLOR_MAP.TWO_COLOR_SPECTRUM;
            updateView(true);
        });
        scene.addEventHandler(ColorMapEvent.HSB_WHEEL_SPECTRUM, e -> {
            colorMap = COLOR_MAP.HSB_WHEEL_SPECTRUM;
            updateView(true);
        });
        scene.addEventHandler(ColorMapEvent.PRESET_COLOR_PALETTE, e -> {
            colorMap = COLOR_MAP.PRESET_COLOR_PALETTE;
            updateView(true);
        });
        scene.addEventHandler(ColorMapEvent.COLOR_DOMAIN_CHANGE, e -> updateView(true));
        //Data Scaling Events
        scene.addEventHandler(HyperspaceEvent.SCALING_AUTO_NORMALIZE, e -> {
            autoScaling = (boolean) e.object;
            updateView(false);
        });
        scene.addEventHandler(HyperspaceEvent.SCALING_MANUAL_BOUNDS, e -> {
            autoScaling = !(boolean) e.object;
            updateView(false);
        });
        scene.addEventHandler(HyperspaceEvent.SCALING_MEAN_CENTERED, e -> {
            meanCentered = (boolean) e.object;
            updateView(false);
        });
        //3D point rendering Events
        scene.addEventHandler(HyperspaceEvent.NODE_QUEUELIMIT_GUI, e -> queueLimit = (int) e.object);
        scene.addEventHandler(HyperspaceEvent.REFRESH_RATE_GUI, e -> hyperspaceRefreshRate = (long) e.object);
        scene.addEventHandler(HyperspaceEvent.POINT3D_SIZE_GUI, e -> {
            point3dSize = (double) e.object;
            updateView(false);
        });
        scene.addEventHandler(HyperspaceEvent.POINT_SCALE_GUI, e -> {
            pointScale = (double) e.object;
            notifyScaleChange();
            updateView(false);
            updateEllipsoids();
        });
        scene.addEventHandler(HyperspaceEvent.SCATTERBUFF_SCALING_GUI, e -> {
            scatterBuffScaling = (double) e.object;
            notifyScaleChange();
            updateView(false);
            updateEllipsoids();
        });
        scene.addEventHandler(HyperspaceEvent.RESET_MAX_ABS, e -> {
            maxAbsValue = 1.0;
            meanCenteredMaxAbsValue = 1.0;
            cubeWorld.maxAbsValue = maxAbsValue;
            cubeWorld.meanCenteredMaxAbsValue = meanCenteredMaxAbsValue;
            cubeWorld.setDirty(true); //signals to animation timer to redraw
            notifyScaleChange();
            updateView(false);
            updateEllipsoids();
        });
        scene.addEventHandler(HyperspaceEvent.RECOMPUTE_MAX_ABS, e -> {
            if (!featureVectors.isEmpty())
                updateMaxAndMeans();
            notifyScaleChange();
            updateView(false);
            updateEllipsoids();
        });

        scene.addEventHandler(ShadowEvent.SHOW_AXES_LABELS, e -> {
            nodeGroup.setVisible((boolean) e.object);
            labelGroup.setVisible((boolean) e.object);
        });
        scene.addEventHandler(ShadowEvent.FIXED_ORHOGRAPHIC_PROJECTION, e -> {
            if ((boolean) e.object) {
                projectionType = PROJECTION_TYPE.FIXED_ORTHOGRAPHIC;
                Platform.runLater(() -> {
                    dataXForm.getChildren().remove(scatterMesh3D);
                    sceneRoot.getChildren().add(scatterMesh3D);
                });
            }
        });
        scene.addEventHandler(ShadowEvent.ROTATING_PERSPECTIVE_PROJECTION, e -> {
            if ((boolean) e.object) {
                projectionType = PROJECTION_TYPE.ROTATING_PERSPECTIVE;
                Platform.runLater(() -> {
                    sceneRoot.getChildren().remove(scatterMesh3D);
                    dataXForm.getChildren().add(scatterMesh3D);
                });

            }
        });

        scene.addEventHandler(TimelineEvent.TIMELINE_SAMPLE_INDEX, e -> {
            anchorIndex = (int) e.object;
            if (anchorIndex < 0)
                anchorIndex = 0;
            else if (anchorIndex > scatterModel.data.size())
                anchorIndex = scatterModel.data.size();
            setSpheroidAnchor(true, anchorIndex);
        });

        AnimationTimer animationTimer = new AnimationTimer() {
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_MILLI = 1_000_000;

            @Override
            public void handle(long now) {
                sleepNs = hyperspaceRefreshRate * NANOS_IN_MILLI;
                if ((now - prevTime) < sleepNs) return;
                prevTime = now;
                if (isDirty) {
                    try {
                        updateView(false); // isDirty set to false inside.
                    } catch (Exception ex) {
                        System.out.println("Hyperspace Animation Timer: " + ex.getMessage());
                    }
                    isDirty = false;
                }
            }

            ;
        };
        animationTimer.start();
        Platform.runLater(() -> {
            cubeWorld.adjustPanelsByPos(cameraTransform.rx.getAngle(),
                cameraTransform.ry.getAngle(), cameraTransform.rz.getAngle());
            updateLabels();
            updateView(true);
            //create callout automatically puts the callout and node into a managed map
            FeatureVector dummy = FeatureVector.EMPTY_FEATURE_VECTOR("", 3);
            anchorCallout = radialOverlayPane.createCallout(anchorTSM, dummy, subScene);
            anchorCallout.play();
            anchorCallout.setVisible(false);
        });
    }

    public void updateOnLabelChange(List<FactorLabel> labels) {
        updatePNodeColorsAndVisibility();
        updateView(false);
        labels.forEach(factorLabel -> {
            ellipsoidToGMMessageMap.forEach((TriaxialSpheroidMesh t, GaussianMixture u) -> {
                PhongMaterial mat = (PhongMaterial) t.getMaterial();
                Color color = FactorLabel.getColorByLabel(u.getLabel()).deriveColor(1, 1, 1, 0.01);
                mat.setDiffuseColor(color);
                mat.setSpecularColor(Color.TRANSPARENT);
                t.setDiffuseColor(color);
                if (factorLabel.getLabel().contentEquals(u.getLabel()))
                    t.setVisible(factorLabel.getEllipsoidsVisible());
            });
        });
    }

    public void updateTrajectory3D(boolean overrideAuto) {
        //Clear out previous trajectory nodes
        boolean wasVisible = anchorTraj3D.isVisible();
        extrasGroup.getChildren().remove(anchorTraj3D);
        trajectorySphereGroup.getChildren().clear();
        //Rebuild the anchor trajectory
        anchorTraj3D = JavaFX3DUtils.buildPolyLineFromTrajectory(
            anchorTrajectory, 8.0f, Color.ALICEBLUE,
            trajectoryTailSize, trajectoryScale, sceneWidth, sceneHeight);
        anchorTraj3D.setVisible(wasVisible);
        extrasGroup.getChildren().add(0, anchorTraj3D);
        for (Point3D point : anchorTraj3D.points) {
            TriaxialSpheroidMesh tsm = createEllipsoid(anchorTraj3D.width / 4.0, anchorTraj3D.width / 4.0, anchorTraj3D.width / 4.0, Color.LIGHTBLUE);
            tsm.setTranslateX(point.x);
            tsm.setTranslateY(point.y);
            tsm.setTranslateZ(point.z);
            trajectorySphereGroup.getChildren().add(tsm);
        }
        if (updatingTrajectories || overrideAuto) {
            //now rebuild all the generic trajectories
            trajectoryGroup.getChildren().clear();
            trajToTraj3DMap.clear();
            for (Trajectory trajectory : Trajectory.getTrajectories()) {
                FeatureCollection fc = Trajectory.globalTrajectoryToFeatureCollectionMap.get(trajectory);
                trajectory.states.clear();
                ArrayList<double[]> newStates = new ArrayList<>(fc.getFeatures().size());
                for (FeatureVector fv : fc.getFeatures()) {
                    //scale each point for the hyperspace
                    newStates.add(scaleXYZ(
                        fv.getData().size() < 1 ? 0.0 : fv.getData().get(xFactorIndex),
                        fv.getData().size() < 2 ? 0.0 : fv.getData().get(yFactorIndex),
                        fv.getData().size() < 3 ? 0.0 : fv.getData().get(zFactorIndex)));
                }
                trajectory.states.addAll(newStates);
                Trajectory3D traj3D = JavaFX3DUtils.buildPolyLineFromTrajectory(
                    trajectory, 8.0f, trajectory.getColor(),
                    trajectory.states.size(), trajectoryScale, sceneWidth, sceneHeight);
                trajectoryGroup.getChildren().add(0, traj3D);
                trajToTraj3DMap.put(trajectory, traj3D);
                traj3D.setVisible(trajectory.getVisible());
            }
        }
    }

    public void makeHull(boolean useVisiblePoints, String label, Double tolerance) {
        //@TODO SMP Limit hull to points with that label
        shape3DToLabel.clear();
        //Add to hashmap so updateLabels() can manage the label position
        shape3DToLabel.put(xSphere, xLabel);
        shape3DToLabel.put(ySphere, yLabel);
        shape3DToLabel.put(zSphere, zLabel);

        Perspective3DNode[] pNodeArray = pNodes.toArray(Perspective3DNode[]::new);
        if (useVisiblePoints) {
            data = getVisiblePoints(pNodeArray);
        } else {
            data = getAllPoints(pNodeArray);
        }
        Manifold3D manifold3D = new Manifold3D(
            data.subList(1, data.size()), true, true, true, tolerance
        );

        System.out.println("scattermodel Manifold complete");
        manifolds.add(manifold3D);
        manifoldGroup.getChildren().add(manifold3D);
        shape3DToLabel.putAll(manifold3D.shape3DToLabel);
        updateLabels();
    }

    public void clearCallouts() {
        radialOverlayPane.clearCallouts();
    }

    private void setupSkyBox() {
        //Load SkyBox image
        Image
            top = new Image(Hyperspace3DPane.class.getResource("images/darkmetalbottom.png").toExternalForm()),
            bottom = new Image(Hyperspace3DPane.class.getResource("images/darkmetalbottom.png").toExternalForm()),
            left = new Image(Hyperspace3DPane.class.getResource("images/1500_blackgrid.png").toExternalForm()),
            right = new Image(Hyperspace3DPane.class.getResource("images/1500_blackgrid.png").toExternalForm()),
            front = new Image(Hyperspace3DPane.class.getResource("images/1500_blackgrid.png").toExternalForm()),
            back = new Image(Hyperspace3DPane.class.getResource("images/1500_blackgrid.png").toExternalForm());

        // Load Skybox AFTER camera is initialized
        double size = 100000D;
        skybox = new Skybox(
            top,
            bottom,
            left,
            right,
            front,
            back,
            size,
            camera
        );
        sceneRoot.getChildren().add(skybox);
        //Add some ambient light so folks can see it
        AmbientLight light = new AmbientLight(Color.WHITE);
        light.getScope().addAll(skybox);
        sceneRoot.getChildren().add(light);
        skybox.setVisible(false);
    }

    private void changeFeatureLayer(FeatureLayer featureLayer) {
        //ignore individual and just update all
        updatePNodeColorsAndVisibility();
        updateView(false);
    }

    private void changeFactorLabels(FactorLabel factorLabel) {
        updatePNodeColorsAndVisibility();
        updateView(false);
        ellipsoidToGMMessageMap.forEach((TriaxialSpheroidMesh t, GaussianMixture u) -> {
            PhongMaterial mat = (PhongMaterial) t.getMaterial();
            Color color = FactorLabel.getColorByLabel(
                u.getLabel()).deriveColor(1, 1, 1, 0.01);
            mat.setDiffuseColor(color);
            mat.setSpecularColor(Color.TRANSPARENT);
            t.setDiffuseColor(color);
            if (factorLabel.getLabel().contentEquals(u.getLabel()))
                t.setVisible(factorLabel.getEllipsoidsVisible());
        });
    }

    private void notifyIndexChange() {
        getScene().getRoot().fireEvent(
            new CommandTerminalEvent("X,Y,Z Indices = ("
                + xFactorIndex + ", " + yFactorIndex + ", " + zFactorIndex + ")",
                new Font("Consolas", 20), Color.GREEN));
    }

    private void notifyScaleChange() {
        getScene().getRoot().fireEvent(
            new CommandTerminalEvent("Point Scale = "
                + pointScale + ", Scatter Range = " + scatterBuffScaling,
                new Font("Consolas", 20), Color.GREEN));
    }

    public void resetView(double milliseconds, boolean rightNow) {
        if (!rightNow) {
            if (projectionType == PROJECTION_TYPE.FIXED_ORTHOGRAPHIC) {
                Timeline timeline = JavaFX3DUtils.transitionCameraTo(milliseconds, camera, cameraTransform,
                    0, 0, cameraDistance, -10.0, -45.0, 0.0);
                timeline.setOnFinished(eh ->
                    cubeWorld.adjustPanelsByPos(cameraTransform.rx.getAngle(),
                        cameraTransform.ry.getAngle(), cameraTransform.rz.getAngle()));
            } else {
                dataXForm.reset();
                cubeWorld.resetProjectionAffine();
            }
        } else {
            if (projectionType == PROJECTION_TYPE.FIXED_ORTHOGRAPHIC) {
                cameraTransform.rx.setAngle(-10);
                cameraTransform.ry.setAngle(-45.0);
                cameraTransform.rz.setAngle(0.0);
                camera.setTranslateX(0);
                camera.setTranslateY(0);
                camera.setTranslateZ(cameraDistance);
                cubeWorld.adjustPanelsByPos(cameraTransform.rx.getAngle(),
                    cameraTransform.ry.getAngle(), cameraTransform.rz.getAngle());
            } else {
                dataXForm.reset();
                cubeWorld.resetProjectionAffine();
            }
        }
    }

    public void intro(double milliseconds) {
        camera.setTranslateZ(DEFAULT_INTRO_DISTANCE);
        JavaFX3DUtils.zoomTransition(milliseconds, camera, cameraDistance);
    }

    public void outtro(double milliseconds) {
        JavaFX3DUtils.zoomTransition(milliseconds, camera, DEFAULT_INTRO_DISTANCE);
    }

    public void updateAll() {
        Platform.runLater(() -> {
            highlightedPoint.setRadius(point3dSize / 2.0);
            updateView(true);
            cubeWorld.setDirty(true); //signals to animation timer to redraw
            updateEllipsoids();
        });
    }

    private void mouseDragCamera(MouseEvent me) {
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);
        double modifier = 1.0;
        double modifierFactor = 0.1;  //@TODO SMP connect to sensitivity property

        if (me.isControlDown()) {
            modifier = 0.1;
        }
        if (me.isShiftDown()) {
            modifier = 25.0;
        }
        if (projectionType == PROJECTION_TYPE.FIXED_ORTHOGRAPHIC) {
            if (me.isPrimaryButtonDown()) {
                if (me.isAltDown()) { //roll
                    cameraTransform.rz.setAngle(((cameraTransform.rz.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
                } else {
                    cameraTransform.ry.setAngle(((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
                    cameraTransform.rx.setAngle(
                        ((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
                }
            } else if (me.isMiddleButtonDown()) {
                cameraTransform.t.setX(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3); // -
                cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3); // -
            }
            cubeWorld.adjustPanelsByPos(cameraTransform.rx.getAngle(), cameraTransform.ry.getAngle(), cameraTransform.rz.getAngle());
        } else {
            //System.out.println("Rotating data and not camera...");
            double yChange = (((mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            double xChange = (((-mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            dataXForm.addRotation(yChange, Rotate.Y_AXIS);
            dataXForm.addRotation(xChange, Rotate.X_AXIS);

            cubeWorld.projectionAffine.setToTransform(dataXForm.getLocalToSceneTransform());
            cubeWorld.setDirty(true); //signals to animation timer to redraw
        }
        updateLabels();
        radialOverlayPane.updateCalloutHeadPoints(subScene);
    }

    private void updateLabels() {
        if (xFactorIndex < featureLabels.size())
            xLabel.setText(featureLabels.get(xFactorIndex));
        else
            xLabel.setText("Factor X(" + String.valueOf(xFactorIndex) + ")");
        if (yFactorIndex < featureLabels.size())
            yLabel.setText(featureLabels.get(yFactorIndex));
        else
            yLabel.setText("Factor Y(" + String.valueOf(yFactorIndex) + ")");
        if (zFactorIndex < featureLabels.size())
            zLabel.setText(featureLabels.get(zFactorIndex));
        else
            zLabel.setText("Factor Z(" + String.valueOf(zFactorIndex) + ")");
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

    public void updateView(boolean forcePNodeUpdate) {
        if (forcePNodeUpdate) //pointScale
            updatePNodes();
        if (null != scatterMesh3D) {
            Perspective3DNode[] pNodeArray = pNodes.toArray(Perspective3DNode[]::new);

            data = getVisiblePoints(pNodeArray);
            //Implementation for directional arrows based on additional dimensions
            if (directionEnabled)
                endPoints = getEndPoints(pNodeArray, 5);
            else //default is equilateral tetrahedra (no direction)
                endPoints = getFixedEndPoints(pNodeArray, 0f);
            scatterModel.pointScale = pointScale;
            Platform.runLater(() -> hardDraw());
        }
    }

    private void hardDraw() {
        if (heightChanged) { //if it hasn't changed, don't call expensive height change
            scatterMesh3D.setHeight(point3dSize);
            heightChanged = false;
        }
        //long startTime2 = System.nanoTime();
        //if there is data and their end points are bounded
        //set the start and end points of the mesh
        //18 ms for 20k points
        if (!data.isEmpty() && !endPoints.isEmpty())
            scatterMesh3D.setScatterDataAndEndPoints(data, endPoints);
        //@DEBUG SMP Rendering timing print
        //System.out.println("UpdateView setScatterDataAndEndPoints time: "
        //    + Utils.totalTimeString(startTime2));
        //Since we changed the mesh unfortunately we have to reset the color mode
        //otherwise the triangles won't have color.
        //5ms for 20k points
        scatterMesh3D.setTextureModeVertices3D(TOTAL_COLORS, colorByLabelFunction, 0.0, 360.0);
        isDirty = false;
    }

    private ArrayList<Point3D> getAllPoints(Perspective3DNode[] pNodeArray) {
        //Build scatter model
        if (null == scatterModel)
            scatterModel = new DirectedScatterDataModel();
        //clear existing data
        scatterModel.reset();
        //synch model reflection status with current boolean value
        scatterModel.reflectY = reflectY;
        //Add our nodes to the model's collection
        Collections.addAll(scatterModel.pNodes, pNodeArray);
        //Check flag to see if we are auto normalizing
        double buff = scatterBuffScaling;
        if (autoScaling)
            buff = meanCentered ? meanCenteredMaxAbsValue : maxAbsValue;
        scatterModel.setLimits(-buff, buff, -buff, buff, -buff, buff);
        //Check flag to see if we are mean centering
        if (meanCentered && !meanVector.isEmpty()) {
            double xShift = meanVector.get(xFactorIndex);
            double yShift = meanVector.get(yFactorIndex);
            double zShift = meanVector.get(zFactorIndex);
            scatterModel.setShifts(xShift, yShift, zShift);
        } else {
            scatterModel.setShifts(0, 0, 0);
        }
        return scatterModel.getVisiblePoints(false, sceneWidth, sceneHeight);
    }

    private ArrayList<Point3D> getVisiblePoints(Perspective3DNode[] pNodeArray) {
        //Build scatter model
        if (null == scatterModel)
            scatterModel = new DirectedScatterDataModel();
        //clear existing data
        scatterModel.reset();
        //synch model reflection status with current boolean value
        scatterModel.reflectY = reflectY;
        //Add our nodes to the model's collection
        Collections.addAll(scatterModel.pNodes, pNodeArray);
        //True calls updateModel which is where the pain is
        updateScatterLimits(scatterBuffScaling, true);
        return scatterModel.data;
    }

    private ArrayList<Point3D> getEndPoints(Perspective3DNode[] pNodes, float fixedSize) {
        ArrayList<Point3D> ends = new ArrayList<>(pNodes.length);
        for (int i = 0; i < pNodes.length; i++) {
            ends.add(new Point3D(pNodes[i].xDirCoord * fixedSize,
                pNodes[i].xDirCoord * fixedSize, pNodes[i].xDirCoord * fixedSize));
        }
        return ends;
    }

    private ArrayList<Point3D> getFixedEndPoints(Perspective3DNode[] pNodes, float fixedSize) {
        Point3D[] endArray = new Point3D[pNodes.length];
        //Fix endpoints so they are just zero adds
        Arrays.parallelSetAll(endArray, i -> new Point3D(fixedSize, fixedSize, fixedSize));
        return new ArrayList<>(Arrays.asList(endArray));
    }

    public void updateScatterLimits(double bufferScale, boolean updateModel) {
        //Check flag to see if we are auto normalizing
        double buff = bufferScale;
        if (autoScaling)
            buff = meanCentered ? meanCenteredMaxAbsValue : maxAbsValue;
        scatterModel.setLimits(-buff, buff, -buff, buff, -buff, buff);
        //Check flag to see if we are mean centering
        if (meanCentered && !meanVector.isEmpty()) {
            double xShift = meanVector.get(xFactorIndex);
            double yShift = meanVector.get(yFactorIndex);
            double zShift = meanVector.get(zFactorIndex);
            scatterModel.setShifts(xShift, yShift, zShift);
        } else {
            scatterModel.setShifts(0, 0, 0);
        }
        if (updateModel)
            scatterModel.updateModel(sceneWidth, sceneHeight);
    }

    public Perspective3DNode addPNodeFromSeed(HyperspaceSeed seed) {
        //make sure our extra dimensions are within the data vector's width
        int xDir = seed.xDir >= seed.vector.length ? seed.vector.length - 1 : seed.xDir;
        int yDir = seed.yDir >= seed.vector.length ? seed.vector.length - 1 : seed.yDir;
        int zDir = seed.zDir >= seed.vector.length ? seed.vector.length - 1 : seed.zDir;
        Perspective3DNode pNode = new Perspective3DNode(
            seed.vector[seed.x], seed.vector[seed.y], seed.vector[seed.z],
            seed.vector[xDir], seed.vector[yDir], seed.vector[zDir],
            seed);
        double minX = -1.0;
        double minY = -1.0;
        double minZ = -1.0;
        double domainWidth = 2.0;
        if (autoScaling) {
            domainWidth = meanCentered ? 2 * meanCenteredMaxAbsValue : 2 * maxAbsValue;
            minX = meanCentered ? -meanCenteredMaxAbsValue : -maxAbsValue;
            minY = meanCentered ? -meanCenteredMaxAbsValue : -maxAbsValue;
            minZ = meanCentered ? -meanCenteredMaxAbsValue : -maxAbsValue;
        }
        pNode.nodeColor = Perspective3DNode.getPNodeColor(colorMode, colorMap, seed,
            minX, minY, minZ, domainWidth);
        pNode.visible = pNode.factorAnalysisSeed.visible;
        pNodes.add(pNode);
        return pNode;
    }

    private void updatePNodes() {
        pNodes.clear();
        HyperspaceSeed[] seeds = hyperspaceSeeds.toArray(HyperspaceSeed[]::new);
        HyperspaceSeed seed;
        for (int i = 0; i < seeds.length; i++) {
            seed = seeds[i];
            seed.x = seed.vector.length > xFactorIndex ? xFactorIndex : seed.vector.length - 1;
            seed.y = seed.vector.length > yFactorIndex ? yFactorIndex : seed.vector.length - 1;
            seed.z = seed.vector.length > zFactorIndex ? zFactorIndex : seed.vector.length - 1;
            seed.xDir = seed.vector.length > xDirFactorIndex ? xDirFactorIndex : seed.vector.length - 1;
            seed.yDir = seed.vector.length > yDirFactorIndex ? yDirFactorIndex : seed.vector.length - 1;
            seed.zDir = seed.vector.length > zDirFactorIndex ? zDirFactorIndex : seed.vector.length - 1;
            seed.visible = FactorLabel.visibilityByLabel(seed.label)
                && FeatureLayer.visibilityByIndex(seed.layer)
                && VisibilityMap.visibilityByIndex(i);
            addPNodeFromSeed(seed);
        }
    }

    private void updatePNodeColorsAndVisibility() {
        double mx = -1.0;
        double my = -1.0;
        double mz = -1.0;
        double dw = 2.0;
        if (autoScaling) {
            dw = meanCentered ? 2 * meanCenteredMaxAbsValue : 2 * maxAbsValue;
            mx = meanCentered ? -meanCenteredMaxAbsValue : -maxAbsValue;
            my = meanCentered ? -meanCenteredMaxAbsValue : -maxAbsValue;
            mz = meanCentered ? -meanCenteredMaxAbsValue : -maxAbsValue;
        }
        double minX = mx;
        double minY = my;
        double minZ = mz;
        double domainWidth = dw;

        pNodes.parallelStream().forEach(pNode -> {
            pNode.nodeColor = Perspective3DNode.getPNodeColor(colorMode, colorMap, pNode.factorAnalysisSeed,
                minX, minY, minZ, domainWidth);
            pNode.visible = FactorLabel.visibilityByLabel(pNode.factorAnalysisSeed.label)
                && FeatureLayer.visibilityByIndex(pNode.factorAnalysisSeed.layer)
                && VisibilityMap.visibilityByPNode(pNode);
        });
    }

    private void updatePNodeIndices(int x, int y, int z, int xDir, int yDir, int zDir) {
        //always skip the first node since its a dummy node.
        pNodes.parallelStream().skip(1).forEach(p -> p.setParamsByIndex(x, y, z, xDir, yDir, zDir));
    }

    public void loadDirectedMesh() {
        //System.out.println("Loading Directed Mesh...");
        //@TODO SMP this is where you might load some dank data from a file
        showAll();
        updatePNodes();
        Perspective3DNode[] pNodeArray = pNodes.toArray(Perspective3DNode[]::new);
        data = getVisiblePoints(pNodeArray);
        endPoints = getFixedEndPoints(pNodeArray, 0f);
        //System.out.println("Rendering 3D Mesh...");
        if (null != scatterMesh3D) {
            scatterMesh3D.setDrawMode(DrawMode.FILL);
            scatterMesh3D.setTextureModeVertices3D(TOTAL_COLORS, colorByLabelFunction, 0.0, 360.0);
            scatterMesh3D.setScatterDataAndEndPoints(data, endPoints);
        } else {
            scatterMesh3D = new DirectedScatterMesh(data, endPoints, true, point3dSize, 0);
            scatterMesh3D.setDrawMode(DrawMode.FILL);
            scatterMesh3D.setTextureModeVertices3D(TOTAL_COLORS, colorByLabelFunction, 0.0, 360.0);
            highlightedPoint.visibleProperty().bind(scatterMesh3D.visibleProperty());
            highlightedPoint.setMouseTransparent(true);
            highlightedPoint.setOpacity(0.5);
            scatterMesh3D.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                PickResult n = event.getPickResult();
                int pointId1 = n.getIntersectedFace() / 4;
                //System.out.println("Intersected Face:  " + n.getIntersectedFace());
                if (pointId1 < scatterMesh3D.scatterDataProperty().get().size()) {
                    Point3D pt1 = scatterMesh3D.scatterDataProperty().get().get(pointId1);
                    Sphere sphere = new Sphere(1, 1);
                    sphere.setTranslateX(pt1.x);
                    sphere.setTranslateY(pt1.y);
                    sphere.setTranslateZ(pt1.z);
                    nodeGroup.getChildren().add(sphere);
                    //find correct feature vector
                    int correctIndex = scatterModel.findIndexFromVisibleFacePoint(pointId1);
                    if (correctIndex >= 0)
                        radialOverlayPane.createCallout(sphere,
                            featureVectors.get(correctIndex), subScene);
                }
            });
            scatterMesh3D.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
                PickResult n = event.getPickResult();
                final int pointId1 = n.getIntersectedFace() / 4;
                Platform.runLater(() -> {
                    if (pointId1 < scatterMesh3D.scatterDataProperty().get().size()) {
                        Point3D pt1 = scatterMesh3D.scatterDataProperty().get().get(pointId1);
                        Translate highlightTranslate = new Translate(pt1.x, pt1.y, pt1.z);
                        highlightedPoint.getTransforms().clear();
                        highlightedPoint.getTransforms().add(highlightTranslate);
                        highlightedPoint.setUserData(pt1);
                        highlightedPoint.setRadius(point3dSize / 2.0);
                        int correctIndex = scatterModel.findIndexFromVisibleFacePoint(pointId1);
                        if (correctIndex >= 0 && correctIndex <= featureVectors.size()) {
                            scene.getRoot().fireEvent(new FeatureVectorEvent(
                                FeatureVectorEvent.SELECT_FEATURE_VECTOR,
                                featureVectors.get(correctIndex), featureLabels));
                        }
                        //@TODO SMP Update HUD with hovered point
                        //HyperspaceSeed seed = seedToDataMap.get(pt1);
                        //dataHudText.setText(seed.prettyPrint());
                    }
                });
            });
        }
        Platform.runLater(() -> {
            //System.out.println("Hyperspace render complete.");
            if (!sceneRoot.getChildren().contains(scatterMesh3D)) {
                sceneRoot.getChildren().add(scatterMesh3D);
                //Add some ambient light so folks can see it
                AmbientLight light = new AmbientLight(Color.WHITE);
                light.getScope().addAll(scatterMesh3D);
                sceneRoot.getChildren().add(light);
                sceneRoot.getChildren().add(ellipsoidGroup);
            }
        });
    }

    private TriaxialSpheroidMesh createEllipsoid(double major, double minor, double gamma, Color color) {
        TriaxialSpheroidMesh triaxialSpheroid = new TriaxialSpheroidMesh(32, major, minor, gamma);
        triaxialSpheroid.setDrawMode(DrawMode.FILL);
        triaxialSpheroid.setCullFace(CullFace.BACK);
        PhongMaterial mat = new PhongMaterial(color.deriveColor(1, 1, 1, 0.01));
        mat.setDiffuseColor(color.deriveColor(1, 1, 1, 0.01));
        mat.setSpecularColor(Color.TRANSPARENT);
        triaxialSpheroid.setMaterial(mat);
        triaxialSpheroid.setDiffuseColor(color.deriveColor(1, 1, 1, 0.01));
        return triaxialSpheroid;
    }

    public void clearAll() {
        featureLabels.clear();
        xFactorIndex = 0;
        yFactorIndex = 1;
        zFactorIndex = 2;
        Platform.runLater(() -> scene.getRoot().fireEvent(
            new HyperspaceEvent(HyperspaceEvent.FACTOR_COORDINATES_KEYPRESS,
                new CoordinateSet(xFactorIndex, yFactorIndex, zFactorIndex))));
        notifyIndexChange();
        ellipsoidGroup.getChildren().clear();
        ellipsoidToGMMessageMap.clear();
        ellipsoidToGMDataMap.clear();
        clearFeatureVectors();
        hyperspaceSeeds.clear();
        pNodes.clear();
        seedToDataMap.clear();
        seedToEndMap.clear();
        shape3DToLabel.clear();
        //Add to hashmap so updateLabels() can manage the label position
        shape3DToLabel.put(xSphere, xLabel);
        shape3DToLabel.put(ySphere, yLabel);
        shape3DToLabel.put(zSphere, zLabel);
        //Create dummy seed and pnode for mesh to form around
        double[] features = new double[]{0, 0, 0, 0, 0, 0};
        HyperspaceSeed seed = new HyperspaceSeed(0, 1, 2, 3, 4, 5, features);
        hyperspaceSeeds.add(seed);
        addPNodeFromSeed(seed);
        VisibilityMap.clearAll();
        updateLabels(); //make sure all remaining floating labels and strings are updated
    }

    public void showAll() {
        updateView(true);
        updateEllipsoids();
    }

    @Override
    public void setSpheroidAnchor(boolean animate, int index) {
        if (index >= scatterModel.data.size()) {
            //System.out.println("Requested anchor index of " + index + " greater than scatterModel.data size");
            return;
        } else if (index < 0) {
            //System.out.println("Requested anchor index of " + index + " less than zero.");
            return;
        }
        Point3D p3d = scatterModel.data.get(index);
        anchorTSM.setTranslateX(p3d.x);
        anchorTSM.setTranslateY(p3d.y);
        anchorTSM.setTranslateZ(p3d.z);

        //make sure we have the latest states using the latest feature indices
        anchorTrajectory.states.clear();
        List<Point3D> p3ds = scatterModel.data;
        if (index < scatterModel.data.size())
            p3ds = scatterModel.data.subList(0, index + 1);
        if (!p3ds.isEmpty()) {
            for (Point3D p : p3ds) {
                anchorTrajectory.states.add(new double[]{p.x, p.y, p.z});
            }
            //recreate and add to the scene the 3D trajectory
            updateTrajectory3D(false);
        }
        if (index < featureVectors.size()) {
            scene.getRoot().fireEvent(new FeatureVectorEvent(
                FeatureVectorEvent.SELECT_FEATURE_VECTOR,
                featureVectors.get(index), featureLabels));
            //try to update the callout anchored to the lead state
            radialOverlayPane.updateCalloutByFeatureVector(anchorCallout, featureVectors.get(index));
            radialOverlayPane.updateCalloutHeadPoint(anchorTSM, anchorCallout, subScene);
        }
    }

    private double[] scaleXYZ(double x, double y, double z) {
        //pixel ranges we wish to fit our scaling to
        double halfSceneWidth = sceneWidth / 2.0;
        double halfSceneHeight = sceneHeight / 2.0;
        //offset used to center placement of points in scene after scaling
        float quarterSceneWidth = (float) sceneWidth / 4.0f;
        //modifiers used to manually adjust scaling by user
        double buff = pointScale * scatterBuffScaling;
        if (autoScaling)
            buff = meanCentered ? meanCenteredMaxAbsValue : maxAbsValue;
        //formula bounds
        double minX = -buff;
        double rangeX = 2 * buff;
        double minY = -buff;
        double rangeY = 2 * buff;
        double minZ = -buff;
        double rangeZ = 2 * buff;
        //Check flag to see if we are mean centering
        double xShift = meanCentered && !meanVector.isEmpty() ? meanVector.get(xFactorIndex) : 0.0;
        double yShift = meanCentered && !meanVector.isEmpty() ? meanVector.get(yFactorIndex) : 0.0;
        double zShift = meanCentered && !meanVector.isEmpty() ? meanVector.get(zFactorIndex) : 0.0;
        //linear coordinate transformation of each covariance to match our 3D scene
        //X ==> X Positive
        double xCoord = (float) ((((x - xShift) * pointScale - minX) * halfSceneWidth) / rangeX);
        //Y ==> Z Positive
        double yCoord = (float) ((((y - yShift) * pointScale - minY) * halfSceneWidth) / rangeY);
        //Z ==> Y Positive
        double zCoord = (float) ((((z - zShift) * pointScale - minZ) * halfSceneWidth) / rangeZ);
        //our data is centered and we need to center again within 3D coordinate system
        double shiftedX = xCoord - quarterSceneWidth;
        double shiftedY = yCoord - quarterSceneWidth;
        double shiftedZ = zCoord - quarterSceneWidth;
        if (reflectY)
            shiftedY = -shiftedY;

        return new double[]{shiftedX, shiftedY, shiftedZ};
    }

    /**
     * @param gaussianMixture
     */
    @Override
    public void addGaussianMixture(GaussianMixture gaussianMixture) {
        //pixel ranges we wish to fit our scaling to
        double halfSceneWidth = sceneWidth / 2.0;
        double halfSceneHeight = sceneHeight / 2.0;
        //offset used to center placement of points in scene after scaling
        float quarterSceneWidth = (float) sceneWidth / 4.0f;
        //modifiers used to manually adjust scaling by user
        double buff = pointScale * scatterBuffScaling;
        if (autoScaling)
            buff = meanCentered ? meanCenteredMaxAbsValue : maxAbsValue;
        //formula bounds
        double minX = -buff;
        double rangeX = 2 * buff;
        double minY = -buff;
        double rangeY = 2 * buff;
        double minZ = -buff;
        double rangeZ = 2 * buff;
        //Check flag to see if we are mean centering
        double xShift = meanCentered && !meanVector.isEmpty() ? meanVector.get(xFactorIndex) : 0.0;
        double yShift = meanCentered && !meanVector.isEmpty() ? meanVector.get(yFactorIndex) : 0.0;
        double zShift = meanCentered && !meanVector.isEmpty() ? meanVector.get(zFactorIndex) : 0.0;

        Color labelColor = FactorLabel.getColorByLabel(gaussianMixture.getLabel());
        //for each GMM data chunk make a 3D ellipsoid
        gaussianMixture.getData().forEach(gmd -> {
            double xCov = Math.sqrt(gmd.getEllipsoidDiagonal().get(xFactorIndex));
            double yCov = Math.sqrt(gmd.getEllipsoidDiagonal().get(yFactorIndex));
            double zCov = Math.sqrt(gmd.getEllipsoidDiagonal().get(zFactorIndex));

            //linear coordinate transformation of each covariance to match our 3D scene
            //X ==> X Positive
            double xCoord = (float) (((pointScale * xCov - minX) * halfSceneWidth) / rangeX);
            //Y ==> Z Positive
            double yCoord = (float) (((pointScale * yCov - minY) * halfSceneHeight) / rangeY);
            //Z ==> Y Positive
            double zCoord = (float) (((pointScale * zCov - minZ) * halfSceneWidth) / rangeZ);

            //create 3D object...
            //subtracting centering offset because numbers are in 3D scene coordinates now
            TriaxialSpheroidMesh tsm = createEllipsoid(
                xCoord - quarterSceneWidth,
                yCoord - quarterSceneWidth, //swapped above with z
                zCoord - quarterSceneWidth, //swapped above with y
                labelColor);
            if (reflectY) {
                tsm.setMinorRadius(tsm.getMinorRadius() * -1);
            }

            //Determine origin location of 3D object in 3D scene coordinates
            //linear coordinate transformation of each mean to match our 3D scene
            double meanX = gmd.getMean().get(xFactorIndex) - xShift;
            double meanY = gmd.getMean().get(yFactorIndex) - yShift;
            double meanZ = gmd.getMean().get(zFactorIndex) - zShift;
            //X ==> X Positive
            xCoord = (float) (((pointScale * meanX - minX) * halfSceneWidth) / rangeX);
            //Y ==> Z Positive
            yCoord = (float) (((pointScale * meanY - minY) * halfSceneHeight) / rangeY);
            //Z ==> Y Positive
            zCoord = (float) (((pointScale * meanZ - minZ) * halfSceneWidth) / rangeZ);

            //translate 3D object...
            //subtracting centering offset because numbers are in 3D scene coordinates now
            tsm.setTranslateX(xCoord - quarterSceneWidth);
            tsm.setTranslateY(yCoord - quarterSceneWidth);
            tsm.setTranslateZ(zCoord - quarterSceneWidth);

            if (reflectY) {
                tsm.setTranslateY(tsm.getTranslateY() * -1);
            }

            //@DEBUG SMP
            Sphere sphere = new Sphere(10);
            sphere.setTranslateX(xCoord - quarterSceneWidth);
            sphere.setTranslateY(yCoord - quarterSceneWidth);
            sphere.setTranslateZ(zCoord - quarterSceneWidth);
            sphere.setMaterial(new PhongMaterial(Color.ALICEBLUE));
            Platform.runLater(() -> debugGroup.getChildren().add(sphere));

            Platform.runLater(() -> {
                //add to the 3D scene
                ellipsoidGroup.getChildren().add(tsm);
                //Add to hashmap tracker at the higher meta data level
                ellipsoidToGMMessageMap.put(tsm, gaussianMixture);
                //add to hashmap tracker at the individual model data level
                ellipsoidToGMDataMap.put(tsm, gmd);
            });
        });
    }

    private void updateEllipsoids() {
        //pixel ranges we wish to fit our scaling to
        double halfSceneWidth = sceneWidth / 2.0;
        double halfSceneHeight = sceneHeight / 2.0;
        //offset used to center placement of points in scene after scaling
        float quarterSceneWidth = (float) sceneWidth / 4.0f;
        //modifiers used to manually adjust scaling by user
        double buff = pointScale * scatterBuffScaling;
        if (autoScaling)
            buff = meanCentered ? meanCenteredMaxAbsValue : maxAbsValue;
        //formula bounds
        double minX = -buff;
        double rangeX = 2 * buff;
        double minY = -buff;
        double rangeY = 2 * buff;
        double minZ = -buff;
        double rangeZ = 2 * buff;
        //Check flag to see if we are mean centering
        double xShift = meanCentered && !meanVector.isEmpty() ? meanVector.get(xFactorIndex) : 0.0;
        double yShift = meanCentered && !meanVector.isEmpty() ? meanVector.get(yFactorIndex) : 0.0;
        double zShift = meanCentered && !meanVector.isEmpty() ? meanVector.get(zFactorIndex) : 0.0;

        ellipsoidToGMDataMap.forEach((TriaxialSpheroidMesh tri, GaussianMixtureData gmd) -> {
//            double xCov = Math.sqrt(gmd.getCovariance().get(xFactorIndex));
//            double yCov = Math.sqrt(gmd.getCovariance().get(yFactorIndex));
//            double zCov = Math.sqrt(gmd.getCovariance().get(zFactorIndex));
            double xCov = Math.sqrt(gmd.getEllipsoidDiagonal().get(xFactorIndex));
            double yCov = Math.sqrt(gmd.getEllipsoidDiagonal().get(yFactorIndex));
            double zCov = Math.sqrt(gmd.getEllipsoidDiagonal().get(zFactorIndex));

            //linear coordinate transformation of each covariance to match our 3D scene
            //X ==> X Positive
            double xCoord = (float) (((pointScale * xCov - minX) * halfSceneWidth) / rangeX);
            //Y ==> Z Positive
            double yCoord = (float) (((pointScale * yCov - minY) * halfSceneHeight) / rangeY);
            //Z ==> Y Positive
            double zCoord = (float) (((pointScale * zCov - minZ) * halfSceneWidth) / rangeZ);
            //resize 3D object...
            //subtracting centering offset because numbers are in 3D scene coordinates now
            tri.setMajorRadius(xCoord - quarterSceneWidth);
            tri.setMinorRadius(yCoord - quarterSceneWidth);
            tri.setGammaRadius(zCoord - quarterSceneWidth);
            if (reflectY) {
                tri.setMinorRadius(tri.getMinorRadius() * -1);
            }

            double meanX = gmd.getMean().get(xFactorIndex) - xShift;
            double meanY = gmd.getMean().get(yFactorIndex) - yShift;
            double meanZ = gmd.getMean().get(zFactorIndex) - zShift;
            //Determine origin location of 3D object in 3D scene coordinates
            //linear coordinate transformation of each mean to match our 3D scene
            //X ==> X Positive
            xCoord = (float) (((pointScale * meanX - minX) * halfSceneWidth) / rangeX);
            //Y ==> Z Positive
            yCoord = (float) (((pointScale * meanY - minY) * halfSceneHeight) / rangeY);
            //Z ==> Y Positive
            zCoord = (float) (((pointScale * meanZ - minZ) * halfSceneWidth) / rangeZ);
            //translate 3D object...
            //subtracting centering offset because numbers are in 3D scene coordinates now
            tri.setTranslateX(xCoord - quarterSceneWidth);
            tri.setTranslateY(yCoord - quarterSceneWidth);
            tri.setTranslateZ(zCoord - quarterSceneWidth);
            if (reflectY) {
                tri.setTranslateY(tri.getTranslateY() * -1);
            }
        });
    }

    @Override
    public void addFeatureVector(FeatureVector featureVector) {
        featureVectors.add(featureVector);
        double[] features = FeatureVector.mapToStateArray.apply(featureVector);
        HyperspaceSeed seed = new HyperspaceSeed(
            xFactorIndex, yFactorIndex, zFactorIndex,
            xDirFactorIndex, yDirFactorIndex, zDirFactorIndex, features);
        seed.label = featureVector.getLabel();
        seed.layer = featureVector.getLayer();
        seed.score = featureVector.getScore();
        seed.pfa = featureVector.getPfa();
        seed.visible = FactorLabel.visibilityByLabel(seed.label)
            && FeatureLayer.visibilityByIndex(seed.layer);
        hyperspaceSeeds.add(seed);
        trimQueueNow();
        addPNodeFromSeed(seed);
        cubeWorld.setDirty(true); //signals to animation timer to redraw
        //rather than directly call updateView() let the rendering thread know there is a change
        isDirty = true;
    }

    @Override
    public void addFeatureCollection(FeatureCollection featureCollection) {
        Platform.runLater(() -> {
            getScene().getRoot().fireEvent(
                new CommandTerminalEvent("Loading Feature Collection... ",
                    new Font("Consolas", 20), Color.GREEN));
            ProgressStatus ps1 = new ProgressStatus("Loading Feature Collection...", -1);
            getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps1));
        });
        if (!featureVectors.isEmpty()) {
            Alert alert = new Alert(AlertType.CONFIRMATION,
                "Data queue currently has " + featureVectors.size() + " items.\n"
                    + "Clear the queue before import?",
                ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            alert.setTitle("Feature Collection Import");
            alert.setHeaderText("Data queue currently has " + featureVectors.size() + " items.");
            alert.setContentText("Clear the queue before import?");
            alert.setGraphic(ResourceUtils.loadIcon("alert", 75));
            alert.initStyle(StageStyle.TRANSPARENT);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setBackground(Background.EMPTY);
            dialogPane.getScene().setFill(Color.TRANSPARENT);

            String DIALOGCSS = this.getClass().getResource("/edu/jhuapl/trinity/css/dialogstyles.css").toExternalForm();
            dialogPane.getStylesheets().add(DIALOGCSS);

            Optional<ButtonType> optBT = alert.showAndWait();
            if (optBT.get().equals(ButtonType.CANCEL))
                return;
            if (optBT.get().equals(ButtonType.YES))
                clearAll();
        }

        Task task = new Task() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    getScene().getRoot().fireEvent(
                        new CommandTerminalEvent("Rendering Feature Collection... ",
                            new Font("Consolas", 20), Color.GREEN));
                });

                int start = 0;
                int end = featureCollection.getFeatures().size();
                if (end >= queueLimit) {
                    start = end - queueLimit;
                }

                featureCollection.getFeatures().subList(start, end)
                    .stream().forEach(featureVector -> {
                        //fail safe to gaurantee 3 dimensions
                        if (featureVector.getData().size() < 1)
                            featureVector.getData().add(0.0);
                        if (featureVector.getData().size() < 2)
                            featureVector.getData().add(0.0);
                        if (featureVector.getData().size() < 3)
                            featureVector.getData().add(0.0);
                        featureVectors.add(featureVector);
                        HyperspaceSeed seed = new HyperspaceSeed(
                            xFactorIndex, yFactorIndex, zFactorIndex,
                            xDirFactorIndex, yDirFactorIndex, zDirFactorIndex,
                            FeatureVector.mapToStateArray.apply(featureVector));
                        seed.label = featureVector.getLabel();
                        seed.layer = featureVector.getLayer();
                        seed.score = featureVector.getScore();
                        seed.pfa = featureVector.getPfa();
                        hyperspaceSeeds.add(seed);
                        VisibilityMap.visibilityList.add(true);
                        addPNodeFromSeed(seed);
                    });

                trimQueueNow();
                //Need to rotate  visibility index
                VisibilityMap.clearAll();
                VisibilityMap.resetVisibilityList(pNodes.toArray(Perspective3DNode[]::new), true);
                updateMaxAndMeans();
                Platform.runLater(() -> {
                    ProgressStatus ps = new ProgressStatus("Finished loading vectors.", -1);
                    getScene().getRoot().fireEvent(
                        new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR, ps));
                });
                return null;
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public void updateMaxAndMeans() {
        meanVector = FeatureVector.getMeanVector(featureVectors);
        maxAbsValue = FeatureVector.getMaxAbsValue(featureVectors);
        meanCenteredMaxAbsValue = FeatureVector.getMeanCenteredMaxAbsValue(featureVectors, meanVector);

        String str = "Mean Centered MaxAbsValue: " + meanCenteredMaxAbsValue;
        cubeWorld.meanVector.clear();
        cubeWorld.meanVector.addAll(meanVector);
        cubeWorld.maxAbsValue = maxAbsValue;
        cubeWorld.meanCenteredMaxAbsValue = meanCenteredMaxAbsValue;
        isDirty = true; //let the rendering thread know there is a change
        cubeWorld.setDirty(true); //signals to animation timer to redraw
        Platform.runLater(() -> {
            getScene().getRoot().fireEvent(
                new HyperspaceEvent(HyperspaceEvent.NEW_MAX_ABS, maxAbsValue));
            getScene().getRoot().fireEvent(
                new HyperspaceEvent(HyperspaceEvent.NEW_MEANCENTEREDMAX_ABS, meanCenteredMaxAbsValue));
            getScene().getRoot().fireEvent(
                new CommandTerminalEvent(str, new Font("Consolas", 20), Color.GREEN));
        });
    }

    public void trimQueueNow() {
        while (hyperspaceSeeds.size() > queueLimit) {
            hyperspaceSeeds.poll();
        }
        while (pNodes.size() > queueLimit) {
            pNodes.poll();
        }
        while (featureVectors.size() > queueLimit) {
            featureVectors.remove(0);
        }
    }

    @Override
    public void clearGaussianMixtures() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addGaussianMixtureCollection(GaussianMixtureCollection gaussianMixtureCollection) {
        if (!ellipsoidGroup.getChildren().isEmpty()) {
            Alert alert = new Alert(AlertType.CONFIRMATION,
                "There are  currently " + ellipsoidGroup.getChildren().size() + " ellipsoid items.\n"
                    + "Clear the ellipsoid group before import?",
                ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            alert.setTitle("Gaussian Mixture Collection Import");
            alert.setHeaderText("There are  currently " + ellipsoidGroup.getChildren().size() + " ellipsoid items.");
            alert.setContentText("Clear the ellipsoid group before import?");
            alert.setGraphic(ResourceUtils.loadIcon("alert", 75));
            alert.initStyle(StageStyle.TRANSPARENT);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setBackground(Background.EMPTY);
            dialogPane.getScene().setFill(Color.TRANSPARENT);
            String DIALOGCSS = this.getClass().getResource("/edu/jhuapl/trinity/css/dialogstyles.css").toExternalForm();
            dialogPane.getStylesheets().add(DIALOGCSS);

            Optional<ButtonType> optBT = alert.showAndWait();
            if (optBT.get().equals(ButtonType.CANCEL))
                return;
            else if (optBT.get().equals(ButtonType.YES)) {
                ellipsoidGroup.getChildren().clear();
                ellipsoidToGMMessageMap.clear();
                ellipsoidToGMDataMap.clear();
            }
        }
        gaussianMixtureCollection.getMixtures().parallelStream().forEach(gm -> {
            addGaussianMixture(gm);
        });
    }

    @Override
    public void locateFeatureVector(FeatureVector featureVector) {
        //pixel ranges we wish to fit our scaling to
        double halfSceneWidth = sceneWidth / 2.0;
        double halfSceneHeight = sceneHeight / 2.0;
        //offset used to center placement of points in scene after scaling
        double quarterSceneWidth = sceneWidth / 4.0;
        //modifiers used to manually adjust scaling by user
        double buff = pointScale * scatterBuffScaling;
        //formula bounds
        double minX = -buff;
        double rangeX = 2 * buff;
        double minY = -buff;
        double rangeY = 2 * buff;
        double minZ = -buff;
        double rangeZ = 2 * buff;
        //Determine origin location of 3D object in 3D scene coordinates
        //linear coordinate transformation of each mean to match our 3D scene
        //X ==> X Positive
        double xCoord = (((pointScale * featureVector.getData().get(xFactorIndex) - minX) * halfSceneWidth) / rangeX);
        //Y ==> Z Positive
        double yCoord = (((pointScale * featureVector.getData().get(yFactorIndex) - minY) * halfSceneHeight) / rangeY);
        //Z ==> Y Positive
        double zCoord = (((pointScale * featureVector.getData().get(zFactorIndex) - minZ) * halfSceneWidth) / rangeZ);
        //subtracting centering offset because numbers are in 3D scene coordinates now
        JavaFX3DUtils.transitionCameraTo(1000, camera, cameraTransform,
            xCoord - quarterSceneWidth, yCoord - quarterSceneWidth,
            zCoord - quarterSceneWidth - 750, 0, 0, 0); //subtract 750 from Z to pull the camera back
    }

    @Override
    public void clearFeatureVectors() {
        featureVectors.clear();
    }

    @Override
    public List<FeatureVector> getAllFeatureVectors() {
        return featureVectors;
    }

    @Override
    public void setVisibleByIndex(int i, boolean b) {
        Perspective3DNode[] d = pNodes.toArray(Perspective3DNode[]::new);
        VisibilityMap.pNodeVisibilityMap.put(d[i], b);

//        VisibilityMap.pNodeVisibilityMap.put(pNodes.toArray(Perspective3DNode[]::new)[i], b);
        VisibilityMap.visibilityList.set(i, b);
    }

    @Override
    public void refresh() {
        updatePNodeColorsAndVisibility();
        updateView(true);
        updateEllipsoids();
        cubeWorld.redraw(true);
        updateLabels();
        updateTrajectory3D(false);
    }

    @Override
    public void addManifold(Manifold manifold, Manifold3D manifold3D) {
        //System.out.println("Sean you need to implement addManifold for Hyperspace3D!!");
    }

    @Override
    public void makeManifold(boolean useVisiblePoints, String label, Double tolerance) {
        makeHull(useVisiblePoints, label, tolerance);
    }

    @Override
    public List<Manifold3D> getAllManifolds() {
        return manifolds;
    }

    @Override
    public void clearAllManifolds() {
        manifoldGroup.getChildren().clear();
        manifolds.clear();
    }

    @Override
    public Group getManifoldViews() {
        return manifoldGroup;
    }

    @Override
    public void setDimensionLabels(List<String> labelStrings) {
        featureLabels = labelStrings;
    }

    @Override
    public void findClusters(ManifoldEvent.ProjectionConfig pc) {
        System.out.println("Find Clusters for Hyperspace view.");
        //convert featurevector space into 2D array of doubles
        System.out.print("Convert features to data array... ");
        long startTime = System.nanoTime();
        double[][] observations = FeatureCollection.toData(featureVectors);
        Utils.printTotalTime(startTime);
        //find clusters
        switch (pc.clusterMethod) {
//            case KMEANS -> {
//                System.out.print("Kmeans fit... ");
//                startTime = System.nanoTime();
//                var kmeansClusters = KMeans.fit(observations, 50);
//                Utils.printTotalTime(startTime);
//                System.out.println("\n===============================================\n");
//                System.out.println("KMeans Clusters: " + kmeansClusters.k
//                    + " Distortion: " + kmeansClusters.distortion);
//            }
            case EX_MAX -> {
                System.out.print("Expectation Maximization... ");
                startTime = System.nanoTime();
                Utils.printTotalTime(startTime);
                System.out.println("\n===============================================\n");
            }
        }
    }

    @Override
    public void addClusters(List<PointCluster> clusters) {

    }

    @Override
    public Point3D projectVector(FeatureVector featureVector) {
        return null;
    }

}
