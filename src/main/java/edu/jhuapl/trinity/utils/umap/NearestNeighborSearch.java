/*
 * BSD 3-Clause License
 * Original Copyright (C) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See UMAPLicense.txt.
 */


package edu.jhuapl.trinity.utils.umap;

import edu.jhuapl.trinity.utils.metric.Metric;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Nearest neighbor search.
 *
 * @author Leland McInnes (Python)
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class NearestNeighborSearch {

    private final Metric mDist;

    NearestNeighborSearch(final Metric dist) {
        mDist = dist;
    }

    void treeInit(final FlatTree tree, final Matrix data, final Matrix queryPoints, final Heap heap, final Random random) {
        for (int i = 0; i < queryPoints.rows(); ++i) {
            final int[] indices = tree.searchFlatTree(queryPoints.row(i), random);
            for (final int index : indices) {
                if (index < 0) {
                    continue;
                }
                final double d = mDist.distance(data.row(index), queryPoints.row(i));
                heap.push(i, d, index, true);
            }
        }
    }

    void randomInit(final int nNeighbors, final Matrix data, final Matrix queryPoints, final Heap heap, final Random random) {
        for (int i = 0; i < queryPoints.rows(); ++i) {
            final int[] indices = Utils.rejectionSample(nNeighbors, data.rows(), random);
            for (final int index : indices) {
                final double d = mDist.distance(data.row(index), queryPoints.row(i));
                heap.push(i, d, index, true);
            }
        }
    }

    Heap initializedNndSearch(final Matrix data, final SearchGraph searchGraph, Heap initialization, final Matrix queryPoints) {
        for (int i = 0; i < queryPoints.rows(); ++i) {

            final Set<Integer> tried = new TreeSet<>();
            for (final int t : initialization.indices()[i]) {
                tried.add(t);
            }

            while (true) {

                // Find smallest flagged vertex
                final int vertex = initialization.smallestFlagged(i);

                if (vertex == -1) {
                    break;
                }
                for (final int candidate : searchGraph.row(vertex)) {
                    if (candidate == vertex || candidate == -1 || tried.contains(candidate)) {
                        continue;
                    }
                    final double d = mDist.distance(data.row(candidate), queryPoints.row(i));
                    initialization.uncheckedHeapPush(i, d, candidate, true);
                    tried.add(candidate);
                }
            }
        }

        return initialization;
    }
}
