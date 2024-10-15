/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */

/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils.umap;

public class ProgressState {
    private final int mTotal;
    private final int mCount;

    protected ProgressState(final int total, final int count) {
        mTotal = total;
        mCount = count;
    }

    public int getTotal() {
        return mTotal;
    }

    public int getCount() {
        return mCount;
    }
}
