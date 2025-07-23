package edu.jhuapl.trinity.utils.fun;

import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.FloatMap;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;

public class CRTCurvature {

    private final Node target;
    private final StackPane container;
    private final ImageView curvatureView;
    private final DoubleProperty curvatureAmount = new SimpleDoubleProperty(0.15);
    private final boolean autoUpdate;
    private final long updateIntervalNanos;
    private long lastUpdate = 0;
    private AnimationTimer updater;

    private CRTCurvature(Node target,
                         double curvatureAmount,
                         boolean autoUpdate,
                         long updateIntervalMillis) {
        this.target = target;
        this.curvatureAmount.set(curvatureAmount);
        this.autoUpdate = autoUpdate;
        this.updateIntervalNanos = updateIntervalMillis * 1_000_000;

        curvatureView = new ImageView();
        curvatureView.setPreserveRatio(false);
        curvatureView.setSmooth(true);
        curvatureView.setCache(false);

        container = new StackPane(curvatureView);
        container.setPickOnBounds(false);
        container.setMouseTransparent(true);

        this.curvatureAmount.addListener((obs, oldVal, newVal) -> applyCurvature());

        applyCurvature();

        if (this.autoUpdate) {
            updater = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (now - lastUpdate >= updateIntervalNanos) {
                        applyCurvature();
                        lastUpdate = now;
                    }
                }
            };
            updater.start();
        }
    }

    private void applyCurvature() {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setTransform(Transform.scale(1, 1));
        WritableImage snapshot = target.snapshot(params, null);

        int w = (int) snapshot.getWidth();
        int h = (int) snapshot.getHeight();

        if (w <= 0 || h <= 0) return;

        FloatMap map = new FloatMap();
        map.setWidth(w);
        map.setHeight(h);

        double strength = curvatureAmount.get();

        for (int y = 0; y < h; y++) {
            double ny = (y - h / 2.0) / (h / 2.0);
            for (int x = 0; x < w; x++) {
                double nx = (x - w / 2.0) / (w / 2.0);
                double dx = nx * strength;
                double dy = ny * strength;
                map.setSamples(x, y, (float) dx, (float) dy);
            }
        }

        DisplacementMap displacement = new DisplacementMap(map);
        curvatureView.setImage(snapshot);
        curvatureView.setEffect(displacement);
    }

    public Node getView() {
        return container;
    }

    public void dispose() {
        if (updater != null) updater.stop();
    }

    // --- Property Access ---
    public DoubleProperty curvatureAmountProperty() {
        return curvatureAmount;
    }

    public double getCurvatureAmount() {
        return curvatureAmount.get();
    }

    public void setCurvatureAmount(double amount) {
        this.curvatureAmount.set(amount);
    }

    // --- Builder ---
    public static class Builder {
        private Node target;
        private double curvature = 0.15;
        private boolean autoUpdate = false;
        private long updateIntervalMillis = 100;

        public Builder target(Node node) {
            this.target = node;
            return this;
        }

        public Builder curvatureAmount(double amount) {
            this.curvature = amount;
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

        public CRTCurvature build() {
            if (target == null) throw new IllegalStateException("Target node is required");
            return new CRTCurvature(target, curvature, autoUpdate, updateIntervalMillis);
        }
    }
}
