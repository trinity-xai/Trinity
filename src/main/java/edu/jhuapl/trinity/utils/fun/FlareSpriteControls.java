package edu.jhuapl.trinity.utils.fun;

import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 *
 * @author Sean Phillips
 */
public class FlareSpriteControls extends VBox {
    
    public FlareSpriteControls(LensFlareGroup flareGroup) {
        setSpacing(6);
        setPadding(new Insets(10));
        getChildren().add(new Label("Toggle Individual Flares"));

        for (FlareSprite flare : flareGroup.getFlares()) {
            String label = flare.getLabel();
            CheckBox check = new CheckBox(label);
            check.setSelected(true); // default to visible
            check.setOnAction(e -> flare.getView().setVisible(check.isSelected()));
            getChildren().add(check);
        }
    }
}
