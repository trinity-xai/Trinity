/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.javafx3d;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.Distance;
import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.javafx.components.panes.JoystickPane;
import edu.jhuapl.trinity.javafx.components.radial.AnimatedNeonCircle;
import edu.jhuapl.trinity.javafx.components.radial.ViewControlsMenu;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.events.ShadowEvent;
import edu.jhuapl.trinity.javafx.javafx3d.ShadowCubeWorld.PROJECTION_TYPE;
import edu.jhuapl.trinity.javafx.javafx3d.animated.AnimatedSphere;
import edu.jhuapl.trinity.javafx.javafx3d.projectiles.HitShape3D;
import edu.jhuapl.trinity.javafx.javafx3d.projectiles.ProjectileSystem;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
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
import javafx.util.Duration;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.scene.Skybox;
import org.fxyz3d.utils.CameraTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sean Phillips
 */

public class AsteroidFieldPane extends StackPane {
    //implements
//    FeatureVectorRenderer, GaussianMixtureRenderer, ManifoldRenderer {
    private static final Logger LOG = LoggerFactory.getLogger(AsteroidFieldPane.class);
    public static double ICON_FIT_HEIGHT = 64;
    public static double ICON_FIT_WIDTH = 64;
    public static double DEFAULT_INTRO_DISTANCE = -60000.0;
    public static double DEFAULT_ZOOM_TIME_MS = 500.0;
    public PROJECTION_TYPE projectionType = ShadowCubeWorld.PROJECTION_TYPE.FIXED_ORTHOGRAPHIC;
    private double cameraDistance = -4000;
    private final double sceneWidth = 4000;
    private final double sceneHeight = 4000;
    private final double cubeSize = sceneWidth / 2.0;
    public PerspectiveCamera camera;
    public CameraTransformer cameraTransform = new CameraTransformer();
    public boolean bindCameraRotations = false;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;
    //    RadialEntityOverlayPane radialOverlayPane;
    ViewControlsMenu viewControlsMenu;
    JoystickPane joystickPane;

    public Group sceneRoot = new Group();
    public Group extrasGroup = new Group();
    public Group connectorsGroup = new Group();
    public Group debugGroup = new Group();
    public Group ellipsoidGroup = new Group();
    public Group projectedGroup = new Group();
    public XFormGroup dataXForm = new XFormGroup();
    public boolean enableXForm = false;
    public boolean enableContextMenu = true;

    public SubScene subScene;

    public double point3dSize = 10.0; //size of 3d tetrahedra
    public double pointScale = 1.0; //scales parameter value in transform
    public double scatterBuffScaling = 1.0; //scales domain range in transform
    public long hyperspaceRefreshRate = 500; //milliseconds

    public Color sceneColor = Color.BLACK;
    boolean isDirty = false;
    boolean heightChanged = false;
    boolean reflectY = true;
    Sphere highlightedPoint = new Sphere(1, 8);

    Group trajectoryGroup;
    HashMap<Trajectory, Trajectory3D> trajToTraj3DMap = new HashMap<>();
    double trajectoryScale = 1.0;
    int trajectoryTailSize = 5;

    double projectionScalar = 100.0;

    //allows 2D labels to track their 3D counterparts
    HashMap<Shape3D, Label> shape3DToLabel = new HashMap<>();

    AnimatedSphere centralBody;

    // initial rotation
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotateZ = new Rotate(0, Rotate.Z_AXIS);

    private Skybox skybox;
    private Group nodeGroup = new Group();
    private Group labelGroup = new Group();
    private Group manifoldGroup = new Group();
    private ArrayList<Manifold3D> manifolds = new ArrayList<>();
    public AnimatedNeonCircle highlighterNeonCircle;
    public Crosshair3D miniCrosshair;
    public AmbientLight ambientLight;
    public PointLight pointLight;

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

    Rectangle selectionRectangle;
    ProjectileSystem projectileSystem;
    boolean clusterSelectionMode = false;

    public AsteroidFieldPane(Scene scene) {
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
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
        cameraTransform.ry.setAngle(-45.0);
        cameraTransform.rx.setAngle(-10.0);

        setupSkyBox();
        debugGroup.setVisible(false);

        //Add 3D subscene stuff to 3D scene root object
        sceneRoot.getChildren().addAll(cameraTransform,
            nodeGroup, manifoldGroup, debugGroup,
            dataXForm, extrasGroup, connectorsGroup);

        PhongMaterial mat;
        try {
            Image diffuseImage = ResourceUtils.load3DTextureImage("retrowaveSun5");
            Image specBumpImage = ResourceUtils.load3DTextureImage("droidBumpNormalMap");
            Image specularImage = ResourceUtils.load3DTextureImage("inverseRetroline");
            mat = new PhongMaterial(Color.YELLOW, diffuseImage, null, null, null);
        } catch (IOException ex) {
            LOG.error(null, ex);
            mat = new PhongMaterial(Color.YELLOW);
        }
        centralBody = new AnimatedSphere(mat, 200, 32, true);
        centralBody.setMaterial(mat);
        extrasGroup.getChildren().add(centralBody);

        miniCrosshair = new Crosshair3D(javafx.geometry.Point3D.ZERO, 2, 1.0f);
//        nodeGroup.getChildren().add(miniCrosshair);

        subScene.setCamera(camera);
        //add a Point Light for better viewing of the grid coordinate system
        pointLight = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(pointLight);
        pointLight.setTranslateX(camera.getTranslateX());
        pointLight.setTranslateY(camera.getTranslateY());
        pointLight.setTranslateZ(camera.getTranslateZ() + 500.0);

        //Add some ambient light so folks can see it
        ambientLight = new AmbientLight(Color.WHITE);
        ambientLight.getScope().addAll(centralBody);
        sceneRoot.getChildren().add(ambientLight);

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
            if (keycode == KeyCode.OPEN_BRACKET && event.isControlDown()) {
//                Platform.runLater(() -> {
//                    cubeWorld.animateOut(1000, 20000);
//                });
            } else if (keycode == KeyCode.CLOSE_BRACKET && event.isControlDown()) {
//                Platform.runLater(() -> {
//                    cubeWorld.animateIn(1000, cubeSize);
//                });
            }
//            if ((keycode == KeyCode.NUMPAD0 && event.isControlDown())
//                || (keycode == KeyCode.DIGIT0 && event.isControlDown())) {
//                resetView(1000, false);
//            } else if ((keycode == KeyCode.NUMPAD0 && event.isShiftDown())
//                || (keycode == KeyCode.DIGIT0 && event.isShiftDown())) {
//                resetView(0, true);
//            }
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

            if (keycode == KeyCode.SLASH && event.isControlDown()) {
                debugGroup.setVisible(!debugGroup.isVisible());
            }

//            //point size and scaling
//            if (keycode == KeyCode.O || (keycode == KeyCode.P && event.isControlDown())) {
//                point3dSize -= 1;
//                Platform.runLater(() -> scene.getRoot().fireEvent(
//                    new HyperspaceEvent(HyperspaceEvent.POINT3D_SIZE_KEYPRESS, point3dSize)));
//                heightChanged = true;
//                updateView(false);
//            }
//            if (keycode == KeyCode.P) {
//                point3dSize += 1;
//                Platform.runLater(() -> scene.getRoot().fireEvent(
//                    new HyperspaceEvent(HyperspaceEvent.POINT3D_SIZE_KEYPRESS, point3dSize)));
//                heightChanged = true;
//                updateView(false);
//            }

//            if (keycode == KeyCode.F) {
//                anchorIndex--;
//                if (anchorIndex < 0) anchorIndex = 0;
//                setSpheroidAnchor(true, anchorIndex);
//            }
//            if (keycode == KeyCode.G) {
//                anchorIndex++;
//                if (anchorIndex > scatterModel.data.size()) anchorIndex = scatterModel.data.size();
//                setSpheroidAnchor(true, anchorIndex);
//            }

            updateFloatingNodes();
//            radialOverlayPane.updateCalloutHeadPoints(subScene);
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
//            radialOverlayPane.updateCalloutHeadPoints(subScene);
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
            updateFloatingNodes();
//            radialOverlayPane.updateCalloutHeadPoints(subScene);
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
//                ManifoldClusterTask manifoldClusterTask = new ManifoldClusterTask(scene,
//                    camera, sphereToFeatureVectorMap, selectionRectangle);
//                if (!manifoldClusterTask.isCancelledByUser())
//                    manifoldClusterTask.run();
                selectionRectangle.setVisible(false);
                selectionRectangle.setWidth(1);
                selectionRectangle.setHeight(1);
                clusterSelectionMode = false;
            }
        });
        bp = new BorderPane(subScene);
//        //RadialOverlayPane will hold all those nifty callouts and radial entities
//        radialOverlayPane = new RadialEntityOverlayPane(this.scene, featureVectors);
//        radialOverlayPane.prefWidthProperty().bind(widthProperty());
//        radialOverlayPane.prefHeightProperty().bind(heightProperty());
//        radialOverlayPane.minWidthProperty().bind(widthProperty());
//        radialOverlayPane.minHeightProperty().bind(heightProperty());
//        radialOverlayPane.maxWidthProperty().bind(widthProperty());
//        radialOverlayPane.maxHeightProperty().bind(heightProperty());

        //Make a 2D circle that we will use to indicate
        highlighterNeonCircle = new AnimatedNeonCircle(
            new AnimatedNeonCircle.Animation(
                Duration.millis(3000), Transition.INDEFINITE, false),
            20, 1.5, 8.0, 5.0);
        highlighterNeonCircle.setManaged(false);
        highlighterNeonCircle.setMouseTransparent(true);
        getChildren().clear();
        getChildren().addAll(bp, labelGroup,
            highlighterNeonCircle, selectionRectangle);

        Glow glow = new Glow(0.5);

        ImageView reset = ResourceUtils.loadIcon("camera", ICON_FIT_HEIGHT);
        reset.setEffect(glow);
        MenuItem resetViewItem = new MenuItem("Reset View", reset);
        resetViewItem.setOnAction(e -> resetView(1000, false));

        ImageView selectPoints = ResourceUtils.loadIcon("boundingbox", ICON_FIT_HEIGHT);
        selectPoints.setEffect(glow);
        MenuItem selectPointsItem = new MenuItem("Select Points", selectPoints);
        selectPointsItem.setOnAction(e -> {
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.CLUSTER_SELECTION_MODE, true));
        });

//        CheckMenuItem animatingProjectionsItem = new CheckMenuItem("Animating Projections");
//        animatingProjectionsItem.setSelected(animatingProjections);
//        animatingProjectionsItem.selectedProperty().addListener(cl ->
//            animatingProjections = animatingProjectionsItem.isSelected());

        ContextMenu cm = new ContextMenu(selectPointsItem,
            resetViewItem
//            animatingProjectionsItem
        );

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


        this.scene.addEventHandler(HyperspaceEvent.HYPERSPACE_BACKGROUND_COLOR, e -> {
            Color color = (Color) e.object;
            subScene.setFill(color);
        });
        this.scene.addEventHandler(HyperspaceEvent.ENABLE_HYPERSPACE_SKYBOX, e -> {
            skybox.setVisible((Boolean) e.object);
        });
        scene.addEventHandler(HyperspaceEvent.REFRESH_RATE_GUI, e -> hyperspaceRefreshRate = (long) e.object);
        scene.addEventHandler(HyperspaceEvent.POINT3D_SIZE_GUI, e -> {
            point3dSize = (double) e.object;
            updateView(false);
        });
        scene.addEventHandler(HyperspaceEvent.POINT_SCALE_GUI, e -> {
            pointScale = (double) e.object;
            updateView(false);
        });

        scene.addEventHandler(ShadowEvent.SHOW_AXES_LABELS, e -> {
            nodeGroup.setVisible((boolean) e.object);
            labelGroup.setVisible((boolean) e.object);
        });
        scene.addEventHandler(ShadowEvent.FIXED_ORHOGRAPHIC_PROJECTION, e -> {
            if ((boolean) e.object) {
                projectionType = PROJECTION_TYPE.FIXED_ORTHOGRAPHIC;
//                Platform.runLater(() -> {
//                    dataXForm.getChildren().remove(scatterMesh3D);
//                    sceneRoot.getChildren().add(scatterMesh3D);
//                });
            }
        });
        scene.addEventHandler(ShadowEvent.ROTATING_PERSPECTIVE_PROJECTION, e -> {
            if ((boolean) e.object) {
                projectionType = PROJECTION_TYPE.ROTATING_PERSPECTIVE;
//                Platform.runLater(() -> {
//                    sceneRoot.getChildren().remove(scatterMesh3D);
//                    dataXForm.getChildren().add(scatterMesh3D);
//                });
            }
        });
        scene.addEventHandler(ShadowEvent.OVERRIDE_XFORM, e -> {
            enableXForm = (boolean) e.object;
        });

        scene.addEventHandler(ManifoldEvent.CLUSTER_SELECTION_MODE, e -> {
            boolean isActive = (boolean) e.object1;
            clusterSelectionMode = isActive;
            LOG.info("Cluster Selection Mode: {}", clusterSelectionMode);
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
                        LOG.info("Hyperspace Animation Timer: {}", ex.getMessage(), ex);
                    }
                    isDirty = false;
                }
            }
        };
        animationTimer.start();

        projectileSystem = new ProjectileSystem(debugGroup, 15);
        projectileSystem.setRunning(false);
        projectileSystem.setEnableProjectileTimer(true);
        viewControlsMenu = new ViewControlsMenu(scene);
//        radialOverlayPane.addEntity(viewControlsMenu);
        viewControlsMenu.setTranslateX(200);
        viewControlsMenu.setTranslateY(200);
        viewControlsMenu.setVisible(false);
        getChildren().addAll(viewControlsMenu);

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
                toggleProjectileViews();
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
//        //Add some ambient light so folks can see it
//        AmbientLight light = new AmbientLight(Color.WHITE);
//        light.getScope().addAll(scatterMesh3D);
//        sceneRoot.getChildren().add(light);
//        sceneRoot.getChildren().add(ellipsoidGroup);

        Platform.runLater(() -> {
            updateFloatingNodes();
            updateView(true);

//            joystickPane = new JoystickPane(scene, App.getAppPathPaneStack());
//            joystickPane.fireButton.setOnAction(e -> projectileSystem.fire());
//            joystickPane.thrustButton.setOnAction(e -> projectileSystem.thrustPlayer());
//            joystickPane.angleproperty.subscribe(c -> {
//                projectileSystem.playerShip.mouseDragged(null,
//                    joystickPane.mouseDeltaX,
//                    joystickPane.mouseDeltaY);
//            });
//
//            joystickPane.valueproperty.subscribe(c -> {
//                projectileSystem.playerShip.mouseDragged(null,
//                    joystickPane.mouseDeltaX,
//                    joystickPane.mouseDeltaY);
//            });
        });
    }

    private void toggleProjectileViews() {
        //first toggle the system
        projectileSystem.setRunning(!projectileSystem.isRunning());
        xSphere.setVisible(!projectileSystem.isRunning());
        ySphere.setVisible(!projectileSystem.isRunning());
        zSphere.setVisible(!projectileSystem.isRunning());
        xLabel.setVisible(!projectileSystem.isRunning());
        yLabel.setVisible(!projectileSystem.isRunning());
        zLabel.setVisible(!projectileSystem.isRunning());
        manifoldGroup.setVisible(!projectileSystem.isRunning());
        connectorsGroup.setVisible(!projectileSystem.isRunning());
        ellipsoidGroup.setVisible(!projectileSystem.isRunning());
        projectedGroup.setVisible(!projectileSystem.isRunning());
        highlighterNeonCircle.setVisible(!projectileSystem.isRunning());
        miniCrosshair.setVisible(!projectileSystem.isRunning());
        //show the cool stuff
        debugGroup.setVisible(projectileSystem.isRunning());
//        skybox.setVisible(projectileSystem.isRunning());
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

    public Manifold3D makeHull(List<Point3D> labelMatchedPoints, String label, Double tolerance) {
        Manifold3D manifold3D = new Manifold3D(
            labelMatchedPoints, true, true, true, tolerance
        );
        manifold3D.quickhullMeshView.setCullFace(CullFace.FRONT);
//        manifold3D.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
//            if ((e.getButton() == MouseButton.PRIMARY && e.isControlDown())
//                || (e.getButton() == MouseButton.PRIMARY && pointToPointDistanceMode)) {
//                processDistanceClick(manifold3D);
//            }
//        });
        manifolds.add(manifold3D);
        manifoldGroup.getChildren().add(manifold3D);
        manifold3D.shape3DToLabel.putAll(manifold3D.shape3DToLabel);
        updateFloatingNodes();
        return manifold3D;
    }

    private void setupSkyBox() {
        // Load Skybox AFTER camera is initialized
        double size = 100000D;
        Image singleImage = new Image(AsteroidFieldPane.class.getResource(
            "images/space-skybox.png").toExternalForm());
        skybox = new Skybox(
            singleImage,
            size,
            camera
        );

        sceneRoot.getChildren().add(skybox);
        skybox.setVisible(true);
    }

    public void resetView(double milliseconds, boolean rightNow) {
        if (!rightNow) {
            cameraTransform.setPivot(0, 0, 0);
            if (projectionType == PROJECTION_TYPE.FIXED_ORTHOGRAPHIC) {
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
                dataXForm.reset();
            }
        } else {
            cameraTransform.setPivot(0, 0, 0);

            if (projectionType == PROJECTION_TYPE.FIXED_ORTHOGRAPHIC) {
                cameraTransform.rx.setAngle(-10);
                cameraTransform.ry.setAngle(-45.0);
                cameraTransform.rz.setAngle(0.0);
                camera.setTranslateX(0);
                camera.setTranslateY(0);
                camera.setTranslateZ(cameraDistance);
            } else {
                dataXForm.reset();
            }
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
            updateView(true);
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
        } else {
            double yChange = (((mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            double xChange = (((-mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            dataXForm.addRotation(yChange, Rotate.Y_AXIS);
            dataXForm.addRotation(xChange, Rotate.X_AXIS);
        }
        //right clicking should rotate pointing objects in the xform group only
        if ((projectionType == PROJECTION_TYPE.FIXED_ORTHOGRAPHIC && enableXForm && me.isSecondaryButtonDown())) {
            double yChange = (((mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            double xChange = (((-mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            LOG.info("BINDCAMERAROTATIONS Rotating data and not camera: {} {}", yChange, xChange);
            dataXForm.addRotation(yChange, Rotate.Y_AXIS);
            dataXForm.addRotation(xChange, Rotate.X_AXIS);
        }
        updateFloatingNodes();
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
//        if (xFactorIndex < featureLabels.size())
//            xLabel.setText(featureLabels.get(xFactorIndex));
//        else
//            xLabel.setText("Factor X(" + String.valueOf(xFactorIndex) + ")");
//        if (yFactorIndex < featureLabels.size())
//            yLabel.setText(featureLabels.get(yFactorIndex));
//        else
//            yLabel.setText("Factor Y(" + String.valueOf(yFactorIndex) + ")");
//        if (zFactorIndex < featureLabels.size())
//            zLabel.setText(featureLabels.get(zFactorIndex));
//        else
//            zLabel.setText("Factor Z(" + String.valueOf(zFactorIndex) + ")");
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

        javafx.geometry.Point3D coordinates =
            highlightedPoint.localToScene(javafx.geometry.Point3D.ZERO, true);
        double x = coordinates.getX();
        double y = coordinates.getY();
        highlighterNeonCircle.setMouseTransparent(true);
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

    public void updateView(boolean forcePNodeUpdate) {
        ellipsoidGroup.getChildren().filtered(n -> n instanceof Sphere).forEach(s -> {
            ((Sphere) s).setRadius(point3dSize);
        });
        Platform.runLater(() -> hardDraw());
    }

    private void hardDraw() {
        isDirty = false;
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
        ellipsoidGroup.getChildren().clear();
        shape3DToLabel.clear();
        //Add to hashmap so updateLabels() can manage the label position
        shape3DToLabel.put(xSphere, xLabel);
        shape3DToLabel.put(ySphere, yLabel);
        shape3DToLabel.put(zSphere, zLabel);
    }

    public void showAll() {
        updateView(true);
    }

    /// /    @Override
//    public void setSpheroidAnchor(boolean animate, int index) {
//        if (index >= featureVectors.size()) {
//            return;
//        } else if (index < 0) {
//            return;
//        }
//        Point3D p3d = new Point3D(
//            featureVectors.get(index).getData().get(0) * projectionScalar,
//            featureVectors.get(index).getData().get(1) * -projectionScalar,
//            featureVectors.get(index).getData().get(2) * projectionScalar
//        );
//        anchorTSM.setTranslateX(p3d.x);
//        anchorTSM.setTranslateY(p3d.y);
//        anchorTSM.setTranslateZ(p3d.z);
//
//        //make sure we have the latest states using the latest feature indices
//        anchorTrajectory.states.clear();
//        for (FeatureVector fv : featureVectors) {
//            anchorTrajectory.states.add(new double[]{
//                fv.getData().get(0) * projectionScalar,
//                fv.getData().get(1) * -projectionScalar,
//                fv.getData().get(2) * projectionScalar
//            });
//        }
//        updateTrajectory3D(false);
//        if (index < featureVectors.size()) {
//            scene.getRoot().fireEvent(new FeatureVectorEvent(
//                FeatureVectorEvent.SELECT_FEATURE_VECTOR,
//                featureVectors.get(index), featureLabels));
//            //try to update the callout anchored to the lead state
//            radialOverlayPane.updateCalloutByFeatureVector(anchorCallout, featureVectors.get(index));
//            radialOverlayPane.updateCalloutHeadPoint(anchorTSM, anchorCallout, subScene);
//        }
//    }
    private void pointToManifold(javafx.geometry.Point3D p1, Manifold3D manifold3D) {
//        javafx.geometry.Point3D p1 = new javafx.geometry.Point3D(
//            selectedSphereA.getTranslateX(),
//            selectedSphereA.getTranslateY(),
//            selectedSphereA.getTranslateZ());
        javafx.geometry.Point3D p2 = manifold3D.getClosestHullPoint(p1);
        LOG.info("Difference: {}", p1.subtract(p2).toString());
        LOG.info("Distance: {}", p1.distance(p2));
        //Fire off event to create new distance object
////        String distanceLabel =
////            sphereToFeatureVectorMap.get(selectedSphereA).getLabel()
////                + " => " + manifold3D.toString();
//        Distance distanceObject = new Distance(
//            distanceLabel, Color.ALICEBLUE, "euclidean", 5);
//        distanceObject.setPoint1(p1);
//        distanceObject.setPoint2(p2);
//        distanceObject.setValue(p1.distance(p2));
//
//        getScene().getRoot().fireEvent(new ManifoldEvent(
//            ManifoldEvent.CREATE_NEW_DISTANCE, distanceObject));
//        //Add 3D line to scene connecting the two points
//        //Creates a new distance trajectory (Trajectory3D polyline)
//        //returns midpoint so we can anchor a numeric distance label
//        Sphere midPointSphere = updateDistanceTrajectory(null, distanceObject);
//        selectedManifoldA.toggleSelection(false);
//        //Clear selected spheres to null state
//        selectedSphereA = null;
//        selectedSphereB = null;
//        selectedManifoldA = null;
//        selectedManifoldB = null;
    }

    private void pointToPoint(javafx.geometry.Point3D p1, javafx.geometry.Point3D p2) {
//        javafx.geometry.Point3D p1 = new javafx.geometry.Point3D(
//            selectedSphereA.getTranslateX(),
//            selectedSphereA.getTranslateY(),
//            selectedSphereA.getTranslateZ());
//        javafx.geometry.Point3D p2 = new javafx.geometry.Point3D(
//            selectedSphereB.getTranslateX(),
//            selectedSphereB.getTranslateY(),
//            selectedSphereB.getTranslateZ());
//        LOG.info("Difference: {}", p1.subtract(p2).toString());
//        LOG.info("Distance: {}", p1.distance(p2));
//        //Fire off event to create new distance object
//        String distanceLabel =
//            sphereToFeatureVectorMap.get(selectedSphereA).getLabel()
//                + " => " +
//                sphereToFeatureVectorMap.get(selectedSphereB).getLabel();
        Distance distanceObject = new Distance(
            "", Color.ALICEBLUE, "euclidean", 5);
        distanceObject.setPoint1(p1);
        distanceObject.setPoint2(p2);
        distanceObject.setValue(p1.distance(p2));

        getScene().getRoot().fireEvent(new ManifoldEvent(
            ManifoldEvent.CREATE_NEW_DISTANCE, distanceObject));
        //Add 3D line to scene connecting the two points
        updateDistanceTrajectory(null, distanceObject);
//        //Clear selected spheres to null state
//        selectedSphereA = null;
//        selectedSphereB = null;
//        if (null != selectedManifoldA) {
//            selectedManifoldA.toggleSelection(false);
//            selectedManifoldA = null;
//        }
//        if (null != selectedManifoldB) {
//            selectedManifoldB.toggleSelection(false);
//            selectedManifoldB = null;
//        }
    }

    //Add 3D line to scene connecting the two points
    private Sphere updateDistanceTrajectory(Trajectory3D distanceTraj3D, Distance distance) {
//        if (null != distanceTraj3D) {
//            connectorsGroup.getChildren().remove(distanceTraj3D);
//            distanceTotrajectory3DMap.remove(distance);
//            //Clear this connector's label overlay
//            Shape3D shape3D = distanceToShape3DMap.get(distance);
//            extrasGroup.getChildren().remove(shape3D);
//            Label label = shape3DToLabel.remove(shape3D);
//            labelGroup.getChildren().remove(label);
//            distanceToShape3DMap.remove(distance);
//
//        }
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
//        distanceToShape3DMap.put(distance, midpointSphere);
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

//        distanceTotrajectory3DMap.put(distance, newDistanceTraj3D);
        connectorsGroup.getChildren().add(0, newDistanceTraj3D);

//        if (null != selectedManifoldA) {
//            Callout ddc = DistanceDataCallout.createByManifold3D(
//                midpointSphere, selectedManifoldA.getManifold(),
//                new Point3D(distance.getPoint1().getX(), distance.getPoint1().getY(),
//                    distance.getPoint1().getZ()), subScene);
//            radialOverlayPane.addCallout(ddc, midpointSphere);
//            ddc.play();
//            newDistanceTraj3D.addEventHandler(
//                MouseEvent.MOUSE_CLICKED, e -> {
//                    ddc.setVisible(true);
//                    ddc.play();
//                });
//        }
        updateFloatingNodes();
        return midpointSphere;
    }

    public void refresh() {
        updateView(true);
//        updateEllipsoids();
    }

    public void addManifold(Manifold manifold, Manifold3D manifold3D) {
//        System.out.println("adding manifold to Projections View.");
        manifold3D.setManifold(manifold);
//        manifold3D.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
//            getScene().getRoot().fireEvent(
//                new ManifoldEvent(ManifoldEvent.MANIFOLD_3D_SELECTED, manifold));
//            if ((e.getButton() == MouseButton.PRIMARY && e.isControlDown())
//                || (e.getButton() == MouseButton.PRIMARY && pointToPointDistanceMode)) {
//                processDistanceClick(manifold3D);
//            }
//
//        });
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

    public List<Manifold3D> getAllManifolds() {
        return manifolds;
    }

    public void clearAllManifolds() {
        manifoldGroup.getChildren().clear();
        manifolds.clear();
    }

    public Group getManifoldViews() {
        return manifoldGroup;
    }

}
