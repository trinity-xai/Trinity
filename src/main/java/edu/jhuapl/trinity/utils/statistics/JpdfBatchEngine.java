package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * JpdfBatchEngine
 * ---------------
 * Orchestrates batch generation of joint PDF/CDF surfaces based on a JpdfRecipe.
 * <p>
 * Supports:
 * • Component-pairs mode (auto enumerate COMPONENT_AT_DIMENSION pairs).
 * • Whitelist mode (explicit AxisParams pairs from the recipe).
 * <p>
 * Honors recipe:
 * • PairSelection: ALL / WHITELIST / TOP_K_BY_SCORE / THRESHOLD_BY_SCORE
 * • BoundsPolicy: DATA_MIN_MAX / FIXED_01 / CANONICAL_BY_FEATURE
 * • ScoreMetric, minAvgCountPerCell, binsX/binsY, cacheEnabled
 * <p>
 * Threading: fixed pool sized to Runtime.availableProcessors().
 * <p>
 * Note: GridDensityResult contains both PDF and CDF; OutputKind is recorded in provenance
 * by the UI later; engine returns both so consumers can choose.
 */
public final class JpdfBatchEngine {

    public JpdfBatchEngine() {
    }

    /**
     * Per-pair output bundle.
     *
     * @param i          component index for X (or -1 if not component-mode)
     * @param j          component index for Y (or -1 if not component-mode)
     * @param rank       may be null in whitelist mode
     * @param provenance may be null if cache doesn't store it
     */
    public record PairJobResult(int i, int j, AxisParams xAxis, AxisParams yAxis, GridSpec grid, GridDensityResult density, PairScorer.PairScore rank,
                                boolean fromCache, long computeMillis, JpdfProvenance provenance) implements Serializable {


    }

    /**
     * Batch summary + outputs.
     */
    public record BatchResult(String datasetFingerprint, String recipeName, String policyId, List<PairJobResult> jobs, long wallMillis, int submittedPairs,
                              int computedPairs, int cacheHits, DensityCache.Stats cacheStatsSnapshot) implements Serializable {


    }

    // =====================================================================================
    // Component-pairs mode
    // =====================================================================================

    /**
     * Backwards-compatible entry (no live append).
     */
    public static BatchResult runComponentPairs(List<FeatureVector> vectors,
                                                JpdfRecipe recipe,
                                                CanonicalGridPolicy canonicalPolicy,
                                                DensityCache cache) {
        return runComponentPairs(vectors, recipe, canonicalPolicy, cache, null);
    }

    /**
     * Live-append overload: calls onResult for each finished pair (may be null).
     */
    public static BatchResult runComponentPairs(List<FeatureVector> vectors,
                                                JpdfRecipe recipe,
                                                CanonicalGridPolicy canonicalPolicy,
                                                DensityCache cache,
                                                Consumer<PairJobResult> onResult) {
        Objects.requireNonNull(vectors);
        Objects.requireNonNull(recipe);
        Objects.requireNonNull(canonicalPolicy);
        Objects.requireNonNull(cache);

        final long t0 = System.currentTimeMillis();
        final String dsFp = DensityCache.fingerprintDataset(vectors, 256, 64);

        if (vectors.isEmpty() || !recipe.isComponentPairsMode()) {
            return new BatchResult(dsFp, recipe.getName(), canonicalPolicy.id(),
                Collections.emptyList(), 0L, 0, 0, 0, cache.stats());
        }

        PairScorer.Config scorerCfg = PairScorer.Config.defaultFor(
            recipe.getScoreMetric(),
            recipe.getBinsX(),
            recipe.getBinsY(),
            recipe.getMinAvgCountPerCell()
        );

        List<PairScorer.PairScore> candidates = PairScorer.scoreComponentPairs(
            vectors,
            recipe.getComponentIndexStart(),
            recipe.getComponentIndexEnd(),
            recipe.isIncludeSelfPairs(),
            recipe.isOrderedPairs(),
            scorerCfg
        );

        List<PairScorer.PairScore> selected = switch (recipe.getPairSelection()) {
            case ALL -> candidates;
            case TOP_K_BY_SCORE -> {
                int k = Math.max(1, recipe.getTopK());
                k = Math.min(k, candidates.size());
                yield new ArrayList<>(candidates.subList(0, k));
            }
            case THRESHOLD_BY_SCORE -> {
                List<PairScorer.PairScore> out = new ArrayList<>();
                for (PairScorer.PairScore ps : candidates) {
                    if (ps.score() >= recipe.getScoreThreshold() && ps.sufficient()) {
                        out.add(ps);
                    }
                }
                yield out;
            }
            case WHITELIST -> Collections.emptyList();
        };

        if (recipe.getPairSelection() == JpdfRecipe.PairSelection.WHITELIST) {
            return new BatchResult(dsFp, recipe.getName(), canonicalPolicy.id(),
                Collections.emptyList(), 0L, 0, 0, 0, cache.stats());
        }

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CompletionService<PairJobResult> ecs = new ExecutorCompletionService<>(pool);

        int submitted = 0;
        for (PairScorer.PairScore ps : selected) {
            AxisParams x = new AxisParams();
            x.setType(StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION);
            x.setComponentIndex(ps.i());

            AxisParams y = new AxisParams();
            y.setType(StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION);
            y.setComponentIndex(ps.j());

            GridSpec grid = resolveGrid(vectors, x, y, recipe, canonicalPolicy, dsFp);
            ecs.submit(new ComputeTask(vectors, x, y, grid, ps, recipe, cache, dsFp));
            submitted++;
        }

        List<PairJobResult> out = new ArrayList<>(submitted);
        int cacheHits = 0, computed = 0;
        try {
            for (int k = 0; k < submitted; k++) {
                Future<PairJobResult> f = ecs.take();
                PairJobResult r = f.get();
                if (r.fromCache) cacheHits++;
                else computed++;
                out.add(r);

                // Live-append hook (safe-guarded)
                if (onResult != null) {
                    try {
                        onResult.accept(r);
                    } catch (Throwable ignore) { /* isolate UI issues */ }
                }
            }
        } catch (Exception ex) {
            pool.shutdownNow();
            throw new RuntimeException("JpdfBatchEngine: execution interrupted", ex);
        } finally {
            pool.shutdown();
        }

        out.sort(Comparator.comparingDouble(o -> o.rank != null ? -o.rank.score() : 0.0));

        long wall = System.currentTimeMillis() - t0;
        return new BatchResult(dsFp, recipe.getName(), canonicalPolicy.id(),
            out, wall, submitted, computed, cacheHits, cache.stats());
    }

    // =====================================================================================
    // Whitelist mode
    // =====================================================================================

    /**
     * Backwards-compatible entry (no live append).
     */
    public static BatchResult runWhitelistPairs(List<FeatureVector> vectors,
                                                JpdfRecipe recipe,
                                                CanonicalGridPolicy canonicalPolicy,
                                                DensityCache cache) {
        return runWhitelistPairs(vectors, recipe, canonicalPolicy, cache, null);
    }

    /**
     * Live-append overload: calls onResult for each finished pair (may be null).
     */
    public static BatchResult runWhitelistPairs(List<FeatureVector> vectors,
                                                JpdfRecipe recipe,
                                                CanonicalGridPolicy canonicalPolicy,
                                                DensityCache cache,
                                                Consumer<PairJobResult> onResult) {
        Objects.requireNonNull(vectors);
        Objects.requireNonNull(recipe);
        Objects.requireNonNull(canonicalPolicy);
        Objects.requireNonNull(cache);

        final long t0 = System.currentTimeMillis();
        final String dsFp = DensityCache.fingerprintDataset(vectors, 256, 64);

        if (vectors.isEmpty()
            || recipe.getPairSelection() != JpdfRecipe.PairSelection.WHITELIST
            || recipe.getExplicitAxisPairs().isEmpty()) {
            return new BatchResult(dsFp, recipe.getName(), canonicalPolicy.id(),
                Collections.emptyList(), 0L, 0, 0, 0, cache.stats());
        }

        int threads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CompletionService<PairJobResult> ecs = new ExecutorCompletionService<>(pool);

        int submitted = 0;
        for (JpdfRecipe.AxisPair ap : recipe.getExplicitAxisPairs()) {
            AxisParams x = ap.xAxis();
            AxisParams y = ap.yAxis();
            GridSpec grid = resolveGrid(vectors, x, y, recipe, canonicalPolicy, dsFp);
            ecs.submit(new ComputeTask(vectors, x, y, grid, null, recipe, cache, dsFp));
            submitted++;
        }

        List<PairJobResult> out = new ArrayList<>(submitted);
        int cacheHits = 0, computed = 0;
        try {
            for (int k = 0; k < submitted; k++) {
                Future<PairJobResult> f = ecs.take();
                PairJobResult r = f.get();
                if (r.fromCache) cacheHits++;
                else computed++;
                out.add(r);

                // Live-append hook (safe-guarded)
                if (onResult != null) {
                    try {
                        onResult.accept(r);
                    } catch (Throwable ignore) { /* isolate UI issues */ }
                }
            }
        } catch (Exception ex) {
            pool.shutdownNow();
            throw new RuntimeException("JpdfBatchEngine: execution interrupted", ex);
        } finally {
            pool.shutdown();
        }

        long wall = System.currentTimeMillis() - t0;
        return new BatchResult(dsFp, recipe.getName(), canonicalPolicy.id(),
            out, wall, submitted, computed, cacheHits, cache.stats());
    }

    // =====================================================================================
    // Worker
    // =====================================================================================

    private record ComputeTask(List<FeatureVector> vectors, AxisParams x, AxisParams y, GridSpec grid, PairScorer.PairScore rank, JpdfRecipe recipe,
                               DensityCache cache, String datasetFp) implements Callable<PairJobResult> {

        @Override
        public PairJobResult call() {
            final boolean useCache = recipe.isCacheEnabled();
            final String key = useCache ? cache.makeKey(vectors, x, y, grid, datasetFp) : null;

            GridDensityResult cached = (useCache ? cache.get(key) : null);

            long t1 = System.currentTimeMillis();
            GridDensityResult res;
            boolean fromCache;

            if (cached != null) {
                res = cached;
                fromCache = true;
            } else if (useCache) {
                // Call a version of getOrCompute that does NOT require a JpdfProvenance upfront.
                res = cache.getOrCompute(vectors, x, y, grid, datasetFp);
                fromCache = false;
            } else {
                res = GridDensity3DEngine.computePdfCdf2D(vectors, x, y, grid);
                fromCache = false;
            }

            long t2 = System.currentTimeMillis();

            int ii = (x.getType() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION && x.getComponentIndex() != null)
                ? x.getComponentIndex() : -1;
            int jj = (y.getType() == StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION && y.getComponentIndex() != null)
                ? y.getComponentIndex() : -1;

            // Try to retrieve provenance if cache tracks it; otherwise leave null.
            JpdfProvenance prov = (useCache && key != null) ? cache.getProvenance(key) : null;

            return new PairJobResult(ii, jj, x, y, grid,
                res, rank, fromCache, Math.max(0, t2 - t1), prov);
        }
    }

    // =====================================================================================
    // Grid resolution
    // =====================================================================================

    private static GridSpec resolveGrid(List<FeatureVector> vectors,
                                        AxisParams x,
                                        AxisParams y,
                                        JpdfRecipe recipe,
                                        CanonicalGridPolicy canonicalPolicy,
                                        String dsFp) {
        int bx = Math.max(2, recipe.getBinsX());
        int by = Math.max(2, recipe.getBinsY());

        return switch (recipe.getBoundsPolicy()) {
            case FIXED_01 -> {
                GridSpec g = new GridSpec(bx, by);
                g.setMinX(0.0);
                g.setMaxX(1.0);
                g.setMinY(0.0);
                g.setMaxY(1.0);
                yield g;
            }
            case CANONICAL_BY_FEATURE -> canonicalPolicy.gridForAxes(vectors, x, y, bx, by, dsFp);
            case DATA_MIN_MAX -> new GridSpec(bx, by); // engine will infer bounds per pair
        };
    }
}
