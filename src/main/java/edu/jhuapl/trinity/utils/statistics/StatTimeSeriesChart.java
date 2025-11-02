package edu.jhuapl.trinity.utils.statistics;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * Simple time-series chart that plots scalar values against sample index (0..N-1)
 * and supports highlighting arbitrary sample indices, plus hover/click callbacks
 * that report the nearest sample index under the mouse.
 * <p>
 * Now supports "append" highlights for a persist-selection workflow.
 */
public class StatTimeSeriesChart extends LineChart<Number, Number> {

    /**
     * Public event payload
     *
     * @param x index
     * @param y value
     */
    public record PointSelection(int sampleIdx, double x, double y) {
    }

    // ===== Series =====
    private final XYChart.Series<Number, Number> lineSeries = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> highlightSeries = new XYChart.Series<>();

    // ===== Data =====
    private List<Double> currentValues = new ArrayList<>();
    private final BitSet highlighted = new BitSet(); // tracks which indices are currently highlighted

    // ===== Callbacks =====
    private Consumer<PointSelection> onHover;
    private Consumer<PointSelection> onClick;

    public StatTimeSeriesChart() {
        super(new NumberAxis(), new NumberAxis());
        setAnimated(false);
        setLegendVisible(false);
        setTitle("Scalar Time Series");

        NumberAxis xAxis = (NumberAxis) getXAxis();
        NumberAxis yAxis = (NumberAxis) getYAxis();
        xAxis.setLabel("Sample Index");
        yAxis.setLabel("Scalar Value");
        xAxis.setForceZeroInRange(false);
        yAxis.setForceZeroInRange(false);

        // We want a clean line for the main series and point symbols only for highlights.
        // createSymbols(true) is chart-wide; we'll hide symbols on the main series manually.
        setCreateSymbols(true);

        getData().add(lineSeries);
        getData().add(highlightSeries);

        // Make highlight series draw only symbols (no connecting stroke).
        Platform.runLater(() -> {
            if (highlightSeries.getNode() != null) {
                highlightSeries.getNode().setStyle("-fx-stroke: transparent;");
            }
        });

        attachPlotInteractions();
    }

    // ===== Public API =====

    /**
     * Replace the entire series (x = sample index 0..N-1, y = values[i]).
     */
    public void setSeries(List<Double> values) {
        currentValues = (values != null) ? new ArrayList<>(values) : new ArrayList<>();
        lineSeries.getData().clear();
        clearHighlights(); // also resets BitSet

        for (int i = 0; i < currentValues.size(); i++) {
            lineSeries.getData().add(new XYChart.Data<>(i, currentValues.get(i)));
        }

        // Hide symbols for the main line, but keep them for highlight points.
        Platform.runLater(() -> {
            hideSymbolsForSeries(lineSeries, true);
            if (highlightSeries.getNode() != null) {
                highlightSeries.getNode().setStyle("-fx-stroke: transparent;");
            }
            styleHighlightDots();
        });
    }

    /**
     * Highlight a set of sample indices (draws visible symbols at those indices),
     * replacing any existing highlights (backward-compatible behavior).
     */
    public void highlightSamples(int[] indices) {
        highlightSamples(indices, false);
    }

    /**
     * Highlight a set of sample indices, optionally appending to any existing highlights.
     *
     * @param indices indices to (add) highlight
     * @param append  if false, replaces existing highlights; if true, appends/dedupes
     */
    public void highlightSamples(int[] indices, boolean append) {
        if (!append) clearHighlights();
        addHighlights(indices);
    }

    /**
     * Append highlights without removing existing ones.
     */
    public void addHighlights(int[] indices) {
        if (indices == null || currentValues.isEmpty()) return;

        for (int idx : indices) {
            if (idx >= 0 && idx < currentValues.size() && !highlighted.get(idx)) {
                highlighted.set(idx);
                Double y = currentValues.get(idx);
                XYChart.Data<Number, Number> d = new XYChart.Data<>(idx, y);
                highlightSeries.getData().add(d);
            }
        }

        Platform.runLater(() -> {
            if (highlightSeries.getNode() != null) {
                highlightSeries.getNode().setStyle("-fx-stroke: transparent;");
            }
            styleHighlightDots();
        });
    }

    /**
     * Clear any highlighted samples.
     */
    public void clearHighlights() {
        highlightSeries.getData().clear();
        highlighted.clear();
    }

    /**
     * Optional: customize axis labels.
     */
    public void setAxisLabels(String xLabel, String yLabel) {
        if (xLabel != null) getXAxis().setLabel(xLabel);
        if (yLabel != null) getYAxis().setLabel(yLabel);
    }

    public void setOnPointHover(Consumer<PointSelection> handler) {
        this.onHover = handler;
    }

    public void setOnPointClick(Consumer<PointSelection> handler) {
        this.onClick = handler;
    }

    // ===== Internals =====

    private void attachPlotInteractions() {
        Platform.runLater(() -> {
            Node plotArea = lookup(".chart-plot-background");
            if (plotArea == null) return;

            plotArea.setOnMouseMoved(evt -> {
                if (onHover == null || currentValues.isEmpty()) return;
                Integer idx = indexFromPlotPixel(plotArea, evt.getX());
                if (idx == null) return;
                double y = currentValues.get(idx);
                onHover.accept(new PointSelection(idx, idx, y));
            });

            plotArea.setOnMouseClicked(evt -> {
                if (onClick == null || currentValues.isEmpty()) return;
                Integer idx = indexFromPlotPixel(plotArea, evt.getX());
                if (idx == null) return;
                double y = currentValues.get(idx);
                onClick.accept(new PointSelection(idx, idx, y));
            });
        });
    }

    private Integer indexFromPlotPixel(Node plotArea, double xInPlotLocal) {
        try {
            Point2D scenePt = plotArea.localToScene(xInPlotLocal, 0);
            Point2D axisPt = getXAxis().sceneToLocal(scenePt);
            double xVal = getXAxis().getValueForDisplay(axisPt.getX()).doubleValue();
            if (currentValues == null || currentValues.isEmpty()) return null;
            int idx = (int) Math.round(xVal);
            if (idx < 0) idx = 0;
            if (idx >= currentValues.size()) idx = currentValues.size() - 1;
            return idx;
        } catch (Exception ex) {
            return null;
        }
    }

    private static void hideSymbolsForSeries(XYChart.Series<Number, Number> series, boolean hide) {
        for (XYChart.Data<Number, Number> d : series.getData()) {
            Node n = d.getNode();
            if (n != null) n.setVisible(!hide);
        }
    }

    private void styleHighlightDots() {
        for (XYChart.Data<Number, Number> d : highlightSeries.getData()) {
            if (d.getNode() != null) {
                d.getNode().setStyle(
                    "-fx-background-color: #00FF00AA; " +
                        "-fx-background-radius: 8px; -fx-padding: 6px;"
                );
            }
        }
    }
}
