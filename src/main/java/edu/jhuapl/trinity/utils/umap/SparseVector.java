/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.umap;

import java.util.Arrays;

/**
 * Vector.
 *
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class SparseVector {

    private final int[] mIndices;
    private final double[] mData;

    /**
     * Vector.
     *
     * @param indices indices of nonzero elements
     * @param data    nonzero elements
     */
    SparseVector(final int[] indices, final double[] data) {
        if (data.length != indices.length) {
            throw new IllegalArgumentException();
        }
        mIndices = indices;
        mData = data;
    }

    int[] getIndices() {
        return mIndices;
    }

    double[] getData() {
        return mData;
    }

    double norm() {
        return Utils.norm(mData);
    }

    void divide(final double v) {
        for (int k = 0; k < mData.length; ++k) {
            mData[k] /= v;
        }
    }

    SparseVector add(final SparseVector right) {
        // Maximum length if the sum of the two input lengths
        final int[] resultInd = new int[mIndices.length + right.mIndices.length];
        final double[] resultData = new double[resultInd.length];

        int i1 = 0;
        int i2 = 0;
        int nnz = 0;

        // pass through both index lists
        while (i1 < mIndices.length && i2 < right.mIndices.length) {
            final int j1 = mIndices[i1];
            final int j2 = right.mIndices[i2];

            if (j1 == j2) {
                final double val = mData[i1++] + right.mData[i2++];
                if (val != 0) {
                    resultInd[nnz] = j1;
                    resultData[nnz++] = val;
                }
            } else if (j1 < j2) {
                final double val = mData[i1++];
                if (val != 0) {
                    resultInd[nnz] = j1;
                    resultData[nnz++] = val;
                }
            } else {
                final double val = right.mData[i2++];
                if (val != 0) {
                    resultInd[nnz] = j2;
                    resultData[nnz++] = val;
                }
            }
        }

        // pass over the tails
        while (i1 < mIndices.length) {
            final double val = mData[i1];
            if (val != 0) {
                resultInd[nnz] = mIndices[i1];
                resultData[nnz++] = val;
            }
            ++i1;
        }

        while (i2 < right.mIndices.length) {
            final double val = right.mData[i2];
            if (val != 0) {
                resultInd[nnz] = right.mIndices[i2];
                resultData[nnz++] = val;
            }
            ++i2;
        }

        if (nnz == resultInd.length) {
            return new SparseVector(resultInd, resultData);
        } else {
            return new SparseVector(Arrays.copyOf(resultInd, nnz), Arrays.copyOf(resultData, nnz));
        }
    }

    SparseVector subtract(final SparseVector right) {
        // Maximum length if the sum of the two input lengths
        final int[] resultInd = new int[mIndices.length + right.mIndices.length];
        final double[] resultData = new double[resultInd.length];

        int i1 = 0;
        int i2 = 0;
        int nnz = 0;

        // pass through both index lists
        while (i1 < mIndices.length && i2 < right.mIndices.length) {
            final int j1 = mIndices[i1];
            final int j2 = right.mIndices[i2];

            if (j1 == j2) {
                final double val = mData[i1++] - right.mData[i2++];
                if (val != 0) {
                    resultInd[nnz] = j1;
                    resultData[nnz++] = val;
                }
            } else if (j1 < j2) {
                final double val = mData[i1++];
                if (val != 0) {
                    resultInd[nnz] = j1;
                    resultData[nnz++] = val;
                }
            } else {
                final double val = right.mData[i2++];
                if (val != 0) {
                    resultInd[nnz] = j2;
                    resultData[nnz++] = -val;
                }
            }
        }

        // pass over the tails
        while (i1 < mIndices.length) {
            final double val = mData[i1];
            if (val != 0) {
                resultInd[nnz] = mIndices[i1];
                resultData[nnz++] = val;
            }
            ++i1;
        }

        while (i2 < right.mIndices.length) {
            final double val = right.mData[i2];
            if (val != 0) {
                resultInd[nnz] = right.mIndices[i2];
                resultData[nnz++] = -val;
            }
            ++i2;
        }

        if (nnz == resultInd.length) {
            return new SparseVector(resultInd, resultData);
        } else {
            return new SparseVector(Arrays.copyOf(resultInd, nnz), Arrays.copyOf(resultData, nnz));
        }
    }

    SparseVector hadamardMultiply(SparseVector right) {
        // Maximum length is the minimum length of the inputs
        final int[] resultInd = new int[Math.min(mIndices.length, right.mIndices.length)];
        final double[] resultData = new double[resultInd.length];

        int i1 = 0;
        int i2 = 0;
        int nnz = 0;

        // pass through both index lists
        while (i1 < mIndices.length && i2 < right.mIndices.length) {
            final int j1 = mIndices[i1];
            final int j2 = right.mIndices[i2];

            if (j1 == j2) {
                final double val = mData[i1++] * right.mData[i2++];
                if (val != 0) {
                    resultInd[nnz] = j1;
                    resultData[nnz++] = val;
                }
            } else if (j1 < j2) {
                ++i1;
            } else {
                ++i2;
            }
        }

        if (nnz == resultInd.length) {
            return new SparseVector(resultInd, resultData);
        } else {
            return new SparseVector(Arrays.copyOf(resultInd, nnz), Arrays.copyOf(resultData, nnz));
        }
    }

    double sum() {
        double sum = 0;
        for (final double v : mData) {
            sum += v;
        }
        return sum;
    }

}
