package edu.jhuapl.trinity.javafx.components.panes;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2024 Sean Phillips
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

import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.data.messages.VectorMaskCollection;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author Sean Phillips
 */
public class NavigatorPane extends LitPathPane {
    private static final Logger LOG = LoggerFactory.getLogger(NavigatorPane.class);
    BorderPane bp;
    public static double DEFAULT_FIT_WIDTH = 512;
    public static int PANE_WIDTH = 800;
    public String imageryBasePath = "imagery/";
    boolean auto = false;
    Image currentImage = null;
    Label imageLabel;
    Label urlLabel;
    ImageView imageView;
    VBox contentVBox;

    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        MediaView mediaView = new MediaView();
        bpOilSpill.setCenter(mediaView);
        return bpOilSpill;
    }

    public NavigatorPane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, PANE_WIDTH, createContent(),
            "Content Navigator", "", 300.0, 400.0);
        this.scene = scene;
        bp = (BorderPane) this.contentPane;

        Image waitingImage = ResourceUtils.loadIconFile("waitingforimage");
        imageView = new ImageView(waitingImage);
        imageView.setFitWidth(DEFAULT_FIT_WIDTH);
        imageView.setFitHeight(DEFAULT_FIT_WIDTH);
        imageView.setPreserveRatio(true);

        urlLabel = new Label("Waiting for Image");
        urlLabel.setMaxWidth(DEFAULT_FIT_WIDTH);
        urlLabel.setTooltip(new Tooltip("Waiting for Image"));
        imageLabel = new Label("No Label");
        imageLabel.setMaxWidth(DEFAULT_FIT_WIDTH);
        contentVBox = new VBox(5, imageView, urlLabel, imageLabel);

        ImageView refresh = ResourceUtils.loadIcon("refresh", 32);

        VBox refreshVBox = new VBox(1, refresh, new Label("Refresh"));
        refreshVBox.setAlignment(Pos.BOTTOM_CENTER);

        InnerShadow innerShadow = new InnerShadow();
        innerShadow.setOffsetX(4);
        innerShadow.setOffsetY(4);
        innerShadow.setColor(Color.CYAN);

        refreshVBox.setOnMouseEntered(e -> {
            refresh.setEffect(innerShadow);
        });
        refreshVBox.setOnMouseClicked(e -> {
            toggleAuto();
            if (auto)
                refreshVBox.setEffect(innerShadow);
            else
                refreshVBox.setEffect(null);
        });
        refreshVBox.setOnMouseExited(e -> {
            refresh.setEffect(null);
        });

        bp.setCenter(contentVBox);

        scene.addEventHandler(ApplicationEvent.SET_IMAGERY_BASEPATH, e -> {
            this.imageryBasePath = (String) e.object;
        });
        scene.addEventHandler(ImageEvent.NEW_VECTORMASK_COLLECTION, e -> {
            VectorMaskCollection vmc = (VectorMaskCollection) e.object;

        });
        scene.addEventHandler(FeatureVectorEvent.SELECT_FEATURE_VECTOR, e -> {
            FeatureVector fv = (FeatureVector) e.object;
            if (null != fv.getLabel())
                imageLabel.setText(fv.getLabel());
            else
                imageLabel.setText("No Label");
            if (null != fv.getImageURL()) {
                try {
                    File file = new File(imageryBasePath + fv.getImageURL());
                    currentImage = new Image(file.toURI().toURL().toExternalForm());
                    imageView.setImage(currentImage);
                    urlLabel.setText(fv.getImageURL());
                    urlLabel.setTooltip(new Tooltip(file.toURI().toURL().toExternalForm()));
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        getScene().getRoot().fireEvent(
                            new CommandTerminalEvent("Unable to load Image, check Path.",
                                new Font("Consolas", 20), Color.RED));
                    });

                    LOG.error(null, ex);
                }
            }
        });
    }

    public void shutdown() {
        close();
        parent.getChildren().remove(this);
    }

    public void toggleAuto() {
        auto = !auto;
    }

    public void setImage(Image image) {
        currentImage = image;
        imageView.setImage(currentImage);
    }
}
