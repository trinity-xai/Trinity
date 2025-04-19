package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.FactorLabel;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * @author Sean Phillips
 */
public class FactorLabelListItem extends HBox {
    public static double PREF_LABEL_WIDTH = 300;
    private String labelString;
    private CheckBox visibleCheckBox;
    private CheckBox ellipsoidVisibleCheckBox;
    private ColorPicker colorPicker;
    private Label label;
    private FactorLabel factorLabel;
    public boolean reactive = true;

    public FactorLabelListItem(FactorLabel factorLabel) {
        this.factorLabel = factorLabel;
        this.labelString = factorLabel.getLabel();
        label = new Label(labelString);
        label.setPrefWidth(PREF_LABEL_WIDTH);
        colorPicker = new ColorPicker(factorLabel.getColor());
        visibleCheckBox = new CheckBox("Visible");
        visibleCheckBox.setSelected(true);
        ellipsoidVisibleCheckBox = new CheckBox("Ellipsoid Visible");
        ellipsoidVisibleCheckBox.setSelected(true);
        HBox checkBoxHBox = new HBox(5, visibleCheckBox, ellipsoidVisibleCheckBox);
        VBox labelVBox = new VBox(2, label, checkBoxHBox);
        getChildren().addAll(labelVBox, colorPicker);
        setSpacing(20);
        colorPicker.valueProperty().addListener(cl -> {
            if (null != colorPicker.getScene()) {
                factorLabel.setColor(colorPicker.getValue());
                if (reactive)
                    FactorLabel.updateFactorLabel(factorLabel.getLabel(), factorLabel);
            }
        });
        visibleCheckBox.selectedProperty().addListener(cl -> {
            if (null != visibleCheckBox.getScene()) {
                factorLabel.setVisible(visibleCheckBox.isSelected());
                if (reactive)
                    FactorLabel.updateFactorLabel(factorLabel.getLabel(), factorLabel);
            }
        });
        ellipsoidVisibleCheckBox.selectedProperty().addListener(cl -> {
            if (null != ellipsoidVisibleCheckBox.getScene()) {
                factorLabel.setEllipsoidsVisible(ellipsoidVisibleCheckBox.isSelected());
                if (reactive)
                    FactorLabel.updateFactorLabel(factorLabel.getLabel(), factorLabel);
            }
        });
    }

    public boolean getDataVisible() {
        return visibleCheckBox.isSelected();
    }

    public void setDataVisible(boolean visible) {
        visibleCheckBox.setSelected(visible);
    }

    public boolean getEllipsoidVisible() {
        return ellipsoidVisibleCheckBox.isSelected();
    }

    public void setEllipsoidVisible(boolean visible) {
        ellipsoidVisibleCheckBox.setSelected(visible);
    }

    public Color getColor() {
        return colorPicker.getValue();
    }

    public void setColor(Color color) {
        colorPicker.setValue(color);
    }
}
