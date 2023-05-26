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
 * Dice distance.
 *
 * @author Sean A. Irvine
 */
public final class DiceMetric extends Metric {

    /**
     * Dice distance.
     */
    public static final DiceMetric SINGLETON = new DiceMetric();

    private DiceMetric() {
        super(true);
    }

    @Override
    public float distance(final float[] x, final float[] y) {
        int numTrueTrue = 0;
        int numNotEqual = 0;
        for (int i = 0; i < x.length; ++i) {
            final boolean xTrue = x[i] != 0;
            final boolean yTrue = y[i] != 0;
            numTrueTrue += xTrue && yTrue ? 1 : 0;
            numNotEqual += xTrue != yTrue ? 1 : 0;
        }

        if (numNotEqual == 0) {
            return 0;
        } else {
            return numNotEqual / (float) (2 * numTrueTrue + numNotEqual);
        }
    }
}
