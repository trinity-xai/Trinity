package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.metric.Metric;
import edu.jhuapl.trinity.utils.statistics.DialogUtils.ScalarInputResult;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Compact chart panel for visualizing PDF and CDF statistics (2D) and
 * triggering a 3D joint PDF/CDF surface computation.
 *
 * Top row (always visible):
 *   - X Feature (2D), Bins, Refresh 2D, Options (MenuButton)
 *
 * Options menu contains:
 *   - Scalars mode (Score/Info% + Paste/Clear)
 *   - Lock X to [0,1]
 *   - Metric/Reference/Index (for X)
 *   - Y Feature / Y Index
 *   - Surface type (PDF/CDF)
 *   - Compute 3D Surface
 *
 * State is still exportable/apply-able for popout workflows.
 *
 * NOTE: No "Pop Out" button here — maximize/popout comes from the parent pane.
 *
 * @author Sean Phillips
 */
public class StatPdfCdfChartPanel extends BorderPane {

    // ===== Charts =====
    private StatPdfCdfChart pdfChart;
    private StatPdfCdfChart cdfChart;

    // ===== Data source mode =====
    private enum DataSource { VECTORS, SCALARS }
    private DataSource dataSource = DataSource.VECTORS;

    // Scalars mode state/UI
    private List<Double> scalarScores = new ArrayList<>();
    private List<Double> scalarInfos  = new ArrayList<>();
    private String scalarField = "Score"; // "Score" or "Info%"

    // Axis lock
    private boolean lockXToUnit = false;

    // Vectors mode controls/state (kept as fields; embedded into Options menu items)
    private ComboBox<StatisticEngine.ScalarType> xFeatureCombo;
    private Spinner<Integer> binsSpinner;

    private ComboBox<String> metricCombo;
    private String referenceMode = "Mean"; // "Mean", "Vector @ Index", "Custom"
    private Spinner<Integer> xIndexSpinner;

    private ComboBox<StatisticEngine.ScalarType> yFeatureCombo;
    private Spinner<Integer> yIndexSpinner;

    private boolean surfaceCDF = false; // false=PDF, true=CDF

    // Data/state
    private List<FeatureVector> currentVectors = new ArrayList<>();
    private StatisticEngine.ScalarType currentXFeature = StatisticEngine.ScalarType.L1_NORM;
    private int currentBins = 40;
    private List<Double> customReferenceVector;

    // Callbacks
    private Consumer<GridDensityResult> onComputeSurface = null;

    // ===== Constructor =====

    public StatPdfCdfChartPanel() {
        this(null, StatisticEngine.ScalarType.L1_NORM, 40);
    }

    public StatPdfCdfChartPanel(List<FeatureVector> initialVectors,
                                StatisticEngine.ScalarType initialType,
                                int initialBins) {
        if (initialVectors != null) currentVectors = initialVectors;
        if (initialType != null) currentXFeature = initialType;
        if (initialBins > 0) currentBins = initialBins;

        // --- Visible row: X Feature (2D), Bins, Refresh, Options ---
        xFeatureCombo = new ComboBox<>();
        xFeatureCombo.getItems().addAll(StatisticEngine.ScalarType.values());
        xFeatureCombo.setValue(currentXFeature);

        binsSpinner = new Spinner<>(5, 100, currentBins, 5);
        binsSpinner.setPrefWidth(100);
        binsSpinner.setPrefHeight(34);

        Button refresh2DButton = new Button("Refresh 2D");
        refresh2DButton.setOnAction(e -> refresh2DCharts());

        MenuButton optionsMenu = buildOptionsMenu();

        HBox topBar = new HBox(12,
            new VBox(2, new Label("X Feature (2D)"), xFeatureCombo),
            new VBox(2, new Label("Bins"), binsSpinner),
            refresh2DButton,
            optionsMenu
        );
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(6, 8, 4, 8));
        HBox.setHgrow(optionsMenu, Priority.NEVER);

        // --- Charts: PDF (top), CDF (bottom) ---
        pdfChart = new StatPdfCdfChart(currentXFeature, currentBins, StatPdfCdfChart.Mode.PDF_ONLY);
        cdfChart = new StatPdfCdfChart(currentXFeature, currentBins, StatPdfCdfChart.Mode.CDF_ONLY);

        // modest sizing so the pane doesn't inflate vertically
        pdfChart.setMinHeight(120);
        cdfChart.setMinHeight(120);
        pdfChart.setPrefHeight(220);
        cdfChart.setPrefHeight(220);

        if (!currentVectors.isEmpty()) {
            applyXFeatureOptions();
            pdfChart.setFeatureVectors(currentVectors);
            cdfChart.setFeatureVectors(currentVectors);
        }
        if (lockXToUnit) {
            pdfChart.setLockXAxis(true, 0.0, 1.0);
            cdfChart.setLockXAxis(true, 0.0, 1.0);
        }

        VBox chartsBox = new VBox(8, pdfChart, cdfChart);
        chartsBox.setPadding(new Insets(6));
        VBox.setVgrow(pdfChart, Priority.ALWAYS);
        VBox.setVgrow(cdfChart, Priority.ALWAYS);
        chartsBox.setFillWidth(true);
        chartsBox.setPrefHeight(Region.USE_COMPUTED_SIZE);

        setTop(topBar);
        setCenter(chartsBox);

        // --- Handlers for the always-visible controls ---
        xFeatureCombo.valueProperty().addListener((obs, ov, nv) -> {
            currentXFeature = nv;
            updateXControlEnablement();
            updateXIndexBoundsAndValue();
        });
    }

    // ===== Options menu (all the advanced/secondary controls) =====
private MenuButton buildOptionsMenu() {
    MenuButton mb = new MenuButton("Options");

    // --- Scalars / Data Source ---
    CheckMenuItem useScalars = new CheckMenuItem("Use Scalars Mode");
    useScalars.setSelected(false);
    useScalars.selectedProperty().addListener((o, oldV, nv) -> {
        dataSource = nv ? DataSource.SCALARS : DataSource.VECTORS;
        if (nv) {
            applyScalarSamplesToCharts();
        } else {
            pdfChart.clearScalarSamples();
            cdfChart.clearScalarSamples();
            refresh2DCharts();
        }
    });

    CheckMenuItem lockX = new CheckMenuItem("Lock X to [0,1]");
    lockX.setSelected(lockXToUnit);
    lockX.selectedProperty().addListener((o, oldV, nv) -> {
        lockXToUnit = nv;
        pdfChart.setLockXAxis(nv, 0.0, 1.0);
        cdfChart.setLockXAxis(nv, 0.0, 1.0);
    });

    // Scalars submenu (no popup children here, so submenu is fine)
    Menu scalarsMenu = new Menu("Scalars");
    ToggleGroup scalarFieldGroup = new ToggleGroup();

    RadioMenuItem scoreItem = new RadioMenuItem("Field: Score");
    scoreItem.setToggleGroup(scalarFieldGroup);
    scoreItem.setSelected("Score".equals(scalarField));
    scoreItem.setOnAction(e -> { scalarField = "Score"; applyScalarSamplesToCharts(); });

    RadioMenuItem infoItem = new RadioMenuItem("Field: Info%");
    infoItem.setToggleGroup(scalarFieldGroup);
    infoItem.setSelected("Info%".equals(scalarField));
    infoItem.setOnAction(e -> { scalarField = "Info%"; applyScalarSamplesToCharts(); });

    MenuItem pasteScalars = new MenuItem("Paste Scalars…");
    pasteScalars.setOnAction(e -> {
        ScalarInputResult res = DialogUtils.showScalarSamplesDialog();
        if (res == null) return;
        if (res.scores != null && !res.scores.isEmpty()) scalarScores = res.scores;
        if (res.infos  != null && !res.infos.isEmpty())  scalarInfos  = res.infos;
        applyScalarSamplesToCharts();
    });

    MenuItem clearScalars = new MenuItem("Clear Scalars");
    clearScalars.setOnAction(e -> {
        scalarScores.clear();
        scalarInfos.clear();
        pdfChart.clearScalarSamples();
        cdfChart.clearScalarSamples();
    });

    scalarsMenu.getItems().addAll(scoreItem, infoItem, new SeparatorMenuItem(), pasteScalars, clearScalars);

    // ---- Metric / Reference (X) custom section (top-level CustomMenuItem) ----
    metricCombo = new ComboBox<>();
    metricCombo.getItems().addAll(Metric.getMetricNames());
    if (metricCombo.getItems().contains("euclidean")) {
        metricCombo.setValue("euclidean");
    } else if (!metricCombo.getItems().isEmpty()) {
        metricCombo.setValue(metricCombo.getItems().get(0));
    }

    ComboBox<String> referenceCombo = new ComboBox<>();
    referenceCombo.getItems().addAll("Mean", "Vector @ Index", "Custom");
    referenceCombo.setValue(referenceMode);
    referenceCombo.valueProperty().addListener((obs, ov, nv) -> {
        referenceMode = nv;
        updateXControlEnablement();
        updateXIndexBoundsAndValue();
    });

    xIndexSpinner = new Spinner<>();
    xIndexSpinner.setPrefWidth(100);

    Button setCustomButton = new Button("Set Custom Vector…");
    setCustomButton.setOnAction(e -> {
        Integer expectedDim = (currentVectors != null && !currentVectors.isEmpty())
            ? currentVectors.get(0).getData().size() : null;
        List<Double> parsed = (expectedDim != null)
            ? DialogUtils.showCustomVectorDialog(expectedDim)
            : DialogUtils.showCustomVectorDialog();
        if (parsed != null && !parsed.isEmpty()) setCustomReferenceVector(parsed);
    });

    VBox metricBox = new VBox(6,
        new HBox(8, new Label("Metric (X)"), metricCombo),
        new HBox(8, new Label("Reference"), referenceCombo),
        new HBox(8, new Label("X Index"), xIndexSpinner),
        setCustomButton
    );
    metricBox.setPadding(new Insets(6, 10, 6, 10));
    CustomMenuItem metricCustom = new CustomMenuItem(metricBox);
    metricCustom.setHideOnClick(false);

    MenuItem metricHeader = new MenuItem("X Metric & Reference");
    metricHeader.setDisable(true);

    // ---- 3D Surface custom section (top-level CustomMenuItem) ----
    yFeatureCombo = new ComboBox<>();
    yFeatureCombo.getItems().addAll(StatisticEngine.ScalarType.values());
    yFeatureCombo.setValue(StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION);
    yFeatureCombo.valueProperty().addListener((obs, ov, nv) -> {
        updateYControlEnablement();
        updateYIndexBoundsAndValue();
    });

    yIndexSpinner = new Spinner<>();
    yIndexSpinner.setPrefWidth(100);

    ToggleGroup surfaceGroup = new ToggleGroup();
    RadioButton surfacePdf = new RadioButton("PDF");
    surfacePdf.setToggleGroup(surfaceGroup);
    surfacePdf.setSelected(!surfaceCDF);
    surfacePdf.setOnAction(e -> surfaceCDF = false);

    RadioButton surfaceCdf = new RadioButton("CDF");
    surfaceCdf.setToggleGroup(surfaceGroup);
    surfaceCdf.setSelected(surfaceCDF);
    surfaceCdf.setOnAction(e -> surfaceCDF = true);

    Button compute3DButton = new Button("Compute 3D Surface");
    compute3DButton.setOnAction(e -> compute3DSurface());

    VBox surfaceBox = new VBox(6,
        new HBox(8, new Label("Y Feature"), yFeatureCombo),
        new HBox(8, new Label("Y Index"), yIndexSpinner),
        new HBox(8, new Label("Surface"), surfacePdf, surfaceCdf),
        compute3DButton
    );
    surfaceBox.setPadding(new Insets(6, 10, 6, 10));
    CustomMenuItem surfaceCustom = new CustomMenuItem(surfaceBox);
    surfaceCustom.setHideOnClick(false);

    MenuItem surfaceHeader = new MenuItem("3D Surface");
    surfaceHeader.setDisable(true);

    // ---- Final, single assembly (no duplicates, no later mutations) ----
    mb.getItems().addAll(
        useScalars,
        lockX,
        new SeparatorMenuItem(),
        scalarsMenu,
        new SeparatorMenuItem(),
        metricHeader,
        metricCustom,
        new SeparatorMenuItem(),
        surfaceHeader,
        surfaceCustom
    );

    // initialize enablement/bounds now that controls exist
    updateXControlEnablement();
    updateYControlEnablement();
    updateXIndexBoundsAndValue();
    updateYIndexBoundsAndValue();

    return mb;
}
    // ===== Public API =====

    public boolean isSurfaceCDF() {
        return surfaceCDF;
    }

    public String getYFeatureTypeForDisplay() {
        if (yFeatureCombo == null) return "N/A";
        StatisticEngine.ScalarType type = yFeatureCombo.getValue();
        if (type == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION) {
            Integer idx = yIndexSpinner.getValue();
            return "COMPONENT_AT_DIMENSION[" + (idx != null ? idx : "?") + "]";
        }
        return (type != null) ? type.name() : "N/A";
    }

    /** Consumer invoked when "Compute 3D Surface" finishes successfully. */
    public void setOnComputeSurface(Consumer<GridDensityResult> handler) {
        this.onComputeSurface = handler;
    }

    public void setFeatureVectors(List<FeatureVector> vectors) {
        this.currentVectors = (vectors != null) ? vectors : new ArrayList<>();
        updateXIndexBoundsAndValue();
        updateYIndexBoundsAndValue();
        if (dataSource == DataSource.VECTORS) {
            applyXFeatureOptions();
            pdfChart.setFeatureVectors(this.currentVectors);
            cdfChart.setFeatureVectors(this.currentVectors);
        }
    }

    public void addFeatureVectors(List<FeatureVector> newVectors) {
        if (newVectors == null || newVectors.isEmpty()) return;
        if (this.currentVectors == null) this.currentVectors = new ArrayList<>(newVectors);
        else this.currentVectors.addAll(newVectors);
        updateXIndexBoundsAndValue();
        updateYIndexBoundsAndValue();
        if (dataSource == DataSource.VECTORS) {
            applyXFeatureOptions();
            pdfChart.addFeatureVectors(newVectors);
            cdfChart.addFeatureVectors(newVectors);
        }
    }

    public int getBins() { return binsSpinner.getValue(); }
    public StatisticEngine.ScalarType getScalarType() { return xFeatureCombo.getValue(); }
    public StatPdfCdfChart getPdfChart() { return pdfChart; }
    public StatPdfCdfChart getCdfChart() { return cdfChart; }

    public void setCustomReferenceVector(List<Double> customVector) {
        this.customReferenceVector = customVector;
        if (dataSource == DataSource.VECTORS
            && xFeatureCombo.getValue() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN
            && "Custom".equals(referenceMode)
            && !currentVectors.isEmpty()) {
            applyXFeatureOptions();
            pdfChart.setFeatureVectors(currentVectors);
            cdfChart.setFeatureVectors(currentVectors);
        }
    }

    // ===== State (for popout workflows) =====

    public static final class State {
        public StatisticEngine.ScalarType xType;
        public int bins;
        public boolean lockX;
        public boolean surfaceIsCDF;
        public String metricName;
        public String referenceMode; // Mean / Vector @ Index / Custom
        public Integer xIndex;
        public StatisticEngine.ScalarType yType;
        public Integer yIndex;
        public boolean usingScalars;
        public String scalarField; // Score / Info%
        public List<Double> scalarScores;
        public List<Double> scalarInfos;
        public List<Double> customRef; // nullable
    }

    public State exportState() {
        State s = new State();
        s.xType = xFeatureCombo.getValue();
        s.bins = binsSpinner.getValue();
        s.lockX = lockXToUnit;
        s.surfaceIsCDF = surfaceCDF;
        s.metricName = metricCombo != null ? metricCombo.getValue() : null;
        s.referenceMode = referenceMode;
        s.xIndex = xIndexSpinner != null ? xIndexSpinner.getValue() : null;
        s.yType = yFeatureCombo != null ? yFeatureCombo.getValue() : null;
        s.yIndex = yIndexSpinner != null ? yIndexSpinner.getValue() : null;
        s.usingScalars = (dataSource == DataSource.SCALARS);
        s.scalarField = scalarField;
        s.scalarScores = new ArrayList<>(scalarScores);
        s.scalarInfos  = new ArrayList<>(scalarInfos);
        s.customRef = (customReferenceVector == null) ? null : new ArrayList<>(customReferenceVector);
        return s;
    }

    public void applyState(State s) {
        if (s == null) return;

        xFeatureCombo.setValue(s.xType != null ? s.xType : xFeatureCombo.getValue());
        binsSpinner.getValueFactory().setValue((s.bins > 0) ? s.bins : binsSpinner.getValue());
        lockXToUnit = s.lockX;
        pdfChart.setLockXAxis(lockXToUnit, 0.0, 1.0);
        cdfChart.setLockXAxis(lockXToUnit, 0.0, 1.0);

        surfaceCDF = s.surfaceIsCDF;

        if (metricCombo != null && s.metricName != null) metricCombo.setValue(s.metricName);
        if (s.referenceMode != null) referenceMode = s.referenceMode;
        if (xIndexSpinner != null && s.xIndex != null) xIndexSpinner.getValueFactory().setValue(s.xIndex);

        if (yFeatureCombo != null && s.yType != null) yFeatureCombo.setValue(s.yType);
        if (yIndexSpinner != null && s.yIndex != null) yIndexSpinner.getValueFactory().setValue(s.yIndex);

        dataSource = s.usingScalars ? DataSource.SCALARS : DataSource.VECTORS;
        scalarField = (s.scalarField != null) ? s.scalarField : scalarField;
        scalarScores = (s.scalarScores != null) ? new ArrayList<>(s.scalarScores) : new ArrayList<>();
        scalarInfos  = (s.scalarInfos  != null) ? new ArrayList<>(s.scalarInfos)  : new ArrayList<>();
        customReferenceVector = (s.customRef != null) ? new ArrayList<>(s.customRef) : null;

        // Apply visuals
        updateXControlEnablement();
        updateYControlEnablement();
        updateXIndexBoundsAndValue();
        updateYIndexBoundsAndValue();

        if (dataSource == DataSource.SCALARS) {
            applyScalarSamplesToCharts();
        } else {
            refresh2DCharts();
        }
    }

    // ===== Scalars helpers =====

    private void applyScalarSamplesToCharts() {
        if (dataSource != DataSource.SCALARS) return;
        List<Double> chosen = "Info%".equals(scalarField) ? scalarInfos : scalarScores;
        if (chosen == null || chosen.isEmpty()) {
            pdfChart.clearScalarSamples();
            cdfChart.clearScalarSamples();
            return;
        }
        pdfChart.setScalarSamples(chosen);
        cdfChart.setScalarSamples(chosen);
        pdfChart.setLockXAxis(lockXToUnit, 0.0, 1.0);
        cdfChart.setLockXAxis(lockXToUnit, 0.0, 1.0);
    }

    // ===== 2D (Vectors) behavior =====

    private void refresh2DCharts() {
        if (dataSource != DataSource.VECTORS) return;

        currentXFeature = xFeatureCombo.getValue();
        currentBins = binsSpinner.getValue();

        pdfChart.setScalarType(currentXFeature);
        pdfChart.setBins(currentBins);

        cdfChart.setScalarType(currentXFeature);
        cdfChart.setBins(currentBins);

        applyXFeatureOptions();

        if (currentVectors != null && !currentVectors.isEmpty()) {
            pdfChart.setFeatureVectors(currentVectors);
            cdfChart.setFeatureVectors(currentVectors);
        }

        pdfChart.setLockXAxis(lockXToUnit, 0.0, 1.0);
        cdfChart.setLockXAxis(lockXToUnit, 0.0, 1.0);
    }

    private void applyXFeatureOptions() {
        // Clear residuals
        pdfChart.setMetricNameForGeneric(null);
        pdfChart.setReferenceVectorForGeneric(null);
        pdfChart.setComponentIndex(null);

        cdfChart.setMetricNameForGeneric(null);
        cdfChart.setReferenceVectorForGeneric(null);
        cdfChart.setComponentIndex(null);

        if (currentXFeature == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) {
            String metricName = (metricCombo != null) ? metricCombo.getValue() : null;
            List<Double> ref = switch (referenceMode) {
                case "Vector @ Index" -> getVectorAtIndexAsList(xIndexSpinner.getValue());
                case "Custom" -> customReferenceVector;
                default -> currentVectors.isEmpty() ? null : FeatureVector.getMeanVector(currentVectors);
            };
            pdfChart.setMetricNameForGeneric(metricName);
            pdfChart.setReferenceVectorForGeneric(ref);
            cdfChart.setMetricNameForGeneric(metricName);
            cdfChart.setReferenceVectorForGeneric(ref);
        } else if (currentXFeature == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION) {
            Integer idx = xIndexSpinner.getValue();
            pdfChart.setComponentIndex(idx);
            cdfChart.setComponentIndex(idx);
        }
    }

    // ===== 3D compute (Vectors only) =====

    private void compute3DSurface() {
        if (onComputeSurface == null || currentVectors == null || currentVectors.isEmpty()) return;
        if (dataSource != DataSource.VECTORS) return;

        AxisParams xAxis = new AxisParams();
        xAxis.setType(xFeatureCombo.getValue());
        if (xAxis.getType() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) {
            xAxis.setMetricName(metricCombo.getValue());
            List<Double> ref = switch (referenceMode) {
                case "Vector @ Index" -> getVectorAtIndexAsList(xIndexSpinner.getValue());
                case "Custom" -> customReferenceVector;
                default -> currentVectors.isEmpty() ? null : FeatureVector.getMeanVector(currentVectors);
            };
            xAxis.setReferenceVec(ref);
        } else if (xAxis.getType() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION) {
            xAxis.setComponentIndex(xIndexSpinner.getValue());
        }

        AxisParams yAxis = new AxisParams();
        yAxis.setType(yFeatureCombo.getValue());
        if (yAxis.getType() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION) {
            yAxis.setComponentIndex(yIndexSpinner.getValue());
        } else if (yAxis.getType() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) {
            yAxis.setMetricName(metricCombo.getValue());
            List<Double> ref = switch (referenceMode) {
                case "Vector @ Index" -> getVectorAtIndexAsList(xIndexSpinner.getValue());
                case "Custom" -> customReferenceVector;
                default -> currentVectors.isEmpty() ? null : FeatureVector.getMeanVector(currentVectors);
            };
            yAxis.setReferenceVec(ref);
        }

        int bins = binsSpinner.getValue();
        GridSpec grid = new GridSpec(bins, bins);
        GridDensityResult result = GridDensity3DEngine.computePdfCdf2D(currentVectors, xAxis, yAxis, grid);
        onComputeSurface.accept(result);
    }

    // ===== Enablement / bounds =====

    private void updateXControlEnablement() {
        boolean isMetric = xFeatureCombo.getValue() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN;
        boolean isComponent = xFeatureCombo.getValue() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION;

        if (metricCombo != null) metricCombo.setDisable(!isMetric);
        boolean indexEnabled = (isMetric && "Vector @ Index".equals(referenceMode)) || isComponent;
        if (xIndexSpinner != null) xIndexSpinner.setDisable(!indexEnabled);
    }

    private void updateYControlEnablement() {
        boolean yNeedsDim = yFeatureCombo.getValue() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION;
        if (yIndexSpinner != null) yIndexSpinner.setDisable(!yNeedsDim);
    }

    private void updateXIndexBoundsAndValue() {
        boolean isMetricVectorIdx = xFeatureCombo.getValue() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN
            && "Vector @ Index".equals(referenceMode);
        boolean isComponent = xFeatureCombo.getValue() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION;

        if (xIndexSpinner == null) return;

        if (isMetricVectorIdx) {
            int maxIdx = Math.max(0, getMaxVectorIndex());
            setSpinnerBounds(xIndexSpinner, 0, maxIdx, safeSpinnerValue(xIndexSpinner, 0, maxIdx));
        } else if (isComponent) {
            int maxDim = getMaxDimensionIndex();
            setSpinnerBounds(xIndexSpinner, 0, maxDim, safeSpinnerValue(xIndexSpinner, 0, maxDim));
        } else {
            int maxDim = getMaxDimensionIndex();
            setSpinnerBounds(xIndexSpinner, 0, Math.max(0, maxDim), 0);
        }
    }

    private void updateYIndexBoundsAndValue() {
        if (yIndexSpinner == null) return;
        boolean yIsComponent = yFeatureCombo.getValue() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION;
        if (yIsComponent) {
            int maxDim = getMaxDimensionIndex();
            setSpinnerBounds(yIndexSpinner, 0, maxDim, safeSpinnerValue(yIndexSpinner, 0, maxDim));
        } else {
            int maxDim = getMaxDimensionIndex();
            setSpinnerBounds(yIndexSpinner, 0, Math.max(0, maxDim), 0);
        }
    }

    // ===== Utilities =====

    private static void setSpinnerBounds(Spinner<Integer> spinner, int min, int max, int value) {
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, Math.max(min, max), value, 1));
        spinner.getValueFactory().setValue(Math.max(min, Math.min(max, value)));
    }

    private static int safeSpinnerValue(Spinner<Integer> spinner, int min, int max) {
        Integer v = (spinner.getValue() == null) ? min : spinner.getValue();
        return Math.max(min, Math.min(max, v));
    }

    private int getMaxVectorIndex() {
        return Math.max(0, (currentVectors == null ? 0 : currentVectors.size()) - 1);
    }

    private int getMaxDimensionIndex() {
        if (currentVectors == null || currentVectors.isEmpty()) return 0;
        return Math.max(0, currentVectors.get(0).getData().size() - 1);
    }

    private List<Double> getVectorAtIndexAsList(int idx) {
        if (currentVectors == null || currentVectors.isEmpty()) return null;
        int safe = Math.max(0, Math.min(idx, currentVectors.size() - 1));
        return currentVectors.get(safe).getData();
    }
}
