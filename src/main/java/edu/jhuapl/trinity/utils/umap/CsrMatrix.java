/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils.umap;

import java.util.Arrays;

/**
 * A form of sparse matrix where only non-zero entries are explicitly recorded.
 * This format is compatible with the Python scipy <code>csr_matrix</code> format.
 *
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class CsrMatrix extends Matrix {

    private final int[] mIndptr;  // indptr[row] to indptr[row + 1] locations of cols in indices
    private final int[] mIndices; // positions of actual data
    private final double[] mData;

    CsrMatrix(final double[] data, final int[] indptr, final int[] indices, final int rowCount, final int colCount) {
        super(rowCount, colCount);
        mIndptr = indptr;
        mIndices = indices;
        mData = data;
    }

    @Override
    double get(final int row, final int col) {
        final int colStart = mIndptr[row];
        final int colEnd = mIndptr[row + 1];
        for (int p = colStart; p < colEnd; ++p) {
            if (mIndices[p] == col) {
                return mData[p];
            }
        }
        return 0;
    }

    @Override
    void set(final int row, final int col, final double val) {
        throw new UnsupportedOperationException();
    }

    @Override
    boolean isFinite() {
        for (final double v : mData) {
            if (!Double.isFinite(v)) {
                return false;
            }
        }
        return true;
    }

    @Override
    Matrix copy() {
        return new CsrMatrix(Arrays.copyOf(mData, mData.length), Arrays.copyOf(mIndptr, mIndptr.length), Arrays.copyOf(mIndices, mIndices.length), rows(), cols());
    }

    @Override
    CsrMatrix toCsr() {
        return this;
    }

    @Override
    Matrix add(final Matrix m) {
        // Sparse implementation would be better than using super.
        //return super.add(m).toCsr();
        throw new UnsupportedOperationException();
    }

    @Override
    Matrix subtract(final Matrix m) {
        // Sparse implementation would be better than using super.
        //return super.subtract(m).toCsr();
        throw new UnsupportedOperationException();
    }

    @Override
    Matrix multiply(final Matrix m) {
        // Sparse implementation would be better than using super.
        //return super.multiply(m).toCsr();
        throw new UnsupportedOperationException();
    }

    @Override
    Matrix transpose() {
        // Sparse implementation would be better than using super.
        //return super.transpose().toCsr();
        throw new UnsupportedOperationException();
    }

    @Override
    Matrix multiply(final double x) {
        final double[] newData = Arrays.copyOf(mData, mData.length);
        for (int i = 0; i < newData.length; ++i) {
            newData[i] *= x;
        }
        return new CsrMatrix(newData, mIndptr, mIndices, rows(), cols());
    }

    @Override
    Matrix rowNormalize() {
        final double[] d = new double[mData.length];
        for (int row = 0; row < rows(); ++row) {
            double max = mData[mIndptr[row]];
            for (int j = mIndptr[row] + 1; j < mIndptr[row + 1]; ++j) {
                max = Math.max(max, mData[j]);
            }
            for (int j = mIndptr[row]; j < mIndptr[row + 1]; ++j) {
                d[j] = mData[j] / max;
            }
        }
        // Note would be safer to copy mIndptr and mIndices arrays
        return new CsrMatrix(d, mIndptr, mIndices, rows(), cols());
    }

    @Override
    Matrix l1Normalize() {
        final double[] d = new double[mData.length];
        for (int row = 0; row < rows(); ++row) {
            double l1 = 0;
            for (int j = mIndptr[row]; j < mIndptr[row + 1]; ++j) {
                l1 += Math.abs(mData[j]);
            }
            for (int j = mIndptr[row]; j < mIndptr[row + 1]; ++j) {
                d[j] = mData[j] / l1;
            }
        }
        // Note would be safer to copy mIndptr and mIndices arrays
        return new CsrMatrix(d, mIndptr, mIndices, rows(), cols());
    }

    int[][] reshapeIndicies(final int rows, final int cols) {
        final int[][] res = new int[rows][cols];
        for (int row = 0; row < rows; ++row) {
            final int end = mIndptr[row + 1];
            // evilness here implicit self match at position 0
            for (int col = 0, pos = mIndptr[row]; col < cols && pos < end; ++col, ++pos) {
                res[row][col] = mIndices[pos];
            }
        }
        return res;
    }

    double[][] reshapeWeights(final int rows, final int cols) {
        final double[][] res = new double[rows][cols];
        for (int row = 0; row < rows; ++row) {
            final int end = mIndptr[row + 1];
            for (int col = 0, pos = mIndptr[row]; col < cols && pos < end; ++col, ++pos) {
                res[row][col] = mData[pos];
            }
        }
        return res;
    }

    void intersect(final CsrMatrix other, final CooMatrix result, final double mixWeight) {

        final double leftMin = Math.max(MathUtils.min(mData) / 2, 1.0e-8F);
        final double rightMin = Math.max(MathUtils.min(other.mData) / 2, 1.0e-8F);

        final int[] row = result.row();
        final int[] col = result.col();
        final double[] data = result.data();
        for (int idx = 0; idx < row.length; ++idx) {
            final int i = row[idx];
            final int j = col[idx];

            double leftVal = leftMin;
            for (int k = mIndptr[i]; k < mIndptr[i + 1]; ++k) {
                if (mIndices[k] == j) {
                    leftVal = mData[k];
                }
            }

            double rightVal = rightMin;
            for (int k = other.mIndptr[i]; k < other.mIndptr[i + 1]; ++k) {
                if (other.mIndices[k] == j) {
                    rightVal = other.mData[k];
                }
            }

            if (leftVal > leftMin || rightVal > rightMin) {
                final double f;
                if (mixWeight < 0.5) {
                    f = (double) (leftVal * Math.pow(rightVal, mixWeight / (1.0 - mixWeight)));
                } else {
                    f = (double) (Math.pow(leftVal, (1.0 - mixWeight) / mixWeight) * rightVal);
                }
                data[idx] = f;
            }
        }
    }

    SparseVector vector(final int row) {
        return new SparseVector(Arrays.copyOfRange(mIndices, mIndptr[row], mIndptr[row + 1]),
            Arrays.copyOfRange(mData, mIndptr[row], mIndptr[row + 1]));
    }
}
