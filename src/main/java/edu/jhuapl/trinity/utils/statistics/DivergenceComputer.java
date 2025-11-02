package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * DivergenceComputer
 * ------------------
 * Builds a feature-by-feature divergence matrix D for two cohorts (A vs B), where
 * each entry D[i][j] measures the separation between cohort A and cohort B
 * on the joint PDF over (component i, component j).
 * <p>
 * Supported divergences:
 * - JS:         Jensen–Shannon divergence (base-2 logs). Range: [0, 1].
 * - HELLINGER:  Hellinger distance. Range: [0, 1].
 * - TV:         Total Variation distance (L1/2). Range: [0, 1].
 * <p>
 * Inputs:
 * - cohortA, cohortB: Feature vectors (rows). Must be non-null; may be empty.
 * - component indices: explicit list or recipe-provided range [start..end].
 * - recipe: controls bins and bounds policy for consistent, aligned grids.
 * - cache: optional; respected if recipe.isCacheEnabled() is true.
 * <p>
 * Outputs:
 * - DivergenceResult: symmetric matrix (F x F), labels, indices, metadata, and a quality matrix.
 * <p>
 * Notes:
 * - For each (i,j), we compute aligned GridDensityResult for A and B on the
 * SAME GridSpec (union bounds under the chosen policy), then convert PDFs
 * to mass per cell (pdf * dx * dy) to form discrete distributions over the grid.
 * - We normalize the flattened masses to sum to 1 (guarding numerics) then
 * compute the divergence.
 * - Diagonal cells compare A vs B on the *joint (i,i)* surface (allowed if desired).
 * If you prefer to force diagonal to 0, you can post-process in the UI.
 * <p>
 * This class does not modify any global UI state and is thread-safe per call.
 *
 * @author Sean Phillips (Trinity)
 * @author (helper) ChatGPT
 */
public final class DivergenceComputer {

    // ---------------------------------------------------------------------
    // Types
    // ---------------------------------------------------------------------

    public enum DivergenceMetric {
        JS,         // Jensen–Shannon divergence (base-2 logs) in [0,1]
        HELLINGER,  // Hellinger distance in [0,1]
        TV          // Total variation distance in [0,1]
    }

    /**
     * Output bundle for a single divergence computation.
     *
     * @param matrix            Symmetric divergence matrix (F x F).
     * @param quality           Optional quality matrix (F x F), e.g., min(coverageA, coverageB) fraction in [0,1].
     * @param metric            Chosen divergence metric.
     * @param componentIndices  Selected component indices (length F).
     * @param labels            Human-friendly labels "Comp i", length F.
     * @param binsX             Binning used for X and Y (from recipe).
     * @param boundsPolicy      Bounds policy applied (from recipe).
     * @param canonicalPolicyId Canonical policy id (when boundsPolicy == CANONICAL_BY_FEATURE).
     * @param cacheEnabled      Whether cache was allowed (recipe-level flag; not a guarantee of hits).
     */
    public record DivergenceResult(double[][] matrix, double[][] quality, DivergenceMetric metric, int[] componentIndices, List<String> labels, int binsX,
                                   int binsY, JpdfRecipe.BoundsPolicy boundsPolicy, String canonicalPolicyId, boolean cacheEnabled) implements Serializable {
    }

    private DivergenceComputer() {
    }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * Convenience: compute over the component index range specified by the recipe.
     */
    public static DivergenceResult computeForComponentRange(
        List<FeatureVector> cohortA,
        List<FeatureVector> cohortB,
        JpdfRecipe recipe,
        DivergenceMetric metric,
        DensityCache cache
    ) {
        Objects.requireNonNull(recipe, "recipe");
        int start = Math.max(0, recipe.getComponentIndexStart());
        int end = Math.max(start, recipe.getComponentIndexEnd());

        // Clamp to dimensionality if available
        int dimA = (cohortA == null || cohortA.isEmpty() || cohortA.get(0).getData() == null)
            ? -1 : cohortA.get(0).getData().size();
        int dimB = (cohortB == null || cohortB.isEmpty() || cohortB.get(0).getData() == null)
            ? -1 : cohortB.get(0).getData().size();
        int dim = Math.max(dimA, dimB);
        if (dim >= 0) end = Math.min(end, Math.max(0, dim - 1));

        List<Integer> comps = new ArrayList<>();
        for (int i = start; i <= end; i++) comps.add(i);

        return computeForComponents(cohortA, cohortB, comps, recipe, metric, cache);
    }

    /**
     * Compute the divergence matrix for explicit component indices.
     *
     * @param cohortA          cohort A feature vectors
     * @param cohortB          cohort B feature vectors
     * @param componentIndices component indices (0-based) to include
     * @param recipe           controls bins & bounds alignment policy
     * @param metric           divergence metric
     * @param cache            optional; used if recipe.isCacheEnabled()
     */
    public static DivergenceResult computeForComponents(
        List<FeatureVector> cohortA,
        List<FeatureVector> cohortB,
        List<Integer> componentIndices,
        JpdfRecipe recipe,
        DivergenceMetric metric,
        DensityCache cache
    ) {
        Objects.requireNonNull(cohortA, "cohortA");
        Objects.requireNonNull(cohortB, "cohortB");
        Objects.requireNonNull(componentIndices, "componentIndices");
        Objects.requireNonNull(recipe, "recipe");
        Objects.requireNonNull(metric, "metric");

        // Early out
        if (componentIndices.isEmpty()) {
            return new DivergenceResult(new double[0][0], new double[0][0], metric,
                new int[0], List.of(),
                recipe.getBinsX(), recipe.getBinsY(),
                recipe.getBoundsPolicy(), recipe.getCanonicalPolicyId(),
                recipe.isCacheEnabled());
        }

        // Canonical policy (alignment)
        final CanonicalGridPolicy policy = CanonicalGridPolicy.get(
            (recipe.getBoundsPolicy() == JpdfRecipe.BoundsPolicy.CANONICAL_BY_FEATURE)
                ? recipe.getCanonicalPolicyId()
                : "default"
        );

        final boolean useCache = recipe.isCacheEnabled() && cache != null;

        // Prepare component list/labels
        final int F = componentIndices.size();
        final int[] comps = new int[F];
        final List<String> labels = new ArrayList<>(F);
        for (int k = 0; k < F; k++) {
            comps[k] = componentIndices.get(k);
            labels.add("Comp " + comps[k]);
        }

        // Prepare outputs
        double[][] D = new double[F][F];
        double[][] Q = new double[F][F]; // quality; min fraction of usable samples across cohorts (heuristic)

        // Diagonal init (will be computed like any pair; but we keep symmetry & quality sane)
        for (int i = 0; i < F; i++) {
            D[i][i] = 0.0; // JS(P,P)=0, H(P,P)=0, TV(P,P)=0 theoretically; we still compute to be robust
            Q[i][i] = 1.0;
        }

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();

        final String idA = DensityCache.fingerprintDataset(cohortA, 256, 64);
        final String idB = DensityCache.fingerprintDataset(cohortB, 256, 64);

        for (int a = 0; a < F; a++) {
            final int ia = a;
            futures.add(pool.submit(() -> {
                for (int b = ia; b < F; b++) {
                    int ci = comps[ia];
                    int cj = comps[b];

                    AxisParams x = new AxisParams();
                    x.setType(StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION);
                    x.setComponentIndex(ci);

                    AxisParams y = new AxisParams();
                    y.setType(StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION);
                    y.setComponentIndex(cj);

                    // Compute aligned baselines using ABComparisonEngine
                    ABComparisonEngine.AbResult ab = ABComparisonEngine.compare(
                        cohortA, cohortB,
                        x, y,
                        recipe,
                        useCache ? cache : null,
                        idA, idB
                    );

                    // Convert PDFs to mass distributions (flatten)
                    MassVec pa = toMass(ab.a());
                    MassVec pb = toMass(ab.b());

                    // Normalize (guard if sums deviate from 1 by numeric noise)
                    normalizeInPlace(pa.values);
                    normalizeInPlace(pb.values);

                    // Divergence
                    double div;
                    switch (metric) {
                        case JS -> div = jsDivergence(pa.values, pb.values);
                        case HELLINGER -> div = hellinger(pa.values, pb.values);
                        case TV -> div = totalVariation(pa.values, pb.values);
                        default -> div = 0.0;
                    }

                    // Simple quality proxy: min coverage ratio across cohorts (if any NaN rows were filtered upstream,
                    // this would reflect in counts; GridDensity3DEngine currently bins every row, so set 1.0).
                    // Keep hook for future enhancements.
                    double q = 1.0;

                    D[ia][b] = div;
                    D[b][ia] = div;
                    Q[ia][b] = q;
                    Q[b][ia] = q;
                }
            }));
        }

        // Wait for all
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                pool.shutdownNow();
                throw new RuntimeException("DivergenceComputer: task failed", e);
            }
        }
        pool.shutdown();

        return new DivergenceResult(
            D, Q, metric,
            comps, labels,
            recipe.getBinsX(), recipe.getBinsY(),
            recipe.getBoundsPolicy(), recipe.getCanonicalPolicyId(),
            recipe.isCacheEnabled()
        );
    }

    // ---------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------

    /**
     * Flatten pdf grid to mass vector (z * dx * dy).
     */
    private static MassVec toMass(GridDensityResult r) {
        double[][] pdf = r.pdfZ();
        int by = pdf.length;
        int bx = (by == 0) ? 0 : pdf[0].length;
        double[] out = new double[bx * by];
        double w = r.dx() * r.dy();
        int k = 0;
        for (int y = 0; y < by; y++) {
            double[] row = pdf[y];
            for (int x = 0; x < bx; x++) {
                double v = row[x] * w;
                out[k++] = (Double.isFinite(v) && v > 0) ? v : 0.0;
            }
        }
        return new MassVec(out);
    }

    private record MassVec(double[] values) {
    }

    /**
     * Normalize a vector to sum=1 (if sum<=0, make uniform).
     */
    private static void normalizeInPlace(double[] p) {
        double s = 0.0;
        for (double v : p) s += v;
        if (!(s > 0)) {
            double u = 1.0 / Math.max(1, p.length);
            for (int i = 0; i < p.length; i++) p[i] = u;
            return;
        }
        for (int i = 0; i < p.length; i++) p[i] /= s;
    }

    // ---------------- Jensen–Shannon divergence (base-2 logs) ----------------
    private static double jsDivergence(double[] p, double[] q) {
        int n = Math.min(p.length, q.length);
        double d = 0.0;
        for (int i = 0; i < n; i++) {
            double pi = clampProb(p[i]);
            double qi = clampProb(q[i]);
            double mi = 0.5 * (pi + qi);
            d += klTerm(pi, mi) + klTerm(qi, mi);
        }
        return 0.5 * d; // already base-2
    }

    /**
     * KL term with base-2 logs, guarding for zeros.
     */
    private static double klTerm(double a, double b) {
        if (a <= 0 || b <= 0) return 0.0;
        return a * log2(a / b);
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2.0);
    }

    private static double clampProb(double v) {
        if (Double.isNaN(v) || v < 0) return 0.0;
        if (v > 1) return 1.0;
        return v;
    }

    // ---------------- Hellinger distance ----------------
    private static double hellinger(double[] p, double[] q) {
        int n = Math.min(p.length, q.length);
        double s = 0.0;
        for (int i = 0; i < n; i++) {
            double a = Math.sqrt(clampProb(p[i]));
            double b = Math.sqrt(clampProb(q[i]));
            double d = a - b;
            s += d * d;
        }
        return Math.min(1.0, Math.sqrt(s) / Math.sqrt(2.0));
    }

    // ---------------- Total variation distance ----------------
    private static double totalVariation(double[] p, double[] q) {
        int n = Math.min(p.length, q.length);
        double s = 0.0;
        for (int i = 0; i < n; i++) {
            s += Math.abs(clampProb(p[i]) - clampProb(q[i]));
        }
        return Math.min(1.0, 0.5 * s);
    }
}
