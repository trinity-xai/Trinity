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
public class TechnoGridEffect implements PlanetaryEffect {
    private final Group grid = new Group();

    public TechnoGridEffect(Color color) {
        for (int i = 0; i <= 8; i++) {
            Line h = new Line(), v = new Line();
            h.setStroke(color);
            v.setStroke(color);
            h.setMouseTransparent(true);
            v.setMouseTransparent(true);
            grid.getChildren().addAll(h, v);
        }
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        double r = disc.getRadius();
        Circle base = disc.getPlanetCircle();
        int divisions = 4;

        for (int i = 0; i <= divisions; i++) {
            double offset = (i - divisions / 2.0) * r * 0.5;

            Line h = (Line) grid.getChildren().get(i * 2);
            Line v = (Line) grid.getChildren().get(i * 2 + 1);

            h.startXProperty().bind(base.centerXProperty().subtract(r));
            h.endXProperty().bind(base.centerXProperty().add(r));
            h.startYProperty().bind(base.centerYProperty().add(offset));
            h.endYProperty().bind(h.startYProperty());

            v.startXProperty().bind(base.centerXProperty().add(offset));
            v.endXProperty().bind(v.startXProperty());
            v.startYProperty().bind(base.centerYProperty().subtract(r));
            v.endYProperty().bind(base.centerYProperty().add(r));
        }
    }

    @Override
    public void update(double occlusionFactor) {
        grid.setOpacity(occlusionFactor);
    }

    @Override
    public Node getNode() {
        return grid;
    }
}

