package edu.jhuapl.trinity.utils.clustering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;

/**
 * Cherry Picked Math functions used for GMM processing.
 */
public class ClusterUtils {
    private static final int M = 7;
    private static final int NSTACK = 64;
    /**
     * The machine precision for the double type, which is the difference between 1
     * and the smallest value greater than 1 that is representable for the double type.
     */
    public static final double EPSILON = 0.00000000000001;
    public static final double LOG2PIE = Math.log(2 * Math.PI * Math.E);
    public static final double LOG2PIE_2 = Math.log(2 * Math.PI * Math.E) / 2;
    public static final double LOG2PI_2 = Math.log(2 * Math.PI) / 2;

    private static Logger LOG = LoggerFactory.getLogger(ClusterUtils.class);

    /**
     * The squared Euclidean distance with handling missing values (represented as NaN).
     * NaN will be treated as missing values and will be excluded from the
     * calculation. Let m be the number nonmissing values, and n be the
     * number of all values. The returned distance is (n * d / m),
     * where d is the square of distance between nonmissing values.
     *
     * @param x a vector.
     * @param y a vector.
     * @return the square of Euclidean distance.
     */
    public static double squaredDistanceWithMissingValues(double[] x, double[] y) {
        int n = x.length;
        int m = 0;
        double dist = 0.0;

        for (int i = 0; i < n; i++) {
            if (!Double.isNaN(x[i]) && !Double.isNaN(y[i])) {
                m++;
                double d = x[i] - y[i];
                dist += d * d;
            }
        }

        if (m == 0) {
            dist = Double.MAX_VALUE;
        } else {
            dist = n * dist / m;
        }

        return dist;
    }
    public static List<List<double[]>> extractGMMClusters(
            double[][] data,
            GaussianMixture gmm,
            double threshold)
    {
        List<List<double[]>> clusterPoints = new ArrayList<>();
        for (int i = 0; i < gmm.components.length; i++) {
            clusterPoints.add(new ArrayList<>());
        }

        for (double[] x : data) {
            double[] post = gmm.posteriori(x);
            int k = ClusterUtils.whichMax(post);
            if (post[k] >= threshold) {
                clusterPoints.get(k).add(x);
            }
        }

        // Filter out tiny/degenerate clusters if needed
        clusterPoints.removeIf(list -> list.size() < 4);

        return clusterPoints;
    }
    /**
     * Returns the sum of an array.
     *
     * @param x the array.
     * @return the sum.
     */
    public static double sum(double[] x) {
        double sum = 0.0;

        for (double n : x) {
            sum += n;
        }

        return sum;
    }

    /**
     * Returns the mean of an array.
     *
     * @param x the array.
     * @return the mean.
     */
    public static double mean(double[] x) {
        return sum(x) / x.length;
    }

    /**
     * The squared Euclidean distance.
     *
     * @param x a vector.
     * @param y a vector.
     * @return the square of Euclidean distance.
     */
    public static double squaredDistance(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Input vector sizes are different.");
        }

        switch (x.length) {
            case 2: {
                double d0 = x[0] - y[0];
                double d1 = x[1] - y[1];
                return d0 * d0 + d1 * d1;
            }

            case 3: {
                double d0 = x[0] - y[0];
                double d1 = x[1] - y[1];
                double d2 = x[2] - y[2];
                return d0 * d0 + d1 * d1 + d2 * d2;
            }

            case 4: {
                double d0 = x[0] - y[0];
                double d1 = x[1] - y[1];
                double d2 = x[2] - y[2];
                double d3 = x[3] - y[3];
                return d0 * d0 + d1 * d1 + d2 * d2 + d3 * d3;
            }
        }

        double sum = 0.0;
        for (int i = 0; i < x.length; i++) {
            double d = x[i] - y[i];
            sum += d * d;
        }

        return sum;
    }

    /**
     * Tests if a floating number is zero in machine precision.
     *
     * @param x a real number.
     * @return true if x is zero in machine precision.
     */
    public static boolean isZero(double x) {
        return isZero(x, EPSILON);
    }

    /**
     * Tests if a floating number is zero in given precision.
     *
     * @param x       a real number.
     * @param epsilon a number close to zero.
     * @return true if x is zero in <code>epsilon</code> precision.
     */
    public static boolean isZero(double x, double epsilon) {
        return abs(x) < epsilon;
    }

    /**
     * Returns the standard deviation of an array.
     *
     * @param x the array.
     * @return the standard deviation.
     */
    public static double sd(double[] x) {
        return sqrt(var(x));
    }

    /**
     * Returns the variance of an array.
     *
     * @param x the array.
     * @return the variance.
     */
    public static double var(double[] x) {
        if (x.length < 2) {
            throw new IllegalArgumentException("Array length is less than 2.");
        }

        double sum = 0.0;
        double sumsq = 0.0;
        for (double xi : x) {
            sum += xi;
            sumsq += xi * xi;
        }

        int n = x.length - 1;
        return sumsq / n - (sum / x.length) * (sum / n);
    }

    /**
     * Returns the dot product between two vectors.
     *
     * @param x a vector.
     * @param y a vector.
     * @return the dot product.
     */
    public static double dot(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Arrays have different length.");
        }

        double sum = 0.0;
        for (int i = 0; i < x.length; i++) {
            sum += x[i] * y[i];
        }

        return sum;
    }

    /**
     * Scale each element of an array by a constant x = a * x.
     *
     * @param a the scale factor.
     * @param x the input and output vector.
     */
    public static void scale(double a, double[] x) {
        for (int i = 0; i < x.length; i++) {
            x[i] *= a;
        }
    }

    /**
     * Returns the column means of a matrix.
     *
     * @param matrix the matrix.
     * @return the column means.
     */
    public static double[] colMeans(double[][] matrix) {
        double[] x = matrix[0].clone();

        for (int i = 1; i < matrix.length; i++) {
            for (int j = 0; j < x.length; j++) {
                x[j] += matrix[i][j];
            }
        }

        scale(1.0 / matrix.length, x);

        return x;
    }

    /**
     * Returns the sample covariance matrix.
     *
     * @param data the samples
     * @return the covariance matrix.
     */
    public static double[][] cov(double[][] data) {
        return cov(data, colMeans(data));
    }

    /**
     * Returns the sample covariance matrix.
     *
     * @param data the samples
     * @param mu   the known mean of data.
     * @return the covariance matrix.
     */
    public static double[][] cov(double[][] data, double[] mu) {
        double[][] sigma = new double[data[0].length][data[0].length];
        for (double[] datum : data) {
            for (int j = 0; j < mu.length; j++) {
                for (int k = 0; k <= j; k++) {
                    sigma[j][k] += (datum[j] - mu[j]) * (datum[k] - mu[k]);
                }
            }
        }

        int n = data.length - 1;
        for (int j = 0; j < mu.length; j++) {
            for (int k = 0; k <= j; k++) {
                sigma[j][k] /= n;
                sigma[k][j] = sigma[j][k];
            }
        }

        return sigma;
    }

    /**
     * Returns the index of maximum value of an array.
     *
     * @param x the array.
     * @return the index of maximum.
     */
    public static int whichMax(double[] x) {
        double max = Double.NEGATIVE_INFINITY;
        int which = 0;

        for (int i = 0; i < x.length; i++) {
            if (x[i] > max) {
                max = x[i];
                which = i;
            }
        }

        return which;
    }

    /**
     * Standardizes an array to mean 0 and variance 1.
     *
     * @param x the matrix.
     */
    public static void standardize(double[] x) {
        double mu = mean(x);
        double sigma = sd(x);

        if (isZero(sigma)) {

            LOG.warn("array has variance of 0.");
            return;
        }

        for (int i = 0; i < x.length; i++) {
            x[i] = (x[i] - mu) / sigma;
        }
    }

    /**
     * Swap two positions.
     *
     * @param x the array.
     * @param i the index of array element.
     * @param j the index of other element.
     */
    static void swap(int[] x, int i, int j) {
        int a = x[i];
        x[i] = x[j];
        x[j] = a;
    }

    /**
     * Swap two positions.
     *
     * @param x the array.
     * @param i the index of array element.
     * @param j the index of other element.
     */
    static void swap(double[] x, int i, int j) {
        double a;
        a = x[i];
        x[i] = x[j];
        x[j] = a;
    }

    /**
     * This is an efficient implementation Quick Sort algorithm without
     * recursive. Besides sorting the first n elements of array x, the first
     * n elements of array y will be also rearranged as the same order of x.
     *
     * @param x the array to sort.
     * @param y the associate array.
     * @param n the first n elements to sort.
     */
    public static void sort(double[] x, int[] y, int n) {
        int jstack = -1;
        int l = 0;
        int[] istack = new int[NSTACK];
        int ir = n - 1;

        int i, j, k;
        double a;
        int b;
        for (; ; ) {
            if (ir - l < M) {
                for (j = l + 1; j <= ir; j++) {
                    a = x[j];
                    b = y[j];
                    for (i = j - 1; i >= l; i--) {
                        if (x[i] <= a) {
                            break;
                        }
                        x[i + 1] = x[i];
                        y[i + 1] = y[i];
                    }
                    x[i + 1] = a;
                    y[i + 1] = b;
                }
                if (jstack < 0) {
                    break;
                }
                ir = istack[jstack--];
                l = istack[jstack--];
            } else {
                k = (l + ir) >> 1;
                swap(x, k, l + 1);
                swap(y, k, l + 1);
                if (x[l] > x[ir]) {
                    swap(x, l, ir);
                    swap(y, l, ir);
                }
                if (x[l + 1] > x[ir]) {
                    swap(x, l + 1, ir);
                    swap(y, l + 1, ir);
                }
                if (x[l] > x[l + 1]) {
                    swap(x, l, l + 1);
                    swap(y, l, l + 1);
                }
                i = l + 1;
                j = ir;
                a = x[l + 1];
                b = y[l + 1];
                for (; ; ) {
                    do {
                        i++;
                    } while (x[i] < a);
                    do {
                        j--;
                    } while (x[j] > a);
                    if (j < i) {
                        break;
                    }
                    swap(x, i, j);
                    swap(y, i, j);
                }
                x[l + 1] = x[j];
                x[j] = a;
                y[l + 1] = y[j];
                y[j] = b;
                jstack += 2;

                if (jstack >= NSTACK) {
                    throw new IllegalStateException("NSTACK too small in sort.");
                }

                if (ir - i + 1 >= j - l) {
                    istack[jstack] = ir;
                    istack[jstack - 1] = i;
                    ir = j - 1;
                } else {
                    istack[jstack] = j - 1;
                    istack[jstack - 1] = l;
                    l = i;
                }
            }
        }
    }

    /**
     * Besides sorting the array x, the array y will be also
     * rearranged as the same order of x.
     *
     * @param x the array to sort.
     * @param y the associate array.
     */
    public static void sort(double[] x, int[] y) {
        sort(x, y, x.length);
    }

    /**
     * Sorts the specified array into ascending numerical order.
     *
     * @param x the array to sort.
     * @return the original index of elements after sorting in range [0, n).
     */
    public static int[] sort(double[] x) {
        int[] order = new int[x.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        sort(x, order);
        return order;
    }

    /**
     * Element-wise sum of two arrays y = x + y.
     *
     * @param x a vector.
     * @param y avector.
     */
    public static void add(double[] y, double[] x) {
        if (x.length != y.length) {
            throw new IllegalArgumentException(String.format("Arrays have different length: x[%d], y[%d]", x.length, y.length));
        }

        for (int i = 0; i < x.length; i++) {
            y[i] += x[i];
        }
    }

    /**
     * Element-wise subtraction of two arrays y = y - x.
     *
     * @param y the minuend array.
     * @param x the subtrahend array.
     */
    public static void sub(double[] y, double[] x) {
        if (x.length != y.length) {
            throw new IllegalArgumentException(String.format("Arrays have different length: x[%d], y[%d]", x.length, y.length));
        }

        for (int i = 0; i < x.length; i++) {
            y[i] -= x[i];
        }
    }
}
