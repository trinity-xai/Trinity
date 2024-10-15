/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.volumetric;

import javafx.geometry.Point3D;
import org.fxyz3d.geometry.Vector3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sean Phillips
 */
public enum VolumeUtils {
    INSTANCE;

    /**
     * This class enumerates different ways that two boxes(cell) are adjacent to each other
     */
    public enum Adjacency {
        /**
         * the two boxes has one common face
         **/
        FACE,
        /**
         * the two boxes has at least one common edge
         **/
        EDGE,
        /**
         * the two boxes has at least one common vertex
         **/
        VERTEX
    }

    public static boolean validPoint(Point3D point) {
        return (!(Double.isNaN(point.getX())) || Double.isNaN(point.getY()) || Double.isNaN(point.getZ())) &&
            !(Double.isInfinite(point.getX()) || Double.isInfinite(point.getY()) || Double.isInfinite(point.getZ()));
    }

    public static List<Integer> organizeEdges(List<Integer> edges, List<Point3D> positions) {
        var visited = new HashMap<Integer, Boolean>();
        var edgeList = new ArrayList<Integer>();
        var resultList = new ArrayList<Integer>();
        var nextIndex = -1;
        while (resultList.size() < edges.size()) {
            if (nextIndex < 0) {
                for (int i = 0; i < edges.size(); i += 2) {
                    if (!visited.containsKey(i)) {
                        nextIndex = edges.get(i);
                        break;
                    }
                }
            }

            for (int i = 0; i < edges.size(); i += 2) {
                if (visited.containsKey(i)) {
                    continue;
                }

                int j = i + 1;
                int k = -1;
                if (edges.get(i) == nextIndex) {
                    k = j;
                } else if (edges.get(j) == nextIndex) {
                    k = i;
                }

                if (k >= 0) {
                    var edge = edges.get(k);
                    visited.put(i, true);
                    edgeList.add(nextIndex);
                    edgeList.add(edge);
                    nextIndex = edge;
                    i = 0;
                }
            }

            // calculate winding order - then add to final result.
            var borderPoints = new ArrayList<Point3D>();
//            edgeList.ForEach(ei => borderPoints.Add(positions[ei]));
            edgeList.forEach(ei -> borderPoints.add(positions.get(ei)));

            var winding = calculateWindingOrder(borderPoints);
            if (winding > 0) {
                edgeList.reversed();
            }

            resultList.addAll(edgeList);
            edgeList = new ArrayList<>();
            nextIndex = -1;
        }

        return resultList;
    }

    /// <summary>
    /// returns 1 for CW, -1 for CCW, 0 for unknown.
    /// </summary>
    public static int calculateWindingOrder(ArrayList<Point3D> points) {
        // the sign of the 'area' of the polygon is all we are interested in.
        var area = calculateSignedArea(points);
        if (area < 0.0)
            return 1;
        else if (area > 0.0)
            return -1;
        return 0; // error condition - not even verts to calculate, non-simple poly, etc.
    }

    public static double calculateSignedArea(ArrayList<Point3D> points) {
        double area = 0.0;
        for (int i = 0; i < points.size(); i++) {
            int j = (i + 1) % points.size();
            area += points.get(i).getX() * points.get(j).getY();
            area -= points.get(i).getY() * points.get(j).getX();
        }
        area /= 2.0f;

        return area;
    }

    /**
     * test if an AABBox intersects with a sphere
     *
     * @param box    the AABBox
     * @param sphere the sphere
     * @return test result
     */
    public static boolean intersect(Box box, Sphere sphere) {
        Point3D c1 = box.getCenter();
        Point3D c2 = sphere.getCenter();
        Vector3D hemiDiagonal = new Vector3D(box.getxExtent(), box.getyExtent(), box.getzExtent());
        Vector3D c1c2 = new Vector3D(c2.getX() - c1.getX(),
            c2.getY() - c1.getY(), c2.getZ() - c1.getZ());
        absoluteVector(c1c2);
        c1c2.sub(hemiDiagonal);
        Vector3D distance = new Vector3D(max(0, c1c2.x), max(0, c1c2.y), max(0, c1c2.z));
        return distance.dotProduct(distance) <= sphere.getRadius() * sphere.getRadius();
    }

    /**
     * test if an AABBox contains a sphere
     *
     * @param box    the AABBox
     * @param sphere the sphere
     * @return test result
     */
    public static boolean contains(Box box, Sphere sphere) {
        Point3D c1 = box.getCenter();
        Point3D c2 = sphere.getCenter();
        Vector3D c1c2 = new Vector3D(c2.getX() - c1.getX(),
            c2.getY() - c1.getY(), c2.getZ() - c1.getZ());
        absoluteVector(c1c2);
        return c1c2.x + sphere.getRadius() <= box.getxExtent() &&
            c1c2.y + sphere.getRadius() <= box.getyExtent() &&
            c1c2.z + sphere.getRadius() <= box.getzExtent();
    }

    /**
     * test if a sphere contains a box
     *
     * @param sphere the sphere
     * @param box    the box
     * @return test result
     */
    public static boolean contains(Sphere sphere, Box box) {
        Point3D c1 = box.getCenter();
        Point3D c2 = sphere.getCenter();
        Vector3D c1c2 = new Vector3D(c2.getX() - c1.getX(),
            c2.getY() - c1.getY(), c2.getZ() - c1.getZ());
        absoluteVector(c1c2);
        Vector3D hemiDiagonal = new Vector3D(box.getxExtent(), box.getyExtent(), box.getzExtent());
        c1c2.add(hemiDiagonal);
        return c1c2.magnitude() <= sphere.getRadius();
    }

    public static void absoluteVector(Vector3D vector3D) {
        vector3D.setValues(Math.abs(vector3D.getX()),
            Math.abs(vector3D.getY()), Math.abs(vector3D.getZ()));
    }

    public static int find(String[] strs, String target) {
        for (int i = 0; i < strs.length; i++) {
            if (strs[i].equals(target)) {
                return i;
            }
        }
        return -1;
    }

    public static void reverse(int[] array) {
        if (array.length < 1) return;
        int left = 0, right = array.length - 1;
        while (left < right) {
            int temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left += 1;
            right -= 1;
        }
    }

    public static List<Point3D> arrayList2VecList(List<float[]> data) {
        List<Point3D> result = new ArrayList<>();
        for (float[] point : data) {
            result.add(new Point3D(point[0], point[1], point[2]));
        }
        return result;
    }

    public static ArrayList<Integer> incrementalIntegerList(int n) {
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
        return list;
    }

    public static double max(double... ds) {
        double result = ds[0];
        for (double d : ds) {
            result = Math.max(d, result);
        }
        return result;
    }

    public static double min(double... ds) {
        double result = ds[0];
        for (double d : ds) {
            result = Math.min(d, result);
        }
        return result;
    }
}
