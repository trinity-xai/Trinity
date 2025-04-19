/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

/**
 * Rogers Tanimoto distance.
 */
public class RogersTanimotoMetric extends Metric {

    /**
     * Rogers Tanimoto distance.
     */
    public static final RogersTanimotoMetric SINGLETON = new RogersTanimotoMetric();

    RogersTanimotoMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        int numNotEqual = 0;
        for (int i = 0; i < x.length; ++i) {
            final boolean xTrue = x[i] != 0;
            final boolean yTrue = y[i] != 0;
            if (xTrue != yTrue) {
                ++numNotEqual;
            }
        }
        return (2 * numNotEqual) / (double) (x.length + numNotEqual);
    }
}
