/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

/**
 * Hamming distance.
 */
public final class HammingMetric extends Metric {

    /**
     * Hamming distance.
     */
    public static final HammingMetric SINGLETON = new HammingMetric();

    private HammingMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        double result = 0;
        for (int i = 0; i < x.length; ++i) {
            if (x[i] != y[i]) {
                ++result;
            }
        }
        return result / x.length;
    }
}
