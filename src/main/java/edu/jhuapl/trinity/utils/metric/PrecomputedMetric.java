/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

/**
 * Special indicator that the metric has been precomputed.
 *
 * @author Sean A. Irvine
 */
public final class PrecomputedMetric extends Metric {

    /**
     * Special indicator that the metric has been precomputed.
     */
    public static final PrecomputedMetric SINGLETON = new PrecomputedMetric();

    private PrecomputedMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        throw new IllegalStateException("Attempt to computed distance when distances precomputed");
    }
}
