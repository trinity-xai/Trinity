package edu.jhuapl.trinity.utils.fun.planetary;

import edu.jhuapl.trinity.utils.fun.planetary.PlanetaryEffectFactory.PlanetStyle;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.Group;

import javafx.scene.Node;

/**
 *
 * @author Sean Phillips
 */
public class PlanetaryDisc extends Group {

    private final double radius;
    private final PlanetStyle style;
    private final Circle planetCircle;

    public PlanetaryDisc(double radius, PlanetStyle style) {
        this.radius = radius;
        this.style = style;

        // Create planet shape
        planetCircle = new Circle(radius, radius, radius);
        planetCircle.setFill(getStyleFill(style));
        planetCircle.getProperties().put("occluderShape", true);
        planetCircle.setMouseTransparent(true);  // Important for occlusion math
        getChildren().add(planetCircle);
        setMouseTransparent(true);
    }

    public void setDebugVisible(boolean debug) {
        if (debug) {
            planetCircle.setStroke(Color.CYAN);
            planetCircle.setStrokeWidth(1);
            planetCircle.setFill(planetCircle.getFill() instanceof Color ? 
                ((Color) planetCircle.getFill()).deriveColor(0, 1, 1, 0.25) :
                planetCircle.getFill());
        } else {
            planetCircle.setStroke(null);
        }
    }

    public Node getOccluderShape() {
        return planetCircle;
    }

    private Paint getStyleFill(PlanetStyle style) {
        return switch (style) {
            case OUTRUN -> new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#FF003C")),
                    new Stop(1, Color.web("#FF7F50")));
            case VAPORWAVE -> new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#ffccff")),
                    new Stop(1, Color.web("#66ccff")));
            case SCIFI -> new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#00FFFF")),
                    new Stop(1, Color.web("#003366")));
            case SPACE_HORROR -> new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#220000")),
                    new Stop(1, Color.web("#550000")));
            case RETROWAVE -> new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#FF00CC")),
                    new Stop(1, Color.web("#6600FF")));
        };
    }
}
