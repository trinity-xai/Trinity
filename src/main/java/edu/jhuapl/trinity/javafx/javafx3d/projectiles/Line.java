package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

/*-
 * #%L
 * trinity-2024.06.03
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

import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.geometry.Vector3D;

/**
 * Geometric line representation for intersection testing.
 */
public class Line {

    public static final double smallDiff = 1.0E-11;
    /**
     * A point on the line.
     */
    public Point3D p; // point
    /**
     * The line direction (note the line extends in both positive and negative
     * multiples of this vector).
     */
    public Vector3D v; // direction from p

    /**
     * Creates a new line.
     */
    public Line() {
        p = new Point3D(0, 0, 0);
        v = new Vector3D(0, 1, 0);
    }

    /**
     * Creates a new line initialized with the specified point on the line, and
     * direction vector.
     *
     * @param _point A point on the line.
     * @param _vector The direction of the line.
     */
    public Line(final Point3D _point, final Vector3D _vector) {
        p = _point;
        v = _vector;
    }

    /**
     * Creates a new line from the specified point and direction.
     *
     * @param _x The x component of a point on the line.
     * @param _y The y component of a point on the line.
     * @param _z The z component of a point on the line.
     * @param _dx The x component of the line direction.
     * @param _dy The y component of the line direction.
     * @param _dz The z component of the line direction.
     */
    public Line(final double _x, final double _y, final double _z, final double _dx, final double _dy, final double _dz) {
        p = new Point3D(_x, _y, _z);
        v = new Vector3D(_dx, _dy, _dz);
    }

    /**
     * Copies parameters from the specified line.
     *
     * @param _line The line to copy parameters from.
     */
    public void set(final Line _line) {
        p.x = _line.p.getX();
        p.y = _line.p.getY();
        p.z = _line.p.getZ();
        v.x = _line.v.x;
        v.y = _line.v.y;
        v.z = _line.v.z;
    }

    /**
     * Sets the parameters of the line.
     *
     * @param _x The x component of a point on the line.
     * @param _y The y component of a point on the line.
     * @param _z The z component of a point on the line.
     * @param _dx The x component of the line direction.
     * @param _dy The y component of the line direction.
     * @param _dz The z component of the line direction.
     */
    public void set(final float _x, final float _y, final float _z, final double _dx, final double _dy,
            final double _dz) {
        p.x = _x;
        p.y = _y;
        p.z = _z;
        v.x = _dx;
        v.y = _dy;
        v.z = _dz;
    }

    public boolean intersects(Point3D point) {
        Vector3D lineToGivenPoint = new Vector3D(
                point.x - p.x, point.y - p.y, point.z - p.z);
        Vector3D normalizedV = new Vector3D(v.x, v.y, v.z);
        normalizedV.normalize();

        double parm = lineToGivenPoint.x * normalizedV.x
                + lineToGivenPoint.y * normalizedV.y
                + lineToGivenPoint.z * normalizedV.z;
        
        Vector3D tempVector = new Vector3D(
            lineToGivenPoint.x - parm * normalizedV.x,
            lineToGivenPoint.y - parm * normalizedV.y,
            lineToGivenPoint.z - parm * normalizedV.z);
        double dist = tempVector.magnitude(); //length
        //if less than an epsilon difference its "close enough"
        return dist < smallDiff;
    }
    //Is the point between the other two points when projected
    public static boolean between(javafx.geometry.Point3D p1,
        javafx.geometry.Point3D p2, javafx.geometry.Point3D p3) {
        //points p1=point1 and p2=point2, and your third point being p3=currPoint:
        //v1 = p2 - p1
        javafx.geometry.Point3D v1 = p2.subtract(p1);
        //v2 = p3 - p1
        javafx.geometry.Point3D  v2 = p3.subtract(p1);
        //v3 = p3 - p2
        javafx.geometry.Point3D  v3 = p3.subtract(p2);
        double v2DotProduct = v2.dotProduct(v1);
        double v3DotProduct = v3.dotProduct(v1);
        //if (dot(v2,v1)>0 and dot(v3,v1)<0) return between
        return (v2DotProduct > smallDiff && v3DotProduct < smallDiff) 
            || (v2DotProduct == 0 && v3DotProduct == 0);
        //else return not between
    }
    //is the point ON the line segment between p1 and p2 within a small epsilon difference
    public static boolean onLine(javafx.geometry.Point3D p1,
        javafx.geometry.Point3D p2, javafx.geometry.Point3D p3) {
        //points p1=point1 and p2=point2, and your third point being p3=currPoint:
        //v1 = normalize(p2 - p1)
        javafx.geometry.Point3D v1 = p2.subtract(p1).normalize();
        //v3 = p3 - p2
        javafx.geometry.Point3D  v2 = p3.subtract(p1).normalize();
        //v3 = p3 - p2
        javafx.geometry.Point3D  v3 = p3.subtract(p2);
        //if (fabs(dot(v2,v1)-1.0)<EPS and dot(v3,v1)<0) return between
        return (Math.abs(v2.dotProduct(v1)-1.0) < smallDiff) 
            && v3.dotProduct(v1) < 0; //else return not between   
    }

    @Override
    public String toString() {
        return "Line:" + p + " Direction: " + v;
    }
}
