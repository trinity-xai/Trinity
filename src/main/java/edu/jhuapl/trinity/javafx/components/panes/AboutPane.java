package edu.jhuapl.trinity.javafx.components.panes;

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
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
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
public class AboutPane extends PathPane {
    Scene scene;

    private static BorderPane createContent() {
        //make transparent so it doesn't interfere with subnode transparency effects
        Background transBack = new Background(new BackgroundFill(
            Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
        FXMLLoader loader = new FXMLLoader(AboutPane.class.getResource("/edu/jhuapl/trinity/fxml/About.fxml"));
        loader.setLocation(AboutPane.class.getResource("/edu/jhuapl/trinity/fxml/About.fxml"));
        BorderPane sgRoot;
        try {
            AnchorPane aboutAnchorPane = loader.load();
            sgRoot = new BorderPane(aboutAnchorPane);
            sgRoot.setBackground(transBack);
        } catch (IOException ex) {
            sgRoot = new BorderPane(new Text("Unable to load About view."));
            Logger.getLogger(AboutPane.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sgRoot;
    }

    public AboutPane(Scene scene, Pane parent) {
        super(scene, parent, 600, 800, createContent(), "About ", "", 200.0, 300.0);
        this.scene = scene;
        // must be set to prevent user from resizing too small.
        setMinWidth(300);
        setMinHeight(200);
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
            fade(100, 0.3);
        });
    }

    public void fade(double timeMS, double toValue) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeMS), contentPane);
        fadeTransition.setToValue(toValue);
        fadeTransition.setOnFinished(e -> contentPane.setOpacity(toValue));
        fadeTransition.play();
    }
}
