package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.Locale;

/** Reproducible chart-spacing diagnostics; not a semantic validity score. */
public record ContextGraphSpatialDiagnostics(int entities, int packedLayers,
        double effectiveStepFactor, double maxChartRadius, int nearBoundaryCount,
        double meanNearestChartDistance, double meanNearestHyperbolicDistance) {

    public String summary() {
        return String.format(Locale.ROOT,
            "Spatial packing: %d layers, %d entities, step-budget factor %.3f%n"
                + "Max Poincare chart radius %.5f; near-boundary count %d%n"
                + "Mean sampled nearest neighbor: chart %.5f, hyperbolic %.5f",
            packedLayers, entities, effectiveStepFactor, maxChartRadius, nearBoundaryCount,
            meanNearestChartDistance, meanNearestHyperbolicDistance);
    }

    public String comparison(ContextGraphSpatialDiagnostics prior) {
        if (prior == null || prior.entities != entities) return "";
        return String.format(Locale.ROOT,
            "%nPrevious layout -> current: mean sampled chart NN %.5f -> %.5f; "
                + "hyperbolic NN %.5f -> %.5f; max radius %.5f -> %.5f",
            prior.meanNearestChartDistance, meanNearestChartDistance,
            prior.meanNearestHyperbolicDistance, meanNearestHyperbolicDistance,
            prior.maxChartRadius, maxChartRadius);
    }
}
