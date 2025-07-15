package edu.jhuapl.trinity.utils.fun;

import java.util.Random;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Transform;
import javafx.util.Duration;

/**
 *
 * @author Sean Phillips
 */
/**
 * Reusable pixelation effect that can be applied to any JavaFX Node.
 */
public class Pixelate {

    private final Node target;
    private final int width;
    private final int height;
    private final int basePixelSize;
    private final boolean jitterPixelSize;
    private final double updateIntervalMs;
    private final Canvas canvas;
    private final GraphicsContext gc;
    private Timeline timeline;

    private double pixelateTime = 300.0;  // Duration each pixelation burst is visible
    private double pixelateFreqRandomOffset = 2000.0; // Random jitter added to update interval

    private final Random random = new Random();

    /**
     * Create a Pixelate effect on a given target Node.
     * @param target The node to pixelate.
     * @param width Width in pixels of the pixelation canvas.
     * @param height Height in pixels of the pixelation canvas.
     * @param basePixelSize Base size of pixel blocks.
     * @param jitterPixelSize If true, pixel size varies randomly ±2 pixels.
     * @param updateIntervalMs How often (ms) the pixelation updates.
     */
    public Pixelate(Node target, int width, int height, int basePixelSize, 
        boolean jitterPixelSize, double updateIntervalMs) {
        this.target = target;
        this.width = width;
        this.height = height;
        this.basePixelSize = basePixelSize;
        this.jitterPixelSize = jitterPixelSize;
        this.updateIntervalMs = updateIntervalMs;

        this.canvas = new Canvas(width, height);
        this.gc = canvas.getGraphicsContext2D();

        setupTimeline();
    }

    private void setupTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.millis(updateIntervalMs + getRandomMillis()), e -> runPixelCycle()));
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    private double getRandomMillis() {
        return random.nextDouble() * getPixelateFreqRandomOffset();
    }

    private void runPixelCycle() {
        updatePixelation();
        // Clear after pixelateTime milliseconds
        PauseTransition reset = new PauseTransition(Duration.millis(getPixelateTime()));
        reset.setOnFinished(e -> clearPixels());
        reset.play();
    }

    private void updatePixelation() {
        int pixelSize = jitterPixelSize
            ? Math.max(1, basePixelSize + (int) (Math.random() * 4 - 2))
            : basePixelSize;
        double pixelOffset = pixelSize/2.0;        
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(0, 0, width, height));
        // Adjust transform if needed for alignment
        double radius = height/2.0;
        if(target instanceof Circle circle)
            radius = circle.getRadius();
        params.setTransform(Transform.translate(-pixelOffset, -radius-pixelOffset));        
        WritableImage snapshot = new WritableImage(width, height);
        target.snapshot(params, snapshot);

        PixelReader reader = snapshot.getPixelReader();
        gc.clearRect(0, 0, width, height);

        for (int y = 0; y < height; y += pixelSize) {
            for (int x = 0; x < width; x += pixelSize) {
                Color color = reader.getColor(x, y);
                gc.setFill(color);
                gc.fillRect(x, y, pixelSize, pixelSize);
            }
        }
    }

    private void clearPixels() {
        gc.clearRect(0, 0, width, height);
    }

    /** Starts the pixelation animation */
    public void start() {
        timeline.play();
    }

    /** Stops the pixelation animation and clears effect */
    public void stop() {
        timeline.stop();
        clearPixels();
    }

    /** Returns whether the pixelation animation is running */
    public boolean isRunning() {
        return timeline.getStatus() == Animation.Status.RUNNING;
    }

    /** Returns the Canvas node displaying the pixelation */
    public Canvas getCanvas() {
        return canvas;
    }

    // Getters and setters for pixelateTime and pixelateFreqRandomOffset if you want to expose them

    /**
     * @return the pixelateTime
     */
    public double getPixelateTime() {
        return pixelateTime;
    }

    /**
     * @param pixelateTime the pixelateTime to set
     */
    public void setPixelateTime(double pixelateTime) {
        this.pixelateTime = pixelateTime;
    }

    /**
     * @return the pixelateFreqRandomOffset
     */
    public double getPixelateFreqRandomOffset() {
        return pixelateFreqRandomOffset;
    }

    /**
     * @param pixelateFreqRandomOffset the pixelateFreqRandomOffset to set
     */
    public void setPixelateFreqRandomOffset(double pixelateFreqRandomOffset) {
        this.pixelateFreqRandomOffset = pixelateFreqRandomOffset;
    }

}