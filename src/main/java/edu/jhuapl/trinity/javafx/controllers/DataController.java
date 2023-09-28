package edu.jhuapl.trinity.javafx.controllers;

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

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.TimelineEvent;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
import edu.jhuapl.trinity.javafx.events.ZeroMQEvent;
import edu.jhuapl.trinity.messages.ZeroMQFeedManager;
import edu.jhuapl.trinity.messages.ZeroMQSubscriberConfig;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * FXML Controller class
 *
 * @author Sean Phillips
 */
public class DataController implements Initializable {

    @FXML
    private BorderPane majorPane;
    @FXML
    private TabPane tabPane;
    //// ZeroMQ stuff
    @FXML
    private TextField hostTextField;
    @FXML
    private TextField portTextField;
    @FXML
    private TextField updateRateTextField;
    @FXML
    private ToggleButton socketToggleButton;
    @FXML
    private ToggleButton feedToggleButton;
    @FXML
    private ProgressIndicator autoProgressIndicator;
    @FXML
    private RadioButton pubsubRadioButton;
    @FXML
    private RadioButton pushpullRadioButton;
    //// Imagery /////
    @FXML
    private TextField imageryBasePathTextField;
    @FXML
    private CheckBox showTimelineCheckBox;
    @FXML
    private CheckBox showStateTrajectoryCheckBox;
    @FXML
    private CheckBox showLeadCalloutCheckBox;

    @FXML
    private Spinner trajectorySizeSpinner;


    ToggleGroup toggleGroup;
    Scene scene;
    File lastDirectory = null;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        scene = App.getAppScene();
        feedToggleButton.disableProperty().bind(socketToggleButton.selectedProperty().not());
        autoProgressIndicator.visibleProperty().bind(feedToggleButton.selectedProperty());
        toggleGroup = new ToggleGroup();
        pubsubRadioButton.setToggleGroup(toggleGroup);
        pushpullRadioButton.setToggleGroup(toggleGroup);
        updateRateTextField.setText(String.valueOf(ZeroMQFeedManager.DEFAULT_TIMER_RATE_MS));
        imageryBasePathTextField.setText("imagery/");
        showTimelineCheckBox.selectedProperty().addListener(cl -> {
            showTimelineCheckBox.getScene().getRoot().fireEvent(
                new TimelineEvent(TimelineEvent.TIMELINE_SET_VISIBLE,
                    showTimelineCheckBox.isSelected()));
        });
        showLeadCalloutCheckBox.selectedProperty().addListener(cl -> {
            showLeadCalloutCheckBox.getScene().getRoot().fireEvent(
                new TrajectoryEvent(TrajectoryEvent.TIMELINE_SHOW_CALLOUT,
                    showLeadCalloutCheckBox.isSelected()));
        });
        showStateTrajectoryCheckBox.selectedProperty().addListener(cl -> {
            showStateTrajectoryCheckBox.getScene().getRoot().fireEvent(
                new TrajectoryEvent(TrajectoryEvent.TIMELINE_SHOW_TRAJECTORY,
                    showStateTrajectoryCheckBox.isSelected()));
        });

        trajectorySizeSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 5, 1));
        trajectorySizeSpinner.setEditable(true);
        //whenever the spinner value is changed...
        trajectorySizeSpinner.valueProperty().addListener(e -> {
            scene.getRoot().fireEvent(
                new TrajectoryEvent(TrajectoryEvent.TRAJECTORY_TAIL_SIZE,
                    (int) trajectorySizeSpinner.getValue()));
        });
        trajectorySizeSpinner.disableProperty().bind(
            showStateTrajectoryCheckBox.selectedProperty().not());

    }

    @FXML
    public void showTrajectoryTracker() {
        scene.getRoot().fireEvent(
            new TrajectoryEvent(TrajectoryEvent.SHOW_TRAJECTORY_TRACKER));
    }

    @FXML
    public void browseBasePath() {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Browse for base path...");
        if (null != lastDirectory) {
            directoryChooser.setInitialDirectory(lastDirectory);
        }

        File file = directoryChooser.showDialog(null);
        if (file != null) {
            lastDirectory = file;
            imageryBasePathTextField.setText(lastDirectory.getAbsolutePath()
                + File.pathSeparator);
        }
    }

    @FXML
    public void applyBasePath() {
        imageryBasePathTextField.getScene().getRoot().fireEvent(
            new ApplicationEvent(ApplicationEvent.SET_IMAGERY_BASEPATH,
                imageryBasePathTextField.getText()));
    }

    private ZeroMQSubscriberConfig getSubscriberConfig() {
        String hostURL = "tcp://" + hostTextField.getText().trim() + ":" +
            Integer.valueOf(portTextField.getText());
        ZeroMQSubscriberConfig config = new ZeroMQSubscriberConfig(
            "ZeroMQ Subscriber", "Testing ZeroMQFeedManager.",
            hostURL, "ALL", "SomeIDValue", Integer.valueOf(updateRateTextField.getText()));
        if (pushpullRadioButton.isSelected())
            config.connection = ZeroMQSubscriberConfig.CONNECTION.PULL;
        else
            config.connection = ZeroMQSubscriberConfig.CONNECTION.SUBSCRIBER;
        return config;
    }

    @FXML
    public void toggleSocket() {
        ZeroMQSubscriberConfig config = getSubscriberConfig();
        //if the button is already pressed that means we have to stop processing
        if (socketToggleButton.isSelected()) {
            socketToggleButton.setText("Disconnect");
            scene.getRoot().fireEvent(
                new ZeroMQEvent(ZeroMQEvent.ZEROMQ_ESTABLISH_CONNECTION, config));
        } else {
            //first stop processing messages
            scene.getRoot().fireEvent(
                new ZeroMQEvent(ZeroMQEvent.ZEROMQ_STOP_PROCESSING, config));
            //update the gui button so they can't enable a feed on a disconnected socket
            feedToggleButton.setText("Start");
            feedToggleButton.setSelected(false);
            //actually kill the connection
            socketToggleButton.setText("Bind");
            scene.getRoot().fireEvent(
                new ZeroMQEvent(ZeroMQEvent.ZEROMQ_TERMINATE_CONNECTION, config));
        }
    }

    @FXML
    public void toggleFeed() {
        ZeroMQSubscriberConfig config = getSubscriberConfig();
        //if the button is already pressed that means we have to stop processing
        if (feedToggleButton.isSelected()) {
            feedToggleButton.setText("Stop");
            scene.getRoot().fireEvent(
                new ZeroMQEvent(ZeroMQEvent.ZEROMQ_START_PROCESSING, config));
        } else {
            feedToggleButton.setText("Start");
            scene.getRoot().fireEvent(
                new ZeroMQEvent(ZeroMQEvent.ZEROMQ_STOP_PROCESSING, config));
        }
    }
}
