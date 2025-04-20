package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

import javafx.geometry.Point3D;

import java.util.Random;

public interface Hittable {
    /**
     * Random instance for random behavior.
     */
    static Random random = new Random();

    public static Point3D getRandomPoint3D(float lowerBound, float upperBound) {
        return new Point3D(
            random.nextFloat() * (upperBound - lowerBound) + lowerBound,
            random.nextFloat() * (upperBound - lowerBound) + lowerBound,
            random.nextFloat() * (upperBound - lowerBound) + lowerBound
        );
    }

    /**
     * physics of the hittable.
     */
    public void setStart(Point3D p3D);

    public Point3D getStart();

    public void setLocation(Point3D p3D);

    public Point3D getLocation();

    public void setVelocity(Point3D p3D);

    public Point3D getVelocity();

    public void flipCheck(double absSafetyPosition);

    /**
     * Updates the particle animation by the specified number of milliseconds.
     *
     * @param _time The number of milliseconds elapsed since the last update.
     * @return true if the particle is still 'alive' (requires further animation), false if it has terminated.
     */
    public abstract boolean update(final double _time);
}
