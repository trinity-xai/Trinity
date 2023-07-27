/*
 * SurfacePlotMesh.java
 *
 * Copyright (c) 2013-2019, F(X)yz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of F(X)yz, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL F(X)yz BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point2D;
import javafx.scene.DepthTest;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.geometry.Face3;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.polygon.PolygonMesh;
import org.fxyz3d.shapes.primitives.TexturedMesh;

import java.util.function.Function;

/**
 * @author Sean Phillips
 * Modified version of SurfacePlotMesh.java
 * SurfacePlotMesh to plot 2D functions z = f(x,y)
 */
public class HyperSurfacePlotMesh extends TexturedMesh {

    private static final Function<Point2D, Number> DEFAULT_FUNCTION = p -> Math.sin(p.magnitude()) / p.magnitude();
    private static final Function<Vert3D, Number> DEFAULT_VERT3D_FUNCTION = p -> Math.sin(p.magnitude()) / p.magnitude();


    private static final double DEFAULT_X_RANGE = 10; // -5 +5
    private static final double DEFAULT_Y_RANGE = 10; // -5 +5
    private static final int DEFAULT_X_DIVISIONS = 64;
    private static final int DEFAULT_Y_DIVISIONS = 64;
    private static final double DEFAULT_FUNCTION_SCALE = 1.0D;
    private static final double DEFAULT_SURF_SCALE = 1.0D;

    private PolygonMesh polygonMesh;

    public HyperSurfacePlotMesh() {
        this(DEFAULT_FUNCTION, DEFAULT_X_RANGE, DEFAULT_Y_RANGE, DEFAULT_X_DIVISIONS, DEFAULT_Y_DIVISIONS, DEFAULT_FUNCTION_SCALE);
    }

    public HyperSurfacePlotMesh(Function<Point2D, Number> function) {
        this(function, DEFAULT_X_RANGE, DEFAULT_Y_RANGE, DEFAULT_X_DIVISIONS, DEFAULT_Y_DIVISIONS, DEFAULT_FUNCTION_SCALE);
    }

    public HyperSurfacePlotMesh(Function<Point2D, Number> function, double rangeX, double rangeY) {
        this(function, rangeX, rangeY, DEFAULT_X_DIVISIONS, DEFAULT_Y_DIVISIONS, DEFAULT_FUNCTION_SCALE);
    }

    public HyperSurfacePlotMesh(Function<Point2D, Number> function, double rangeX, double rangeY, double functionScale) {
        this(function, rangeX, rangeY, DEFAULT_X_DIVISIONS, DEFAULT_Y_DIVISIONS, functionScale);
    }

    public HyperSurfacePlotMesh(Function<Point2D, Number> function, double rangeX, double rangeY, int divisionsX, int divisionsY, double functionScale) {
        setFunction2D(function);
        setRangeX(rangeX);
        setRangeY(rangeY);
        setDivisionsX(divisionsX);
        setDivisionsY(divisionsY);
        setFunctionScale(functionScale);

        updateMesh();
        setCullFace(CullFace.BACK);
        setDrawMode(DrawMode.FILL);
        setDepthTest(DepthTest.ENABLE);
    }

    public HyperSurfacePlotMesh(int rangeX, int rangeY, int divisionsX, int divisionsY,
                                double functionScale, double surfScale, Function<Vert3D, Number> functionVert3D) {
        setFunction2D(DEFAULT_FUNCTION);
        setFunctionVert3D(functionVert3D);
        setRangeX(rangeX);
        setRangeY(rangeY);
        setDivisionsX(divisionsX);
        setDivisionsY(divisionsY);
        setFunctionScale(functionScale);
        setSurfScale(surfScale);
        updateMeshRaw(rangeX, rangeY, surfScale, functionScale, surfScale);
        setCullFace(CullFace.BACK);
        setDrawMode(DrawMode.FILL);
        setDepthTest(DepthTest.ENABLE);
    }

    public javafx.geometry.Point3D getPoint3DByVertNumber(int pointId) {
        Point3D p = listVertices.get(pointId);
        return new javafx.geometry.Point3D(p.x, p.y, p.z);
    }

    public final void injectMesh(TriangleMesh newMesh) {
        setMesh(null);
        mesh = newMesh;

//            int[] faceSmoothingGroups = new int[listFaces.size()]; // 0 == hard edges
//            Arrays.fill(faceSmoothingGroups, 1); // 1: soft edges, all the faces in same surface
//            if(smoothingGroups!=null){
//                triangleMesh.getFaceSmoothingGroups().addAll(smoothingGroups);
//            } else {
//                triangleMesh.getFaceSmoothingGroups().addAll(faceSmoothingGroups);
//            }
//
//            vertCountBinding.invalidate();
//            faceCountBinding.invalidate();
        setMesh(mesh);
    }

    public final void updateMeshRaw(int rangeX, int rangeY,
                                    double xScale, double yScale, double zScale) {
        setMesh(null);
        mesh = createRawMesh(getFunctionVert3D(), rangeX, rangeY, xScale, yScale, zScale);
        setMesh(mesh);
    }

    protected final void updateMeshSmooth(int rangeX, int rangeY) {
        setMesh(null);
        mesh = createSmoothMesh(getFunctionVert3D(),
            rangeX, rangeY,
            getDivisionsX(), getDivisionsY(),
            getFunctionScale());
        setMesh(mesh);
    }

    @Override
    protected final void updateMesh() {
        setMesh(null);
        mesh = createPlotMesh(
            getFunction2D(),
            getRangeX(), getRangeY(),
            getDivisionsX(), getDivisionsY(),
            getFunctionScale());
        setMesh(mesh);
    }

    private final ObjectProperty<Function<Vert3D, Number>> functionVert3D = new SimpleObjectProperty<Function<Vert3D, Number>>(DEFAULT_VERT3D_FUNCTION) {
        @Override
        protected void invalidated() {
            if (mesh != null) {
                updateMeshSmooth(
                    Double.valueOf(getRangeX()).intValue(),
                    Double.valueOf(getRangeY()).intValue()
                );
            }
        }
    };

    public Function<Vert3D, Number> getFunctionVert3D() {
        return functionVert3D.get();
    }

    public final void setFunctionVert3D(Function<Vert3D, Number> value) {
        functionVert3D.set(value);
    }

    public ObjectProperty functionVert3DProperty() {
        return functionVert3D;
    }


    private final ObjectProperty<Function<Point2D, Number>> function2D = new SimpleObjectProperty<Function<Point2D, Number>>(DEFAULT_FUNCTION) {
        @Override
        protected void invalidated() {
            if (mesh != null) {
                updateMesh();
            }
        }
    };

    public Function<Point2D, Number> getFunction2D() {
        return function2D.get();
    }

    public final void setFunction2D(Function<Point2D, Number> value) {
        function2D.set(value);
    }

    public ObjectProperty function2DProperty() {
        return function2D;
    }

    private final DoubleProperty rangeX = new SimpleDoubleProperty(DEFAULT_X_RANGE) {
        @Override
        protected void invalidated() {
            if (mesh != null) {
                updateMesh();
            }
        }
    };

    public double getRangeX() {
        return rangeX.get();
    }

    public final void setRangeX(double value) {
        rangeX.set(value);
    }

    public DoubleProperty rangeXProperty() {
        return rangeX;
    }

    private final DoubleProperty rangeY = new SimpleDoubleProperty(DEFAULT_Y_RANGE) {
        @Override
        protected void invalidated() {
            if (mesh != null) {
                updateMesh();
            }
        }
    };

    public double getRangeY() {
        return rangeY.get();
    }

    public final void setRangeY(double value) {
        rangeY.set(value);
    }

    public DoubleProperty rangeYProperty() {
        return rangeY;
    }

    private final IntegerProperty divisionsX = new SimpleIntegerProperty(DEFAULT_X_DIVISIONS) {
        @Override
        protected void invalidated() {
            if (mesh != null) {
                updateMesh();
            }
        }
    };

    public int getDivisionsX() {
        return divisionsX.get();
    }

    public final void setDivisionsX(int value) {
        divisionsX.set(value);
    }

    public IntegerProperty divisionsXProperty() {
        return divisionsX;
    }

    private final IntegerProperty divisionsY = new SimpleIntegerProperty(DEFAULT_Y_DIVISIONS) {
        @Override
        protected void invalidated() {
            if (mesh != null) {
                updateMesh();
            }
        }
    };

    public int getDivisionsY() {
        return divisionsY.get();
    }

    public final void setDivisionsY(int value) {
        divisionsY.set(value);
    }

    public IntegerProperty divisionsYProperty() {
        return divisionsY;
    }

    private final DoubleProperty functionScale = new SimpleDoubleProperty(DEFAULT_FUNCTION_SCALE) {
        @Override
        protected void invalidated() {
            if (mesh != null) {
                updateMesh();
            }
        }
    };

    public double getFunctionScale() {
        return functionScale.get();
    }

    public final void setFunctionScale(double value) {
        functionScale.set(value);
    }

    public DoubleProperty functionScaleProperty() {
        return functionScale;
    }

    private final DoubleProperty surfScale = new SimpleDoubleProperty(DEFAULT_FUNCTION_SCALE) {
        @Override
        protected void invalidated() {
            if (mesh != null) {
                updateMesh();
            }
        }
    };

    public double getSurfScale() {
        return surfScale.get();
    }

    public final void setSurfScale(double value) {
        surfScale.set(value);
    }

    public DoubleProperty surfScaleProperty() {
        return surfScale;
    }

    public PolygonMesh getPolygonMesh() {
        return polygonMesh;
    }

    private TriangleMesh createRawMesh(Function<Vert3D, Number> vertFunction,
                                       int rangeX, int rangeZ, double xScale, double yScale, double zScale) {
        listVertices.clear();
        listTextures.clear();
        listFaces.clear();

        float height, dz, dx;
        int numDivX = rangeX + 1;
        // Create textures indices
        int p00, p01, p10, p11;

        areaMesh.setWidth(rangeX);
        areaMesh.setHeight(rangeZ);
        // Create texture coordinates
        createTexCoords(rangeX, rangeZ);

        // Create points
        for (int z = 0; z <= rangeZ; z++) {
            dz = (float) (z * zScale);
            for (int x = 0; x <= rangeX; x++) {
                dx = (float) (x * xScale);
                height = (float) yScale * vertFunction.apply(new Vert3D(dx, dz, x, z)).floatValue();
                listVertices.add(new Point3D(dx, height, dz));
                if (z < rangeZ && x < rangeX) {
                    p00 = z * numDivX + x;
                    p01 = p00 + 1;
                    p10 = p00 + numDivX;
                    p11 = p10 + 1;
                    listTextures.add(new Face3(p00, p10, p11));
                    listTextures.add(new Face3(p11, p01, p00));
                    listFaces.add(new Face3(p00, p10, p11));
                    listFaces.add(new Face3(p11, p01, p00));
                }
            }
        }
        return createMesh();
    }

    public void setVert(int index, Point3D p3d) {
        listVertices.set(index, p3d);
        mesh.getPoints().set(3 * index, p3d.z);
        mesh.getPoints().set(3 * index + 1, p3d.y);
        mesh.getPoints().set(3 * index + 2, p3d.x);
    }

    public void setVert(int index, float[] src) {
        //set(int destIndex, float[] src, int srcIndex, int length)
        //Copies a portion of specified array into this observable array.
        mesh.getPoints().set(3 * index, src, 0, 3);
    }

    public Point3D getVert(int index) {
        return listVertices.get(index);
    }

    public void scaleHeight(float yScale) {
        int size = mesh.getPoints().size();
        ObservableFloatArray ofa = mesh.getPoints();
        for (int i = 0; i < size; i += 3) {
            mesh.getPoints().set(i + 1, ofa.get(i + 1) * yScale);
        }
    }

    public Float getMaxY() {
        float[] points = new float[mesh.getPoints().size()];
        mesh.getPoints().toArray(points);
        Float max = points[1];
        for (int i = 0; i < points.length; i += 3) {
            if (points[i + 1] > max)
                max = points[i + 1];
        }
        return max;
    }

    private TriangleMesh createSmoothMesh(Function<Vert3D, Number> vertFunction, int rangeX, int rangeZ, int divisionsX, int divisionsZ, double yScale) {
        listVertices.clear();
        listTextures.clear();
        listFaces.clear();
        areaMesh.setWidth(rangeX);
        areaMesh.setHeight(rangeZ);

        float pointY, dz, dx;
        // Create texture coordinates
        createTexCoords(rangeX, rangeZ);
        // Create textures indices
        int numDivX = divisionsX + 1;
        int p00, p01, p10, p11;
        // Create points
        //System.out.println("Divisions X/Z: " + divisionsX + "/" + divisionsZ);
//        for (int z = 0; z <= rangeZ; z++) {
//            dz = (float)(((float)z /(float)divisionsZ)*rangeZ);
//            System.out.print("dz: " + dz + " dx:");
//            for (int x = 0; x <= rangeX; x++) {
//                dx = (float)(((float)x /(float)divisionsX)*rangeX);

        for (int z = 0; z <= divisionsZ; z++) {
            dz = (float) (((float) z / (float) divisionsZ) * rangeZ);
            for (int x = 0; x <= divisionsX; x++) {
                dx = (float) (((float) x / (float) divisionsX) * rangeX);
                pointY = (float) yScale * vertFunction.apply(new Vert3D(dx, dz, x, z)).floatValue();
                listVertices.add(new Point3D(dx, pointY, dz));
                //System.out.print(" " + dx);
//                if(z < rangeZ && x < rangeX) {
//                    p00 = z * numDivX + x;
//                    p01 = p00 + 1;
//                    p10 = p00 + numDivX;
//                    p11 = p10 + 1;
//                    listTextures.add(new Face3(p00,p10,p11));
//                    listTextures.add(new Face3(p11,p01,p00));
//                    listFaces.add(new Face3(p00,p10,p11));
//                    listFaces.add(new Face3(p11,p01,p00));
//                }
            }
            //System.out.print("\n");
        }
        // Create textures indices
        for (int z = 0; z < divisionsZ; z++) {
            for (int x = 0; x < divisionsX; x++) {
                p00 = z * numDivX + x;
                p01 = p00 + 1;
                p10 = p00 + numDivX;
                p11 = p10 + 1;
                listTextures.add(new Face3(p00, p10, p11));
                listTextures.add(new Face3(p11, p01, p00));
                listFaces.add(new Face3(p00, p10, p11));
                listFaces.add(new Face3(p11, p01, p00));
            }
        }
        return createMesh();
    }

    private TriangleMesh createPlotMesh(Function<Point2D, Number> function2D, double rangeX, double rangeY, int divisionsX, int divisionsY, double scale) {
        listVertices.clear();
        listTextures.clear();
        listFaces.clear();

        int numDivX = divisionsX + 1;
        float pointY, dy, dx;

        areaMesh.setWidth(rangeX);
        areaMesh.setHeight(rangeY);

        // Create points
        for (int y = 0; y <= divisionsY; y++) {
//            dy = (float)(-rangeY/2d + ((float)y /(float)divisionsY)*rangeY);
            dy = (float) (((float) y / (float) divisionsY) * rangeY);
            for (int x = 0; x <= divisionsX; x++) {
//                dx = (float)(-rangeX/2d + ((float)x /(float)divisionsX)*rangeX);
                dx = (float) (((float) x / (float) divisionsX) * rangeX);
                pointY = (float) scale * function2D.apply(new Point2D(dx, dy)).floatValue();
                listVertices.add(new Point3D(dx, pointY, dy));
            }
        }
        // Create texture coordinates
        createTexCoords(divisionsX, divisionsY);

        int p00, p01, p10, p11;

        // Create textures indices
        for (int y = 0; y < divisionsY; y++) {
            for (int x = 0; x < divisionsX; x++) {
                p00 = y * numDivX + x;
                p01 = p00 + 1;
                p10 = p00 + numDivX;
                p11 = p10 + 1;
                listTextures.add(new Face3(p00, p10, p11));
                listTextures.add(new Face3(p11, p01, p00));
                listFaces.add(new Face3(p00, p10, p11));
                listFaces.add(new Face3(p11, p01, p00));
            }
        }
        return createMesh();
    }
}
