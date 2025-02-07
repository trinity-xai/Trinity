/* Copyright (C) 2021 - 2025 Sean Phillips */

package edu.jhuapl.trinity.javafx.javafx3d.tasks;

import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.data.messages.AnalysisConfig;
import edu.jhuapl.trinity.data.messages.UmapConfig;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.umap.Umap;
import java.time.Duration;
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
public class AutomaticUmapForThePeopleTask extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(AutomaticUmapForThePeopleTask.class);
    Scene scene;
    AnalysisConfig analysisConfig;
    UmapConfig umapConfig;
    
    Umap umap;
    private boolean cancelledByUser = false;
    private double projectionScalar = 100.0; //used for sizing values to 3D scene later
    private boolean enableLoadingMedia = false;

    public AutomaticUmapForThePeopleTask(Scene scene, AnalysisConfig acDC, UmapConfig umapConfig, boolean skipQuestions, boolean enableLoadingMedia) {
        this.scene = scene;
        this.analysisConfig = acDC;
        this.umapConfig = umapConfig;
        this.enableLoadingMedia = enableLoadingMedia;

        if (enableLoadingMedia) {
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
                    scene.getRoot().fireEvent(
                        new ApplicationEvent(ApplicationEvent.SHOW_VIDEO_PANE,
                            "EMPTY VISION ", "A past never had for a Retrowave Future"));
                }
            });
            alert.show();
        }

//        setOnSucceeded(e -> {
//            Platform.runLater(() -> {
//                scene.getRoot().fireEvent(
//                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
//            });
//        });
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
        if (isCancelled()) return null;
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_HYPERSPACE));
        });
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.NEW_UMAP_CONFIG, umapConfig));
        });         
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new FeatureVectorEvent(
                FeatureVectorEvent.CLEAR_ALL_FEATUREVECTORS));
        });
        Thread.sleep(Duration.ofMillis(1000));
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Loading Data...", 0.5);
            ps.fillStartColor = Color.AZURE;
            ps.fillEndColor = Color.LIME;
            ps.innerStrokeColor = Color.AZURE;
            ps.outerStrokeColor = Color.LIME;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });
        for(String data : analysisConfig.getDataSources()) {
            FeatureCollectionFile fcf = new FeatureCollectionFile(data, true);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new FeatureVectorEvent(
                    FeatureVectorEvent.NEW_FEATURE_COLLECTION, fcf.featureCollection));
            });            
        }
        if(null != analysisConfig.getContentBasePath()) {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new ApplicationEvent(
                    ApplicationEvent.SET_IMAGERY_BASEPATH, 
                        analysisConfig.getContentBasePath()));
            });            
        }
        Thread.sleep(Duration.ofSeconds(1));
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_PROJECTIONS));
        });        
        Thread.sleep(Duration.ofSeconds(1));
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new ManifoldEvent(ManifoldEvent.GENERATE_NEW_UMAP));
        });
        return null;
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

    /**
     * @return the enableLoadingMedia
     */
    public boolean isEnableLoadingMedia() {
        return enableLoadingMedia;
    }

    /**
     * @param enableLoadingMedia the enableLoadingMedia to set
     */
    public void setEnableLoadingMedia(boolean enableLoadingMedia) {
        this.enableLoadingMedia = enableLoadingMedia;
    }
}
