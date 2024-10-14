/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.metric;

/**
 * Kulsinski distance.
 */
public final class KulsinskiMetric extends Metric {

    /**
     * Kulsinski distance.
     */
    public static final KulsinskiMetric SINGLETON = new KulsinskiMetric();

    private KulsinskiMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        int numTrueTrue = 0;
        int numNotEqual = 0;
        for (int i = 0; i < x.length; ++i) {
            final boolean xTrue = x[i] != 0;
            final boolean yTrue = y[i] != 0;
            if (xTrue && yTrue) {
                ++numTrueTrue;
            }
            if (xTrue != yTrue) {
                ++numNotEqual;
            }
        }
        return numNotEqual == 0 ? 0 : (numNotEqual - numTrueTrue + x.length) / (double) (numNotEqual + x.length);
    }
}
