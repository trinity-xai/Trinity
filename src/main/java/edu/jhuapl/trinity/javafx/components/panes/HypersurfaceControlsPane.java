package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.GraphControlsView;
import edu.jhuapl.trinity.javafx.components.GraphStyleControlsView;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.HypersurfaceEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Hypersurface3DPane;
import edu.jhuapl.trinity.javafx.javafx3d.SurfaceHeightOrientation;
import edu.jhuapl.trinity.javafx.javafx3d.SurfaceRowOrientation;
import edu.jhuapl.trinity.javafx.javafx3d.SurfaceUtils;
import edu.jhuapl.trinity.javafx.javafx3d.TiledLodManager;
import edu.jhuapl.trinity.javafx.javafx3d.TiledSurfaceRenderer;
import edu.jhuapl.trinity.utils.DataUtils.HeightMode;
import javafx.animation.AnimationTimer;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;

public class HypersurfaceControlsPane extends LitPathPane {

    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 640;
    private static final int TAB_CONTENT_HEIGHT = PANEL_HEIGHT - 96;

    public static double SPINNER_PREF_WIDTH = 125.0;
    public static double COMBO_PREF_WIDTH = 220.0;
    public static double CHECKBOX_PREF_WIDTH = 100.0;
    public static double COLOR_PICKER_PREF_WIDTH = 150.0;

    private final Hypersurface3DPane target;

    // Core controls
    private Spinner<Double> yScaleSpinner;
    private Spinner<Double> surfScaleSpinner;
    private Spinner<Integer> xWidthSpinner;
    private Spinner<Integer> zWidthSpinner;
    private Label xWidthLabel;
    private Label zWidthLabel;
    private Label sourceDimensionsCaption;
    private Label sourceDimensionsLabel;
    private Label renderL0DimensionsCaption;
    private Label renderL0DimensionsLabel;
    private Label worldDimensionsCaption;
    private Label worldDimensionsLabel;

    // Rendering
    private ComboBox<String> meshTypeCombo;
    private ComboBox<DrawMode> drawModeCombo;
    private ComboBox<CullFace> cullFaceCombo;
    private ComboBox<Hypersurface3DPane.COLORATION> colorationCombo;
    private ComboBox<SurfaceRowOrientation> rowOrientationCombo;
    private ComboBox<SurfaceHeightOrientation> heightOrientationCombo;

    // Processing
    private ComboBox<HeightMode> heightModeCombo;
    private CheckBox enableSmoothingCheck;
    private ComboBox<SurfaceUtils.Smoothing> smoothingCombo;
    private Spinner<Integer> smoothingRadiusSpinner;
    private Spinner<Double> gaussianSigmaSpinner;
    private ComboBox<SurfaceUtils.Interpolation> interpCombo;
    private CheckBox enableToneMapCheck;
    private ComboBox<SurfaceUtils.ToneMap> toneMapCombo;
    private Spinner<Double> toneParamSpinner;

    // Scene / Lighting
    private ColorPicker bgPicker;
    private CheckBox skyboxCheck;
    private CheckBox enableAmbientCheck;
    private ColorPicker ambientColorPicker;
    private CheckBox enablePointCheck;
    private ColorPicker pointColorPicker;

    // Graph overlay visibility
    private ToggleButton showGraphToggle;

    // Tiled LOD controls
    private ComboBox<Integer> tileSizeCombo;
    private ComboBox<String> maxDetailCombo;
    private Spinner<Double> targetPixelsSpinner;
    private Spinner<Double> coarsenThresholdSpinner;
    private Spinner<Double> refineThresholdSpinner;
    private Spinner<Integer> throttleMsSpinner;
    private Spinner<Integer> settleMsSpinner;
    private Spinner<Integer> initialSettleMsSpinner;
    private Spinner<Integer> activeTransitionsSpinner;
    private Spinner<Integer> settledTransitionsSpinner;
    private Spinner<Integer> activeBuildsSpinner;
    private Spinner<Integer> settledBuildsSpinner;
    private CheckBox geometryBudgetCheck;
    private CheckBox verboseLodDiagnosticsCheck;
    private Spinner<Double> triangleBudgetSpinner;

    private Label lodStatusLabel;
    private Label visibleTilesLabel;
    private Label lod0CountLabel;
    private Label lod1CountLabel;
    private Label lod2CountLabel;
    private Label lod3CountLabel;
    private Label lod4CountLabel;
    private Label triangleCountLabel;
    private Label rawRequestedTrianglesLabel;
    private Label budgetTargetTrianglesLabel;
    private Label budgetCoarsenedLabel;
    private Label pendingTransitionsLabel;
    private AnimationTimer lodDiagnosticsTimer;

    public HypersurfaceControlsPane(Scene scene, Pane parent, Hypersurface3DPane target) {
        super(scene, parent, PANEL_WIDTH, PANEL_HEIGHT, new BorderPane(), "Hypersurface Controls", "", 200.0, 300.0);
        this.scene = scene;
        this.target = target;

        setPickOnBounds(false);
        setFocusTraversable(false);

        BorderPane bp = (BorderPane) this.contentPane;
        bp.setPadding(new Insets(6));
        bp.setCenter(buildTabs());

        // --- GUI sync from model → controls
        scene.addEventHandler(HypersurfaceEvent.SET_XWIDTH_GUI, e -> {
            if (xWidthSpinner != null && e.object instanceof Integer value) {
                if (xWidthSpinner.getValueFactory() instanceof SpinnerValueFactory.IntegerSpinnerValueFactory vf
                    && value > vf.getMax()) {
                    vf.setMax(value);
                }
                if (!xWidthSpinner.getValue().equals(value)) {
                    xWidthSpinner.getValueFactory().setValue(value);
                }
            }
            e.consume();
        });
        scene.addEventHandler(HypersurfaceEvent.SET_ZWIDTH_GUI, e -> {
            if (zWidthSpinner != null && e.object instanceof Integer value) {
                if (zWidthSpinner.getValueFactory() instanceof SpinnerValueFactory.IntegerSpinnerValueFactory vf
                    && value > vf.getMax()) {
                    vf.setMax(value);
                }
                if (!zWidthSpinner.getValue().equals(value)) {
                    zWidthSpinner.getValueFactory().setValue(value);
                }
            }
            e.consume();
        });
        scene.addEventHandler(HypersurfaceEvent.SET_YSCALE_GUI, e -> {
            if (yScaleSpinner != null && !yScaleSpinner.getValue().equals(e.object)) {
                yScaleSpinner.getValueFactory().setValue((Double) e.object);
            }
            e.consume();
        });
        scene.addEventHandler(HypersurfaceEvent.SET_SURFSCALE_GUI, e -> {
            if (surfScaleSpinner != null && !surfScaleSpinner.getValue().equals(e.object)) {
                surfScaleSpinner.getValueFactory().setValue((Double) e.object);
            }
            e.consume();
        });

        // Graph overlay visibility GUI sync
        scene.addEventHandler(GraphEvent.SET_GRAPH_VISIBILITY_GUI, e -> {
            if (showGraphToggle != null) {
                boolean v = (Boolean) e.object;
                if (showGraphToggle.isSelected() != v) showGraphToggle.setSelected(v);
            }
            e.consume();
        });

        startLodDiagnosticsTimer();
    }

    private TabPane buildTabs() {
        // Initial values
        double yScale0 = (target != null) ? target.yScale : 5.0;
        double surfScale0 = (target != null) ? target.surfScale : 5.0;
        int xWidth0 = (target != null) ? target.xWidth : Hypersurface3DPane.DEFAULT_XWIDTH;
        int zWidth0 = (target != null) ? target.zWidth : Hypersurface3DPane.DEFAULT_ZWIDTH;
        Color bg0 = (target != null) ? target.sceneColor : Color.BLACK;

        // === Dimensions ===
        GridPane dimsGrid = formGrid();
        xWidthSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 4000, xWidth0, 4));
        zWidthSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 4000, zWidth0, 10));
        yScaleSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 500.0, yScale0, 1.0));
        surfScaleSpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 100.0, surfScale0, 1.0));
        styleSpinner(xWidthSpinner);
        styleSpinner(zWidthSpinner);
        styleSpinner(yScaleSpinner);
        styleSpinner(surfScaleSpinner);

        xWidthSpinner.setEditable(true);
        zWidthSpinner.setEditable(true);
        yScaleSpinner.setEditable(true);
        surfScaleSpinner.setEditable(true);

        xWidthLabel = new Label("X width");
        zWidthLabel = new Label("Z length");
        sourceDimensionsCaption = new Label("Source dimensions");
        sourceDimensionsLabel = new Label("-");
        renderL0DimensionsCaption = new Label("Render L0");
        renderL0DimensionsLabel = new Label("-");
        worldDimensionsCaption = new Label("World dimensions");
        worldDimensionsLabel = new Label("-");

        addRow(dimsGrid, 0, xWidthLabel, xWidthSpinner);
        addRow(dimsGrid, 1, zWidthLabel, zWidthSpinner);
        addRow(dimsGrid, 2, sourceDimensionsCaption, sourceDimensionsLabel);
        addRow(dimsGrid, 3, renderL0DimensionsCaption, renderL0DimensionsLabel);
        addRow(dimsGrid, 4, worldDimensionsCaption, worldDimensionsLabel);
        addRow(dimsGrid, 5, "Y scale", yScaleSpinner);
        addRow(dimsGrid, 6, "Range scale", surfScaleSpinner);
        refreshDimensionControls();

        // Core: Spinner value listeners
        xWidthSpinner.valueProperty().addListener((obs, oldVal, newVal) ->
            fireOnRoot(HypersurfaceEvent.xWidth(newVal)));
        zWidthSpinner.valueProperty().addListener((obs, oldVal, newVal) ->
            fireOnRoot(HypersurfaceEvent.zWidth(newVal)));
        yScaleSpinner.valueProperty().addListener((obs, oldVal, newVal) ->
            fireOnRoot(HypersurfaceEvent.yScale(newVal)));
        surfScaleSpinner.valueProperty().addListener((obs, oldVal, newVal) ->
            fireOnRoot(HypersurfaceEvent.surfScale(newVal)));

        // === Rendering ===
        GridPane renderGrid = formGrid();
        meshTypeCombo = new ComboBox<>();
        meshTypeCombo.getItems().addAll("Surface", "Cylindrical");
        meshTypeCombo.getSelectionModel().select("Surface");
        styleCombo(meshTypeCombo);
        addRow(renderGrid, 0, "Mesh", meshTypeCombo);

        drawModeCombo = new ComboBox<>();
        drawModeCombo.getItems().addAll(DrawMode.LINE, DrawMode.FILL);
        drawModeCombo.getSelectionModel().select(DrawMode.FILL);
        styleCombo(drawModeCombo);
        addRow(renderGrid, 1, "Draw", drawModeCombo);

        cullFaceCombo = new ComboBox<>();
        cullFaceCombo.getItems().addAll(CullFace.FRONT, CullFace.BACK, CullFace.NONE);
        cullFaceCombo.getSelectionModel().select(CullFace.BACK);
        styleCombo(cullFaceCombo);
        addRow(renderGrid, 2, "Cull", cullFaceCombo);

        colorationCombo = new ComboBox<>();
        colorationCombo.getItems().addAll(
            Hypersurface3DPane.COLORATION.COLOR_BY_IMAGE,
            Hypersurface3DPane.COLORATION.COLOR_BY_FEATURE,
            Hypersurface3DPane.COLORATION.COLOR_BY_SHAPLEY
        );
        colorationCombo.getSelectionModel().select(Hypersurface3DPane.COLORATION.COLOR_BY_FEATURE);
        styleCombo(colorationCombo);
        addRow(renderGrid, 3, "Color", colorationCombo);

        rowOrientationCombo = new ComboBox<>();
        rowOrientationCombo.getItems().addAll(SurfaceRowOrientation.values());
        rowOrientationCombo.getSelectionModel().select(target != null
            ? target.getSurfaceRowOrientation()
            : SurfaceRowOrientation.FIRST_ROW_NEAR);
        styleCombo(rowOrientationCombo);
        addRow(renderGrid, 4, "Row orientation", rowOrientationCombo);

        heightOrientationCombo = new ComboBox<>();
        heightOrientationCombo.getItems().addAll(SurfaceHeightOrientation.values());
        heightOrientationCombo.getSelectionModel().select(target != null
            ? target.getSurfaceHeightOrientation()
            : SurfaceHeightOrientation.HIGH_VALUES_UP);
        styleCombo(heightOrientationCombo);
        addRow(renderGrid, 5, "Height orientation", heightOrientationCombo);

        meshTypeCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.surfaceRender("Surface".equals(meshTypeCombo.getValue()))));
        drawModeCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.drawMode(drawModeCombo.getValue())));
        cullFaceCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.cullFace(cullFaceCombo.getValue())));
        colorationCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.coloration(colorationCombo.getValue())));
        rowOrientationCombo.setOnAction(e -> {
            if (target != null && rowOrientationCombo.getValue() != null) {
                target.setSurfaceRowOrientation(rowOrientationCombo.getValue());
            }
        });
        heightOrientationCombo.setOnAction(e -> {
            if (target != null && heightOrientationCombo.getValue() != null) {
                target.setSurfaceHeightOrientation(heightOrientationCombo.getValue());
            }
        });
        if (target != null) {
            target.surfaceRowOrientationProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null && rowOrientationCombo.getValue() != newValue) {
                    rowOrientationCombo.getSelectionModel().select(newValue);
                }
            });
            target.surfaceHeightOrientationProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null && heightOrientationCombo.getValue() != newValue) {
                    heightOrientationCombo.getSelectionModel().select(newValue);
                }
            });
        }

        // === Scene / Lighting ===
        GridPane sceneGrid = formGrid();
        bgPicker = new ColorPicker(bg0);
        styleColorPicker(bgPicker);
        skyboxCheck = new CheckBox("Skybox");
        styleCheck(skyboxCheck);

        addRow(sceneGrid, 0, "Background", bgPicker);
        addRow(sceneGrid, 1, "Sky", skyboxCheck);

        bgPicker.setOnAction(e ->
            fireOnRoot(new HyperspaceEvent(HyperspaceEvent.HYPERSPACE_BACKGROUND_COLOR, bgPicker.getValue())));
        skyboxCheck.setOnAction(e ->
            fireOnRoot(new HyperspaceEvent(HyperspaceEvent.ENABLE_HYPERSPACE_SKYBOX, skyboxCheck.isSelected())));

        enableAmbientCheck = new CheckBox("Ambient");
        enableAmbientCheck.setSelected(true);
        styleCheck(enableAmbientCheck);
        ambientColorPicker = new ColorPicker(Color.WHITE);
        styleColorPicker(ambientColorPicker);

        HBox ambientBox = new HBox(6, enableAmbientCheck, ambientColorPicker);
        addRow(sceneGrid, 2, "Ambient", ambientBox);

        enableAmbientCheck.setOnAction(e -> {
            boolean on = enableAmbientCheck.isSelected();
            ambientColorPicker.setDisable(!on);
            fireOnRoot(HypersurfaceEvent.ambientEnabled(on));
        });
        ambientColorPicker.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.ambientColor(ambientColorPicker.getValue())));

        enablePointCheck = new CheckBox("Point light");
        enablePointCheck.setSelected(true);
        styleCheck(enablePointCheck);
        pointColorPicker = new ColorPicker(Color.WHITE);
        styleColorPicker(pointColorPicker);
        HBox pointBox = new HBox(6, enablePointCheck, pointColorPicker);
        addRow(sceneGrid, 3, "Point Light", pointBox);

        enablePointCheck.setOnAction(e -> {
            boolean on = enablePointCheck.isSelected();
            pointColorPicker.setDisable(!on);
            fireOnRoot(HypersurfaceEvent.pointEnabled(on));
        });
        pointColorPicker.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.pointColor(pointColorPicker.getValue())));

        // --- Graph Overlay visibility ---
        GridPane graphOverlayGrid = formGrid();
        showGraphToggle = new ToggleButton("Show Graph");
        showGraphToggle.setSelected(true); // will be synced on startup via SET_GRAPH_VISIBILITY_GUI
        showGraphToggle.setOnAction(e ->
            fireOnRoot(new GraphEvent(GraphEvent.GRAPH_VISIBILITY_CHANGED, showGraphToggle.isSelected()))
        );
        addRow(graphOverlayGrid, 0, "Graph Overlay", showGraphToggle);

        VBox viewTabContent = new VBox(10,
            titledBox("Dimensions", dimsGrid),
            titledBox("Rendering", renderGrid),
            titledBox("Scene", sceneGrid),
            titledBox("Graph Overlay", graphOverlayGrid)
        );
        viewTabContent.setPadding(new Insets(6));

        // === Processing tab ===
        GridPane heightGrid = formGrid();
        heightModeCombo = new ComboBox<>();
        heightModeCombo.getItems().addAll(HeightMode.values());
        heightModeCombo.getSelectionModel().select(HeightMode.RAW);
        styleCombo(heightModeCombo);
        addRow(heightGrid, 0, "Height mode", heightModeCombo);
        heightModeCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.heightMode(heightModeCombo.getValue())));

        GridPane smoothingGrid = formGrid();
        enableSmoothingCheck = new CheckBox("Enable");
        styleCheck(enableSmoothingCheck);
        smoothingCombo = new ComboBox<>();
        smoothingCombo.getItems().addAll(SurfaceUtils.Smoothing.values());
        smoothingCombo.getSelectionModel().select(SurfaceUtils.Smoothing.GAUSSIAN);
        styleCombo(smoothingCombo);
        smoothingRadiusSpinner = new Spinner<>(1, 25, 2, 1);
        styleSpinner(smoothingRadiusSpinner);
        gaussianSigmaSpinner = new Spinner<>(0.10, 10.0, 1.0, 0.10);
        styleSpinner(gaussianSigmaSpinner);

        smoothingRadiusSpinner.setEditable(true);
        gaussianSigmaSpinner.setEditable(true);

        addRow(smoothingGrid, 0, "Smoothing", enableSmoothingCheck);
        addRow(smoothingGrid, 1, "Method", smoothingCombo);
        addRow(smoothingGrid, 2, "Radius", smoothingRadiusSpinner);
        addRow(smoothingGrid, 3, "Sigma", gaussianSigmaSpinner);

        enableSmoothingCheck.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.smoothingEnabled(enableSmoothingCheck.isSelected())));
        smoothingCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.smoothingMethod(smoothingCombo.getValue())));
        smoothingRadiusSpinner.valueProperty().addListener((o, ov, nv) ->
            fireOnRoot(HypersurfaceEvent.smoothingRadius(nv)));
        gaussianSigmaSpinner.valueProperty().addListener((o, ov, nv) ->
            fireOnRoot(HypersurfaceEvent.gaussianSigma(nv)));

        GridPane interpGrid = formGrid();
        interpCombo = new ComboBox<>();
        interpCombo.getItems().addAll(SurfaceUtils.Interpolation.values());
        interpCombo.getSelectionModel().select(SurfaceUtils.Interpolation.NEAREST);
        styleCombo(interpCombo);
        addRow(interpGrid, 0, "Mode", interpCombo);
        interpCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.interp(interpCombo.getValue())));

        GridPane toneGrid = formGrid();
        enableToneMapCheck = new CheckBox("Enable");
        styleCheck(enableToneMapCheck);
        toneMapCombo = new ComboBox<>();
        toneMapCombo.getItems().addAll(SurfaceUtils.ToneMap.values());
        toneMapCombo.getSelectionModel().select(SurfaceUtils.ToneMap.NONE);
        styleCombo(toneMapCombo);
        toneParamSpinner = new Spinner<>(0.10, 10.0, 2.0, 0.10);
        styleSpinner(toneParamSpinner);

        toneParamSpinner.setEditable(true);

        addRow(toneGrid, 0, "Tone map", enableToneMapCheck);
        addRow(toneGrid, 1, "Operator", toneMapCombo);
        addRow(toneGrid, 2, "k / γ", toneParamSpinner);

        enableToneMapCheck.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.toneEnabled(enableToneMapCheck.isSelected())));
        toneMapCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.toneOperator(toneMapCombo.getValue())));
        toneParamSpinner.valueProperty().addListener((o, ov, nv) ->
            fireOnRoot(HypersurfaceEvent.toneParam(nv)));

        VBox procTabContent = new VBox(10,
            titledBox("Height", heightGrid),
            titledBox("Smoothing", smoothingGrid),
            titledBox("Interpolation", interpGrid),
            titledBox("Tone Mapping", toneGrid)
        );
        procTabContent.setPadding(new Insets(6));

        // === LOD tab ===
        TiledLodManager.Config lodConfig = target != null
            ? target.getTiledLodConfigCopy()
            : new TiledLodManager.Config();

        GridPane tileGrid = formGrid();
        tileSizeCombo = new ComboBox<>();
        tileSizeCombo.getItems().addAll(128, 256, 512);
        int tileSize0 = target != null ? target.getTileCellsL0() : 256;
        if (!tileSizeCombo.getItems().contains(tileSize0)) {
            tileSizeCombo.getItems().add(tileSize0);
        }
        tileSizeCombo.getSelectionModel().select(Integer.valueOf(tileSize0));
        styleCombo(tileSizeCombo);

        maxDetailCombo = new ComboBox<>();
        maxDetailCombo.getItems().addAll("L0", "L1", "L2", "L3", "L4");
        int finest0 = Math.max(0, Math.min(4, lodConfig.finestAllowedLod));
        maxDetailCombo.getSelectionModel().select(finest0);
        styleCombo(maxDetailCombo);

        addRow(tileGrid, 0, "Tile size", tileSizeCombo);
        addRow(tileGrid, 1, "Maximum detail", maxDetailCombo);

        GridPane selectionGrid = formGrid();
        targetPixelsSpinner = new Spinner<>(0.25, 5.0,
            clampDouble(lodConfig.targetPixelsPerCell, 0.25, 5.0), 0.10);
        coarsenThresholdSpinner = new Spinner<>(0.25, 5.0,
            clampDouble(lodConfig.lowThreshold, 0.25, 5.0), 0.10);
        refineThresholdSpinner = new Spinner<>(0.25, 5.0,
            clampDouble(lodConfig.highThreshold, 0.25, 5.0), 0.10);
        styleSpinner(targetPixelsSpinner);
        styleSpinner(coarsenThresholdSpinner);
        styleSpinner(refineThresholdSpinner);
        targetPixelsSpinner.setEditable(true);
        coarsenThresholdSpinner.setEditable(true);
        refineThresholdSpinner.setEditable(true);

        addRow(selectionGrid, 0, "Target px/cell", targetPixelsSpinner);
        addRow(selectionGrid, 1, "Coarsen below", coarsenThresholdSpinner);
        addRow(selectionGrid, 2, "Refine above", refineThresholdSpinner);

        GridPane budgetGrid = formGrid();
        geometryBudgetCheck = new CheckBox();
        geometryBudgetCheck.setSelected(lodConfig.geometryBudgetEnabled);
        triangleBudgetSpinner = new Spinner<>(0.25, 50.0,
            clampDouble(lodConfig.triangleBudget / 1_000_000.0, 0.25, 50.0), 0.25);
        styleSpinner(triangleBudgetSpinner);
        triangleBudgetSpinner.setEditable(true);
        addRow(budgetGrid, 0, "Enable budget", geometryBudgetCheck);
        addRow(budgetGrid, 1, "Triangle budget (M)", triangleBudgetSpinner);

        GridPane timingGrid = formGrid();
        throttleMsSpinner = new Spinner<>(0, 1000, clampInt(lodConfig.throttleMs, 0, 1000), 5);
        settleMsSpinner = new Spinner<>(0, 2000, clampInt(lodConfig.debounceMs, 0, 2000), 10);
        initialSettleMsSpinner = new Spinner<>(0, 3000,
            clampInt(lodConfig.initialSettleMs, 0, 3000), 25);
        activeTransitionsSpinner = new Spinner<>(1, 32,
            clampInt(lodConfig.activeTransitionsPerPulse, 1, 32), 1);
        settledTransitionsSpinner = new Spinner<>(1, 64,
            clampInt(lodConfig.settledTransitionsPerPulse, 1, 64), 1);
        activeBuildsSpinner = new Spinner<>(0, 8,
            clampInt(lodConfig.activeBuildsPerPulse, 0, 8), 1);
        settledBuildsSpinner = new Spinner<>(1, 16,
            clampInt(lodConfig.settledBuildsPerPulse, 1, 16), 1);

        styleSpinner(throttleMsSpinner);
        styleSpinner(settleMsSpinner);
        styleSpinner(initialSettleMsSpinner);
        styleSpinner(activeTransitionsSpinner);
        styleSpinner(settledTransitionsSpinner);
        styleSpinner(activeBuildsSpinner);
        styleSpinner(settledBuildsSpinner);

        throttleMsSpinner.setEditable(true);
        settleMsSpinner.setEditable(true);
        initialSettleMsSpinner.setEditable(true);
        activeTransitionsSpinner.setEditable(true);
        settledTransitionsSpinner.setEditable(true);
        activeBuildsSpinner.setEditable(true);
        settledBuildsSpinner.setEditable(true);

        addRow(timingGrid, 0, "Update throttle", throttleMsSpinner);
        addRow(timingGrid, 1, "Camera settle", settleMsSpinner);
        addRow(timingGrid, 2, "Initial settle", initialSettleMsSpinner);
        addRow(timingGrid, 3, "Active transitions", activeTransitionsSpinner);
        addRow(timingGrid, 4, "Settled transitions", settledTransitionsSpinner);
        addRow(timingGrid, 5, "Active builds", activeBuildsSpinner);
        addRow(timingGrid, 6, "Settled builds", settledBuildsSpinner);

        GridPane diagnosticsGrid = formGrid();
        lodStatusLabel = new Label("Inactive");
        visibleTilesLabel = new Label("0 / 0");
        lod0CountLabel = new Label("0");
        lod1CountLabel = new Label("0");
        lod2CountLabel = new Label("0");
        lod3CountLabel = new Label("0");
        lod4CountLabel = new Label("0");
        triangleCountLabel = new Label("0");
        rawRequestedTrianglesLabel = new Label("0");
        budgetTargetTrianglesLabel = new Label("0");
        budgetCoarsenedLabel = new Label("0");
        pendingTransitionsLabel = new Label("0");
        verboseLodDiagnosticsCheck = new CheckBox();
        verboseLodDiagnosticsCheck.setSelected(lodConfig.verboseDiagnostics);

        addRow(diagnosticsGrid, 0, "Renderer", lodStatusLabel);
        addRow(diagnosticsGrid, 1, "Visible tiles", visibleTilesLabel);
        addRow(diagnosticsGrid, 2, "L0 tiles", lod0CountLabel);
        addRow(diagnosticsGrid, 3, "L1 tiles", lod1CountLabel);
        addRow(diagnosticsGrid, 4, "L2 tiles", lod2CountLabel);
        addRow(diagnosticsGrid, 5, "L3 tiles", lod3CountLabel);
        addRow(diagnosticsGrid, 6, "L4 tiles", lod4CountLabel);
        addRow(diagnosticsGrid, 7, "Current triangles", triangleCountLabel);
        addRow(diagnosticsGrid, 8, "Raw requested", rawRequestedTrianglesLabel);
        addRow(diagnosticsGrid, 9, "Budget target", budgetTargetTrianglesLabel);
        addRow(diagnosticsGrid, 10, "Budget-coarsened", budgetCoarsenedLabel);
        addRow(diagnosticsGrid, 11, "Pending", pendingTransitionsLabel);
        addRow(diagnosticsGrid, 12, "Verbose console", verboseLodDiagnosticsCheck);

        tileSizeCombo.setOnAction(e -> {
            if (target != null && tileSizeCombo.getValue() != null) {
                target.setTileCellsL0(tileSizeCombo.getValue());
            }
        });
        maxDetailCombo.setOnAction(e -> applyLodConfigFromControls());
        targetPixelsSpinner.valueProperty().addListener((o, ov, nv) -> applyLodConfigFromControls());
        coarsenThresholdSpinner.valueProperty().addListener((o, ov, nv) -> applyLodConfigFromControls());
        refineThresholdSpinner.valueProperty().addListener((o, ov, nv) -> applyLodConfigFromControls());
        geometryBudgetCheck.setOnAction(e -> applyLodConfigFromControls());
        triangleBudgetSpinner.valueProperty().addListener((o, ov, nv) -> applyLodConfigFromControls());
        throttleMsSpinner.valueProperty().addListener((o, ov, nv) -> applyLodConfigFromControls());
        settleMsSpinner.valueProperty().addListener((o, ov, nv) -> applyLodConfigFromControls());
        initialSettleMsSpinner.valueProperty().addListener((o, ov, nv) -> applyLodConfigFromControls());
        activeTransitionsSpinner.valueProperty().addListener((o, ov, nv) -> applyLodConfigFromControls());
        settledTransitionsSpinner.valueProperty().addListener((o, ov, nv) -> applyLodConfigFromControls());
        activeBuildsSpinner.valueProperty().addListener((o, ov, nv) -> applyLodConfigFromControls());
        settledBuildsSpinner.valueProperty().addListener((o, ov, nv) -> applyLodConfigFromControls());
        verboseLodDiagnosticsCheck.setOnAction(e -> applyLodConfigFromControls());

        VBox lodTabContent = new VBox(10,
            titledBox("Tile Layout", tileGrid),
            titledBox("LOD Selection", selectionGrid),
            titledBox("Geometry Budget", budgetGrid),
            titledBox("Scheduling", timingGrid),
            titledBox("Live Diagnostics", diagnosticsGrid)
        );
        lodTabContent.setPadding(new Insets(6));

        ScrollPane lodScrollPane = new ScrollPane(lodTabContent);
        lodScrollPane.setFitToWidth(true);
        lodScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        lodScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        // Do not let the tall LOD form propagate its preferred height into
        // TabPane -> BorderPane -> LitPathPane.  The LOD content scrolls
        // inside the fixed-height controls pane instead.
        lodScrollPane.setMinHeight(0.0);
        lodScrollPane.setPrefViewportHeight(TAB_CONTENT_HEIGHT);
        lodScrollPane.setPrefHeight(TAB_CONTENT_HEIGHT);
        lodScrollPane.setMaxHeight(Double.MAX_VALUE);

        // === TabPane ===
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefWidth(PANEL_WIDTH - 8);
        // Keep the tab control bounded by the LitPathPane's intended default
        // height.  Individual tabs may scroll internally rather than forcing
        // the floating pane to grow to their preferred content height.
        tabs.setMinHeight(0.0);
        tabs.setPrefHeight(TAB_CONTENT_HEIGHT);
        tabs.setMaxHeight(Double.MAX_VALUE);

        GraphControlsView graphLayoutView = new GraphControlsView(scene);
        GraphStyleControlsView graphStyleView = new GraphStyleControlsView(scene);

        Tab t1 = new Tab("View", viewTabContent);
        Tab t2 = new Tab("Processing", procTabContent);
        Tab t3 = new Tab("LOD", lodScrollPane);
        Tab t4 = new Tab("Graph Layout", graphLayoutView);
        Tab t5 = new Tab("Graph Style", graphStyleView);

        tabs.getTabs().addAll(t1, t2, t3, t4, t5);
        return tabs;
    }

    private void applyLodConfigFromControls() {
        if (target == null
            || targetPixelsSpinner == null
            || coarsenThresholdSpinner == null
            || refineThresholdSpinner == null) {
            return;
        }

        double low = coarsenThresholdSpinner.getValue();
        double high = refineThresholdSpinner.getValue();
        if (!(low > 0.0) || !(high > low)) {
            return;
        }

        TiledLodManager.Config config = target.getTiledLodConfigCopy();
        config.targetPixelsPerCell = targetPixelsSpinner.getValue();
        config.lowThreshold = low;
        config.highThreshold = high;
        config.finestAllowedLod = maxDetailCombo != null
            ? Math.max(0, maxDetailCombo.getSelectionModel().getSelectedIndex())
            : 0;
        config.geometryBudgetEnabled = geometryBudgetCheck != null && geometryBudgetCheck.isSelected();
        config.triangleBudget = triangleBudgetSpinner != null
            ? Math.max(1L, Math.round(triangleBudgetSpinner.getValue() * 1_000_000.0))
            : 5_000_000L;
        config.verboseDiagnostics = verboseLodDiagnosticsCheck != null
            && verboseLodDiagnosticsCheck.isSelected();
        config.throttleMs = throttleMsSpinner.getValue();
        config.debounceMs = settleMsSpinner.getValue();
        config.initialSettleMs = initialSettleMsSpinner.getValue();
        config.activeTransitionsPerPulse = activeTransitionsSpinner.getValue();
        config.settledTransitionsPerPulse = settledTransitionsSpinner.getValue();
        config.activeBuildsPerPulse = activeBuildsSpinner.getValue();
        config.settledBuildsPerPulse = settledBuildsSpinner.getValue();

        target.setTiledLodConfig(config);
    }

    private void startLodDiagnosticsTimer() {
        if (lodDiagnosticsTimer != null) return;
        lodDiagnosticsTimer = new AnimationTimer() {
            private long lastUpdateNanos;

            @Override
            public void handle(long now) {
                if (now - lastUpdateNanos < 250_000_000L) return;
                lastUpdateNanos = now;
                refreshLodDiagnostics();
            }
        };
        lodDiagnosticsTimer.start();
    }

    private void refreshLodDiagnostics() {
        refreshDimensionControls();
        if (target == null || lodStatusLabel == null) return;
        TiledSurfaceRenderer.LodStatistics stats = target.getTiledLodStatistics();
        lodStatusLabel.setText(target.isTiledHeightFieldRendererActive() ? "Active" : "Inactive");
        visibleTilesLabel.setText(stats.visibleTiles() + " / " + stats.totalTiles());
        lod0CountLabel.setText(Integer.toString(stats.lod0Tiles()));
        lod1CountLabel.setText(Integer.toString(stats.lod1Tiles()));
        lod2CountLabel.setText(Integer.toString(stats.lod2Tiles()));
        lod3CountLabel.setText(Integer.toString(stats.lod3Tiles()));
        lod4CountLabel.setText(Integer.toString(stats.lod4Tiles()));
        triangleCountLabel.setText(formatTriangleCount(stats.visibleTriangles()));
        rawRequestedTrianglesLabel.setText(formatTriangleCount(stats.rawRequestedTriangles()));
        budgetTargetTrianglesLabel.setText(formatTriangleCount(stats.budgetedTargetTriangles()));
        budgetCoarsenedLabel.setText(Integer.toString(stats.budgetCoarsenedTiles()));
        pendingTransitionsLabel.setText(Integer.toString(stats.pendingTransitions()));
    }

    private void refreshDimensionControls() {
        if (target == null
            || xWidthSpinner == null
            || zWidthSpinner == null
            || sourceDimensionsLabel == null) {
            return;
        }

        boolean imageBacked = target.isImageBackedSurface();
        setManagedVisible(xWidthLabel, !imageBacked);
        setManagedVisible(xWidthSpinner, !imageBacked);
        setManagedVisible(zWidthLabel, !imageBacked);
        setManagedVisible(zWidthSpinner, !imageBacked);

        setManagedVisible(sourceDimensionsCaption, imageBacked);
        setManagedVisible(sourceDimensionsLabel, imageBacked);
        setManagedVisible(renderL0DimensionsCaption, imageBacked);
        setManagedVisible(renderL0DimensionsLabel, imageBacked);
        setManagedVisible(worldDimensionsCaption, imageBacked);
        setManagedVisible(worldDimensionsLabel, imageBacked);

        if (imageBacked) {
            sourceDimensionsLabel.setText(
                target.getSourceWidth() + " x " + target.getSourceHeight());
            renderL0DimensionsLabel.setText(
                target.getRenderL0Width() + " x " + target.getRenderL0Height());
            worldDimensionsLabel.setText(String.format(
                "%.1f x %.1f", target.getWorldWidth(), target.getWorldDepth()));
        }
    }

    private static void setManagedVisible(javafx.scene.Node node, boolean visible) {
        if (node == null) return;
        node.setManaged(visible);
        node.setVisible(visible);
    }

    private static String formatTriangleCount(long triangles) {
        if (triangles >= 1_000_000L) {
            return String.format("%.2f M", triangles / 1_000_000.0);
        }
        if (triangles >= 1_000L) {
            return String.format("%.1f K", triangles / 1_000.0);
        }
        return Long.toString(triangles);
    }

    private static int clampInt(long value, int min, int max) {
        return (int) Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void fireOnRoot(Event evt) {
        if (scene != null && scene.getRoot() != null) {
            scene.getRoot().fireEvent(evt);
        } else {
            this.fireEvent(evt);
        }
    }

    private static GridPane formGrid() {
        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(6);
        gp.setAlignment(Pos.TOP_LEFT);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(25);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(75);
        c1.setHgrow(Priority.ALWAYS);

        gp.getColumnConstraints().addAll(c0, c1);
        return gp;
    }

    private static void addRow(GridPane gp, int row, String label, javafx.scene.Node control) {
        addRow(gp, row, new Label(label), control);
    }

    private static void addRow(GridPane gp, int row, Label label, javafx.scene.Node control) {
        gp.add(label, 0, row);

        if (control instanceof Spinner || control instanceof ComboBox) {
            GridPane.setHgrow(control, Priority.NEVER);
        } else {
            GridPane.setHgrow(control, Priority.ALWAYS);
            if (control instanceof javafx.scene.control.Control control1) {
                control1.setMaxWidth(Double.MAX_VALUE);
            }
        }
        gp.add(control, 1, row);
    }

    private static VBox titledBox(String title, javafx.scene.Node content) {
        Label t = new Label(title);
        t.getStyleClass().add("section-title");
        VBox box = new VBox(6, t, new Separator(), content);
        box.setPadding(new Insets(4, 2, 6, 2));
        return box;
    }

    private static <T> void styleSpinner(Spinner<T> spinner) {
        spinner.setPrefWidth(SPINNER_PREF_WIDTH);
        spinner.setMaxWidth(SPINNER_PREF_WIDTH);
        spinner.setMinWidth(Region.USE_PREF_SIZE);
    }

    private static <T> void styleCombo(ComboBox<T> combo) {
        combo.setPrefWidth(COMBO_PREF_WIDTH);
        combo.setMaxWidth(COMBO_PREF_WIDTH);
        combo.setMinWidth(Region.USE_PREF_SIZE);
    }

    private static void styleCheck(CheckBox cb) {
        cb.setPrefWidth(CHECKBOX_PREF_WIDTH);
        cb.setMaxWidth(CHECKBOX_PREF_WIDTH);
        cb.setMinWidth(Region.USE_PREF_SIZE);
    }

    private static void styleColorPicker(ColorPicker cp) {
        cp.setPrefWidth(COLOR_PICKER_PREF_WIDTH);
        cp.setMaxWidth(COLOR_PICKER_PREF_WIDTH);
        cp.setMinWidth(Region.USE_PREF_SIZE);
    }
}
