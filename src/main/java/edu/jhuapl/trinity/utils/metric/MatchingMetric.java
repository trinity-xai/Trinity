/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.metric;

/**
 * Matching distance.
 */
public final class MatchingMetric extends Metric {

    /**
     * Matching distance.
     */
    public static final MatchingMetric SINGLETON = new MatchingMetric();

    private MatchingMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        int numNotEqual = 0;
        for (int i = 0; i < x.length; ++i) {
            final boolean xTrue = x[i] != 0;
            final boolean yTrue = y[i] != 0;
            if (xTrue != yTrue) {
                ++numNotEqual;
            }
        }
        return numNotEqual / (double) x.length;
    }
}
