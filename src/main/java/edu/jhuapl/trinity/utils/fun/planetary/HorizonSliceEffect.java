package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author Sean Phillips
 */
public class HorizonSliceEffect implements PlanetaryEffect {
    private final Group slices = new Group();
    private final int count;
    private final Color color;

    public HorizonSliceEffect(int count, Color color) {
        this.count = count;
        this.color = color;
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        slices.getChildren().clear();
        double spacing = disc.getRadius() * 0.05;

        for (int i = 0; i < count; i++) {
            Rectangle rect = new Rectangle();
            rect.setFill(color);
            rect.setWidth(disc.getRadius() * 2);
            rect.setHeight(spacing);
            rect.setMouseTransparent(true);
            rect.setTranslateY(i * spacing);
            slices.getChildren().add(rect);
        }

        slices.layoutXProperty().bind(disc.getPlanetCircle().centerXProperty().subtract(disc.getRadius()));
        slices.layoutYProperty().bind(disc.getPlanetCircle().centerYProperty());
    }

    @Override
    public void update(double occlusionFactor) {
        slices.setOpacity(occlusionFactor);
    }

    @Override
    public Node getNode() {
        return slices;
    }
}

