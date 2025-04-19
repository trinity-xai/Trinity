package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.geometry.Point2D;

/**
 * @author Sean Phillips
 */
public class Vert3D extends Point2D {
    public Integer xIndex = null;
    public Integer yIndex = null;

    public Vert3D(double d1, double d2, int xIndex, int yIndex) {
        super(d1, d2);
        this.xIndex = xIndex;
        this.yIndex = yIndex;
    }
}
