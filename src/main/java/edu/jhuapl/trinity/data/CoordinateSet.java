package edu.jhuapl.trinity.data;

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

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Sean Phillips
 */
public class CoordinateSet {
    public ArrayList<Integer> coordinateIndices;

    public CoordinateSet(int x, int y, int z) {
        coordinateIndices = new ArrayList<>();
        Collections.addAll(coordinateIndices, x, y, z);
    }

    public CoordinateSet(ArrayList<Integer> indices) {
        coordinateIndices = indices;
    }
}
