package edu.jhuapl.trinity.javafx.javafx3d.animated;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
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
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Sean Phillips
 */
public class FastScatter3D extends Group {
    public static int DEFAULT_POINT_COUNT = 1000;
    public static double DEFAULT_RADIUS = 1.0;
    public static int DEFAULT_DIVISIONS = 16;
    private int pointCount = DEFAULT_POINT_COUNT;
    private double radius = DEFAULT_RADIUS;
    private int divisions = DEFAULT_DIVISIONS;
    private Point3D centerPoint = Point3D.ZERO;
    ArrayList<Boolean> visible;
    ArrayList<Sphere> points;

    public FastScatter3D(int pointCount, double radius, int divisions) {
        this.pointCount = pointCount;
        this.radius = radius;
        this.divisions = divisions;
        points = new ArrayList<>(pointCount);
        visible = new ArrayList<>(pointCount);
        regeneratePoints();
        //updateVisibility(); //no need to update vis in constructor...
    }

    public FastScatter3D() {
        this(DEFAULT_POINT_COUNT, DEFAULT_RADIUS, DEFAULT_DIVISIONS);
    }

    public void setColorByIndex(int index, Color color) {
        if (index <= points.size()) {
            ((PhongMaterial) points.get(index).getMaterial()).setDiffuseColor(color);
        }
    }

    public void setColorList(ArrayList<Color> colors) {
        for (int i = 0; i < colors.size() && i < points.size(); i++) {
            ((PhongMaterial) points.get(i).getMaterial())
                .setDiffuseColor(colors.get(i));
        }
    }

    public void setAllVisible(Boolean isVisible) {
        Collections.fill(visible, isVisible);
        updateVisibility();
    }

    public void setVisibleByIndex(int index, Boolean isVisible) {
        if (index <= visible.size()) {
            visible.set(index, isVisible);
            if (index <= points.size()) {
                points.get(index).setVisible(isVisible);
            }
        }
    }

    public void setVisibileList(ArrayList<Boolean> newVisible) {
        visible.clear();
        visible.addAll(newVisible);
        updateVisibility();
    }

    private void updateVisibility() {
        for (int i = 0; i < visible.size() && i < points.size(); i++) {
            points.get(i).setVisible(visible.get(i));
        }
    }

    private Sphere createPoint() {
        Sphere sphere = new Sphere(getRadius(), getDivisions());
        sphere.setVisible(false);
        PhongMaterial mat = new PhongMaterial(Color.ALICEBLUE);
        sphere.setMaterial(mat);
        return sphere;
    }

    private void regeneratePoints() {
        getChildren().clear();
        points.clear();
        visible.clear();
        for (int i = 0; i < getPointCount(); i++) {
            points.add(createPoint());
            visible.add(false);
        }
        getChildren().addAll(points);
    }

    private void updatePointCount() {
        if (pointCount > points.size()) {
            for (int i = points.size(); i < pointCount; i++) {
                Sphere sphere = createPoint();
                points.add(sphere);
                getChildren().add(sphere);
                visible.add(false);
            }
        } else {
            for (int i = pointCount; i < points.size(); i++) {
                getChildren().remove(points.get(i));
                points.remove(i);
                visible.remove(i);
            }
        }
    }

    public void updatePositionByIndex(int index, Point3D position) {
        if (index <= points.size()) {
            points.get(index).setTranslateX(position.getX());
            points.get(index).setTranslateY(position.getY());
            points.get(index).setTranslateZ(position.getZ());
        }
    }

    public void updatePositionsList(ArrayList<Point3D> newPositions) {
        Sphere sphere;
        Point3D point3D;
        for (int i = 0; i < pointCount && i < newPositions.size(); i++) {
            sphere = points.get(i);
            point3D = newPositions.get(i);
            sphere.setTranslateX(point3D.getX());
            sphere.setTranslateY(point3D.getY());
            sphere.setTranslateZ(point3D.getZ());
        }
    }

    /**
     * @return the pointCount
     */
    public int getPointCount() {
        return pointCount;
    }

    /**
     * @param pointCount the pointCount to set
     */
    public void setPointCount(int pointCount) {
        this.pointCount = pointCount;
        updatePointCount();
    }

    /**
     * @return the centerPoint
     */
    public Point3D getCenterPoint() {
        return centerPoint;
    }

    /**
     * @param centerPoint the centerPoint to set
     */
    public void setCenterPoint(Point3D centerPoint) {
        this.centerPoint = centerPoint;
        setTranslateX(centerPoint.getX());
        setTranslateY(centerPoint.getY());
        setTranslateZ(centerPoint.getZ());
    }

    /**
     * @return the radius
     */
    public double getRadius() {
        return radius;
    }

    /**
     * @param radius the radius to set
     */
    public void setRadius(double radius) {
        this.radius = radius;
        for (Sphere sphere : points) {
            sphere.setRadius(radius);
        }
    }

    /**
     * @return the divisions
     */
    public int getDivisions() {
        return divisions;
    }

    /**
     * @param divisions the divisions to set
     */
    public void setDivisions(int divisions) {
        this.divisions = divisions;
    }
}
