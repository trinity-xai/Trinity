package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import javafx.animation.Timeline;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import org.fxyz3d.utils.CameraTransformer;

/**
 * Owns camera framing and canonical view presets for the Hypersurface projection.
 *
 * <p>The controller deliberately operates on world-space surface bounds rather than
 * source or render resolution. This keeps camera framing stable across LOD changes,
 * tiled rendering, and large image sources.</p>
 */
public final class HypersurfaceCameraController {

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

    private final PerspectiveCamera camera;
    private final CameraTransformer cameraTransform;
    private final SubScene subScene;
    private final BoundsProvider boundsProvider;
    private final Runnable cameraSettledCallback;

    private Timeline activeTransition;
    private double fitPadding = DEFAULT_FIT_PADDING;

    public HypersurfaceCameraController(PerspectiveCamera camera,
                                        CameraTransformer cameraTransform,
                                        SubScene subScene,
                                        BoundsProvider boundsProvider,
                                        Runnable cameraSettledCallback) {
        if (camera == null) throw new IllegalArgumentException("camera cannot be null");
        if (cameraTransform == null) throw new IllegalArgumentException("cameraTransform cannot be null");
        if (subScene == null) throw new IllegalArgumentException("subScene cannot be null");
        if (boundsProvider == null) throw new IllegalArgumentException("boundsProvider cannot be null");

        this.camera = camera;
        this.cameraTransform = cameraTransform;
        this.subScene = subScene;
        this.boundsProvider = boundsProvider;
        this.cameraSettledCallback = cameraSettledCallback != null
            ? cameraSettledCallback
            : () -> { };
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
