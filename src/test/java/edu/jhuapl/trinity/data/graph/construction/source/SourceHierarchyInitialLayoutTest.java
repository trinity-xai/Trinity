package edu.jhuapl.trinity.data.graph.construction.source;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.semantics.GraphHierarchyIndex;
import edu.jhuapl.trinity.data.graph.semantics.GraphSemanticValidator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SourceHierarchyInitialLayoutTest {
    private SourceDocumentSnapshot example() {
        String documentId = SourceDocumentIdentity.documentId("layout-example");
        String text = "first passage\nsecond passage\nthird passage\n";
        String revision = SourceDocumentIdentity.revision(text);
        int[] cut = {0, 14, 29, text.length()};
        List<SourceDocumentSnapshot.Passage> passages = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String content = text.substring(cut[i], cut[i+1]);
            passages.add(new SourceDocumentSnapshot.Passage(
                SourceDocumentIdentity.passageId(documentId, revision, i, content),
                i, content, "test", Map.of(), cut[i], cut[i + 1], i + 1));
        }
        return new SourceDocumentSnapshot(documentId, revision, "example.pdf", null,
            "PDFBOX_PER_PAGE_SORT_BY_POSITION", text,
            List.of(new SourceDocumentSnapshot.PageSpan(1, cut[0], cut[1]),
                new SourceDocumentSnapshot.PageSpan(2, cut[1], cut[2]),
                new SourceDocumentSnapshot.PageSpan(3, cut[2], cut[3])), passages, Map.of());
    }

    @Test
    void radialCoordinatesSeparateSourceNodesWithoutChangingTypedSourceGraph() {
        SourceDocumentSnapshot source = example();
        GraphDirectedCollection graph = SourceDocumentHierarchyBuilder.build(source);
        String docId = source.documentId();
        assertNull(graph.findNodeById(docId).orElseThrow().getVector());
        List<String> edgeIds = graph.getEdges().stream().map(e -> e.getEdgeId()).toList();
        SourceHierarchyInitialLayout.assign(graph);
        assertEquals(List.of(0.0, 0.0, 0.0),
            graph.findNodeById(docId).orElseThrow().getVector());
        for (GraphNode node : graph.getNodes()) {
            assertEquals(3, node.getVector().size());
            for (double coordinate : node.getVector()) assertTrue(Double.isFinite(coordinate));
        }
        List<ArrayList<Double>> firstPositions = graph.getNodes().stream()
            .map(node -> new ArrayList<>(node.getVector())).toList();
        assertEquals(3L, graph.getNodes().stream()
            .filter(node -> "PASSAGE".equals(node.getNodeKind()))
            .map(node -> node.getVector().toString()).distinct().count());
        SourceHierarchyInitialLayout.assign(graph);
        assertEquals(firstPositions, graph.getNodes().stream().map(GraphNode::getVector).toList());
        assertEquals(edgeIds, graph.getEdges().stream().map(e -> e.getEdgeId()).toList());
        assertTrue(GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED).isValid());
        assertEquals(3L, GraphHierarchyIndex.build(graph)
            .hierarchy("source:" + docId, "CONTAINS").descendants(docId).stream()
            .filter(id -> source.passages().stream().anyMatch(p -> p.passageId().equals(id))).count());
    }

    @Test
    void interruptedCoordinateAssignmentDoesNotChangeTypedAssertions() {
        var graph = SourceDocumentHierarchyBuilder.build(example());
        assertThrows(java.util.concurrent.CancellationException.class,
            () -> SourceHierarchyInitialLayout.assign(graph, () -> true));
        assertTrue(GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED).isValid());
    }
}
