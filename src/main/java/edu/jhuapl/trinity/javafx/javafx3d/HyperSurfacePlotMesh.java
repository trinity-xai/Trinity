/*
 * Modified from SurfacePlotMesh.java
 *
 * Original Copyright (C) 2013-2019, F(X)yz
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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point2D;
import javafx.scene.DepthTest;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.geometry.Face3;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.polygon.PolygonMesh;
import org.fxyz3d.shapes.primitives.TexturedMesh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * @author Sean Phillips
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
    public List<Double> functionValues;
    private PolygonMesh polygonMesh;

    // Direct primitive HeightField mesh state. The legacy FXyz Point3D/Face3 lists
    // intentionally remain empty while this mode is active.
    private boolean directHeightFieldMesh = false;
    private HeightField directHeightField;
    private int directStartX;
    private int directStartZ;
    private int directCellsX;
    private int directCellsZ;
    private int directVertsX;
    private int directVertsZ;
    private double directXScale;
    private double directYScale;
    private double directZScale;
    private WritableImage directPaletteImage;
    private int directPaletteColors = -1;

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
        functionValues = new ArrayList<>();
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
        functionValues = new ArrayList<>();
        updateMeshRaw(rangeX, rangeY, surfScale, functionScale, surfScale);
        setCullFace(CullFace.BACK);
        setDrawMode(DrawMode.FILL);
        setDepthTest(DepthTest.ENABLE);
    }

    public javafx.geometry.Point3D getPoint3DByVertNumber(int pointId) {
        if (directHeightFieldMesh && mesh != null) {
            int base = Math.multiplyExact(pointId, 3);
            if (base < 0 || base + 2 >= mesh.getPoints().size()) {
                throw new IndexOutOfBoundsException("pointId out of range: " + pointId);
            }
            return new javafx.geometry.Point3D(
                mesh.getPoints().get(base),
                mesh.getPoints().get(base + 1),
                mesh.getPoints().get(base + 2)
            );
        }
        Point3D p = listVertices.get(pointId);
        return new javafx.geometry.Point3D(p.x, p.y, p.z);
    }

    public final void injectMesh(TriangleMesh newMesh) {
        clearDirectHeightFieldState();
        setMesh(null);
        mesh = newMesh;
        setMesh(mesh);
    }

    public final void updateMeshRaw(int rangeX, int rangeY,
                                    double xScale, double yScale, double zScale) {
        clearDirectHeightFieldState();
        setMesh(null);
        mesh = createRawMesh(getFunctionVert3D(), rangeX, rangeY, xScale, yScale, zScale);
        setMesh(mesh);
    }

    /**
     * Builds the entire HeightField directly into JavaFX primitive mesh buffers.
     * This avoids the legacy per-vertex Point3D and per-face Face3 object graph.
     *
     * <p>The cell-oriented convention intentionally matches the existing Hypersurface:
     * a width x height HeightField produces (width + 1) x (height + 1) vertices. The
     * outermost row/column repeats the last source sample, preserving the existing
     * width*xScale and height*zScale world extents without a zero-height border.</p>
     */
    public final void updateMeshHeightField(HeightField heightField,
                                            double xScale, double yScale, double zScale) {
        updateMeshHeightField(heightField, 0, 0,
            heightField.width(), heightField.height(),
            xScale, yScale, zScale);
    }

    /**
     * Builds a rectangular cell region directly from a HeightField. This overload is
     * intentionally tile-ready: future tile MeshViews can request independent regions
     * without changing the primitive mesh builder.
     *
     * @param heightField source primitive height field
     * @param startX      first source cell/sample x index
     * @param startZ      first source cell/sample z index
     * @param cellsX      number of mesh cells in x (vertices = cellsX + 1)
     * @param cellsZ      number of mesh cells in z (vertices = cellsZ + 1)
     * @param xScale      world units per x cell
     * @param yScale      height multiplier
     * @param zScale      world units per z cell
     */
    public final void updateMeshHeightField(HeightField heightField,
                                            int startX, int startZ,
                                            int cellsX, int cellsZ,
                                            double xScale, double yScale, double zScale) {
        validateHeightFieldRegion(heightField, startX, startZ, cellsX, cellsZ);

        directHeightFieldMesh = true;
        directHeightField = heightField;
        directStartX = startX;
        directStartZ = startZ;
        directCellsX = cellsX;
        directCellsZ = cellsZ;
        directVertsX = cellsX + 1;
        directVertsZ = cellsZ + 1;
        directXScale = xScale;
        directYScale = yScale;
        directZScale = zScale;

        // Release any object-heavy legacy geometry retained by TexturedMesh.
        listVertices.clear();
        listTextures.clear();
        listFaces.clear();
        smoothingGroups = null;

        setMesh(null);
        mesh = createHeightFieldMesh(heightField, startX, startZ, cellsX, cellsZ,
            xScale, yScale, zScale);
        setMesh(mesh);
    }

    public boolean isDirectHeightFieldMesh() {
        return directHeightFieldMesh;
    }

    /**
     * Restores spatial UV coordinates and applies an image directly to a primitive
     * HeightField mesh. Faces are not rebuilt.
     */
    public void setDirectTextureModeImage(Image image) {
        if (!directHeightFieldMesh || mesh == null || image == null) return;
        restoreDirectSpatialTexCoords();
        PhongMaterial material = directPhongMaterial();
        material.setDiffuseColor(Color.WHITE);
        material.setDiffuseMap(image);
        setMaterial(material);
    }

    /**
     * Colors a direct HeightField mesh by its rendered Y value using a compact rainbow
     * palette. Only texture coordinates are updated; point and face buffers are retained.
     */
    public void setDirectTextureModeByHeight(int colors, double min, double max) {
        if (!directHeightFieldMesh || mesh == null) return;
        if (colors < 2) throw new IllegalArgumentException("colors must be >= 2");
        if (!(max > min)) throw new IllegalArgumentException("max must be > min");

        WritableImage palette = getOrCreateDirectPalette(colors);
        ObservableFloatArray texCoords = mesh.getTexCoords();
        final float[] source = directHeightField.data();
        final int sourceWidth = directHeightField.width();
        final int sourceHeight = directHeightField.height();
        final float[] texRow = new float[directVertsX * 2];

        for (int localZ = 0; localZ < directVertsZ; localZ++) {
            int sampleZ = Math.min(directStartZ + localZ, sourceHeight - 1);
            int sourceRow = sampleZ * sourceWidth;
            int out = 0;
            for (int localX = 0; localX < directVertsX; localX++) {
                int sampleX = Math.min(directStartX + localX, sourceWidth - 1);
                double y = source[sourceRow + sampleX] * directYScale;
                double normalized = (y - min) / (max - min);
                if (normalized < 0.0) normalized = 0.0;
                else if (normalized > 1.0) normalized = 1.0;
                int colorIndex = (int) Math.round(normalized * (colors - 1));
                texRow[out++] = (float) ((colorIndex + 0.5) / colors);
                texRow[out++] = 0.5f;
            }
            texCoords.set(localZ * directVertsX * 2,
                texRow, 0, texRow.length);
        }

        PhongMaterial material = directPhongMaterial();
        material.setDiffuseColor(Color.WHITE);
        material.setDiffuseMap(palette);
        setMaterial(material);
    }

    protected final void updateMeshSmooth(int rangeX, int rangeY) {
        clearDirectHeightFieldState();
        setMesh(null);
        mesh = createSmoothMesh(getFunctionVert3D(),
            rangeX, rangeY,
            getDivisionsX(), getDivisionsY(),
            getFunctionScale());
        setMesh(mesh);
    }

    @Override
    protected final void updateMesh() {
        clearDirectHeightFieldState();
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

    private TriangleMesh createHeightFieldMesh(HeightField heightField,
                                               int startX, int startZ,
                                               int cellsX, int cellsZ,
                                               double xScale, double yScale, double zScale) {
        final int vertsX = cellsX + 1;
        final int vertsZ = cellsZ + 1;
        final long vertexCount = (long) vertsX * vertsZ;
        final long pointFloatCount = vertexCount * 3L;
        final long texFloatCount = vertexCount * 2L;
        final long faceCount = (long) cellsX * cellsZ * 2L;
        final long faceIntCount = faceCount * 6L;

        if (pointFloatCount > Integer.MAX_VALUE
            || texFloatCount > Integer.MAX_VALUE
            || faceCount > Integer.MAX_VALUE
            || faceIntCount > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                "HeightField region is too large for a JavaFX TriangleMesh: "
                    + cellsX + "x" + cellsZ);
        }

        areaMesh.setWidth(cellsX);
        areaMesh.setHeight(cellsZ);
        rectMesh.setWidth(cellsX);
        rectMesh.setHeight(cellsZ);

        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getPoints().resize((int) pointFloatCount);
        triangleMesh.getTexCoords().resize((int) texFloatCount);
        triangleMesh.getFaces().resize((int) faceIntCount);
        triangleMesh.getFaceSmoothingGroups().resize((int) faceCount);

        final float[] source = heightField.data();
        final int sourceWidth = heightField.width();
        final int sourceHeight = heightField.height();
        final float[] pointRow = new float[vertsX * 3];
        final float[] texRow = new float[vertsX * 2];

        for (int localZ = 0; localZ < vertsZ; localZ++) {
            final int globalVertexZ = startZ + localZ;
            final int sampleZ = Math.min(globalVertexZ, sourceHeight - 1);
            final int sourceRow = sampleZ * sourceWidth;
            final float worldZ = (float) (localZ * zScale);
            final float v = (float) globalVertexZ / (float) sourceHeight;

            int p = 0;
            int t = 0;
            for (int localX = 0; localX < vertsX; localX++) {
                final int globalVertexX = startX + localX;
                final int sampleX = Math.min(globalVertexX, sourceWidth - 1);
                final float worldX = (float) (localX * xScale);
                final float worldY = source[sourceRow + sampleX] * (float) yScale;
                final float u = (float) globalVertexX / (float) sourceWidth;

                pointRow[p++] = worldX;
                pointRow[p++] = worldY;
                pointRow[p++] = worldZ;

                texRow[t++] = u;
                texRow[t++] = v;
            }

            triangleMesh.getPoints().set(localZ * vertsX * 3,
                pointRow, 0, pointRow.length);
            triangleMesh.getTexCoords().set(localZ * vertsX * 2,
                texRow, 0, texRow.length);
        }

        // Bound temporary allocations while populating the very large persistent JavaFX
        // face buffers. Point and texture indices intentionally match one-to-one.
        final int rowsPerBlock = 16;
        for (int z0 = 0; z0 < cellsZ; z0 += rowsPerBlock) {
            final int rows = Math.min(rowsPerBlock, cellsZ - z0);
            final int trianglesInBlock = Math.multiplyExact(Math.multiplyExact(rows, cellsX), 2);
            final int[] faces = new int[Math.multiplyExact(trianglesInBlock, 6)];
            final int[] groups = new int[trianglesInBlock];
            Arrays.fill(groups, 1);

            int fi = 0;
            for (int localZ = 0; localZ < rows; localZ++) {
                final int z = z0 + localZ;
                final int rowStart = z * vertsX;
                for (int x = 0; x < cellsX; x++) {
                    final int p00 = rowStart + x;
                    final int p01 = p00 + 1;
                    final int p10 = p00 + vertsX;
                    final int p11 = p10 + 1;

                    faces[fi++] = p00;
                    faces[fi++] = p00;
                    faces[fi++] = p10;
                    faces[fi++] = p10;
                    faces[fi++] = p11;
                    faces[fi++] = p11;

                    faces[fi++] = p11;
                    faces[fi++] = p11;
                    faces[fi++] = p01;
                    faces[fi++] = p01;
                    faces[fi++] = p00;
                    faces[fi++] = p00;
                }
            }

            final int firstTriangle = z0 * cellsX * 2;
            triangleMesh.getFaces().set(firstTriangle * 6,
                faces, 0, faces.length);
            triangleMesh.getFaceSmoothingGroups().set(firstTriangle,
                groups, 0, groups.length);
        }

        return triangleMesh;
    }

    private void restoreDirectSpatialTexCoords() {
        if (!directHeightFieldMesh || mesh == null || directHeightField == null) return;
        final int sourceWidth = directHeightField.width();
        final int sourceHeight = directHeightField.height();
        final float[] texRow = new float[directVertsX * 2];

        for (int localZ = 0; localZ < directVertsZ; localZ++) {
            final int globalVertexZ = directStartZ + localZ;
            final float v = (float) globalVertexZ / (float) sourceHeight;
            int t = 0;
            for (int localX = 0; localX < directVertsX; localX++) {
                final int globalVertexX = directStartX + localX;
                texRow[t++] = (float) globalVertexX / (float) sourceWidth;
                texRow[t++] = v;
            }
            mesh.getTexCoords().set(localZ * directVertsX * 2,
                texRow, 0, texRow.length);
        }
    }

    private WritableImage getOrCreateDirectPalette(int colors) {
        if (directPaletteImage != null && directPaletteColors == colors) {
            return directPaletteImage;
        }

        WritableImage palette = new WritableImage(colors, 1);
        PixelWriter writer = palette.getPixelWriter();
        for (int i = 0; i < colors; i++) {
            double d = (double) i / (double) (colors - 1);
            Color c;
            if (i == 0) c = Color.BLACK;
            else if (i == colors - 1) c = Color.WHITE;
            else c = Color.hsb(360.0 * d, 1.0, 1.0, 1.0);
            writer.setColor(i, 0, c);
        }
        directPaletteImage = palette;
        directPaletteColors = colors;
        return palette;
    }

    private PhongMaterial directPhongMaterial() {
        if (getMaterial() instanceof PhongMaterial material) {
            return material;
        }
        return new PhongMaterial(Color.WHITE);
    }

    private void validateHeightFieldRegion(HeightField heightField,
                                           int startX, int startZ,
                                           int cellsX, int cellsZ) {
        if (heightField == null) throw new IllegalArgumentException("heightField cannot be null");
        if (startX < 0 || startZ < 0) {
            throw new IllegalArgumentException("startX/startZ must be >= 0");
        }
        if (cellsX <= 0 || cellsZ <= 0) {
            throw new IllegalArgumentException("cellsX/cellsZ must be > 0");
        }
        if (startX >= heightField.width() || startZ >= heightField.height()) {
            throw new IllegalArgumentException("region start is outside HeightField");
        }
        if ((long) startX + cellsX > heightField.width()
            || (long) startZ + cellsZ > heightField.height()) {
            throw new IllegalArgumentException(
                "region exceeds HeightField cell extent: start=" + startX + "," + startZ
                    + " cells=" + cellsX + "x" + cellsZ
                    + " field=" + heightField.width() + "x" + heightField.height());
        }
    }

    private void clearDirectHeightFieldState() {
        directHeightFieldMesh = false;
        directHeightField = null;
        directStartX = 0;
        directStartZ = 0;
        directCellsX = 0;
        directCellsZ = 0;
        directVertsX = 0;
        directVertsZ = 0;
        directXScale = Double.NaN;
        directYScale = Double.NaN;
        directZScale = Double.NaN;
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

        int functionIndex = 0;
        Double currentFValue = 0.0;
        // Create points
        for (int z = 0; z <= rangeZ; z++) {
            dz = (float) (z * zScale);
            for (int x = 0; x <= rangeX; x++) {
                dx = (float) (x * xScale);
                height = (float) yScale * vertFunction.apply(new Vert3D(dx, dz, x, z)).floatValue();
                functionIndex = (z * rangeX) + x;
                if (functionIndex < functionValues.size())
                    currentFValue = functionValues.get(functionIndex);
                listVertices.add(new Point3D(dx, height, dz, currentFValue.floatValue()));

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
        int[] faceSmoothingGroups = new int[listFaces.size()]; // 0 == hard edges
        Arrays.fill(faceSmoothingGroups, 1); // 1: soft edges, all the faces in same surface
        smoothingGroups = faceSmoothingGroups;
        return createMesh();
    }

    public void setAllVerts(float... floats) {
        mesh.getPoints().setAll(floats);
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
        for (int z = 0; z <= divisionsZ; z++) {
            dz = ((float) z / (float) divisionsZ) * rangeZ;
            for (int x = 0; x <= divisionsX; x++) {
                dx = ((float) x / (float) divisionsX) * rangeX;
                pointY = (float) yScale * vertFunction.apply(new Vert3D(dx, dz, x, z)).floatValue();
                listVertices.add(new Point3D(dx, pointY, dz));
            }
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
        int[] faceSmoothingGroups = new int[listFaces.size()]; // 0 == hard edges
        Arrays.fill(faceSmoothingGroups, 1); // 1: soft edges, all the faces in same surface
        smoothingGroups = faceSmoothingGroups;
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
            dy = (float) (((float) y / (float) divisionsY) * rangeY);
            for (int x = 0; x <= divisionsX; x++) {
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
        int[] faceSmoothingGroups = new int[listFaces.size()]; // 0 == hard edges
        Arrays.fill(faceSmoothingGroups, 1); // 1: soft edges, all the faces in same surface
        smoothingGroups = faceSmoothingGroups;
        return createMesh();
    }

    public List<Point3D> getListVertices() {
        return listVertices;
    }
}
