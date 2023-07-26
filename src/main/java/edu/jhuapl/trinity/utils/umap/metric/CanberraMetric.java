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
 * Canberra distance.
 */
public final class CanberraMetric extends Metric {

    /**
     * Canberra distance.
     */
    public static final CanberraMetric SINGLETON = new CanberraMetric();

    private CanberraMetric() {
        super(false);
    }

    @Override
    public float distance(final float[] x, final float[] y) {
        float result = 0;
        for (int i = 0; i < x.length; ++i) {
            final float denominator = Math.abs(x[i]) + Math.abs(y[i]);
            if (denominator > 0) {
                result += Math.abs(x[i] - y[i]) / denominator;
            }
        }
        return result;
    }
}
