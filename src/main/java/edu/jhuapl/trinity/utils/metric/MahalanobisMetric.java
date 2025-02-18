/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils.metric;

/**
 * Mahalanobis distance.
 */
public class MahalanobisMetric extends Metric {

    public static final MahalanobisMetric SINGLETON = new MahalanobisMetric();

    private double[][] mV; //inverse convariance matrix of independent variables (each column)

    public MahalanobisMetric() {
        super(false);
    }

    public MahalanobisMetric(final double[][] v) {
        this();
        mV = v;
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        double result = 0;
        final double[] diff = new double[x.length];
        for (int i = 0; i < x.length; ++i) {
            diff[i] = x[i] - y[i];
        }
        for (int i = 0; i < x.length; ++i) {
            double tmp = 0.0;
            for (int j = 0; j < x.length; ++j) {
                tmp += mV[i][j] * diff[j];
            }
            result += tmp * diff[i];
        }
        return (double) Math.sqrt(result);
    }
}
