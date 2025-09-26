package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.statistics.DivergenceComputer.DivergenceMetric;
import edu.jhuapl.trinity.utils.statistics.PairScorer.Config;
import edu.jhuapl.trinity.utils.statistics.PairScorer.PairScore;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * PairwiseMatrixEngine
 * --------------------
 * High-level orchestration to produce N×N matrices over component indices:
 *
 * 1) Similarity (single cohort)
 *    - Uses {@link PairScorer} for pairwise scores (PEARSON, KENDALL, MI_LITE, DIST_CORR).
 *    - Fills a symmetric matrix S[i][j] over a selected component index range from {@link JpdfRecipe}.
 *    - Also returns a "quality" matrix Q[i][j] based on PairScorer's sufficiency flag (1.0 or 0.0).
 *
 * 2) Divergence (A vs B)
 *    - Uses {@link DivergenceComputer} (JS / HELLINGER / TV).
 *    - Reuses canonical grid alignment via {@link ABComparisonEngine} under the hood.
 *    - Returns D[i][j] and (optionally) a quality matrix carried from DivergenceComputer.
 *
 * Output is a compact {@link MatrixResult} suitable for {@code MatrixHeatmapView}.
 *
 * This class contains no UI code and is thread-safe per call.
 *
 * @author Sean Phillips
 */
public final class PairwiseMatrixEngine {

    private PairwiseMatrixEngine() { }

    // ------------------------------------------------------------------------------------
    // Result DTO
    // ------------------------------------------------------------------------------------

    /** Immutable result bundle for a computed matrix (similarity or divergence). */
    public static final class MatrixResult implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        /** The primary N×N matrix (scores or divergences). */
        public final double[][] matrix;

        /** Optional N×N quality matrix (same shape), may be null. */
        public final double[][] quality;

        /** Component indices (length N). */
        public final int[] componentIndices;

        /** Human-readable labels for rows/cols (length N). */
        public final List<String> labels;

        /** Legend label for UI (e.g., "Pearson |r|" or "JS divergence"). */
        public final String legendLabel;

        /** Suggested title (e.g., "Similarity: PEARSON" or "Divergence: JS"). */
        public final String title;

        private MatrixResult(double[][] matrix,
                             double[][] quality,
                             int[] componentIndices,
                             List<String> labels,
                             String legendLabel,
                             String title) {
            this.matrix = matrix;
            this.quality = quality;
            this.componentIndices = componentIndices;
            this.labels = labels;
            this.legendLabel = legendLabel;
            this.title = title;
        }

        public static MatrixResult of(double[][] matrix,
                                      double[][] quality,
                                      int[] comps,
                                      List<String> labels,
                                      String legend,
                                      String title) {
            return new MatrixResult(matrix, quality, comps, labels, legend, title);
        }
    }

    // ------------------------------------------------------------------------------------
    // Public API: Similarity (single cohort)
    // ------------------------------------------------------------------------------------

    /**
     * Compute a component-by-component similarity matrix for a single cohort
     * using the score metric specified in the recipe.
     *
     * Diagonal entries are computed as the metric of a component with itself
     * when {@code includeDiagonal} is true; otherwise set to 0.
     */
    public static MatrixResult computeSimilarityMatrix(
            List<FeatureVector> cohort,
            JpdfRecipe recipe,
            boolean includeDiagonal
    ) {
        Objects.requireNonNull(cohort, "cohort");
        Objects.requireNonNull(recipe, "recipe");

        // Resolve component range from recipe and clamp to dimensionality if available
        int start = Math.max(0, recipe.getComponentIndexStart());
        int end = Math.max(start, recipe.getComponentIndexEnd());
        int dim = inferMaxDim(cohort);
        if (dim >= 0) end = Math.min(end, Math.max(0, dim - 1));

        List<Integer> compsList = new ArrayList<>();
        for (int i = start; i <= end; i++) compsList.add(i);

        return computeSimilarityMatrixForComponents(
                cohort,
                compsList,
                recipe.getScoreMetric(),
                recipe.getBinsX(),
                recipe.getBinsY(),
                recipe.getMinAvgCountPerCell(),
                includeDiagonal
        );
    }

    /**
     * Compute a similarity matrix for an explicit list of component indices using a chosen score metric.
     */
    public static MatrixResult computeSimilarityMatrixForComponents(
            List<FeatureVector> cohort,
            List<Integer> componentIndices,
            JpdfRecipe.ScoreMetric scoreMetric,
            int binsX,
            int binsY,
            double minAvgCountPerCell,
            boolean includeDiagonal
    ) {
        Objects.requireNonNull(cohort, "cohort");
        Objects.requireNonNull(componentIndices, "componentIndices");
        Objects.requireNonNull(scoreMetric, "scoreMetric");

        if (componentIndices.isEmpty()) {
            return MatrixResult.of(new double[0][0], new double[0][0], new int[0], List.of(),
                    legendForScore(scoreMetric), "Similarity: " + scoreMetric.name());
        }

        // Build contiguous range if possible, otherwise map indices through a dense temporary space
        // For simplicity and performance we will compute by direct column extraction using PairScorer.
        int minIdx = Integer.MAX_VALUE, maxIdx = Integer.MIN_VALUE;
        for (Integer idx : componentIndices) {
            if (idx == null) continue;
            if (idx < minIdx) minIdx = idx;
            if (idx > maxIdx) maxIdx = idx;
        }
        if (minIdx == Integer.MAX_VALUE) {
            return MatrixResult.of(new double[0][0], new double[0][0], new int[0], List.of(),
                    legendForScore(scoreMetric), "Similarity: " + scoreMetric.name());
        }

        // Configure PairScorer (used only for scoring and sufficiency flags)
        Config cfg = Config.defaultFor(scoreMetric, binsX, binsY, minAvgCountPerCell);

        // We will compute pairwise scores for all pairs in [minIdx..maxIdx], then select the subset we need.
        List<PairScore> allScores = PairScorer.scoreComponentPairs(
                cohort,
                minIdx,
                maxIdx,
                includeDiagonal,   // include (i,i) if requested
                false,             // unordered pairs → only i<j from scorer
                cfg
        );

        // Build a lookup table for quick access (i,j) → score/sufficient
        // Since scorer returns only i<j (and possibly i==i), we mirror to j<i.
        int N = componentIndices.size();
        int[] comps = new int[N];
        for (int k = 0; k < N; k++) comps[k] = componentIndices.get(k);

        double[][] M = new double[N][N];
        double[][] Q = new double[N][N];

        // Initialize diagonal per includeDiagonal
        for (int a = 0; a < N; a++) {
            if (includeDiagonal) {
                // We may fill it later if scorer returned a self-pair; otherwise set to 1 (for correlations) or 0.
                // Using a neutral, depends on metric semantics; Pearson(|r|) with self is 1.0.
                M[a][a] = defaultDiagonal(scoreMetric);
                Q[a][a] = 1.0;
            } else {
                M[a][a] = 0.0;
                Q[a][a] = 1.0;
            }
        }

        // Populate from PairScorer results
        for (PairScore ps : allScores) {
            // Ignore pairs outside requested subset
            int iComp = ps.i;
            int jComp = ps.j;
            int ai = indexOf(comps, iComp);
            int aj = indexOf(comps, jComp);
            if (ai < 0 || aj < 0) continue;

            double v = ps.score;
            double q = ps.sufficient ? 1.0 : 0.0;

            // If scorer gave i==j (allowed when includeSelfPairs=true), it lands on the diagonal.
            M[ai][aj] = v;
            Q[ai][aj] = q;
            M[aj][ai] = v;
            Q[aj][ai] = q;
        }

        // Labels
        List<String> labels = new ArrayList<>(N);
        for (int k = 0; k < N; k++) labels.add("Comp " + comps[k]);

        return MatrixResult.of(
                M,
                Q,
                comps,
                labels,
                legendForScore(scoreMetric),
                "Similarity: " + scoreMetric.name()
        );
    }

    // ------------------------------------------------------------------------------------
    // Public API: Divergence (A vs B)
    // ------------------------------------------------------------------------------------

    /**
     * Compute a component-by-component divergence matrix for two cohorts A vs B.
     * This defers to {@link DivergenceComputer} to ensure aligned PDF grids and stable metrics.
     */
    public static MatrixResult computeDivergenceMatrix(
            List<FeatureVector> cohortA,
            List<FeatureVector> cohortB,
            JpdfRecipe recipe,
            DivergenceMetric metric,
            DensityCache cache
    ) {
        Objects.requireNonNull(cohortA, "cohortA");
        Objects.requireNonNull(cohortB, "cohortB");
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(metric, "metric");

        DivergenceComputer.DivergenceResult dr = DivergenceComputer.computeForComponentRange(
                cohortA, cohortB, recipe, metric, cache
        );

        // Labels/indices already computed by DivergenceComputer
        List<String> labels = (dr.labels != null) ? dr.labels : buildDefaultLabels(dr.componentIndices);

        return MatrixResult.of(
                dr.matrix,
                dr.quality,
                dr.componentIndices,
                labels,
                legendForDivergence(metric),
                "Divergence: " + metric.name()
        );
    }

    // ------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------

    private static int inferMaxDim(List<FeatureVector> cohort) {
        if (cohort == null || cohort.isEmpty()) return -1;
        List<Double> row0 = cohort.get(0).getData();
        if (row0 == null) return -1;
        return row0.size();
    }

    private static String legendForScore(JpdfRecipe.ScoreMetric m) {
        return switch (m) {
            case PEARSON -> "Pearson |r|";
            case KENDALL -> "Kendall |τ|";
            case MI_LITE -> "Normalized MI";
            case DIST_CORR -> "Distance Correlation";
        };
    }

    private static String legendForDivergence(DivergenceMetric m) {
        return switch (m) {
            case JS -> "Jensen–Shannon (0..1)";
            case HELLINGER -> "Hellinger (0..1)";
            case TV -> "Total Variation (0..1)";
        };
    }

    private static List<String> buildDefaultLabels(int[] comps) {
        if (comps == null) return List.of();
        List<String> out = new ArrayList<>(comps.length);
        for (int c : comps) out.add("Comp " + c);
        return out;
    }

    private static int indexOf(int[] arr, int v) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == v) return i;
        return -1;
    }

    private static double defaultDiagonal(JpdfRecipe.ScoreMetric m) {
        // For self-vs-self similarity:
        //  - Pearson(|r|): 1
        //  - Kendall(|tau|): 1
        //  - NMI-lite: 1
        //  - DistCorr: 1
        return 1.0;
    }
}
