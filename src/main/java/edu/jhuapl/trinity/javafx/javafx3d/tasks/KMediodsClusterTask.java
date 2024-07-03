package edu.jhuapl.trinity.javafx.javafx3d.tasks;

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

import com.clust4j.algo.KMedoids;
import com.clust4j.algo.KMedoidsParameters;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent.ProjectionConfig;
import edu.jhuapl.trinity.utils.Utils;
import javafx.application.Platform;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;

/**
 * @author Sean Phillips
 */
public class KMediodsClusterTask extends ClusterTask {

    ProjectionConfig pc;
    double [][] observations;
    
    public KMediodsClusterTask(Scene scene, PerspectiveCamera camera,
        double projectionScalar, double[][] observations, ProjectionConfig pc) {
        super(scene, camera);
        setProjectionScalar(projectionScalar);
        this.observations = observations;
        this.pc = pc;        
    }

    @Override
    protected void processTask() throws Exception {
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Fitting KMedoids from Observations...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });
        System.out.print("KMedoids fit... ");
        long startTime = System.nanoTime();
        Array2DRowRealMatrix obsMatrix = new Array2DRowRealMatrix(observations);
        KMedoids kmedoids = new KMedoidsParameters(pc.components)
            .setMaxIter(pc.maxIterations)
            .setConvergenceCriteria(pc.toleranceConvergence)
//                    .setInitializationStrategy(AbstractCentroidClusterer.InitializationStrategy.AUTO)
//                    .setMetric(new CauchyKernel())
            .setForceParallel(pc.forceParallel)
            .setVerbose(pc.verbose)
            .fitNewModel(obsMatrix);
        final int[] labels = kmedoids.getLabels();
        final int clusters = kmedoids.getK();
        Utils.printTotalTime(startTime);
        System.out.println("===============================================");

        System.out.print("Converting clusters to Manifold Geometry... ");
        startTime = System.nanoTime();
        convertToManifoldGeometry(observations, labels, clusters, "KMedoids Cluster ");
        Utils.printTotalTime(startTime);
        System.out.println("===============================================");        
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(
                new CommandTerminalEvent("Completed KMedoids Fit and Manifold Geometry Task.",
                    new Font("Consolas", 20), Color.GREEN));
        });        
    }
}
