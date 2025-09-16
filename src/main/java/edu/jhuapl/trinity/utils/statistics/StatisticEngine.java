package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import edu.jhuapl.trinity.utils.metric.Metric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * StatisticEngine for extracting scalar statistics and their distributions
 * from FeatureVector collections.
 *
 * @author Sean Phillips
 */
public class StatisticEngine {

    public enum ScalarType {
        L1_NORM,
        LINF_NORM,
        MEAN,
        MAX,
        MIN,
        DIST_TO_MEAN,
        COSINE_TO_MEAN,
        PC1_PROJECTION,
        METRIC_DISTANCE_TO_MEAN,
        COMPONENT_AT_DIMENSION   //marginal of a single component across vectors
    }

    /**
     * Main entry point to compute selected statistics for a list of FeatureVectors.
     * @param vectors List of FeatureVector
     * @param selectedTypes Set of ScalarTypes to calculate
     * @param pdfBins Number of bins for PDF/CDF histograms
     * @param metricNameForGeneric (Optional) Metric name for METRIC_DISTANCE_TO_MEAN, e.g. "manhattan"
     * @param referenceVectorForGeneric (Optional) Reference vector (mean, centroid, etc.) for METRIC_DISTANCE_TO_MEAN
     * @return Map of ScalarType to StatisticResult
     */
    public static Map<ScalarType, StatisticResult> computeStatistics(
            List<FeatureVector> vectors,
            Set<ScalarType> selectedTypes,
            int pdfBins,
            String metricNameForGeneric,
            List<Double> referenceVectorForGeneric
    ) {
        // Default to no component selection
        return computeStatistics(vectors, selectedTypes, pdfBins, metricNameForGeneric,
                referenceVectorForGeneric, null);
    }

    /**
     * Overload supporting COMPONENT_AT_DIMENSION via componentIndex.
     * If componentIndex is null or out of range when COMPONENT_AT_DIMENSION is requested,
     * the component result will be omitted.
     */
    public static Map<ScalarType, StatisticResult> computeStatistics(
            List<FeatureVector> vectors,
            Set<ScalarType> selectedTypes,
            int pdfBins,
            String metricNameForGeneric,
            List<Double> referenceVectorForGeneric,
            Integer componentIndex
    ) {
        Map<ScalarType, StatisticResult> results = new HashMap<>();
        if (vectors == null || vectors.isEmpty() || selectedTypes == null || selectedTypes.isEmpty()) {
            return results;
        }

        List<Double> meanVector = null;
        if (selectedTypes.contains(ScalarType.DIST_TO_MEAN) || selectedTypes.contains(ScalarType.COSINE_TO_MEAN)) {
            meanVector = FeatureVector.getMeanVector(vectors);
        }

        // PC1 projections, if needed
        if (selectedTypes.contains(ScalarType.PC1_PROJECTION)) {
            double[][] dataArr = vectors.stream()
                    .map(fv -> fv.getData().stream().mapToDouble(Double::doubleValue).toArray())
                    .toArray(double[][]::new);

            double[][] pcaProjected = AnalysisUtils.doCommonsPCA(dataArr);
            List<Double> pc1Projections = new ArrayList<>();
            for (int i = 0; i < pcaProjected.length; i++) {
                pc1Projections.add(pcaProjected[i][0]);
            }
            results.put(ScalarType.PC1_PROJECTION, buildStatResult(pc1Projections, pdfBins));
        }

        // COMPONENT_AT_DIMENSION, if needed
        if (selectedTypes.contains(ScalarType.COMPONENT_AT_DIMENSION)) {
            if (componentIndex != null && componentIndex >= 0) {
                List<Double> comps = new ArrayList<>(vectors.size());
                for (FeatureVector fv : vectors) {
                    List<Double> d = fv.getData();
                    if (componentIndex < d.size()) {
                        comps.add(d.get(componentIndex));
                    }
                }
                if (!comps.isEmpty()) {
                    results.put(ScalarType.COMPONENT_AT_DIMENSION, buildStatResult(comps, pdfBins));
                }
            }
        }

        // Main per-vector scalar calculations
        for (ScalarType type : selectedTypes) {
            if (type == ScalarType.PC1_PROJECTION || type == ScalarType.COMPONENT_AT_DIMENSION) continue;

            if (type == ScalarType.METRIC_DISTANCE_TO_MEAN) {
                if (metricNameForGeneric != null && referenceVectorForGeneric != null) {
                    Metric metric = Metric.getMetric(metricNameForGeneric);
                    double[] refVec = referenceVectorForGeneric.stream().mapToDouble(Double::doubleValue).toArray();
                    List<Double> metricDists = new ArrayList<>(vectors.size());
                    for (FeatureVector fv : vectors) {
                        double[] fvArr = fv.getData().stream().mapToDouble(Double::doubleValue).toArray();
                        double value = metric.distance(fvArr, refVec);
                        metricDists.add(value);
                    }
                    results.put(type, buildStatResult(metricDists, pdfBins));
                }
                continue;
            }

            List<Double> scalars = new ArrayList<>(vectors.size());
            for (FeatureVector fv : vectors) {
                double value = switch (type) {
                    case L1_NORM -> fv.getData().stream().mapToDouble(Math::abs).sum();
                    case LINF_NORM -> fv.getData().stream().mapToDouble(Math::abs).max().orElse(0.0);
                    case MEAN -> fv.getData().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    case MAX -> fv.getMax();
                    case MIN -> fv.getMin();
                    case DIST_TO_MEAN -> meanVector != null
                            ? AnalysisUtils.l2Norm(diffList(fv.getData(), meanVector))
                            : 0.0;
                    case COSINE_TO_MEAN -> meanVector != null
                            ? AnalysisUtils.cosineSimilarity(fv.getData(), meanVector)
                            : 0.0;
                    default -> 0.0;
                };
                scalars.add(value);
            }
            results.put(type, buildStatResult(scalars, pdfBins));
        }
        return results;
    }

    public static double[] diffList(List<Double> a, List<Double> b) {
        double[] out = new double[a.size()];
        for (int i = 0; i < a.size(); i++) out[i] = a.get(i) - b.get(i);
        return out;
    }

    public static StatisticResult buildStatResult(List<Double> scalars, int bins) {
        PDFCDFResult pdfcdf = computePDFCDF(scalars, bins);
        return new StatisticResult(scalars, pdfcdf.bins, pdfcdf.pdf, pdfcdf.cdf);
    }

    public static class PDFCDFResult {
        double[] bins;
        double[] pdf;
        double[] cdf;
        PDFCDFResult(double[] bins, double[] pdf, double[] cdf) {
            this.bins = bins; this.pdf = pdf; this.cdf = cdf;
        }
    }

    public static PDFCDFResult computePDFCDF(List<Double> values, int binsCount) {
        if (values == null || values.isEmpty()) {
            int n = Math.max(5, binsCount);
            return new PDFCDFResult(new double[n], new double[n], new double[n]);
        }
        int n = Math.max(1, binsCount);

        double min = values.stream().min(Double::compare).get();
        double max = values.stream().max(Double::compare).get();
        if (min == max) max += 1e-8;

        double binWidth = (max - min) / n;
        double[] bins = new double[n];
        double[] pdf = new double[n];
        double[] cdf = new double[n];

        for (int i = 0; i < n; i++) bins[i] = min + (i + 0.5) * binWidth;
        for (double v : values) {
            int idx = (int) ((v - min) / binWidth);
            if (idx < 0) idx = 0;
            if (idx >= n) idx = n - 1;
            pdf[idx] += 1.0;
        }
        for (int i = 0; i < n; i++) pdf[i] /= (values.size() * binWidth);
        cdf[0] = pdf[0] * binWidth;
        for (int i = 1; i < n; i++) cdf[i] = cdf[i - 1] + pdf[i] * binWidth;
        cdf[n - 1] = Math.min(1.0, Math.max(0.0, cdf[n - 1]));
        return new PDFCDFResult(bins, pdf, cdf);
    }
}
