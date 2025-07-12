package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
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
    private final Rectangle overlay = new Rectangle();

    public VaporGradientOverlayEffect() {
        overlay.setMouseTransparent(true);
        overlay.setFill(new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#ffccff", 0.25)),
            new Stop(1.0, Color.web("#66ccff", 0.25))
        ));
        overlay.setBlendMode(BlendMode.OVERLAY);
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        double d = disc.getRadius() * 2;
        overlay.widthProperty().set(d);
        overlay.heightProperty().set(d);
        overlay.layoutXProperty().bind(disc.getPlanetCircle().centerXProperty().subtract(disc.getRadius()));
        overlay.layoutYProperty().bind(disc.getPlanetCircle().centerYProperty().subtract(disc.getRadius()));
    }

    @Override
    public void update(double occlusionFactor) {
        overlay.setOpacity(occlusionFactor);
    }

    @Override
    public Node getNode() {
        return overlay;
    }
}

