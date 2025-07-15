package edu.jhuapl.trinity.utils.fun.planetary;

import java.util.Random;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.util.Duration;

/**
 *
 * @author Sean Phillips
 */
public class PixelationEffect implements PlanetaryEffect {

    private final Group group = new Group();
    private final int basePixelSize;
    private final double updateIntervalMs;
    private double pixelateTime = 300.0;
    private double pixelateFreqRandomOffset = 2000;
    private final boolean jitterPixelSize;

    private final Canvas canvas;
    private final GraphicsContext gc;
    private Timeline timeline;

    private PlanetaryDisc discWorld;

    public PixelationEffect(int basePixelSize, double updateIntervalMs, boolean jitterPixelSize) {
        this.basePixelSize = basePixelSize;
        this.updateIntervalMs = updateIntervalMs;
        this.jitterPixelSize = jitterPixelSize;
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();
        group.getChildren().add(canvas);
        group.setMouseTransparent(true);
        // Handle visibility-based lifecycle
        group.visibleProperty().addListener(e -> {
            if (timeline != null) {
                if (group.isVisible()) {
                    if (timeline.getStatus() != Animation.Status.RUNNING) {
                        timeline.play();
                    }
                } else {
                    timeline.stop();
                    gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                }
            }
        });
        group.parentProperty().addListener(il -> {
            if (group.getParent() == null && timeline != null) {
                timeline.stop();
            }
        });
        pixelateTime = 1000.0;
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        this.discWorld = disc;

        double diameter = disc.getRadius() * 2;
        canvas.setWidth(diameter);
        canvas.setHeight(diameter);

        ClipUtils.applyCircularClip(group, disc.getPlanetCircle(), 4.0);        

        timeline = new Timeline(new KeyFrame(Duration.millis(updateIntervalMs+getRandomMillis()), 
            e -> runPixelCycle()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
    private double getRandomMillis() {
        Random rando = new Random();
        return rando.nextDouble() * pixelateFreqRandomOffset;
    }    
    private void clearPixels() {
        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();   
        gc.clearRect(0, 0, width, height);        
    }
    private void runPixelCycle() {
        updatePixelation();
        // Schedule reset after glitchTime
        PauseTransition reset = new PauseTransition(Duration.millis(pixelateTime));
        reset.setOnFinished(e -> clearPixels());
        reset.play();
    }       
    private void updatePixelation() {
        if (discWorld == null) return;

        int width = (int) canvas.getWidth();
        int height = (int) canvas.getHeight();
        int pixelSize = jitterPixelSize
            ? Math.max(1, basePixelSize + (int) (Math.random() * 4 - 2))
            : basePixelSize;
        double pixelOffset = pixelSize/2.0;
        
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(0, 0, width, height));

        WritableImage snapshot = new WritableImage(width, height);
        double radius = discWorld.getRadius();
        params.setTransform(Transform.translate(-pixelOffset, -radius-pixelOffset));        
        discWorld.snapshot(params, snapshot);

        PixelReader reader = snapshot.getPixelReader();
        gc.clearRect(0, 0, width, height);

        // Properly aligned rendering loop
        for (int y = 0; y < height; y += pixelSize) {
            for (int x = 0; x < width; x += pixelSize) {
                Color color = reader.getColor(x, y);
                gc.setFill(color);
                gc.fillRect(x, y, pixelSize, pixelSize);
            }
        }
    }

    @Override
    public void update(double occlusion) {
        group.setOpacity(occlusion);
    }

    @Override
    public Node getNode() {
        return group;
    }
}