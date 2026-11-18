package edu.jhuapl.trinity.utils.umap;

import edu.jhuapl.trinity.data.analysis.FeatureVectorAnalysisSnapshot;
import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphFeatureCollectionAdapter;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.construction.UmapFittedGraphData;
import edu.jhuapl.trinity.data.graph.construction.UmapFuzzyGraphBuilder;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Focused regression coverage for initial layout and feature provenance. */
class UmapContextGraphProjectionTest {
    @Test
    void tinyTwoDimensionalUmapCoordinatesProduceDistinctUsablePositions() {
        FeatureVector first = new FeatureVector(List.of(0.1, 0.2), "APT");
        first.setEntityId("a");
        first.setImageURL("/media/images/a.png");
        first.setMediaURL("/media/audio/a.wav");
        first.setImageId(42);
        first.setFrameId(7);
        first.setBbox(List.of(1.0, 2.0, 3.0, 4.0));
        first.setText("source passage text");
        first.getMetaData().put("sourceFile", "report.pdf");
        FeatureVector second = new FeatureVector(List.of(0.3, 0.4), "APT");
        second.setEntityId("b");
        FeatureVector third = new FeatureVector(List.of(0.5, 0.6), "APT");
        third.setEntityId("c");
        var source = FeatureVectorAnalysisSnapshot.fromVectors("document", List.of(first, second, third));
        var graphSnapshot = new UmapGraphSnapshot(source.inputRowIds(), List.of(
            new UmapGraphSnapshot.WeightedEdge(0, 1, 0.75),
            new UmapGraphSnapshot.WeightedEdge(1, 0, 0.75)
        ), List.of(), Map.of("metric", "euclidean"));
        double[][] projection = {{1e-9, 2e-9}, {5e-9, 3e-9}, {3e-9, 9e-9}};
        var fitted = new UmapFittedGraphData(source, graphSnapshot, projection);
        projection[0][0] = 500; // Snapshot must not share mutable matrix data.
        first.setImageURL("/wrong.png");
        first.getMetaData().put("sourceFile", "wrong.pdf");
        assertEquals(1e-9, fitted.projectedCoordinates().get(0).get(0));
        assertThrows(UnsupportedOperationException.class,
            () -> fitted.projectedCoordinates().get(0).set(0, 42.0));
        var graph = UmapFuzzyGraphBuilder.build(fitted, 0.25, "test");
        assertEquals(3, graph.getNodes().size());
        assertEquals(1, graph.getEdges().size());
        GraphNode a = graph.getNodes().get(0);
        GraphNode b = graph.getNodes().get(1);
        assertNotEquals(a.getVector(), b.getVector());
        assertEquals(0.0, a.getVector().get(2)); // 2D UMAP is placed in XY.
        double maxX = graph.getNodes().stream().mapToDouble(n -> n.getVector().get(0)).max().orElseThrow();
        double minX = graph.getNodes().stream().mapToDouble(n -> n.getVector().get(0)).min().orElseThrow();
        double maxY = graph.getNodes().stream().mapToDouble(n -> n.getVector().get(1)).max().orElseThrow();
        double minY = graph.getNodes().stream().mapToDouble(n -> n.getVector().get(1)).min().orElseThrow();
        assertEquals(600.0, Math.max(maxX - minX, maxY - minY), 1e-8);
        assertEquals("/media/images/a.png", a.getProperties().get("imageURL"));
        assertEquals("/media/audio/a.wav", a.getProperties().get("mediaURL"));
        assertEquals("source passage text", a.getProperties().get("text"));
        assertEquals(42L, a.getProperties().get("imageId"));
        assertEquals(7L, a.getProperties().get("frameId"));
        assertEquals(List.of(1.0, 2.0, 3.0, 4.0), a.getProperties().get("bbox"));
        @SuppressWarnings("unchecked")
        Map<String, String> metadata = (Map<String, String>) a.getProperties().get("sourceMetadata");
        assertEquals("report.pdf", metadata.get("sourceFile"));
        assertEquals("UMAP_PROJECTION", a.getProperties().get("coordinateSource"));
        assertEquals("UMAP_FUZZY_ASSOCIATION",
            graph.getEdges().get(0).getRelationship().getLabels().get(0));
        var adapted = new GraphFeatureCollectionAdapter(graph).featureFor(a);
        assertEquals("/media/images/a.png", adapted.getImageURL());
        assertEquals("/media/audio/a.wav", adapted.getMediaURL());
        assertEquals("source passage text", adapted.getText());
    }

    @Test
    void projectedRowCountMustMatchInputAndThreeDimensionalProjectionKeepsDepth() {
        FeatureVector a = new FeatureVector(List.of(1.0), "A");
        a.setEntityId("a");
        FeatureVector b = new FeatureVector(List.of(2.0), "B");
        b.setEntityId("b");
        var source = FeatureVectorAnalysisSnapshot.fromVectors("sample", List.of(a, b));
        var fuzzy = new UmapGraphSnapshot(source.inputRowIds(), List.of(), List.of(), Map.of());
        assertThrows(IllegalArgumentException.class, () -> new UmapFittedGraphData(
            source, fuzzy, new double[][]{{1,2,3}}));
        var fitted = new UmapFittedGraphData(source, fuzzy,
            new double[][]{{1,2,3}, {4,5,6}});
        GraphDirectedCollection graph = UmapFuzzyGraphBuilder.build(fitted, 0.0, "test3d");
        assertEquals(3, graph.getNodes().get(0).getVector().size());
        assertNotEquals(0.0, graph.getNodes().get(0).getVector().get(2));
        assertNotEquals(graph.getNodes().get(0).getVector().get(2),
            graph.getNodes().get(1).getVector().get(2));
    }
}
