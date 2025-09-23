package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import java.io.Serial;
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
 *
 * Supports:
 *  • Component-pairs mode (auto enumerate COMPONENT_AT_DIMENSION pairs).
 *  • Whitelist mode (explicit AxisParams pairs from the recipe).
 *
 * Honors recipe:
 *  • PairSelection: ALL / WHITELIST / TOP_K_BY_SCORE / THRESHOLD_BY_SCORE
 *  • BoundsPolicy: DATA_MIN_MAX / FIXED_01 / CANONICAL_BY_FEATURE
 *  • ScoreMetric, minAvgCountPerCell, binsX/binsY, cacheEnabled
 *
 * Threading: fixed pool sized to Runtime.availableProcessors().
 *
 * Note: GridDensityResult contains both PDF and CDF; OutputKind is recorded in provenance
 *       by the UI later; engine returns both so consumers can choose.
 */
public final class JpdfBatchEngine {

    public JpdfBatchEngine() {}

    /** Per-pair output bundle. */
    public static final class PairJobResult implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        public final int i;                     // component index for X (or -1 if not component-mode)
        public final int j;                     // component index for Y (or -1 if not component-mode)
        public final AxisParams xAxis;
        public final AxisParams yAxis;
        public final GridSpec grid;
        public final GridDensityResult density;
        public final PairScorer.PairScore rank; // may be null in whitelist mode
        public final boolean fromCache;
        public final long computeMillis;
        public final JpdfProvenance provenance; // may be null if cache doesn't store it

        public PairJobResult(int i,
                             int j,
                             AxisParams xAxis,
                             AxisParams yAxis,
                             GridSpec grid,
                             GridDensityResult density,
                             PairScorer.PairScore rank,
                             boolean fromCache,
                             long computeMillis,
                             JpdfProvenance provenance) {
            this.i = i;
            this.j = j;
            this.xAxis = xAxis;
            this.yAxis = yAxis;
            this.grid = grid;
            this.density = density;
            this.rank = rank;
            this.fromCache = fromCache;
            this.computeMillis = computeMillis;
            this.provenance = provenance;
        }
    }

    /** Batch summary + outputs. */
    public static final class BatchResult implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        public final String datasetFingerprint;
        public final String recipeName;
        public final String policyId;
        public final List<PairJobResult> jobs;
        public final long wallMillis;
        public final int submittedPairs;
        public final int computedPairs;
        public final int cacheHits;
        public final DensityCache.Stats cacheStatsSnapshot;

        public BatchResult(String datasetFingerprint,
                           String recipeName,
                           String policyId,
                           List<PairJobResult> jobs,
                           long wallMillis,
                           int submittedPairs,
                           int computedPairs,
                           int cacheHits,
                           DensityCache.Stats cacheStatsSnapshot) {
            this.datasetFingerprint = datasetFingerprint;
            this.recipeName = recipeName;
            this.policyId = policyId;
            this.jobs = jobs;
            this.wallMillis = wallMillis;
            this.submittedPairs = submittedPairs;
            this.computedPairs = computedPairs;
            this.cacheHits = cacheHits;
            this.cacheStatsSnapshot = cacheStatsSnapshot;
        }
    }

    // =====================================================================================
    // Component-pairs mode
    // =====================================================================================

    /** Backwards-compatible entry (no live append). */
    public static BatchResult runComponentPairs(List<FeatureVector> vectors,
                                                JpdfRecipe recipe,
                                                CanonicalGridPolicy canonicalPolicy,
                                                DensityCache cache) {
        return runComponentPairs(vectors, recipe, canonicalPolicy, cache, null);
    }

    /** Live-append overload: calls onResult for each finished pair (may be null). */
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
                    if (ps.score >= recipe.getScoreThreshold() && ps.sufficient) {
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
            x.setComponentIndex(ps.i);

            AxisParams y = new AxisParams();
            y.setType(StatisticEngine.ScalarType.COMPONENT_AT_DIMENSION);
            y.setComponentIndex(ps.j);

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
                if (r.fromCache) cacheHits++; else computed++;
                out.add(r);

                // Live-append hook (safe-guarded)
                if (onResult != null) {
                    try { onResult.accept(r); } catch (Throwable ignore) { /* isolate UI issues */ }
                }
            }
        } catch (Exception ex) {
            pool.shutdownNow();
            throw new RuntimeException("JpdfBatchEngine: execution interrupted", ex);
        } finally {
            pool.shutdown();
        }

        out.sort(Comparator.comparingDouble(o -> o.rank != null ? -o.rank.score : 0.0));

        long wall = System.currentTimeMillis() - t0;
        return new BatchResult(dsFp, recipe.getName(), canonicalPolicy.id(),
                out, wall, submitted, computed, cacheHits, cache.stats());
    }

    // =====================================================================================
    // Whitelist mode
    // =====================================================================================

    /** Backwards-compatible entry (no live append). */
    public static BatchResult runWhitelistPairs(List<FeatureVector> vectors,
                                                JpdfRecipe recipe,
                                                CanonicalGridPolicy canonicalPolicy,
                                                DensityCache cache) {
        return runWhitelistPairs(vectors, recipe, canonicalPolicy, cache, null);
    }

    /** Live-append overload: calls onResult for each finished pair (may be null). */
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
                if (r.fromCache) cacheHits++; else computed++;
                out.add(r);

                // Live-append hook (safe-guarded)
                if (onResult != null) {
                    try { onResult.accept(r); } catch (Throwable ignore) { /* isolate UI issues */ }
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

    private static final class ComputeTask implements Callable<PairJobResult> {
        private final List<FeatureVector> vectors;
        private final AxisParams x;
        private final AxisParams y;
        private final GridSpec grid;
        private final PairScorer.PairScore rank;
        private final JpdfRecipe recipe;
        private final DensityCache cache;
        private final String datasetFp;

        ComputeTask(List<FeatureVector> vectors,
                    AxisParams x,
                    AxisParams y,
                    GridSpec grid,
                    PairScorer.PairScore rank,
                    JpdfRecipe recipe,
                    DensityCache cache,
                    String datasetFp) {
            this.vectors = vectors;
            this.x = x;
            this.y = y;
            this.grid = grid;
            this.rank = rank;
            this.recipe = recipe;
            this.cache = cache;
            this.datasetFp = datasetFp;
        }

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
                g.setMinX(0.0); g.setMaxX(1.0);
                g.setMinY(0.0); g.setMaxY(1.0);
                yield g;
            }
            case CANONICAL_BY_FEATURE -> canonicalPolicy.gridForAxes(vectors, x, y, bx, by, dsFp);
            case DATA_MIN_MAX -> new GridSpec(bx, by); // engine will infer bounds per pair
        };
    }
}
