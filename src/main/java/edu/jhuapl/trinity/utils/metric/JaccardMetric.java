/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

/**
 * Jaccard distance.
 *
 * @author Sean A. Irvine
 */
public final class JaccardMetric extends Metric {

    /**
     * Jaccard distance.
     */
    public static final JaccardMetric SINGLETON = new JaccardMetric();

    private JaccardMetric() {
        super(true);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        int numNonZero = 0;
        int numEqual = 0;
        for (int i = 0; i < x.length; ++i) {
            final boolean xTrue = x[i] != 0;
            final boolean yTrue = y[i] != 0;
            numNonZero += xTrue || yTrue ? 1 : 0;
            numEqual += xTrue && yTrue ? 1 : 0;
        }

        if (numNonZero == 0) {
            return 0;
        } else {
            return (numNonZero - numEqual) / (double) numNonZero;
        }
    }
}
