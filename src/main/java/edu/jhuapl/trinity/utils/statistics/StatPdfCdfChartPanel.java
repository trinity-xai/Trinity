package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.metric.Metric;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Chart panel with GUI controls on top and TWO independent charts stacked vertically:
 * - Top chart renders PDF-only
 * - Bottom chart renders CDF-only
 * Controls (Feature type and Bins) update both charts in sync.
 *
 * Supports METRIC_DISTANCE_TO_MEAN by letting user choose a metric and using the mean vector as reference.
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
    private ComboBox<String> metricCombo; // NEW

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

        // Controls
        scalarTypeCombo = new ComboBox<>();
        scalarTypeCombo.getItems().addAll(StatisticEngine.ScalarType.values());
        scalarTypeCombo.setValue(currentScalarType);

        binsSpinner = new Spinner<>(5, 100, currentBins, 5);
        binsSpinner.setPrefWidth(100);
        binsSpinner.setPrefHeight(40);

        // NEW: Metric selector (disabled unless METRIC_DISTANCE_TO_MEAN)
        metricCombo = new ComboBox<>();
        metricCombo.getItems().addAll(Metric.getMetricNames());
        // try to default to euclidean if present
        if (metricCombo.getItems().contains("euclidean")) {
            metricCombo.setValue("euclidean");
        } else if (!metricCombo.getItems().isEmpty()) {
            metricCombo.setValue(metricCombo.getItems().get(0));
        }
        metricCombo.setDisable(currentScalarType != StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN);

        Button refreshButton = new Button("Refresh");
        Button popOutButton = new Button("Pop Out");

        HBox controlBar = new HBox(
            20,
            new VBox(5, new Label("Feature:"), scalarTypeCombo),
            new VBox(5, new Label("Bins:"), binsSpinner),
            new VBox(5, new Label("Metric:"), metricCombo),
            refreshButton,
            popOutButton
        );
        controlBar.setPadding(new Insets(5));
        controlBar.setAlignment(Pos.CENTER);

        // Charts: PDF (top), CDF (bottom)
        pdfChart = new StatPdfCdfChart(currentScalarType, currentBins, StatPdfCdfChart.Mode.PDF_ONLY);
        cdfChart = new StatPdfCdfChart(currentScalarType, currentBins, StatPdfCdfChart.Mode.CDF_ONLY);

        if (!currentVectors.isEmpty()) {
            // If metric feature selected, set metric & reference (mean)
            applyMetricOptionsIfNeeded();
            pdfChart.setFeatureVectors(currentVectors);
            cdfChart.setFeatureVectors(currentVectors);
        }

        VBox chartsBox = new VBox(8, pdfChart, cdfChart);
        chartsBox.setPadding(new Insets(6));
        VBox.setVgrow(pdfChart, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(cdfChart, javafx.scene.layout.Priority.ALWAYS);

        setTop(controlBar);
        setCenter(chartsBox);

        // Actions
        refreshButton.setOnAction(e -> refreshCharts());
        popOutButton.setOnAction(e -> { if (onPopOut != null) onPopOut.run(); });

        // Keep metric control enabled state in sync with feature selection
        scalarTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isMetric = newVal == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN;
            metricCombo.setDisable(!isMetric);
        });
    }

    private void refreshCharts() {
        currentScalarType = scalarTypeCombo.getValue();
        currentBins = binsSpinner.getValue();

        pdfChart.setScalarType(currentScalarType);
        pdfChart.setBins(currentBins);

        cdfChart.setScalarType(currentScalarType);
        cdfChart.setBins(currentBins);

        // NEW: set metric + reference vector if needed
        applyMetricOptionsIfNeeded();

        if (currentVectors != null && !currentVectors.isEmpty()) {
            pdfChart.setFeatureVectors(currentVectors);
            cdfChart.setFeatureVectors(currentVectors);
        }
    }

    private void applyMetricOptionsIfNeeded() {
        if (currentScalarType == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) {
            // Reference = mean vector of current data
            List<Double> meanVector = FeatureVector.getMeanVector(currentVectors);
            String metricName = metricCombo.getValue();

            pdfChart.setMetricNameForGeneric(metricName);
            pdfChart.setReferenceVectorForGeneric(meanVector);

            cdfChart.setMetricNameForGeneric(metricName);
            cdfChart.setReferenceVectorForGeneric(meanVector);
        } else {
            // Clear any prior settings so other features don't accidentally use them
            pdfChart.setMetricNameForGeneric(null);
            pdfChart.setReferenceVectorForGeneric(null);
            cdfChart.setMetricNameForGeneric(null);
            cdfChart.setReferenceVectorForGeneric(null);
        }
    }

    public void setOnPopOut(Runnable handler) {
        this.onPopOut = handler;
    }

    public void setFeatureVectors(List<FeatureVector> vectors) {
        this.currentVectors = (vectors != null) ? vectors : new ArrayList<>();
        // NEW: keep metric options correct when data change
        applyMetricOptionsIfNeeded();
        pdfChart.setFeatureVectors(this.currentVectors);
        cdfChart.setFeatureVectors(this.currentVectors);
    }

    public void addFeatureVectors(List<FeatureVector> newVectors) {
        if (newVectors == null || newVectors.isEmpty()) return;
        if (this.currentVectors == null) this.currentVectors = new ArrayList<>(newVectors);
        else this.currentVectors.addAll(newVectors);

        // Keep metric options correct when appending data
        applyMetricOptionsIfNeeded();
        pdfChart.addFeatureVectors(newVectors);
        cdfChart.addFeatureVectors(newVectors);
    }

    public int getBins() {
        return binsSpinner.getValue();
    }

    public StatisticEngine.ScalarType getScalarType() {
        return scalarTypeCombo.getValue();
    }

    public StatPdfCdfChart getPdfChart() {
        return pdfChart;
    }

    public StatPdfCdfChart getCdfChart() {
        return cdfChart;
    }
}
