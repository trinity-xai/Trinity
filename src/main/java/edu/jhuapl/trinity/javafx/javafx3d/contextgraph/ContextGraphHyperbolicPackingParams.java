package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.List;

/** Geometry-only tuning. None of these parameters changes the detected communities. */
public record ContextGraphHyperbolicPackingParams(
        Placement placement, AngularAllocation allocation, double angularSpreadDegrees,
        double siblingGapDegrees, double massInfluence, RadialProfile radialProfile,
        double radialStep, List<Double> customSteps) {

    public enum Placement {
        SEMANTIC_HIERARCHY("Multilevel semantic hierarchy"),
        STRUCTURAL_BACKBONE("Classic Poincare (4B-A default)");
        private final String label;
        Placement(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum AngularAllocation {
        WEIGHTED("Population-weighted spherical sectors"),
        EQUAL("Equal spherical sectors");
        private final String label;
        AngularAllocation(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum RadialProfile {
        AUTO("Auto (coarse-to-fine adaptive)"),
        UNIFORM("Uniform hyperbolic steps"),
        DEPTH_ADAPTIVE("Depth-adaptive steps"),
        CUSTOM("Custom per-layer steps");
        private final String label;
        RadialProfile(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public ContextGraphHyperbolicPackingParams {
        if (placement == null || allocation == null || radialProfile == null)
            throw new IllegalArgumentException("Packing modes are required");
        if (!Double.isFinite(angularSpreadDegrees) || angularSpreadDegrees < 20.0
                || angularSpreadDegrees > 150.0)
            throw new IllegalArgumentException("Angular spread must be in [20, 150] degrees");
        if (!Double.isFinite(siblingGapDegrees) || siblingGapDegrees < 0.0
                || siblingGapDegrees > 25.0)
            throw new IllegalArgumentException("Sibling gap must be in [0, 25] degrees");
        if (!Double.isFinite(massInfluence) || massInfluence < 0.0 || massInfluence > 1.0)
            throw new IllegalArgumentException("Mass influence must be in [0, 1]");
        if (!Double.isFinite(radialStep) || radialStep < 0.15 || radialStep > 2.0)
            throw new IllegalArgumentException("Radial step must be in [0.15, 2.0]");
        customSteps = List.copyOf(customSteps == null ? List.of() : customSteps);
        for (Double value : customSteps) {
            if (value == null || !Double.isFinite(value) || value < 0.05 || value > 4.0)
                throw new IllegalArgumentException("Custom hyperbolic steps must be in [0.05, 4.0]");
        }
        if (radialProfile == RadialProfile.CUSTOM && customSteps.isEmpty())
            throw new IllegalArgumentException("Custom radial profile requires one or more steps");
    }

    public static ContextGraphHyperbolicPackingParams defaults() {
        return new ContextGraphHyperbolicPackingParams(Placement.STRUCTURAL_BACKBONE,
            AngularAllocation.WEIGHTED, 110.0, 5.0, 0.75,
            RadialProfile.AUTO, 0.85, List.of());
    }

    /** Depth zero is the coarsest community shell; the last shell contains entities. */
    public double stepAt(int depth, int layers) {
        if (radialProfile == RadialProfile.CUSTOM)
            return customSteps.get(Math.min(depth, customSteps.size() - 1));
        double fraction = (double) depth / Math.max(1, layers - 1);
        return switch (radialProfile) {
            case AUTO -> radialStep * (1.20 - 0.35 * fraction);
            case DEPTH_ADAPTIVE -> radialStep / (1.0 + 0.75 * fraction);
            case UNIFORM -> radialStep;
            case CUSTOM -> throw new IllegalStateException("Handled above");
        };
    }
}
