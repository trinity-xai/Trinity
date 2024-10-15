/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */
/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils.umap;

/**
 * Container for a hyperplane.
 *
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class Hyperplane {

    //private final int[] mInds;
    private final double[] mData;
    private final int[] mShape;

    Hyperplane(final int[] inds, final double[] data) {
        //mInds = inds;
        mData = data;
        mShape = inds == null ? new int[]{data.length} : new int[]{inds.length, 2};
    }

    Hyperplane(final double[] data) {
        this(null, data);
    }

    public double[] data() {
        return mData;
    }

    public int[] shape() {
        return mShape;
    }
}
