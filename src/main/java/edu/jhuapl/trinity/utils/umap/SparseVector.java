/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
package edu.jhuapl.trinity.utils.umap;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
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

/**
 * Vector.
 *
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class SparseVector {

    private final int[] mIndices;
    private final float[] mData;

    /**
     * Vector.
     *
     * @param indices indices of nonzero elements
     * @param data    nonzero elements
     */
    SparseVector(final int[] indices, final float[] data) {
        if (data.length != indices.length) {
            throw new IllegalArgumentException();
        }
        mIndices = indices;
        mData = data;
    }

    int[] getIndices() {
        return mIndices;
    }

    float[] getData() {
        return mData;
    }

    float norm() {
        return Utils.norm(mData);
    }

    void divide(final float v) {
        for (int k = 0; k < mData.length; ++k) {
            mData[k] /= v;
        }
    }

    SparseVector add(final SparseVector right) {
        // Maximum length if the sum of the two input lengths
        final int[] resultInd = new int[mIndices.length + right.mIndices.length];
        final float[] resultData = new float[resultInd.length];

        int i1 = 0;
        int i2 = 0;
        int nnz = 0;

        // pass through both index lists
        while (i1 < mIndices.length && i2 < right.mIndices.length) {
            final int j1 = mIndices[i1];
            final int j2 = right.mIndices[i2];

            if (j1 == j2) {
                final float val = mData[i1++] + right.mData[i2++];
                if (val != 0) {
                    resultInd[nnz] = j1;
                    resultData[nnz++] = val;
                }
            } else if (j1 < j2) {
                final float val = mData[i1++];
                if (val != 0) {
                    resultInd[nnz] = j1;
                    resultData[nnz++] = val;
                }
            } else {
                final float val = right.mData[i2++];
                if (val != 0) {
                    resultInd[nnz] = j2;
                    resultData[nnz++] = val;
                }
            }
        }

        // pass over the tails
        while (i1 < mIndices.length) {
            final float val = mData[i1];
            if (val != 0) {
                resultInd[nnz] = mIndices[i1];
                resultData[nnz++] = val;
            }
            ++i1;
        }

        while (i2 < right.mIndices.length) {
            final float val = right.mData[i2];
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
        final float[] resultData = new float[resultInd.length];

        int i1 = 0;
        int i2 = 0;
        int nnz = 0;

        // pass through both index lists
        while (i1 < mIndices.length && i2 < right.mIndices.length) {
            final int j1 = mIndices[i1];
            final int j2 = right.mIndices[i2];

            if (j1 == j2) {
                final float val = mData[i1++] - right.mData[i2++];
                if (val != 0) {
                    resultInd[nnz] = j1;
                    resultData[nnz++] = val;
                }
            } else if (j1 < j2) {
                final float val = mData[i1++];
                if (val != 0) {
                    resultInd[nnz] = j1;
                    resultData[nnz++] = val;
                }
            } else {
                final float val = right.mData[i2++];
                if (val != 0) {
                    resultInd[nnz] = j2;
                    resultData[nnz++] = -val;
                }
            }
        }

        // pass over the tails
        while (i1 < mIndices.length) {
            final float val = mData[i1];
            if (val != 0) {
                resultInd[nnz] = mIndices[i1];
                resultData[nnz++] = val;
            }
            ++i1;
        }

        while (i2 < right.mIndices.length) {
            final float val = right.mData[i2];
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
        final float[] resultData = new float[resultInd.length];

        int i1 = 0;
        int i2 = 0;
        int nnz = 0;

        // pass through both index lists
        while (i1 < mIndices.length && i2 < right.mIndices.length) {
            final int j1 = mIndices[i1];
            final int j2 = right.mIndices[i2];

            if (j1 == j2) {
                final float val = mData[i1++] * right.mData[i2++];
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

    float sum() {
        float sum = 0;
        for (final float v : mData) {
            sum += v;
        }
        return sum;
    }

}
