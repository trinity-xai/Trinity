package edu.jhuapl.trinity.javafx.javafx3d.animated;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 */
public class RadialGrid extends Group {

    private static final Logger LOG = LoggerFactory.getLogger(RadialGrid.class);
    private final Rotate worldRotateY = new Rotate(0, Rotate.Y_AXIS);
    private static final int NUM_CIRCLES = 5;
    private static final int NUM_RADIAL_LINES = 20;
    private static final double MAX_RADIUS = 1000;
    private static final double LINE_RADIUS = 0.5;
    private static final double CIRCLE_SEGMENTS = 60;
    AnimationTimer pulseAnimator;
    private double pulseScalar = 0.25;
    private double baseRadius = LINE_RADIUS;
    private double rotationSpeed = 7; // degrees per second
    private double pulseSpeedHz = 0.12; // pulses per second
    private boolean enableRotation = false;
    private boolean enablePulsation = false;
    public PhongMaterial gridMaterial = new PhongMaterial(Color.DEEPSKYBLUE); // deep sky blue

    public RadialGrid() {
        this(NUM_CIRCLES, NUM_RADIAL_LINES, MAX_RADIUS, LINE_RADIUS, CIRCLE_SEGMENTS);
    }

    public RadialGrid(int circles, int radialLines, double maxRadius, double lineRadius, double circleSegments) {
        gridMaterial.setSpecularColor(Color.LIGHTCYAN);
        getTransforms().add(worldRotateY); // apply the rotating transform
        // Build grid and axis markers
        createRadialGrid(circles, radialLines, maxRadius, lineRadius, circleSegments);

        pulseAnimator = new AnimationTimer() {
            private long startTime = -1;
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (startTime < 0) startTime = now;
                if (lastTime > 0) {
                    if (isEnableRotation()) {
                        double deltaSeconds = (now - lastTime) / 1_000_000_000.0;
                        double deltaAngle = deltaSeconds * getRotationSpeed();
                        getWorldRotateY().setAngle(getWorldRotateY().getAngle() + deltaAngle);
                    }
                }
                lastTime = now;

                if (isEnablePulsation()) {
                    double elapsedSec = (now - startTime) / 1_000_000_000.0;
                    double pulse = (Math.sin(2 * Math.PI * getPulseSpeedHz() * elapsedSec) + 1) / 2; // 0 to 1
                    // Optional: vary radius slightly for "breathing"
                    for (Node node : getChildren()) {
                        if (node instanceof Cylinder cylinder) {
                            cylinder.setRadius(getBaseRadius() * (getPulseScalar() + pulse));
                        }
                    }
                }
            }
        };
    }

    public void regenerate(int circles, int radialLines, double maxRadius, double lineRadius, double circleSegments) {
        getChildren().clear();
        createRadialGrid(circles, radialLines, maxRadius, lineRadius, circleSegments);
    }

    public void setEnableAnimation(boolean enable) {
        if (enable)
            pulseAnimator.start();
        else
            pulseAnimator.stop();
    }

    private void createRadialGrid(int numCircles, int radialLines, double maxRadius, double lineRadius, double circleSegments) {

        // Draw concentric circles using short cylinders
        for (int i = 1; i <= numCircles; i++) {
            double radius = (maxRadius / numCircles) * i;
            for (int j = 0; j < circleSegments; j++) {
                double angle1 = 2 * Math.PI * j / circleSegments;
                double angle2 = 2 * Math.PI * (j + 1) / circleSegments;

                double x1 = radius * Math.cos(angle1);
                double z1 = radius * Math.sin(angle1);
                double x2 = radius * Math.cos(angle2);
                double z2 = radius * Math.sin(angle2);

                addLine3D(this, x1, 0, z1, x2, 0, z2, lineRadius, gridMaterial);
            }
        }

        // Draw radial lines
        for (int i = 0; i < radialLines; i++) {
            double angle = 2 * Math.PI * i / radialLines;
            double x = maxRadius * Math.cos(angle);
            double z = maxRadius * Math.sin(angle);

            addLine3D(this, 0, 0, 0, x, 0, z, lineRadius, gridMaterial);
        }
    }

    public static void addLine3D(Group group, double x1, double y1, double z1,
                                 double x2, double y2, double z2, double radius, PhongMaterial material) {
        // Vector from point A to B
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1e-6) return;

        // Create cylinder (default aligned along Y)
        Cylinder line = new Cylinder(radius, length);
        line.setMaterial(material);

        // Move to midpoint
        double midX = (x1 + x2) / 2.0;
        double midY = (y1 + y2) / 2.0;
        double midZ = (z1 + z2) / 2.0;
        line.setTranslateX(midX);
        line.setTranslateY(midY);
        line.setTranslateZ(midZ);

        // Default direction of cylinder is (0,1,0)
        Point3D originDirection = new Point3D(0, 1, 0);
        Point3D targetDirection = new Point3D(dx, dy, dz).normalize();

        // Compute rotation axis and angle
        Point3D rotationAxis = originDirection.crossProduct(targetDirection);
        double angle = Math.toDegrees(Math.acos(originDirection.dotProduct(targetDirection)));

        if (!rotationAxis.equals(Point3D.ZERO)) {
            line.getTransforms().add(new Rotate(angle, rotationAxis));
        }

        group.getChildren().add(line);
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
     * @return the pulseSpeedHz
     */
    public double getPulseSpeedHz() {
        return pulseSpeedHz;
    }

    /**
     * @param pulseSpeedHz the pulseSpeedHz to set
     */
    public void setPulseSpeedHz(double pulseSpeedHz) {
        this.pulseSpeedHz = pulseSpeedHz;
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

    /**
     * @return the enablePulsation
     */
    public boolean isEnablePulsation() {
        return enablePulsation;
    }

    /**
     * @param enablePulsation the enablePulsation to set
     */
    public void setEnablePulsation(boolean enablePulsation) {
        this.enablePulsation = enablePulsation;
    }

    /**
     * @return the baseRadius
     */
    public double getBaseRadius() {
        return baseRadius;
    }

    /**
     * @param baseRadius the baseRadius to set
     */
    public void setBaseRadius(double baseRadius) {
        this.baseRadius = baseRadius;
    }

    /**
     * @return the pulseScalar
     */
    public double getPulseScalar() {
        return pulseScalar;
    }

    /**
     * @param pulseScalar the pulseScalar to set
     */
    public void setPulseScalar(double pulseScalar) {
        this.pulseScalar = pulseScalar;
    }
    /**
     * @return the worldRotateY
     */
    public Rotate getWorldRotateY() {
        return worldRotateY;
    }    
}
