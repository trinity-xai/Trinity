/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.javafx3d;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import org.fxyz3d.utils.CameraTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PathTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Reflection;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.transform.Rotate;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.fxyz3d.geometry.MathUtils;

/**
 * @author Sean Phillips
 */

public class ProjectorPane extends StackPane {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectorPane.class);
    PerspectiveCamera camera = new PerspectiveCamera(true);
    public Group sceneRoot = new Group();
    public SubScene subScene;
    public CameraTransformer cameraTransform = new CameraTransformer();
    private double cameraDistance = -2000;
    private final double sceneWidth = 10000;
    private final double sceneHeight = 4000;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    ArrayList<ImageView> nodes;
    ArrayList<Integer> randomIndices;
    List<ParallelTransition> transitionList = new ArrayList<>();
    double transitionXOffset = -2000;
    double transitionYOffset = -2000;
    double transitionZOffset = 0;
    ImageView overlayImageView;

//    Image[] images = new Image[]{
//        new Image(ResourceUtils.class.getResourceAsStream("/edu/jhuapl/trinity/javafx/dalle/arcadesign.png"))
//        , new Image(ResourceUtils.class.getResourceAsStream("/edu/jhuapl/trinity/javafx/dalle/clubbarsign.png"))
//        , new Image(ResourceUtils.class.getResourceAsStream("/edu/jhuapl/trinity/javafx/dalle/motel.png"))
//        , new Image(ResourceUtils.class.getResourceAsStream("/edu/jhuapl/trinity/javafx/dalle/tiki.png"))
//        , new Image(ResourceUtils.class.getResourceAsStream("/edu/jhuapl/trinity/javafx/dalle/retrowaveSun1.png"))
//        , new Image(ResourceUtils.class.getResourceAsStream("/edu/jhuapl/trinity/javafx/dalle/retrowaveSun2.png"))
//        , new Image(ResourceUtils.class.getResourceAsStream("/edu/jhuapl/trinity/javafx/dalle/retrowaveSun3.png"))
//        , new Image(ResourceUtils.class.getResourceAsStream("/edu/jhuapl/trinity/javafx/dalle/retrowaveSun4.png"))
//        , new Image(ResourceUtils.class.getResourceAsStream("/edu/jhuapl/trinity/javafx/dalle/retrowaveSun5.png"))
//        , new Image(ResourceUtils.class.getResourceAsStream("/edu/jhuapl/trinity/javafx/dalle/retrowaveSun6.png"))
//    };
    ArrayList<Image> imageList;
    Image[] images;
    int imageIndex = 0;
    File analysisDirectory = new File(".");

    double originRadius = 2300;

    BorderPane bp;
    public Color sceneColor = Color.ALICEBLUE;

    public ProjectorPane() {

        setBackground(new Background(new BackgroundFill(sceneColor, CornerRadii.EMPTY, Insets.EMPTY)));
        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        subScene.setFill(Color.CYAN);
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
            scanAnalysisDirectory(analysisDirectory);
        });

        cm.getItems().addAll(animateItem, basePathItem, scanItem);
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

        nodes = generateNodes(false);
        sceneRoot.getChildren().addAll(nodes);
        Sphere origin = new Sphere(200.0);
        Sphere northPole = new Sphere(200.0);
        PhongMaterial northPhong = new PhongMaterial(Color.DODGERBLUE);
        northPole.setMaterial(northPhong);
        northPole.setTranslateY(-originRadius);

        Sphere southPole = new Sphere(200.0);
        PhongMaterial southPhong = new PhongMaterial(Color.TOMATO);
        southPole.setMaterial(southPhong);
        southPole.setTranslateY(originRadius);

        sceneRoot.getChildren().addAll(origin, northPole, southPole);
//        Platform.runLater(() -> {
//            animateImages();
//        });
    }
    private void scanAnalysisDirectory(File directory) {
        for (File file : directory.listFiles()) {
            if(file.isDirectory()) {
                //create new column 
                //read all json files in and processs
                //do not follow recursively
            }  
        }
    }
    private ArrayList<ImageView> generateNodes(boolean generateRandom) {
        ArrayList<ImageView> newNodes = new ArrayList<>();

        double yOffset = 150;
        double angle2 = Math.PI;
        int min = -3;
        int max = 3;
        Reflection refl = new Reflection();
        refl.setFraction(0.9f);
        Glow glow = new Glow();
        glow.setLevel(0.9);
        glow.setInput(refl);

        Random rando = new Random();
        // full ring angle1+=0.1 about 440 images
        for (double angle1 = -Math.PI; angle1 <= Math.PI; angle1 += 0.1) {
            // only a wall
            //for( double angle1=Math.toRadians(angleOffsetDeg); angle1 <= Math.toRadians(90-angleOffsetDeg); angle1+=0.1) {
            for (int i = min; i <= max; i++) {
                ImageView newImageView;
                if (generateRandom)
                    newImageView = makeRandomImageView();
                else
                    newImageView = new ImageView(); //Image will be filled in later
// Ring formula
                double x = originRadius * Math.sin(angle1) * Math.cos(angle2);
                //double y = r * Math.sin(angle1) * Math.sin(angle2);
                double y = yOffset * i;
                double z = originRadius * Math.cos(angle1);

                newImageView.setTranslateX(x);
                newImageView.setTranslateY(y);
                newImageView.setTranslateZ(z);

                Rotate ry = new Rotate();
                ry.setAxis(Rotate.Y_AXIS);
                ry.setAngle(Math.toDegrees(-angle1));
                newImageView.getTransforms().addAll(ry);

                // reflection on bottom row
                if (i == max) {
                    newImageView.setEffect(glow);
                }
                newImageView.setVisible(false);
                transitionList.add(createTransition(newImageView));
                newNodes.add(newImageView);
            }
        }
        return newNodes;
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
//                    fadeOutTransition, ivFadeTransition, flipTransition);
                    fadeOutTransition, ivFadeTransition);
                seq.setOnFinished(f -> flipTransition.playFromStart());
                seq.playFromStart();
            } catch (FileNotFoundException | InterruptedException ex) {
                LOG.error(null, ex);
            }
        });
    }


    public ImageView makeRandomImageView() {
        Random rando = new Random();
        Image image = imageList.get(rando.nextInt(imageList.size()));
        ImageView iv = new ImageView(image);
        iv.setFitHeight(150);
        iv.setFitWidth(200);
        return iv;
    }

    public void animateImages() {
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
                //if( (now - last) > 1_000_000_000)
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
