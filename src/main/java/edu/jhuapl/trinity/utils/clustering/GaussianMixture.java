package edu.jhuapl.trinity.utils.clustering;

import javafx.util.Pair;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Provides Expectation Maximization process for mixtures of distributions.
 * The EM algorithm can be used to learn the mixture model from data.
 */
public class GaussianMixture {
    private static final Logger LOG = LoggerFactory.getLogger(GaussianMixture.class);

    /**
     * The components of finite mixture model.
     */
    public final GaussianMixtureComponent[] components;
    /**
     * The log-likelihood when the distribution is fit on a sample data.
     */
    public final double logLikelihood;
    /**
     * The BIC score when the distribution is fit on a sample data.
     */
    public final double bic;

    /**
     * @param components the gaussian mixture distributions.
     */
    public GaussianMixture(GaussianMixtureComponent... components) {
        this(0.0, 1, components);
    }

    /**
     * @param components a list of discrete exponential family distributions.
     * @param L          the log-likelihood.
     * @param n          the number of samples to fit the distribution.
     */
    GaussianMixture(double L, int n, GaussianMixtureComponent... components) {
        this.components = components;
        this.logLikelihood = L;
        this.bic = L - 0.5 * length() * Math.log(n);
    }

    /**
     * Fits the mixture model with the EM algorithm.
     *
     * @param x          the training data.
     * @param components the initial configuration of mixture. Components may have
     *                   different distribution form.
     * @return the distribution.
     */
    public static GaussianMixture fit(double[][] x, GaussianMixtureComponent... components) {
        return fit(x, components, 0.2, 500, 1E-8);
    }

    /**
     * Fits the mixture model with the EM algorithm.
     *
     * @param x          the training data.
     * @param components the initial configuration of mixture. Components may have
     *                   different distribution form.
     * @param gamma      the regularization parameter.
     * @param maxIter    the maximum number of iterations.
     * @param tol        the tolerance of convergence test.
     * @return the distribution.
     */
    public static GaussianMixture fit(double[][] x, GaussianMixtureComponent[] components, double gamma, int maxIter, double tol) {
        if (x.length < components.length / 2) {
            throw new IllegalArgumentException("Too many components");
        }

        if (gamma < 0.0 || gamma > 0.2) {
            throw new IllegalArgumentException("Invalid regularization factor gamma.");
        }

        int n = x.length;
        int k = components.length;

        double[][] posteriori = new double[k][n];

        // Log Likelihood
        double L = 0.0;
        double log2 = Math.log(2);

        // EM loop until convergence
        int iterationsComplete = 0;
        maxIter = 200;
        tol = -1e6;
        gamma = 0.0;
        double diff = Double.MAX_VALUE;
        for (int iter = 1; iter <= maxIter && diff > tol; iter++) {
//        for (int iter = 1; iter <= maxIter; iter++) {
            // Expectation step
            for (int i = 0; i < k; i++) {
                GaussianMixtureComponent c = components[i];

                for (int j = 0; j < n; j++) {
                    posteriori[i][j] = c.priori() * c.distribution().p(x[j]);
                }
            }

            // Normalize posteriori probability.
            for (int j = 0; j < n; j++) {
                double p = 0.0;

                for (int i = 0; i < k; i++) {
                    p += posteriori[i][j];
                }

                for (int i = 0; i < k; i++) {
                    posteriori[i][j] /= p;
                }

                // Adjust posterior probabilites based on Regularized EM algorithm.
                if (gamma > 0) {
                    for (int i = 0; i < k; i++) {
                        posteriori[i][j] *= (1 + gamma *
                            Math.log(posteriori[i][j]) / log2);
                        if (Double.isNaN(posteriori[i][j]) || posteriori[i][j] < 0.0) {
                            posteriori[i][j] = 0.0;
                        }
                    }
                }
            }

            // Maximization step
            double Z = 0.0;
            for (int i = 0; i < k; i++) {
                components[i] = components[i].distribution().maximization(x, posteriori[i]);
                Z += components[i].priori();
            }

            for (int i = 0; i < k; i++) {
                components[i] = new GaussianMixtureComponent(components[i].priori() / Z, components[i].distribution());
            }

            double loglikelihood = 0.0;
            for (double[] xi : x) {
                double p = 0.0;
                for (GaussianMixtureComponent c : components) {
                    p += c.priori() * c.distribution().p(xi);
                }
                if (p > 0) loglikelihood += Math.log(p);
            }


            diff = loglikelihood - L;
            L = loglikelihood;

            if (iter % 10 == 0) {
                LOG.info("The log-likelihood after {} iterations: {}", iter, L);
            }
            iterationsComplete++;
        }
        LOG.info("EM Iterations: {}", iterationsComplete);
        return new GaussianMixture(L, x.length, components);
    }

    public int length() {
        int f = components.length - 1; // independent priori parameters
        for (GaussianMixtureComponent component : components) {
            f += component.distribution().length();
        }

        return f;
    }

    public double p(double[] x) {
        double p = 0.0;

        for (GaussianMixtureComponent c : components) {
            p += c.priori() * c.distribution().p(x);
        }

        return p;
    }

    /**
     * Returns the BIC score.
     *
     * @param data the data to calculate likelihood.
     * @return the BIC score.
     */
    public double bic(double[][] data) {
        int n = data.length;

        double logLikelihood = 0.0;
        for (double[] x : data) {
            double p = p(x);
            if (p > 0) {
                logLikelihood += Math.log(p);
            }
        }

        return logLikelihood - 0.5 * length() * Math.log(n);
    }

    /**
     * Returns the posteriori probabilities.
     *
     * @param x a real vector.
     * @return the posteriori probabilities.
     */
    public double[] posteriori(double[] x) {
        int k = components.length;
        double[] prob = new double[k];
        for (int i = 0; i < k; i++) {
            GaussianMixtureComponent c = components[i];
            prob[i] = c.priori() * c.distribution().p(x);
        }

        double p = ClusterUtils.sum(prob);
        for (int i = 0; i < k; i++) {
            prob[i] /= p;
        }
        return prob;
    }

    public double[] mean() {
        double w = components[0].priori();
        double[] m = components[0].distribution().mean();
        double[] mu = new double[m.length];
        for (int i = 0; i < m.length; i++) {
            mu[i] = w * m[i];
        }

        for (int k = 1; k < components.length; k++) {
            w = components[k].priori();
            m = components[k].distribution().mean();
            for (int i = 0; i < m.length; i++) {
                mu[i] += w * m[i];
            }
        }

        return mu;
    }

    public RealMatrix cov() {
        double[] mu = mean();
        int d = mu.length;
        RealMatrix C = MatrixUtils.createRealMatrix(d, d);

        // within-component variance
        for (GaussianMixtureComponent c : components) {
            double w = c.priori;
            RealMatrix Sk = c.distribution.cov();
            C = C.add(Sk.scalarMultiply(w));
        }

        // between-component variance
        for (GaussianMixtureComponent c : components) {
            double w = c.priori;
            double[] mk = c.distribution.mean();
            double[] diff = mk.clone();
            ClusterUtils.sub(diff, mu);
            RealMatrix outer = MatrixUtils.createColumnRealMatrix(diff)
                                  .multiply(MatrixUtils.createRowRealMatrix(diff));
            C = C.add(outer.scalarMultiply(w));
        }
        return C;
    }
public boolean inDistribution(double[] x, double q) {
    // choose most responsible component
    double[] r = posteriori(x);
    int idx = ClusterUtils.whichMax(r);
    GaussianMixtureComponent c = components[idx];
    double d2 = c.distribution.mahalanobis2(x);
    // chi-square threshold
    org.apache.commons.math3.distribution.ChiSquaredDistribution chi =
        new org.apache.commons.math3.distribution.ChiSquaredDistribution(c.distribution.dim());
    double thresh = chi.inverseCumulativeProbability(q);
    return d2 <= thresh;
}

public boolean inDistributionByLogP(double[] x, double tau) {
    return Math.log(p(x)) >= tau;
}
    public Pair<Integer, Double> maxPostProb(double[] x) {
        int k = components.length;
        double[] prob = new double[k];
        for (int i = 0; i < k; i++) {
            GaussianMixtureComponent c = components[i];
            prob[i] = c.priori() * c.distribution().p(x);
        }
        int maxIndex = ClusterUtils.whichMax(prob);
        return new Pair<>(maxIndex, prob[maxIndex]);
    }
}
