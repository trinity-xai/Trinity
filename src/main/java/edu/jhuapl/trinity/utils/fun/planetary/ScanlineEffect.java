package edu.jhuapl.trinity.utils.fun.planetary;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
/**
 *
 * @author Sean Phillips
 */
public class ScanlineEffect implements PlanetaryEffect {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final int lineSpacing;
    private final Color lineColor;

    public ScanlineEffect(int lineSpacing, Color color) {
        this.lineSpacing = lineSpacing;
        this.lineColor = color;
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        double diameter = disc.getRadius() * 2;
        canvas.setWidth(diameter);
        canvas.setHeight(diameter);
        canvas.setMouseTransparent(true);
        canvas.setOpacity(lineColor.getOpacity());

        // Clip to planet shape with padding
        ClipUtils.applyCircularClip(canvas, disc.getPlanetCircle(), 4.0);

        // Draw horizontal lines
        gc.clearRect(0, 0, diameter, diameter);
        gc.setStroke(lineColor);
        gc.setLineWidth(1.0);
        for (int y = 0; y < diameter; y += lineSpacing) {
            gc.strokeLine(0, y, diameter, y);
        }
    }

    @Override
    public void update(double occlusionFactor) {
        canvas.setVisible(occlusionFactor > 0.05); // Optional perf optimization
        canvas.setOpacity(lineColor.getOpacity() * occlusionFactor);
    }

    @Override
    public Node getNode() {
        return canvas;
    }
}
