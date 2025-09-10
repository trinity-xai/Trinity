package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.List;
import java.util.Set;

public class StatPdfCdfChart extends LineChart<Number, Number> {

    private StatisticEngine.ScalarType scalarType;
    private int pdfBins;

    public StatPdfCdfChart(StatisticEngine.ScalarType scalarType, int pdfBins) {
        super(new NumberAxis(), new NumberAxis());
        this.scalarType = scalarType;
        this.pdfBins = pdfBins;

        setTitle(scalarType + " (PDF and CDF)");
        getXAxis().setLabel("Scalar Value");
        getYAxis().setLabel("Density / Probability");
        setLegendVisible(true);
    }

    /**
     * Set the input vectors, recompute, and update the plot.
     */
    public void setFeatureVectors(List<FeatureVector> vectors) {
        updateChart(vectors);
    }

    private void updateChart(List<FeatureVector> vectors) {
        getData().clear();

        Set<StatisticEngine.ScalarType> types = Set.of(scalarType);
        StatisticResult stat = StatisticEngine.computeStatistics(
                vectors, types, pdfBins, null, null
        ).get(scalarType);

        // PDF line
        XYChart.Series<Number, Number> pdfSeries = new XYChart.Series<>();
        pdfSeries.setName("PDF");
        double[] bins = stat.getPdfBins();
        double[] pdf = stat.getPdf();
        for (int i = 0; i < bins.length; i++) {
            pdfSeries.getData().add(new XYChart.Data<>(bins[i], pdf[i]));
        }

        // CDF line
        XYChart.Series<Number, Number> cdfSeries = new XYChart.Series<>();
        cdfSeries.setName("CDF");
        double[] cdf = stat.getCdf();
        for (int i = 0; i < bins.length; i++) {
            cdfSeries.getData().add(new XYChart.Data<>(bins[i], cdf[i]));
        }

        getData().addAll(pdfSeries, cdfSeries);

        // Optional: distinct styling for CDF line
        cdfSeries.getNode().setStyle("-fx-stroke: #ff7f0e; -fx-stroke-dash-array: 8 4;"); // orange dashed
        pdfSeries.getNode().setStyle("-fx-stroke: #1f77b4;"); // blue solid
    }

    /**
     * Optional: Support adding vectors (appending) instead of replacing.
     */
    public void addFeatureVectors(List<FeatureVector> vectors) {
        // Combine current and new vectors, then call setFeatureVectors(...)
        // (implement as needed)
    }
}
