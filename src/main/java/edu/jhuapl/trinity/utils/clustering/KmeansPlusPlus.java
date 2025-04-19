package edu.jhuapl.trinity.utils.clustering;

import java.util.Random;

/**
 * @author Sean Phillips
 */
public enum KmeansPlusPlus {
    INSTANCE;

    /**
     * @param k    number of clusters
     * @param data observation samples
     * @return array of Points, each one a centroid, ordered from 0 to k
     */
    public static Point[] kmeansPlusPlus(int k, double[][] data) {
        Random rando = new Random();
        int dimensions = data.length;
        int sampleCount = data[0].length;
//        double[] currentCentroid = data[rando.nextInt(dimensions)];
        Point[] centroids = new Point[k];
        //the kmeans++ algorithm to find the  centers.
        double[] currentDistance = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            currentDistance[i] = Double.MAX_VALUE;
        }

        // pick the next center
        for (int i = 0; i < k; i++) {
            double[] currentCentroid = data[rando.nextInt(dimensions)];
            int maxIterations = 500;
            int currentIteration = 0;
            double previousCost = 0.0;
            double costThreshold = 1e-8;
            while (currentIteration < maxIterations) {
                // Loop over the samples and compare them to the most recent center.
                // store the distance from each sample to its closest center in scores.
                for (int j = 0; j < dimensions; j++) {
                    // compute the distance between this sample and the current center
                    double dimensionalDistance = ClusterUtils.squaredDistance(data[j], currentCentroid);
                    if (dimensionalDistance < currentDistance[j]) {
                        currentDistance[j] = dimensionalDistance;
                    }
                }
                //sum the distances across the dimensions and add a bit of random jitter
                //double cutoff = rando.nextDouble() * ClusterUtils.sum(currentDistance);
                double cost = ClusterUtils.sum(currentDistance);
                double difference = previousCost - cost;
                //if the cumulative cost difference is less than the threshold break out
                //ie... do NOT assign this as the best known centroid
                if (difference <= costThreshold) {
                    break;
                }
                //current costs across dimensions are less than previous cutoff.
                //Assign the new centroid
                for (int j = 0; j < dimensions; j++) {
                    currentCentroid[j] = currentCentroid[j] + currentDistance[j] / 2.0;
                }
                currentIteration++;
            }
            centroids[i] = new Point(currentCentroid);
        }
        return centroids;
    }
}
