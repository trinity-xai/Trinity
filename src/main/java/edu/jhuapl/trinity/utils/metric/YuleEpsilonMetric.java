/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.metric;

import org.apache.commons.math3.util.Precision;

/**
 * @author Sean Phillips
 * Modified Yule similarity distance that uses the threshold value as a small
 * EPSILON difference check. If the values agree within the EPSILON difference
 * then the total number of agreements is incremented. This replaces the original
 * numTrueTrue and numFalseFalse with a single numAgreements value in the final
 * distance ratio calculation.
 * Useful similarity check for continuous data which has been normalized between
 * 0 and 1 where agreement at a single value is important.
 */
public final class YuleEpsilonMetric extends Metric {

    public static final double DEFAULT_EPSILON = 0.001;
    public static final YuleEpsilonMetric SINGLETON = new YuleEpsilonMetric();

    private YuleEpsilonMetric() {
        super(false);
        setThreshold(DEFAULT_EPSILON);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        int numAgreements = 0;
        int numDisagreements = 0;
        final double threshold = getThreshold();
        for (int i = 0; i < x.length; ++i) {
            if (Precision.equals(x[i], y[i], threshold))
                numAgreements++;
            else
                numDisagreements++;
        }
        return (2 * numDisagreements * numDisagreements) / (double) (numAgreements * numAgreements + numDisagreements * numDisagreements);
    }
}
