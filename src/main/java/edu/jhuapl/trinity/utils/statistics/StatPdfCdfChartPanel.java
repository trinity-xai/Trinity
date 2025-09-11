package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.metric.Metric;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Chart panel with GUI controls on top and TWO independent charts stacked vertically:
 *  - Top chart renders PDF-only
 *  - Bottom chart renders CDF-only
 *
 * Controls split across two rows:
 *  Row 1: Feature, Bins, Refresh, Pop Out
 *  Row 2: Metric, Reference, Index (shared), Set Custom Vector...
 *
 * Features:
 *  - METRIC_DISTANCE_TO_MEAN: choose metric; reference = Mean / Vector@Index / Custom
 *  - COMPONENT_AT_DIMENSION: choose a component index for the marginal using the same Index spinner
 *
 * Can be instantiated empty or with initial data.
 *
 * @author Sean Phillips
 */
public class StatPdfCdfChartPanel extends BorderPane {
    private StatPdfCdfChart pdfChart;
    private StatPdfCdfChart cdfChart;

    private ComboBox<StatisticEngine.ScalarType> scalarTypeCombo;
    private Spinner<Integer> binsSpinner;

    // Metric/reference controls (row 2)
    private ComboBox<String> metricCombo;
    private ComboBox<String> referenceCombo; // "Mean", "Vector @ Index", "Custom"

    // Shared index spinner for both:
    //  - Vector @ Index (reference selection)
    //  - COMPONENT_AT_DIMENSION (component index)
    private Spinner<Integer> indexSpinner;
    private Label indexLabel;

    // Custom vector storage
    private List<Double> customReferenceVector;

    private List<FeatureVector> currentVectors;
    private StatisticEngine.ScalarType currentScalarType;
    private int currentBins;

    private Runnable onPopOut = null;

    public StatPdfCdfChartPanel() {
        this(null, StatisticEngine.ScalarType.L1_NORM, 40);
    }

    public StatPdfCdfChartPanel(List<FeatureVector> initialVectors, StatisticEngine.ScalarType initialType, int initialBins) {
        this.currentVectors = (initialVectors != null) ? initialVectors : new ArrayList<>();
        this.currentScalarType = (initialType != null) ? initialType : StatisticEngine.ScalarType.L1_NORM;
        this.currentBins = (initialBins > 0) ? initialBins : 40;

        // ===== Row 1 controls =====
        scalarTypeCombo = new ComboBox<>();
        scalarTypeCombo.getItems().addAll(StatisticEngine.ScalarType.values());
        scalarTypeCombo.setValue(currentScalarType);

        binsSpinner = new Spinner<>(5, 100, currentBins, 5);
        binsSpinner.setPrefWidth(100);
        binsSpinner.setPrefHeight(40);

        Button refreshButton = new Button("Refresh");
        Button popOutButton = new Button("Pop Out");

        HBox row1 = new HBox(
            16,
            new VBox(5, new Label("Feature"), scalarTypeCombo),
            new VBox(5, new Label("Bins"), binsSpinner),
            refreshButton,
            popOutButton
        );
        row1.setAlignment(Pos.CENTER_LEFT);
        row1.setPadding(new Insets(6, 6, 0, 6));

        // ===== Row 2 controls =====
        metricCombo = new ComboBox<>();
        metricCombo.getItems().addAll(Metric.getMetricNames());
        if (metricCombo.getItems().contains("euclidean")) metricCombo.setValue("euclidean");
        else if (!metricCombo.getItems().isEmpty()) metricCombo.setValue(metricCombo.getItems().get(0));

        referenceCombo = new ComboBox<>();
        referenceCombo.getItems().addAll("Mean", "Vector @ Index", "Custom");
        referenceCombo.setValue("Mean");

        indexLabel = new Label("Index");
        indexSpinner = new Spinner<>();
        indexSpinner.setPrefWidth(100);

        Button setCustomButton = new Button("Set Custom Vector…");

        HBox row2 = new HBox(
            16,
            new VBox(5, new Label("Metric"), metricCombo),
            new VBox(5, new Label("Reference"), referenceCombo),
            new VBox(5, indexLabel, indexSpinner),
            setCustomButton
        );
        row2.setAlignment(Pos.CENTER_LEFT);
        row2.setPadding(new Insets(4, 6, 6, 6));

        // Make rows stretch nicely if needed
        HBox.setHgrow(row1, Priority.ALWAYS);
        HBox.setHgrow(row2, Priority.ALWAYS);

        VBox controlBar = new VBox(row1, row2);
        controlBar.setPadding(new Insets(0));

        // ===== Charts: PDF (top), CDF (bottom) =====
        pdfChart = new StatPdfCdfChart(currentScalarType, currentBins, StatPdfCdfChart.Mode.PDF_ONLY);
        cdfChart = new StatPdfCdfChart(currentScalarType, currentBins, StatPdfCdfChart.Mode.CDF_ONLY);

        if (!currentVectors.isEmpty()) {
            applyFeatureSpecificOptions(); // sets metric/ref or component index
            pdfChart.setFeatureVectors(currentVectors);
            cdfChart.setFeatureVectors(currentVectors);
        }

        VBox chartsBox = new VBox(8, pdfChart, cdfChart);
        chartsBox.setPadding(new Insets(2));
        VBox.setVgrow(pdfChart, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(cdfChart, javafx.scene.layout.Priority.ALWAYS);

        setTop(controlBar);
        setCenter(chartsBox);

        // ===== Actions =====
        refreshButton.setOnAction(e -> refreshCharts());
        popOutButton.setOnAction(e -> { if (onPopOut != null) onPopOut.run(); });

        // Paste/import custom vector via dialog
        setCustomButton.setOnAction(e -> {
            List<Double> parsed = showCustomVectorDialog();
            if (parsed != null && !parsed.isEmpty()) {
                setCustomReferenceVector(parsed);
            }
        });

        // Inter-control enable/disable + index label/bounds management
        scalarTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentScalarType = newVal;
            updateControlsEnablement();
            updateIndexSpinnerBoundsAndLabel(); // reacts to feature change
        });

        referenceCombo.valueProperty().addListener((obs, o, nv) -> {
            updateControlsEnablement();
            updateIndexSpinnerBoundsAndLabel(); // reacts to reference change
        });

        // Initialize UI states
        updateControlsEnablement();
        updateIndexSpinnerBoundsAndLabel();
    }

    // ===== Public API =====

    public void setOnPopOut(Runnable handler) {
        this.onPopOut = handler;
    }

    public void setFeatureVectors(List<FeatureVector> vectors) {
        this.currentVectors = (vectors != null) ? vectors : new ArrayList<>();
        updateIndexSpinnerBoundsAndLabel();
        applyFeatureSpecificOptions();
        pdfChart.setFeatureVectors(this.currentVectors);
        cdfChart.setFeatureVectors(this.currentVectors);
    }

    public void addFeatureVectors(List<FeatureVector> newVectors) {
        if (newVectors == null || newVectors.isEmpty()) return;
        if (this.currentVectors == null) this.currentVectors = new ArrayList<>(newVectors);
        else this.currentVectors.addAll(newVectors);

        updateIndexSpinnerBoundsAndLabel();
        applyFeatureSpecificOptions();
        pdfChart.addFeatureVectors(newVectors);
        cdfChart.addFeatureVectors(newVectors);
    }

    /** Provide a custom reference vector for metric distance mode. */
    public void setCustomReferenceVector(List<Double> customVector) {
        this.customReferenceVector = customVector;
        if (scalarTypeCombo.getValue() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN
                && "Custom".equals(referenceCombo.getValue())) {
            applyFeatureSpecificOptions();
            if (!currentVectors.isEmpty()) {
                pdfChart.setFeatureVectors(currentVectors);
                cdfChart.setFeatureVectors(currentVectors);
            }
        }
    }

    public int getBins() { return binsSpinner.getValue(); }
    public StatisticEngine.ScalarType getScalarType() { return scalarTypeCombo.getValue(); }
    public StatPdfCdfChart getPdfChart() { return pdfChart; }
    public StatPdfCdfChart getCdfChart() { return cdfChart; }

    // ===== Internal: behavior =====

    private void refreshCharts() {
        currentScalarType = scalarTypeCombo.getValue();
        currentBins = binsSpinner.getValue();

        pdfChart.setScalarType(currentScalarType);
        pdfChart.setBins(currentBins);

        cdfChart.setScalarType(currentScalarType);
        cdfChart.setBins(currentBins);

        applyFeatureSpecificOptions();

        if (currentVectors != null && !currentVectors.isEmpty()) {
            pdfChart.setFeatureVectors(currentVectors);
            cdfChart.setFeatureVectors(currentVectors);
        }
    }

    private void applyFeatureSpecificOptions() {
        // Clear residuals first
        pdfChart.setMetricNameForGeneric(null);
        pdfChart.setReferenceVectorForGeneric(null);
        pdfChart.setComponentIndex(null);

        cdfChart.setMetricNameForGeneric(null);
        cdfChart.setReferenceVectorForGeneric(null);
        cdfChart.setComponentIndex(null);

        if (currentScalarType == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) {
            String metricName = metricCombo.getValue();
            List<Double> ref = switch (referenceCombo.getValue()) {
                case "Vector @ Index" -> getVectorAtIndexAsList(indexSpinner.getValue());
                case "Custom" -> customReferenceVector;
                default -> currentVectors.isEmpty() ? null : FeatureVector.getMeanVector(currentVectors);
            };

            pdfChart.setMetricNameForGeneric(metricName);
            pdfChart.setReferenceVectorForGeneric(ref);

            cdfChart.setMetricNameForGeneric(metricName);
            cdfChart.setReferenceVectorForGeneric(ref);
        } else if (currentScalarType == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION) {
            pdfChart.setComponentIndex(indexSpinner.getValue());
            cdfChart.setComponentIndex(indexSpinner.getValue());
        }
    }

    private void updateControlsEnablement() {
        boolean isMetric = scalarTypeCombo.getValue() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN;
        boolean isComponent = scalarTypeCombo.getValue() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION;

        metricCombo.setDisable(!isMetric);
        referenceCombo.setDisable(!isMetric);

        // The single Index spinner is enabled if:
        //  - metric + "Vector @ Index", OR
        //  - component feature is selected
        boolean indexEnabled = (isMetric && "Vector @ Index".equals(referenceCombo.getValue())) || isComponent;
        indexSpinner.setDisable(!indexEnabled);
    }

    private void updateIndexSpinnerBoundsAndLabel() {
        // Decide what the index means right now & set bounds accordingly
        boolean isMetricVectorIdx = scalarTypeCombo.getValue() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN
                && "Vector @ Index".equals(referenceCombo.getValue());
        boolean isComponent = scalarTypeCombo.getValue() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION;

        if (isMetricVectorIdx) {
            // bounds: 0..(N-1)
            int maxIdx = Math.max(0, getMaxVectorIndex());
            setSpinnerBounds(indexSpinner, 0, maxIdx, Math.min(indexSpinner.getValue() == null ? 0 : indexSpinner.getValue(), maxIdx));
            indexLabel.setText("Index (Vector)");
        } else if (isComponent) {
            // bounds: 0..(D-1)
            int maxDim = getMaxDimensionIndex();
            setSpinnerBounds(indexSpinner, 0, maxDim, Math.min(indexSpinner.getValue() == null ? 0 : indexSpinner.getValue(), maxDim));
            indexLabel.setText("Index (Dimension)");
        } else {
            // Not applicable right now; keep bounds reasonable but disabled by enablement logic
            int maxDim = getMaxDimensionIndex();
            setSpinnerBounds(indexSpinner, 0, Math.max(0, maxDim), 0);
            indexLabel.setText("Index");
        }
    }

    private static void setSpinnerBounds(Spinner<Integer> spinner, int min, int max, int value) {
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, Math.max(min, max), value, 1));
        spinner.getValueFactory().setValue(Math.max(min, Math.min(max, value)));
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

    /** Simple paste dialog for a custom reference vector (comma/space separated doubles). */
    private List<Double> showCustomVectorDialog() {
        Dialog<List<Double>> dialog = new Dialog<>();
        dialog.setTitle("Set Custom Reference Vector");
        dialog.getDialogPane().getButtonTypes().addAll(javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);

        TextArea ta = new TextArea();
        ta.setPromptText("Paste numbers separated by commas or spaces, e.g.\n0.12, -1.3, 2.5, 0.0");
        ta.setPrefRowCount(6);
        dialog.getDialogPane().setContent(ta);

        dialog.setResultConverter(bt -> {
            if (bt == javafx.scene.control.ButtonType.OK) {
                String text = ta.getText();
                List<Double> parsed = parseDoubles(text);
                return (parsed == null || parsed.isEmpty()) ? null : parsed;
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private static List<Double> parseDoubles(String s) {
        if (s == null || s.isBlank()) return null;
        String norm = s.replaceAll("[\\n\\t]", " ").replaceAll(",", " ");
        String[] parts = norm.trim().split("\\s+");
        List<Double> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            try {
                out.add(Double.valueOf(p));
            } catch (NumberFormatException ignore) {
                // skip tokens that are not numbers
            }
        }
        return out;
    }
}
