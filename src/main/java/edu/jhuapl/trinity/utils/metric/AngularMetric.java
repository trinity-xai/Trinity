package edu.jhuapl.trinity.utils.metric;

/**
 * Bray Curtis distance.
 */
public final class AngularMetric extends Metric {

    /**
     * Simple Angular distance.
     */
    public static final AngularMetric SINGLETON = new AngularMetric();

    private AngularMetric() {
        super(false);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        //def angular_dist(x1, x2):
        //  """Angular (i.e. cosine) distance between two vectors."""

        //x1_norm = np.maximum(l2_norm(x1), 1e-20)
        double x1_norm = Math.max(l2Norm(x), 1e-20);
        //x2_norm = np.maximum(l2_norm(x2), 1e-20)
        double x2_norm = Math.max(l2Norm(y), 1e-20);
        double result = 0.0;
        //for i in range(x1.shape[0]):
        for (int i = 0; i < x.length; i++)
            result += x[i] * y[i]; //result += x1[i] * x2[i]
        //# angular is multiplied by a factor of 2.0 in annoy
        return Double.valueOf(2.0 * (1.0 - result / x1_norm / x2_norm)).floatValue();

    }

    //    """L2 norm of a vector."""
    public double l2Norm(double[] x) {
        double result = 0.0;
        for (int i = 0; i < x.length; i++) {
            result += x[i] * x[i];
        }
        return Math.sqrt(result);
    }

}
