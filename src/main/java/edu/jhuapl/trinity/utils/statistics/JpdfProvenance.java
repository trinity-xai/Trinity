package edu.jhuapl.trinity.utils.statistics;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * JpdfProvenance
 * ---------------
 * Metadata describing how a Joint PDF/CDF (or comparison map) was produced:
 *  - operation & surface kind (baseline PDF/CDF, signed difference, mixture, conditional, time-windowed)
 *  - canonical grid spec & alignment method (for cross-cohort comparability)
 *  - axis summaries (scalar types, metric/reference choices, component indices)
 *  - data summary (N, sufficiency checks), scoring used for preselection, cohort labels
 *  - recipe & cache keys so results are reproducible and deduplicated
 *
 * This is intentionally lightweight and immutable; attach one instance per produced grid.
 *
 * @author Sean Phillips
 */
public final class JpdfProvenance implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    // ---------- Enums ----------

    /** High-level operation that produced the surface. */
    public enum Operation {
        BASELINE,           // a single cohort/slice
        MIXTURE,            // weighted combination of baselines
        DIFFERENCE,         // signed A - B (comparison surface; not a PDF)
        CONDITIONAL,        // conditioned on a predicate over a third variable or metadata tag
        TIME_WINDOW         // produced from a time window / rolling window
    }

    /** What the Z values represent. */
    public enum SurfaceKind {
        PDF,                // integrates to ≈1 over domain
        CDF,                // monotone, ends near 1
        COMPARISON_SIGNED   // signed difference map (e.g., A−B), can be negative/positive
    }

    /** How we aligned input grids to a canonical spec before ops. */
    public enum Alignment {
        NONE,       // already on the same spec
        REBIN,      // mass-conservative bin merging/splitting onto target edges
        REMAP       // mass-conservative mapping by area overlap to target cells
    }

    /** How bounds were decided (mirrors JpdfRecipe.BoundsPolicy). */
    public enum BoundsPolicy {
        DATA_MIN_MAX,
        FIXED_01,
        CANONICAL_BY_FEATURE
    }

    /** Summary of a single axis' semantic definition. */
    public static final class AxisSummary implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        public enum ReferenceKind { NONE, MEAN, VECTOR_AT_INDEX, CUSTOM }

        private final StatisticEngine.ScalarType scalarType;
        private final String metricName;          // for METRIC_DISTANCE_TO_MEAN
        private final Integer componentIndex;     // for COMPONENT_AT_DIMENSION
        private final ReferenceKind referenceKind;
        private final Integer referenceIndex;     // when VECTOR_AT_INDEX
        private final String label;               // optional display label

        public AxisSummary(StatisticEngine.ScalarType scalarType,
                           String metricName,
                           Integer componentIndex,
                           ReferenceKind referenceKind,
                           Integer referenceIndex,
                           String label) {
            this.scalarType = Objects.requireNonNull(scalarType, "scalarType");
            this.metricName = metricName;
            this.componentIndex = componentIndex;
            this.referenceKind = referenceKind == null ? ReferenceKind.NONE : referenceKind;
            this.referenceIndex = referenceIndex;
            this.label = label;
        }

        public StatisticEngine.ScalarType scalarType() { return scalarType; }
        public String metricName() { return metricName; }
        public Integer componentIndex() { return componentIndex; }
        public ReferenceKind referenceKind() { return referenceKind; }
        public Integer referenceIndex() { return referenceIndex; }
        public String label() { return label; }

        @Override public String toString() {
            return "AxisSummary{" +
                    "scalarType=" + scalarType +
                    ", metricName='" + metricName + '\'' +
                    ", componentIndex=" + componentIndex +
                    ", referenceKind=" + referenceKind +
                    ", referenceIndex=" + referenceIndex +
                    ", label='" + label + '\'' +
                    '}';
        }
    }

    /** Canonical grid & bounds. */
    public static final class GridSummary implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        private final int binsX, binsY;
        private final double minX, maxX, minY, maxY;
        private final double dx, dy;
        private final BoundsPolicy boundsPolicy;
        private final String canonicalPolicyId;   // when CANONICAL_BY_FEATURE

        public GridSummary(int binsX, int binsY,
                           double minX, double maxX, double minY, double maxY,
                           double dx, double dy,
                           BoundsPolicy boundsPolicy,
                           String canonicalPolicyId) {
            this.binsX = binsX; this.binsY = binsY;
            this.minX = minX; this.maxX = maxX; this.minY = minY; this.maxY = maxY;
            this.dx = dx; this.dy = dy;
            this.boundsPolicy = boundsPolicy == null ? BoundsPolicy.DATA_MIN_MAX : boundsPolicy;
            this.canonicalPolicyId = canonicalPolicyId;
        }

        public int binsX() { return binsX; }
        public int binsY() { return binsY; }
        public double minX() { return minX; }
        public double maxX() { return maxX; }
        public double minY() { return minY; }
        public double maxY() { return maxY; }
        public double dx() { return dx; }
        public double dy() { return dy; }
        public BoundsPolicy boundsPolicy() { return boundsPolicy; }
        public String canonicalPolicyId() { return canonicalPolicyId; }

        @Override public String toString() {
            return "GridSummary{" +
                    "binsX=" + binsX +
                    ", binsY=" + binsY +
                    ", minX=" + minX +
                    ", maxX=" + maxX +
                    ", minY=" + minY +
                    ", maxY=" + maxY +
                    ", dx=" + dx +
                    ", dy=" + dy +
                    ", boundsPolicy=" + boundsPolicy +
                    ", canonicalPolicyId='" + canonicalPolicyId + '\'' +
                    '}';
        }
    }

    /** Data/quality/selection details useful for auditing & UI badges. */
    public static final class DataSummary implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        private final long nSamples;                 // number of (x,y) pairs used
        private final double minAvgCountPerCell;     // guard from recipe
        private final boolean sufficiencyPass;       // N/(bx*by) >= minAvgCountPerCell
        private final JpdfRecipe.ScoreMetric scoreMetric;
        private final Double selectionScore;         // preselection score for this pair (nullable)
        private final Integer selectionRank;         // rank among candidates (nullable)

        public DataSummary(long nSamples,
                           double minAvgCountPerCell,
                           boolean sufficiencyPass,
                           JpdfRecipe.ScoreMetric scoreMetric,
                           Double selectionScore,
                           Integer selectionRank) {
            this.nSamples = nSamples;
            this.minAvgCountPerCell = minAvgCountPerCell;
            this.sufficiencyPass = sufficiencyPass;
            this.scoreMetric = scoreMetric;
            this.selectionScore = selectionScore;
            this.selectionRank = selectionRank;
        }

        public long nSamples() { return nSamples; }
        public double minAvgCountPerCell() { return minAvgCountPerCell; }
        public boolean sufficiencyPass() { return sufficiencyPass; }
        public JpdfRecipe.ScoreMetric scoreMetric() { return scoreMetric; }
        public Double selectionScore() { return selectionScore; }
        public Integer selectionRank() { return selectionRank; }

        @Override public String toString() {
            return "DataSummary{" +
                    "nSamples=" + nSamples +
                    ", minAvgCountPerCell=" + minAvgCountPerCell +
                    ", sufficiencyPass=" + sufficiencyPass +
                    ", scoreMetric=" + scoreMetric +
                    ", selectionScore=" + selectionScore +
                    ", selectionRank=" + selectionRank +
                    '}';
        }
    }

    /** Numeric hygiene checks for the produced surface. */
    public static final class NumericChecks implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        private final Double pdfMass;          // sum(pdf)*dx*dy (≈1 for PDFs)
        private final Double cdfTerminal;      // cdf[bY-1][bX-1] (≈1 for CDFs)
        private final Boolean cdfMonotoneXY;   // true if monotone in +x and +y
        private final Double minZ, maxZ;       // value range

        public NumericChecks(Double pdfMass, Double cdfTerminal, Boolean cdfMonotoneXY, Double minZ, Double maxZ) {
            this.pdfMass = pdfMass;
            this.cdfTerminal = cdfTerminal;
            this.cdfMonotoneXY = cdfMonotoneXY;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        public Double pdfMass() { return pdfMass; }
        public Double cdfTerminal() { return cdfTerminal; }
        public Boolean cdfMonotoneXY() { return cdfMonotoneXY; }
        public Double minZ() { return minZ; }
        public Double maxZ() { return maxZ; }

        @Override public String toString() {
            return "NumericChecks{" +
                    "pdfMass=" + pdfMass +
                    ", cdfTerminal=" + cdfTerminal +
                    ", cdfMonotoneXY=" + cdfMonotoneXY +
                    ", minZ=" + minZ +
                    ", maxZ=" + maxZ +
                    '}';
        }
    }

    // ---------- Immutable fields ----------

    private final Operation operation;
    private final SurfaceKind surfaceKind;

    private final AxisSummary xAxis;
    private final AxisSummary yAxis;
    private final GridSummary grid;

    private final Alignment alignment;
    private final String recipeName;           // from JpdfRecipe
    private final String canonicalSpecKey;     // cache/spec identity (e.g., FeatureName@policy)
    private final String cacheKey;             // content-hash cache key (if used)

    private final DataSummary data;
    private final NumericChecks numericChecks;

    private final String cohortALabel;         // provenance labels (A/B)
    private final String cohortBLabel;         // nullable for single-cohort
    private final Instant computedAt;
    private final Long computeMillis;          // nullable wall clock time

    // ---------- CTOR/Builder ----------

    private JpdfProvenance(Builder b) {
        this.operation = Objects.requireNonNull(b.operation, "operation");
        this.surfaceKind = Objects.requireNonNull(b.surfaceKind, "surfaceKind");
        this.xAxis = Objects.requireNonNull(b.xAxis, "xAxis");
        this.yAxis = Objects.requireNonNull(b.yAxis, "yAxis");
        this.grid = Objects.requireNonNull(b.grid, "grid");
        this.alignment = b.alignment == null ? Alignment.NONE : b.alignment;
        this.recipeName = b.recipeName;
        this.canonicalSpecKey = b.canonicalSpecKey;
        this.cacheKey = b.cacheKey;
        this.data = b.data;
        this.numericChecks = b.numericChecks;
        this.cohortALabel = b.cohortALabel;
        this.cohortBLabel = b.cohortBLabel;
        this.computedAt = b.computedAt == null ? Instant.now() : b.computedAt;
        this.computeMillis = b.computeMillis;
    }

    public static Builder newBuilder(Operation op, SurfaceKind kind, AxisSummary x, AxisSummary y, GridSummary grid) {
        return new Builder(op, kind, x, y, grid);
    }

    public static final class Builder {
        private final Operation operation;
        private final SurfaceKind surfaceKind;
        private final AxisSummary xAxis;
        private final AxisSummary yAxis;
        private final GridSummary grid;

        private Alignment alignment = Alignment.NONE;
        private String recipeName;
        private String canonicalSpecKey;
        private String cacheKey;

        private DataSummary data;
        private NumericChecks numericChecks;

        private String cohortALabel;
        private String cohortBLabel;
        private Instant computedAt;
        private Long computeMillis;

        private Builder(Operation op, SurfaceKind kind, AxisSummary x, AxisSummary y, GridSummary grid) {
            this.operation = op;
            this.surfaceKind = kind;
            this.xAxis = x;
            this.yAxis = y;
            this.grid = grid;
        }

        public Builder alignment(Alignment v) { this.alignment = v; return this; }
        public Builder recipeName(String v) { this.recipeName = v; return this; }
        public Builder canonicalSpecKey(String v) { this.canonicalSpecKey = v; return this; }
        public Builder cacheKey(String v) { this.cacheKey = v; return this; }
        public Builder data(DataSummary v) { this.data = v; return this; }
        public Builder numericChecks(NumericChecks v) { this.numericChecks = v; return this; }
        public Builder cohortLabels(String a, String b) { this.cohortALabel = a; this.cohortBLabel = b; return this; }
        public Builder computedAt(Instant t) { this.computedAt = t; return this; }
        public Builder computeMillis(Long ms) { this.computeMillis = ms; return this; }

        public JpdfProvenance build() { return new JpdfProvenance(this); }
    }

    // ---------- Getters ----------

    public Operation operation() { return operation; }
    public SurfaceKind surfaceKind() { return surfaceKind; }
    public AxisSummary xAxis() { return xAxis; }
    public AxisSummary yAxis() { return yAxis; }
    public GridSummary grid() { return grid; }
    public Alignment alignment() { return alignment; }
    public String recipeName() { return recipeName; }
    public String canonicalSpecKey() { return canonicalSpecKey; }
    public String cacheKey() { return cacheKey; }
    public DataSummary data() { return data; }
    public NumericChecks numericChecks() { return numericChecks; }
    public String cohortALabel() { return cohortALabel; }
    public String cohortBLabel() { return cohortBLabel; }
    public Instant computedAt() { return computedAt; }
    public Long computeMillis() { return computeMillis; }

    // ---------- Convenience factories ----------

    public static JpdfProvenance baselinePdf(AxisSummary x, AxisSummary y, GridSummary g,
                                             String recipeName, String cohortLabel) {
        return JpdfProvenance.newBuilder(Operation.BASELINE, SurfaceKind.PDF, x, y, g)
                .recipeName(recipeName)
                .cohortLabels(cohortLabel, null)
                .build();
    }

    public static JpdfProvenance baselineCdf(AxisSummary x, AxisSummary y, GridSummary g,
                                             String recipeName, String cohortLabel) {
        return JpdfProvenance.newBuilder(Operation.BASELINE, SurfaceKind.CDF, x, y, g)
                .recipeName(recipeName)
                .cohortLabels(cohortLabel, null)
                .build();
    }

    public static JpdfProvenance signedDiff(AxisSummary x, AxisSummary y, GridSummary g,
                                            String recipeName, String cohortA, String cohortB) {
        return JpdfProvenance.newBuilder(Operation.DIFFERENCE, SurfaceKind.COMPARISON_SIGNED, x, y, g)
                .recipeName(recipeName)
                .cohortLabels(cohortA, cohortB)
                .build();
    }

    @Override public String toString() {
        return "JpdfProvenance{" +
                "operation=" + operation +
                ", surfaceKind=" + surfaceKind +
                ", xAxis=" + xAxis +
                ", yAxis=" + yAxis +
                ", grid=" + grid +
                ", alignment=" + alignment +
                ", recipeName='" + recipeName + '\'' +
                ", canonicalSpecKey='" + canonicalSpecKey + '\'' +
                ", cacheKey='" + cacheKey + '\'' +
                ", data=" + data +
                ", numericChecks=" + numericChecks +
                ", cohortALabel='" + cohortALabel + '\'' +
                ", cohortBLabel='" + cohortBLabel + '\'' +
                ", computedAt=" + computedAt +
                ", computeMillis=" + computeMillis +
                '}';
    }
}
