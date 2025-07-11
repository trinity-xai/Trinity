package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 *
 * @author Sean Phillips
 */
public class TechnoGridEffect implements PlanetaryEffect {
    private final Color gridColor;

    public TechnoGridEffect(Color gridColor) {
        this.gridColor = gridColor;
    }

    @Override
    public void applyTo(Group group, double width, double height) {
        Group grid = new Group();
        int count = 10;
        double spacing = width / count;

        for (int i = 1; i < count; i++) {
            double x = i * spacing;
            Line v = new Line(x, 0, x, height);
            Line h = new Line(0, x, width, x);
            v.setStroke(gridColor);
            h.setStroke(gridColor);
            v.setStrokeWidth(0.5);
            h.setStrokeWidth(0.5);
            grid.getChildren().addAll(v, h);
        }

        group.getChildren().add(grid);
    }
}
