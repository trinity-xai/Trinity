package edu.jhuapl.trinity.javafx.javafx3d.tasks;

import com.github.trinity.supermds.SuperMDS;
import com.github.trinity.supermds.SuperMDS.Params;
import com.github.trinity.supermds.SuperMDSHelper;
import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import static edu.jhuapl.trinity.utils.Utils.printTotalTime;
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

import java.time.Duration;
import java.util.Arrays;

/**
 * @author Sean Phillips
 */
public class ProjectMdsFeaturesTask extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectMdsFeaturesTask.class);
    Scene scene;
    FeatureCollection originalFC;
    Params params;
    
    private boolean cancelledByUser = false;
    private double projectionScalar = 100.0; //used for sizing values to 3D scene later
    private boolean enableLoadingMedia = false;

    public ProjectMdsFeaturesTask(Scene scene, FeatureCollection originalFC, Params params, boolean enableLoadingMedia) {
        this.scene = scene;
        this.originalFC = originalFC;
        this.params = params;
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
            String DIALOGCSS = StyleResourceProvider.getResource("dialogstyles.css").toExternalForm();
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
        }

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
            ProgressStatus ps = new ProgressStatus("Preparing MDS Process...", 0.5);
            ps.fillStartColor = Color.AZURE;
            ps.fillEndColor = Color.LIME;
            ps.innerStrokeColor = Color.AZURE;
            ps.outerStrokeColor = Color.LIME;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });
        Thread.sleep(Duration.ofSeconds(1));
        System.out.println("Preparing data...");
        long startTime = System.nanoTime();
        double[][] originalData = originalFC.convertFeaturesToArray();
        double[][] weights = new double[originalData.length][originalData.length]; 
        for (int i = 0; i < originalData.length; i++) {
            Arrays.fill(weights[i], 1.0);
        }

        printTotalTime(startTime);         
        System.out.println("Symmetrizing and Normalizing Matrix...");
        startTime = System.nanoTime();
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Symmetrizing and Normalizing Matrix...", 0.5);
            ps.fillStartColor = Color.AZURE;
            ps.fillEndColor = Color.LIME;
            ps.innerStrokeColor = Color.AZURE;
            ps.outerStrokeColor = Color.LIME;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });
        double[][] symmetricDistanceMatrix = SuperMDS.ensureSymmetricDistanceMatrix(originalData);
        double[][] normalizedDistanceMatrix = SuperMDSHelper.normalizeDistancesParallel(symmetricDistanceMatrix);
        printTotalTime(startTime);
        System.out.println("Running MDS...");
        startTime = System.nanoTime();
        double[][] mdsEmbedding = SuperMDS.runMDS(normalizedDistanceMatrix, params);
        printTotalTime(startTime);

        
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Converting to FeatureCollection...", 0.5);
            ps.fillStartColor = Color.CYAN;
            ps.fillEndColor = Color.NAVY;
            ps.innerStrokeColor = Color.CYAN;
            ps.outerStrokeColor = Color.SILVER;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });
        LOG.info("mapping projected UMAP data back to FeatureVectors...");
        FeatureCollection projectedFC = FeatureCollection.fromData(
            mdsEmbedding, 3, getProjectionScalar());
        for (int i = 0; i < originalFC.getFeatures().size(); i++) {
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
