package edu.jhuapl.trinity.data;

/*-
 * #%L
 * trinity
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

import edu.jhuapl.trinity.data.messages.FeatureVector;

import java.util.Comparator;

/**
 * @author Sean Phillips
 */
public class FeatureVectorComparator implements Comparator<FeatureVector> {
    private int compareIndex = 0;

    public FeatureVectorComparator(int compareIndex) {
        this.compareIndex = compareIndex;
    }

    @Override
    public int compare(FeatureVector o1, FeatureVector o2) {
        if (o1.getData().get(getCompareIndex()) > o2.getData().get(getCompareIndex())) {
            return 1;
        } else if (o1.getData().get(getCompareIndex()) < o2.getData().get(getCompareIndex())) {
            return -1;
        } else
            return 0;
    }

    /**
     * @return the compareIndex
     */
    public int getCompareIndex() {
        return compareIndex;
    }

    /**
     * @param compareIndex the compareIndex to set
     */
    public void setCompareIndex(int compareIndex) {
        this.compareIndex = compareIndex;
    }


}
