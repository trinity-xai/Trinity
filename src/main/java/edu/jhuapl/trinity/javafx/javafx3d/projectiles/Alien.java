package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

import edu.jhuapl.trinity.javafx.javafx3d.animated.Opticon;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;

/**
 *
 * @author Sean Phillips
 */
public class Alien extends Opticon implements Hittable {
    private Point3D start = new Point3D(0, 0, 0);
    private Point3D location = new Point3D(0, 0, 0);
    private Point3D velocity = new Point3D(0, 0, 0);
    private boolean disappearsOnFlip = true;
   /**
    * Flag indicating whether the particle is active.
    */
    public SimpleBooleanProperty activeProperty = new SimpleBooleanProperty(true);
    
    public Alien(Color lightColor, double scannerBaseRadius) {
        super(lightColor, scannerBaseRadius);
        enableCycle(false);
        enableOrbiting(false);
        setScanning(false);        
    }
    public Point3D randomStart(double distance) {
        //Start the alien somewhere on the outskirts of play
        int directionX = random.nextBoolean() ? 1 : -1;
        int directionY = random.nextBoolean() ? 1 : -1;
        int directionZ = random.nextBoolean() ? 1 : -1;
        Point3D randomStart = new Point3D(
            directionX * (distance-50), 
            directionY * random.nextDouble() * (distance-50), 
            directionZ * random.nextDouble() * (distance-50));
        return randomStart;
    }
    /**
     * Reset transform to identity transform
     */
    public void reset() {
        velocity = Point3D.ZERO;
        location = Point3D.ZERO;
    }       
    @Override
    public void setStart(Point3D p3D) {
        start = p3D;
    }

    @Override
    public Point3D getStart() {
        return start;
    }

    @Override
    public void setLocation(Point3D p3D) {
        location = p3D;
        setTranslateX(location.getX());
        setTranslateY(location.getY());
        setTranslateZ(location.getZ());        
    }

    @Override
    public Point3D getLocation() {
        return location;
    }

    @Override
    public void setVelocity(Point3D p3D) {
        velocity = p3D;
    }

    @Override
    public Point3D getVelocity() {
        return velocity;
    }
    @Override
    public void flipCheck(double absSafetyPosition) {
        double bufferX = 50;
        double bufferY = 50;
        double bufferZ = 50;
        Point3D loc = getLocation();
        boolean flipped = false;
        if (loc.getX() < -absSafetyPosition) {
            setLocation(new Point3D(absSafetyPosition - bufferX, loc.getY(), loc.getZ()));
            flipped = true;
        }
        if (loc.getX() > absSafetyPosition) {
            setLocation(new Point3D(-absSafetyPosition + bufferX, loc.getY(), loc.getZ()));
            flipped = true;
        }
        if (loc.getY() < -absSafetyPosition) {
            setLocation(new Point3D(loc.getX(), absSafetyPosition - bufferY, loc.getZ()));
            flipped = true;
        }
        if (loc.getY() > absSafetyPosition) {
            setLocation(new Point3D(loc.getX(), -absSafetyPosition + bufferY, loc.getZ()));
            flipped = true;
        }
        if (loc.getZ() < -absSafetyPosition) {
            setLocation(new Point3D(loc.getX(), loc.getY(), absSafetyPosition - bufferZ));
            flipped = true;
        }
        if (loc.getZ() > absSafetyPosition) {
            setLocation(new Point3D(loc.getX(), loc.getY(), -absSafetyPosition + bufferZ));
            flipped = true;
        }
        activeProperty.set(!flipped);
    }
    @Override
    public boolean update(double _time) {
        location = location.add(velocity);
        setTranslateX(location.getX());
        setTranslateY(location.getY());
        setTranslateZ(location.getZ());
        return true;
    }

    /**
     * @return the disappearsOnFlip
     */
    public boolean isDisappearsOnFlip() {
        return disappearsOnFlip;
    }

    /**
     * @param disappearsOnFlip the disappearsOnFlip to set
     */
    public void setDisappearsOnFlip(boolean disappearsOnFlip) {
        this.disappearsOnFlip = disappearsOnFlip;
    }
}
