package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.metric.Metric;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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
 * Minimal additions:
 *  - Y feature + Y index
 *  - "Compute 3D Surface" button
 *
 * 2D charts still use X feature controls (existing).
 *
 * @author Sean Phillips
 */
public class StatPdfCdfChartPanel extends BorderPane {
    private StatPdfCdfChart pdfChart;
    private StatPdfCdfChart cdfChart;

    // Existing X-feature controls
    private ComboBox<StatisticEngine.ScalarType> xFeatureCombo;
    private Spinner<Integer> binsSpinner;

    // Metric/reference shared with X (unchanged)
    private ComboBox<String> metricCombo;
    private ComboBox<String> referenceCombo; // "Mean", "Vector @ Index", "Custom"
    private Spinner<Integer> xIndexSpinner;
    private Label xIndexLabel;

    // Y-feature (new, minimal)
    private ComboBox<StatisticEngine.ScalarType> yFeatureCombo;
    private Spinner<Integer> yIndexSpinner;
    private Label yIndexLabel;

    private RadioButton pdfRadio;
    private RadioButton cdfRadio;
    private ToggleGroup surfaceToggle;    
    
    // Custom vector storage (used if X uses Custom)
    private List<Double> customReferenceVector;

    private List<FeatureVector> currentVectors = new ArrayList<>();
    private StatisticEngine.ScalarType currentXFeature = StatisticEngine.ScalarType.L1_NORM;
    private int currentBins = 40;

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

        // ===== Row 1: X feature + bins + 2D actions =====
        xFeatureCombo = new ComboBox<>();
        xFeatureCombo.getItems().addAll(StatisticEngine.ScalarType.values());
        xFeatureCombo.setValue(currentXFeature);

        binsSpinner = new Spinner<>(5, 100, currentBins, 5);
        binsSpinner.setPrefWidth(100);
        binsSpinner.setPrefHeight(40);

        Button refreshButton = new Button("Refresh 2D");
        Button popOutButton = new Button("Pop Out");

        HBox row1 = new HBox(
            16,
            new VBox(5, new Label("X Feature (2D)"), xFeatureCombo),
            new VBox(5, new Label("Bins"), binsSpinner),
            refreshButton,
            popOutButton
        );
        row1.setAlignment(Pos.CENTER_LEFT);
        row1.setPadding(new Insets(6, 6, 0, 6));

        // ===== Row 2: X metric/ref/index =====
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

        Button setCustomButton = new Button("Set Custom Vector…");

        HBox row2 = new HBox(
            16,
            new VBox(5, new Label("Metric (X)"), metricCombo),
            new VBox(5, new Label("Reference (X)"), referenceCombo),
            new VBox(5, xIndexLabel, xIndexSpinner),
            setCustomButton
        );
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.setPadding(new Insets(4, 6, 0, 6));

        // ===== Row 3: Y feature + Y index + 3D action =====
        yFeatureCombo = new ComboBox<>();
        yFeatureCombo.getItems().addAll(StatisticEngine.ScalarType.values());
        yFeatureCombo.setValue(StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION);

        yIndexLabel = new Label("Y Index");
        yIndexSpinner = new Spinner<>();
        yIndexSpinner.setPrefWidth(100);

        surfaceToggle = new ToggleGroup();

        pdfRadio = new RadioButton("PDF");
        pdfRadio.setToggleGroup(surfaceToggle);
        pdfRadio.setSelected(true); // default

        cdfRadio = new RadioButton("CDF");
        cdfRadio.setToggleGroup(surfaceToggle);

        HBox surfaceBox = new HBox(8, pdfRadio, cdfRadio);
        surfaceBox.setAlignment(Pos.CENTER_LEFT);

        Button compute3DButton = new Button("Compute 3D Surface");
        
        HBox row3 = new HBox(
            16,
            new VBox(5, new Label("Y Feature (3D)"), yFeatureCombo),
            new VBox(5, yIndexLabel, yIndexSpinner),
            new VBox(5, new Label("Surface"), surfaceBox),
            compute3DButton
        );

        row3.setAlignment(Pos.CENTER_LEFT);
        row3.setPadding(new Insets(4, 6, 6, 6));

        HBox.setHgrow(row1, Priority.ALWAYS);
        HBox.setHgrow(row2, Priority.ALWAYS);
        HBox.setHgrow(row3, Priority.ALWAYS);

        VBox controlBar = new VBox(row1, row2, row3);
        controlBar.setPadding(new Insets(0));

        // ===== Charts: PDF (top), CDF (bottom) =====
        pdfChart = new StatPdfCdfChart(currentXFeature, currentBins, StatPdfCdfChart.Mode.PDF_ONLY);
        cdfChart = new StatPdfCdfChart(currentXFeature, currentBins, StatPdfCdfChart.Mode.CDF_ONLY);

        if (!currentVectors.isEmpty()) {
            applyXFeatureOptions();
            pdfChart.setFeatureVectors(currentVectors);
            cdfChart.setFeatureVectors(currentVectors);
        }

        VBox chartsBox = new VBox(8, pdfChart, cdfChart);
        chartsBox.setPadding(new Insets(6));
        VBox.setVgrow(pdfChart, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(cdfChart, javafx.scene.layout.Priority.ALWAYS);

        setTop(controlBar);
        setCenter(chartsBox);

        // ===== Handlers =====
        refreshButton.setOnAction(e -> refresh2DCharts());
        popOutButton.setOnAction(e -> { if (onPopOut != null) onPopOut.run(); });

        setCustomButton.setOnAction(e -> {
            List<Double> parsed = DialogUtils.showCustomVectorDialog();
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

        // initialize control states
        updateXControlEnablement();
        updateYControlEnablement();
        updateXIndexBoundsAndLabel();
        updateYIndexBoundsAndLabel();
    }

    // ----------------- Public API -----------------
    public boolean isSurfaceCDF() {
        return cdfRadio.isSelected();
    }
    /**
     * Get a human-friendly description of the current Y feature type.
     * Includes the component index if the Y feature is COMPONENT_AT_DIMENSION.
     */
    public String getYFeatureTypeForDisplay() {
        if (yFeatureCombo == null) {
            return "N/A";
        }
        StatisticEngine.ScalarType type = yFeatureCombo.getValue();
        if (type == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION) {
            Integer idx = yIndexSpinner.getValue();
            return "COMPONENT_AT_DIMENSION[" + (idx != null ? idx : "?") + "]";
        }
        return type != null ? type.name() : "N/A";
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
        applyXFeatureOptions();
        pdfChart.setFeatureVectors(this.currentVectors);
        cdfChart.setFeatureVectors(this.currentVectors);
    }

    public void addFeatureVectors(List<FeatureVector> newVectors) {
        if (newVectors == null || newVectors.isEmpty()) return;
        if (this.currentVectors == null) this.currentVectors = new ArrayList<>(newVectors);
        else this.currentVectors.addAll(newVectors);

        updateXIndexBoundsAndLabel();
        updateYIndexBoundsAndLabel();
        applyXFeatureOptions();
        pdfChart.addFeatureVectors(newVectors);
        cdfChart.addFeatureVectors(newVectors);
    }

    public int getBins() { return binsSpinner.getValue(); }
    public StatisticEngine.ScalarType getScalarType() { return xFeatureCombo.getValue(); }
    public StatPdfCdfChart getPdfChart() { return pdfChart; }
    public StatPdfCdfChart getCdfChart() { return cdfChart; }

    public void setCustomReferenceVector(List<Double> customVector) {
        this.customReferenceVector = customVector;
        if (xFeatureCombo.getValue() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN
                && "Custom".equals(referenceCombo.getValue())
                && !currentVectors.isEmpty()) {
            applyXFeatureOptions();
            pdfChart.setFeatureVectors(currentVectors);
            cdfChart.setFeatureVectors(currentVectors);
        }
    }

    // ----------------- 2D (X feature) behavior -----------------

    private void refresh2DCharts() {
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

    // ----------------- 3D compute (new) -----------------

    private void compute3DSurface() {
        if (onComputeSurface == null || currentVectors == null || currentVectors.isEmpty()) return;

        // Build X axis params from existing controls
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

        // Build Y axis params (minimal: support component index and mirror metric/ref if used)
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
