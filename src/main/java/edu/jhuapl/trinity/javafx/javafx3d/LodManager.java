package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Screen-space Level-of-Detail (LOD) manager for a heightfield surface.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Compute a pixels-per-cell estimate using camera projection + view-space depth.</li>
 *   <li>Select an LOD index using hysteresis (low/high thresholds).</li>
 *   <li>Schedule evaluations using a combined leading throttle + trailing debounce policy.</li>
 * </ul>
 *
 * <p>This class does not rebuild meshes. It notifies a listener when a new LOD is selected.</p>
 */
public final class LodManager {

    private static final Logger LOG = LoggerFactory.getLogger(LodManager.class);

    public enum UpdateReason {
        DRAG,
        SCROLL,
        RESIZE,
        DATA_CHANGED,
        OTHER
    }

    /**
     * Minimal per-LOD metadata required for screen-space selection.
     * (The LodManager does not need the HeightField data itself.)
     */
    public static final class LodLevel {
        public final int width;
        public final int height;

        public LodLevel(int width, int height) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("LOD level dimensions must be > 0");
            }
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return "LodLevel{" + width + "x" + height + "}";
        }
    }

    /**
     * Configuration for screen-space LOD selection and scheduling.
     */
    public static final class LodConfig {
        /** Target pixels per cell used for "initial pick" (not hysteresis). */
        public double targetPixelsPerCell = 1.5;
        /** Switch to a coarser LOD when pixelsPerCell drops below this. */
        public double lowThreshold = 1.0;
        /** Switch to a finer LOD when pixelsPerCell rises above this. */
        public double highThreshold = 2.0;

        /** Leading-edge throttle (ms): during continuous interaction, evaluate at most this often. */
        public long throttleMs = 75;
        /** Trailing-edge debounce (ms): always perform a final evaluation after input settles. */
        public long debounceMs = 75;

        /** Minimum valid camera depth to avoid divide-by-zero. */
        public double minDepth = 1e-3;

        /** If true, uses the smaller of X/Z cell sizes for a conservative pixels-per-cell. */
        public boolean conservativeCell = true;

        public LodConfig() { }

        public LodConfig copy() {
            LodConfig c = new LodConfig();
            c.targetPixelsPerCell = targetPixelsPerCell;
            c.lowThreshold = lowThreshold;
            c.highThreshold = highThreshold;
            c.throttleMs = throttleMs;
            c.debounceMs = debounceMs;
            c.minDepth = minDepth;
            c.conservativeCell = conservativeCell;
            return c;
        }
    }

    private final PerspectiveCamera camera;
    private final SubScene subScene;
    private final Node surfaceNode;

    private LodConfig config = new LodConfig();

    // LOD metadata + surface world extents (constant across LODs)
    private List<LodLevel> levels = List.of();
    private int activeIndex = -1;
    private double baseWorldWidth = Double.NaN;
    private double baseWorldDepth = Double.NaN;

    private IntConsumer onLodSelected = null;

    // Scheduling
    private long lastEvalNanos = 0L;
    private final PauseTransition debounceTimer;

    // Resize listeners for automatic LOD re-eval
    private final javafx.beans.value.ChangeListener<Number> resizeListener = (obs, ov, nv) -> requestUpdate(UpdateReason.RESIZE);

    public LodManager(PerspectiveCamera camera, SubScene subScene, Node surfaceNode) {
        this.camera = Objects.requireNonNull(camera, "camera");
        this.subScene = Objects.requireNonNull(subScene, "subScene");
        this.surfaceNode = Objects.requireNonNull(surfaceNode, "surfaceNode");

        debounceTimer = new PauseTransition(Duration.millis(config.debounceMs));
        debounceTimer.setOnFinished(e -> evaluateAndNotify(false));

        // Observe viewport changes directly (pane can also call requestUpdate explicitly; both are fine)
        this.subScene.widthProperty().addListener(resizeListener);
        this.subScene.heightProperty().addListener(resizeListener);
    }

    /**
     * Update configuration. Takes effect immediately for subsequent evaluations.
     */
    public void setConfig(LodConfig cfg) {
        Objects.requireNonNull(cfg, "cfg");
        this.config = cfg.copy();
        // Update debounce timer duration immediately
        runOnFx(() -> debounceTimer.setDuration(Duration.millis(this.config.debounceMs)));
    }

    public LodConfig getConfigCopy() {
        return config.copy();
    }

    /**
     * Provide the current LOD pyramid metadata and the constant world extents.
     *
     * @param levels          ordered LOD levels: index 0 is finest (L0), increasing is coarser
     * @param baseWorldWidth  constant surface width in surfaceNode local world units
     * @param baseWorldDepth  constant surface depth in surfaceNode local world units
     * @param activeIndex     current active index (or -1 if unknown)
     */
    public void setPyramid(List<LodLevel> levels, double baseWorldWidth, double baseWorldDepth, int activeIndex) {
        Objects.requireNonNull(levels, "levels");
        if (levels.isEmpty()) throw new IllegalArgumentException("levels must not be empty");
        if (!(baseWorldWidth > 0.0) || !(baseWorldDepth > 0.0)) {
            throw new IllegalArgumentException("baseWorldWidth/baseWorldDepth must be > 0");
        }
        for (LodLevel l : levels) Objects.requireNonNull(l, "levels contains null");

        // Copy to prevent external mutation surprises
        List<LodLevel> copy = new ArrayList<>(levels);

        this.levels = List.copyOf(copy);
        this.baseWorldWidth = baseWorldWidth;
        this.baseWorldDepth = baseWorldDepth;
        this.activeIndex = clamp(activeIndex, -1, this.levels.size() - 1);

        System.out.println("Hypersurface LOD pyramid configured: levels=" + formatLevels(this.levels)
            + ", world=" + baseWorldWidth + "x" + baseWorldDepth
            + ", activeIndex=" + this.activeIndex);
    }

    /**
     * Convenience helper if you're holding HeightField levels: convert to LodLevels.
     */
    public static List<LodLevel> fromHeightFields(List<HeightField> hfs) {
        Objects.requireNonNull(hfs, "hfs");
        ArrayList<LodLevel> out = new ArrayList<>(hfs.size());
        for (HeightField hf : hfs) {
            Objects.requireNonNull(hf, "hfs contains null");
            out.add(new LodLevel(hf.width(), hf.height()));
        }
        return out;
    }

    public void setOnLodSelected(IntConsumer cb) {
        this.onLodSelected = cb;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    /**
     * Request an LOD evaluation. Uses combined throttle + debounce policy.
     * Safe to call from any thread.
     */
    public void requestUpdate(UpdateReason reason) {
        runOnFx(() -> {
            // Always schedule a trailing evaluation
            debounceTimer.stop();
            debounceTimer.setDuration(Duration.millis(config.debounceMs));
            debounceTimer.playFromStart();

            // Leading throttle for responsiveness during continuous interaction
            long now = System.nanoTime();
            long throttleNanos = Math.max(0L, config.throttleMs) * 1_000_000L;
            if (throttleNanos == 0L || (now - lastEvalNanos) >= throttleNanos) {
                evaluateAndNotify(false);
            }
        });
    }

    /**
     * Force an immediate evaluation (no throttle). This bypasses scheduling only;
     * it does not bypass LOD hysteresis or repick from the target threshold. Safe to call from any thread.
     */
    public void forceUpdate() {
        runOnFx(() -> {
            debounceTimer.stop();
            evaluateAndNotify(true);
        });
    }

    /**
     * Remove listeners and stop internal timers.
     */
    public void dispose() {
        runOnFx(() -> {
            debounceTimer.stop();
            subScene.widthProperty().removeListener(resizeListener);
            subScene.heightProperty().removeListener(resizeListener);
        });
    }

    // ============================================================
    // Core selection logic
    // ============================================================

    private void evaluateAndNotify(boolean force) {
        if (levels == null || levels.isEmpty()) return;
        if (!Double.isFinite(baseWorldWidth) || !Double.isFinite(baseWorldDepth)) return;
        if (camera == null || subScene == null || surfaceNode == null) return;

        int next = chooseIndexScreenSpace(force);
        if (next < 0) return;

        lastEvalNanos = System.nanoTime();

        boolean changed = next != activeIndex;
        if (force || changed) {
            logEvaluation(next, force, changed);
        }

        if (changed) {
            activeIndex = next;
            if (onLodSelected != null) {
                onLodSelected.accept(next);
            }
        }
    }

    private int chooseIndexScreenSpace(boolean force) {
        double d = computeDepthToSurfaceCenter();
        if (!(d > 0.0)) return (activeIndex >= 0 ? activeIndex : 0);
        d = Math.max(d, config.minDepth);

        double fovY = Math.toRadians(camera.getFieldOfView());
        double Hpx = Math.max(1.0, subScene.getHeight());
        double denom = 2.0 * d * Math.tan(fovY / 2.0);
        if (!(denom > 1e-12)) return (activeIndex >= 0 ? activeIndex : 0);

        // If no current index, pick based on target threshold (finest meeting target)
        if (activeIndex < 0) {
            int best = levels.size() - 1; // default to coarsest
            for (int i = 0; i < levels.size(); i++) {
                double ppc = pixelsPerCell(levels.get(i), Hpx, denom);
                if (ppc >= config.targetPixelsPerCell) {
                    best = i;
                    break; // levels ordered fine->coarse, so first meeting target is best
                }
            }
            return best;
        }

        int cur = clamp(activeIndex, 0, levels.size() - 1);
        double curPpc = pixelsPerCell(levels.get(cur), Hpx, denom);

        if (curPpc < config.lowThreshold && cur < levels.size() - 1) {
            return cur + 1; // go coarser
        }
        if (curPpc > config.highThreshold && cur > 0) {
            return cur - 1; // go finer
        }
        return cur;
    }

    private double pixelsPerCell(LodLevel lvl, double Hpx, double denom) {
        // Cell size in world units for this LOD, preserving constant world extents
        double cellX = baseWorldWidth / lvl.width;
        double cellZ = baseWorldDepth / lvl.height;
        double cell = config.conservativeCell ? Math.min(cellX, cellZ) : 0.5 * (cellX + cellZ);
        return (cell * Hpx) / denom;
    }

    /**
     * Computes view-space depth (|z| in camera local coordinates) from camera to the surface center.
     *
     * <p>Assumes surfaceNode local space uses world units (i.e., the mesh is built with x*scale, z*scale).
     * The center point is defined as (baseWorldWidth/2, 0, baseWorldDepth/2) in surfaceNode local coords.</p>
     */
    private double computeDepthToSurfaceCenter() {
        double cx = baseWorldWidth * 0.5;
        double cz = baseWorldDepth * 0.5;

        Point3D scenePt = surfaceNode.localToScene(cx, 0.0, cz);
        Point3D camPt = camera.sceneToLocal(scenePt);
        return Math.abs(camPt.getZ());
    }


    private void logEvaluation(int selectedIndex, boolean force, boolean changed) {
        double d = computeDepthToSurfaceCenter();
        double fovY = Math.toRadians(camera.getFieldOfView());
        double hpx = Math.max(1.0, subScene.getHeight());
        double denom = 2.0 * Math.max(d, config.minDepth) * Math.tan(fovY / 2.0);

        StringBuilder ppc = new StringBuilder();
        for (int i = 0; i < levels.size(); i++) {
            if (i > 0) ppc.append(", ");
            double value = denom > 1e-12 ? pixelsPerCell(levels.get(i), hpx, denom) : Double.NaN;
            ppc.append("L").append(i)
                .append("=").append(levels.get(i).width).append("x").append(levels.get(i).height)
                .append("@").append(String.format(java.util.Locale.ROOT, "%.3f", value)).append("px/cell");
        }

        System.out.println("Hypersurface LOD evaluation: depth="
            + String.format(java.util.Locale.ROOT, "%.3f", d)
            + ", viewportHeight=" + String.format(java.util.Locale.ROOT, "%.1f", hpx)
            + ", selected=L" + selectedIndex
            + ", previous=L" + activeIndex
            + ", force=" + force
            + ", changed=" + changed
            + ", " + ppc);
    }

    private static String formatLevels(List<LodLevel> levels) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < levels.size(); i++) {
            if (i > 0) sb.append(", ");
            LodLevel l = levels.get(i);
            sb.append("L").append(i).append("=").append(l.width).append("x").append(l.height);
        }
        return sb.append(']').toString();
    }

    // ============================================================
    // Helpers
    // ============================================================

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    private static void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}