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

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class BusyIndicatorTest extends Application {
    StackPane stackPane;
    CircleProgressIndicator circleSpinner;

    ContextMenu cm = new ContextMenu();

    @Override
    public void start(Stage stage) {
        circleSpinner = new CircleProgressIndicator();
        circleSpinner.setLabelLater("Current Status message...");
        circleSpinner.defaultOpacity = 1.0;

        stackPane = new StackPane(circleSpinner);
        stackPane.setBackground(Background.EMPTY);
        BorderPane bpOilSpill = new BorderPane(stackPane);
        bpOilSpill.setBackground(Background.EMPTY);

        CheckMenuItem toggleNeonCircleItem = new CheckMenuItem("Neon Circle Visible");
        toggleNeonCircleItem.setSelected(true);
        circleSpinner.outerNeonCircle.visibleProperty().bind(toggleNeonCircleItem.selectedProperty());
        circleSpinner.innerNeonCircle.visibleProperty().bind(toggleNeonCircleItem.selectedProperty());

        CheckMenuItem toggleCircleAnimationItem = new CheckMenuItem("Animated Neon Circle ");
        toggleCircleAnimationItem.setSelected(false);
        toggleCircleAnimationItem.setOnAction(e -> {
            circleSpinner.spin(toggleCircleAnimationItem.isSelected());
        });
        MenuItem restartProgressCircleItem = new MenuItem("Restart Progress Circle");
        restartProgressCircleItem.setOnAction(e -> {
            Timeline timeline = new Timeline();
            circleSpinner.setPercentComplete(0);
            KeyFrame keyFrame1 = new KeyFrame(Duration.millis(2000),
                new KeyValue(circleSpinner.percentComplete, 1,
                    Interpolator.EASE_BOTH)
            );
            timeline.getKeyFrames().addAll(keyFrame1);
            timeline.setCycleCount(1);
            timeline.play();
        });

        CheckMenuItem toggleSpinnerItem = new CheckMenuItem("Show Spinner Progress Pane");
        toggleSpinnerItem.setSelected(true);
        toggleSpinnerItem.setOnAction(e -> {
            circleSpinner.fadeBusy(!toggleSpinnerItem.isSelected());
        });

        cm.getItems().addAll(
            toggleNeonCircleItem, toggleCircleAnimationItem,
            restartProgressCircleItem, toggleSpinnerItem);
        cm.setAutoFix(true);
        cm.setAutoHide(true);
        cm.setHideOnEscape(true);

        stackPane.setOnMouseClicked((MouseEvent e) -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (null != cm)
                    if (!cm.isShowing())
                        cm.show(stackPane.getParent(), e.getScreenX(), e.getScreenY());
                    else
                        cm.hide();
                e.consume();
            }
        });

        Scene scene = new Scene(bpOilSpill, Color.BLACK);
        //Make everything pretty
        String CSS = this.getClass().getResource("/edu/jhuapl/trinity/css/styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        stage.setTitle("Busy Indicator tester");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
