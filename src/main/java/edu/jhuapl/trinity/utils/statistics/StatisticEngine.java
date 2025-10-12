package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import static edu.jhuapl.trinity.utils.AnalysisUtils.clamp01;
import static edu.jhuapl.trinity.utils.AnalysisUtils.clip;
import edu.jhuapl.trinity.utils.metric.Metric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * StatisticEngine for extracting scalar statistics and their distributions
 * from FeatureVector collections, plus utilities for stepwise "contribution"
 * series derived from time-ordered FeatureVectors.
 *
 * - PDF/CDF: histogram-based density/cumulative distributions for chosen scalars
 * - Contribution series: aggregate each FeatureVector's [0,1] similarities to S_t,
 *   convert to log-odds L_t=log(S_t/(1-S_t)), then Δ_t=L_t-L_{t-1}.
 *
 * NOTE: FeatureVectors are assumed order-aligned with time when computing contributions.
 *
 * @author Sean Phillips
 */
public class StatisticEngine {

    // ===== Scalar selection for PDF/CDF =====
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
        COMPONENT_AT_DIMENSION   // marginal of a single component across vectors
    }

    // ===== Aggregation for contribution series =====
    public enum SimilarityAggregator {
        MEAN,        // weighted arithmetic mean on [0,1]
        GEOMEAN,     // (weighted) geometric mean, penalizes weak dimensions
        MIN          // strict: weakest dimension dominates
    }

    /** Holder for contribution computation outputs. */
    public static final class ContributionSeries {
        public final List<Double> similarity; // S_t in (0,1)
        public final List<Double> logit;      // L_t
        public final List<Double> delta;      // Δ_t = L_t - L_{t-1}, with Δ_0 = L_0
        public ContributionSeries(List<Double> s, List<Double> l, List<Double> d) {
            this.similarity = s; this.logit = l; this.delta = d;
        }
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
                    case DIST_TO_MEAN -> (meanVector != null)
                            ? AnalysisUtils.l2Norm(diffList(fv.getData(), meanVector))
                            : 0.0;
                    case COSINE_TO_MEAN -> (meanVector != null)
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
    /**
     * Build a cumulative series from a list of delta values.
     * Intended for converting per-step Δ log-odds into cumulative log-odds.
     *
     * Rules:
     * - Null or NaN entries are treated as 0.0.
     * - Empty or null input returns an empty list.
     *
     * @param deltas List of per-step changes (e.g., Δ log-odds)
     * @return cumulative sum series of the same length
     */
    public static List<Double> cumulativeFromDeltas(List<Double> deltas) {
        List<Double> out = new ArrayList<>();
        if (deltas == null || deltas.isEmpty()) return out;

        double acc = 0.0;
        for (Double d : deltas) {
            double v = (d == null || d.isNaN()) ? 0.0 : d;
            acc += v;
            out.add(acc);
        }
        return out;
    }

    // ===== Contribution series (Similarity -> Logit -> Δ) =====

    /**
     * Compute per-step contributions from a sequence of FeatureVectors.
     * @param vectors   sequential FeatureVectors (each element in [0,1])
     * @param agg       how to aggregate each vector to a single S_t
     * @param weights   optional per-dimension weights (will be normalized to sum=1). If null, uniform.
     * @param epsilon   small value used to clip S_t into (ε, 1-ε) and for GEOMEAN stability (e.g., 1e-6).
     */
    public static ContributionSeries computeContributions(
            List<FeatureVector> vectors,
            SimilarityAggregator agg,
            double[] weights,
            double epsilon
    ) {
        List<Double> S = aggregateSimilaritySeries(vectors, agg, weights, epsilon);
        List<Double> L = logitSeries(S, epsilon);
        List<Double> D = contributionSeriesFromLogit(L);
        return new ContributionSeries(S, L, D);
    }

    /** Aggregate each FeatureVector to a single similarity S_t in (0,1). */
    public static List<Double> aggregateSimilaritySeries(
            List<FeatureVector> vectors,
            SimilarityAggregator agg,
            double[] weights,
            double epsilon
    ) {
        List<Double> out = new ArrayList<>();
        if (vectors == null || vectors.isEmpty()) return out;

        int dim = Math.max(1, vectors.get(0).getData() != null ? vectors.get(0).getData().size() : 1);
        double[] w = normalizedWeights(weights, dim);

        for (FeatureVector fv : vectors) {
            List<Double> z = fv.getData();
            if (z == null || z.isEmpty()) { out.add(0.5); continue; }

            double s;
            switch (agg != null ? agg : SimilarityAggregator.MEAN) {
                case GEOMEAN -> {
                    // weighted geometric mean on [0,1], using exponents w[i] (sum=1)
                    double prod = 1.0;
                    int m = Math.min(dim, z.size());
                    for (int i = 0; i < m; i++) {
                        double zi = clamp01(z.get(i));
                        prod *= Math.pow(Math.max(zi, epsilon), w[i]);
                    }
                    s = prod;
                }
                case MIN -> {
                    double mval = 1.0;
                    int m = Math.min(dim, z.size());
                    for (int i = 0; i < m; i++) {
                        mval = Math.min(mval, clamp01(z.get(i)));
                    }
                    s = mval;
                }
//                case MEAN: //For now the default case is the same as standard mean
                default -> {
                    double sum = 0.0, wsum = 0.0;
                    int m = Math.min(dim, z.size());
                    for (int i = 0; i < m; i++) {
                        double zi = clamp01(z.get(i));
                        sum += w[i] * zi;
                        wsum += w[i];
                    }
                    s = (wsum > 0 ? sum / wsum : sum);
                }
            }
            out.add(clip(s, epsilon, 1.0 - epsilon));
        }
        return out;
    }

    /** Logit(L) for each S_t with clipping to avoid infinities. */
    public static List<Double> logitSeries(List<Double> s, double epsilon) {
        List<Double> out = new ArrayList<>();
        if (s == null) return out;
        for (Double v : s) {
            double p = clip(v != null ? v : 0.5, epsilon, 1.0 - epsilon);
            out.add(Math.log(p / (1.0 - p)));
        }
        return out;
    }

    /** Δ_t = L_t - L_{t-1} with Δ_0 = L_0 (baseline probability 0.5 → log-odds 0). */
    public static List<Double> contributionSeriesFromLogit(List<Double> L) {
        List<Double> out = new ArrayList<>();
        if (L == null || L.isEmpty()) return out;
        out.add(L.get(0)); // Δ_0 vs. baseline 0.5
        for (int t = 1; t < L.size(); t++) {
            out.add(L.get(t) - L.get(t - 1));
        }
        return out;
    }

    // ===== Core helpers for PDF/CDF =====

    public static double[] diffList(List<Double> a, List<Double> b) {
        int m = Math.min(a.size(), b.size());
        double[] out = new double[m];
        for (int i = 0; i < m; i++) out[i] = a.get(i) - b.get(i);
        return out;
    }

    public static StatisticResult buildStatResult(List<Double> scalars, int bins) {
        PDFCDFResult pdfcdf = computePDFCDF(scalars, bins);
        StatisticResult sr = new StatisticResult(scalars, pdfcdf.bins, pdfcdf.pdf, pdfcdf.cdf);
        sr.setBinEdges(pdfcdf.binEdges);
        sr.setSampleToBin(pdfcdf.sampleToBin);
        sr.setBinCounts(pdfcdf.binCounts);
        sr.setBinToSampleIdx(pdfcdf.binToSampleIdx);
        sr.setTotalSamples(pdfcdf.totalSamples);
        return sr;
    }

    /** Internal container for histogram outputs. */
    public static class PDFCDFResult {
        double[] bins;           // bin centers
        double[] pdf;            // density values
        double[] cdf;            // cumulative probability
        double[] binEdges;       // edges, length = n+1
        int[] sampleToBin;       // per-sample bin mapping
        int[] binCounts;         // counts per bin
        int[][] binToSampleIdx;  // for each bin, list of sample indices
        int totalSamples;        // number of valid samples counted

        PDFCDFResult(
                double[] bins, double[] pdf, double[] cdf,
                double[] binEdges, int[] sampleToBin,
                int[] binCounts, int[][] binToSampleIdx, int totalSamples
        ) {
            this.bins = bins; this.pdf = pdf; this.cdf = cdf;
            this.binEdges = binEdges; this.sampleToBin = sampleToBin;
            this.binCounts = binCounts; this.binToSampleIdx = binToSampleIdx;
            this.totalSamples = totalSamples;
        }
    }

    /**
     * Compute histogram-based PDF/CDF and sample-to-bin mapping, including counts and
     * bin-to-sample index lists for interactive linking.
     * Density is normalized so that sum(pdf[i] * binWidth) ≈ 1.
     */
    public static PDFCDFResult computePDFCDF(List<Double> values, int binsCount) {
        int n = Math.max(1, Math.max(5, binsCount));
        if (values == null) values = new ArrayList<>();

        // min/max over valid values
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        int validCount = 0;
        for (Double v : values) {
            if (v == null || v.isNaN() || v.isInfinite()) continue;
            validCount++;
            if (v < min) min = v;
            if (v > max) max = v;
        }

        // Handle no valid values
        if (validCount == 0 || !Double.isFinite(min) || !Double.isFinite(max)) {
            double[] zerosN = new double[n];
            double[] zerosN1 = new double[n + 1];
            int[] emptyMap = new int[values.size()];
            int[] zeroCounts = new int[n];
            int[][] emptyIdx = new int[n][0];
            return new PDFCDFResult(zerosN, zerosN, zerosN, zerosN1, emptyMap, zeroCounts, emptyIdx, 0);
        }
        if (min == max) max = min + 1e-8;

        // build edges & centers
        double binWidth = (max - min) / n;
        double[] edges = new double[n + 1];
        double[] centers = new double[n];
        for (int i = 0; i <= n; i++) edges[i] = min + i * binWidth;
        for (int i = 0; i < n; i++) centers[i] = (edges[i] + edges[i + 1]) * 0.5;

        // counts & mappings
        int[] sampleToBin = new int[values.size()];
        int[] binCounts = new int[n];
        List<List<Integer>> binLists = new ArrayList<>(n);
        for (int i = 0; i < n; i++) binLists.add(new ArrayList<>());

        int totalSamples = 0;
        for (int si = 0; si < values.size(); si++) {
            Double vv = values.get(si);
            if (vv == null || vv.isNaN() || vv.isInfinite()) {
                sampleToBin[si] = -1;
                continue;
            }
            int idx = (int) Math.floor((vv - min) / binWidth);
            if (idx < 0) idx = 0;
            if (idx >= n) idx = n - 1; // include rightmost edge
            binCounts[idx] += 1;
            binLists.get(idx).add(si);
            sampleToBin[si] = idx;
            totalSamples++;
        }

        // pdf normalization: count / (N_valid * width)
        double[] pdf = new double[n];
        if (totalSamples > 0 && binWidth > 0) {
            for (int i = 0; i < n; i++) pdf[i] = binCounts[i] / (totalSamples * binWidth);
        }

        // cdf via cumulative sum
        double[] cdf = new double[n];
        double acc = 0.0;
        for (int i = 0; i < n; i++) {
            acc += pdf[i] * binWidth;
            cdf[i] = acc;
        }
        // clamp numerical drift
        if (cdf[n - 1] > 1.0) cdf[n - 1] = 1.0;
        if (cdf[n - 1] < 0.0) cdf[n - 1] = 0.0;

        // finalize bin-to-sample arrays
        int[][] binToSampleIdx = new int[n][];
        for (int i = 0; i < n; i++) {
            List<Integer> lst = binLists.get(i);
            binToSampleIdx[i] = new int[lst.size()];
            for (int j = 0; j < lst.size(); j++) binToSampleIdx[i][j] = lst.get(j);
        }

        return new PDFCDFResult(centers, pdf, cdf, edges, sampleToBin, binCounts, binToSampleIdx, totalSamples);
    }

    // ===== small helpers =====

    private static double[] normalizedWeights(double[] w, int dim) {
        double[] out = new double[dim];
        if (w == null || w.length == 0) {
            double u = 1.0 / dim;
            for (int i = 0; i < dim; i++) out[i] = u;
            return out;
        }
        double sum = 0.0;
        for (int i = 0; i < dim; i++) {
            out[i] = (i < w.length ? Math.max(0.0, w[i]) : 0.0);
            sum += out[i];
        }
        if (sum <= 0) {
            double u = 1.0 / dim;
            for (int i = 0; i < dim; i++) out[i] = u;
        } else {
            for (int i = 0; i < dim; i++) out[i] /= sum;
        }
        return out;
    }
}
