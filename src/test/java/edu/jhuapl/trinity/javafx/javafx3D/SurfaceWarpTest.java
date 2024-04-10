package edu.jhuapl.trinity.javafx.javafx3D;

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

import com.github.sarxos.webcam.Webcam;
import edu.jhuapl.trinity.data.HyperspaceSeed;
import edu.jhuapl.trinity.javafx.events.ColorMapEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.javafx.events.TerrainEvent;
import edu.jhuapl.trinity.javafx.javafx3d.DirectedScatterDataModel;
import edu.jhuapl.trinity.javafx.javafx3d.DirectedScatterMesh;
import edu.jhuapl.trinity.javafx.javafx3d.HyperSurfacePlotMesh;
import edu.jhuapl.trinity.javafx.javafx3d.Perspective3DNode;
import edu.jhuapl.trinity.javafx.javafx3d.Vert3D;
import edu.jhuapl.trinity.javafx.javafx3d.animated.Opticon;
import edu.jhuapl.trinity.javafx.javafx3d.animated.TessellationTube;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.stage.Stage;
import org.fxyz3d.geometry.MathUtils;
import org.fxyz3d.utils.CameraTransformer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SurfaceWarpTest extends Application {

    PerspectiveCamera camera = new PerspectiveCamera(true);
    public Group sceneRoot = new Group();
    public SubScene subScene;
    public CameraTransformer cameraTransform = new CameraTransformer();
    private double cameraDistance = -2000;
    private final double sceneWidth = 10000;
    private final double sceneHeight = 4000;
    Opticon opticon;
    HyperSurfacePlotMesh surfPlot;
    HyperSurfacePlotMesh cylinderSurfPlot;

    DirectedScatterMesh scatterMesh3D;
    DirectedScatterDataModel scatterModel;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;
    AnimationTimer tessellationTimer;
    public TriangleMesh surfPlotLinesTriangleMesh;
    public MeshView surfPlotLinesMeshView;
    ColorPicker diffuseColorPicker;
    ColorPicker specColorPicker;
    Box boundingBox;
    Webcam webCam = null;
    Image currentImage;
    ArrayList<org.fxyz3d.geometry.Point3D> data;
    ArrayList<org.fxyz3d.geometry.Point3D> endPoints;
    public ConcurrentLinkedQueue<HyperspaceSeed> hyperspaceSeeds = new ConcurrentLinkedQueue<>();
    public ConcurrentLinkedQueue<Perspective3DNode> pNodes = new ConcurrentLinkedQueue<>();
    int TOTAL_COLORS = 1530; //colors used by map function
    Function<org.fxyz3d.geometry.Point3D, Number> colorByLabelFunction = p -> p.f; //Color mapping function
    public double point3dSize = 5.0; //size of 3d tetrahedra
    int currentPskip = 1;

    @Override
    public void start(Stage primaryStage) throws Exception {
//        webCam = Webcam.getDefault();
//        webCam.open();
//        Dimension d = webCam.getViewSizes()[2];
//        d.setSize(1920, 1080);
//        webCam.close();
//        webCam.setViewSize(d);
//        webCam.open();

        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        //Start Tracking mouse movements only when a button is pressed
        subScene.setOnMousePressed((MouseEvent me) -> {
            if (me.isSynthesized()) {
                System.out.println("isSynthesized");
            }
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
        subScene.setOnMouseDragged((MouseEvent me) -> mouseDragCamera(me));
        subScene.setOnScroll((ScrollEvent event) -> {
            double modifier = 50.0;
            double modifierFactor = 0.1;

            if (event.isControlDown()) {
                modifier = 1;
            }
            if (event.isShiftDown()) {
                modifier = 100.0;
            }
            double z = camera.getTranslateZ();
            double newZ = z + event.getDeltaY() * modifierFactor * modifier;
            camera.setTranslateZ(newZ);
        });
        subScene.setOnKeyPressed(event -> {
            //What key did the user press?
            KeyCode keycode = event.getCode();

            double change = 10.0;
            //Add shift modifier to simulate "Running Speed"
            if (event.isShiftDown()) {
                change = 100.0;
            }
            //Zoom controls
            if (keycode == KeyCode.W) {
                camera.setTranslateZ(camera.getTranslateZ() + change);
            }
            if (keycode == KeyCode.S) {
                camera.setTranslateZ(camera.getTranslateZ() - change);
            }
            //Strafe controls
            if (keycode == KeyCode.A) {
                camera.setTranslateX(camera.getTranslateX() - change);
            }
            if (keycode == KeyCode.D) {
                camera.setTranslateX(camera.getTranslateX() + change);
            }
            //jump controls
            if (keycode == KeyCode.SPACE) {
                camera.setTranslateY(camera.getTranslateY() - change);
            }
            if (keycode == KeyCode.C) {
                camera.setTranslateY(camera.getTranslateY() + change);
            }
            //show/Hide scatterplot
            if (keycode == KeyCode.Y) {
                scatterMesh3D.setVisible(!scatterMesh3D.isVisible());
            }
            //point size and scaling
            if (keycode == KeyCode.O || (keycode == KeyCode.P && event.isControlDown())) {
                point3dSize -= 1;
                scatterMesh3D.setHeight(point3dSize);
                updateView(false);
            }
            if (keycode == KeyCode.P) {
                point3dSize += 1;
                scatterMesh3D.setHeight(point3dSize);
                updateView(false);
            }
        });
        StackPane stackPane = new StackPane(subScene);
        subScene.widthProperty().bind(stackPane.widthProperty());
        subScene.heightProperty().bind(stackPane.heightProperty());
        subScene.setFill(Color.BLACK);

        camera = new PerspectiveCamera(true);
        //setup camera transform for rotational support
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
//        cameraTransform.ry.setAngle(-45.0);
//        cameraTransform.rx.setAngle(-10.0);
        subScene.setCamera(camera);
        sceneRoot.getChildren().addAll(cameraTransform);
        ContextMenu cm = new ContextMenu();
        diffuseColorPicker = new ColorPicker(Color.SKYBLUE);
        diffuseColorPicker.valueProperty().addListener(cl -> {
            ((PhongMaterial) surfPlotLinesMeshView.getMaterial()).setDiffuseColor(diffuseColorPicker.getValue());
        });
        MenuItem diffuseColorItem = new MenuItem("Diffuse Color", diffuseColorPicker);
        specColorPicker = new ColorPicker(Color.SKYBLUE);
        specColorPicker.valueProperty().addListener(cl -> {
            ((PhongMaterial) surfPlotLinesMeshView.getMaterial()).setSpecularColor(specColorPicker.getValue());
        });
        MenuItem specColorItem = new MenuItem("Specular Color", specColorPicker);

        MenuItem captureItem = new MenuItem("Capture Image");
        captureItem.setOnAction(e -> {
            Image image = captureImage();
            currentImage = image;
            currentPskip = 1;
            vectorizeImage(currentPskip, image);
        });

        MenuItem warpItem = new MenuItem("Warp Image");
        warpItem.setOnAction(e -> {
            sceneRoot.getChildren().removeIf(n -> n instanceof TessellationTube);
            buildWarp(dataGrid, currentPskip, 5, 5, 5);
            TessellationTube tube = new TessellationTube(dataGrid, Color.WHITE, 400, 5, 2);
            tube.setMouseTransparent(true);
            if (null != currentImage) {
                tube.meshView.setDrawMode(DrawMode.FILL);
//                tube.meshView.setCullFace(CullFace.NONE);

                tube.colorByImage = true;
                tube.updateMaterial(currentImage);
            }
            Platform.runLater(() -> {
                sceneRoot.getChildren().add(tube);

            });
        });

        Spinner<Integer> delaySpinner = new Spinner<>(1, 1000, 10, 10);
        Spinner<Integer> rowsSpinner = new Spinner<>(5, 100, 5, 10);
        HBox hbox = new HBox(10,
            new VBox(5, new Label("Delay (ms)"), delaySpinner),
            new VBox(5, new Label("Row Stride"), rowsSpinner)
        );
        MenuItem tessallateItem = new MenuItem("Tessellate", hbox);
        tessallateItem.setOnAction(e -> {
            Integer delayInteger = delaySpinner.getValue();
            Integer rowsInteger = rowsSpinner.getValue();
            animateTessellation(delayInteger.longValue(), rowsInteger);
        });

        cm.getItems().addAll(diffuseColorItem, specColorItem,
            captureItem, tessallateItem, warpItem);
        cm.setAutoFix(true);
        cm.setAutoHide(true);
        cm.setHideOnEscape(true);
        cm.setOpacity(0.85);
        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (!cm.isShowing()) {
                    cm.show(subScene.getParent(), e.getScreenX(), e.getScreenY());
                } else {
                    cm.hide();
                }
                e.consume();
            }
        });

        loadSurf3D();
        makeLines();

        BorderPane bpOilSpill = new BorderPane(subScene);
        stackPane.getChildren().clear();
        stackPane.getChildren().addAll(bpOilSpill);
        stackPane.setPadding(new Insets(10));
        stackPane.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255), CornerRadii.EMPTY, Insets.EMPTY)));
        Scene scene = new Scene(stackPane, 1000, 1000);
        scene.setOnMouseEntered(event -> subScene.requestFocus());
        scene.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        scene.addEventHandler(DragEvent.DRAG_DROPPED,
            e -> ResourceUtils.onDragDropped(e, scene));
        scene.addEventHandler(ImageEvent.NEW_TEXTURE_SURFACE,
            e -> vectorizeImage(1, (Image) e.object));
        scene.addEventHandler(TerrainEvent.NEW_TERRAIN_TEXTFILE, e -> {
            buildTerrain((List<List<Double>>) e.object, 4, 1, 4);
        });
        scene.addEventHandler(TerrainEvent.NEW_FIREAREA_TEXTFILE,
            e -> buildTerrain((List<List<Double>>) e.object, 1, 1, 1));

        primaryStage.setTitle("Vectorize Image Test");
        primaryStage.setScene(scene);
        primaryStage.show();
        loadDirectedMesh();

        Platform.runLater(() -> {
            try {
//                Image image = new Image(SurfaceWarpTest.class.getResourceAsStream("tiki.png"));
                Image image = new Image(SurfaceWarpTest.class.getResourceAsStream("retrowaveSun_512.png"));
//                Image image = new Image(SurfaceWarpTest.class.getResourceAsStream("moon_1024.png"));
                currentImage = image;

                cylinderSurfPlot = new HyperSurfacePlotMesh(
                    Double.valueOf(image.getWidth()).intValue(),
                    Double.valueOf(image.getHeight()).intValue(),
                    64, 64, 5, 5, vert3DLookup);
                cylinderSurfPlot.setTextureModeVertices3D(1530, p -> p.y, 0.0, 360.0);

                vectorizeImage(1, image);
                ImageView iv = new ImageView(image);
                sceneRoot.getChildren().add(iv);

            } catch (Exception ex) {
                Logger.getLogger(SurfaceWarpTest.class.getName()).log(Level.SEVERE, null, ex);
            }

        });
    }

    public BufferedImage scale(BufferedImage img, int targetWidth, int targetHeight) {

        int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;

        BufferedImage ret = img;
        BufferedImage scratchImage = null;
        Graphics2D g2 = null;

        int w = img.getWidth();
        int h = img.getHeight();

        int prevW = w;
        int prevH = h;

        do {
            if (w > targetWidth) {
                w /= 2;
                w = (w < targetWidth) ? targetWidth : w;
            }

            if (h > targetHeight) {
                h /= 2;
                h = (h < targetHeight) ? targetHeight : h;
            }

            if (scratchImage == null) {
                scratchImage = new BufferedImage(w, h, type);
                g2 = scratchImage.createGraphics();
            }

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(ret, 0, 0, w, h, 0, 0, prevW, prevH, null);

            prevW = w;
            prevH = h;
            ret = scratchImage;
        } while (w != targetWidth || h != targetHeight);

        if (g2 != null) {
            g2.dispose();
        }

        if (targetWidth != ret.getWidth() || targetHeight != ret.getHeight()) {
            scratchImage = new BufferedImage(targetWidth, targetHeight, type);
            g2 = scratchImage.createGraphics();
            g2.drawImage(ret, 0, 0, null);
            g2.dispose();
            ret = scratchImage;
        }

        return ret;

    }

    public Image captureImage() {
        System.out.println("Capturing Web Cam Image...");
        BufferedImage img = null;
        WritableImage image = null;
        if ((img = webCam.getImage()) != null) {
            BufferedImage scaledImage = scale(img, 1024, 768);
            image = new WritableImage(scaledImage.getWidth(), scaledImage.getHeight());
            SwingFXUtils.toFXImage(scaledImage, image);
        } else {
            image = new WritableImage(1024, 768);
        }
        return image;
    }

    public void buildTerrain(List<List<Double>> dataGrid, float xScale, float yScale, float zScale) {
        System.out.println("terrain file dropped...");
        this.dataGrid = dataGrid;

        int subDivX = dataGrid.get(0).size() / currentPskip;
        int subDivZ = dataGrid.size() / currentPskip;
        surfPlot.updateMeshRaw(subDivX, subDivZ, xScale, yScale, zScale);
        surfPlot.setTranslateX(-subDivX * xScale);
    }

    public Color colorLookup(int x, int y) {
        return currentImage.getPixelReader().getColor(y, x);
    }

    public void buildWarp(List<List<Double>> dataGrid, int pskip, float xScale, float yScale, float zScale) {
        System.out.println("warping mesh...");
        pNodes.clear();
        long startTime = System.nanoTime();
//        int pskip = 1;
        int columnWidth = dataGrid.get(0).size() - 1;
        int rowHeight = dataGrid.size() - 1;
        //make a warped version of the dataGrid
//        this.dataGrid = dataGrid;
//        List<List<Point3D>> warpedGrid = new ArrayList<>();
        float[] pointFloats = new float[3 * rowHeight * columnWidth];
        int pointFloatIndex = 0;
        double radius = 400;
        double degreeSpacing = 360.0 / columnWidth;
        double rowYSpacing = 5;
        double currentY = 0;
        double elevationScale = 2;
        //for each row
        for (int rowIndex = 0; rowIndex < rowHeight; rowIndex++) {
            double currentDegree = 0;
//            List<Point3D> pointRow = new ArrayList<>();
            for (int colIndex = 0; colIndex < columnWidth; colIndex++) {
                double d = dataGrid.get(rowIndex).get(colIndex);
                //circle formula
                Double x = ((d * elevationScale) + radius) * Math.cos(Math.toRadians(currentDegree));
                Double y = currentY;
                Double z = ((d * elevationScale) + radius) * -Math.sin(Math.toRadians(currentDegree));

//                pointRow.add(new Point3D(x, y, z));
                pointFloats[pointFloatIndex++] = x.floatValue();
                pointFloats[pointFloatIndex++] = y.floatValue();
                pointFloats[pointFloatIndex++] = z.floatValue();
                currentDegree += degreeSpacing;
                Perspective3DNode pNode = new Perspective3DNode(x, y, z, 0, 0, 0);
                pNode.nodeColor = colorLookup(rowIndex * pskip, colIndex * pskip);
                pNode.visible = true;
                pNodes.add(pNode);
            }
            currentY += rowYSpacing;
//            warpedGrid.add(pointRow);
        }
        Utils.printTotalTime(startTime);
        System.out.println("dude the warp is done.");
        System.out.println("Updating the scatter mesh... ");
        updateView(true);
//        surfPlot.setTranslateX(-subDivX * xScale);
//
//        if (!sceneRoot.getChildren().contains(cylinderSurfPlot)) {
//            sceneRoot.getChildren().add(cylinderSurfPlot);
//        }
//        cylinderSurfPlot = new HyperSurfacePlotMesh(
//            Double.valueOf(currentImage.getWidth()).intValue(),
//            Double.valueOf(currentImage.getHeight()).intValue(),
//            64, 64, 5, 5, vert3DLookup);
//        cylinderSurfPlot.setTextureModeVertices3D(1530, colorByLabelFunction, 0.0, 360.0);
//        cylinderSurfPlot.setAllVerts(pointFloats);
    }

    public void loadDirectedMesh() {
        System.out.println("Loading Directed Mesh...");
//        //@TODO SMP this is where you might load some dank data from a file
//        showAll();
//        updatePNodes();
        Perspective3DNode pNode = new Perspective3DNode(0, 0, 0, 0, 0, 0);
        pNode.nodeColor = Color.WHITE;
        pNode.visible = true;
        pNodes.add(pNode);
        Perspective3DNode[] pNodeArray = pNodes.toArray(Perspective3DNode[]::new);
        data = getVisiblePoints(pNodeArray);
        endPoints = getFixedEndPoints(pNodeArray, 0f);
        System.out.println("Rendering 3D Mesh...");

        scatterMesh3D = new DirectedScatterMesh(data, endPoints, true, 10, 0);
        scatterMesh3D.setHeight(point3dSize);
        scatterMesh3D.setDrawMode(DrawMode.FILL);
        scatterMesh3D.setTextureModeVertices3D(TOTAL_COLORS, colorByLabelFunction, 0.0, 360.0);
        scatterMesh3D.setMouseTransparent(true);
        Platform.runLater(() -> {
            //System.out.println("Hyperspace render complete.");
            if (!sceneRoot.getChildren().contains(scatterMesh3D)) {
                sceneRoot.getChildren().add(scatterMesh3D);
            }
        });
    }
//    private void updatePNodes() {
//        pNodes.clear();
//        HyperspaceSeed[] seeds = hyperspaceSeeds.toArray(HyperspaceSeed[]::new);
//        HyperspaceSeed seed;
//        for (int i = 0; i < seeds.length; i++) {
//            seed = seeds[i];
//            seed.x = 0;
//            seed.y = 1;
//            seed.z = 2;
//            seed.xDir = 3;
//            seed.yDir = 4;
//            seed.zDir = 5;
//            seed.visible = true;
//            addPNodeFromSeed(seed);
//        }
//    }

    public Perspective3DNode addPNodeFromSeed(HyperspaceSeed seed) {
        //make sure our extra dimensions are within the data vector's width
        int xDir = seed.xDir >= seed.vector.length ? seed.vector.length - 1 : seed.xDir;
        int yDir = seed.yDir >= seed.vector.length ? seed.vector.length - 1 : seed.yDir;
        int zDir = seed.zDir >= seed.vector.length ? seed.vector.length - 1 : seed.zDir;
        Perspective3DNode pNode = new Perspective3DNode(
            seed.vector[seed.x], seed.vector[seed.y], seed.vector[seed.z],
            seed.vector[xDir], seed.vector[yDir], seed.vector[zDir],
            seed);
        double minX = -1.0;
        double minY = -1.0;
        double minZ = -1.0;
        double domainWidth = 2.0;
        pNode.nodeColor = Perspective3DNode.getPNodeColor(
            HyperspaceEvent.COLOR_MODE.COLOR_BY_LABEL,
            ColorMapEvent.COLOR_MAP.ONE_COLOR_SPECTRUM, seed,
            minX, minY, minZ, domainWidth);
        pNode.visible = pNode.factorAnalysisSeed.visible;
        pNodes.add(pNode);
        return pNode;
    }

    public void updateView(boolean forcePNodeUpdate) {
//        if (forcePNodeUpdate) //pointScale
//            updatePNodes();
        if (null != scatterMesh3D) {
            Perspective3DNode[] pNodeArray = pNodes.toArray(Perspective3DNode[]::new);
            data.clear();
            data.addAll(pNodes.stream()
                .map(JavaFX3DUtils.pNodetoFXYZ3D) //automatically sets f value to color hue scalar
                .toList());
//            data = getVisiblePoints(pNodeArray);
//            //Implementation for directional arrows based on additional dimensions
//            if(false) //if (directionEnabled)
//                endPoints = getEndPoints(pNodeArray, 5);
//            else //default is equilateral tetrahedra (no direction)
            endPoints = getFixedEndPoints(pNodeArray, 0f);
            scatterModel.pointScale = 1.0;
            Platform.runLater(() -> hardDraw());
        }
    }

    private ArrayList<org.fxyz3d.geometry.Point3D> getVisiblePoints(Perspective3DNode[] pNodeArray) {
        //Build scatter model
        if (null == scatterModel) {
            scatterModel = new DirectedScatterDataModel();
        }
        //clear existing data
        scatterModel.reset();
        //synch model reflection status with current boolean value
        scatterModel.reflectY = true;
        //Add our nodes to the model's collection
        Collections.addAll(scatterModel.pNodes, pNodeArray);
        //True calls updateModel which is where the pain is
        updateScatterLimits(1.0, true);
        return scatterModel.data;
    }

    public void updateScatterLimits(double bufferScale, boolean updateModel) {
        //Check flag to see if we are auto normalizing
        double buff = bufferScale;
        scatterModel.setLimits(-buff, buff, -buff, buff, -buff, buff);
        scatterModel.setShifts(0, 0, 0);
        if (updateModel) {
            scatterModel.updateModel(sceneWidth, sceneHeight);
        }
    }

    private ArrayList<org.fxyz3d.geometry.Point3D> getFixedEndPoints(Perspective3DNode[] pNodes, float fixedSize) {
        org.fxyz3d.geometry.Point3D[] endArray = new org.fxyz3d.geometry.Point3D[pNodes.length];
        //Fix endpoints so they are just zero adds
        Arrays.parallelSetAll(endArray, i -> new org.fxyz3d.geometry.Point3D(fixedSize, fixedSize, fixedSize));
        return new ArrayList<>(Arrays.asList(endArray));
    }

    private ArrayList<org.fxyz3d.geometry.Point3D> getEndPoints(Perspective3DNode[] pNodes, float fixedSize) {
        ArrayList<org.fxyz3d.geometry.Point3D> ends = new ArrayList<>(pNodes.length);
        for (int i = 0; i < pNodes.length; i++) {
            ends.add(new org.fxyz3d.geometry.Point3D(pNodes[i].xDirCoord * fixedSize,
                pNodes[i].xDirCoord * fixedSize, pNodes[i].xDirCoord * fixedSize));
        }
        return ends;
    }

    private void hardDraw() {
//        if (heightChanged) { //if it hasn't changed, don't call expensive height change
//            scatterMesh3D.setHeight(point3dSize);
//            heightChanged = false;
//        }
        //if there is data and their end points are bounded
        //set the start and end points of the mesh
        if (!data.isEmpty() && !endPoints.isEmpty()) {
            scatterMesh3D.setScatterDataAndEndPoints(data, endPoints);
        }
        //Since we changed the mesh unfortunately we have to reset the color mode
        //otherwise the triangles won't have color.
        scatterMesh3D.setTextureModeVertices3D(TOTAL_COLORS, colorByLabelFunction, 0.0, 360.0);
//        isDirty = false;
    }

    public void makeLines() {
        if (null != surfPlotLinesMeshView) {
            sceneRoot.getChildren().remove(surfPlotLinesMeshView);
        }
        surfPlotLinesTriangleMesh = new TriangleMesh();
        TriangleMesh surfMesh = (TriangleMesh) surfPlot.getMesh();
        surfPlotLinesTriangleMesh.getPoints().addAll(surfMesh.getPoints());
        surfPlotLinesTriangleMesh.getTexCoords().addAll(surfMesh.getTexCoords());
        surfPlotLinesTriangleMesh.getFaces().addAll(surfMesh.getFaces());

        surfPlotLinesMeshView = new MeshView(surfPlotLinesTriangleMesh);
        PhongMaterial surfPlotLinesMaterial = new PhongMaterial(diffuseColorPicker.getValue());
        surfPlotLinesMeshView.setMaterial(surfPlotLinesMaterial);
        surfPlotLinesMeshView.setDrawMode(DrawMode.LINE);
        surfPlotLinesMeshView.setCullFace(CullFace.NONE);
        surfPlotLinesMeshView.setMouseTransparent(true);

        sceneRoot.getChildren().add(surfPlotLinesMeshView);
//        surfPlotLinesMeshView.setTranslateX(surfPlot.getTranslateX());
//        surfPlotLinesMeshView.setTranslateZ(surfPlot.getTranslateZ());
    }

    public void animateTessellation(long ms, int rows) {
        if (null != tessellationTimer) {
            tessellationTimer.stop();
        }
        makeLines();
        surfPlotLinesTriangleMesh.getFaces().clear();
        TriangleMesh surfMesh = (TriangleMesh) surfPlot.getMesh();
        surfPlotLinesTriangleMesh.getFaces().ensureCapacity(surfMesh.getFaces().size());

        tessellationTimer = new AnimationTimer() {
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_MILLI = 1_000_000;
            int faceIndex = 0;

            @Override
            public void handle(long now) {
                sleepNs = ms * NANOS_IN_MILLI;
                if ((now - prevTime) < sleepNs) {
                    return;
                }
                prevTime = now;
                TriangleMesh surfMesh = (TriangleMesh) surfPlot.getMesh();
                int facesRowWidth = (dataGrid.get(0).size() * 6) * 2;
                int newFaces = facesRowWidth * rows;

                if (faceIndex + newFaces > surfMesh.getFaces().size()) {
                    newFaces = surfMesh.getFaces().size() - faceIndex;
                }
                surfPlotLinesTriangleMesh.getFaces().addAll(surfMesh.getFaces(), faceIndex, newFaces);

                faceIndex += newFaces;
                if (faceIndex >= surfMesh.getFaces().size()) {
                    this.stop();
                    System.out.println("Tessellation Complete.");
                    animateTexture();
                }
            }

            ;
        };
        tessellationTimer.start();
    }

    public void animateTexture() {
        Task task = new Task() {
            @Override
            protected Void call() throws Exception {
                Random rando = new Random();
                Image diffuseImage = ((PhongMaterial) surfPlot.getMaterial()).getDiffuseMap();
                PixelReader pr = diffuseImage.getPixelReader();
                int rgb, r, g, b;
                int totalHeight = Double.valueOf(diffuseImage.getHeight()).intValue();
                int totalWidth = Double.valueOf(diffuseImage.getWidth()).intValue();

                WritableImage writeImage = new WritableImage(totalWidth, totalHeight);
                PhongMaterial phong = new PhongMaterial(Color.GRAY,
                    writeImage, null, null, null);
                surfPlotLinesMeshView.setMaterial(phong);
                int halfWidth = totalWidth / 2;
                int fivePercent = halfWidth / 20;
                if (fivePercent < 1) {
                    fivePercent = 1;
                }
                int randomWidth;
                for (int height = 0; height < totalHeight - 1; height++) {
                    randomWidth = rando.nextInt(fivePercent) + halfWidth;
                    for (int width = 0; width < totalWidth - 1; width++) {
                        rgb = ((int) pr.getArgb(width, height));
                        r = (rgb >> 16) & 0xFF;
                        g = (rgb >> 8) & 0xFF;
                        b = rgb & 0xFF;
                        if (width <= randomWidth) {
                            writeImage.getPixelWriter().setArgb(width, height, rgb);
                        } else {
                            writeImage.getPixelWriter().setColor(width, height, diffuseColorPicker.getValue());
                        }
                    }
                }
                return null;
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

    }

    private void vectorizeImage(int pskip, Image image) {
//        Float pSkip = 2.0f;
//        int pskip = pSkip.intValue();
        float scale = 1.0f;
        float maxH = 50;
        int rgb, r, g, b;
        // Create points and texCoords
        dataGrid.clear();
        List<Double> newRow;
        int subDivX = (int) image.getWidth() / pskip;
        int subDivZ = (int) image.getHeight() / pskip;
        int numDivX = subDivX + 1;
        int numVerts = (subDivZ + 1) * numDivX;
        final int texCoordSize = 2;
        float currZ, currX;
        float texCoords[] = new float[numVerts * texCoordSize];
        int faceCount = subDivX * subDivZ * 2;
        final int faceSize = 6; //should always be 6 for a triangle mesh
        int faces[] = new int[faceCount * faceSize];
        int index, p00, p01, p10, p11, tc00, tc01, tc10, tc11;
        double yValue;
        for (int z = 0; z < subDivZ; z++) {
            currZ = (float) z / subDivZ;
            newRow = new ArrayList<>(subDivX);
            for (int x = 0; x < subDivX; x++) {
                currX = (float) x / subDivX;
                // color value for pixel at point
                rgb = ((int) image.getPixelReader().getArgb(x * pskip, z * pskip));
                r = (rgb >> 16) & 0xFF;
                g = (rgb >> 8) & 0xFF;
                b = rgb & 0xFF;
                yValue = (((r + g + b) / 3.0f) / 255.0f) * maxH;
                newRow.add(yValue);

                index = z * numDivX * texCoordSize + (x * texCoordSize);
                texCoords[index] = currX;
                texCoords[index + 1] = currZ;

                // Create faces
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
            dataGrid.add(newRow);
        }
        surfPlot.updateMeshRaw(subDivX, subDivZ, scale, scale, scale);
        ((TriangleMesh) surfPlot.getMesh()).getTexCoords().setAll(texCoords);
        ((TriangleMesh) surfPlot.getMesh()).getFaces().setAll(faces);
        surfPlotLinesMeshView.setCullFace(CullFace.NONE);
        surfPlot.setDrawMode(DrawMode.FILL);
        ((PhongMaterial) surfPlot.getMaterial()).setDiffuseMap(image);
////            ((PhongMaterial)surfPlot.getMaterial()).setBumpMap(image);
        surfPlot.setTranslateX(-(image.getWidth() / (Float.valueOf(pskip))));
        if (null == boundingBox) {
            boundingBox = new Box(dataGrid.get(0).size(), surfPlot.getMaxY() * 2, dataGrid.size());
            boundingBox.setDrawMode(DrawMode.LINE);
            sceneRoot.getChildren().add(boundingBox);
        } else {
            boundingBox.setWidth(dataGrid.get(0).size());
            boundingBox.setHeight(surfPlot.getRangeY());
            boundingBox.setDepth(dataGrid.size());
        }
        boundingBox.setTranslateX(boundingBox.getWidth() / 2.0);
        boundingBox.setTranslateZ(boundingBox.getDepth() / 2.0);
        surfPlotLinesTriangleMesh.getFaces().clear();
    }

    List<List<Double>> dataGrid = new ArrayList<>();
    Function<org.fxyz3d.geometry.Point3D, Number> colorByHeight = p -> p.y; //Color mapping function
    Function<Vert3D, Number> vert3DLookup = p -> vertToHeight(p);

    private Number vertToHeight(Vert3D p) {
        if (null != dataGrid) {
            return lookupPoint(p);
        } else {
            return 0.0;
        }
    }

    private Number lookupPoint(Vert3D p) {
        //hacky bounds check
        if (p.yIndex >= dataGrid.size()
            || p.xIndex >= dataGrid.get(0).size()) {
            return 0.0;
        }
        return dataGrid.get(p.yIndex).get(p.xIndex);
    }

    private void generateRandos(int xWidth, int zWidth, float yScale) {
        Random rando = new Random();
        if (null == dataGrid) {
            dataGrid = new ArrayList<>(zWidth);
        } else {
            dataGrid.clear();
        }
        List<Double> xList;
        for (int z = 0; z < zWidth; z++) {
            xList = new ArrayList<>(xWidth);
            for (int x = 0; x < xWidth; x++) {
                xList.add(rando.nextDouble() * yScale);
            }
            dataGrid.add(xList);
        }
    }

    private void loadSurf3D() {
        System.out.println("Rendering Surf3D Mesh...");
        int TOTAL_COLORS = 1530; //colors used by map function
        int xWidth = 200;
        int zWidth = 200;
        float yScale = 5;
        float surfScale = 5;
        generateRandos(xWidth, zWidth, yScale);

        surfPlot = new HyperSurfacePlotMesh(xWidth, zWidth,
            64, 64, yScale, surfScale, vert3DLookup);
        surfPlot.setTextureModeVertices3D(TOTAL_COLORS, colorByHeight, 0.0, 360.0);

//        surfPlot.setTranslateX(-(xWidth*surfScale)/2.0);
//        surfPlot.setTranslateZ(-(zWidth*surfScale)/2.0);
        sceneRoot.getChildren().add(surfPlot);
    }

    private void mouseDragCamera(MouseEvent me) {
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);
        double modifier = 1.0;
        double modifierFactor = 0.1;

        if (me.isControlDown()) {
            modifier = 0.1;
        }
        if (me.isShiftDown()) {
            modifier = 10.0;
        }
        if (me.isPrimaryButtonDown()) {
            if (me.isAltDown()) { //roll
                cameraTransform.rz.setAngle(((cameraTransform.rz.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
            } else {
                cameraTransform.ry.setAngle(((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
                cameraTransform.rx.setAngle(
                    MathUtils.clamp(-60,
                        (((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180),
                        60)); // -
            }
        } else if (me.isMiddleButtonDown()) {
            cameraTransform.t.setX(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3); // -
            cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3); // -
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
