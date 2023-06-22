package edu.jhuapl.trinity.utils.loaders;

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

import edu.jhuapl.trinity.data.ZeroPilotLatents;
import edu.jhuapl.trinity.data.files.TextEmbeddingCollectionFile;
import edu.jhuapl.trinity.data.files.ZeroPilotLatentsFile;
import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.data.messages.TextEmbeddingCollection;
import edu.jhuapl.trinity.data.messages.TextEmbeddingSet;
import edu.jhuapl.trinity.javafx.components.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.utils.DataUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.StageStyle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Sean Phillips
 */
public class ZeroPilotLatentsLoader extends Task {
    Scene scene;
    File file;

    public ZeroPilotLatentsLoader(Scene scene, File file) {
        this.scene = scene;
        this.file = file;
        setOnSucceeded(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
        setOnFailed(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
        setOnCancelled(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Clear existing data?",
            ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Loading ZERO Pilot Latents...");
        alert.setGraphic(ResourceUtils.loadIcon("alert", 75));
        alert.initStyle(StageStyle.TRANSPARENT);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setBackground(Background.EMPTY);
        dialogPane.getScene().setFill(Color.TRANSPARENT);
        String DIALOGCSS = ResourceUtils.class.getResource("/edu/jhuapl/trinity/css/dialogstyles.css").toExternalForm();
        dialogPane.getStylesheets().add(DIALOGCSS);
        Optional<ButtonType> optBT = alert.showAndWait();
        if (optBT.get().equals(ButtonType.YES))
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new HyperspaceEvent(HyperspaceEvent.CLEAR_HYPERSPACE_NOW));
            });
    }

    @Override
    protected Void call() throws Exception {
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Loading ZERO Pilot Latents...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });
        ZeroPilotLatentsFile zeroPilotLatentsFile = new ZeroPilotLatentsFile(file.getAbsolutePath(), true);
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new CommandTerminalEvent(
            "Done Loading ZERO Pilot Latents from File.", new Font("Consolas", 20), Color.GREEN));
            System.out.println("ZERO pilot latents file read.");

            ProgressStatus ps = new ProgressStatus("Converting ZERO Pilot latent vectors to Feature Vectors...", -1);
            ps.fillStartColor = Color.CYAN;
            ps.fillEndColor = Color.CYAN;
            ps.innerStrokeColor = Color.CYAN;
            ps.outerStrokeColor = Color.CYAN;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });
        
        try {
            List<ZeroPilotLatents> latents = zeroPilotLatentsFile.zeroPilotLatentsList;
            FeatureCollection fc = new FeatureCollection();
            
            final int n = latents.size(); //how many total
            int updatePercent = n / 10; //rounded percent progress            
            if(updatePercent < 1)
                updatePercent = 1;
            ZeroPilotLatents zero;
            for(int i=0;i<latents.size();i++) {
                zero = latents.get(i); 
                FeatureVector fv = new FeatureVector();
                fv.getData().addAll(zero.getLatents());
                fv.setLabel(zero.getLabels());
                fv.setFrameId(zero.getNumber());
                fv.setLayer(zero.getTraj_num().intValue());
                fc.getFeatures().add(fv);
                //update the progress indicator
                if (i % updatePercent == 0) {
                    double percentComplete = Double.valueOf(i) / Double.valueOf(n);
                    Platform.runLater(() -> {
                        ProgressStatus ps = new ProgressStatus(
                            "Converting Text Embeddings to Feature Vectors...", percentComplete);
                        ps.fillStartColor = Color.CYAN;
                        ps.fillEndColor = Color.CADETBLUE;
                        ps.innerStrokeColor = Color.DARKMAGENTA;
                        ps.outerStrokeColor = Color.CADETBLUE;
                        scene.getRoot().fireEvent(
                            new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
                    });
                }                
            }            
            Platform.runLater(() -> {
                ProgressStatus ps = new ProgressStatus("Injecting as FeatureCollection...", -1);
                ps.fillStartColor = Color.CYAN;
                ps.fillEndColor = Color.CYAN;
                ps.innerStrokeColor = Color.CYAN;
                ps.outerStrokeColor = Color.CYAN;
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
                scene.getRoot().fireEvent(
                    new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc));
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
