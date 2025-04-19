/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

/**
 * Cosine distance.
 *
 * @author Sean A. Irvine
 */
public final class CosineMetric extends Metric {

    /**
     * Cosine distance.
     */
    public static final CosineMetric SINGLETON = new CosineMetric();

    private CosineMetric() {
        super(true);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        double result = 0.0;
        double normX = 0.0;
        double normY = 0.0;

        for (int i = 0; i < x.length; ++i) {
            result += x[i] * y[i];
            normX += x[i] * x[i];
            normY += y[i] * y[i];
        }
        if (normX == 0.0 && normY == 0.0) {
            return 0;
        } else if (normX == 0.0 || normY == 0.0) {
            return 1;
        } else {
            return (double) (1 - (result / Math.sqrt(normX * normY)));
        }
    }
}
