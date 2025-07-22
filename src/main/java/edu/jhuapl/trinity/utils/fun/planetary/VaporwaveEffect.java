package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.List;

/**
 * @author Sean Phillips
 */
public class VaporwaveEffect implements PlanetaryEffect {

    private final Group group = new Group();
    private final List<Color> radialGradientColors;
    private final int noiseRingCount;
    private final Color noiseRingColor;
    private final double noiseRingOpacity;
    private final boolean includeScanlines;

    public VaporwaveEffect(List<Color> radialGradientColors,
                           int noiseRingCount,
                           Color noiseRingColor,
                           double noiseRingOpacity,
                           boolean includeScanlines) {
        this.radialGradientColors = radialGradientColors;
        this.noiseRingCount = noiseRingCount;
        this.noiseRingColor = noiseRingColor;
        this.noiseRingOpacity = noiseRingOpacity;
        this.includeScanlines = includeScanlines;
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        group.getChildren().clear();
        double radius = disc.getRadius();
        double centerX = radius;
        double centerY = radius;

        // Create dreamy radial gradient fill (soft pastel blend)
        Stop[] stops = new Stop[radialGradientColors.size()];
        for (int i = 0; i < radialGradientColors.size(); i++) {
            stops[i] = new Stop(i / (double) (radialGradientColors.size() - 1), radialGradientColors.get(i));
        }
        Circle gradientFill = new Circle(centerX, centerY, radius);
        gradientFill.setFill(new RadialGradient(0, 0, centerX, centerY, radius,
            false, CycleMethod.NO_CYCLE, stops));
        group.getChildren().add(gradientFill);

        // Add concentric semi-transparent noise rings
        for (int i = 0; i < noiseRingCount; i++) {
            double ringRadius = radius * ((i + 1) / (double) (noiseRingCount + 1));
            Circle ring = new Circle(centerX, centerY, ringRadius);
            ring.setFill(Color.TRANSPARENT);
            ring.setStroke(noiseRingColor);
            ring.setOpacity(noiseRingOpacity * (Math.random() * 0.5 + 0.5)); // slight randomness
            ring.getStrokeDashArray().addAll(2.0, 4.0);
            group.getChildren().add(ring);
        }

        // Optional horizontal scanlines (subtle VHS effect)
        if (includeScanlines) {
            int lineCount = 40;
            for (int i = 0; i < lineCount; i++) {
                double y = i * (radius * 2 / lineCount);
                Line line = new Line(centerX - radius, y, centerX + radius, y);
                line.setStroke(Color.web("#FFB6C1", noiseRingOpacity)); // pastel pink, very faint
                group.getChildren().add(line);
            }
        }

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
