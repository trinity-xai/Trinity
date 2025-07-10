package edu.jhuapl.trinity.utils.fun;

import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
/**
 *
 * @author Sean Phillips
 */

public class FlareSprite {
    private ImageView view;
    private double scale;
    private double position;
    private double baseOpacity;
    private double opacity;
    private boolean centered;
    private String label; 
    private ColorAdjust colorAdjust = new ColorAdjust();
    
    public FlareSprite(Image image, double scale, double position, double baseOpacity, boolean centered) {
        this(image, scale, position, baseOpacity, centered, null);
    }
    
    public FlareSprite(Image image, double scale, double position, double baseOpacity, boolean centered, String label) {
        this.view = new ImageView(image);
        this.scale = scale;
        this.position = position;
        this.baseOpacity = baseOpacity;
        this.opacity = baseOpacity;
        this.centered = centered;
        this.label = (label != null) 
            ? label 
            : String.format("Flare pos=%.2f scale=%.2f", position, scale);

        view.setPreserveRatio(true);
        view.setOpacity(baseOpacity);
        view.setMouseTransparent(true);
        view.setScaleX(scale);
        view.setScaleY(scale);
        view.setEffect(colorAdjust); 
    }
    public void update(double sunX, double sunY, double centerX, double centerY) {
        double dx = centerX - sunX;
        double dy = centerY - sunY;

        double px = sunX + dx * position;
        double py = sunY + dy * position;

        // Original (unscaled) image dimensions
        double iw = view.getImage().getWidth();
        double ih = view.getImage().getHeight();

        // Centering offset should be in *unscaled* layout coordinates
        double offsetX = centered ? iw / 2.0 : 0;
        double offsetY = centered ? ih / 2.0 : 0;

        view.setX(px - offsetX);
        view.setY(py - offsetY);

        view.setScaleX(scale);
        view.setScaleY(scale);
    }
    /**
     * Animate sprite based on position and time phase.
     * Can be overridden for dynamic effects like pulsing, color-shift, or shape-mod.
     */
    public void animate(double screenX, double screenY, double timePhase) {
        // Default: no-op
    }    
    // Use HSV-style color adjustment (hue [-1,1], saturation [-1,1], brightness [-1,1])
    public void setTintHSB(double hue, double saturation, double brightness) {
        colorAdjust.setHue(hue);             // -1.0 to 1.0
        colorAdjust.setSaturation(saturation); // -1.0 to 1.0
        colorAdjust.setBrightness(brightness); // -1.0 to 1.0
    }
    public boolean isVisible() {
        return view.isVisible();
    }
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }
    public ImageView getView() {
        return view;
    }
    public String getLabel() { return label; }
    
    /**
     * @return the baseOpacity
     */
    public double getBaseOpacity() {
        return baseOpacity;
    }
    public void setBaseOpacity(double baseOpacity) {
        this.baseOpacity = baseOpacity;
    }
    
    /**
     * @return the opacity
     */
    public double getOpacity() {
        return opacity;
    }

    /**
     * @param opacity the opacity to set
     */
    public void setOpacity(double opacity) {
        this.opacity = opacity;
        view.setOpacity(opacity);
    }
    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
        update(view.getX(), view.getY(), view.getX(), view.getY()); // trigger redraw
    }
}