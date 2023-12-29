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
 * Bray Curtis distance.
 */
public final class AngularMetric extends Metric {

    /**
     * Simple Angular distance.
     */
    public static final AngularMetric SINGLETON = new AngularMetric();

    private AngularMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        //def angular_dist(x1, x2):
        //  """Angular (i.e. cosine) distance between two vectors."""

        //x1_norm = np.maximum(l2_norm(x1), 1e-20)
        double x1_norm = Math.max(l2Norm(x), 1e-20);
        //x2_norm = np.maximum(l2_norm(x2), 1e-20)
        double x2_norm = Math.max(l2Norm(y), 1e-20);
        double result = 0.0;
        //for i in range(x1.shape[0]):
        for (int i = 0; i < x.length; i++)
            result += x[i] * y[i]; //result += x1[i] * x2[i]
        //# angular is multiplied by a factor of 2.0 in annoy
        return Double.valueOf(2.0 * (1.0 - result / x1_norm / x2_norm)).floatValue();

    }

    //    """L2 norm of a vector."""
    public double l2Norm(double[] x) {
        double result = 0.0;
        for (int i = 0; i < x.length; i++) {
            result += x[i] * x[i];
        }
        return Math.sqrt(result);
    }

}
