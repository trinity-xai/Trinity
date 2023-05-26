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

import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;
import javafx.animation.AnimationTimer;
import javafx.scene.Group;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import org.fxyz3d.geometry.Point3D;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */
public class Manifold3D extends Group {
    public Group extrasGroup = new Group();
    public Group labelGroup = new Group();

    public QuickHull3D hull;
    public TriangleMesh quickhullTriangleMesh;
    public MeshView quickhullMeshView;
    public TriangleMesh quickhullLinesTriangleMesh;
    public MeshView quickhullLinesMeshView;

    Function<Point3D, Point3d> point3DToHullPoint = p -> new Point3d(p.x, p.y, p.z);
    Function<Point3d, Point3D> hullPointToPoint3D = p -> new Point3D(p.x, p.y, p.z);

    //allows 2D labels to track their 3D counterparts
    HashMap<Shape3D, Label> shape3DToLabel = new HashMap<>();
    AnimationTimer tessellationTimer;

    public Manifold3D(List<Point3D> point3DList, boolean triangulate, boolean makeLines, boolean makePoints) {
        hull = new QuickHull3D();
        //Construct an array of Point3D's
        com.github.quickhull3d.Point3d[] points = point3DList.stream()
            .map(point3DToHullPoint)
            .toArray(Point3d[]::new);
        hull.build(points);
        if (triangulate) {
            System.out.println("Triangulating...");
            hull.triangulate();
        }
        System.out.println("Faces: " + hull.getNumFaces());
        System.out.println("Verts: " + hull.getNumVertices());
        System.out.println("Making Quickhull mesh...");
        float artScale = 1.0f;
        quickhullTriangleMesh = makeQuickhullMesh(hull, artScale);
        quickhullMeshView = new MeshView(quickhullTriangleMesh);
        PhongMaterial quickhullMaterial = new PhongMaterial(Color.SKYBLUE);
        quickhullMeshView.setMaterial(quickhullMaterial);
        quickhullMeshView.setDrawMode(DrawMode.FILL);
        quickhullMeshView.setCullFace(CullFace.NONE);
        getChildren().addAll(quickhullMeshView);

        if (makeLines)
            makeLines();

        if (makePoints)
            makeDebugPoints(hull, artScale, false);

        ContextMenu cm = new ContextMenu();
        ColorPicker diffuseColorPicker = new ColorPicker(Color.SKYBLUE);
        diffuseColorPicker.valueProperty().addListener(cl -> {
            ((PhongMaterial) quickhullMeshView.getMaterial()).setDiffuseColor(diffuseColorPicker.getValue());
        });
        MenuItem diffuseColorItem = new MenuItem("Diffuse Color", diffuseColorPicker);
        ColorPicker specColorPicker = new ColorPicker(Color.SKYBLUE);
        specColorPicker.valueProperty().addListener(cl -> {
            ((PhongMaterial) quickhullMeshView.getMaterial()).setSpecularColor(specColorPicker.getValue());
        });
        MenuItem specColorItem = new MenuItem("Specular Color", specColorPicker);

        Spinner<Integer> spinner = new Spinner<>(10, 1000, 100, 10);
        MenuItem tessallateItem = new MenuItem("Tessellate", spinner);
        tessallateItem.setOnAction(e -> {
            Integer i = spinner.getValue();
            animateTessellation(i.longValue());
        });

        cm.getItems().addAll(diffuseColorItem, specColorItem, tessallateItem);
        cm.setAutoFix(true);
        cm.setAutoHide(true);
        cm.setHideOnEscape(true);
        cm.setOpacity(0.85);

        quickhullMeshView.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (null != cm)
                    if (!cm.isShowing())
                        cm.show(this.getParent(), e.getScreenX(), e.getScreenY());
                    else
                        cm.hide();
                e.consume();
            }
        });
    }

    public void matrixRotate(double alf, double bet, double gam) {
        double A11 = Math.cos(alf) * Math.cos(gam);
        double A12 = Math.cos(bet) * Math.sin(alf) + Math.cos(alf) * Math.sin(bet) * Math.sin(gam);
        double A13 = Math.sin(alf) * Math.sin(bet) - Math.cos(alf) * Math.cos(bet) * Math.sin(gam);
        double A21 = -Math.cos(gam) * Math.sin(alf);
        double A22 = Math.cos(alf) * Math.cos(bet) - Math.sin(alf) * Math.sin(bet) * Math.sin(gam);
        double A23 = Math.cos(alf) * Math.sin(bet) + Math.cos(bet) * Math.sin(alf) * Math.sin(gam);
        double A31 = Math.sin(gam);
        double A32 = -Math.cos(gam) * Math.sin(bet);
        double A33 = Math.cos(bet) * Math.cos(gam);

        double d = Math.acos((A11 + A22 + A33 - 1d) / 2d);
        if (d != 0d) {
            double den = 2d * Math.sin(d);
            javafx.geometry.Point3D p = new javafx.geometry.Point3D((A32 - A23) / den, (A13 - A31) / den, (A21 - A12) / den);
            setRotationAxis(p);
            setRotate(Math.toDegrees(d));
        }
    }

    public TriangleMesh makeQuickhullMesh(QuickHull3D hull, float scale) {
        TriangleMesh mesh = new TriangleMesh();

        for (int i = 0; i < hull.getNumVertices(); i++) {
            Point3d p3d = hull.getVertices()[i];
            Point3D point3D = hullPointToPoint3D.apply(p3d);
            mesh.getPoints().addAll(point3D.x * scale, point3D.y * scale, point3D.z * scale);
            mesh.getTexCoords().addAll(point3D.x * scale, point3D.z * scale);
        }
        for (int[] face : hull.getFaces()) {
            mesh.getFaces().addAll(face[0], face[2], face[1], face[1], face[2], face[0]);
        }

        return mesh;
    }

    public void animateTessellation(long ms) {
        if (null != tessellationTimer)
            tessellationTimer.stop();
        quickhullLinesTriangleMesh.getFaces().clear();
        quickhullLinesMeshView.setVisible(true);

        tessellationTimer = new AnimationTimer() {
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_MILLI = 1_000_000;
            int faceIndex = 0;

            @Override
            public void handle(long now) {
                sleepNs = ms * NANOS_IN_MILLI;
                if ((now - prevTime) < sleepNs) return;
                prevTime = now;

                quickhullLinesTriangleMesh.getFaces().addAll(
                    quickhullTriangleMesh.getFaces().get(faceIndex),
                    quickhullTriangleMesh.getFaces().get(faceIndex + 1),
                    quickhullTriangleMesh.getFaces().get(faceIndex + 2),
                    quickhullTriangleMesh.getFaces().get(faceIndex + 3),
                    quickhullTriangleMesh.getFaces().get(faceIndex + 4),
                    quickhullTriangleMesh.getFaces().get(faceIndex + 5)
                );

                faceIndex += 6;
                if (faceIndex >= quickhullTriangleMesh.getFaces().size()) {
                    this.stop();
                    System.out.println("Tessellation Complete.");
                }
            }

            ;
        };
        tessellationTimer.start();
    }

    public void makeLines() {
        quickhullLinesTriangleMesh = new TriangleMesh();
        quickhullLinesTriangleMesh.getPoints().addAll(quickhullTriangleMesh.getPoints());
        quickhullLinesTriangleMesh.getTexCoords().addAll(quickhullTriangleMesh.getTexCoords());
        quickhullLinesTriangleMesh.getFaces().addAll(quickhullTriangleMesh.getFaces());

        quickhullLinesMeshView = new MeshView(quickhullLinesTriangleMesh);
        PhongMaterial quickhullLinesMaterial = new PhongMaterial(Color.BLUE);
        quickhullLinesMeshView.setMaterial(quickhullLinesMaterial);
        quickhullLinesMeshView.setDrawMode(DrawMode.LINE);
        quickhullLinesMeshView.setCullFace(CullFace.NONE);
        quickhullLinesMeshView.setMouseTransparent(true);

        getChildren().add(quickhullLinesMeshView);
    }

    public void makeDebugPoints(QuickHull3D hull, float scale, boolean print) {
        if (print)
            System.out.print("double [] factors = new double[] { ");
        for (int i = 0; i < hull.getNumVertices(); i++) {
            Point3d p3d = hull.getVertices()[i];
            Point3D point3D = hullPointToPoint3D.apply(p3d);
            point3D.x *= scale;
            point3D.y *= scale;
            point3D.z *= scale;
            if (print) {
                System.out.print(point3D.x + ", " + point3D.y + ", " + point3D.z);
                if (i < hull.getNumVertices() - 1)
                    System.out.print(", ");
            }
            Sphere sphere = new Sphere(2.5);
            PhongMaterial mat = new PhongMaterial(Color.BLUE);
            sphere.setMaterial(mat);
            sphere.setTranslateX(point3D.x);
            sphere.setTranslateY(point3D.y);
            sphere.setTranslateZ(point3D.z);
            extrasGroup.getChildren().add(sphere);
            Label newLabel = new Label(String.valueOf(i));
            labelGroup.getChildren().addAll(newLabel);
            newLabel.setTextFill(Color.SKYBLUE);
            newLabel.setFont(new Font("calibri", 20));
            shape3DToLabel.put(sphere, newLabel);
        }
        if (print)
            System.out.println("};");
        getChildren().add(extrasGroup);
    }
}
