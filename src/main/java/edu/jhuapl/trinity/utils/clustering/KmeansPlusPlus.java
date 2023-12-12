package edu.jhuapl.trinity.utils.clustering;

/*-
 * #%L
 * trinity-2023.11.27
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

/**
 *
 * @author phillsm1
 */
public enum KmeansPlusPlus {
    INSTANCE;
    
    public static void kmeansPlusPlus(int k, double[][] data) {
        Random rando = new Random();
        int dimensions = data.length;
        int sampleCount = data[0].length;        
        double[] currentCentroid = data[rando.nextInt(dimensions)];
        
        //the kmeans++ algorithm to find the  centers.
        double[] currentDistance = new double[dimensions];
        for (int i = 0; i < dimensions; i++) {
            currentDistance[i] = Double.MAX_VALUE;
        }

        // pick the next center
        for (int i = 1; i < k; i++) {
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
            double cutoff = rando.nextDouble() * ClusterUtils.sum(currentDistance);
            double cost = 0.0;
            int index = 0;
            for (; index < dimensions; index++) {
                //sum the distances at each dimension as a cumulative cost
                cost += currentDistance[index];
                //if the cumulative cost is greater than the threshold break out
                //ie... do NOT assign this as the best known centroid
                if (cost >= cutoff) break;
            }
            //current costs across dimensions are less than previous cutoff.
            //Assign this data point as the new centroid
            currentCentroid = data[index];
        }
        
    }
}
