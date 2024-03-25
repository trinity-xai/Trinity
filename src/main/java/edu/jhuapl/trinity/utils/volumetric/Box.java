package edu.jhuapl.trinity.utils.volumetric;

/*-
 * #%L
 * trinity-2024.03.11
 * %%
 * Copyright (C) 2021 - 2024 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import javafx.geometry.Point3D;

/**
 * an Axis-Align Bounding Box
 */
public class Box {

    protected Point3D center;

    protected double xExtent;

    protected double yExtent;

    protected double zExtent;

    public Box() {
        center = Point3D.ZERO;
        xExtent = 1d;
        yExtent = 1d;
        zExtent = 1d;
    }

    /**
     * the constructor of Box
     * @throws IllegalArgumentException x/z/z extent is NaN or negative
     * @param center the center of box, the x/y/z cannot be NaN
     * @param xAxis the x-extent of box, cannot be NaN, must be positive
     * @param yAxis the y-extent of box, cannot be NaN, must be positive
     * @param zAxis the z-extent of box, cannot be NaN, must be positive
     */
    public Box(Point3D center, double xAxis, double yAxis, double zAxis) {
        if (Double.isNaN(center.getX())
                || Double.isNaN(center.getY())
                || Double.isNaN(center.getZ())
                || Double.isNaN(xAxis)
                || Double.isNaN(yAxis)
                || Double.isNaN(zAxis)) {
            throw new IllegalArgumentException("The param cannot be Double.NaN");
        }
        if (xAxis < 0 || yAxis < 0 || zAxis < 0) throw new IllegalArgumentException("The x/y/z extent of box should be positive");
        this.center = center;
        this.xExtent = xAxis;
        this.yExtent = yAxis;
        this.zExtent = zAxis;
    }

    public Point3D getCenter() {
        return center;
    }

    public void setCenter(Point3D center) {
        this.center = center;
    }

    public double getxExtent() {
        return xExtent;
    }

    public void setxExtent(double xExtent) {
        this.xExtent = xExtent;
    }

    public double getyExtent() {
        return yExtent;
    }

    public void setyExtent(double yExtent) {
        this.yExtent = yExtent;
    }

    public double getzExtent() {
        return zExtent;
    }

    public void setzExtent(double zExtent) {
        this.zExtent = zExtent;
    }

    public double maxX() {
        return center.getX() + xExtent;
    }

    public double minX() {
        return center.getX() - xExtent;
    }

    public double maxY() {
        return center.getY() + yExtent;
    }

    public double minY() {
        return center.getY() - yExtent;
    }

    public double maxZ() {
        return center.getZ() + zExtent;
    }

    public double minZ() {
        return center.getZ() - zExtent;
    }

    private static final double TOLERANCE = 1E-5;

    /**
     * test if this box contains the specified point
     * @param point the user-specified point
     * @return test result
     */
    public boolean contains(Point3D point) {
        return contains(point, TOLERANCE);
    }

    /**
     * test if this box contains the specified point
     * @param point the user-specified point
     * @param tolerance the error within the tolerance is acceptable
     * @return test result
     */
    public boolean contains(Point3D point, double tolerance) {
        return (point.getX() <= tolerance + xExtent + center.getX())
                && (point.getX() >= center.getX() - xExtent - tolerance)
                && (point.getY() <= center.getY() + yExtent + tolerance)
                && (point.getY() >= center.getY() - yExtent - tolerance)
                && (point.getZ() <= center.getZ() + zExtent + tolerance)
                && (point.getZ() >= center.getY() - zExtent - tolerance);
    }

}
