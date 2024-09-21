package edu.jhuapl.trinity.javafx.javafx3d.tasks;

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

import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import edu.jhuapl.trinity.utils.PCAConfig;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
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

/**
 * @author Sean Phillips
 */
public class ProjectPcaFeaturesTask extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectPcaFeaturesTask.class);
    Scene scene;
    FeatureCollection originalFC;
    PCAConfig config;
    private boolean cancelledByUser = false;
    private double projectionScalar = 100.0; //used for sizing values to 3D scene later

    public ProjectPcaFeaturesTask(Scene scene, FeatureCollection originalFC, PCAConfig config) {
        this.scene = scene;
        this.originalFC = originalFC;
        this.config = config;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("Watch a TV while you wait?");
        alert.setGraphic(ResourceUtils.loadIcon("retrowave-tv", 100));
        alert.initStyle(StageStyle.TRANSPARENT);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setBackground(Background.EMPTY);
        dialogPane.getScene().setFill(Color.TRANSPARENT);
        String DIALOGCSS = this.getClass().getResource("/edu/jhuapl/trinity/css/dialogstyles.css").toExternalForm();
        dialogPane.getStylesheets().add(DIALOGCSS);
        alert.setX(scene.getWidth() - 500);
        alert.setY(500);
        alert.resultProperty().addListener(r -> {
            if (alert.getResult().equals(ButtonType.YES)) {
//                manifoldControlPane.minimize();
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.SHOW_VIDEO_PANE,
                        "EMPTY VISION ", "A past never had for a Retrowave Future"));
            }
        });
        alert.show();

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
    protected FeatureCollection call() throws Exception {
        if (isCancelled()) return null;
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Fitting PCA Projection...", 0.5);
            ps.fillStartColor = Color.AZURE;
            ps.fillEndColor = Color.LIME;
            ps.innerStrokeColor = Color.AZURE;
            ps.outerStrokeColor = Color.LIME;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });

        double[][] featureArray = originalFC.getFeatures().stream()
            .map(FeatureVector.mapToStateArray)
            .toArray(double[][]::new);
        LOG.info("featureArray sizes: {} {}", featureArray.length, featureArray[0].length);

        int start = config.startIndex;
        if (start < 0 || start >= featureArray.length) {
            LOG.info("PCA Start index no bueno... setting to Zero.");
            start = 0;
        }
        int end = config.endIndex;
        if (end <= start
            || end >= originalFC.getFeatures().size()
            || end <= 0) {
            LOG.info("PCA End index no bueno... setting to Max.");
            end = originalFC.getFeatures().size() - 1;
        }

        int truncSize = featureArray[0].length;
        double[][] truncArray = originalFC.getFeatures().stream()
            .skip(start).limit(end)
            .map((FeatureVector t) -> {
                double[] states = new double[truncSize];
                for (int i = 0; i < truncSize && i < states.length; i++) {
                    states[i] = t.getData().get(i);
                }
                return states;
            })
            .toArray(double[][]::new);
        LOG.info("truncArray sizes: {} {}", truncArray.length, truncArray[0].length);

        LOG.info("PCA... ");
        long startTime = System.nanoTime();
        double[][] pcaProjection = null;
        if (config.method == AnalysisUtils.ANALYSIS_METHOD.SVD)
            pcaProjection = AnalysisUtils.doCommonsSVD(truncArray);
        else
            pcaProjection = AnalysisUtils.doCommonsPCA(truncArray);
        Utils.printTotalTime(startTime);

        LOG.info("mapping projected PCA data back to FeatureVectors...");
        FeatureCollection projectedFC = FeatureCollection.fromData(
            pcaProjection, config.pcaDimensions, config.scaling);
        for (int i = 0; i < projectedFC.getFeatures().size() - 1; i++) {
            if (i >= originalFC.getFeatures().size())
                break;
            FeatureVector origFV = originalFC.getFeatures().get(i);
            projectedFC.getFeatures().get(i).setLabel(origFV.getLabel());
            projectedFC.getFeatures().get(i).setScore(origFV.getScore());
            projectedFC.getFeatures().get(i).setImageURL(origFV.getImageURL());
            projectedFC.getFeatures().get(i).setText(origFV.getText());
            projectedFC.getFeatures().get(i).setMetaData(origFV.getMetaData());
        }
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR, ps));
        });
        return projectedFC;
    }

    /**
     * @return the cancelledByUser
     */
    public boolean isCancelledByUser() {
        return cancelledByUser;
    }

    /**
     * @param cancelledByUser the cancelledByUser to set
     */
    public void setCancelledByUser(boolean cancelledByUser) {
        this.cancelledByUser = cancelledByUser;
    }

    /**
     * @return the projectionScalar
     */
    public double getProjectionScalar() {
        return projectionScalar;
    }

    /**
     * @param projectionScalar the projectionScalar to set
     */
    public void setProjectionScalar(double projectionScalar) {
        this.projectionScalar = projectionScalar;
    }
}
