package edu.jhuapl.trinity.javafx.components;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import edu.jhuapl.trinity.data.Distance;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * @author Sean Phillips
 */
public class DistanceListItem extends HBox {
    public static double PREF_LABEL_WIDTH = 300;
    private String labelString;
    private CheckBox visibleCheckBox;
    private ColorPicker colorPicker;
    private Label label;
    private Label distanceValueLabel;
    private Distance distance;
    public boolean reactive = true;

    public DistanceListItem(Distance distance) {
        this.distance = distance;
        this.labelString = distance.getLabel();
        distanceValueLabel = new Label(String.valueOf(distance.getValue()));
        distanceValueLabel.setPrefWidth(PREF_LABEL_WIDTH);
        label = new Label(labelString);
        label.setPrefWidth(PREF_LABEL_WIDTH);
        colorPicker = new ColorPicker(distance.getColor());
        visibleCheckBox = new CheckBox("Visible");
        visibleCheckBox.setSelected(true);
        HBox checkBoxHBox = new HBox(5, distanceValueLabel, visibleCheckBox);
        VBox labelVBox = new VBox(2, label, checkBoxHBox);
        getChildren().addAll(labelVBox, colorPicker);
        setSpacing(20);
        colorPicker.valueProperty().addListener(cl -> {
            if (null != colorPicker.getScene()) {
                distance.setColor(colorPicker.getValue());
                if (reactive)
                    Distance.updateDistance(distance.getLabel(), distance);
            }
        });
        visibleCheckBox.selectedProperty().addListener(cl -> {
            if (null != visibleCheckBox.getScene()) {
                distance.setVisible(visibleCheckBox.isSelected());
                if (reactive)
                    Distance.updateDistance(distance.getLabel(), distance);
            }
        });
    }

    public boolean getDataVisible() {
        return visibleCheckBox.isSelected();
    }

    public void setDataVisible(boolean visible) {
        visibleCheckBox.setSelected(visible);
    }

    public Color getColor() {
        return colorPicker.getValue();
    }

    public void setColor(Color color) {
        colorPicker.setValue(color);
    }
}
