/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils;

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
