package edu.jhuapl.trinity.javafx.javafx3d.animated;

import javafx.animation.AnimationTimer;
import org.fxyz3d.utils.CameraTransformer;

/**
 * @author Sean Phillips
 */
public class CameraOrbiter extends AnimationTimer {
    private long startTime = -1;
    private long lastTime = 0;
    private CameraTransformer cameraTransform;
    private double rotationSpeed = 7; // degrees per second
    private boolean enableRotation = false;

    public CameraOrbiter(CameraTransformer cameraTransform, double rotationDegreesPerSecond) {
        this.cameraTransform = cameraTransform;
        rotationSpeed = rotationDegreesPerSecond;
    }

    @Override
    public void handle(long now) {

        if (startTime < 0) startTime = now;
        if (lastTime > 0) {
            if (isEnableRotation()) {
                double deltaSeconds = (now - lastTime) / 1_000_000_000.0;
                double deltaAngle = deltaSeconds * getRotationSpeed();
                cameraTransform.ry.setAngle(cameraTransform.ry.getAngle() + deltaAngle);
            }
        }
        lastTime = now;

    }

    /**
     * @return the rotationSpeed
     */
    public double getRotationSpeed() {
        return rotationSpeed;
    }

    /**
     * @param rotationSpeed the rotationSpeed to set
     */
    public void setRotationSpeed(double rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    /**
     * @return the enableRotation
     */
    public boolean isEnableRotation() {
        return enableRotation;
    }

    /**
     * @param enableRotation the enableRotation to set
     */
    public void setEnableRotation(boolean enableRotation) {
        this.enableRotation = enableRotation;
    }
}
