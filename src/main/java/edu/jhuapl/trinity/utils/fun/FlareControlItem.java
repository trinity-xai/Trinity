package edu.jhuapl.trinity.utils.fun;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 *
 * @author Sean Phillips
 */
public class FlareControlItem extends HBox {
    private final FlareSprite flare;

    public FlareControlItem(FlareSprite flare) {
        this.flare = flare;
        setSpacing(6);
        setPadding(new Insets(4));
        setAlignment(Pos.CENTER_LEFT);
        // Visible checkbox
        CheckBox visibleBox = new CheckBox(flare.getLabel());
        visibleBox.setSelected(flare.isVisible());
        visibleBox.selectedProperty().addListener((obs, old, val) -> flare.setVisible(val));
        visibleBox.setPrefWidth(125);
        
        // Opacity slider
        Slider opacitySlider = new Slider(0.0, 1.0, flare.getBaseOpacity());
        opacitySlider.setPrefWidth(125);
        opacitySlider.valueProperty().addListener((obs, old, val) -> flare.setBaseOpacity(val.doubleValue()));

        // Color picker (tint)
        ColorPicker colorPicker = new ColorPicker(Color.WHITE);
        colorPicker.setPrefWidth(150);        
        colorPicker.setOnAction(e -> {
            Color c = colorPicker.getValue();
            double hue = (c.getHue() - 180) / 180.0;           // Convert 0–360° to -1.0–1.0
            double saturation = (c.getSaturation() * 2) - 1.0; // Convert 0–1.0 to -1.0–1.0
            double brightness = (c.getBrightness() * 2) - 1.0; // Convert 0–1.0 to -1.0–1.0
            flare.setTintHSB(hue, saturation, brightness);
        });

        getChildren().addAll(visibleBox, 
            new VBox(5, new Label("Base Opacity"), opacitySlider), 
            colorPicker);
    }
}