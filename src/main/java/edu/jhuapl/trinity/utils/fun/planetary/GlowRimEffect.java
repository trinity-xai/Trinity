package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class GlowRimEffect implements PlanetaryEffect {
    private final Circle glowCircle = new Circle();
    private final Group nodeGroup = new Group();

    public GlowRimEffect(Color glowColor) {
        this(glowColor, 8);
    }
    public GlowRimEffect(Color glowColor, double strokeWidth) {
        glowCircle.setFill(null);
        glowCircle.setStroke(glowColor);
        glowCircle.setStrokeWidth(strokeWidth);
        glowCircle.setEffect(new GaussianBlur(15));
        glowCircle.setOpacity(1.0);
        glowCircle.setMouseTransparent(true);
        nodeGroup.getChildren().add(glowCircle);
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        Circle base = disc.getPlanetCircle();
        glowCircle.centerXProperty().bind(base.centerXProperty());
        glowCircle.centerYProperty().bind(base.centerYProperty());
        glowCircle.radiusProperty().bind(base.radiusProperty().multiply(1.1));
    }

    @Override
    public void update(double occlusionFactor) {
        glowCircle.setOpacity(occlusionFactor);
    }

    @Override
    public Node getNode() {
        return nodeGroup;
    }
}