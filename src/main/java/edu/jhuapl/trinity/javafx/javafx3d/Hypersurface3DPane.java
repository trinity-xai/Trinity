/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.javafx3d;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.CoordinateSet;
import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.data.messages.bci.SemanticMap;
import edu.jhuapl.trinity.data.messages.bci.SemanticMapCollection;
import edu.jhuapl.trinity.data.messages.bci.SemanticReconstruction;
import edu.jhuapl.trinity.data.messages.bci.SemanticReconstructionMap;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.data.messages.xai.ShapleyCollection;
import edu.jhuapl.trinity.data.messages.xai.ShapleyVector;
import edu.jhuapl.trinity.javafx.components.callouts.Callout;
import edu.jhuapl.trinity.javafx.components.callouts.CalloutBuilder;
import edu.jhuapl.trinity.javafx.components.panes.SurfaceChartPane;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FactorAnalysisEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.events.ShadowEvent;
import edu.jhuapl.trinity.javafx.events.TimelineEvent;
import edu.jhuapl.trinity.javafx.javafx3d.animated.TessellationTube;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.AffinityClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.DBSCANClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.ExMaxClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.HDDBSCANClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.KMeansClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.KMediodsClusterTask;
import edu.jhuapl.trinity.javafx.renderers.FeatureVectorRenderer;
import edu.jhuapl.trinity.javafx.renderers.SemanticMapRenderer;
import edu.jhuapl.trinity.javafx.renderers.ShapleyVectorRenderer;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import edu.jhuapl.trinity.utils.metric.Metric;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */

public class Hypersurface3DPane extends StackPane
    implements SemanticMapRenderer, FeatureVectorRenderer, ShapleyVectorRenderer {
    private static final Logger LOG = LoggerFactory.getLogger(Hypersurface3DPane.class);
    public static double DEFAULT_INTRO_DISTANCE = -30000.0;
    public static double DEFAULT_ZOOM_TIME_MS = 500.0;
    public static double CHIP_FIT_WIDTH = 200;
    public static int DEFAULT_XWIDTH = 200;
    public static int DEFAULT_ZWIDTH = 200;
    public static int DEFAULT_SURFSCALE = 5;
    public static int DEFAULT_YSCALE = 5;

    public PerspectiveCamera camera;
    public CameraTransformer cameraTransform = new CameraTransformer();
    public XFormGroup dataXForm = new XFormGroup();

    private double cameraDistance = -1000;
    private final double sceneWidth = 4000;
    private final double sceneHeight = 4000;
    private final double planeSize = sceneWidth / 2.0;

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    public Group sceneRoot = new Group();
    public Group extrasGroup = new Group();
    public Group debugGroup = new Group();
    public Group ellipsoidGroup = new Group();
    public SubScene subScene;

    public long hypersurfaceRefreshRate = 500; //milliseconds
    public int queueLimit = 20000;

    //feature vector indices for 3D coordinates
    private int xFactorIndex = 0;
    private int yFactorIndex = 1;
    private int zFactorIndex = 2;
    private int factorMaxIndex = 512;

    public Color sceneColor = Color.BLACK;
    boolean isDirty = false;
    boolean computeRandos = false;
    boolean animated = false;
    boolean heightChanged = false;
    public boolean surfaceRender = true;

    enum COLORATION {COLOR_BY_IMAGE, COLOR_BY_FEATURE, COLOR_BY_SHAPLEY}

    COLORATION colorationMethod = COLORATION.COLOR_BY_FEATURE;
    boolean hoverInteractionsEnabled = false;
    boolean surfaceChartsEnabled = false;
    boolean crosshairsEnabled = false;

    //Shapley value support
    private Image lastImage = null;
    private String lastImageSource = null;
    public List<ShapleyVector> shapleyVectors = new ArrayList<>();

    WritableImage diffusePaintImage;
    PhongMaterial paintPhong;
    TriangleMesh paintTriangleMesh;
    MeshView paintMeshView;

    //allows 2D labels to track their 3D counterparts
    HashMap<Shape3D, Node> shape3DToLabel = new HashMap<>();
    public List<FeatureVector> featureVectors = new ArrayList<>();
    public List<List<Double>> dataGrid = new ArrayList<>();

    private Random rando = new Random();
    public HyperSurfacePlotMesh surfPlot;

    public int xWidth = DEFAULT_XWIDTH;
    public int zWidth = DEFAULT_ZWIDTH;
    public float yScale = DEFAULT_YSCALE;
    public float surfScale = DEFAULT_SURFSCALE;

    int TOTAL_COLORS = 1530; //colors used by map function
    Function<Point3D, Number> colorByHeight = p -> {
        return p.y; //Color mapping function
    };
    Function<Point3D, Number> colorByShapley = p -> {
        return p.f;
    };

    Function<Vert3D, Number> vert3DLookup = p -> {
        return vertToHeight(p);
    };
    // initial rotation
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotateZ = new Rotate(0, Rotate.Z_AXIS);

    private Skybox skybox;
    private Group nodeGroup = new Group();
    private Group labelGroup = new Group();

    BorderPane bp;
    Box glowLineBox;
    Cylinder eastPole, westPole;
    Sphere eastKnob, westKnob;
    Label eastLabel, westLabel;
    int anchorIndex = 0;
    Callout anchorCallout;

    //For each label you'll need some Shape3D to derive a point3d from.
    //For this we will use simple spheres.  These can be optionally invisible.
    private Sphere xSphere = new Sphere(10);
    private Sphere ySphere = new Sphere(10);
    private Sphere zSphere = new Sphere(10);
    Sphere highlightedPoint = new Sphere(2, 32);
    private Label xLabel = new Label("Features (ordered)");
    private Label yLabel = new Label("Magnitude");
    private Label zLabel = new Label("Time (Samples)");
    Text hoverText = new Text("Coordinates: ");

    public List<String> featureLabels = new ArrayList<>();
    public Spinner xWidthSpinner, zWidthSpinner;
    public Scene scene;
    HashMap<Shape3D, Callout> shape3DToCalloutMap;
    public String imageryBasePath = "imagery/";
    SurfaceChartPane surfaceChartPane;
    public AmbientLight ambientLight;
    public PointLight pointLight;

    public Hypersurface3DPane(Scene scene) {
        this.scene = scene;
        shape3DToCalloutMap = new HashMap<>();
        ambientLight = new AmbientLight(Color.WHITE);

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
        xSphere.setTranslateX(planeSize / 2.0);
        xSphere.setMaterial(new PhongMaterial(Color.RED));
        ySphere.setTranslateY(-planeSize / 2.0);
        ySphere.setMaterial(new PhongMaterial(Color.GREEN));
        zSphere.setTranslateZ(planeSize / 2.0);
        zSphere.setMaterial(new PhongMaterial(Color.BLUE));
        highlightedPoint.setMaterial(new PhongMaterial(Color.ALICEBLUE));
        highlightedPoint.setDrawMode(DrawMode.FILL);
        highlightedPoint.setMouseTransparent(true);

        //customize the labels to match
        Font font = new Font("Consolas", 20);
        xLabel.setTextFill(Color.YELLOW);
        xLabel.setFont(font);
        xLabel.setMouseTransparent(true);
        yLabel.setTextFill(Color.SKYBLUE);
        yLabel.setFont(font);
        yLabel.setMouseTransparent(true);
        zLabel.setTextFill(Color.LIGHTGREEN);
        zLabel.setFont(font);
        zLabel.setMouseTransparent(true);

        hoverText.setStroke(Color.ALICEBLUE);
        hoverText.setStrokeWidth(2);
        hoverText.setFill(Color.CYAN);
        hoverText.setFont(new Font("Consolas", 30));
        hoverText.setMouseTransparent(true);

        //add our labels to the group that will be added to the StackPane
        labelGroup.getChildren().addAll(xLabel, yLabel, zLabel, hoverText);
        labelGroup.setManaged(false);
        //Add to hashmap so updateLabels() can manage the label position
        shape3DToLabel.put(xSphere, xLabel);
        shape3DToLabel.put(ySphere, yLabel);
        shape3DToLabel.put(zSphere, zLabel);
        shape3DToLabel.put(highlightedPoint, hoverText);
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
        extrasGroup.setVisible(false);
        labelGroup.setVisible(false);
        //Add 3D subscene stuff to 3D scene root object
        sceneRoot.getChildren().addAll(cameraTransform, highlightedPoint,
            nodeGroup, extrasGroup, debugGroup, dataXForm);

        subScene.setCamera(camera);
        //add a Point Light for better viewing of the grid coordinate system
        pointLight = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(pointLight);
        pointLight.setTranslateX(camera.getTranslateX());
        pointLight.setTranslateY(camera.getTranslateY());
        pointLight.setTranslateZ(camera.getTranslateZ() + 500.0);

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
                        //updatePNodeIndices(xFactorIndex, yFactorIndex, zFactorIndex);
                        redraw = true;
                    } catch (Exception ex) {
                        scene.getRoot().fireEvent(
                            new CommandTerminalEvent("Feature Indexing Error: ("
                                + xFactorIndex + ", " + yFactorIndex + ", " + zFactorIndex + ")",
                                new Font("Consolas", 20), Color.RED));
                    }
                    if (redraw) {
                        updateView(false);
                        notifyIndexChange();
                    }
                    updateLabels();
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
                        //updatePNodeIndices(xFactorIndex, yFactorIndex, zFactorIndex);
                        redraw = true;
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        scene.getRoot().fireEvent(
                            new CommandTerminalEvent("Feature Indexing Error: ("
                                + xFactorIndex + ", " + yFactorIndex + ", " + zFactorIndex + ")",
                                new Font("Consolas", 20), Color.RED));
                    }
                    if (redraw) {
                        updateView(false);
                        notifyIndexChange();
                    }
                    updateLabels();
                } else {
                    scene.getRoot().fireEvent(
                        new CommandTerminalEvent("Feature Index Max Reached: ("
                            + featureSize + ")", new Font("Consolas", 20), Color.YELLOW));
                }
            }
            if (keycode == KeyCode.SLASH && event.isControlDown()) {
                debugGroup.setVisible(!debugGroup.isVisible());
            }
            if (keycode == KeyCode.Y) {
                surfPlot.scaleHeight(1.1f);
            }
            if (keycode == KeyCode.H) {
                surfPlot.scaleHeight(0.9f);
            }

            if (keycode == KeyCode.I) {
                double tz = 5;
                if (event.isShiftDown())
                    tz = 50;
                glowLineBox.setTranslateZ(glowLineBox.getTranslateZ() + tz);
            }
            if (keycode == KeyCode.K) {
                double tz = 5;
                if (event.isShiftDown())
                    tz = 50;
                glowLineBox.setTranslateZ(glowLineBox.getTranslateZ() - tz);
            }

            updateLabels();
            //update surface callouts
            updateCalloutHeadPoints(subScene);
        });

        subScene.setOnMousePressed((MouseEvent me) -> {
            if (me.isSynthesized())
                LOG.info("isSynthesized");
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
            //update surface callouts
            updateCalloutHeadPoints(subScene);
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
            //update surface callouts
            updateCalloutHeadPoints(subScene);
        });

        //Start Tracking mouse movements only when a button is pressed
        subScene.setOnMouseDragged((MouseEvent me) -> mouseDragCamera(me));
        //Premake the SurfaceChartPane
        Pane pathPane = App.getAppPathPaneStack();
        surfaceChartPane = new SurfaceChartPane(scene, pathPane);
        bp = new BorderPane(subScene);
        getChildren().clear();
        getChildren().addAll(bp, labelGroup);

        MenuItem copyAsImageItem = new MenuItem("Copy Scene to Clipboard");
        copyAsImageItem.setOnAction((ActionEvent e) -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putImage(this.snapshot(new SnapshotParameters(), null));
            clipboard.setContent(content);
        });
        MenuItem saveSnapshotItem = new MenuItem("Save Scene as Image");
        saveSnapshotItem.setOnAction((ActionEvent e) -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save scene as...");
            fileChooser.setInitialFileName("trinity_hypersurface.png");
            fileChooser.setInitialDirectory(Paths.get(".").toFile());
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                WritableImage image = this.snapshot(new SnapshotParameters(), null);
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                } catch (IOException ioe) {
                    // TODO: handle exception here
                }
            }
        });
        MenuItem unrollHyperspaceItem = new MenuItem("Unroll Hyperspace Data");
        unrollHyperspaceItem.setOnAction(e -> unrollHyperspace());

        MenuItem vectorDistanceItem = new MenuItem("Show Vector Distances");
        vectorDistanceItem.setOnAction(e -> computeVectorDistances());

        MenuItem collectionDifferenceItem = new MenuItem("Compare Feature Collection");
        collectionDifferenceItem.setOnAction(e -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load FeatureCollection to Compare...");
            fileChooser.setInitialDirectory(Paths.get(".").toFile());
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                FeatureCollectionFile fcf;
                try {
                    fcf = new FeatureCollectionFile(file.getAbsolutePath(), true);
                    computeSurfaceDifference(fcf.featureCollection);
                } catch (IOException ex) {
                    LOG.error(ex.getMessage());
                }
            }
        });

        CheckMenuItem enableHoverItem = new CheckMenuItem("Hover Interactions");
        enableHoverItem.setOnAction(e -> {
            hoverInteractionsEnabled = enableHoverItem.isSelected();
            if (hoverInteractionsEnabled) {
                //TODO Make coordinate labels visible
            }
        });

        CheckMenuItem surfaceChartsItem = new CheckMenuItem("Surface Charts");
        surfaceChartsItem.setOnAction(e -> {
            surfaceChartsEnabled = surfaceChartsItem.isSelected();
            if (surfaceChartsEnabled) {
                Pane pp = App.getAppPathPaneStack();
                if (null == surfaceChartPane) {
                    surfaceChartPane = new SurfaceChartPane(scene, pp);
                    surfaceChartPane.visibleProperty().bind(this.visibleProperty());
                }
                if (!pp.getChildren().contains(surfaceChartPane)) {
                    pp.getChildren().add(surfaceChartPane);
                    //slideInPane(surfaceChartPane);
                    surfaceChartPane.slideInPane();
                } else {
                    surfaceChartPane.show();
                }
            }
        });

        MenuItem updateAllItem = new MenuItem("Update Render");
        updateAllItem.setOnAction(e -> updateAll());
        MenuItem clearDataItem = new MenuItem("Clear Data");
        clearDataItem.setOnAction(e -> {
            clearAll();
            xWidth = DEFAULT_XWIDTH;
            zWidth = DEFAULT_ZWIDTH;
            xWidthSpinner.getValueFactory().setValue(xWidth);
            zWidthSpinner.getValueFactory().setValue(zWidth);
            generateRandos(xWidth, zWidth, yScale);
            updateTheMesh();
            updateView(true);
        });

        CheckMenuItem showDataMarkersItem = new CheckMenuItem("Show Data Markers");
        showDataMarkersItem.setOnAction(e -> {
            extrasGroup.setVisible(showDataMarkersItem.isSelected());
            labelGroup.setVisible(showDataMarkersItem.isSelected());
            if (null != anchorCallout)
                anchorCallout.setVisible(showDataMarkersItem.isSelected());
        });

        CheckMenuItem enableCrosshairsItem = new CheckMenuItem("Enable Crosshairs");
        enableCrosshairsItem.setOnAction(e -> {
            crosshairsEnabled = enableCrosshairsItem.isSelected();
        });

        MenuItem resetViewItem = new MenuItem("Reset View");
        resetViewItem.setOnAction(e -> resetView(1000, false));
        ContextMenu cm = new ContextMenu(copyAsImageItem, saveSnapshotItem,
            unrollHyperspaceItem, collectionDifferenceItem, vectorDistanceItem,
            enableHoverItem, surfaceChartsItem, showDataMarkersItem, enableCrosshairsItem,
            updateAllItem, clearDataItem, resetViewItem);
        cm.setAutoFix(true);
        cm.setAutoHide(true);
        cm.setHideOnEscape(true);
        cm.setOpacity(0.85);

        subScene.setOnMouseClicked((MouseEvent e) -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (!cm.isShowing())
                    cm.show(this.getParent(), e.getScreenX(), e.getScreenY());
                else
                    cm.hide();
                e.consume();
            }
        });
        //load empty surface
        loadSurf3D();
        this.scene.addEventHandler(HyperspaceEvent.HYPERSPACE_BACKGROUND_COLOR, e -> {
            Color color = (Color) e.object;
            subScene.setFill(color);
        });
        this.scene.addEventHandler(HyperspaceEvent.ENABLE_HYPERSPACE_SKYBOX, e -> {
            skybox.setVisible((Boolean) e.object);
        });
        this.scene.addEventHandler(ImageEvent.NEW_TEXTURE_SURFACE, e -> {
            Image image = (Image) e.object;
            int x1 = 0;
            int y1 = 0;
            int x2 = Double.valueOf(image.getWidth()).intValue();
            int y2 = Double.valueOf(image.getHeight()).intValue();
            if (x2 > 512 || y2 > 512) {
                boolean split = false;
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Image has " + x2 + " rows and " + y2 + " columns.\n"
                        + "Split the image before tessellation?",
                    ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
                alert.setTitle("Image Tessellation Import");
                alert.setHeaderText("Image has " + x2 + " rows  and " + y2 + " columns.\n");
                alert.setContentText("Select subregion from image before tessellation?");
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
                split = optBT.get().equals(ButtonType.YES);
                if (split) {
                    //@TODO SMP popup helper tool to select region
                    scene.getRoot().fireEvent(new ApplicationEvent(
                        ApplicationEvent.SHOW_PIXEL_SELECTION, image));
                    return;
                }
            }
            tessellateImage(image, x1, y1, x2, y2);
            lastImage = image;
            lastImageSource = image.getUrl();
        });
        this.scene.addEventHandler(HyperspaceEvent.FACTOR_COORDINATES_GUI, e -> {
            CoordinateSet coords = (CoordinateSet) e.object;
            xFactorIndex = coords.coordinateIndices.get(0);
            yFactorIndex = coords.coordinateIndices.get(1);
            zFactorIndex = coords.coordinateIndices.get(2);
            updateLabels();
            updateView(true);
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
                    notifyIndexChange();
                }
            } else
                factorMaxIndex = newFactorMaxIndex;
        });

        scene.addEventHandler(HyperspaceEvent.NODE_QUEUELIMIT_GUI, e -> queueLimit = (int) e.object);
        scene.addEventHandler(HyperspaceEvent.REFRESH_RATE_GUI, e -> hypersurfaceRefreshRate = (long) e.object);

        scene.addEventHandler(ShadowEvent.SHOW_AXES_LABELS, e -> {
            nodeGroup.setVisible((boolean) e.object);
            labelGroup.setVisible((boolean) e.object);
        });
        scene.addEventHandler(ApplicationEvent.SET_IMAGERY_BASEPATH, e ->
            imageryBasePath = (String) e.object);
        Platform.runLater(() -> {
            updateLabels();
            updateView(true);
            updateTheMesh();
        });
        AnimationTimer surfUpdateAnimationTimer = new AnimationTimer() {
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_MILLI = 1_000_000;

            @Override
            public void handle(long now) {
                sleepNs = hypersurfaceRefreshRate * NANOS_IN_MILLI;
                if ((now - prevTime) < sleepNs) return;
                prevTime = now;
                long startTime;
                if (computeRandos) {
                    generateRandos(xWidth, zWidth, yScale);
                }
                if (animated || isDirty) {
                    startTime = System.nanoTime();
                    updateTheMesh();
                    LOG.info("updateTheMesh(): {}", Utils.totalTimeString(startTime));
                }
            }
        };
        surfUpdateAnimationTimer.start();
    }

    public void computeVectorDistances() {
        System.out.println("Computing Vector Distances... ");
        long startTime = System.nanoTime();
        Metric metric = Metric.getMetric("cosine");
        List<List<Double>> distancesGrid = new ArrayList<>();

        //get all the values in this row
        dataGrid.stream().forEach(row -> {
            double[] rowVector = row.stream().mapToDouble(Double::doubleValue).toArray();
            List<Double> distanceVector = new ArrayList<>();
            for (int i = 0; i < dataGrid.size(); i++) {
                double[] xVector = dataGrid.get(i).stream()
                    .mapToDouble(Double::doubleValue).toArray();
                double currentDistance = metric.distance(xVector, rowVector);
                distanceVector.add(currentDistance);
            }
            distancesGrid.add(distanceVector);
        });
        Utils.printTotalTime(startTime);

        dataGrid.clear();
        dataGrid.addAll(distancesGrid);
        xWidth = dataGrid.get(0).size();
        zWidth = dataGrid.size();
        xWidthSpinner.getValueFactory().setValue(xWidth);
        zWidthSpinner.getValueFactory().setValue(zWidth);
        updateTheMesh();
        updateView(true);
    }

    public void computeSurfaceDifference(FeatureCollection collection) {
        double[][] newRayRay = collection.convertFeaturesToArray();
        System.out.println("Computing Surface Differences... ");
        long startTime = System.nanoTime();
        List<List<Double>> differencesGrid = new ArrayList<>();

        for (int rowIndex = 0; rowIndex < dataGrid.size(); rowIndex++) {
            List<Double> differenceVector = new ArrayList<>();
            List<Double> currentRow = dataGrid.get(rowIndex);
            int width = currentRow.size();
            for (int colIndex = 0; colIndex < width; colIndex++) {
                if (rowIndex < newRayRay.length && colIndex < newRayRay[rowIndex].length) {
                    differenceVector.add(
                        currentRow.get(colIndex) - newRayRay[rowIndex][colIndex]);
                } else {
                    differenceVector.add(0.0);
                }
            }
            differencesGrid.add(differenceVector);
        }
        Utils.printTotalTime(startTime);

        dataGrid.clear();
        dataGrid.addAll(differencesGrid);
        xWidth = dataGrid.get(0).size();
        zWidth = dataGrid.size();
        xWidthSpinner.getValueFactory().setValue(xWidth);
        zWidthSpinner.getValueFactory().setValue(zWidth);
        updateTheMesh();
        updateView(true);
    }

    public void unrollHyperspace() {
        getScene().getRoot().fireEvent(
            new CommandTerminalEvent("Requesting Hyperspace Vectors...",
                new Font("Consolas", 20), Color.GREEN));
        getScene().getRoot().fireEvent(
            new FeatureVectorEvent(FeatureVectorEvent.REQUEST_FEATURE_COLLECTION)
        );
    }

    public void updateCalloutHeadPoint(Shape3D node, Callout callout, SubScene subScene) {
        Point2D p2d = JavaFX3DUtils.getTransformedP2D(node, subScene, callout.head.getRadius() + 5);
        callout.updateHeadPoint(p2d.getX(), p2d.getY());
    }

    public void updateCalloutHeadPoints(SubScene subScene) {
        shape3DToCalloutMap.forEach((node, callout) -> {
            updateCalloutHeadPoint(node, callout, subScene);
        });
    }

    public Callout createCallout(Shape3D shape3D, FeatureVector featureVector, SubScene subScene) {
        ImageView iv = loadImageView(featureVector, featureVector.isBBoxValid());
        iv.setPreserveRatio(true);
        iv.setFitWidth(CHIP_FIT_WIDTH);
        iv.setFitHeight(CHIP_FIT_WIDTH);

        TitledPane imageTP = new TitledPane();
        imageTP.setContent(iv);
        imageTP.setText("Imagery");

        Point2D p2D = JavaFX3DUtils.getTransformedP2D(shape3D, subScene, Callout.DEFAULT_HEAD_RADIUS + 5);
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : featureVector.getMetaData().entrySet()) {
            sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        Text metaText = new Text(sb.toString());
        TitledPane metaTP = new TitledPane();
        metaTP.setContent(metaText);
        metaTP.setText("Metadata");

        Callout infoCallout = CalloutBuilder.create()
            .headPoint(p2D.getX(), p2D.getY())
            .leaderLineToPoint(p2D.getX() - 100, p2D.getY() - 150)
            .endLeaderLineRight()
            .mainTitle(featureVector.getLabel(), new VBox(3, imageTP, metaTP))
            .subTitle(featureVector.getEntityId())
            .pause(10)
            .build();
        infoCallout.setPickOnBounds(false);
        infoCallout.setManaged(false);
        addCallout(infoCallout, shape3D);
        infoCallout.play().setOnFinished(eh -> {
            if (null == featureVector.getImageURL() || featureVector.getImageURL().isBlank()) {
                imageTP.setExpanded(false);
            }
        });
        return infoCallout;
    }

    public void addCallout(Callout callout, Shape3D shape3D) {
        //calloutList.add(callout);
        callout.setManaged(false);
        getChildren().add(callout);
        //Anchor mapping for callout in 3D space
        shape3DToCalloutMap.put(shape3D, callout);
    }

    public void updateTheMesh() {
        surfPlot.setVisible(surfaceRender);
        sceneRoot.getChildren().removeIf(n -> n instanceof TessellationTube);
        if (surfaceRender) {
            surfPlot.updateMeshRaw(xWidth, zWidth, surfScale, yScale, surfScale);
        } else {
            sceneRoot.getChildren().removeIf(n -> n instanceof TessellationTube);
            TessellationTube tube = new TessellationTube(dataGrid, Color.WHITE, yScale * 10, surfScale, yScale);
            tube.setMouseTransparent(true);
            if (null != lastImage) {
                tube.meshView.setDrawMode(DrawMode.FILL);
                tube.colorByImage = colorationMethod == COLORATION.COLOR_BY_IMAGE;
                tube.updateMaterial(lastImage);
            }
            Platform.runLater(() -> {
                sceneRoot.getChildren().add(tube);
            });
        }
        Platform.runLater(() -> {
            updatePaintMesh();
        });
    }

    public void updatePaintMesh() {
        //in case the data grid dimensions have changed
        //make the painting image the same dimension as the data grid for easy math
        diffusePaintImage = new WritableImage(
            Double.valueOf(xWidth).intValue(),
            Double.valueOf(zWidth).intValue()
        );

        if (null == paintTriangleMesh) {
            paintTriangleMesh = new TriangleMesh();
            paintMeshView = new MeshView(paintTriangleMesh);
            paintMeshView.setMouseTransparent(true);
            paintMeshView.setMesh(paintTriangleMesh);
            paintMeshView.setCullFace(CullFace.NONE);
            paintPhong = new PhongMaterial(Color.WHITE,
                diffusePaintImage, null, null, null);
            paintPhong.setSpecularColor(Color.WHITE);
            paintPhong.setDiffuseColor(Color.WHITE);
            paintMeshView.setMaterial(paintPhong);
            sceneRoot.getChildren().add(paintMeshView);
            surfPlot.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                if (e.getClickCount() > 1 && e.isControlDown()) {
                    Color color = new Color(rando.nextDouble(), rando.nextDouble(), rando.nextDouble(), 1.0);
                    paintSingleColor(color);
                }
            });
        }

        TriangleMesh surfMesh = (TriangleMesh) surfPlot.getMesh();
        paintTriangleMesh.getPoints().setAll(surfMesh.getPoints());

        paintTriangleMesh.getFaces().clear();
        paintTriangleMesh.getTexCoords().clear();
        final int texCoordSize = 2;

        Float pSkip = 2.0f;
        int pskip = pSkip.intValue();
        int subDivX = (int) diffusePaintImage.getWidth() / pskip;
        int subDivZ = (int) diffusePaintImage.getHeight() / pskip;
        int numDivX = subDivX + 1;
        int numVerts = (subDivZ + 1) * numDivX;
        float currZ, currX;
        float texCoords[] = new float[numVerts * texCoordSize];
        int faceCount = subDivX * subDivZ * 2;
        final int faceSize = 6; //should always be 6 for a triangle mesh
        int faces[] = new int[faceCount * faceSize];
        int index, p00, p01, p10, p11, tc00, tc01, tc10, tc11;

        //Map the 2D data grid to UV coordinates and paint a single color to test
        for (int z = 0; z < subDivZ; z++) {
            currZ = (float) z / subDivZ;
            for (int x = 0; x < subDivX; x++) {
                currX = (float) x / subDivX;
                index = z * numDivX * texCoordSize + (x * texCoordSize);
                texCoords[index] = currX;
                texCoords[index + 1] = currZ;

                // Create faces
                p00 = z * numDivX + x;
                p01 = p00 + 1;
                p10 = p00 + numDivX;
                p11 = p10 + 1;
                tc00 = z * numDivX + x;
                tc01 = tc00 + 1;
                tc10 = tc00 + numDivX;
                tc11 = tc10 + 1;

                index = (z * subDivX * faceSize + (x * faceSize)) * 2;
                faces[index + 0] = p00;
                faces[index + 1] = tc00;
                faces[index + 2] = p10;
                faces[index + 3] = tc10;
                faces[index + 4] = p11;
                faces[index + 5] = tc11;

                index += faceSize;
                faces[index + 0] = p11;
                faces[index + 1] = tc11;
                faces[index + 2] = p01;
                faces[index + 3] = tc01;
                faces[index + 4] = p00;
                faces[index + 5] = tc00;
                diffusePaintImage.getPixelWriter().setColor(x, z, Color.TRANSPARENT);
            }
        }
        paintTriangleMesh.getTexCoords().setAll(texCoords);
        paintTriangleMesh.getFaces().setAll(faces);
        //force update of the material (is this actually necessary?)
        paintPhong.setDiffuseMap(diffusePaintImage);
        paintMeshView.setTranslateZ(-1); //slight offset "above" to avoid z fighting
        paintMeshView.setTranslateX(-(xWidth * surfScale) / 2.0);
        paintMeshView.setTranslateZ(-(zWidth * surfScale) / 2.0);
    }

    public void paintSingleColor(Color color) {
        //Map the 2D data grid to UV coordinates and paint a single color to test
        for (int z = 0; z < diffusePaintImage.getHeight(); z++) {
            for (int x = 0; x < diffusePaintImage.getWidth(); x++) {
                diffusePaintImage.getPixelWriter().setColor(x, z, color);
            }
        }
    }

    public void illuminateCrosshair(Point3D center) {
        if (null == diffusePaintImage)
            return;
        int x = (int) (center.getX() / surfScale);
        int z = (int) (center.getZ() / surfScale); //Image Y is projected into  Z

        PixelWriter pw = diffusePaintImage.getPixelWriter();
        for (int i = 0; i < diffusePaintImage.getWidth(); i++)
            pw.setColor(i, z, Color.WHITE);
        for (int i = 0; i < diffusePaintImage.getHeight(); i++)
            pw.setColor(x, i, Color.WHITE);
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
        ambientLight.getScope().addAll(skybox);
        skybox.setVisible(false);
    }

    private void notifyIndexChange() {
        getScene().getRoot().fireEvent(
            new CommandTerminalEvent("X,Y,Z Indices = ("
                + xFactorIndex + ", " + yFactorIndex + ", " + zFactorIndex + ")",
                new Font("Consolas", 20), Color.GREEN));
    }

    public void resetView(double milliseconds, boolean rightNow) {
        if (!rightNow) {
            Timeline timeline = JavaFX3DUtils.transitionCameraTo(milliseconds, camera, cameraTransform,
                0, 0, cameraDistance, -10.0, -45.0, 0.0);
            timeline.play();
        } else {
            dataXForm.reset();
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
        updateLabels();
        //update surface callouts
        updateCalloutHeadPoints(subScene);
    }

    private void updateLabels() {
        shape3DToLabel.forEach((shape3D, node) -> {
            Point2D p2Ditty = JavaFX3DUtils.getTransformedP2D(shape3D, subScene, 5);
            //@DEBUG SMP  useful debugging print
            //System.out.println("subSceneToScene Coordinates: " + coordinates.toString());
            double x = p2Ditty.getX();
            double y = p2Ditty.getY() - 25; //simple offset to keep labels above nodes
            //update the local transform of the label.
            node.getTransforms().setAll(new Translate(x, y));
        });
    }

    private WritableImage loadImage(String fileSource) throws IOException {
        WritableImage image = null;
        try {
            image = ResourceUtils.loadImageFile(imageryBasePath + fileSource);
        } catch (IOException ex) {
            image = ResourceUtils.loadIconAsWritableImage("noimage");
        }
        return image;
    }

    private ImageView loadImageView(FeatureVector featureVector, boolean bboxOnly) {
        ImageView iv = null;
        try {
            if (bboxOnly) {
                WritableImage image = ResourceUtils.loadImageFileSubset(imageryBasePath + featureVector.getImageURL(),
                    featureVector.getBbox().get(0).intValue(),
                    featureVector.getBbox().get(1).intValue(),
                    featureVector.getBbox().get(2).intValue(),
                    featureVector.getBbox().get(3).intValue()
                );
                iv = new ImageView(image);
            } else {
                iv = new ImageView(ResourceUtils.loadImageFile(imageryBasePath + featureVector.getImageURL()));
            }
        } catch (IOException ex) {
            iv = new ImageView(ResourceUtils.loadIconFile("noimage"));
        }
        return iv;
    }

    public void updateView(boolean forcePNodeUpdate) {
        if (null != surfPlot) {
            Platform.runLater(() -> {
                if (heightChanged) { //if it hasn't changed, don't call expensive height change
                    heightChanged = false;
                }
                //@DEBUG SMP Rendering timing print
                //System.out.println("UpdateView setScatterDataAndEndPoints time: "
                //    + Utils.totalTimeString(startTime2));
                //Since we changed the mesh unfortunately we have to reset the color mode
                //otherwise the triangles won't have color.
                //5ms for 20k points
                isDirty = false;
            });
        }
    }

    private void generateRandos(int xWidth, int zWidth, float yScale) {
        if (null == dataGrid) {
            dataGrid = new ArrayList<>(zWidth);
        } else
            dataGrid.clear();
        List<Double> xList;
        for (int z = 0; z < zWidth; z++) {
            xList = new ArrayList<>(xWidth);
            for (int x = 0; x < xWidth; x++) {
                xList.add(rando.nextDouble() * yScale);
            }
            dataGrid.add(xList);
        }
    }

    private Number lookupShapley(Point3D p) {
        if (null != shapleyVectors && null != dataGrid) {
//@SMP This approach won't work because it is dependent on translation and scaling
//            //number of rows multiplied by width of rows (stride length)
//            //plus column index
//            return shapleyVectors.get(
//                Float.valueOf(
//                    ((p.z/surfScale * dataGrid.size()) * dataGrid.get(0).size()) + p.x/surfScale
//                ).intValue()).getData().get(0);
        }
        return 0.0;
    }

    private Number vertToHeight(Vert3D p) {
        if (null != dataGrid) {
            if (surfaceRender)
                return lookupPoint(p);
            else
                return findBlerpHeight(p);
        } else
            return 0.0;
    }

    private Number lookupPoint(Vert3D p) {
        //hacky bounds check
        if (p.yIndex >= dataGrid.size()
            || p.xIndex >= dataGrid.get(0).size())
            return 0.0;
        return dataGrid.get(p.yIndex).get(p.xIndex);
    }

    private Number findBlerpHeight(Vert3D p) {
        int x1Index = p.xIndex <= 0 ? 0 : p.xIndex - 1;
        if (x1Index >= dataGrid.get(0).size() - 1)
            x1Index = dataGrid.get(0).size() - 1;

        int x2Index = p.xIndex >= dataGrid.get(0).size() - 1
            ? dataGrid.get(0).size() - 1 : p.xIndex + 1;

        int y1Index = p.yIndex <= 0 ? 0 : p.yIndex - 1;
        if (y1Index >= dataGrid.size() - 1)
            y1Index = dataGrid.size() - 1;
        int y2Index = p.yIndex >= dataGrid.size() - 1
            ? dataGrid.size() - 1 : p.yIndex + 1;
        //System.out.println("x1,x2,y1,y2:" + x1Index + ", " + x2Index + ", " + y1Index + ", " + y2Index);

        double c11 = dataGrid.get(y1Index).get(x1Index) * yScale;
        double c21 = dataGrid.get(y1Index).get(x2Index) * yScale;
        double c12 = dataGrid.get(y2Index).get(x1Index) * yScale;
        double c22 = dataGrid.get(y2Index).get(x2Index) * yScale;
        //System.out.println("x1,x2,y1,y2:" + x1Index + ", " + x2Index + ", " + y1Index + ", " + y2Index);

        return quickBlerp(c11, c21, c12, c22, p.getX(), p.getY());
    }

    private Number quickBlerp(double f1, double f2, double f3, double f4, double x, double y) {
        double xratio = x - Math.floor(x);
        double yratio = y - Math.floor(y);
        double f12 = f1 + (f2 - f1) * xratio;
        double f34 = f3 + (f4 - f3) * xratio;
        return f12 + (f34 - f12) * yratio;
    }

    int vert;
    Point3D vertP3D;

    private void loadSurf3D() {
        LOG.info("Rendering Hypersurface Mesh...");
        generateRandos(xWidth, zWidth, yScale);
        surfPlot = new HyperSurfacePlotMesh(xWidth, zWidth,
            1, 1, yScale, surfScale, vert3DLookup);
        surfPlot.setTextureModeVertices3D(TOTAL_COLORS, colorByHeight, 0.0, 360.0);
        surfPlot.setDrawMode(DrawMode.LINE);
        sceneRoot.getChildren().add(surfPlot);
        surfPlot.setCullFace(CullFace.NONE);
        surfPlot.setTranslateX(-(xWidth * surfScale) / 2.0);
        surfPlot.setTranslateZ(-(zWidth * surfScale) / 2.0);
        surfPlot.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            if (hoverInteractionsEnabled) {
                javafx.geometry.Point3D p3D = e.getPickResult().getIntersectedPoint();
                vertP3D = Point3D.convertFromJavaFXPoint3D(p3D);
                highlightedPoint.setTranslateX(vertP3D.x - (xWidth * surfScale) / 2.0);
                highlightedPoint.setTranslateY(vertP3D.y);
                highlightedPoint.setTranslateZ(vertP3D.z - (zWidth * surfScale) / 2.0);
                updateCalloutHeadPoints(subScene);
                updateLabels();
                int row = Float.valueOf(vertP3D.getZ() / surfScale).intValue();
                int column = Float.valueOf(vertP3D.getX() / surfScale).intValue();
                if (null != anchorCallout) {
                    if (row < featureVectors.size())
                        updateCalloutByFeatureVector(anchorCallout, featureVectors.get(row));
                    setSpheroidAnchor(false, row);
                }

                if (crosshairsEnabled) {
                    paintSingleColor(Color.TRANSPARENT);
                    illuminateCrosshair(vertP3D);
                }

                if (surfaceChartsEnabled) {
                    //get all the values in this row
                    List<Double> xlist = dataGrid.get(row);
                    Double[] xRay = xlist.toArray(Double[]::new);
                    //get the column values in time.
                    Double[] zRay = new Double[dataGrid.size()];
                    for (int i = 0; i < dataGrid.size(); i++) {
                        zRay[i] = dataGrid.get(i).get(column);
                    }
                    String text = "Coordinates: " + column + ", " + row + System.lineSeparator();
                    text = text.concat("Value: ").concat(String.valueOf(dataGrid.get(row).get(column))).concat(System.lineSeparator());
                    double maxX = xlist.stream().max(Double::compare).get();
                    text = text.concat("Max X: ").concat(String.valueOf(maxX)).concat(System.lineSeparator());
                    double minX = xlist.stream().min(Double::compare).get();
                    text = text.concat("Min X: ").concat(String.valueOf(minX)).concat(System.lineSeparator());
                    double maxZ = Arrays.stream(zRay).max(Double::compare).get();
                    text = text.concat("Max Z: ").concat(String.valueOf(maxZ)).concat(System.lineSeparator());
                    double minZ = Arrays.stream(zRay).min(Double::compare).get();
                    text = text.concat("Min Z: ").concat(String.valueOf(minZ)).concat(System.lineSeparator());

                    hoverText.setText(text);
                    hoverText.setStrokeWidth(1);
                    hoverText.setLayoutX(50);
                    hoverText.setLayoutY(50);

                    scene.getRoot().fireEvent(new FactorAnalysisEvent(
                        FactorAnalysisEvent.SURFACE_XFACTOR_VECTOR, xRay));

                    scene.getRoot().fireEvent(new FactorAnalysisEvent(
                        FactorAnalysisEvent.SURFACE_ZFACTOR_VECTOR, zRay));
                }

                e.consume();
            }
        });

        Glow glow = new Glow(0.8);
        double poleHeight = 60;
        double radius = 3;
        glowLineBox = new Box(xWidth * surfScale, poleHeight, radius);
        glowLineBox.setMaterial(new PhongMaterial(Color.ALICEBLUE.deriveColor(1, 1, 1, 0.2)));
        glowLineBox.setDrawMode(DrawMode.FILL);
        glowLineBox.setEffect(glow);
        glowLineBox.setTranslateZ(-(zWidth * surfScale) / 2.0);
        eastPole = new Cylinder(radius * 2, poleHeight * 1.2);
        westPole = new Cylinder(radius * 2, poleHeight * 1.2);
        eastKnob = new Sphere(radius * 3);
        westKnob = new Sphere(radius * 3);
        PhongMaterial eastPoleMaterial = new PhongMaterial(Color.STEELBLUE);
        PhongMaterial westPoleMaterial = new PhongMaterial(Color.STEELBLUE);
        PhongMaterial knobMaterial = new PhongMaterial(Color.ALICEBLUE);
        eastPole.setMaterial(eastPoleMaterial);
        westPole.setMaterial(westPoleMaterial);
        eastKnob.setMaterial(knobMaterial);
        westKnob.setMaterial(knobMaterial);
        eastPole.setTranslateX((xWidth * surfScale) / 2.0);
        westPole.setTranslateX(-(xWidth * surfScale) / 2.0);
        eastKnob.setTranslateX((xWidth * surfScale) / 2.0);
        westKnob.setTranslateX(-(xWidth * surfScale) / 2.0);
        eastKnob.setTranslateY(-(poleHeight * 1.2) / 2.0);
        westKnob.setTranslateY(-(poleHeight * 1.2) / 2.0);
        eastPole.translateZProperty().bind(glowLineBox.translateZProperty());
        westPole.translateZProperty().bind(glowLineBox.translateZProperty());
        eastKnob.translateZProperty().bind(glowLineBox.translateZProperty());
        westKnob.translateZProperty().bind(glowLineBox.translateZProperty());

        //customize the labels to match
        eastLabel = new Label("Data Index");
        eastLabel.setTextFill(Color.ALICEBLUE);
        eastLabel.setFont(new Font("calibri", 20));
        westLabel = new Label("Data Index");
        westLabel.setTextFill(Color.ALICEBLUE);
        westLabel.setFont(new Font("calibri", 20));
        //add our labels to the group that will be added to the StackPane
        labelGroup.getChildren().addAll(eastLabel, westLabel);
        //Add to hashmap so updateLabels() can manage the label position
        shape3DToLabel.put(eastKnob, eastLabel);
        shape3DToLabel.put(westKnob, westLabel);

        scene.addEventHandler(TimelineEvent.TIMELINE_SAMPLE_INDEX, e -> {
            anchorIndex = (int) e.object;
            if (anchorIndex < 0)
                anchorIndex = 0;
            else if (anchorIndex > dataGrid.size())
                anchorIndex = dataGrid.size();
            //move the glowLineBox based on the step index
            glowLineBox.setTranslateZ((anchorIndex * surfScale) - ((zWidth * surfScale) / 2.0));
            //@TODO SMP this is stubbed but we should update an anchored callout
            setSpheroidAnchor(true, anchorIndex);
            eastLabel.setText("Sample: " + anchorIndex + ", Neural Feature: " + xWidth);
            westLabel.setText("Sample: " + anchorIndex + ", Neural Feature: 0");
            updateLabels();
            updateCalloutHeadPoints(subScene);
        });
        scene.addEventHandler(FeatureVectorEvent.SELECT_FEATURE_VECTOR, e -> {
            if (null != anchorCallout) {
                FeatureVector fv = (FeatureVector) e.object;
                //try to update the callout anchored to the lead state
                updateCalloutByFeatureVector(anchorCallout, fv);
            }
        });

        extrasGroup.getChildren().addAll(eastPole, eastKnob, westPole, westKnob, glowLineBox);

        Spinner yScaleSpinner = new Spinner(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 500.0, yScale, 1.00));
        yScaleSpinner.setEditable(true);
        //whenever the spinner value is changed...
        yScaleSpinner.getValueFactory().valueProperty().addListener(e -> {
            yScale = ((Double) yScaleSpinner.getValue()).floatValue();
            surfPlot.setFunctionScale(yScale);
            updateTheMesh();
        });
        //whenever the spinner value is changed...
        yScaleSpinner.setOnKeyTyped(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                yScale = ((Double) yScaleSpinner.getValue()).floatValue();
                surfPlot.setFunctionScale(yScale);
                updateTheMesh();
            }
        });

        yScaleSpinner.setPrefWidth(125);
        Spinner surfScaleSpinner = new Spinner(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 100.0, surfScale, 1.0));
        surfScaleSpinner.setEditable(true);
        //whenever the spinner value is changed...
        surfScaleSpinner.valueProperty().addListener(e -> {
            surfScale = ((Double) surfScaleSpinner.getValue()).floatValue();
            surfPlot.setRangeX(xWidth * surfScale);
            surfPlot.setRangeY(zWidth * surfScale);
            updateTheMesh();
            surfPlot.setTranslateX(-(xWidth * surfScale) / 2.0);
            surfPlot.setTranslateZ(-(zWidth * surfScale) / 2.0);
        });
        surfScaleSpinner.setPrefWidth(125);
//        Spinner divisionsSpinner = new Spinner(
//            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 512, 64, 4));
//        divisionsSpinner.setEditable(true);
//        //whenever the spinner value is changed...
//        divisionsSpinner.valueProperty().addListener(e -> {
//            surfPlot.setDivisionsX((int) divisionsSpinner.getValue());
//            surfPlot.setDivisionsY((int) divisionsSpinner.getValue());
//            updateTheMesh();
//        });
//        divisionsSpinner.setPrefWidth(125);

        xWidthSpinner = new Spinner(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 4000, 200, 4));
        xWidthSpinner.setEditable(true);
        //whenever the spinner value is changed...
        xWidthSpinner.valueProperty().addListener(e -> {
            xWidth = ((int) xWidthSpinner.getValue());
            updateTheMesh();
            surfPlot.setTranslateX(-(xWidth * surfScale) / 2.0);
            surfPlot.setTranslateZ(-(zWidth * surfScale) / 2.0);
        });
        xWidthSpinner.setPrefWidth(125);
        zWidthSpinner = new Spinner(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 4000, 200, 10));
        zWidthSpinner.setEditable(true);
        //whenever the spinner value is changed...
        zWidthSpinner.valueProperty().addListener(e -> {
            zWidth = ((int) zWidthSpinner.getValue());
            updateTheMesh();
            surfPlot.setTranslateX(-(xWidth * surfScale) / 2.0);
            surfPlot.setTranslateZ(-(zWidth * surfScale) / 2.0);
        });
        zWidthSpinner.setPrefWidth(125);

        ToggleGroup colorationToggle = new ToggleGroup();
        RadioButton colorByImageRadioButton = new RadioButton("Color by Image");
        colorByImageRadioButton.setToggleGroup(colorationToggle);

        RadioButton colorByFeatureValueRadioButton = new RadioButton("Color by Feature Value");
        colorByFeatureValueRadioButton.setSelected(true);
        colorByFeatureValueRadioButton.setToggleGroup(colorationToggle);

        RadioButton colorByShapleyValueRadioButton = new RadioButton("Color by Shapley Value");
        colorByShapleyValueRadioButton.setSelected(false);
        colorByShapleyValueRadioButton.setToggleGroup(colorationToggle);

        colorationToggle.selectedToggleProperty().addListener(cl -> {
            if (colorByImageRadioButton.isSelected()) {
                colorationMethod = COLORATION.COLOR_BY_IMAGE;
                File imageFile = new File(imageryBasePath + lastImageSource);
                surfPlot.setTextureModeImage(imageFile.toURI().toString());
            } else if (colorByFeatureValueRadioButton.isSelected()) {
                colorationMethod = COLORATION.COLOR_BY_FEATURE;
                surfPlot.setTextureModeVertices3D(TOTAL_COLORS, colorByHeight, 0.0, 360.0);
            } else {
                colorationMethod = COLORATION.COLOR_BY_SHAPLEY;
                surfPlot.setTextureModeVertices3D(TOTAL_COLORS, colorByShapley, 0.0, 360.0);
            }
            updateTheMesh();
        });
        HBox colorationHBox = new HBox(10, colorByImageRadioButton,
            colorByFeatureValueRadioButton, colorByShapleyValueRadioButton);

        ToggleGroup meshTypeToggle = new ToggleGroup();
        RadioButton surfaceRadioButton = new RadioButton("Surface Projection");
        surfaceRadioButton.setSelected(true);
        surfaceRadioButton.setToggleGroup(meshTypeToggle);
        RadioButton cylinderRadioButton = new RadioButton("Cylindrical");
        cylinderRadioButton.setToggleGroup(meshTypeToggle);
        meshTypeToggle.selectedToggleProperty().addListener(cl -> {
            surfaceRender = surfaceRadioButton.isSelected();
            updateTheMesh();
        });
        HBox meshTypeHBox = new HBox(10, surfaceRadioButton, cylinderRadioButton);

        ToggleGroup drawModeToggle = new ToggleGroup();
        RadioButton drawModeLine = new RadioButton("Line");
        drawModeLine.setSelected(true);
        drawModeLine.setToggleGroup(drawModeToggle);
        RadioButton drawModeFill = new RadioButton("Fill");
        drawModeFill.setToggleGroup(drawModeToggle);
        drawModeToggle.selectedToggleProperty().addListener(cl -> {
            if (drawModeLine.isSelected()) {
                surfPlot.setDrawMode(DrawMode.LINE);
            } else {
                surfPlot.setDrawMode(DrawMode.FILL);
            }
        });
        HBox drawModeHBox = new HBox(10, drawModeLine, drawModeFill);

        ToggleGroup cullFaceToggle = new ToggleGroup();
        RadioButton cullFaceFront = new RadioButton("Front");
        cullFaceFront.setToggleGroup(cullFaceToggle);
        RadioButton cullFaceBack = new RadioButton("Back");
        cullFaceBack.setToggleGroup(cullFaceToggle);
        RadioButton cullFaceNone = new RadioButton("None");
        cullFaceNone.setSelected(true);
        cullFaceNone.setToggleGroup(cullFaceToggle);
        cullFaceToggle.selectedToggleProperty().addListener(cl -> {
            if (cullFaceFront.isSelected()) {
                surfPlot.setCullFace(CullFace.FRONT);
            } else if (cullFaceBack.isSelected()) {
                surfPlot.setCullFace(CullFace.BACK);
            } else {
                surfPlot.setCullFace(CullFace.NONE);
            }
        });
        HBox cullFaceHBox = new HBox(10, cullFaceFront, cullFaceBack, cullFaceNone);

        //add a Point Light for better viewing of the grid coordinate system
        pointLight.getScope().addAll(surfPlot);
        sceneRoot.getChildren().add(pointLight);
        pointLight.translateXProperty().bind(camera.translateXProperty());
        pointLight.translateYProperty().bind(camera.translateYProperty());
        pointLight.translateZProperty().bind(camera.translateZProperty().add(500));

        ambientLight.getScope().addAll(surfPlot);
        sceneRoot.getChildren().add(ambientLight);

        ColorPicker lightPicker = new ColorPicker(Color.WHITE);
        ambientLight.colorProperty().bind(lightPicker.valueProperty());

        ColorPicker specPicker = new ColorPicker(Color.CYAN);
        specPicker.setOnAction(e -> {
            ((PhongMaterial) surfPlot.getMaterial()).setSpecularColor(specPicker.getValue());
        });

        CheckBox enableAmbient = new CheckBox("Enable Ambient Light");
        enableAmbient.setSelected(true);
        enableAmbient.setOnAction(e -> {
            if (enableAmbient.isSelected()) {
                lightPicker.setDisable(false);
                ambientLight.getScope().addAll(surfPlot);
            } else {
                lightPicker.setDisable(true);
                ambientLight.getScope().clear();
            }
        });

        CheckBox enablePoint = new CheckBox("Enable Point Light");
        enablePoint.setSelected(true);
        enablePoint.setOnAction(e -> {
            if (enablePoint.isSelected()) {
                specPicker.setDisable(false);
                pointLight.getScope().addAll(surfPlot);
            } else {
                specPicker.setDisable(true);
                pointLight.getScope().clear();
            }
        });

        ToggleButton startRandos = new ToggleButton("startRandos");
        startRandos.setOnAction(e -> computeRandos = startRandos.isSelected());
        ToggleButton animate = new ToggleButton("animated");
        animate.setOnAction(e -> animated = animate.isSelected());

        Label divLabel = new Label("Divisions");
        divLabel.setPrefWidth(125);
        Label xWidthLabel = new Label("Usable X Width");
        xWidthLabel.setPrefWidth(125);
        Label zWidthLabel = new Label("Usable Z Length");
        zWidthLabel.setPrefWidth(125);
        Label yScaleLabel = new Label("Y Scale");
        yScaleLabel.setPrefWidth(125);
        Label surfScaleLabel = new Label("Surface Range Scale");
        surfScaleLabel.setPrefWidth(125);

        VBox vbox = new VBox(10,
            //new HBox(10, startRandos, animate),
            //new HBox(10, divLabel, divisionsSpinner),
            new HBox(10, xWidthLabel, xWidthSpinner),
            new HBox(10, zWidthLabel, zWidthSpinner),
            new HBox(10, yScaleLabel, yScaleSpinner),
            new HBox(10, surfScaleLabel, surfScaleSpinner),
            new Label("Color Method"),
            colorationHBox,
            new Label("Draw Mode"),
            meshTypeHBox,
            drawModeHBox,
            new Label("Cull Face"),
            cullFaceHBox,
            new Label("Ambient Light Color"),
            enableAmbient,
            lightPicker,
//            new Label("Diffuse Color"),
//            diffusePicker,
            new Label("Specular Color"),
            enablePoint,
            specPicker
        );
        StackPane.setAlignment(vbox, Pos.BOTTOM_LEFT);
        vbox.setPickOnBounds(false);
        getChildren().add(vbox);
//        Utils.printTotalTime(startTime);
        updateLabels();

        //create callout automatically puts the callout and node into a managed map
        Platform.runLater(() -> {
            FeatureVector dummy = FeatureVector.EMPTY_FEATURE_VECTOR("", 3);
            anchorCallout = createCallout(highlightedPoint, dummy, subScene);
            anchorCallout.play().setOnFinished(fin -> anchorCallout.setVisible(false));
        });
    }

    public void updateCalloutByFeatureVector(Callout callout, FeatureVector featureVector) {
        //UPdate label
        callout.setMainTitleText(featureVector.getLabel());
        callout.mainTitleTextNode.setText(callout.getMainTitleText());
        //update image (incoming hypersonic hack)
        VBox vbox = (VBox) callout.mainTitleNode;
        TitledPane tp0 = (TitledPane) vbox.getChildren().get(0);
        ImageView iv = loadImageView(featureVector, featureVector.isBBoxValid());
        Image image = iv.getImage();
        ((ImageView) tp0.getContent()).setImage(image);
        //update metadata
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : featureVector.getMetaData().entrySet()) {
            sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        TitledPane tp1 = (TitledPane) vbox.getChildren().get(1);
        ((Text) tp1.getContent()).setText(sb.toString());
    }

    private void addDebugPoint(Point3D point3D) {
        Sphere sphere = new Sphere(1);
        PhongMaterial mat = new PhongMaterial(Color.ALICEBLUE);
        sphere.setMaterial(mat);
        sphere.setTranslateX(point3D.x);
        sphere.setTranslateY(point3D.y);
        sphere.setTranslateZ(point3D.z);
        extrasGroup.getChildren().add(sphere);
        Label newLabel = new Label(point3D.toString());
        labelGroup.getChildren().addAll(newLabel);
        newLabel.setTextFill(Color.SKYBLUE);
        newLabel.setFont(new Font("calibri", 8));
        shape3DToLabel.put(sphere, newLabel);
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
        shape3DToLabel.clear();
        //Add to hashmap so updateLabels() can manage the label position
        shape3DToLabel.put(xSphere, xLabel);
        shape3DToLabel.put(ySphere, yLabel);
        shape3DToLabel.put(zSphere, zLabel);
        shape3DToLabel.put(highlightedPoint, hoverText);
        dataGrid.clear();
        featureVectors.clear();
    }

    public void showAll() {
        updateView(true);
    }

    public void hideFA3D() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(0.1), new KeyValue(opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.2), e -> outtro(1000)),
            new KeyFrame(Duration.seconds(2.0), new KeyValue(opacityProperty(), 0.0)),
            new KeyFrame(Duration.seconds(2.0), e -> setVisible(false)),
            new KeyFrame(Duration.seconds(2.1), e -> setOpacity(1.0))
        );
        timeline.setOnFinished(e -> setVisible(false));
        timeline.playFromStart();
    }

    public void showFA3D() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(0.1), e ->
                camera.setTranslateZ(DEFAULT_INTRO_DISTANCE)),
            new KeyFrame(Duration.seconds(0.1), new KeyValue(opacityProperty(), 0.0)),
            new KeyFrame(Duration.seconds(0.3), e -> setVisible(true)),
            new KeyFrame(Duration.seconds(0.3), new KeyValue(opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.6), e -> intro(1000))
        );
        timeline.playFromStart();
    }

    @Override
    public void setFeatureCollection(FeatureCollection fc) {
        featureVectors = fc.getFeatures();
    }

    public void findClusters(ManifoldEvent.ProjectionConfig pc) {
        //safety check
        if (pc.dataSource != ManifoldEvent.ProjectionConfig.DATA_SOURCE.HYPERSURFACE) return;
        //convert featurevector space into 2D array of doubles
        double[][] observations = FeatureCollection.toData(featureVectors);
        double projectionScalar = 1000.0;
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
    public void addSemanticMapCollection(SemanticMapCollection semanticMapCollection) {
        SemanticReconstruction reconstruction = semanticMapCollection.getReconstruction();
        SemanticReconstructionMap rMap = reconstruction.getData_vars().getNeural_timeseries();
        List<List<Double>> neuralData = rMap.getData();
        LOG.info("Neural Data dimensions: {} entries at {} frame width.", neuralData.size(), neuralData.get(0).size());
        //need to add every other data point in the width dimension (goes in phase/mag pairs)
        long startTime = System.nanoTime();
        dataGrid.clear();
        List<Double> justTheMags;
        for (List<Double> phaseMagPairs : neuralData) {
            justTheMags = new ArrayList<>(neuralData.get(0).size() / 2);
            for (int i = 0; i < phaseMagPairs.size(); i += 2) {
                justTheMags.add(phaseMagPairs.get(i) * yScale);
            }
            dataGrid.add(justTheMags);
        }
        LOG.info("Mapped Neural Magnitudes to Hypersurface: {}", Utils.totalTimeString(startTime));
        zWidth = neuralData.size();
        xWidth = neuralData.get(0).size() / 2;
        zWidthSpinner.getValueFactory().setValue(zWidth);
        xWidthSpinner.getValueFactory().setValue(xWidth);
        updateTheMesh();

        xSphere.setTranslateX((xWidth * surfScale) / 2.0);
        zSphere.setTranslateZ((zWidth * surfScale) / 2.0);

        double poleHeight = surfPlot.getMaxY() * 2;
        glowLineBox.setWidth(xWidth * surfScale);
        glowLineBox.setHeight(poleHeight);
        eastPole.setHeight(poleHeight * 1.2);
        westPole.setHeight(poleHeight * 1.2);
        eastPole.setTranslateX(-(xWidth * surfScale) / 2.0);
        westPole.setTranslateX((xWidth * surfScale) / 2.0);
        eastKnob.setTranslateX((xWidth * surfScale) / 2.0);
        westKnob.setTranslateX(-(xWidth * surfScale) / 2.0);
        eastKnob.setTranslateY(-(poleHeight * 1.2) / 2.0);
        westKnob.setTranslateY(-(poleHeight * 1.2) / 2.0);
        eastLabel.setText("Sample: " + anchorIndex + ", Neural Feature: " + xWidth);
        westLabel.setText("Sample: " + anchorIndex + ", Neural Feature: 0");
        updateLabels();
    }

    @Override
    public void addSemanticMap(SemanticMap semanticMap) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SemanticMap getSemanticMap(long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void locateSemanticMap(SemanticMap semanticMap) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clearSemanticMaps() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addFeatureCollection(FeatureCollection featureCollection, boolean clearQueue) {
        if (null == dataGrid) {
            dataGrid = new ArrayList<>(featureCollection.getFeatures().size());
        } else
            dataGrid.clear();

        List<Double> xList;
        for (FeatureVector fv : featureCollection.getFeatures()) {
            xList = new ArrayList<>(fv.getData().size());
            xList.addAll(fv.getData());
            dataGrid.add(xList);
        }
        zWidth = dataGrid.size();
        xWidth = dataGrid.get(0).size();
        zWidthSpinner.getValueFactory().setValue(zWidth);
        xWidthSpinner.getValueFactory().setValue(xWidth);
        updateTheMesh();

        getScene().getRoot().fireEvent(
            new CommandTerminalEvent("Hypersurface updated. ",
                new Font("Consolas", 20), Color.GREEN));
        //@TODO SMP fix so this works
//        if(clearQueue)
//            featureVectors = featureCollection.getFeatures();
//        else
//            featureVectors.addAll(featureCollection.getFeatures());
        featureVectors = featureCollection.getFeatures();
    }

    @Override
    public void addFeatureVector(FeatureVector featureVector) {
        featureVectors.add(featureVector);
        dataGrid.add(featureVector.getData());
    }

    @Override
    public void locateFeatureVector(FeatureVector featureVector) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clearFeatureVectors() {
        featureVectors.clear();
        dataGrid.clear();
    }

    @Override
    public List<FeatureVector> getAllFeatureVectors() {
        if (null == featureVectors)
            return Collections.EMPTY_LIST;
        return featureVectors;
    }

    @Override
    public void setColorByID(String iGotID, Color color) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setColorByIndex(int i, Color color) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setVisibleByIndex(int i, boolean b) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void refresh() {
        refresh(true);
    }

    @Override
    public void refresh(boolean forceNodeUpdate) {
        updateTheMesh();
    }

    @Override
    public void setDimensionLabels(List<String> labelStrings) {
        featureLabels = labelStrings;
    }

    @Override
    public void setSpheroidAnchor(boolean animate, int index) {
        double z = index * surfScale;
//        int column = Float.valueOf(vertP3D.getX() / surfScale).intValue();
    }

    private void tessellateImage(Image image, int x1, int y1, int x2, int y2) {
        lastImage = image;
        long startTime = System.nanoTime();
        LOG.info("Mapping Image Raster to Feature Vector... ");
        startTime = System.nanoTime();
        int rows = Double.valueOf(image.getHeight()).intValue();
        int columns = Double.valueOf(image.getWidth()).intValue();
        PixelReader pr = image.getPixelReader();
        Color color = null;
        int rgb, r, g, b = 0;
        double dataValue = 0;
        if (null == dataGrid) {
            dataGrid = new ArrayList<>(rows);
        } else
            dataGrid.clear();
        featureVectors.clear();
        for (int rowIndex = y1; rowIndex < y2; rowIndex++) {
            List<Double> currentDataRow = new ArrayList<>();
            for (int colIndex = x1; colIndex < x2; colIndex++) {
                color = pr.getColor(colIndex, rowIndex);
                rgb = ((int) pr.getArgb(colIndex, rowIndex));
                FeatureVector fv = FeatureVector.EMPTY_FEATURE_VECTOR(color.toString(), 3);
                fv.getData().set(0, Double.valueOf(colIndex) / columns);
                fv.getData().set(1, Double.valueOf(rowIndex) / rows);
                r = (rgb >> 16) & 0xFF;
                g = (rgb >> 8) & 0xFF;
                b = rgb & 0xFF;
                dataValue = (((r + g + b) / 3.0) / 255.0);
                fv.getData().set(2, dataValue);
                featureVectors.add(fv);
                currentDataRow.add(dataValue);
            }
            dataGrid.add(currentDataRow);
        }
        Utils.printTotalTime(startTime);
        LOG.info("Injecting Mesh into Hypersurface... ");
        startTime = System.nanoTime();
        zWidth = rows;
        xWidth = columns;
        zWidthSpinner.getValueFactory().setValue(zWidth);
        xWidthSpinner.getValueFactory().setValue(xWidth);
        updateTheMesh();
        xSphere.setTranslateX((xWidth * surfScale) / 2.0);
        zSphere.setTranslateZ((zWidth * surfScale) / 2.0);
        Utils.printTotalTime(startTime);
    }

    @Override
    public void addShapleyCollection(ShapleyCollection shapleyCollection) {
        shapleyVectors.clear();
        lastImageSource = shapleyCollection.getSourceInput();
        shapleyVectors.addAll(shapleyCollection.getValues());
        try {
            //method will automatically prepend base path for imagery
            WritableImage wi = loadImage(shapleyCollection.getSourceInput());
            if (null != wi) {
                int x2 = Double.valueOf(wi.getWidth()).intValue();
                int y2 = Double.valueOf(wi.getHeight()).intValue();
                tessellateImage(wi, 0, 0, x2, y2);
                lastImage = wi;
                //sneakily insert current function values (shapley) into vertices
                LOG.info("injecting Shapley function values into Vertices... ");
                long startTime = System.nanoTime();
                surfPlot.functionValues.clear();
                for (int i = 0; i < shapleyVectors.size(); i++) {
                    surfPlot.functionValues.add(
                        shapleyVectors.get(i).getData().get(0) * yScale);
                }
                Utils.printTotalTime(startTime);
                if (null == colorationMethod)
                    surfPlot.setTextureModeVertices3D(TOTAL_COLORS, colorByHeight, 0.0, 360.0);
                switch (colorationMethod) {
                    case COLOR_BY_IMAGE -> surfPlot.setTextureModeImage(imageryBasePath + lastImageSource);
                    case COLOR_BY_FEATURE -> surfPlot.setTextureModeVertices3D(TOTAL_COLORS, colorByHeight, 0.0, 360.0);
                    default -> surfPlot.setTextureModeVertices3D(TOTAL_COLORS, colorByShapley, 0.0, 360.0);
                }
            }
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
    }

    @Override
    public void addShapleyVector(ShapleyVector shapleyVector) {
        shapleyVectors.add(shapleyVector);
    }

    @Override
    public void clearShapleyVectors() {
        shapleyVectors.clear();
    }
}
