package edu.jhuapl.trinity.utils.fun;

import java.util.Random;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.FloatMap;
import javafx.util.Duration;

/**
 *
 * @author Sean Phillips
 */
public class Glitch {

    private final Node target;
    private final double intensity;
    private final int rows;
    private double glitchFrequency;
    private double glitchTime;
    private double glitchFreqRandomOffset = 2000.0;
    private final int width;
    private final int height;
    private double bandThicknessRatio;
    private final FloatMap floatMap;
    private final DisplacementMap displacementMap;
    private final Timeline timeline;

    public Glitch(Node target, int width, int height, double intensity, int rows, double glitchFrequencyMs, double glitchTimeMs, double bandThicknessRatio) {
        this.bandThicknessRatio = bandThicknessRatio;
        this.target = target;
        this.intensity = intensity;
        this.rows = rows;
        this.glitchFrequency = glitchFrequencyMs;
        this.glitchTime = glitchTimeMs;
        this.width = width;
        this.height = height;

        this.floatMap = new FloatMap(width, height);
        this.displacementMap = new DisplacementMap();
        this.displacementMap.setMapData(floatMap);
        this.displacementMap.setScaleX(intensity);
        this.displacementMap.setScaleY(0); // horizontal glitch only
        this.timeline = new Timeline(
                new KeyFrame(Duration.millis(glitchFrequency + getRandomMillis()),
                        e -> runGlitchCycle())
        );
        this.timeline.setCycleCount(Animation.INDEFINITE);
    }

    private double getRandomMillis() {
        Random rando = new Random();
        return rando.nextDouble() * getGlitchFreqRandomOffset();
    }

    private void runGlitchCycle() {
        applyRandomGlitch();
        // Schedule reset after glitchTime
        PauseTransition reset = new PauseTransition(Duration.millis(glitchTime));
        reset.setOnFinished(e -> resetFloatMap());
        reset.play();
    }

    private void applyRandomGlitch() {
        resetFloatMap();  // Clear previous glitch

        for (int i = 0; i < rows; i++) {
            // Random vertical position
            int bandStartY = (int) (Math.random() * height);

            // Thickness is based on the configured ratio, then jittered randomly ±50%
            double baseHeight = height * bandThicknessRatio;
            double randomizedHeight = baseHeight * (0.5 + Math.random()); // 50%–150% of base
            int bandHeight = Math.max(1, (int) randomizedHeight);

            // Random horizontal displacement scaled by intensity
            double maxOffset = 0.5 * intensity;
            double offset = Math.random() * 2 * maxOffset - maxOffset;

            for (int y = bandStartY; y < bandStartY + bandHeight && y < height; y++) {
                // Bell curve fade (strongest at center)
                double relativeY = (double) (y - bandStartY) / bandHeight;
                float strength = (float) (1.0 - Math.abs(relativeY - 0.5) * 2);  // peak at center
                float appliedOffset = (float) (offset * strength);

                for (int x = 0; x < width; x++) {
                    floatMap.setSamples(x, y, appliedOffset, 0);
                }
            }
        }
    }

    public void resetFloatMap() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                floatMap.setSamples(x, y, 0f, 0f);
            }
        }
    }

    public void start() {
        target.setEffect(displacementMap);
        timeline.play();
    }

    public void stop() {
        timeline.stop();
        target.setEffect(null);
    }

    public boolean isRunning() {
        return timeline.getStatus() == Animation.Status.RUNNING;
    }

    /**
     * @return the glitchFreqRandomOffset
     */
    public double getGlitchFreqRandomOffset() {
        return glitchFreqRandomOffset;
    }

    /**
     * @param glitchFreqRandomOffset the glitchFreqRandomOffset to set
     */
    public void setGlitchFreqRandomOffset(double glitchFreqRandomOffset) {
        this.glitchFreqRandomOffset = glitchFreqRandomOffset;
    }
}
