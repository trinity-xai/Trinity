package edu.jhuapl.trinity.utils.loaders;

import edu.jhuapl.trinity.data.SaturnShot;
import edu.jhuapl.trinity.data.files.SaturnFile;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.utils.DataUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class SaturnLoader extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(SaturnLoader.class);
    Scene scene;
    File file;

    public SaturnLoader(Scene scene, File file) {
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
            ProgressStatus ps = new ProgressStatus("Loading Saturn File...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });
        SaturnFile saturnFile = new SaturnFile(file.getPath(), Boolean.FALSE);
        List<SaturnShot> saturnMeasurements = saturnFile.parseContent();

        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Converting measurements to FeatureVectors...", -1);
            ps.fillStartColor = Color.CYAN;
            ps.fillEndColor = Color.CYAN;
            ps.innerStrokeColor = Color.CYAN;
            ps.outerStrokeColor = Color.CYAN;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });
 
//        try {
            FeatureCollection fc = DataUtils.convertSaturnMeasurements(saturnMeasurements);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc));
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
//        } catch (Exception ex) {
//            LOG.error("Exception", ex);
//        }
        return null;
    }
}
