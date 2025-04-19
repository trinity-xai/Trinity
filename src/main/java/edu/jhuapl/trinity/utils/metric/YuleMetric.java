/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

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
    public double distance(final double[] x, final double[] y) {
        int numTrueTrue = 0;
        int numTrueFalse = 0;
        int numFalseTrue = 0;
        boolean xTrue, yTrue;
        for (int i = 0; i < x.length; ++i) {
            xTrue = x[i] != 0;
            yTrue = y[i] != 0;
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
