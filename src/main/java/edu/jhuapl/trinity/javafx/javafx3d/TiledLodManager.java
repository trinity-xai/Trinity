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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Camera/frustum-aware per-tile LOD selector and transition scheduler.
 *
 * <p>The manager owns desired LOD state, hysteresis, frustum decisions, the
 * prioritized transition queue, and per-pulse transition budgets. It does not
 * construct JavaFX meshes; transitions are delegated to the renderer through
 * {@link TransitionApplier}.</p>
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
        TransitionResult apply(LodTransition transition);
    }

    public static final class Config {
        public double targetPixelsPerCell = 1.5;
        public double lowThreshold = 1.0;
        public double highThreshold = 2.0;
        /** Finest LOD index allowed to be selected (0 = L0, 1 = L1, ...). */
        public int finestAllowedLod = 0;
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
        /** Maximum first-time mesh constructions per pulse while moving. */
        public int activeBuildsPerPulse = 1;
        /** Maximum first-time mesh constructions per pulse after settling. */
        public int settledBuildsPerPulse = 2;

        public Config copy() {
            Config c = new Config();
            c.targetPixelsPerCell = targetPixelsPerCell;
            c.lowThreshold = lowThreshold;
            c.highThreshold = highThreshold;
            c.finestAllowedLod = finestAllowedLod;
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
    /** Stable hysteresis state: where each tile wants to be. */
    private final Map<Integer, Integer> desiredLodByTile = new HashMap<>();
    /** Renderer state: where each tile has actually transitioned to. */
    private final Map<Integer, Integer> appliedLodByTile = new HashMap<>();
    private final Set<Integer> visibleTileIds = new HashSet<>();

    private final AnimationTimer transitionTimer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            processTransitionPulse();
        }
    };

    private Config config = new Config();
    private List<LodManager.LodLevel> levels = List.of();
    private List<TileSpec> tiles = List.of();
    private double baseWorldWidth = Double.NaN;
    private double baseWorldDepth = Double.NaN;
    private double heightRadius = 0.0;
    private long lastEvalNanos;
    private Consumer<List<TileDecision>> onDecisions;
    private TransitionApplier transitionApplier;
    private boolean transitionTimerRunning;
    private boolean settledMode;
    private boolean initialEvaluationPending;
    private int lastVisibleCount;
    private int lastDeferredRefinements;

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

    public void setConfig(Config config) {
        this.config = Objects.requireNonNull(config, "config").copy();
        validateConfig(this.config);
        runOnFx(() -> {
            debounceTimer.setDuration(Duration.millis(this.config.debounceMs));
            initialEvaluationTimer.setDuration(Duration.millis(this.config.initialSettleMs));
            System.out.println("Tiled Hypersurface LOD config updated: targetPpc="
                + this.config.targetPixelsPerCell
                + ", coarsenBelow=" + this.config.lowThreshold
                + ", refineAbove=" + this.config.highThreshold
                + ", maxDetail=L" + this.config.finestAllowedLod
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

    public void setOnDecisions(Consumer<List<TileDecision>> onDecisions) {
        this.onDecisions = onDecisions;
    }

    public void setTransitionApplier(TransitionApplier transitionApplier) {
        this.transitionApplier = transitionApplier;
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
        desiredLodByTile.clear();
        appliedLodByTile.clear();
        visibleTileIds.clear();
        pendingTransitions.clear();
        stopTransitionTimer();
        lastEvalNanos = 0L;
        initialEvaluationPending = true;
        runOnFx(() -> {
            debounceTimer.stop();
            initialEvaluationTimer.stop();
            initialEvaluationTimer.setDuration(Duration.millis(config.initialSettleMs));
            initialEvaluationTimer.playFromStart();
            System.out.println("Tiled Hypersurface initial LOD deferred: waiting "
                + config.initialSettleMs + " ms for stable viewport/camera state");
        });
    }

    public void clearSurface() {
        runOnFx(() -> {
            levels = List.of();
            tiles = List.of();
            desiredLodByTile.clear();
            appliedLodByTile.clear();
            visibleTileIds.clear();
            pendingTransitions.clear();
            debounceTimer.stop();
            initialEvaluationTimer.stop();
            initialEvaluationPending = false;
            stopTransitionTimer();
        });
    }

    /**
     * Tells the manager that all renderer-side LOD views were discarded while
     * preserving the current surface/pyramid configuration.
     */
    public void invalidateRenderedState() {
        runOnFx(() -> {
            appliedLodByTile.clear();
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

            // Camera interaction takes priority over refinement. Stop any
            // refinement work released by a previous settled evaluation
            // immediately, even if this camera event is inside the throttle window.
            settledMode = false;
            int cancelledRefinements = cancelQueuedRefinements();
            if (cancelledRefinements > 0) {
                System.out.println("Tiled Hypersurface active camera: paused "
                    + cancelledRefinements + " queued refinements");
            }

            long now = System.nanoTime();
            long throttleNanos = Math.max(0L, config.throttleMs) * 1_000_000L;
            if (throttleNanos == 0L || now - lastEvalNanos >= throttleNanos) {
                evaluateAndSchedule(false, false);
            }
        });
    }

    /**
     * Force a settled evaluation. During initial surface setup this is deliberately
     * deferred until the viewport/camera has remained quiet for the configured
     * initial-settle interval.
     */
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
            System.out.println("Tiled Hypersurface initial LOD still deferred: viewport="
                + width + "x" + height);
            scheduleInitialEvaluation();
            return;
        }

        initialEvaluationPending = false;
        System.out.println("Tiled Hypersurface initial LOD released: viewport="
            + String.format("%.1fx%.1f", width, height));
        evaluateAndSchedule(true, true);
    }

    private void evaluateAndSchedule(boolean force, boolean settled) {
        if (levels.isEmpty() || tiles.isEmpty()) return;
        if (!Double.isFinite(baseWorldWidth) || !Double.isFinite(baseWorldDepth)) return;

        final double viewportWidth = subScene.getWidth();
        final double viewportHeight = subScene.getHeight();
        if (!(viewportWidth > 1.0) || !(viewportHeight > 1.0)) return;
        final double aspect = viewportWidth / viewportHeight;
        final double fovRadians = Math.toRadians(camera.getFieldOfView());
        final double tanHalfFov = Math.tan(fovRadians * 0.5);
        if (!(tanHalfFov > 0.0)) return;

        ArrayList<TileDecision> decisions = new ArrayList<>(tiles.size());
        Map<Long, Integer> visibleLods = new HashMap<>();
        Map<Integer, Integer> selectedLods = new HashMap<>();
        int desiredChanges = 0;
        int deferredRefinements = 0;
        int visibleCount = 0;
        visibleTileIds.clear();

        for (TileSpec tile : tiles) {
            CameraTile cameraTile = cameraTile(tile);
            boolean visible = isVisible(cameraTile, aspect, tanHalfFov);
            int previousDesired = desiredLodByTile.getOrDefault(tile.id,
                appliedLodByTile.getOrDefault(tile.id, -1));

            if (!visible) {
                decisions.add(new TileDecision(tile.id, false,
                    previousDesired >= 0 ? previousDesired : levels.size() - 1,
                    cameraTile.depth, 0.0,
                    screenDistance(cameraTile, aspect, tanHalfFov)));
                continue;
            }

            visibleCount++;
            visibleTileIds.add(tile.id);
            double depth = Math.max(config.minDepth,
                cameraTile.depth - cameraTile.radius * 0.35);
            int selected = chooseLod(previousDesired, depth,
                viewportWidth, viewportHeight, aspect, tanHalfFov);

            if (!settled) {
                int applied = appliedLodByTile.getOrDefault(tile.id, -1);
                if (applied >= 0 && selected < applied) {
                    // Refinement is deferred while the camera is active. Keep the
                    // currently rendered detail and release refinement on settle.
                    deferredRefinements++;
                    selected = applied;
                }
            }

            if (selected != previousDesired) desiredChanges++;
            selectedLods.put(tile.id, selected);
            visibleLods.put(gridKey(tile.column, tile.row), selected);
            decisions.add(new TileDecision(tile.id, true, selected, depth,
                pixelsPerCell(levels.get(selected), depth,
                    viewportWidth, viewportHeight, aspect, tanHalfFov),
                screenDistance(cameraTile, aspect, tanHalfFov)));
        }

        if (config.maxNeighborLodDelta >= 0) {
            enforceNeighborDelta(selectedLods, visibleLods);
            for (int i = 0; i < decisions.size(); i++) {
                TileDecision d = decisions.get(i);
                if (!d.visible()) continue;
                int lod = selectedLods.getOrDefault(d.tileId(), d.lodIndex());
                if (lod != d.lodIndex()) {
                    decisions.set(i, new TileDecision(d.tileId(), true, lod, d.depth(),
                        pixelsPerCell(levels.get(lod), d.depth(),
                            viewportWidth, viewportHeight, aspect, tanHalfFov),
                        d.screenDistance()));
                }
            }
        }

        if (!settled) {
            for (int i = 0; i < decisions.size(); i++) {
                TileDecision d = decisions.get(i);
                if (!d.visible()) continue;
                int applied = appliedLodByTile.getOrDefault(d.tileId(), -1);
                if (applied >= 0 && d.lodIndex() < applied) {
                    if (d.lodIndex() != selectedLods.getOrDefault(d.tileId(), d.lodIndex())) {
                        deferredRefinements++;
                    }
                    decisions.set(i, new TileDecision(d.tileId(), true, applied, d.depth(),
                        pixelsPerCell(levels.get(applied), d.depth(),
                            viewportWidth, viewportHeight, aspect, tanHalfFov),
                        d.screenDistance()));
                    selectedLods.put(d.tileId(), applied);
                }
            }
        }

        desiredChanges = 0;
        for (TileDecision decision : decisions) {
            if (!decision.visible()) continue;
            int previousDesired = desiredLodByTile.getOrDefault(decision.tileId(),
                appliedLodByTile.getOrDefault(decision.tileId(), -1));
            if (decision.lodIndex() != previousDesired) desiredChanges++;
            desiredLodByTile.put(decision.tileId(), decision.lodIndex());
        }

        lastVisibleCount = visibleCount;
        lastDeferredRefinements = deferredRefinements;
        settledMode = settled;
        if (onDecisions != null) onDecisions.accept(List.copyOf(decisions));
        rebuildTransitionQueue(decisions);
        lastEvalNanos = System.nanoTime();

        if (desiredChanges > 0 || !pendingTransitions.isEmpty() || force || deferredRefinements > 0) {
            int[] desiredCounts = countDesiredLods(decisions);
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
                .append(", ");
            appendLodCounts(sb, desiredCounts);
            System.out.println(sb);
            if (settled && queuedRefine > 0) {
                System.out.println("Tiled Hypersurface settled: refinementReleased="
                    + queuedRefine + ", queuedTotal=" + pendingTransitions.size());
            }
        }

        if (!pendingTransitions.isEmpty()) startTransitionTimer();
        else stopTransitionTimer();
    }

    private int cancelQueuedRefinements() {
        int before = pendingTransitions.size();
        pendingTransitions.removeIf(LodTransition::isRefining);
        int removed = before - pendingTransitions.size();
        if (pendingTransitions.isEmpty()) stopTransitionTimer();
        return removed;
    }

    private void rebuildTransitionQueue(List<TileDecision> decisions) {
        pendingTransitions.clear();
        for (TileDecision decision : decisions) {
            if (!decision.visible()) continue;
            int from = appliedLodByTile.getOrDefault(decision.tileId(), -1);
            int to = decision.lodIndex();
            if (from == to) continue;

            int priorityClass;
            if (from >= 0 && to > from) {
                // Coarsening reduces render cost, so it is highest priority.
                priorityClass = 0;
            } else if (from < 0) {
                // Newly visible/uninitialized tile.
                priorityClass = 1;
            } else {
                // Refinement can trail slightly behind camera motion.
                priorityClass = 2;
            }
            pendingTransitions.add(new LodTransition(
                decision.tileId(), from, to, priorityClass,
                decision.screenDistance(), decision.depth(), decision.pixelsPerCell()));
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

        while (applied < transitionBudget && !pendingTransitions.isEmpty()) {
            LodTransition transition = pendingTransitions.poll();
            Integer desired = desiredLodByTile.get(transition.tileId());
            if (desired == null || desired != transition.toLod()) {
                continue; // stale request superseded by a newer camera evaluation
            }

            int actualFrom = appliedLodByTile.getOrDefault(transition.tileId(), -1);
            if (actualFrom == transition.toLod()) continue;

            LodTransition actualTransition = actualFrom == transition.fromLod()
                ? transition
                : new LodTransition(transition.tileId(), actualFrom, transition.toLod(),
                    transition.priorityClass(), transition.screenDistance(),
                    transition.depth(), transition.pixelsPerCell());

            TransitionResult result = transitionApplier.apply(actualTransition);
            if (!result.applied()) continue;

            appliedLodByTile.put(actualTransition.tileId(), actualTransition.toLod());
            applied++;
            if (result.built()) built++;
            if (result.cacheHit()) cacheHits++;
            if (actualTransition.isInitial()) initial++;
            else if (actualTransition.isCoarsening()) coarsened++;
            else if (actualTransition.isRefining()) refined++;

            // Building TriangleMesh buffers is substantially more expensive than
            // toggling cached MeshViews. Keep first-use construction tightly bounded.
            if (built >= buildBudget) break;
        }

        if (applied > 0) {
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
            System.out.println(sb);
        }

        if (pendingTransitions.isEmpty()) stopTransitionTimer();
    }

    private int currentTransitionBudget() {
        return Math.max(1, settledMode
            ? config.settledTransitionsPerPulse
            : config.activeTransitionsPerPulse);
    }

    private int currentBuildBudget() {
        return Math.max(1, settledMode
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

    private CameraTile cameraTile(TileSpec tile) {
        Point3D scenePoint = surfaceNode.localToScene(tile.centerX(), 0.0, tile.centerZ());
        Point3D cameraPoint = camera.sceneToLocal(scenePoint);
        double radius = Math.sqrt(tile.halfWidth() * tile.halfWidth()
            + tile.halfDepth() * tile.halfDepth()
            + heightRadius * heightRadius) * Math.max(1.0, config.frustumMargin);
        return new CameraTile(cameraPoint.getX(), cameraPoint.getY(), cameraPoint.getZ(), radius);
    }

    private boolean isVisible(CameraTile ct, double aspect, double tanHalfFov) {
        double z = ct.depth;
        double radius = ct.radius;
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
        return Math.abs(ct.x) <= halfWidth + radius
            && Math.abs(ct.y) <= halfHeight + radius;
    }

    private double screenDistance(CameraTile ct, double aspect, double tanHalfFov) {
        double depth = Math.max(config.minDepth, ct.depth);
        double halfWidth;
        double halfHeight;
        if (camera.isVerticalFieldOfView()) {
            halfHeight = depth * tanHalfFov;
            halfWidth = halfHeight * aspect;
        } else {
            halfWidth = depth * tanHalfFov;
            halfHeight = halfWidth / Math.max(1.0e-9, aspect);
        }
        double nx = ct.x / Math.max(config.minDepth, halfWidth);
        double ny = ct.y / Math.max(config.minDepth, halfHeight);
        return Math.hypot(nx, ny);
    }

    private void enforceNeighborDelta(Map<Integer, Integer> selectedLods,
                                      Map<Long, Integer> visibleLods) {
        if (visibleLods.isEmpty()) return;

        boolean changed;
        int guard = Math.max(1, levels.size() * 2);
        do {
            changed = false;
            for (TileSpec tile : tiles) {
                Integer lodObject = selectedLods.get(tile.id);
                if (lodObject == null) continue;
                int lod = lodObject;
                int refined = lod;
                refined = refineAgainstNeighbor(refined,
                    visibleLods.get(gridKey(tile.column - 1, tile.row)));
                refined = refineAgainstNeighbor(refined,
                    visibleLods.get(gridKey(tile.column + 1, tile.row)));
                refined = refineAgainstNeighbor(refined,
                    visibleLods.get(gridKey(tile.column, tile.row - 1)));
                refined = refineAgainstNeighbor(refined,
                    visibleLods.get(gridKey(tile.column, tile.row + 1)));
                if (refined != lod) {
                    selectedLods.put(tile.id, refined);
                    visibleLods.put(gridKey(tile.column, tile.row), refined);
                    changed = true;
                }
            }
        } while (changed && --guard > 0);
    }

    private int refineAgainstNeighbor(int lod, Integer neighborLod) {
        if (neighborLod == null) return lod;
        int maxAllowed = neighborLod + config.maxNeighborLodDelta;
        return lod > maxAllowed ? maxAllowed : lod;
    }


    private int[] countAppliedVisibleLods() {
        int[] counts = new int[levels.size()];
        for (Integer tileId : visibleTileIds) {
            Integer lod = appliedLodByTile.get(tileId);
            if (lod != null && lod >= 0 && lod < counts.length) {
                counts[lod]++;
            }
        }
        return counts;
    }

    private int[] countDesiredLods(List<TileDecision> decisions) {
        int[] counts = new int[levels.size()];
        for (TileDecision decision : decisions) {
            if (decision.visible()
                && decision.lodIndex() >= 0
                && decision.lodIndex() < counts.length) {
                counts[decision.lodIndex()]++;
            }
        }
        return counts;
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
        if (config.throttleMs < 0L || config.debounceMs < 0L || config.initialSettleMs < 0L) {
            throw new IllegalArgumentException("LOD timing values must be >= 0");
        }
        if (config.activeTransitionsPerPulse < 1
            || config.settledTransitionsPerPulse < 1
            || config.activeBuildsPerPulse < 1
            || config.settledBuildsPerPulse < 1) {
            throw new IllegalArgumentException("Transition/build budgets must be >= 1");
        }
    }

    private static long gridKey(int column, int row) {
        return (((long) column) << 32) ^ (row & 0xffffffffL);
    }

    private static void runOnFx(Runnable runnable) {
        if (Platform.isFxApplicationThread()) runnable.run();
        else Platform.runLater(runnable);
    }

    private record CameraTile(double x, double y, double depth, double radius) { }
}
