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
        visibleCheckBox.setSelected(true);
        labelTextField = new TextField(trajectory.getLabel());
        labelTextField.setPrefWidth(225);
        colorPicker = new ColorPicker(trajectory.getColor());
        colorPicker.setPrefWidth(175);
        String stateCountString = "States: ";
        if(null != trajectory.totalStates)
            stateCountString += trajectory.totalStates;
        stateCountLabel = new Label(stateCountString);
        VBox vbox = new VBox(2, labelTextField,
        new HBox(15, visibleCheckBox, stateCountLabel));
        getChildren().addAll(vbox, colorPicker);
        setSpacing(5);
        visibleCheckBox.selectedProperty().addListener(cl -> {
            if (null != visibleCheckBox.getScene()) {
                trajectory.setVisible(visibleCheckBox.isSelected());
                if (reactive)
                    Trajectory.updateTrajectory(this.trajectory.getLabel(), this.trajectory);
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
