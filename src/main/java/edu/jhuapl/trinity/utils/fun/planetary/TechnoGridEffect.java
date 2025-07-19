package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 *
 * @author Sean Phillips
 */
public class TechnoGridEffect implements PlanetaryEffect {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Color gridColor;
    private final double spacing;

    public TechnoGridEffect(Color color) {
        this.gridColor = color;
        this.spacing = 20; // Fixed grid spacing
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        double diameter = disc.getRadius() * 2;
        canvas.setWidth(diameter);
        canvas.setHeight(diameter);
        canvas.setMouseTransparent(true);
        canvas.setOpacity(gridColor.getOpacity());

        // Clip to the planet circle with inner padding
        ClipUtils.applyCircularClip(canvas, disc.getPlanetCircle(), 5.0);

        gc.clearRect(0, 0, diameter, diameter);
        gc.setStroke(gridColor);
        gc.setLineWidth(1.0);

        for (double x = 0; x < diameter; x += spacing) {
            gc.strokeLine(x, 0, x, diameter);
        }
        for (double y = 0; y < diameter; y += spacing) {
            gc.strokeLine(0, y, diameter, y);
        }
    }

    @Override
    public void update(double occlusionFactor) {
        canvas.setVisible(occlusionFactor > 0.05);
        canvas.setOpacity(gridColor.getOpacity() * occlusionFactor);
    }

    @Override
    public Node getNode() {
        return canvas;
    }
}