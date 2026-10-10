package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.css.StyleResourceProvider;
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
import edu.jhuapl.trinity.javafx.javafx3d.XFormGroup;
import edu.jhuapl.trinity.javafx.javafx3d.animated.AnimatedSphere;
import edu.jhuapl.trinity.javafx.javafx3d.animated.Tracer;
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
import edu.jhuapl.trinity.utils.DataUtils.HeightMode;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import edu.jhuapl.trinity.utils.graph.GraphStyleParams;
import edu.jhuapl.trinity.utils.metric.Metric;
import edu.jhuapl.trinity.utils.statistics.GridDensityResult;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape3D;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.fxyz3d.scene.Skybox;
import org.fxyz3d.utils.CameraTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

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
    private static final double POINT_LIGHT_CAMERA_OFFSET_Z = 100.0;
    private static final double POINT_LIGHT_QUADRATIC_ATTENUATION = 0.000001;

    public PerspectiveCamera camera;
    public CameraTransformer cameraTransform = new CameraTransformer();
    private final HypersurfaceCameraController cameraController;
    public XFormGroup dataXForm = new XFormGroup();

    private double cameraDistance = -1000;
    private final double sceneWidth = 4000;
    private final double sceneHeight = 4000;
    private final double planeSize = sceneWidth / 2.0;


    public Group sceneRoot = new Group();
    public Group extrasGroup;
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
    private final HypersurfaceSourceModel sourceModel =
        new HypersurfaceSourceModel(DEFAULT_XWIDTH, DEFAULT_SURFSCALE);
    private final HypersurfaceProcessingController processingController =
        new HypersurfaceProcessingController(sourceModel);
    private boolean suppressRowOrientationRefresh;

    // Public collection aliases retained for compatibility. The source model owns
    // these mutable instances and all internal source-state semantics.
    public List<ShapleyVector> shapleyVectors = sourceModel.getShapleyVectors();

    private final SurfaceCrosshairOverlay surfaceCrosshairOverlay = new SurfaceCrosshairOverlay();
    private final HypersurfaceInteractionController interactionController;
    private final HypersurfaceOverlayManager overlayManager;
    /**
     * Desktop-level legend HUD. Constructed lazily on the JavaFX Application Thread
     * because Hypersurface3DPane itself is created by AppAsyncManager on a worker thread.
     */
    private HypersurfaceLegendOverlay legendOverlay;
    private final BooleanProperty legendEnabled = new SimpleBooleanProperty(true);

    public List<FeatureVector> featureVectors = sourceModel.getFeatureVectors();

    /**
     * Authoritative boxed working/source grid retained for compatibility with existing
     * analysis and ingestion paths. LOD selection must never replace this with a
     * downsampled render grid; sourceModel.getActiveHeightField() owns render-side data.
     */
    public List<List<Double>> dataGrid = sourceModel.getDataGrid();

    private Random rando = new Random();
    public HyperSurfacePlotMesh surfPlot;

    public int xWidth = DEFAULT_XWIDTH;
    public int zWidth = DEFAULT_ZWIDTH;
    public float yScale = DEFAULT_YSCALE;
    public float surfScale = DEFAULT_SURFSCALE;

    private Skybox skybox;

    BorderPane bp;

    public List<String> featureLabels = sourceModel.getFeatureLabels();
    public Scene scene;
    public String imageryBasePath = "";
    SurfaceChartPane surfaceChartPane;
    public AmbientLight ambientLight;
    public PointLight pointLight;
    private AmbientLight skyboxAmbientLight;

    // ============================================================
    // Surface renderer / LOD orchestration
    // ============================================================

    /**
     * True only while model state is being pushed into HypersurfaceControlsPane.
     * GUI control listeners echo their changes back as HypersurfaceEvents, so those
     * echoes must not be interpreted as user-requested geometry rebuilds.
     */
    private boolean syncingGuiControls = false;

    private final SurfaceCoordinateMapper coordinateMapper = new SurfaceCoordinateMapper();

    private final HypersurfaceRenderController renderController;

    // --- Graph layer support ---
    private final Group graphLayer = new Group(); // sits in sceneRoot
    private boolean graphVisible = true;
    private GraphDirectedCollection currentGraph = null;
    private Graph3DRenderer.Params graphParams = new Graph3DRenderer.Params()
        .withNodeRadius(20.0)
        .withEdgeWidth(8.0f)
        .withPositionScalar(1.0);
    // Graph visual style state (synced with GraphStyleControlsView)
    private GraphStyleParams styleParams = new GraphStyleParams();

    public Hypersurface3DPane(Scene scene) {
        this.scene = scene;
        visibleProperty().addListener((obs, oldValue, newValue) -> refreshLegendVisibility());
        // AppAsyncManager constructs this pane on a worker thread, but the Node is
        // attached to centerStack later on the JavaFX Application Thread. Defer all
        // desktop-overlay creation/attachment until that scene-attachment point.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) attachLegendOverlay(App.getAppPathPaneStack());
        });
        ambientLight = new AmbientLight(Color.WHITE);

        setBackground(Background.EMPTY);
        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        subScene.setFill(sceneColor);

        overlayManager = new HypersurfaceOverlayManager(
            this, scene, subScene, coordinateMapper, planeSize);
        extrasGroup = overlayManager.getExtrasGroup();
        interactionController = new HypersurfaceInteractionController(
            this, scene, coordinateMapper, surfaceCrosshairOverlay, overlayManager);
        camera = new PerspectiveCamera(true);

        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
        cameraTransform.ry.setAngle(-45.0);
        cameraTransform.rx.setAngle(-10.0);
        cameraController = new HypersurfaceCameraController(
            camera,
            cameraTransform,
            subScene,
            this::getCameraSurfaceBounds,
            this::handleCameraInteraction,
            this::handleCameraSettled);
        renderController = new HypersurfaceRenderController(
            this, sourceModel, processingController, surfaceCrosshairOverlay, this::refreshCoordinateMapper);
        sourceModel.rowOrientationProperty().addListener((obs, oldValue, newValue) -> {
            renderController.setRowOrientation(newValue);
            refreshCoordinateMapper();
            if (!suppressRowOrientationRefresh && surfPlot != null) {
                updateTheMesh();
            }
        });
        sourceModel.heightOrientationProperty().addListener((obs, oldValue, newValue) -> {
            renderController.setHeightOrientation(newValue);
            refreshCrosshairSurfaceContext();
            refreshLegendOverlay();
            if (surfPlot != null) {
                updateTheMesh();
            }
        });
        cameraController.installInputHandlers();
        setupSkyBox();
        debugGroup.setVisible(false);
        overlayManager.setInitialVisibility(false);
        sceneRoot.getChildren().addAll(cameraTransform, overlayManager.getHighlightedPoint(),
            overlayManager.getNodeGroup(), extrasGroup, debugGroup, dataXForm);
        // Add graph layer last so it draws above the surface (z-order within Group)
        sceneRoot.getChildren().add(graphLayer);
        sceneRoot.getChildren().add(surfaceCrosshairOverlay);
        graphLayer.setVisible(graphVisible);
        // Sync controls with current visibility on startup
        fireOnRoot(new GraphEvent(GraphEvent.SET_GRAPH_VISIBILITY_GUI, graphVisible));
        subScene.setCamera(camera);
        pointLight = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(pointLight);
        pointLight.translateXProperty().bind(camera.translateXProperty());
        pointLight.translateYProperty().bind(camera.translateYProperty());
        pointLight.translateZProperty().bind(
            camera.translateZProperty().add(POINT_LIGHT_CAMERA_OFFSET_Z));
        pointLight.setConstantAttenuation(1.0);
        pointLight.setLinearAttenuation(0.0);
        pointLight.setQuadraticAttenuation(POINT_LIGHT_QUADRATIC_ATTENUATION);

        subScene.setOnKeyPressed(event -> {
            KeyCode keycode = event.getCode();
            boolean cameraHandled = cameraController.handleKeyPressed(event);

            if (keycode == KeyCode.COMMA) {
                if (xFactorIndex > 0 && yFactorIndex > 0 && zFactorIndex > 0) {
                    xFactorIndex -= 1;
                    yFactorIndex -= 1;
                    zFactorIndex -= 1;
                    Platform.runLater(() -> scene.getRoot().fireEvent(
                        new HyperspaceEvent(HyperspaceEvent.FACTOR_COORDINATES_KEYPRESS,
                            new CoordinateSet(xFactorIndex, yFactorIndex, zFactorIndex))));
                    boolean redraw = true;
                    if (redraw) {
                        updateView(false);
                        notifyIndexChange();
                    }
                    overlayManager.updateLabels();
                }
            }
            if (keycode == KeyCode.PERIOD) {
                int featureSize = featureVectors.isEmpty() ? factorMaxIndex : featureVectors.get(0).getData().size();
                if (xFactorIndex < factorMaxIndex - 1 && yFactorIndex < factorMaxIndex - 1
                    && zFactorIndex < factorMaxIndex - 1 && xFactorIndex < featureSize - 1
                    && yFactorIndex < featureSize - 1 && zFactorIndex < featureSize - 1) {
                    xFactorIndex += 1;
                    yFactorIndex += 1;
                    zFactorIndex += 1;
                    Platform.runLater(() -> scene.getRoot().fireEvent(
                        new HyperspaceEvent(HyperspaceEvent.FACTOR_COORDINATES_KEYPRESS,
                            new CoordinateSet(xFactorIndex, yFactorIndex, zFactorIndex))));
                    boolean redraw = true;
                    if (redraw) {
                        updateView(false);
                        notifyIndexChange();
                    }
                    overlayManager.updateLabels();
                } else {
                    scene.getRoot().fireEvent(new CommandTerminalEvent("Feature Index Max Reached: ("
                        + featureSize + ")", new Font("Consolas", 20), Color.YELLOW));
                }
            }
            if (keycode == KeyCode.SLASH && event.isControlDown()) debugGroup.setVisible(!debugGroup.isVisible());
            if (keycode == KeyCode.Y) renderController.scaleHeight(1.1);
            if (keycode == KeyCode.H) renderController.scaleHeight(0.9);

            if (keycode == KeyCode.I) {
                double tz = event.isShiftDown() ? 50 : 5;
                overlayManager.moveTimeline(tz);
            }
            if (keycode == KeyCode.K) {
                double tz = event.isShiftDown() ? 50 : 5;
                overlayManager.moveTimeline(-tz);
            }

            if (!cameraHandled) {
                overlayManager.refreshProjectedOverlays();
                requestLodUpdate(LodManager.UpdateReason.OTHER);
            }
        });

        Pane pathPane = App.getAppPathPaneStack();
        surfaceChartPane = new SurfaceChartPane(scene, pathPane);
        bp = new BorderPane(subScene);
        getChildren().clear();
        getChildren().addAll(bp, overlayManager.getLabelGroup());


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
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                } catch (IOException ioe) {
                }
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
                } catch (IOException ex) {
                    LOG.error(null, ex);
                }
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
                } catch (IOException ex) {
                    LOG.error(null, ex);
                }
            }
        });

        Glow glow = new Glow(0.5);
        ImageView analysisImageView = ResourceUtils.loadIcon("analysis", ICON_FIT_HEIGHT);
        analysisImageView.setEffect(glow);
        Menu analysisMenu = new Menu("Analysis", analysisImageView,
            vectorDistanceItem, collectionDifferenceItem, cosineSimilarityItem
        );

        CheckMenuItem enableHoverItem = new CheckMenuItem("Hover Interactions");
        enableHoverItem.setOnAction(e ->
            interactionController.setHoverEnabled(enableHoverItem.isSelected()));

        CheckMenuItem surfaceChartsItem = new CheckMenuItem("Surface Charts");
        surfaceChartsItem.setOnAction(e ->
            interactionController.setSurfaceChartsEnabled(surfaceChartsItem.isSelected()));

        MenuItem navigatorItem = new MenuItem("Content Navigator");
        navigatorItem.setOnAction(e -> interactionController.showContentNavigator(
            sourceModel.isImageBackedSurface() ? sourceModel.getSourceImage() : null));

        MenuItem updateAllItem = new MenuItem("Update Render");
        updateAllItem.setOnAction(e -> updateAll());
        MenuItem clearDataItem = new MenuItem("Clear Data");
        clearDataItem.setOnAction(e -> {
            clearAll();
            xWidth = DEFAULT_XWIDTH;
            zWidth = DEFAULT_ZWIDTH;
            syncGuiControls();
            generateRandos(xWidth, zWidth, yScale);
            captureDataGridAsSource();
            rebuildProcessedGridAndRefresh();
        });

        CheckMenuItem showDataMarkersItem = new CheckMenuItem("Show Data Markers");
        showDataMarkersItem.setOnAction(e ->
            overlayManager.setDataMarkersVisible(showDataMarkersItem.isSelected()));

        CheckMenuItem enableCrosshairsItem = new CheckMenuItem("Enable Crosshairs");
        enableCrosshairsItem.setOnAction(e ->
            interactionController.setCrosshairsEnabled(enableCrosshairsItem.isSelected()));

        MenuItem resetViewItem = new MenuItem("Reset View");
        resetViewItem.setOnAction(e -> resetView(1000, false));
        ContextMenu cm = new ContextMenu(showControlsItem, navigatorItem,
            copyAsImageItem, saveSnapshotItem, unrollHyperspaceItem, analysisMenu,
            enableHoverItem, surfaceChartsItem, showDataMarkersItem, enableCrosshairsItem,
            updateAllItem, clearDataItem, resetViewItem);
        cm.setAutoFix(true);
        cm.setAutoHide(true);
        cm.setHideOnEscape(true);
        cm.setOpacity(0.85);

        subScene.setOnMouseClicked((MouseEvent e) -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (!cm.isShowing()) cm.show(this.getParent(), e.getScreenX(), e.getScreenY());
                else cm.hide();
                e.consume();
            }
        });
// Style params changed from GraphStyleControlsView
        this.scene.addEventHandler(GraphEvent.GRAPH_STYLE_PARAMS_CHANGED, e -> {
            GraphStyleParams p = (GraphStyleParams) e.object;
            if (p == null) return;

            // Update local style state
            styleParams.nodeColor = p.nodeColor;
            styleParams.nodeRadius = p.nodeRadius;
            styleParams.nodeOpacity = clamp01(p.nodeOpacity);
            styleParams.edgeColor = p.edgeColor;
            styleParams.edgeWidth = p.edgeWidth;
            styleParams.edgeOpacity = clamp01(p.edgeOpacity);

            // Apply style. Rebuild graph only if edge width changed.
            applyGraphStyle(styleParams, /*rebuildIfNeeded*/ true);
        });

        // Reset style defaults
        this.scene.addEventHandler(GraphEvent.GRAPH_STYLE_RESET_DEFAULTS, e -> {
            styleParams = new GraphStyleParams(); // back to defaults

            // Keep renderer params consistent for rebuilds
            graphParams.withNodeRadius(styleParams.nodeRadius)
                .withEdgeWidth((float) styleParams.edgeWidth);

            // Rebuild (edge width) then apply everything else live
            if (currentGraph != null) {
                graphLayer.getChildren().setAll(
                    Graph3DRenderer.buildGraphGroup(currentGraph, graphParams)
                );
            }
            applyGraphStyle(styleParams, /*rebuildIfNeeded*/ false);

            // GUI-sync so controls show defaults
            fireOnRoot(new GraphEvent(GraphEvent.SET_STYLE_GUI, styleParams));
        });

        this.scene.addEventHandler(GraphEvent.NEW_GRAPHDIRECTED_COLLECTION, e -> {
            if (!(e.object instanceof GraphDirectedCollection gc)) return;
            currentGraph = gc;
            interactionController.setGraph(gc);
            graphLayer.getChildren().clear();
            graphLayer.getChildren().add(Graph3DRenderer.buildGraphGroup(gc, graphParams));

            // Apply current style to the freshly built graph
            applyGraphStyle(styleParams, /*rebuildIfNeeded*/ false);

            // GUI-sync so pickers/sliders reflect the active style
            fireOnRoot(new GraphEvent(GraphEvent.SET_STYLE_GUI, styleParams));
            fireOnRoot(new GraphEvent(GraphEvent.SET_GRAPH_VISIBILITY_GUI, graphVisible));
            scene.getRoot().fireEvent(new CommandTerminalEvent(
                "Rendered 3D graph: nodes=" + gc.getNodes().size() + ", edges=" + gc.getEdges().size(),
                new Font("Consolas", 18), Color.LIGHTGREEN));
        });

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
            int x2 = (int) image.getWidth();
            int y2 = (int) image.getHeight();
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
                String DIALOGCSS = StyleResourceProvider.getResource("dialogstyles.css").toExternalForm();
                dialogPane.getStylesheets().add(DIALOGCSS);                
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
            sourceModel.setSourceImage(image);
        });
        this.scene.addEventHandler(HyperspaceEvent.FACTOR_COORDINATES_GUI, e -> {
            CoordinateSet coords = (CoordinateSet) e.object;
            xFactorIndex = coords.coordinateIndices.get(0);
            yFactorIndex = coords.coordinateIndices.get(1);
            zFactorIndex = coords.coordinateIndices.get(2);
            overlayManager.updateLabels();
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
            } else factorMaxIndex = newFactorMaxIndex;
        });

        scene.addEventHandler(HypersurfaceGridEvent.RENDER_PDF, e -> {
            applySurfaceGridToHypersurface(e.getZGrid());
            e.consume();
        });
        scene.addEventHandler(HypersurfaceGridEvent.RENDER_CDF, e -> {
            applySurfaceGridToHypersurface(e.getZGrid());
            e.consume();
        });

        scene.addEventHandler(HyperspaceEvent.NODE_QUEUELIMIT_GUI, e -> queueLimit = (int) e.object);
        scene.addEventHandler(HyperspaceEvent.REFRESH_RATE_GUI, e -> hypersurfaceRefreshRate = (long) e.object);

        scene.addEventHandler(ShadowEvent.SHOW_AXES_LABELS, e ->
            overlayManager.setAxesAndLabelsVisible((boolean) e.object));
        scene.addEventHandler(ApplicationEvent.SET_IMAGERY_BASEPATH, e -> imageryBasePath = (String) e.object);
        Platform.runLater(() -> {
            overlayManager.updateLabels();
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
                    captureDataGridAsSource();
                    rebuildProcessedGridAndRefresh();
                } else if (animated || isDirty) {
                    startTime = System.nanoTime();
                    updateTheMesh();
                    LOG.info("updateTheMesh(): {}", Utils.totalTimeString(startTime));
                }
            }
        };
        surfUpdateAnimationTimer.start();
    }

    /**
     * Apply style to current graph. Rebuild only if edge-width changed and requested.
     */
    private void applyGraphStyle(GraphStyleParams p, boolean rebuildIfNeeded) {
        if (p == null) return;

        // Determine if edge width differs from the built state
        double currentEdgeWidth = graphParams.edgeWidth; // float in params, promoted to double
        boolean needRebuild = Math.abs(p.edgeWidth - currentEdgeWidth) > 1e-6;

        if (needRebuild && rebuildIfNeeded && currentGraph != null) {
            // Update params and rebuild to reflect edge width + node radius
            graphParams.withNodeRadius(p.nodeRadius)
                .withEdgeWidth((float) p.edgeWidth);
            graphLayer.getChildren().setAll(
                Graph3DRenderer.buildGraphGroup(currentGraph, graphParams)
            );
        }

        // Apply color/opacity/radius live to existing nodes/edges
        for (Node n : graphLayer.getChildren()) {
            applyGraphStyleRecursive(n, p);
        }
    }

    private void applyGraphStyleRecursive(Node n, GraphStyleParams p) {
        if (n instanceof AnimatedSphere s) {
            // color
            if (p.nodeColor != null) {
                s.setColor(new Color(
                    p.nodeColor.getRed(),
                    p.nodeColor.getGreen(),
                    p.nodeColor.getBlue(),
                    // keep whatever alpha the sphere currently has; set below
                    s.getPhongMaterial().getDiffuseColor() != null
                        ? s.getPhongMaterial().getDiffuseColor().getOpacity()
                        : 1.0
                ));
            }
            // radius
            s.setSphereRadius(p.nodeRadius);
            // opacity via material alpha
            s.setMaterialOpacity(p.nodeOpacity);

        } else if (n instanceof Tracer t) {
            // color
            if (p.edgeColor != null) {
                t.setDiffuseColor(new Color(
                    p.edgeColor.getRed(),
                    p.edgeColor.getGreen(),
                    p.edgeColor.getBlue(),
                    // keep current alpha; set below
                    1.0
                ));
            }
            // opacity via material alpha
            t.setOpacityAlpha(p.edgeOpacity);

        } else if (n instanceof Parent parent) {
            for (Node c : parent.getChildrenUnmodifiable()) {
                applyGraphStyleRecursive(c, p);
            }
        }
    }


    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
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
        LOG.info("{}", cosineDistancesGrid);
    }

    private void applySurfaceGridToHypersurface(List<List<Double>> grid) {
        double userScale = 1.0; // future: user control
        List<List<Double>> scaled = processingController.normalizeAndScale(grid, userScale);
        dataGrid.clear();
        dataGrid.addAll(scaled);
        captureDataGridAsSource();
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
        captureDataGridAsSource();
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
        captureDataGridAsSource();
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
        captureDataGridAsSource();
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
        overlayManager.updateCalloutHeadPoint(node, callout, subScene);
    }

    public void updateCalloutHeadPoints(SubScene subScene) {
        overlayManager.updateCalloutHeadPoints(subScene);
    }

    public Callout createCallout(Shape3D shape3D, FeatureVector featureVector, SubScene subScene) {
        return overlayManager.createCallout(shape3D, featureVector, subScene);
    }

    public void addCallout(Callout callout, Shape3D shape3D) {
        overlayManager.addCallout(callout, shape3D);
    }

    public void updateTheMesh() {
        renderController.updateMesh();
    }

    private void applyCurrentColoration() {
        renderController.applyCurrentColoration();
    }

    private void setupSkyBox() {
        Image top = new Image(ImageResourceProvider.getResource("darkmetalbottom.png").toExternalForm());
        Image bottom = new Image(ImageResourceProvider.getResource("darkmetalbottom.png").toExternalForm());
        Image left = new Image(ImageResourceProvider.getResource("1500_blackgrid.png").toExternalForm());
        Image right = new Image(ImageResourceProvider.getResource("1500_blackgrid.png").toExternalForm());
        Image front = new Image(ImageResourceProvider.getResource("1500_blackgrid.png").toExternalForm());
        Image back = new Image(ImageResourceProvider.getResource("1500_blackgrid.png").toExternalForm());
        double size = 100000D;
        skybox = new Skybox(top, bottom, left, right, front, back, size, camera);
        sceneRoot.getChildren().add(skybox);
        skyboxAmbientLight = new AmbientLight(Color.WHITE);
        skyboxAmbientLight.getScope().add(skybox);
        sceneRoot.getChildren().add(skyboxAmbientLight);
        skybox.setVisible(false);
    }

    private void notifyIndexChange() {
        getScene().getRoot().fireEvent(new CommandTerminalEvent("X,Y,Z Indices = ("
            + xFactorIndex + ", " + yFactorIndex + ", " + zFactorIndex + ")",
            new Font("Consolas", 20), Color.GREEN));
    }

    public void resetView(double milliseconds, boolean rightNow) {
        cameraController.reset(rightNow ? 0.0 : milliseconds);
    }

    /**
     * Fits the complete current surface while preserving the current view orientation.
     */
    public void fitCameraView(double milliseconds) {
        cameraController.fit(milliseconds);
    }

    /**
     * Applies a canonical camera orientation and fits the complete current surface.
     */
    public void applyCameraPreset(HypersurfaceCameraController.Preset preset, double milliseconds) {
        cameraController.applyPreset(preset, milliseconds);
    }

    public void intro(double milliseconds) {
        cameraController.intro(milliseconds, DEFAULT_INTRO_DISTANCE);
    }

    public void outtro(double milliseconds) {
        cameraController.outtro(milliseconds, DEFAULT_INTRO_DISTANCE);
    }

    public void updateAll() {
        Platform.runLater(() -> updateView(true));
    }

    private void handleCameraInteraction(HypersurfaceCameraController.InteractionType interactionType) {
        overlayManager.refreshProjectedOverlays();
        LodManager.UpdateReason reason = switch (interactionType) {
            case DRAG -> LodManager.UpdateReason.DRAG;
            case ZOOM -> LodManager.UpdateReason.SCROLL;
            case KEYBOARD -> LodManager.UpdateReason.OTHER;
        };
        requestLodUpdate(reason);
    }

    private void handleCameraSettled() {
        overlayManager.refreshProjectedOverlays();
        forceLodUpdate();
    }

    HeightField getActiveHeightFieldForInteraction() {
        return sourceModel.getActiveHeightField();
    }

    List<FeatureVector> getFeatureVectorsForInteraction() {
        return featureVectors;
    }

    List<String> getFeatureLabelsForInteraction() {
        return featureLabels;
    }

    TiledSurfaceRenderer getTiledSurfaceRendererForInteraction() {
        return renderController.getTiledSurfaceRenderer();
    }

    Image getSourceImageForInteraction() {
        return sourceModel.getSourceImage();
    }

    int getImageSourceStartXForInteraction() {
        return sourceModel.getImageSourceStartX();
    }

    int getImageSourceStartYForInteraction() {
        return sourceModel.getImageSourceStartY();
    }

    void showSurfaceChartsPane() {
        Pane pathPane = App.getAppPathPaneStack();
        if (surfaceChartPane == null) {
            surfaceChartPane = new SurfaceChartPane(scene, pathPane);
            surfaceChartPane.visibleProperty().bind(visibleProperty());
        }
        if (!pathPane.getChildren().contains(surfaceChartPane)) {
            pathPane.getChildren().add(surfaceChartPane);
            surfaceChartPane.slideInPane();
        } else {
            surfaceChartPane.show();
        }
    }

    public void updateView(boolean forcePNodeUpdate) {
        if (null != surfPlot) {
            Platform.runLater(() -> {
                if (heightChanged) {
                    heightChanged = false;
                }
                isDirty = false;
            });
        }
    }

    private void generateRandos(int xWidth, int zWidth, float yScale) {
        dataGrid.clear();
        List<Double> xList;
        for (int z = 0; z < zWidth; z++) {
            xList = new ArrayList<>(xWidth);
            for (int x = 0; x < xWidth; x++) xList.add(rando.nextDouble() * yScale);
            dataGrid.add(xList);
        }
    }

    private void configureLightingScopes() {
        // Keep the camera-following point light focused on the analytical surface/graph.
        pointLight.getScope().setAll(
            surfPlot, renderController.getTiledSurfaceRenderer(), graphLayer);

        // Ambient lighting also owns the auxiliary 3D markers. Because this light uses
        // explicit scope, omitting these groups leaves their ordinary PhongMaterials
        // effectively unlit (for example the X/Y/Z axis spheres and timeline markers).
        ambientLight.getScope().setAll(
            surfPlot,
            renderController.getTiledSurfaceRenderer(),
            graphLayer,
            overlayManager.getNodeGroup(),
            extrasGroup,
            debugGroup,
            overlayManager.getHighlightedPoint(),
            surfaceCrosshairOverlay);
    }

    private void loadSurf3D() {
        LOG.info("Rendering Hypersurface Mesh...");
        generateRandos(xWidth, zWidth, yScale);
        captureDataGridAsSource();

        surfPlot = renderController.initializeRenderers();
        interactionController.installSurfacePicking(
            surfPlot, renderController.getTiledSurfaceRenderer());

        // Build processed pyramid and apply initial LOD. The primitive source cache is
        // created by rebuildProcessedGridAndRefresh() from the current source snapshot.
        rebuildProcessedGridAndRefresh();

        overlayManager.initializeTimelineMarkers();

        wireEventHandlers();

        configureLightingScopes();
        sceneRoot.getChildren().add(ambientLight);

        overlayManager.updateLabels();
    }

    /**
     * Fires HypersurfaceEvent GUI sync events for all core geometry controls
     * (xWidth, zWidth, yScale, surfScale) to synchronize GUI controls with model state.
     */
    public void syncGuiControls() {
        // SET_*_GUI events update Spinner values synchronously. Those Spinner value
        // listeners echo XWIDTH_CHANGED/ZWIDTH_CHANGED/etc. back through the Scene.
        // Mark the entire model -> GUI synchronization window so those echoes do not
        // trigger geometry rebuilds.
        syncingGuiControls = true;
        try {
            fireOnRoot(HypersurfaceEvent.setXWidthGUI(xWidth));
            fireOnRoot(HypersurfaceEvent.setZWidthGUI(zWidth));
            fireOnRoot(HypersurfaceEvent.setYScaleGUI(yScale));
            fireOnRoot(HypersurfaceEvent.setSurfScaleGUI(surfScale));
        } finally {
            syncingGuiControls = false;
        }
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
        if (scene == null) return;
        // Geometry / scale
        scene.addEventHandler(HypersurfaceEvent.XWIDTH_CHANGED, e -> {
            if (syncingGuiControls) return;
            this.xWidth = (int) e.object;
            renderController.updateRawSurfaceTranslation();
            refreshCoordinateMapper();
            updateTheMesh();
        });

        scene.addEventHandler(HypersurfaceEvent.ZWIDTH_CHANGED, e -> {
            if (syncingGuiControls) return;
            this.zWidth = (int) e.object;
            renderController.updateRawSurfaceTranslation();
            refreshCoordinateMapper();
            updateTheMesh();
        });

        scene.addEventHandler(HypersurfaceEvent.Y_SCALE_CHANGED, e -> {
            if (syncingGuiControls) return;
            this.yScale = ((Double) e.object).floatValue();
            renderController.setFunctionScale(yScale);
            refreshCrosshairSurfaceContext();
            refreshLegendOverlay();
            updateTheMesh();
        });

        scene.addEventHandler(HypersurfaceEvent.SURF_SCALE_CHANGED, e -> {
            if (syncingGuiControls) return;
            this.surfScale = ((Double) e.object).floatValue();
            refreshWorldExtentsAndLodMetadata();
            updateTheMesh();
        });

        // Rendering modes
        scene.addEventHandler(HypersurfaceEvent.SURFACE_RENDER_CHANGED, e -> {
            this.surfaceRender = (boolean) e.object;
            updateTheMesh();
        });
        scene.addEventHandler(HypersurfaceEvent.DRAW_MODE_CHANGED, e -> {
            renderController.setDrawMode((DrawMode) e.object);
        });
        scene.addEventHandler(HypersurfaceEvent.CULL_FACE_CHANGED, e -> {
            renderController.setCullFace((CullFace) e.object);
        });
        scene.addEventHandler(HypersurfaceEvent.COLORATION_CHANGED, e -> {
            COLORATION previous = this.colorationMethod;
            COLORATION next = (Hypersurface3DPane.COLORATION) e.object;
            this.colorationMethod = next;
            renderController.handleColorationChanged(previous, next);
            refreshLegendOverlay();
        });

        // Processing pipeline
        scene.addEventHandler(HypersurfaceEvent.HEIGHT_MODE_CHANGED, e -> {
            processingController.setHeightMode((HeightMode) e.object);
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.SMOOTHING_ENABLE_CHANGED, e -> {
            processingController.setSmoothingEnabled((boolean) e.object);
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.SMOOTHING_METHOD_CHANGED, e -> {
            processingController.setSmoothingMethod((SurfaceUtils.Smoothing) e.object);
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.SMOOTHING_RADIUS_CHANGED, e -> {
            processingController.setSmoothingRadius((int) e.object);
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.GAUSSIAN_SIGMA_CHANGED, e -> {
            processingController.setGaussianSigma((double) e.object);
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.INTERP_MODE_CHANGED, e -> {
            processingController.setInterpolationMode((SurfaceUtils.Interpolation) e.object);
            updateTheMesh();
        });
        scene.addEventHandler(HypersurfaceEvent.TONEMAP_ENABLE_CHANGED, e -> {
            processingController.setToneEnabled((boolean) e.object);
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.TONEMAP_OPERATOR_CHANGED, e -> {
            processingController.setToneOperator((SurfaceUtils.ToneMap) e.object);
            rebuildProcessedGridAndRefresh();
        });
        scene.addEventHandler(HypersurfaceEvent.TONEMAP_PARAM_CHANGED, e -> {
            processingController.setToneParam((double) e.object);
            rebuildProcessedGridAndRefresh();
        });

        // Lighting
        scene.addEventHandler(HypersurfaceEvent.AMBIENT_ENABLED_CHANGED, e -> {
            if (ambientLight != null) ambientLight.setLightOn((boolean) e.object);
        });
        scene.addEventHandler(HypersurfaceEvent.AMBIENT_COLOR_CHANGED, e -> {
            if (ambientLight != null) ambientLight.setColor((Color) e.object);
        });
        scene.addEventHandler(HypersurfaceEvent.POINT_ENABLED_CHANGED, e -> {
            if (pointLight != null) pointLight.setLightOn((boolean) e.object);
        });
        scene.addEventHandler(HypersurfaceEvent.POINT_COLOR_CHANGED, e -> {
            if (pointLight != null) pointLight.setColor((Color) e.object);
        });
        scene.addEventHandler(HypersurfaceEvent.SPECULAR_COLOR_CHANGED, e -> {
            renderController.setSpecularColor((Color) e.object);
        });

        // UX toggles
        scene.addEventHandler(HypersurfaceEvent.HOVER_ENABLE_CHANGED, e ->
            interactionController.setHoverEnabled((boolean) e.object));
        scene.addEventHandler(HypersurfaceEvent.SURFACE_CHARTS_ENABLE_CHANGED, e ->
            interactionController.setSurfaceChartsEnabled((boolean) e.object));
        scene.addEventHandler(HypersurfaceEvent.DATA_MARKERS_ENABLE_CHANGED, e ->
            overlayManager.setExtrasVisible((boolean) e.object));
        scene.addEventHandler(HypersurfaceEvent.CROSSHAIRS_ENABLE_CHANGED, e ->
            interactionController.setCrosshairsEnabled((boolean) e.object));

        // Commands/actions
        scene.addEventHandler(HypersurfaceEvent.RESET_VIEW, e -> resetView(1000, false));
        scene.addEventHandler(HypersurfaceEvent.UPDATE_RENDER, e -> updateTheMesh());
        scene.addEventHandler(HypersurfaceEvent.CLEAR_DATA, e -> clearAll());
        scene.addEventHandler(HypersurfaceEvent.UNROLL_REQUESTED, e -> unrollHyperspace());
        scene.addEventHandler(HypersurfaceEvent.COMPUTE_VECTOR_DISTANCES, e -> computeVectorDistances());
        scene.addEventHandler(HypersurfaceEvent.COMPUTE_COLLECTION_DIFF, e -> computeSurfaceDifference((FeatureCollection) e.object));
        scene.addEventHandler(HypersurfaceEvent.COMPUTE_COSINE_DISTANCE, e -> computeCosineDistance((FeatureCollection) e.object));
        scene.addEventHandler(GraphEvent.GRAPH_VISIBILITY_CHANGED, e -> {
            if (!(e.object instanceof Boolean b)) return;
            graphVisible = b;
            graphLayer.setVisible(graphVisible);
        });
    }

    private void attachLegendOverlay(Pane desktopPane) {
        if (desktopPane == null) return;
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> attachLegendOverlay(desktopPane));
            return;
        }

        if (legendOverlay == null) {
            legendOverlay = new HypersurfaceLegendOverlay();
            legendOverlay.setMouseTransparent(true);
        }

        if (!legendOverlay.layoutXProperty().isBound()) {
            legendOverlay.layoutXProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(12.0,
                    desktopPane.getWidth() - legendOverlay.getWidth() - 24.0),
                desktopPane.widthProperty(),
                legendOverlay.widthProperty()));
        }
        if (!legendOverlay.layoutYProperty().isBound()) {
            legendOverlay.layoutYProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(12.0,
                    desktopPane.getHeight() * 0.45 - legendOverlay.getHeight() * 0.5),
                desktopPane.heightProperty(),
                legendOverlay.heightProperty()));
        }
        if (!desktopPane.getChildren().contains(legendOverlay)) {
            // Keep passive HUD overlays below floating LitPathPane windows.
            desktopPane.getChildren().add(0, legendOverlay);
        }
        refreshLegendVisibility();
        refreshLegendOverlay();
    }

    private HypersurfaceLegendState buildLegendState() {
        HeightField rangeField = sourceModel.getLodProcessedLevels() != null && !sourceModel.getLodProcessedLevels().isEmpty()
            ? sourceModel.getLodProcessedLevels().get(0)
            : sourceModel.getActiveHeightField();

        double minimum = Double.NaN;
        double maximum = Double.NaN;
        if (rangeField != null) {
            float[] minMax = rangeField.minMax();
            minimum = minMax[0];
            maximum = minMax[1];
        }

        String heightLabel = sourceModel.isImageBackedSurface() ? "Image Intensity" : "Feature Value";
        String colorLabel = switch (colorationMethod) {
            case COLOR_BY_IMAGE -> "Source Image";
            case COLOR_BY_FEATURE -> "Feature Value";
            case COLOR_BY_SHAPLEY -> "Shapley Value";
        };
        boolean numericColorRange = colorationMethod == COLORATION.COLOR_BY_FEATURE
            && Double.isFinite(minimum)
            && Double.isFinite(maximum);

        return new HypersurfaceLegendState(
            heightLabel,
            minimum,
            maximum,
            yScale,
            getSurfaceHeightOrientation(),
            colorationMethod,
            colorLabel,
            numericColorRange,
            minimum,
            maximum);
    }

    private void refreshLegendOverlay() {
        final HypersurfaceLegendState state = buildLegendState();
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> applyLegendState(state));
            return;
        }
        applyLegendState(state);
    }

    private void applyLegendState(HypersurfaceLegendState state) {
        if (legendOverlay != null) legendOverlay.update(state);
    }

    private void refreshLegendVisibility() {
        final boolean show = isVisible() && legendEnabled.get();
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> {
                if (legendOverlay != null) legendOverlay.setVisible(show);
            });
            return;
        }
        if (legendOverlay != null) legendOverlay.setVisible(show);
    }

    public boolean isLegendEnabled() {
        return legendEnabled.get();
    }

    public void setLegendEnabled(boolean enabled) {
        legendEnabled.set(enabled);
        refreshLegendVisibility();
        if (enabled) refreshLegendOverlay();
    }

    public BooleanProperty legendEnabledProperty() {
        return legendEnabled;
    }

    public void updateCalloutByFeatureVector(Callout callout, FeatureVector featureVector) {
        overlayManager.updateCalloutByFeatureVector(callout, featureVector);
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
        overlayManager.resetLabelTracking();
        dataGrid.clear();
        featureVectors.clear();
        sourceModel.clearOriginalGrid();
        interactionController.resetSelectionState();
        resetLodDataState();
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
            new KeyFrame(Duration.seconds(0.1), e -> cameraController.setCameraDistance(DEFAULT_INTRO_DISTANCE)),
            new KeyFrame(Duration.seconds(0.1), new KeyValue(opacityProperty(), 0.0)),
            new KeyFrame(Duration.seconds(0.3), e -> setVisible(true)),
            new KeyFrame(Duration.seconds(0.3), new KeyValue(opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.6), e -> intro(1000))
        );
        timeline.playFromStart();
    }

    @Override
    public void setFeatureCollection(FeatureCollection fc) {
        featureVectors.clear();
        featureVectors.addAll(fc.getFeatures());
        interactionController.resetSelectionState();
    }

    public void findClusters(ManifoldEvent.ProjectionConfig pc) {
        if (pc.dataSource != ManifoldEvent.ProjectionConfig.DATA_SOURCE.HYPERSURFACE) return;
        double[][] observations = FeatureCollection.toData(featureVectors);
        double projectionScalar = 1000.0;
        switch (pc.clusterMethod) {
            case DBSCAN -> {
                DBSCANClusterTask t = new DBSCANClusterTask(scene, camera, projectionScalar, observations, pc);
                if (!t.isCancelledByUser()) {
                    Thread th = new Thread(t);
                    th.setDaemon(true);
                    th.start();
                }
            }
            case HDDBSCAN -> {
                HDDBSCANClusterTask t = new HDDBSCANClusterTask(scene, camera, projectionScalar, observations, pc);
                if (!t.isCancelledByUser()) {
                    Thread th = new Thread(t);
                    th.setDaemon(true);
                    th.start();
                }
            }
            case KMEANS -> {
                KMeansClusterTask t = new KMeansClusterTask(scene, camera, projectionScalar, observations, pc);
                if (!t.isCancelledByUser()) {
                    Thread th = new Thread(t);
                    th.setDaemon(true);
                    th.start();
                }
            }
            case KMEDIODS -> {
                KMediodsClusterTask t = new KMediodsClusterTask(scene, camera, projectionScalar, observations, pc);
                if (!t.isCancelledByUser()) {
                    Thread th = new Thread(t);
                    th.setDaemon(true);
                    th.start();
                }
            }
            case EX_MAX -> {
                ExMaxClusterTask t = new ExMaxClusterTask(scene, camera, projectionScalar, observations, pc);
                if (!t.isCancelledByUser()) {
                    Thread th = new Thread(t);
                    th.setDaemon(true);
                    th.start();
                }
            }
            case AFFINITY -> {
                AffinityClusterTask t = new AffinityClusterTask(scene, camera, projectionScalar, observations, pc);
                if (!t.isCancelledByUser()) {
                    Thread th = new Thread(t);
                    th.setDaemon(true);
                    th.start();
                }
            }
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
        captureDataGridAsSource();
        rebuildProcessedGridAndRefresh();      // NEW

        overlayManager.updateAxisExtents();
        overlayManager.updateTimelineGeometry(surfPlot.getMaxAbsY() * 2);
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
        dataGrid.clear();
        List<Double> xList;
        for (FeatureVector fv : featureCollection.getFeatures()) {
            xList = new ArrayList<>(fv.getData().size());
            xList.addAll(fv.getData());
            dataGrid.add(xList);
        }
        zWidth = dataGrid.size();
        xWidth = dataGrid.get(0).size();
        syncGuiControls();
        captureDataGridAsSource();
        rebuildProcessedGridAndRefresh();
        getScene().getRoot().fireEvent(new CommandTerminalEvent("Hypersurface updated. ", new Font("Consolas", 20), Color.GREEN));
        featureVectors.clear();
        featureVectors.addAll(featureCollection.getFeatures());
        interactionController.resetSelectionState();
    }

    @Override
    public void addFeatureVector(FeatureVector featureVector) {
        featureVectors.add(featureVector);
        dataGrid.add(featureVector.getData());
        captureDataGridAsSource();
        rebuildProcessedGridAndRefresh();
    }

    @Override
    public void locateFeatureVector(FeatureVector featureVector) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clearFeatureVectors() {
        featureVectors.clear();
        dataGrid.clear();
        sourceModel.clearOriginalGrid();
        interactionController.resetSelectionState();
        resetLodDataState();
    }

    @Override
    public List<FeatureVector> getAllFeatureVectors() {
        if (null == featureVectors) return Collections.EMPTY_LIST;
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
        featureLabels.clear();
        if (labelStrings != null) featureLabels.addAll(labelStrings);
    }

    @Override
    public void setSpheroidAnchor(boolean animate, int index) {
        double z = index * surfScale;
    }

    private void tessellateImage(Image image, int x1, int y1, int x2, int y2) {
        final int imageWidth = (int) image.getWidth();
        final int imageHeight = (int) image.getHeight();
        final int startX = Math.max(0, Math.min(x1, imageWidth));
        final int startY = Math.max(0, Math.min(y1, imageHeight));
        final int endX = Math.max(startX, Math.min(x2, imageWidth));
        final int endY = Math.max(startY, Math.min(y2, imageHeight));
        final int sourceWidth = endX - startX;
        final int sourceHeight = endY - startY;

        if (sourceWidth <= 0 || sourceHeight <= 0) {
            LOG.warn("Ignoring empty image tessellation region: ({}, {}) to ({}, {}) for {}x{} image",
                x1, y1, x2, y2, imageWidth, imageHeight);
            return;
        }

        long startTime = System.nanoTime();
        System.out.println("Mapping Image Raster directly to capped primitive L0: "
            + sourceWidth + "x" + sourceHeight);

        PixelReader pixelReader = image.getPixelReader();
        if (pixelReader == null) {
            LOG.warn("Unable to tessellate image because PixelReader is unavailable.");
            return;
        }

        // Validate the incoming image first, then release every large render-side object
        // derived from the previous image before allocating the new full-resolution
        // primitive height array. This minimizes the transient heap peak when replacing
        // 4K/8K sources. The method parameter retains the new Image while sourceModel.getSourceImage() and
        // the tiled renderer are allowed to release the old one.
        releaseImageBackedStateForReplacement();

        // Retain source-region metadata, but do not allocate a full-resolution
        // HeightField for large images. Build the capped raw L0 directly from PixelReader.
        HeightField rawL0 = processingController.buildImageRawL0(
            pixelReader, startX, startY, sourceWidth, sourceHeight);
        sourceModel.activateImageSource(
            image, startX, startY, sourceWidth, sourceHeight, rawL0);
        interactionController.resetSelectionState();
        setSourceDefaultRowOrientation(SurfaceRowOrientation.FIRST_ROW_FAR);

        Utils.printTotalTime(startTime);
        System.out.println("Injecting primitive image HeightField into Hypersurface...");
        startTime = System.nanoTime();

        xWidth = sourceWidth;
        zWidth = sourceHeight;
        syncGuiControls();
        rebuildProcessedGridAndRefresh();
        overlayManager.updateAxisExtents();
        interactionController.publishSourceImageChanged(image);
        Utils.printTotalTime(startTime);
    }

    @Override
    public void addShapleyCollection(ShapleyCollection shapleyCollection) {
        shapleyVectors.clear();
        shapleyVectors.addAll(shapleyCollection.getValues());
        try {
            WritableImage wi = ResourceUtils.loadImageFile(imageryBasePath + shapleyCollection.getSourceInput());
            if (null != wi) {
                int x2 = (int) wi.getWidth();
                int y2 = (int) wi.getHeight();
                tessellateImage(wi, 0, 0, x2, y2);
                sourceModel.setSourceImage(wi);
                LOG.info("injecting Shapley function values into Vertices... ");
                long startTime = System.nanoTime();
                renderController.updateShapleyFunctionValues(shapleyVectors, yScale);
                Utils.printTotalTime(startTime);
                if (colorationMethod == COLORATION.COLOR_BY_SHAPLEY) {
                    // Rebuild through the legacy metadata path so p.f remains available.
                    updateTheMesh();
                } else {
                    applyCurrentColoration();
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

    // ================= source-model orchestration =================

    /**
     * Releases renderer-side objects derived from the previous image, then drops
     * the corresponding image/HeightField state from the source model.
     */
    private void releaseImageBackedStateForReplacement() {
        renderController.releaseImageBackedRenderStateForReplacement();
        sourceModel.releaseImageBackedStateForReplacement();
    }

    /** Snapshot the current boxed working grid as the authoritative data source. */
    private void captureDataGridAsSource() {
        boolean wasImageBacked = sourceModel.captureDataGridAsSource();
        if (wasImageBacked) {
            setSourceDefaultRowOrientation(SurfaceRowOrientation.FIRST_ROW_NEAR);
        }
        if (dataGrid.isEmpty()) {
            resetLodDataState();
        }
    }

    private void resetLodDataState() {
        sourceModel.resetLodDataState();
        setSourceDefaultRowOrientation(SurfaceRowOrientation.FIRST_ROW_NEAR);
        renderController.resetLodRenderState();
        refreshLegendOverlay();
    }

    public boolean isImageBackedSurface() {
        return sourceModel.isImageBackedSurface();
    }

    public int getRenderL0Width() {
        return sourceModel.getRenderL0Width(xWidth);
    }

    public int getRenderL0Height() {
        return sourceModel.getRenderL0Height(zWidth);
    }

    public int getSourceWidth() {
        return sourceModel.getSourceWidth(xWidth);
    }

    public int getSourceHeight() {
        return sourceModel.getSourceHeight(zWidth);
    }

    public int getRenderWidth() {
        return sourceModel.getRenderWidth(xWidth);
    }

    public int getRenderHeight() {
        return sourceModel.getRenderHeight(zWidth);
    }

    private HypersurfaceCameraController.SurfaceBounds getCameraSurfaceBounds() {
        double maxAbsY = 0.0;
        HeightField activeField = sourceModel.getActiveHeightField();
        HeightField processingField = sourceModel.getProcessingSourceHeightField();
        if (activeField != null) {
            float[] minMax = activeField.minMax();
            maxAbsY = Math.max(Math.abs(minMax[0] * yScale), Math.abs(minMax[1] * yScale));
        } else if (processingField != null) {
            float[] minMax = processingField.minMax();
            maxAbsY = Math.max(Math.abs(minMax[0] * yScale), Math.abs(minMax[1] * yScale));
        } else {
            maxAbsY = renderController.getSurfaceMaxAbsY();
        }

        double worldHeight = Math.max(1.0, maxAbsY * 2.0);
        return new HypersurfaceCameraController.SurfaceBounds(
            getWorldWidth(), worldHeight, getWorldDepth());
    }

    public double getWorldWidth() {
        return sourceModel.getWorldWidth(xWidth, zWidth, surfScale);
    }

    public double getWorldDepth() {
        return sourceModel.getWorldDepth(xWidth, zWidth, surfScale);
    }

    private void refreshCoordinateMapper() {
        coordinateMapper.configure(
            getSourceWidth(),
            getSourceHeight(),
            getRenderWidth(),
            getRenderHeight(),
            getWorldWidth(),
            getWorldDepth(),
            getSurfaceRowOrientation());
        refreshCrosshairSurfaceContext();
    }

    private void refreshCrosshairSurfaceContext() {
        surfaceCrosshairOverlay.configureSurface(
            sourceModel.getActiveHeightField(),
            coordinateMapper,
            yScale,
            getSurfaceRowOrientation(),
            getSurfaceHeightOrientation());
    }

    double getActiveRenderValue(int row, int column) {
        return sourceModel.getActiveRenderValue(row, column);
    }

    List<Double> getActiveRenderRow(int row) {
        return sourceModel.getActiveRenderRow(row);
    }

    Double[] getActiveRenderColumn(int column) {
        return sourceModel.getActiveRenderColumn(column);
    }

    /** Recompute world extents after surfScale changes without rebuilding source data. */
    private void refreshWorldExtentsAndLodMetadata() {
        renderController.refreshWorldExtentsAndLodMetadata();
    }

    /**
     * Returns the long-axis world extent used for image-backed surfaces at the default
     * surface scale. Image aspect ratio determines the other axis.
     */
    public double getNominalImageWorldExtent() {
        return sourceModel.getNominalImageWorldExtent();
    }

    /**
     * Sets the long-axis world extent used for image-backed surfaces at the default
     * surface scale. This changes physical display size, not source or LOD resolution.
     */
    public void setNominalImageWorldExtent(double nominalImageWorldExtent) {
        if (Double.compare(sourceModel.getNominalImageWorldExtent(), nominalImageWorldExtent) == 0) return;
        sourceModel.setNominalImageWorldExtent(nominalImageWorldExtent);
        if (sourceModel.isImageBackedSurface()
            && sourceModel.getProcessingSourceHeightField() != null) {
            refreshWorldExtentsAndLodMetadata();
            updateTheMesh();
        }
    }

    public SurfaceRowOrientation getSurfaceRowOrientation() {
        return sourceModel.getRowOrientation();
    }

    public void setSurfaceRowOrientation(SurfaceRowOrientation orientation) {
        sourceModel.setRowOrientation(orientation);
    }

    public ObjectProperty<SurfaceRowOrientation> surfaceRowOrientationProperty() {
        return sourceModel.rowOrientationProperty();
    }

    private void setSourceDefaultRowOrientation(SurfaceRowOrientation orientation) {
        suppressRowOrientationRefresh = true;
        try {
            setSurfaceRowOrientation(orientation);
        } finally {
            suppressRowOrientationRefresh = false;
        }
    }

    public SurfaceHeightOrientation getSurfaceHeightOrientation() {
        return sourceModel.getHeightOrientation();
    }

    public void setSurfaceHeightOrientation(SurfaceHeightOrientation orientation) {
        sourceModel.setHeightOrientation(orientation);
    }

    public ObjectProperty<SurfaceHeightOrientation> surfaceHeightOrientationProperty() {
        return sourceModel.heightOrientationProperty();
    }

    public boolean isTiledHeightFieldRenderingEnabled() {
        return renderController.isTiledHeightFieldRenderingEnabled();
    }

    public void setTiledHeightFieldRenderingEnabled(boolean enabled) {
        renderController.setTiledHeightFieldRenderingEnabled(enabled);
    }

    public int getTileCellsL0() {
        return renderController.getTileCellsL0();
    }

    public TiledLodManager.Config getTiledLodConfigCopy() {
        return renderController.getTiledLodConfigCopy();
    }

    public void setTiledLodConfig(TiledLodManager.Config config) {
        renderController.setTiledLodConfig(config);
    }

    public TiledSurfaceRenderer.LodStatistics getTiledLodStatistics() {
        return renderController.getTiledLodStatistics();
    }

    public void setTileCellsL0(int tileCellsL0) {
        renderController.setTileCellsL0(tileCellsL0);
    }

    public int getMaxRenderResolution() {
        return processingController.getMaxRenderResolution();
    }

    public void setMaxRenderResolution(int maxRenderResolution) {
        if (!processingController.setMaxRenderResolution(maxRenderResolution)) return;
        processingController.rebuildCurrentImageRawL0();
        rebuildProcessedGridAndRefresh();
    }

    public int getMinRenderResolution() {
        return processingController.getMinRenderResolution();
    }

    public void setMinRenderResolution(int minRenderResolution) {
        if (!processingController.setMinRenderResolution(minRenderResolution)) return;
        rebuildProcessedGridAndRefresh();
    }

    public boolean isTiledHeightFieldRendererActive() {
        return renderController.isTiledHeightFieldRendererActive();
    }

    private void requestLodUpdate(LodManager.UpdateReason reason) {
        renderController.requestLodUpdate(reason);
    }

    private void forceLodUpdate() {
        renderController.forceLodUpdate();
    }

    private void rebuildProcessedGridAndRefresh() {
        if (!processingController.rebuildProcessedPyramid(xWidth, zWidth, surfScale)) return;

        TiledSurfaceRenderer tiledRenderer = renderController.getTiledSurfaceRenderer();
        if (sourceModel.isImageBackedSurface()
            && (tiledRenderer == null || tiledRenderer.isVerboseDiagnosticsEnabled())) {
            System.out.println(processingController.buildPyramidSummary());
        }

        refreshCoordinateMapper();
        renderController.activateProcessedPyramid();
        updateView(true);
        refreshLegendOverlay();
    }

}
