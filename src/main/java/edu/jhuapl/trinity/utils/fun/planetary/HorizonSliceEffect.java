package edu.jhuapl.trinity.utils.fun.planetary;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author Sean Phillips
 */
public class HorizonSliceEffect implements PlanetaryEffect {
    private final int bands;
    private final Color darkColor;

    public HorizonSliceEffect(int bands, Color darkColor) {
        this.bands = bands;
        this.darkColor = darkColor;
    }

    @Override
    public void applyTo(Group group, double width, double height) {
        Group slices = new Group();
        double sliceHeight = height / bands;
        for (int i = 0; i < bands; i++) {
            Rectangle r = new Rectangle(0, i * sliceHeight, width, sliceHeight / 2);
            r.setFill(i % 2 == 0 ? Color.BLACK : darkColor);
            slices.getChildren().add(r);
        }
        group.getChildren().add(slices);
    }
}

