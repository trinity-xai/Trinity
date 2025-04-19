/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

/**
 * Special indicator for categorical data.
 *
 * @author Sean A. Irvine
 */
public final class CategoricalMetric extends Metric {

    /**
     * Special indicator for categorical data.
     */
    public static final CategoricalMetric SINGLETON = new CategoricalMetric();

    private CategoricalMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        throw new IllegalStateException();
    }
}
