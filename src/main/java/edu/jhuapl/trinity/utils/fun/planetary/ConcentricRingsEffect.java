package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 *
 * @author Sean Phillips
 */
public class ConcentricRingsEffect implements PlanetaryEffect {
    private final Color ringColor;

    public ConcentricRingsEffect(Color ringColor) {
        this.ringColor = ringColor;
    }

    @Override
    public void applyTo(Group group, double width, double height) {
        Group rings = new Group();
        double center = width / 2;
        int ringCount = 6;
        for (int i = 1; i <= ringCount; i++) {
            Circle ring = new Circle(center, center, i * (width / 12));
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(ringColor);
            ring.setStrokeWidth(1.5);
            rings.getChildren().add(ring);
        }
        group.getChildren().add(rings);
    }
}