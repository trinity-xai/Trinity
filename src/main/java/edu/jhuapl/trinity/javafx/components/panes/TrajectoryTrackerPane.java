package edu.jhuapl.trinity.javafx.components.panes;

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
import edu.jhuapl.trinity.javafx.components.TrajectoryListItem;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class TrajectoryTrackerPane extends LitPathPane {
    BorderPane bp;
    private ListView<TrajectoryListItem> trajectoryListView;

    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public TrajectoryTrackerPane(Scene scene, Pane parent) {
        super(scene, parent, 500, 400, createContent(),
            "Trajectory Tracker", "", 300.0, 400.0);
        this.scene = scene;

        bp = (BorderPane) this.contentPane;

        Button showAll = new Button("Show All");
        showAll.setOnAction(e -> setVisibleAll(true));
        Button hideAll = new Button("Hide All");
        hideAll.setOnAction(e -> setVisibleAll(false));
        Button clearAll = new Button("Clear All");
        clearAll.setOnAction(e -> {
            trajectoryListView.getItems().clear();
            Trajectory.removeAllTrajectories();
            Trajectory.globalTrajectoryToFeatureCollectionMap.clear();
            clearAll.getScene().getRoot().fireEvent(new TrajectoryEvent(
                TrajectoryEvent.CLEAR_ALL_TRAJECTORIES));
        });

        HBox topHBox = new HBox(10, showAll, hideAll, clearAll);
        CheckBox autoUpdateCheckBox = new CheckBox("Auto Update Trajectories");
        autoUpdateCheckBox.selectedProperty().addListener(e -> {
            scene.getRoot().fireEvent(new TrajectoryEvent(
                TrajectoryEvent.AUTO_UDPATE_TRAJECTORIES, autoUpdateCheckBox.isSelected()));
        });
        Button refresh = new Button("Request Refresh");
        refresh.setOnAction(eh -> {
            scene.getRoot().fireEvent(new TrajectoryEvent(
                TrajectoryEvent.REFRESH_3D_TRAJECTORIES));
        });
        HBox top2HBox = new HBox(10, autoUpdateCheckBox, refresh);
        VBox topVBox = new VBox(10, topHBox, top2HBox);
        bp.setTop(topVBox);
        //Get a reference to any Distances already collected
        List<TrajectoryListItem> existingItems = new ArrayList<>();
        for (Trajectory t : Trajectory.getTrajectories()) {
            TrajectoryListItem item = new TrajectoryListItem(t);
            existingItems.add(item);
        }
        trajectoryListView = new ListView<>();
        //add them all in one shot
        trajectoryListView.getItems().addAll(existingItems);
        ImageView iv = ResourceUtils.loadIcon("trajectory", 300);
        VBox placeholder = new VBox(10, iv, new Label("No Trajectories Acquired"));
        placeholder.setAlignment(Pos.CENTER);
        trajectoryListView.setPlaceholder(placeholder);
        VBox trajectoryVBox = new VBox(10, new Label("Trajectories"), trajectoryListView);
        trajectoryVBox.setPrefWidth(450);
        bp.setCenter(trajectoryVBox);
        scene.addEventHandler(TrajectoryEvent.NEW_TRAJECTORY_OBJECT, e -> {
            Trajectory trajectory = (Trajectory) e.eventObject;
            TrajectoryListItem item = new TrajectoryListItem(trajectory);
            trajectoryListView.getItems().add(item);
        });

    }

    public void setVisibleAll(boolean visible) {
        trajectoryListView.getItems().forEach(item -> item.setDataVisible(visible));
    }

}
