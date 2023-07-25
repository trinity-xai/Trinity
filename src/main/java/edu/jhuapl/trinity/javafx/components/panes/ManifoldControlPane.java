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

import edu.jhuapl.trinity.utils.ResourceUtils;
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
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;

/**
 * @author Sean Phillips
 */
public class ManifoldControlPane extends PathPane {
    Scene scene;
    boolean fadeEnabled = true;
    double fadeSideInset = -40;
    private static BorderPane createContent() {
        //make transparent so it doesn't interfere with subnode transparency effects
        Background transBack = new Background(new BackgroundFill(
            Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
        FXMLLoader loader = new FXMLLoader(ManifoldControlPane.class.getResource("/edu/jhuapl/trinity/fxml/ManifoldControl.fxml"));
        loader.setLocation(ManifoldControlPane.class.getResource("/edu/jhuapl/trinity/fxml/ManifoldControl.fxml"));
        BorderPane sgRoot;
        try {
            AnchorPane manifoldControlAnchorPane = loader.load();
            sgRoot = new BorderPane(manifoldControlAnchorPane);
            sgRoot.setBackground(transBack);
        } catch (IOException ex) {
            sgRoot = new BorderPane(new Text("Unable to load Manifold Controls."));
            Logger.getLogger(ManifoldControlPane.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sgRoot;
    }

    public ManifoldControlPane(Scene scene, Pane parent) {
        super(scene, parent, 500, 600, createContent(), "Manifolds ", "", 200.0, 300.0);
        this.scene = scene;
        // must be set to prevent user from resizing too small.
        setMinWidth(300);
        setMinHeight(200);
        ImageView iv = ResourceUtils.loadIcon("fade", 50);
        Label labelFadeout = new Label("Fadeout", iv);
        Border border = new Border(new BorderStroke(
        Color.CYAN, BorderStrokeStyle.DOTTED, 
            CornerRadii.EMPTY, new BorderWidths(1), new Insets(0, fadeSideInset, 0, fadeSideInset))
        );
        
        AnchorPane.setBottomAnchor(labelFadeout, -16.0);
        AnchorPane.setRightAnchor(labelFadeout, 40.0);
        this.mainContentBorderFrame.getChildren().add(labelFadeout);
        Glow glow = new Glow(0.9);
        
        labelFadeout.setOnMouseEntered(e-> labelFadeout.setBorder(border));
        labelFadeout.setOnMouseExited(e-> labelFadeout.setBorder(null));
        Background background = new Background(new BackgroundFill(
        Color.CYAN.deriveColor(1,1,1,0.05), 
        CornerRadii.EMPTY, new Insets(0, fadeSideInset, 0, fadeSideInset)));
        labelFadeout.setEffect(glow);
        labelFadeout.setBackground(background);
        labelFadeout.setOnMouseClicked(e -> {
            fadeEnabled = !fadeEnabled;
            if(fadeEnabled) {
                labelFadeout.setEffect(glow);
                labelFadeout.setBackground(background);
            }
            else {
                labelFadeout.setEffect(null);
                labelFadeout.setBackground(null);
            }
        });
        
        this.scene.getRoot().addEventHandler(CovalentPaneEvent.COVALENT_PANE_CLOSE, e -> {
            if (e.pathPane == this)
                parent.getChildren().remove(this);
        });
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> this.toFront());
        //transparency fade effects...
        addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if(fadeEnabled)
                fade(100, 0.8);
            else
                contentPane.setOpacity(0.8);
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            if(fadeEnabled)
                fade(100, 0.3);
            else
                contentPane.setOpacity(0.8);
        });
    }

    public void fade(double timeMS, double toValue) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeMS), contentPane);
        fadeTransition.setToValue(toValue);
        fadeTransition.setOnFinished(e -> contentPane.setOpacity(toValue));
        fadeTransition.play();
    }
}
