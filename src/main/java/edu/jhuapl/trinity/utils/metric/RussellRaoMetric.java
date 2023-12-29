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

/**
 * Russell Rao distance.
 */
public final class RussellRaoMetric extends Metric {

    /**
     * Russell Rao distance.
     */
    public static final RussellRaoMetric SINGLETON = new RussellRaoMetric();

    private RussellRaoMetric() {
        super(false);
    }

    private int countNonZero(final double[] values) {
        int c = 0;
        for (final double v : values) {
            if (v != 0) {
                ++c;
            }
        }
        return c;
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        int numTrueTrue = 0;
        for (int i = 0; i < x.length; ++i) {
            final boolean xTrue = x[i] != 0;
            final boolean yTrue = y[i] != 0;
            if (xTrue && yTrue) {
                ++numTrueTrue;
            }
        }
        if (numTrueTrue == countNonZero(x) && numTrueTrue == countNonZero(y)) {
            return 0;
        } else {
            return (x.length - numTrueTrue) / (double) x.length;
        }
    }
}
