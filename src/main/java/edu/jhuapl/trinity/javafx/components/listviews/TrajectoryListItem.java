/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * @author Sean Phillips
 */
public class TrajectoryListItem extends HBox {
    private CheckBox visibleCheckBox;
    private TextField labelTextField;
    private ColorPicker colorPicker;
    private Trajectory trajectory;
    private Label stateCountLabel;
    public boolean reactive = true;

    public TrajectoryListItem(Trajectory trajectory) {
        this.trajectory = trajectory;
        visibleCheckBox = new CheckBox("Visible");
        visibleCheckBox.setSelected(trajectory.getVisible());
        labelTextField = new TextField(trajectory.getLabel());
        labelTextField.setPrefWidth(225);
        colorPicker = new ColorPicker(trajectory.getColor());
        colorPicker.setPrefWidth(175);
        String stateCountString = "States: ";
        if (null != trajectory.totalStates)
            stateCountString += trajectory.totalStates;
        stateCountLabel = new Label(stateCountString);
        VBox vbox = new VBox(2, labelTextField,
            new HBox(15, visibleCheckBox, stateCountLabel));
        getChildren().addAll(vbox, colorPicker);
        setSpacing(5);
        colorPicker.valueProperty().addListener(cl -> {
            if (null != colorPicker.getScene()) {
                trajectory.setColor(colorPicker.getValue());
                if (reactive) {
                    Trajectory.updateTrajectory(this.trajectory.getLabel(), this.trajectory);
                    //Let application know this trajectory's visibility has changed
                    getScene().getRoot().fireEvent(new TrajectoryEvent(
                        TrajectoryEvent.TRAJECTORY_COLOR_CHANGED, this.trajectory));
                }
            }
        });
        visibleCheckBox.selectedProperty().addListener(cl -> {
            if (null != visibleCheckBox.getScene()) {
                trajectory.setVisible(visibleCheckBox.isSelected());
                if (reactive) {
                    Trajectory.updateTrajectory(this.trajectory.getLabel(), this.trajectory);
                    //Let application know this trajectory's visibility has changed
                    getScene().getRoot().fireEvent(new TrajectoryEvent(
                        TrajectoryEvent.TRAJECTORY_VISIBILITY_CHANGED, this.trajectory));
                }
            }
        });

        setOnMouseClicked(e -> {
            //Let application know this distance object has been selected
            getScene().getRoot().fireEvent(new TrajectoryEvent(
                TrajectoryEvent.TRAJECTORY_OBJECT_SELECTED, this.trajectory));
        });
    }

    public void setDataVisible(boolean visible) {
        visibleCheckBox.setSelected(visible);
    }

}
