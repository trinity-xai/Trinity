package edu.jhuapl.trinity.utils.fun;

/**
 *
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
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Transform;

public class VignetteOverlay {

    private final Node target;
    private final StackPane container;
    private final ImageView baseView;
    private final Rectangle vignetteMask;

    private final DoubleProperty darkness = new SimpleDoubleProperty(0.5);
    private final DoubleProperty radius = new SimpleDoubleProperty(0.8);
    private final BooleanProperty autoUpdate = new SimpleBooleanProperty(false);
    private final LongProperty updateIntervalMS = new SimpleLongProperty(100);

    private AnimationTimer updater;
    private long lastUpdate = 0;

    private VignetteOverlay(Node target,
                            double darkness,
                            double radius,
                            boolean autoUpdate,
                            long updateIntervalMillis) {
        this.target = target;
        this.darkness.set(darkness);
        this.radius.set(radius);
        this.autoUpdate.set(autoUpdate);
        this.updateIntervalMS.set(updateIntervalMillis);

        baseView = new ImageView();
        baseView.setPreserveRatio(false);
        baseView.setSmooth(true);
        baseView.setCache(false);

        vignetteMask = new Rectangle();
        vignetteMask.setMouseTransparent(true);

        Blend blend = new Blend(BlendMode.DARKEN);
        blend.setTopInput(null);
        vignetteMask.setEffect(blend);

        container = new StackPane(baseView, vignetteMask);
        container.setPickOnBounds(false);
        container.setMouseTransparent(true);

        applyVignette();

        if (autoUpdate) {
            updater = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (now - lastUpdate >= updateIntervalMS.get() * 1_000_000) {
                        applyVignette();
                        lastUpdate = now;
                    }
                }
            };
            updater.start();
        }
    }

    private void applyVignette() {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setTransform(Transform.scale(1, 1));

        WritableImage snapshot = target.snapshot(params, null);
        baseView.setImage(snapshot);

        double w = snapshot.getWidth();
        double h = snapshot.getHeight();

        vignetteMask.setWidth(w);
        vignetteMask.setHeight(h);

        double centerX = w / 2.0;
        double centerY = h / 2.0;
        double maxRadius = Math.min(w, h) * radius.get();

        RadialGradient gradient = new RadialGradient(
            0, 0, centerX, centerY, maxRadius, false,
            CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.TRANSPARENT),
            new Stop(1.0, Color.color(0, 0, 0, darkness.get()))
        );

        vignetteMask.setFill(gradient);
    }

    public Node getView() {
        return container;
    }

    public void dispose() {
        if (updater != null) updater.stop();
    }

    // --- Properties ---
    public DoubleProperty darknessProperty() { return darkness; }
    public DoubleProperty radiusProperty() { return radius; }
    public BooleanProperty autoUpdateProperty() { return autoUpdate; }
    public LongProperty updateIntervalMillisProperty() { return updateIntervalMS; }

    // --- Builder ---
    public static class Builder {
        private Node target;
        private double darkness = 0.5;
        private double radius = 0.8;
        private boolean autoUpdate = false;
        private long updateIntervalMillis = 100;

        public Builder target(Node target) {
            this.target = target;
            return this;
        }

        public Builder darkness(double darkness) {
            this.darkness = darkness;
            return this;
        }

        public Builder radius(double radius) {
            this.radius = radius;
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

        public VignetteOverlay build() {
            if (target == null) throw new IllegalStateException("Target node must be set");
            return new VignetteOverlay(target, darkness, radius, autoUpdate, updateIntervalMillis);
        }
    }
}
