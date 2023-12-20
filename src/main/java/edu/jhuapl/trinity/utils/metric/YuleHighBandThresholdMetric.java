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
 * @author Sean Phillips
 * Yule similarity distance that does a thresholded agreement check close to one.
 * Useful similarity check for continuous data which has been normalized between 
 * 0 and 1 where agreement near 1.0 is important.
 */
public final class YuleHighBandThresholdMetric extends Metric {

    public static final double DEFAULT_THRESHOLD = 0.5;
    public static final YuleHighBandThresholdMetric SINGLETON = new YuleHighBandThresholdMetric();

    private YuleHighBandThresholdMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        int numTrueTrue = 0;
        int numTrueFalse = 0;
        int numFalseTrue = 0;
        for (int i = 0; i < x.length; ++i) {
            final boolean xTrue = Math.abs(x[i]) < DEFAULT_THRESHOLD;
            final boolean yTrue = Math.abs(y[i]) < DEFAULT_THRESHOLD;
            
//            if(Math.abs(x[i])>1.0)
//                System.out.println("Dimension over 1.0: " + x[i]);
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

        return numTrueFalse == 0 || numFalseTrue == 0 ? 0 : (2 * numTrueFalse * numFalseTrue) / (double) (numTrueTrue * numFalseFalse + numTrueFalse * numFalseTrue);
    }
}
