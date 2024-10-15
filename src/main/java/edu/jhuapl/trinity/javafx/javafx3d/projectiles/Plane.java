/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.javafx3d.projectiles;

import edu.jhuapl.trinity.utils.MathConstants;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.geometry.Vector3D;

/**
 * Representation of a plane in 3-space using the four component plane equation:
 *
 * <code>
 * aX + bY + cZ + d = 0
 * </code>.
 */
public final class Plane {
    /**
     * The x component of the plane normal.
     */
    public double a;
    /**
     * The y component of the plane normal.
     */
    public double b;
    /**
     * The z component of the plane normal.
     */
    public double c;
    /**
     * The distance component of the plane equation: aX + bY + cZ + d = 0.
     */
    public double d;

    /**
     * Creates a new plane object. The plane is invalid until parameters are set.
     */
    public Plane() {
        /*
         * Initialized to zero (invalid plane)
         */
    }

    /**
     * Defines a plane from the plane equation Ax + By + Cz + D = 0.
     *
     * @param _a The x component of the plane normal
     * @param _b The y component of the plane normal
     * @param _c The z component of the plane normal
     * @param _d The negative distance between the plane and the origin in the direction of the normal.
     */
    public Plane(final double _a, final double _b, final double _c, final double _d) {
        this.a = _a;
        this.b = _b;
        this.c = _c;
        this.d = _d;
    }

    /**
     * Copy constructor.
     *
     * @param _p A non-null plane
     */
    public Plane(final Plane _p) {
        this.a = _p.a;
        this.b = _p.b;
        this.c = _p.c;
        this.d = _p.d;
    }

    /**
     * Defines a plane from a point on the plane and the plane normal.
     *
     * @param point  A non-null point on the plane.
     * @param normal The normal vector of the plane.
     */
    public Plane(final Point3D point, final Point3D normal) {
        a = normal.x;
        b = normal.y;
        c = normal.z;
        d = -(normal.x * point.x + normal.y * point.y + normal.z * point.z);
    }

    /**
     * Creates a plane from three (non-collinear) points.
     *
     * @param p1 A point on the plane.
     * @param p2 A point on the plane.
     * @param p3 A point on the plane.
     */
    public Plane(final Point3D p1, final Point3D p2, final Point3D p3) {
        set(p1, p2, p3);
    }

    /**
     * Creates a new plane from three non-colinear points.
     *
     * @param x1 The x component of the first point.
     * @param y1 The y component of the first point.
     * @param z1 The z component of the first point.
     * @param x2 The x component of the second point.
     * @param y2 The y component of the second point.
     * @param z2 The z component of the second point.
     * @param x3 The x component of the third point.
     * @param y3 The y component of the third point.
     * @param z3 The z component of the third point.
     */
    public Plane(final double x1, final double y1, final double z1, final double x2, final double y2, final double z2,
                 final double x3, final double y3, final double z3) {
        set(new Point3D(x1, y1, z1), new Point3D(x2, y2, z2), new Point3D(x3, y3, z3));
    }

    /**
     * Copies parameters from the specified plane.
     *
     * @param _plane A non-null plane to copy.
     */
    public void set(final Plane _plane) {
        set(_plane.a, _plane.b, _plane.c, _plane.d);
    }

    /**
     * Defines a plane from the plane equation Ax + By + Cz + D = 0.
     *
     * @param _a The x component of the plane normal
     * @param _b The y component of the plane normal
     * @param _c The z component of the plane normal
     * @param _d The negative distance between the plane and the origin in the direction of the normal.
     */
    public void set(final double _a, final double _b, final double _c, final double _d) {
        a = _a;
        b = _b;
        c = _c;
        d = _d;
    }

    /**
     * Defines a plane from three (non-colinear) points.
     *
     * @param p1 A point on the plane.
     * @param p2 A point on the plane.
     * @param p3 A point on the plane.
     */
    public void set(Point3D p1, Point3D p2, Point3D p3) {
        Point3D normalPoint = p2.substract(p1);
        Point3D tempPoint = p3.substract(p1);
        Point3D normal = tempPoint.crossProduct(normalPoint);

        a = normal.x;
        b = normal.y;
        c = normal.z;
        d = -(a * p1.x) - (b * p1.y) - (c * p1.z);
    }

//   /**
//    * Transforms this Plane instance by the specified matrix.
//    *
//    * @param matrix
//    *           A non-null Matrix
//    */
//   public void transform(final Matrix4d matrix) {
//      /*
//       * Rotate the normal
//       */
//      final double nA = matrix.m00 * a + matrix.m01 * b + matrix.m02 * c;
//      final double nB = matrix.m10 * a + matrix.m11 * b + matrix.m12 * c;
//      final double nC = matrix.m20 * a + matrix.m21 * b + matrix.m22 * c;
//
//      /*
//       * Transform a point on the plane
//       */
//      final double pX = -d * (nA) + matrix.m03;
//      final double pY = -d * (nB) + matrix.m13;
//      final double pZ = -d * (nC) + matrix.m23;
//
//      a = nA;
//      b = nB;
//      c = nC;
//      d = -(pX * nA + pY * nB + pZ * nC);
//   }
//
//   /**
//    * Returns a new Plane instance representing this plane transformed by the specified matrix.
//    *
//    * @param matrix
//    *           A non-null Matrix
//    * @return A new Plane instance.
//    */
//   public Plane transformed(final Matrix4d matrix) {
//      final Plane result = new Plane(a, b, c, d);
//      result.transform(matrix);
//      return result;
//   }

    /**
     * Signed distance from the plane to the specified point.
     *
     * @param x X component of the point to find the distance to.
     * @param y Y component of the point to find the distance to.
     * @param z Z component of the point to find the distance to.
     * @return The shortest distance between the specified point and the plane. This number is positive if the point is
     * on the side of the plane that the normal points toward, 0 if the point is on the plane, or negative if the
     * point is on the opposite side of the plane. May return {@link Double#NaN} if the normal length is 0.
     */
    public double distance(final double x, final double y, final double z) {
        final double normalLenSq = a * a + b * b + c * c;
        /* If normal length is near 1.0, no need to normalize by length. */
        final double diff = normalLenSq - 1.0;
        if (diff < MathConstants.SMALL_VALUE && diff > -MathConstants.SMALL_VALUE) {
            return (x * a + y * b + z * c + d);
        }
        return (x * a + y * b + z * c + d) / Math.sqrt(normalLenSq);
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(a);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(b);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(c);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(d);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Plane other = (Plane) obj;
        if (Double.doubleToLongBits(a) != Double.doubleToLongBits(other.a))
            return false;
        if (Double.doubleToLongBits(b) != Double.doubleToLongBits(other.b))
            return false;
        if (Double.doubleToLongBits(c) != Double.doubleToLongBits(other.c))
            return false;
        if (Double.doubleToLongBits(d) != Double.doubleToLongBits(other.d))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Plane: " + a + "x + " + b + "y + " + c + "z + " + d + " = 0";
    }

    /**
     * Computes the distance from a specified point to a plane defined by its normal and a point on the plane.
     *
     * @param planeNormal The plane normal.
     * @param planePoint  A point on the plane.
     * @param x           The x component of the point to find the distance to.W
     * @param y           The y component of the point to find the distance to.W
     * @param z           The z component of the point to find the distance to.W
     * @return The smallest magnitude distance from the plane to the point. Positive values indicate the point is on the
     * positive side of the plane (where the normal points), and negative values indicate the opposite side.
     */
    public static final double distanceToPlane(final Vector3D planeNormal, final Point3D planePoint, final double x,
                                               final double y, final double z) {
        return ((planeNormal.x * (x - planePoint.x)) + (planeNormal.y * (y - planePoint.y)) + (planeNormal.z * (z - planePoint.z)));
    }

}
