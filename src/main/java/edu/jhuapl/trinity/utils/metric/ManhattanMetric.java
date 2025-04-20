/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

/**
 * Manhattan distance.
 */
public final class ManhattanMetric extends Metric {

    /**
     * Manhattan distance.
     */
    public static final ManhattanMetric SINGLETON = new ManhattanMetric();

    private ManhattanMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        //  D(x, y) = \sum_i |x_i - y_i|
        double result = 0;
        for (int i = 0; i < x.length; ++i) {
            result += Math.abs(x[i] - y[i]);
        }
        return result;
    }
}
