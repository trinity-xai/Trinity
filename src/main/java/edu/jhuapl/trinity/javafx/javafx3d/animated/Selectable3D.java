package edu.jhuapl.trinity.javafx.javafx3d.animated;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * just a cheesy way to have a instanceof selectable Shape3D.
 *
 * @author Sean Phillips
 */
public interface Selectable3D {
    public static PhongMaterial DEFAULT_SELECTED_MATERIAL = new PhongMaterial(
        Color.ALICEBLUE.deriveColor(1, 1, 1, 0.5));
    public PhongMaterial previousMaterial = null;

    public void select();

    public void unselect();

    public boolean isSelected();

}
