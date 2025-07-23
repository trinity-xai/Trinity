package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 * @author Sean Phillips
 */
public class DripShadowEffect implements PlanetaryEffect {

    private final Group group = new Group();

    @Override
    public void attachTo(PlanetaryDisc disc) {
        group.getChildren().clear();
        double radius = disc.getRadius();
        double centerX = radius;
        double centerY = radius;

        for (int i = 0; i < 8; i++) {
            double x = centerX + Math.cos(i * Math.PI / 4) * radius * 0.7;
            double y = centerY + Math.sin(i * Math.PI / 4) * radius * 0.5;
            Line drip = new Line(x, y, x, y + radius * 0.3);
            drip.setStroke(Color.BLACK);
            drip.setStrokeWidth(2);
            group.getChildren().add(drip);
        }

        // Clip to planet shape with padding
        ClipUtils.applyCircularClip(group, disc.getPlanetCircle(), 4.0);
    }

    @Override
    public void update(double occlusion) {
        group.setOpacity(occlusion);
    }

    @Override
    public Node getNode() {
        return group;
    }
}
