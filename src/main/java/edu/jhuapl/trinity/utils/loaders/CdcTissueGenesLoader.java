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
import edu.jhuapl.trinity.data.files.CdcTissueGenesFile;
import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
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
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * @author Sean Phillips
 */
public class CdcTissueGenesLoader extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(CdcTissueGenesLoader.class);
    Scene scene;
    File file;

    public CdcTissueGenesLoader(Scene scene, File file) {
        this.scene = scene;
        this.file = file;
        setOnSucceeded(e -> {
            FeatureCollection fc;
            try {
                fc = (FeatureCollection) get();
                Trajectory trajectory = new Trajectory(file.getName());
                trajectory.totalStates = fc.getFeatures().size();
                Trajectory.addTrajectory(trajectory);
                Trajectory.globalTrajectoryToFeatureCollectionMap.put(trajectory, fc);
                Platform.runLater(() -> {
                    scene.getRoot().fireEvent(
                        new TrajectoryEvent(TrajectoryEvent.NEW_TRAJECTORY_OBJECT, trajectory, fc));
                    scene.getRoot().fireEvent(
                        new FeatureVectorEvent(FeatureVectorEvent.NEW_FEATURE_COLLECTION, fc));
                });
            } catch (InterruptedException | ExecutionException ex) {
                LOG.error(null, ex);
            }
        });

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Clear existing data?",
            ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Loading Gene Tissue Data");
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
    protected FeatureCollection call() throws Exception {
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Loading Gene File...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });
        CdcTissueGenesFile cdcTissueGenesFile = new CdcTissueGenesFile(file.getAbsolutePath(), true);

        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Converting Genes to FeatureVectors...", -1);
            ps.fillStartColor = Color.CYAN;
            ps.fillEndColor = Color.CYAN;
            ps.innerStrokeColor = Color.CYAN;
            ps.outerStrokeColor = Color.CYAN;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });
        //convert to Feature Vector Collection for the cdcTissueGenesList
        boolean normalize = true;
        FeatureCollection fc = null;
        try {
            fc = DataUtils.convertCdcTissueGenes(cdcTissueGenesFile.cdcTissueGenesList, normalize);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        } catch (Exception ex) {
            LOG.error("Exception", ex);
        }
        return fc;
    }
}
