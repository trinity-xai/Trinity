package edu.jhuapl.trinity.javafx.javafx3d;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
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

import org.fxyz3d.geometry.Point3D;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Sean Phillips
 */
public class DirectedScatterDataModel {
    public ArrayList<Perspective3DNode> pNodes;
    public ArrayList<Point3D> data;
    public ArrayList<Point3D> endPoints;
    public Double nodeMaxX;
    public Double nodeMinX;
    public Double nodeRangeX;
    public Double nodeMaxY;
    public Double nodeMinY;
    public Double nodeRangeY;
    public Double nodeMaxZ;
    public Double nodeMinZ;
    public Double nodeRangeZ;
    public Double directMaxX;
    public Double directMinX;
    public Double directRangeX;
    public Double directMaxY;
    public Double directMinY;
    public Double directRangeY;
    public Double directMaxZ;
    public Double directMinZ;
    public Double directRangeZ;
    public Double totalMinX;
    public Double totalMaxX;
    public Double totalRangeX;
    public Double totalMinY;
    public Double totalMaxY;
    public Double totalRangeY;
    public Double totalMinZ;
    public Double totalMaxZ;
    public Double totalRangeZ;
    public boolean useTotalPositioning = true;
    public boolean reflectY = true;
    public boolean ignoreVisibility = false;
    public Double xShift = 0.0;
    public Double yShift = 0.0;
    public Double zShift = 0.0;
    public double pointScale = 1.0;

    public DirectedScatterDataModel() {
        pNodes = new ArrayList<>();
        data = new ArrayList<>();
        endPoints = new ArrayList<>();
        nodeMaxX = null;
        nodeMinX = null;
        nodeRangeX = null;
        nodeMaxY = null;
        nodeMinY = null;
        nodeRangeY = null;
        nodeMaxZ = null;
        nodeMinZ = null;
        nodeRangeZ = null;
        directMaxX = null;
        directMinX = null;
        directRangeX = null;
        directMaxY = null;
        directMinY = null;
        directRangeY = null;
        directMaxZ = null;
        directMinZ = null;
        directRangeZ = null;
    }

    public void addNode(Perspective3DNode pNode) {
        //update the list of all perspective nodes
        pNodes.add(pNode);
        //Its actually quicker to add them brute force and then just quick sort later
        //This facilitates binarySearch() based on converted screen coordinates
        //update coordinate transformation information for X
        nodeMaxX = null == nodeMaxX || pNode.xCoord > nodeMaxX ? pNode.xCoord : nodeMaxX;
        nodeMinX = null == nodeMinX || pNode.xCoord < nodeMinX ? pNode.xCoord : nodeMinX;
        nodeRangeX = nodeMaxX - nodeMinX;
        //update coordinate transformation information for Y
        nodeMaxY = null == nodeMaxY || pNode.yCoord > nodeMaxY ? pNode.yCoord : nodeMaxY;
        nodeMinY = null == nodeMinY || pNode.yCoord < nodeMinY ? pNode.yCoord : nodeMinY;
        nodeRangeY = nodeMaxY - nodeMinY;
        //update coordinate transformation information for Y
        nodeMaxZ = null == nodeMaxZ || pNode.zCoord > nodeMaxZ ? pNode.zCoord : nodeMaxZ;
        nodeMinZ = null == nodeMinZ || pNode.zCoord < nodeMinZ ? pNode.zCoord : nodeMinZ;
        nodeRangeZ = nodeMaxZ - nodeMinZ;
    }

    public int findIndexFromVisibleFacePoint(int visibleFacePoint) {
        int visibleCounter = 0;
        for (int index = 0; index < pNodes.size(); index++) {
            if (pNodes.get(index).visible) {
                visibleCounter++;
                if (visibleCounter == visibleFacePoint)
                    return index;
            }
        }
        return -1;
    }

    public ArrayList<Point3D> getVisiblePoints(boolean useVisibility, double sceneWidth, double sceneHeight) {
        double halfSceneWidth = sceneWidth / 2.0;
        double halfSceneHeight = sceneHeight / 2.0;
        float quarterSceneWidth = (float) sceneWidth / 4.0f;

        double minX = null != nodeMinX ? nodeMinX : totalMinX;
        double rangeX = null != nodeRangeX ? nodeRangeX : totalRangeX;
        if (useTotalPositioning && null != totalMinX && null != totalMaxX) {
            minX = totalMinX;
            rangeX = totalRangeX;
        }
        double minY = null != nodeMinY ? nodeMinY : totalMinY;
        double rangeY = null != nodeRangeY ? nodeRangeY : totalRangeY;
        if (useTotalPositioning && null != totalMinY && null != totalMaxY) {
            minY = totalMinY;
            rangeY = totalRangeY;
        }
        double minZ = null != nodeMinZ ? nodeMinZ : totalMinZ;
        double rangeZ = null != nodeRangeZ ? nodeRangeZ : totalRangeZ;
        if (useTotalPositioning && null != totalMinZ && null != totalMaxZ) {
            minZ = totalMinZ;
            rangeZ = totalRangeZ;
        }

        ArrayList<Point3D> data = new ArrayList<>(pNodes.size());
        float xCoord;
        float yCoord;
        float zCoord;
        Point3D point3D;
        for (Perspective3DNode pNode : pNodes) {
            if (null == pNode) {
                System.out.println("Null... wtf...");
            } else if (pNode.visible) {
                try {
                    //X ==> X Positive
                    xCoord = rangeX == 0.0 ? 0.0f : (float) ((((pNode.xCoord - xShift) * pointScale - minX) * halfSceneWidth) / rangeX);
                    //Y ==> Z Positive
                    yCoord = rangeY == 0.0 ? 0.0f : (float) ((((pNode.yCoord - yShift) * pointScale - minY) * halfSceneHeight) / rangeY);
                    //Z ==> Y Positive
                    zCoord = rangeZ == 0.0 ? 0.0f : (float) ((((pNode.zCoord - zShift) * pointScale - minZ) * halfSceneWidth) / rangeZ);
                    //the offset of a quarter the size of the 3D scene because
                    //our data is centered and we need to center again within 3D coordinate system
                    point3D = new Point3D(xCoord - quarterSceneWidth, yCoord - quarterSceneWidth, zCoord - quarterSceneWidth);
                    if (reflectY) {
                        point3D.y *= -1;
                    }
                    point3D.f = Double.valueOf(pNode.nodeColor.getHue()).floatValue();
                    data.add(point3D);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return data;
    }

    public void updateModel(double sceneWidth, double sceneHeight) {
        data = getVisiblePoints(true, sceneWidth, sceneHeight);
        endPoints = getFixedEndPoints(0f);
    }

    public ArrayList<Point3D> getFixedEndPoints(float fixedSize) {
        Point3D[] endArray = new Point3D[pNodes.size()];
        Arrays.parallelSetAll(endArray, i -> new Point3D(fixedSize, fixedSize, fixedSize));
        return new ArrayList<>(Arrays.asList(endArray));
    }

    public void setShifts(double xShift, double yShift, double zShift) {
        this.xShift = xShift;
        this.yShift = yShift;
        this.zShift = zShift;
    }

    public void setLimits(double totalMinX, double totalMaxX,
                          double totalMinY, double totalMaxY,
                          double totalMinZ, double totalMaxZ) {
        this.totalMinX = totalMinX;
        this.totalMaxX = totalMaxX;
        this.totalMinY = totalMinY;
        this.totalMaxY = totalMaxY;
        this.totalMinZ = totalMinZ;
        this.totalMaxZ = totalMaxZ;
        this.totalRangeX = totalMaxX - totalMinX;
        this.totalRangeY = totalMaxY - totalMinY;
        this.totalRangeZ = totalMaxZ - totalMinZ;
    }

    public void reset() {
        pNodes.clear();
        data.clear();
        endPoints.clear();
        nodeMaxX = null;
        nodeMinX = null;
        nodeRangeX = null;
        nodeMaxY = null;
        nodeMinY = null;
        nodeRangeY = null;
        nodeMaxZ = null;
        nodeMinZ = null;
        nodeRangeZ = null;
        directMaxX = null;
        directMinX = null;
        directRangeX = null;
        directMaxY = null;
        directMinY = null;
        directRangeY = null;
        directMaxZ = null;
        directMinZ = null;
        directRangeZ = null;
    }
}
