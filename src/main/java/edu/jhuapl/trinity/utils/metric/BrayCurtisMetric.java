/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.metric;

/**
 * Bray Curtis distance.
 */
public final class BrayCurtisMetric extends Metric {

    /**
     * Bray Curtis distance.
     */
    public static final BrayCurtisMetric SINGLETON = new BrayCurtisMetric();

    private BrayCurtisMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        double numerator = 0;
        double denominator = 0;
        for (int i = 0; i < x.length; ++i) {
            numerator += Math.abs(x[i] - y[i]);
            denominator += Math.abs(x[i] + y[i]);
        }
        return denominator > 0 ? numerator / denominator : 0;
    }
}
