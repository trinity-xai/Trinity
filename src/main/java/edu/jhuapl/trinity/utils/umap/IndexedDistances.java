/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.umap;

import java.util.List;

/**
 * Container for indices and distances.
 *
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class IndexedDistances {

    private final int[][] mIndices;
    private final double[][] mDistances;
    private final List<FlatTree> mForest;

    IndexedDistances(final int[][] indices, final double[][] distances, final List<FlatTree> forest) {
        mIndices = indices;
        mDistances = distances;
        mForest = forest;
    }

    int[][] getIndices() {
        return mIndices;
    }

    double[][] getDistances() {
        return mDistances;
    }

    List<FlatTree> getForest() {
        return mForest;
    }
}
