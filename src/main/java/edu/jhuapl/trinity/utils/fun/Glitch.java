package edu.jhuapl.trinity.utils.fun;

import java.util.HashSet;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.scene.Node;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.FloatMap;

/**
 *
 * @author Sean Phillips
 */
public class Glitch {

    private final Node target;
    private final double intensity;
    private final int rows;
    private double glitchFrequencyMS;
    private double glitchTime;
    private double glitchFreqRandomOffset = 2000.0;
    private final int width;
    private final int height;
    private double bandThicknessRatio;
    private final FloatMap floatMap;
    private final DisplacementMap displacementMap;
    private final HashSet<Integer> modifiedRows = new HashSet<>();
    private AnimationTimer glitchTimer;
    private long glitchStartNanos = 0;
    private boolean glitchActive = false;
    private final long glitchFrequencyNanos;
    private final long glitchDurationNanos;
    private long lastTriggerNanos = 0;
    private long checkIntervalNanos = 100_000_000; // e.g. 100ms
    private long lastCheckNanos = 0;
    Random random = new Random();
    public enum GlitchDisplacementMode {
        FEATHERED,   // Smooth bell-curve displacement (current)
        HARD         // Uniform full-offset band
    }    
    private GlitchDisplacementMode displacementMode = GlitchDisplacementMode.FEATHERED;

    public Glitch(Node target, int width, int height, double intensity, int rows,
            double glitchFrequencyMs, double glitchTimeMs, double bandThicknessRatio) {
        this.bandThicknessRatio = bandThicknessRatio;
        this.target = target;
        this.intensity = intensity;
        this.rows = rows;
        this.glitchFrequencyMS = glitchFrequencyMs;
        this.glitchTime = glitchTimeMs;
        this.width = width;
        this.height = height;
        this.floatMap = new FloatMap(width, height);
        this.displacementMap = new DisplacementMap();
        this.displacementMap.setMapData(floatMap);
        this.displacementMap.setScaleX(intensity);
        this.displacementMap.setScaleY(0); // horizontal glitch only
        this.glitchFrequencyNanos = (long) (glitchFrequencyMs * 1_000_000);
        this.glitchDurationNanos = (long) (glitchTimeMs * 1_000_000);

        glitchTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if ((now - lastCheckNanos) < checkIntervalNanos) {
                    return; // too soon — skip this frame
                }
                lastCheckNanos = now;
                if (!target.isVisible() || null == target.getLayoutBounds()) return;                
                if (!glitchActive && 
                    (now - lastTriggerNanos >= glitchFrequencyNanos 
                        + (long) (getRandomMillis() * 1_000_000))) {
                    applyRandomGlitch();
                    glitchStartNanos = now;
                    glitchActive = true;
                    lastTriggerNanos = now;
                } else if (glitchActive && (now - glitchStartNanos >= glitchDurationNanos)) {
                    resetFloatMap();
                    glitchActive = false;
                }
            }
        };
    }
public void setDisplacementMode(GlitchDisplacementMode mode) {
    this.displacementMode = mode;
}    

    public void setCheckIntervalMillis(long millis) {
        this.checkIntervalNanos = millis * 1_000_000L;
    }

    private double getRandomMillis() {
        return random.nextDouble() * getGlitchFreqRandomOffset();
    }

    private void applyRandomGlitch() {
        resetFloatMap();  // Clear previous glitch
        // Thickness is based on the configured ratio, then jittered randomly ±50%
        double baseHeight = height * bandThicknessRatio;
        double maxOffset = 0.5 * intensity;
        for (int i = 0; i < rows; i++) {
            // Random vertical position
            int bandStartY = (int) (Math.random() * height);
            double randomizedHeight = baseHeight * (0.5 + Math.random()); // 50%–150% of base
            int bandHeight = Math.max(1, (int) randomizedHeight);
            // Random horizontal displacement scaled by intensity
            double offset = Math.random() * 2 * maxOffset - maxOffset;
            int bandEndY = Math.min(bandStartY + bandHeight, height);
            for (int y = bandStartY; y < bandEndY; y++) {
                modifiedRows.add(y);
                float appliedOffset = (float) offset; // HARD mode — full offset for all pixels in the band
                if (displacementMode == GlitchDisplacementMode.FEATHERED) {
                    double relativeY = (double) (y - bandStartY) / bandHeight;
                    float strength = (float) (1.0 - Math.abs(relativeY - 0.5) * 2); // bell curve
                    appliedOffset = (float) (offset * strength);
                }                 // Bell curve fade (strongest at center)
                for (int x = 0; x < width; x++) {
                    if(appliedOffset != 0f)
                        floatMap.setSamples(x, y, appliedOffset, 0);
                }
            }
        }
    }

    public void resetFloatMap() {
        for (int y : modifiedRows) {
            for (int x = 0; x < width; x++) {
                floatMap.setSamples(x, y, 0f, 0f);
            }
        }
        modifiedRows.clear();
    }

    public void start() {
        target.setEffect(displacementMap);
        glitchTimer.start();
    }

    public void stop() {
        glitchTimer.stop();
        resetFloatMap();
        target.setEffect(null);
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
