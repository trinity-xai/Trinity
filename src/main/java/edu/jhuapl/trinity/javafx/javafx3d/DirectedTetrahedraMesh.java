/*
 * @DERIVED FROM FXyz TetrahedraMesh class. Below is the copyright and link to GNU GPL
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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.DepthTest;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;
import org.fxyz3d.geometry.Face3;
import org.fxyz3d.geometry.Point3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * @author José Pereda Llamas
 * Created on 22-dic-2014 - 21:51:51
 * @adapted Sean M Phillips
 * 02/04/2020
 */
public class DirectedTetrahedraMesh extends DirectedTexturedMesh {
    private static final Logger LOG = LoggerFactory.getLogger(DirectedTetrahedraMesh.class);

    private final static double DEFAULT_HEIGHT = 10;
    private final static int DEFAULT_LEVEL = 1;
    private final static Point3D DEFAULT_CENTER = new Point3D(0f, 0f, 0f);


    public DirectedTetrahedraMesh() {
        this(DEFAULT_HEIGHT, DEFAULT_LEVEL, null, null);
    }

    public DirectedTetrahedraMesh(double height) {
        this(height, DEFAULT_LEVEL, null, null);
    }

    public DirectedTetrahedraMesh(double height, int level, Point3D center, Point3D endPoint) {
        setHeight(height);
        setLevel(level);
        setCenter(center);
        setEndPoint(endPoint);
        updateMesh();
        setCullFace(CullFace.BACK);
        setDrawMode(DrawMode.FILL);
        setDepthTest(DepthTest.ENABLE);
    }

    private final DoubleProperty height = new SimpleDoubleProperty(DEFAULT_HEIGHT) {

        @Override
        protected void invalidated() {
            if (mesh != null) {
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
            if (mesh != null) {
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

    private final ObjectProperty<Point3D> center = new SimpleObjectProperty<Point3D>(DEFAULT_CENTER) {

        @Override
        protected void invalidated() {
            if (mesh != null) {
                updateMesh();
            }
        }

    };

    public Point3D getCenter() {
        return center.get();
    }

    public final void setCenter(Point3D value) {
        center.set(value);
    }

    public ObjectProperty<Point3D> centerProperty() {
        return center;
    }

    private final ObjectProperty<Point3D> endPoint = new SimpleObjectProperty<Point3D>(DEFAULT_CENTER) {
        @Override
        protected void invalidated() {
            if (mesh != null) {
                updateMesh();
            }
        }
    };

    public Point3D getEndPoint() {
        return endPoint.get();
    }

    public final void setEndPoint(Point3D value) {
        endPoint.set(value);
    }

    public ObjectProperty<Point3D> endPointProperty() {
        return endPoint;
    }

    @Override
    protected final void updateMesh() {
        setMesh(null);
        mesh = createTetrahedra((float) getHeight(), getLevel());
        setMesh(mesh);
    }

    private int numVertices, numTexCoords, numFaces;
    private float[] points0, texCoord0;
    private int[] faces0;
    private List<Point2D> texCoord1;
    private Transform a = new Affine();

    private TriangleMesh createTetrahedra(float height, int level) {

        TriangleMesh m0 = null;
        if (level > 0) {
            m0 = createTetrahedra(height, level - 1);
        }

        if (level == 0) {
            a = new Affine();
            float hw = height;
            if (center.get() != null) {
                a = a.createConcatenation(new Translate(center.get().x, center.get().y, center.get().z));
            }
            float xAdd = 0.0f;
            float yAdd = 0.0f;
            float zAdd = 0.0f;
            if (null != endPoint.get()) {
                xAdd = endPoint.get().x;
                yAdd = endPoint.get().y;
                zAdd = endPoint.get().z;
            }
            final float[] baseVertices = new float[]{
                0f + xAdd, 0f + yAdd, (0.612372f + zAdd) * hw,
                -0.288675f * hw, -0.5f * hw, -0.204124f * hw,
                -0.288675f * hw, 0.5f * hw, -0.204124f * hw,
                0.57735f * hw, 0f, -0.204124f * hw
            };

            final float[] baseTexCoords = new float[]{
                0f, 0f,
                0.5f, 0.866025f,
                1f, 0f,
                1f, 1.73205f,
                1.5f, 0.866025f,
                2f, 0f
            };

            final int[] baseTexture = new int[]{
                0, 2, 1,
                3, 1, 4,
                2, 4, 1,
                4, 2, 5
            };

            final List<Integer> baseFaces = Arrays.asList(
                1, 2, 3,
                2, 1, 0,
                3, 0, 1,
                0, 3, 2
            );

            for (int i = 0; i < baseVertices.length / 3; i++) {
                Point3D ta = transform(baseVertices[3 * i], baseVertices[3 * i + 1], baseVertices[3 * i + 2]);
                baseVertices[3 * i] = ta.x;
                baseVertices[3 * i + 1] = ta.y;
                baseVertices[3 * i + 2] = ta.z;
            }
            points0 = baseVertices;
            numVertices = baseVertices.length / 3;

            texCoord0 = baseTexCoords;
            numTexCoords = baseTexCoords.length / 2;

            faces0 = IntStream.range(0, baseFaces.size() / 3)
                .mapToObj(i -> IntStream.of(baseFaces.get(3 * i), baseTexture[3 * i],
                    baseFaces.get(3 * i + 1), baseTexture[3 * i + 1],
                    baseFaces.get(3 * i + 2), baseTexture[3 * i + 2]))
                .flatMapToInt(i -> i).toArray();
            numFaces = baseFaces.size() / 3;
        } else if (m0 != null) {
            points0 = new float[numVertices * m0.getPointElementSize()];
            m0.getPoints().toArray(points0);
        }


        List<Point3D> points1 = IntStream.range(0, numVertices)
            .mapToObj(i -> new Point3D(points0[3 * i], points0[3 * i + 1], points0[3 * i + 2]))
            .collect(Collectors.toList());

        if (level > 0 && m0 != null) {
            texCoord0 = new float[numTexCoords * m0.getTexCoordElementSize()];
            m0.getTexCoords().toArray(texCoord0);
        }

        texCoord1 = IntStream.range(0, numTexCoords)
            .mapToObj(i -> new Point2D(texCoord0[2 * i], texCoord0[2 * i + 1]))
            .collect(Collectors.toList());

        if (level > 0 && m0 != null) {
            faces0 = new int[numFaces * m0.getFaceElementSize()];
            m0.getFaces().toArray(faces0);
        }

        List<Face3> faces1 = IntStream.range(0, numFaces)
            .mapToObj(i -> new Face3(faces0[6 * i], faces0[6 * i + 2], faces0[6 * i + 4]))
            .collect(Collectors.toList());

        index.set(points1.size());
        map.clear();
        listVertices.clear();
        listFaces.clear();
        listVertices.addAll(points1);

        faces1.forEach(face -> {
            int v1 = face.p0;
            int v2 = face.p1;
            int v3 = face.p2;
            if (level > 0) {
                int a = getMiddle(v1, points1.get(v1), v2, points1.get(v2));
                int b = getMiddle(v2, points1.get(v2), v3, points1.get(v3));
                int c = getMiddle(v3, points1.get(v3), v1, points1.get(v1));

                listFaces.add(new Face3(v1, a, c));
                listFaces.add(new Face3(v2, b, a));
                listFaces.add(new Face3(v3, c, b));
                listFaces.add(new Face3(a, b, c));
            } else {
                listFaces.add(new Face3(v1, v2, v3));
            }
        });
        map.clear();
        numVertices = listVertices.size();
        numFaces = listFaces.size();

        List<Face3> textures1;
        if (level == 0) {
            textures1 = IntStream.range(0, faces0.length / 6)
                .mapToObj(i -> new Face3(faces0[6 * i + 1], faces0[6 * i + 3], faces0[6 * i + 5]))
                .collect(Collectors.toList());
        } else {
            textures1 = listTextures.stream().map(t -> t).collect(Collectors.toList());
        }

        index.set(texCoord1.size());
        listTextures.clear();
        textures1.forEach(face -> {
            int v1 = face.p0;
            int v2 = face.p1;
            int v3 = face.p2;
            if (level > 0) {
                int a = getMiddle(v1, texCoord1.get(v1), v2, texCoord1.get(v2));
                int b = getMiddle(v2, texCoord1.get(v2), v3, texCoord1.get(v3));
                int c = getMiddle(v3, texCoord1.get(v3), v1, texCoord1.get(v1));

                listTextures.add(new Face3(v1, a, c));
                listTextures.add(new Face3(v2, b, a));
                listTextures.add(new Face3(v3, c, b));
                listTextures.add(new Face3(a, b, c));
            } else {
                listTextures.add(new Face3(v1, v2, v3));
            }
        });
        map.clear();

        texCoord0 = texCoord1.stream().flatMapToDouble(p -> DoubleStream.of(p.getX(), p.getY()))
            .collect(() -> new FloatCollector(texCoord1.size() * 2), FloatCollector::add, FloatCollector::join).toArray();
        numTexCoords = texCoord0.length / 2;
        textureCoords = texCoord0;
        if (level == getLevel()) {
            areaMesh.setWidth(2f * height);
            areaMesh.setHeight(height * Math.sqrt(3));

            // 1<<j -> bitset, 00100. Otherwise: 000111 will mean they are shared
            smoothingGroups = IntStream.range(0, listFaces.size()).map(i -> 1 << (i / (listFaces.size() / 4))).toArray();
        }
        return createMesh();
    }

    private Point3D transform(Point3D p) {
        javafx.geometry.Point3D ta = a.transform(p.x, p.y, p.z);
        return new Point3D((float) ta.getX(), (float) ta.getY(), (float) ta.getZ());
    }

    private Point3D transform(double x, double y, double z) {
        javafx.geometry.Point3D ta = a.transform(x, y, z);
        return new Point3D((float) ta.getX(), (float) ta.getY(), (float) ta.getZ());
    }

    public Point3D unTransform(Point3D p) {
        try {
            javafx.geometry.Point3D ta = a.inverseTransform(p.x, p.y, p.z);
            return new Point3D((float) ta.getX(), (float) ta.getY(), (float) ta.getZ());
        } catch (NonInvertibleTransformException ex) {
            LOG.info("p not invertible {}", p);
        }
        return p;
    }

    private final AtomicInteger index = new AtomicInteger();
    private final HashMap<String, Integer> map = new HashMap<>();

    private int getMiddle(int v1, Point3D p1, int v2, Point3D p2) {
        String key = "" + Math.min(v1, v2) + "_" + Math.max(v1, v2);
        if (map.get(key) != null) {
            return map.get(key);
        }

        listVertices.add(p1.add(p2).multiply(0.5f));

        map.put(key, index.get());
        return index.getAndIncrement();
    }

    private int getMiddle(int v1, Point2D p1, int v2, Point2D p2) {
        String key = "" + Math.min(v1, v2) + "_" + Math.max(v1, v2);
        if (map.get(key) != null) {
            return map.get(key);
        }

        texCoord1.add(p1.add(p2).multiply(0.5f));

        map.put(key, index.get());
        return index.getAndIncrement();
    }

}
