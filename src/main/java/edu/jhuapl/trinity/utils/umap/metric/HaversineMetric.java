/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
package edu.jhuapl.trinity.utils.umap.metric;

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

/**
 * Haversine distance.
 */
public final class HaversineMetric extends Metric {

    /**
     * Haversine distance.
     */
    public static final HaversineMetric SINGLETON = new HaversineMetric();

    private HaversineMetric() {
        super(false);
    }

    @Override
    public float distance(final float[] x, final float[] y) {
        if (x.length != 2) {
            throw new IllegalArgumentException("haversine is only defined for 2 dimensional data");
        }
        final double sinLat = Math.sin(0.5 * (x[0] - y[0]));
        final double sinLong = Math.sin(0.5 * (x[1] - y[1]));
        final double result = Math.sqrt(sinLat * sinLat + Math.cos(x[0]) * Math.cos(y[0]) * sinLong * sinLong);
        return (float) (2 * Math.asin(result));
    }
}
