/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.javafx3d;

import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.geometry.Face3;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.primitives.TexturedMesh;

import java.util.List;
import java.util.stream.DoubleStream;

/**
 * @author Sean Phillips
 */
public class TexturedManifold extends TexturedMesh {

    private List<Point3D> vertices;
    private List<Face3> faces;

    public TexturedManifold(List<Point3D> vertices, List<Face3> faces) {
        this.vertices = vertices;
        this.faces = faces;
        updateMesh();
    }

    @Override
    protected void updateMesh() {
        setMesh(makeMesh());
    }

    private TriangleMesh makeMesh() {
        TriangleMesh mesh = new TriangleMesh();
        Point3D point3D;
        for (int i = 0; i < getVertices().size(); i++) {
            point3D = getVertices().get(i);
            mesh.getPoints().addAll(point3D.x, point3D.y, point3D.z);
        }
        float maxY = getMaxY().floatValue();
        float maxX = getMaxX().floatValue();
        for (int i = 0; i < getVertices().size(); i++) {
            mesh.getTexCoords().addAll(
                getVertices().get(i).getX() / maxX, getVertices().get(i).getY() / maxY
            );
        }

        for (Face3 face : getFaces()) {
            mesh.getFaces().addAll(
                face.p0, face.p2, face.p1,
                face.p1, face.p2, face.p0
            );
        }
        return mesh;
    }

    public void flipFaces(int indexSkip) {
        TriangleMesh tm = (TriangleMesh) getMesh();
        int size = tm.getFaces().size();
        int swap = 0;
        for (int i = 0; i < size; i += indexSkip) {
            swap = tm.getFaces().get(i);
            tm.getFaces().set(i, tm.getFaces().get(i + 2));
            tm.getFaces().set(i + 2, swap);
            if (i + indexSkip + 3 > size) //safety check
                return;
        }
    }

    private Double getMaxX() {
        return getVertices().stream()
            .flatMapToDouble(p -> DoubleStream.of(Math.abs(p.x)))
            .max().getAsDouble();
    }

    private Double getMaxY() {
        return getVertices().stream()
            .flatMapToDouble(p -> DoubleStream.of(Math.abs(p.y)))
            .max().getAsDouble();
    }

    private Double distance(Point3D p1, Point3D p2) {
        return Math.sqrt(
            ((p2.getX() - p1.getX()) * (p2.getX() - p1.getX())) +
                ((p2.getY() - p1.getY()) * (p2.getY() - p1.getY())) +
                ((p2.getZ() - p1.getZ()) * (p2.getZ() - p1.getZ()))
        );
    }

    public Point3D getAverageConvexCentroid() {
        double aveX = getVertices().stream()
            .flatMapToDouble(p -> DoubleStream.of(p.x))
            .average().getAsDouble();
        double aveY = getVertices().stream()
            .flatMapToDouble(p -> DoubleStream.of(p.y))
            .average().getAsDouble();
        double aveZ = getVertices().stream()
            .flatMapToDouble(p -> DoubleStream.of(p.z))
            .average().getAsDouble();
        return new Point3D(aveX, aveY, aveZ);
    }

    /**
     * @return the vertices
     */
    public List<Point3D> getVertices() {
        return vertices;
    }

    /**
     * @return the faces
     */
    public List<Face3> getFaces() {
        return faces;
    }

}
