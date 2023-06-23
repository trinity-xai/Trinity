/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
package edu.jhuapl.trinity.utils.umap.metric;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
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

import java.util.HashMap;
import java.util.Map;

/**
 * Definition of metrics. Individual subclasses implement specific metrics.
 * A convenience function to select metrics by a string name is also provided.
 *
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 */
public abstract class Metric {

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
    public abstract float distance(final float[] x, final float[] y);

    /**
     * Is this an angular metric.
     *
     * @return true iff this metric is angular.
     */
    public boolean isAngular() {
        return mIsAngular;
    }

    private static Map<String, Metric> sMETRICS = null;

    /**
     * Retrieve a metric by name.
     *
     * @param name name of metric
     * @return metric
     */
    public static Metric getMetric(final String name) {
        if (sMETRICS == null) {
            sMETRICS = new HashMap<>();
            sMETRICS.put("euclidean", EuclideanMetric.SINGLETON);
            sMETRICS.put("reducedeuclidean", ReducedEuclideanMetric.SINGLETON);
            sMETRICS.put("l2", EuclideanMetric.SINGLETON);
            sMETRICS.put("manhattan", ManhattanMetric.SINGLETON);
            sMETRICS.put("l1", ManhattanMetric.SINGLETON);
            sMETRICS.put("taxicab", ManhattanMetric.SINGLETON);
            sMETRICS.put("chebyshev", ChebyshevMetric.SINGLETON);
            sMETRICS.put("linfinity", ChebyshevMetric.SINGLETON);
            sMETRICS.put("linfty", ChebyshevMetric.SINGLETON);
            sMETRICS.put("linf", ChebyshevMetric.SINGLETON);
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
            sMETRICS.put("sokalmichener", SokalMichenerMetric.SINGLETON);
            sMETRICS.put("yule", YuleMetric.SINGLETON);
        }

        final Metric m = sMETRICS.get(name.toLowerCase());
        if (m == null) {
            throw new IllegalArgumentException("Unknown metric: " + name);
        }
        return m;
    }
}
