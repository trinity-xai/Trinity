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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Chart panel for visualizing PDF and CDF statistics (2D) and
 * triggering a 3D joint PDF/CDF surface computation.
 *
 * Adds "Precomputed Scalars" data source support:
 *  - Paste rows of "score" or "score, infoPercent" (both in [0,1])
 *  - Choose which field (Score or Info%) to visualize
 *  - Axis lock control to fix X to [0,1] for consistent comparisons
 *
 * Vectors mode remains unchanged; 3D surface is enabled only in Vectors mode.
 *
 * @author Sean Phillips
 */
public class StatPdfCdfChartPanel extends BorderPane {
    // Charts
    private StatPdfCdfChart pdfChart;
    private StatPdfCdfChart cdfChart;

    // Data source mode
    private enum DataSource { VECTORS, SCALARS }
    private ComboBox<DataSource> dataSourceCombo;

    // --- Scalars mode state/UI ---
    private List<Double> scalarScores = new ArrayList<>();
    private List<Double> scalarInfos  = new ArrayList<>();
    private ComboBox<String> scalarFieldCombo; // "Score" or "Info%"
    private Button pasteScalarsButton;
    private Button clearScalarsButton;

    // Axis lock (both modes)
    private CheckBox lockXCheck; // lock to [0,1]

    // --- Vectors mode controls/state ---
    private ComboBox<StatisticEngine.ScalarType> xFeatureCombo;
    private Spinner<Integer> binsSpinner;

    private ComboBox<String> metricCombo;
    private ComboBox<String> referenceCombo; // "Mean", "Vector @ Index", "Custom"
    private Spinner<Integer> xIndexSpinner;
    private Label xIndexLabel;

    // Y-feature (for 3D)
    private ComboBox<StatisticEngine.ScalarType> yFeatureCombo;
    private Spinner<Integer> yIndexSpinner;
    private Label yIndexLabel;

    private RadioButton pdfRadio3D;
    private RadioButton cdfRadio3D;
    private ToggleGroup surfaceToggle;

    private Button refresh2DButton;
    private Button popOutButton;
    private Button setCustomButton;
    private Button compute3DButton;

    // Data/state
    private List<FeatureVector> currentVectors = new ArrayList<>();
    private StatisticEngine.ScalarType currentXFeature = StatisticEngine.ScalarType.L1_NORM;
    private int currentBins = 40;
    private List<Double> customReferenceVector;

    // Callbacks
    private Runnable onPopOut = null;
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

        // ===== Row 0: Data source + Axis Lock =====
        dataSourceCombo = new ComboBox<>();
        dataSourceCombo.getItems().addAll(DataSource.VECTORS, DataSource.SCALARS);
        dataSourceCombo.setValue(DataSource.VECTORS);

        lockXCheck = new CheckBox("Lock X to [0,1]");
        lockXCheck.setSelected(false);
        lockXCheck.selectedProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                if (pdfChart != null) pdfChart.setLockXAxis(true, 0.0, 1.0);
                if (cdfChart != null) cdfChart.setLockXAxis(true, 0.0, 1.0);
            } else {
                if (pdfChart != null) pdfChart.setLockXAxis(false, 0.0, 1.0);
                if (cdfChart != null) cdfChart.setLockXAxis(false, 0.0, 1.0);
            }
        });

        HBox row0 = new HBox(16,
            new VBox(5, new Label("Data Source"), dataSourceCombo),
            new VBox(18, new Label(""), lockXCheck)
        );
        row0.setAlignment(Pos.CENTER_LEFT);
        row0.setPadding(new Insets(6, 6, 0, 6));

        // ===== Row 1: Scalars controls (hidden in Vectors mode) =====
        scalarFieldCombo = new ComboBox<>();
        scalarFieldCombo.getItems().addAll("Score", "Info%");
        scalarFieldCombo.setValue("Score");

        pasteScalarsButton = new Button("Paste Scalars…");
        clearScalarsButton = new Button("Clear Scalars");

        HBox rowScalars = new HBox(
            16,
            new VBox(5, new Label("Field (2D)"), scalarFieldCombo),
            pasteScalarsButton,
            clearScalarsButton
        );
        rowScalars.setAlignment(Pos.CENTER_LEFT);
        rowScalars.setPadding(new Insets(4, 6, 0, 6));

        // ===== Row 1 (Vectors): X feature + bins + actions =====
        xFeatureCombo = new ComboBox<>();
        xFeatureCombo.getItems().addAll(StatisticEngine.ScalarType.values());
        xFeatureCombo.setValue(currentXFeature);

        binsSpinner = new Spinner<>(5, 100, currentBins, 5);
        binsSpinner.setPrefWidth(100);
        binsSpinner.setPrefHeight(40);

        refresh2DButton = new Button("Refresh 2D");
        popOutButton = new Button("Pop Out");

        HBox row1Vectors = new HBox(
            16,
            new VBox(5, new Label("X Feature (2D)"), xFeatureCombo),
            new VBox(5, new Label("Bins"), binsSpinner),
            refresh2DButton,
            popOutButton
        );
        row1Vectors.setAlignment(Pos.CENTER_LEFT);
        row1Vectors.setPadding(new Insets(6, 6, 0, 6));

        // ===== Row 2 (Vectors): X metric/ref/index =====
        metricCombo = new ComboBox<>();
        metricCombo.getItems().addAll(Metric.getMetricNames());
        if (metricCombo.getItems().contains("euclidean")) metricCombo.setValue("euclidean");
        else if (!metricCombo.getItems().isEmpty()) metricCombo.setValue(metricCombo.getItems().get(0));

        referenceCombo = new ComboBox<>();
        referenceCombo.getItems().addAll("Mean", "Vector @ Index", "Custom");
        referenceCombo.setValue("Mean");

        xIndexLabel = new Label("X Index");
        xIndexSpinner = new Spinner<>();
        xIndexSpinner.setPrefWidth(100);

        setCustomButton = new Button("Set Custom Vector…");

        HBox row2Vectors = new HBox(
            16,
            new VBox(5, new Label("Metric (X)"), metricCombo),
            new VBox(5, new Label("Reference (X)"), referenceCombo),
            new VBox(5, xIndexLabel, xIndexSpinner),
            setCustomButton
        );
        row2Vectors.setAlignment(Pos.CENTER_LEFT);
        row2Vectors.setPadding(new Insets(4, 6, 0, 6));

        // ===== Row 3 (Vectors): Y feature + Y index + 3D surface =====
        yFeatureCombo = new ComboBox<>();
        yFeatureCombo.getItems().addAll(StatisticEngine.ScalarType.values());
        yFeatureCombo.setValue(StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION);

        yIndexLabel = new Label("Y Index");
        yIndexSpinner = new Spinner<>();
        yIndexSpinner.setPrefWidth(100);

        surfaceToggle = new ToggleGroup();
        pdfRadio3D = new RadioButton("PDF");
        pdfRadio3D.setToggleGroup(surfaceToggle);
        pdfRadio3D.setSelected(true);
        cdfRadio3D = new RadioButton("CDF");
        cdfRadio3D.setToggleGroup(surfaceToggle);

        HBox surfaceBox = new HBox(8, pdfRadio3D, cdfRadio3D);
        surfaceBox.setAlignment(Pos.CENTER_LEFT);

        compute3DButton = new Button("Compute 3D Surface");

        HBox row3Vectors = new HBox(
            16,
            new VBox(5, new Label("Y Feature (3D)"), yFeatureCombo),
            new VBox(5, yIndexLabel, yIndexSpinner),
            new VBox(5, new Label("Surface"), surfaceBox),
            compute3DButton
        );
        row3Vectors.setAlignment(Pos.CENTER_LEFT);
        row3Vectors.setPadding(new Insets(4, 6, 6, 6));

        // Grow hints
        HBox.setHgrow(row0, Priority.ALWAYS);
        HBox.setHgrow(rowScalars, Priority.ALWAYS);
        HBox.setHgrow(row1Vectors, Priority.ALWAYS);
        HBox.setHgrow(row2Vectors, Priority.ALWAYS);
        HBox.setHgrow(row3Vectors, Priority.ALWAYS);

        // Control area (we toggle rows depending on data source)
        VBox controlBar = new VBox(row0, rowScalars, row1Vectors, row2Vectors, row3Vectors);
        controlBar.setPadding(new Insets(0));

        // ===== Charts: PDF (top), CDF (bottom) =====
        pdfChart = new StatPdfCdfChart(currentXFeature, currentBins, StatPdfCdfChart.Mode.PDF_ONLY);
        cdfChart = new StatPdfCdfChart(currentXFeature, currentBins, StatPdfCdfChart.Mode.CDF_ONLY);

        if (!currentVectors.isEmpty()) {
            applyXFeatureOptions();
            pdfChart.setFeatureVectors(currentVectors);
            cdfChart.setFeatureVectors(currentVectors);
        }

        if (lockXCheck.isSelected()) {
            pdfChart.setLockXAxis(true, 0.0, 1.0);
            cdfChart.setLockXAxis(true, 0.0, 1.0);
        }

        VBox chartsBox = new VBox(8, pdfChart, cdfChart);
        chartsBox.setPadding(new Insets(6));
        VBox.setVgrow(pdfChart, Priority.ALWAYS);
        VBox.setVgrow(cdfChart, Priority.ALWAYS);

        setTop(controlBar);
        setCenter(chartsBox);

        // ===== Handlers =====

        // Data source toggle
        dataSourceCombo.valueProperty().addListener((obs, ov, nv) -> {
            boolean usingScalars = (nv == DataSource.SCALARS);
            rowScalars.setManaged(usingScalars);
            rowScalars.setVisible(usingScalars);

            row1Vectors.setManaged(!usingScalars);
            row1Vectors.setVisible(!usingScalars);
            row2Vectors.setManaged(!usingScalars);
            row2Vectors.setVisible(!usingScalars);
            row3Vectors.setManaged(!usingScalars);
            row3Vectors.setVisible(!usingScalars);

            if (usingScalars) {
                // Switch charts to raw scalar mode
                applyScalarSamplesToCharts();
                // Disable 3D compute in scalars mode
                compute3DButton.setDisable(true);
            } else {
                // Back to Vectors mode
                pdfChart.clearScalarSamples();
                cdfChart.clearScalarSamples();
                compute3DButton.setDisable(false);
                refresh2DCharts();
            }
        });
        // Initialize visibility (Vectors by default)
        rowScalars.setManaged(false);
        rowScalars.setVisible(false);

        // Scalars handlers
        pasteScalarsButton.setOnAction(e -> {
            ScalarInputResult res = DialogUtils.showScalarSamplesDialog();
            if (res == null) return; // cancelled
            if (res.scores != null && !res.scores.isEmpty()) scalarScores = res.scores;
            if (res.infos != null && !res.infos.isEmpty())   scalarInfos  = res.infos;
            applyScalarSamplesToCharts();
        });
        clearScalarsButton.setOnAction(e -> {
            scalarScores.clear();
            scalarInfos.clear();
            pdfChart.clearScalarSamples();
            cdfChart.clearScalarSamples();
        });
        scalarFieldCombo.valueProperty().addListener((obs, ov, nv) -> applyScalarSamplesToCharts());

        // Vectors handlers
        refresh2DButton.setOnAction(e -> refresh2DCharts());
        popOutButton.setOnAction(e -> { if (onPopOut != null) onPopOut.run(); });

        setCustomButton.setOnAction(e -> {
            // Optional: enforce expected dimension
            Integer expectedDim = (currentVectors != null && !currentVectors.isEmpty())
                    ? currentVectors.get(0).getData().size() : null;
            List<Double> parsed = (expectedDim != null)
                    ? DialogUtils.showCustomVectorDialog(expectedDim)
                    : DialogUtils.showCustomVectorDialog();
            if (parsed != null && !parsed.isEmpty()) setCustomReferenceVector(parsed);
        });

        compute3DButton.setOnAction(e -> compute3DSurface());

        xFeatureCombo.valueProperty().addListener((obs, ov, nv) -> {
            currentXFeature = nv;
            updateXControlEnablement();
            updateXIndexBoundsAndLabel();
        });
        referenceCombo.valueProperty().addListener((obs, ov, nv) -> {
            updateXControlEnablement();
            updateXIndexBoundsAndLabel();
        });
        yFeatureCombo.valueProperty().addListener((obs, ov, nv) -> {
            updateYControlEnablement();
            updateYIndexBoundsAndLabel();
        });

        // initialize states
        updateXControlEnablement();
        updateYControlEnablement();
        updateXIndexBoundsAndLabel();
        updateYIndexBoundsAndLabel();
    }

    // ----------------- Public API -----------------

    public boolean isSurfaceCDF() {
        return cdfRadio3D.isSelected();
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

    public void setOnPopOut(Runnable handler) { this.onPopOut = handler; }

    /** Consumer invoked when "Compute 3D Surface" finishes successfully. */
    public void setOnComputeSurface(Consumer<GridDensityResult> handler) {
        this.onComputeSurface = handler;
    }

    public void setFeatureVectors(List<FeatureVector> vectors) {
        this.currentVectors = (vectors != null) ? vectors : new ArrayList<>();
        updateXIndexBoundsAndLabel();
        updateYIndexBoundsAndLabel();
        if (dataSourceCombo.getValue() == DataSource.VECTORS) {
            applyXFeatureOptions();
            pdfChart.setFeatureVectors(this.currentVectors);
            cdfChart.setFeatureVectors(this.currentVectors);
        }
    }

    public void addFeatureVectors(List<FeatureVector> newVectors) {
        if (newVectors == null || newVectors.isEmpty()) return;
        if (this.currentVectors == null) this.currentVectors = new ArrayList<>(newVectors);
        else this.currentVectors.addAll(newVectors);

        updateXIndexBoundsAndLabel();
        updateYIndexBoundsAndLabel();
        if (dataSourceCombo.getValue() == DataSource.VECTORS) {
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
        if (dataSourceCombo.getValue() == DataSource.VECTORS
                && xFeatureCombo.getValue() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN
                && "Custom".equals(referenceCombo.getValue())
                && !currentVectors.isEmpty()) {
            applyXFeatureOptions();
            pdfChart.setFeatureVectors(currentVectors);
            cdfChart.setFeatureVectors(currentVectors);
        }
    }

    // ----------------- Scalars helpers -----------------

    private void applyScalarSamplesToCharts() {
        if (dataSourceCombo.getValue() != DataSource.SCALARS) return;

        List<Double> chosen = "Info%".equals(scalarFieldCombo.getValue()) ? scalarInfos : scalarScores;

        if (chosen == null || chosen.isEmpty()) {
            pdfChart.clearScalarSamples();
            cdfChart.clearScalarSamples();
            return;
        }
        pdfChart.setScalarSamples(chosen);
        cdfChart.setScalarSamples(chosen);

        // Respect lock checkbox
        boolean lock = lockXCheck.isSelected();
        pdfChart.setLockXAxis(lock, 0.0, 1.0);
        cdfChart.setLockXAxis(lock, 0.0, 1.0);
    }

    // ----------------- 2D (Vectors) behavior -----------------

    private void refresh2DCharts() {
        if (dataSourceCombo.getValue() != DataSource.VECTORS) return;

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

        boolean lock = lockXCheck.isSelected();
        pdfChart.setLockXAxis(lock, 0.0, 1.0);
        cdfChart.setLockXAxis(lock, 0.0, 1.0);
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
            String metricName = metricCombo.getValue();
            List<Double> ref = switch (referenceCombo.getValue()) {
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

    private void updateXControlEnablement() {
        boolean isMetric = xFeatureCombo.getValue() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN;
        boolean isComponent = xFeatureCombo.getValue() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION;

        metricCombo.setDisable(!isMetric);
        referenceCombo.setDisable(!isMetric);

        boolean indexEnabled = (isMetric && "Vector @ Index".equals(referenceCombo.getValue())) || isComponent;
        xIndexSpinner.setDisable(!indexEnabled);
    }

    private void updateXIndexBoundsAndLabel() {
        boolean isMetricVectorIdx = xFeatureCombo.getValue() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN
                && "Vector @ Index".equals(referenceCombo.getValue());
        boolean isComponent = xFeatureCombo.getValue() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION;

        if (isMetricVectorIdx) {
            int maxIdx = Math.max(0, getMaxVectorIndex());
            setSpinnerBounds(xIndexSpinner, 0, maxIdx, safeSpinnerValue(xIndexSpinner, 0, maxIdx));
            xIndexLabel.setText("X Index (Vector)");
        } else if (isComponent) {
            int maxDim = getMaxDimensionIndex();
            setSpinnerBounds(xIndexSpinner, 0, maxDim, safeSpinnerValue(xIndexSpinner, 0, maxDim));
            xIndexLabel.setText("X Index (Dimension)");
        } else {
            int maxDim = getMaxDimensionIndex();
            setSpinnerBounds(xIndexSpinner, 0, Math.max(0, maxDim), 0);
            xIndexLabel.setText("X Index");
        }
    }

    // ----------------- 3D compute (Vectors only) -----------------

    private void compute3DSurface() {
        if (onComputeSurface == null || currentVectors == null || currentVectors.isEmpty()) return;
        if (dataSourceCombo.getValue() != DataSource.VECTORS) return;

        AxisParams xAxis = new AxisParams();
        xAxis.setType(xFeatureCombo.getValue());
        if (xAxis.getType() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) {
            xAxis.setMetricName(metricCombo.getValue());
            List<Double> ref = switch (referenceCombo.getValue()) {
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
            List<Double> ref = (referenceCombo.getValue().equals("Custom"))
                    ? customReferenceVector
                    : (currentVectors.isEmpty() ? null : FeatureVector.getMeanVector(currentVectors));
            if ("Vector @ Index".equals(referenceCombo.getValue())) {
                ref = getVectorAtIndexAsList(xIndexSpinner.getValue());
            }
            yAxis.setReferenceVec(ref);
        }

        int bins = binsSpinner.getValue();
        GridSpec grid = new GridSpec(bins, bins);
        GridDensityResult result = GridDensity3DEngine.computePdfCdf2D(currentVectors, xAxis, yAxis, grid);
        onComputeSurface.accept(result);
    }

    private void updateYControlEnablement() {
        boolean yNeedsDim = yFeatureCombo.getValue() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION;
        yIndexSpinner.setDisable(!yNeedsDim);
    }

    private void updateYIndexBoundsAndLabel() {
        boolean yIsComponent = yFeatureCombo.getValue() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION;
        if (yIsComponent) {
            int maxDim = getMaxDimensionIndex();
            setSpinnerBounds(yIndexSpinner, 0, maxDim, safeSpinnerValue(yIndexSpinner, 0, maxDim));
            yIndexLabel.setText("Y Index (Dimension)");
        } else {
            int maxDim = getMaxDimensionIndex();
            setSpinnerBounds(yIndexSpinner, 0, Math.max(0, maxDim), 0);
            yIndexLabel.setText("Y Index");
        }
    }

    // ----------------- Utilities -----------------

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
