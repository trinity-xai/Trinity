package edu.jhuapl.trinity.utils.fun;

/**
 * @author Sean Phillips
 */

import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;

public class ChromaticAberration {

    private final Node target;
    private final StackPane container;

    private final ImageView redView = new ImageView();
    private final ImageView greenView = new ImageView();
    private final ImageView blueView = new ImageView();

    private final DoubleProperty offsetX = new SimpleDoubleProperty(2.0);
    private final DoubleProperty offsetY = new SimpleDoubleProperty(0.0);
    private final BooleanProperty autoUpdate = new SimpleBooleanProperty(false);
    private final LongProperty updateIntervalMillis = new SimpleLongProperty(100);

    private AnimationTimer updater;
    private long lastUpdate = 0;

    private ChromaticAberration(Node target,
                                double offsetX,
                                double offsetY,
                                boolean autoUpdate,
                                long updateIntervalMillis) {
        this.target = target;
        this.offsetX.set(offsetX);
        this.offsetY.set(offsetY);
        this.autoUpdate.set(autoUpdate);
        this.updateIntervalMillis.set(updateIntervalMillis);

        container = new StackPane(blueView, greenView, redView);
        container.setPickOnBounds(false);
        container.setMouseTransparent(true);

        applyAberration();

        if (autoUpdate) {
            updater = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (now - lastUpdate >= updateIntervalMillis * 1_000_000) {
                        applyAberration();
                        lastUpdate = now;
                    }
                }
            };
            updater.start();
        }
    }

    private void applyAberration() {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setTransform(Transform.scale(1, 1));

        WritableImage baseImage = target.snapshot(params, null);
        int width = (int) baseImage.getWidth();
        int height = (int) baseImage.getHeight();

        redView.setImage(extractColorChannel(baseImage, ColorChannel.RED));
        greenView.setImage(extractColorChannel(baseImage, ColorChannel.GREEN));
        blueView.setImage(extractColorChannel(baseImage, ColorChannel.BLUE));

        redView.setTranslateX(offsetX.get());
        redView.setTranslateY(offsetY.get());

        greenView.setTranslateX(0);
        greenView.setTranslateY(0);

        blueView.setTranslateX(-offsetX.get());
        blueView.setTranslateY(-offsetY.get());
    }

    private enum ColorChannel {RED, GREEN, BLUE}

    private Image extractColorChannel(Image input, ColorChannel channel) {
        int w = (int) input.getWidth();
        int h = (int) input.getHeight();

        WritableImage result = new WritableImage(w, h);
        PixelReader reader = input.getPixelReader();
        PixelWriter writer = result.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = reader.getColor(x, y);
                double r = (channel == ColorChannel.RED) ? c.getRed() : 0;
                double g = (channel == ColorChannel.GREEN) ? c.getGreen() : 0;
                double b = (channel == ColorChannel.BLUE) ? c.getBlue() : 0;
                writer.setColor(x, y, new Color(r, g, b, c.getOpacity()));
            }
        }

        return result;
    }

    public Node getView() {
        return container;
    }

    public void dispose() {
        if (updater != null) updater.stop();
    }

    // --- Properties ---
    public DoubleProperty offsetXProperty() {
        return offsetX;
    }

    public DoubleProperty offsetYProperty() {
        return offsetY;
    }

    public BooleanProperty autoUpdateProperty() {
        return autoUpdate;
    }

    public LongProperty updateIntervalMillisProperty() {
        return updateIntervalMillis;
    }

    // --- Builder ---
    public static class Builder {
        private Node target;
        private double offsetX = 2.0;
        private double offsetY = 0.0;
        private boolean autoUpdate = false;
        private long updateIntervalMillis = 100;

        public Builder target(Node target) {
            this.target = target;
            return this;
        }

        public Builder offsetX(double offsetX) {
            this.offsetX = offsetX;
            return this;
        }

        public Builder offsetY(double offsetY) {
            this.offsetY = offsetY;
            return this;
        }

        public Builder autoUpdate(boolean enable) {
            this.autoUpdate = enable;
            return this;
        }

        public Builder updateIntervalMillis(long millis) {
            this.updateIntervalMillis = millis;
            return this;
        }

        public ChromaticAberration build() {
            if (target == null) throw new IllegalStateException("Target node must be set");
            return new ChromaticAberration(target, offsetX, offsetY, autoUpdate, updateIntervalMillis);
        }
    }
}
