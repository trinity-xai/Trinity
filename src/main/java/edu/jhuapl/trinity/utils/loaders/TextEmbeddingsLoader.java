package edu.jhuapl.trinity.utils.loaders;

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
import edu.jhuapl.trinity.data.files.TextEmbeddingCollectionFile;
import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.data.messages.TextEmbeddingSet;
import edu.jhuapl.trinity.javafx.components.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
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
public class TextEmbeddingsLoader extends Task {
    Scene scene;
    File file;

    public TextEmbeddingsLoader(Scene scene, File file) {
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
        alert.setHeaderText("Loading Text Embeddings...");
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
            ProgressStatus ps = new ProgressStatus("Loading Text Embedding Collection File...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });
        TextEmbeddingCollectionFile textEmbeddingCollectionFile = new TextEmbeddingCollectionFile(file.getAbsolutePath(), true);

        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new CommandTerminalEvent(
                "Done Text Embeddings from File.", new Font("Consolas", 20), Color.GREEN));
            System.out.println("Done loading embeddings file.");
            ProgressStatus ps = new ProgressStatus("Converting Text Embeddings to Feature Vectors...", -1);
            ps.fillStartColor = Color.CYAN;
            ps.fillEndColor = Color.CYAN;
            ps.innerStrokeColor = Color.CYAN;
            ps.outerStrokeColor = Color.CYAN;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });

        try {
            List<TextEmbeddingSet> embeddings = textEmbeddingCollectionFile
                .textEmbeddingCollection.getText_embeddings();
            String collectionLabel = textEmbeddingCollectionFile.textEmbeddingCollection.getLabel();
            FeatureCollection fc = new FeatureCollection();

            final int n = embeddings.size(); //how many total
            int updatePercent = n / 10; //rounded percent progress

            for (int i = 0; i < embeddings.size(); i++) {
                //First do the original embeddings as a special case
                FeatureVector fv = new FeatureVector();
                List<Double> flatList = new ArrayList<>();
                flatList.addAll(embeddings.get(i).getText_embedding());
                fv.setData(flatList);
                fv.setLabel(collectionLabel + "_Original_Embedding");
                if (null != embeddings.get(i).getScore())
                    fv.setScore(embeddings.get(i).getScore());
                else
                    fv.setScore(textEmbeddingCollectionFile.textEmbeddingCollection.getScore());
                fv.setText(embeddings.get(i).getText());
                fv.setLayer(i);
                fc.getFeatures().add(fv);

                //now for each text chunk make a feature vector
                int width = embeddings.get(i).getParsed().size();
                for (int parsedIndex = 0; parsedIndex < width; parsedIndex++) {
                    FeatureVector parsedChunkFV = new FeatureVector();
                    List<Double> chunkEmbedding = new ArrayList<>();
                    chunkEmbedding.addAll(embeddings.get(i).getEmbeddings().get(parsedIndex));
                    parsedChunkFV.setData(chunkEmbedding);
                    //get actual text
                    String parsedText = embeddings.get(i).getParsed().get(parsedIndex);
                    parsedChunkFV.setText(parsedText);
                    //Extract first three words of chunk
                    String[] tokens = parsedText.split("\\s+");
                    String parsedPreview = tokens[0];
                    if (tokens.length > 2) {
                        parsedPreview += " " + tokens[1] + " " + tokens[2];
                    }
                    String parsedChunkLabel = collectionLabel + "_" + parsedPreview;
                    if (null != embeddings.get(i).getLabel())
                        parsedChunkLabel = embeddings.get(i).getLabel() + "_" + parsedPreview;
                    parsedChunkFV.setLabel(parsedChunkLabel);
                    if (null != embeddings.get(i).getScore())
                        parsedChunkFV.setScore(embeddings.get(i).getScore());
                    else
                        parsedChunkFV.setScore(textEmbeddingCollectionFile.textEmbeddingCollection.getScore());
                    parsedChunkFV.setLayer(i);
                    fc.getFeatures().add(parsedChunkFV);
                }
                //update the progress indicator
                if (i % updatePercent == 0) {
                    double percentComplete = Double.valueOf(i) / Double.valueOf(n);
                    Platform.runLater(() -> {
                        ProgressStatus ps = new ProgressStatus(
                            "Converting Text Embeddings to Feature Vectors...", percentComplete);
                        ps.fillStartColor = Color.AZURE;
                        ps.fillEndColor = Color.LIME;
                        ps.innerStrokeColor = Color.AZURE;
                        ps.outerStrokeColor = Color.LIME;
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
                Trajectory trajectory = new Trajectory(file.getName());
                trajectory.totalStates = fc.getFeatures().size();   
                Trajectory.addTrajectory(trajectory);
                Trajectory.globalTrajectoryToFeatureCollectionMap.put(trajectory, fc);
                scene.getRoot().fireEvent(
                    new TrajectoryEvent(TrajectoryEvent.NEW_TRAJECTORY_OBJECT,trajectory, fc));                    
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
