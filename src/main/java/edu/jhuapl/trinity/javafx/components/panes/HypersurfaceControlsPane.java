package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.HypersurfaceEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Hypersurface3DPane;
import edu.jhuapl.trinity.javafx.javafx3d.SurfaceUtils;
import edu.jhuapl.trinity.utils.DataUtils.HeightMode;
import java.io.File;
import java.nio.file.Paths;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import javafx.stage.FileChooser;

public class HypersurfaceControlsPane extends LitPathPane {

    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 640;

    /** preferred width for all Spinners */
    public static double SPINNER_PREF_WIDTH = 125.0;
    /** preferred width for all ComboBoxes */
    public static double COMBO_PREF_WIDTH = 220.0;
    /** preferred width for all CheckBoxes */
    public static double CHECKBOX_PREF_WIDTH = 100.0;
    /** preferred width for all ColorPickers */
    public static double COLOR_PICKER_PREF_WIDTH = 150.0;

    private final Hypersurface3DPane target;

    // Core
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

    // UX / Runtime
    private CheckBox hoverCheck;
    private CheckBox chartsCheck;
    private CheckBox markersCheck;
    private CheckBox crosshairsCheck;
    private Spinner<Integer> refreshSpinner;
    private Spinner<Integer> queueSpinner;

    public HypersurfaceControlsPane(Scene scene, Pane parent, Hypersurface3DPane target) {
        super(scene, parent, PANEL_WIDTH, PANEL_HEIGHT, new BorderPane(), "Hypersurface Controls", "", 200.0, 300.0);
        this.scene = scene;
        this.target = target;

        setPickOnBounds(false);
        setFocusTraversable(false);

        BorderPane bp = (BorderPane) this.contentPane;
        bp.setPadding(new Insets(6));
        bp.setCenter(buildTabs());
    }

    private TabPane buildTabs() {
        // seed values
        double yScale0 = (target != null) ? target.yScale : 5.0;
        double surfScale0 = (target != null) ? target.surfScale : 5.0;
        int xWidth0 = (target != null) ? target.xWidth : Hypersurface3DPane.DEFAULT_XWIDTH;
        int zWidth0 = (target != null) ? target.zWidth : Hypersurface3DPane.DEFAULT_ZWIDTH;
        Color bg0 = (target != null) ? target.sceneColor : Color.BLACK;

        // ---- Dimensions ----
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

        addRow(dimsGrid, 0, "X width", xWidthSpinner, e -> fireOnRoot(HypersurfaceEvent.xWidth(xWidthSpinner.getValue())));
        addRow(dimsGrid, 1, "Z length", zWidthSpinner, e -> fireOnRoot(HypersurfaceEvent.zWidth(zWidthSpinner.getValue())));
        addRow(dimsGrid, 2, "Y scale", yScaleSpinner, e -> fireOnRoot(HypersurfaceEvent.yScale(yScaleSpinner.getValue())));
        addRow(dimsGrid, 3, "Range scale", surfScaleSpinner, e -> fireOnRoot(HypersurfaceEvent.surfScale(surfScaleSpinner.getValue())));

        // ---- Rendering ----
        GridPane renderGrid = formGrid();
        meshTypeCombo = new ComboBox<>();
        meshTypeCombo.getItems().addAll("Surface", "Cylindrical");
        meshTypeCombo.getSelectionModel().select("Surface");
        styleCombo(meshTypeCombo);
        meshTypeCombo.setOnAction(e ->
            fireOnRoot(HypersurfaceEvent.surfaceRender("Surface".equals(meshTypeCombo.getValue()))));

        drawModeCombo = new ComboBox<>();
        drawModeCombo.getItems().addAll(DrawMode.LINE, DrawMode.FILL);
        drawModeCombo.getSelectionModel().select(DrawMode.LINE);
        styleCombo(drawModeCombo);
        drawModeCombo.setOnAction(e -> fireOnRoot(HypersurfaceEvent.drawMode(drawModeCombo.getValue())));

        cullFaceCombo = new ComboBox<>();
        cullFaceCombo.getItems().addAll(CullFace.FRONT, CullFace.BACK, CullFace.NONE);
        cullFaceCombo.getSelectionModel().select(CullFace.NONE);
        styleCombo(cullFaceCombo);
        cullFaceCombo.setOnAction(e -> fireOnRoot(HypersurfaceEvent.cullFace(cullFaceCombo.getValue())));

        colorationCombo = new ComboBox<>();
        colorationCombo.getItems().addAll(Hypersurface3DPane.COLORATION.COLOR_BY_IMAGE,
                                          Hypersurface3DPane.COLORATION.COLOR_BY_FEATURE,
                                          Hypersurface3DPane.COLORATION.COLOR_BY_SHAPLEY);
        colorationCombo.getSelectionModel().select(Hypersurface3DPane.COLORATION.COLOR_BY_FEATURE);
        styleCombo(colorationCombo);
        colorationCombo.setOnAction(e -> fireOnRoot(HypersurfaceEvent.coloration(colorationCombo.getValue())));

        addRow(renderGrid, 0, "Mesh", meshTypeCombo, null);
        addRow(renderGrid, 1, "Draw", drawModeCombo, null);
        addRow(renderGrid, 2, "Cull", cullFaceCombo, null);
        addRow(renderGrid, 3, "Color", colorationCombo, null);

        // ---- Scene (incl. Lighting) ----
        GridPane sceneGrid = formGrid();
        bgPicker = new ColorPicker(bg0);
        styleColorPicker(bgPicker);
        skyboxCheck = new CheckBox("Skybox");
        styleCheck(skyboxCheck);

        skyboxCheck.setOnAction(e -> fireOnRoot(new HyperspaceEvent(HyperspaceEvent.ENABLE_HYPERSPACE_SKYBOX, skyboxCheck.isSelected())));
        bgPicker.setOnAction(e -> fireOnRoot(new HyperspaceEvent(HyperspaceEvent.HYPERSPACE_BACKGROUND_COLOR, bgPicker.getValue())));

        enableAmbientCheck = new CheckBox("Ambient");
        enableAmbientCheck.setSelected(true);
        styleCheck(enableAmbientCheck);
        ambientColorPicker = new ColorPicker(Color.WHITE);
        styleColorPicker(ambientColorPicker);
        ambientColorPicker.setOnAction(e -> fireOnRoot(HypersurfaceEvent.ambientColor(ambientColorPicker.getValue())));
        enableAmbientCheck.setOnAction(e -> {
            boolean on = enableAmbientCheck.isSelected();
            ambientColorPicker.setDisable(!on);
            fireOnRoot(HypersurfaceEvent.ambientEnabled(on));
        });

        enablePointCheck = new CheckBox("Point light");
        enablePointCheck.setSelected(true);
        styleCheck(enablePointCheck);
        specularColorPicker = new ColorPicker(Color.CYAN);
        styleColorPicker(specularColorPicker);
        specularColorPicker.setOnAction(e -> fireOnRoot(HypersurfaceEvent.specularColor(specularColorPicker.getValue())));
        enablePointCheck.setOnAction(e -> {
            boolean on = enablePointCheck.isSelected();
            specularColorPicker.setDisable(!on);
            fireOnRoot(HypersurfaceEvent.pointEnabled(on));
        });

        addRow(sceneGrid, 0, "Background", bgPicker, null);
        addRow(sceneGrid, 1, "Sky", skyboxCheck, null);
        addRow(sceneGrid, 2, "Ambient", new HBox(6, enableAmbientCheck, ambientColorPicker), null);
        addRow(sceneGrid, 3, "Point", new HBox(6, enablePointCheck, specularColorPicker), null);

        VBox viewTabContent = new VBox(10,
            titledBox("Dimensions", dimsGrid),
            titledBox("Rendering", renderGrid),
            titledBox("Scene", sceneGrid)
        );
        viewTabContent.setPadding(new Insets(6));

        // ---- Processing tab ----
        GridPane heightGrid = formGrid();
        heightModeCombo = new ComboBox<>();
        heightModeCombo.getItems().addAll(HeightMode.values());
        heightModeCombo.getSelectionModel().select(HeightMode.RAW);
        styleCombo(heightModeCombo);
        heightModeCombo.setOnAction(e -> fireOnRoot(HypersurfaceEvent.heightMode(heightModeCombo.getValue())));
        addRow(heightGrid, 0, "Height mode", heightModeCombo, null);

        GridPane smoothingGrid = formGrid();
        enableSmoothingCheck = new CheckBox("Enable");
        styleCheck(enableSmoothingCheck);
        smoothingCombo = new ComboBox<>();
        smoothingCombo.getItems().addAll(SurfaceUtils.Smoothing.values());
        smoothingCombo.getSelectionModel().select(SurfaceUtils.Smoothing.GAUSSIAN);
        styleCombo(smoothingCombo);
        smoothingRadiusSpinner = new Spinner<>(1, 25, 2, 1);
        gaussianSigmaSpinner = new Spinner<>(0.10, 10.0, 1.0, 0.10);
        styleSpinner(smoothingRadiusSpinner);
        styleSpinner(gaussianSigmaSpinner);

        smoothingRadiusSpinner.setEditable(true);
        gaussianSigmaSpinner.setEditable(true);

        enableSmoothingCheck.setOnAction(e -> fireOnRoot(HypersurfaceEvent.smoothingEnabled(enableSmoothingCheck.isSelected())));
        smoothingCombo.setOnAction(e -> fireOnRoot(HypersurfaceEvent.smoothingMethod(smoothingCombo.getValue())));
        smoothingRadiusSpinner.valueProperty().addListener((o, ov, nv) -> fireOnRoot(HypersurfaceEvent.smoothingRadius(nv)));
        gaussianSigmaSpinner.valueProperty().addListener((o, ov, nv) -> fireOnRoot(HypersurfaceEvent.gaussianSigma(nv)));

        addRow(smoothingGrid, 0, "Smoothing", enableSmoothingCheck, null);
        addRow(smoothingGrid, 1, "Method", smoothingCombo, null);
        addRow(smoothingGrid, 2, "Radius", smoothingRadiusSpinner, null);
        addRow(smoothingGrid, 3, "Sigma", gaussianSigmaSpinner, null);

        GridPane interpGrid = formGrid();
        interpCombo = new ComboBox<>();
        interpCombo.getItems().addAll(SurfaceUtils.Interpolation.values());
        interpCombo.getSelectionModel().select(SurfaceUtils.Interpolation.NEAREST);
        styleCombo(interpCombo);
        interpCombo.setOnAction(e -> fireOnRoot(HypersurfaceEvent.interp(interpCombo.getValue())));
        addRow(interpGrid, 0, "Mode", interpCombo, null);

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

        enableToneMapCheck.setOnAction(e -> fireOnRoot(HypersurfaceEvent.toneEnabled(enableToneMapCheck.isSelected())));
        toneMapCombo.setOnAction(e -> fireOnRoot(HypersurfaceEvent.toneOperator(toneMapCombo.getValue())));
        toneParamSpinner.valueProperty().addListener((o, ov, nv) -> fireOnRoot(HypersurfaceEvent.toneParam(nv)));

        addRow(toneGrid, 0, "Tone map", enableToneMapCheck, null);
        addRow(toneGrid, 1, "Operator", toneMapCombo, null);
        addRow(toneGrid, 2, "k / γ", toneParamSpinner, null);

        VBox procTabContent = new VBox(10,
            titledBox("Height", heightGrid),
            titledBox("Smoothing", smoothingGrid),
            titledBox("Interpolation", interpGrid),
            titledBox("Tone Mapping", toneGrid)
        );
        procTabContent.setPadding(new Insets(6));

        // ---- Ops tab (UX, Runtime, Actions) ----
        GridPane uxGrid = formGrid();
        hoverCheck = new CheckBox("Hover");
        chartsCheck = new CheckBox("Surface charts");
        markersCheck = new CheckBox("Markers");
        crosshairsCheck = new CheckBox("Crosshairs");
        styleCheck(hoverCheck);
        styleCheck(chartsCheck);
        styleCheck(markersCheck);
        styleCheck(crosshairsCheck);

        hoverCheck.setOnAction(e -> fireOnRoot(HypersurfaceEvent.hoverEnabled(hoverCheck.isSelected())));
        chartsCheck.setOnAction(e -> fireOnRoot(HypersurfaceEvent.surfaceChartsEnabled(chartsCheck.isSelected())));
        markersCheck.setOnAction(e -> fireOnRoot(HypersurfaceEvent.dataMarkersEnabled(markersCheck.isSelected())));
        crosshairsCheck.setOnAction(e -> fireOnRoot(HypersurfaceEvent.crosshairsEnabled(crosshairsCheck.isSelected())));

        // Two rows, no extra "UX" label in the grid
        addRow(uxGrid, 0, "", new HBox(12, hoverCheck, chartsCheck), null);
        addRow(uxGrid, 1, "", new HBox(12, markersCheck, crosshairsCheck), null);

        GridPane runtimeGrid = formGrid();
        refreshSpinner = new Spinner<>(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                10, 10_000,
                (target != null) ? (int) target.hypersurfaceRefreshRate : 500,
                10
            )
        );
        queueSpinner = new Spinner<>(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                100, 1_000_000,
                (target != null) ? target.queueLimit : 20_000,
                100
            )
        );
        styleSpinner(refreshSpinner);
        styleSpinner(queueSpinner);

        refreshSpinner.setEditable(true);
        queueSpinner.setEditable(true);

        refreshSpinner.valueProperty().addListener((o, ov, nv) ->
            fireOnRoot(new HyperspaceEvent(HyperspaceEvent.REFRESH_RATE_GUI, nv.longValue())));
        queueSpinner.valueProperty().addListener((o, ov, nv) ->
            fireOnRoot(new HyperspaceEvent(HyperspaceEvent.NODE_QUEUELIMIT_GUI, nv)));

        addRow(runtimeGrid, 0, "Refresh (ms)", refreshSpinner, null);
        addRow(runtimeGrid, 1, "Queue limit", queueSpinner, null);

        VBox actionsBox = new VBox(6,
            buttonRow("Reset View", e -> fireOnRoot(HypersurfaceEvent.resetView())),
            buttonRow("Update Render", e -> fireOnRoot(HypersurfaceEvent.updateRender())),
            buttonRow("Clear Data", e -> fireOnRoot(HypersurfaceEvent.clearData())),
            buttonRow("Unroll Hyperspace Data", e -> fireOnRoot(HypersurfaceEvent.unroll())),
            buttonRow("Show Vector Distances", e -> fireOnRoot(HypersurfaceEvent.computeVectorDistances())),
            buttonRow("Feature Collection Difference…", e -> openFCAndFire(true)),
            buttonRow("Feature Collection Cosine Distance…", e -> openFCAndFire(false))
        );

        VBox opsTabContent = new VBox(10,
            titledBox("UX", uxGrid),
            titledBox("Runtime", runtimeGrid),
            titledBox("Actions", actionsBox)
        );
        opsTabContent.setPadding(new Insets(6));

        // ---- TabPane ----
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefWidth(PANEL_WIDTH - 8);

        Tab t1 = new Tab("View", viewTabContent);
        Tab t2 = new Tab("Processing", procTabContent);
        Tab t3 = new Tab("Ops", opsTabContent);

        tabs.getTabs().addAll(t1, t2, t3);
        return tabs;
    }

    private void openFCAndFire(boolean difference) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(difference ? "Load FeatureCollection to Compare…" : "Load FeatureCollection for Cosine Distance…");
        fileChooser.setInitialDirectory(Paths.get(".").toFile());
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                FeatureCollectionFile fcf = new FeatureCollectionFile(file.getAbsolutePath(), true);
                FeatureCollection fc = fcf.featureCollection;
                if (difference) {
                    fireOnRoot(HypersurfaceEvent.computeCollectionDiff(fc));
                } else {
                    fireOnRoot(HypersurfaceEvent.computeCosineDistance(fc));
                }
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to read FeatureCollection:\n" + ex.getMessage(), ButtonType.OK);
                alert.showAndWait();
            }
        }
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

    private static void addRow(GridPane gp, int row, String label, javafx.scene.Node control,
                               javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        Label l = new Label(label);
        gp.add(l, 0, row);

        if (control instanceof Spinner || control instanceof ComboBox) {
            GridPane.setHgrow(control, Priority.NEVER);
        } else {
            GridPane.setHgrow(control, Priority.ALWAYS);
            if (control instanceof javafx.scene.control.Control) {
                ((javafx.scene.control.Control) control).setMaxWidth(Double.MAX_VALUE);
            }
        }
        gp.add(control, 1, row);

        if (onAction != null) {
            if (control instanceof ComboBox) {
                ((ComboBox<?>) control).setOnAction(onAction);
            } else if (control instanceof CheckBox) {
                ((CheckBox) control).setOnAction(onAction);
            }
        }
    }

    private static VBox titledBox(String title, javafx.scene.Node content) {
        Label t = new Label(title);
        t.getStyleClass().add("section-title");
        VBox box = new VBox(6, t, new Separator(), content);
        box.setPadding(new Insets(4, 2, 6, 2));
        return box;
    }

    private static HBox buttonRow(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(handler);
        HBox row = new HBox(b);
        HBox.setHgrow(b, Priority.ALWAYS);
        return row;
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
