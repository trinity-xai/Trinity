package edu.jhuapl.trinity.utils;

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

import edu.jhuapl.trinity.javafx.javafx3d.Perspective3DNode;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Sean Phillips
 */
public enum VisibilityMap {
    INSTANCE;

    public static HashMap<Perspective3DNode, Boolean> pNodeVisibilityMap = new HashMap<>();
    public static ArrayList<Boolean> visibilityList = new ArrayList<>();

    public static boolean visibilityByPNode(Perspective3DNode pNode) {
        return pNodeVisibilityMap.getOrDefault(pNode, true);
    }

    public static boolean visibilityByIndex(int index) {
        return index < visibilityList.size() ? visibilityList.get(index) : true;
    }

    public static void clearAll() {
        visibilityList.clear();
        pNodeVisibilityMap.clear();
    }

    public static void resetVisibilityList(Perspective3DNode[] pNodes, boolean defaultVisibility) {
        visibilityList = new ArrayList<>(pNodes.length);
        pNodeVisibilityMap = new HashMap<>(pNodes.length);
        for (int i = 0; i < pNodes.length; i++) {
            visibilityList.add(defaultVisibility);
            pNodeVisibilityMap.put(pNodes[i], defaultVisibility);
        }
    }
}
