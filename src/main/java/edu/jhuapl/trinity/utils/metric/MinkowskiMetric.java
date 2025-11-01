/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

/**
 * Minkowski distance. Singleton defaults to a power of 1.5 to be a blend between
 * Manhattan and Euclidean distances
 */
public class MinkowskiMetric extends Metric {

    private final double mPower;
    public static final MinkowskiMetric SINGLETON = new MinkowskiMetric(1.5);

    public MinkowskiMetric(final double power) {
        super(false);
        mPower = power;
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        // D(x, y) = \left(\sum_i |x_i - y_i|^p\right)^{\frac{1}{p}}
        double result = 0.0;
        for (int i = 0; i < x.length; ++i) {
            result += Math.pow(Math.abs(x[i] - y[i]), mPower);
        }
        return Math.pow(result, 1 / mPower);
    }
}
