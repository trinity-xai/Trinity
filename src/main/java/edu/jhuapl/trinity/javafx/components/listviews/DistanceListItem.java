/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.listviews;

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
        String distanceLabel = distance.getMetric() + ":" + pc.toString(distance.getValue());
        distanceValueLabel = new Label(distanceLabel);
        getChildren().addAll(visibleCheckBox, label, distanceValueLabel);
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
