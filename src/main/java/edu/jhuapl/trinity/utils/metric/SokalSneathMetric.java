/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

/**
 * Sokal Sneath distance.
 */
public final class SokalSneathMetric extends Metric {

    /**
     * Sokal Sneath distance.
     */
    public static final SokalSneathMetric SINGLETON = new SokalSneathMetric();

    private SokalSneathMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        int numTrueTrue = 0;
        int numNotEqual = 0;
        for (int i = 0; i < x.length; ++i) {
            final boolean xTrue = x[i] != 0;
            final boolean yTrue = y[i] != 0;
            if (xTrue && yTrue) {
                ++numTrueTrue;
            }
            if (xTrue != yTrue) {
                ++numNotEqual;
            }
        }
        return numNotEqual == 0 ? 0 : numNotEqual / (0.5 * numTrueTrue + numNotEqual);
    }
}
