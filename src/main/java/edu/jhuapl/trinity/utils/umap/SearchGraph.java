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

import java.util.Set;
import java.util.TreeSet;

/**
 * Stores unordered pairs.
 *
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class SearchGraph {

    private final TreeSet<Integer>[] mRows;

    @SuppressWarnings("unchecked")
    SearchGraph(final int rows) {
        mRows = (TreeSet<Integer>[]) new TreeSet[rows];
        for (int k = 0; k < rows; ++k) {
            mRows[k] = new TreeSet<>();
        }
    }

    /**
     * Set the unordered pair of instances.
     *
     * @param x instance index
     * @param y instance index
     */
    void set(final int x, final int y) {
        mRows[x].add(y);
        mRows[y].add(x);
    }

    /**
     * Set of indices for an instance.
     *
     * @param row instance number
     * @return set of instance numbers
     */
    Set<Integer> row(final int row) {
        return mRows[row];
    }
}
