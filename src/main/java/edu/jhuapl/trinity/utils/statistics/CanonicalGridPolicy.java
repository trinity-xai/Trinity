package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import edu.jhuapl.trinity.utils.metric.Metric;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CanonicalGridPolicy
 * -------------------
 * Produces consistent 2D grid specs (bounds + bins) for joint PDF/CDF surfaces
 * given axis definitions (AxisParams) and a dataset (FeatureVectors).
 *
 * Modes:
 *  - FIXED_01:     [0,1] x [0,1]
 *  - DATA_MIN_MAX: per-axis min..max from data
 *  - ROBUST_PCT:   per-axis [pLow..pHigh] percentiles (robust to outliers)
 *
 * Per-axis overrides (by AxisKey) can supply explicit min/max and/or bins.
 * A static registry allows lookup by policy id (e.g., "default").
 *
 * Note: This class re-derives axis scalars (x_i or y_i) in order to compute bounds.
 * It mirrors the scalar extraction used in GridDensity3DEngine.
 *
 * @author Sean Phillips
 */
public final class CanonicalGridPolicy implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    // ---------- Modes ----------

    public enum Mode {
        FIXED_01,
        DATA_MIN_MAX,
        ROBUST_PCT   // [pLow, pHigh]
    }

    // ---------- Axis identity & overrides ----------

    /**
     * AxisKey identifies the semantic axis (ScalarType + metric/component/ref hints).
     * This lets you set per-axis canonical overrides and cache computed ranges.
     */
    public static final class AxisKey implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        public final StatisticEngine.ScalarType type;
        public final String metricName;     // for METRIC_DISTANCE_TO_MEAN
        public final Integer componentIndex;// for COMPONENT_AT_DIMENSION
        public final String label;          // optional display tag

        public AxisKey(StatisticEngine.ScalarType type, String metricName, Integer componentIndex, String label) {
            this.type = Objects.requireNonNull(type, "type");
            this.metricName = metricName;
            this.componentIndex = componentIndex;
            this.label = label;
        }

        public static AxisKey from(AxisParams a) {
            return new AxisKey(a.getType(), a.getMetricName(), a.getComponentIndex(), a.getMetricName());
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AxisKey)) return false;
            AxisKey that = (AxisKey) o;
            return type == that.type &&
                    Objects.equals(metricName, that.metricName) &&
                    Objects.equals(componentIndex, that.componentIndex) &&
                    Objects.equals(label, that.label);
        }

        @Override public int hashCode() {
            return Objects.hash(type, metricName, componentIndex, label);
        }

        @Override public String toString() {
            return "AxisKey{" +
                    "type=" + type +
                    ", metricName='" + metricName + '\'' +
                    ", componentIndex=" + componentIndex +
                    ", label='" + label + '\'' +
                    '}';
        }
    }

    /** Per-axis explicit override (any field nullable = leave to policy). */
    public static final class AxisOverride implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        public final Double min;
        public final Double max;
        public final Integer bins;

        public AxisOverride(Double min, Double max, Integer bins) {
            this.min = min; this.max = max; this.bins = bins;
        }

        public AxisOverride withBins(Integer b) { return new AxisOverride(min, max, b); }
    }

    /** Simple holder for numeric range. */
    public static final class AxisRange implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        public final double min, max;
        public AxisRange(double min, double max) { this.min = min; this.max = max; }
        @Override public String toString() { return "[" + min + ", " + max + "]"; }
    }

    // ---------- Policy fields ----------

    private final String id;
    private final Mode mode;
    private final int defaultBinsX;
    private final int defaultBinsY;

    // For ROBUST_PCT
    private final double pLow;   // e.g., 0.01
    private final double pHigh;  // e.g., 0.99

    // Small epsilon to avoid zero-span ranges
    private final double epsilon;

    // Optional per-axis overrides
    private final Map<AxisKey, AxisOverride> overrides = new ConcurrentHashMap<>();

    // Simple cache for computed ranges per dataset signature (optional use via callers)
    // Map<cacheKey(dataset), Map<AxisKey, AxisRange>>
    private final Map<String, Map<AxisKey, AxisRange>> datasetRangeCache = new ConcurrentHashMap<>();

    // ---------- Registry ----------

    private static final Map<String, CanonicalGridPolicy> REGISTRY = new ConcurrentHashMap<>();

    /** Default policy: robust percentiles [1%, 99%], 64x64 bins. */
    static {
        CanonicalGridPolicy def = new Builder("default", Mode.ROBUST_PCT)
                .bins(64, 64)
                .percentiles(0.01, 0.99)
                .epsilon(1e-8)
                .build();
        register(def);
        register(new Builder("fixed01", Mode.FIXED_01).bins(64, 64).build());
        register(new Builder("minmax", Mode.DATA_MIN_MAX).bins(64, 64).epsilon(1e-8).build());
    }

    public static void register(CanonicalGridPolicy policy) {
        REGISTRY.put(Objects.requireNonNull(policy).id, policy);
    }

    public static CanonicalGridPolicy get(String id) {
        return REGISTRY.getOrDefault(id, REGISTRY.get("default"));
    }

    // ---------- Builder ----------

    public static final class Builder {
        private final String id;
        private final Mode mode;
        private int defaultBinsX = 64, defaultBinsY = 64;
        private double pLow = 0.01, pHigh = 0.99;
        private double epsilon = 1e-8;
        private final Map<AxisKey, AxisOverride> overrides = new HashMap<>();

        public Builder(String id, Mode mode) {
            this.id = Objects.requireNonNull(id, "id");
            this.mode = Objects.requireNonNull(mode, "mode");
        }

        public Builder bins(int bx, int by) { this.defaultBinsX = bx; this.defaultBinsY = by; return this; }
        public Builder percentiles(double low, double high) { this.pLow = low; this.pHigh = high; return this; }
        public Builder epsilon(double e) { this.epsilon = e; return this; }
        public Builder override(AxisKey key, AxisOverride ov) { this.overrides.put(key, ov); return this; }

        public CanonicalGridPolicy build() {
            if (mode == Mode.ROBUST_PCT && !(pLow >= 0 && pLow < pHigh && pHigh <= 1))
                throw new IllegalArgumentException("Invalid percentiles: pLow < pHigh and both in [0,1].");
            return new CanonicalGridPolicy(this);
        }
    }

    private CanonicalGridPolicy(Builder b) {
        this.id = b.id;
        this.mode = b.mode;
        this.defaultBinsX = b.defaultBinsX;
        this.defaultBinsY = b.defaultBinsY;
        this.pLow = b.pLow;
        this.pHigh = b.pHigh;
        this.epsilon = b.epsilon;
        this.overrides.putAll(b.overrides);
    }

    // ---------- Public API ----------

    /**
     * Compute a GridSpec for the given axes and vectors, using this policy.
     * Falls back to per-axis overrides when present.
     */
    public GridSpec gridForAxes(List<FeatureVector> vectors,
                                AxisParams xAxis,
                                AxisParams yAxis,
                                Integer binsX,
                                Integer binsY,
                                String datasetCacheKey // optional: stable id for caching (e.g., SHA of data)
    ) {
        int bx = binsX != null ? binsX : defaultBinsX;
        int by = binsY != null ? binsY : defaultBinsY;

        AxisKey kx = AxisKey.from(xAxis);
        AxisKey ky = AxisKey.from(yAxis);

        AxisOverride ox = overrides.get(kx);
        AxisOverride oy = overrides.get(ky);

        AxisRange rx = (ox != null && ox.min != null && ox.max != null)
                ? new AxisRange(ox.min, ox.max)
                : computeAxisRange(vectors, xAxis, datasetCacheKey);

        AxisRange ry = (oy != null && oy.min != null && oy.max != null)
                ? new AxisRange(oy.min, oy.max)
                : computeAxisRange(vectors, yAxis, datasetCacheKey);

        if (ox != null && ox.bins != null) bx = ox.bins;
        if (oy != null && oy.bins != null) by = oy.bins;

        // Construct GridSpec with explicit bounds.
        GridSpec g = new GridSpec(bx, by);
        g.setMinX(rx.min); g.setMaxX(rx.max);
        g.setMinY(ry.min); g.setMaxY(ry.max);
        return g;
    }

    /**
     * Precompute and cache ranges for a set of axes over a dataset.
     * Useful when you will request many pairs repeatedly.
     */
    public void warmupRanges(String datasetCacheKey,
                             List<FeatureVector> vectors,
                             Collection<AxisParams> axes) {
        for (AxisParams ax : axes) {
            computeAxisRange(vectors, ax, datasetCacheKey);
        }
    }

    /**
     * Returns the canonical range for a single axis (applies overrides if present).
     */
    public AxisRange axisRange(List<FeatureVector> vectors, AxisParams axis, String datasetCacheKey) {
        AxisKey k = AxisKey.from(axis);
        AxisOverride ov = overrides.get(k);
        if (ov != null && ov.min != null && ov.max != null) {
            return new AxisRange(ov.min, ov.max);
        }
        return computeAxisRange(vectors, axis, datasetCacheKey);
    }

    // ---------- Internals ----------

    private AxisRange computeAxisRange(List<FeatureVector> vectors,
                                       AxisParams axis,
                                       String datasetCacheKey) {
        // Cache path
        if (datasetCacheKey != null) {
            AxisRange cached = datasetRangeCache
                    .computeIfAbsent(datasetCacheKey, k -> new ConcurrentHashMap<>())
                    .get(AxisKey.from(axis));
            if (cached != null) return cached;
        }

        AxisRange r;
        switch (mode) {
            case FIXED_01 -> r = new AxisRange(0.0, 1.0);
            case DATA_MIN_MAX -> {
                double[] vals = extractScalars(vectors, axis);
                double min = Arrays.stream(vals).min().orElse(0.0);
                double max = Arrays.stream(vals).max().orElse(min + epsilon);
                if (max - min < epsilon) max = min + epsilon;
                r = new AxisRange(min, max);
            }
            case ROBUST_PCT -> {
                double[] vals = extractScalars(vectors, axis);
                if (vals.length == 0) {
                    r = new AxisRange(0.0, 1.0);
                } else {
                    Arrays.sort(vals);
                    double lo = percentileSorted(vals, pLow);
                    double hi = percentileSorted(vals, pHigh);
                    if (hi - lo < epsilon) hi = lo + epsilon;
                    r = new AxisRange(lo, hi);
                }
            }
            default -> r = new AxisRange(0.0, 1.0);
        }

        if (datasetCacheKey != null) {
            datasetRangeCache.computeIfAbsent(datasetCacheKey, k -> new ConcurrentHashMap<>())
                    .put(AxisKey.from(axis), r);
        }
        return r;
    }

    // Extract scalar values per AxisParams, mirroring GridDensity3DEngine
    private double[] extractScalars(List<FeatureVector> vectors, AxisParams axis) {
        if (vectors == null || vectors.isEmpty()) return new double[0];

        // Precompute mean if needed
        List<Double> meanVector = null;
        Set<StatisticEngine.ScalarType> needMean = Set.of(
                StatisticEngine.ScalarType.DIST_TO_MEAN,
                StatisticEngine.ScalarType.COSINE_TO_MEAN
        );
        if (needMean.contains(axis.getType())) {
            meanVector = FeatureVector.getMeanVector(vectors);
        }

        // Metric if needed
        Metric metric = null;
        if (axis.getType() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN
                && axis.getMetricName() != null
                && axis.getReferenceVec() != null) {
            metric = Metric.getMetric(axis.getMetricName());
        }

        double[] out = new double[vectors.size()];
        for (int i = 0; i < vectors.size(); i++) {
            FeatureVector fv = vectors.get(i);
            out[i] = scalarValue(fv, axis.getType(), meanVector, metric, axis.getReferenceVec(), axis.getComponentIndex());
        }
        return out;
    }

    private double scalarValue(FeatureVector fv,
                               StatisticEngine.ScalarType type,
                               List<Double> meanVec,
                               Metric metric,
                               List<Double> refVec,
                               Integer componentIndex) {
        switch (type) {
            case L1_NORM:
                return fv.getData().stream().mapToDouble(Math::abs).sum();
            case LINF_NORM:
                return fv.getData().stream().mapToDouble(Math::abs).max().orElse(0.0);
            case MEAN:
                return fv.getData().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            case MAX:
                return fv.getMax();
            case MIN:
                return fv.getMin();
            case DIST_TO_MEAN:
                if (meanVec == null) return 0.0;
                return AnalysisUtils.l2Norm(StatisticEngine.diffList(fv.getData(), meanVec));
            case COSINE_TO_MEAN:
                if (meanVec == null) return 0.0;
                return AnalysisUtils.cosineSimilarity(fv.getData(), meanVec);
            case METRIC_DISTANCE_TO_MEAN:
                if (metric == null || refVec == null) return 0.0;
                double[] a = fv.getData().stream().mapToDouble(Double::doubleValue).toArray();
                double[] b = refVec.stream().mapToDouble(Double::doubleValue).toArray();
                return metric.distance(a, b);
            case COMPONENT_AT_DIMENSION:
                if (componentIndex == null) return 0.0;
                List<Double> d = fv.getData();
                if (componentIndex >= 0 && componentIndex < d.size()) return d.get(componentIndex);
                return 0.0;
            case PC1_PROJECTION:
                // Not computed here (expensive). If needed, precompute externally and pass via COMPONENT_AT_DIMENSION.
                return 0.0;
            default:
                return 0.0;
        }
    }

    // Percentile for sorted array, linear interpolation between nearest ranks
    private static double percentileSorted(double[] sorted, double p) {
        if (sorted.length == 0) return 0.0;
        if (p <= 0) return sorted[0];
        if (p >= 1) return sorted[sorted.length - 1];
        double pos = p * (sorted.length - 1);
        int i = (int) Math.floor(pos);
        int j = (int) Math.ceil(pos);
        if (i == j) return sorted[i];
        double w = pos - i;
        return (1 - w) * sorted[i] + w * sorted[j];
    }

    // ---------- Accessors ----------

    public String id() { return id; }
    public Mode mode() { return mode; }
    public int defaultBinsX() { return defaultBinsX; }
    public int defaultBinsY() { return defaultBinsY; }
    public double pLow() { return pLow; }
    public double pHigh() { return pHigh; }
    public double epsilon() { return epsilon; }

    public Map<AxisKey, AxisOverride> overridesView() { return Collections.unmodifiableMap(overrides); }

    @Override public String toString() {
        return "CanonicalGridPolicy{" +
                "id='" + id + '\'' +
                ", mode=" + mode +
                ", defaultBinsX=" + defaultBinsX +
                ", defaultBinsY=" + defaultBinsY +
                ", pLow=" + pLow +
                ", pHigh=" + pHigh +
                ", epsilon=" + epsilon +
                ", overrides=" + overrides.size() +
                '}';
    }
}
