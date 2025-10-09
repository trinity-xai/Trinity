package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.metric.Metric;
import edu.jhuapl.trinity.utils.statistics.DialogUtils.ScalarInputResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javafx.scene.control.ScrollPane;
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
 * Chart panel showing PDF, CDF, and a time-series chart (vertical stack),
 * with linked interactions between the distribution charts and the time-series,
 * label filtering, Contribution (Δ log-odds), Cumulative log-odds, and a
 * Top-K contributors list ranked by |Δ log-odds|.
 *
 * PDF/CDF hover/click -> highlights contributing samples in the time-series.
 * Time-series hover shows sample index/value (and bin info in the readout).
 */
public class StatPdfCdfChartPanel extends BorderPane {

    // ===== Layout constants =====
    private static final double RIGHT_PANE_WIDTH = 260.0;
    private static final double TOPK_SPINNER_WIDTH = 72.0;

    // ===== Charts =====
    private StatPdfCdfChart pdfChart;
    private StatPdfCdfChart cdfChart;
    private StatTimeSeriesChart tsChart;

    // ===== Data source mode =====
    private enum DataSource { VECTORS, SCALARS }
    private DataSource dataSource = DataSource.VECTORS;

    // Scalars mode state/UI
    private List<Double> scalarScores = new ArrayList<>();
    private List<Double> scalarInfos  = new ArrayList<>();
    private String scalarField = "Score"; // "Score" or "Info%"

    // Axis lock
    private boolean lockXToUnit = false;

    // Vectors mode controls/state
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

    // Time-series state
    private List<Double> scalarSeries = new ArrayList<>();
    private boolean persistSelection = false;

    // Maintain persistent highlight indices locally (no addHighlights() needed on tsChart)
    private final Set<Integer> persistentHighlights = new LinkedHashSet<>();

    // ===== Time-series mode: Similarity vs Contribution vs Cumulative =====
    private enum TSMode { SIMILARITY, CONTRIBUTION, CUMULATIVE }
    private TSMode tsMode = TSMode.CONTRIBUTION; // default
    private StatisticEngine.SimilarityAggregator agg = StatisticEngine.SimilarityAggregator.MEAN;
    private final double contribEps = 1e-6;

    // Keep last contributions for Top-K list
    private StatisticEngine.ContributionSeries lastContrib = null;

    // Readout
    private final Label selectionInfo = new Label("—");

    // ===== Label filter state & UI =====
    private static final String UNLABELED = "(unlabeled)";
    private List<String> allLabels = new ArrayList<>();
    private List<String> selectedLabels = new ArrayList<>();
    private final Map<String, CheckMenuItem> labelChecks = new LinkedHashMap<>();
    private Menu labelsMenu; // built in Options

    // ===== Top-K contributors panel =====
    private VBox topKRoot;
    private VBox topKListBox;
    private Spinner<Integer> topKSpinner;
    private int topK = 5;

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

        // --- Visible row ---
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

        // --- Charts: PDF (top), CDF (middle), Time Series (bottom) ---
        pdfChart = new StatPdfCdfChart(currentXFeature, currentBins, StatPdfCdfChart.Mode.PDF_ONLY);
        cdfChart = new StatPdfCdfChart(currentXFeature, currentBins, StatPdfCdfChart.Mode.CDF_ONLY);
        tsChart  = new StatTimeSeriesChart();

        // modest sizing
        pdfChart.setMinHeight(120);
        cdfChart.setMinHeight(120);
        tsChart.setMinHeight(140);
        pdfChart.setPrefHeight(220);
        cdfChart.setPrefHeight(220);
        tsChart.setPrefHeight(240);

        if (!currentVectors.isEmpty()) {
            rebuildLabelsFromCurrentVectors(); // build labels before first render
            applyXFeatureOptions();
            List<FeatureVector> use = getFilteredVectors();
            pdfChart.setFeatureVectors(use);
            cdfChart.setFeatureVectors(use);
            updateTimeSeries(); // populate TS based on tsMode
        }
        if (lockXToUnit) {
            pdfChart.setLockXAxis(true, 0.0, 1.0);
            cdfChart.setLockXAxis(true, 0.0, 1.0);
        }

        // linked interactions: PDF/CDF -> TS
        wireChartInteractions();

        // Bottom utility bar
        Button clearBtn = new Button("Clear Highlights");
        clearBtn.setOnAction(e -> {
            persistentHighlights.clear();
            tsChart.clearHighlights();
            selectionInfo.setText("—");
        });

        HBox bottomBar = new HBox(12, new Label("Selection:"), selectionInfo, new Region(), clearBtn);
        HBox.setHgrow(bottomBar.getChildren().get(2), Priority.ALWAYS);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(2, 8, 8, 8));

        VBox chartsBox = new VBox(8, pdfChart, cdfChart, tsChart, bottomBar);
        chartsBox.setPadding(new Insets(6));
        VBox.setVgrow(pdfChart, Priority.ALWAYS);
        VBox.setVgrow(cdfChart, Priority.ALWAYS);
        VBox.setVgrow(tsChart, Priority.ALWAYS);
        chartsBox.setFillWidth(true);
        chartsBox.setPrefHeight(Region.USE_COMPUTED_SIZE);

        setTop(topBar);
        setCenter(chartsBox);

        // Right-side: Top-K contributors (fixed width)
        setRight(buildTopKPane());

        // handler
        xFeatureCombo.valueProperty().addListener((obs, ov, nv) -> {
            currentXFeature = nv;
            updateXControlEnablement();
            updateXIndexBoundsAndValue();
        });
    }

    // ===== Options menu =====
    private MenuButton buildOptionsMenu() {
        MenuButton mb = new MenuButton("Options");

        // Data source
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

        CheckMenuItem persistSel = new CheckMenuItem("Persist Selection");
        persistSel.setSelected(persistSelection);
        persistSel.selectedProperty().addListener((o, oldV, nv) -> {
            persistSelection = nv;
            if (!persistSelection) persistentHighlights.clear();
            applyCurrentHighlights();
        });

        // Scalars submenu
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
            tsChart.clearHighlights();
            tsChart.setSeries(new ArrayList<>());
            selectionInfo.setText("—");
            lastContrib = null;
            updateTopKList(); // clear list
        });

        scalarsMenu.getItems().addAll(scoreItem, infoItem, new SeparatorMenuItem(), pasteScalars, clearScalars);

        // Labels Filter submenu
        labelsMenu = new Menu("Labels (Filter)");
        labelsMenu.getItems().add(new MenuItem("No labels found"));

        // Metric/Reference (X) section
        metricCombo = new ComboBox<>();
        metricCombo.getItems().addAll(Metric.getMetricNames());
        if (metricCombo.getItems().contains("euclidean")) metricCombo.setValue("euclidean");
        else if (!metricCombo.getItems().isEmpty()) metricCombo.setValue(metricCombo.getItems().get(0));

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

        // 3D Surface section
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

        // ---- Time-Series Mode & Aggregator ----
        Menu tsModeMenu = new Menu("Time-Series Mode");
        ToggleGroup tsModeGroup = new ToggleGroup();
        RadioMenuItem tsSimilarity = new RadioMenuItem("Similarity (matches PDF feature)");
        tsSimilarity.setToggleGroup(tsModeGroup);
        tsSimilarity.setSelected(tsMode == TSMode.SIMILARITY);
        tsSimilarity.setOnAction(e -> { tsMode = TSMode.SIMILARITY; updateTimeSeries(); });

        RadioMenuItem tsContribution = new RadioMenuItem("Contribution (Δ log-odds)");
        tsContribution.setToggleGroup(tsModeGroup);
        tsContribution.setSelected(tsMode == TSMode.CONTRIBUTION);
        tsContribution.setOnAction(e -> { tsMode = TSMode.CONTRIBUTION; updateTimeSeries(); });

        RadioMenuItem tsCumulative = new RadioMenuItem("Cumulative Log-odds (ΣΔ)");
        tsCumulative.setToggleGroup(tsModeGroup);
        tsCumulative.setSelected(tsMode == TSMode.CUMULATIVE);
        tsCumulative.setOnAction(e -> { tsMode = TSMode.CUMULATIVE; updateTimeSeries(); });

        tsModeMenu.getItems().addAll(tsSimilarity, tsContribution, tsCumulative);

        Menu aggregatorMenu = new Menu("Aggregator (Contribution/Cumulative)");
        ToggleGroup aggGroup = new ToggleGroup();
        RadioMenuItem aggMean = new RadioMenuItem("Mean");
        RadioMenuItem aggGeo  = new RadioMenuItem("Geomean");
        RadioMenuItem aggMin  = new RadioMenuItem("Min");
        aggMean.setToggleGroup(aggGroup);
        aggGeo.setToggleGroup(aggGroup);
        aggMin.setToggleGroup(aggGroup);
        aggMean.setSelected(agg == StatisticEngine.SimilarityAggregator.MEAN);
        aggGeo.setSelected(agg == StatisticEngine.SimilarityAggregator.GEOMEAN);
        aggMin.setSelected(agg == StatisticEngine.SimilarityAggregator.MIN);
        aggMean.setOnAction(e -> { agg = StatisticEngine.SimilarityAggregator.MEAN; if (tsMode!=TSMode.SIMILARITY) updateTimeSeries(); });
        aggGeo.setOnAction(e -> { agg = StatisticEngine.SimilarityAggregator.GEOMEAN; if (tsMode!=TSMode.SIMILARITY) updateTimeSeries(); });
        aggMin.setOnAction(e -> { agg = StatisticEngine.SimilarityAggregator.MIN; if (tsMode!=TSMode.SIMILARITY) updateTimeSeries(); });
        aggregatorMenu.getItems().addAll(aggMean, aggGeo, aggMin);

        // ---- Final assembly ----
        mb.getItems().addAll(
            useScalars,
            lockX,
            persistSel,
            new SeparatorMenuItem(),
            scalarsMenu,
            labelsMenu,
            new SeparatorMenuItem(),
            tsModeMenu,
            aggregatorMenu,
            new SeparatorMenuItem(),
            metricHeader,
            metricCustom,
            new SeparatorMenuItem(),
            surfaceHeader,
            surfaceCustom
        );

        // initialize enablement/bounds
        updateXControlEnablement();
        updateYControlEnablement();
        updateXIndexBoundsAndValue();
        updateYIndexBoundsAndValue();

        return mb;
    }

    // ===== Top-K Pane (fixed width) =====
    private VBox buildTopKPane() {
        Label title = new Label("Top-K Contributors");

        topKSpinner = new Spinner<>(1, 100, topK, 1);
        topKSpinner.setEditable(false);
        // Fix spinner width so it doesn't stretch the parent
        topKSpinner.setMinWidth(TOPK_SPINNER_WIDTH);
        topKSpinner.setPrefWidth(TOPK_SPINNER_WIDTH);
        topKSpinner.setMaxWidth(TOPK_SPINNER_WIDTH);
        // Optional: constrain editor text columns (even though not editable)
        if (topKSpinner.getEditor() != null) {
            topKSpinner.getEditor().setPrefColumnCount(3);
        }
        topKSpinner.valueProperty().addListener((obs, ov, nv) -> {
            topK = (nv != null) ? nv : topK;
            updateTopKList();
        });

        HBox header = new HBox(8, title, new Region(), new Label("K:"), topKSpinner);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        topKListBox = new VBox(4);

        ScrollPane scroll = new ScrollPane(topKListBox);
        // Grow vertically with parent; let the viewport width be respected
        VBox.setVgrow(scroll, Priority.ALWAYS);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        topKRoot = new VBox(8, header, scroll);
        topKRoot.setPadding(new Insets(8));
        topKRoot.setFillWidth(false);

        // Fix the right pane width
        topKRoot.setMinWidth(RIGHT_PANE_WIDTH);
        topKRoot.setPrefWidth(RIGHT_PANE_WIDTH);
        topKRoot.setMaxWidth(RIGHT_PANE_WIDTH);

        return topKRoot;
    }

    private void updateTopKList() {
        if (topKListBox == null) return;
        topKListBox.getChildren().clear();

        // Only defined in Contribution/Cumulative (uses Δ)
        if (lastContrib == null || lastContrib.delta == null || lastContrib.delta.isEmpty()) {
            Label none = new Label("No contribution data.");
            topKListBox.getChildren().add(none);
            return;
        }

        // Build indices and sort by |Δ|
        List<Integer> indices = new ArrayList<>();
        int n = lastContrib.delta.size();
        for (int i = 0; i < n; i++) indices.add(i);

        indices.sort((a, b) -> {
            double da = Math.abs(lastContrib.delta.get(a));
            double db = Math.abs(lastContrib.delta.get(b));
            return Double.compare(db, da);
        });

        int count = Math.min(topK, indices.size());
        for (int i = 0; i < count; i++) {
            int idx = indices.get(i);
            double abs = Math.abs(lastContrib.delta.get(idx));
            String text = String.format("#%d  |Δ|= %.6f", idx, abs);

            Button row = new Button(text);
            // Keep buttons compact; do not stretch to container width
            row.setMaxWidth(RIGHT_PANE_WIDTH * 0.9);
            row.setMinWidth(RIGHT_PANE_WIDTH * 0.9);
            row.setOnAction(e -> {
                if (!persistSelection) persistentHighlights.clear();
                persistentHighlights.add(idx);
                applyCurrentHighlights();
                selectionInfo.setText(String.format("Top-K • Sample #%d: |Δ| = %.6f", idx, abs));
            });
            topKListBox.getChildren().add(row);
        }
    }

    private void applyCurrentHighlights() {
        if (persistentHighlights.isEmpty()) {
            tsChart.clearHighlights();
            return;
        }
        int[] arr = persistentHighlights.stream().mapToInt(Integer::intValue).toArray();
        tsChart.highlightSamples(arr);
    }

    // ===== Public API =====

    public boolean isSurfaceCDF() { return surfaceCDF; }

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
        rebuildLabelsFromCurrentVectors();
        updateXIndexBoundsAndValue();
        updateYIndexBoundsAndValue();
        if (dataSource == DataSource.VECTORS) {
            applyXFeatureOptions();
            List<FeatureVector> use = getFilteredVectors();
            pdfChart.setFeatureVectors(use);
            cdfChart.setFeatureVectors(use);
            updateTimeSeries();
        }
    }

    public void addFeatureVectors(List<FeatureVector> newVectors) {
        if (newVectors == null || newVectors.isEmpty()) return;
        if (this.currentVectors == null) this.currentVectors = new ArrayList<>(newVectors);
        else this.currentVectors.addAll(newVectors);
        rebuildLabelsFromCurrentVectors();
        updateXIndexBoundsAndValue();
        updateYIndexBoundsAndValue();
        if (dataSource == DataSource.VECTORS) {
            applyXFeatureOptions();
            List<FeatureVector> use = getFilteredVectors();
            pdfChart.setFeatureVectors(use);
            cdfChart.setFeatureVectors(use);
            updateTimeSeries();
        }
    }

    public int getBins() { return binsSpinner.getValue(); }
    public StatisticEngine.ScalarType getScalarType() { return xFeatureCombo.getValue(); }
    public StatPdfCdfChart getPdfChart() { return pdfChart; }
    public StatPdfCdfChart getCdfChart() { return cdfChart; }
    public StatTimeSeriesChart getTimeSeriesChart() { return tsChart; }

    public void setCustomReferenceVector(List<Double> customVector) {
        this.customReferenceVector = customVector;
        if (dataSource == DataSource.VECTORS
            && xFeatureCombo.getValue() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN
            && "Custom".equals(referenceMode)
            && !currentVectors.isEmpty()) {
            applyXFeatureOptions();
            List<FeatureVector> use = getFilteredVectors();
            pdfChart.setFeatureVectors(use);
            cdfChart.setFeatureVectors(use);
            updateTimeSeries();
        }
    }

    // ===== State =====

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
        public boolean persistSelection;

        // Label filter
        public List<String> selectedLabels;

        // Time-series mode & aggregator
        public String tsMode; // "SIMILARITY" or "CONTRIBUTION" or "CUMULATIVE"
        public String aggregator; // "MEAN","GEOMEAN","MIN"
        public Integer topK; // optional
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
        s.persistSelection = persistSelection;
        s.selectedLabels = new ArrayList<>(selectedLabels);
        s.tsMode = this.tsMode.name();
        s.aggregator = this.agg.name();
        s.topK = this.topK;
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
        persistSelection = s.persistSelection;

        if (s.selectedLabels != null) selectedLabels = new ArrayList<>(s.selectedLabels);
        if (s.tsMode != null) tsMode = TSMode.valueOf(s.tsMode);
        if (s.aggregator != null) agg = StatisticEngine.SimilarityAggregator.valueOf(s.aggregator);
        if (s.topK != null) {
            topK = Math.max(1, Math.min(100, s.topK));
            if (topKSpinner != null) topKSpinner.getValueFactory().setValue(topK);
        }

        // Reset persisted highlights when applying state (safer)
        persistentHighlights.clear();
        applyCurrentHighlights();

        // Apply visuals
        rebuildLabelsFromCurrentVectors();
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
            tsChart.setSeries(new ArrayList<>());
            selectionInfo.setText("—");
            lastContrib = null;
            updateTopKList();
            return;
        }
        pdfChart.setScalarSamples(chosen);
        cdfChart.setScalarSamples(chosen);

        // In Scalars mode, treat TS as similarity only.
        tsMode = TSMode.SIMILARITY;
        tsChart.setAxisLabels("Sample Index", "Scalar Value");
        tsChart.setSeries(chosen);
        scalarSeries = new ArrayList<>(chosen);
        lastContrib = null;
        persistentHighlights.clear();
        applyCurrentHighlights();
        updateTopKList();

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

        List<FeatureVector> use = getFilteredVectors();
        if (use != null && !use.isEmpty()) {
            pdfChart.setFeatureVectors(use);
            cdfChart.setFeatureVectors(use);
            updateTimeSeries();
        } else {
            pdfChart.setFeatureVectors(new ArrayList<>());
            cdfChart.setFeatureVectors(new ArrayList<>());
            tsChart.setSeries(new ArrayList<>());
            selectionInfo.setText("—");
            lastContrib = null;
            persistentHighlights.clear();
            applyCurrentHighlights();
            updateTopKList();
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
                default -> {
                    List<FeatureVector> use = getFilteredVectors();
                    yield use.isEmpty() ? null : FeatureVector.getMeanVector(use);
                }
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

    private void compute3DSurface() {
        if (onComputeSurface == null) return;
        List<FeatureVector> use = getFilteredVectors();
        if (use == null || use.isEmpty()) return;
        if (dataSource != DataSource.VECTORS) return;

        AxisParams xAxis = new AxisParams();
        xAxis.setType(xFeatureCombo.getValue());
        if (xAxis.getType() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) {
            xAxis.setMetricName(metricCombo.getValue());
            List<Double> ref = switch (referenceMode) {
                case "Vector @ Index" -> getVectorAtIndexAsList(xIndexSpinner.getValue());
                case "Custom" -> customReferenceVector;
                default -> use.isEmpty() ? null : FeatureVector.getMeanVector(use);
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
                case "Vector @ Index" -> getVectorAtIndexAsList(yIndexSpinner.getValue());
                case "Custom" -> customReferenceVector;
                default -> use.isEmpty() ? null : FeatureVector.getMeanVector(use);
            };
            yAxis.setReferenceVec(ref);
        }

        int bins = binsSpinner.getValue();
        GridSpec grid = new GridSpec(bins, bins);
        GridDensityResult result = GridDensity3DEngine.computePdfCdf2D(use, xAxis, yAxis, grid);
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
            int maxIdx = Math.max(0, getFilteredVectors().size() - 1);
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
        List<FeatureVector> use = getFilteredVectors();
        if (use == null || use.isEmpty()) return null;
        int safe = Math.max(0, Math.min(idx, use.size() - 1));
        return use.get(safe).getData();
    }

    // ===== Label filter logic =====

    private void rebuildLabelsFromCurrentVectors() {
        Set<String> labelSet = new LinkedHashSet<>();
        for (FeatureVector fv : currentVectors) {
            String lab = fv.getLabel();
            labelSet.add(lab != null ? lab : UNLABELED);
        }
        allLabels = new ArrayList<>(labelSet);

        if (selectedLabels.isEmpty() && !allLabels.isEmpty()) {
            selectedLabels = new ArrayList<>(allLabels);
        } else {
            selectedLabels.removeIf(l -> !labelSet.contains(l));
        }
        rebuildLabelsMenu();
    }

    private void rebuildLabelsMenu() {
        if (labelsMenu == null) return;
        labelsMenu.getItems().clear();
        labelChecks.clear();

        if (allLabels.isEmpty()) {
            MenuItem none = new MenuItem("No labels found");
            none.setDisable(true);
            labelsMenu.getItems().add(none);
            return;
        }

        for (String lab : allLabels) {
            CheckMenuItem ci = new CheckMenuItem(lab);
            ci.setSelected(selectedLabels.contains(lab));
            ci.selectedProperty().addListener((obs, ov, nv) -> {
                if (nv) {
                    if (!selectedLabels.contains(lab)) selectedLabels.add(lab);
                } else {
                    selectedLabels.remove(lab);
                }
            });
            labelChecks.put(lab, ci);
            labelsMenu.getItems().add(ci);
        }

        labelsMenu.getItems().add(new SeparatorMenuItem());

        MenuItem selectAll = new MenuItem("Select All");
        selectAll.setOnAction(e -> {
            selectedLabels = new ArrayList<>(allLabels);
            labelChecks.values().forEach(c -> c.setSelected(true));
            refresh2DCharts();
        });

        MenuItem selectNone = new MenuItem("Select None");
        selectNone.setOnAction(e -> {
            selectedLabels.clear();
            labelChecks.values().forEach(c -> c.setSelected(false));
            refresh2DCharts();
        });

        MenuItem apply = new MenuItem("Apply Label Filter");
        apply.setOnAction(e -> refresh2DCharts());

        labelsMenu.getItems().addAll(selectAll, selectNone, new SeparatorMenuItem(), apply);
    }

    private List<FeatureVector> getFilteredVectors() {
        if (currentVectors == null || currentVectors.isEmpty()) return new ArrayList<>();
        if (allLabels.isEmpty() || selectedLabels.isEmpty() || selectedLabels.size() == allLabels.size()) {
            return new ArrayList<>(currentVectors);
        }
        List<FeatureVector> out = new ArrayList<>();
        for (FeatureVector fv : currentVectors) {
            String lab = fv.getLabel();
            String norm = (lab != null) ? lab : UNLABELED;
            if (selectedLabels.contains(norm)) out.add(fv);
        }
        return out;
    }

    // ===== Linking & time-series update =====

    private void updateTimeSeries() {
        if (dataSource == DataSource.SCALARS) {
            // handled in applyScalarSamplesToCharts
            return;
        }
        List<FeatureVector> use = getFilteredVectors();
        if (use == null || use.isEmpty()) {
            tsChart.setSeries(new ArrayList<>());
            lastContrib = null;
            persistentHighlights.clear();
            applyCurrentHighlights();
            updateTopKList();
            return;
        }

        if (tsMode == TSMode.CONTRIBUTION || tsMode == TSMode.CUMULATIVE) {
            lastContrib = StatisticEngine.computeContributions(use, agg, null, contribEps);

            if (tsMode == TSMode.CONTRIBUTION) {
                tsChart.setAxisLabels("Sample Index", "Δ log-odds");
                tsChart.setSeries(lastContrib.delta);
            } else {
                // Cumulative: C_t = sum_{k<=t} Δ_k; start at 0 (50-50 prior odds)
                List<Double> cum = new ArrayList<>(lastContrib.delta.size());
                double running = 0.0;
                for (double d : lastContrib.delta) {
                    running += d;
                    cum.add(running);
                }
                tsChart.setAxisLabels("Sample Index", "Cumulative log-odds (ΣΔ)");
                tsChart.setSeries(cum);
            }

            persistentHighlights.clear();
            applyCurrentHighlights();
            updateTopKList(); // uses Δ for ranking
        } else {
            // Similarity: same scalar values that fed the PDF/CDF
            StatisticResult stat = (pdfChart != null) ? pdfChart.getLastStatisticResult() : null;
            List<Double> vals = (stat != null) ? stat.getValues() : null;
            if (vals == null) vals = new ArrayList<>();
            tsChart.setAxisLabels("Sample Index", "Scalar Value");
            tsChart.setSeries(vals);
            scalarSeries = new ArrayList<>(vals);
            lastContrib = null; // not defined in similarity mode
            persistentHighlights.clear();
            applyCurrentHighlights();
            updateTopKList();
        }
    }

    private void wireChartInteractions() {
        // PDF -> TS
        pdfChart.setOnBinHover(sel -> {
            handleIncomingHighlight(sel.sampleIdx);
            selectionInfo.setText(
                String.format("PDF bin %d: [%.4f, %.4f) center≈%.4f • count=%d (%.2f%%)",
                    sel.bin, sel.xFrom, sel.xTo, sel.xCenter, sel.count, 100.0 * sel.fraction)
            );
        });
        pdfChart.setOnBinClick(sel -> {
            handleIncomingHighlight(sel.sampleIdx);
            selectionInfo.setText(
                String.format("PDF bin %d (clicked): [%.4f, %.4f) center≈%.4f • count=%d (%.2f%%)",
                    sel.bin, sel.xFrom, sel.xTo, sel.xCenter, sel.count, 100.0 * sel.fraction)
            );
        });

        // CDF -> TS
        cdfChart.setOnBinHover(sel -> {
            handleIncomingHighlight(sel.sampleIdx);
            selectionInfo.setText(
                String.format("CDF bin %d: [%.4f, %.4f) center≈%.4f • count=%d (%.2f%%)",
                    sel.bin, sel.xFrom, sel.xTo, sel.xCenter, sel.count, 100.0 * sel.fraction)
            );
        });
        cdfChart.setOnBinClick(sel -> {
            handleIncomingHighlight(sel.sampleIdx);
            selectionInfo.setText(
                String.format("CDF bin %d (clicked): [%.4f, %.4f) center≈%.4f • count=%d (%.2f%%)",
                    sel.bin, sel.xFrom, sel.xTo, sel.xCenter, sel.count, 100.0 * sel.fraction)
            );
        });

        // TS -> readout
        tsChart.setOnPointHover(p -> {
            if (tsMode == TSMode.CONTRIBUTION) {
                selectionInfo.setText(String.format("Sample #%d: Δ log-odds = %.6f", p.sampleIdx, p.y));
            } else if (tsMode == TSMode.CUMULATIVE) {
                selectionInfo.setText(String.format("Sample #%d: Cumulative log-odds = %.6f", p.sampleIdx, p.y));
            } else {
                StatisticResult stat = pdfChart.getLastStatisticResult();
                String extra = "";
                if (stat != null && stat.getSampleToBin() != null && p.sampleIdx >= 0 && p.sampleIdx < stat.getSampleToBin().length) {
                    int b = stat.getSampleToBin()[p.sampleIdx];
                    if (b >= 0 && stat.getBinEdges() != null && b + 1 < stat.getBinEdges().length) {
                        double from = stat.getBinEdges()[b];
                        double to = stat.getBinEdges()[b + 1];
                        extra = String.format(" • bin=%d [%.4f, %.4f)", b, from, to);
                    }
                }
                selectionInfo.setText(String.format("Sample #%d: value=%.6f%s", p.sampleIdx, p.y, extra));
            }
        });

        tsChart.setOnPointClick(p -> {
            handleIncomingHighlight(new int[]{p.sampleIdx});
            if (tsMode == TSMode.CONTRIBUTION) {
                selectionInfo.setText(String.format("Sample #%d (clicked): Δ log-odds = %.6f", p.sampleIdx, p.y));
            } else if (tsMode == TSMode.CUMULATIVE) {
                selectionInfo.setText(String.format("Sample #%d (clicked): Cumulative log-odds = %.6f", p.sampleIdx, p.y));
            } else {
                selectionInfo.setText(String.format("Sample #%d (clicked): value=%.6f", p.sampleIdx, p.y));
            }
        });
    }

    private void handleIncomingHighlight(int[] newIdx) {
        if (!persistSelection) {
            persistentHighlights.clear();
        }
        for (int idx : newIdx) persistentHighlights.add(idx);
        applyCurrentHighlights();
    }
}
