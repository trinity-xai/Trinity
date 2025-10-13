package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.metric.Metric;
import edu.jhuapl.trinity.utils.statistics.DialogUtils.ScalarInputResult;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * PDF, CDF, and a time-series chart (vertical stack) with linked interactions,
 * label filtering, and Similarity / Contribution / Cumulative modes.
 * <p>
 * No scroll panes, no wildcard imports, and no inline CSS.
 * <p>
 * Top-K contributors live to the right of the time-series in a fixed-width panel.
 *
 * @author Sean Phillips
 */
public class StatPdfCdfChartPanel extends BorderPane {

    // ===== Charts =====
    private StatPdfCdfChart pdfChart;
    private StatPdfCdfChart cdfChart;
    private StatTimeSeriesChart tsChart;

    // ===== Top-K contributors UI =====
    private Spinner<Integer> topKSpinner;
    private ListView<String> topKList;
    private final ObservableList<String> topKItems = FXCollections.observableArrayList();
    private final List<Integer> topKIndices = new ArrayList<>();
    private int topKFixedWidth = 220;   // fixed width for side panel
    private int topKSpinnerWidth = 80;  // fixed width for spinner

    // Cap time-series row height (no pref height set)
    private double tsMaxHeight = 280.0;

    // ===== Data source mode =====
    private enum DataSource {VECTORS, SCALARS}

    private DataSource dataSource = DataSource.VECTORS;

    // Scalars mode state/UI
    private List<Double> scalarScores = new ArrayList<>();
    private List<Double> scalarInfos = new ArrayList<>();
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

    // Time-series mode & aggregator
    private enum TSMode {SIMILARITY, CONTRIBUTION, CUMULATIVE}

    private TSMode tsMode = TSMode.CONTRIBUTION;
    private StatisticEngine.SimilarityAggregator agg = StatisticEngine.SimilarityAggregator.MEAN;
    private final double contribEps = 1e-6;
    private HBox tsRow;
    // Selection
    private boolean persistSelection = false;
    private final Label selectionInfo = new Label("—");

    // ===== Label filter =====
    private static final String UNLABELED = "(unlabeled)";
    private List<String> allLabels = new ArrayList<>();
    private List<String> selectedLabels = new ArrayList<>();
    private final Map<String, CheckBox> labelChecks = new LinkedHashMap<>();
    private Menu labelsMenu; // built in Options
    private CustomMenuItem labelsCustom;
    private VBox labelsBox;
    // Callback
    private Consumer<GridDensityResult> onComputeSurface = null;

    public StatPdfCdfChartPanel() {
        this(null, StatisticEngine.ScalarType.L1_NORM, 40);
    }

    public StatPdfCdfChartPanel(List<FeatureVector> initialVectors,
                                StatisticEngine.ScalarType initialType,
                                int initialBins) {
        if (initialVectors != null) currentVectors = initialVectors;
        if (initialType != null) currentXFeature = initialType;
        if (initialBins > 0) currentBins = initialBins;

        // --- Visible row: X Feature (2D), Bins, X Dim (always visible), Refresh, Options ---
        xFeatureCombo = new ComboBox<>();
        xFeatureCombo.getItems().addAll(StatisticEngine.ScalarType.values());
        xFeatureCombo.setValue(currentXFeature);

        binsSpinner = new Spinner<>(5, 100, currentBins, 5);
        binsSpinner.setPrefWidth(100);
        binsSpinner.setPrefHeight(34);

        // Always-visible X dimension spinner
        xIndexSpinner = new Spinner<>();
        xIndexSpinner.setPrefWidth(100);
        xIndexSpinner.setMinWidth(100);
        xIndexSpinner.setMaxWidth(100);
        xIndexSpinner.valueProperty().addListener((obs, ov, nv) -> {
            if (nv == null) return;
            if (dataSource == DataSource.VECTORS) refresh2DCharts();
        });

        Button refresh2DButton = new Button("Refresh");
        refresh2DButton.setOnAction(e -> refresh2DCharts());

        MenuButton optionsMenu = buildOptionsMenu();

        HBox topBar = new HBox(12,
            new VBox(2, new Label("X Feature"), xFeatureCombo),
            new VBox(2, new Label("Bins"), binsSpinner),
            new VBox(2, new Label("X Dim"), xIndexSpinner),
            refresh2DButton,
            optionsMenu
        );
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(6, 8, 4, 8));
        HBox.setHgrow(optionsMenu, Priority.NEVER);

        // --- Charts: PDF (top), CDF (middle), TS+TopK (bottom) ---
        pdfChart = new StatPdfCdfChart(currentXFeature, currentBins, StatPdfCdfChart.Mode.PDF_ONLY);
        cdfChart = new StatPdfCdfChart(currentXFeature, currentBins, StatPdfCdfChart.Mode.CDF_ONLY);
        tsChart = new StatTimeSeriesChart();

        // Build the Top-K panel and place it to the right of the time-series
        VBox topKPanel = buildTopKPanel();
        this.tsRow = new HBox(8, tsChart, topKPanel);   // <-- use the FIELD, not a local
        HBox.setHgrow(tsChart, Priority.ALWAYS);
        this.tsRow.setFillHeight(true);

        // Initial data
        if (!currentVectors.isEmpty()) {
            rebuildLabelsFromCurrentVectors();
            applyXFeatureOptions();
            List<FeatureVector> use = getFilteredVectors();
            pdfChart.setFeatureVectors(use);
            cdfChart.setFeatureVectors(use);
            updateTimeSeries(); // also refreshes Top-K
        }
        if (lockXToUnit) {
            pdfChart.setLockXAxis(true, 0.0, 1.0);
            cdfChart.setLockXAxis(true, 0.0, 1.0);
        }

        wireChartInteractions();

        Button clearBtn = new Button("Clear Highlights");
        clearBtn.setOnAction(e -> {
            tsChart.clearHighlights();
            selectionInfo.setText("—");
        });

        HBox bottomBar = new HBox(12, new Label("Selection:"), selectionInfo, new Region(), clearBtn);
        HBox.setHgrow(bottomBar.getChildren().get(2), Priority.ALWAYS);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(2, 8, 8, 8));

        // Container
        VBox chartsBox = new VBox(8, pdfChart, cdfChart, this.tsRow, bottomBar);
        chartsBox.setPadding(new Insets(6));
        chartsBox.setFillWidth(true);

        // Make all three main sections share vertical space equally
        enforceEqualSectionHeights();

        setTop(topBar);
        setCenter(chartsBox);

        xFeatureCombo.valueProperty().addListener((obs, ov, nv) -> {
            currentXFeature = nv;
            updateXControlEnablement();
            updateXIndexBoundsAndValue();
        });
    }

    /**
     * Force PDF, CDF, and TS rows to share vertical space and not inflate the parent.
     */
    private void enforceEqualSectionHeights() {
        // Give each section a reasonable minimum so they don't collapse
        pdfChart.setMinHeight(120);
        cdfChart.setMinHeight(120);
        tsChart.setMinHeight(120);

        // Shrink preferred heights so the parent does NOT expand to large computed prefs.
        // (Charts default to a large prefHeight; zeroing it avoids vertical oversizing.)
        pdfChart.setPrefHeight(0);
        cdfChart.setPrefHeight(0);
        this.tsRow.setPrefHeight(0);

        // Let VBox distribute extra space equally across all three
        VBox.setVgrow(pdfChart, Priority.ALWAYS);
        VBox.setVgrow(cdfChart, Priority.ALWAYS);
        VBox.setVgrow(this.tsRow, Priority.ALWAYS);
    }

    // ===== Top-K panel =====
    private VBox buildTopKPanel() {
        Label title = new Label("Top-K Contributors");
        Label kLabel = new Label("K:");
        topKSpinner = new Spinner<>();
        topKSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 10, 1));
        topKSpinner.setPrefWidth(topKSpinnerWidth);
        topKSpinner.setMinWidth(topKSpinnerWidth);
        topKSpinner.setMaxWidth(topKSpinnerWidth);
        topKSpinner.valueProperty().addListener((obs, ov, nv) -> refreshTopK());

        HBox header = new HBox(6, title, new Region(), kLabel, topKSpinner);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        topKList = new ListView<>(topKItems);
        // Fixed width for the whole side panel
        VBox panel = new VBox(6, header, topKList);
        panel.setPadding(new Insets(0, 0, 0, 0));
        panel.setMinWidth(topKFixedWidth);
        panel.setPrefWidth(topKFixedWidth);
        panel.setMaxWidth(topKFixedWidth);
        VBox.setVgrow(topKList, Priority.ALWAYS); // list consumes vertical space inside panel only

        // Clicking an item highlights that sample
        topKList.setOnMouseClicked(e -> {
            int idx = topKList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < topKIndices.size()) {
                int sampleIdx = topKIndices.get(idx);
                tsChart.clearHighlights();
                tsChart.highlightSamples(new int[]{sampleIdx});
                selectionInfo.setText("Top-K picked sample #" + sampleIdx);
            }
        });

        return panel;
    }

    private void refreshTopK() {
        List<Double> series = getCurrentTSValues();
        topKItems.clear();
        topKIndices.clear();
        if (series == null || series.isEmpty()) return;

        int k = Math.max(1, Math.min(topKSpinner.getValue(), series.size()));
        List<int[]> pairs = new ArrayList<>(series.size());
        for (int i = 0; i < series.size(); i++) {
            double v = series.get(i) == null ? 0.0 : series.get(i);
            pairs.add(new int[]{i, (int) Math.round(Math.abs(v) * 1_000_000)}); // keep stable sort with abs magnitude
        }
        pairs.sort((a, b) -> Integer.compare(b[1], a[1]));

        for (int i = 0; i < k; i++) {
            int idx = pairs.get(i)[0];
            double val = series.get(idx) == null ? 0.0 : series.get(idx);
            topKIndices.add(idx);
            topKItems.add(String.format("#%d  |  Δ=%.6f", idx, val));
        }
    }

    private List<Double> getCurrentTSValues() {
        List<FeatureVector> use = getFilteredVectors();
        if (dataSource == DataSource.SCALARS) {
            // For pasted scalars, only Similarity mode is supported
            return new ArrayList<>(scalarField.equals("Info%") ? scalarInfos : scalarScores);
        }
        if (use == null || use.isEmpty()) return new ArrayList<>();

        if (tsMode == TSMode.CONTRIBUTION) {
            StatisticEngine.ContributionSeries cs =
                StatisticEngine.computeContributions(use, agg, null, contribEps);
            return cs.delta;
        } else if (tsMode == TSMode.CUMULATIVE) {
            StatisticEngine.ContributionSeries cs =
                StatisticEngine.computeContributions(use, agg, null, contribEps);
            return StatisticEngine.cumulativeFromDeltas(cs.delta);
        } else {
            // SIMILARITY: show the same scalar values that fed the PDF/CDF
            StatisticResult stat = (pdfChart != null) ? pdfChart.getLastStatisticResult() : null;
            List<Double> vals = (stat != null) ? stat.getValues() : null;
            return (vals != null) ? new ArrayList<>(vals) : new ArrayList<>();
        }
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
        persistSel.selectedProperty().addListener((o, oldV, nv) -> persistSelection = nv);

        // Scalars submenu
        Menu scalarsMenu = new Menu("Scalars");
        ToggleGroup scalarFieldGroup = new ToggleGroup();

        RadioMenuItem scoreItem = new RadioMenuItem("Field: Score");
        scoreItem.setToggleGroup(scalarFieldGroup);
        scoreItem.setSelected("Score".equals(scalarField));
        scoreItem.setOnAction(e -> {
            scalarField = "Score";
            applyScalarSamplesToCharts();
        });

        RadioMenuItem infoItem = new RadioMenuItem("Field: Info%");
        infoItem.setToggleGroup(scalarFieldGroup);
        infoItem.setSelected("Info%".equals(scalarField));
        infoItem.setOnAction(e -> {
            scalarField = "Info%";
            applyScalarSamplesToCharts();
        });

        MenuItem pasteScalars = new MenuItem("Paste Scalars…");
        pasteScalars.setOnAction(e -> {
            ScalarInputResult res = DialogUtils.showScalarSamplesDialog();
            if (res == null) return;
            if (res.scores != null && !res.scores.isEmpty()) scalarScores = res.scores;
            if (res.infos != null && !res.infos.isEmpty()) scalarInfos = res.infos;
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
            refreshTopK();
        });

        scalarsMenu.getItems().addAll(scoreItem, infoItem, new SeparatorMenuItem(), pasteScalars, clearScalars);

        // ----- Labels Filter submenu (custom content that stays open) -----
        labelsMenu = new Menu("Labels (Filter)");
        labelsBox = new VBox(6);
        labelsBox.setPadding(new Insets(6, 10, 6, 10));
        labelsCustom = new CustomMenuItem(labelsBox);
        labelsCustom.setHideOnClick(false); // keep menu open while clicking checkboxes
        labelsMenu.getItems().setAll(labelsCustom); // single custom content node

        // Metric/Reference (X) section (no X Index here anymore)
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
        yIndexSpinner.setMinWidth(100);
        yIndexSpinner.setMaxWidth(100);

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

        // Time-Series Mode & Aggregator
        Menu tsModeMenu = new Menu("Time-Series Mode");
        ToggleGroup tsModeGroup = new ToggleGroup();
        RadioMenuItem tsSimilarity = new RadioMenuItem("Similarity (matches PDF feature)");
        tsSimilarity.setToggleGroup(tsModeGroup);
        tsSimilarity.setSelected(tsMode == TSMode.SIMILARITY);
        tsSimilarity.setOnAction(e -> {
            tsMode = TSMode.SIMILARITY;
            updateTimeSeries();
        });

        RadioMenuItem tsContribution = new RadioMenuItem("Contribution (Δ log-odds)");
        tsContribution.setToggleGroup(tsModeGroup);
        tsContribution.setSelected(tsMode == TSMode.CONTRIBUTION);
        tsContribution.setOnAction(e -> {
            tsMode = TSMode.CONTRIBUTION;
            updateTimeSeries();
        });

        RadioMenuItem tsCumulative = new RadioMenuItem("Cumulative log-odds");
        tsCumulative.setToggleGroup(tsModeGroup);
        tsCumulative.setSelected(tsMode == TSMode.CUMULATIVE);
        tsCumulative.setOnAction(e -> {
            tsMode = TSMode.CUMULATIVE;
            updateTimeSeries();
        });

        tsModeMenu.getItems().addAll(tsSimilarity, tsContribution, tsCumulative);

        Menu aggregatorMenu = new Menu("Aggregator (Contribution)");
        ToggleGroup aggGroup = new ToggleGroup();
        RadioMenuItem aggMean = new RadioMenuItem("Mean");
        RadioMenuItem aggGeo = new RadioMenuItem("Geomean");
        RadioMenuItem aggMin = new RadioMenuItem("Min");
        aggMean.setToggleGroup(aggGroup);
        aggGeo.setToggleGroup(aggGroup);
        aggMin.setToggleGroup(aggGroup);
        aggMean.setSelected(agg == StatisticEngine.SimilarityAggregator.MEAN);
        aggGeo.setSelected(agg == StatisticEngine.SimilarityAggregator.GEOMEAN);
        aggMin.setSelected(agg == StatisticEngine.SimilarityAggregator.MIN);
        aggMean.setOnAction(e -> {
            agg = StatisticEngine.SimilarityAggregator.MEAN;
            if (tsMode != TSMode.SIMILARITY) updateTimeSeries();
        });
        aggGeo.setOnAction(e -> {
            agg = StatisticEngine.SimilarityAggregator.GEOMEAN;
            if (tsMode != TSMode.SIMILARITY) updateTimeSeries();
        });
        aggMin.setOnAction(e -> {
            agg = StatisticEngine.SimilarityAggregator.MIN;
            if (tsMode != TSMode.SIMILARITY) updateTimeSeries();
        });
        aggregatorMenu.getItems().addAll(aggMean, aggGeo, aggMin);

        // Final assembly
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

        // Populate labels panel with current state (if vectors already set)
        rebuildLabelsMenu();

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

    /**
     * Consumer invoked when "Compute 3D Surface" finishes successfully.
     */
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

    public int getBins() {
        return binsSpinner.getValue();
    }

    public StatisticEngine.ScalarType getScalarType() {
        return xFeatureCombo.getValue();
    }

    public StatPdfCdfChart getPdfChart() {
        return pdfChart;
    }

    public StatPdfCdfChart getCdfChart() {
        return cdfChart;
    }

    public StatTimeSeriesChart getTimeSeriesChart() {
        return tsChart;
    }

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
        public List<String> selectedLabels;
        public String tsMode; // "SIMILARITY" / "CONTRIBUTION" / "CUMULATIVE"
        public String aggregator; // "MEAN","GEOMEAN","MIN"
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
        s.scalarInfos = new ArrayList<>(scalarInfos);
        s.customRef = (customReferenceVector == null) ? null : new ArrayList<>(customReferenceVector);
        s.persistSelection = persistSelection;
        s.selectedLabels = new ArrayList<>(selectedLabels);
        s.tsMode = this.tsMode.name();
        s.aggregator = this.agg.name();
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
        scalarInfos = (s.scalarInfos != null) ? new ArrayList<>(s.scalarInfos) : new ArrayList<>();
        customReferenceVector = (s.customRef != null) ? new ArrayList<>(s.customRef) : null;
        persistSelection = s.persistSelection;

        if (s.selectedLabels != null) selectedLabels = new ArrayList<>(s.selectedLabels);
        if (s.tsMode != null) tsMode = TSMode.valueOf(s.tsMode);
        if (s.aggregator != null) agg = StatisticEngine.SimilarityAggregator.valueOf(s.aggregator);

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
            refreshTopK();
            return;
        }
        pdfChart.setScalarSamples(chosen);
        cdfChart.setScalarSamples(chosen);

        // For pasted scalars, show Similarity
        tsMode = TSMode.SIMILARITY;
        tsChart.setAxisLabels("Sample Index", "Scalar Value");
        tsChart.setSeries(chosen);

        pdfChart.setLockXAxis(lockXToUnit, 0.0, 1.0);
        cdfChart.setLockXAxis(lockXToUnit, 0.0, 1.0);
        refreshTopK();
    }

    // ===== 2D (Vectors) behavior =====

    private void refresh2DCharts() {
        if (dataSource != DataSource.VECTORS) return;
        if (null == pdfChart || null == cdfChart) return;
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
            refreshTopK();
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

        if (metricCombo != null) {
            metricCombo.setDisable(!isMetric);
        }

        // Keep the spinner ALWAYS visible; only disable when it doesn't apply
        boolean indexEnabled = (isMetric && "Vector @ Index".equals(referenceMode)) || isComponent;
        if (xIndexSpinner != null) {
            xIndexSpinner.setDisable(!indexEnabled);
            // NOTE: do NOT toggle visible/managed here; we want it shown at all times.
        }
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
        if (labelsMenu == null || labelsBox == null) return;

        labelsBox.getChildren().clear();
        labelChecks.clear();

        if (allLabels.isEmpty()) {
            labelsBox.getChildren().add(new Label("No labels found"));
            return;
        }

        // Header row with All / None / Apply (doesn't close the menu)
        Label lbl = new Label("Visible labels:");
        Button bAll = new Button("All");
        Button bNone = new Button("None");
        Button bApply = new Button("Apply");
        HBox header = new HBox(8, lbl, new Region(), bAll, bNone, bApply);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox list = new VBox(4);
        for (String lab : allLabels) {
            CheckBox cb = new CheckBox(lab);
            cb.setSelected(selectedLabels.contains(lab));
            cb.selectedProperty().addListener((obs, ov, nv) -> {
                if (nv) {
                    if (!selectedLabels.contains(lab)) selectedLabels.add(lab);
                } else {
                    selectedLabels.remove(lab);
                }
            });
            labelChecks.put(lab, cb);
            list.getChildren().add(cb);
        }

        bAll.setOnAction(e -> {
            selectedLabels = new ArrayList<>(allLabels);
            for (CheckBox cb : labelChecks.values()) cb.setSelected(true);
        });
        bNone.setOnAction(e -> {
            selectedLabels.clear();
            for (CheckBox cb : labelChecks.values()) cb.setSelected(false);
        });
        bApply.setOnAction(e -> refresh2DCharts());

        labelsBox.getChildren().addAll(header, list);
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
// ===== Linking & time-series update =====
    private void updateTimeSeries() {
        if (dataSource == DataSource.SCALARS) {
            // handled in applyScalarSamplesToCharts()
            refreshTopK();
            return;
        }
        List<FeatureVector> use = getFilteredVectors();
        if (use == null || use.isEmpty()) {
            tsChart.setSeries(new ArrayList<>());
            selectionInfo.setText("—");
            refreshTopK();
            return;
        }

        if (tsMode == TSMode.CONTRIBUTION) {
            StatisticEngine.ContributionSeries cs =
                StatisticEngine.computeContributions(use, agg, null, contribEps);
            tsChart.setAxisLabels("Sample Index", "Δ log-odds");
            tsChart.setSeries(cs.delta);
        } else if (tsMode == TSMode.CUMULATIVE) {
            StatisticEngine.ContributionSeries cs =
                StatisticEngine.computeContributions(use, agg, null, contribEps);
            List<Double> cum = StatisticEngine.cumulativeFromDeltas(cs.delta);
            tsChart.setAxisLabels("Sample Index", "Cumulative log-odds");
            tsChart.setSeries(cum);
        } else { // SIMILARITY
            StatisticResult stat = (pdfChart != null) ? pdfChart.getLastStatisticResult() : null;
            List<Double> vals = (stat != null) ? stat.getValues() : new ArrayList<>();
            tsChart.setAxisLabels("Sample Index", "Scalar Value");
            tsChart.setSeries(vals);
        }

        // Make sure the Top-K list reflects the current TS mode/series
        refreshTopK();
    }


    private void wireChartInteractions() {
        // PDF -> TS
        pdfChart.setOnBinHover(sel -> {
            if (!persistSelection) tsChart.clearHighlights();
            tsChart.highlightSamples(sel.sampleIdx);
            selectionInfo.setText(
                String.format("PDF bin %d: [%.4f, %.4f) center≈%.4f • count=%d (%.2f%%)",
                    sel.bin, sel.xFrom, sel.xTo, sel.xCenter, sel.count, 100.0 * sel.fraction)
            );
        });
        pdfChart.setOnBinClick(sel -> {
            tsChart.highlightSamples(sel.sampleIdx);
            selectionInfo.setText(
                String.format("PDF bin %d (clicked): [%.4f, %.4f) center≈%.4f • count=%d (%.2f%%)",
                    sel.bin, sel.xFrom, sel.xTo, sel.xCenter, sel.count, 100.0 * sel.fraction)
            );
        });

        // CDF -> TS
        cdfChart.setOnBinHover(sel -> {
            if (!persistSelection) tsChart.clearHighlights();
            tsChart.highlightSamples(sel.sampleIdx);
            selectionInfo.setText(
                String.format("CDF bin %d: [%.4f, %.4f) center≈%.4f • count=%d (%.2f%%)",
                    sel.bin, sel.xFrom, sel.xTo, sel.xCenter, sel.count, 100.0 * sel.fraction)
            );
        });
        cdfChart.setOnBinClick(sel -> {
            tsChart.highlightSamples(sel.sampleIdx);
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
                selectionInfo.setText(String.format("Sample #%d: cumulative log-odds = %.6f", p.sampleIdx, p.y));
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
            if (!persistSelection) tsChart.clearHighlights();
            tsChart.highlightSamples(new int[]{p.sampleIdx});
            if (tsMode == TSMode.CONTRIBUTION) {
                selectionInfo.setText(String.format("Sample #%d (clicked): Δ log-odds = %.6f", p.sampleIdx, p.y));
            } else if (tsMode == TSMode.CUMULATIVE) {
                selectionInfo.setText(String.format("Sample #%d (clicked): cumulative log-odds = %.6f", p.sampleIdx, p.y));
            } else {
                selectionInfo.setText(String.format("Sample #%d (clicked): value=%.6f", p.sampleIdx, p.y));
            }
        });
    }
}
