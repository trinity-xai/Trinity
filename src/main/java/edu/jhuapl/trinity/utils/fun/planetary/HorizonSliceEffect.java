package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 *
 * @author Sean Phillips
 */
public class HorizonSliceEffect implements PlanetaryEffect {

    private final Group group = new Group();
    private final int lineCount;
    private final Color color;

    public HorizonSliceEffect(int lineCount, Color color) {
        this.lineCount = lineCount;
        this.color = color;
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        group.getChildren().clear();
        double radius = disc.getRadius();
        double centerX = radius;
        double centerY = radius;

        for (int i = 1; i <= lineCount; i++) {
            double y = centerY + i * (radius / lineCount);
            Line line = new Line(centerX - radius, y, centerX + radius, y);
            line.setStroke(color);
            line.setOpacity(1.0 - (i / (double) lineCount));
            group.getChildren().add(line);
        }

        ClipUtils.applyCircularClip(group,disc.getPlanetCircle(), 4.0);
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
