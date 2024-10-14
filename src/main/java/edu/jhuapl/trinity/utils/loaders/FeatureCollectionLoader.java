/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.loaders;

import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Sean Phillips
 */
public class FeatureCollectionLoader extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureCollectionLoader.class);
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
            LOG.error(null, ex);
        }

        return null;
    }
}
