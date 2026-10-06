package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.function.Consumer;

/**
 * Camera/frustum-aware per-tile LOD selector and transition scheduler.
 *
 * <p>The hot camera-evaluation path is array-backed. Tile IDs are used as direct
 * indexes into reusable primitive state arrays, avoiding HashMap/HashSet lookups,
 * boxing, and per-evaluation TileDecision allocation for the normal renderer path.</p>
 */
public final class TiledLodManager {

    public static final class TileSpec {
        public final int id;
        public final int column;
        public final int row;
        public final double minX;
        public final double maxX;
        public final double minZ;
        public final double maxZ;

        public TileSpec(int id, int column, int row,
                        double minX, double maxX,
                        double minZ, double maxZ) {
            this.id = id;
            this.column = column;
            this.row = row;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        public double centerX() { return (minX + maxX) * 0.5; }
        public double centerZ() { return (minZ + maxZ) * 0.5; }
        public double halfWidth() { return Math.max(0.0, (maxX - minX) * 0.5); }
        public double halfDepth() { return Math.max(0.0, (maxZ - minZ) * 0.5); }
    }

    /**
     * Compatibility snapshot type. The renderer no longer requires these records;
     * they are materialized only if an external compatibility listener is installed.
     */
    public record TileDecision(int tileId, boolean visible, int lodIndex,
                               double depth, double pixelsPerCell,
                               double screenDistance) { }

    public record TransitionResult(boolean applied, boolean built, boolean cacheHit) {
        public static TransitionResult builtResult() {
            return new TransitionResult(true, true, false);
        }

        public static TransitionResult cacheHitResult() {
            return new TransitionResult(true, false, true);
        }

        public static TransitionResult notApplied() {
            return new TransitionResult(false, false, false);
        }
    }

    @FunctionalInterface
    public interface TransitionApplier {
        TransitionResult apply(LodTransition transition, boolean allowBuild);
    }

    @FunctionalInterface
    public interface GeometryCostProvider {
        long trianglesFor(int tileId, int lodIndex);
    }

    /** Immediate visibility callback using the manager's reusable primitive state. */
    @FunctionalInterface
    public interface VisibilityApplier {
        void apply(boolean[] visibleByTile, int tileStateSize);
    }

    public static final class Config {
        public double targetPixelsPerCell = 1.5;
        public double lowThreshold = 1.0;
        public double highThreshold = 2.0;
        /** Finest LOD index allowed to be selected (0 = L0, 1 = L1, ...). */
        public int finestAllowedLod = 0;
        /** Apply a second-stage aggregate triangle budget after per-tile LOD selection. */
        public boolean geometryBudgetEnabled = true;
        /** Maximum target triangles across all currently visible tiles. */
        public long triangleBudget = 5_000_000L;
        /** Emit detailed tiled-LOD diagnostics to stdout. Disabled by default. */
        public boolean verboseDiagnostics = false;
        public long throttleMs = 75;
        public long debounceMs = 75;
        public double minDepth = 1e-3;
        /** Expand the conservative tile bounding sphere used for frustum tests. */
        public double frustumMargin = 1.10;
        /** Adjacent visible tiles may differ by at most this many LOD levels. */
        public int maxNeighborLodDelta = 1;

        /** Maximum cached/cheap LOD switches applied per JavaFX pulse while moving. */
        public int activeTransitionsPerPulse = 6;
        /** Maximum transitions per pulse after camera interaction settles. */
        public int settledTransitionsPerPulse = 8;
        /** Quiet period used before the first authoritative camera/LOD evaluation. */
        public long initialSettleMs = 150;
        /** Maximum first-time mesh constructions per pulse while moving. Zero disables new mesh builds while active. */
        public int activeBuildsPerPulse = 1;
        /** Maximum first-time mesh constructions per pulse after settling. */
        public int settledBuildsPerPulse = 2;

        public Config copy() {
            Config c = new Config();
            c.targetPixelsPerCell = targetPixelsPerCell;
            c.lowThreshold = lowThreshold;
            c.highThreshold = highThreshold;
            c.finestAllowedLod = finestAllowedLod;
            c.geometryBudgetEnabled = geometryBudgetEnabled;
            c.triangleBudget = triangleBudget;
            c.verboseDiagnostics = verboseDiagnostics;
            c.throttleMs = throttleMs;
            c.debounceMs = debounceMs;
            c.minDepth = minDepth;
            c.frustumMargin = frustumMargin;
            c.maxNeighborLodDelta = maxNeighborLodDelta;
            c.activeTransitionsPerPulse = activeTransitionsPerPulse;
            c.settledTransitionsPerPulse = settledTransitionsPerPulse;
            c.initialSettleMs = initialSettleMs;
            c.activeBuildsPerPulse = activeBuildsPerPulse;
            c.settledBuildsPerPulse = settledBuildsPerPulse;
            return c;
        }
    }

    private final PerspectiveCamera camera;
    private final SubScene subScene;
    private final Node surfaceNode;
    private final PauseTransition debounceTimer;
    private final PauseTransition initialEvaluationTimer;
    private final javafx.beans.value.ChangeListener<Number> resizeListener =
        (obs, oldValue, newValue) -> requestUpdate(LodManager.UpdateReason.RESIZE);

    private final PriorityQueue<LodTransition> pendingTransitions = new PriorityQueue<>();
    private final ArrayList<LodTransition> buildDeferredTransitions = new ArrayList<>();

    private Config config = new Config();
    private List<LodManager.LodLevel> levels = List.of();
    private List<TileSpec> tiles = List.of();
    private TileSpec[] tileById = new TileSpec[0];
    private int stateSize;
    private int gridColumns;
    private int gridRows;
    private int[] tileIdByGrid = new int[0];

    // Reusable per-tile state. -1 means unknown/uninitialized for LOD arrays.
    private int[] qualityLodByTile = new int[0];
    private int[] desiredLodByTile = new int[0];
    private int[] appliedLodByTile = new int[0];
    private int[] selectedLodByTile = new int[0];
    private boolean[] visibleByTile = new boolean[0];
    private boolean[] budgetCoarsenedByTile = new boolean[0];
    private double[] depthByTile = new double[0];
    private double[] pixelsPerCellByTile = new double[0];
    private double[] screenDistanceByTile = new double[0];
    private double[] cameraXByTile = new double[0];
    private double[] cameraYByTile = new double[0];
    private double[] cameraDepthByTile = new double[0];
    private double[] cameraRadiusByTile = new double[0];

    private final AnimationTimer transitionTimer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            processTransitionPulse();
        }
    };

    private double baseWorldWidth = Double.NaN;
    private double baseWorldDepth = Double.NaN;
    private double heightRadius = 0.0;
    private long lastEvalNanos;
    private Consumer<List<TileDecision>> compatibilityDecisionListener;
    private VisibilityApplier visibilityApplier;
    private TransitionApplier transitionApplier;
    private GeometryCostProvider geometryCostProvider;
    private boolean transitionTimerRunning;
    private boolean settledMode;
    private boolean initialEvaluationPending;
    private int lastVisibleCount;
    private int lastDeferredRefinements;
    private long lastRawRequestedTriangles;
    private long lastBudgetedTargetTriangles;
    private int lastBudgetCoarsenedTiles;

    public TiledLodManager(PerspectiveCamera camera, SubScene subScene, Node surfaceNode) {
        this.camera = Objects.requireNonNull(camera, "camera");
        this.subScene = Objects.requireNonNull(subScene, "subScene");
        this.surfaceNode = Objects.requireNonNull(surfaceNode, "surfaceNode");

        debounceTimer = new PauseTransition(Duration.millis(config.debounceMs));
        debounceTimer.setOnFinished(e -> evaluateAndSchedule(false, true));
        initialEvaluationTimer = new PauseTransition(Duration.millis(config.initialSettleMs));
        initialEvaluationTimer.setOnFinished(e -> attemptInitialEvaluation());
        subScene.widthProperty().addListener(resizeListener);
        subScene.heightProperty().addListener(resizeListener);
    }

    public boolean isVerboseDiagnosticsEnabled() {
        return config.verboseDiagnostics;
    }

    private void diagnostic(Object message) {
        if (config.verboseDiagnostics) {
            System.out.println(message);
        }
    }

    public void setConfig(Config config) {
        this.config = Objects.requireNonNull(config, "config").copy();
        validateConfig(this.config);
        runOnFx(() -> {
            debounceTimer.setDuration(Duration.millis(this.config.debounceMs));
            initialEvaluationTimer.setDuration(Duration.millis(this.config.initialSettleMs));
            diagnostic("Tiled Hypersurface LOD config updated: targetPpc="
                + this.config.targetPixelsPerCell
                + ", coarsenBelow=" + this.config.lowThreshold
                + ", refineAbove=" + this.config.highThreshold
                + ", maxDetail=L" + this.config.finestAllowedLod
                + ", geometryBudget=" + this.config.geometryBudgetEnabled
                + ", triangleBudget=" + formatTriangleCount(this.config.triangleBudget)
                + ", throttleMs=" + this.config.throttleMs
                + ", settleMs=" + this.config.debounceMs
                + ", initialSettleMs=" + this.config.initialSettleMs
                + ", activeTransitions=" + this.config.activeTransitionsPerPulse
                + ", settledTransitions=" + this.config.settledTransitionsPerPulse
                + ", activeBuilds=" + this.config.activeBuildsPerPulse
                + ", settledBuilds=" + this.config.settledBuildsPerPulse);
            if (!levels.isEmpty()) {
                if (initialEvaluationPending) {
                    initialEvaluationTimer.stop();
                    initialEvaluationTimer.playFromStart();
                } else {
                    evaluateAndSchedule(true, true);
                }
            }
        });
    }

    public Config getConfigCopy() {
        return config.copy();
    }

    /** Compatibility API; normal tiled rendering uses setVisibilityApplier(). */
    public void setOnDecisions(Consumer<List<TileDecision>> onDecisions) {
        this.compatibilityDecisionListener = onDecisions;
    }

    public void setVisibilityApplier(VisibilityApplier visibilityApplier) {
        this.visibilityApplier = visibilityApplier;
    }

    public void setTransitionApplier(TransitionApplier transitionApplier) {
        this.transitionApplier = transitionApplier;
    }

    public void setGeometryCostProvider(GeometryCostProvider geometryCostProvider) {
        this.geometryCostProvider = geometryCostProvider;
    }

    public long getLastRawRequestedTriangles() {
        return lastRawRequestedTriangles;
    }

    public long getLastBudgetedTargetTriangles() {
        return lastBudgetedTargetTriangles;
    }

    public int getLastBudgetCoarsenedTiles() {
        return lastBudgetCoarsenedTiles;
    }

    public void setSurface(List<LodManager.LodLevel> levels,
                           List<TileSpec> tiles,
                           double baseWorldWidth,
                           double baseWorldDepth,
                           double heightRadius) {
        Objects.requireNonNull(levels, "levels");
        Objects.requireNonNull(tiles, "tiles");
        if (levels.isEmpty()) throw new IllegalArgumentException("levels must not be empty");
        if (!(baseWorldWidth > 0.0) || !(baseWorldDepth > 0.0)) {
            throw new IllegalArgumentException("base world dimensions must be > 0");
        }
        this.levels = List.copyOf(levels);
        this.tiles = List.copyOf(tiles);
        this.baseWorldWidth = baseWorldWidth;
        this.baseWorldDepth = baseWorldDepth;
        this.heightRadius = Math.max(0.0, heightRadius);
        initializeTileState(this.tiles);
        pendingTransitions.clear();
        lastRawRequestedTriangles = 0L;
        lastBudgetedTargetTriangles = 0L;
        lastBudgetCoarsenedTiles = 0;
        stopTransitionTimer();
        lastEvalNanos = 0L;
        initialEvaluationPending = true;
        runOnFx(() -> {
            debounceTimer.stop();
            initialEvaluationTimer.stop();
            initialEvaluationTimer.setDuration(Duration.millis(config.initialSettleMs));
            initialEvaluationTimer.playFromStart();
            diagnostic("Tiled Hypersurface initial LOD deferred: waiting "
                + config.initialSettleMs + " ms for stable viewport/camera state");
        });
    }

    public void clearSurface() {
        runOnFx(() -> {
            levels = List.of();
            tiles = List.of();
            clearTileState();
            pendingTransitions.clear();
            lastRawRequestedTriangles = 0L;
            lastBudgetedTargetTriangles = 0L;
            lastBudgetCoarsenedTiles = 0;
            debounceTimer.stop();
            initialEvaluationTimer.stop();
            initialEvaluationPending = false;
            stopTransitionTimer();
        });
    }

    /** Tells the manager that renderer-side LOD views were discarded. */
    public void invalidateRenderedState() {
        runOnFx(() -> {
            Arrays.fill(appliedLodByTile, -1);
            pendingTransitions.clear();
            stopTransitionTimer();
        });
    }

    public void requestUpdate(LodManager.UpdateReason reason) {
        runOnFx(() -> {
            if (initialEvaluationPending) {
                scheduleInitialEvaluation();
                return;
            }

            debounceTimer.stop();
            debounceTimer.setDuration(Duration.millis(config.debounceMs));
            debounceTimer.playFromStart();

            settledMode = false;
            int cancelledRefinements = cancelQueuedRefinements();
            if (cancelledRefinements > 0) {
                diagnostic("Tiled Hypersurface active camera: paused "
                    + cancelledRefinements + " queued refinements");
            }

            long now = System.nanoTime();
            long throttleNanos = Math.max(0L, config.throttleMs) * 1_000_000L;
            if (throttleNanos == 0L || now - lastEvalNanos >= throttleNanos) {
                evaluateAndSchedule(false, false);
            }
        });
    }

    public void forceUpdate() {
        runOnFx(() -> {
            debounceTimer.stop();
            if (initialEvaluationPending) {
                scheduleInitialEvaluation();
                return;
            }
            evaluateAndSchedule(true, true);
        });
    }

    public int getPendingTransitionCount() {
        return pendingTransitions.size();
    }

    public void dispose() {
        runOnFx(() -> {
            debounceTimer.stop();
            initialEvaluationTimer.stop();
            stopTransitionTimer();
            subScene.widthProperty().removeListener(resizeListener);
            subScene.heightProperty().removeListener(resizeListener);
        });
    }

    private void initializeTileState(List<TileSpec> tileSpecs) {
        int maxId = -1;
        int maxColumn = -1;
        int maxRow = -1;
        for (TileSpec tile : tileSpecs) {
            maxId = Math.max(maxId, tile.id);
            maxColumn = Math.max(maxColumn, tile.column);
            maxRow = Math.max(maxRow, tile.row);
        }

        stateSize = Math.max(0, maxId + 1);
        gridColumns = Math.max(0, maxColumn + 1);
        gridRows = Math.max(0, maxRow + 1);
        tileById = new TileSpec[stateSize];
        tileIdByGrid = new int[Math.max(0, gridColumns * gridRows)];
        Arrays.fill(tileIdByGrid, -1);

        qualityLodByTile = newIntState(stateSize, -1);
        desiredLodByTile = newIntState(stateSize, -1);
        appliedLodByTile = newIntState(stateSize, -1);
        selectedLodByTile = newIntState(stateSize, -1);
        visibleByTile = new boolean[stateSize];
        budgetCoarsenedByTile = new boolean[stateSize];
        depthByTile = new double[stateSize];
        pixelsPerCellByTile = new double[stateSize];
        screenDistanceByTile = new double[stateSize];
        cameraXByTile = new double[stateSize];
        cameraYByTile = new double[stateSize];
        cameraDepthByTile = new double[stateSize];
        cameraRadiusByTile = new double[stateSize];

        for (TileSpec tile : tileSpecs) {
            if (tile.id < 0 || tile.id >= stateSize) continue;
            tileById[tile.id] = tile;
            if (tile.column >= 0 && tile.column < gridColumns
                && tile.row >= 0 && tile.row < gridRows) {
                tileIdByGrid[tile.row * gridColumns + tile.column] = tile.id;
            }
        }
    }

    private void clearTileState() {
        stateSize = 0;
        gridColumns = 0;
        gridRows = 0;
        tileById = new TileSpec[0];
        tileIdByGrid = new int[0];
        qualityLodByTile = new int[0];
        desiredLodByTile = new int[0];
        appliedLodByTile = new int[0];
        selectedLodByTile = new int[0];
        visibleByTile = new boolean[0];
        budgetCoarsenedByTile = new boolean[0];
        depthByTile = new double[0];
        pixelsPerCellByTile = new double[0];
        screenDistanceByTile = new double[0];
        cameraXByTile = new double[0];
        cameraYByTile = new double[0];
        cameraDepthByTile = new double[0];
        cameraRadiusByTile = new double[0];
    }

    private static int[] newIntState(int size, int initialValue) {
        int[] values = new int[size];
        Arrays.fill(values, initialValue);
        return values;
    }

    private void scheduleInitialEvaluation() {
        initialEvaluationTimer.stop();
        initialEvaluationTimer.setDuration(Duration.millis(config.initialSettleMs));
        initialEvaluationTimer.playFromStart();
    }

    private void attemptInitialEvaluation() {
        if (!initialEvaluationPending) return;

        double width = subScene.getWidth();
        double height = subScene.getHeight();
        if (!(width > 1.0) || !(height > 1.0)
            || !Double.isFinite(width) || !Double.isFinite(height)) {
            diagnostic("Tiled Hypersurface initial LOD still deferred: viewport="
                + width + "x" + height);
            scheduleInitialEvaluation();
            return;
        }

        initialEvaluationPending = false;
        diagnostic("Tiled Hypersurface initial LOD released: viewport="
            + String.format("%.1fx%.1f", width, height));
        evaluateAndSchedule(true, true);
    }

    private void evaluateAndSchedule(boolean force, boolean settled) {
        if (levels.isEmpty() || tiles.isEmpty() || stateSize == 0) return;
        if (!Double.isFinite(baseWorldWidth) || !Double.isFinite(baseWorldDepth)) return;

        final double viewportWidth = subScene.getWidth();
        final double viewportHeight = subScene.getHeight();
        if (!(viewportWidth > 1.0) || !(viewportHeight > 1.0)) return;
        final double aspect = viewportWidth / viewportHeight;
        final double fovRadians = Math.toRadians(camera.getFieldOfView());
        final double tanHalfFov = Math.tan(fovRadians * 0.5);
        if (!(tanHalfFov > 0.0)) return;

        Arrays.fill(visibleByTile, false);
        Arrays.fill(selectedLodByTile, -1);
        Arrays.fill(budgetCoarsenedByTile, false);

        int deferredRefinements = 0;
        int visibleCount = 0;

        for (TileSpec tile : tiles) {
            int id = tile.id;
            if (id < 0 || id >= stateSize) continue;

            computeCameraTile(tile, id);
            boolean visible = isVisible(id, aspect, tanHalfFov);
            visibleByTile[id] = visible;
            int previousQuality = qualityLodByTile[id] >= 0
                ? qualityLodByTile[id]
                : appliedLodByTile[id];

            if (!visible) {
                selectedLodByTile[id] = previousQuality >= 0
                    ? previousQuality : levels.size() - 1;
                depthByTile[id] = cameraDepthByTile[id];
                pixelsPerCellByTile[id] = 0.0;
                screenDistanceByTile[id] = screenDistance(id, aspect, tanHalfFov);
                continue;
            }

            visibleCount++;
            double depth = Math.max(config.minDepth,
                cameraDepthByTile[id] - cameraRadiusByTile[id] * 0.35);
            int selected = chooseLod(previousQuality, depth,
                viewportWidth, viewportHeight, aspect, tanHalfFov);
            selectedLodByTile[id] = selected;
            depthByTile[id] = depth;
            pixelsPerCellByTile[id] = pixelsPerCell(levels.get(selected), depth,
                viewportWidth, viewportHeight, aspect, tanHalfFov);
            screenDistanceByTile[id] = screenDistance(id, aspect, tanHalfFov);
        }

        if (config.maxNeighborLodDelta >= 0) {
            enforceNeighborDelta();
            refreshPixelsPerCell(viewportWidth, viewportHeight, aspect, tanHalfFov);
        }

        // Preserve hysteresis quality before aggregate geometry budgeting.
        lastVisibleCount = visibleCount;
        for (TileSpec tile : tiles) {
            int id = tile.id;
            if (isVisibleTile(id)) {
                qualityLodByTile[id] = selectedLodByTile[id];
            }
        }

        applyGeometryBudget(viewportWidth, viewportHeight, aspect, tanHalfFov);

        if (!settled) {
            for (TileSpec tile : tiles) {
                int id = tile.id;
                if (!isVisibleTile(id)) continue;
                int applied = appliedLodByTile[id];
                if (applied >= 0 && selectedLodByTile[id] < applied) {
                    deferredRefinements++;
                    selectedLodByTile[id] = applied;
                    pixelsPerCellByTile[id] = pixelsPerCell(levels.get(applied), depthByTile[id],
                        viewportWidth, viewportHeight, aspect, tanHalfFov);
                }
            }
        }

        int desiredChanges = 0;
        for (TileSpec tile : tiles) {
            int id = tile.id;
            if (!isVisibleTile(id)) continue;
            int previousDesired = desiredLodByTile[id] >= 0
                ? desiredLodByTile[id] : appliedLodByTile[id];
            if (selectedLodByTile[id] != previousDesired) desiredChanges++;
            desiredLodByTile[id] = selectedLodByTile[id];
        }

        lastDeferredRefinements = deferredRefinements;
        settledMode = settled;

        if (visibilityApplier != null) {
            visibilityApplier.apply(visibleByTile, stateSize);
        }
        publishCompatibilityDecisions();
        rebuildTransitionQueue();
        lastEvalNanos = System.nanoTime();

        if (config.verboseDiagnostics
            && (desiredChanges > 0 || !pendingTransitions.isEmpty() || force || deferredRefinements > 0)) {
            int[] desiredCounts = countDesiredLods();
            int queuedCoarsen = 0;
            int queuedRefine = 0;
            int queuedInitial = 0;
            for (LodTransition transition : pendingTransitions) {
                if (transition.isInitial()) queuedInitial++;
                else if (transition.isCoarsening()) queuedCoarsen++;
                else if (transition.isRefining()) queuedRefine++;
            }
            StringBuilder sb = new StringBuilder("Tiled Hypersurface target: visible=")
                .append(visibleCount).append('/').append(tiles.size())
                .append(", desiredChanges=").append(desiredChanges)
                .append(", queued=").append(pendingTransitions.size())
                .append(", coarsenQueued=").append(queuedCoarsen)
                .append(", refineQueued=").append(queuedRefine)
                .append(", initialQueued=").append(queuedInitial)
                .append(", refineDeferred=").append(deferredRefinements)
                .append(", mode=").append(settled ? "settled" : "active")
                .append(", transitionBudget=").append(currentTransitionBudget())
                .append(", buildBudget=").append(currentBuildBudget())
                .append(", rawTriangles=").append(formatTriangleCount(lastRawRequestedTriangles))
                .append(", budgetTriangles=").append(formatTriangleCount(lastBudgetedTargetTriangles))
                .append(", budgetCoarsened=").append(lastBudgetCoarsenedTiles)
                .append(", geometryBudget=")
                .append(config.geometryBudgetEnabled ? formatTriangleCount(config.triangleBudget) : "OFF")
                .append(", ");
            appendLodCounts(sb, desiredCounts);
            diagnostic(sb);
            if (settled && queuedRefine > 0) {
                diagnostic("Tiled Hypersurface settled: refinementReleased="
                    + queuedRefine + ", queuedTotal=" + pendingTransitions.size());
            }
        }

        if (!pendingTransitions.isEmpty()) startTransitionTimer();
        else stopTransitionTimer();
    }

    private void applyGeometryBudget(double viewportWidth, double viewportHeight,
                                     double aspect, double tanHalfFov) {
        long rawTriangles = totalSelectedTriangles();
        lastRawRequestedTriangles = rawTriangles;
        lastBudgetedTargetTriangles = rawTriangles;
        lastBudgetCoarsenedTiles = 0;

        if (!config.geometryBudgetEnabled
            || config.triangleBudget <= 0L
            || geometryCostProvider == null
            || rawTriangles <= config.triangleBudget) {
            return;
        }

        long triangles = rawTriangles;
        int guard = Math.max(1, lastVisibleCount * Math.max(1, levels.size()) * 2);

        while (triangles > config.triangleBudget && guard-- > 0) {
            int bestTileId = -1;
            int bestToLod = -1;
            long bestSavings = 0L;

            for (TileSpec tile : tiles) {
                int id = tile.id;
                if (!isVisibleTile(id)) continue;
                int current = selectedLodByTile[id];
                if (current < 0 || current >= levels.size() - 1) continue;

                int proposed = current + 1;
                if (!budgetNeighborCompatible(tile, proposed)) continue;

                long savings = triangleCost(id, current) - triangleCost(id, proposed);
                if (savings <= 0L) continue;

                if (bestTileId < 0 || lowerBudgetPriority(id, savings, bestTileId, bestSavings)) {
                    bestTileId = id;
                    bestToLod = proposed;
                    bestSavings = savings;
                }
            }

            if (bestTileId < 0) break;
            selectedLodByTile[bestTileId] = bestToLod;
            if (!budgetCoarsenedByTile[bestTileId]) {
                budgetCoarsenedByTile[bestTileId] = true;
                lastBudgetCoarsenedTiles++;
            }
            triangles -= bestSavings;
        }

        refreshPixelsPerCell(viewportWidth, viewportHeight, aspect, tanHalfFov);
        long exactBudgetedTriangles = totalSelectedTriangles();
        lastBudgetedTargetTriangles = exactBudgetedTriangles;
        if (exactBudgetedTriangles > config.triangleBudget) {
            diagnostic("Tiled Hypersurface geometry budget warning: requested="
                + formatTriangleCount(rawTriangles)
                + ", budget=" + formatTriangleCount(config.triangleBudget)
                + ", achievable=" + formatTriangleCount(exactBudgetedTriangles)
                + ", neighborDelta=" + config.maxNeighborLodDelta);
        }
    }

    private boolean lowerBudgetPriority(int candidateId, long candidateSavings,
                                        int currentBestId, long currentBestSavings) {
        int screenCmp = Double.compare(screenDistanceByTile[candidateId],
            screenDistanceByTile[currentBestId]);
        if (screenCmp != 0) return screenCmp > 0;
        int depthCmp = Double.compare(depthByTile[candidateId], depthByTile[currentBestId]);
        if (depthCmp != 0) return depthCmp > 0;
        return candidateSavings > currentBestSavings;
    }

    private long totalSelectedTriangles() {
        if (geometryCostProvider == null) return 0L;
        long total = 0L;
        for (TileSpec tile : tiles) {
            int id = tile.id;
            if (!isVisibleTile(id)) continue;
            int lod = selectedLodByTile[id];
            if (lod >= 0) total += triangleCost(id, lod);
        }
        return total;
    }

    private long triangleCost(int tileId, int lod) {
        if (geometryCostProvider == null) return 0L;
        return Math.max(0L, geometryCostProvider.trianglesFor(tileId, lod));
    }

    private boolean budgetNeighborCompatible(TileSpec tile, int proposedLod) {
        if (config.maxNeighborLodDelta < 0) return true;
        return budgetNeighborCompatible(proposedLod, neighborLod(tile.column - 1, tile.row))
            && budgetNeighborCompatible(proposedLod, neighborLod(tile.column + 1, tile.row))
            && budgetNeighborCompatible(proposedLod, neighborLod(tile.column, tile.row - 1))
            && budgetNeighborCompatible(proposedLod, neighborLod(tile.column, tile.row + 1));
    }

    private boolean budgetNeighborCompatible(int proposedLod, int neighborLod) {
        return neighborLod < 0
            || proposedLod <= neighborLod + config.maxNeighborLodDelta;
    }

    private int cancelQueuedRefinements() {
        int before = pendingTransitions.size();
        pendingTransitions.removeIf(LodTransition::isRefining);
        int removed = before - pendingTransitions.size();
        if (pendingTransitions.isEmpty()) stopTransitionTimer();
        return removed;
    }

    private void rebuildTransitionQueue() {
        pendingTransitions.clear();
        for (TileSpec tile : tiles) {
            int id = tile.id;
            if (!isVisibleTile(id)) continue;
            int from = appliedLodByTile[id];
            int to = desiredLodByTile[id];
            if (to < 0 || from == to) continue;

            int priorityClass;
            if (from >= 0 && to > from) priorityClass = 0;
            else if (from < 0) priorityClass = 1;
            else priorityClass = 2;

            pendingTransitions.add(new LodTransition(
                id, from, to, priorityClass,
                screenDistanceByTile[id], depthByTile[id], pixelsPerCellByTile[id]));
        }
    }

    private void processTransitionPulse() {
        if (pendingTransitions.isEmpty() || transitionApplier == null) {
            stopTransitionTimer();
            return;
        }

        int transitionBudget = currentTransitionBudget();
        int buildBudget = currentBuildBudget();
        int applied = 0;
        int built = 0;
        int cacheHits = 0;
        int coarsened = 0;
        int refined = 0;
        int initial = 0;

        int transitionsAvailableAtStart = pendingTransitions.size();
        buildDeferredTransitions.clear();
        int examined = 0;

        while (applied < transitionBudget
            && !pendingTransitions.isEmpty()
            && examined < transitionsAvailableAtStart) {

            LodTransition transition = pendingTransitions.poll();
            examined++;
            int id = transition.tileId();
            if (id < 0 || id >= stateSize) continue;
            int desired = desiredLodByTile[id];
            if (desired < 0 || desired != transition.toLod()) continue;

            int actualFrom = appliedLodByTile[id];
            if (actualFrom == transition.toLod()) continue;

            LodTransition actualTransition = actualFrom == transition.fromLod()
                ? transition
                : new LodTransition(id, actualFrom, transition.toLod(),
                    transition.priorityClass(), transition.screenDistance(),
                    transition.depth(), transition.pixelsPerCell());

            boolean allowBuild = built < buildBudget;
            TransitionResult result = transitionApplier.apply(actualTransition, allowBuild);
            if (!result.applied()) {
                if (!allowBuild) buildDeferredTransitions.add(actualTransition);
                continue;
            }

            appliedLodByTile[id] = actualTransition.toLod();
            applied++;
            if (result.built()) built++;
            if (result.cacheHit()) cacheHits++;
            if (actualTransition.isInitial()) initial++;
            else if (actualTransition.isCoarsening()) coarsened++;
            else if (actualTransition.isRefining()) refined++;

            if (buildBudget > 0 && built >= buildBudget) break;
        }

        pendingTransitions.addAll(buildDeferredTransitions);

        if (config.verboseDiagnostics && applied > 0) {
            int[] actualCounts = countAppliedVisibleLods();
            StringBuilder sb = new StringBuilder("Tiled Hypersurface transition pulse: visible=")
                .append(lastVisibleCount).append('/').append(tiles.size())
                .append(", applied=").append(applied)
                .append(", built=").append(built)
                .append(", cacheHits=").append(cacheHits)
                .append(", coarsened=").append(coarsened)
                .append(", refined=").append(refined)
                .append(", initial=").append(initial)
                .append(", remaining=").append(pendingTransitions.size())
                .append(", refineDeferred=").append(lastDeferredRefinements)
                .append(", mode=").append(settledMode ? "settled" : "active")
                .append(", actual=");
            appendLodCounts(sb, actualCounts);
            diagnostic(sb);
        }

        if (pendingTransitions.isEmpty()
            || (!settledMode && buildBudget == 0 && applied == 0)) {
            stopTransitionTimer();
        }
    }

    private int currentTransitionBudget() {
        return Math.max(1, settledMode
            ? config.settledTransitionsPerPulse
            : config.activeTransitionsPerPulse);
    }

    private int currentBuildBudget() {
        return Math.max(0, settledMode
            ? config.settledBuildsPerPulse
            : config.activeBuildsPerPulse);
    }

    private int chooseLod(int current, double depth,
                          double viewportWidth, double viewportHeight,
                          double aspect, double tanHalfFov) {
        int finestAllowed = Math.max(0, Math.min(config.finestAllowedLod, levels.size() - 1));
        if (current < finestAllowed || current >= levels.size()) {
            int best = levels.size() - 1;
            for (int i = finestAllowed; i < levels.size(); i++) {
                if (pixelsPerCell(levels.get(i), depth,
                    viewportWidth, viewportHeight, aspect, tanHalfFov)
                    >= config.targetPixelsPerCell) {
                    best = i;
                    break;
                }
            }
            return best;
        }

        int selected = current;
        while (selected < levels.size() - 1) {
            double ppc = pixelsPerCell(levels.get(selected), depth,
                viewportWidth, viewportHeight, aspect, tanHalfFov);
            if (ppc >= config.lowThreshold) break;
            selected++;
        }
        while (selected > finestAllowed) {
            double ppc = pixelsPerCell(levels.get(selected), depth,
                viewportWidth, viewportHeight, aspect, tanHalfFov);
            if (ppc <= config.highThreshold) break;
            selected--;
        }
        return Math.max(finestAllowed, selected);
    }

    private double pixelsPerCell(LodManager.LodLevel level, double depth,
                                 double viewportWidth, double viewportHeight,
                                 double aspect, double tanHalfFov) {
        double pixelsPerWorldUnit;
        if (camera.isVerticalFieldOfView()) {
            double worldHeight = 2.0 * depth * tanHalfFov;
            pixelsPerWorldUnit = viewportHeight / worldHeight;
        } else {
            double worldWidth = 2.0 * depth * tanHalfFov;
            pixelsPerWorldUnit = viewportWidth / worldWidth;
        }
        double cellX = baseWorldWidth / level.width;
        double cellZ = baseWorldDepth / level.height;
        return Math.min(cellX, cellZ) * pixelsPerWorldUnit;
    }

    private void computeCameraTile(TileSpec tile, int id) {
        Point3D scenePoint = surfaceNode.localToScene(tile.centerX(), 0.0, tile.centerZ());
        Point3D cameraPoint = camera.sceneToLocal(scenePoint);
        cameraXByTile[id] = cameraPoint.getX();
        cameraYByTile[id] = cameraPoint.getY();
        cameraDepthByTile[id] = cameraPoint.getZ();
        cameraRadiusByTile[id] = Math.sqrt(tile.halfWidth() * tile.halfWidth()
            + tile.halfDepth() * tile.halfDepth()
            + heightRadius * heightRadius) * Math.max(1.0, config.frustumMargin);
    }

    private boolean isVisible(int id, double aspect, double tanHalfFov) {
        double z = cameraDepthByTile[id];
        double radius = cameraRadiusByTile[id];
        if (z + radius < camera.getNearClip()) return false;
        if (z - radius > camera.getFarClip()) return false;
        if (z + radius <= 0.0) return false;

        double depthForExtent = Math.max(config.minDepth, z + radius);
        double halfWidth;
        double halfHeight;
        if (camera.isVerticalFieldOfView()) {
            halfHeight = depthForExtent * tanHalfFov;
            halfWidth = halfHeight * aspect;
        } else {
            halfWidth = depthForExtent * tanHalfFov;
            halfHeight = halfWidth / Math.max(1.0e-9, aspect);
        }
        return Math.abs(cameraXByTile[id]) <= halfWidth + radius
            && Math.abs(cameraYByTile[id]) <= halfHeight + radius;
    }

    private double screenDistance(int id, double aspect, double tanHalfFov) {
        double depth = Math.max(config.minDepth, cameraDepthByTile[id]);
        double halfWidth;
        double halfHeight;
        if (camera.isVerticalFieldOfView()) {
            halfHeight = depth * tanHalfFov;
            halfWidth = halfHeight * aspect;
        } else {
            halfWidth = depth * tanHalfFov;
            halfHeight = halfWidth / Math.max(1.0e-9, aspect);
        }
        double nx = cameraXByTile[id] / Math.max(config.minDepth, halfWidth);
        double ny = cameraYByTile[id] / Math.max(config.minDepth, halfHeight);
        return Math.hypot(nx, ny);
    }

    private void enforceNeighborDelta() {
        boolean changed;
        int guard = Math.max(1, levels.size() * 2);
        do {
            changed = false;
            for (TileSpec tile : tiles) {
                int id = tile.id;
                if (!isVisibleTile(id)) continue;
                int lod = selectedLodByTile[id];
                int refined = lod;
                refined = refineAgainstNeighbor(refined, neighborLod(tile.column - 1, tile.row));
                refined = refineAgainstNeighbor(refined, neighborLod(tile.column + 1, tile.row));
                refined = refineAgainstNeighbor(refined, neighborLod(tile.column, tile.row - 1));
                refined = refineAgainstNeighbor(refined, neighborLod(tile.column, tile.row + 1));
                if (refined != lod) {
                    selectedLodByTile[id] = refined;
                    changed = true;
                }
            }
        } while (changed && --guard > 0);
    }

    private int refineAgainstNeighbor(int lod, int neighborLod) {
        if (neighborLod < 0) return lod;
        int maxAllowed = neighborLod + config.maxNeighborLodDelta;
        return lod > maxAllowed ? maxAllowed : lod;
    }

    private int neighborLod(int column, int row) {
        int neighborId = tileIdAt(column, row);
        return isVisibleTile(neighborId) ? selectedLodByTile[neighborId] : -1;
    }

    private int tileIdAt(int column, int row) {
        if (column < 0 || column >= gridColumns || row < 0 || row >= gridRows) return -1;
        return tileIdByGrid[row * gridColumns + column];
    }

    private boolean isVisibleTile(int id) {
        return id >= 0 && id < stateSize && visibleByTile[id];
    }

    private void refreshPixelsPerCell(double viewportWidth, double viewportHeight,
                                      double aspect, double tanHalfFov) {
        for (TileSpec tile : tiles) {
            int id = tile.id;
            if (!isVisibleTile(id)) continue;
            int lod = selectedLodByTile[id];
            if (lod >= 0 && lod < levels.size()) {
                pixelsPerCellByTile[id] = pixelsPerCell(levels.get(lod), depthByTile[id],
                    viewportWidth, viewportHeight, aspect, tanHalfFov);
            }
        }
    }

    private int[] countAppliedVisibleLods() {
        int[] counts = new int[levels.size()];
        for (TileSpec tile : tiles) {
            int id = tile.id;
            if (!isVisibleTile(id)) continue;
            int lod = appliedLodByTile[id];
            if (lod >= 0 && lod < counts.length) counts[lod]++;
        }
        return counts;
    }

    private int[] countDesiredLods() {
        int[] counts = new int[levels.size()];
        for (TileSpec tile : tiles) {
            int id = tile.id;
            if (!isVisibleTile(id)) continue;
            int lod = selectedLodByTile[id];
            if (lod >= 0 && lod < counts.length) counts[lod]++;
        }
        return counts;
    }

    private void publishCompatibilityDecisions() {
        if (compatibilityDecisionListener == null) return;
        ArrayList<TileDecision> decisions = new ArrayList<>(tiles.size());
        for (TileSpec tile : tiles) {
            int id = tile.id;
            boolean visible = isVisibleTile(id);
            int lod = id >= 0 && id < stateSize && selectedLodByTile[id] >= 0
                ? selectedLodByTile[id] : levels.size() - 1;
            decisions.add(new TileDecision(id, visible, lod,
                id >= 0 && id < stateSize ? depthByTile[id] : 0.0,
                id >= 0 && id < stateSize ? pixelsPerCellByTile[id] : 0.0,
                id >= 0 && id < stateSize ? screenDistanceByTile[id] : 0.0));
        }
        compatibilityDecisionListener.accept(List.copyOf(decisions));
    }

    private static void appendLodCounts(StringBuilder sb, int[] counts) {
        for (int i = 0; i < counts.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append('L').append(i).append('=').append(counts[i]);
        }
    }

    private void startTransitionTimer() {
        if (transitionTimerRunning || transitionApplier == null) return;
        transitionTimerRunning = true;
        transitionTimer.start();
    }

    private void stopTransitionTimer() {
        if (!transitionTimerRunning) return;
        transitionTimer.stop();
        transitionTimerRunning = false;
    }

    private static void validateConfig(Config config) {
        if (!(config.lowThreshold > 0.0)
            || !(config.targetPixelsPerCell > 0.0)
            || !(config.highThreshold > 0.0)
            || config.lowThreshold >= config.highThreshold) {
            throw new IllegalArgumentException(
                "LOD thresholds must be > 0 and lowThreshold < highThreshold");
        }
        if (config.finestAllowedLod < 0) {
            throw new IllegalArgumentException("finestAllowedLod must be >= 0");
        }
        if (config.triangleBudget < 1L) {
            throw new IllegalArgumentException("triangleBudget must be >= 1");
        }
        if (config.throttleMs < 0L || config.debounceMs < 0L || config.initialSettleMs < 0L) {
            throw new IllegalArgumentException("LOD timing values must be >= 0");
        }
        if (config.activeTransitionsPerPulse < 1
            || config.settledTransitionsPerPulse < 1
            || config.activeBuildsPerPulse < 0
            || config.settledBuildsPerPulse < 1) {
            throw new IllegalArgumentException(
                "Transition budgets and settled build budget must be >= 1; active build budget must be >= 0");
        }
    }

    private static void runOnFx(Runnable runnable) {
        if (Platform.isFxApplicationThread()) runnable.run();
        else Platform.runLater(runnable);
    }

    private static String formatTriangleCount(long triangles) {
        if (triangles >= 1_000_000L) {
            return String.format("%.2fM", triangles / 1_000_000.0);
        }
        if (triangles >= 1_000L) {
            return String.format("%.1fK", triangles / 1_000.0);
        }
        return Long.toString(triangles);
    }
}
