package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.shape.Shape3D;

import java.util.Random;

public abstract class Projectile {
    /**
     * Random instance for random behavior.
     */
    protected static Random random = new Random();

    /**
     * Flag indicating whether the particle is active.
     */
    public SimpleBooleanProperty activeProperty = new SimpleBooleanProperty(true);
    /**
     * physics of the particle.
     */
    public Point3D start = new Point3D(0, 0, 0);
    public Point3D location = new Point3D(0, 0, 0);
    public Point3D velocity = new Point3D(0, 0, 0);

    public static Point3D getRandomPoint3D(float lowerBound, float upperBound) {
        return new Point3D(
            random.nextFloat() * (upperBound - lowerBound) + lowerBound,
            random.nextFloat() * (upperBound - lowerBound) + lowerBound,
            random.nextFloat() * (upperBound - lowerBound) + lowerBound
        );
    }

    /**
     * Logic to tell implementation to reset to defaults/initial conditions
     */
    public abstract void reset();

    /**
     * @return the Shape3D that is added to the 3D scene
     */
    public abstract Shape3D getShape3D();

    /**
     * Updates the particle animation by the specified number of milliseconds.
     *
     * @param _time The number of milliseconds elapsed since the last update.
     * @return true if the particle is still 'alive' (requires further animation),
     * false if it has terminated.
     */
    public abstract boolean update(final double _time);
}
