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
 * @author Sean
 */
public class StatisticEngine {

    public enum ScalarType {
        NORM, MEAN, MAX, MIN,
        DIST_TO_MEAN,
        COSINE_TO_MEAN,
        PC1_PROJECTION,
        METRIC_DISTANCE_TO_MEAN // requires metricName argument
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
        Map<ScalarType, StatisticResult> results = new HashMap<>();
        List<Double> meanVector = null;
        if (selectedTypes.contains(ScalarType.DIST_TO_MEAN) || selectedTypes.contains(ScalarType.COSINE_TO_MEAN)) {
            meanVector = FeatureVector.getMeanVector(vectors);
        }

        // PC1 projections, if needed
        List<Double> pc1Projections = null;
        if (selectedTypes.contains(ScalarType.PC1_PROJECTION)) {
            double[][] dataArr = vectors.stream()
                .map(fv -> fv.getData().stream().mapToDouble(Double::doubleValue).toArray())
                .toArray(double[][]::new);

            double[][] pcaProjected = AnalysisUtils.doCommonsPCA(dataArr);
            pc1Projections = new ArrayList<>();
            for (int i = 0; i < pcaProjected.length; i++) {
                pc1Projections.add(pcaProjected[i][0]);
            }
            results.put(ScalarType.PC1_PROJECTION, buildStatResult(pc1Projections, pdfBins));
        }

        // Main per-vector scalar calculations
        for (ScalarType type : selectedTypes) {
            if (type == ScalarType.PC1_PROJECTION) continue; // already handled
            if (type == ScalarType.METRIC_DISTANCE_TO_MEAN) {
                // Metric distance (user-selected)
                if (metricNameForGeneric != null && referenceVectorForGeneric != null) {
                    Metric metric = Metric.getMetric(metricNameForGeneric);
                    double[] refVec = referenceVectorForGeneric.stream().mapToDouble(Double::doubleValue).toArray();
                    List<Double> metricDists = new ArrayList<>();
                    for (FeatureVector fv : vectors) {
                        double[] fvArr = fv.getData().stream().mapToDouble(Double::doubleValue).toArray();
                        double value = metric.distance(fvArr, refVec);
                        metricDists.add(value);
                    }
                    results.put(type, buildStatResult(metricDists, pdfBins));
                }
                continue;
            }

            List<Double> scalars = new ArrayList<>();
            for (FeatureVector fv : vectors) {
                double value = switch (type) {
                    case NORM -> AnalysisUtils.l2Norm(fv.getData().stream().mapToDouble(Double::doubleValue).toArray());
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

    // Utility: vector difference as double[]
    public static double[] diffList(List<Double> a, List<Double> b) {
        double[] out = new double[a.size()];
        for (int i = 0; i < a.size(); i++) out[i] = a.get(i) - b.get(i);
        return out;
    }

    // Utility: generate StatisticResult from scalars
    public static StatisticResult buildStatResult(List<Double> scalars, int bins) {
        PDFCDFResult pdfcdf = computePDFCDF(scalars, bins);
        return new StatisticResult(scalars, pdfcdf.bins, pdfcdf.pdf, pdfcdf.cdf);
    }

    // PDF/CDF calculation using histogram
    public static class PDFCDFResult {
        double[] bins;
        double[] pdf;
        double[] cdf;
        PDFCDFResult(double[] bins, double[] pdf, double[] cdf) {
            this.bins = bins; this.pdf = pdf; this.cdf = cdf;
        }
    }

    public static PDFCDFResult computePDFCDF(List<Double> values, int binsCount) {
        double min = values.stream().min(Double::compare).get();
        double max = values.stream().max(Double::compare).get();
        if (min == max) max += 1e-8; // Avoid divide by zero for constant values
        double binWidth = (max - min) / binsCount;
        double[] bins = new double[binsCount];
        double[] pdf = new double[binsCount];
        double[] cdf = new double[binsCount];

        for (int i = 0; i < binsCount; i++) {
            bins[i] = min + (i + 0.5) * binWidth;
        }
        for (double v : values) {
            int idx = (int) ((v - min) / binWidth);
            if (idx < 0) idx = 0;
            if (idx >= binsCount) idx = binsCount - 1;
            pdf[idx] += 1;
        }
        for (int i = 0; i < binsCount; i++) {
            pdf[i] /= (values.size() * binWidth);
        }
        cdf[0] = pdf[0] * binWidth;
        for (int i = 1; i < binsCount; i++) {
            cdf[i] = cdf[i - 1] + pdf[i] * binWidth;
        }
        return new PDFCDFResult(bins, pdf, cdf);
    }
}
