package edu.jhuapl.trinity.utils.graph;

import java.util.List;

/**
 * Generic 3D layout engine interface.
 *
 * @author Sean Phillips
 */
public interface GraphLayoutEngine {

    /**
     * Compute 3D coordinates for N nodes.
     *
     * @param n         number of nodes
     * @param labels    optional labels (length n) or null
     * @param distances optional NxN distance matrix; may be null if the engine doesn’t need it
     * @param weights   optional NxN edge-weight matrix; may be null if the engine doesn’t need it
     * @param params    engine parameters (tunable)
     * @return double[n][3] positions (x,y,z) in the same order as labels/nodes
     */
    double[][] layout(int n,
                      List<String> labels,
                      double[][] distances,
                      double[][] weights,
                      GraphLayoutParams params);
}
