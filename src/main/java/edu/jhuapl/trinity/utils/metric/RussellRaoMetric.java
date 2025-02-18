/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.metric;

/**
 * Russell Rao distance.
 */
public final class RussellRaoMetric extends Metric {

    /**
     * Russell Rao distance.
     */
    public static final RussellRaoMetric SINGLETON = new RussellRaoMetric();

    private RussellRaoMetric() {
        super(false);
    }

    private int countNonZero(final double[] values) {
        int c = 0;
        for (final double v : values) {
            if (v != 0) {
                ++c;
            }
        }
        return c;
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        int numTrueTrue = 0;
        for (int i = 0; i < x.length; ++i) {
            final boolean xTrue = x[i] != 0;
            final boolean yTrue = y[i] != 0;
            if (xTrue && yTrue) {
                ++numTrueTrue;
            }
        }
        if (numTrueTrue == countNonZero(x) && numTrueTrue == countNonZero(y)) {
            return 0;
        } else {
            return (x.length - numTrueTrue) / (double) x.length;
        }
    }
}
