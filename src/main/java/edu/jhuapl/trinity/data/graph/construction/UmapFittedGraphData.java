package edu.jhuapl.trinity.data.graph.construction;

import edu.jhuapl.trinity.data.analysis.FeatureVectorAnalysisSnapshot;
import edu.jhuapl.trinity.utils.umap.UmapGraphSnapshot;
import java.util.Objects;

/**
 * One completed UMAP fit and the exact ordered observations that produced it.
 * Keep both snapshots together: matching row IDs from unrelated fits are not
 * sufficient to establish provenance for graph construction.
 */
public record UmapFittedGraphData(FeatureVectorAnalysisSnapshot source,
                                  UmapGraphSnapshot graph) {
    public UmapFittedGraphData {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(graph, "graph");
        if (!source.inputRowIds().equals(graph.inputRowIds())) {
            throw new IllegalArgumentException("UMAP graph input rows do not match source snapshot");
        }
    }
}
