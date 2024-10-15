/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * @author Sean Phillips
 */
public class Dimensions {

    private SimpleDoubleProperty minX;
    private SimpleDoubleProperty minY;
    private SimpleDoubleProperty maxX;
    private SimpleDoubleProperty maxY;
    private SimpleDoubleProperty width;
    private SimpleDoubleProperty height;
    //To maintain aspect ratio (user option...default)
    private SimpleDoubleProperty square;
    //To center the points if square is used (see transform methods)
    private SimpleDoubleProperty squareOffset;

    public Dimensions() {

    }

    public Dimensions(double x1, double y1, double x2, double y2) {
        minX = new SimpleDoubleProperty(x1);
        minY = new SimpleDoubleProperty(y1);
        maxX = new SimpleDoubleProperty(x2);
        maxY = new SimpleDoubleProperty(y2);
        width = new SimpleDoubleProperty(x2 - x1);
        height = new SimpleDoubleProperty(y2 - y1);
        width.bind(maxX.subtract(minX));
        height.bind(maxY.subtract(minY));

        square = new SimpleDoubleProperty(width.get());
        squareOffset = new SimpleDoubleProperty((width.get() - height.get()) / 2.0);
        updateSquare();
        width.addListener((ov, t, t1) -> updateSquare());
        height.addListener((ov, t, t1) -> updateSquare());

    }

    private void updateSquare() {
        //Square off the screen rendering based on the shortest dimension.
        //This will inherently create a view smaller than the canvas
        square.set(width.get());
        squareOffset.set((width.get() - height.get()) / 2.0);
        if (width.get() < height.get()) {
            square.set(height.get());
            squareOffset.set(0.0);
        }
    }

    public ReadOnlyDoubleProperty widthProperty() {
        return width;
    }

    public ReadOnlyDoubleProperty heightProperty() {
        return height;
    }

    public double getWidth() {
        return width.get();
    }

    public double getHeight() {
        return height.get();
    }

    public double getSquare() {
        return square.get();
    }

    public double getSquareOffset() {
        return squareOffset.get();
    }

    /**
     * @return the minX
     */
    public SimpleDoubleProperty getMinX() {
        return minX;
    }

    /**
     * @param minX the minX to set
     */
    public void setMinX(SimpleDoubleProperty minX) {
        this.minX = minX;
    }

    /**
     * @return the minY
     */
    public SimpleDoubleProperty getMinY() {
        return minY;
    }

    /**
     * @param minY the minY to set
     */
    public void setMinY(SimpleDoubleProperty minY) {
        this.minY = minY;
    }

    /**
     * @return the maxX
     */
    public SimpleDoubleProperty getMaxX() {
        return maxX;
    }

    /**
     * @param maxX the maxX to set
     */
    public void setMaxX(SimpleDoubleProperty maxX) {
        this.maxX = maxX;
    }

    /**
     * @return the maxY
     */
    public SimpleDoubleProperty getMaxY() {
        return maxY;
    }

    /**
     * @param maxY the maxY to set
     */
    public void setMaxY(SimpleDoubleProperty maxY) {
        this.maxY = maxY;
    }

}
