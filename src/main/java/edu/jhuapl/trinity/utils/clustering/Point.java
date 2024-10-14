/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.clustering;

/**
 * An N-dimensional point.
 */
public class Point {

    protected double[] position;
    protected int dimensions;

    public Point(int size) {
        position = new double[size];
        this.dimensions = size;
    }

    public Point(double... p) {
        this.position = p;
        this.dimensions = position.length;
    }


    public int getDimensions() {
        return this.dimensions;
    }

    public double[] getPosition() {
        return position;
    }

    public double average() {
        double average = 0;
        for (int i = 0; i < getPosition().length; i++) {
            average += getPosition()[i];
        }
        return average / getPosition().length;
    }

    /**
     * Calculates the euclidean distance between this point and the other point.
     * Assumes the dimensions of each point are the same (this is not checked).
     *
     * @param o other point
     * @return
     */
    double dist(Point o) {
        double sum = 0.0;
        for (int i = 0; i < position.length; ++i) {
            final double d = position[i] - o.position[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    double distSquared(Point o) {
        double sum = 0.0;
        for (int i = 0; i < position.length; ++i) {
            final double d = position[i] - o.position[i];
            sum += d * d;
        }
        return sum;
    }

    @Override
    public boolean equals(Object o) {
        Point p = (Point) o;
        if (this.dimensions != p.dimensions) {
            return false;
        }
        for (int i = 0; i < this.dimensions; i++) {
            if (this.position[i] != p.position[i]) {
                return false;
            }
        }
        return true;
    }
}
