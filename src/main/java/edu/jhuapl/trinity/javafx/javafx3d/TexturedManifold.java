package edu.jhuapl.trinity.javafx.javafx3d;

/*-
 * #%L
 * trinity-2023.08.19
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

import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.geometry.Face3;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.primitives.TexturedMesh;

import java.util.List;

/**
 * @author phillsm1
 */
public class TexturedManifold extends TexturedMesh {

    List<Point3D> vertices;
    List<Face3> faces;

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
        for (int i = 0; i < vertices.size(); i++) {
            point3D = vertices.get(i);
            mesh.getPoints().addAll(point3D.x, point3D.y, point3D.z);
            mesh.getTexCoords().addAll(point3D.x, point3D.z);
        }
        for (Face3 face : faces) {
            mesh.getFaces().addAll(face.p0, face.p2, face.p1, face.p1, face.p2, face.p0);
//            mesh.getFaceSmoothingGroups().addAll(face[2], face[1],face[0]);
        }
        return mesh;
    }

}
