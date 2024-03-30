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
import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class FeatureCollectionLoader extends Task {
    Scene scene;
    File file;

    public FeatureCollectionLoader(Scene scene, File file) {
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
    }

    @Override
    protected Void call() throws Exception {
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Deserializing Feature Collection File...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });

        try {
            FeatureCollectionFile fcFile = new FeatureCollectionFile(file.getAbsolutePath(), true);
            Platform.runLater(() -> scene.getRoot().fireEvent(
                new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fcFile.featureCollection)));
            Trajectory trajectory = new Trajectory(file.getName());
            trajectory.totalStates = fcFile.featureCollection.getFeatures().size();
            Trajectory.addTrajectory(trajectory);
            Trajectory.globalTrajectoryToFeatureCollectionMap.put(trajectory, fcFile.featureCollection);
            Platform.runLater(() -> scene.getRoot().fireEvent(
                new TrajectoryEvent(TrajectoryEvent.NEW_TRAJECTORY_OBJECT, trajectory, fcFile.featureCollection)));
        } catch (Exception ex) {
            Logger.getLogger(FeatureCollectionLoader.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
