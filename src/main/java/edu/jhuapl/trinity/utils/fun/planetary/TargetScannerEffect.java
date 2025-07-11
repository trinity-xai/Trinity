package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.scene.Group;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 *
 * @author Sean Phillips
 */
public class TargetScannerEffect implements PlanetaryEffect {
    private final Color scanColor;

    public TargetScannerEffect(Color scanColor) {
        this.scanColor = scanColor;
    }

    @Override
    public void applyTo(Group group, double width, double height) {
        double center = width / 2;

        Circle scanner = new Circle(center, center, width / 6);
        scanner.setFill(Color.TRANSPARENT);
        scanner.setStroke(scanColor);
        scanner.setStrokeWidth(2);
        scanner.setEffect(new GaussianBlur(5));

        ScaleTransition scale = new ScaleTransition(Duration.seconds(2.5), scanner);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(2.5);
        scale.setToY(2.5);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setAutoReverse(true);
        scale.play();

        group.getChildren().add(scanner);
    }
}
