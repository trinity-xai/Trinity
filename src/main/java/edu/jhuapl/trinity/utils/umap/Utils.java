/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.umap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

/**
 * Utility functions.
 *
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 */
final class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private Utils() {
    }

    /**
     * Get the current date and time as a string of the form
     * <code>YYYY-MM-DD hh:mm:ss</code>.
     *
     * @return date string
     */
    static String now() {
        final StringBuilder sb = new StringBuilder();
        final Calendar cal = new GregorianCalendar();
        sb.append(cal.get(Calendar.YEAR)).append('-');
        final int month = 1 + cal.get(Calendar.MONTH);
        if (month < 10) {
            sb.append('0');
        }
        sb.append(month).append('-');
        final int date = cal.get(Calendar.DATE);
        if (date < 10) {
            sb.append('0');
        }
        sb.append(date).append(' ');
        final int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 10) {
            sb.append('0');
        }
        sb.append(hour).append(':');
        final int min = cal.get(Calendar.MINUTE);
        if (min < 10) {
            sb.append('0');
        }
        sb.append(min).append(':');
        final int sec = cal.get(Calendar.SECOND);
        if (sec < 10) {
            sb.append('0');
        }
        sb.append(sec).append(' ');
        return sb.toString();
    }

    /**
     * Print a dated message on standard output.
     *
     * @param message message to print
     */
    static void message(final String message) {
        LOG.info("{}{}", now(), message);
    }

    /**
     * A fast computation of knn indices.
     *
     * @param instances  array of shape <code>(nSamples, nFeatures)</code>
     * @param nNeighbors the number of nearest neighbors to compute for each sample in <code>instances</code>
     * @return array of shape <code>(nSamples, nNeighbors)</code> containing the indices of the <code>nNeighbours</code>
     * closest points in the dataset.
     */
    static int[][] fastKnnIndices(final Matrix instances, final int nNeighbors) {
        final int[][] knnIndices = new int[instances.rows()][nNeighbors];
        for (int row = 0; row < instances.rows(); ++row) {
            final int[] v = MathUtils.argsort(Arrays.copyOf(instances.row(row), instances.cols())); // copy to avoid changing original instances
            knnIndices[row] = Arrays.copyOf(v, nNeighbors); // todo Math.min(nNeighbors, v.length) ???
        }
        return knnIndices;
    }

    /**
     * L2 norm of a vector.
     *
     * @param vec vector
     * @return L2 norm
     */
    static double norm(final double[] vec) {
        double result = 0;
        for (final double v : vec) {
            result += v * v;
        }
        return (double) Math.sqrt(result);
    }

    /**
     * Generate <code>nSamples</code> many integers from 0 to <code>poolSize</code> such that no
     * integer is selected twice. The duplication constraint is achieved via
     * rejection sampling.
     *
     * @param nSamples The number of random samples to select from the pool
     * @param poolSize The size of the total pool of candidates to sample from
     * @param random   Randomness source
     * @return <code>nSamples </code>randomly selected elements from the pool.
     */
    static int[] rejectionSample(final int nSamples, final int poolSize, final Random random) {
        if (nSamples > poolSize) {
            throw new IllegalArgumentException();
        }
        final int[] result = new int[nSamples];
        for (int i = 0; i < result.length; ++i) {
            int j;
            boolean ok;
            do {
                j = random.nextInt(poolSize);
                ok = true;
                for (int k = 0; k < i; ++k) {
                    if (j == result[k]) {
                        ok = false;
                        break;
                    }
                }
            } while (!ok);
            result[i] = j;
        }
        return result;
    }

    /**
     * Return a submatrix given an original matrix and the indices to keep.
     *
     * @param matrix     Original matrix of shape <code>(nSamples, nSamples)</code>.
     * @param indicesCol Indices to keep of shape <code>(nSamples, nNeighbors)</code>.
     *                   Each row consists of the indices of the columns.
     * @param nNeighbors Number of neighbors.
     * @return array, shape <code>(nSamples, nNeighbors)</code>
     * The corresponding submatrix.
     */
    static double[][] submatrix(Matrix matrix, int[][] indicesCol, int nNeighbors) {
        final int nSamplesTransform = matrix.rows();
        final double[][] submat = new double[nSamplesTransform][nNeighbors];
        for (int i = 0; i < nSamplesTransform; ++i) {
            for (int j = 0; j < nNeighbors; ++j) {
                submat[i][j] = matrix.get(i, indicesCol[i][j]);
            }
        }
        return submat;
    }

    static Random[] splitRandom(final Random random, final int n) {
        final Random[] randoms = new Random[n];
        final long baseSeed = random.nextLong();
        for (int j = 0; j < n; ++j) {
            randoms[j] = new Random(baseSeed * (j + 1) + j);
        }
        return randoms;
    }
}
