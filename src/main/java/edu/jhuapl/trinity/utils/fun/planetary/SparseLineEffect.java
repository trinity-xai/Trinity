package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

/**
 * @author Sean Phillips
 */
public class SparseLineEffect implements PlanetaryEffect {

    private final Group linesGroup = new Group();
    private final int lineCount;
    private final Color lineColor;
    private final boolean vertical;  // new flag: true = vertical lines, false = horizontal

    private Circle planetCircle;
    private double radius;
    private double innerPadding = 4.0;  // configurable padding inside planet radius

    public SparseLineEffect(int lineCount, Color lineColor, boolean vertical) {
        this.lineCount = lineCount;
        this.lineColor = lineColor;
        this.vertical = vertical;
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        this.planetCircle = disc.getPlanetCircle();
        this.radius = disc.getRadius();

        linesGroup.getChildren().clear();

        double centerX = planetCircle.getCenterX();
        double centerY = planetCircle.getCenterY();

        // Calculate step spacing based on radius and line count
        double step = (2 * radius - 2 * innerPadding) / (lineCount - 1);

        for (int i = 0; i < lineCount; i++) {
            Line line;
            if (vertical) {
                // X fixed, Y varies from top to bottom inside circle
                double x = centerX - radius + innerPadding + step * i;
                double startY = centerY - radius + innerPadding;
                double endY = centerY + radius - innerPadding;
                line = new Line(x, startY, x, endY);
            } else {
                // Y fixed, X varies from left to right inside circle
                double y = centerY - radius + innerPadding + step * i;
                double startX = centerX - radius + innerPadding;
                double endX = centerX + radius - innerPadding;
                line = new Line(startX, y, endX, y);
            }

            line.setStroke(lineColor);
            line.setStrokeWidth(1.2);
            line.setMouseTransparent(true);

            linesGroup.getChildren().add(line);
        }

        // Apply circular clip with padding to entire group
        ClipUtils.applyCircularClip(linesGroup, planetCircle, innerPadding);
    }

    @Override
    public void update(double occlusion) {
        // Adjust opacity based on occlusion
        linesGroup.setOpacity(occlusion);
    }

    @Override
    public Node getNode() {
        return linesGroup;
    }
}
