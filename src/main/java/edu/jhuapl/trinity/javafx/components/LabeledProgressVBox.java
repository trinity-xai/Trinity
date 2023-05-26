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

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * @author Sean Phillips
 */
public class LabeledProgressVBox extends VBox {

    public final ProgressIndicator scatterPi = new ProgressIndicator();
    public final ProgressBar progressBar = new ProgressBar();
    public Label progressLabel = new Label();
    public SimpleStringProperty labelString = new SimpleStringProperty();
    public double defaultOpacity = 0.6;
    private FadeTransition ft = new FadeTransition(Duration.seconds(1), this);

    public LabeledProgressVBox() {
        setMouseTransparent(true);
        setAlignment(Pos.CENTER);
        scatterPi.setBackground(Background.EMPTY);
        scatterPi.setBackground(new Background(new BackgroundFill(
            Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
        scatterPi.setVisible(true);
        scatterPi.setMinSize(200, 200);

        progressBar.setMinSize(200, 25);
        progressBar.setVisible(false);
        progressLabel.setText("Loading...");
        progressLabel.setTextAlignment(TextAlignment.CENTER);
        labelString.set("Loading...");
        progressLabel.textProperty().bind(labelString);
        progressLabel.setFont(new Font("ARIAL", 24));
        progressLabel.setMinSize(200, 25);
        setAlignment(Pos.CENTER);
        this.getChildren().addAll(scatterPi, progressBar, progressLabel);

        ft.setAutoReverse(false);
        ft.setCycleCount(0);

    }

    public void setLabelLater(final String newText) {
        Platform.runLater(() -> labelString.set(newText));
    }

    public void fadeBusy(boolean fadeOut) {
        if (fadeOut) {
            ft.setFromValue(defaultOpacity);
            ft.setToValue(0.0);
        } else {
            ft.setFromValue(0.0);
            ft.setToValue(defaultOpacity);
        }
        setVisible(true);
        ft.setOnFinished(e -> setVisible(!fadeOut));
        ft.playFromStart();
    }
}
