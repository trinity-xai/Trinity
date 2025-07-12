package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 *
 * @author Sean Phillips
 */
public class AuraPulseEffect implements PlanetaryEffect {
    private final Circle aura = new Circle();
    private final Group nodeGroup = new Group();
    private final Timeline pulse;

    public AuraPulseEffect(Color auraColor) {
        aura.setFill(auraColor);
        aura.setMouseTransparent(true);
        aura.setOpacity(0.2);

        nodeGroup.getChildren().add(aura);

        // Create a pulsating scale + fade animation
        pulse = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(aura.scaleXProperty(), 1.0),
                new KeyValue(aura.scaleYProperty(), 1.0),
                new KeyValue(aura.opacityProperty(), 0.2)
            ),
            new KeyFrame(Duration.seconds(2),
                new KeyValue(aura.scaleXProperty(), 1.4),
                new KeyValue(aura.scaleYProperty(), 1.4),
                new KeyValue(aura.opacityProperty(), 0.0)
            )
        );
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        Circle base = disc.getPlanetCircle();
        aura.centerXProperty().bind(base.centerXProperty());
        aura.centerYProperty().bind(base.centerYProperty());
        aura.radiusProperty().bind(base.radiusProperty().multiply(1.2));
        pulse.playFromStart();
    }

    @Override
    public void update(double occlusionFactor) {
        nodeGroup.setOpacity(occlusionFactor);
    }

    @Override
    public Node getNode() {
        return nodeGroup;
    }
}