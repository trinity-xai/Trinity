package edu.jhuapl.trinity.utils.statistics;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * JpdfRecipe
 * -----------
 * Serializable configuration for batch generation of pairwise Joint PDF/CDF surfaces.
 *
 * Scope:
 *  - Generate from COMPONENT_AT_DIMENSION pairs (index-index) OR from an explicit whitelist of Axis pairs.
 *  - Select pairs via ALL / WHITELIST / TOP_K_BY_SCORE / THRESHOLD_BY_SCORE.
 *  - Choose scoring metric used during preselection (Pearson / Kendall / MI_LITE / DIST_CORR).
 *
 * Grid / Bounds:
 *  - binsX/binsY plus a bounds policy (DATA_MIN_MAX, FIXED_01, or CANONICAL_BY_FEATURE via CanonicalGridPolicy id).
 *  - Data-sufficiency guard (minAvgCountPerCell) to suppress low-sample surfaces.
 *
 * Outputs:
 *  - PDF, CDF or BOTH.
 *  - Optional flags: enable cache, save thumbnails, labels for cohorts (for provenance/reporting).
 *
 * Note: This class is a pure configuration object. Execution is handled by JpdfBatchEngine/AbComparisonEngine.
 *
 * @author Sean Phillips
 */
public final class JpdfRecipe implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    // ---------- Enums ----------

    /** How to choose which pairs are built. */
    public enum PairSelection {
        /** Build all valid pairs from COMPONENT_AT_DIMENSION (respecting includeSelf/ordered flags). */
        ALL,
        /** Only build the pairs explicitly provided in {@link #explicitAxisPairs}. */
        WHITELIST,
        /** Pre-score all candidate pairs, then build the top-K by {@link #scoreMetric}. */
        TOP_K_BY_SCORE,
        /** Pre-score all candidate pairs, then build all pairs >= {@link #scoreThreshold}. */
        THRESHOLD_BY_SCORE
    }

    /** Which dependence score to use for preselection. */
    public enum ScoreMetric {
        PEARSON,
        KENDALL,
        MI_LITE,        // inexpensive MI approximation
        DIST_CORR       // distance correlation (lite implementation)
    }

    /** What to emit for each selected pair. */
    public enum OutputKind {
        PDF_ONLY,
        CDF_ONLY,
        PDF_AND_CDF
    }

    /** How X/Y bounds are defined for the 2D grid. */
    public enum BoundsPolicy {
        /** Use min/max from the data actually used for this surface (per-axis). */
        DATA_MIN_MAX,
        /** Lock to [0,1] x [0,1] (useful for probabilities/scores). */
        FIXED_01,
        /**
         * Ask CanonicalGridPolicy (by {@link #canonicalPolicyId}) to provide per-feature canonical bounds.
         * Keeps specs consistent across pairs & cohorts.
         */
        CANONICAL_BY_FEATURE
    }

    // ---------- Axis pair holder (explicit whitelist mode) ----------

    /**
     * AxisPair wraps the axis definitions used by GridDensity3DEngine.
     * In whitelist mode, each pair defines (X axis, Y axis) explicitly.
     */
    public static final class AxisPair implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private final AxisParams xAxis;
        private final AxisParams yAxis;

        public AxisPair(AxisParams xAxis, AxisParams yAxis) {
            this.xAxis = Objects.requireNonNull(xAxis, "xAxis");
            this.yAxis = Objects.requireNonNull(yAxis, "yAxis");
        }
        public AxisParams xAxis() { return xAxis; }
        public AxisParams yAxis() { return yAxis; }
    }

    // ---------- Immutable fields ----------

    private final String name;
    private final String description;

    // Pair selection strategy
    private final PairSelection pairSelection;
    private final ScoreMetric scoreMetric;       // used when TOP_K or THRESHOLD
    private final int topK;                      // used when TOP_K
    private final double scoreThreshold;         // used when THRESHOLD

    // COMPONENT_AT_DIMENSION space (auto generation)
    private final boolean componentPairsMode;    // if true, enumerate index-index pairs
    private final int componentIndexStart;       // inclusive
    private final int componentIndexEnd;         // inclusive
    private final boolean includeSelfPairs;      // allow (i,i)
    private final boolean orderedPairs;          // if false, only i<j

    // Explicit whitelist of axis pairs (used when pairSelection == WHITELIST)
    private final List<AxisPair> explicitAxisPairs;

    // Grid spec policy
    private final int binsX;
    private final int binsY;
    private final BoundsPolicy boundsPolicy;
    private final String canonicalPolicyId;      // name/id for CanonicalGridPolicy to look up (when CANONICAL_BY_FEATURE)

    // Data sufficiency (guards)
    private final double minAvgCountPerCell;     // e.g., require N/(binsX*binsY) >= this

    // Outputs & runtime options
    private final OutputKind outputKind;
    private final boolean cacheEnabled;
    private final boolean saveThumbnails;        // for PairGrid previews

    // Optional cohort labels for provenance/reporting
    private final String cohortALabel;
    private final String cohortBLabel;

    // ---------- Builders / CTOR ----------

    private JpdfRecipe(Builder b) {
        this.name = b.name;
        this.description = b.description;

        this.pairSelection = b.pairSelection;
        this.scoreMetric = b.scoreMetric;
        this.topK = b.topK;
        this.scoreThreshold = b.scoreThreshold;

        this.componentPairsMode = b.componentPairsMode;
        this.componentIndexStart = b.componentIndexStart;
        this.componentIndexEnd = b.componentIndexEnd;
        this.includeSelfPairs = b.includeSelfPairs;
        this.orderedPairs = b.orderedPairs;

        this.explicitAxisPairs = Collections.unmodifiableList(new ArrayList<>(b.explicitAxisPairs));

        this.binsX = b.binsX;
        this.binsY = b.binsY;
        this.boundsPolicy = b.boundsPolicy;
        this.canonicalPolicyId = b.canonicalPolicyId;

        this.minAvgCountPerCell = b.minAvgCountPerCell;

        this.outputKind = b.outputKind;
        this.cacheEnabled = b.cacheEnabled;
        this.saveThumbnails = b.saveThumbnails;

        this.cohortALabel = b.cohortALabel;
        this.cohortBLabel = b.cohortBLabel;

        validate();
    }

    private void validate() throws IllegalArgumentException {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Recipe name is required.");
        if (binsX < 2 || binsY < 2) throw new IllegalArgumentException("binsX and binsY must be >= 2.");
        if (pairSelection == PairSelection.TOP_K_BY_SCORE && topK <= 0)
            throw new IllegalArgumentException("topK must be > 0 for TOP_K_BY_SCORE.");
        if (pairSelection == PairSelection.THRESHOLD_BY_SCORE && !Double.isFinite(scoreThreshold))
            throw new IllegalArgumentException("scoreThreshold must be finite for THRESHOLD_BY_SCORE.");
        if (pairSelection == PairSelection.WHITELIST && explicitAxisPairs.isEmpty())
            throw new IllegalArgumentException("explicitAxisPairs must be non-empty for WHITELIST.");
        if (boundsPolicy == BoundsPolicy.CANONICAL_BY_FEATURE && (canonicalPolicyId == null || canonicalPolicyId.isBlank()))
            throw new IllegalArgumentException("canonicalPolicyId is required for CANONICAL_BY_FEATURE.");
        if (componentPairsMode && (componentIndexEnd < componentIndexStart))
            throw new IllegalArgumentException("componentIndexEnd must be >= componentIndexStart.");
        if (minAvgCountPerCell < 0.0)
            throw new IllegalArgumentException("minAvgCountPerCell cannot be negative.");
    }

    public static Builder newBuilder(String name) {
        return new Builder(name);
    }

    public static final class Builder {
        private final String name;
        private String description = "";

        private PairSelection pairSelection = PairSelection.ALL;
        private ScoreMetric scoreMetric = ScoreMetric.PEARSON;
        private int topK = 20;
        private double scoreThreshold = 0.2;

        private boolean componentPairsMode = true;
        private int componentIndexStart = 0;
        private int componentIndexEnd = 1;
        private boolean includeSelfPairs = false;
        private boolean orderedPairs = false;

        private final List<AxisPair> explicitAxisPairs = new ArrayList<>();

        private int binsX = 64;
        private int binsY = 64;
        private BoundsPolicy boundsPolicy = BoundsPolicy.DATA_MIN_MAX;
        private String canonicalPolicyId = "default";

        private double minAvgCountPerCell = 3.0;

        private OutputKind outputKind = OutputKind.PDF_AND_CDF;
        private boolean cacheEnabled = true;
        private boolean saveThumbnails = true;

        private String cohortALabel = "A";
        private String cohortBLabel = "B";

        private Builder(String name) { this.name = name; }

        public Builder description(String v) { this.description = v; return this; }

        public Builder pairSelection(PairSelection v) { this.pairSelection = v; return this; }
        public Builder scoreMetric(ScoreMetric v) { this.scoreMetric = v; return this; }
        public Builder topK(int v) { this.topK = v; return this; }
        public Builder scoreThreshold(double v) { this.scoreThreshold = v; return this; }

        public Builder componentPairsMode(boolean v) { this.componentPairsMode = v; return this; }
        public Builder componentIndexRange(int startInclusive, int endInclusive) {
            this.componentIndexStart = startInclusive;
            this.componentIndexEnd = endInclusive;
            return this;
        }
        public Builder includeSelfPairs(boolean v) { this.includeSelfPairs = v; return this; }
        public Builder orderedPairs(boolean v) { this.orderedPairs = v; return this; }

        public Builder addAxisPair(AxisPair p) { this.explicitAxisPairs.add(Objects.requireNonNull(p)); return this; }
        public Builder addAxisPair(AxisParams x, AxisParams y) { return addAxisPair(new AxisPair(x, y)); }
        public Builder clearAxisPairs() { this.explicitAxisPairs.clear(); return this; }

        public Builder bins(int sameForBoth) { this.binsX = sameForBoth; this.binsY = sameForBoth; return this; }
        public Builder bins(int bx, int by) { this.binsX = bx; this.binsY = by; return this; }
        public Builder boundsPolicy(BoundsPolicy v) { this.boundsPolicy = v; return this; }
        public Builder canonicalPolicyId(String v) { this.canonicalPolicyId = v; return this; }

        public Builder minAvgCountPerCell(double v) { this.minAvgCountPerCell = v; return this; }

        public Builder outputKind(OutputKind v) { this.outputKind = v; return this; }
        public Builder cacheEnabled(boolean v) { this.cacheEnabled = v; return this; }
        public Builder saveThumbnails(boolean v) { this.saveThumbnails = v; return this; }

        public Builder cohortLabels(String a, String b) { this.cohortALabel = a; this.cohortBLabel = b; return this; }

        public JpdfRecipe build() { return new JpdfRecipe(this); }
    }

    // ---------- Getters ----------

    public String getName() { return name; }
    public String getDescription() { return description; }

    public PairSelection getPairSelection() { return pairSelection; }
    public ScoreMetric getScoreMetric() { return scoreMetric; }
    public int getTopK() { return topK; }
    public double getScoreThreshold() { return scoreThreshold; }

    public boolean isComponentPairsMode() { return componentPairsMode; }
    public int getComponentIndexStart() { return componentIndexStart; }
    public int getComponentIndexEnd() { return componentIndexEnd; }
    public boolean isIncludeSelfPairs() { return includeSelfPairs; }
    public boolean isOrderedPairs() { return orderedPairs; }

    public List<AxisPair> getExplicitAxisPairs() { return explicitAxisPairs; }

    public int getBinsX() { return binsX; }
    public int getBinsY() { return binsY; }
    public BoundsPolicy getBoundsPolicy() { return boundsPolicy; }
    public String getCanonicalPolicyId() { return canonicalPolicyId; }

    public double getMinAvgCountPerCell() { return minAvgCountPerCell; }

    public OutputKind getOutputKind() { return outputKind; }
    public boolean isCacheEnabled() { return cacheEnabled; }
    public boolean isSaveThumbnails() { return saveThumbnails; }

    public String getCohortALabel() { return cohortALabel; }
    public String getCohortBLabel() { return cohortBLabel; }

    // ---------- Convenience ----------

    @Override public String toString() {
        return "JpdfRecipe{" +
                "name='" + name + '\'' +
                ", pairSelection=" + pairSelection +
                ", scoreMetric=" + scoreMetric +
                ", topK=" + topK +
                ", scoreThreshold=" + scoreThreshold +
                ", componentPairsMode=" + componentPairsMode +
                ", componentIndexRange=[" + componentIndexStart + "," + componentIndexEnd + "]" +
                ", includeSelfPairs=" + includeSelfPairs +
                ", orderedPairs=" + orderedPairs +
                ", explicitAxisPairs=" + explicitAxisPairs.size() +
                ", binsX=" + binsX +
                ", binsY=" + binsY +
                ", boundsPolicy=" + boundsPolicy +
                ", canonicalPolicyId='" + canonicalPolicyId + '\'' +
                ", minAvgCountPerCell=" + minAvgCountPerCell +
                ", outputKind=" + outputKind +
                ", cacheEnabled=" + cacheEnabled +
                ", saveThumbnails=" + saveThumbnails +
                ", cohortALabel='" + cohortALabel + '\'' +
                ", cohortBLabel='" + cohortBLabel + '\'' +
                '}';
    }
}
