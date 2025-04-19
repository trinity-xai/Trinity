package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 * Modified from https://stackoverflow.com/questions/46176489/javafx-3d-rotation-around-scene-fixed-axes
 */
public class XFormGroup extends Group {
    private static final Logger LOG = LoggerFactory.getLogger(XFormGroup.class);

    public XFormGroup() {
        super();
        getTransforms().add(new Affine());
    }

    /**
     * Accumulate rotation about specified axis
     *
     * @param angle
     * @param axis
     */
    public void addRotation(double angle, Point3D axis) {
        Rotate r = new Rotate(angle, axis);
        /**
         * This is the important bit and thanks to bronkowitz in this post
         * https://stackoverflow.com/questions/31382634/javafx-3d-rotations for
         * getting me to the solution that the rotations need accumulated in
         * this way
         */
        getTransforms().set(0, r.createConcatenation(getTransforms().get(0)));
    }

    public void matrixRotate(double alf, double bet, double gam) {
        double A11 = Math.cos(alf) * Math.cos(gam);
        double A12 = Math.cos(bet) * Math.sin(alf) + Math.cos(alf) * Math.sin(bet) * Math.sin(gam);
        double A13 = Math.sin(alf) * Math.sin(bet) - Math.cos(alf) * Math.cos(bet) * Math.sin(gam);
        double A21 = -Math.cos(gam) * Math.sin(alf);
        double A22 = Math.cos(alf) * Math.cos(bet) - Math.sin(alf) * Math.sin(bet) * Math.sin(gam);
        double A23 = Math.cos(alf) * Math.sin(bet) + Math.cos(bet) * Math.sin(alf) * Math.sin(gam);
        double A31 = Math.sin(gam);
        double A32 = -Math.cos(gam) * Math.sin(bet);
        double A33 = Math.cos(bet) * Math.cos(gam);

        double d = Math.acos((A11 + A22 + A33 - 1d) / 2d);
        if (d != 0d) {
            double den = 2d * Math.sin(d);
            Point3D p = new Point3D((A32 - A23) / den, (A13 - A31) / den, (A21 - A12) / den);
            setRotationAxis(p);
            setRotate(Math.toDegrees(d));
        }
    }

    /**
     * Set current X rotation by inserting rotate value into Affine
     *
     * @param angle
     */
    public void setRotationX(double angle) {
        setRotationAxis(Rotate.X_AXIS);
        double delta = angle - getRotate();
        Rotate r = new Rotate(angle, Rotate.X_AXIS);
        Affine a = (Affine) getTransforms().get(0);
        LOG.info("X rotate delta: {}", delta);
        addRotation(delta, Rotate.X_AXIS);
    }

    /**
     * Set current Y rotation by inserting rotate value into Affine
     *
     * @param angle
     */
    public void setRotationY(double angle) {
        setRotationAxis(Rotate.Y_AXIS);
        double delta = angle - getRotate();
        LOG.info("Y rotate delta: {}", delta);
        addRotation(delta, Rotate.Y_AXIS);
    }

    /**
     * Set current Z rotation by inserting rotate value into Affine
     *
     * @param angle
     */
    public void setRotationZ(double angle) {
        setRotationAxis(Rotate.Z_AXIS);
        double delta = angle - getRotate();
        LOG.info("Z rotate delta: {}", delta);
        addRotation(delta, Rotate.Z_AXIS);
    }

    public void rotateByEuler(double yaw, double pitch, double roll) {
        //    Sy1, Cy = math.sin(yaw), math.cos(yaw)
        double sinYaw = Math.sin(yaw);
        double cosYaw = Math.cos(yaw);
        //    Sp, Cp = math.sin(pitch), math.cos(pitch)
        double sinPitch = Math.sin(pitch);
        double cosPitch = Math.cos(pitch);
        //    Sr, Cr = math.sin(roll), math.cos(roll)
        double sinRoll = Math.sin(roll);
        double cosRoll = Math.cos(roll);
        //    R_ypr = np.array([[Cy*Cp, -Sy*Cr + Cy*Sp*Sr, Sy*Sr + Cy*Sp*Cr],
        //                      [Sy*Cp, Cy*Cr + Sy*Sp*Sr, -Cy*Sr + Sy*Sp*Cr],
        //                      [-Sp, Cp*Sr, Cp*Cr]])

        //d - the X coordinate scaling element
        //d1 - the XY coordinate element
        //d2 - the XZ coordinate element
        //d3 - the X coordinate translation element
        //d4 - the YX coordinate element
        //d5 - the Y coordinate scaling element
        //d6 - the YZ coordinate element
        //d7 - the Y coordinate translation element
        //d8 - the ZX coordinate element
        //d9 - the ZY coordinate element
        //d10 - the Z coordinate scaling element
        //d11 - the Z coordinate translation element
        Affine affine = new Affine(
            1.0, cosYaw * cosPitch, -sinYaw * cosRoll + cosYaw * sinPitch * sinRoll, sinYaw * sinRoll + cosYaw * sinPitch * cosRoll,
            sinYaw * cosPitch, 1.0, cosYaw * cosRoll + sinYaw * sinPitch * sinRoll, -cosYaw * sinRoll + sinYaw * sinPitch * cosRoll,
            -sinPitch, cosPitch * sinRoll, 1.0, cosPitch * cosRoll
        );

        getTransforms().set(0, affine.createConcatenation(getTransforms().get(0)));
    }

    /**
     * Reset transform to identity transform
     */
    public void reset() {
        getTransforms().set(0, new Affine());
    }
}
