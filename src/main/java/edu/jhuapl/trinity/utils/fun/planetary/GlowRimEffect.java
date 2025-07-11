package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class GlowRimEffect implements PlanetaryEffect {
    private final Color glowColor;

    public GlowRimEffect(Color glowColor) {
        this.glowColor = glowColor;
    }

    @Override
    public void applyTo(Group group, double width, double height) {
        double radius = width / 2;
        Circle glow = new Circle(radius, radius, radius * 1.2);
        glow.setFill(Color.TRANSPARENT);
        glow.setStroke(glowColor);
        glow.setStrokeWidth(8);
        glow.setEffect(new GaussianBlur(25));
        group.getChildren().add(glow);
    }
}