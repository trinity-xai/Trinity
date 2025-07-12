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
public class TargetScannerEffect implements PlanetaryEffect {
    private final Group grid = new Group();

    public TargetScannerEffect(Color color) {
        Line hLine = new Line();
        Line vLine = new Line();
        hLine.setStroke(color);
        vLine.setStroke(color);
        hLine.setStrokeWidth(1.5);
        vLine.setStrokeWidth(1.5);
        hLine.setMouseTransparent(true);
        vLine.setMouseTransparent(true);
        grid.getChildren().addAll(hLine, vLine);
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        Circle base = disc.getPlanetCircle();
        double diameter = disc.getRadius() * 2;

        Line h = (Line) grid.getChildren().get(0);
        Line v = (Line) grid.getChildren().get(1);

        h.startXProperty().bind(base.centerXProperty().subtract(diameter / 2));
        h.endXProperty().bind(base.centerXProperty().add(diameter / 2));
        h.startYProperty().bind(base.centerYProperty());
        h.endYProperty().bind(base.centerYProperty());

        v.startXProperty().bind(base.centerXProperty());
        v.endXProperty().bind(base.centerXProperty());
        v.startYProperty().bind(base.centerYProperty().subtract(diameter / 2));
        v.endYProperty().bind(base.centerYProperty().add(diameter / 2));
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

