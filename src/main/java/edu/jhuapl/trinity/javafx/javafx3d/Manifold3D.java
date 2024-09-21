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

import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;
import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.utils.ConcaveUtils;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.fxyz3d.geometry.Face3;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.scene.paint.Palette;
import org.fxyz3d.shapes.primitives.helper.TriangleMeshHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.DoubleStream;


/**
 * @author Sean Phillips
 */
public class Manifold3D extends Group {
    public Group extrasGroup = new Group();
    public Group labelGroup = new Group();
    private Manifold manifold = null;
    public QuickHull3D hull;

    public TexturedManifold selectionTexturedManifold;
    public TriangleMesh quickhullTriangleMesh;
    public MeshView quickhullMeshView;
    public TriangleMesh quickhullLinesTriangleMesh;
    public MeshView quickhullLinesMeshView;
    public float artScale = 1.0f;
    public static Function<Point3D, Point3d> point3DToHullPoint = p -> new Point3d(p.x, p.y, p.z);
    public static Function<Point3d, Point3D> hullPointToPoint3D = p -> new Point3D(p.x, p.y, p.z);
    public TexturedManifold texturedManifold;
    public SimpleBooleanProperty selected = new SimpleBooleanProperty(false);
    public ContextMenu cm; //allows adding actions by local subscenes
    //allows 2D labels to track their 3D counterparts
    HashMap<Shape3D, Label> shape3DToLabel = new HashMap<>();
    AnimationTimer tessellationTimer;
    private List<Point3D> originalPoint3Ds = null;
    public static File latestDir = new File(".");

    public Manifold3D(List<Point3D> point3DList, boolean triangulate, boolean makeLines, boolean makePoints, Double tolerance) {
        originalPoint3Ds = point3DList;
        buildHullMesh(point3DList, triangulate, makeLines, makePoints, tolerance);

        List<Point3D> fxyzPoints = new ArrayList<>();
        for (int i = 0; i < hull.getNumVertices(); i++) {
            Point3d p3d = hull.getVertices()[i];
            fxyzPoints.add(hullPointToPoint3D.apply(p3d));
        }
        List<Face3> faces = new ArrayList<>();
        for (int[] face : hull.getFaces()) {
            faces.add(new Face3(face[0], face[1], face[2]));
        }
        texturedManifold = new TexturedManifold(fxyzPoints, faces);

        quickhullMeshView = new MeshView(quickhullTriangleMesh);
        PhongMaterial quickhullMaterial = new PhongMaterial(Color.SKYBLUE);
        quickhullMeshView.setMaterial(quickhullMaterial);
        quickhullMeshView.setDrawMode(DrawMode.FILL);
        quickhullMeshView.setCullFace(CullFace.NONE);
        getChildren().addAll(quickhullMeshView);

        selectionTexturedManifold = new TexturedManifold(fxyzPoints, faces);
        Palette.FunctionColorPalette trans = new Palette.FunctionColorPalette(
            1, d -> Color.ALICEBLUE.deriveColor(1, 1, 1, 0.75));
        selectionTexturedManifold.setTextureModeFaces(trans);
        getChildren().addAll(selectionTexturedManifold);
        selectionTexturedManifold.setScaleX(1.25);
        selectionTexturedManifold.setScaleY(1.25);
        selectionTexturedManifold.setScaleZ(1.25);
        selectionTexturedManifold.setDrawMode(DrawMode.LINE);
        selectionTexturedManifold.setMouseTransparent(true);
        selectionTexturedManifold.visibleProperty().bind(selected);

        if (makeLines)
            makeLines();

        if (makePoints)
            makeDebugPoints(hull, artScale, false);

        cm = new ContextMenu();


        MenuItem editPointsItem = new MenuItem("Edit Shape");
        editPointsItem.setOnAction(e -> {
            getScene().getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_SHAPE3D_CONTROLS, this));
        });

        MenuItem makeConcaveItem = new MenuItem("Make Concave");
        makeConcaveItem.setOnAction(e -> {
//            List<javafx.geometry.Point3D> points =
//                getOriginalPoint3DList().stream().map(fxyzPoint3DTofxPoint3D).toList();
//            // ****
//            int n = 5; // number of neighbors
//            int i = 6; // if you want to find the nearest neighbors of the ith point.
//            Octree octree = new Octree();
//            octree.buildIndex(points);
//            int[] neighborIndices = octree.searchNearestNeighbors(n, i);
//            System.out.println("Octree stuff done.");
            ConcaveUtils.makeConcave(this);
        });

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

        MenuItem exportItem = new MenuItem("Export Manifold3D");
        exportItem.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose ManifoldData file output...");
            fc.setInitialFileName("ManifoldData.json");
            if (!latestDir.isDirectory())
                latestDir = new File(".");
            fc.setInitialDirectory(latestDir);
            File file = fc.showSaveDialog(getScene().getWindow());
            if (null != file) {
                if (file.getParentFile().isDirectory())
                    latestDir = file;
                getScene().getRoot().fireEvent(new ManifoldEvent(
                    ManifoldEvent.EXPORT_MANIFOLD_DATA, file, this));
            }
        });
        MenuItem printBoundsCentroid = new MenuItem("Print Bounds Centroid");
        printBoundsCentroid.setOnAction(eh -> {
            Point3D p = getBoundsCentroid();
            System.out.println(p);
        });
        cm.getItems().addAll(editPointsItem, makeConcaveItem, exportItem, diffuseColorItem,
            specColorItem, tessallateItem, printBoundsCentroid);
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

    public void toggleSelection(boolean selected) {
        if (selected) {
            this.selected.set(selected);
            Timeline t = new Timeline(
                new KeyFrame(Duration.millis(0),
                    new KeyValue(selectionTexturedManifold.scaleXProperty(), 0.1, Interpolator.EASE_BOTH),
                    new KeyValue(selectionTexturedManifold.scaleYProperty(), 0.1, Interpolator.EASE_BOTH),
                    new KeyValue(selectionTexturedManifold.scaleZProperty(), 0.1, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(350),
                    new KeyValue(selectionTexturedManifold.scaleXProperty(), 1.5, Interpolator.EASE_BOTH),
                    new KeyValue(selectionTexturedManifold.scaleYProperty(), 1.5, Interpolator.EASE_BOTH),
                    new KeyValue(selectionTexturedManifold.scaleZProperty(), 1.5, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(500),
                    new KeyValue(selectionTexturedManifold.scaleXProperty(), 1.25, Interpolator.EASE_BOTH),
                    new KeyValue(selectionTexturedManifold.scaleYProperty(), 1.25, Interpolator.EASE_BOTH),
                    new KeyValue(selectionTexturedManifold.scaleZProperty(), 1.25, Interpolator.EASE_BOTH))
            );
            t.playFromStart();
        } else {
            Timeline t = new Timeline(
                new KeyFrame(Duration.millis(0),
                    new KeyValue(selectionTexturedManifold.scaleXProperty(), 1.25, Interpolator.EASE_BOTH),
                    new KeyValue(selectionTexturedManifold.scaleYProperty(), 1.25, Interpolator.EASE_BOTH),
                    new KeyValue(selectionTexturedManifold.scaleZProperty(), 1.25, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(250),
                    new KeyValue(selectionTexturedManifold.scaleXProperty(), 1.5, Interpolator.EASE_BOTH),
                    new KeyValue(selectionTexturedManifold.scaleYProperty(), 1.5, Interpolator.EASE_BOTH),
                    new KeyValue(selectionTexturedManifold.scaleZProperty(), 1.5, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(500),
                    new KeyValue(selectionTexturedManifold.scaleXProperty(), 0.1, Interpolator.EASE_BOTH),
                    new KeyValue(selectionTexturedManifold.scaleYProperty(), 0.1, Interpolator.EASE_BOTH),
                    new KeyValue(selectionTexturedManifold.scaleZProperty(), 0.1, Interpolator.EASE_BOTH))
            );
            t.setOnFinished(e -> this.selected.set(selected));
            t.playFromStart();
        }
    }

    public double getBoundsWidth() {
        return quickhullLinesMeshView.getBoundsInLocal().getWidth();
    }

    public double getBoundsHeight() {
        return quickhullLinesMeshView.getBoundsInLocal().getHeight();
    }

    public double getBoundsDepth() {
        return quickhullLinesMeshView.getBoundsInLocal().getDepth();
    }

    /**
     * The mathematical middle (the mean of the extrema) in each dimension
     *
     * @return The center of the Bounding Box
     */
    public Point3D getBoundsCentroid() {
        return new Point3D(quickhullMeshView.getBoundsInLocal().getCenterX(),
            quickhullMeshView.getBoundsInLocal().getCenterY(),
            quickhullMeshView.getBoundsInLocal().getCenterZ());
    }

    public boolean insideBounds(javafx.geometry.Point3D point3D) {
        return texturedManifold.getBoundsInLocal().contains(point3D);
    }

    //https://stackoverflow.com/questions/77936/whats-the-best-way-to-calculate-a-3d-or-n-d-centroid
    public Point3D getAverageConvexCentroid() {
        double aveX = Arrays.stream(hull.getVertices())
            .flatMapToDouble(p -> DoubleStream.of(p.x))
            .average().getAsDouble();
        double aveY = Arrays.stream(hull.getVertices())
            .flatMapToDouble(p -> DoubleStream.of(p.y))
            .average().getAsDouble();
        double aveZ = Arrays.stream(hull.getVertices())
            .flatMapToDouble(p -> DoubleStream.of(p.z))
            .average().getAsDouble();
        return new Point3D(aveX, aveY, aveZ);
    }

    /**
     * https://stackoverflow.com/questions/61702154/calculate-median-using-java-stream-api
     *
     * @return the Point3D median center of the hull
     */
    public Point3D getMedianConvexCentroid() {
        int size = hull.getVertices().length;
        DoubleStream sorted = Arrays.stream(hull.getVertices())
            .flatMapToDouble(p -> DoubleStream.of(p.x)).sorted();
        double medianX = size % 2 == 0 ?
            sorted.skip((size / 2) - 1)
                .limit(2).average().getAsDouble() :
            sorted.skip(size / 2)
                .findFirst().getAsDouble();

        sorted = Arrays.stream(hull.getVertices())
            .flatMapToDouble(p -> DoubleStream.of(p.y)).sorted();
        double medianY = size % 2 == 0 ?
            sorted.skip((size / 2) - 1)
                .limit(2).average().getAsDouble() :
            sorted.skip(size / 2)
                .findFirst().getAsDouble();

        sorted = Arrays.stream(hull.getVertices())
            .flatMapToDouble(p -> DoubleStream.of(p.z)).sorted();
        double medianZ = size % 2 == 0 ?
            sorted.skip((size / 2) - 1)
                .limit(2).average().getAsDouble() :
            sorted.skip(size / 2)
                .findFirst().getAsDouble();
        return new Point3D(medianX, medianY, medianZ);
    }

    public List<Face3> getIntersections(Point3D startingPoint) {
        TriangleMeshHelper helper = new TriangleMeshHelper();
        Point3D insideMeshPoint = getBoundsCentroid();

        List<Face3> listIntersections = helper.getListIntersections(startingPoint, insideMeshPoint, texturedManifold.getVertices(), texturedManifold.getFaces());
        return listIntersections;
    }

//    public javafx.geometry.Point3D getClosestFacePoint(javafx.geometry.Point3D startingPoint) {
//        javafx.geometry.Point3D shortestPoint = null;
//        TriangleMeshHelper helper = new TriangleMeshHelper();
//
//        List<Face3> listIntersections = helper.getListIntersections(
//            startingPoint, direction,
//                texturedManifold.vertices, texturedManifold.faces);
//
//
//        return shortestPoint;
//    }

    public javafx.geometry.Point3D getClosestHullPoint(javafx.geometry.Point3D startingPoint) {
        javafx.geometry.Point3D shortestPoint = null;
        double distance = 0;
        for (Point3D point : originalPoint3Ds) {
            double currentDistance = startingPoint.distance(point.x, point.y, point.z);
            if (null == shortestPoint || currentDistance < distance) {
                distance = currentDistance;
                shortestPoint = new javafx.geometry.Point3D(point.x, point.y, point.z);
            }
        }
        return shortestPoint;
    }

    public void refreshMesh(List<Point3D> point3DList, boolean triangulate, boolean makeLines, boolean makePoints, Double tolerance) {
        quickhullLinesTriangleMesh.getPoints().clear();
        quickhullLinesTriangleMesh.getTexCoords().clear();
        quickhullLinesTriangleMesh.getFaces().clear();
        buildHullMesh(point3DList, triangulate, makeLines, makePoints, tolerance);
        quickhullMeshView.setMesh(quickhullTriangleMesh);
        if (makeLines) {
            quickhullLinesTriangleMesh.getPoints().addAll(quickhullTriangleMesh.getPoints());
            quickhullLinesTriangleMesh.getTexCoords().addAll(quickhullTriangleMesh.getTexCoords());
            quickhullLinesTriangleMesh.getFaces().addAll(quickhullTriangleMesh.getFaces());
            quickhullLinesMeshView.setMesh(quickhullLinesTriangleMesh);
        }
//        if (makePoints)
//            makeDebugPoints(hull, artScale, false);
    }

    private void buildHullMesh(List<Point3D> point3DList, boolean triangulate, boolean makeLines, boolean makePoints, Double tolerance) {
        hull = new QuickHull3D();
        if (null != tolerance)
            hull.setExplicitDistanceTolerance(tolerance);
        //Construct an array of Point3D's
        Point3d[] points = point3DList.stream()
            .map(point3DToHullPoint)
            .toArray(Point3d[]::new);
        hull.build(points);
        if (triangulate) {
//            System.out.println("Triangulating...");
            hull.triangulate();
        }
//        System.out.println("Faces: " + hull.getNumFaces());
//        System.out.println("Verts: " + hull.getNumVertices());
//        System.out.println("Making Quickhull mesh...");
        quickhullTriangleMesh = makeQuickhullMesh(hull, artScale);
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

    /**
     * @return the manifold
     */
    public Manifold getManifold() {
        return manifold;
    }

    /**
     * @param manifold the manifold to set
     */
    public void setManifold(Manifold manifold) {
        this.manifold = manifold;
    }

    /**
     * @return the point3DList
     */
    public List<Point3D> getOriginalPoint3DList() {
        return originalPoint3Ds;
    }
}
