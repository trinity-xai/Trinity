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
 * Correlation distance.
 */
public final class CorrelationMetric extends Metric {

    /**
     * Correlation distance.
     */
    public static final CorrelationMetric SINGLETON = new CorrelationMetric();

    private CorrelationMetric() {
        super(true);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        double muX = 0.0F;
        double muY = 0.0F;
        double normX = 0.0F;
        double normY = 0.0F;
        double dotProduct = 0.0F;

        for (int i = 0; i < x.length; ++i) {
            muX += x[i];
            muY += y[i];
        }

        muX /= x.length;
        muY /= x.length;

        for (int i = 0; i < x.length; ++i) {
            final double shiftedX = x[i] - muX;
            final double shiftedY = y[i] - muY;
            normX += shiftedX * shiftedX;
            normY += shiftedY * shiftedY;
            dotProduct += shiftedX * shiftedY;
        }

        if (normX == 0.0 && normY == 0.0) {
            return 0;
        } else if (dotProduct == 0.0) {
            return 1;
        } else {
            return (double) (1 - (dotProduct / Math.sqrt(normX * normY)));
        }
    }
}
