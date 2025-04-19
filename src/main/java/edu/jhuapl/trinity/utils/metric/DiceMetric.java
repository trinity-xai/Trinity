/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

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
    public double distance(final double[] x, final double[] y) {
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
            return numNotEqual / (double) (2 * numTrueTrue + numNotEqual);
        }
    }
}
