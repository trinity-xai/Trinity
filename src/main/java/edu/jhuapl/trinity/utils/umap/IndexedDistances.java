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

import java.util.List;

/**
 * Container for indices and distances.
 *
 * @author Sean A. Irvine
 * @author Richard Littin
 */
class IndexedDistances {

    private final int[][] mIndices;
    private final float[][] mDistances;
    private final List<FlatTree> mForest;

    IndexedDistances(final int[][] indices, final float[][] distances, final List<FlatTree> forest) {
        mIndices = indices;
        mDistances = distances;
        mForest = forest;
    }

    int[][] getIndices() {
        return mIndices;
    }

    float[][] getDistances() {
        return mDistances;
    }

    List<FlatTree> getForest() {
        return mForest;
    }
}
