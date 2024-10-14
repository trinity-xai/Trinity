/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils.umap;

import java.util.Random;

/**
 * Flattened tree.
 *
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class FlatTree {

    // Used for a doubleing point "nearly zero" comparison
    private static final double EPS = 1e-8F;

    private final Object mHyperplanes;
    private final double[] mOffsets;
    private final int[][] mChildren;
    private final int[][] mIndices;

    FlatTree(final Object hyperplanes, final double[] offsets, final int[][] children, final int[][] indices) {
        mHyperplanes = hyperplanes;
        mOffsets = offsets;
        mChildren = children;
        mIndices = indices;
    }

    int[][] getIndices() {
        return mIndices;
    }

    private static boolean selectSide(final double[] hyperplane, final double offset, final double[] point, final Random random) {
        double margin = offset;
        for (int d = 0; d < point.length; ++d) {
            margin += hyperplane[d] * point[d];
        }
        if (Math.abs(margin) < EPS) {
            return random.nextBoolean();
        } else {
            return margin <= 0;
        }
    }

    int[] searchFlatTree(final double[] point, final Random random) {
        int node = 0;
        while (mChildren[node][0] > 0) {
            final boolean side = selectSide(((double[][]) mHyperplanes)[node], mOffsets[node], point, random);
            node = mChildren[node][side ? 1 : 0];
        }
        return mIndices[-mChildren[node][0]];
    }
}
