/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
package edu.jhuapl.trinity.utils.umap;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Arrays;
import java.util.Random;

/**
 * Math utilities equivalent to Python numpy functionality.
 *
 * @author Sean A. Irvine
 * @author Richard Littin
 */
final class MathUtils {

    private MathUtils() {
    }

    private static final double INV_LOG2 = 1.0 / Math.log(2);

    static double log2(final double x) {
        return Math.log(x) * INV_LOG2;
    }

    static double max(final double... x) {
        double max = Float.NEGATIVE_INFINITY;
        for (final double v : x) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    static double min(final double... x) {
        double min = Float.POSITIVE_INFINITY;
        for (final double v : x) {
            if (v < min) {
                min = v;
            }
        }
        return min;
    }

    static double mean(final double... x) {
        double s = 0;
        for (final double v : x) {
            s += v;
        }
        return s / x.length;
    }

    static double mean(final double[][] x) {
        double s = 0;
        long c = 0;
        for (final double[] row : x) {
            for (final double v : row) {
                s += v;
                ++c;
            }
        }
        return s / c;
    }

    static float sum(final float[][] x) {
        float s = 0;
        for (final float[] row : x) {
            for (final float v : row) {
                s += v;
            }
        }
        return s;
    }

    static double sum(final double[][] x) {
        double s = 0;
        for (final double[] row : x) {
            for (final double v : row) {
                s += v;
            }
        }
        return s;
    }

    /**
     * Retain only positive members of x in a new array.
     *
     * @param x array
     * @return positives
     */
    static double[] filterPositive(final double... x) {
        int len = 0;
        for (final double v : x) {
            if (v > 0) {
                ++len;
            }
        }
        final double[] res = new double[len];
        int k = 0;
        for (final double v : x) {
            if (v > 0) {
                res[k++] = v;
            }
        }
        return res;
    }

    static boolean containsNegative(final int[][] x) {
        for (final int[] row : x) {
            for (final int v : row) {
                if (v < 0) {
                    return true;
                }
            }
        }
        return false;
    }

    static double[] multiply(final double[] x, final double s) {
        final double[] res = new double[x.length];
        for (int k = 0; k < x.length; ++k) {
            res[k] = x[k] * s;
        }
        return res;
    }

    static double[] divide(final double[] x, final double s) {
        return multiply(x, 1.0F / s);
    }

    static double[] linspace(final double start, final double end, final int n) {
        final double[] res = new double[n];
        final double span = end - start;
        final double step = span / (n - 1);
        for (int k = 0; k < res.length; ++k) {
            res[k] = start + k * step;
        }
        return res;
    }

    static int[] identity(final int n) {
        final int[] id = new int[n];
        for (int k = 0; k < n; ++k) {
            id[k] = k;
        }
        return id;
    }

    static int[] argsort(final double[] x) {
        final int[] id = identity(x.length);
        Sort.sort(x, id);
        return id;
    }

    static void zeroEntriesBelowLimit(final double[] x, final double limit) {
        for (int k = 0; k < x.length; ++k) {
            if (x[k] < limit) {
                x[k] = 0;
            }
        }
    }

    static double[][] subarray(final double[][] x, final int cols) {
        final double[][] res = new double[x.length][];
        for (int k = 0; k < x.length; ++k) {
            res[k] = Arrays.copyOf(x[k], cols);
        }
        return res;
    }

    static int[][] subarray(final int[][] x, final int cols) {
        final int[][] res = new int[x.length][];
        for (int k = 0; k < x.length; ++k) {
            res[k] = Arrays.copyOf(x[k], cols);
        }
        return res;
    }

    // Do equivalent of numpy: x[np.newaxis, :].T
    static Matrix promoteTranspose(final double[] a) {
        final double[][] res = new double[a.length][1];
        for (int k = 0; k < a.length; ++k) {
            res[k][0] = a[k];
        }
        return new DefaultMatrix(res);
    }

    static double[][] uniform(final Random random, final double lo, final double hi, final int n, final int m) {
        // replacement for numpy.random.RandomState.uniform (2D specialization)
        final double len = hi - lo;
        if (len <= 0) {
            throw new IllegalArgumentException("lo must be smaller than hi");
        }
        final double[][] res = new double[n][m];
        for (int k = 0; k < n; ++k) {
            for (int j = 0; j < m; ++j) {
                res[k][j] = lo + random.nextFloat() * len;
            }
        }
        return res;
    }

    static int[] subarray(final int[] a, final int lo, final int hi) {
        final int[] res = new int[hi - lo];
        System.arraycopy(a, lo, res, 0, res.length);
        return res;
    }

    static double[] subarray(final double[] a, final int lo, final int hi) {
        final double[] res = new double[hi - lo];
        System.arraycopy(a, lo, res, 0, res.length);
        return res;
    }
}
