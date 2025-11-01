/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.metric;

/**
 * Haversine distance.
 */
public final class HaversineMetric extends Metric {

    /**
     * Haversine distance.
     */
    public static final HaversineMetric SINGLETON = new HaversineMetric();

    private HaversineMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        if (x.length != 2) {
            throw new IllegalArgumentException("haversine is only defined for 2 dimensional data");
        }
        final double sinLat = Math.sin(0.5 * (x[0] - y[0]));
        final double sinLong = Math.sin(0.5 * (x[1] - y[1]));
        final double result = Math.sqrt(sinLat * sinLat + Math.cos(x[0]) * Math.cos(y[0]) * sinLong * sinLong);
        return 2 * Math.asin(result);
    }
}
