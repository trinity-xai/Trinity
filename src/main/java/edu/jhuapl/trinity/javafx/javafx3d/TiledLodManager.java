package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Camera/frustum-aware per-tile LOD selector for a tiled heightfield surface.
 *
 * <p>This class performs no mesh construction. It owns only tile visibility/LOD
 * decisions plus the same leading-throttle/trailing-debounce scheduling used by
 * the original global {@link LodManager}.</p>
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
                               double depth, double pixelsPerCell) { }

    public static final class Config {
        public double targetPixelsPerCell = 1.5;
        public double lowThreshold = 1.0;
        public double highThreshold = 2.0;
        public long throttleMs = 75;
        public long debounceMs = 75;
        public double minDepth = 1e-3;
        /** Expand the conservative tile bounding sphere used for frustum tests. */
        public double frustumMargin = 1.10;
        /** Adjacent visible tiles may differ by at most this many LOD levels. */
        public int maxNeighborLodDelta = 1;

        public Config copy() {
            Config c = new Config();
            c.targetPixelsPerCell = targetPixelsPerCell;
            c.lowThreshold = lowThreshold;
            c.highThreshold = highThreshold;
            c.throttleMs = throttleMs;
            c.debounceMs = debounceMs;
            c.minDepth = minDepth;
            c.frustumMargin = frustumMargin;
            c.maxNeighborLodDelta = maxNeighborLodDelta;
            return c;
        }
    }

    private final PerspectiveCamera camera;
    private final SubScene subScene;
    private final Node surfaceNode;
    private final PauseTransition debounceTimer;
    private final javafx.beans.value.ChangeListener<Number> resizeListener =
        (obs, oldValue, newValue) -> requestUpdate(LodManager.UpdateReason.RESIZE);

    private Config config = new Config();
    private List<LodManager.LodLevel> levels = List.of();
    private List<TileSpec> tiles = List.of();
    private double baseWorldWidth = Double.NaN;
    private double baseWorldDepth = Double.NaN;
    private double heightRadius = 0.0;
    private long lastEvalNanos;
    private Consumer<List<TileDecision>> onDecisions;

    /** Current per-tile LOD state. Invisible tiles keep their last LOD for hysteresis. */
    private final Map<Integer, Integer> activeLodByTile = new HashMap<>();

    public TiledLodManager(PerspectiveCamera camera, SubScene subScene, Node surfaceNode) {
        this.camera = Objects.requireNonNull(camera, "camera");
        this.subScene = Objects.requireNonNull(subScene, "subScene");
        this.surfaceNode = Objects.requireNonNull(surfaceNode, "surfaceNode");

        debounceTimer = new PauseTransition(Duration.millis(config.debounceMs));
        debounceTimer.setOnFinished(e -> evaluateAndNotify(false));
        subScene.widthProperty().addListener(resizeListener);
        subScene.heightProperty().addListener(resizeListener);
    }

    public void setConfig(Config config) {
        this.config = Objects.requireNonNull(config, "config").copy();
        runOnFx(() -> debounceTimer.setDuration(Duration.millis(this.config.debounceMs)));
    }

    public Config getConfigCopy() {
        return config.copy();
    }

    public void setOnDecisions(Consumer<List<TileDecision>> onDecisions) {
        this.onDecisions = onDecisions;
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
        activeLodByTile.clear();
    }

    public void requestUpdate(LodManager.UpdateReason reason) {
        runOnFx(() -> {
            debounceTimer.stop();
            debounceTimer.setDuration(Duration.millis(config.debounceMs));
            debounceTimer.playFromStart();

            long now = System.nanoTime();
            long throttleNanos = Math.max(0L, config.throttleMs) * 1_000_000L;
            if (throttleNanos == 0L || now - lastEvalNanos >= throttleNanos) {
                evaluateAndNotify(false);
            }
        });
    }

    /** Force an immediate evaluation while preserving per-tile hysteresis. */
    public void forceUpdate() {
        runOnFx(() -> {
            debounceTimer.stop();
            evaluateAndNotify(true);
        });
    }

    public void dispose() {
        runOnFx(() -> {
            debounceTimer.stop();
            subScene.widthProperty().removeListener(resizeListener);
        });
    }

    private void evaluateAndNotify(boolean force) {
        if (levels.isEmpty() || tiles.isEmpty()) return;
        if (!Double.isFinite(baseWorldWidth) || !Double.isFinite(baseWorldDepth)) return;

        final double viewportWidth = Math.max(1.0, subScene.getWidth());
        final double viewportHeight = Math.max(1.0, subScene.getHeight());
        final double aspect = viewportWidth / viewportHeight;
        final double fovRadians = Math.toRadians(camera.getFieldOfView());
        final double tanHalfFov = Math.tan(fovRadians * 0.5);
        if (!(tanHalfFov > 0.0)) return;

        ArrayList<TileDecision> decisions = new ArrayList<>(tiles.size());
        Map<Long, Integer> visibleLods = new HashMap<>();

        for (TileSpec tile : tiles) {
            CameraTile cameraTile = cameraTile(tile);
            boolean visible = isVisible(cameraTile, tile, aspect, tanHalfFov);
            int current = activeLodByTile.getOrDefault(tile.id, -1);

            if (!visible) {
                decisions.add(new TileDecision(tile.id, false,
                    current >= 0 ? current : levels.size() - 1,
                    cameraTile.depth, 0.0));
                continue;
            }

            double depth = Math.max(config.minDepth,
                cameraTile.depth - cameraTile.radius * 0.35);
            int selected = chooseLod(current, depth, viewportWidth, viewportHeight, aspect, tanHalfFov);
            activeLodByTile.put(tile.id, selected);
            visibleLods.put(gridKey(tile.column, tile.row), selected);
            decisions.add(new TileDecision(tile.id, true, selected, depth,
                pixelsPerCell(levels.get(selected), depth, viewportWidth, viewportHeight, aspect, tanHalfFov)));
        }

        // Prevent abrupt neighboring resolution differences. We only refine the coarser
        // neighbor; never coarsen a tile selected by screen-space demand.
        if (config.maxNeighborLodDelta >= 0) {
            enforceNeighborDelta(decisions, visibleLods);
        }

        lastEvalNanos = System.nanoTime();
        if (onDecisions != null) onDecisions.accept(List.copyOf(decisions));
    }

    private int chooseLod(int current, double depth,
                          double viewportWidth, double viewportHeight,
                          double aspect, double tanHalfFov) {
        if (current < 0 || current >= levels.size()) {
            int best = levels.size() - 1;
            for (int i = 0; i < levels.size(); i++) {
                if (pixelsPerCell(levels.get(i), depth,
                    viewportWidth, viewportHeight, aspect, tanHalfFov) >= config.targetPixelsPerCell) {
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
        while (selected > 0) {
            double ppc = pixelsPerCell(levels.get(selected), depth,
                viewportWidth, viewportHeight, aspect, tanHalfFov);
            if (ppc <= config.highThreshold) break;
            selected--;
        }
        return selected;
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

    private boolean isVisible(CameraTile ct, TileSpec tile, double aspect, double tanHalfFov) {
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

    private void enforceNeighborDelta(List<TileDecision> decisions, Map<Long, Integer> visibleLods) {
        if (visibleLods.isEmpty()) return;
        Map<Integer, TileDecision> byId = new HashMap<>();
        for (TileDecision d : decisions) byId.put(d.tileId(), d);

        boolean changed;
        int guard = Math.max(1, levels.size() * 2);
        do {
            changed = false;
            for (TileSpec tile : tiles) {
                TileDecision decision = byId.get(tile.id);
                if (decision == null || !decision.visible()) continue;
                int lod = activeLodByTile.getOrDefault(tile.id, decision.lodIndex());
                int refined = lod;
                refined = refineAgainstNeighbor(refined, visibleLods.get(gridKey(tile.column - 1, tile.row)));
                refined = refineAgainstNeighbor(refined, visibleLods.get(gridKey(tile.column + 1, tile.row)));
                refined = refineAgainstNeighbor(refined, visibleLods.get(gridKey(tile.column, tile.row - 1)));
                refined = refineAgainstNeighbor(refined, visibleLods.get(gridKey(tile.column, tile.row + 1)));
                if (refined != lod) {
                    activeLodByTile.put(tile.id, refined);
                    visibleLods.put(gridKey(tile.column, tile.row), refined);
                    changed = true;
                }
            }
        } while (changed && --guard > 0);

        for (int i = 0; i < decisions.size(); i++) {
            TileDecision d = decisions.get(i);
            if (!d.visible()) continue;
            int lod = activeLodByTile.getOrDefault(d.tileId(), d.lodIndex());
            if (lod != d.lodIndex()) {
                double ppc = d.pixelsPerCell();
                decisions.set(i, new TileDecision(d.tileId(), true, lod, d.depth(), ppc));
            }
        }
    }

    private int refineAgainstNeighbor(int lod, Integer neighborLod) {
        if (neighborLod == null) return lod;
        int maxAllowed = neighborLod + config.maxNeighborLodDelta;
        return lod > maxAllowed ? maxAllowed : lod;
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
