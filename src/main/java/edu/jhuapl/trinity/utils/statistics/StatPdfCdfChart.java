package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LineChart that renders EITHER PDF-only or CDF-only for a chosen scalar type.
 * Use two instances if you want separate axes (one for PDF, one for CDF).
 *
 * Supports:
 *  - METRIC_DISTANCE_TO_MEAN via metric name and reference vector
 *  - COMPONENT_AT_DIMENSION via componentIndex
 *
 * @author Sean Phillips
 */
public class StatPdfCdfChart extends LineChart<Number, Number> {

    public enum Mode { PDF_ONLY, CDF_ONLY }

    private StatisticEngine.ScalarType scalarType;
    private int bins;
    private Mode mode;
    private List<FeatureVector> vectors;

    // Metric mode
    private String metricNameForGeneric;
    private List<Double> referenceVectorForGeneric;

    // Component marginal mode
    private Integer componentIndex;

    public StatPdfCdfChart(StatisticEngine.ScalarType scalarType, int bins, Mode mode) {
        super(new NumberAxis(), new NumberAxis());
        this.scalarType = (scalarType != null) ? scalarType : StatisticEngine.ScalarType.L1_NORM;
        this.bins = Math.max(5, bins);
        this.mode = (mode != null) ? mode : Mode.PDF_ONLY;
        this.vectors = new ArrayList<>();

        NumberAxis xAxis = (NumberAxis) getXAxis();
        NumberAxis yAxis = (NumberAxis) getYAxis();
        xAxis.setLabel("Scalar Value");
        yAxis.setLabel(this.mode == Mode.PDF_ONLY ? "Density (PDF)" : "Probability (CDF)");
        yAxis.setForceZeroInRange(false);

        setAnimated(false);
        setLegendVisible(false);
        setCreateSymbols(false);
        setTitle((this.mode == Mode.PDF_ONLY ? "PDF" : "CDF") + " • " + this.scalarType);
    }

    public void setScalarType(StatisticEngine.ScalarType scalarType) {
        this.scalarType = (scalarType != null) ? scalarType : StatisticEngine.ScalarType.L1_NORM;
        setTitle((this.mode == Mode.PDF_ONLY ? "PDF" : "CDF") + " • " + this.scalarType);
        refresh();
    }

    public void setBins(int bins) {
        this.bins = Math.max(5, bins);
        refresh();
    }

    public void setMode(Mode mode) {
        this.mode = (mode != null) ? mode : Mode.PDF_ONLY;
        NumberAxis yAxis = (NumberAxis) getYAxis();
        yAxis.setLabel(this.mode == Mode.PDF_ONLY ? "Density (PDF)" : "Probability (CDF)");
        setTitle((this.mode == Mode.PDF_ONLY ? "PDF" : "CDF") + " • " + this.scalarType);
        refresh();
    }

    public void setFeatureVectors(List<FeatureVector> vectors) {
        this.vectors = (vectors != null) ? new ArrayList<>(vectors) : new ArrayList<>();
        refresh();
    }

    public void addFeatureVectors(List<FeatureVector> newVectors) {
        if (newVectors == null || newVectors.isEmpty()) return;
        if (this.vectors == null) this.vectors = new ArrayList<>(newVectors);
        else this.vectors.addAll(newVectors);
        refresh();
    }

    // Metric mode
    public void setMetricNameForGeneric(String metricNameForGeneric) {
        this.metricNameForGeneric = metricNameForGeneric;
        refresh();
    }

    public void setReferenceVectorForGeneric(List<Double> referenceVectorForGeneric) {
        this.referenceVectorForGeneric = referenceVectorForGeneric;
        refresh();
    }

    // Component marginal mode
    public void setComponentIndex(Integer componentIndex) {
        this.componentIndex = componentIndex;
        refresh();
    }

    private void refresh() {
        getData().clear();
        if (vectors == null || vectors.isEmpty()) return;

        Set<StatisticEngine.ScalarType> types = Set.of(scalarType);

        String metricName = (scalarType == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) ? metricNameForGeneric : null;
        List<Double> refVec = (scalarType == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) ? referenceVectorForGeneric : null;

        Integer compIdx = (scalarType == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION) ? componentIndex : null;

        Map<StatisticEngine.ScalarType, StatisticResult> resultMap =
                StatisticEngine.computeStatistics(vectors, types, bins, metricName, refVec, compIdx);

        StatisticResult stat = resultMap.get(scalarType);
        if (stat == null) return;

        double[] x = stat.getPdfBins();
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        if (mode == Mode.PDF_ONLY) {
            double[] y = stat.getPdf();
            for (int i = 0; i < x.length; i++) series.getData().add(new XYChart.Data<>(x[i], y[i]));
        } else {
            double[] y = stat.getCdf();
            for (int i = 0; i < x.length; i++) series.getData().add(new XYChart.Data<>(x[i], y[i]));
        }
        getData().add(series);
    }

    public StatisticEngine.ScalarType getScalarType() { return scalarType; }
    public int getBins() { return bins; }
    public Mode getMode() { return mode; }
    public String getMetricNameForGeneric() { return metricNameForGeneric; }
    public List<Double> getReferenceVectorForGeneric() { return referenceVectorForGeneric; }
    public Integer getComponentIndex() { return componentIndex; }
}
