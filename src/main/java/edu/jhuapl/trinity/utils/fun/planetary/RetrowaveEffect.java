package edu.jhuapl.trinity.utils.fun.planetary;

import java.util.List;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author Sean Phillips
 */
public class RetrowaveEffect implements PlanetaryEffect {

    private final Group group = new Group();
    private final List<Color> topGradientColors;
    private final List<Color> bottomBandColors;
    private final double bandGap;
    private final double bandHeight;
    private final boolean useTopGradient;

    public RetrowaveEffect(List<Color> topGradientColors,
                           List<Color> bottomBandColors,
                           double bandGap,
                           double bandHeight,
                           boolean useTopGradient) {
        this.topGradientColors = topGradientColors;
        this.bottomBandColors = bottomBandColors;
        this.bandGap = bandGap;
        this.bandHeight = bandHeight;
        this.useTopGradient = useTopGradient;
    }

    @Override
    public void attachTo(PlanetaryDisc disc) {
        group.getChildren().clear();
        double radius = disc.getRadius();
        double centerX = radius;
        double centerY = radius;

        // Draw top half as vertical gradient
        if (useTopGradient && !topGradientColors.isEmpty()) {
            Stop[] stops = new Stop[topGradientColors.size()];
            for (int i = 0; i < topGradientColors.size(); i++) {
                stops[i] = new Stop(i / (double)(topGradientColors.size() - 1), topGradientColors.get(i));
            }

            Rectangle topHalf = new Rectangle(centerX - radius, 0, radius * 2, centerY);
            topHalf.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops));
            group.getChildren().add(topHalf);
        }

        // Draw bottom half using horizontal color bands
        double currentY = centerY;
        int bandIndex = 0;
        while (currentY < centerY + radius && bandIndex < bottomBandColors.size()) {
            Rectangle band = new Rectangle(centerX - radius, currentY, radius * 2, bandHeight);
            band.setFill(bottomBandColors.get(bandIndex));
            group.getChildren().add(band);

            currentY += bandHeight + bandGap;
            bandIndex++;
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

