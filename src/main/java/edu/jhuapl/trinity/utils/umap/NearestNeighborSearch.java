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

import edu.jhuapl.trinity.utils.umap.metric.Metric;

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
                final float d = mDist.distance(data.row(index), queryPoints.row(i));
                heap.push(i, d, index, true);
            }
        }
    }

    void randomInit(final int nNeighbors, final Matrix data, final Matrix queryPoints, final Heap heap, final Random random) {
        for (int i = 0; i < queryPoints.rows(); ++i) {
            final int[] indices = Utils.rejectionSample(nNeighbors, data.rows(), random);
            for (final int index : indices) {
                final float d = mDist.distance(data.row(index), queryPoints.row(i));
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
                    final float d = mDist.distance(data.row(candidate), queryPoints.row(i));
                    initialization.uncheckedHeapPush(i, d, candidate, true);
                    tried.add(candidate);
                }
            }
        }

        return initialization;
    }
}
