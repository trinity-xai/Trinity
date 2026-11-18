package edu.jhuapl.trinity.utils.umap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Detached and immutable view of a completed UMAP fit's fuzzy simplicial set.
 * Edges contain directed matrix entries, NOT factual semantic assertions.
 * Most UMAP fuzzy graphs are symmetric; a future graph builder must choose
 * whether to collapse reciprocal entries before creating GraphEdge objects.
 * Original-space KNN entries are optional: the small-data fit branch does not
 * retain its KNN arrays. The fuzzy graph is available in both branches.
 */
public final class UmapGraphSnapshot {

    public record WeightedEdge(int sourceRow, int targetRow, double membership) {
        public WeightedEdge {
            if (sourceRow < 0 || targetRow < 0 || !Double.isFinite(membership)
                || membership < 0 || membership > 1.0000001) {
                throw new IllegalArgumentException("Invalid UMAP fuzzy edge");
            }
        }
    }

    public record MetricNeighbor(int sourceRow, int targetRow, double distance) {
        public MetricNeighbor {
            if (sourceRow < 0 || targetRow < 0 || !Double.isFinite(distance) || distance < 0) {
                throw new IllegalArgumentException("Invalid UMAP KNN distance");
            }
        }
    }

    private final List<String> inputRowIds;
    private final List<WeightedEdge> fuzzyEdges;
    private final List<MetricNeighbor> originalSpaceNeighbors;
    private final Map<String, String> fitParameters;

    UmapGraphSnapshot(List<String> inputRowIds, List<WeightedEdge> fuzzyEdges,
                      List<MetricNeighbor> originalSpaceNeighbors, Map<String, String> fitParameters) {
        this.inputRowIds = List.copyOf(Objects.requireNonNull(inputRowIds, "inputRowIds"));
        this.fuzzyEdges = List.copyOf(Objects.requireNonNull(fuzzyEdges, "fuzzyEdges"));
        this.originalSpaceNeighbors = List.copyOf(Objects.requireNonNull(originalSpaceNeighbors, "originalSpaceNeighbors"));
        this.fitParameters = Collections.unmodifiableMap(new LinkedHashMap<>(
            Objects.requireNonNull(fitParameters, "fitParameters")));
        int n = inputRowIds.size();
        if (this.inputRowIds.stream().anyMatch(id -> id == null || id.isBlank())
            || this.inputRowIds.stream().distinct().count() != n) {
            throw new IllegalArgumentException("UMAP input row IDs must be nonblank and unique");
        }
        for (WeightedEdge edge : fuzzyEdges) {
            if (edge.sourceRow() >= n || edge.targetRow() >= n) {
                throw new IllegalArgumentException("UMAP graph edge index exceeds input rows");
            }
        }
        for (MetricNeighbor neighbor : originalSpaceNeighbors) {
            if (neighbor.sourceRow() >= n || neighbor.targetRow() >= n) {
                throw new IllegalArgumentException("UMAP neighbor index exceeds input rows");
            }
        }
    }

    public int size() { return inputRowIds.size(); }
    public List<String> inputRowIds() { return inputRowIds; }
    public String inputIdAt(int row) { return inputRowIds.get(row); }
    public List<WeightedEdge> fuzzyEdges() { return fuzzyEdges; }
    public List<MetricNeighbor> originalSpaceNeighbors() { return originalSpaceNeighbors; }
    public boolean hasRetainedOriginalSpaceNeighbors() { return !originalSpaceNeighbors.isEmpty(); }
    public Map<String, String> fitParameters() { return fitParameters; }
}
