package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * @author Sean Phillips
 */
public class ConcentricRingsEffect implements PlanetaryEffect {
    private final Group ringGroup = new Group();

    public ConcentricRingsEffect(Color ringColor) {
        int ringCount = 4;
        for (int i = 1; i <= ringCount; i++) {
            Circle ring = new Circle();
            ring.setFill(null);
            ring.setStroke(ringColor);
            ring.setStrokeWidth(1.5);
            ring.setOpacity(0.2 / i);
            ring.setMouseTransparent(true);
            ringGroup.getChildren().add(ring);
        }
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        Circle base = disc.getPlanetCircle();
        for (int i = 0; i < ringGroup.getChildren().size(); i++) {
            Circle ring = (Circle) ringGroup.getChildren().get(i);
            ring.centerXProperty().bind(base.centerXProperty());
            ring.centerYProperty().bind(base.centerYProperty());
            ring.radiusProperty().bind(base.radiusProperty().multiply(1.1 + i * 0.2));
        }
    }

    @Override
    public void update(double occlusionFactor) {
        ringGroup.setOpacity(occlusionFactor);
    }

    @Override
    public Node getNode() {
        return ringGroup;
    }
}
