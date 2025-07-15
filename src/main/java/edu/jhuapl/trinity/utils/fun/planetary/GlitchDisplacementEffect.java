package edu.jhuapl.trinity.utils.fun.planetary;

import edu.jhuapl.trinity.utils.fun.Glitch;
import javafx.scene.Group;
import javafx.scene.Node;

/**
 *
 * @author Sean Phillips
 */
public class GlitchDisplacementEffect implements PlanetaryEffect {

    private Group group = new Group();
    private final double intensity;
    private final int rows;
    private double glitchFrequency;
    private double glitchTime;
    private double bandThicknessRatio;
    private Glitch glitch;
    PlanetaryDisc discWorld;

    public GlitchDisplacementEffect(double intensity, int rows, 
        double glitchTime, double glitchFrequencyMs, double bandThicknessRatio) {
        this.intensity = intensity;
        this.rows = rows;
        this.glitchFrequency = glitchFrequencyMs;
        this.glitchTime = glitchTime;
        this.bandThicknessRatio = bandThicknessRatio;
        group.setMouseTransparent(true);
        group.visibleProperty().addListener(e -> {
            if(null != glitch) {
                if(group.isVisible()) {
                    //if its not running we need to start it
                    if(!glitch.isRunning()) {
                        glitch.start();
                    } 
                } else {
                    //if it running now, stop it
                    if(glitch.isRunning()) {
                        glitch.stop();
                        glitch.resetFloatMap();
                    }
                }
                    
            }
        });
        group.parentProperty().addListener(il -> {
            if(null == group.getParent()) {
                glitch.stop();
                glitch.resetFloatMap();
            }
        });
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        group.getChildren().clear();
        
        discWorld = disc;
        int diameter = (int) (disc.getRadius() * 2);
        glitch = new Glitch(
            disc,           // target node
            diameter, diameter,       // width & height
            intensity,            // intensity
            rows,              // max bands per burst
            glitchFrequency,           // glitchFrequency
            glitchTime,            // glitchTime
            bandThicknessRatio           // base band thickness ~7% of height
        );
        glitch.start();
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
