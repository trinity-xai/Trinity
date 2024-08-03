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

import com.clust4j.algo.AffinityPropagation;
import com.clust4j.algo.AffinityPropagationParameters;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent.ProjectionConfig;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import edu.jhuapl.trinity.utils.Utils;
import javafx.application.Platform;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Pair;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * @author Sean Phillips
 */
public class AffinityClusterTask extends ClusterTask {

    ProjectionConfig pc;
    double[][] observations;

    public AffinityClusterTask(Scene scene, PerspectiveCamera camera,
                               double projectionScalar, double[][] observations, ProjectionConfig pc) {
        super(scene, camera);
        setProjectionScalar(projectionScalar);
        this.pc = pc;
        this.observations = observations;
    }

    @Override
    protected void processTask() throws Exception {
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Fitting Affinity Propagation from Observations...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });
        Array2DRowRealMatrix obsMatrix = null;

        double[][] shuffledObservations = null;

        if (observations.length > 1000) {
            System.out.println("Total observations count too large for AffinityPropagation.");
            System.out.println("Using random Resovoir sample of size 1000...");
            Double[][] boxedObservations = AnalysisUtils.boxDoubleArrays(observations);
            ArrayList<Pair<Double[], Integer>> pairList = new ArrayList<>();
            for (int i = 0; i < observations.length; i++) {
                pairList.add(new Pair<>(boxedObservations[i], i));
            }
            Collections.shuffle(pairList);
            shuffledObservations = new double[1000][observations[0].length];
            for (int i = 0; i < 1000; i++) {
                Double[] dd = pairList.get(i).getKey();
                double[] unboxed = Stream.of(dd).mapToDouble(Double::doubleValue).toArray();
                shuffledObservations[i] = unboxed;
            }
            obsMatrix = new Array2DRowRealMatrix(shuffledObservations);
        } else {
            obsMatrix = new Array2DRowRealMatrix(observations);
        }

        System.out.print("Affinity Propagation fit... ");
        long startTime = System.nanoTime();
        try {
            AffinityPropagation aff = new AffinityPropagationParameters()
                .setDampingFactor(pc.epsilonAlpha)
                .setMinChange(pc.toleranceConvergence)
                .setIterBreak(AffinityPropagation.DEF_ITER_BREAK) //@TODO SMP add gui control and config support
                .setMaxIter(pc.maxIterations)
                .setForceParallel(pc.forceParallel)
                .setVerbose(pc.verbose)
                .useGaussianSmoothing(AffinityPropagation.DEF_ADD_GAUSSIAN_NOISE) //@TODO SMP add GUI and config support
//            .setMetric(new ExponentialKernel(0.1))
                .fitNewModel(obsMatrix);
            final int[] labels = aff.getLabels();
            final int clusters = aff.getNumberOfIdentifiedClusters();

            Utils.printTotalTime(startTime);
            System.out.println("===============================================");

            System.out.print("Converting clusters to Manifold Geometry... ");
            startTime = System.nanoTime();
            if (observations.length > 1000 && null != shuffledObservations)
                convertToManifoldGeometry(shuffledObservations, labels, clusters, "Affinity Propagation Cluster ");
            else
                convertToManifoldGeometry(observations, labels, clusters, "Affinity Propagation Cluster ");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Utils.printTotalTime(startTime);
        System.out.println("===============================================");
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(
                new CommandTerminalEvent("Completed Affinity Propagation Fit and Manifold Geometry Task.",
                    new Font("Consolas", 20), Color.GREEN));
        });
    }
}
