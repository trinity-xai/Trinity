package edu.jhuapl.trinity.utils.clustering;

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

import com.clust4j.algo.KMeans;
import com.clust4j.algo.KMeansParameters;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;

/**
 * Gaussian mixture model that provides methods for fitting to observation
 * samples using an Expectation Maximization process seeded by KMeans++.
 */
public class GaussianMixtureModel extends GaussianMixture {
    private static Random rando = new Random();

    /**
     * @param components the Gaussian distributions.
     */
    public GaussianMixtureModel(GaussianMixtureComponent... components) {
        this(0.0, 1, components);
    }

    /**
     * @param components Gaussian distributions.
     * @param L          the log-likelihood.
     * @param n          the number of samples to fit the distribution.
     */
    private GaussianMixtureModel(double L, int n, GaussianMixtureComponent... components) {
        super(L, n, components);
    }

    /**
     * Fits the Gaussian mixture model with the EM algorithm.
     *
     * @param data the training data.
     * @param k    the number of components.
     * @return the distribution.
     */
    public static GaussianMixtureModel fit(int k, double[][] data) {
        return fit(k, data, false);
    }

    /**
     * Fits the Gaussian mixture model with the EM algorithm.
     *
     * @param data     the training data.
     * @param k        the number of components.
     * @param diagonal true if the components have diagonal covariance matrix.
     * @return the distribution.
     */
    public static GaussianMixtureModel fit(int k, double[][] data, boolean diagonal) {
        int dimensions = data.length;
        int sampleCount = data[0].length;
        double[] mu = ClusterUtils.colMeans(data);
        double[] componentCentroid = mu;

        double[] variance = null;
        RealMatrix cov = null;
        GaussianDistribution gaussian;
        if (diagonal) {
            variance = new double[sampleCount];
            for (double[] x : data) {
                for (int j = 0; j < sampleCount; j++) {
                    variance[j] += (x[j] - mu[j]) * (x[j] - mu[j]);
                }
            }

            int n1 = dimensions - 1;
            for (int j = 0; j < sampleCount; j++) {
                variance[j] /= n1;
            }
            gaussian = new GaussianDistribution(componentCentroid, variance);
        } else {
            cov = MatrixUtils.createRealMatrix(ClusterUtils.cov(data, mu));
            gaussian = new GaussianDistribution(componentCentroid, cov);
        }

        GaussianMixtureComponent[] components = new GaussianMixtureComponent[k];
        components[0] = new GaussianMixtureComponent(1.0 / k, gaussian);

        // We use the kmeans++ algorithm to find the initial centers.
        // Initially, all components have same covariance matrix.
        double[] currentDistance = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            currentDistance[i] = Double.MAX_VALUE;
        }

        System.out.println("Finding initial centroids using KMeans++...");
//        // pick the next center..
//        //start at index 1 because we already computed
//        //the first component above to get the common variance
//        for (int i = 1; i < k; i++) {
//            // Loop over the samples and compare them to the most recent center.  Store
//            // the distance from each sample to its closest center in scores.
//            for (int j = 0; j < dimensions; j++) {
//                // compute the distance between this sample and the current center
//                double dimensionalDistance = ClusterUtils.squaredDistance(data[j], componentCentroid);
//                if (dimensionalDistance < currentDistance[j]) {
//                    currentDistance[j] = dimensionalDistance;
//                }
//            }
//            //sum the distances across the dimensions and add a bit of random jitter
//            double cutoff = rando.nextDouble() * ClusterUtils.sum(currentDistance);
//            double cost = 0.0;
//            int cutoffIndex = 0;
//            for (; cutoffIndex < dimensions; cutoffIndex++) {
//                //sum the distances at each dimension as a cumulative cost
//                cost += currentDistance[cutoffIndex];
//                //if the cumulative cost is greater than the threshold break out
//                //we will use this as the data index for the currentCentroid
//                if (cost >= cutoff)
//                    break;
//            }
//            componentCentroid = data[cutoffIndex];
//            gaussian = diagonal ? new GaussianDistribution(componentCentroid, variance) : new GaussianDistribution(componentCentroid, cov);
//            components[i] = new GaussianMixtureComponent(1.0 / k, gaussian);
//            //System.out.println("Centroid " + i + ": " + Arrays.toString(componentCentroid));
//        }


//        //@DEBUG SMP
//        Point[] centroids = KmeansPlusPlus.kmeansPlusPlus(k, data);
//        for (int i = 0; i < centroids.length; i++) {
//            gaussian = diagonal ? new GaussianDistribution(centroids[i].getPosition(), variance)
//                : new GaussianDistribution(centroids[i].getPosition(), cov);
//            components[i] = new GaussianMixtureComponent(1.0 / k, gaussian);
//        }
        Array2DRowRealMatrix obsMatrix = new Array2DRowRealMatrix(data);
        KMeans kmeans = new KMeansParameters(k)
                    .setMaxIter(100)
//                    .setConvergenceCriteria(0.01)
//                    .setInitializationStrategy(AbstractCentroidClusterer.InitializationStrategy.AUTO)
//                    .setMetric(new CauchyKernel())
            .fitNewModel(obsMatrix);
        final int[] labels = kmeans.getLabels();
        final int clusters = kmeans.getK();

        for (int i = 0; i < kmeans.getCentroids().size(); i++) {
            Point center = new Point(kmeans.getCentroids().get(i));
            gaussian = diagonal ? new GaussianDistribution(center.getPosition(), variance)
                : new GaussianDistribution(center.getPosition(), cov);
            components[i] = new GaussianMixtureComponent(1.0 / k, gaussian);
        }
        GaussianMixture model = fit(data, components);
        return new GaussianMixtureModel(model.logLikelihood, data.length, model.components);
    }

    /**
     * Fits the Gaussian mixture model with the EM algorithm.
     * The number of components will be selected by BIC.
     *
     * @param data the training data.
     * @return the distribution.
     */
    public static GaussianMixtureModel fit(double[][] data) {
        return fit(data, false);
    }

    /**
     * Fits the Gaussian mixture model with the EM algorithm.
     * The number of components will be selected by BIC.
     *
     * @param data     the training data.
     * @param diagonal true if the components have diagonal covariance matrix.
     * @return the distribution.
     */
    public static GaussianMixtureModel fit(double[][] data, boolean diagonal) {
        if (data.length < 20) {
            throw new IllegalArgumentException("Too few samples.");
        }

        GaussianMixtureModel mixture = new GaussianMixtureModel(new GaussianMixtureComponent(1.0, GaussianDistribution.fit(data, diagonal)));
        double bic = mixture.bic(data);
        Logger.getLogger(GaussianMixtureModel.class.getName()).log(Level.FINE,
            String.format("The BIC of %s = %.4f", mixture, bic));

        for (int k = 2; k < data.length / 20; k++) {
            GaussianMixture model = fit(k, data);
            Logger.getLogger(GaussianMixtureModel.class.getName()).log(Level.FINE,
                String.format("The BIC of %s = %.4f", model, model.bic));

            if (model.bic <= bic) break;

            mixture = new GaussianMixtureModel(model.logLikelihood, data.length, model.components);
            bic = model.bic;
        }

        return mixture;
    }
}
