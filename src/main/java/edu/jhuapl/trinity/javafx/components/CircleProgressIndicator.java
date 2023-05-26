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

import edu.jhuapl.trinity.javafx.components.radial.AnimatedFillCircle;
import edu.jhuapl.trinity.javafx.components.radial.AnimatedNeonCircle;
import javafx.animation.FadeTransition;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * @author Sean Phillips
 */
public class CircleProgressIndicator extends StackPane {

    public Label progressLabel = new Label();
    public SimpleStringProperty labelString = new SimpleStringProperty();
    public double defaultOpacity = 0.75;
    private FadeTransition ft = new FadeTransition(Duration.seconds(1), this);
    public AnimatedNeonCircle outerNeonCircle, innerNeonCircle;
    public AnimatedFillCircle fillCircle;
    public SimpleDoubleProperty percentComplete = new SimpleDoubleProperty(0.0);

    public CircleProgressIndicator() {
        setMouseTransparent(true);
        setAlignment(Pos.CENTER);

        outerNeonCircle = new AnimatedNeonCircle(
            new AnimatedNeonCircle.Animation(
                Duration.millis(2000), Transition.INDEFINITE, false),
            100, 3, 50.0, 100.0);

        innerNeonCircle = new AnimatedNeonCircle(
            new AnimatedNeonCircle.Animation(
                Duration.millis(3000), Transition.INDEFINITE, false),
            70, 1.5, 50.0, 20.0);

        fillCircle = new AnimatedFillCircle(65.0, 0.0, 2.0, 1.0);

        progressLabel.setText("Loading...");
        progressLabel.setTextAlignment(TextAlignment.CENTER);
        labelString.set("Loading...");
        progressLabel.textProperty().bind(labelString);
        progressLabel.setFont(new Font("Consolas", 24));
        progressLabel.setMinSize(200, 25);
        setAlignment(Pos.CENTER);
        getChildren().addAll(fillCircle, innerNeonCircle,
            outerNeonCircle, progressLabel);
        progressLabel.setTranslateY(120);

        ft.setAutoReverse(false);
        ft.setCycleCount(0);

        fillCircle.percentComplete.bind(percentComplete);
    }

    public void updateStatus(ProgressStatus ps) {
        if (null == ps)
            return;
        setLabelLater(ps.statusMessage);
        if (ps.percentComplete < 0) {
            //@TODO SMP something fancier for indeterminate
            setPercentComplete(0);
        } else {
            setPercentComplete(ps.percentComplete);
        }
        innerNeonCircle.setStroke(ps.innerStrokeColor);
        outerNeonCircle.setStroke(ps.outerStrokeColor);
        fillCircle.fillStartColor = ps.fillStartColor;
        fillCircle.fillEndColor = ps.fillEndColor;
    }

    public void setLabelLater(final String newText) {
        Platform.runLater(() -> labelString.set(newText));
    }

    public void setPercentComplete(double percentComplete) {
        this.percentComplete.set(percentComplete);
    }

    public void spin(boolean spin) {
        outerNeonCircle.play(spin);
        innerNeonCircle.play(spin);
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
