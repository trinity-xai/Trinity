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

import java.util.Arrays;
import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.geometry.Face3;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.primitives.TexturedMesh;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 * @author Birdasaur
 */
public class TexturedManifold extends TexturedMesh {

    private List<Point3D> vertices;
    private List<Face3> faces;
    int div;
    public TexturedManifold(List<Point3D> vertices, List<Face3> faces) {
        this.vertices = vertices;
        this.faces = faces;
        div = 64;
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
//            mesh.getTexCoords().addAll(point3D.x, point3D.z);
        }
        div = getVertices().size();
        final float rDiv = 1.f / div;
        float textureDelta = 1.f / 256;
        //2*r*PI x 2*r        
//        double h = 2 * r;
//        double w = 2 * r * 3.125; // 3.125 is ~ PI, rounded to get perfect squares.
        
        Point3D centroid = getAverageConvexCentroid();
        float maxY = getMaxY().floatValue();
        float maxX = getMaxX().floatValue();
        float radius = 0;
//        for (int i = 0; i < div; ++i) {
        for (int i = 0; i < getVertices().size(); i++) {
            radius = distance(getVertices().get(i),centroid).floatValue();
            mesh.getTexCoords().addAll(
//            tPoints[tPos + 0] = 1.0f - rDiv * (0.5f + i);
              getVertices().get(i).getX()/maxX, getVertices().get(i).getY()/maxY
            );
        }        
        
        for (Face3 face : getFaces()) {
            mesh.getFaces().addAll(face.p0, face.p2, face.p1, face.p1, face.p2, face.p0);
//            mesh.getFaceSmoothingGroups().addAll(face[2], face[1],face[0]);
        }
        return mesh;
    }
    private Double getMaxX() {
        return getVertices().stream()
            .flatMapToDouble(p -> DoubleStream.of(Math.abs(p.x)))
            .max().getAsDouble();
    }    private Double getMaxY() {
        return getVertices().stream()
            .flatMapToDouble(p -> DoubleStream.of(Math.abs(p.y)))
            .max().getAsDouble();
    }
    private Double distance(Point3D p1, Point3D p2) {
        return Math.sqrt(
            ((p2.getX()-p1.getX())*(p2.getX()-p1.getX()))+
            ((p2.getY()-p1.getY())*(p2.getY()-p1.getY()))+
            ((p2.getZ()-p1.getZ())*(p2.getZ()-p1.getZ()))
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
