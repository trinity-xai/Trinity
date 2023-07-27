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
 * Mahalanobis distance.
 */
public class MahalanobisMetric extends Metric {

    public static final MahalanobisMetric SINGLETON = new MahalanobisMetric();

    private float[][] mV; //inverse convariance matrix of independent variables (each column)

    public MahalanobisMetric() {
        super(false);
    }

    public MahalanobisMetric(final float[][] v) {
        this();
        mV = v;
    }

    @Override
    public float distance(final float[] x, final float[] y) {
        float result = 0;
        final float[] diff = new float[x.length];
        for (int i = 0; i < x.length; ++i) {
            diff[i] = x[i] - y[i];
        }
        for (int i = 0; i < x.length; ++i) {
            double tmp = 0.0;
            for (int j = 0; j < x.length; ++j) {
                tmp += mV[i][j] * diff[j];
            }
            result += tmp * diff[i];
        }
        return (float) Math.sqrt(result);
    }
}
