package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import javafx.animation.Timeline;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import org.fxyz3d.utils.CameraTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Owns camera input, camera motion, framing, and canonical view presets for the
 * Hypersurface projection.
 *
 * <p>The controller deliberately operates on world-space surface bounds rather than
 * source or render resolution. This keeps camera framing stable across LOD changes,
 * tiled rendering, and large image sources. Rendering/LOD policy remains outside the
 * controller and is notified through narrow interaction callbacks.</p>
 */
public final class HypersurfaceCameraController {

    private static final Logger LOG = LoggerFactory.getLogger(HypersurfaceCameraController.class);

    public enum InteractionType {
        DRAG,
        ZOOM,
        KEYBOARD
    }

    public enum Preset {
        OBLIQUE(-10.0, -45.0, 0.0),
        TOP(-90.0, 0.0, 0.0),
        FRONT(0.0, 0.0, 0.0),
        SIDE(0.0, -90.0, 0.0);

        private final double rotateX;
        private final double rotateY;
        private final double rotateZ;

        Preset(double rotateX, double rotateY, double rotateZ) {
            this.rotateX = rotateX;
            this.rotateY = rotateY;
            this.rotateZ = rotateZ;
        }

        double rotateX() {
            return rotateX;
        }

        double rotateY() {
            return rotateY;
        }

        double rotateZ() {
            return rotateZ;
        }
    }

    /**
     * World-space extents centered around the Hypersurface origin.
     */
    public record SurfaceBounds(double width, double height, double depth) {
        public SurfaceBounds {
            width = sanitizeExtent(width);
            height = sanitizeExtent(height);
            depth = sanitizeExtent(depth);
        }

        private static double sanitizeExtent(double value) {
            return Double.isFinite(value) && value > 0.0 ? value : 1.0;
        }
    }

    @FunctionalInterface
    public interface BoundsProvider {
        SurfaceBounds getSurfaceBounds();
    }

    private static final double DEFAULT_FIT_PADDING = 1.08;
    private static final double MIN_HALF_FOV_RADIANS = Math.toRadians(1.0);
    private static final double MIN_CAMERA_DISTANCE = 10.0;
    private static final double GESTURE_ZOOM_STEP = 50.0;
    private static final double MOUSE_ROTATION_FACTOR = 0.1;
    private static final double MOUSE_PAN_FACTOR = 0.3;

    private final PerspectiveCamera camera;
    private final CameraTransformer cameraTransform;
    private final SubScene subScene;
    private final BoundsProvider boundsProvider;
    private final Consumer<InteractionType> cameraChangedCallback;
    private final Runnable cameraSettledCallback;

    private Timeline activeTransition;
    private double fitPadding = DEFAULT_FIT_PADDING;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;

    public HypersurfaceCameraController(PerspectiveCamera camera,
                                        CameraTransformer cameraTransform,
                                        SubScene subScene,
                                        BoundsProvider boundsProvider,
                                        Consumer<InteractionType> cameraChangedCallback,
                                        Runnable cameraSettledCallback) {
        if (camera == null) throw new IllegalArgumentException("camera cannot be null");
        if (cameraTransform == null) throw new IllegalArgumentException("cameraTransform cannot be null");
        if (subScene == null) throw new IllegalArgumentException("subScene cannot be null");
        if (boundsProvider == null) throw new IllegalArgumentException("boundsProvider cannot be null");

        this.camera = camera;
        this.cameraTransform = cameraTransform;
        this.subScene = subScene;
        this.boundsProvider = boundsProvider;
        this.cameraChangedCallback = cameraChangedCallback != null
            ? cameraChangedCallback
            : ignored -> { };
        this.cameraSettledCallback = cameraSettledCallback != null
            ? cameraSettledCallback
            : () -> { };
    }

    /**
     * Installs the camera-specific mouse and gesture handlers on the Hypersurface
     * SubScene. Keyboard handling remains callable from the pane because the same
     * key handler also owns non-camera Hypersurface shortcuts.
     */
    public void installInputHandlers() {
        subScene.setOnMousePressed(this::handleMousePressed);
        subScene.setOnZoom(this::handleZoom);
        subScene.setOnScroll(this::handleScroll);
        subScene.setOnMouseDragged(this::handleMouseDragged);
        subScene.setOnMouseReleased(this::handleMouseReleased);
    }

    /**
     * Handles camera-related keyboard shortcuts while allowing the owning pane to
     * continue handling data/analysis shortcuts in the same KeyEvent callback.
     *
     * @return true when at least one camera action was applied
     */
    public boolean handleKeyPressed(KeyEvent event) {
        if (event == null) return false;

        KeyCode keycode = event.getCode();
        boolean cameraChanged = false;

        if ((keycode == KeyCode.NUMPAD0 && event.isControlDown())
            || (keycode == KeyCode.DIGIT0 && event.isControlDown())) {
            reset(1000.0);
            cameraChanged = true;
        } else if ((keycode == KeyCode.NUMPAD0 && event.isShiftDown())
            || (keycode == KeyCode.DIGIT0 && event.isShiftDown())) {
            reset(0.0);
            cameraChanged = true;
        }

        double change = event.isShiftDown() ? 100.0 : 10.0;
        if (keycode == KeyCode.W) {
            camera.setTranslateZ(camera.getTranslateZ() + change);
            cameraChanged = true;
        }
        if (keycode == KeyCode.S) {
            camera.setTranslateZ(camera.getTranslateZ() - change);
            cameraChanged = true;
        }
        if (keycode == KeyCode.PLUS && event.isShortcutDown()) {
            camera.setTranslateZ(camera.getTranslateZ() + change);
            cameraChanged = true;
        }
        if (keycode == KeyCode.MINUS && event.isShortcutDown()) {
            camera.setTranslateZ(camera.getTranslateZ() - change);
            cameraChanged = true;
        }
        if (keycode == KeyCode.A) {
            camera.setTranslateX(camera.getTranslateX() - change);
            cameraChanged = true;
        }
        if (keycode == KeyCode.D) {
            camera.setTranslateX(camera.getTranslateX() + change);
            cameraChanged = true;
        }
        if (keycode == KeyCode.SPACE) {
            camera.setTranslateY(camera.getTranslateY() + change);
            cameraChanged = true;
        }
        if (keycode == KeyCode.X) {
            camera.setTranslateY(camera.getTranslateY() - change);
            cameraChanged = true;
        }

        change = event.isShiftDown() ? 10.0 : 1.0;
        if (keycode == KeyCode.NUMPAD7 || keycode == KeyCode.DIGIT8) {
            cameraTransform.ry.setAngle(cameraTransform.ry.getAngle() + change);
            cameraChanged = true;
        }
        if (keycode == KeyCode.NUMPAD9 || (keycode == KeyCode.DIGIT8 && event.isControlDown())) {
            cameraTransform.ry.setAngle(cameraTransform.ry.getAngle() - change);
            cameraChanged = true;
        }
        if (keycode == KeyCode.NUMPAD4 || keycode == KeyCode.DIGIT9) {
            cameraTransform.rx.setAngle(cameraTransform.rx.getAngle() + change);
            cameraChanged = true;
        }
        if (keycode == KeyCode.NUMPAD6 || (keycode == KeyCode.DIGIT9 && event.isControlDown())) {
            cameraTransform.rx.setAngle(cameraTransform.rx.getAngle() - change);
            cameraChanged = true;
        }
        if (keycode == KeyCode.NUMPAD1 || keycode == KeyCode.DIGIT0) {
            cameraTransform.rz.setAngle(cameraTransform.rz.getAngle() + change);
            cameraChanged = true;
        }
        if (keycode == KeyCode.NUMPAD3 || (keycode == KeyCode.DIGIT0 && event.isControlDown())) {
            cameraTransform.rz.setAngle(cameraTransform.rz.getAngle() - change);
            cameraChanged = true;
        }

        if (cameraChanged) {
            cameraChangedCallback.accept(InteractionType.KEYBOARD);
        }
        return cameraChanged;
    }

    /** Moves the camera immediately to the supplied local-Z distance. */
    public void setCameraDistance(double distance) {
        camera.setTranslateZ(distance);
    }

    /** Historical Hypersurface intro: begin far away, then fit the current surface. */
    public void intro(double milliseconds, double introDistance) {
        setCameraDistance(introDistance);
        fit(milliseconds);
    }

    /** Historical Hypersurface outtro: animate directly to the supplied distance. */
    public void outtro(double milliseconds, double outroDistance) {
        JavaFX3DUtils.zoomTransition(milliseconds, camera, outroDistance);
    }

    /**
     * Fits the complete surface while preserving the current camera rotation.
     */
    public void fit(double milliseconds) {
        transitionTo(
            cameraTransform.rx.getAngle(),
            cameraTransform.ry.getAngle(),
            cameraTransform.rz.getAngle(),
            computeFitDistance(
                cameraTransform.rx.getAngle(),
                cameraTransform.ry.getAngle(),
                cameraTransform.rz.getAngle()),
            milliseconds);
    }

    /**
     * Applies a canonical orientation and fits the complete surface in that view.
     */
    public void applyPreset(Preset preset, double milliseconds) {
        if (preset == null) return;
        transitionTo(
            preset.rotateX(),
            preset.rotateY(),
            preset.rotateZ(),
            computeFitDistance(preset.rotateX(), preset.rotateY(), preset.rotateZ()),
            milliseconds);
    }

    /**
     * Historical reset semantics are the canonical oblique orientation, now fitted
     * to the actual surface instead of using a fixed camera Z distance.
     */
    public void reset(double milliseconds) {
        applyPreset(Preset.OBLIQUE, milliseconds);
    }

    public double getFitPadding() {
        return fitPadding;
    }

    public void setFitPadding(double fitPadding) {
        if (!Double.isFinite(fitPadding) || fitPadding < 1.0) {
            throw new IllegalArgumentException("fitPadding must be finite and >= 1.0");
        }
        this.fitPadding = fitPadding;
    }

    private void handleMousePressed(MouseEvent event) {
        if (event.isSynthesized()) LOG.info("isSynthesized");
        mousePosX = event.getSceneX();
        mousePosY = event.getSceneY();
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
    }

    private void handleMouseDragged(MouseEvent event) {
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = event.getSceneX();
        mousePosY = event.getSceneY();

        double mouseDeltaX = mousePosX - mouseOldX;
        double mouseDeltaY = mousePosY - mouseOldY;
        double modifier = 1.0;
        if (event.isControlDown()) modifier = 0.1;
        if (event.isShiftDown()) modifier = 25.0;

        if (event.isPrimaryButtonDown()) {
            if (event.isAltDown()) {
                cameraTransform.rz.setAngle(normalizeAngle(
                    cameraTransform.rz.getAngle()
                        + mouseDeltaX * MOUSE_ROTATION_FACTOR * modifier * 2.0));
            } else {
                cameraTransform.ry.setAngle(normalizeAngle(
                    cameraTransform.ry.getAngle()
                        + mouseDeltaX * MOUSE_ROTATION_FACTOR * modifier * 2.0));
                cameraTransform.rx.setAngle(normalizeAngle(
                    cameraTransform.rx.getAngle()
                        - mouseDeltaY * MOUSE_ROTATION_FACTOR * modifier * 2.0));
            }
        } else if (event.isMiddleButtonDown()) {
            cameraTransform.t.setX(cameraTransform.t.getX()
                + mouseDeltaX * MOUSE_ROTATION_FACTOR * modifier * MOUSE_PAN_FACTOR);
            cameraTransform.t.setY(cameraTransform.t.getY()
                + mouseDeltaY * MOUSE_ROTATION_FACTOR * modifier * MOUSE_PAN_FACTOR);
        }

        // Preserve the prior behavior: every drag event requests an active-camera
        // refresh, even when no recognized mouse button changed the transform.
        cameraChangedCallback.accept(InteractionType.DRAG);
    }

    private void handleZoom(ZoomEvent event) {
        double deltaZ = event.getZoomFactor() > 1.0
            ? GESTURE_ZOOM_STEP
            : -GESTURE_ZOOM_STEP;
        zoom(deltaZ);
        event.consume();
    }

    private void handleScroll(ScrollEvent event) {
        double modifier = 50.0;
        if (event.isControlDown()) modifier = 1.0;
        if (event.isShiftDown()) modifier = 100.0;
        zoom(event.getDeltaY() * MOUSE_ROTATION_FACTOR * modifier);
    }

    private void handleMouseReleased(MouseEvent event) {
        cameraSettledCallback.run();
    }

    private void zoom(double deltaZ) {
        camera.setTranslateZ(camera.getTranslateZ() + deltaZ);
        cameraChangedCallback.accept(InteractionType.ZOOM);
    }

    private static double normalizeAngle(double angle) {
        return ((angle % 360.0) + 540.0) % 360.0 - 180.0;
    }

    /**
     * Returns the negative local-Z camera translation required to contain the complete
     * axis-aligned surface bounds at the requested camera orientation. The calculation
     * evaluates all eight bounds corners in camera-local coordinates so Top/Front/Side
     * views fit tightly instead of inheriting the excess margin of a bounding sphere.
     */
    double computeFitDistance(double rotateX, double rotateY, double rotateZ) {
        SurfaceBounds bounds = boundsProvider.getSurfaceBounds();
        if (bounds == null) bounds = new SurfaceBounds(1.0, 1.0, 1.0);

        double viewportWidth = Math.max(1.0, subScene.getWidth());
        double viewportHeight = Math.max(1.0, subScene.getHeight());
        double aspect = viewportWidth / viewportHeight;

        double primaryHalfFov = Math.toRadians(camera.getFieldOfView()) * 0.5;
        primaryHalfFov = Math.max(MIN_HALF_FOV_RADIANS, primaryHalfFov);

        double verticalHalfFov;
        double horizontalHalfFov;
        if (camera.isVerticalFieldOfView()) {
            verticalHalfFov = primaryHalfFov;
            horizontalHalfFov = Math.atan(Math.tan(verticalHalfFov) * aspect);
        } else {
            horizontalHalfFov = primaryHalfFov;
            verticalHalfFov = Math.atan(Math.tan(horizontalHalfFov) / aspect);
        }

        horizontalHalfFov = Math.max(MIN_HALF_FOV_RADIANS, horizontalHalfFov);
        verticalHalfFov = Math.max(MIN_HALF_FOV_RADIANS, verticalHalfFov);
        double tanHorizontal = Math.tan(horizontalHalfFov);
        double tanVertical = Math.tan(verticalHalfFov);

        // Use the same CameraTransformer implementation as the live camera to map the
        // world-space bounds into the requested camera orientation. parentToLocal() is
        // the inverse of the transform that positions/orients the camera in the scene.
        CameraTransformer orientation = new CameraTransformer();
        orientation.setRotate(rotateX, rotateY, rotateZ);

        double halfWidth = bounds.width() * 0.5;
        double halfHeight = bounds.height() * 0.5;
        double halfDepth = bounds.depth() * 0.5;
        double requiredDistance = MIN_CAMERA_DISTANCE;

        double[] xs = {-halfWidth, halfWidth};
        double[] ys = {-halfHeight, halfHeight};
        double[] zs = {-halfDepth, halfDepth};
        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    Point3D local = orientation.parentToLocal(new Point3D(x, y, z));
                    if (local == null) continue;

                    // Camera local Z is negative while it looks toward +Z. For a camera
                    // distance d, a corner's forward distance is d + local.z.
                    double horizontalRequirement = Math.abs(local.getX()) / tanHorizontal - local.getZ();
                    double verticalRequirement = Math.abs(local.getY()) / tanVertical - local.getZ();
                    double nearClipRequirement = camera.getNearClip() - local.getZ();
                    requiredDistance = Math.max(requiredDistance, horizontalRequirement);
                    requiredDistance = Math.max(requiredDistance, verticalRequirement);
                    requiredDistance = Math.max(requiredDistance, nearClipRequirement);
                }
            }
        }

        requiredDistance = Math.max(MIN_CAMERA_DISTANCE, requiredDistance * fitPadding);
        return -requiredDistance;
    }

    private void transitionTo(double rotateX,
                              double rotateY,
                              double rotateZ,
                              double cameraZ,
                              double milliseconds) {
        stopActiveTransition();

        // Camera pan is interaction state, not part of a canonical fit. Re-center it
        // before fitting while keeping the Hypersurface world origin as the orbit target.
        cameraTransform.setPivot(0.0, 0.0, 0.0);
        cameraTransform.setTranslate(0.0, 0.0, 0.0);

        double duration = Math.max(0.0, milliseconds);
        if (duration == 0.0) {
            cameraTransform.rx.setAngle(rotateX);
            cameraTransform.ry.setAngle(rotateY);
            cameraTransform.rz.setAngle(rotateZ);
            camera.setTranslateX(0.0);
            camera.setTranslateY(0.0);
            camera.setTranslateZ(cameraZ);
            cameraSettledCallback.run();
            return;
        }

        activeTransition = JavaFX3DUtils.transitionCameraTo(
            duration,
            camera,
            cameraTransform,
            0.0,
            0.0,
            cameraZ,
            rotateX,
            rotateY,
            rotateZ);
        activeTransition.setOnFinished(event -> {
            activeTransition = null;
            cameraSettledCallback.run();
        });
    }

    private void stopActiveTransition() {
        if (activeTransition == null) return;
        activeTransition.stop();
        activeTransition = null;
    }
}
