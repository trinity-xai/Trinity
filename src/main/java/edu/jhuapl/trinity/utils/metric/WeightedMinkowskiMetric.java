/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.metric;

/**
 * Weighted Minkowski distance.
 */
public class WeightedMinkowskiMetric extends Metric {

    private final double mPower;
    private final double[] mWeights;

    public WeightedMinkowskiMetric(final double power, final double[] weights) {
        super(false);
        mPower = power;
        mWeights = weights;
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        // D(x, y) = \left(\sum_i w_i |x_i - y_i|^p\right)^{\frac{1}{p}}
        double result = 0;
        for (int i = 0; i < x.length; ++i) {
            result += Math.pow(mWeights[i] * Math.abs(x[i] - y[i]), mPower);
        }
        return (double) Math.pow(result, 1 / mPower);
    }
}
