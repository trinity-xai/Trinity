/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

/**
 * Euclidean distance standardised against a vector of standard deviations per coordinate.
 *
 * @author Sean A. Irvine
 */
public class StandardisedEuclideanMetric extends Metric {

    private final double[] mSigma;

    public StandardisedEuclideanMetric(final double[] sigma) {
        super(false);
        mSigma = sigma;
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        //  D(x, y) = \sqrt{\sum_i \frac{(x_i - y_i)**2}{v_i}}
        double result = 0;
        for (int i = 0; i < x.length; ++i) {
            final double d = x[i] - y[i];
            result += d * d / mSigma[i];
        }
        return Math.sqrt(result);
    }
}
