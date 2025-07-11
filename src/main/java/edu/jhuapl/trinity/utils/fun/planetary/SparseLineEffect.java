package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 *
 * @author Sean Phillips
 */
public class SparseLineEffect implements PlanetaryEffect {
    private final int lineCount;
    private final Color lineColor;

    public SparseLineEffect(int lineCount, Color lineColor) {
        this.lineCount = lineCount;
        this.lineColor = lineColor;
    }

    @Override
    public void applyTo(Group group, double width, double height) {
        Group lines = new Group();
        double spacing = height / (lineCount + 1);
        for (int i = 1; i <= lineCount; i++) {
            Line line = new Line(0, i * spacing, width, i * spacing);
            line.setStroke(lineColor);
            line.setStrokeWidth(3.0);
            lines.getChildren().add(line);
        }
        group.getChildren().add(lines);
    }
}
