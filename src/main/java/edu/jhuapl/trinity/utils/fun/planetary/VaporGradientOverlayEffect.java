package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author Sean Phillips
 */
public class VaporGradientOverlayEffect implements PlanetaryEffect {
    @Override
    public void applyTo(Group group, double width, double height) {
        Rectangle gradient = new Rectangle(0, 0, width, height);
        gradient.setFill(new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.PINK),
            new Stop(0.5, Color.LIGHTBLUE),
            new Stop(1.0, Color.MEDIUMPURPLE)
        ));
        gradient.setOpacity(0.25);
        group.getChildren().add(gradient);
    }
}

