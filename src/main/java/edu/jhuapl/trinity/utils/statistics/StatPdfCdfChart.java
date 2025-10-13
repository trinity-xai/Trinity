package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class StatPdfCdfChart extends LineChart<Number, Number> {

    public enum Mode {PDF_ONLY, CDF_ONLY}

    private StatisticEngine.ScalarType scalarType;
    private int bins;
    private Mode mode;
    private List<FeatureVector> vectors;

    // Metric mode
    private String metricNameForGeneric;
    private List<Double> referenceVectorForGeneric;

    // Component marginal mode
    private Integer componentIndex;

    // X-axis lock
    private boolean lockXAxis = false;
    private double lockLower = 0.0;
    private double lockUpper = 1.0;

    // Presentation
    private boolean showSymbols = true;
    private double seriesStrokeWidth = 2.0;

    // --- raw-sample override for 2D charts ---
    private List<Double> scalarSamples = null; // if non-null & non-empty, use this instead of vectors

    //retain last stat and expose interactions
    private StatisticResult lastStat = null;

    public static final class BinSelection {
        public final int bin;
        public final double xCenter;
        public final double xFrom, xTo;
        public final int count;
        public final double fraction;
        public final int[] sampleIdx;

        public BinSelection(int bin, double xCenter, double xFrom, double xTo,
                            int count, double fraction, int[] sampleIdx) {
            this.bin = bin;
            this.xCenter = xCenter;
            this.xFrom = xFrom;
            this.xTo = xTo;
            this.count = count;
            this.fraction = fraction;
            this.sampleIdx = sampleIdx;
        }
    }

    private Consumer<BinSelection> onBinHover;
    private Consumer<BinSelection> onBinClick;

    public void setOnBinHover(Consumer<BinSelection> h) {
        this.onBinHover = h;
    }

    public void setOnBinClick(Consumer<BinSelection> h) {
        this.onBinClick = h;
    }

    public StatisticResult getLastStatisticResult() {
        return lastStat;
    }

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
        setCreateSymbols(showSymbols);
        setTitle(titleForCurrentState());
    }

    // ---- configuration ----

    public void setScalarType(StatisticEngine.ScalarType scalarType) {
        this.scalarType = (scalarType != null) ? scalarType : StatisticEngine.ScalarType.L1_NORM;
        setTitle(titleForCurrentState());
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
        setTitle(titleForCurrentState());
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

    // Axis lock control
    public void setLockXAxis(boolean lock, double lower, double upper) {
        this.lockXAxis = lock;
        this.lockLower = lower;
        this.lockUpper = upper;
        applyAxisLock();
    }

    // Presentation controls
    public void setShowSymbols(boolean show) {
        this.showSymbols = show;
        setCreateSymbols(showSymbols);
    }

    public void setSeriesStrokeWidth(double px) {
        this.seriesStrokeWidth = Math.max(0.5, px);
        getData().forEach(s -> {
            if (s.getNode() != null) {
                s.getNode().setStyle("-fx-stroke-width: " + this.seriesStrokeWidth + "px;");
            }
        });
    }

    // ---- raw samples API ----

    /**
     * Use precomputed scalar samples instead of deriving scalars from FeatureVectors.
     */
    public void setScalarSamples(List<Double> samples) {
        if (samples == null || samples.isEmpty()) {
            this.scalarSamples = null;
        } else {
            this.scalarSamples = new ArrayList<>(samples);
        }
        refresh();
    }

    public void addScalarSamples(List<Double> more) {
        if (more == null || more.isEmpty()) return;
        if (this.scalarSamples == null) this.scalarSamples = new ArrayList<>(more);
        else this.scalarSamples.addAll(more);
        refresh();
    }

    public void clearScalarSamples() {
        this.scalarSamples = null;
        refresh();
    }

    // getters
    public StatisticEngine.ScalarType getScalarType() {
        return scalarType;
    }

    public int getBins() {
        return bins;
    }

    public Mode getMode() {
        return mode;
    }

    public String getMetricNameForGeneric() {
        return metricNameForGeneric;
    }

    public List<Double> getReferenceVectorForGeneric() {
        return referenceVectorForGeneric;
    }

    public Integer getComponentIndex() {
        return componentIndex;
    }

    public boolean isLockXAxis() {
        return lockXAxis;
    }

    public double getLockLower() {
        return lockLower;
    }

    public double getLockUpper() {
        return lockUpper;
    }

    public boolean isShowSymbols() {
        return showSymbols;
    }

    public double getSeriesStrokeWidth() {
        return seriesStrokeWidth;
    }

    public boolean isUsingRawSamples() {
        return scalarSamples != null && !scalarSamples.isEmpty();
    }

    // internal
    private String titleForCurrentState() {
        return (this.mode == Mode.PDF_ONLY ? "PDF" : "CDF") + " • " + this.scalarType;
    }

    private void refresh() {
        getData().clear();
        lastStat = null;

        if (isUsingRawSamples()) {
            // Directly bin the provided samples
            StatisticResult stat = StatisticEngine.buildStatResult(scalarSamples, bins);
            plotFromStatistic(stat);
            return;
        }

        if (vectors == null || vectors.isEmpty()) {
            applyAxisLock();
            return;
        }

        Set<StatisticEngine.ScalarType> types = Set.of(scalarType);
        String metricName = (scalarType == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) ? metricNameForGeneric : null;
        List<Double> refVec = (scalarType == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) ? referenceVectorForGeneric : null;
        Integer compIdx = (scalarType == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION) ? componentIndex : null;

        Map<StatisticEngine.ScalarType, StatisticResult> resultMap =
            StatisticEngine.computeStatistics(vectors, types, bins, metricName, refVec, compIdx);

        plotFromStatistic(resultMap.get(scalarType));
    }

    private void plotFromStatistic(StatisticResult stat) {
        lastStat = stat;
        if (stat == null) {
            applyAxisLock();
            return;
        }
        double[] x = stat.getPdfBins();
        XYChart.Series<Number, Number> series = new XYChart.Series<>();

        if (mode == Mode.PDF_ONLY) {
            double[] y = stat.getPdf();
            int n = Math.min(x.length, y.length);
            for (int i = 0; i < n; i++) series.getData().add(new XYChart.Data<>(x[i], y[i]));
        } else {
            double[] y = stat.getCdf();
            int n = Math.min(x.length, y.length);
            for (int i = 0; i < n; i++) series.getData().add(new XYChart.Data<>(x[i], y[i]));
        }
        getData().add(series);

        if (series.getNode() != null) {
            series.getNode().setStyle("-fx-stroke-width: " + seriesStrokeWidth + "px;");
        }
        applyAxisLock();

        attachPlotInteractions();
    }

    private void applyAxisLock() {
        NumberAxis xAxis = (NumberAxis) getXAxis();
        if (xAxis == null) return;

        if (lockXAxis) {
            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(lockLower);
            xAxis.setUpperBound(lockUpper);
            double span = Math.max(1e-12, lockUpper - lockLower);
            double tick = span / 10.0;
            xAxis.setTickUnit(tick);
            xAxis.setMinorTickCount(2);
        } else {
            xAxis.setAutoRanging(true);
        }
    }

    //interaction plumbing
    private void attachPlotInteractions() {
        Platform.runLater(() -> {
            Node plotArea = lookup(".chart-plot-background");
            if (plotArea == null) return;

            plotArea.setOnMouseMoved(evt -> {
                if (onBinHover == null || lastStat == null || lastStat.getBinEdges() == null) return;
                Double xVal = xValueFromPlotPixel(plotArea, evt.getX());
                if (xVal == null) return;
                BinSelection sel = selectionForX(xVal);
                if (sel != null) onBinHover.accept(sel);
            });

            plotArea.setOnMouseClicked(evt -> {
                if (onBinClick == null || lastStat == null || lastStat.getBinEdges() == null) return;
                Double xVal = xValueFromPlotPixel(plotArea, evt.getX());
                if (xVal == null) return;
                BinSelection sel = selectionForX(xVal);
                if (sel != null) onBinClick.accept(sel);
            });
        });
    }

    private Double xValueFromPlotPixel(Node plotArea, double xInPlotLocal) {
        try {
            Point2D scenePt = plotArea.localToScene(xInPlotLocal, 0);
            Point2D axisPt = getXAxis().sceneToLocal(scenePt);
            return ((NumberAxis) getXAxis()).getValueForDisplay(axisPt.getX()).doubleValue();
        } catch (Exception ex) {
            return null;
        }
    }

    private BinSelection selectionForX(double xVal) {
        double[] edges = (lastStat != null) ? lastStat.getBinEdges() : null;
        if (edges == null || edges.length < 2) return null;

        int n = edges.length - 1;
        if (xVal <= edges[0]) xVal = edges[0] + 1e-12;
        if (xVal >= edges[n]) xVal = edges[n] - 1e-12;

        // binary search for bin
        int lo = 0, hi = n - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (xVal < edges[mid]) {
                hi = mid - 1;
            } else if (xVal >= edges[mid + 1]) {
                lo = mid + 1;
            } else {
                int b = mid;
                double center = lastStat.getPdfBins()[b];
                int count = lastStat.getBinCounts()[b];
                double frac = (lastStat.getTotalSamples() > 0)
                    ? ((double) count) / lastStat.getTotalSamples()
                    : 0.0;
                int[] idx = lastStat.getBinToSampleIdx()[b];
                return new BinSelection(b, center, edges[b], edges[b + 1], count, frac, idx);
            }
        }
        return null;
    }
}
