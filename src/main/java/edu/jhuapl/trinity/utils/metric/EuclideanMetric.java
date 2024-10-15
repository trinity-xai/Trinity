/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.metric;

/**
 * Euclidean distance.
 */
public final class EuclideanMetric extends Metric {

    /**
     * Euclidean metric.
     */
    public static final EuclideanMetric SINGLETON = new EuclideanMetric();

    private EuclideanMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        //  D(x, y) = \sqrt{\sum_i (x_i - y_i)^2}
        double result = 0;
        for (int i = 0; i < x.length; ++i) {
            final double d = x[i] - y[i];
            result += d * d;
        }
        return (double) Math.sqrt(result);
    }
}
