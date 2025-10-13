package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AbComparisonEngine
 * ------------------
 * Computes aligned baseline PDF/CDF surfaces for cohorts A and B on the same axes,
 * then (optionally) produces signed difference maps (A - B). Bins come from the recipe;
 * bounds come from the recipe policy (unioned across cohorts for canonical alignment).
 * <p>
 * This class does NOT select axis pairs; it assumes the caller already chose (xAxis,yAxis).
 * For batch pair selection use JpdfBatchEngine (which can call into this for A/B).
 * <p>
 * Outputs:
 * - Baseline A and B GridDensityResult (PDF and/or CDF depending on recipe)
 * - Signed difference maps as List<List<Double>> (PDF and/or CDF), same grid
 * - JpdfProvenance for each produced surface
 * <p>
 * Notes:
 * - We align both cohorts on one canonical GridSpec to ensure apples-to-apples differences.
 * - Bounds are the union of each cohort's axis ranges under the chosen CanonicalGridPolicy.
 * - When cache is provided & enabled in the recipe, we use it; otherwise compute directly.
 *
 * @author Sean Phillips
 */
public final class ABComparisonEngine {

    /**
     * Immutable result bundle for a single A/B compare on one (x,y) pair.
     */
    public static final class AbResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public final GridSpec grid;

        public final GridDensityResult a;             // baseline A (may contain both PDF/CDF)
        public final GridDensityResult b;             // baseline B

        public final List<List<Double>> pdfDiff;      // A.pdf - B.pdf (nullable if not requested)
        public final List<List<Double>> cdfDiff;      // A.cdf - B.cdf (nullable if not requested)

        public final JpdfProvenance pdfProvA;         // nullable when recipe does not request PDF
        public final JpdfProvenance pdfProvB;         // nullable when recipe does not request PDF
        public final JpdfProvenance pdfProvDiff;      // nullable when recipe does not request PDF

        public final JpdfProvenance cdfProvA;         // nullable when recipe does not request CDF
        public final JpdfProvenance cdfProvB;         // nullable when recipe does not request CDF
        public final JpdfProvenance cdfProvDiff;      // nullable when recipe does not request CDF

        public AbResult(GridSpec grid,
                        GridDensityResult a,
                        GridDensityResult b,
                        List<List<Double>> pdfDiff,
                        List<List<Double>> cdfDiff,
                        JpdfProvenance pdfProvA,
                        JpdfProvenance pdfProvB,
                        JpdfProvenance pdfProvDiff,
                        JpdfProvenance cdfProvA,
                        JpdfProvenance cdfProvB,
                        JpdfProvenance cdfProvDiff) {
            this.grid = grid;
            this.a = a;
            this.b = b;
            this.pdfDiff = pdfDiff;
            this.cdfDiff = cdfDiff;
            this.pdfProvA = pdfProvA;
            this.pdfProvB = pdfProvB;
            this.pdfProvDiff = pdfProvDiff;
            this.cdfProvA = cdfProvA;
            this.cdfProvB = cdfProvB;
            this.cdfProvDiff = cdfProvDiff;
        }
    }

    private ABComparisonEngine() {
    }

    /**
     * Compute an A/B comparison for given axes. Uses recipe bins & bounds policy.
     *
     * @param aVectors   cohort A
     * @param bVectors   cohort B
     * @param xAxis      X axis params
     * @param yAxis      Y axis params
     * @param recipe     Jpdf recipe (bins/bounds/output-kind/cache flags)
     * @param cache      optional cache (respected if recipe.isCacheEnabled()==true)
     * @param datasetIdA optional stable ID for cohort A (for cache keying)
     * @param datasetIdB optional stable ID for cohort B (for cache keying)
     */
    public static AbResult compare(List<FeatureVector> aVectors,
                                   List<FeatureVector> bVectors,
                                   AxisParams xAxis,
                                   AxisParams yAxis,
                                   JpdfRecipe recipe,
                                   DensityCache cache,
                                   String datasetIdA,
                                   String datasetIdB) {

        Objects.requireNonNull(xAxis, "xAxis");
        Objects.requireNonNull(yAxis, "yAxis");
        Objects.requireNonNull(recipe, "recipe");

        // 1) Build a single canonical grid shared by A and B.
        GridSpec grid = buildAlignedGrid(aVectors, bVectors, xAxis, yAxis, recipe);

        // 2) Compute baselines (A and B) using cache if available/allowed.
        boolean useCache = cache != null && recipe.isCacheEnabled();
        GridDensityResult resA = useCache
            ? cache.getOrCompute(aVectors, xAxis, yAxis, grid, datasetIdA)
            : GridDensity3DEngine.computePdfCdf2D(aVectors, xAxis, yAxis, grid);

        GridDensityResult resB = useCache
            ? cache.getOrCompute(bVectors, xAxis, yAxis, grid, datasetIdB)
            : GridDensity3DEngine.computePdfCdf2D(bVectors, xAxis, yAxis, grid);

        // 3) Optionally build signed differences.
        boolean needPdf = recipe.getOutputKind() == JpdfRecipe.OutputKind.PDF_ONLY
            || recipe.getOutputKind() == JpdfRecipe.OutputKind.PDF_AND_CDF;
        boolean needCdf = recipe.getOutputKind() == JpdfRecipe.OutputKind.CDF_ONLY
            || recipe.getOutputKind() == JpdfRecipe.OutputKind.PDF_AND_CDF;

        List<List<Double>> pdfDiff = null;
        List<List<Double>> cdfDiff = null;

        if (needPdf) {
            List<List<Double>> aPdf = resA.pdfAsListGrid();
            List<List<Double>> bPdf = resB.pdfAsListGrid();
            pdfDiff = subtractGrids(aPdf, bPdf);
        }
        if (needCdf) {
            List<List<Double>> aCdf = resA.cdfAsListGrid();
            List<List<Double>> bCdf = resB.cdfAsListGrid();
            cdfDiff = subtractGrids(aCdf, bCdf);
        }

        // 4) Provenance for A, B, and Diff (PDF and CDF separately when requested).
        String recipeName = recipe.getName();
        String cohortA = recipe.getCohortALabel();
        String cohortB = recipe.getCohortBLabel();

        // Axis & grid summaries reused across provs
        JpdfProvenance.AxisSummary xSum = toAxisSummary(xAxis);
        JpdfProvenance.AxisSummary ySum = toAxisSummary(yAxis);
        JpdfProvenance.GridSummary gSum = toGridSummary(grid, recipe);

        JpdfProvenance pdfProvA = null, pdfProvB = null, pdfProvDiff = null;
        JpdfProvenance cdfProvA = null, cdfProvB = null, cdfProvDiff = null;

        if (needPdf) {
            pdfProvA = JpdfProvenance.baselinePdf(xSum, ySum, gSum, recipeName, cohortA);
            pdfProvB = JpdfProvenance.baselinePdf(xSum, ySum, gSum, recipeName, cohortB);
            pdfProvDiff = JpdfProvenance.signedDiff(xSum, ySum, gSum, recipeName, cohortA, cohortB);
        }
        if (needCdf) {
            cdfProvA = JpdfProvenance.baselineCdf(xSum, ySum, gSum, recipeName, cohortA);
            cdfProvB = JpdfProvenance.baselineCdf(xSum, ySum, gSum, recipeName, cohortB);
            cdfProvDiff = JpdfProvenance.signedDiff(xSum, ySum, gSum, recipeName, cohortA, cohortB);
        }

        return new AbResult(
            grid,
            resA,
            resB,
            pdfDiff,
            cdfDiff,
            pdfProvA,
            pdfProvB,
            pdfProvDiff,
            cdfProvA,
            cdfProvB,
            cdfProvDiff
        );
    }

    // -------------------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------------------

    /**
     * Build one canonical grid for BOTH cohorts using the recipe's bounds policy and bins.
     */
    private static GridSpec buildAlignedGrid(List<FeatureVector> aVectors,
                                             List<FeatureVector> bVectors,
                                             AxisParams xAxis,
                                             AxisParams yAxis,
                                             JpdfRecipe recipe) {
        int bx = recipe.getBinsX();
        int by = recipe.getBinsY();

        GridSpec g = new GridSpec(bx, by);

        switch (recipe.getBoundsPolicy()) {
            case FIXED_01 -> {
                g.setMinX(0.0);
                g.setMaxX(1.0);
                g.setMinY(0.0);
                g.setMaxY(1.0);
            }
            case DATA_MIN_MAX -> {
                // derive per-cohort ranges then take union
                CanonicalGridPolicy policy = CanonicalGridPolicy.get("minmax");
                CanonicalGridPolicy.AxisRange rxA = policy.axisRange(aVectors, xAxis, "A");
                CanonicalGridPolicy.AxisRange ryA = policy.axisRange(aVectors, yAxis, "A");
                CanonicalGridPolicy.AxisRange rxB = policy.axisRange(bVectors, xAxis, "B");
                CanonicalGridPolicy.AxisRange ryB = policy.axisRange(bVectors, yAxis, "B");
                g.setMinX(Math.min(rxA.min, rxB.min));
                g.setMaxX(Math.max(rxA.max, rxB.max));
                g.setMinY(Math.min(ryA.min, ryB.min));
                g.setMaxY(Math.max(ryA.max, ryB.max));
            }
            case CANONICAL_BY_FEATURE -> {
                // Use the named policy, but union A/B axis ranges to avoid clipping one cohort.
                CanonicalGridPolicy policy = CanonicalGridPolicy.get(recipe.getCanonicalPolicyId());
                CanonicalGridPolicy.AxisRange rxA = policy.axisRange(aVectors, xAxis, "A");
                CanonicalGridPolicy.AxisRange ryA = policy.axisRange(aVectors, yAxis, "A");
                CanonicalGridPolicy.AxisRange rxB = policy.axisRange(bVectors, xAxis, "B");
                CanonicalGridPolicy.AxisRange ryB = policy.axisRange(bVectors, yAxis, "B");
                g.setMinX(Math.min(rxA.min, rxB.min));
                g.setMaxX(Math.max(rxA.max, rxB.max));
                g.setMinY(Math.min(ryA.min, ryB.min));
                g.setMaxY(Math.max(ryA.max, ryB.max));
            }
        }
        return g;
    }

    /**
     * Create a provenance AxisSummary from AxisParams (best-effort without extra context).
     */
    private static JpdfProvenance.AxisSummary toAxisSummary(AxisParams a) {
        // We don't know if a "mean" or "vector@idx" was used; treat METRIC reference as CUSTOM when present.
        JpdfProvenance.AxisSummary.ReferenceKind refKind;
        Integer refIndex = null;

        if (a.getType() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN) {
            refKind = (a.getReferenceVec() != null) ? JpdfProvenance.AxisSummary.ReferenceKind.CUSTOM
                : JpdfProvenance.AxisSummary.ReferenceKind.NONE;
        } else if (a.getType() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION) {
            refKind = JpdfProvenance.AxisSummary.ReferenceKind.NONE;
        } else {
            refKind = JpdfProvenance.AxisSummary.ReferenceKind.NONE;
        }

        String label = a.getMetricName(); // harmless display hint; may be null
        return new JpdfProvenance.AxisSummary(
            a.getType(),
            a.getMetricName(),
            a.getComponentIndex(),
            refKind,
            refIndex,
            label
        );
    }

    /**
     * Convert GridSpec + recipe bounds policy into a provenance GridSummary.
     */
    private static JpdfProvenance.GridSummary toGridSummary(GridSpec g, JpdfRecipe recipe) {
        int bx = g.getBinsX();
        int by = g.getBinsY();
        double minX = nz(g.getMinX(), 0.0);
        double maxX = nz(g.getMaxX(), 1.0);
        double minY = nz(g.getMinY(), 0.0);
        double maxY = nz(g.getMaxY(), 1.0);
        double dx = (maxX - minX) / Math.max(1, bx);
        double dy = (maxY - minY) / Math.max(1, by);

        JpdfProvenance.BoundsPolicy bp = switch (recipe.getBoundsPolicy()) {
            case DATA_MIN_MAX -> JpdfProvenance.BoundsPolicy.DATA_MIN_MAX;
            case FIXED_01 -> JpdfProvenance.BoundsPolicy.FIXED_01;
            case CANONICAL_BY_FEATURE -> JpdfProvenance.BoundsPolicy.CANONICAL_BY_FEATURE;
        };

        return new JpdfProvenance.GridSummary(
            bx, by,
            minX, maxX, minY, maxY,
            dx, dy,
            bp,
            recipe.getCanonicalPolicyId()
        );
    }

    private static double nz(Double v, double dflt) {
        return v == null ? dflt : v;
    }

    /**
     * Element-wise A - B (assumes equal shape). Returns null if either is null.
     */
    private static List<List<Double>> subtractGrids(List<List<Double>> a, List<List<Double>> b) {
        if (a == null || b == null) return null;
        int rows = Math.min(a.size(), b.size());
        List<List<Double>> out = new ArrayList<>(rows);
        for (int r = 0; r < rows; r++) {
            List<Double> ar = a.get(r);
            List<Double> br = b.get(r);
            int cols = Math.min(ar.size(), br.size());
            List<Double> rr = new ArrayList<>(cols);
            for (int c = 0; c < cols; c++) rr.add(ar.get(c) - br.get(c));
            out.add(rr);
        }
        return out;
    }
}
