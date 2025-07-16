package edu.jhuapl.trinity.utils.fun;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.*;

public class VHSScanline extends Canvas {

    private final Node target;
    private final AnimationTimer timer;
    private boolean running = false;

    private int scanlineSpacing = 3;
    private double baseScanlineOpacity = 0.08;
    private boolean retroFlickerEnabled = true;
    private boolean chunkyModeEnabled = true;
    private boolean autoSize = false;

    private int bandCount = 3;
    private double bandHeight = 20;
    private double bandOpacity = 0.15;
    private double bandSpeed = 0.5;
    private double[] bandOffsets;
private long lastDrawNanos = 0;
private long drawIntervalNanos = 33_000_000; // default ~30 FPS (33ms)    
private double flickerOpacity = 1.0;
private long lastFlickerTime = 0;

    private final ChangeListener<Bounds> layoutListener = (obs, oldVal, newVal) -> {
        if (autoSize) Platform.runLater(this::autoSizeNow);
    };

    public VHSScanline(Node target, double width, double height) {
        super(width, height);
        this.target = target;
        setMouseTransparent(true);
//        setBlendMode(BlendMode.MULTIPLY);

        initBands();
        target.layoutBoundsProperty().addListener(layoutListener);

        // === Safe animation lifecycle ===
        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if ((now - lastDrawNanos) >= drawIntervalNanos) {
                    if(target.isVisible() && isVisible())
                        draw();
                    lastDrawNanos = now;
                }
            }
        };

        // Auto-manage based on scene lifecycle
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                start(); // attached to scene
            } else {
                stop();  // removed from scene
            }
        });
    }

    private void initBands() {
        bandOffsets = new double[bandCount];
        double height = getHeight();
        for (int i = 0; i < bandCount; i++) {
            bandOffsets[i] = Math.random() * height;
        }
    }
private void updateFlicker() {
    long now = System.currentTimeMillis();
    if (retroFlickerEnabled && now - lastFlickerTime > 60) {
        flickerOpacity = 0.5 + 0.5 * Math.sin(now * 0.005);
        lastFlickerTime = now;
    }
}
    private void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        double width = getWidth();
        double height = getHeight();
        gc.clearRect(0, 0, width, height);

        updateFlicker();
        double scanlineOpacity = baseScanlineOpacity * flickerOpacity;        

        // Scanlines
        gc.setFill(Color.rgb(0, 0, 0, scanlineOpacity));
        int offsetY = chunkyModeEnabled ? (int) (Math.random() * scanlineSpacing) : 0;
        for (int y = -offsetY; y < height; y += scanlineSpacing) {
            gc.fillRect(0, y, width, 1);
        }

        // Brightness bands
        for (int i = 0; i < bandCount; i++) {
            double y = bandOffsets[i];

            Color bandColor = Color.rgb(255, 255, 255, bandOpacity);
            gc.setFill(bandColor);
            gc.fillRect(0, y, width, bandHeight);            

            bandOffsets[i] -= bandSpeed;
            if (bandOffsets[i] + bandHeight < 0) {
                bandOffsets[i] = height + Math.random() * 50;
            }
        }
    }

    private void autoSizeNow() {
        if (target != null) {
            Bounds layoutBounds = target.getLayoutBounds();
            if (layoutBounds != null) {
                setWidth(layoutBounds.getWidth() - 5);
                setHeight(layoutBounds.getHeight() - 5);
            }
        }
    }

    // === Controls ===

    public void start() {
        if (!running) {
            timer.start();
            running = true;
        }
    }

    public void stop() {
        if (running) {
            timer.stop();
            running = false;
        }
    }

    public void dispose() {
        stop();
        target.layoutBoundsProperty().removeListener(layoutListener);
    }

    // === Configuration Setters ===

    public void setAutoSize(boolean autoSize) {
        this.autoSize = autoSize;
        if (this.autoSize) {
            Platform.runLater(this::autoSizeNow);
        }
    }
/**
 * Sets the minimum time (in milliseconds) between animation draw updates.
 * Recommended values: 16 (60fps), 33 (30fps), 50 (20fps), 100 (10fps), etc.
 */
public void setDrawIntervalMillis(long millis) {
    this.drawIntervalNanos = Math.max(1, millis) * 1_000_000;
}
    public void setScanlineSpacing(int spacing) {
        this.scanlineSpacing = spacing;
    }

    public void setBaseScanlineOpacity(double opacity) {
        this.baseScanlineOpacity = opacity;
    }

    public void setRetroFlickerEnabled(boolean enabled) {
        this.retroFlickerEnabled = enabled;
    }

    public void setChunkyModeEnabled(boolean enabled) {
        this.chunkyModeEnabled = enabled;
    }

    public void setBandCount(int count) {
        this.bandCount = count;
        initBands();
    }

    public void setBandHeight(double height) {
        this.bandHeight = height;
    }

    public void setBandOpacity(double opacity) {
        this.bandOpacity = opacity;
    }

    public void setBandSpeed(double speed) {
        this.bandSpeed = speed;
    }
}
