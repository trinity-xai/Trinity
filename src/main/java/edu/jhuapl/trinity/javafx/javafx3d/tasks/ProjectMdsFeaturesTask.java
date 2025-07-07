package edu.jhuapl.trinity.javafx.javafx3d.tasks;

import com.github.trinity.supermds.SuperMDS;
import com.github.trinity.supermds.SuperMDS.Params;
import com.github.trinity.supermds.SuperMDSHelper;
import com.github.trinity.supermds.SuperMDSValidator;
import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import static edu.jhuapl.trinity.utils.Utils.totalTimeString;
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
import java.util.Formatter;

/**
 * @author Sean Phillips
 */
public class ProjectMdsFeaturesTask extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(ProjectMdsFeaturesTask.class);
    Scene scene;
    FeatureCollection originalFC;
    Params params;
    private boolean computeMetrics = false;
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
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_TEXT_CONSOLE, "Preparing MDS Process..."));            
        });
        Thread.sleep(Duration.ofSeconds(1));
        LOG.info("Preparing MDS Process...");
        long startTime = System.nanoTime();
        double[][] originalData = originalFC.convertFeaturesToArray();
        double[][] weights = new double[originalData.length][originalData.length]; 
        for (int i = 0; i < originalData.length; i++) {
            Arrays.fill(weights[i], 1.0);
        }
        String totesTime = totalTimeString(startTime);
        LOG.info(totesTime);
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_TEXT_CONSOLE, totesTime, true));            
        });        
        
        LOG.info("Symmetrizing and Normalizing Matrix...");
        startTime = System.nanoTime();
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Symmetrizing and Normalizing Matrix...", 0.5);
            ps.fillStartColor = Color.AZURE;
            ps.fillEndColor = Color.LIME;
            ps.innerStrokeColor = Color.AZURE;
            ps.outerStrokeColor = Color.LIME;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_TEXT_CONSOLE, "Symmetrizing and Normalizing Matrix...", true));            
        });
        double[][] symmetricDistanceMatrix = SuperMDS.ensureSymmetricDistanceMatrix(originalData);
        double[][] normalizedDistanceMatrix = SuperMDSHelper.normalizeDistancesParallel(symmetricDistanceMatrix);
        String totesTime2 = totalTimeString(startTime);
        LOG.info(totesTime2);
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_TEXT_CONSOLE, totesTime2, true));            
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_TEXT_CONSOLE, "Running MDS: " + params.mode.name(), true));            
        });        
        LOG.info("Running MDS: " + params.mode.name());
        startTime = System.nanoTime();
        double[][] mdsEmbedding = SuperMDS.runMDS(normalizedDistanceMatrix, params);
        String totesTime3 = totalTimeString(startTime);
        LOG.info(totesTime3);
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_TEXT_CONSOLE, totesTime3, true));            
        });        
        
        if(isComputeMetrics()) {
            computeStressMetrics(normalizedDistanceMatrix, weights, mdsEmbedding);
        }
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Converting to FeatureCollection...", 0.5);
            ps.fillStartColor = Color.CYAN;
            ps.fillEndColor = Color.NAVY;
            ps.innerStrokeColor = Color.CYAN;
            ps.outerStrokeColor = Color.SILVER;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
        });
        LOG.info("mapping projected MDS embeddings back to FeatureVectors...");
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

    private void computeStressMetrics(double[][] normalizedDistanceMatrix, double[][] weights, double[][] embeddings) {
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Computing Error and Stress Metrics...", 0.5);
            ps.fillStartColor = Color.CYAN;
            ps.fillEndColor = Color.NAVY;
            ps.innerStrokeColor = Color.CYAN;
            ps.outerStrokeColor = Color.SILVER;
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.UPDATE_BUSY_INDICATOR, ps));
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_TEXT_CONSOLE, "Computing Error and Stress Metrics...", true));
        });
        LOG.info("Computing Error and Stress Metrics...");
        
////        System.out.println("Computing Error and Stress Metrics for Classical MDS...");
////        SuperMDSValidator.computeStressMetricsClassic(rawInputData, classicalEmbeddings);        
////        
////        System.out.printf("Results for Landmark MDS on synthetic data (%d points, %dD → %dD):\n",
////                nPoints, inputDim, outputDim);
////        SuperMDSValidator.computeStressMetricsClassic(rawInputData, landmarkEmbeddings);
//
//        System.out.printf("Results for Approximate Landmark MDS on synthetic data (%d points, %dD → %dD):\n",
//                nPoints, inputDim, outputDim);
//        SuperMDSValidator.computeStressMetricsClassic(rawInputData, approximateLandmarkEmbeddings);

        //System.out.println("Computing Error and Stress Metrics...");
        double [][] reconstructed = SuperMDSHelper.computeReconstructedDistances(embeddings);
        double maxError = SuperMDSValidator.maxDistanceError(normalizedDistanceMatrix, reconstructed);
        double mse = SuperMDSValidator.meanSquaredError(normalizedDistanceMatrix, reconstructed);
        double rawStress = SuperMDSValidator.rawStress(normalizedDistanceMatrix, reconstructed, weights);

        StringBuilder sb = new StringBuilder();
        try (Formatter formatter = new Formatter(sb)) {
            formatter.format("Results for SMACOF MDS on data (%d points, %dD → %dD):\n",
                normalizedDistanceMatrix.length, normalizedDistanceMatrix[0].length, params.outputDim);
            
            formatter.format("Max error: %.6f\n", maxError);
            formatter.format("MSE:       %.6f\n", mse);
            formatter.format("Raw stress: %.6f\n", rawStress);
            
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new ApplicationEvent(
                        ApplicationEvent.SHOW_TEXT_CONSOLE, sb.toString(),true));
            });
            LOG.info(sb.toString());
            
            SuperMDSValidator.StressMetrics smacofStressMetrics =
                    SuperMDSValidator.computeStressMetrics(normalizedDistanceMatrix, reconstructed);
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(new ApplicationEvent(
                        ApplicationEvent.SHOW_TEXT_CONSOLE, smacofStressMetrics.toString(), true));
            });
            LOG.info(smacofStressMetrics.toString());
        }
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

    /**
     * @return the computeMetrics
     */
    public boolean isComputeMetrics() {
        return computeMetrics;
    }

    /**
     * @param computeMetrics the computeMetrics to set
     */
    public void setComputeMetrics(boolean computeMetrics) {
        this.computeMetrics = computeMetrics;
    }
}
