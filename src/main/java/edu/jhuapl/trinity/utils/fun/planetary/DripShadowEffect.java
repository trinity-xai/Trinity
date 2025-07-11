package edu.jhuapl.trinity.utils.fun.planetary;

import java.util.Random;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 *
 * @author Sean Phillips
 */
public class DripShadowEffect implements PlanetaryEffect {
    @Override
    public void applyTo(Group group, double width, double height) {
        Group drips = new Group();
        Random rand = new Random();
        for (int i = 0; i < 6; i++) {
            double x = width * rand.nextDouble();
            double length = 20 + rand.nextDouble() * 30;
            Line drip = new Line(x, height - 5, x, height - 5 + length);
            drip.setStroke(Color.web("#550000", 0.3));
            drip.setStrokeWidth(2);
            drips.getChildren().add(drip);
        }
        group.getChildren().add(drips);
    }
}
