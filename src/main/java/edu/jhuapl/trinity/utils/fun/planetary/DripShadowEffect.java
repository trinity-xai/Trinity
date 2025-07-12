package edu.jhuapl.trinity.utils.fun.planetary;

import java.util.Random;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

/**
 *
 * @author Sean Phillips
 */
public class DripShadowEffect implements PlanetaryEffect {
    private final Group drips = new Group();

    public DripShadowEffect() {
        for (int i = 0; i < 5; i++) {
            Line drip = new Line();
            drip.setStroke(Color.web("#220000", 0.6));
            drip.setStrokeWidth(2 + Math.random() * 2);
            drip.setMouseTransparent(true);
            drips.getChildren().add(drip);
        }
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        Circle base = disc.getPlanetCircle();
        for (int i = 0; i < drips.getChildren().size(); i++) {
            Line drip = (Line) drips.getChildren().get(i);
            double angle = (Math.PI / 2) + (i - 2) * 0.2;
            double offset = Math.cos(angle) * base.getRadius();
            drip.startXProperty().bind(base.centerXProperty().add(offset));
            drip.startYProperty().bind(base.centerYProperty().add(base.radiusProperty()));
            drip.endXProperty().bind(drip.startXProperty());
            drip.endYProperty().bind(drip.startYProperty().add(20 + Math.random() * 20));
        }
    }

    @Override
    public void update(double occlusionFactor) {
        drips.setOpacity(occlusionFactor);
    }

    @Override
    public Node getNode() {
        return drips;
    }
}

