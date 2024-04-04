package edu.jhuapl.trinity.utils;

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

import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Perspective3DNode;
import edu.jhuapl.trinity.javafx.javafx3d.Trajectory3D;
import edu.jhuapl.trinity.javafx.javafx3d.animated.TessellationMesh;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.AmbientLight;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.primitives.helper.MeshHelper;
import org.fxyz3d.utils.CameraTransformer;
import org.fxyz3d.utils.MeshUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities used by various 3D rendering code.
 *
 * @author Sean Phillips
 */
public enum JavaFX3DUtils {
    INSTANCE;
    public static double EPSILON = 0.000000001;
    static List<Image> tiles = null;
    public static Function<Point3D, javafx.geometry.Point3D> toFX =
        p -> new javafx.geometry.Point3D(p.x, p.y, p.z);
    public static Function<javafx.geometry.Point3D, Point3D> toFXYZ3D =
        p -> new Point3D(p.getX(), p.getY(), p.getZ());
    public static Function<Perspective3DNode, Point3D> pNodetoFXYZ3D =
        p -> {
            Point3D p3D = new Point3D(p.xCoord, p.yCoord, p.zCoord);
            p3D.f = Double.valueOf(p.nodeColor.getHue()).floatValue();
            return p3D;
        };
    
    public static Comparator<Point3D> Point3DXComparator = (Point3D p1, Point3D p2) -> {
        if (p1.x < p2.x) return -1;
        else if (p1.x > p2.x) return 1;
        else return 0;
    };
    public static Comparator<Point3D> Point3DYComparator = (Point3D p1, Point3D p2) -> {
        if (p1.y < p2.y) return -1;
        else if (p1.y > p2.y) return 1;
        else return 0;
    };
    public static Comparator<Point3D> Point3DZComparator = (Point3D p1, Point3D p2) -> {
        if (p1.z < p2.z) return -1;
        else if (p1.z > p2.z) return 1;
        else return 0;
    };

    public static boolean matches(Point3D p1, Point3D p2, double tolerance) {
        return (p1.getX() - p2.getX() < tolerance)
            && (p1.getY() - p2.getY() < tolerance)
            && (p1.getZ() - p2.getZ() < tolerance);
    }

    public static boolean matches(javafx.geometry.Point3D p1, javafx.geometry.Point3D p2, double tolerance) {
        return (p1.getX() - p2.getX() < tolerance)
            && (p1.getY() - p2.getY() < tolerance)
            && (p1.getZ() - p2.getZ() < tolerance);
    }

    public static boolean matches(Point3D p1, Point3D p2) {
        return matches(p1, p2, EPSILON);
    }

    public static boolean matches(javafx.geometry.Point3D p1, javafx.geometry.Point3D p2) {
        return matches(p1, p2, EPSILON);
    }

    public static Function<Sphere, javafx.geometry.Point3D> mapShape3DToPoint3D = (s) -> {
        return new javafx.geometry.Point3D(s.getTranslateX(),
            s.getTranslateY(), s.getTranslateZ());
    };

    /**
     * Assumes indices 0,1 and 2 map to X, Y and Z
     */
    public static Function<FeatureVector, javafx.geometry.Point3D> mapFeatureToPoint3D = (fv) -> {
        return new javafx.geometry.Point3D(fv.getData().get(0),
            fv.getData().get(1), fv.getData().get(2));
    };

    public static List<Integer> pickIndicesByBox(PerspectiveCamera camera,
                                                 List<? extends Shape3D> shapes, Point2D upperLeft, Point2D lowerRight) {

        List<Integer> indices = new ArrayList<>();
        //reuse this point reference
        Shape3D shape3D;
        javafx.geometry.Point3D coordinates;
        boolean c;
        BoundingBox screenBox = new BoundingBox(
            upperLeft.getX(), upperLeft.getY(),
            lowerRight.getX() - upperLeft.getX(), lowerRight.getY() - upperLeft.getY()
        );
        int totalContains = 0;
        for (int i = 0; i < shapes.size(); i++) {
            shape3D = shapes.get(i);
            coordinates = shape3D.localToScene(javafx.geometry.Point3D.ZERO, true);
            c = screenBox.contains(coordinates);
            if (c) {
                totalContains++;
                indices.add(i);
            }
        }
        System.out.println("screenBox contains " + totalContains + " shapes.");
        return indices;
    }

    public static Image snapshotShape3D(Node node) {
        Group group = new Group(node);
        Scene scene = new Scene(group, 1000, 1000, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.TRANSPARENT);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        scene.setCamera(camera);
        camera.setTranslateZ(500);
        PointLight light = new PointLight(Color.WHITE);
        group.getChildren().add(light);
        light.getScope().add(node);
        light.setTranslateY(-500);  //interragation lamp

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = scene.getRoot().snapshot(params, null);
        return image;
    }

    public static Timeline creditsReel(Group nodeGroup, Point3D centroid) {
        List<Image> images = new ArrayList<>();
        File folder = new File("credits/");
        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length < 1) {
            return null;
        }
        File[] files = folder.listFiles();

        for (File file : files) {
            Image image = new Image(file.getAbsolutePath());
            images.add(image);
        }
        Timeline timeline = new Timeline();
        double timeIndex = 0;
        nodeGroup.getChildren().removeIf(n -> n instanceof TessellationMesh);
        AmbientLight light = new AmbientLight(Color.WHITE);

        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            TessellationMesh tm = new TessellationMesh(image,
                Color.GREEN, 1, 25, 2, true);
            tm.setVisible(false);
            nodeGroup.getChildren().add(tm);
            tm.setRotationAxis(Rotate.Y_AXIS);
            tm.setRotate(180);
            tm.setRotationAxis(Rotate.X_AXIS);
            tm.setRotate(-90);
            tm.setTranslateX(centroid.getX() - image.getWidth() / 4.0);
            tm.setTranslateY(centroid.getY() - image.getHeight() / 4.0);
            tm.setTranslateZ(centroid.getZ());
            light.getScope().add(tm);
            String name = files[i].getName().split("\\.")[0];
            timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.seconds(timeIndex + 0.0), kv -> {
                    tm.setScaleX(1);
                    tm.setScaleY(1);
                    tm.setScaleZ(1);
                })
                , new KeyFrame(Duration.seconds(timeIndex + 0.1), kv -> tm.setVisible(true))
                , new KeyFrame(Duration.seconds(timeIndex + 1), kv -> tm.animateTessellation(15, 2))
                , new KeyFrame(Duration.seconds(timeIndex + 2), kv ->
                    nodeGroup.getScene().getRoot().fireEvent(
                        new CommandTerminalEvent(name,
                            new Font("Consolas", 50), Color.GREEN)))
                , new KeyFrame(Duration.seconds(timeIndex + 6),
                    kv -> {
                        tm.colorByImage = false;
                        tm.updateMaterial(image);
                        tm.enableMatrix(true);
                    })
                , new KeyFrame(Duration.seconds(timeIndex + 14), new KeyValue(
                    tm.scaleXProperty(), 1, Interpolator.EASE_BOTH))
                , new KeyFrame(Duration.seconds(timeIndex + 14), new KeyValue(
                    tm.scaleYProperty(), 1, Interpolator.EASE_BOTH))
                , new KeyFrame(Duration.seconds(timeIndex + 14), new KeyValue(
                    tm.scaleZProperty(), 1, Interpolator.EASE_BOTH))
                , new KeyFrame(Duration.seconds(timeIndex + 15), new KeyValue(
                    tm.scaleXProperty(), 0.1, Interpolator.EASE_BOTH))
                , new KeyFrame(Duration.seconds(timeIndex + 15), new KeyValue(
                    tm.scaleYProperty(), 0.1, Interpolator.EASE_BOTH))
                , new KeyFrame(Duration.seconds(timeIndex + 15), new KeyValue(
                    tm.scaleZProperty(), 0.1, Interpolator.EASE_BOTH))
                , new KeyFrame(Duration.seconds(timeIndex + 15), kv -> {
                    tm.setVisible(false);
                    nodeGroup.getChildren().remove(tm);
                    tm.enableMatrix(false);
                })
            );
            timeIndex += 15;
        }
        timeline.setCycleCount(1);
        return timeline;
    }

    public static Point2D getTransformedP2D(Shape3D node, SubScene subScene, double clipDistance) {
        javafx.geometry.Point3D coordinates = node.localToScene(javafx.geometry.Point3D.ZERO, true);
        //@DEBUG SMP  useful debugging print
        //System.out.println("subSceneToScene Coordinates: " + coordinates.toString());
        //Clipping Logic
        //if coordinates are outside of the scene it could
        //stretch the screen so don't transform them
        double x = coordinates.getX();
        double y = coordinates.getY();

        //is it left of the view?
        if (x < 0) {
            x = 0;
        }
        //is it right of the view?
        if ((x + clipDistance) > subScene.getWidth()) {
            x = subScene.getWidth() - (clipDistance);
        }
        //is it above the view?
        if (y < 0) {
            y = 0;
        }
        //is it below the view
        if ((y + clipDistance) > subScene.getHeight())
            y = subScene.getHeight() - (clipDistance);
        return new Point2D(x, y);
    }

    public static List<Image> getTiles() throws URISyntaxException, IOException {
        if (tiles == null || tiles.isEmpty()) {
            tiles = new ArrayList<>();
            tiles.add(ResourceUtils.load3DTextureImage("1500_blackgrid"));
            File folder = new File("textures/tiles/");
            if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length < 1) {
                return tiles;
            }
            File[] files = folder.listFiles();

            for (File file : files) {
                tiles.add(new Image(file.getAbsolutePath()));
            }
        }
        return tiles;
    }

    public static List<Image> getTops() throws URISyntaxException, IOException {
        List<Image> images = new ArrayList<>();
        images.add(ResourceUtils.load3DTextureImage("neonrectanglepurple"));
        File folder = new File("textures/tops/");
        if (!folder.exists() || !folder.isDirectory() || folder.listFiles().length < 1) {
            return images;
        }
        File[] files = folder.listFiles();
        for (File file : files) {
            images.add(new Image(file.getAbsolutePath()));
        }
        return images;
    }

    /*
        https://stackoverflow.com/questions/28731460/javafx-moving-3d-objects-with-mouse-on-a-virtual-plane
     From fx83dfeatures.Camera3D
     http://hg.openjdk.java.net/openjfx/8u-dev/rt/file/5d371a34ddf1/apps/toys/FX8-3DFeatures/src/fx83dfeatures/Camera3D.java
    */
    public static javafx.geometry.Point3D unProjectDirection(double sceneX, double sceneY,
                                                             double sWidth, double sHeight, PerspectiveCamera camera) {
        double tanHFov = Math.tan(Math.toRadians(camera.getFieldOfView()) * 0.5f);
        javafx.geometry.Point3D vMouse = new javafx.geometry.Point3D(tanHFov * (2 * sceneX / sWidth - 1), tanHFov * (2 * sceneY / sWidth - sHeight / sWidth), 1);

        javafx.geometry.Point3D result = localToSceneDirection(vMouse, camera);
        return result.normalize();
    }

    public static javafx.geometry.Point3D localToScene(javafx.geometry.Point3D pt, PerspectiveCamera camera) {
        javafx.geometry.Point3D res = camera.localToParentTransformProperty().get().transform(pt);
        if (camera.getParent() != null) {
            res = camera.getParent().localToSceneTransformProperty().get().transform(res);
        }
        return res;
    }

    public static javafx.geometry.Point3D localToSceneDirection(javafx.geometry.Point3D dir, PerspectiveCamera camera) {
        javafx.geometry.Point3D res = localToScene(dir, camera);
        return res.subtract(localToScene(new javafx.geometry.Point3D(0, 0, 0), camera));
    }

    public static Affine lookAt(Node node, javafx.geometry.Point3D from, javafx.geometry.Point3D to, boolean applyTranslate) {
        //zVec is "forward"
        javafx.geometry.Point3D zVec = to.subtract(from).normalize();
        //ydir is "up"
        javafx.geometry.Point3D ydir = Rotate.Y_AXIS;
        javafx.geometry.Point3D tangent0 = zVec.crossProduct(ydir);
        //handle edge case where to location is precisely the "up" direction
        if (tangent0.magnitude() < 0.001) {
            //pick a different axis to use
            ydir = Rotate.X_AXIS;
            tangent0 = zVec.crossProduct(ydir);
        }
        tangent0.normalize();
        ydir = zVec.crossProduct(tangent0);

        javafx.geometry.Point3D xVec = ydir.normalize().crossProduct(zVec).normalize();
        javafx.geometry.Point3D yVec = zVec.crossProduct(xVec).normalize();

        Affine affine = new Affine(
            xVec.getX(), yVec.getX(), zVec.getX(), 0,
            xVec.getY(), yVec.getY(), zVec.getY(), 0,
            xVec.getZ(), yVec.getZ(), zVec.getZ(), 0);
        if (applyTranslate) {
            affine.setTx(from.getX());
            affine.setTy(from.getY());
            affine.setTz(from.getZ());
        }
        node.getTransforms().setAll(affine);
        return affine;
    }

    public static void zoomTransition(double milliseconds, Camera camera, double distance) {
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().addAll(new KeyFrame[]{
            new KeyFrame(Duration.millis(milliseconds), new KeyValue[]{// Frame End
                new KeyValue(camera.translateZProperty(), distance, Interpolator.EASE_OUT),
            })
        });
        timeline.playFromStart();

    }

    public static Timeline transitionCameraTo(double milliseconds, Camera camera, CameraTransformer transformer,
                                              double tx, double ty, double tz, double rx, double ry, double rz) {
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().addAll(new KeyFrame[]{
            new KeyFrame(Duration.millis(milliseconds), new KeyValue[]{// Frame End
                new KeyValue(transformer.rx.angleProperty(), rx, Interpolator.EASE_BOTH),
                new KeyValue(transformer.ry.angleProperty(), ry, Interpolator.EASE_BOTH),
                new KeyValue(transformer.rz.angleProperty(), rz, Interpolator.EASE_BOTH),
                new KeyValue(camera.translateXProperty(), tx, Interpolator.EASE_BOTH),
                new KeyValue(camera.translateYProperty(), ty, Interpolator.EASE_BOTH),
                new KeyValue(camera.translateZProperty(), tz, Interpolator.EASE_BOTH)
            })
        });
        timeline.playFromStart();
        return timeline;
    }

    public static WritableImage convertToGreyScale(Image image) {
        WritableImage tmp = new WritableImage(image.getPixelReader(), (int) image.getWidth(), (int) image.getHeight());
        for (int y = 0; y < (int) tmp.getHeight(); y++) {
            for (int x = 0; x < (int) tmp.getWidth(); x++) {
                tmp.getPixelWriter().setColor(x, y, image.getPixelReader().getColor(x, y).grayscale());
//                updateMessage("Converting ... ");
            }
        }
        return tmp;
    }

    /**
     * Generically creates a TriangleMesh that represents the surface height map
     * from an Image object.      *
     *
     * @param image The Image object to convert to TriangleMesh
     * @param pskip will skip this many pixels at each sampling step.
     * @param maxH  Height Scale in formula:
     *              points[index + 2] = -((float) ((r + g + b) / 3) / 255) * maxH;
     * @param scale Provides spread scaling for formulas:
     *              points[index] = (float) fx * scale;
     *              points[index + 1] = (float) fy * scale;  // y
     * @return TriangleMesh 3D surface height object generated from the Image.
     */
    public static TriangleMesh createHeightMap(Image image, int pskip, float maxH, float scale) {
        float minX = -(float) image.getWidth() / 2;
        float maxX = (float) image.getWidth() / 2;
        float minZ = -(float) image.getHeight() / 2;
        float maxZ = (float) image.getHeight() / 2;

        if (pskip <= 0)
            pskip = 1;
        int subDivX = (int) image.getWidth() / pskip;
        int subDivZ = (int) image.getHeight() / pskip;

        final int pointSize = 3;
        final int texCoordSize = 2;
        // 3 point indices and 3 texCoord indices per triangle
        final int faceSize = 6;
        int numDivX = subDivX + 1;
        int numVerts = (subDivZ + 1) * numDivX;
        float points[] = new float[numVerts * pointSize];
        float texCoords[] = new float[numVerts * texCoordSize];
        int faceCount = subDivX * subDivZ * 2;
        int faces[] = new int[faceCount * faceSize];
        float currZ, currX;
        double fz, fx;
        int index, rgb, r, g, b;
        // Create points and texCoords
        for (int z = 0; z < subDivZ; z++) {
            currZ = (float) z / subDivZ;
            fz = (1 - currZ) * minZ + currZ * maxZ;
            for (int x = 0; x < subDivX; x++) {
                currX = (float) x / subDivX;
                fx = (1 - currX) * minX + currX * maxX;
                index = z * numDivX * pointSize + (x * pointSize);
                points[index] = (float) fx * scale;   // x

                // color value for pixel at point
                rgb = ((int) image.getPixelReader().getArgb(x * pskip, z * pskip));
                r = (rgb >> 16) & 0xFF;
                g = (rgb >> 8) & 0xFF;
                b = rgb & 0xFF;

                points[index + 1] = -(((r + g + b) / 3.0f) / 255.0f) * maxH; // y
                points[index + 2] = (float) fz * scale;  // z

                index = z * numDivX * texCoordSize + (x * texCoordSize);
                texCoords[index] = currX;
                texCoords[index + 1] = currZ;
            }
        }
        int p00, p01, p10, p11, tc00, tc01, tc10, tc11;
        // Create faces
        for (int z = 0; z < subDivZ; z++) {
            for (int x = 0; x < subDivX; x++) {
                p00 = z * numDivX + x;
                p01 = p00 + 1;
                p10 = p00 + numDivX;
                p11 = p10 + 1;
                tc00 = z * numDivX + x;
                tc01 = tc00 + 1;
                tc10 = tc00 + numDivX;
                tc11 = tc10 + 1;

                index = (z * subDivX * faceSize + (x * faceSize)) * 2;
                faces[index + 0] = p00;
                faces[index + 1] = tc00;
                faces[index + 2] = p10;
                faces[index + 3] = tc10;
                faces[index + 4] = p11;
                faces[index + 5] = tc11;

                index += faceSize;
                faces[index + 0] = p11;
                faces[index + 1] = tc11;
                faces[index + 2] = p01;
                faces[index + 3] = tc01;
                faces[index + 4] = p00;
                faces[index + 5] = tc00;
            }
        }
        //to do
        //int smoothingGroups[] = new int[faces.length / faceSize];

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(points);
        mesh.getTexCoords().addAll(texCoords);
        mesh.getFaces().addAll(faces);
        //mesh.getFaceSmoothingGroups().addAll(smoothingGroups);
        return mesh;
    }

    public static List<Point3D> convertToPoint3D(Trajectory trajectory, double scale, double sceneWidth, double sceneScale) {
        //Convert the Intermediate states to 3D coordinate space
        List<Point3D> points = new ArrayList<>();

        ArrayList<double[]> stateList = trajectory.states;
        for (int i = 0; i < stateList.size(); i++) {
            double[] value = stateList.get(i).clone();
            //X ==> X Positive
            float xCoord = (float) (value[0] * scale);
            //Y ==> Y Positive
            float yCoord = (float) (value[1] * scale);
            //Z ==> Z Positive
            float zCoord = (float) (value[2] * scale);

            points.add(new Point3D(xCoord, yCoord, zCoord));
        }
        return points;
    }

    /**
     * Convert a Trajectory object to a 3D TriangleMesh in the form of a PolyLine3D.The integrator has the following orientation:
     * X axis positive to the right of the screen
     * Y axis positive towards the screen
     * Z axis positive to the top of the screen
     * <p>
     * Trajectory coordinate system is converted to match JavaFX 3D coordinate
     * system as follows:
     * X ==> X Positive
     * Y ==> Z Positive
     * Z ==> Y Positive
     *
     * @param trajectory The Trajectory to convert to a PolyLine3D
     * @param color      The Color to apply to the PhongMaterial
     * @param sceneWidth Width of the 3D scene in FX coordinates (used for positioning the 3D coordinates)
     * @param sceneScale Scale of the 3D Scene in arbitrary scalar. (1.0 would keep everything the same.
     * @param scale      Scalar multiplier applied to all coordinates
     * @return PolyLine3D 3D object whose coordinates have been mapped to the current scene
     */
    public static Trajectory3D buildPolyLineFromTrajectory(int trial, int day,
                                                           Trajectory trajectory, Color color,
                                                           double scale, double sceneWidth, double sceneScale) {
        if (trajectory.states.isEmpty()) {
            return null;
        }
        List<Point3D> points = convertToPoint3D(trajectory, scale, sceneWidth, sceneScale);
//        Double width = Options.getGlobalDoubleByProp(Options.polyLine3dSizeProp);
        float width = 8.0f;
        return new Trajectory3D(trial, day, trajectory, points, width, color);
    }

    public static Trajectory3D buildPolyLineFromTrajectory(
        Trajectory trajectory, float trajWidth, Color color, int tailSize,
        double scale, double sceneWidth, double sceneScale) {
        if (trajectory.states.isEmpty()) {
            return null;
        }
        List<Point3D> points = convertToPoint3D(trajectory, scale, sceneWidth, sceneScale);
        //Trim older points based on tail size
        if (points.size() > tailSize)
            return new Trajectory3D(1, 1, trajectory,
                points.subList(points.size() - tailSize, points.size()), trajWidth, color);
        else
            return new Trajectory3D(1, 1, trajectory, points, trajWidth, color);
    }

    /**
     * Checks whether the file can be used as a texture.
     *
     * @param file The File object to check.
     * @return boolean true if it is a file, can be read and is an image type file
     */
    public static boolean isTextureFile(File file) {
        if (file.isFile() && file.canRead()) {
            try {
                String contentType = Files.probeContentType(file.toPath());
                switch (contentType) {
                    case "image/jpeg":
                    case "image/png":
                        return true;
                }
                //System.out.println(contentType);
            } catch (IOException ex) {
                Logger.getLogger(JavaFX3DUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    /**
     * Experimental 3D Tessalation algorithm that converts groups of trajectories
     * to a single 3D Mesh object. It is designed to handle manifolds and other
     * mostly contiguous groups of trajectories.  Assumes trajectories are each
     * close together in coordinate space and eventually loop around like a tunnel.
     *
     * @param trajectories List of Trajectory3D objects to tessallate into a single Mesh
     * @param pointSize    Number of points to use in the sampling along each Trajectory
     * @param wrapInside   True tells the algorithm to attempt to tessalate the inside of the tube as well.
     * @return Mesh The continuous Mesh object representing the List of Trajectories
     */
    public static Mesh tessalateToSurface(List<Trajectory3D> trajectories, int pointSize, boolean wrapInside) {
        //For each trajectory, evenly sample along the trajectory based on point size
        //Each subsequent sampled list must have the same number of points
        ArrayList<Trajectory3D> sampledTrajs = new ArrayList<>();
        trajectories.stream().forEachOrdered(traj -> {
            ArrayList<Point3D> sampledPoints = new ArrayList<>();
            int pointTotal = traj.getPolyLinePoints().size();
            Integer interpSize = pointTotal / pointSize;
            int currentIndex = 0;
            for (int i = 0; i < pointSize; i++) {
                sampledPoints.add(traj.getPolyLinePoints().get(currentIndex));
                currentIndex += interpSize;
            }
            sampledPoints.add(traj.getPolyLinePoints().get(pointTotal - 1));
            sampledTrajs.add(new Trajectory3D(0, 0, traj.trajectory, sampledPoints, 4, Color.CORAL));
            System.out.println("Sample Size: " + interpSize + " with " + sampledPoints.size() + " total sampled points");
        });
        //Create a new mesh to hold the tessalation
        TriangleMesh surfaceMesh = new TriangleMesh();
        //Map all the trajectory points to a single contiguous stream and then add these ordered points to the new mesh
        sampledTrajs.stream().flatMap(t ->
            t.getPolyLinePoints().stream()).forEachOrdered(point3D ->
            surfaceMesh.getPoints().addAll(point3D.x, point3D.y, point3D.z));

        //add dummy Texture Coordinate
        surfaceMesh.getTexCoords().addAll(0, 0);
        //move from point to point on the sampled trajectories, tessalating with the next trajectory
        int totalPoints = (sampledTrajs.size() - 1) * pointSize - 1 * 3;
        //Loop to handle the outside of the surface
        for (int i = 0; i < totalPoints; i++) {
            //Vertices wound counter-clockwise which is the default front face of any Triangle
            //These triangles live on the outside of the surface facing the camera
            surfaceMesh.getFaces().addAll(i, 0, i + 1, 0, i + pointSize, 0);
            surfaceMesh.getFaces().addAll(i + 1, 0, i + pointSize + 1, 0, i + pointSize, 0);
        }
        if (wrapInside) {
            //Loop to handle the inside of the surface
            //Just reverse the curse
            for (int i = totalPoints; i > pointSize; i--) {
                //Vertices wound clockwise which is flipped from the front face
                //These triangles live on the inside of the surface facing the camera
                surfaceMesh.getFaces().addAll(i, 0, i - 1, 0, i - pointSize, 0);
                surfaceMesh.getFaces().addAll(i - 1, 0, i - pointSize - 1, 0, i - pointSize, 0);
            }
        }
        return surfaceMesh;
    }

    /**
     * Helper method which takes an arbitrary number of 3D Node objects and
     * combines them into a single Mesh object. Uses MeshHelper under the hood.
     *
     * @param nodes List of all the Node objects. The expectation is that the Node
     *              objects will be 3D Nodes (some Mesh).
     * @return Mesh The continuous Mesh object representing the List of Nodes
     */
    public static Mesh mergeToMesh(ArrayList<Node> nodes) {
        TriangleMesh baseMesh = getMesh(nodes.get(0));
        if (null != baseMesh) {
            MeshHelper baseMeshHelper = new MeshHelper(baseMesh);
            nodes.stream().skip(0).forEach(node -> {
                TriangleMesh nodeTM = getMesh(node);
                if (null != nodeTM) {
                    MeshHelper nodeMeshHelper = new MeshHelper(nodeTM);
                    baseMeshHelper.addMesh(nodeMeshHelper);
                }
            });
            TriangleMesh finalMesh = new TriangleMesh();
            finalMesh.getPoints().setAll(baseMeshHelper.getPoints());
            finalMesh.getTexCoords().setAll(baseMeshHelper.getTexCoords());
            finalMesh.getFaces().setAll(baseMeshHelper.getFaces());
            finalMesh.getFaceSmoothingGroups().setAll(baseMeshHelper.getFaceSmoothingGroups());
            return finalMesh;
        }
        return baseMesh;
    }

    /**
     * Helper method that determines which type of custom 3D object the
     * node is and returns the TriangleMesh representing it.
     * Currently supports:
     * Trajectory3D
     *
     * @param node The 3D Object to obtain a reference to the TriangleMesh
     * @return TriangleMesh Taken from the node
     */
    public static TriangleMesh getMesh(Node node) {
        if (node instanceof Trajectory3D)
            return (TriangleMesh) ((Trajectory3D) node).meshView.getMesh();
        return null;
    }

    /**
     * Convenience method to save an arbitrary 3D Mesh object to a 3D STL model file.
     * Presents the user with a FileChooser dialog to browse the location and
     * file name.
     *
     * @param mesh The 3D Mesh object to save as an STL.
     */
    public static void saveMeshToSTL(Mesh mesh) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Browse to output file location...");
        fc.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("STL 3D Model Files", "stl", "STL"));
        File file = fc.showSaveDialog(null);
        if (null != file) {
            try {
                MeshUtils.mesh2STL(file.getPath(), mesh);
            } catch (IOException ex) {
                Logger.getLogger(JavaFX3DUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
