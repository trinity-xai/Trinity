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

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import lit.litfx.controls.covalent.PathPane;
import lit.litfx.controls.covalent.events.CovalentPaneEvent;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class HyperspaceControlsPane extends PathPane {
    Scene scene;

    private static AnchorPane createContent() {
        //make transparent so it doesn't interfere with subnode transparency effects
        Background transBack = new Background(new BackgroundFill(
            Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
        FXMLLoader loader = new FXMLLoader(HyperspaceControlsPane.class.getResource("/edu/jhuapl/trinity/fxml/Hyperspace.fxml"));
        loader.setLocation(HyperspaceControlsPane.class.getResource("/edu/jhuapl/trinity/fxml/Hyperspace.fxml"));

        AnchorPane hyperspaceAnchorPane;
        try {
            hyperspaceAnchorPane = loader.load();
            hyperspaceAnchorPane.setBackground(transBack);
        } catch (IOException ex) {
            hyperspaceAnchorPane = new AnchorPane(new Text("Unable to load Hyperspace Controls view."));
            Logger.getLogger(HyperspaceControlsPane.class.getName()).log(Level.SEVERE, null, ex);
        }
        return hyperspaceAnchorPane;
    }

    public HyperspaceControlsPane(Scene scene, Pane parent) {
        super(scene, parent, 600, 400, createContent(), "Hyperspace ", " controls", 200.0, 300.0);
        this.scene = scene;
        // must be set to prevent user from resizing too small.
        setMinWidth(600);
        setMinHeight(400);

        this.scene.getRoot().addEventHandler(CovalentPaneEvent.COVALENT_PANE_CLOSE, e -> {
            if (e.pathPane == this)
                parent.getChildren().remove(this);
        });
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> this.toFront());
        //transparency fade effects...
        addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            fade(100, 0.8);
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            fade(100, 0.5);
        });
    }

    public void fade(double timeMS, double toValue) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeMS), contentPane);
        fadeTransition.setToValue(toValue);
        fadeTransition.setOnFinished(e -> contentPane.setOpacity(toValue));
        fadeTransition.play();
    }
}
