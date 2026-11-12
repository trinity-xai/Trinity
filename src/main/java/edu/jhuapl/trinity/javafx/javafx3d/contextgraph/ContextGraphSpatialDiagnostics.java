package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.Locale;

/** Reproducible chart-spacing diagnostics; not a semantic validity score. */
public record ContextGraphSpatialDiagnostics(int entities, int packedLayers,
        double effectiveStepFactor, double maxChartRadius, int nearBoundaryCount,
        double meanNearestChartDistance, double meanNearestHyperbolicDistance,
        double linkedTopLevelAngleBefore, double linkedTopLevelAngleAfter) {

    /** Preserve the original seven-argument diagnostic constructor. */
    public ContextGraphSpatialDiagnostics(int entities, int packedLayers,
            double effectiveStepFactor, double maxChartRadius, int nearBoundaryCount,
            double meanNearestChartDistance, double meanNearestHyperbolicDistance) {
        this(entities, packedLayers, effectiveStepFactor, maxChartRadius,
            nearBoundaryCount, meanNearestChartDistance, meanNearestHyperbolicDistance,
            Double.NaN, Double.NaN);
    }

    public String summary() {
        String summary = String.format(Locale.ROOT,
            "Spatial packing: %d layers, %d entities, step-budget factor %.3f%n"
                + "Max Poincare chart radius %.5f; near-boundary count %d%n"
                + "Mean sampled nearest neighbor: chart %.5f, hyperbolic %.5f",
            packedLayers, entities, effectiveStepFactor, maxChartRadius, nearBoundaryCount,
            meanNearestChartDistance, meanNearestHyperbolicDistance);
        if (Double.isFinite(linkedTopLevelAngleBefore)
                && Double.isFinite(linkedTopLevelAngleAfter)) {
            summary += String.format(Locale.ROOT,
                "%nLinked top-level community mean angle: %.3f -> %.3f rad "
                    + "(weighted, chart-direction proxy; lower is closer)",
                linkedTopLevelAngleBefore, linkedTopLevelAngleAfter);
        }
        return summary;
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
