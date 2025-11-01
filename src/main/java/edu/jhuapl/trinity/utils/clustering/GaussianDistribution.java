package edu.jhuapl.trinity.utils.clustering;

import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.special.Erf;

import java.util.Arrays;
import java.util.Random;

/**
 * Gaussian distribution.
 */
public class GaussianDistribution {
    public static Random rando = new Random();

    /* useful for centroid tracking */
    public double[] initialMean;
    /**
     * The mean vector.
     */
    public final double[] mu;
    /**
     * The covariance matrix.
     */
    public final RealMatrix sigma;
    /**
     * True if the covariance matrix is diagonal.
     */
    public final boolean diagonal;

    /**
     * The dimension.
     */
    private int dim;
    /**
     * The inverse of covariance matrix.
     */
    private RealMatrix sigmaInv;
    /**
     * The Cholesky decomposition of covariance matrix.
     */
    private RealMatrix sigmaL;
    /**
     * The determinant of covariance matrix.
     */
    private double sigmaDet;
    /**
     * The constant factor in PDF.
     */
    private double pdfConstant;
    /**
     * The number of parameters.
     */
    private final int length;

    /**
     * distribution with diagonal covariance matrix of
     * the same variance.
     *
     * @param mean     mean vector.
     * @param variance variance.
     */
    public GaussianDistribution(double[] mean, double variance) {
        if (variance <= 0) {
            throw new IllegalArgumentException("Variance is not positive: " + variance);
        }
        initialMean = new double[mean.length];
        System.arraycopy(mean, 0, initialMean, 0, mean.length);
        mu = mean;
        double[] diagonalVector = new double[mu.length];
        sigma = MatrixUtils.createRealDiagonalMatrix(diagonalVector);
        diagonal = true;
        length = mu.length + 1;

        init();
    }

    /**
     * distribution will have a diagonal covariance matrix.
     * Each element has different variance.
     *
     * @param mean     mean vector.
     * @param variance variance vector.
     */
    public GaussianDistribution(double[] mean, double[] variance) {
        if (mean.length != variance.length) {
            throw new IllegalArgumentException("Mean vector and covariance matrix have different dimension");
        }

        for (double v : variance) {
            if (v <= 0) {
                throw new IllegalArgumentException("Variance is not positive: " + v);
            }
        }
        initialMean = new double[mean.length];
        System.arraycopy(mean, 0, initialMean, 0, mean.length);
        mu = mean;
        sigma = MatrixUtils.createRealDiagonalMatrix(variance);
        diagonal = true;
        length = 2 * mu.length;

        init();
    }

    /**
     * Full Covariance Distribution
     *
     * @param mean mean vector.
     * @param cov  covariance matrix.
     */
    public GaussianDistribution(double[] mean, RealMatrix cov) {
        if (mean.length != cov.getRowDimension()) {
            throw new IllegalArgumentException("Mean vector and covariance matrix have different dimension");
        }
        initialMean = new double[mean.length];
        System.arraycopy(mean, 0, initialMean, 0, mean.length);
        mu = mean;
        sigma = cov;
        diagonal = false;
        length = mu.length + mu.length * (mu.length + 1) / 2;
        init();
    }

    /**
     * Estimates the mean and diagonal covariance by MLE.
     *
     * @param data the training data.
     * @return the distribution.
     */
    public static GaussianDistribution fit(double[][] data) {
        return fit(data, false);
    }

    /**
     * Estimates the mean and covariance by MLE.
     *
     * @param data     the training data.
     * @param diagonal true if covariance matrix is diagonal.
     * @return the distribution.
     */
    public static GaussianDistribution fit(double[][] data, boolean diagonal) {
        double[] mu = ClusterUtils.colMeans(data);
        int n = data.length;
        int d = mu.length;

        if (diagonal) {
            double[] variance = new double[d];
            for (double[] x : data) {
                for (int j = 0; j < d; j++) {
                    variance[j] += (x[j] - mu[j]) * (x[j] - mu[j]);
                }
            }

            int n1 = n - 1;
            for (int j = 0; j < d; j++) {
                variance[j] /= n1;
            }

            return new GaussianDistribution(mu, variance);
        } else {
            return new GaussianDistribution(mu, MatrixUtils.createRealMatrix(ClusterUtils.cov(data, mu)));
        }
    }

    /**
     * Initialize the object.
     */
    private void init() {
        dim = mu.length;
        CholeskyDecomposition cd = new CholeskyDecomposition(sigma);
        sigmaInv = cd.getSolver().getInverse();
        sigmaDet = cd.getDeterminant();
        sigmaL = cd.getL();
        pdfConstant = (dim * Math.log(2 * Math.PI) + Math.log(sigmaDet)) / 2.0;
    }

    public int length() {
        return length;
    }

    public double entropy() {
        return (dim * ClusterUtils.LOG2PIE + Math.log(sigmaDet)) / 2;
    }

    public double[] mean() {
        return mu;
    }

    public RealMatrix cov() {
        return sigma;
    }

    /**
     * Returns the scatter of distribution, which is defined as |&Sigma;|.
     *
     * @return the scatter of distribution.
     */
    public double scatter() {
        return sigmaDet;
    }
public double mahalanobis2(double[] x) {
    double[] v = x.clone();
    ClusterUtils.sub(v, mu);
    double[] Av = sigmaInv.operate(v);
    return ClusterUtils.dot(v, Av);
}
    public double logp(double[] x) {
        if (x.length != dim) throw new IllegalArgumentException("Sample has different dimension.");
        double[] v = x.clone();
        ClusterUtils.sub(v, mu);                 // v = x - μ
        double[] Av = sigmaInv.operate(v);       // Σ⁻¹ v
        double quad = ClusterUtils.dot(v, Av);   // vᵀ Σ⁻¹ v   
        return -0.5 * quad - pdfConstant;
    }

    public double p(double[] x) {
        return Math.exp(logp(x));
    }

    /**
     * Multidimensional CDF calculation
     *
     * @param x
     * @return
     */
    public double cdf(double[] x) {
        if (x.length != dim) {
            throw new IllegalArgumentException("Sample has different dimension.");
        }

        int Nmax = 10000;
//        double alph = GaussianDistribution.getInstance().quantile(0.999);
        double alph = singleDimQuantile(0.999);
        double errMax = 0.001;

        double[] v = x.clone();
        ClusterUtils.sub(v, mu);

        double p = 0.0;
        double varSum = 0.0;

        // d is always zero
        double[] e = new double[dim];
        double[] f = new double[dim];
//        e[0] = GaussianDistribution.getInstance().cdf(v[0] / sigmaL.getEntry(0, 0));
        e[0] = singleDimCDF(v[0] / sigmaL.getEntry(0, 0));
        f[0] = e[0];

        double[] y = new double[dim];

        double err = 2 * errMax;
        int N;
        for (N = 1; err > errMax && N <= Nmax; N++) {
            double[] w = rando.doubles(dim - 1).toArray();

            for (int i = 1; i < dim; i++) {
//                y[i - 1] = GaussianDistribution.getInstance().quantile(w[i - 1] * e[i - 1]);
                y[i - 1] = singleDimQuantile(w[i - 1] * e[i - 1]);
                double q = 0.0;
                for (int j = 0; j < i; j++) {
                    q += sigmaL.getEntry(i, j) * y[j];
                }

//                e[i] = GaussianDistribution.getInstance().cdf((v[i] - q) / sigmaL.getEntry(i, i));
                e[i] = singleDimCDF((v[i] - q) / sigmaL.getEntry(i, i));
                f[i] = e[i] * f[i - 1];
            }

            double del = (f[dim - 1] - p) / N;
            p += del;
            varSum = (N - 2) * varSum / N + del * del;
            err = alph * Math.sqrt(varSum);
        }

        return p;
    }

    /**
     * A single dimensional quantile computation based on a mu and sigma of 0.0 and 1.0
     * The inverse of cdf.
     *
     * @param p
     * @return the quantile
     */
    public double singleDimQuantile(double p) {
        if (p < 0.0)
            p = 0.0;
        if (p > 1.0)
            p = 1.0;
//        double localMu = 0.0;
//        double localSigma = 1.0;
//        double variance = sigma * sigma;

//        double entropy = Math.log(sigma) + ClusterUtils.LOG2PIE_2;
//        double pdfConstant = Math.log(sigma) + ClusterUtils.LOG2PI_2;

//        if (localSigma == 0.0) {
//            if (p < 1.0) {
//                return localMu - 1E-10;
//            } else {
//                return localMu;
//            }
//        }
//        return -1.41421356237309505 * localSigma * Erf.erfcInv(2.0 * p) + localMu;
        return -1.41421356237309505 * Erf.erfcInv(2.0 * p);

    }

    /**
     * A single dimensional cdf computation
     * Assumes a mu and sigma of 0.0 and 1.0
     * The inverse of cdf.
     *
     * @param x
     * @return the cdf
     */

    public double singleDimCDF(double x) {
//        double localMu = 0.0;
//        double localSigma = 1.0;
//        if (localSigma == 0) {
//            if (x < localMu) {
//                return 0.0;
//            } else {
//                return 1.0;
//            }
//        }
//        return 0.5 * Erf.erfc(-0.707106781186547524 * (x - localMu) / localSigma);
        return 0.5 * Erf.erfc(-0.707106781186547524 * x);
    }

    /**
     * random Gaussian sample.
     *
     * @return a random sample.
     */
    public double[] randomGaussianVector() {
        double[] spt = new double[mu.length];

        for (int i = 0; i < mu.length; i++) {
            double u, v, q;
            do {
                u = rando.nextDouble();
                v = 1.7156 * (rando.nextDouble() - 0.5);
                double x = u - 0.449871;
                double y = Math.abs(v) + 0.386595;
                q = x * x + y * (0.19600 * y - 0.25472 * x);
            } while (q > 0.27597 && (q > 0.27846 || v * v > -4 * Math.log(u) * u * u));

            spt[i] = v / u;
        }

        double[] pt = new double[sigmaL.getRowDimension()];

        // pt = sigmaL * spt
        for (int i = 0; i < pt.length; i++) {
            for (int j = 0; j <= i; j++) {
                pt[i] += sigmaL.getEntry(i, j) * spt[j];
            }
        }

        ClusterUtils.add(pt, mu);

        return pt;
    }

    /**
     * Generates a set of random numbers following this distribution.
     *
     * @param n the number of random samples to generate.
     * @return a set of random samples.
     */
    public double[][] randomGaussianSamples(int n) {
        double[][] data = new double[n][];
        for (int i = 0; i < n; i++) {
            data[i] = randomGaussianVector();
        }
        return data;
    }

    /**
     * Maximization Phase of EM.
     *
     * @param data       observation data
     * @param posteriori the posteriori probability.
     * @return the (unnormalized) weight of this distribution in the mixture.
     */
    public GaussianMixtureComponent maximization(double[][] data, double[] posteriori) {
        int n = data.length;
        int d = data[0].length;

        double alpha = 0.0;
        double[] mean = new double[d];

        for (int k = 0; k < n; k++) {
            alpha += posteriori[k];
            double[] x = data[k];
            for (int i = 0; i < d; i++) {
                mean[i] += x[i] * posteriori[k];
            }
        }

        for (int i = 0; i < d; i++) {
            mean[i] /= alpha;
        }

        GaussianDistribution gaussian;
        if (diagonal) {
            double[] variance = new double[d];
            for (int k = 0; k < n; k++) {
                double[] x = data[k];
                for (int i = 0; i < d; i++) {
                    variance[i] += (x[i] - mean[i]) * (x[i] - mean[i]) * posteriori[k];
                }
            }

            for (int i = 0; i < d; i++) {
                variance[i] /= alpha;
            }

            gaussian = new GaussianDistribution(mean, variance);
        } else {
            RealMatrix cov = MatrixUtils.createRealMatrix(d, d);
            for (int k = 0; k < n; k++) {
                double[] x = data[k];
                for (int i = 0; i < d; i++) {
                    for (int j = 0; j < d; j++) {
                        cov.addToEntry(i, j, (x[i] - mean[i]) * (x[j] - mean[j]) * posteriori[k]);
                    }
                }
            }

            for (int i = 0; i < d; i++) {
                for (int j = 0; j < d; j++) {
                    double updatedEntry = cov.getEntry(i, j) / alpha;
                    cov.setEntry(i, j, updatedEntry);
                }

                // make sure the covariance matrix is positive definite.
                cov.multiplyEntry(i, i, 1.00001);
            }

            gaussian = new GaussianDistribution(mean, cov);
        }

        return new GaussianMixtureComponent(alpha, gaussian);
    }

    /**
     * The likelihood of the sample set following this distribution.
     *
     * @param x a set of samples.
     * @return the likelihood.
     */
    public double likelihood(double[][] x) {
        return Math.exp(logLikelihood(x));
    }

    /**
     * The log likelihood of the sample set following this distribution.
     *
     * @param x a set of samples.
     * @return the log likelihood.
     */
    public double logLikelihood(double[][] x) {
        double L = 0.0;

        for (double[] xi : x)
            L += logp(xi);

        return L;
    }
    public int dim() {
        return dim;
    }
    @Override
    public String toString() {
        return String.format("Gaussian(mu = %s, sigma = %s)", Arrays.toString(mu), sigma);
    }
}
