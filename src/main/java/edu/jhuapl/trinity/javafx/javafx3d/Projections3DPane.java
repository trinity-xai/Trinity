package edu.jhuapl.trinity.javafx.javafx3d;

import com.github.trinity.supermds.SuperMDS;
import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.CoordinateSet;
import edu.jhuapl.trinity.data.Dimension;
import edu.jhuapl.trinity.data.Distance;
import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.FeatureLayer;
import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.data.files.GaussianMixtureCollectionFile;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.data.messages.xai.GaussianMixture;
import edu.jhuapl.trinity.data.messages.xai.GaussianMixtureCollection;
import edu.jhuapl.trinity.data.messages.xai.GaussianMixtureData;
import edu.jhuapl.trinity.data.messages.xai.PointCluster;
import edu.jhuapl.trinity.data.messages.xai.UmapConfig;
import edu.jhuapl.trinity.javafx.components.callouts.Callout;
import edu.jhuapl.trinity.javafx.components.callouts.DistanceDataCallout;
import edu.jhuapl.trinity.javafx.components.panes.JoystickPane;
import edu.jhuapl.trinity.javafx.components.panes.ManifoldControlPane;
import edu.jhuapl.trinity.javafx.components.panes.RadarPlotPane;
import edu.jhuapl.trinity.javafx.components.panes.RadialEntityOverlayPane;
import edu.jhuapl.trinity.javafx.components.panes.RadialGridControlsPane;
import edu.jhuapl.trinity.javafx.components.panes.VideoPane;
import edu.jhuapl.trinity.javafx.components.projector.ProjectorNode;
import edu.jhuapl.trinity.javafx.components.projector.ProjectorTextNode;
import edu.jhuapl.trinity.javafx.components.radial.AnimatedNeonCircle;
import edu.jhuapl.trinity.javafx.components.radial.ViewControlsMenu;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.EffectEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent.ProjectionConfig;
import edu.jhuapl.trinity.javafx.events.ShadowEvent;
import edu.jhuapl.trinity.javafx.events.TimelineEvent;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
import edu.jhuapl.trinity.javafx.javafx3d.animated.AnimatedSphere;
import edu.jhuapl.trinity.javafx.javafx3d.animated.CameraOrbiter;
import edu.jhuapl.trinity.javafx.javafx3d.animated.Opticon;
import edu.jhuapl.trinity.javafx.javafx3d.animated.RadialGrid;
import edu.jhuapl.trinity.javafx.javafx3d.images.ImageResourceProvider;
import edu.jhuapl.trinity.javafx.javafx3d.projectiles.HitShape3D;
import edu.jhuapl.trinity.javafx.javafx3d.projectiles.ProjectileSystem;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.AffinityClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.DBSCANClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.ExMaxClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.HDDBSCANClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.KMeansClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.KMediodsClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.ManifoldClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.ProjectMdsFeaturesTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.ProjectPcaFeaturesTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.ProjectUmapFeaturesTask;
import edu.jhuapl.trinity.javafx.renderers.FeatureVectorRenderer;
import edu.jhuapl.trinity.javafx.renderers.GaussianMixtureRenderer;
import edu.jhuapl.trinity.javafx.renderers.ManifoldRenderer;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.PCAConfig;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.VisibilityMap;
import edu.jhuapl.trinity.utils.umap.Umap;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.scene.Skybox;
import org.fxyz3d.utils.CameraTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static edu.jhuapl.trinity.javafx.handlers.GaussianMixtureEventHandler.generateEllipsoidDiagonal;
import static edu.jhuapl.trinity.utils.ResourceUtils.removeExtension;

/**
 * @author Sean Phillips
 */

public class Projections3DPane extends StackPane implements
    FeatureVectorRenderer, GaussianMixtureRenderer, ManifoldRenderer {
    private static final Logger LOG = LoggerFactory.getLogger(Projections3DPane.class);
    public static double ICON_FIT_HEIGHT = 64;
    public static double ICON_FIT_WIDTH = 64;
    public static double DEFAULT_INTRO_DISTANCE = -60000.0;
    public static double DEFAULT_ZOOM_TIME_MS = 500.0;
    private double cameraDistance = -4000;
    private final double sceneWidth = 4000;
    private final double sceneHeight = 4000;
    private final double cubeSize = sceneWidth / 2.0;
    public PerspectiveCamera camera;
    public CameraTransformer cameraTransform;
    CameraOrbiter cameraOrbiter;
    PointLight cameraLight;
    PointLight centerLight;
    public boolean bindCameraRotations = false;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;
    RadialEntityOverlayPane radialOverlayPane;
    ManifoldControlPane manifoldControlPane;
    RadialGridControlsPane radialGridControlPane;
    RadarPlotPane radarPlotPane;
    ViewControlsMenu viewControlsMenu;
    JoystickPane joystickPane;
    RadialGrid radialGrid;

    public Group sceneRoot = new Group();
    public Group extrasGroup = new Group();
    public Group connectorsGroup = new Group();
    public Group debugGroup = new Group();
    public Group ellipsoidGroup = new Group();
    public XFormGroup dataXForm = new XFormGroup();
    public boolean enableXForm = false;
    public boolean enableContextMenu = true;

    public Sphere selectedSphereA = null;
    public Sphere selectedSphereB = null;
    public Manifold3D selectedManifoldA = null;
    public Manifold3D selectedManifoldB = null;

    public SubScene subScene;

    public double dataAnimationMS = 500; //milliseconds
    public double animationSleepMS = 100; //milliseconds
    public double point3dSize = 10.0; //size of 3d tetrahedra
    public double pointScale = 1.0; //scales parameter value in transform
    public double scatterBuffScaling = 1.0; //scales domain range in transform
    public int queueLimit = 50000;

    //feature vector indices for 3D coordinates
    private int xFactorIndex = 0;
    private int yFactorIndex = 1;
    private int zFactorIndex = 2;
    private int factorMaxIndex = 512;
    private int anchorIndex = 0;

    public Color sceneColor = Color.BLACK;
    boolean reflectY = true;
    Sphere highlightedPoint = new Sphere(1, 8);

    Callout anchorCallout;
    TriaxialSpheroidMesh anchorTSM;
    Trajectory anchorTrajectory;
    Trajectory3D anchorTraj3D;
    Trajectory3D projectorConnector;
    Group trajectorySphereGroup;
    Group trajectoryGroup;
    HashMap<Trajectory, Trajectory3D> trajToTraj3DMap = new HashMap<>();
    double trajectoryScale = 1.0;
    int trajectoryTailSize = 5;
    double projectionScalar = 100.0;

    HashMap<FeatureVector, ProjectorNode> featureVectorToProjectorNode = new HashMap<>();

    //This maps each ellipsoid to a GMM
    HashMap<Sphere, FeatureVector> sphereToFeatureVectorMap = new HashMap<>();
    //This maps each ellipsoid to a GMM
    HashMap<TriaxialSpheroidMesh, GaussianMixture> ellipsoidToGMMessageMap = new HashMap<>();
    //This maps each ellipsoid to its specific GaussianMixtureData
    HashMap<TriaxialSpheroidMesh, GaussianMixtureData> ellipsoidToGMDataMap = new HashMap<>();
    //allows 2D labels to track their 3D counterparts
    HashMap<Shape3D, Label> shape3DToLabel = new HashMap<>();
    //This maps 3D connectors to their respective Distance object
    HashMap<Distance, Trajectory3D> distanceTotrajectory3DMap = new HashMap<>();
    //This maps distance objects to label overlays
    HashMap<Distance, Shape3D> distanceToShape3DMap = new HashMap<>();

    public List<FeatureVector> featureVectors = new ArrayList<>();
    public List<FeatureVector> hyperFeatures = new ArrayList<>();
    public List<FeatureVector> autoProjectedFeatures = new ArrayList<>();
    //@TODO SMP
    public int autoProjectionQueueSize = 1000;

    public boolean meanCentered = true;
    public boolean autoScaling = true;
    public boolean colorByLabel = true;
    public boolean colorByCoordinate = false;
    public List<Double> meanVector = new ArrayList<>();
    public double maxAbsValue = 1.0;
    public double meanCenteredMaxAbsValue = 1.0;
    public boolean pointToPointDistanceMode = false;
    public boolean clusterSelectionMode = false;
    public boolean updatingTrajectories = false;
    public boolean animatingProjections = false;
    public boolean emptyVisionEnabled = false;
    public SimpleBooleanProperty autoProjectionProperty = new SimpleBooleanProperty(false);
    public SimpleBooleanProperty projectionWallProperty = new SimpleBooleanProperty(false);
    Opticon projectionOpticon;

    // initial rotation
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotateZ = new Rotate(0, Rotate.Z_AXIS);

    private Skybox skybox;
    ProjectorNodeGroup projectorNodeGroup;

    private Group nodeGroup = new Group();
    private Group labelGroup = new Group();
    private Group manifoldGroup = new Group();
    private ArrayList<Manifold3D> manifolds = new ArrayList<>();
    public AnimatedNeonCircle highlighterNeonCircle;
    public Crosshair3D miniCrosshair;
    public Crosshair3D crosshair3D;

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
    public Umap latestUmap = null;

    Rectangle selectionRectangle;
    ProjectileSystem projectileSystem;
    File mostRecentPath = Paths.get(".").toFile();

    public Projections3DPane(Scene scene) {
        this.scene = scene;

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
        Font font = new Font("calibri", 20);
        xLabel.setFont(font);
        xLabel.setTextFill(Color.YELLOW);
        yLabel.setFont(new Font("calibri", 20));
        yLabel.setTextFill(Color.SKYBLUE);
        zLabel.setFont(new Font("calibri", 20));
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
        cameraTransform = new CameraTransformer();
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
        cameraTransform.ry.setAngle(-45.0);
        cameraTransform.rx.setAngle(-10.0);
        cameraOrbiter = new CameraOrbiter(cameraTransform, 7);
        cameraTransform.ry.angleProperty().addListener(e -> updateFloatingNodes());
//        setupSkyBox();
        debugGroup.setVisible(false);

        //Anchor and event trajectory setup
        anchorTSM = createEllipsoid(10, 10, 10, Color.STEELBLUE);
        anchorTrajectory = new Trajectory("Anchor History");
        trajectoryScale = 1.0;
        anchorTraj3D = JavaFX3DUtils.buildPolyLineFromTrajectory(1, 1,
            anchorTrajectory, Color.CYAN, trajectoryScale, sceneWidth, sceneHeight);
        extrasGroup.getChildren().add(anchorTraj3D);
        anchorTSM.visibleProperty().bind(anchorTraj3D.visibleProperty());

        projectorConnector = JavaFX3DUtils.buildPolyLineFromTrajectory(1, 1,
            anchorTrajectory, Color.WHITE, trajectoryScale, sceneWidth, sceneHeight);
        extrasGroup.getChildren().add(projectorConnector);

        trajectoryGroup = new Group();
        trajectorySphereGroup = new Group();
        double ellipsoidWidth = anchorTraj3D.width / 2.0;
        for (Point3D point : anchorTraj3D.points) {
            TriaxialSpheroidMesh tsm = createEllipsoid(ellipsoidWidth,
                ellipsoidWidth, ellipsoidWidth, Color.TOMATO);
            tsm.setTranslateX(point.x);
            tsm.setTranslateY(point.y);
            tsm.setTranslateZ(point.z);
            trajectorySphereGroup.getChildren().add(tsm);
        }
        extrasGroup.getChildren().add(0, trajectorySphereGroup);
        extrasGroup.getChildren().add(0, trajectoryGroup);

        scene.addEventHandler(ApplicationEvent.CAMERA_ORBIT_MODE, e -> {
            if (null != e.object) {
                boolean rotating = (boolean) e.object;
                if (rotating) {
                    cameraOrbiter.start();
                    cameraOrbiter.setEnableRotation(rotating);
                } else {
                    cameraOrbiter.stop();
                    cameraOrbiter.setEnableRotation(rotating);
                }
            }
        });

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
            Platform.runLater(() -> {
                updateTrajectory3D(false);
            });
        });

        this.scene.addEventHandler(TrajectoryEvent.CLEAR_ALL_TRAJECTORIES, e -> {
            Platform.runLater(() -> {
                updateTrajectory3D(true);
            });
        });

        this.scene.addEventHandler(TrajectoryEvent.TRAJECTORY_VISIBILITY_CHANGED, e -> {
            Trajectory trajectory = (Trajectory) e.eventObject;
            Trajectory3D traj3D = trajToTraj3DMap.get(trajectory);
            if (null != traj3D) {
                traj3D.setVisible(trajectory.getVisible());
            }
        });
        this.scene.addEventHandler(TrajectoryEvent.TRAJECTORY_COLOR_CHANGED, e -> {
            Platform.runLater(() -> {
                updateTrajectory3D(false);
            });
        });
        this.scene.addEventHandler(EffectEvent.ENABLE_EMPTY_VISION, e -> {
            emptyVisionEnabled = (boolean) e.object;
        });
        this.scene.addEventHandler(EffectEvent.OPTICON_ENABLE_ORBITING, e -> {
            projectionOpticon.enableOrbiting((boolean) e.object);
        });

        projectorNodeGroup = new ProjectorNodeGroup(subScene, camera, cameraTransform, labelGroup);
        projectorNodeGroup.visibleProperty().bind(projectionWallProperty);
        projectorNodeGroup.yOffset = 636.0; //bigger than 512...
        sceneRoot.getChildren().addAll(projectorNodeGroup);

        radialGrid = new RadialGrid();
        crosshair3D = new Crosshair3D(javafx.geometry.Point3D.ZERO, sceneWidth / 2.0, 10.0f);

        //Add 3D subscene stuff to 3D scene root object
        sceneRoot.getChildren().addAll(cameraTransform, radialGrid, highlightedPoint,
            nodeGroup, manifoldGroup, debugGroup, crosshair3D,
            dataXForm, extrasGroup, connectorsGroup, anchorTSM);
        centerLight = new PointLight(Color.WHITE);
        sceneRoot.getChildren().add(centerLight);
        sceneRoot.getChildren().add(ellipsoidGroup);

        projectionOpticon = new Opticon(Color.CYAN, 100);
        extrasGroup.getChildren().add(projectionOpticon);
        projectionOpticon.visibleProperty().bind(projectionOpticon.orbitingProperty);

        miniCrosshair = new Crosshair3D(javafx.geometry.Point3D.ZERO,
            2, 1.0f);
        nodeGroup.getChildren().add(miniCrosshair);

        highlightedPoint.setMaterial(new PhongMaterial(Color.ALICEBLUE));
        highlightedPoint.setDrawMode(DrawMode.LINE);
        subScene.setCamera(camera);
        //add a Point Light for better viewing of the grid coordinate system
        cameraLight = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(cameraLight);
        cameraLight.setTranslateX(camera.getTranslateX());
        cameraLight.setTranslateY(camera.getTranslateY());
        cameraLight.setTranslateZ(camera.getTranslateZ());

        //Some camera controls...
        subScene.setOnMouseEntered(event -> subScene.requestFocus());
        setOnMouseEntered(event -> subScene.requestFocus());
        subScene.setOnZoom(event -> {
            double modifier = 50.0;
            double modifierFactor = 0.1;
            double z = camera.getTranslateZ();
            double newZ = z + event.getZoomFactor() * modifierFactor * modifier;
            camera.setTranslateZ(newZ);
            updateFloatingNodes();
        });

        subScene.setOnKeyPressed(event -> {
            //What key did the user press?
            KeyCode keycode = event.getCode();
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
            if (keycode == KeyCode.SPACE) {
                camera.setTranslateY(camera.getTranslateY() + change);
            }
            if (keycode == KeyCode.X) {
                camera.setTranslateY(camera.getTranslateY() - change);
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
                        redraw = true;
                    } catch (Exception ex) {
                        scene.getRoot().fireEvent(
                            new CommandTerminalEvent("Feature Indexing Error: ("
                                + xFactorIndex + ", " + yFactorIndex + ", " + zFactorIndex + ")",
                                new Font("Consolas", 20), Color.RED));
                    }
                    if (redraw) {
                        updateEllipsoids();
                        notifyIndexChange();
                    }
                    updateFloatingNodes();
                    setSpheroidAnchor(true, anchorIndex);
                }
            }
            if (keycode == KeyCode.PERIOD) {
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
                        redraw = true;
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        scene.getRoot().fireEvent(
                            new CommandTerminalEvent("Feature Indexing Error: ("
                                + xFactorIndex + ", " + yFactorIndex + ", " + zFactorIndex + ")",
                                new Font("Consolas", 20), Color.RED));
                    }
                    if (redraw) {
                        updateEllipsoids();
                        notifyIndexChange();
                    }
                    updateFloatingNodes();
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
                notifyScaleChange();
                updateEllipsoids();
            }
            if (keycode == KeyCode.Y) {
                scatterBuffScaling += 0.1;
                Platform.runLater(() -> scene.getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.SCATTERBUFF_SCALING_KEYPRESS, scatterBuffScaling)));
                notifyScaleChange();
                updateEllipsoids();
            }

            //point size and scaling
            if (keycode == KeyCode.O || (keycode == KeyCode.P && event.isControlDown())) {
                point3dSize -= 1;
                Platform.runLater(() -> scene.getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.POINT3D_SIZE_KEYPRESS, point3dSize)));
                updateDataRadius();
            }
            if (keycode == KeyCode.P) {
                point3dSize += 1;
                Platform.runLater(() -> scene.getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.POINT3D_SIZE_KEYPRESS, point3dSize)));
                updateDataRadius();
            }
            if (keycode == KeyCode.U) {
                pointScale -= 0.1;
                Platform.runLater(() -> scene.getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.POINT_SCALE_KEYPRESS, pointScale)));
                notifyScaleChange();
            }
            if (keycode == KeyCode.I) {
                pointScale += 0.1;
                Platform.runLater(() -> scene.getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.POINT_SCALE_KEYPRESS, pointScale)));
                notifyScaleChange();
            }

            if (event.isAltDown() && keycode == KeyCode.H) {
                double w = highlighterNeonCircle.getStrokeWidth() - 0.1;
                if (w <= 0)
                    highlighterNeonCircle.setStrokeWidth(0.1);
                else
                    highlighterNeonCircle.setStrokeWidth(w);
            }
            if (event.isAltDown() && keycode == KeyCode.J) {
                double w = highlighterNeonCircle.getStrokeWidth() + 0.1;
                if (w > 50)
                    highlighterNeonCircle.setStrokeWidth(20);
                else
                    highlighterNeonCircle.setStrokeWidth(w);
            }
            if (keycode == KeyCode.F) {
                anchorIndex--;
                if (anchorIndex < 0) anchorIndex = 0;
                setSpheroidAnchor(true, anchorIndex);
            }
            if (keycode == KeyCode.G) {
                anchorIndex++;
                if (anchorIndex >= featureVectors.size()) anchorIndex = featureVectors.size();
                setSpheroidAnchor(true, anchorIndex);
            }

            updateFloatingNodes();
            radialOverlayPane.updateCalloutHeadPoints(subScene);
        });

        //Setup selection rectangle and event handling
        selectionRectangle = new Rectangle(1, 1,
            Color.ALICEBLUE.deriveColor(1, 1, 1, 0.2));
        selectionRectangle.setManaged(false);
        selectionRectangle.setMouseTransparent(true);

        selectionRectangle.setVisible(false);
        subScene.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
            if (clusterSelectionMode) {
                selectionRectangle.setWidth(1);
                selectionRectangle.setHeight(1);
                selectionRectangle.setX(mousePosX);
                selectionRectangle.setY(mousePosY);
                selectionRectangle.setVisible(true);
            }
        });
        subScene.setOnZoom(e -> {
            double zoom = e.getZoomFactor();
            if (zoom > 1) {
                camera.setTranslateZ(camera.getTranslateZ() + 50.0);
            } else {
                camera.setTranslateZ(camera.getTranslateZ() - 50.0);
            }
            updateFloatingNodes();
            radialOverlayPane.updateCalloutHeadPoints(subScene);
            cameraLight.setTranslateX(camera.getTranslateX());
            cameraLight.setTranslateY(camera.getTranslateY());
            cameraLight.setTranslateZ(camera.getTranslateZ());

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
            cameraLight.setTranslateX(camera.getTranslateX());
            cameraLight.setTranslateY(camera.getTranslateY());
            cameraLight.setTranslateZ(camera.getTranslateZ());
            updateFloatingNodes();
            radialOverlayPane.updateCalloutHeadPoints(subScene);
        });
        //Start Tracking mouse movements only when a button is pressed
        subScene.setOnMouseDragged((MouseEvent me) -> {
            if (clusterSelectionMode) {
                mouseOldX = mousePosX;
                mouseOldY = mousePosY;
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                mouseDeltaX = (mousePosX - mouseOldX);
                mouseDeltaY = (mousePosY - mouseOldY);
                selectionRectangle.setWidth(
                    mousePosX - selectionRectangle.getX());
                selectionRectangle.setHeight(
                    mousePosY - selectionRectangle.getY());
            } else
                mouseDragCamera(me);
        });
        subScene.setOnMouseReleased((MouseEvent me) -> {
            if (clusterSelectionMode) {
                ManifoldClusterTask manifoldClusterTask = new ManifoldClusterTask(scene,
                    camera, sphereToFeatureVectorMap, selectionRectangle);
                if (!manifoldClusterTask.isCancelledByUser())
                    manifoldClusterTask.run();
                selectionRectangle.setVisible(false);
                selectionRectangle.setWidth(1);
                selectionRectangle.setHeight(1);
                clusterSelectionMode = false;
            }
        });
        //@HACKY QUICK FIX SMP Handle GMM drops separately here
        subScene.addEventHandler(DragEvent.DRAG_OVER, e -> {
            if (ResourceUtils.canDragOver(e)) {
                e.acceptTransferModes(TransferMode.COPY);
            } else {
                e.consume();
            }
        });
        subScene.addEventHandler(DragEvent.DRAG_DROPPED, e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                final List<File> files = db.getFiles();
                for (File file : files) {
                    try {
                        if (GaussianMixtureCollectionFile.isGaussianMixtureCollectionFile(file)) {
                            GaussianMixtureCollectionFile gmcFile = new GaussianMixtureCollectionFile(file.getAbsolutePath(), true);
                            //generate the diagonal for ellipsoid rendering
                            generateEllipsoidDiagonal(gmcFile.gaussianMixtureCollection);
                            addGaussianMixtureCollection(gmcFile.gaussianMixtureCollection);
                            e.consume();
                        }
                    } catch (IOException ex) {
                        LOG.error(null, ex);
                    }
                }
            }
        });
        bp = new BorderPane(subScene);
        //RadialOverlayPane will hold all those nifty callouts and radial entities
        radialOverlayPane = new RadialEntityOverlayPane(this.scene, featureVectors);
        radialOverlayPane.prefWidthProperty().bind(widthProperty());
        radialOverlayPane.prefHeightProperty().bind(heightProperty());
        radialOverlayPane.minWidthProperty().bind(widthProperty());
        radialOverlayPane.minHeightProperty().bind(heightProperty());
        radialOverlayPane.maxWidthProperty().bind(widthProperty());
        radialOverlayPane.maxHeightProperty().bind(heightProperty());

        //Make a 2D circle that we will use to indicate
        highlighterNeonCircle = new AnimatedNeonCircle(
            new AnimatedNeonCircle.Animation(
                Duration.millis(3000), Transition.INDEFINITE, false),
            20, 2.5, 8.0, 5.0);
        highlighterNeonCircle.setManaged(false);
        highlighterNeonCircle.setMouseTransparent(true);
        getChildren().clear();
        getChildren().addAll(bp, labelGroup, radialOverlayPane,
            highlighterNeonCircle, selectionRectangle);

        Glow glow = new Glow(0.5);

        ImageView reset = ResourceUtils.loadIcon("camera", ICON_FIT_HEIGHT);
        reset.setEffect(glow);
        MenuItem resetViewItem = new MenuItem("Reset View", reset);
        resetViewItem.setOnAction(e -> resetView(1000, false));

        ImageView callouts = ResourceUtils.loadIcon("callouts", ICON_FIT_HEIGHT);
        callouts.setEffect(glow);
        MenuItem clearCalloutsItem = new MenuItem("Clear Callouts", callouts);
        clearCalloutsItem.setOnAction(e -> radialOverlayPane.clearCallouts());

        ImageView manifold = ResourceUtils.loadIcon("manifold", ICON_FIT_HEIGHT);
        manifold.setEffect(glow);
        MenuItem manifoldsItem = new MenuItem("Manifold Controls", manifold);

        manifoldControlPane = new ManifoldControlPane(scene, App.getAppPathPaneStack());
        manifoldControlPane.visibleProperty().bind(this.visibleProperty());
        manifoldsItem.setOnAction(e -> {
            Pane pathPane = App.getAppPathPaneStack();
            if (null == manifoldControlPane) {
                manifoldControlPane = new ManifoldControlPane(scene, pathPane);
                manifoldControlPane.visibleProperty().bind(this.visibleProperty());
            }
            if (!pathPane.getChildren().contains(manifoldControlPane)) {
                pathPane.getChildren().add(manifoldControlPane);
                manifoldControlPane.slideInPane();
            } else {
                manifoldControlPane.show();
            }
        });

        ImageView radial = ResourceUtils.loadIcon("radial", ICON_FIT_HEIGHT);
        radial.setEffect(glow);
        MenuItem radialItem = new MenuItem("Radial Grid Controls", radial);

        radialGridControlPane = new RadialGridControlsPane(scene, App.getAppPathPaneStack(), radialGrid);
        radialGridControlPane.visibleProperty().bind(this.visibleProperty());
        radialItem.setOnAction(e -> {
            Pane pathPane = App.getAppPathPaneStack();
            if (null == radialGridControlPane) {
                radialGridControlPane = new RadialGridControlsPane(scene, pathPane, radialGrid);
                radialGridControlPane.visibleProperty().bind(this.visibleProperty());
            }
            if (!pathPane.getChildren().contains(radialGridControlPane)) {
                pathPane.getChildren().add(radialGridControlPane);
                radialGridControlPane.slideInPane();
            } else {
                radialGridControlPane.show();
            }
        });
        ImageView radar = ResourceUtils.loadIcon("radar", ICON_FIT_HEIGHT);
        radar.setEffect(glow);
        MenuItem radarItem = new MenuItem("Parameter RADAR", radar);
        radarItem.setOnAction(e -> {
            Pane pathPane = App.getAppPathPaneStack();
            if (null == radarPlotPane) {
                radarPlotPane = new RadarPlotPane(scene, pathPane);
            }
            if (!pathPane.getChildren().contains(radarPlotPane)) {
                pathPane.getChildren().add(radarPlotPane);
                radarPlotPane.slideInPane();
            } else {
                radarPlotPane.show();
            }
        });
        ImageView export = ResourceUtils.loadIcon("export", ICON_FIT_HEIGHT);
        export.setEffect(glow);
        MenuItem exportItem = new MenuItem("Export", export);
        exportItem.setOnAction(e -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save scene as...");
            fileChooser.setInitialFileName("trinity_projection.png");
            fileChooser.setInitialDirectory(mostRecentPath);
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                mostRecentPath = file.getParentFile();
                WritableImage image = this.snapshot(new SnapshotParameters(), null);
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                } catch (IOException ioe) {
                    // TODO: handle exception here
                    //drop it like its hot
                }
            }
        });

        ImageView navigator = ResourceUtils.loadIcon("navigator", ICON_FIT_HEIGHT);
        navigator.setEffect(glow);
        MenuItem navigatorItem = new MenuItem("Content Navigator", navigator);
        navigatorItem.setOnAction(e -> {
            subScene.getParent().getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_NAVIGATOR_PANE));
        });

        ImageView analysis = ResourceUtils.loadIcon("analysis", ICON_FIT_HEIGHT);
        analysis.setEffect(glow);
        MenuItem analysisItem = new MenuItem("Analysis Log", analysis);
        analysisItem.setOnAction(e -> {
            subScene.getParent().getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_ANALYSISLOG_PANE));
        });

        ImageView clearProjection = ResourceUtils.loadIcon("clear", ICON_FIT_HEIGHT);
        radar.setEffect(glow);
        MenuItem clearProjectionItem = new MenuItem("Clear Projection Data", clearProjection);
        clearProjectionItem.setOnAction(e -> {
            featureVectors.clear();
            sphereToFeatureVectorMap.clear();
            ellipsoidGroup.getChildren().clear();
            projectorNodeGroup.clearAll();
            clearAll();
            refresh(true);
        });

        ImageView selectPoints = ResourceUtils.loadIcon("boundingbox", ICON_FIT_HEIGHT);
        selectPoints.setEffect(glow);
        MenuItem selectPointsItem = new MenuItem("Select Points", selectPoints);
        selectPointsItem.setOnAction(e -> {
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.CLUSTER_SELECTION_MODE, true));
        });

        CheckMenuItem enableProjectionWallItem = new CheckMenuItem("Enable Projection Wall");
        enableProjectionWallItem.setSelected(false);
        enableProjectionWallItem.selectedProperty().addListener(cl -> {
            projectionWallProperty.set(enableProjectionWallItem.isSelected());
        });

        CheckMenuItem animatingProjectionsItem = new CheckMenuItem("Animating Projections");
        animatingProjectionsItem.setSelected(animatingProjections);
        animatingProjectionsItem.selectedProperty().addListener(cl ->
            animatingProjections = animatingProjectionsItem.isSelected());
        CheckMenuItem updatingTrajectoriesItem = new CheckMenuItem("Auto Update Trajectories");
        updatingTrajectoriesItem.setSelected(updatingTrajectories);
        updatingTrajectoriesItem.selectedProperty().addListener(cl ->
            updatingTrajectories = updatingTrajectoriesItem.isSelected());

        ToggleButton cameraAnimateToggle = new ToggleButton("Orbiting Camera");
        cameraAnimateToggle.setOnAction(e -> {
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.CAMERA_ORBIT_MODE,
                    cameraAnimateToggle.isSelected()));
        });
        MenuItem cameraAnimateItem = new CustomMenuItem(cameraAnimateToggle, false);

        Spinner<Double> projectionScalarSpinner = new Spinner<>();
        projectionScalarSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(1, 1000, projectionScalar, 1));
        projectionScalarSpinner.setEditable(true);
        projectionScalarSpinner.valueProperty().addListener(e -> {
            projectionScalar = projectionScalarSpinner.getValue();
            sphereToFeatureVectorMap.forEach((sphere, featureVector) -> {
                sphere.setTranslateX(featureVector.getData().get(0) * projectionScalar);
                sphere.setTranslateY(featureVector.getData().get(1) * -projectionScalar);
                sphere.setTranslateZ(featureVector.getData().size() > 2
                    ? featureVector.getData().get(2) * projectionScalar
                    : 0.0);
            });
        });
        MenuItem projectionScalarItem = new CustomMenuItem(
            new VBox(2, new Label("Projection Scalar)"), projectionScalarSpinner), false);

        Spinner<Double> dataAnimationSpinner = new Spinner<>();
        dataAnimationSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(50, 1000, dataAnimationMS, 50));
        dataAnimationSpinner.setEditable(true);
        dataAnimationSpinner.valueProperty().addListener(e -> {
            dataAnimationMS = dataAnimationSpinner.getValue();
        });
        MenuItem dataAnimationItem = new CustomMenuItem(
            new VBox(2, new Label("Data Animation (ms)"), dataAnimationSpinner), false);

        Spinner<Double> sleepAnimationSpinner = new Spinner<>();
        sleepAnimationSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(50, 1000, animationSleepMS, 50));
        sleepAnimationSpinner.setEditable(true);
        sleepAnimationSpinner.valueProperty().addListener(e -> {
            animationSleepMS = sleepAnimationSpinner.getValue();
        });
        MenuItem sleepAnimationItem = new CustomMenuItem(
            new VBox(2, new Label("Sleep Animation (ms)"), sleepAnimationSpinner), false);

        ColorPicker centerLightPicker = new ColorPicker(Color.WHITE);
        centerLight.colorProperty().bind(centerLightPicker.valueProperty());
        MenuItem centerLightItem = new CustomMenuItem(
            new VBox(2, new Label("Center Light"), centerLightPicker), false);

        ColorPicker cameraLightPicker = new ColorPicker(Color.WHITE);
        cameraLight.colorProperty().bind(cameraLightPicker.valueProperty());
        MenuItem cameraLightItem = new CustomMenuItem(
            new VBox(2, new Label("Camera Light"), cameraLightPicker), false);

        ImageView optionsImageView = ResourceUtils.loadIcon("configuration", ICON_FIT_HEIGHT);
        optionsImageView.setEffect(glow);
        Menu optionsMenu = new Menu("Options", optionsImageView,
            cameraAnimateItem, projectionScalarItem,
            dataAnimationItem, sleepAnimationItem,
            centerLightItem, cameraLightItem,
            enableProjectionWallItem, animatingProjectionsItem, updatingTrajectoriesItem);

        ContextMenu cm = new ContextMenu(selectPointsItem,
            resetViewItem, manifoldsItem, radialItem, radarItem,
            exportItem, analysisItem, navigatorItem,
            clearCalloutsItem, clearProjectionItem, optionsMenu);

        cm.setAutoFix(true);
        cm.setAutoHide(true);
        cm.setHideOnEscape(true);
        cm.setOpacity(0.85);

        subScene.setOnMouseClicked((MouseEvent e) -> {
            if (e.getButton() == MouseButton.SECONDARY && enableContextMenu && !enableXForm) {
                if (!cm.isShowing())
                    cm.show(this.getParent(), e.getScreenX(), e.getScreenY());
                else
                    cm.hide();
                e.consume();
            }
        });

        this.scene.addEventHandler(HyperspaceEvent.REMOVED_FEATURE_LAYER, e -> {
            Integer index = ((FeatureLayer) e.object).getIndex();
            if (null != FeatureLayer.removeFeatureLayer(index)) {
                refresh(true);
            }
        });
        this.scene.addEventHandler(HyperspaceEvent.CLEARED_FACTOR_LABELS, e -> {
            refresh(true);
        });
        this.scene.addEventHandler(HyperspaceEvent.CLEARED_FEATURE_LAYERS, e -> {
            refresh(true);
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
                refresh(true);
            }
        });

        this.scene.addEventHandler(HyperspaceEvent.HYPERSPACE_BACKGROUND_COLOR, e -> {
            Color color = (Color) e.object;
            subScene.setFill(color);
        });
        this.scene.addEventHandler(HyperspaceEvent.ENABLE_HYPERSPACE_SKYBOX, e -> {
            skybox.setVisible((Boolean) e.object);
        });

        this.scene.addEventHandler(HyperspaceEvent.FACTOR_COORDINATES_GUI, e -> {
            CoordinateSet coords = (CoordinateSet) e.object;
            xFactorIndex = coords.coordinateIndices.get(0);
            yFactorIndex = coords.coordinateIndices.get(1);
            zFactorIndex = coords.coordinateIndices.get(2);
            updateFloatingNodes();
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
                    updateEllipsoids();
                    notifyIndexChange();
                }
            } else
                factorMaxIndex = newFactorMaxIndex;
        });
        scene.addEventHandler(HyperspaceEvent.COLOR_BY_LABEL, e -> {
            colorByLabel = (boolean) e.object;
            updateDataColors();
        });
        scene.addEventHandler(HyperspaceEvent.COLOR_BY_LAYER, e -> {
            colorByLabel = !(boolean) e.object;
            updateDataColors();
        });

        scene.addEventHandler(HyperspaceEvent.NODE_QUEUELIMIT_GUI, e -> queueLimit = (int) e.object);

        scene.addEventHandler(HyperspaceEvent.POINT3D_SIZE_GUI, e -> {
            point3dSize = (double) e.object;
            updateDataRadius();
        });
        scene.addEventHandler(HyperspaceEvent.POINT_SCALE_GUI, e -> {
            pointScale = (double) e.object;
            notifyScaleChange();
            updateEllipsoids();
        });
        scene.addEventHandler(HyperspaceEvent.SCATTERBUFF_SCALING_GUI, e -> {
            scatterBuffScaling = (double) e.object;
            notifyScaleChange();
            updateEllipsoids();
        });
        scene.addEventHandler(HyperspaceEvent.RESET_MAX_ABS, e -> {
            maxAbsValue = 1.0;
            meanCenteredMaxAbsValue = 1.0;
            notifyScaleChange();
            updateEllipsoids();
        });
        scene.addEventHandler(HyperspaceEvent.RECOMPUTE_MAX_ABS, e -> {
            if (!featureVectors.isEmpty())
                updateMaxAndMeans();
            notifyScaleChange();
            updateEllipsoids();
        });

        scene.addEventHandler(ShadowEvent.SHOW_AXES_LABELS, e -> {
            nodeGroup.setVisible((boolean) e.object);
            labelGroup.setVisible((boolean) e.object);
        });
        scene.addEventHandler(ShadowEvent.OVERRIDE_XFORM, e -> {
            enableXForm = (boolean) e.object;
        });

        scene.addEventHandler(ManifoldEvent.CLUSTER_SELECTION_MODE, e -> {
            boolean isActive = (boolean) e.object1;
            clusterSelectionMode = isActive;
            LOG.info("Cluster Selection Mode: {}", clusterSelectionMode);
        });
        scene.addEventHandler(ManifoldEvent.TAKESNAPSHOT_PROJECTION_SCENE, e -> {
            WritableImage image = this.snapshot(new SnapshotParameters(), null);
            Platform.runLater(() -> {
                getScene().getRoot().fireEvent(
                    new ManifoldEvent(ManifoldEvent.NEWSNAPSHOT_PROJECTION_SCENE, image)
                );
            });
        });
        scene.addEventHandler(ManifoldEvent.EXPORT_PROJECTION_SCENE, e -> {
            File file = (File) e.object1;
            if (file != null) {
                mostRecentPath = file.getParentFile();
                WritableImage image = this.snapshot(new SnapshotParameters(), null);
                try {
                    String newImageFile = mostRecentPath.getPath() + File.separator + removeExtension(file.getName()) + ".png";
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png",
                        new File(newImageFile));
                } catch (IOException ioe) {
                    // TODO: handle exception here
                    //drop it like its hot
                }
            }
        });
        scene.addEventHandler(ManifoldEvent.SAVE_PROJECTION_DATA, e -> {
            File file = (File) e.object1;
            FeatureCollection fc = new FeatureCollection();
            fc.setFeatures(featureVectors);
            try {
                FeatureCollectionFile fcf = new FeatureCollectionFile(file.getAbsolutePath(), false);
                fc.setDimensionLabels(Dimension.getDimensionsAsStrings());
                fcf.featureCollection = fc;
                fcf.writeContent();
            } catch (IOException ex) {
                LOG.error(null, ex);
            }
        });
        scene.addEventHandler(ManifoldEvent.CLEAR_DISTANCE_CONNECTORS, e -> {
            connectorsGroup.getChildren().removeIf(n -> n instanceof Trajectory3D);
            //remove label and sphere overlay components
            distanceTotrajectory3DMap.forEach((distance, traj3D) -> {
                //Clear this connector's label overlay
                Shape3D shape3D = distanceToShape3DMap.get(distance);
                extrasGroup.getChildren().remove(shape3D);
                Label label = shape3DToLabel.remove(shape3D);
                labelGroup.getChildren().remove(label);
                distanceToShape3DMap.remove(distance);
            });
            distanceTotrajectory3DMap.clear();
        });
        scene.addEventHandler(ManifoldEvent.DISTANCE_CONNECTOR_WIDTH, e -> {
            Distance eventDistance = (Distance) e.object1;
            Trajectory3D traj3D = distanceTotrajectory3DMap.get(eventDistance);
            if (null != traj3D) {
                updateDistanceTrajectory(traj3D, eventDistance);
            }
        });
        scene.addEventHandler(ManifoldEvent.DISTANCE_CONNECTOR_COLOR, e -> {
            Distance eventDistance = (Distance) e.object1;
            Trajectory3D traj3D = distanceTotrajectory3DMap.get(eventDistance);
            if (null != traj3D) {
                updateDistanceTrajectory(traj3D, eventDistance);
            }
        });
        scene.addEventHandler(ManifoldEvent.SET_PROJECTIONQUEUE_SIZE, e -> {
            int newQueueSize = (int) e.object1;
            autoProjectionQueueSize = newQueueSize;
            //age out older items...
            int ageOffCount = -autoProjectionQueueSize - autoProjectedFeatures.size();
            if (ageOffCount > 0) {
                LOG.info("Aging off {} projected features.", ageOffCount);
            }
        });


        scene.addEventHandler(TimelineEvent.TIMELINE_SAMPLE_INDEX, e -> {
            anchorIndex = (int) e.object;
            if (anchorIndex < 0)
                anchorIndex = 0;
            else if (anchorIndex > featureVectors.size())
                anchorIndex = featureVectors.size();
            setSpheroidAnchor(true, anchorIndex);
        });

        projectileSystem = new ProjectileSystem(debugGroup, 15);
        projectileSystem.setRunning(false);
        projectileSystem.setEnableProjectileTimer(true);
        viewControlsMenu = new ViewControlsMenu(scene);
        radialOverlayPane.addEntity(viewControlsMenu);
        viewControlsMenu.setTranslateX(200);
        viewControlsMenu.setTranslateY(200);
        viewControlsMenu.setVisible(false);

        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, me -> {
            if (me.isSecondaryButtonDown())
                projectileSystem.playerShip.mouseDragged(me, mouseDeltaX, mouseDeltaY);
        });
        subScene.addEventHandler(KeyEvent.KEY_PRESSED, k -> {
            //What key did the user press?
            KeyCode keycode = k.getCode();
            if (keycode == KeyCode.F11) {
                projectileSystem.setInMotion(!projectileSystem.isInMotion());
            }
            if (keycode == KeyCode.F12 && k.isControlDown()) {
                resetAsteroids();
            } else if (keycode == KeyCode.F12 && k.isAltDown()) {
                //do we need an intro?
                if (k.isShiftDown())
                    projectileSystem.introPlayed = true; //just skip the intro
                if (!projectileSystem.isRunning() && !projectileSystem.introPlayed) {
                    projectileSystem.introPlayed = true;
                    VideoPane vp = App.getVideoPane();
                    vp.setVideo(false);
                    Pane desktopPane = App.getAppPathPaneStack();
                    App.getAppPathPaneStack().getChildren().add(vp);
                    vp.moveTo(desktopPane.getWidth() / 2.0
                            - vp.getBoundsInLocal().getWidth() / 2.0,
                        desktopPane.getHeight() / 2.0
                            - vp.getBoundsInLocal().getHeight() / 2.0);
                    vp.restore();
                    vp.show();
                    vp.setOpacity(1);
                    vp.mediaPlayer.setOnPaused(() -> {
                        toggleProjectileViews();
                        vp.shutdown();
                    });
                    vp.mediaPlayer.setOnEndOfMedia(() -> {
                        vp.shutdown();
                        toggleProjectileViews();
                    });
                } else {
                    toggleProjectileViews();
                }
            }
            if (keycode == KeyCode.Z && k.isControlDown()) {
                projectileSystem.thrustPlayer();
            }
            if (keycode == KeyCode.SPACE && k.isControlDown()) {
                projectileSystem.fire();
            }
            if (keycode == KeyCode.SPACE && k.isShiftDown()) {
                projectileSystem.fireTracer();
            }

            if (keycode == KeyCode.F10 && k.isControlDown()) {
                projectileSystem.toggleAlien();
            }
            if (keycode == KeyCode.F12 && k.isShiftDown()) {
                if (!dataXForm.getChildren().contains(projectileSystem.playerShip)) {
                    dataXForm.getChildren().add(projectileSystem.playerShip);
                    enableContextMenu = false; //right clicking interferes...
                } else {
                    dataXForm.getChildren().remove(projectileSystem.playerShip);
                    enableContextMenu = true;
                }
            }
        });
        scene.addEventHandler(ApplicationEvent.SHOW_JOYSTICK_CONTROLS, e -> {
            Pane pathPane = App.getAppPathPaneStack();
            if (!pathPane.getChildren().contains(joystickPane)) {
                pathPane.getChildren().add(joystickPane);
                joystickPane.slideInPane();
            } else {
                joystickPane.show();
            }
        });
        scene.addEventHandler(ApplicationEvent.BACK_TO_WORK, e -> {
            Pane pathPane = App.getAppPathPaneStack();
            pathPane.getChildren().remove(joystickPane);
            dataXForm.getChildren().remove(projectileSystem.playerShip);
            resetView(250, false);
            projectileSystem.playerShip.setCockPitView(false);
            bindCameraRotations = false;
            toggleProjectileViews();
            enableContextMenu = true;
        });

        scene.addEventHandler(ApplicationEvent.FPS_CAMERA_MODE, e -> {
            cameraTransform.setPivot(0, 0, 0);
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(100),
                    new KeyValue(cameraTransform.translateXProperty(), 0),
                    new KeyValue(cameraTransform.translateYProperty(), 0),
                    new KeyValue(cameraTransform.translateZProperty(), 0)
                )
            );
            timeline.setOnFinished(f -> {
                Timeline zoomTimeline = JavaFX3DUtils.transitionCameraTo(
                    100, camera, cameraTransform,
                    0, 0, 0, 0, 0, 0.0);
                zoomTimeline.setOnFinished(zt -> {
                    projectileSystem.playerShip.setCockPitView(true);
                    bindCameraRotations = true;
                });
            });
            timeline.playFromStart();
        });
        scene.addEventHandler(ApplicationEvent.SHOULDER_CAMERA_MODE, e -> {
            javafx.geometry.Point3D p = sceneRoot.localToScene(
                projectileSystem.playerShip.getLocation(), false);
            LOG.info("Pivoting for Shoulder Cam at: {}", p.toString());
            cameraTransform.setTranslateX(0);
            cameraTransform.setTranslateY(0);
            cameraTransform.setTranslateZ(0);
//                cameraTransform.setPivot(p.getX(), p.getY(), p.getZ());

            double[] rots = JavaFX3DUtils.extractRotationAngles(
                projectileSystem.playerShip.affineTransform);
            cameraTransform.rz.setAngle(rots[2]);
            cameraTransform.ry.setAngle(rots[1]);
            cameraTransform.rx.setAngle(rots[0]);
            camera.setTranslateX(p.getX());
            camera.setTranslateY(p.getY());
            camera.setTranslateZ(p.getZ() - 250);
            projectileSystem.playerShip.setCockPitView(true);
            bindCameraRotations = true;

        });
        scene.addEventHandler(ApplicationEvent.FREE_CAMERA_MODE, e -> {
            resetView(250, false);
            projectileSystem.playerShip.setCockPitView(false);
            bindCameraRotations = false;
        });
        subScene.sceneProperty().addListener(cl -> {
            Platform.runLater(() -> {
                updateFloatingNodes();
                //create callout automatically puts the callout and node into a managed map
                FeatureVector dummy = FeatureVector.EMPTY_FEATURE_VECTOR("", 3);
                anchorCallout = radialOverlayPane.createCallout(anchorTSM, dummy, subScene);
                anchorCallout.play();
                anchorCallout.setVisible(false);

                joystickPane = new JoystickPane(scene, App.getAppPathPaneStack());
                joystickPane.fireButton.setOnAction(e -> projectileSystem.fire());
                joystickPane.thrustButton.setOnAction(e -> projectileSystem.thrustPlayer());
                joystickPane.angleproperty.subscribe(c -> {
                    projectileSystem.playerShip.mouseDragged(null,
                        joystickPane.mouseDeltaX,
                        joystickPane.mouseDeltaY);
                });

                joystickPane.valueproperty.subscribe(c -> {
                    projectileSystem.playerShip.mouseDragged(null,
                        joystickPane.mouseDeltaX,
                        joystickPane.mouseDeltaY);
                });
            });
        });
    }

    private void toggleProjectileViews() {
        //first toggle the system
        projectileSystem.setRunning(!projectileSystem.isRunning());
        //hide the boring serious stuff
        xSphere.setVisible(!projectileSystem.isRunning());
        ySphere.setVisible(!projectileSystem.isRunning());
        zSphere.setVisible(!projectileSystem.isRunning());
        xLabel.setVisible(!projectileSystem.isRunning());
        yLabel.setVisible(!projectileSystem.isRunning());
        zLabel.setVisible(!projectileSystem.isRunning());
        manifoldGroup.setVisible(!projectileSystem.isRunning());
        connectorsGroup.setVisible(!projectileSystem.isRunning());
        ellipsoidGroup.setVisible(!projectileSystem.isRunning());
        highlighterNeonCircle.setVisible(!projectileSystem.isRunning());
        anchorTrajectory.setVisible(!projectileSystem.isRunning());
        anchorTraj3D.setVisible(!projectileSystem.isRunning());
        trajectorySphereGroup.setVisible(!projectileSystem.isRunning());
        trajectoryGroup.setVisible(!projectileSystem.isRunning());
        miniCrosshair.setVisible(!projectileSystem.isRunning());
        //show the cool stuff
        debugGroup.setVisible(projectileSystem.isRunning());
        skybox.setVisible(projectileSystem.isRunning());
        viewControlsMenu.setVisible(projectileSystem.isRunning());
    }

    private void resetAsteroids() {
        LOG.info("Resetting Shapes...");
        projectileSystem.clearAllHitShapes();
        projectileSystem.clearAllHittables();
        debugGroup.getChildren().removeIf(c -> c instanceof HitShape3D);
        //copy all shapes into the projectile system
        getManifoldViews().getChildren()
            .filtered(m -> m instanceof Manifold3D)
            .forEach(t -> {
                Manifold3D man3D = (Manifold3D) t;
                try {
                    projectileSystem.makeAsteroidFromPoints(man3D);
                } catch (Exception ex) {
                    LOG.info("Could not make HitShape3D: {}", ex.getMessage(), ex);
                }
            });
    }

    public void updateOnLabelChange(List<FactorLabel> labels) {
        labels.forEach(factorLabel -> {
            sphereToFeatureVectorMap.forEach((s, fv) -> {
                if (fv.getLabel().contentEquals(factorLabel.getLabel())) {
                    s.setVisible(factorLabel.getVisible());
                    ((PhongMaterial) s.getMaterial()).setDiffuseColor(factorLabel.getColor());
                }
            });
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
        anchorTraj3D = JavaFX3DUtils.buildPolyLineFromTrajectory(anchorTrajectory,
            8.0f, Color.ALICEBLUE, trajectoryTailSize,
            trajectoryScale, sceneWidth, sceneHeight);
        anchorTraj3D.setVisible(wasVisible);
        extrasGroup.getChildren().add(0, anchorTraj3D);
        for (Point3D point : anchorTraj3D.points) {
            TriaxialSpheroidMesh tsm = createEllipsoid(anchorTraj3D.width / 4.0, anchorTraj3D.width / 4.0, anchorTraj3D.width / 4.0, Color.LIGHTBLUE);
            tsm.setTranslateX(point.x);
            tsm.setTranslateY(point.y);
            tsm.setTranslateZ(point.z);
            trajectorySphereGroup.getChildren().add(tsm);
        }
        if (null == latestUmap)
            return; //can't do this without a transform matrix

        if (updatingTrajectories || overrideAuto) {
            //now rebuild all the generic trajectories
            trajectoryGroup.getChildren().clear();
            trajToTraj3DMap.clear();
            //@TODO SMP check to see if each trajectory is visible
            //no need to do all the work to transform and render lines if they're not visible
            //Maybe short cut for now is a simple boolean variable blocking all updates
            for (Trajectory trajectory : Trajectory.getTrajectories()) {
                FeatureCollection fc = Trajectory.globalTrajectoryToFeatureCollectionMap.get(trajectory);
                trajectory.states.clear();
                //These are the original feature values.
                //They need to be transformed using the current UMAP transformation matrix
                ArrayList<double[]> newStates = transformXYZ(fc);

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

    public Manifold3D makeHull(List<Point3D> labelMatchedPoints, String label, Double tolerance) {
        Manifold3D manifold3D = new Manifold3D(
            labelMatchedPoints, true, true, true, tolerance
        );
        manifold3D.quickhullMeshView.setCullFace(CullFace.FRONT);
        manifold3D.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if ((e.getButton() == MouseButton.PRIMARY && e.isControlDown())
                || (e.getButton() == MouseButton.PRIMARY && pointToPointDistanceMode)) {
                processDistanceClick(manifold3D);
            }
        });
        manifolds.add(manifold3D);
        manifoldGroup.getChildren().add(manifold3D);
        manifold3D.shape3DToLabel.putAll(manifold3D.shape3DToLabel);
        updateFloatingNodes();
        return manifold3D;
    }

    private void setupSkyBox() {
        // Load Skybox AFTER camera is initialized
        double size = 100000D;
        Image singleImage = new Image(ImageResourceProvider.getResource("space-skybox.png").toExternalForm());
        skybox = new Skybox(
            singleImage,
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

    private void updateDataColors() {
        sphereToFeatureVectorMap.forEach((s, fv) -> {
            if (null != fv.getLabel()) {
                FactorLabel fl = FactorLabel.getFactorLabel(fv.getLabel());
                s.setVisible(fl.getVisible());
                ((PhongMaterial) s.getMaterial()).setDiffuseColor(fl.getColor());
                ((PhongMaterial) s.getMaterial()).setSpecularColor(fl.getColor());
            }
        });
    }

    private void changeFactorLabels(FactorLabel factorLabel) {
        sphereToFeatureVectorMap.forEach((s, fv) -> {
            if (null != fv.getLabel() && fv.getLabel().contentEquals(factorLabel.getLabel())) {
                s.setVisible(factorLabel.getVisible());
                ((PhongMaterial) s.getMaterial()).setDiffuseColor(factorLabel.getColor());
            }
        });
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
            cameraTransform.setPivot(0, 0, 0);
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(milliseconds),
                    new KeyValue(cameraTransform.translateXProperty(), 0),
                    new KeyValue(cameraTransform.translateYProperty(), 0),
                    new KeyValue(cameraTransform.translateZProperty(), 0)
                )
            );
            timeline.setOnFinished(eh -> {
                Timeline zoomTimeline = JavaFX3DUtils.transitionCameraTo(milliseconds, camera, cameraTransform,
                    0, 0, cameraDistance, -10.0, -45.0, 0.0);
            });
            timeline.playFromStart();
        } else {
            cameraTransform.setPivot(0, 0, 0);

            cameraTransform.rx.setAngle(-10);
            cameraTransform.ry.setAngle(-45.0);
            cameraTransform.rz.setAngle(0.0);
            camera.setTranslateX(0);
            camera.setTranslateY(0);
            camera.setTranslateZ(cameraDistance);

            cameraTransform.setPivot(0, 0, 0);
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

        if (me.isPrimaryButtonDown()) {
            if (me.isAltDown()) { //roll
                cameraTransform.rz.setAngle(((cameraTransform.rz.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
                if (bindCameraRotations)
                    projectileSystem.playerShip.addRotation(cameraTransform.rz.getAngle(), Rotate.Z_AXIS);
            } else {
                cameraTransform.ry.setAngle(((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
                cameraTransform.rx.setAngle(
                    ((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
                if (bindCameraRotations) {
                    //must clamp the turns
                    double yChange =
                        (((mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
                    double xChange =
                        (((-mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
                    projectileSystem.playerShip.addRotation(yChange, Rotate.Y_AXIS);
                    projectileSystem.playerShip.addRotation(xChange, Rotate.X_AXIS);
                }
            }
        } else if (me.isMiddleButtonDown()) {
            cameraTransform.t.setX(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3); // -
            cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3); // -
        }

        //right clicking should rotate pointing objects in the xform group only
        if (enableXForm && me.isSecondaryButtonDown()) {
            double yChange = (((mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            double xChange = (((-mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            //LOG.info("BINDCAMERAROTATIONS Rotating data and not camera: {} {}", yChange, xChange);
            dataXForm.addRotation(yChange, Rotate.Y_AXIS);
            dataXForm.addRotation(xChange, Rotate.X_AXIS);
        }
        cameraLight.setTranslateX(camera.getTranslateX());
        cameraLight.setTranslateY(camera.getTranslateY());
        cameraLight.setTranslateZ(camera.getTranslateZ());

        updateFloatingNodes();
        radialOverlayPane.updateCalloutHeadPoints(subScene);
    }

    private void setCircleRadiusByDistance(AnimatedNeonCircle circle, Sphere sphere) {
        javafx.geometry.Point3D p1 = new javafx.geometry.Point3D(
            sphere.getTranslateX(), sphere.getTranslateY(), sphere.getTranslateZ());
        javafx.geometry.Point3D rP3D = new javafx.geometry.Point3D(camera.getTranslateX(),
            camera.getTranslateY(), camera.getTranslateZ());
        javafx.geometry.Point3D rP3D2 = cameraTransform.ry.transform(rP3D);
        javafx.geometry.Point3D rP3D3 = cameraTransform.rx.transform(rP3D2);
        double ratio = Math.abs(rP3D3.distance(p1) / camera.getTranslateZ());
        circle.setRadius(25.0 / ratio);
    }

    private void updateFloatingNodes() {
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
        updateHighlighterCircle(); //special case
    }

    private void updateHighlighterCircle() {
        javafx.geometry.Point3D coordinates =
            highlightedPoint.localToScene(javafx.geometry.Point3D.ZERO, true);
        double x = coordinates.getX();
        double y = coordinates.getY();
        //highlighterNeonCircle.setMouseTransparent(true);
        //is it left of the view?
        if (x < 0) {
            x = 0;
        }
        //is it right of the view?
        if ((x + highlighterNeonCircle.getRadius() + 5) > subScene.getWidth()) {
            x = subScene.getWidth() - (highlighterNeonCircle.getRadius() + 5);
        }
        //is it above the view?
        if (y < 0) {
            y = 0;
        }
        //is it below the view
        if ((y + highlighterNeonCircle.getRadius()) > subScene.getHeight())
            y = subScene.getHeight() - (highlighterNeonCircle.getRadius() + 5);
        //update the local transform of the label.
        highlighterNeonCircle.getTransforms().setAll(new Translate(x, y));
        setCircleRadiusByDistance(highlighterNeonCircle, highlightedPoint);
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
        shape3DToLabel.clear();
        //Add to hashmap so updateLabels() can manage the label position
        shape3DToLabel.put(xSphere, xLabel);
        shape3DToLabel.put(ySphere, yLabel);
        shape3DToLabel.put(zSphere, zLabel);
        //@DIRTY BUG! SMP... since projections visibility is determined by
        //I leave this in here as a warning to the future
        //hyperspace visibility we should NOT be clearing this teensy hacky
        //static method of tracking visibility by point.
        //VisibilityMap.clearAll();
    }

    public void showAll() {
        updateEllipsoids();
    }

    @Override
    public void setSpheroidAnchor(boolean animate, int index) {
        if (index >= featureVectors.size()) {
            return;
        } else if (index < 0) {
            return;
        }
        Point3D p3d = new Point3D(
            featureVectors.get(index).getData().get(0) * projectionScalar,
            featureVectors.get(index).getData().get(1) * -projectionScalar,
            featureVectors.get(index).getData().get(2) * projectionScalar
        );
        anchorTSM.setTranslateX(p3d.x);
        anchorTSM.setTranslateY(p3d.y);
        anchorTSM.setTranslateZ(p3d.z);

        //make sure we have the latest states using the latest feature indices
        anchorTrajectory.states.clear();
        for (FeatureVector fv : featureVectors) {
            anchorTrajectory.states.add(new double[]{
                fv.getData().get(0) * projectionScalar,
                fv.getData().get(1) * -projectionScalar,
                fv.getData().get(2) * projectionScalar
            });
        }
        updateTrajectory3D(false);
        if (index < featureVectors.size()) {
            scene.getRoot().fireEvent(new FeatureVectorEvent(
                FeatureVectorEvent.SELECT_FEATURE_VECTOR,
                featureVectors.get(index), featureLabels));
            //try to update the callout anchored to the lead state
            radialOverlayPane.updateCalloutByFeatureVector(anchorCallout, featureVectors.get(index));
            radialOverlayPane.updateCalloutHeadPoint(anchorTSM, anchorCallout, subScene);
        }
    }

    private ArrayList<double[]> transformXYZ(FeatureCollection fc) {
        ArrayList<double[]> states = new ArrayList<>();
        if (null == latestUmap || null == latestUmap.getmEmbedding()) {
            Platform.runLater(() -> {
                getScene().getRoot().fireEvent(new CommandTerminalEvent(
                    "Could not transform Feature Collection since current UMAP embeddings are null.",
                    new Font("Consolas", 20), Color.RED,
                    ResourceUtils.loadIcon("error", ICON_FIT_WIDTH)));
            });
            return states;
        }
        double[][] transformed = AnalysisUtils.transformUMAP(fc, latestUmap);
        for (int row = 0; row < transformed.length; row++) {
            double[] stateVector = new double[transformed[row].length];
            for (int columns = 0; columns < transformed[row].length; columns++) {
                stateVector[columns] = transformed[row][columns] * projectionScalar;
                if (columns == 1 && reflectY)
                    stateVector[columns] = -stateVector[columns];
            }
            states.add(stateVector);
        }
        return states;
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

    private void updateDataRadius() {
        ellipsoidGroup.getChildren().filtered(n -> n instanceof Sphere).forEach(s -> {
            ((Sphere) s).setRadius(point3dSize);
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
        trimQueueNow();
    }

    @Override
    public void addFeatureCollection(FeatureCollection featureCollection, boolean clearQueue) {
        Platform.runLater(() -> {
            getScene().getRoot().fireEvent(
                new CommandTerminalEvent("Projecting Feature Collection... ",
                    new Font("Consolas", 20), Color.GREEN));
        });
        clearAll();
        ellipsoidGroup.getChildren().clear();
        sphereToFeatureVectorMap.clear();
        Platform.runLater(() -> {
            getScene().getRoot().fireEvent(
                new CommandTerminalEvent("Projecting Feature Collection... ",
                    new Font("Consolas", 20), Color.GREEN));
        });
        //Make a 3D sphere for each projected feature vector
        //@DEBUG SMP
        //System.out.println("Total Features to project: " + featureCollection.getFeatures().size());
        for (int i = 0; i < featureCollection.getFeatures().size(); i++) {
            FeatureVector featureVector = featureCollection.getFeatures().get(i);
            addProjectedFeatureVector(featureVector);
            //@EXPERIMENTAL SMP
            addProjectorNode(featureVector);
        }
        trimQueueNow();
    }

    private void addProjectorNode(FeatureVector featureVector) {
        if (null != featureVector.getText() && !featureVector.getText().isBlank()) {
            ProjectorNode pn = new ProjectorTextNode(featureVector.getText());
            featureVectorToProjectorNode.put(featureVector, pn);
            Platform.runLater(() -> {
//                scene.getRoot().fireEvent(new FeatureVectorEvent(
//                    FeatureVectorEvent.SELECT_FEATURE_VECTOR, featureVector, featureLabels));
                projectorNodeGroup.addNodeToScene(pn, featureVector.getLabel());
                pn.setVisible(true);
                if (animatingProjections) {
                    ParallelTransition pt = projectorNodeGroup.createTransition(pn);
                    pt.play();
                }
            });
        }
    }

    public void addProjectedFeatureVector(FeatureVector featureVector) {
        Color labelColor = FactorLabel.getColorByLabel(featureVector.getLabel());
        PhongMaterial mat = new PhongMaterial(labelColor);
        mat.setSpecularColor(labelColor); //fix for aarch64 Mac Ventura
        Sphere sphere = new AnimatedSphere(mat, point3dSize, 32, true);

        sphere.setMaterial(mat);
        sphere.setTranslateX(featureVector.getData().get(0) * projectionScalar);
        sphere.setTranslateY(featureVector.getData().get(1) * -projectionScalar);
        sphere.setTranslateZ(featureVector.getData().size() > 2
            ? featureVector.getData().get(2) * projectionScalar
            : 0.0);
        Platform.runLater(() -> {
            ellipsoidGroup.getChildren().add(sphere);
            if (animatingProjections) {
                highlightedPoint = sphere;
                //System.out.println("Found matching Feature Vector...");
                updateHighlighterCircle(); //Will transform location of circle
            }
        });
        sphereToFeatureVectorMap.put(sphere, featureVector);

        //Add Circle as highlight when mouse hovering
        sphere.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            highlightedPoint = sphere;
            updateFloatingNodes(); //Will transform location of all floating 2D nodes
            javafx.geometry.Point3D p1 = new javafx.geometry.Point3D(
                sphere.getTranslateX(), sphere.getTranslateY(), sphere.getTranslateZ());
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.SELECT_PROJECTION_POINT3D, p1));
            miniCrosshair.size = point3dSize * 4.0;
            miniCrosshair.setCenter(p1);
            setCircleRadiusByDistance(highlighterNeonCircle, sphere);
            //update selection listeners with original hyper dimensions (eg RADAR plot)
            FeatureVector fv = sphereToFeatureVectorMap.get(sphere);
            if (null != fv) {
                scene.getRoot().fireEvent(new FeatureVectorEvent(
                    FeatureVectorEvent.SELECT_FEATURE_VECTOR, fv, featureLabels));
                if (animatingProjections) {
                    ProjectorNode pn = featureVectorToProjectorNode.get(fv);
                    if (null != pn) {
                        extrasGroup.getChildren().remove(projectorConnector);
                        Trajectory trajectory = new Trajectory("Projector Connector");
                        trajectory.states.clear();
                        //These are the original feature values.
                        //They need to be transformed using the current UMAP transformation matrix
                        trajectory.states.add(new double[]{sphere.getTranslateX(),
                            sphere.getTranslateY(), sphere.getTranslateZ()});
                        trajectory.states.add(new double[]{pn.node.getTranslateX(),
                            pn.node.getTranslateY(), pn.node.getTranslateZ()});
                        projectorConnector = JavaFX3DUtils.buildPolyLineFromTrajectory(
                            trajectory, 8.0f, trajectory.getColor(),
                            trajectory.states.size(), trajectoryScale, sceneWidth, sceneHeight);
                        extrasGroup.getChildren().add(projectorConnector);
                        projectorConnector.setVisible(true);
                    }
                }
            } else {
                projectorConnector.setVisible(false);
            }
        });

        //Add click handler to popup callout or point distance measurements
        sphere.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && !e.isControlDown())
                radialOverlayPane.createCallout(sphere, featureVector, subScene);
            else if ((e.getButton() == MouseButton.PRIMARY && e.isControlDown())
                || (e.getButton() == MouseButton.PRIMARY && pointToPointDistanceMode)) {
                processDistanceClick(sphere);
            }

            MenuItem orbitItem = new MenuItem("Orbit On Point");
            orbitItem.setOnAction(o -> {
                javafx.geometry.Point3D orbitPoint3D = new javafx.geometry.Point3D(
                    sphere.getTranslateX(), sphere.getTranslateY(), sphere.getTranslateZ());
                JavaFX3DUtils.orbitAt(camera, cameraTransform, orbitPoint3D, true);
            });
            ContextMenu cm = new ContextMenu(orbitItem);
            cm.setAutoFix(true);
            cm.setAutoHide(true);
            cm.setHideOnEscape(true);
            cm.setOpacity(0.85);

            if (e.getButton() == MouseButton.SECONDARY) {
                if (null != cm)
                    if (!cm.isShowing())
                        cm.show(this.getParent(), e.getScreenX(), e.getScreenY());
                    else
                        cm.hide();
                e.consume();
            }
        });

        featureVectors.add(featureVector);
    }

    private void pointToManifold(Manifold3D manifold3D) {
        javafx.geometry.Point3D p1 = new javafx.geometry.Point3D(
            selectedSphereA.getTranslateX(),
            selectedSphereA.getTranslateY(),
            selectedSphereA.getTranslateZ());
        javafx.geometry.Point3D p2 = selectedManifoldA.getClosestHullPoint(p1);
        LOG.info("Difference: {}", p1.subtract(p2).toString());
        LOG.info("Distance: {}", p1.distance(p2));
        //Fire off event to create new distance object
        String distanceLabel =
            sphereToFeatureVectorMap.get(selectedSphereA).getLabel()
                + " => " + manifold3D.toString();
        Distance distanceObject = new Distance(
            distanceLabel, Color.ALICEBLUE, "euclidean", 5);
        distanceObject.setPoint1(p1);
        distanceObject.setPoint2(p2);
        distanceObject.setValue(p1.distance(p2));

        getScene().getRoot().fireEvent(new ManifoldEvent(
            ManifoldEvent.CREATE_NEW_DISTANCE, distanceObject));
        //Add 3D line to scene connecting the two points
        //Creates a new distance trajectory (Trajectory3D polyline)
        //returns midpoint so we can anchor a numeric distance label
        Sphere midPointSphere = updateDistanceTrajectory(null, distanceObject);
        selectedManifoldA.toggleSelection(false);
        //Clear selected spheres to null state
        selectedSphereA = null;
        selectedSphereB = null;
        selectedManifoldA = null;
        selectedManifoldB = null;
    }

    private void pointToPoint() {
        javafx.geometry.Point3D p1 = new javafx.geometry.Point3D(
            selectedSphereA.getTranslateX(),
            selectedSphereA.getTranslateY(),
            selectedSphereA.getTranslateZ());
        javafx.geometry.Point3D p2 = new javafx.geometry.Point3D(
            selectedSphereB.getTranslateX(),
            selectedSphereB.getTranslateY(),
            selectedSphereB.getTranslateZ());
        LOG.info("Difference: {}", p1.subtract(p2).toString());
        LOG.info("Distance: {}", p1.distance(p2));
        //Fire off event to create new distance object
        String distanceLabel =
            sphereToFeatureVectorMap.get(selectedSphereA).getLabel()
                + " => " +
                sphereToFeatureVectorMap.get(selectedSphereB).getLabel();
        Distance distanceObject = new Distance(
            distanceLabel, Color.ALICEBLUE, "euclidean", 5);
        distanceObject.setPoint1(p1);
        distanceObject.setPoint2(p2);
        distanceObject.setValue(p1.distance(p2));

        getScene().getRoot().fireEvent(new ManifoldEvent(
            ManifoldEvent.CREATE_NEW_DISTANCE, distanceObject));
        //Add 3D line to scene connecting the two points
        updateDistanceTrajectory(null, distanceObject);
        //Clear selected spheres to null state
        selectedSphereA = null;
        selectedSphereB = null;
        if (null != selectedManifoldA) {
            selectedManifoldA.toggleSelection(false);
            selectedManifoldA = null;
        }
        if (null != selectedManifoldB) {
            selectedManifoldB.toggleSelection(false);
            selectedManifoldB = null;
        }
    }

    private void processDistanceClick(Manifold3D manifold3D) {
        LOG.info("Point: {}", manifold3D.toString());
        if (null == selectedManifoldA) {
            selectedManifoldA = manifold3D;
            //add some sort of visual highlight
            selectedManifoldA.toggleSelection(true);
        } else {
            selectedManifoldB = manifold3D;
            //@TODO add some sort of visual highlight
        }
        //Are we doing a point to manifold check?
        if (null != selectedSphereA && null != selectedManifoldA) {
            pointToManifold(selectedManifoldA);
        }
    }

    private void processDistanceClick(Sphere sphere) {
        LOG.info("Point: {}", sphere.toString());
        if (null == selectedSphereA) {
            selectedSphereA = sphere;
            //@TODO add some sort of visual highlight
        } else {
            selectedSphereB = sphere;
            //@TODO add some sort of visual highlight
        }
        if (null != selectedSphereA && null != selectedSphereB) {
            pointToPoint();
        } else if (null != selectedSphereA && null != selectedManifoldA) {
            pointToManifold(selectedManifoldA);
        }

    }

    //Add 3D line to scene connecting the two points
    private Sphere updateDistanceTrajectory(Trajectory3D distanceTraj3D, Distance distance) {
        if (null != distanceTraj3D) {
            connectorsGroup.getChildren().remove(distanceTraj3D);
            distanceTotrajectory3DMap.remove(distance);
            //Clear this connector's label overlay
            Shape3D shape3D = distanceToShape3DMap.get(distance);
            extrasGroup.getChildren().remove(shape3D);
            Label label = shape3DToLabel.remove(shape3D);
            labelGroup.getChildren().remove(label);
            distanceToShape3DMap.remove(distance);

        }
        //Build trajectory by 3D coordinate states
        Trajectory distanceTrajectory = new Trajectory("Distance Line");
        distanceTrajectory.states.clear();
        distanceTrajectory.states.add(
            new double[]{distance.getPoint1().getX(), distance.getPoint1().getY(), distance.getPoint1().getZ()});
        //add midpoint so we can anchor a numeric distance label
        javafx.geometry.Point3D midpoint = distance.getPoint1().midpoint(distance.getPoint2());
        distanceTrajectory.states.add(
            new double[]{midpoint.getX(), midpoint.getY(), midpoint.getZ()});
        distanceTrajectory.states.add(
            new double[]{distance.getPoint1().getX(), distance.getPoint1().getY(), distance.getPoint1().getZ()});
        distanceTrajectory.states.add(
            new double[]{distance.getPoint2().getX(), distance.getPoint2().getY(), distance.getPoint2().getZ()}
        );
        //Add 2D distance label overlay at midpoint
        Sphere midpointSphere = new Sphere(1);

        midpointSphere.setTranslateX(midpoint.getX());
        midpointSphere.setTranslateY(midpoint.getY());
        midpointSphere.setTranslateZ(midpoint.getZ());
        NumberFormat doubleFormat = new DecimalFormat("0.0000");
        Label midpointLabel = new Label(distance.getLabel() + "\n"
            + distance.getMetric() + ": " + doubleFormat.format(distance.getValue()));
        midpointLabel.setTextAlignment(TextAlignment.LEFT);
        midpointLabel.setMouseTransparent(true);
        extrasGroup.getChildren().add(midpointSphere);
        shape3DToLabel.put(midpointSphere, midpointLabel);
        distanceToShape3DMap.put(distance, midpointSphere);
        labelGroup.getChildren().add(midpointLabel);

        Trajectory3D newDistanceTraj3D = JavaFX3DUtils.buildPolyLineFromTrajectory(
            distanceTrajectory, distance.getWidth(), distance.getColor(),
            trajectoryTailSize, trajectoryScale, sceneWidth, sceneHeight);
        newDistanceTraj3D.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            getScene().getRoot().fireEvent(
                new ManifoldEvent(ManifoldEvent.DISTANCE_CONNECTOR_SELECTED, distance));
        });
        newDistanceTraj3D.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            getScene().getRoot().fireEvent(
                new ManifoldEvent(ManifoldEvent.DISTANCE_CONNECTOR_SELECTED, distance));
            midpointLabel.setScaleX(3);
            midpointLabel.setScaleY(3);
            midpointLabel.setScaleZ(3);
            midpointSphere.setRadius(5);
        });
        newDistanceTraj3D.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            midpointLabel.setScaleX(1);
            midpointLabel.setScaleY(1);
            midpointLabel.setScaleZ(1);
            midpointSphere.setRadius(1);
        });
        newDistanceTraj3D.visibleProperty().bind(distance.visible);
        midpointSphere.visibleProperty().bind(distance.visible);
        midpointLabel.visibleProperty().bind(distance.visible);
        distanceTotrajectory3DMap.put(distance, newDistanceTraj3D);
        connectorsGroup.getChildren().add(0, newDistanceTraj3D);

        if (null != selectedManifoldA) {
            Callout ddc = DistanceDataCallout.createByManifold3D(
                midpointSphere, selectedManifoldA.getManifold(),
                new Point3D(distance.getPoint1().getX(), distance.getPoint1().getY(),
                    distance.getPoint1().getZ()), subScene);
            radialOverlayPane.addCallout(ddc, midpointSphere);
            ddc.play();
            newDistanceTraj3D.addEventHandler(
                MouseEvent.MOUSE_CLICKED, e -> {
                    ddc.setVisible(true);
                    ddc.play();
                });
        }
        updateFloatingNodes();
        return midpointSphere;
    }

    public void updateMaxAndMeans() {
        meanVector = FeatureVector.getMeanVector(featureVectors);
        maxAbsValue = FeatureVector.getMaxAbsValue(featureVectors);
        meanCenteredMaxAbsValue = FeatureVector.getMeanCenteredMaxAbsValue(featureVectors, meanVector);

        String str = "Mean Centered MaxAbsValue: " + meanCenteredMaxAbsValue;
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
            Alert a = new Alert(AlertType.CONFIRMATION,
                "There are  currently " + ellipsoidGroup.getChildren().size() + " ellipsoid items.\n"
                    + "Clear the ellipsoid group before import?",
                ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            a.setTitle("Gaussian Mixture Collection Import");
            a.setHeaderText("There are  currently " + ellipsoidGroup.getChildren().size() + " ellipsoid items.");
            a.setContentText("Clear the ellipsoid group before import?");
            Optional<ButtonType> optBT = a.showAndWait();
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
    public void setColorByID(String iGotID, Color color) {
        //sweet song reference
        //https://en.wikipedia.org/wiki/Merkin_Ball
        featureVectors.stream()
            .filter(fv -> fv.getEntityId().contentEquals(iGotID))
            .forEach(f -> {
                setColorByIndex(featureVectors.indexOf(f), color);
            });
    }

    @Override
    public void setColorByIndex(int i, Color color) {
        //I'm thinking this is the ugliest thing I've ever written
        ((PhongMaterial) sphereToFeatureVectorMap.keySet()
            .toArray(Sphere[]::new)[i]
            .getMaterial()).setDiffuseColor(color);
    }

    @Override
    public void setVisibleByIndex(int i, boolean b) {
        VisibilityMap.visibilityList.set(i, b);
    }

    @Override
    public void refresh(boolean forceNodeUpdate) {
        updateDataColors();
        updateEllipsoids();
    }

    private List<Point3D> getPointsByLabel(boolean useVisiblePoints, String label) {
        List<Point3D> labelMatchedPoints = new ArrayList<>();
        for (int i = 0; i < featureVectors.size(); i++) {
            FeatureVector fv = featureVectors.get(i);
            Point3D newPoint3D = new Point3D(
                fv.getData().get(0) * projectionScalar,
                reflectY ? fv.getData().get(1) * -projectionScalar : fv.getData().get(1) * projectionScalar,
                fv.getData().get(2) * projectionScalar);
            if (null == label) {
                if (VisibilityMap.visibilityByIndex(i))
                    labelMatchedPoints.add(newPoint3D);
            } else if (useVisiblePoints) {
                if (VisibilityMap.visibilityByIndex(i) && FactorLabel.visibilityByLabel(fv.getLabel()))
                    labelMatchedPoints.add(newPoint3D);
            } else {
                if (FactorLabel.visibilityByLabel(fv.getLabel()))
                    labelMatchedPoints.add(newPoint3D);
            }
        }
        return labelMatchedPoints;
    }

    @Override
    public void addManifold(Manifold manifold, Manifold3D manifold3D) {
        manifold3D.setManifold(manifold);
        manifold3D.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            getScene().getRoot().fireEvent(
                new ManifoldEvent(ManifoldEvent.MANIFOLD_3D_SELECTED, manifold));
            if ((e.getButton() == MouseButton.PRIMARY && e.isControlDown())
                || (e.getButton() == MouseButton.PRIMARY && pointToPointDistanceMode)) {
                processDistanceClick(manifold3D);
            }
        });
        MenuItem orbitItem = new MenuItem("Orbit Here");
        orbitItem.setOnAction(e -> {
            javafx.geometry.Point3D orbitPoint3D = JavaFX3DUtils.toFX.apply(manifold3D.getBoundsCentroid());
            JavaFX3DUtils.orbitAt(camera, cameraTransform, orbitPoint3D, true);
        });
        manifold3D.cm.getItems().add(0, orbitItem);
        if (!manifolds.contains(manifold3D))
            manifolds.add(manifold3D);
        if (!manifoldGroup.getChildren().contains(manifold3D))
            manifoldGroup.getChildren().add(manifold3D);
        shape3DToLabel.putAll(manifold3D.shape3DToLabel);
        updateFloatingNodes();
    }

    @Override
    public void makeManifold(boolean useVisiblePoints, String label, Double tolerance) {
        //Create Manifold Object based on points that share the label
        List<Point3D> labelMatchedPoints = getPointsByLabel(useVisiblePoints, label);
        ArrayList<javafx.geometry.Point3D> fxPoints = labelMatchedPoints.stream()
            .map(p3D -> new javafx.geometry.Point3D(p3D.x, p3D.y, p3D.z))
            .collect(Collectors.toCollection(ArrayList::new));
        Manifold manifold = new Manifold(fxPoints, label, label, sceneColor);
        //Create the 3D manifold shape
        Manifold3D manifold3D = makeHull(labelMatchedPoints, label, tolerance);
        manifold3D.setManifold(manifold);
        manifold3D.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            getScene().getRoot().fireEvent(
                new ManifoldEvent(ManifoldEvent.MANIFOLD_3D_SELECTED, manifold));
        });
        //Add this Manifold data object to the global tracker
        Manifold.addManifold(manifold);
        //update the manifold to manifold3D mapping
        Manifold.globalManifoldToManifold3DMap.put(manifold, manifold3D);
        //announce to the world of the new manifold and its shape
        LOG.info("Manifold3D generation complete for {}", label);
        getScene().getRoot().fireEvent(new ManifoldEvent(
            ManifoldEvent.MANIFOLD3D_OBJECT_GENERATED, manifold, manifold3D));
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

    public void setHyperDimensionFeatures(FeatureCollection originalFC) {
        hyperFeatures = originalFC.getFeatures();
    }

    public void projectFeatureCollection(FeatureCollection originalFC, PCAConfig config) {
        ProjectPcaFeaturesTask task = new ProjectPcaFeaturesTask(scene, originalFC, config);
        task.setOnSucceeded(e -> {
            FeatureCollection fc;
            try {
                setHyperDimensionFeatures(originalFC);
                fc = (FeatureCollection) task.get();
                if (null != originalFC.getDimensionLabels()) {
                    setDimensionLabels(originalFC.getDimensionLabels());
                    fc.setDimensionLabels(originalFC.getDimensionLabels());
                }
                addFeatureCollection(fc, false);
                updateTrajectory3D(false);
            } catch (InterruptedException | ExecutionException ex) {
                LOG.error(null, ex);
            }
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * @param featureVector the unscaled, untransformed raw feature vector
     * @return The point3d of the vector after projection transform. Any scaling
     * to fit the 3D plot must still be done.
     */
    @Override
    public Point3D projectVector(FeatureVector featureVector) {
        if (null != latestUmap && null != latestUmap.getmEmbedding()) {
            double[] stateArray = FeatureVector.mapToStateArray.apply(featureVector);
            double[][] transformInstances = new double[2][stateArray.length];
            System.arraycopy(stateArray, 0, transformInstances[0], 0, stateArray.length);
            System.arraycopy(stateArray, 0, transformInstances[1], 0, stateArray.length);
            double[][] projections = latestUmap.transform(transformInstances);
            FeatureVector projectedFV = FeatureVector.fromData(projections[0],
                projections[0].length, 1.0);
            projectedFV.setLayer(featureVector.getLayer());
            projectedFV.setLabel(featureVector.getLabel());
            projectedFV.setImageURL(featureVector.getImageURL());
            projectedFV.setMediaURL(featureVector.getMediaURL());
            projectedFV.setText(featureVector.getText());
            //Convert projected point to sphere
            //set color according to label and projection status
            //add feature vector to sphere lookup
            //add to scene in projected group
            addProjectedFeatureVector(projectedFV);
            return new Point3D(projectedFV.getData().get(0),
                projectedFV.getData().get(1), projectedFV.getData().get(2));
//@TODO SMP
//            if in auto measurement mode, do distance check to all known manifolds
//            update spark line views with distance and threshold checks
        }
        return null;
    }


    @Override
    public void transformFeatureVector(FeatureVector featureVector) {
        Point3D transformedPoint = projectVector(featureVector);
        addProjectorNode(featureVector);
        if (animatingProjections && null != transformedPoint) {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new FeatureVectorEvent(
                    FeatureVectorEvent.SELECT_FEATURE_VECTOR, featureVector, featureLabels));
                //For the lulz... (and also to provide a visual indicator to user!)
                projectionOpticon.fireData(
                    extrasGroup,
                    new javafx.geometry.Point3D(
                        transformedPoint.getX() * projectionScalar,
                        transformedPoint.getY() * -projectionScalar,
                        transformedPoint.getZ() * projectionScalar
                    ),
                    dataAnimationMS, FactorLabel.getColorByLabel(featureVector.getLabel()));
            });
        }
    }

    public void transformFeatureCollection(final FeatureCollection fc) {
        Task task = new Task() {
            @Override
            protected Void call() throws Exception {
                //Make a 3D sphere for each projected feature vector
                for (int i = 0; i < fc.getFeatures().size(); i++) {
                    FeatureVector featureVector = fc.getFeatures().get(i);
                    Point3D transformedPoint = projectVector(featureVector);
                    //@EXPERIMENTAL SMP
                    addProjectorNode(featureVector);
                    if (animatingProjections && null != transformedPoint) {
                        Platform.runLater(() -> {
                            //For the lulz... (and also to provide a visual indicator to user!)
                            projectionOpticon.fireData(new javafx.geometry.Point3D(
                                transformedPoint.getX() * projectionScalar,
                                transformedPoint.getY() * -projectionScalar,
                                transformedPoint.getZ() * projectionScalar
                            ), dataAnimationMS, FactorLabel.getColorByLabel(featureVector.getLabel()));
                        });
                    }
                    if (animatingProjections) {
                        Thread.sleep(Double.valueOf(animationSleepMS).longValue()); //I hate this but...
                    }
                }
                return null;
            }
        };
        Thread t = new Thread(task);
        t.setDaemon(false);
        t.start();
    }

    public void enableAutoProjection(boolean enabled) {
        autoProjectionProperty.set(enabled);
    }

    public void projectFeatureCollection(FeatureCollection originalFC,
                                         SuperMDS.Params params, boolean computeMetrics) {
        ProjectMdsFeaturesTask task = new ProjectMdsFeaturesTask(
            this.getScene(), originalFC, params, false);
        task.setProjectionScalar(projectionScalar);
        task.setComputeMetrics(computeMetrics);
        task.setOnSucceeded(e -> {
            FeatureCollection fc;
            try {
                setHyperDimensionFeatures(originalFC);
                fc = (FeatureCollection) task.get();
                if (null != originalFC.getDimensionLabels()) {
                    setDimensionLabels(originalFC.getDimensionLabels());
                    fc.setDimensionLabels(originalFC.getDimensionLabels());
                }
                addFeatureCollection(fc, false);
            } catch (InterruptedException | ExecutionException ex) {
                LOG.error(null, ex);
            }
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public void projectFeatureCollection(FeatureCollection originalFC, Umap umap) {
        latestUmap = umap;
        ProjectUmapFeaturesTask task = new ProjectUmapFeaturesTask(
            scene, originalFC, umap, emptyVisionEnabled);
        task.setOnSucceeded(e -> {
            FeatureCollection fc;
            try {
                setHyperDimensionFeatures(originalFC);
                fc = (FeatureCollection) task.get();
                if (null != originalFC.getDimensionLabels()) {
                    setDimensionLabels(originalFC.getDimensionLabels());
                    fc.setDimensionLabels(originalFC.getDimensionLabels());
                }
                addFeatureCollection(fc, false);
                updateTrajectory3D(false);
            } catch (InterruptedException | ExecutionException ex) {
                LOG.error(null, ex);
            }
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void findClusters(ProjectionConfig pc) {
        //safety check
        if (pc.dataSource != ProjectionConfig.DATA_SOURCE.PROJECTIONS) return;
        //convert featurevector space into 2D array of doubles
        double[][] observations = FeatureCollection.toData(featureVectors);
        //find clusters
        switch (pc.clusterMethod) {
            case DBSCAN -> {
                DBSCANClusterTask dbscanClusterTask = new DBSCANClusterTask(
                    scene, camera, projectionScalar, observations, pc);
                if (!dbscanClusterTask.isCancelledByUser()) {
                    Thread t = new Thread(dbscanClusterTask);
                    t.setDaemon(true);
                    t.start();
                }
                break;
            }
            case HDDBSCAN -> {
                HDDBSCANClusterTask hddbscanClusterTask = new HDDBSCANClusterTask(
                    scene, camera, projectionScalar, observations, pc);
                if (!hddbscanClusterTask.isCancelledByUser()) {
                    Thread t = new Thread(hddbscanClusterTask);
                    t.setDaemon(true);
                    t.start();
                }
                break;
            }
            case KMEANS -> {
                KMeansClusterTask kmeansClusterTask = new KMeansClusterTask(
                    scene, camera, projectionScalar, observations, pc);
                if (!kmeansClusterTask.isCancelledByUser()) {
                    Thread t = new Thread(kmeansClusterTask);
                    t.setDaemon(true);
                    t.start();
                }
                break;
            }
            case KMEDIODS -> {
                KMediodsClusterTask kmediodsClusterTask = new KMediodsClusterTask(
                    scene, camera, projectionScalar, observations, pc);
                if (!kmediodsClusterTask.isCancelledByUser()) {
                    Thread t = new Thread(kmediodsClusterTask);
                    t.setDaemon(true);
                    t.start();
                }
                break;
            }

            case EX_MAX -> {
                ExMaxClusterTask exMaxClusterTask = new ExMaxClusterTask(
                    scene, camera, projectionScalar, observations, pc);
                if (!exMaxClusterTask.isCancelledByUser()) {
                    Thread t = new Thread(exMaxClusterTask);
                    t.setDaemon(true);
                    t.start();
                }
                break;
            }
            case AFFINITY -> {
                AffinityClusterTask affinityClusterTask = new AffinityClusterTask(
                    scene, camera, projectionScalar, observations, pc);
                if (!affinityClusterTask.isCancelledByUser()) {
                    Thread t = new Thread(affinityClusterTask);
                    t.setDaemon(true);
                    t.start();
                }
                break;
            }
        }
    }

    @Override
    public void addClusters(List<PointCluster> clusters) {
        //first add all the new data points if necessary
        //@TODO SMP This is temporary... we need to guard this with user prompts
        if (clusters.isEmpty()) {
            clusters.stream().sorted((o1, o2) -> {
                if (o1.getClusterId() < o2.getClusterId()) return -1;
                else if (o1.getClusterId() > o2.getClusterId()) return 1;
                else return 0;
            }).forEach(pointCluster -> {
                if (null != pointCluster.getData() && !pointCluster.getData().isEmpty()) {
                    //@TODO SMP add new data points based on these clusters
                    pointCluster.getData().forEach(p -> {
                        FeatureVector fv = new FeatureVector();
                        fv.setLabel(pointCluster.getClusterName());
                        fv.getData().addAll(p);
                        featureVectors.add(fv);
                        PhongMaterial mat = new PhongMaterial(Color.ALICEBLUE);
                        Sphere sphere = new AnimatedSphere(mat, point3dSize, 32, true);
                        mat.setSpecularColor(Color.AQUA);
                        sphere.setMaterial(mat);
                        sphere.setTranslateX(fv.getData().get(0) * projectionScalar);
                        sphere.setTranslateY(fv.getData().get(1) * -projectionScalar);
                        sphere.setTranslateZ(fv.getData().get(2) * projectionScalar);
                        ellipsoidGroup.getChildren().add(sphere);
                        sphereToFeatureVectorMap.put(sphere, fv);
                    });
                }
            });
        }
        int i = 0;

        for (PointCluster pointCluster : clusters) {
            if (pointCluster.getMap().size() >= 4) {
                List<Point3D> points = pointCluster.getMap().stream()
                    .map(p -> new Point3D(
                        featureVectors.get(p).getData().get(0) * projectionScalar,
                        featureVectors.get(p).getData().get(1) * -projectionScalar,
                        featureVectors.get(p).getData().get(2) * projectionScalar))
                    .toList();

                ArrayList<javafx.geometry.Point3D> fxPoints = points.stream()
                    .map(p3D -> new javafx.geometry.Point3D(p3D.x, p3D.y, p3D.z))
                    .collect(Collectors.toCollection(ArrayList::new));
                Manifold manifold = new Manifold(fxPoints,
                    pointCluster.getClusterName(),
                    pointCluster.getClusterName(),
                    sceneColor);
                //Create the 3D manifold shape
                Manifold3D manifold3D = makeHull(points, pointCluster.getClusterName(), null);
                manifold3D.setManifold(manifold);
                manifold3D.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                    getScene().getRoot().fireEvent(
                        new ManifoldEvent(ManifoldEvent.MANIFOLD_3D_SELECTED, manifold));
                });
                //Add this Manifold data object to the global tracker
                Manifold.addManifold(manifold);
                //update the manifold to manifold3D mapping
                Manifold.globalManifoldToManifold3DMap.put(manifold, manifold3D);
                //announce to the world of the new manifold and its shape
                //System.out.println("Manifold3D generation complete for " + label);
                getScene().getRoot().fireEvent(new ManifoldEvent(
                    ManifoldEvent.MANIFOLD3D_OBJECT_GENERATED, manifold, manifold3D));
            } else {
                LOG.info("Cluster has less than 4 points");
            }
        }
    }

    @Override
    public void setUmapConfig(UmapConfig config) {
        latestUmap = AnalysisUtils.umapConfigToUmap(config);
    }
}
