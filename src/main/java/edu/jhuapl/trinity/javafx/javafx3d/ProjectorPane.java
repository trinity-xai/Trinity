package edu.jhuapl.trinity.javafx.javafx3d;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.messages.xai.AnalysisConfig;
import edu.jhuapl.trinity.data.messages.xai.UmapConfig;
import edu.jhuapl.trinity.javafx.components.projector.ProjectorAnalysisNode;
import edu.jhuapl.trinity.javafx.components.projector.ProjectorNode;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
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
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.fxyz3d.geometry.MathUtils;
import org.fxyz3d.utils.CameraTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.ParallelTransition;

/**
 * @author Sean Phillips
 */

public class ProjectorPane extends StackPane {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectorPane.class);
    PerspectiveCamera camera = new PerspectiveCamera(true);
    public Group sceneRoot = new Group();
    ProjectorNodeGroup projectorNodeGroup;

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

    ImageView overlayImageView;
    File analysisDirectory = new File("./analysis");
    public Color sceneColor = Color.TRANSPARENT;
    /**
     * Provides deserialization support for JSON messages
     */
    ObjectMapper mapper;
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
            projectorNodeGroup.updateLabels();
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
            projectorNodeGroup.updateLabels();
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
            projectorNodeGroup.animateImages();
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
            projectorNodeGroup.clearAll();
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
        //add our labels to the group that will be added to the StackPane
        labelGroup = new Group();
        labelGroup.setManaged(false);
        getChildren().add(labelGroup);
        
        projectorNodeGroup = new ProjectorNodeGroup(subScene, camera, cameraTransform, labelGroup);
        projectorNodeGroup.yOffset = 1300.0; //bigger than 512...
        sceneRoot.getChildren().addAll(projectorNodeGroup);
        Sphere origin = new Sphere(20.0);
        Sphere northPole = new Sphere(20.0);
        PhongMaterial northPhong = new PhongMaterial(Color.DODGERBLUE);
        northPole.setMaterial(northPhong);
        northPole.setTranslateY(-projectorNodeGroup.originRadius);

        Sphere southPole = new Sphere(20.0);
        PhongMaterial southPhong = new PhongMaterial(Color.TOMATO);
        southPole.setMaterial(southPhong);
        southPole.setTranslateY(projectorNodeGroup.originRadius);

        sceneRoot.getChildren().addAll(origin, northPole, southPole);

    }

    public void scanAndAnimate() {
        scanAnalysisDirectory(analysisDirectory);
        projectorNodeGroup.updateLabels();
        projectorNodeGroup.animateImages();
    }

    private void scanAnalysisDirectory(File directory) {
        //sceneRoot.getChildren().removeIf(n -> n instanceof Sphere);
        projectorNodeGroup.getChildren().removeIf(n -> n instanceof Sphere);
        labelGroup.getChildren().clear();
        projectorNodeGroup.shape3DToLabel.clear();
        projectorNodeGroup.originRadius = 9001;
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
                            pn.setVisible(true);
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
                        projectorNodeGroup.addNodeToScene(pn, row, angle1, projectorNodeGroup.originRadius);

                        ParallelTransition pt = projectorNodeGroup.createTransition(pn);
                        projectorNodeGroup.transitionList.add(pt);                        

                        pn.setVisible(true);
                        row++;
                    }
                    //Add header Label based on directory name
                    //Add to hashmap so updateLabels() can manage the label position
                    Sphere columnLabelSphere = new Sphere(0.5, 1);
                    columnLabelSphere.setMaterial(new PhongMaterial(Color.GREEN));

                    // Ring formula
                    //bump the x coordinate a bit the left to left justify the label
                    double x = projectorNodeGroup.originRadius * Math.sin(angle1 * 1.1) * Math.cos(Math.PI);
                    double y = 1800 * -(projectorNodes.size() / 2); //extra offset
                    double z = projectorNodeGroup.originRadius * Math.cos(angle1);
                    columnLabelSphere.setTranslateX(x);
                    columnLabelSphere.setTranslateY(y);
                    columnLabelSphere.setTranslateZ(z);
                    //sceneRoot.getChildren().add(columnLabelSphere);
                    projectorNodeGroup.getChildren().add(columnLabelSphere);
                    Label columnLabel = new Label(subDirectory.getName());
                    columnLabel.setFont(new Font("Consolas", 20));
                    columnLabel.setTextFill(Color.SKYBLUE);
                    labelGroup.getChildren().add(columnLabel);
                    projectorNodeGroup.shape3DToLabel.put(columnLabelSphere, columnLabel);
                    //only bump the directional angle if the subdir had nodes in it
                    angle1 += angleStepSize;
                }
            }
        }
        projectorNodeGroup.updateLabels();
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
//
//    private void playNewImage(int index, String fixedPath) {
//        Platform.runLater(() -> {
//            try {
//                Thread.sleep(250); //give the watch service time to release the file
//                FileInputStream stream = new FileInputStream(fixedPath);
//                Image image = new Image(stream);
//                overlayImageView.setImage(image);
//                overlayImageView.setFitWidth(image.getWidth() * 0.9);
//                overlayImageView.setFitHeight(image.getHeight() * 0.9);
//
//                FadeTransition fadeOutTransition = new FadeTransition(Duration.seconds(8), overlayImageView);
//                fadeOutTransition.setFromValue(1.0);
//                fadeOutTransition.setToValue(0.0);
//
//                ParallelTransition flipTransition = transitionList.get(randomIndices.get(index));
//                ImageView iv = (ImageView) flipTransition.getNode();
//                iv.setImage(image);
//                iv.setFitHeight(150);
//                iv.setFitWidth(200);
//                iv.setTranslateX(iv.getTranslateX() + transitionXOffset);
//                iv.setTranslateY(iv.getTranslateY() + transitionYOffset);
//                iv.setTranslateZ(iv.getTranslateZ());
//                fadeOutTransition.setOnFinished(e -> iv.setVisible(true));
//
//                FadeTransition ivFadeTransition = new FadeTransition(Duration.seconds(2), iv);
//                ivFadeTransition.setFromValue(0.0);
//                ivFadeTransition.setToValue(1.0);
//
//                //play in order
//                SequentialTransition seq = new SequentialTransition(
//                    fadeOutTransition, ivFadeTransition);
//                seq.setOnFinished(f -> flipTransition.playFromStart());
//                seq.playFromStart();
//            } catch (FileNotFoundException | InterruptedException ex) {
//                LOG.error(null, ex);
//            }
//        });
//    }

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
                projectorNodeGroup.updateLabels();
                Timeline zoomTimeline = JavaFX3DUtils.transitionCameraTo(milliseconds, camera, cameraTransform,
                    0, 0, cameraDistance, 0.0, 0.0, 0.0);
                zoomTimeline.setOnFinished(fin -> projectorNodeGroup.updateLabels());
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
        projectorNodeGroup.updateLabels();
    }
}
