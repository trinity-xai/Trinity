package edu.jhuapl.trinity.javafx.javafx3d;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.CoordinateSet;
import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
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
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.HypersurfaceEvent;
import edu.jhuapl.trinity.javafx.events.HypersurfaceGridEvent;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.events.ShadowEvent;
import edu.jhuapl.trinity.javafx.events.TimelineEvent;
import edu.jhuapl.trinity.javafx.javafx3d.animated.TessellationTube;
import edu.jhuapl.trinity.javafx.javafx3d.images.ImageResourceProvider;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.AffinityClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.DBSCANClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.ExMaxClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.HDDBSCANClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.KMeansClusterTask;
import edu.jhuapl.trinity.javafx.javafx3d.tasks.KMediodsClusterTask;
import edu.jhuapl.trinity.javafx.renderers.FeatureVectorRenderer;
import edu.jhuapl.trinity.javafx.renderers.Graph3DRenderer;
import edu.jhuapl.trinity.javafx.renderers.SemanticMapRenderer;
import edu.jhuapl.trinity.javafx.renderers.ShapleyVectorRenderer;
import edu.jhuapl.trinity.utils.DataUtils;
import edu.jhuapl.trinity.utils.DataUtils.HeightMode;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import edu.jhuapl.trinity.utils.metric.Metric;
import edu.jhuapl.trinity.utils.statistics.GridDensityResult;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
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
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TitledPane;
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
import java.util.*;
import java.util.function.Function;
import javafx.event.Event;
import javafx.scene.control.Menu;

/**
 * @author Sean Phillips
 */
public class Hypersurface3DPane extends StackPane
    implements SemanticMapRenderer, FeatureVectorRenderer, ShapleyVectorRenderer {

    private static final Logger LOG = LoggerFactory.getLogger(Hypersurface3DPane.class);
    public static double ICON_FIT_HEIGHT = 64;
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

    public enum COLORATION {COLOR_BY_IMAGE, COLOR_BY_FEATURE, COLOR_BY_SHAPLEY}

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
    Function<Point3D, Number> colorByHeight = p -> p.y; //Color mapping function
    Function<Point3D, Number> colorByShapley = p -> p.f;

    Function<Vert3D, Number> vert3DLookup = p -> vertToHeight(p);

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
    private Sphere xSphere = new Sphere(10);
    private Sphere ySphere = new Sphere(10);
    private Sphere zSphere = new Sphere(10);
    Sphere highlightedPoint = new Sphere(2, 32);
    private Label xLabel = new Label("Features (ordered)");
    private Label yLabel = new Label("Magnitude");
    private Label zLabel = new Label("Time (Samples)");
    Text hoverText = new Text("Coordinates: ");

    public List<String> featureLabels = new ArrayList<>();
    public Scene scene;
    HashMap<Shape3D, Callout> shape3DToCalloutMap;
    public String imageryBasePath = "";
    SurfaceChartPane surfaceChartPane;
    public AmbientLight ambientLight;
    public PointLight pointLight;
    private List<List<Double>> originalGrid = new ArrayList<>();

    // Event-driven state fields for settings previously set via controls
    private HeightMode heightMode = HeightMode.RAW;
    private boolean smoothingEnabled = false;
    private SurfaceUtils.Smoothing smoothingMethod = SurfaceUtils.Smoothing.GAUSSIAN;
    private int smoothingRadius = 2;
    private double gaussianSigma = 1.0;
    private SurfaceUtils.Interpolation interpMode = SurfaceUtils.Interpolation.NEAREST;
    private boolean toneEnabled = false;
    private SurfaceUtils.ToneMap toneOperator = SurfaceUtils.ToneMap.NONE;
    private double toneParam = 2.0;    
    // --- Graph layer support ---
    private final Group graphLayer = new Group(); // sits in sceneRoot
    private GraphDirectedCollection currentGraph = null;
    private Graph3DRenderer.Params graphParams = new Graph3DRenderer.Params()
            .withNodeRadius(20.0)
            .withEdgeWidth(8.0f)
            .withPositionScalar(1.0);

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
        nodeGroup.getTransforms().addAll(rotateX, rotateY, rotateZ);
        xSphere.setTranslateX(planeSize / 2.0);
        xSphere.setMaterial(new PhongMaterial(Color.RED));
        ySphere.setTranslateY(-planeSize / 2.0);
        ySphere.setMaterial(new PhongMaterial(Color.GREEN));
        zSphere.setTranslateZ(planeSize / 2.0);
        zSphere.setMaterial(new PhongMaterial(Color.BLUE));
        highlightedPoint.setMaterial(new PhongMaterial(Color.ALICEBLUE));
        highlightedPoint.setDrawMode(DrawMode.FILL);
        highlightedPoint.setMouseTransparent(true);

        // Labels
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

        labelGroup.getChildren().addAll(xLabel, yLabel, zLabel, hoverText);
        labelGroup.setManaged(false);
        shape3DToLabel.put(xSphere, xLabel);
        shape3DToLabel.put(ySphere, yLabel);
        shape3DToLabel.put(zSphere, zLabel);
        shape3DToLabel.put(highlightedPoint, hoverText);
        camera = new PerspectiveCamera(true);

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
        sceneRoot.getChildren().addAll(cameraTransform, highlightedPoint,
            nodeGroup, extrasGroup, debugGroup, dataXForm);
        // Add graph layer last so it draws above the surface (z-order within Group)
        sceneRoot.getChildren().add(graphLayer);
        subScene.setCamera(camera);
        pointLight = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(pointLight);
        pointLight.setTranslateX(camera.getTranslateX());
        pointLight.setTranslateY(camera.getTranslateY());
        pointLight.setTranslateZ(camera.getTranslateZ() + 500.0);

//        subScene.setOnMouseEntered(event -> subScene.requestFocus());
//        setOnMouseEntered(event -> subScene.requestFocus());
        subScene.setOnZoom(event -> {
            double modifier = 50.0;
            double modifierFactor = 0.1;
            double z = camera.getTranslateZ();
            double newZ = z + event.getZoomFactor() * modifierFactor * modifier;
            camera.setTranslateZ(newZ);
            updateLabels();
        });

        subScene.setOnKeyPressed(event -> {
            KeyCode keycode = event.getCode();

            if ((keycode == KeyCode.NUMPAD0 && event.isControlDown())
                || (keycode == KeyCode.DIGIT0 && event.isControlDown())) {
                resetView(1000, false);
            } else if ((keycode == KeyCode.NUMPAD0 && event.isShiftDown())
                || (keycode == KeyCode.DIGIT0 && event.isShiftDown())) {
                resetView(0, true);
            }
            double change = 10.0;
            if (event.isShiftDown()) change = 100.0;

            if (keycode == KeyCode.W) camera.setTranslateZ(camera.getTranslateZ() + change);
            if (keycode == KeyCode.S) camera.setTranslateZ(camera.getTranslateZ() - change);
            if (keycode == KeyCode.PLUS && event.isShortcutDown()) camera.setTranslateZ(camera.getTranslateZ() + change);
            if (keycode == KeyCode.MINUS && event.isShortcutDown()) camera.setTranslateZ(camera.getTranslateZ() - change);

            if (keycode == KeyCode.A) camera.setTranslateX(camera.getTranslateX() - change);
            if (keycode == KeyCode.D) camera.setTranslateX(camera.getTranslateX() + change);
            if (keycode == KeyCode.SPACE) camera.setTranslateY(camera.getTranslateY() + change);
            if (keycode == KeyCode.X) camera.setTranslateY(camera.getTranslateY() - change);

            change = event.isShiftDown() ? 10.0 : 1.0;
            if (keycode == KeyCode.NUMPAD7 || (keycode == KeyCode.DIGIT8)) cameraTransform.ry.setAngle(cameraTransform.ry.getAngle() + change);
            if (keycode == KeyCode.NUMPAD9 || (keycode == KeyCode.DIGIT8 && event.isControlDown())) cameraTransform.ry.setAngle(cameraTransform.ry.getAngle() - change);
            if (keycode == KeyCode.NUMPAD4 || (keycode == KeyCode.DIGIT9)) cameraTransform.rx.setAngle(cameraTransform.rx.getAngle() + change);
            if (keycode == KeyCode.NUMPAD6 || (keycode == KeyCode.DIGIT9 && event.isControlDown())) cameraTransform.rx.setAngle(cameraTransform.rx.getAngle() - change);
            if (keycode == KeyCode.NUMPAD1 || (keycode == KeyCode.DIGIT0)) cameraTransform.rz.setAngle(cameraTransform.rz.getAngle() + change);
            if (keycode == KeyCode.NUMPAD3 || (keycode == KeyCode.DIGIT0 && event.isControlDown())) cameraTransform.rz.setAngle(cameraTransform.rz.getAngle() - change);

            if (keycode == KeyCode.COMMA) {
                if (xFactorIndex > 0 && yFactorIndex > 0 && zFactorIndex > 0) {
                    xFactorIndex -= 1; yFactorIndex -= 1; zFactorIndex -= 1;
                    Platform.runLater(() -> scene.getRoot().fireEvent(
                        new HyperspaceEvent(HyperspaceEvent.FACTOR_COORDINATES_KEYPRESS,
                            new CoordinateSet(xFactorIndex, yFactorIndex, zFactorIndex))));
                    boolean redraw = true;
                    if (redraw) { updateView(false); notifyIndexChange(); }
                    updateLabels();
                }
            }
            if (keycode == KeyCode.PERIOD) {
                int featureSize = featureVectors.isEmpty()? factorMaxIndex : featureVectors.get(0).getData().size();
                if (xFactorIndex < factorMaxIndex - 1 && yFactorIndex < factorMaxIndex - 1
                    && zFactorIndex < factorMaxIndex - 1 && xFactorIndex < featureSize - 1
                    && yFactorIndex < featureSize - 1 && zFactorIndex < featureSize - 1) {
                    xFactorIndex += 1; yFactorIndex += 1; zFactorIndex += 1;
                    Platform.runLater(() -> scene.getRoot().fireEvent(
                        new HyperspaceEvent(HyperspaceEvent.FACTOR_COORDINATES_KEYPRESS,
                            new CoordinateSet(xFactorIndex, yFactorIndex, zFactorIndex))));
                    boolean redraw = true;
                    if (redraw) { updateView(false); notifyIndexChange(); }
                    updateLabels();
                } else {
                    scene.getRoot().fireEvent(new CommandTerminalEvent("Feature Index Max Reached: ("
                        + featureSize + ")", new Font("Consolas", 20), Color.YELLOW));
                }
            }
            if (keycode == KeyCode.SLASH && event.isControlDown()) debugGroup.setVisible(!debugGroup.isVisible());
            if (keycode == KeyCode.Y) surfPlot.scaleHeight(1.1f);
            if (keycode == KeyCode.H) surfPlot.scaleHeight(0.9f);

            if (keycode == KeyCode.I) { double tz = event.isShiftDown()? 50:5; glowLineBox.setTranslateZ(glowLineBox.getTranslateZ() + tz);} 
            if (keycode == KeyCode.K) { double tz = event.isShiftDown()? 50:5; glowLineBox.setTranslateZ(glowLineBox.getTranslateZ() - tz);} 

            updateLabels();
            updateCalloutHeadPoints(subScene);
        });

        subScene.setOnMousePressed((MouseEvent me) -> {
            if (me.isSynthesized()) LOG.info("isSynthesized");
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
        subScene.setOnZoom(e -> {
            double zoom = e.getZoomFactor();
            if (zoom > 1) camera.setTranslateZ(camera.getTranslateZ() + 50.0);
            else camera.setTranslateZ(camera.getTranslateZ() - 50.0);
            updateLabels();
            updateCalloutHeadPoints(subScene);
            e.consume();
        });
        subScene.setOnScroll((ScrollEvent event) -> {
            double modifier = 50.0; double modifierFactor = 0.1;
            if (event.isControlDown()) modifier = 1;
            if (event.isShiftDown()) modifier = 100.0;
            double z = camera.getTranslateZ();
            double newZ = z + event.getDeltaY() * modifierFactor * modifier;
            camera.setTranslateZ(newZ);
            updateLabels();
            updateCalloutHeadPoints(subScene);
        });

        subScene.setOnMouseDragged((MouseEvent me) -> mouseDragCamera(me));
        Pane pathPane = App.getAppPathPaneStack();
        surfaceChartPane = new SurfaceChartPane(scene, pathPane);
        bp = new BorderPane(subScene);
        getChildren().clear();
        getChildren().addAll(bp, labelGroup);

        
        MenuItem showControlsItem = new MenuItem("Hypersurface Controls");
        showControlsItem.setOnAction(e -> {
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_HYPERSPACE_CONTROLS, Boolean.TRUE));
        });
                
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
                try { ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file); } catch (IOException ioe) { }
            }
        });
        MenuItem unrollHyperspaceItem = new MenuItem("Unroll Hyperspace Data");
        unrollHyperspaceItem.setOnAction(e -> unrollHyperspace());

        MenuItem vectorDistanceItem = new MenuItem("Show Vector Distances");
        vectorDistanceItem.setOnAction(e -> computeVectorDistances());

        MenuItem collectionDifferenceItem = new MenuItem("Feature Collection Difference");
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
                } catch (IOException ex) { LOG.error(null, ex); }
            }
        });
        MenuItem cosineSimilarityItem = new MenuItem("Feature Collection Cosine Distance");
        cosineSimilarityItem.setOnAction(e -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load FeatureCollection to Compare...");
            fileChooser.setInitialDirectory(Paths.get(".").toFile());
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                FeatureCollectionFile fcf;
                try {
                    fcf = new FeatureCollectionFile(file.getAbsolutePath(), true);
                    computeCosineDistance(fcf.featureCollection);
                } catch (IOException ex) { LOG.error(null, ex); }
            }
        });

        Glow glow = new Glow(0.5);
        ImageView analysisImageView = ResourceUtils.loadIcon("analysis", ICON_FIT_HEIGHT);
        analysisImageView.setEffect(glow);
        Menu analysisMenu = new Menu("Analysis", analysisImageView,
            vectorDistanceItem, collectionDifferenceItem, cosineSimilarityItem
        );

        CheckMenuItem enableHoverItem = new CheckMenuItem("Hover Interactions");
        enableHoverItem.setOnAction(e -> {
            hoverInteractionsEnabled = enableHoverItem.isSelected();
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
            syncGuiControls();
            generateRandos(xWidth, zWidth, yScale);
            originalGrid = deepCopyGrid(dataGrid);
            updateTheMesh();
            updateView(true);
        });

        CheckMenuItem showDataMarkersItem = new CheckMenuItem("Show Data Markers");
        showDataMarkersItem.setOnAction(e -> {
            extrasGroup.setVisible(showDataMarkersItem.isSelected());
            labelGroup.setVisible(showDataMarkersItem.isSelected());
            if (null != anchorCallout) anchorCallout.setVisible(showDataMarkersItem.isSelected());
        });

        CheckMenuItem enableCrosshairsItem = new CheckMenuItem("Enable Crosshairs");
        enableCrosshairsItem.setOnAction(e -> crosshairsEnabled = enableCrosshairsItem.isSelected());

        MenuItem resetViewItem = new MenuItem("Reset View");
        resetViewItem.setOnAction(e -> resetView(1000, false));
        ContextMenu cm = new ContextMenu(showControlsItem, 
            copyAsImageItem, saveSnapshotItem, unrollHyperspaceItem, analysisMenu,
            enableHoverItem, surfaceChartsItem, showDataMarkersItem, enableCrosshairsItem,
            updateAllItem, clearDataItem, resetViewItem);
        cm.setAutoFix(true); cm.setAutoHide(true); cm.setHideOnEscape(true); cm.setOpacity(0.85);

        subScene.setOnMouseClicked((MouseEvent e) -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (!cm.isShowing()) cm.show(this.getParent(), e.getScreenX(), e.getScreenY());
                else cm.hide();
                e.consume();
            }
        });
        
        this.scene.addEventHandler(GraphEvent.NEW_GRAPHDIRECTED_COLLECTION, e -> {
            if (!(e.object instanceof GraphDirectedCollection gc)) return;
            currentGraph = gc;
            graphLayer.getChildren().clear();
            graphLayer.getChildren().add(Graph3DRenderer.buildGraphGroup(gc, graphParams));
            scene.getRoot().fireEvent(new CommandTerminalEvent(
                "Rendered 3D graph: nodes=" + gc.getNodes().size() + ", edges=" + gc.getEdges().size(),
                new Font("Consolas", 18), Color.LIGHTGREEN));
        });


        loadSurf3D();
        this.scene.addEventHandler(HyperspaceEvent.HYPERSPACE_BACKGROUND_COLOR, e -> {
            Color color = (Color) e.object; subScene.setFill(color);
        });
        this.scene.addEventHandler(HyperspaceEvent.ENABLE_HYPERSPACE_SKYBOX, e -> {
            skybox.setVisible((Boolean) e.object);
        });
        this.scene.addEventHandler(ImageEvent.NEW_TEXTURE_SURFACE, e -> {
            Image image = (Image) e.object;
            int x1 = 0; int y1 = 0;
            int x2 = (int) image.getWidth(); int y2 = (int) image.getHeight();
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
                Optional<ButtonType> optBT = alert.showAndWait();
                if (optBT.get().equals(ButtonType.CANCEL)) return;
                split = optBT.get().equals(ButtonType.YES);
                if (split) {
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
                if (xFactorIndex > factorMaxIndex) { xFactorIndex = factorMaxIndex; update = true; }
                if (yFactorIndex > factorMaxIndex) { yFactorIndex = factorMaxIndex; update = true; }
                if (zFactorIndex > factorMaxIndex) { zFactorIndex = factorMaxIndex; update = true; }
                if (update) { updateView(true); notifyIndexChange(); }
            } else factorMaxIndex = newFactorMaxIndex;
        });

        scene.addEventHandler(HypersurfaceGridEvent.RENDER_PDF, e -> { applySurfaceGridToHypersurface(e.getZGrid()); e.consume(); });
        scene.addEventHandler(HypersurfaceGridEvent.RENDER_CDF, e -> { applySurfaceGridToHypersurface(e.getZGrid()); e.consume(); });

        scene.addEventHandler(HyperspaceEvent.NODE_QUEUELIMIT_GUI, e -> queueLimit = (int) e.object);
        scene.addEventHandler(HyperspaceEvent.REFRESH_RATE_GUI, e -> hypersurfaceRefreshRate = (long) e.object);

        scene.addEventHandler(ShadowEvent.SHOW_AXES_LABELS, e -> {
            nodeGroup.setVisible((boolean) e.object);
            labelGroup.setVisible((boolean) e.object);
        });
        scene.addEventHandler(ApplicationEvent.SET_IMAGERY_BASEPATH, e -> imageryBasePath = (String) e.object);
        Platform.runLater(() -> {
            updateLabels();
            updateView(true);
            updateTheMesh();
        });
        AnimationTimer surfUpdateAnimationTimer = new AnimationTimer() {
            long sleepNs = 0; long prevTime = 0; long NANOS_IN_MILLI = 1_000_000;
            @Override public void handle(long now) {
                sleepNs = hypersurfaceRefreshRate * NANOS_IN_MILLI;
                if ((now - prevTime) < sleepNs) return; prevTime = now;
                long startTime;
                if (computeRandos) { generateRandos(xWidth, zWidth, yScale); }
                if (animated || isDirty) { startTime = System.nanoTime(); updateTheMesh(); LOG.info("updateTheMesh(): {}", Utils.totalTimeString(startTime)); }
            }
        };
        surfUpdateAnimationTimer.start();
    }

    public void computeCosineDistance(FeatureCollection collection) {
        double[][] newRayRay = collection.convertFeaturesToArray();
        Metric metric = Metric.getMetric("cosine");
        List<Double> cosineDistancesGrid = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < dataGrid.size(); rowIndex++) {
            double[] dataGridVector = dataGrid.get(rowIndex).stream().mapToDouble(Double::doubleValue).toArray();
            double currentDistance = metric.distance(dataGridVector, newRayRay[rowIndex]);
            cosineDistancesGrid.add(currentDistance);
        }
        scene.getRoot().fireEvent(new FactorAnalysisEvent(
            FactorAnalysisEvent.ANALYSIS_DATA_VECTOR, "Feature Collection Cosine Similarity",
                cosineDistancesGrid.toArray(Double[]::new)));
        System.out.println(cosineDistancesGrid.toString());
    }

    private void applySurfaceGridToHypersurface(List<List<Double>> grid) {
        double userScale = 1.0; // future: user control
        List<List<Double>> scaled = DataUtils.normalizeAndScale(grid, heightMode, userScale);
        dataGrid.clear();
        dataGrid.addAll(scaled);
        originalGrid = deepCopyGrid(dataGrid); // NEW
        xWidth = dataGrid.get(0).size();
        zWidth = dataGrid.size();
        syncGuiControls();
        rebuildProcessedGridAndRefresh(); // NEW: run pipeline
    }

    public void setSurfaceFromDensity(GridDensityResult res, boolean useCDF, boolean flipY) {
        List<List<Double>> grid = useCDF ? res.cdfAsListGrid() : res.pdfAsListGrid();
        if (flipY) Collections.reverse(grid);
        dataGrid.clear();
        dataGrid.addAll(grid);
        originalGrid = deepCopyGrid(dataGrid); // NEW
        xWidth = dataGrid.get(0).size();
        zWidth = dataGrid.size();
        syncGuiControls();
        rebuildProcessedGridAndRefresh(); // NEW
    }

    public void computeSurfaceDifference(FeatureCollection collection) {
        double[][] newRayRay = collection.convertFeaturesToArray();
        List<List<Double>> differencesGrid = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < dataGrid.size(); rowIndex++) {
            List<Double> differenceVector = new ArrayList<>();
            List<Double> currentRow = dataGrid.get(rowIndex);
            int width = currentRow.size();
            for (int colIndex = 0; colIndex < width; colIndex++) {
                if (rowIndex < newRayRay.length && colIndex < newRayRay[rowIndex].length) {
                    differenceVector.add(currentRow.get(colIndex) - newRayRay[rowIndex][colIndex]);
                } else differenceVector.add(0.0);
            }
            differencesGrid.add(differenceVector);
        }
        dataGrid.clear();
        dataGrid.addAll(differencesGrid);
        originalGrid = deepCopyGrid(dataGrid); // NEW
        xWidth = dataGrid.get(0).size();
        zWidth = dataGrid.size();
        syncGuiControls();
        rebuildProcessedGridAndRefresh(); // NEW
    }

    public void computeVectorDistances() {
        Metric metric = Metric.getMetric("cosine");
        List<List<Double>> distancesGrid = new ArrayList<>();
        dataGrid.stream().forEach(row -> {
            double[] rowVector = row.stream().mapToDouble(Double::doubleValue).toArray();
            List<Double> distanceVector = new ArrayList<>();
            for (int i = 0; i < dataGrid.size(); i++) {
                double[] xVector = dataGrid.get(i).stream().mapToDouble(Double::doubleValue).toArray();
                double currentDistance = metric.distance(xVector, rowVector);
                distanceVector.add(currentDistance);
            }
            distancesGrid.add(distanceVector);
        });
        dataGrid.clear();
        dataGrid.addAll(distancesGrid);
        originalGrid = deepCopyGrid(dataGrid); // NEW
        xWidth = dataGrid.get(0).size();
        zWidth = dataGrid.size();
        syncGuiControls();
        rebuildProcessedGridAndRefresh(); // NEW
    }

    public void unrollHyperspace() {
        getScene().getRoot().fireEvent(new CommandTerminalEvent("Requesting Hyperspace Vectors...",
            new Font("Consolas", 20), Color.GREEN));
        getScene().getRoot().fireEvent(new FeatureVectorEvent(FeatureVectorEvent.REQUEST_FEATURE_COLLECTION));
    }

    public void updateCalloutHeadPoint(Shape3D node, Callout callout, SubScene subScene) {
        Point2D p2d = JavaFX3DUtils.getTransformedP2D(node, subScene, callout.head.getRadius() + 5);
        callout.updateHeadPoint(p2d.getX(), p2d.getY());
    }

    public void updateCalloutHeadPoints(SubScene subScene) {
        shape3DToCalloutMap.forEach((node, callout) -> updateCalloutHeadPoint(node, callout, subScene));
    }

    public Callout createCallout(Shape3D shape3D, FeatureVector featureVector, SubScene subScene) {
        ImageView iv = loadImageView(featureVector, featureVector.isBBoxValid());
        iv.setPreserveRatio(true); iv.setFitWidth(CHIP_FIT_WIDTH); iv.setFitHeight(CHIP_FIT_WIDTH);
        TitledPane imageTP = new TitledPane(); imageTP.setContent(iv); imageTP.setText("Imagery");
        Point2D p2D = JavaFX3DUtils.getTransformedP2D(shape3D, subScene, Callout.DEFAULT_HEAD_RADIUS + 5);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : featureVector.getMetaData().entrySet()) sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        Text metaText = new Text(sb.toString());
        TitledPane metaTP = new TitledPane(); metaTP.setContent(metaText); metaTP.setText("Metadata");
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
        infoCallout.play().setOnFinished(eh -> { if (null == featureVector.getImageURL() || featureVector.getImageURL().isBlank()) imageTP.setExpanded(false); });
        return infoCallout;
    }

    public void addCallout(Callout callout, Shape3D shape3D) {
        callout.setManaged(false);
        getChildren().add(callout);
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
            Platform.runLater(() -> sceneRoot.getChildren().add(tube));
        }
        Platform.runLater(this::updatePaintMesh);
    }

    public void updatePaintMesh() {
        diffusePaintImage = new WritableImage((int) xWidth, (int) zWidth);
        if (null == paintTriangleMesh) {
            paintTriangleMesh = new TriangleMesh();
            paintMeshView = new MeshView(paintTriangleMesh);
            paintMeshView.setMouseTransparent(true);
            paintMeshView.setMesh(paintTriangleMesh);
            paintMeshView.setCullFace(CullFace.NONE);
            paintPhong = new PhongMaterial(Color.WHITE, diffusePaintImage, null, null, null);
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
        int pskip = 2;
        int subDivX = (int) diffusePaintImage.getWidth() / pskip;
        int subDivZ = (int) diffusePaintImage.getHeight() / pskip;
        int numDivX = subDivX + 1;
        int numVerts = (subDivZ + 1) * numDivX;
        float currZ, currX;
        float texCoords[] = new float[numVerts * texCoordSize];
        int faceCount = subDivX * subDivZ * 2;
        final int faceSize = 6;
        int faces[] = new int[faceCount * faceSize];
        int index, p00, p01, p10, p11, tc00, tc01, tc10, tc11;

        for (int z = 0; z < subDivZ; z++) {
            currZ = (float) z / subDivZ;
            for (int x = 0; x < subDivX; x++) {
                currX = (float) x / subDivX;
                index = z * numDivX * texCoordSize + (x * texCoordSize);
                texCoords[index] = currX;
                texCoords[index + 1] = currZ;

                p00 = z * numDivX + x; p01 = p00 + 1; p10 = p00 + numDivX; p11 = p10 + 1;
                tc00 = z * numDivX + x; tc01 = tc00 + 1; tc10 = tc00 + numDivX; tc11 = tc10 + 1;

                index = (z * subDivX * faceSize + (x * faceSize)) * 2;
                faces[index + 0] = p00; faces[index + 1] = tc00; faces[index + 2] = p10; faces[index + 3] = tc10; faces[index + 4] = p11; faces[index + 5] = tc11;
                index += faceSize;
                faces[index + 0] = p11; faces[index + 1] = tc11; faces[index + 2] = p01; faces[index + 3] = tc01; faces[index + 4] = p00; faces[index + 5] = tc00;
                diffusePaintImage.getPixelWriter().setColor(x, z, Color.TRANSPARENT);
            }
        }
        paintTriangleMesh.getTexCoords().setAll(texCoords);
        paintTriangleMesh.getFaces().setAll(faces);
        paintPhong.setDiffuseMap(diffusePaintImage);
        paintMeshView.setTranslateZ(-1);
        paintMeshView.setTranslateX(-(xWidth * surfScale) / 2.0);
        paintMeshView.setTranslateZ(-(zWidth * surfScale) / 2.0);
    }

    public void paintSingleColor(Color color) {
        for (int z = 0; z < diffusePaintImage.getHeight(); z++) {
            for (int x = 0; x < diffusePaintImage.getWidth(); x++) {
                diffusePaintImage.getPixelWriter().setColor(x, z, color);
            }
        }
    }

    public void illuminateCrosshair(Point3D center) {
        if (null == diffusePaintImage) return;
        int x = (int) (center.getX() / surfScale);
        int z = (int) (center.getZ() / surfScale);
        PixelWriter pw = diffusePaintImage.getPixelWriter();
        for (int i = 0; i < diffusePaintImage.getWidth(); i++) pw.setColor(i, z, Color.WHITE);
        for (int i = 0; i < diffusePaintImage.getHeight(); i++) pw.setColor(x, i, Color.WHITE);
    }

    private void setupSkyBox() {
        Image top = new Image(ImageResourceProvider.getResource("darkmetalbottom.png").toExternalForm());
        Image bottom = new Image(ImageResourceProvider.getResource("darkmetalbottom.png").toExternalForm());
        Image left = new Image(ImageResourceProvider.getResource("1500_blackgrid.png").toExternalForm());
        Image right = new Image(ImageResourceProvider.getResource("1500_blackgrid.png").toExternalForm());
        Image front = new Image(ImageResourceProvider.getResource("1500_blackgrid.png").toExternalForm());
        Image back = new Image(ImageResourceProvider.getResource("1500_blackgrid.png").toExternalForm());
        double size = 100000D;
        skybox = new Skybox(top,bottom,left,right,front,back,size,camera);
        sceneRoot.getChildren().add(skybox);
        ambientLight.getScope().addAll(skybox);
        skybox.setVisible(false);
    }

    private void notifyIndexChange() {
        getScene().getRoot().fireEvent(new CommandTerminalEvent("X,Y,Z Indices = ("
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

    public void outtro(double milliseconds) { JavaFX3DUtils.zoomTransition(milliseconds, camera, DEFAULT_INTRO_DISTANCE); }

    public void updateAll() { Platform.runLater(() -> updateView(true)); }

    private void mouseDragCamera(MouseEvent me) {
        mouseOldX = mousePosX; mouseOldY = mousePosY;
        mousePosX = me.getSceneX(); mousePosY = me.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX); mouseDeltaY = (mousePosY - mouseOldY);
        double modifier = 1.0; double modifierFactor = 0.1;
        if (me.isControlDown()) modifier = 0.1;
        if (me.isShiftDown()) modifier = 25.0;
        if (me.isPrimaryButtonDown()) {
            if (me.isAltDown()) cameraTransform.rz.setAngle(((cameraTransform.rz.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            else {
                cameraTransform.ry.setAngle(((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
                cameraTransform.rx.setAngle(((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            }
        } else if (me.isMiddleButtonDown()) {
            cameraTransform.t.setX(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3);
            cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3);
        }
        updateLabels();
        updateCalloutHeadPoints(subScene);
    }

    private void updateLabels() {
        shape3DToLabel.forEach((shape3D, node) -> {
            Point2D p2Ditty = JavaFX3DUtils.getTransformedP2D(shape3D, subScene, 5);
            double x = p2Ditty.getX(); double y = p2Ditty.getY() - 25;
            node.getTransforms().setAll(new Translate(x, y));
        });
    }

    private ImageView loadImageView(FeatureVector featureVector, boolean bboxOnly) {
        ImageView iv = null;
        try {
            if (bboxOnly) {
                WritableImage image = ResourceUtils.loadImageFileSubset(imageryBasePath + featureVector.getImageURL(),
                    featureVector.getBbox().get(0).intValue(),
                    featureVector.getBbox().get(1).intValue(),
                    featureVector.getBbox().get(2).intValue(),
                    featureVector.getBbox().get(3).intValue());
                iv = new ImageView(image);
            } else if (null != featureVector.getImageURL() && !featureVector.getImageURL().isBlank()) {
                iv = new ImageView(ResourceUtils.loadImageFile(imageryBasePath + featureVector.getImageURL()));
            } else iv = new ImageView(ResourceUtils.loadIconFile("noimage"));
        } catch (Exception ex) { iv = new ImageView(ResourceUtils.loadIconFile("noimage")); }
        return iv;
    }

    public void updateView(boolean forcePNodeUpdate) {
        if (null != surfPlot) {
            Platform.runLater(() -> {
                if (heightChanged) { heightChanged = false; }
                isDirty = false;
            });
        }
    }

    private void generateRandos(int xWidth, int zWidth, float yScale) {
        if (null == dataGrid) dataGrid = new ArrayList<>(zWidth); else dataGrid.clear();
        List<Double> xList;
        for (int z = 0; z < zWidth; z++) {
            xList = new ArrayList<>(xWidth);
            for (int x = 0; x < xWidth; x++) xList.add(rando.nextDouble() * yScale);
            dataGrid.add(xList);
        }
    }

private static double frac(double v) {
    v = v - Math.floor(v);
    return (v < 0) ? v + 1.0 : v;
}

private Number vertToHeight(Vert3D p) {
    if (dataGrid == null) return 0.0;

    if (!surfaceRender) {
        // cylinder path unchanged
        return findBlerpHeight(p);
    }
    switch (interpMode) {
        case BILINEAR:
        case BICUBIC: {
            // Convert to grid space: index + in-cell fraction.
            // If p.getX()/getY() are already grid-space, this still works.
            // If they are world-space, the /surfScale fixes it.
            double gx = p.xIndex + frac(p.getX() / Math.max(1.0, (double) surfScale));
            double gy = p.yIndex + frac(p.getY() / Math.max(1.0, (double) surfScale));
            return SurfaceUtils.sample(dataGrid, gx, gy, interpMode);
        }
        case NEAREST:
        default:
            return lookupPoint(p);
    }
}

    private Number lookupPoint(Vert3D p) {
        if (p.yIndex >= dataGrid.size() || p.xIndex >= dataGrid.get(0).size()) return 0.0;
        return dataGrid.get(p.yIndex).get(p.xIndex);
    }

    private Number findBlerpHeight(Vert3D p) {
        int x1Index = p.xIndex <= 0 ? 0 : p.xIndex - 1;
        if (x1Index >= dataGrid.get(0).size() - 1) x1Index = dataGrid.get(0).size() - 1;
        int x2Index = p.xIndex >= dataGrid.get(0).size() - 1 ? dataGrid.get(0).size() - 1 : p.xIndex + 1;
        int y1Index = p.yIndex <= 0 ? 0 : p.yIndex - 1;
        if (y1Index >= dataGrid.size() - 1) y1Index = dataGrid.size() - 1;
        int y2Index = p.yIndex >= dataGrid.size() - 1 ? dataGrid.size() - 1 : p.yIndex + 1;
        double c11 = dataGrid.get(y1Index).get(x1Index) * yScale;
        double c21 = dataGrid.get(y1Index).get(x2Index) * yScale;
        double c12 = dataGrid.get(y2Index).get(x1Index) * yScale;
        double c22 = dataGrid.get(y2Index).get(x2Index) * yScale;
        return quickBlerp(c11, c21, c12, c22, p.getX(), p.getY());
    }

    private Number quickBlerp(double f1, double f2, double f3, double f4, double x, double y) {
        double xratio = x - Math.floor(x);
        double yratio = y - Math.floor(y);
        double f12 = f1 + (f2 - f1) * xratio;
        double f34 = f3 + (f4 - f3) * xratio;
        return f12 + (f34 - f12) * yratio;
    }

    int vert; Point3D vertP3D;

    private void loadSurf3D() {
        LOG.info("Rendering Hypersurface Mesh...");
        generateRandos(xWidth, zWidth, yScale);
        originalGrid = deepCopyGrid(dataGrid); // NEW
        surfPlot = new HyperSurfacePlotMesh(xWidth, zWidth, 1, 1, yScale, surfScale, vert3DLookup);
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
                    if (row < featureVectors.size()) updateCalloutByFeatureVector(anchorCallout, featureVectors.get(row));
                    setSpheroidAnchor(false, row);
                }
                if (crosshairsEnabled) { paintSingleColor(Color.TRANSPARENT); illuminateCrosshair(vertP3D); }
                if (surfaceChartsEnabled) {
                    List<Double> xlist = dataGrid.get(Math.max(0, Math.min(row, dataGrid.size()-1)));
                    Double[] xRay = xlist.toArray(Double[]::new);
                    Double[] zRay = new Double[dataGrid.size()];
                    for (int i = 0; i < dataGrid.size(); i++) zRay[i] = dataGrid.get(i).get(Math.max(0, Math.min(column, dataGrid.get(0).size()-1)));
                    String text = "Coordinates: " + column + ", " + row + System.lineSeparator();
                    text = text.concat("Value: ").concat(String.valueOf(dataGrid.get(Math.max(0, Math.min(row, dataGrid.size()-1))).get(Math.max(0, Math.min(column, dataGrid.get(0).size()-1))))).concat(System.lineSeparator());
                    double maxX = xlist.stream().max(Double::compare).get(); text = text.concat("Max X: ").concat(String.valueOf(maxX)).concat(System.lineSeparator());
                    double minX = xlist.stream().min(Double::compare).get(); text = text.concat("Min X: ").concat(String.valueOf(minX)).concat(System.lineSeparator());
                    double maxZ = Arrays.stream(zRay).max(Double::compare).get(); text = text.concat("Max Z: ").concat(String.valueOf(maxZ)).concat(System.lineSeparator());
                    double minZ = Arrays.stream(zRay).min(Double::compare).get(); text = text.concat("Min Z: ").concat(String.valueOf(minZ)).concat(System.lineSeparator());
                    hoverText.setText(text); hoverText.setStrokeWidth(1); hoverText.setLayoutX(50); hoverText.setLayoutY(50);
                    scene.getRoot().fireEvent(new FactorAnalysisEvent(FactorAnalysisEvent.SURFACE_XFACTOR_VECTOR, xRay));
                    scene.getRoot().fireEvent(new FactorAnalysisEvent(FactorAnalysisEvent.SURFACE_ZFACTOR_VECTOR, zRay));
                }
                e.consume();
            }
        });

        Glow glow = new Glow(0.8);
        double poleHeight = 60; double radius = 3;
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
        eastPole.setMaterial(eastPoleMaterial); westPole.setMaterial(westPoleMaterial);
        eastKnob.setMaterial(knobMaterial); westKnob.setMaterial(knobMaterial);
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

        eastLabel = new Label("Data Index"); eastLabel.setTextFill(Color.ALICEBLUE); eastLabel.setFont(new Font("calibri", 20));
        westLabel = new Label("Data Index"); westLabel.setTextFill(Color.ALICEBLUE); westLabel.setFont(new Font("calibri", 20));
        labelGroup.getChildren().addAll(eastLabel, westLabel);
        shape3DToLabel.put(eastKnob, eastLabel); shape3DToLabel.put(westKnob, westLabel);

        scene.addEventHandler(TimelineEvent.TIMELINE_SAMPLE_INDEX, e -> {
            anchorIndex = (int) e.object;
            if (anchorIndex < 0) anchorIndex = 0; else if (anchorIndex > dataGrid.size()) anchorIndex = dataGrid.size();
            glowLineBox.setTranslateZ((anchorIndex * surfScale) - ((zWidth * surfScale) / 2.0));
            setSpheroidAnchor(true, anchorIndex);
            eastLabel.setText("Sample: " + anchorIndex + ", Neural Feature: " + xWidth);
            westLabel.setText("Sample: " + anchorIndex + ", Neural Feature: 0");
            updateLabels(); updateCalloutHeadPoints(subScene);
        });
        scene.addEventHandler(FeatureVectorEvent.SELECT_FEATURE_VECTOR, e -> {
            if (null != anchorCallout) {
                FeatureVector fv = (FeatureVector) e.object;
                updateCalloutByFeatureVector(anchorCallout, fv);
            }
        });

        extrasGroup.getChildren().addAll(eastPole, eastKnob, westPole, westKnob, glowLineBox);
        wireEventHandlers();

        pointLight.getScope().addAll(surfPlot);
        sceneRoot.getChildren().add(pointLight);
        pointLight.translateXProperty().bind(camera.translateXProperty());
        pointLight.translateYProperty().bind(camera.translateYProperty());
        pointLight.translateZProperty().bind(camera.translateZProperty().add(500));
        ambientLight.getScope().addAll(surfPlot);
        sceneRoot.getChildren().add(ambientLight);

        updateLabels();
        subScene.sceneProperty().addListener(c -> {
            Platform.runLater(() -> {
                FeatureVector dummy = FeatureVector.EMPTY_FEATURE_VECTOR("", 3);
                anchorCallout = createCallout(highlightedPoint, dummy, subScene);
                anchorCallout.play().setOnFinished(fin -> anchorCallout.setVisible(false));
            });
        });
    }
    /** 
     * Fires HypersurfaceEvent GUI sync events for all core geometry controls 
     * (xWidth, zWidth, yScale, surfScale) to synchronize GUI controls with model state.
     */
    public void syncGuiControls() {
        // Fire events to update GUI controls in the controls pane
        // These will be handled by HypersurfaceControlsPane to update Spinner values.
        fireOnRoot(HypersurfaceEvent.setXWidthGUI(xWidth));
        fireOnRoot(HypersurfaceEvent.setZWidthGUI(zWidth));
        fireOnRoot(HypersurfaceEvent.setYScaleGUI(yScale));
        fireOnRoot(HypersurfaceEvent.setSurfScaleGUI(surfScale));
    }

    /** 
     * Helper to fire on the JavaFX root, or self as fallback (copy this if not already present) 
     */
    private void fireOnRoot(Event evt) {
        if (scene != null && scene.getRoot() != null) {
            scene.getRoot().fireEvent(evt);
        } else {
            this.fireEvent(evt);
        }
    }
    
    /**
     * Sets up event handlers for HypersurfaceEvents sent from HypersurfaceControlsPane.
     * Updates all rendering state and triggers updates as needed.
     */
    private void wireEventHandlers() {
//        Scene scene = getScene();
        if (scene == null) return;
        // Geometry / scale
        scene.addEventHandler(HypersurfaceEvent.XWIDTH_CHANGED, e -> {
            this.xWidth = (int) e.object;
            if (surfPlot != null) {
                surfPlot.setTranslateX(-(xWidth * surfScale) / 2.0);
                surfPlot.setTranslateZ(-(zWidth * surfScale) / 2.0);
            }
            updateTheMesh();
        });

        scene.addEventHandler(HypersurfaceEvent.ZWIDTH_CHANGED, e -> {
            this.zWidth = (int) e.object;
            if (surfPlot != null) {
                surfPlot.setTranslateX(-(xWidth * surfScale) / 2.0);
                surfPlot.setTranslateZ(-(zWidth * surfScale) / 2.0);
            }
            updateTheMesh();
        });

        scene.addEventHandler(HypersurfaceEvent.Y_SCALE_CHANGED, e -> {
            this.yScale = ((Double) e.object).floatValue();
            if (surfPlot != null) surfPlot.setFunctionScale(yScale);
            updateTheMesh();
        });

        scene.addEventHandler(HypersurfaceEvent.SURF_SCALE_CHANGED, e -> {
            this.surfScale = ((Double) e.object).floatValue();
            if (surfPlot != null) {
                surfPlot.setRangeX(xWidth * surfScale);
                surfPlot.setRangeY(zWidth * surfScale);
                surfPlot.setTranslateX(-(xWidth * surfScale) / 2.0);
                surfPlot.setTranslateZ(-(zWidth * surfScale) / 2.0);
            }
            updateTheMesh();
        });

        // Rendering modes
        scene.addEventHandler(HypersurfaceEvent.SURFACE_RENDER_CHANGED, e -> {
            this.surfaceRender = (boolean) e.object;
            updateTheMesh();
        });
        scene.addEventHandler(HypersurfaceEvent.DRAW_MODE_CHANGED, e -> {
            if (surfPlot != null) surfPlot.setDrawMode((DrawMode) e.object);
        });
        scene.addEventHandler(HypersurfaceEvent.CULL_FACE_CHANGED, e -> {
            if (surfPlot != null) surfPlot.setCullFace((CullFace) e.object);
        });
        scene.addEventHandler(HypersurfaceEvent.COLORATION_CHANGED, e -> {
            this.colorationMethod = (Hypersurface3DPane.COLORATION) e.object;
            updateTheMesh();
        });

        // Processing pipeline
        scene.addEventHandler(HypersurfaceEvent.HEIGHT_MODE_CHANGED, e -> {
            this.heightMode = (HeightMode) e.object;
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.SMOOTHING_ENABLE_CHANGED, e -> {
            this.smoothingEnabled = (boolean) e.object;
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.SMOOTHING_METHOD_CHANGED, e -> {
            this.smoothingMethod = (SurfaceUtils.Smoothing) e.object;
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.SMOOTHING_RADIUS_CHANGED, e -> {
            this.smoothingRadius = (int) e.object;
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.GAUSSIAN_SIGMA_CHANGED, e -> {
            this.gaussianSigma = (double) e.object;
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.INTERP_MODE_CHANGED, e -> {
            this.interpMode = (SurfaceUtils.Interpolation) e.object;
            updateTheMesh();
        });
        scene.addEventHandler(HypersurfaceEvent.TONEMAP_ENABLE_CHANGED, e -> {
            this.toneEnabled = (boolean) e.object;
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.TONEMAP_OPERATOR_CHANGED, e -> {
            this.toneOperator = (SurfaceUtils.ToneMap) e.object;
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.TONEMAP_PARAM_CHANGED, e -> {
            this.toneParam = (double) e.object;
            rebuildProcessedGridAndRefresh();
        });

        // Lighting
        scene.addEventHandler(HypersurfaceEvent.AMBIENT_ENABLED_CHANGED, e -> {
            // (Optional: enable/disable ambientLight as desired)
        });
        scene.addEventHandler(HypersurfaceEvent.AMBIENT_COLOR_CHANGED, e -> {
            if (ambientLight != null) ambientLight.setColor((Color) e.object);
        });
        scene.addEventHandler(HypersurfaceEvent.POINT_ENABLED_CHANGED, e -> {
            // (Optional: enable/disable pointLight as desired)
        });
        scene.addEventHandler(HypersurfaceEvent.SPECULAR_COLOR_CHANGED, e -> {
            if (surfPlot != null && surfPlot.getMaterial() instanceof PhongMaterial mat)
                mat.setSpecularColor((Color) e.object);
        });

        // UX toggles
        scene.addEventHandler(HypersurfaceEvent.HOVER_ENABLE_CHANGED, e -> hoverInteractionsEnabled = (boolean) e.object);
        scene.addEventHandler(HypersurfaceEvent.SURFACE_CHARTS_ENABLE_CHANGED, e -> surfaceChartsEnabled = (boolean) e.object);
        scene.addEventHandler(HypersurfaceEvent.DATA_MARKERS_ENABLE_CHANGED, e -> extrasGroup.setVisible((boolean) e.object));
        scene.addEventHandler(HypersurfaceEvent.CROSSHAIRS_ENABLE_CHANGED, e -> crosshairsEnabled = (boolean) e.object);

        // Commands/actions
        scene.addEventHandler(HypersurfaceEvent.RESET_VIEW, e -> resetView(1000, false));
        scene.addEventHandler(HypersurfaceEvent.UPDATE_RENDER, e -> updateTheMesh());
        scene.addEventHandler(HypersurfaceEvent.CLEAR_DATA, e -> clearAll());
        scene.addEventHandler(HypersurfaceEvent.UNROLL_REQUESTED, e -> unrollHyperspace());
        scene.addEventHandler(HypersurfaceEvent.COMPUTE_VECTOR_DISTANCES, e -> computeVectorDistances());
        scene.addEventHandler(HypersurfaceEvent.COMPUTE_COLLECTION_DIFF, e -> computeSurfaceDifference((FeatureCollection) e.object));
        scene.addEventHandler(HypersurfaceEvent.COMPUTE_COSINE_DISTANCE, e -> computeCosineDistance((FeatureCollection) e.object));
    }
    public void updateCalloutByFeatureVector(Callout callout, FeatureVector featureVector) {
        callout.setMainTitleText(featureVector.getLabel());
        callout.mainTitleTextNode.setText(callout.getMainTitleText());
        VBox vbox = (VBox) callout.mainTitleNode;
        TitledPane tp0 = (TitledPane) vbox.getChildren().get(0);
        ImageView iv = loadImageView(featureVector, featureVector.isBBoxValid());
        Image image = iv.getImage();
        ((ImageView) tp0.getContent()).setImage(image);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : featureVector.getMetaData().entrySet()) sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
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
        xFactorIndex = 0; yFactorIndex = 1; zFactorIndex = 2;
        Platform.runLater(() -> scene.getRoot().fireEvent(
            new HyperspaceEvent(HyperspaceEvent.FACTOR_COORDINATES_KEYPRESS,
                new CoordinateSet(xFactorIndex, yFactorIndex, zFactorIndex))));
        notifyIndexChange();
        ellipsoidGroup.getChildren().clear();
        shape3DToLabel.clear();
        shape3DToLabel.put(xSphere, xLabel);
        shape3DToLabel.put(ySphere, yLabel);
        shape3DToLabel.put(zSphere, zLabel);
        shape3DToLabel.put(highlightedPoint, hoverText);
        dataGrid.clear();
        featureVectors.clear();
        originalGrid.clear();
    }

    public void showAll() { updateView(true); }

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
            new KeyFrame(Duration.seconds(0.1), e -> camera.setTranslateZ(DEFAULT_INTRO_DISTANCE)),
            new KeyFrame(Duration.seconds(0.1), new KeyValue(opacityProperty(), 0.0)),
            new KeyFrame(Duration.seconds(0.3), e -> setVisible(true)),
            new KeyFrame(Duration.seconds(0.3), new KeyValue(opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.6), e -> intro(1000))
        );
        timeline.playFromStart();
    }

    @Override public void setFeatureCollection(FeatureCollection fc) { featureVectors = fc.getFeatures(); }

    public void findClusters(ManifoldEvent.ProjectionConfig pc) {
        if (pc.dataSource != ManifoldEvent.ProjectionConfig.DATA_SOURCE.HYPERSURFACE) return;
        double[][] observations = FeatureCollection.toData(featureVectors);
        double projectionScalar = 1000.0;
        switch (pc.clusterMethod) {
            case DBSCAN -> { DBSCANClusterTask t = new DBSCANClusterTask(scene, camera, projectionScalar, observations, pc); if (!t.isCancelledByUser()) { Thread th = new Thread(t); th.setDaemon(true); th.start(); } }
            case HDDBSCAN -> { HDDBSCANClusterTask t = new HDDBSCANClusterTask(scene, camera, projectionScalar, observations, pc); if (!t.isCancelledByUser()) { Thread th = new Thread(t); th.setDaemon(true); th.start(); } }
            case KMEANS -> { KMeansClusterTask t = new KMeansClusterTask(scene, camera, projectionScalar, observations, pc); if (!t.isCancelledByUser()) { Thread th = new Thread(t); th.setDaemon(true); th.start(); } }
            case KMEDIODS -> { KMediodsClusterTask t = new KMediodsClusterTask(scene, camera, projectionScalar, observations, pc); if (!t.isCancelledByUser()) { Thread th = new Thread(t); th.setDaemon(true); th.start(); } }
            case EX_MAX -> { ExMaxClusterTask t = new ExMaxClusterTask(scene, camera, projectionScalar, observations, pc); if (!t.isCancelledByUser()) { Thread th = new Thread(t); th.setDaemon(true); th.start(); } }
            case AFFINITY -> { AffinityClusterTask t = new AffinityClusterTask(scene, camera, projectionScalar, observations, pc); if (!t.isCancelledByUser()) { Thread th = new Thread(t); th.setDaemon(true); th.start(); } }
        }
    }

    @Override
    public void addSemanticMapCollection(SemanticMapCollection semanticMapCollection) {
        SemanticReconstruction reconstruction = semanticMapCollection.getReconstruction();
        SemanticReconstructionMap rMap = reconstruction.getData_vars().getNeural_timeseries();
        List<List<Double>> neuralData = rMap.getData();
        LOG.info("Neural Data dimensions: {} entries at {} frame width.", neuralData.size(), neuralData.get(0).size());
        long startTime = System.nanoTime();
        dataGrid.clear();
        List<Double> justTheMags;
        for (List<Double> phaseMagPairs : neuralData) {
            justTheMags = new ArrayList<>(neuralData.get(0).size() / 2);
            for (int i = 0; i < phaseMagPairs.size(); i += 2) justTheMags.add(phaseMagPairs.get(i) * yScale);
            dataGrid.add(justTheMags);
        }
        LOG.info("Mapped Neural Magnitudes to Hypersurface: {}", Utils.totalTimeString(startTime));
        zWidth = neuralData.size();
        xWidth = neuralData.get(0).size() / 2;
        syncGuiControls();
        originalGrid = deepCopyGrid(dataGrid); // NEW
        rebuildProcessedGridAndRefresh();      // NEW

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

    @Override public void addSemanticMap(SemanticMap semanticMap) { throw new UnsupportedOperationException("Not supported yet."); }
    @Override public SemanticMap getSemanticMap(long id) { throw new UnsupportedOperationException("Not supported yet."); }
    @Override public void locateSemanticMap(SemanticMap semanticMap) { throw new UnsupportedOperationException("Not supported yet."); }
    @Override public void clearSemanticMaps() { throw new UnsupportedOperationException("Not supported yet."); }

    @Override
    public void addFeatureCollection(FeatureCollection featureCollection, boolean clearQueue) {
        if (null == dataGrid) dataGrid = new ArrayList<>(featureCollection.getFeatures().size()); else dataGrid.clear();
        List<Double> xList;
        for (FeatureVector fv : featureCollection.getFeatures()) {
            xList = new ArrayList<>(fv.getData().size());
            xList.addAll(fv.getData());
            dataGrid.add(xList);
        }
        zWidth = dataGrid.size();
        xWidth = dataGrid.get(0).size();
        syncGuiControls();
        originalGrid = deepCopyGrid(dataGrid); 
        rebuildProcessedGridAndRefresh();      
        getScene().getRoot().fireEvent(new CommandTerminalEvent("Hypersurface updated. ", new Font("Consolas", 20), Color.GREEN));
        featureVectors = featureCollection.getFeatures();
    }

    @Override public void addFeatureVector(FeatureVector featureVector) { featureVectors.add(featureVector); dataGrid.add(featureVector.getData()); originalGrid = deepCopyGrid(dataGrid); rebuildProcessedGridAndRefresh(); }
    @Override public void locateFeatureVector(FeatureVector featureVector) { throw new UnsupportedOperationException("Not supported yet."); }
    @Override public void clearFeatureVectors() { featureVectors.clear(); dataGrid.clear(); originalGrid.clear(); }
    @Override public List<FeatureVector> getAllFeatureVectors() { if (null == featureVectors) return Collections.EMPTY_LIST; return featureVectors; }
    @Override public void setColorByID(String iGotID, Color color) { throw new UnsupportedOperationException("Not supported yet."); }
    @Override public void setColorByIndex(int i, Color color) { throw new UnsupportedOperationException("Not supported yet."); }
    @Override public void setVisibleByIndex(int i, boolean b) { throw new UnsupportedOperationException("Not supported yet."); }
    @Override public void refresh() { refresh(true); }
    @Override public void refresh(boolean forceNodeUpdate) { updateTheMesh(); }
    @Override public void setDimensionLabels(List<String> labelStrings) { featureLabels = labelStrings; }
    @Override public void setSpheroidAnchor(boolean animate, int index) { double z = index * surfScale; }

    private void tessellateImage(Image image, int x1, int y1, int x2, int y2) {
        lastImage = image;
        long startTime = System.nanoTime();
        LOG.info("Mapping Image Raster to Feature Vector... ");
        int rows = (int) image.getHeight();
        int columns = (int) image.getWidth();
        PixelReader pr = image.getPixelReader();
        Color color = null; int rgb, r, g, b = 0; double dataValue = 0;
        if (null == dataGrid) dataGrid = new ArrayList<>(rows); else dataGrid.clear();
        featureVectors.clear();
        for (int rowIndex = y1; rowIndex < y2; rowIndex++) {
            List<Double> currentDataRow = new ArrayList<>();
            for (int colIndex = x1; colIndex < x2; colIndex++) {
                color = pr.getColor(colIndex, rowIndex);
                rgb = (pr.getArgb(colIndex, rowIndex));
                FeatureVector fv = FeatureVector.EMPTY_FEATURE_VECTOR(color.toString(), 3);
                fv.getData().set(0, (double) colIndex / columns);
                fv.getData().set(1, (double) rowIndex / rows);
                r = (rgb >> 16) & 0xFF; g = (rgb >> 8) & 0xFF; b = rgb & 0xFF;
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
        zWidth = rows; xWidth = columns;
        syncGuiControls();
        originalGrid = deepCopyGrid(dataGrid); 
        rebuildProcessedGridAndRefresh();      
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
            WritableImage wi = ResourceUtils.loadImageFile(imageryBasePath + shapleyCollection.getSourceInput());
            if (null != wi) {
                int x2 = (int) wi.getWidth(); int y2 = (int) wi.getHeight();
                tessellateImage(wi, 0, 0, x2, y2);
                lastImage = wi;
                LOG.info("injecting Shapley function values into Vertices... ");
                long startTime = System.nanoTime();
                surfPlot.functionValues.clear();
                for (int i = 0; i < shapleyVectors.size(); i++) {
                    surfPlot.functionValues.add(shapleyVectors.get(i).getData().get(0) * yScale);
                }
                Utils.printTotalTime(startTime);
                if (null == colorationMethod) surfPlot.setTextureModeVertices3D(TOTAL_COLORS, colorByHeight, 0.0, 360.0);
                switch (colorationMethod) {
                    case COLOR_BY_IMAGE -> surfPlot.setTextureModeImage(imageryBasePath + lastImageSource);
                    case COLOR_BY_FEATURE -> surfPlot.setTextureModeVertices3D(TOTAL_COLORS, colorByHeight, 0.0, 360.0);
                    default -> surfPlot.setTextureModeVertices3D(TOTAL_COLORS, colorByShapley, 0.0, 360.0);
                }
            }
        } catch (IOException ex) { LOG.error(null, ex); }
    }

    @Override public void addShapleyVector(ShapleyVector shapleyVector) { shapleyVectors.add(shapleyVector); }
    @Override public void clearShapleyVectors() { shapleyVectors.clear(); }

    // ================= helpers for processing pipeline =================
    private static List<List<Double>> deepCopyGrid(List<List<Double>> src) {
        List<List<Double>> out = new ArrayList<>(src.size());
        for (List<Double> row : src) out.add(new ArrayList<>(row));
        return out;
    }

    private void rebuildProcessedGridAndRefresh() {
        if (originalGrid == null || originalGrid.isEmpty()) return;
        List<List<Double>> g = deepCopyGrid(originalGrid);
        if (smoothingEnabled) {
            g = SurfaceUtils.smooth(g, smoothingMethod, gaussianSigma, smoothingRadius);
        }
        if (toneEnabled) {
            g = SurfaceUtils.toneMapGrid(g, toneOperator, toneParam);
        }
        dataGrid.clear(); dataGrid.addAll(g);
        xWidth = dataGrid.get(0).size(); zWidth = dataGrid.size();
        syncGuiControls();
        updateTheMesh(); updateView(true);
    }
}