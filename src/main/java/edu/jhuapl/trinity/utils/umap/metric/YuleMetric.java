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
 * Yule distance.
 */
public final class YuleMetric extends Metric {

    /**
     * Yule distance.
     */
    public static final YuleMetric SINGLETON = new YuleMetric();

    private YuleMetric() {
        super(false);
    }

    @Override
    public float distance(final float[] x, final float[] y) {
        int numTrueTrue = 0;
        int numTrueFalse = 0;
        int numFalseTrue = 0;
        for (int i = 0; i < x.length; ++i) {
            final boolean xTrue = x[i] != 0;
            final boolean yTrue = y[i] != 0;
            if (xTrue && yTrue) {
                ++numTrueTrue;
            }
            if (xTrue && !yTrue) {
                ++numTrueFalse;
            }
            if (!xTrue && yTrue) {
                ++numFalseTrue;
            }
        }
        final int numFalseFalse = x.length - numTrueTrue - numTrueFalse - numFalseTrue;

        return numTrueFalse == 0 || numFalseTrue == 0 ? 0 : (2 * numTrueFalse * numFalseTrue) / (float) (numTrueTrue * numFalseFalse + numTrueFalse * numFalseTrue);
    }
}
