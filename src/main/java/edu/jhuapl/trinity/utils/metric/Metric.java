/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.metric;

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
            sMETRICS.put("yuleepsilon", YuleEpsilonMetric.SINGLETON);
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
