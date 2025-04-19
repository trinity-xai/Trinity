package edu.jhuapl.trinity.utils.metric;

/**
 * @author Sean Phillips
 * Yule similarity distance that does a thresholded agreement check close to zero.
 * Useful similarity check for continuous data which has been normalized between
 * 0 and 1 where agreement near 0.0 is important.
 */

public final class YuleThresholdMetric extends Metric {

    public static final double DEFAULT_THRESHOLD = 0.001;
    public static final YuleThresholdMetric SINGLETON = new YuleThresholdMetric();

    private YuleThresholdMetric() {
        super(false);
        setThreshold(DEFAULT_THRESHOLD);
    }

    @Override
    public double distance(final double[] x, final double[] y) {
        int numTrueTrue = 0;
        int numTrueFalse = 0;
        int numFalseTrue = 0;
        final double threshold = getThreshold();
        boolean xTrue, yTrue;
        for (int i = 0; i < x.length; ++i) {
            xTrue = Math.abs(x[i]) > threshold;
            yTrue = Math.abs(y[i]) > threshold;
            if (xTrue && yTrue) {
                ++numTrueTrue;
            }
            if (xTrue && !yTrue) {
                ++numTrueFalse;
            }
            if (!xTrue && yTrue) {
                ++numFalseTrue;
            }
        }
        final int numFalseFalse = x.length - numTrueTrue - numTrueFalse - numFalseTrue;

        return numTrueFalse == 0 || numFalseTrue == 0 ? 0 : (2 * numTrueFalse * numFalseTrue) / (double) (numTrueTrue * numFalseFalse + numTrueFalse * numFalseTrue);
    }
}
