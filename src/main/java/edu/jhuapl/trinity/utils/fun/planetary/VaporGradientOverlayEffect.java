package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Node;
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

    private final Rectangle gradientRect = new Rectangle();

    @Override
    public void attachTo(PlanetaryDisc disc) {
        double radius = disc.getRadius();
        double size = radius * 2;

        gradientRect.setWidth(size);
        gradientRect.setHeight(size);
        gradientRect.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#ffccff", 0.4)),
            new Stop(1, Color.web("#66ccff", 0.2))
        ));

        // ✅ Use your current ClipUtils with the actual planet circle
        ClipUtils.applyCircularClip(gradientRect, disc.getPlanetCircle(), 0.0);
    }

    @Override
    public void update(double occlusion) {
        gradientRect.setOpacity(occlusion);
    }

    @Override
    public Node getNode() {
        return gradientRect;
    }
}