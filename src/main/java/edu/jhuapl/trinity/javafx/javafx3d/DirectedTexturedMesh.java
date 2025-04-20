/*
 * @DERIVED FROM FXyz TexturedMesh class. Below is the copyright and link to GNU GPL
 * Original Copyright (C) 2013-2015 F(X)yz,
 * Sean Phillips, Jason Pollastrini and Jose Pereda
 * All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import org.fxyz3d.geometry.Face3;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.scene.paint.Palette.ColorPalette;
import org.fxyz3d.scene.paint.Palette.FunctionColorPalette;
import org.fxyz3d.scene.paint.Patterns.CarbonPatterns;
import org.fxyz3d.shapes.primitives.helper.MeshHelper;
import org.fxyz3d.shapes.primitives.helper.TextureMode;
import org.fxyz3d.shapes.primitives.helper.TriangleMeshHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.fxyz3d.scene.paint.Palette.DEFAULT_COLOR_PALETTE;
import static org.fxyz3d.shapes.primitives.helper.TriangleMeshHelper.*;

/**
 * TexturedMesh is a base class that provides support for different mesh implementations
 * taking into account four different kind of textures
 * - None
 * - Image
 * - Colored vertices
 * - Colored faces
 * <p>
 * For the last two ones, number of colors and density map have to be provided
 * <p>
 * Any subclass must use mesh, listVertices and listFaces
 *
 * @author José Pereda
 */
public abstract class DirectedTexturedMesh extends MeshView implements TextureMode {

    private TriangleMeshHelper helper = new TriangleMeshHelper();
    protected TriangleMesh mesh;

    protected List<Point3D> listVertices = new ArrayList<>();
    protected final List<Face3> listTextures = new ArrayList<>();
    protected final List<Face3> listFaces = new ArrayList<>();
    protected float[] textureCoords;
    protected int[] smoothingGroups;

    protected final Rectangle rectMesh = new Rectangle(0, 0);
    protected final Rectangle areaMesh = new Rectangle(0, 0);

    protected Rotate rotateX, rotateY, rotateZ;
    protected Translate translate;
    protected Scale scale;

    protected DirectedTexturedMesh() {
        setMaterial(helper.getMaterial());
    }

    private final ObjectProperty<SectionType> sectionType = new SimpleObjectProperty<SectionType>(SectionType.CIRCLE) {

        @Override
        protected void invalidated() {
            if (mesh != null) {
                updateMesh();
            }
        }

    };

    public SectionType getSectionType() {
        return sectionType.get();
    }

    public void setSectionType(SectionType value) {
        sectionType.set(value);
    }

    public ObjectProperty sectionTypeProperty() {
        return sectionType;
    }

    private final ObjectProperty<TextureType> textureType = new SimpleObjectProperty<TextureType>(TextureType.NONE) {

        @Override
        protected void invalidated() {
            if (mesh != null) {
                updateTexture();
                updateTextureOnFaces();
            }
        }
    };

    @Override
    public void setTextureModeNone() {
        setTextureModeNone(Color.WHITE);
    }

    @Override
    public void setTextureModeNone(Color color) {
        if (color != null) {
            helper.setTextureType(TextureType.NONE);
            helper.getMaterialWithColor(color);
        }
        setTextureType(helper.getTextureType());
    }

    @Override
    public void setTextureModeNone(Color color, String image) {
        if (color != null) {
            helper.setTextureType(TextureType.NONE);
            setMaterial(helper.getMaterialWithColor(color, image));
        }
        setTextureType(helper.getTextureType());
    }

    @Override
    public void setTextureModeImage(String image) {
        if (image != null && !image.isEmpty()) {
            helper.setTextureType(TextureType.IMAGE);
            helper.getMaterialWithImage(image);
            setTextureType(helper.getTextureType());
        }
    }

    @Override
    public void setTextureModePattern(CarbonPatterns pattern, double scale) {
        helper.setTextureType(TextureType.PATTERN);
        patternScale.set(scale);
        carbonPatterns.set(pattern);
        helper.getMaterialWithPattern(pattern);
        setTextureType(helper.getTextureType());
    }

    @Override
    public void setTextureModeVertices3D(int colors, Function<Point3D, Number> dens) {
        helper.setTextureType(TextureType.COLORED_VERTICES_3D);
        setColors(colors);
        createPalette(getColors());
        setDensity(dens);
        helper.setDensity(dens);
        setTextureType(helper.getTextureType());
    }

    @Override
    public void setTextureModeVertices3D(ColorPalette palette, Function<Point3D, Number> dens) {
        helper.setTextureType(TextureType.COLORED_VERTICES_3D);
//        setColors(colors);
        setColorPalette(palette);
        createPalette(getColors());
        setDensity(dens);
        helper.setDensity(dens);
        setTextureType(helper.getTextureType());
    }

    @Override
    public void setTextureModeVertices3D(int colors, Function<Point3D, Number> dens, double min, double max) {
        helper.setTextureType(TextureType.COLORED_VERTICES_3D);
        setMinGlobal(min);
        setMaxGlobal(max);
        setColors(colors);
        createPalette(getColors());
        setDensity(dens);
        helper.setDensity(dens);
        setTextureType(helper.getTextureType());
    }

    @Override
    public void setTextureModeVertices1D(int colors, Function<Number, Number> function) {
        helper.setTextureType(TextureType.COLORED_VERTICES_1D);
        setColors(colors);
        createPalette(getColors());
        setFunction(function);
        helper.setFunction(function);
        setTextureType(helper.getTextureType());
    }

    @Override
    public void setTextureModeVertices1D(ColorPalette palette, Function<Number, Number> function) {
        helper.setTextureType(TextureType.COLORED_VERTICES_1D);
//        setColors(colors);
        setColorPalette(palette);
        createPalette(getColors());
        setFunction(function);
        helper.setFunction(function);
        setTextureType(helper.getTextureType());
    }

    @Override
    public void setTextureModeVertices1D(int colors, Function<Number, Number> function, double min, double max) {
        helper.setTextureType(TextureType.COLORED_VERTICES_1D);
        setMinGlobal(min);
        setMaxGlobal(max);
        setColors(colors);
        createPalette(getColors());
        setFunction(function);
        helper.setFunction(function);
        setTextureType(helper.getTextureType());
    }

    @Override
    public void setTextureModeFaces(int colors) {
        helper.setTextureType(TextureType.COLORED_FACES);
        setColors(colors);
        createPalette(getColors());
        setTextureType(helper.getTextureType());
    }

    @Override
    public void setTextureModeFaces(ColorPalette palette) {
        helper.setTextureType(TextureType.COLORED_FACES);
//        setColors(colors);
        setColorPalette(palette);
        createPalette(getColors());
        setTextureType(helper.getTextureType());
    }

    @Override
    public void updateF(List<Number> values) {
        listVertices = IntStream.range(0, values.size()).mapToObj(i -> {
            Point3D p = listVertices.get(i);
            p.f = values.get(i).floatValue();
            return p;
        }).collect(Collectors.toList());
        updateTextureOnFaces();

    }

    @Override
    public void setTextureOpacity(double value) {
        helper.setTextureOpacity(value);
    }

    public TextureType getTextureType() {
        return textureType.get();
    }

    public void setTextureType(TextureType value) {
        textureType.set(value);
    }

    public ObjectProperty textureTypeProperty() {
        return textureType;
    }

    private final DoubleProperty patternScale = new SimpleDoubleProperty(DEFAULT_PATTERN_SCALE) {

        @Override
        protected void invalidated() {
            updateTexture();
        }

    };

    public final double getPatternScale() {
        return patternScale.get();
    }

    public final void setPatternScale(double scale) {
        patternScale.set(scale);
    }

    public DoubleProperty patternScaleProperty() {
        return patternScale;
    }

    private final IntegerProperty colors = new SimpleIntegerProperty(DEFAULT_COLORS) {

        @Override
        protected void invalidated() {
            createPalette(getColors());
        }
    };

    public final int getColors() {
        return colors.get();
    }

    public final void setColors(int value) {
        colors.set(value);
    }

    public IntegerProperty colorsProperty() {
        return colors;
    }

    private final ObjectProperty<ColorPalette> colorPalette = new SimpleObjectProperty<ColorPalette>(DEFAULT_COLOR_PALETTE) {

        @Override
        protected void invalidated() {
            createPalette(getColors());
            updateTexture();
            updateTextureOnFaces();
        }
    };

    public ColorPalette getColorPalette() {
        return colorPalette.get();
    }

    public final void setColorPalette(ColorPalette value) {
        colorPalette.set(value);
    }

    public ObjectProperty colorPaletteProperty() {
        return colorPalette;
    }

    private final ObjectProperty<Color> diffuseColor = new SimpleObjectProperty<Color>(DEFAULT_DIFFUSE_COLOR) {

        @Override
        protected void invalidated() {
            updateMaterial();
        }
    };

    private final ObjectProperty<CarbonPatterns> carbonPatterns = new SimpleObjectProperty<CarbonPatterns>(DEFAULT_PATTERN) {
        @Override
        protected void invalidated() {
            helper.getMaterialWithPattern(get());
        }
    };

    public final CarbonPatterns getCarbonPattern() {
        return carbonPatterns.get();
    }

    public final void setCarbonPattern(CarbonPatterns cp) {
        carbonPatterns.set(cp);
    }

    public ObjectProperty<CarbonPatterns> getCarbonPatterns() {
        return carbonPatterns;
    }

    public Color getDiffuseColor() {
        return diffuseColor.get();
    }

    public void setDiffuseColor(Color value) {
        diffuseColor.set(value);
    }

    public ObjectProperty diffuseColorProperty() {
        return diffuseColor;
    }


    private final ObjectProperty<Function<Point3D, Number>> density = new SimpleObjectProperty<Function<Point3D, Number>>(DEFAULT_DENSITY_FUNCTION) {

        @Override
        protected void invalidated() {
            helper.setDensity(density.get());
            updateTextureOnFaces();
        }
    };

    public final Function<Point3D, Number> getDensity() {
        return density.get();
    }

    public final void setDensity(Function<Point3D, Number> value) {
        this.density.set(value);
    }

    public final ObjectProperty<Function<Point3D, Number>> densityProperty() {
        return density;
    }

    private final ObjectProperty<Function<Number, Number>> function = new SimpleObjectProperty<Function<Number, Number>>(DEFAULT_UNIDIM_FUNCTION) {

        @Override
        protected void invalidated() {
            helper.setFunction(function.get());
            updateTextureOnFaces();
        }
    };

    public Function<Number, Number> getFunction() {
        return function.get();
    }

    public void setFunction(Function<Number, Number> value) {
        function.set(value);
    }

    public ObjectProperty functionProperty() {
        return function;
    }

    private final DoubleProperty minGlobal = new SimpleDoubleProperty();

    public double getMinGlobal() {
        return minGlobal.get();
    }

    public void setMinGlobal(double value) {
        minGlobal.set(value);
    }

    public DoubleProperty minGlobalProperty() {
        return minGlobal;
    }

    private final DoubleProperty maxGlobal = new SimpleDoubleProperty();

    public double getMaxGlobal() {
        return maxGlobal.get();
    }

    public void setMaxGlobal(double value) {
        maxGlobal.set(value);
    }

    public DoubleProperty maxGlobalProperty() {
        return maxGlobal;
    }

    //@INFO SMP
//    private final static double DEFAULT_OPACITY = 1.0;
//    private final static int DEFAULT_NUMCOLORS = 1530; // 39x40 palette image
//    public final static ColorPalette EASY_COLOR_PALETTE =
//            new FunctionColorPalette(1530, d -> Color.hsb(360d * d, 1, 1, DEFAULT_OPACITY));
    private void createPalette(int colors) {
//        helper.createPalette(colors, false, colorPalette.get());
        ColorPalette EASY_COLOR_PALETTE =
            new FunctionColorPalette(1530, d -> {
                if (d == 0) return Color.BLACK;
                if (d == 1) return Color.WHITE;
                return Color.hsb(360d * d, 1, 1, 1.0);
            });
        helper.createPalette(colors, false, EASY_COLOR_PALETTE);
        helper.getMaterialWithPalette();
    }

    public void updateMaterial() {
        helper.getMaterialWithColor(diffuseColor.get());
    }

    public void updateVertices(float factor) {
        if (mesh != null) {
            mesh.getPoints().setAll(helper.updateVertices(listVertices, factor));
        }
    }

    private void updateTexture() {
        if (mesh != null) {
            switch (textureType.get()) {
                case NONE:
                    mesh.getTexCoords().setAll(0f, 0f);
                    break;
                case IMAGE:
                    mesh.getTexCoords().setAll(textureCoords);
                    break;
                case PATTERN:
                    if (areaMesh.getHeight() > 0 && areaMesh.getWidth() > 0) {
                        mesh.getTexCoords().setAll(
                            helper.updateTexCoordsWithPattern((int) rectMesh.getWidth(),
                                (int) rectMesh.getHeight(), patternScale.get(),
                                areaMesh.getHeight() / areaMesh.getWidth()));
                    } else {
                        mesh.getTexCoords().setAll(
                            helper.updateTexCoordsWithPattern((int) rectMesh.getWidth(),
                                (int) rectMesh.getHeight(), patternScale.get()));
                    }
                    break;
                case COLORED_VERTICES_1D:
                    mesh.getTexCoords().setAll(helper.getTexturePaletteArray());
                    break;
                case COLORED_VERTICES_3D:
                    mesh.getTexCoords().setAll(helper.getTexturePaletteArray());
                    break;
                case COLORED_FACES:
                    mesh.getTexCoords().setAll(helper.getTexturePaletteArray());
                    break;
            }
        }
    }

    private void updateTextureOnFaces() {
        // textures for level
        if (mesh != null) {
            switch (textureType.get()) {
                case NONE:
                    mesh.getFaces().setAll(helper.updateFacesWithoutTexture(listFaces));
                    break;
                case IMAGE:
                    if (listTextures.size() > 0) {
                        mesh.getFaces().setAll(helper.updateFacesWithTextures(listFaces, listTextures));
                    } else {
                        mesh.getFaces().setAll(helper.updateFacesWithVertices(listFaces));
                    }
                    break;
                case PATTERN:
                    mesh.getFaces().setAll(helper.updateFacesWithTextures(listFaces, listTextures));
                    break;
                case COLORED_VERTICES_1D:
                    if (minGlobal.get() < maxGlobal.get()) {
                        mesh.getFaces().setAll(helper.updateFacesWithFunctionMap(listVertices, listFaces, minGlobal.get(), maxGlobal.get()));
                    } else {
                        mesh.getFaces().setAll(helper.updateFacesWithFunctionMap(listVertices, listFaces));
                    }
                    break;
                case COLORED_VERTICES_3D:
                    if (minGlobal.get() < maxGlobal.get()) {
                        mesh.getFaces().setAll(helper.updateFacesWithDensityMap(listVertices, listFaces, minGlobal.get(), maxGlobal.get()));
                    } else {
                        mesh.getFaces().setAll(helper.updateFacesWithDensityMap(listVertices, listFaces));
                    }
                    break;
                case COLORED_FACES:
                    mesh.getFaces().setAll(helper.updateFacesWithFaces(listFaces));
                    break;
            }
        }
    }

    protected abstract void updateMesh();

    /*
    This method allows replacing the original mesh with one given by a TriangleMesh
    being set with MeshHelper.

    This allows combining several meshes into one, creating one single node.
    */
    protected void updateMesh(MeshHelper meshHelper) {
        setMesh(null);
        mesh = createMesh(meshHelper);
        setMesh(mesh);
    }

    protected void createTexCoords(int width, int height) {
        rectMesh.setWidth(width);
        rectMesh.setHeight(height);
        textureCoords = helper.createTexCoords(width, height);
    }

    protected void createReverseTexCoords(int width, int height) {
        rectMesh.setWidth(width);
        rectMesh.setHeight(height);
        textureCoords = helper.createReverseTexCoords(width, height);
    }

    protected MeshHelper precreateMesh() {
        MeshHelper mh = new MeshHelper();
        mh.setPoints(helper.updateVertices(listVertices));
        switch (textureType.get()) {
            case NONE:
                mh.setTexCoords(textureCoords);
                mh.setFaces(helper.updateFacesWithTextures(listFaces, listTextures));
                break;
            case PATTERN:
                if (areaMesh.getHeight() > 0 && areaMesh.getWidth() > 0) {
                    mh.setTexCoords(
                        helper.updateTexCoordsWithPattern((int) rectMesh.getWidth(),
                            (int) rectMesh.getHeight(), patternScale.get(),
                            areaMesh.getHeight() / areaMesh.getWidth()));
                } else {
                    mh.setTexCoords(
                        helper.updateTexCoordsWithPattern((int) rectMesh.getWidth(),
                            (int) rectMesh.getHeight(), patternScale.get()));
                }
                mh.setFaces(helper.updateFacesWithTextures(listFaces, listTextures));
                break;
            case IMAGE:
                mh.setTexCoords(textureCoords);
                if (listTextures.size() > 0) {
                    mh.setFaces(helper.updateFacesWithTextures(listFaces, listTextures));
                } else {
                    mh.setFaces(helper.updateFacesWithVertices(listFaces));
                }
                break;
            case COLORED_VERTICES_1D:
                mh.setTexCoords(helper.getTexturePaletteArray());
                mh.setFaces(helper.updateFacesWithFunctionMap(listVertices, listFaces));
                break;
            case COLORED_VERTICES_3D:
                mh.setTexCoords(helper.getTexturePaletteArray());
                mh.setFaces(helper.updateFacesWithDensityMap(listVertices, listFaces));
                break;
            case COLORED_FACES:
                mh.setTexCoords(helper.getTexturePaletteArray());
                mh.setFaces(helper.updateFacesWithFaces(listFaces));
                break;
        }

        int[] faceSmoothingGroups = new int[listFaces.size()]; // 0 == hard edges
        Arrays.fill(faceSmoothingGroups, 1); // 1: soft edges, all the faces in same surface
        if (smoothingGroups != null) {
            mh.setFaceSmoothingGroups(smoothingGroups);
        } else {
            mh.setFaceSmoothingGroups(faceSmoothingGroups);
        }

        return mh;
    }

    protected TriangleMesh createMesh(MeshHelper mh) {
        float[] points0 = mh.getPoints();
        float[] f = mh.getF();
        listVertices.clear();
        listVertices.addAll(IntStream.range(0, points0.length / 3)
            .mapToObj(i -> new Point3D(points0[3 * i], points0[3 * i + 1], points0[3 * i + 2], f[i]))
            .collect(Collectors.toList()));

        textureCoords = mh.getTexCoords();

        int[] faces0 = mh.getFaces();
        listFaces.clear();
        listFaces.addAll(IntStream.range(0, faces0.length / 6)
            .mapToObj(i -> new Face3(faces0[6 * i + 0], faces0[6 * i + 2], faces0[6 * i + 4]))
            .collect(Collectors.toList()));

        listTextures.clear();
        listTextures.addAll(IntStream.range(0, faces0.length / 6)
            .mapToObj(i -> new Face3(faces0[6 * i + 1], faces0[6 * i + 3], faces0[6 * i + 5]))
            .collect(Collectors.toList()));

        smoothingGroups = mh.getFaceSmoothingGroups();

        return createMesh();
    }

    protected TriangleMesh createMesh() {
        TriangleMesh triangleMesh = new TriangleMesh();
        triangleMesh.getPoints().setAll(helper.updateVertices(listVertices));
        switch (textureType.get()) {
            case NONE:
                triangleMesh.getTexCoords().setAll(textureCoords);
                triangleMesh.getFaces().setAll(helper.updateFacesWithTextures(listFaces, listTextures));
                break;
            case PATTERN:
                if (areaMesh.getHeight() > 0 && areaMesh.getWidth() > 0) {
                    triangleMesh.getTexCoords().setAll(
                        helper.updateTexCoordsWithPattern((int) rectMesh.getWidth(),
                            (int) rectMesh.getHeight(), patternScale.get(),
                            areaMesh.getHeight() / areaMesh.getWidth()));
                } else {
                    triangleMesh.getTexCoords().setAll(
                        helper.updateTexCoordsWithPattern((int) rectMesh.getWidth(),
                            (int) rectMesh.getHeight(), patternScale.get()));
                }
                triangleMesh.getFaces().setAll(helper.updateFacesWithTextures(listFaces, listTextures));
                break;
            case IMAGE:
                triangleMesh.getTexCoords().setAll(textureCoords);
                if (listTextures.size() > 0) {
                    triangleMesh.getFaces().setAll(helper.updateFacesWithTextures(listFaces, listTextures));
                } else {
                    triangleMesh.getFaces().setAll(helper.updateFacesWithVertices(listFaces));
                }
                break;
            case COLORED_VERTICES_1D:
                triangleMesh.getTexCoords().setAll(helper.getTexturePaletteArray());
                triangleMesh.getFaces().setAll(helper.updateFacesWithFunctionMap(listVertices, listFaces));
                break;
            case COLORED_VERTICES_3D:
                triangleMesh.getTexCoords().setAll(helper.getTexturePaletteArray());
                triangleMesh.getFaces().setAll(helper.updateFacesWithDensityMap(listVertices, listFaces));
                break;
            case COLORED_FACES:
                triangleMesh.getTexCoords().setAll(helper.getTexturePaletteArray());
                triangleMesh.getFaces().setAll(helper.updateFacesWithFaces(listFaces));
                break;
        }

        int[] faceSmoothingGroups = new int[listFaces.size()]; // 0 == hard edges
        Arrays.fill(faceSmoothingGroups, 1); // 1: soft edges, all the faces in same surface
        if (smoothingGroups != null) {
            triangleMesh.getFaceSmoothingGroups().addAll(smoothingGroups);
        } else {
            triangleMesh.getFaceSmoothingGroups().addAll(faceSmoothingGroups);
        }
        //@DEBUG SMP
        //System.out.println("nodes: "+listVertices.size()+", faces: "+listFaces.size());

        return triangleMesh;
    }

    protected void updateTransforms() {
        getTransforms().removeAll(rotateX, rotateY, rotateZ, scale);
        Bounds bounds = getBoundsInLocal();
        javafx.geometry.Point3D p = new javafx.geometry.Point3D(
            (bounds.getMaxX() + bounds.getMinX()) / 2d,
            (bounds.getMaxY() + bounds.getMinY()) / 2d,
            (bounds.getMaxZ() + bounds.getMinZ()) / 2d);
        translate = new Translate(0, 0, 0);
        rotateX = new Rotate(0, p.getX(), p.getY(), p.getZ(), Rotate.X_AXIS);
        rotateY = new Rotate(0, p.getX(), p.getY(), p.getZ(), Rotate.Y_AXIS);
        rotateZ = new Rotate(0, p.getX(), p.getY(), p.getZ(), Rotate.Z_AXIS);
        scale = new Scale(1, 1, 1, p.getX(), p.getY(), p.getZ());
        getTransforms().addAll(translate, rotateZ, rotateY, rotateX, scale);
    }

    public Translate getTranslate() {
        if (translate == null) {
            updateTransforms();
        }
        return translate;
    }

    public Rotate getRotateX() {
        if (rotateX == null) {
            updateTransforms();
        }
        return rotateX;
    }

    public Rotate getRotateY() {
        if (rotateY == null) {
            updateTransforms();
        }
        return rotateY;
    }

    public Rotate getRotateZ() {
        if (rotateZ == null) {
            updateTransforms();
        }
        return rotateZ;
    }

    public Scale getScale() {
        if (scale == null) {
            updateTransforms();
        }
        return scale;
    }

    protected double polygonalSection(double angle) {
        if (sectionType.get().equals(SectionType.CIRCLE)) {
            return 1d;
        }
        int n = sectionType.get().getSides();
        return Math.cos(Math.PI / n) / Math.cos((2d * Math.atan(1d / Math.tan((n * angle) / 2d))) / n);
    }

    protected double polygonalSize(double radius) {
        if (sectionType.get().equals(SectionType.CIRCLE)) {
            return 2d * Math.PI * radius;
        }
        int n = sectionType.get().getSides();
        return n * Math.cos(Math.PI / n) * Math.log(-1d - 2d / (-1d + Math.sin(Math.PI / n))) * radius;
    }

    public Point3D getOrigin() {
        if (listVertices.size() > 0) {
            return listVertices.get(0);
        }
        return new Point3D(0f, 0f, 0f);
    }

    public int getIntersections(Point3D origin, Point3D direction) {
        setTextureModeFaces(10);

        int[] faces = helper.updateFacesWithIntersections(origin, direction, listVertices, listFaces);
        mesh.getFaces().setAll(faces);
        long time = System.currentTimeMillis();
        List<Face3> listIntersections = helper.getListIntersections(origin, direction, listVertices, listFaces);
        //@DEBUG SMP
        //System.out.println("t: "+(System.currentTimeMillis()-time));
        //listIntersections.forEach(System.out::println);
        return listIntersections.size();
    }
}
