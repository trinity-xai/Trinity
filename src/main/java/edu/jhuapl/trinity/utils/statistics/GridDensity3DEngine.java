package edu.jhuapl.trinity.utils.statistics;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.AnalysisUtils;
import edu.jhuapl.trinity.utils.metric.Metric;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builds 2D joint PDF/CDF grids (Z surfaces) from a collection of FeatureVectors
 * for Trinity's 3D hypersurface renderer.
 *
 * Core:
 *  - Joint PDF surface z = f(x, y) via 2D histogram (normalized to density).
 *  - Joint CDF surface z = F(x, y) = P(X ≤ x, Y ≤ y) via 2D prefix sums over per-cell probability mass.
 *
 * Axes are defined by AxisParams (per-axis scalar type + optional metric/reference/componentIndex).
 * Grid discretization and optional bounds are defined by GridSpec.
 *
 * Notes:
 *  - PDF integrates to ~1: sum(pdfZ) * dx * dy ≈ 1.
 *  - CDF is monotone in +x and +y and ends near 1.
 *  - If you need PCA/UMAP coordinates as axes, precompute them and pass via COMPONENT_AT_DIMENSION,
 *    or adapt this engine to accept externally supplied (x,y) arrays.
 *
 * @author Sean Phillips
 */
public class GridDensity3DEngine {

    private GridDensity3DEngine() {
        // Utility class
    }

    /**
     * Compute 2D histogram-based joint PDF and joint CDF for two scalar features across FeatureVectors.
     * If gridSpec bounds are null, they are inferred from data (with a tiny epsilon to avoid degenerate bins).
     *
     * @param vectors  feature vectors (rows)
     * @param xAxis    axis params for X
     * @param yAxis    axis params for Y
     * @param gridSpec discretization and optional bounds
     * @return GridDensityResult containing PDF grid, CDF grid, axis edges/centers, and bin sizes
     */
    public static GridDensityResult computePdfCdf2D(
            List<FeatureVector> vectors,
            AxisParams xAxis,
            AxisParams yAxis,
            GridSpec gridSpec
    ) {
        Objects.requireNonNull(xAxis, "xAxis");
        Objects.requireNonNull(yAxis, "yAxis");
        Objects.requireNonNull(gridSpec, "gridSpec");

        if (vectors == null || vectors.isEmpty()) {
            return emptyResult(gridSpec);
        }

        // Prepare auxiliary data only when required by selected scalar types
        List<Double> meanVector = null;
        Set<StatisticEngine.ScalarType> needMean = Set.of(
                StatisticEngine.ScalarType.DIST_TO_MEAN,
                StatisticEngine.ScalarType.COSINE_TO_MEAN
        );
        if (needMean.contains(xAxis.getType()) || needMean.contains(yAxis.getType())) {
            meanVector = FeatureVector.getMeanVector(vectors);
        }

        Metric metricX = null;
        if (xAxis.getType() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN
                && xAxis.getMetricName() != null
                && xAxis.getReferenceVec() != null) {
            metricX = Metric.getMetric(xAxis.getMetricName());
        }
        Metric metricY = null;
        if (yAxis.getType() == StatisticEngine.ScalarType.METRIC_DISTANCE_TO_MEAN
                && yAxis.getMetricName() != null
                && yAxis.getReferenceVec() != null) {
            metricY = Metric.getMetric(yAxis.getMetricName());
        }

        // Compute scalar pairs (x_i, y_i)
        final int n = vectors.size();
        double[] xs = new double[n];
        double[] ys = new double[n];

        for (int i = 0; i < n; i++) {
            FeatureVector fv = vectors.get(i);
            xs[i] = scalarValue(
                    fv,
                    xAxis.getType(),
                    meanVector,
                    metricX,
                    xAxis.getReferenceVec(),
                    xAxis.getComponentIndex()
            );
            ys[i] = scalarValue(
                    fv,
                    yAxis.getType(),
                    meanVector,
                    metricY,
                    yAxis.getReferenceVec(),
                    yAxis.getComponentIndex()
            );
        }

        // Bounds (auto or explicit)
        double minX = (gridSpec.getMinX() != null) ? gridSpec.getMinX() : min(xs);
        double maxX = (gridSpec.getMaxX() != null) ? gridSpec.getMaxX() : max(xs);
        double minY = (gridSpec.getMinY() != null) ? gridSpec.getMinY() : min(ys);
        double maxY = (gridSpec.getMaxY() != null) ? gridSpec.getMaxY() : max(ys);
        if (minX == maxX) maxX += 1e-8;
        if (minY == maxY) maxY += 1e-8;

        final int binsX = gridSpec.getBinsX();
        final int binsY = gridSpec.getBinsY();

        final double dx = (maxX - minX) / binsX;
        final double dy = (maxY - minY) / binsY;

        double[] xEdges = new double[binsX + 1];
        double[] yEdges = new double[binsY + 1];
        double[] xCenters = new double[binsX];
        double[] yCenters = new double[binsY];

        for (int bx = 0; bx <= binsX; bx++) xEdges[bx] = minX + bx * dx;
        for (int by = 0; by <= binsY; by++) yEdges[by] = minY + by * dy;
        for (int bx = 0; bx < binsX; bx++) xCenters[bx] = 0.5 * (xEdges[bx] + xEdges[bx + 1]);
        for (int by = 0; by < binsY; by++) yCenters[by] = 0.5 * (yEdges[by] + yEdges[by + 1]);

        // 2D histogram (counts)
        double[][] counts = new double[binsY][binsX];
        for (int i = 0; i < n; i++) {
            int bx = (int) Math.floor((xs[i] - minX) / dx);
            int by = (int) Math.floor((ys[i] - minY) / dy);
            if (bx < 0) bx = 0;
            if (bx >= binsX) bx = binsX - 1;
            if (by < 0) by = 0;
            if (by >= binsY) by = binsY - 1;
            counts[by][bx] += 1.0;
        }

        // Normalize to PDF: density = count / (N * dx * dy)
        final double invAreaN = 1.0 / (n * dx * dy);
        double[][] pdfZ = new double[binsY][binsX];
        for (int by = 0; by < binsY; by++) {
            for (int bx = 0; bx < binsX; bx++) {
                pdfZ[by][bx] = counts[by][bx] * invAreaN;
            }
        }

        // Convert to per-cell probability mass, then 2D prefix sum → CDF
        double[][] mass = new double[binsY][binsX];
        for (int by = 0; by < binsY; by++) {
            for (int bx = 0; bx < binsX; bx++) {
                mass[by][bx] = pdfZ[by][bx] * dx * dy;
            }
        }
        double[][] cdfZ = prefixSum2D(mass);
        // Numerical guard
        cdfZ[binsY - 1][binsX - 1] = Math.min(1.0, Math.max(0.0, cdfZ[binsY - 1][binsX - 1]));

        return new GridDensityResult(pdfZ, cdfZ, xEdges, yEdges, xCenters, yCenters, dx, dy);
    }

    // ---------- Helpers ----------

    private static double scalarValue(FeatureVector fv,
                                      StatisticEngine.ScalarType type,
                                      List<Double> meanVec,
                                      Metric metric,
                                      List<Double> refVec,
                                      Integer componentIndex) {
        switch (type) {
            case L1_NORM:
                return fv.getData().stream().mapToDouble(Math::abs).sum();
            case LINF_NORM:
                return fv.getData().stream().mapToDouble(Math::abs).max().orElse(0.0);
            case MEAN:
                return fv.getData().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            case MAX:
                return fv.getMax();
            case MIN:
                return fv.getMin();
            case DIST_TO_MEAN:
                if (meanVec == null) return 0.0;
                return AnalysisUtils.l2Norm(StatisticEngine.diffList(fv.getData(), meanVec));
            case COSINE_TO_MEAN:
                if (meanVec == null) return 0.0;
                return AnalysisUtils.cosineSimilarity(fv.getData(), meanVec);
            case PC1_PROJECTION:
                // If you need PC projections as axes, precompute externally
                // and pass via COMPONENT_AT_DIMENSION (or extend this engine).
                return 0.0;
            case METRIC_DISTANCE_TO_MEAN:
                if (metric == null || refVec == null) return 0.0;
                double[] a = fv.getData().stream().mapToDouble(Double::doubleValue).toArray();
                double[] b = refVec.stream().mapToDouble(Double::doubleValue).toArray();
                return metric.distance(a, b);
            case COMPONENT_AT_DIMENSION:
                if (componentIndex == null) return 0.0;
                List<Double> d = fv.getData();
                if (componentIndex >= 0 && componentIndex < d.size()) return d.get(componentIndex);
                return 0.0;
            default:
                return 0.0;
        }
    }

    private static double[][] prefixSum2D(double[][] mass) {
        int h = mass.length;
        int w = mass[0].length;
        double[][] c = new double[h][w];
        for (int y = 0; y < h; y++) {
            double rowSum = 0.0;
            for (int x = 0; x < w; x++) {
                rowSum += mass[y][x];
                c[y][x] = rowSum + (y > 0 ? c[y - 1][x] : 0.0);
            }
        }
        return c;
    }

    private static double min(double[] v) {
        double m = Double.POSITIVE_INFINITY;
        for (double x : v) if (x < m) m = x;
        return m;
    }

    private static double max(double[] v) {
        double m = Double.NEGATIVE_INFINITY;
        for (double x : v) if (x > m) m = x;
        return m;
    }

    private static GridDensityResult emptyResult(GridSpec grid) {
        int bx = Math.max(5, grid.getBinsX());
        int by = Math.max(5, grid.getBinsY());
        double[][] z0 = new double[by][bx];
        double[] xEdges = new double[bx + 1];
        double[] yEdges = new double[by + 1];
        double[] xCenters = new double[bx];
        double[] yCenters = new double[by];
        return new GridDensityResult(z0, z0, xEdges, yEdges, xCenters, yCenters, 1.0, 1.0);
    }
}
