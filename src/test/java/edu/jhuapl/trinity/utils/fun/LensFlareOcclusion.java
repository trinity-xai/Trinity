package edu.jhuapl.trinity.utils.fun;

import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.fun.solar.FlareOcclusionUtil;
import edu.jhuapl.trinity.utils.fun.solar.LensFlareControls;
import edu.jhuapl.trinity.utils.fun.solar.LensFlareGroup;
import edu.jhuapl.trinity.utils.fun.solar.SunPositionControls;
import edu.jhuapl.trinity.utils.fun.solar.SunPositionTimer;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class LensFlareOcclusion extends Application {

    private double dragOffsetX, dragOffsetY;
    private LensFlareGroup flareGroup;

    @Override
    public void start(Stage stage) {
        final double SCENE_W = 800, SCENE_H = 600;
        Pane rootPane = new Pane();
        try {
            Image image = ResourceUtils.load3DTextureImage("milkywaygalaxy");
            BackgroundSize backgroundSize = new BackgroundSize(
                100, 100, true, true, false, true);
            rootPane.setBackground(new Background(new BackgroundImage(
                image, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER, backgroundSize)));
        } catch (IOException ex) {
        }

        BorderPane root = new BorderPane(rootPane);
        root.getStyleClass().add("trinity-pane");
        Scene scene = new Scene(root, SCENE_W, SCENE_H, Color.BLACK);

        // Simulated sun disk
        Circle sun = new Circle(15, Color.LIGHTYELLOW);
        sun.setOpacity(1.0);
        sun.setTranslateX(200);
        sun.setTranslateY(200);
        rootPane.getChildren().add(sun);
        enableSolarDrag(sun);

        // Draggable occluder panel
        Rectangle occluder = new Rectangle(200, 150, Color.rgb(30, 30, 30, 0.8));
        occluder.setTranslateX(300);
        occluder.setTranslateY(150);
        makeDraggable(occluder);
        rootPane.getChildren().add(occluder);

        Circle sunNeonRim = new Circle(sun.getRadius(), Color.TRANSPARENT);
        sunNeonRim.setStroke(Color.CYAN);
        sunNeonRim.setStrokeWidth(1);
        sunNeonRim.setOpacity(0.1); //inverted to alphaMultiplier
        sun.opacityProperty().addListener(cl -> sunNeonRim.setOpacity(1 - sun.getOpacity()));
        sunNeonRim.translateXProperty().bind(sun.translateXProperty());
        sunNeonRim.translateYProperty().bind(sun.translateYProperty());
        sunNeonRim.setMouseTransparent(true);
        rootPane.getChildren().add(sunNeonRim);

        SunPositionControls positionControls = new SunPositionControls();
        makeDraggable(positionControls);
        rootPane.getChildren().add(positionControls);

        flareGroup = new LensFlareGroup();
        root.getChildren().add(flareGroup);

        LensFlareControls flareControls = new LensFlareControls(flareGroup);
        makeDraggable(flareControls);
        rootPane.getChildren().add(flareControls);

        // Main loop for dynamic updates
        List<Node> occluders = List.of(occluder, positionControls, flareControls);

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                Point2D sunScene = sun.localToScene(0, 0);
                Point2D center = new Point2D(scene.getWidth() / 2, scene.getHeight() / 2);

                double flareAlpha = FlareOcclusionUtil.computeFlareAlpha(
                    sunScene.getX(), sunScene.getY(),
                    center.getX(), center.getY(),
                    scene.getWidth(), scene.getHeight());
                double occlusionFactor = FlareOcclusionUtil.computeSunOcclusionFactor(
                    sunScene, sun.getRadius(), occluders); // fade radius in pixels
                sun.setOpacity(flareAlpha * occlusionFactor);
                flareGroup.updateOpacity(flareAlpha, occlusionFactor);
            }
        }.start();

        //Make everything pretty
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);

        stage.setTitle("Lens Flare Occlusion Demo");
        stage.setScene(scene);
        stage.show();

        SunPositionTimer sunPositionTimer = new SunPositionTimer(rootPane, sun);
        sunPositionTimer.start();

        flareGroup.update(sun.getTranslateX(), sun.getTranslateY(),
            sun.getScene().getWidth() / 2.0,
            sun.getScene().getHeight() / 2.0);
    }

    private void enableSolarDrag(Circle sun) {
        final double[] dragOffset = new double[2];

        sun.setOnMousePressed(e -> {
            dragOffset[0] = sun.getTranslateX() - e.getSceneX();
            dragOffset[1] = sun.getTranslateY() - e.getSceneY();
        });

        sun.setOnMouseDragged(e -> {
            double newX = e.getSceneX() + dragOffset[0];
            double newY = e.getSceneY() + dragOffset[1];
            sun.setTranslateX(newX);
            sun.setTranslateY(newY);
        });
        sun.translateXProperty().addListener(e -> {
            flareGroup.update(sun.getTranslateX(), sun.getTranslateY(),
                sun.getScene().getWidth() / 2.0,
                sun.getScene().getHeight() / 2.0);
        });
        sun.translateYProperty().addListener(e -> {
            flareGroup.update(sun.getTranslateX(), sun.getTranslateY(),
                sun.getScene().getWidth() / 2.0,
                sun.getScene().getHeight() / 2.0);
        });
    }

    private void makeDraggable(Node node) {
        node.setOnMousePressed(e -> {
            dragOffsetX = e.getSceneX() - node.getTranslateX();
            dragOffsetY = e.getSceneY() - node.getTranslateY();
        });

        node.setOnMouseDragged(e -> {
            node.setTranslateX(e.getSceneX() - dragOffsetX);
            node.setTranslateY(e.getSceneY() - dragOffsetY);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
