/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.metric;

/**
 * Reduced Euclidean distance.
 *
 * @author Leland McInnes
 * @author Sean A. Irvine
 * @author Richard Littin
 */
public final class ReducedEuclideanMetric extends Metric {

    /**
     * Reduced Euclidean distance.
     */
    public static final ReducedEuclideanMetric SINGLETON = new ReducedEuclideanMetric();

    private ReducedEuclideanMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        //  D(x, y) = \sum_i (x_i - y_i)^2
        double result = 0;
        for (int i = 0; i < x.length; ++i) {
            final double d = x[i] - y[i];
            result += d * d;
        }
        return result;
    }
}
