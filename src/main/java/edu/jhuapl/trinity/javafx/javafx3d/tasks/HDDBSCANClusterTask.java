package edu.jhuapl.trinity.javafx.javafx3d.tasks;

import com.clust4j.algo.HDBSCAN;
import com.clust4j.algo.HDBSCANParameters;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent.ProjectionConfig;
import edu.jhuapl.trinity.utils.Utils;
import javafx.application.Platform;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 */
public class HDDBSCANClusterTask extends ClusterTask {
    private static final Logger LOG = LoggerFactory.getLogger(HDDBSCANClusterTask.class);

    ProjectionConfig pc;
    double[][] observations;

    public HDDBSCANClusterTask(Scene scene, PerspectiveCamera camera,
                               double projectionScalar, double[][] observations, ProjectionConfig pc) {
        super(scene, camera);
        setProjectionScalar(projectionScalar);
        this.pc = pc;
        this.observations = observations;
    }

    @Override
    protected void processTask() throws Exception {
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Fitting HDDBSCAN from Observations...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });
        LOG.info("HDBSCAN fit... ");
        long startTime = System.nanoTime();
        Array2DRowRealMatrix obsMatrix = new Array2DRowRealMatrix(observations);
        HDBSCAN hdb = new HDBSCANParameters()
            .setAlpha(pc.epsilonAlpha)
            .setMinPts(pc.minimumPoints)
            .setMinClustSize(pc.minimumClusterSize)
            .setLeafSize(pc.minimumLeafSize)
            .setForceParallel(pc.forceParallel)
            .setVerbose(pc.verbose)
            .fitNewModel(obsMatrix);
        final int[] labels = hdb.getLabels();
        final int clusters = hdb.getNumberOfIdentifiedClusters();
        Utils.printTotalTime(startTime);
        LOG.info("\n===============================================\n");

        LOG.info("Converting clusters to Manifold Geometry... ");
        startTime = System.nanoTime();
        convertToManifoldGeometry(observations, labels, clusters, "HDDBSCAN Cluster ");
        Utils.printTotalTime(startTime);
        LOG.info("\n===============================================\n");
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(
                new CommandTerminalEvent("Completed HDDBSCAN Fit and Manifold Geometry Task.",
                    new Font("Consolas", 20), Color.GREEN));
        });
    }
}
