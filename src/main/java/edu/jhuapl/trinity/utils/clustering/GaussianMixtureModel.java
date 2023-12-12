package edu.jhuapl.trinity.utils.clustering;

/*-
 * #%L
 * trinity-2023.12.05
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

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

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
     * @param L the log-likelihood.
     * @param n the number of samples to fit the distribution.
     */
    private GaussianMixtureModel(double L, int n, GaussianMixtureComponent... components) {
        super(L, n, components);
    }

    /**
     * Fits the Gaussian mixture model with the EM algorithm.
     * @param data the training data.
     * @param k the number of components.
     * @return the distribution.
     */
    public static GaussianMixtureModel fit(int k, double[][] data) {
        return fit(k, data, false);
    }

    /**
     * Fits the Gaussian mixture model with the EM algorithm.
     * @param data the training data.
     * @param k the number of components.
     * @param diagonal true if the components have diagonal covariance matrix.
     * @return the distribution.
     */
    public static GaussianMixtureModel fit(int k, double[][] data, boolean diagonal) {
        if (k < 2) {
            throw new IllegalArgumentException("Invalid number of components in the mixture.");
        }

        int n = data.length;
        int d = data[0].length;
        double[] mu = ClusterUtils.colMeans(data);

        
        double[] centroid = data[rando.nextInt(n)];
        double[] variance = null;
        RealMatrix cov = null;
        GaussianDistribution gaussian;
        if (diagonal) {
            variance = new double[d];
            for (double[] x : data) {
                for (int j = 0; j < d; j++) {
                    variance[j] += (x[j] - mu[j]) * (x[j] - mu[j]);
                }
            }

            int n1 = n - 1;
            for (int j = 0; j < d; j++) {
                variance[j] /= n1;
            }
            gaussian = new GaussianDistribution(centroid, variance);
        } else {
            cov = MatrixUtils.createRealMatrix(ClusterUtils.cov(data, mu));
            gaussian = new GaussianDistribution(centroid, cov);
        }

        GaussianMixtureComponent[] components = new GaussianMixtureComponent[k];
        components[0] = new GaussianMixtureComponent(1.0 / k, gaussian);

        // We use the kmeans++ algorithm to find the initial centers.
        // Initially, all components have same covariance matrix.
        double[] D = new double[n];
        for (int i = 0; i < n; i++) {
            D[i] = Double.MAX_VALUE;
        }

        // pick the next center
        for (int i = 1; i < k; i++) {
            // Loop over the samples and compare them to the most recent center.  Store
            // the distance from each sample to its closest center in scores.
            for (int j = 0; j < n; j++) {
                // compute the distance between this sample and the current center
                double dist = ClusterUtils.squaredDistance(data[j], centroid);
                if (dist < D[j]) {
                    D[j] = dist;
                }
            }

            double cutoff = rando.nextDouble() * ClusterUtils.sum(D);
            double cost = 0.0;
            int index = 0;
            for (; index < n; index++) {
                cost += D[index];
                if (cost >= cutoff) break;
            }

            centroid = data[index];
            gaussian = diagonal ? new GaussianDistribution(centroid, variance) : new GaussianDistribution(centroid, cov);
            components[i] = new GaussianMixtureComponent(1.0 / k, gaussian);
        }

        GaussianMixture model = fit(data, components);
        return new GaussianMixtureModel(model.L, data.length, model.components);
    }

    /**
     * Fits the Gaussian mixture model with the EM algorithm.
     * The number of components will be selected by BIC.
     * @param data the training data.
     * @return the distribution.
     */
    public static GaussianMixtureModel fit(double[][] data) {
        return fit(data, false);
    }

    /**
     * Fits the Gaussian mixture model with the EM algorithm.
     * The number of components will be selected by BIC.
     * @param data the training data.
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

            mixture = new GaussianMixtureModel(model.L, data.length, model.components);
            bic = model.bic;
        }

        return mixture;
    }
}
