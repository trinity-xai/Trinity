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

import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent.ProjectionConfig;
import edu.jhuapl.trinity.utils.Utils;
import edu.jhuapl.trinity.utils.clustering.Cluster;
import edu.jhuapl.trinity.utils.clustering.GaussianMixtureComponent;
import edu.jhuapl.trinity.utils.clustering.GaussianMixtureModel;
import edu.jhuapl.trinity.utils.clustering.Point;
import javafx.application.Platform;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Pair;
import org.fxyz3d.geometry.Point3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class ExMaxClusterTask extends ClusterTask {
    private static final Logger LOG = LoggerFactory.getLogger(ExMaxClusterTask.class);

    ProjectionConfig pc;
    double[][] observations;

    public ExMaxClusterTask(Scene scene, PerspectiveCamera camera,
                            double projectionScalar, double[][] observations, ProjectionConfig pc) {
        super(scene, camera);
        setProjectionScalar(projectionScalar);
        this.pc = pc;
        this.observations = observations;
    }

    @Override
    protected void processTask() throws Exception {
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Fitting GMM from Observations...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });
        LOG.info("Expectation Maximization... ");
        long startTime = System.nanoTime();
        boolean diagonal = pc.covariance == ProjectionConfig.COVARIANCE_MODE.DIAGONAL;
        GaussianMixtureModel gmm = GaussianMixtureModel.fit(pc.components, observations, diagonal);
        Utils.printTotalTime(startTime);
        LOG.info("Components found: {}", gmm.components.length);
        LOG.info("Mapping observations to clusters by component probability... ");
        startTime = System.nanoTime();
        ArrayList<Cluster> clusters = new ArrayList<>();
        int i = 0;
        for (GaussianMixtureComponent c : gmm.components) {
            LOG.info("After GMM Fit Centroid {}: {}", i, Arrays.toString(c.distribution.mu));
            clusters.add(new Cluster(observations[0].length));
            i++;
        }
        ArrayList<Double> maxPostProbList = new ArrayList<>(observations.length);
        for (int dataIndex = 0; dataIndex < observations.length; dataIndex++) {
            Pair<Integer, Double> maxPostProb = gmm.maxPostProb(observations[dataIndex]);
            maxPostProbList.add(maxPostProb.getValue());
//                    int component = gmm.map(observations[dataIndex]);
            clusters.get(maxPostProb.getKey())
                .addPointToCluster(dataIndex, new Point(observations[dataIndex]));
        }
        Collections.sort(maxPostProbList);
        double min = Collections.min(maxPostProbList);
        double max = Collections.max(maxPostProbList);
//                maxPostProbList.stream().forEach(m ->
//                    System.out.println(DataUtils.normalize(m, min, max))
//                );
        Utils.printTotalTime(startTime);
        LOG.info("Generating Hulls from Clusters... ");
        startTime = System.nanoTime();
        int index = 0;
        for (Cluster cluster : clusters) {
            if (cluster.getClusterPoints().size() >= 4) {
                String label = "GMM Cluster " + index;
                List<Point3D> points = cluster.getClusterPoints().stream()
                    .map((Point t) -> new Point3D(
                        t.getPosition()[0] * getProjectionScalar(),
                        t.getPosition()[1] * -getProjectionScalar(),
                        t.getPosition()[2] * getProjectionScalar()))
                    .toList();
                createManifold(points, label);
            } else {
                LOG.info("Cluster has less than 4 points");
            }
            index++;
        }

        Utils.printTotalTime(startTime);
        LOG.info("===============================================");
        //System.out.println("EmDriver Results : " + emDriver.learnedModel.toString());
        Platform.runLater(() -> {
            scene.getRoot().fireEvent(
                new CommandTerminalEvent("Completed KMeans Fit and Manifold Geometry Task.",
                    new Font("Consolas", 20), Color.GREEN));
        });
    }
}
