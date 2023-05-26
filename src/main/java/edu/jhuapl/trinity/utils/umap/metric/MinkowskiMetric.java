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

/**
 * Minkowski distance. Singleton defaults to a power of 1.5 to be a blend between
 * Manhattan and Euclidean distances
 */
public class MinkowskiMetric extends Metric {

    private final double mPower;
    public static final MinkowskiMetric SINGLETON = new MinkowskiMetric(1.5);

    public MinkowskiMetric(final double power) {
        super(false);
        mPower = power;
    }

    @Override
    public float distance(final float[] x, final float[] y) {
        // D(x, y) = \left(\sum_i |x_i - y_i|^p\right)^{\frac{1}{p}}
        double result = 0.0;
        for (int i = 0; i < x.length; ++i) {
            result += Math.pow(Math.abs(x[i] - y[i]), mPower);
        }
        return (float) Math.pow(result, 1 / mPower);
    }
}
