/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.fxyz3d.geometry.Point3D;

import java.util.function.Function;

/**
 * @author Sean Phillips
 * Cheesy little JSONable version of Point3D without all the cruft
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class P3D extends MessageData {
    public static Function<Point3D, P3D> fxyzPoint3DToP3D =
        p -> new P3D(p.x, p.y, p.z);
    public static Function<P3D, Point3D> p3DToFxyzPoint3D =
        p -> new Point3D(p.x, p.y, p.z);

    public static final String TYPESTRING = "p3d";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {
        "x":1.0,
        "y":2.5,
        "z":9000.1
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private double x;
    private double y;
    private double z;
    //</editor-fold>

    public P3D() {
    }

    public P3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the x
     */
    public double getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * @return the z
     */
    public double getZ() {
        return z;
    }

    /**
     * @param z the z to set
     */
    public void setZ(double z) {
        this.z = z;
    }

    //</editor-fold>
}
