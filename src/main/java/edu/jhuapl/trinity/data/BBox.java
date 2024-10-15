/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.data;

import java.util.List;

/**
 * @author Sean Phillips
 */
public class BBox {
    private double x1;
    private double y1;
    private double x2;
    private double y2;

    public BBox(List<Double> bboxList) {
        x1 = bboxList.get(0);
        y1 = bboxList.get(1);
        x2 = bboxList.get(2);
        y2 = bboxList.get(3);
    }

    public double getWidth() {
        return x2 - x1;
    }

    public double getHeight() {
        return y2 - y1;
    }

    /**
     * @return the x1
     */
    public double getX1() {
        return x1;
    }

    /**
     * @param x1 the x1 to set
     */
    public void setX1(double x1) {
        this.x1 = x1;
    }

    /**
     * @return the y1
     */
    public double getY1() {
        return y1;
    }

    /**
     * @param y1 the y1 to set
     */
    public void setY1(double y1) {
        this.y1 = y1;
    }

    /**
     * @return the x2
     */
    public double getX2() {
        return x2;
    }

    /**
     * @param x2 the x2 to set
     */
    public void setX2(double x2) {
        this.x2 = x2;
    }

    /**
     * @return the y2
     */
    public double getY2() {
        return y2;
    }

    /**
     * @param y2 the y2 to set
     */
    public void setY2(double y2) {
        this.y2 = y2;
    }
}
