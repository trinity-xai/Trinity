package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.scene.Group;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 *
 * @author Sean Phillips
 */
public class AuraPulseEffect implements PlanetaryEffect {
    private final Color pulseColor;

    public AuraPulseEffect(Color pulseColor) {
        this.pulseColor = pulseColor;
    }

    @Override
    public void applyTo(Group group, double width, double height) {
        double radius = width / 2;
        Circle pulse = new Circle(radius, radius, radius * 1.1);
        pulse.setFill(pulseColor);
        pulse.setEffect(new GaussianBlur(30));

        // Animate opacity pulse
        FadeTransition fade = new FadeTransition(Duration.seconds(3), pulse);
        fade.setFromValue(0.15);
        fade.setToValue(0.35);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();

        group.getChildren().add(pulse);
    }
}

