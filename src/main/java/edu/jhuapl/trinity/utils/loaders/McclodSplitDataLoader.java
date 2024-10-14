/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils.loaders;

import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.data.files.McclodSplitDataTsvFile;
import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.data.messages.SystemFeatures;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.TrajectoryEvent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Sean Phillips
 */
public class McclodSplitDataLoader extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(McclodSplitDataLoader.class);
    Scene scene;
    File file;

    public McclodSplitDataLoader(Scene scene, File file) {
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
        alert.setHeaderText("Loading Split Data...");
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
    protected List<SystemFeatures> call() throws Exception {
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Loading Split Data File...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });
        McclodSplitDataTsvFile mcclodSplitDataTsvFile = new McclodSplitDataTsvFile(file.getAbsolutePath(), true);

        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Converting SplitData to SystemFeatures...", -1);
            ps.fillStartColor = Color.CYAN;
            ps.fillEndColor = Color.CYAN;
            ps.innerStrokeColor = Color.CYAN;
            ps.outerStrokeColor = Color.CYAN;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });

        List<SystemFeatures> systemFeatures = null;
        try {
            systemFeatures = DataUtils.convertSplitData(mcclodSplitDataTsvFile.mcclodSplitDataTsvList);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new CommandTerminalEvent(
                    "Done loading split data.", new Font("Consolas", 20), Color.GREEN));
            });
            LOG.info("Done loading split data.");
            Platform.runLater(() -> {
                ProgressStatus ps = new ProgressStatus("Injecting as FeatureCollection...", -1);
                ps.fillStartColor = Color.CYAN;
                ps.fillEndColor = Color.CYAN;
                ps.innerStrokeColor = Color.CYAN;
                ps.outerStrokeColor = Color.CYAN;
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
            });

            FeatureCollection fc = new FeatureCollection();
            systemFeatures.stream().forEach(sf -> {
                FeatureVector fv = new FeatureVector();
                List<Double> flatList = new ArrayList<>();
                flatList.addAll(sf.getInput());
                flatList.addAll(sf.getLatent());
                flatList.addAll(sf.getOutput());
                fv.setData(flatList);
                fv.setMessageId(sf.getMessageId());
                fc.getFeatures().add(fv);
            });
            Platform.runLater(() -> {
                Trajectory trajectory = new Trajectory(file.getName());
                trajectory.totalStates = fc.getFeatures().size();
                scene.getRoot().fireEvent(
                    new TrajectoryEvent(TrajectoryEvent.NEW_TRAJECTORY_OBJECT, trajectory));
                scene.getRoot().fireEvent(
                    new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc));
            });
        } catch (Exception ex) {
            LOG.error("Exception", ex);
        }
        return systemFeatures;
    }
}
