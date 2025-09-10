package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

/**
 * Chart panel for visualizing PDF and CDF statistics for Trinity analytics.
 * Can be instantiated empty or with initial data.
 *
 * @author Sean Phillips
 */
public class StatPdfCdfChartPanel extends BorderPane {
    private StatPdfCdfChart chart;
    private ComboBox<StatisticEngine.ScalarType> scalarTypeCombo;
    private Spinner<Integer> binsSpinner;
    private List<FeatureVector> currentVectors;
    private StatisticEngine.ScalarType currentScalarType;
    private int currentBins;

    private Runnable onPopOut = null;

    /**
     * Create an empty chart panel (no data, uses default scalar and bins).
     */
    public StatPdfCdfChartPanel() {
        this(null, StatisticEngine.ScalarType.NORM, 40);
    }

    /**
     * Create a chart panel with initial data and options.
     */
    public StatPdfCdfChartPanel(List<FeatureVector> initialVectors, StatisticEngine.ScalarType initialType, int initialBins) {
        this.currentVectors = (initialVectors != null) ? initialVectors : new ArrayList<>();
        this.currentScalarType = (initialType != null) ? initialType : StatisticEngine.ScalarType.NORM;
        this.currentBins = (initialBins > 0) ? initialBins : 40;

        scalarTypeCombo = new ComboBox<>();
        scalarTypeCombo.getItems().addAll(StatisticEngine.ScalarType.values());
        scalarTypeCombo.setValue(currentScalarType);

        binsSpinner = new Spinner<>(5, 100, currentBins, 5);
        binsSpinner.setPrefWidth(100);
        binsSpinner.setPrefHeight(40);
        
        Button refreshButton = new Button("Refresh");
        Button popOutButton = new Button("Pop Out");

        HBox controlBar = new HBox(20, 
            new VBox(5, new Label("Feature:"), scalarTypeCombo),
            new VBox(5, new Label("Bins:"), binsSpinner), 
            refreshButton, popOutButton);
        controlBar.setPadding(new Insets(5));
        controlBar.setAlignment(Pos.CENTER);

        // Chart
        chart = new StatPdfCdfChart(currentScalarType, currentBins);
        if (!currentVectors.isEmpty()) {
            chart.setFeatureVectors(currentVectors);
        }

        setTop(controlBar);
        setCenter(chart);

        refreshButton.setOnAction(e -> refreshChart());
        popOutButton.setOnAction(e -> { if (onPopOut != null) onPopOut.run(); });
    }

    private void refreshChart() {
        currentScalarType = scalarTypeCombo.getValue();
        currentBins = binsSpinner.getValue();
        chart = new StatPdfCdfChart(currentScalarType, currentBins);
        if (currentVectors != null && !currentVectors.isEmpty()) {
            chart.setFeatureVectors(currentVectors);
        }
        setCenter(chart);
    }

    public void setOnPopOut(Runnable handler) {
        this.onPopOut = handler;
    }

    public void setFeatureVectors(List<FeatureVector> vectors) {
        this.currentVectors = (vectors != null) ? vectors : new ArrayList<>();
        chart.setFeatureVectors(currentVectors);
    }

    public int getBins() {
        return binsSpinner.getValue();
    }

    public StatisticEngine.ScalarType getScalarType() {
        return scalarTypeCombo.getValue();
    }

    public StatPdfCdfChart getChart() {
        return chart;
    }
}
