/*
 * @DERIVED FROM FXyz ScatterMesh class. Below is the copyright and link to GNU GPL
 * Copyright (C) 2013-2015 F(X)yz,
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
/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.scene.paint.Palette.ColorPalette;
import org.fxyz3d.scene.paint.Patterns;
import org.fxyz3d.shapes.primitives.helper.TextureMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author José Pereda
 * @adapted Sean M Phillips
 */
public class DirectedScatterMesh extends Group implements TextureMode {

    private final static List<Point3D> DEFAULT_SCATTER_DATA = Arrays.asList(new Point3D(0f, 0f, 0f),
        new Point3D(1f, 1f, 1f), new Point3D(2f, 2f, 2f));
    private final static double DEFAULT_HEIGHT = 0.1d;
    private final static int DEFAULT_LEVEL = 0;
    private final static boolean DEFAULT_JOIN_SEGMENTS = true;
    private ObservableList<DirectedTexturedMesh> meshes = FXCollections.<DirectedTexturedMesh>observableArrayList();
    public boolean renderOnChange = true;

    public DirectedScatterMesh() {
        this(DEFAULT_SCATTER_DATA, DEFAULT_JOIN_SEGMENTS, DEFAULT_HEIGHT, DEFAULT_LEVEL);
    }

    public DirectedScatterMesh(List<Point3D> scatterData) {
        this(scatterData, DEFAULT_JOIN_SEGMENTS, DEFAULT_HEIGHT, DEFAULT_LEVEL);
    }

    public DirectedScatterMesh(List<Point3D> scatterData, double height) {
        this(scatterData, DEFAULT_JOIN_SEGMENTS, height, DEFAULT_LEVEL);
    }

    public DirectedScatterMesh(List<Point3D> scatterData, boolean joinSegments, double height, int level) {
        setScatterData(scatterData);
        setJoinSegments(joinSegments);
        setHeight(height);
        setLevel(level);
        updateMesh();
    }

    public DirectedScatterMesh(List<Point3D> scatterData, List<Point3D> endPoints, boolean joinSegments, double height, int level) {
        setScatterData(scatterData);
        setEndPoints(endPoints);
        setJoinSegments(joinSegments);
        setHeight(height);
        setLevel(level);
        updateMesh();
    }

    private final ObjectProperty<List<Point3D>> scatterData = new SimpleObjectProperty<List<Point3D>>(DEFAULT_SCATTER_DATA) {
        @Override
        protected void invalidated() {
            if (meshes != null && renderOnChange) {
                updateMesh();
            }
        }
    };

    public List<Point3D> getScatterData() {
        return scatterData.get();
    }

    public final void setScatterData(List<Point3D> value) {
        scatterData.set(value);
    }

    public ObjectProperty<List<Point3D>> scatterDataProperty() {
        return scatterData;
    }

    private final ObjectProperty<List<Point3D>> endPoints = new SimpleObjectProperty<List<Point3D>>(DEFAULT_SCATTER_DATA) {
        @Override
        protected void invalidated() {
            if (meshes != null && renderOnChange) {
                updateMesh();
            }
        }
    };

    public List<Point3D> getEndPoints() {
        return endPoints.get();
    }

    public final void setEndPoints(List<Point3D> value) {
        endPoints.set(value);
    }

    public ObjectProperty<List<Point3D>> endPointsProperty() {
        return endPoints;
    }

    public final void setScatterDataAndEndPoints(List<Point3D> data, List<Point3D> ends) {
        renderOnChange = false;  //temporarily disable automatic rendering
        scatterData.setValue(data);
        endPoints.setValue(ends);
        renderOnChange = true; //re-enable automatic rendering
        if (meshes != null) updateMesh();
    }

    private final ObjectProperty<List<Number>> functionData = new SimpleObjectProperty<List<Number>>() {
        @Override
        protected void invalidated() {
            if (meshes != null) {
                updateF(get());
            }
        }
    };

    public List<Number> getFunctionData() {
        return functionData.get();
    }

    public void setFunctionData(List<Number> value) {
        functionData.set(value);
    }

    public ObjectProperty<List<Number>> functionDataProperty() {
        return functionData;
    }

    private final DoubleProperty height = new SimpleDoubleProperty(DEFAULT_HEIGHT) {
        @Override
        protected void invalidated() {
            if (meshes != null) {
                updateMesh();
            }
        }
    };

    public double getHeight() {
        return height.get();
    }

    public final void setHeight(double value) {
        height.set(value);
    }

    public DoubleProperty heightProperty() {
        return height;
    }

    private final IntegerProperty level = new SimpleIntegerProperty(DEFAULT_LEVEL) {

        @Override
        protected void invalidated() {
            if (meshes != null) {
                updateMesh();
            }
        }

    };

    public final int getLevel() {
        return level.get();
    }

    public final void setLevel(int value) {
        level.set(value);
    }

    public final IntegerProperty levelProperty() {
        return level;
    }

    private final BooleanProperty joinSegments = new SimpleBooleanProperty(DEFAULT_JOIN_SEGMENTS) {
        @Override
        protected void invalidated() {
            if (meshes != null) {
                updateMesh();
            }
        }
    };

    public boolean isJoinSegments() {
        return joinSegments.get();
    }

    public final void setJoinSegments(boolean value) {
        joinSegments.set(value);
    }

    public BooleanProperty joinSegmentsProperty() {
        return joinSegments;
    }

    protected final void updateMesh() {
        meshes.clear();
        createDots();
        getChildren().setAll(meshes);
        updateTransforms();
    }

    /**
     * @WIP @EXPERIMENTAL
     */
    public void dirtyUpdate(List<Point3D> startPoints, List<Point3D> endPoints) {
        renderOnChange = false;  //temporarily disable automatic rendering
        if (meshes != null) {
            addToMesh(startPoints, endPoints);
            updateTransforms();
        }
        renderOnChange = true; //re-enable automatic rendering
    }

    /**
     * @WIP @EXPERIMENTAL
     */
    public void addToMesh(List<Point3D> startPoints, List<Point3D> endPoints) {
        DirectedTexturedMesh originalTexturedMesh = meshes.get(0); //get current mesh
        DirectedMeshHelper originalMeshHelper = new DirectedMeshHelper((TriangleMesh) originalTexturedMesh.getMesh());

        DirectedTexturedMesh newTexturedMesh = new DirectedTetrahedraMesh(
            height.get(), level.get(), null, null);
        DirectedMeshHelper newTexturedMeshHelper = new DirectedMeshHelper((TriangleMesh) newTexturedMesh.getMesh());

        originalMeshHelper.addMesh(newTexturedMeshHelper,
            startPoints, endPoints);
        originalTexturedMesh.updateMesh(originalMeshHelper);
        meshes.add(originalTexturedMesh);
    }

    private AtomicInteger index;

    private void createDots() {
        if (!joinSegments.get()) {
            List<DirectedTexturedMesh> dots = new ArrayList<>();
            index = new AtomicInteger();
            scatterData.get().forEach(point3d -> {
                index.getAndIncrement();
                //Get endpoint
                Point3D endPoint = endPoints.get().get(index.get() - 1);
                DirectedTexturedMesh dot = new DirectedTetrahedraMesh(height.get(), level.get(), point3d, endPoint);
                dot.setCullFace(CullFace.BACK);
                dot.setDrawMode(DrawMode.FILL);
                dot.setDepthTest(DepthTest.ENABLE);
                dot.setId("" + index);
                dots.add(dot);
            });
            meshes.addAll(dots);
        } else {
//            Point3D dotZeroEndPoint = endPoints.get().get(0);
//            DirectedTexturedMesh dot = new DirectedTetrahedraMesh(
//                height.get(), level.get(), scatterData.get().get(0), dotZeroEndPoint);
            DirectedTexturedMesh dot = new DirectedTetrahedraMesh(
                height.get(), level.get(),
                new Point3D(0, 0, 0, 0), new Point3D(0, 0, 0, 0));
            dot.setCullFace(CullFace.BACK);
            dot.setDrawMode(DrawMode.FILL);
            dot.setDepthTest(DepthTest.ENABLE);
            dot.setId("0");

            DirectedMeshHelper mh = new DirectedMeshHelper((TriangleMesh) dot.getMesh());
            DirectedTexturedMesh dot1 = new DirectedTetrahedraMesh(height.get(), level.get(), null, null);
            DirectedMeshHelper mh1 = new DirectedMeshHelper((TriangleMesh) dot1.getMesh());
            mh.addMesh(mh1,
                scatterData.get().stream().skip(1).toList(),
                endPoints.get().stream().skip(1).toList());
//            mh.addMesh(mh1,
//                scatterData.get().subList(1, scatterData.get().size()),
//                endPoints.get().subList(1, endPoints.get().size()));
            dot.updateMesh(mh);
            meshes.add(dot);
        }
    }

    @Override
    public void setTextureModeNone() {
        meshes.stream().forEach(m -> m.setTextureModeNone());
    }

    @Override
    public void setTextureModeNone(Color color) {
        meshes.stream().forEach(m -> m.setTextureModeNone(color));
    }

    @Override
    public void setTextureModeNone(Color color, String image) {
        meshes.stream().forEach(m -> m.setTextureModeNone(color, image));
    }

    @Override
    public void setTextureModeImage(String image) {
        meshes.stream().forEach(m -> m.setTextureModeImage(image));
    }

    @Override
    public void setTextureModePattern(Patterns.CarbonPatterns pattern, double scale) {
        meshes.stream().forEach(m -> m.setTextureModePattern(pattern, scale));
    }

    @Override
    public void setTextureModeVertices3D(int colors, Function<Point3D, Number> dens) {
        meshes.stream().forEach(m -> m.setTextureModeVertices3D(colors, dens));
    }

    @Override
    public void setTextureModeVertices3D(ColorPalette palette, Function<Point3D, Number> dens) {
        meshes.stream().forEach(m -> m.setTextureModeVertices3D(palette, dens));
    }

    @Override
    public void setTextureModeVertices3D(int colors, Function<Point3D, Number> dens, double min, double max) {
        meshes.stream().forEach(m -> m.setTextureModeVertices3D(colors, dens, min, max));
    }

    @Override
    public void setTextureModeVertices1D(int colors, Function<Number, Number> function) {
        meshes.stream().forEach(m -> m.setTextureModeVertices1D(colors, function));
    }

    @Override
    public void setTextureModeVertices1D(ColorPalette palette, Function<Number, Number> function) {
        meshes.stream().forEach(m -> m.setTextureModeVertices1D(palette, function));
    }

    @Override
    public void setTextureModeVertices1D(int colors, Function<Number, Number> function, double min, double max) {
        meshes.stream().forEach(m -> m.setTextureModeVertices1D(colors, function, min, max));
    }

    @Override
    public void setTextureModeFaces(int colors) {
        meshes.stream().forEach(m -> m.setTextureModeFaces(colors));
    }

    @Override
    public void setTextureModeFaces(ColorPalette palette) {
        meshes.stream().forEach(m -> m.setTextureModeFaces(palette));
    }

    @Override
    public void updateF(List<Number> values) {
        meshes.stream().forEach(m -> m.updateF(values));
    }

    public void setDrawMode(DrawMode mode) {
        meshes.stream().forEach(m -> m.setDrawMode(mode));
    }

    private void updateTransforms() {
        meshes.stream().forEach(m -> m.updateTransforms());
    }

    public DirectedTexturedMesh getMeshFromId(String id) {
        return meshes.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(meshes.get(0));
    }

    @Override
    public void setTextureOpacity(double value) {
        meshes.stream().forEach(m -> m.setTextureOpacity(value));
    }
}
