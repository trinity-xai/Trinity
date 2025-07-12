package edu.jhuapl.trinity.utils.fun.solar;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Group;
import javafx.scene.image.ImageView;

/**
 *
 * @author Sean Phillips
 */
public class LensFlareGroup extends Group {

    private final List<FlareSprite> flares = new ArrayList<>();
    //dumb but safe presets
    private Double sunX = 200.0;
    private Double sunY = 200.0; 
    private Double centerX = 200.0;
    private Double centerY = 200.0;
    private Double flareAlpha = 1.0;
    private Double occlusionFactor = 1.0;
    
    public LensFlareGroup() {
        flares.addAll(FlarePresets.createDefault());

        for (FlareSprite fs : flares) {
            getChildren().add(fs.getView());
        }
    }

    private void addFlare(FlareSprite sprite) {
        flares.add(sprite);
    }
    public List<FlareSprite> getFlares() {
        return flares;
    }
    public void setFlares(List<FlareSprite> newFlares) {
        // Clear current flares
        getChildren().removeIf(n -> n instanceof ImageView);
        flares.clear();

        // Add new ones
        for (FlareSprite sprite : newFlares) {
            flares.add(sprite);
            getChildren().add(sprite.getView());
        }
    }    
    public void updateOpacity(double flareAlpha, double occlusionFactor) {
        this.flareAlpha = flareAlpha;
        this.occlusionFactor = occlusionFactor;
        for (FlareSprite flare : flares) {
            double baseAlpha = flare.getBaseOpacity(); // or constant per sprite
            flare.setOpacity(baseAlpha * flareAlpha * occlusionFactor);            
        }        
    } 
    //whatever the previously received values are, just use them. Basically a redraw
    public void update() {
        if(null != sunX && null != sunY && null != centerX && null != centerY)
            for (FlareSprite flare : flares) {
                flare.update(sunX, sunY, centerX, centerY);
            }
        if(null != flareAlpha && null != occlusionFactor)
            updateOpacity(flareAlpha, occlusionFactor);
    }
    public void update(double sunX, double sunY, double centerX, double centerY) {
        this.sunX = sunX;
        this.sunY = sunY;
        this.centerX = centerX;
        this.centerY = centerY;
        for (FlareSprite flare : flares) {
            flare.update(sunX, sunY, centerX, centerY);
        }
    }
}