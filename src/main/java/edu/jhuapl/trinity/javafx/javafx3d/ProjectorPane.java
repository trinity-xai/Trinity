package edu.jhuapl.trinity.javafx.javafx3d;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.xai.AnalysisConfig;
import edu.jhuapl.trinity.data.messages.xai.UmapConfig;
import edu.jhuapl.trinity.javafx.components.projector.ProjectorAnalysisNode;
import edu.jhuapl.trinity.javafx.components.projector.ProjectorNode;
import edu.jhuapl.trinity.javafx.components.projector.ProjectorTextNode;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.fxyz3d.geometry.MathUtils;
import org.fxyz3d.utils.CameraTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * @author Sean Phillips
 */

public class ProjectorPane extends StackPane {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectorPane.class);
    PerspectiveCamera camera = new PerspectiveCamera(true);
    public Group sceneRoot = new Group();
    public Group projectorNodeGroup;
    private Group labelGroup;
    public SubScene subScene;
    public CameraTransformer cameraTransform = new CameraTransformer();
    private double cameraDistance = -10000;
    private final double sceneWidth = 10000;
    private final double sceneHeight = 4000;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    ArrayList<ProjectorNode> nodes;
    ArrayList<Integer> randomIndices;
    List<ParallelTransition> transitionList = new ArrayList<>();
    double transitionXOffset = -15000;
    double transitionYOffset = -15000;
    double transitionZOffset = 0;
    ImageView overlayImageView;
    File analysisDirectory = new File("./analysis");
    double originRadius = 9001;
    public Color sceneColor = Color.TRANSPARENT;
    /**
     * Provides deserialization support for JSON messages
     */
    ObjectMapper mapper;
    //allows 2D labels to track their 3D counterparts
    HashMap<Shape3D, Label> shape3DToLabel = new HashMap<>();
    public boolean firstTime = true;

    public ProjectorPane() {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        setBackground(new Background(new BackgroundFill(sceneColor, CornerRadii.EMPTY, Insets.EMPTY)));
        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        subScene.setOnMouseEntered(event -> subScene.requestFocus());
        setOnMouseEntered(event -> subScene.requestFocus());
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        subScene.setFill(Color.TRANSPARENT);
        getChildren().add(subScene);
        //Start Tracking mouse movements only when a button is pressed
        subScene.setOnMousePressed((MouseEvent me) -> {
            if (me.isSynthesized())
                LOG.info("isSynthesized");
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
            updateLabels();
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
            if (keycode == KeyCode.X) {
                camera.setTranslateY(camera.getTranslateY() + change);
            }
            updateLabels();
        });
        camera = new PerspectiveCamera(true);
        //setup camera transform for rotational support
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
        subScene.setCamera(camera);
        sceneRoot.getChildren().addAll(cameraTransform);
        ContextMenu cm = new ContextMenu();
        MenuItem resetViewItem = new MenuItem("Reset View");
        resetViewItem.setOnAction(e -> {
            resetView(1000, false);
        });

        MenuItem animateItem = new MenuItem("Animate");
        animateItem.setOnAction(e -> {
            animateImages();
        });
        MenuItem basePathItem = new MenuItem("Set Analysis Directory");
        basePathItem.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Choose Analysis Directory...");
            if (!analysisDirectory.isDirectory())
                analysisDirectory = new File(".");
            dc.setInitialDirectory(analysisDirectory);
            File file = dc.showDialog(getScene().getWindow());
            if (null != file) {
                if (file.isDirectory())
                    analysisDirectory = file;
            }
        });
        MenuItem scanItem = new MenuItem("Scan Folder");
        scanItem.setOnAction(e -> {
            sceneRoot.getChildren().removeIf(n -> n instanceof ProjectorNode);
            nodes.clear();
            transitionList.clear();
            scanAndAnimate();
        });

        cm.getItems().addAll(resetViewItem, animateItem, basePathItem, scanItem);
        cm.setAutoFix(true);
        cm.setAutoHide(true);
        cm.setHideOnEscape(true);
        cm.setOpacity(0.85);
        subScene.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (!cm.isShowing())
                    cm.show(subScene.getParent(), e.getScreenX(), e.getScreenY());
                else
                    cm.hide();
                e.consume();
            }
        });

        nodes = new ArrayList<>();
        projectorNodeGroup = new Group();
        projectorNodeGroup.getChildren().addAll(nodes);
        sceneRoot.getChildren().addAll(projectorNodeGroup);
        Sphere origin = new Sphere(20.0);
        Sphere northPole = new Sphere(20.0);
        PhongMaterial northPhong = new PhongMaterial(Color.DODGERBLUE);
        northPole.setMaterial(northPhong);
        northPole.setTranslateY(-originRadius);

        Sphere southPole = new Sphere(20.0);
        PhongMaterial southPhong = new PhongMaterial(Color.TOMATO);
        southPole.setMaterial(southPhong);
        southPole.setTranslateY(originRadius);

        sceneRoot.getChildren().addAll(origin, northPole, southPole);
        //add our labels to the group that will be added to the StackPane
        labelGroup = new Group();
        labelGroup.setManaged(false);
        getChildren().add(labelGroup);
    }

    public void scanAndAnimate() {
        scanAnalysisDirectory(analysisDirectory);
        updateLabels();
        animateImages();
    }

    private void scanAnalysisDirectory(File directory) {
        sceneRoot.getChildren().removeIf(n -> n instanceof Sphere);
        labelGroup.getChildren().clear();
        shape3DToLabel.clear();
        originRadius = 9001;
        double angle1 = -Math.PI * 0.35; //@TODO SMP Dynamically figure this out
        double angleStepSize = 0.25; //@TODO SMP Dynamically figure this out
        for (File subDirectory : directory.listFiles()) {
            if (subDirectory.isDirectory()) {
                //We will recognize three different file types...
                //map images to their respective UMAP and Analysis config names
                List<ProjectorNode> projectorNodes = new ArrayList<>();

                //ProjectorNode test = new ProjectorTextNode("Dude this is a test!");
                //projectorNodes.add(test);                
                
                //search for and use image files as pivot points, do NOT follow recursively
                for (File subDirFile : subDirectory.listFiles()) {
                    if (ResourceUtils.isImageFile(subDirFile)) {
                        Image image;
                        try {
                            image = new Image(subDirFile.toURI().toURL().toExternalForm());
                            //Can we find linked umap and analysis configs?
                            UmapConfig ucForMe = findUmapConfigByName(subDirectory, subDirFile.getName());
                            AnalysisConfig acDC = findAnalysisConfigByName(subDirectory, subDirFile.getName());
                            //the configs may be null but that is ok
                            ProjectorNode pn = new ProjectorAnalysisNode(image, ucForMe, acDC);
                            projectorNodes.add(pn);
                        } catch (MalformedURLException ex) {
                            LOG.error(null, ex);
                        }
                    }
                }
                //should we create create new column
                if (!projectorNodes.isEmpty()) {
                    int row = projectorNodes.size() / 2;
                    if (row > 0)
                        row *= -1;
                    //add each projector node to the column
                    for (ProjectorNode pn : projectorNodes) {
                        addNodeToScene(pn, row, projectorNodes.size() / 2, angle1);
                        row++;
                    }
                    //Add header Label based on directory name
                    //Add to hashmap so updateLabels() can manage the label position
                    Sphere columnLabelSphere = new Sphere(0.5, 1);
                    columnLabelSphere.setMaterial(new PhongMaterial(Color.GREEN));

                    // Ring formula
                    //bump the x coordinate a bit the left to left justify the label
                    double x = originRadius * Math.sin(angle1 * 1.1) * Math.cos(Math.PI);
                    double y = 1800 * -(projectorNodes.size() / 2); //extra offset
                    double z = originRadius * Math.cos(angle1);
                    columnLabelSphere.setTranslateX(x);
                    columnLabelSphere.setTranslateY(y);
                    columnLabelSphere.setTranslateZ(z);
                    sceneRoot.getChildren().add(columnLabelSphere);
                    Label columnLabel = new Label(subDirectory.getName());
                    columnLabel.setFont(new Font("Consolas", 20));
                    columnLabel.setTextFill(Color.SKYBLUE);
                    labelGroup.getChildren().add(columnLabel);
                    shape3DToLabel.put(columnLabelSphere, columnLabel);
                    //only bump the directional angle if the subdir had nodes in it
                    angle1 += angleStepSize;
                }
            }
        }
        updateLabels();
    }

    private void animateLabelVisibility(long ms) {
        //sweet music reference: https://en.wikipedia.org/wiki/Street_Spirit_(Fade_Out)
        FadeTransition streetSpiritFadeIn = new FadeTransition(Duration.millis(ms), labelGroup);
        streetSpiritFadeIn.setFromValue(0.0);
        streetSpiritFadeIn.setToValue(1);
        streetSpiritFadeIn.playFromStart();
    }

    private void addNodeToScene(ProjectorNode projectorNode, int row, int max, double angle1) {
        double yOffset = 1300; //a bit more than 1080p height
        double angle2 = Math.PI;
        Reflection refl = new Reflection();
        refl.setFraction(0.9f);
        Glow glow = new Glow();
        glow.setLevel(0.9);
        glow.setInput(refl);

        // Ring formula
        final double x = originRadius * Math.sin(angle1) * Math.cos(angle2);
        final double y = yOffset * row;
        final double z = originRadius * Math.cos(angle1);

        projectorNode.setTranslateX(x);
        projectorNode.setTranslateY(y);
        projectorNode.setTranslateZ(z);

        Rotate ry = new Rotate();
        ry.setAxis(Rotate.Y_AXIS);
        ry.setAngle(Math.toDegrees(-angle1));
        projectorNode.getTransforms().addAll(ry);
        final double ryAngle = ry.getAngle();

//        // reflection on bottom row
//        if (row == max) {
//            projectorNode.setEffect(glow);
//        }
        //Add special click handler to zoom camera
        projectorNode.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.MIDDLE) {
                long milliseconds = 500;
                Timeline rotateTimeline = JavaFX3DUtils.transitionCameraTo(milliseconds, camera, cameraTransform,
                    0, 0, 0, 0.0, ryAngle, 0.0);
                rotateTimeline.setOnFinished(eh -> {
                    updateLabels();
                    Point3D p3D = new Point3D(x, y, z);
                    Point3D lessP3D = p3D.subtract(x * 0.5, y * 0.5, z * 0.5);
                    cameraTransform.setPivot(0, 0, 0);
                    Timeline timeline = new Timeline(
                        new KeyFrame(Duration.millis(milliseconds),
                            new KeyValue(cameraTransform.translateXProperty(), lessP3D.getX()),
                            new KeyValue(cameraTransform.translateYProperty(), y),
                            new KeyValue(cameraTransform.translateZProperty(), lessP3D.getZ())
                        )
                    );
                    timeline.setOnFinished(fin -> updateLabels());
                    timeline.playFromStart();
                });
                rotateTimeline.playFromStart();
            }
        });

        projectorNode.setVisible(false); //must animate or manually set visible later
        nodes.add(projectorNode);
        projectorNodeGroup.getChildren().add(projectorNode);
        transitionList.add(createTransition(projectorNode));
    }

    private AnalysisConfig findAnalysisConfigByName(File directory, String name) {
        String filenameOnly = ResourceUtils.removeExtension(name);
        //read all json files, do NOT follow recursively
        for (File jsonFile : directory.listFiles(f ->
            f.getName().endsWith(".json")
                || f.getName().endsWith(".JSON"))) {
            String jsonFileName = ResourceUtils.removeExtension(jsonFile.getName());
            if (jsonFileName.startsWith("AnalysisConfig-")) {
                jsonFileName = jsonFileName.replaceFirst("AnalysisConfig-", "");
                if (filenameOnly.endsWith(jsonFileName)) {
                    try {
                        String messageBody = Files.readString(jsonFile.toPath());
                        if (AnalysisConfig.isAnalysisConfig(messageBody)) {
                            return mapper.readValue(messageBody, AnalysisConfig.class);
                        }
                    } catch (IOException ex) {
                        LOG.error(null, ex);
                    }
                }
            }
        }
        return null; //if you get this far its because the file isn't there
    }

    private UmapConfig findUmapConfigByName(File directory, String name) {
        String filenameOnly = ResourceUtils.removeExtension(name);
        //read all json files, do NOT follow recursively
        for (File jsonFile : directory.listFiles(f ->
            f.getName().endsWith(".json")
                || f.getName().endsWith(".JSON"))) {
            String jsonFileName = ResourceUtils.removeExtension(jsonFile.getName());
            if (jsonFileName.contentEquals(filenameOnly)) {
                try {
                    String messageBody = Files.readString(jsonFile.toPath());
                    if (UmapConfig.isUmapConfig(messageBody)) {
                        return mapper.readValue(messageBody, UmapConfig.class);
                    }
                } catch (IOException ex) {
                    LOG.error(null, ex);
                }
            }
        }
        return null; //if you get this far its because the file isn't there
    }

    private void playNewImage(int index, String fixedPath) {
        Platform.runLater(() -> {
            try {
                Thread.sleep(250); //give the watch service time to release the file
                FileInputStream stream = new FileInputStream(fixedPath);
                Image image = new Image(stream);
                overlayImageView.setImage(image);
                overlayImageView.setFitWidth(image.getWidth() * 0.9);
                overlayImageView.setFitHeight(image.getHeight() * 0.9);

                FadeTransition fadeOutTransition = new FadeTransition(Duration.seconds(8), overlayImageView);
                fadeOutTransition.setFromValue(1.0);
                fadeOutTransition.setToValue(0.0);

                ParallelTransition flipTransition = transitionList.get(randomIndices.get(index));
                ImageView iv = (ImageView) flipTransition.getNode();
                iv.setImage(image);
                iv.setFitHeight(150);
                iv.setFitWidth(200);
                iv.setTranslateX(iv.getTranslateX() + transitionXOffset);
                iv.setTranslateY(iv.getTranslateY() + transitionYOffset);
                iv.setTranslateZ(iv.getTranslateZ());
                fadeOutTransition.setOnFinished(e -> iv.setVisible(true));

                FadeTransition ivFadeTransition = new FadeTransition(Duration.seconds(2), iv);
                ivFadeTransition.setFromValue(0.0);
                ivFadeTransition.setToValue(1.0);

                //play in order
                SequentialTransition seq = new SequentialTransition(
                    fadeOutTransition, ivFadeTransition);
                seq.setOnFinished(f -> flipTransition.playFromStart());
                seq.playFromStart();
            } catch (FileNotFoundException | InterruptedException ex) {
                LOG.error(null, ex);
            }
        });
    }

    public void animateImages() {
        labelGroup.setOpacity(0.0);
        nodes.forEach(n -> n.setVisible(false));
        AnimationTimer timer = createAnimation();
        timer.start();
    }

    private AnimationTimer createAnimation() {
        Collections.sort(transitionList, (ParallelTransition arg0, ParallelTransition arg1) -> {
            // bottom right to top left
            Point2D ref = new Point2D(1000, 1000);
            Point2D pt0 = new Point2D(arg0.getNode().getTranslateX(), arg0.getNode().getTranslateY());
            Point2D pt1 = new Point2D(arg1.getNode().getTranslateX(), arg1.getNode().getTranslateY());

            return Double.compare(ref.distance(pt0), ref.distance(pt1));
            // bottom row first
            // return -Double.compare( arg0.getNode().getTranslateY(), arg1.getNode().getTranslateY());
        });

        AnimationTimer timer = new AnimationTimer() {
            long last = 0;
            int transitionIndex = 0;

            @Override
            public void handle(long now) {
                if ((now - last) > 30_000_000) {
                    if (transitionIndex < transitionList.size()) {
                        ParallelTransition t = transitionList.get(transitionIndex);
                        t.getNode().setVisible(true);
                        t.play();
                        transitionIndex++;
                    }
                    last = now;
                }
                if (transitionIndex >= transitionList.size()) {
                    stop();
                    animateLabelVisibility(1000);
                }
            }
        };

        return timer;
    }

    private ParallelTransition createTransition(final Node node) {
        Path path = new Path();
        path.getElements().add(new MoveToAbs(node,
            node.getTranslateX() + transitionXOffset,
            node.getTranslateY() + transitionYOffset));
        path.getElements().add(new LineToAbs(node, node.getTranslateX(), node.getTranslateY()));

        Duration duration = Duration.millis(1000);

        PathTransition pt = new PathTransition(duration, path, node);

        RotateTransition rt = new RotateTransition(duration, node);
        rt.setByAngle(720);
        rt.setAutoReverse(true);

        ParallelTransition parallelTransition = new ParallelTransition();
        parallelTransition.setNode(node);
        parallelTransition.getChildren().addAll(pt, rt);
        parallelTransition.setCycleCount(1);
        return parallelTransition;
    }

    public void resetView(double milliseconds, boolean rightNow) {
        if (!rightNow) {
            cameraTransform.setPivot(0, 0, 0);
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(milliseconds),
                    new KeyValue(cameraTransform.translateXProperty(), 0),
                    new KeyValue(cameraTransform.translateYProperty(), 0),
                    new KeyValue(cameraTransform.translateZProperty(), 0)
                )
            );
            timeline.setOnFinished(eh -> {
                updateLabels();
                Timeline zoomTimeline = JavaFX3DUtils.transitionCameraTo(milliseconds, camera, cameraTransform,
                    0, 0, cameraDistance, 0.0, 0.0, 0.0);
                zoomTimeline.setOnFinished(fin -> updateLabels());
            });
            timeline.playFromStart();
        } else {
            cameraTransform.setPivot(0, 0, 0);
            cameraTransform.rx.setAngle(-10);
            cameraTransform.ry.setAngle(-45.0);
            cameraTransform.rz.setAngle(0.0);
            camera.setTranslateX(0);
            camera.setTranslateY(0);
            camera.setTranslateZ(cameraDistance);
            cameraTransform.setPivot(0, 0, 0);
        }
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
        updateLabels();
    }

    private void updateLabels() {
        shape3DToLabel.forEach((node, label) -> {
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
            if ((x + label.getWidth() + 5) > subScene.getWidth()) {
                x = subScene.getWidth() - (label.getWidth() + 5);
            }
            //is it above the view?
            if (y < 0) {
                y = 0;
            }
            //is it below the view
            if ((y + label.getHeight()) > subScene.getHeight())
                y = subScene.getHeight() - (label.getHeight() + 5);
            //@DEBUG SMP  useful debugging print
            //System.out.println("clipping Coordinates: " + x + ", " + y);
            //update the local transform of the label.
            label.getTransforms().setAll(new Translate(x, y));
        });
    }

    public static class MoveToAbs extends MoveTo {

        public MoveToAbs(Node node, double x, double y) {
            super(x - node.getLayoutX() + node.getLayoutBounds().getWidth() / 2,
                y - node.getLayoutY() + node.getLayoutBounds().getHeight() / 2);
        }
    }

    public static class LineToAbs extends LineTo {

        public LineToAbs(Node node, double x, double y) {
            super(x - node.getLayoutX() + node.getLayoutBounds().getWidth() / 2,
                y - node.getLayoutY() + node.getLayoutBounds().getHeight() / 2);
        }
    }
}
