package edu.jhuapl.trinity.javafx.javafx3d;

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
import org.fxyz3d.shapes.composites.PolyLine3D;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class Crosshair3D extends Group {

    public double size;
    public Point3D centerPoint;
    public float lineWidth;
    public PolyLine3D xPositivePoly;
    public PolyLine3D xNegativePoly;
    public PolyLine3D yPositivePoly;
    public PolyLine3D yNegativePoly;
    public PolyLine3D zPositivePoly;
    public PolyLine3D zNegativePoly;
    public Color xPositiveColor = Color.ALICEBLUE;
    public Color xNegativeColor = Color.ALICEBLUE;
    public Color yPositiveColor = Color.ALICEBLUE;
    public Color yNegativeColor = Color.ALICEBLUE;
    public Color zPositiveColor = Color.ALICEBLUE;
    public Color zNegativeColor = Color.ALICEBLUE;

    public Crosshair3D(Point3D centerPoint, double size, float lineWidth) {
        this.centerPoint = centerPoint;
        this.size = size;
        this.lineWidth = lineWidth;

        setCenter(centerPoint);
    }

    public void setCenter(Point3D newCenter) {
        this.centerPoint = newCenter;
        //remove the current polylines so they detach and get GC'd
        getChildren().clear();
        //create set of polylines that create a 3D crosshair
        float half = Double.valueOf(size / 2.0).floatValue();
        float startPointX = Double.valueOf(centerPoint.getX()).floatValue();
        float startPointY = Double.valueOf(centerPoint.getY()).floatValue();
        float startPointZ = Double.valueOf(centerPoint.getZ()).floatValue();

        //x Axis - Positive direction from centerPoint
        List<org.fxyz3d.geometry.Point3D> xPositiveData = new ArrayList<>();
        xPositiveData.add(new org.fxyz3d.geometry.Point3D(startPointX, startPointY, startPointZ));
        xPositiveData.add(new org.fxyz3d.geometry.Point3D(startPointX + half, startPointY, startPointZ));
        xPositivePoly = new PolyLine3D(xPositiveData, lineWidth, xPositiveColor, PolyLine3D.LineType.TRIANGLE);
        //x Axis - Negative direction from centerPoint
        List<org.fxyz3d.geometry.Point3D> xNegativeData = new ArrayList<>();
        xNegativeData.add(new org.fxyz3d.geometry.Point3D(startPointX, startPointY, startPointZ));
        xNegativeData.add(new org.fxyz3d.geometry.Point3D(startPointX - half, startPointY, startPointZ));
        xNegativePoly = new PolyLine3D(xNegativeData, lineWidth, xNegativeColor, PolyLine3D.LineType.TRIANGLE);

        //y Axis - Positive direction from centerPoint
        List<org.fxyz3d.geometry.Point3D> yPositiveData = new ArrayList<>();
        yPositiveData.add(new org.fxyz3d.geometry.Point3D(startPointX, startPointY, startPointZ));
        yPositiveData.add(new org.fxyz3d.geometry.Point3D(startPointX, startPointY + half, startPointZ));
        yPositivePoly = new PolyLine3D(yPositiveData, lineWidth, yPositiveColor, PolyLine3D.LineType.TRIANGLE);
        //y Axis - Negative direction from centerPoint
        List<org.fxyz3d.geometry.Point3D> yNegativeData = new ArrayList<>();
        yNegativeData.add(new org.fxyz3d.geometry.Point3D(startPointX, startPointY, startPointZ));
        yNegativeData.add(new org.fxyz3d.geometry.Point3D(startPointX, startPointY - half, startPointZ));
        yNegativePoly = new PolyLine3D(yNegativeData, lineWidth, yNegativeColor, PolyLine3D.LineType.TRIANGLE);

        //z Axis - Positive direction from centerPoint
        List<org.fxyz3d.geometry.Point3D> zPositiveData = new ArrayList<>();
        zPositiveData.add(new org.fxyz3d.geometry.Point3D(startPointX, startPointY, startPointZ));
        zPositiveData.add(new org.fxyz3d.geometry.Point3D(startPointX, startPointY, startPointZ + half));
        zPositivePoly = new PolyLine3D(zPositiveData, lineWidth, zPositiveColor, PolyLine3D.LineType.TRIANGLE);
        //z Axis - Negative direction from centerPoint
        List<org.fxyz3d.geometry.Point3D> zNegativeData = new ArrayList<>();
        zNegativeData.add(new org.fxyz3d.geometry.Point3D(startPointX, startPointY, startPointZ));
        zNegativeData.add(new org.fxyz3d.geometry.Point3D(startPointX, startPointY, startPointZ - half));
        zNegativePoly = new PolyLine3D(zNegativeData, lineWidth, zNegativeColor, PolyLine3D.LineType.TRIANGLE);
        getChildren().addAll(
            xPositivePoly, xNegativePoly,
            yPositivePoly, yNegativePoly,
            zPositivePoly, zNegativePoly);
    }
}
