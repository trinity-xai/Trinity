package edu.jhuapl.trinity.utils.fun;

import javafx.scene.image.Image;

/**
 *
 * @author Sean Phillips
 */
public abstract class AnimatedFlareSprite extends FlareSprite {

    public AnimatedFlareSprite(Image image, double scale, double position, double baseOpacity, boolean centered, String label) {
        super(image, scale, position, baseOpacity, centered, label);
    }
    @Override
    public abstract void animate(double screenX, double screenY, double timePhase);
}