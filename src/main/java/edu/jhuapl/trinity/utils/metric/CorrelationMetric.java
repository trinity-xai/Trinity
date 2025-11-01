/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

/**
 * Correlation distance.
 */
public final class CorrelationMetric extends Metric {

    /**
     * Correlation distance.
     */
    public static final CorrelationMetric SINGLETON = new CorrelationMetric();

    private CorrelationMetric() {
        super(true);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        double muX = 0.0F;
        double muY = 0.0F;
        double normX = 0.0F;
        double normY = 0.0F;
        double dotProduct = 0.0F;

        for (int i = 0; i < x.length; ++i) {
            muX += x[i];
            muY += y[i];
        }

        muX /= x.length;
        muY /= x.length;

        for (int i = 0; i < x.length; ++i) {
            final double shiftedX = x[i] - muX;
            final double shiftedY = y[i] - muY;
            normX += shiftedX * shiftedX;
            normY += shiftedY * shiftedY;
            dotProduct += shiftedX * shiftedY;
        }

        if (normX == 0.0 && normY == 0.0) {
            return 0;
        } else if (dotProduct == 0.0) {
            return 1;
        } else {
            return 1 - (dotProduct / Math.sqrt(normX * normY));
        }
    }
}
