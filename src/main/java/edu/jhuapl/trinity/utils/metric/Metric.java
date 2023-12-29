/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
package edu.jhuapl.trinity.utils.metric;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Definition of metrics. Individual subclasses implement specific metrics.
 * A convenience function to select metrics by a string name is also provided.
 *
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 * @author Sean Phillips
 */
public abstract class Metric {

    private double mThreshold = 0.001;
    private final boolean mIsAngular;

    Metric(final boolean isAngular) {
        mIsAngular = isAngular;
    }

    /**
     * Distance metric.
     *
     * @param x first point
     * @param y second point
     * @return distance between the points
     */
    public abstract double distance(final double[] x, final double[] y);

    /**
     * Is this an angular metric.
     *
     * @return true iff this metric is angular.
     */
    public boolean isAngular() {
        return mIsAngular;
    }
    /**
     * Set optional Threshold.
     *
     * @param threshold the threshold value used by some metrics
     */
    public void setThreshold(double threshold) {
        mThreshold = threshold;
    }
    public double getThreshold() {
        return mThreshold;
    }
    
    private static Map<String, Metric> sMETRICS = null;

    public static List<String> getMetricNames() {
        if (sMETRICS == null) generateDefaultMetrics();
        ArrayList<String> metricKeys = new ArrayList<>(sMETRICS.keySet());
        Collections.sort(metricKeys);
        return metricKeys;
    }

    /**
     * Retrieve a metric by name.
     *
     * @param name name of metric
     * @return metric
     */
    private static void generateDefaultMetrics() {
        if (sMETRICS == null) {
            sMETRICS = new HashMap<>();
            sMETRICS.put("angular", AngularMetric.SINGLETON);
            sMETRICS.put("euclidean", EuclideanMetric.SINGLETON);
            sMETRICS.put("reducedeuclidean", ReducedEuclideanMetric.SINGLETON);
            sMETRICS.put("manhattan", ManhattanMetric.SINGLETON);
            sMETRICS.put("chebyshev", ChebyshevMetric.SINGLETON);
            //@TODO SMP Get this working with a inverse covariance matrix parameter
            //sMETRICS.put("mahalanobis", MahalanobisMetric.SINGLETON);
            sMETRICS.put("canberra", CanberraMetric.SINGLETON);
            sMETRICS.put("minkowski", MinkowskiMetric.SINGLETON);
            sMETRICS.put("cosine", CosineMetric.SINGLETON);
            sMETRICS.put("correlation", CorrelationMetric.SINGLETON);
            sMETRICS.put("haversine", HaversineMetric.SINGLETON);
            sMETRICS.put("braycurtis", BrayCurtisMetric.SINGLETON);
            sMETRICS.put("hamming", HammingMetric.SINGLETON);
            sMETRICS.put("jaccard", JaccardMetric.SINGLETON);
            sMETRICS.put("dice", DiceMetric.SINGLETON);
            sMETRICS.put("matching", MatchingMetric.SINGLETON);
            sMETRICS.put("kulsinski", KulsinskiMetric.SINGLETON);
            sMETRICS.put("rogerstanimoto", RogersTanimotoMetric.SINGLETON);
            sMETRICS.put("russellrao", RussellRaoMetric.SINGLETON);
            sMETRICS.put("sokalsneath", SokalSneathMetric.SINGLETON);
            sMETRICS.put("yule", YuleMetric.SINGLETON);
            sMETRICS.put("yulethreshold", YuleThresholdMetric.SINGLETON);
            sMETRICS.put("yulehighbandthreshold", YuleHighBandThresholdMetric.SINGLETON);
        }
    }

    public static Metric getMetric(final String name) {
        if (sMETRICS == null) generateDefaultMetrics();

        final Metric m = sMETRICS.get(name.toLowerCase());
        if (m == null) {
            throw new IllegalArgumentException("Unknown metric: " + name);
        }
        return m;
    }
}
