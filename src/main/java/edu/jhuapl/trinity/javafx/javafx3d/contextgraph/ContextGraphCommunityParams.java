package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

/** Settings for topology-derived semantic communities; no source graph mutation. */
public record ContextGraphCommunityParams(Algorithm algorithm, double resolution,
                                          int maximumLevels, int targetCommunitySize,
                                          double administrativeWeight, double structuralWeight,
                                          double semanticWeight) {
    public enum Algorithm {
        AUTO("Auto (Louvain)"),
        LOUVAIN("Multilevel Louvain"),
        LEGACY("Legacy Weighted Modularity");
        private final String display;
        Algorithm(String display) { this.display = display; }
        @Override public String toString() { return display; }
    }

    public static final int AUTO = 0;
    public static final int AUTOMATIC_LEVEL_LIMIT = 32;

    public ContextGraphCommunityParams {
        if (algorithm == null) throw new IllegalArgumentException("Algorithm is required");
        if (!Double.isFinite(resolution) || resolution < 0.2 || resolution > 3.0)
            throw new IllegalArgumentException("Resolution must be between 0.2 and 3.0");
        if (maximumLevels < 0 || maximumLevels > AUTOMATIC_LEVEL_LIMIT)
            throw new IllegalArgumentException("Maximum levels must be Auto (0) or 1-32");
        if (targetCommunitySize < 0)
            throw new IllegalArgumentException("Target size must be Auto (0) or positive");
        if (!validWeight(administrativeWeight) || !validWeight(structuralWeight)
            || !validWeight(semanticWeight))
            throw new IllegalArgumentException("Relationship weights must be finite and positive (0, 10]");
    }

    private static boolean validWeight(double weight) {
        return Double.isFinite(weight) && weight > 0 && weight <= 10;
    }

    public static ContextGraphCommunityParams defaults() {
        return new ContextGraphCommunityParams(Algorithm.AUTO, 1.0, AUTO, AUTO, 0.08, 0.65, 1.0);
    }

    public Algorithm effectiveAlgorithm() {
        return algorithm == Algorithm.AUTO ? Algorithm.LOUVAIN : algorithm;
    }

    public int levelLimit() {
        return maximumLevels == AUTO ? AUTOMATIC_LEVEL_LIMIT : maximumLevels;
    }
}
