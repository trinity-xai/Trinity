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

import java.util.Collection;

/**
 * The AABoundingBox of 3d model.
 */
public class BoundingBox extends Box {

    public BoundingBox(Box box) {
        this(box.minX(), box.maxX(), box.minY(), box.maxY(), box.minZ(), box.maxZ());
    }

    public BoundingBox() {
        this(0, 1, 0, 1, 0, 1);
    }

    public BoundingBox(double minx, double maxx, double miny, double maxy, double minz, double maxz) {
        if (minx > maxx) {
            throw new IllegalArgumentException("minX larger than maxX");
        }
        if (miny > maxy) {
            throw new IllegalArgumentException("minY larger than maxY");
        }
        if (minz > maxz) {
            throw new IllegalArgumentException("minZ larger than maxZ");
        }
        center = new Point3D((minx + maxx) / 2, (miny + maxy) / 2, (minz + maxz) / 2);
        // use max y - min y, in case that maxY is +Inf, minY is constant
        // do not use maxx - center.x, maxy - center.y ...
        xExtent = (maxx - minx) / 2;
        yExtent = (maxy - miny) / 2;
        zExtent = (maxz - minz) / 2;
    }

    /**
     * construct an empty bounding box
     *
     * @return return an bounding box whose size is zero
     */
    private static BoundingBox empty() {
        return new BoundingBox(0, 0, 0, 0, 0, 0);
    }

    /**
     * Obtain the bounding box of a collection of 3d points.
     *
     * @param data the collection of 3d points, Not null.
     *             - if this collection has no points, return a box whose size is zero.
     *             - if this collection has NaN points, the NaN points are ignored.
     *             - if this collection has Infinite points, the Infinite points are ignored.
     * @return the bounding box
     */
    public static BoundingBox of(Collection<Point3D> data) {
        if (data.size() < 1) return empty();
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Point3D p : data) {
            if (!VolumeUtils.validPoint(p)) continue;
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
        }
        if (Double.isInfinite(minX)) return empty();
        return new BoundingBox(minX, maxX, minY, maxY, minZ, maxZ);
    }

    /**
     * get the length of the diagonal line of the bounding box
     *
     * @return the length of diagonal
     */
    public double diagonalLength() {
        return 2 * Math.sqrt(xExtent * xExtent + yExtent * yExtent + zExtent * zExtent);
    }
}
