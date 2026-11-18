package edu.jhuapl.trinity.utils.umap;

import edu.jhuapl.trinity.data.analysis.FeatureVectorAnalysisSnapshot;
import edu.jhuapl.trinity.data.graph.construction.UmapFuzzyGraphBuilder;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UmapGraphSnapshotTest {
    @Test
    void fuzzyGraphBuildDeduplicatesSymmetricEntriesAndPreservesIds() {
        FeatureVector first = new FeatureVector(List.of(1.0, 2.0), "Passage");
        first.setEntityId("first");
        FeatureVector second = new FeatureVector(List.of(2.0, 3.0), "Passage");
        second.setEntityId("second");
        FeatureVector third = new FeatureVector(List.of(3.0, 4.0), "Passage");
        third.setEntityId("third");
        var data = FeatureVectorAnalysisSnapshot.fromVectors("articles", List.of(first, second, third));
        var snapshot = new UmapGraphSnapshot(data.inputRowIds(), List.of(
            new UmapGraphSnapshot.WeightedEdge(0, 1, 0.75),
            new UmapGraphSnapshot.WeightedEdge(1, 0, 0.74),
            new UmapGraphSnapshot.WeightedEdge(0, 2, 0.1)
        ), List.of(), Map.of("metricClass", "test"));
        var graph = UmapFuzzyGraphBuilder.build(data, snapshot, 0.5, "trial");
        assertEquals(3, graph.getNodes().size());
        assertEquals(1, graph.getEdges().size());
        assertEquals("first", graph.getEdges().get(0).getStartID());
        assertEquals("second", graph.getEdges().get(0).getEndID());
        assertEquals(0.75, graph.getEdges().get(0).getRelationship().getProperties().get("membership"));
        assertNull(graph.getNodes().get(0).getVector());
        assertEquals("UMAP_FUZZY_ASSOCIATION", graph.getEdges().get(0).getRelationship().getLabels().get(0));
        assertThrows(IllegalArgumentException.class, () ->
            UmapFuzzyGraphBuilder.build(data,
                new UmapGraphSnapshot(List.of("second", "first", "third"),
                    snapshot.fuzzyEdges(), List.of(), Map.of()), 0, null));
    }
}
