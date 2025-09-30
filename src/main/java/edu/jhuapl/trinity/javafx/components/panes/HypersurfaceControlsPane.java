package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.GraphControlsView;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.HypersurfaceEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Hypersurface3DPane;
import edu.jhuapl.trinity.javafx.javafx3d.SurfaceUtils;
import edu.jhuapl.trinity.utils.DataUtils.HeightMode;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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

    // Rendering
    private ComboBox<String> meshTypeCombo;
    private ComboBox<DrawMode> drawModeCombo;
    private ComboBox<CullFace> cullFaceCombo;
    private ComboBox<Hypersurface3DPane.COLORATION> colorationCombo;

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
    private ColorPicker specularColorPicker;

    public HypersurfaceControlsPane(Scene scene, Pane parent, Hypersurface3DPane target) {
        super(scene, parent, PANEL_WIDTH, PANEL_HEIGHT, new BorderPane(), "Hypersurface Controls", "", 200.0, 300.0);
        this.scene = scene;
        this.target = target;

        setPickOnBounds(false);
        setFocusTraversable(false);

        BorderPane bp = (BorderPane) this.contentPane;
        bp.setPadding(new Insets(6));
        bp.setCenter(buildTabs());
        
        scene.addEventHandler(HypersurfaceEvent.SET_XWIDTH_GUI, e -> {
            // Set spinner value if different, *without* firing another event
            if (xWidthSpinner != null && !xWidthSpinner.getValue().equals((Integer)e.object)) {
                xWidthSpinner.getValueFactory().setValue((Integer)e.object);
            }
            e.consume();
        });
        scene.addEventHandler(HypersurfaceEvent.SET_ZWIDTH_GUI, e -> {
            if (zWidthSpinner != null && !zWidthSpinner.getValue().equals((Integer)e.object)) {
                zWidthSpinner.getValueFactory().setValue((Integer)e.object);
            }
            e.consume();
        });
        scene.addEventHandler(HypersurfaceEvent.SET_YSCALE_GUI, e -> {
            if (yScaleSpinner != null && !yScaleSpinner.getValue().equals((Double)e.object)) {
                yScaleSpinner.getValueFactory().setValue((Double)e.object);
            }
            e.consume();
        });
        scene.addEventHandler(HypersurfaceEvent.SET_SURFSCALE_GUI, e -> {
            if (surfScaleSpinner != null && !surfScaleSpinner.getValue().equals((Double)e.object)) {
                surfScaleSpinner.getValueFactory().setValue((Double)e.object);
            }
            e.consume();
        });
        
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

        addRow(dimsGrid, 0, "X width", xWidthSpinner);
        addRow(dimsGrid, 1, "Z length", zWidthSpinner);
        addRow(dimsGrid, 2, "Y scale", yScaleSpinner);
        addRow(dimsGrid, 3, "Range scale", surfScaleSpinner);

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
        drawModeCombo.getSelectionModel().select(DrawMode.LINE);
        styleCombo(drawModeCombo);
        addRow(renderGrid, 1, "Draw", drawModeCombo);

        cullFaceCombo = new ComboBox<>();
        cullFaceCombo.getItems().addAll(CullFace.FRONT, CullFace.BACK, CullFace.NONE);
        cullFaceCombo.getSelectionModel().select(CullFace.NONE);
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

        meshTypeCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.surfaceRender("Surface".equals(meshTypeCombo.getValue()))));
        drawModeCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.drawMode(drawModeCombo.getValue())));
        cullFaceCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.cullFace(cullFaceCombo.getValue())));
        colorationCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.coloration(colorationCombo.getValue())));

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
        specularColorPicker = new ColorPicker(Color.CYAN);
        styleColorPicker(specularColorPicker);

        HBox pointBox = new HBox(6, enablePointCheck, specularColorPicker);
        addRow(sceneGrid, 3, "Point", pointBox);

        enablePointCheck.setOnAction(e -> {
            boolean on = enablePointCheck.isSelected();
            specularColorPicker.setDisable(!on);
            fireOnRoot(HypersurfaceEvent.pointEnabled(on));
        });
        specularColorPicker.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.specularColor(specularColorPicker.getValue())));

        VBox viewTabContent = new VBox(10,
            titledBox("Dimensions", dimsGrid),
            titledBox("Rendering", renderGrid),
            titledBox("Scene", sceneGrid)
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

        // === TabPane ===
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefWidth(PANEL_WIDTH - 8);
        GraphControlsView gcv = new GraphControlsView(scene);
        
        Tab t1 = new Tab("View", viewTabContent);
        Tab t2 = new Tab("Processing", procTabContent);
        Tab t3 = new Tab("Graph", gcv);
        tabs.getTabs().addAll(t1, t2, t3);
        return tabs;
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
        Label l = new Label(label);
        gp.add(l, 0, row);

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
