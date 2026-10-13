package edu.jhuapl.trinity.javafx.javafx3d.images;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * SolidColorTexture
 *
 * Builds a 1x1 {@link Image} filled with a single {@link Color}, for use as a
 * material's self-illumination map. A self-illumination map with transparent
 * pixels, paired with a null diffuse color, is this engine's established way
 * to fake a flat, unlit, semi-transparent glow - {@code Node.setOpacity()} is
 * ignored in 3D subscenes, so this is the substitute.
 *
 * <p>No animation, no ramp, no state: this returns one static image per call,
 * intended for parts whose emissive color never changes at runtime (e.g. a
 * static laser sight). For a part whose glow color or intensity changes over
 * time, see {@code LaserEmitterEntity}'s heat-glow LUT instead - that class
 * pre-builds a strip of these same 1x1 images and swaps between them.
 */
public final class SolidColorTexture {

    private SolidColorTexture() {
    }

    /** Returns a fresh 1x1 image filled with {@code color}, alpha included.
     * @param color
     * @return  */
    public static Image of(Color color) {
        WritableImage img = new WritableImage(1, 1);
        img.getPixelWriter().setColor(0, 0, color);
        return img;
    }
}