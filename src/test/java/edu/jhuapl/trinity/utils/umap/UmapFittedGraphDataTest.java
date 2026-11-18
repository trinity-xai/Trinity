package edu.jhuapl.trinity.utils.umap;

import edu.jhuapl.trinity.data.analysis.FeatureVectorAnalysisSnapshot;
import edu.jhuapl.trinity.data.graph.construction.UmapFittedGraphData;
import edu.jhuapl.trinity.data.graph.construction.UmapFuzzyGraphBuilder;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UmapFittedGraphDataTest {
    @Test
    void fittedPairMustHaveIdenticalOrderedSourceIds() {
        FeatureVector a = new FeatureVector(List.of(1.0, 2.0), "Passage");
        a.setEntityId("a");
        FeatureVector b = new FeatureVector(List.of(3.0, 4.0), "Passage");
        b.setEntityId("b");
        FeatureVectorAnalysisSnapshot source = FeatureVectorAnalysisSnapshot.fromVectors(
            "articles", List.of(a, b));
        UmapGraphSnapshot good = new UmapGraphSnapshot(List.of("a", "b"),
            List.of(new UmapGraphSnapshot.WeightedEdge(0, 1, 0.8),
                new UmapGraphSnapshot.WeightedEdge(1, 0, 0.8)),
            List.of(), Map.of("metricClass", "test"));
        UmapFittedGraphData result = new UmapFittedGraphData(source, good);
        assertEquals(source.inputRowIds(), result.graph().inputRowIds());
        var graph = UmapFuzzyGraphBuilder.build(result.source(), result.graph(), 0.5, "test");
        assertEquals(2, graph.getNodes().size());
        assertEquals(1, graph.getEdges().size());
        assertEquals("a", graph.getEdges().get(0).getStartID());
        assertEquals("b", graph.getEdges().get(0).getEndID());
        assertThrows(IllegalArgumentException.class, () -> new UmapFittedGraphData(source,
            new UmapGraphSnapshot(List.of("b", "a"), good.fuzzyEdges(), List.of(), Map.of())));
    }
}
