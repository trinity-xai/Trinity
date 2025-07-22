package edu.jhuapl.trinity.utils.fun;

import javafx.animation.AnimationTimer;
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

import java.util.Random;

public class Pixelate {

    public enum PixelationMode {
        FULL_SURFACE,
        RANDOM_BLOCKS
    }

    private final Node target;
    private final int width;
    private final int height;
    private int basePixelSize;
    private final boolean jitterPixelSize;
    private final double updateIntervalMs;

    private final Canvas canvas;
    private final GraphicsContext gc;
    private PixelationMode mode = PixelationMode.FULL_SURFACE;
    private int blockCount = 8; // only for RANDOM_BLOCKS
    private int minBlockSize = 10;
    private int maxBlockSize = 40;
    private double pixelateTime = 300.0;  // How long the pixelation lasts
    private double pixelateFreqRandomOffset = 2000.0;
    private final Random random = new Random();

    private AnimationTimer animationTimer;
    AnimationTimer pixelateTimer;
    private long lastTriggerTimeNs = 0;
    private long pixelationStartTimeNs = -1;
    private long currentIntervalNs;
    private long checkIntervalNanos = 200_000_000; // e.g. 100ms
    private long lastCheckNanos = 0;
    private boolean pixelating = false;
    private boolean centerOnY = true;

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
        canvas.setMouseTransparent(true);
        setupTimer();
    }

    private void setupTimer() {
        currentIntervalNs = millisToNanos(updateIntervalMs + getRandomMillis());
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if ((now - lastCheckNanos) < checkIntervalNanos) {
                    return; // too soon — skip this frame
                }
                lastCheckNanos = now;
                if (!pixelating) {
                    if (now - lastTriggerTimeNs >= currentIntervalNs) {
                        runPixelCycle(now);
                        lastTriggerTimeNs = now;
                        currentIntervalNs = millisToNanos(updateIntervalMs + getRandomMillis());
                    }
                } else {
                    if (now - pixelationStartTimeNs >= millisToNanos(pixelateTime)) {
                        clearPixels();
                        pixelating = false;
                    }
                }
            }
        };
    }

    private void runPixelCycle(long now) {
        updatePixelation();
        pixelating = true;
        pixelationStartTimeNs = now;
    }

    private void updatePixelation() {
        int pixelSize = jitterPixelSize
            ? Math.max(1, basePixelSize + (int) (Math.random() * 4 - 2))
            : basePixelSize;

        double pixelOffset = pixelSize / 2.0;

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(0, 0, width, height));
        if (centerOnY) {
            double radius = height / 2.0;
            if (target instanceof Circle circle) {
                radius = circle.getRadius();
            }
            params.setTransform(Transform.translate(-pixelOffset, -radius - pixelOffset));
        } else
            params.setTransform(Transform.translate(-pixelOffset, -pixelOffset));
        WritableImage snapshot = new WritableImage(width, height);
        target.snapshot(params, snapshot);

        PixelReader reader = snapshot.getPixelReader();
        gc.clearRect(0, 0, width, height);

        if (mode == PixelationMode.FULL_SURFACE) {
            for (int y = 0; y < height; y += pixelSize) {
                for (int x = 0; x < width; x += pixelSize) {
                    Color color = reader.getColor(x, y);
                    gc.setFill(color);
                    gc.fillRect(x - pixelOffset, y - pixelOffset, pixelSize, pixelSize);
                }
            }
        } else if (mode == PixelationMode.RANDOM_BLOCKS) {
            for (int i = 0; i < blockCount; i++) {
                int blockW = random.nextInt(maxBlockSize - minBlockSize + 1) + minBlockSize;
                int blockH = random.nextInt(maxBlockSize - minBlockSize + 1) + minBlockSize;

                int x = random.nextInt(Math.max(1, width - blockW));
                int y = random.nextInt(Math.max(1, height - blockH));

                for (int py = y; py < y + blockH; py += pixelSize) {
                    for (int px = x; px < x + blockW; px += pixelSize) {
                        if (px >= width || py >= height) {
                            continue;
                        }
                        Color color = reader.getColor(px, py);
                        gc.setFill(color);
                        gc.fillRect(px, py, pixelSize, pixelSize);
                    }
                }
            }
        }
    }

    private void clearPixels() {
        gc.clearRect(0, 0, width, height);
    }

    private long millisToNanos(double ms) {
        return (long) (ms * 1_000_000);
    }

    private double getRandomMillis() {
        return random.nextDouble() * pixelateFreqRandomOffset;
    }

    public void start() {
        animationTimer.start();
    }

    public void stop() {
        animationTimer.stop();
        clearPixels();
        pixelating = false;
    }

    public boolean isRunning() {
        return pixelating;
    }

    public void setMode(PixelationMode mode) {
        this.mode = mode;
    }

    public void setBlockCount(int count) {
        this.blockCount = count;
    }

    public void setBlockSizeRange(int minSize, int maxSize) {
        this.minBlockSize = minSize;
        this.maxBlockSize = maxSize;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public double getPixelateTime() {
        return pixelateTime;
    }

    public void setPixelateTime(double pixelateTime) {
        this.pixelateTime = pixelateTime;
    }

    public double getPixelateFreqRandomOffset() {
        return pixelateFreqRandomOffset;
    }

    public void setPixelateFreqRandomOffset(double pixelateFreqRandomOffset) {
        this.pixelateFreqRandomOffset = pixelateFreqRandomOffset;
    }

    /**
     * @return the centerOnY
     */
    public boolean isCenterOnY() {
        return centerOnY;
    }

    /**
     * @param centerOnY the centerOnY to set
     */
    public void setCenterOnY(boolean centerOnY) {
        this.centerOnY = centerOnY;
    }

    public void animatePixelSize(double fromSize, double toSize, long durationMillis, boolean autoReverse, boolean repeat) {

        if (pixelateTimer != null) pixelateTimer.stop();
        pixelateTimer = new AnimationTimer() {
            long pixelateDurationNanos = durationMillis * 1_000_000;
            long pixelateStartTime = System.nanoTime();
            long lastUpdateTime = 0;
            long pixelateIntervalNanos = 100_000_000;
            double pixelateFromSize = fromSize;
            double pixelateToSize = toSize;
            boolean pixelateAutoReverse = autoReverse;
            boolean pixelateRepeat = repeat;

            @Override
            public void handle(long now) {
                long elapsed = now - pixelateStartTime;
                if ((now - lastUpdateTime) < pixelateIntervalNanos) return;
                lastUpdateTime = now;
                double progress = Math.min(1.0, (double) elapsed / pixelateDurationNanos);
                //Double currentSize = pixelateFromSize + (pixelateToSize - pixelateFromSize);
                Double currentSize = pixelateFromSize + (pixelateToSize - pixelateFromSize) * progress;
                basePixelSize = currentSize.intValue();
                clearPixels();
                updatePixelation();

                if (progress >= 1.0) {
                    if (pixelateAutoReverse) {
                        // Swap direction and restart
                        double temp = pixelateFromSize;
                        pixelateFromSize = pixelateToSize;
                        pixelateToSize = temp;
                        pixelateStartTime = now;
                    } else if (pixelateRepeat) {
                        pixelateStartTime = now;
                    } else {
                        this.stop();
                    }
                }
            }
        };
        pixelateTimer.start();
    }
}
