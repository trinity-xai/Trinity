package edu.jhuapl.trinity.utils.fun;

import edu.jhuapl.trinity.utils.fun.solar.LensFlareGroup;
import edu.jhuapl.trinity.utils.fun.solar.FlareOcclusionUtil;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
/**
 *
 * @author Sean Phillips
 */
public class LensFlare extends Application {
    private double sceneW = 800;
    private double sceneH = 800;
    private Circle sun;
    private Circle sunNeonRim;
    private LensFlareGroup flareGroup;

    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Scene scene = new Scene(root, sceneW, sceneH, Color.BLACK);

        // Sun disc
        sun = new Circle(100, 100, 20, Color.LIGHTYELLOW.deriveColor(0, 1, 1, 0.5));
        GaussianBlur blur = new GaussianBlur(10);
        Glow glow = new Glow(1.0);
        glow.setInput(blur);
        sun.setEffect(glow);

        sunNeonRim = new Circle(100, 100, 20, Color.TRANSPARENT);
        sunNeonRim.setStroke(Color.LIGHTSKYBLUE);
        sunNeonRim.setStrokeWidth(1);
        sunNeonRim.setOpacity(0.1); //inverted to alphaMultiplier
        sunNeonRim.centerXProperty().bind(sun.centerXProperty());
        sunNeonRim.centerYProperty().bind(sun.centerYProperty());
        sunNeonRim.setMouseTransparent(true);
        
        flareGroup = new LensFlareGroup();
        enableDrag(sun);

        root.getChildren().addAll(flareGroup, sun, sunNeonRim);
        flareGroup.update(sun.getCenterX(), sun.getCenterY(), sceneW / 2.0, sceneH / 2.0);
        
        stage.setTitle("JavaFX Solar Lens Flare");
        stage.setScene(scene);
        stage.show();
        
        stage.heightProperty().addListener(cl -> {
            sceneH = flareGroup.getScene().getHeight();
            flareGroup.update(sun.getCenterX(), sun.getCenterY(), sceneW / 2.0, sceneH / 2.0);
        });
        stage.widthProperty().addListener(cl -> {
            sceneW = flareGroup.getScene().getWidth();
            flareGroup.update(sun.getCenterX(), sun.getCenterY(), sceneW / 2.0, sceneH / 2.0);
        });
        
    }

    private void enableDrag(Circle sun) {
        final double[] dragOffset = new double[2];

        sun.setOnMousePressed(e -> {
            dragOffset[0] = sun.getCenterX() - e.getSceneX();
            dragOffset[1] = sun.getCenterY() - e.getSceneY();
        });

        sun.setOnMouseDragged(e -> {
            double newX = e.getSceneX() + dragOffset[0];
            double newY = e.getSceneY() + dragOffset[1];
            sun.setCenterX(newX);
            sun.setCenterY(newY);
            flareGroup.update(newX, newY, sceneW / 2.0, sceneH / 2.0);
            double alphaMultiplier = FlareOcclusionUtil.computeFlareAlpha(
                newX, newY, sceneW / 2.0, sceneH / 2.0, sceneW, sceneH);
            sun.setOpacity(Math.max(alphaMultiplier, 0.1));
            sunNeonRim.setOpacity(Math.max(1-alphaMultiplier, 0.1));
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
