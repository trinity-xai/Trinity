/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.metric;

/**
 * Canberra distance.
 */
public final class CanberraMetric extends Metric {

    /**
     * Canberra distance.
     */
    public static final CanberraMetric SINGLETON = new CanberraMetric();

    private CanberraMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        double result = 0;
        for (int i = 0; i < x.length; ++i) {
            final double denominator = Math.abs(x[i]) + Math.abs(y[i]);
            if (denominator > 0) {
                result += Math.abs(x[i] - y[i]) / denominator;
            }
        }
        return result;
    }
}
