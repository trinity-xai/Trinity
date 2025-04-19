package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.FeatureLayer;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/**
 * @author Sean Phillips
 */
public class FeatureLayerListItem extends HBox {
    public static double PREF_LABEL_WIDTH = 100;
    private CheckBox visibleCheckBox;
    private ColorPicker colorPicker;
    private Label label;
    private FeatureLayer featureLayer;

    public FeatureLayerListItem(FeatureLayer featureLayer) {
        this.featureLayer = featureLayer;
        label = new Label("Index: " + featureLayer.getIndex());
        label.setPrefWidth(PREF_LABEL_WIDTH);
        colorPicker = new ColorPicker(featureLayer.getColor());
        visibleCheckBox = new CheckBox("Visible");
        visibleCheckBox.setSelected(true);

        getChildren().addAll(label, colorPicker, visibleCheckBox);
        setSpacing(20);
        colorPicker.valueProperty().addListener(cl -> {
            if (null != colorPicker.getScene()) {
                featureLayer.setColor(colorPicker.getValue());
                FeatureLayer.updateFeatureLayer(featureLayer.getIndex(), featureLayer);
                getScene().getRoot().fireEvent(new HyperspaceEvent(
                    HyperspaceEvent.UPDATED_FEATURE_LAYER, featureLayer));
            }
        });
        visibleCheckBox.selectedProperty().addListener(cl -> {
            if (null != visibleCheckBox.getScene()) {
                featureLayer.setVisible(visibleCheckBox.isSelected());
                FeatureLayer.updateFeatureLayer(featureLayer.getIndex(), featureLayer);
                getScene().getRoot().fireEvent(new HyperspaceEvent(
                    HyperspaceEvent.UPDATED_FEATURE_LAYER, featureLayer));
            }
        });
    }

    public Color getColor() {
        return colorPicker.getValue();
    }

    public void setColor(Color color) {
        colorPicker.setValue(color);
    }
}
