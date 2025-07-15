package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Paint;

/**
 *
 * @author Sean Phillips
 */
public class BlendOverlayEffect implements PlanetaryEffect {

    private final Group group = new Group();
    private final BlendMode blendMode;
    private final Paint overlayPaint;
    private final double opacity;

    public BlendOverlayEffect(Paint overlayPaint, BlendMode blendMode, double opacity) {
        this.overlayPaint = overlayPaint;
        this.blendMode = blendMode;
        this.opacity = opacity;
        group.setMouseTransparent(true);
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        double diameter = disc.getRadius() * 2;

        Canvas canvas = new Canvas(diameter, diameter);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(overlayPaint);
        gc.fillRect(0, 0, diameter, diameter);

        canvas.setBlendMode(blendMode);
        canvas.setOpacity(opacity);

        group.getChildren().setAll(canvas);
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