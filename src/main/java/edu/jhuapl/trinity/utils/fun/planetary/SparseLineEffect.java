package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

/**
 *
 * @author Sean Phillips
 */
public class SparseLineEffect implements PlanetaryEffect {
    private final Group lines = new Group();
    private final int count;
    private final Color color;
    private final boolean vertical;

    /**
     * @param count   Number of sparse lines to render
     * @param color   Stroke color of the lines
     * @param vertical True for vertical lines, false for horizontal
     */
    public SparseLineEffect(int count, Color color, boolean vertical) {
        this.count = Math.max(1, count);
        this.color = color;
        this.vertical = vertical;

        for (int i = 0; i < count; i++) {
            Line line = new Line();
            line.setStroke(color);
            line.setStrokeWidth(1.5);
            line.setMouseTransparent(true);
            lines.getChildren().add(line);
        }
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        double diameter = disc.getRadius() * 2;
        double spacing = diameter / (count + 1);
        Circle base = disc.getPlanetCircle();

        for (int i = 0; i < count; i++) {
            Line line = (Line) lines.getChildren().get(i);
            double offset = (i + 1) * spacing - disc.getRadius();

            if (vertical) {
                line.startXProperty().bind(base.centerXProperty().add(offset));
                line.endXProperty().bind(line.startXProperty());
                line.startYProperty().bind(base.centerYProperty().subtract(disc.getRadius()));
                line.endYProperty().bind(base.centerYProperty().add(disc.getRadius()));
            } else {
                line.startYProperty().bind(base.centerYProperty().add(offset));
                line.endYProperty().bind(line.startYProperty());
                line.startXProperty().bind(base.centerXProperty().subtract(disc.getRadius()));
                line.endXProperty().bind(base.centerXProperty().add(disc.getRadius()));
            }
        }
    }

    @Override
    public void update(double occlusionFactor) {
        lines.setOpacity(occlusionFactor);
    }

    @Override
    public Node getNode() {
        return lines;
    }
}

