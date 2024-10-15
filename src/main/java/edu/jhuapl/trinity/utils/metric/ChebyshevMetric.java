/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.metric;

/**
 * Chebyshev distance.
 */
public final class ChebyshevMetric extends Metric {

    /**
     * Chebyshev distance.
     */
    public static final ChebyshevMetric SINGLETON = new ChebyshevMetric();

    private ChebyshevMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        // D(x, y) = \max_i |x_i - y_i|
        double result = 0;
        for (int i = 0; i < x.length; ++i) {
            result = Math.max(result, Math.abs(x[i] - y[i]));
        }
        return result;
    }
}
