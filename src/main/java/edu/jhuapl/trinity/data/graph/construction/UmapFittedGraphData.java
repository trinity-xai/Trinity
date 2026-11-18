package edu.jhuapl.trinity.data.graph.construction;

import edu.jhuapl.trinity.data.analysis.FeatureVectorAnalysisSnapshot;
import edu.jhuapl.trinity.utils.umap.UmapGraphSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One completed UMAP fit: original source observations, fuzzy graph, and
 * optional low-dimensional coordinates from that SAME fit and input row order.
 * Coordinates are only an initial Euclidean view; Poincare positions are
 * computed separately from the resulting graph's topology.
 */
public record UmapFittedGraphData(FeatureVectorAnalysisSnapshot source,
                                  UmapGraphSnapshot graph,
                                  List<List<Double>> projectedCoordinates) {
    public UmapFittedGraphData {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(graph, "graph");
        if (!source.inputRowIds().equals(graph.inputRowIds())) {
            throw new IllegalArgumentException("UMAP graph input rows do not match source snapshot");
        }
        projectedCoordinates = copyAndValidate(projectedCoordinates, source.size());
    }

    /** Compatible with existing fuzzy-graph-only callers. */
    public UmapFittedGraphData(FeatureVectorAnalysisSnapshot source, UmapGraphSnapshot graph) {
        this(source, graph, List.of());
    }

    /** Deep-copy the fitted matrix while its row mapping is still available. */
    public UmapFittedGraphData(FeatureVectorAnalysisSnapshot source, UmapGraphSnapshot graph,
                               double[][] coordinates) {
        this(source, graph, toRows(coordinates));
    }

    public boolean hasProjectedCoordinates() { return !projectedCoordinates.isEmpty(); }

    private static List<List<Double>> toRows(double[][] matrix) {
        if (matrix == null) throw new IllegalArgumentException("Null UMAP embedding matrix");
        List<List<Double>> result = new ArrayList<>(matrix.length);
        for (double[] values : matrix) {
            if (values == null) throw new IllegalArgumentException("Null projected row");
            List<Double> row = new ArrayList<>(values.length);
            for (double value : values) row.add(value);
            result.add(row);
        }
        return result;
    }

    private static List<List<Double>> copyAndValidate(List<List<Double>> rows, int count) {
        Objects.requireNonNull(rows, "projectedCoordinates");
        if (rows.isEmpty()) return List.of();
        if (rows.size() != count) {
            throw new IllegalArgumentException("UMAP projected row count differs from source observations");
        }
        int dimensions = rows.get(0).size();
        if (dimensions < 1) throw new IllegalArgumentException("UMAP projected rows need at least one coordinate");
        List<List<Double>> result = new ArrayList<>(rows.size());
        for (List<Double> row : rows) {
            if (row == null || row.size() != dimensions) {
                throw new IllegalArgumentException("UMAP projected rows have inconsistent dimensions");
            }
            for (Double value : row) {
                if (value == null || !Double.isFinite(value)) {
                    throw new IllegalArgumentException("UMAP projected rows contain non-finite coordinates");
                }
            }
            result.add(List.copyOf(row));
        }
        return List.copyOf(result);
    }
}
