package edu.jhuapl.trinity.javafx.components;

/*-
 * #%L
 * trinity
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
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.utils.PrecisionConverter;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * @author Sean Phillips
 */
public class DistanceListItem extends HBox {
    private String labelString;
    private CheckBox visibleCheckBox;
    private Label label;
    private Label distanceValueLabel;
    private Distance distance;
    public boolean reactive = true;

    public DistanceListItem(Distance distance) {
        this.distance = distance;
        this.labelString = distance.getLabel();
        visibleCheckBox = new CheckBox("Visible");
        visibleCheckBox.setSelected(true);
        label = new Label(labelString);
        PrecisionConverter pc = new PrecisionConverter(7);
        String distanceLabel = distance.getMetric()+":"+pc.toString(distance.getValue());
        distanceValueLabel = new Label(distanceLabel);
        getChildren().addAll(visibleCheckBox,label, distanceValueLabel);
        setSpacing(5);
        visibleCheckBox.selectedProperty().addListener(cl -> {
            if (null != visibleCheckBox.getScene()) {
                distance.setVisible(visibleCheckBox.isSelected());
                if (reactive)
                    Distance.updateDistance(distance.getLabel(), distance);
            }
        });
        setOnMouseClicked(e -> {
            //Let application know this distance object has been selected
            getScene().getRoot().fireEvent(new ManifoldEvent(
            ManifoldEvent.DISTANCE_OBJECT_SELECTED, distance));
        });
    }

    public boolean getDataVisible() {
        return visibleCheckBox.isSelected();
    }

    public void setDataVisible(boolean visible) {
        visibleCheckBox.setSelected(visible);
    }

    /**
     * @return the distance
     */
    public Distance getDistance() {
        return distance;
    }

    /**
     * @param distance the distance to set
     */
    public void setDistance(Distance distance) {
        this.distance = distance;
    }
}
