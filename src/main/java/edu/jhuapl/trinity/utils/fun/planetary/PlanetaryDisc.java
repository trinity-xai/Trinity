package edu.jhuapl.trinity.utils.fun.planetary;

import edu.jhuapl.trinity.utils.fun.planetary.PlanetaryEffectFactory.PlanetStyle;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.RadialGradient;

/**
 *
 * @author Sean Phillips
 */
public class PlanetaryDisc extends Group {

    private double radius;
    private PlanetStyle planetStyle;
    private Circle planetCircle;

    private Group scatteringGroup = new Group();
    private List<Circle> scatteringRings = new ArrayList<>();

    private Circle shadowOverlay;
    private double occlusionFactor = 1.0;
    private final List<PlanetaryEffect> effects = new ArrayList<>();

    private boolean scatteringEnabled = true;
    private boolean shadowEnabled = true;

    private Color scatteringColor = Color.LIGHTBLUE;
    private double scatteringBlurRadius = 8.0;
    private double scatteringShadowStrength = 0.3;
    private double shadowIntensity = 0.25;

    public PlanetaryDisc(double radius, PlanetStyle style) {
        this.radius = radius;
        this.planetStyle = style;

        // Core planet shape
        planetCircle = new Circle(radius, radius, radius);
        planetCircle.setFill(getStyleFill(style));
        planetCircle.getProperties().put("occluderShape", true);
        planetCircle.setMouseTransparent(true);
        getChildren().add(planetCircle);

        // Atmospheric scattering rings
        buildScatteringRings();
        getChildren().add(scatteringGroup);

        // Soft shadow overlay
        shadowOverlay = new Circle(radius, radius, radius * 1.25);
        shadowOverlay.setFill(Color.BLACK);
        shadowOverlay.setOpacity(shadowIntensity);
        shadowOverlay.setMouseTransparent(true);
        shadowOverlay.setVisible(shadowEnabled);
        getChildren().add(shadowOverlay);

        setPlanetStyle(style);        
        
        setMouseTransparent(true);
    }

    public void setPlanetStyle(PlanetStyle style) {
        this.planetStyle = style;
        planetCircle.setFill(getStyleFill(style));  

        // Remove old effect nodes
        getChildren().removeIf(node -> node.getUserData() instanceof PlanetaryEffect);
        effects.clear();

        for (PlanetaryEffect effect : PlanetaryEffectFactory.createEffectsFor(style)) {
            effect.attachTo(this);
            Node node = effect.getNode();
            if (node != null) {
                node.setUserData(effect);
                getChildren().add(node);
            }
            effects.add(effect);
        }

        updateScattering(occlusionFactor);  // Optional
    }

    public void updateOcclusionFactor(double computedOcclusion) {
        occlusionFactor = Math.max(0, Math.min(1, computedOcclusion));
        for (PlanetaryEffect effect : effects) {
            effect.update(occlusionFactor);
        }
    }

    public void updateScattering(double visibilityFactor) {
        visibilityFactor = Math.max(0, Math.min(1, visibilityFactor));
        double adjustedStrength = scatteringShadowStrength * visibilityFactor;

        for (int i = 0; i < scatteringRings.size(); i++) {
            Circle ring = scatteringRings.get(i);
            double baseOpacity = 1.0 / (i + 1);
            double finalOpacity = baseOpacity * adjustedStrength;
            Paint oldFill = ring.getFill();

            if (oldFill instanceof RadialGradient rg) {
                List<Stop> stops = List.of(
                        new Stop(0, scatteringColor.deriveColor(0, 1, 1, finalOpacity)),
                        new Stop(1, Color.TRANSPARENT)
                );
                RadialGradient newFill = new RadialGradient(
                        rg.getFocusAngle(), rg.getFocusDistance(),
                        rg.getCenterX(), rg.getCenterY(),
                        rg.getRadius(), rg.isProportional(),
                        rg.getCycleMethod(), stops
                );
                ring.setFill(newFill);
            }
        }

        setShadowIntensity(shadowIntensity * visibilityFactor);
    }

    private void buildScatteringRings() {
        scatteringGroup.getChildren().clear();
        scatteringRings.clear();

        int ringCount = 3;
        for (int i = 1; i <= ringCount; i++) {
            double ringRadius = radius + (i * radius * 0.15);
            double opacity = (1.0 / i) * scatteringShadowStrength;

            Circle ring = new Circle(radius, radius, ringRadius);
            ring.setFill(new RadialGradient(
                    0, 0, radius, radius, ringRadius,
                    false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, scatteringColor.deriveColor(0, 1, 1, opacity)),
                    new Stop(1.0, Color.TRANSPARENT)
            ));
            ring.setEffect(new GaussianBlur(scatteringBlurRadius));
            ring.setMouseTransparent(true);
            scatteringRings.add(ring);
        }

        scatteringGroup.getChildren().addAll(scatteringRings);
        scatteringGroup.setVisible(scatteringEnabled);
    }

    private void updateScatteringVisuals() {
        buildScatteringRings();
    }

    public void setScatteringEnabled(boolean enabled) {
        this.scatteringEnabled = enabled;
        scatteringGroup.setVisible(enabled);
    }

    public void setScatteringColor(Color color) {
        this.scatteringColor = color;
        updateScatteringVisuals();
    }

    public void setScatteringBlurRadius(double blurRadius) {
        this.scatteringBlurRadius = blurRadius;
        updateScatteringVisuals();
    }

    public void setScatteringShadowStrength(double strength) {
        this.scatteringShadowStrength = strength;
        updateScatteringVisuals();
    }

    public void setShadowEnabled(boolean enabled) {
        this.shadowEnabled = enabled;
        shadowOverlay.setVisible(enabled);
    }

    public void setShadowIntensity(double intensity) {
        this.shadowIntensity = intensity;
        shadowOverlay.setOpacity(intensity);
    }

    public void setDebugVisible(boolean debug) {
        if (debug) {
            planetCircle.setStroke(Color.CYAN);
            planetCircle.setStrokeWidth(1);
            planetCircle.setFill(
                    planetCircle.getFill() instanceof Color color
                    ? color.deriveColor(0, 1, 1, 0.25)
                    : planetCircle.getFill()
            );
        } else {
            planetCircle.setStroke(null);
        }
    }

    public Node getOccluderShape() {
        return planetCircle;
    }

    public Circle getPlanetCircle() {
        return planetCircle;
    }

    public double getRadius() {
        return radius;
    }

    public double getOcclusionFactor() {
        return occlusionFactor;
    }

    public PlanetStyle getPlanetStyle() {
        return planetStyle;
    }

    private Paint getStyleFill(PlanetStyle style) {
        return switch (style) {
            case OUTRUN ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FF003C")),
                new Stop(1, Color.web("#FF7F50")));
            case VAPORWAVE ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffccff")),
                new Stop(1, Color.web("#66ccff")));
            case SCIFI ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#00FFFF")),
                new Stop(1, Color.web("#003366")));
            case SPACE_HORROR ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#220000")),
                new Stop(1, Color.web("#550000")));
            case RETROWAVE ->
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FF00CC")),
                new Stop(1, Color.web("#6600FF")));
        };
    }
}
