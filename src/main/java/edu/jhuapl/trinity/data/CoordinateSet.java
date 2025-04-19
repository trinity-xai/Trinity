package edu.jhuapl.trinity.data;

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
