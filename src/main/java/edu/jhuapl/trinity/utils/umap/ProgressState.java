/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


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
