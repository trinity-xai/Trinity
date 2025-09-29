package edu.jhuapl.trinity.utils.graph;

import java.io.Serial;
import java.io.Serializable;

/**
 * Tunable parameters for 3D layouts (used by MDS wrapper and Force-FR).
 *
 * @author Sean Phillips
 */
public final class GraphLayoutParams implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public enum LayoutKind { CIRCLE_XZ, CIRCLE_XY, SPHERE, MDS_3D, FORCE_FR }

    /** Which layout engine to use. */
    public LayoutKind kind = LayoutKind.SPHERE;

    /** Radius / scale for static layouts and initial placement for force layouts. */
    public double radius = 600.0;

    // --- Force-FR knobs ---
    /** Number of force iterations (ticks). */
    public int iterations = 500;
    /** Global step size / temperature start (will cool). */
    public double step = 1.0;
    /** Repulsion constant Cr (higher → more spread). */
    public double repulsion = 900.0;
    /** Attraction constant Ca (higher → tighter edges). */
    public double attraction = 0.08;
    /** Gravity pulls nodes toward origin to avoid drift. */
    public double gravity = 0.02;
    /** Cooling schedule factor (0..1). Smaller → faster cooling. */
    public double cooling = 0.96;

    // --- Edge building from matrix ---
    /** Keep at most this many heaviest edges (per-node cap applied after global sort). */
    public int maxEdges = 5000;
    /** Optional per-node degree cap; <=0 disables. */
    public int maxDegreePerNode = 32;
    /** Cut edges below this weight (after transform). */
    public double minEdgeWeight = 0.0;

    /** When true, normalize input matrix to [0,1] before transforms. */
    public boolean normalizeWeights01 = true;

    public GraphLayoutParams withKind(LayoutKind k) { this.kind = k; return this; }
    public GraphLayoutParams withRadius(double r) { this.radius = r; return this; }

    // Force
    public GraphLayoutParams withIterations(int it) { this.iterations = it; return this; }
    public GraphLayoutParams withStep(double s) { this.step = s; return this; }
    public GraphLayoutParams withRepulsion(double r) { this.repulsion = r; return this; }
    public GraphLayoutParams withAttraction(double a) { this.attraction = a; return this; }
    public GraphLayoutParams withGravity(double g) { this.gravity = g; return this; }
    public GraphLayoutParams withCooling(double c) { this.cooling = c; return this; }

    // Edges
    public GraphLayoutParams withMaxEdges(int e) { this.maxEdges = e; return this; }
    public GraphLayoutParams withMaxDegreePerNode(int d) { this.maxDegreePerNode = d; return this; }
    public GraphLayoutParams withMinEdgeWeight(double w) { this.minEdgeWeight = w; return this; }
    public GraphLayoutParams withNormalizeWeights01(boolean on) { this.normalizeWeights01 = on; return this; }
}
