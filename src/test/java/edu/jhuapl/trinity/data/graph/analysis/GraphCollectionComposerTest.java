package edu.jhuapl.trinity.data.graph.analysis;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.semantics.GraphAssertionOrigin;
import edu.jhuapl.trinity.data.graph.semantics.GraphEvidence;
import edu.jhuapl.trinity.data.graph.semantics.GraphHierarchyIndex;
import edu.jhuapl.trinity.data.graph.semantics.GraphPredicateDefinition;
import edu.jhuapl.trinity.data.graph.semantics.GraphPredicateFamily;
import edu.jhuapl.trinity.data.graph.semantics.GraphSemanticValidator;
import edu.jhuapl.trinity.data.graph.semantics.GraphRunProvenance;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.*;

/** Non-reconciling composition preserves distinct identities and provenance. */
public class GraphCollectionComposerTest {
    @Test
    void sameLocalIdsStayDistinctWithIndependentEvidenceAndHierarchyScopes() {
        GraphDirectedCollection a = source("doc-a", "first quote");
        GraphDirectedCollection b = source("doc-b", "contradictory quote");
        var composed = GraphCollectionComposer.compose("hybrid",
            List.of(new GraphCollectionComposer.Input("source-A", a),
                    new GraphCollectionComposer.Input("source-B", b)));
        GraphDirectedCollection union = composed.graph();
        assertEquals(4, union.getNodes().size());
        assertEquals(2, union.getEdges().size());
        assertEquals(2, union.getEvidenceRecords().size());
        assertEquals(2, union.getSchemaVersion().intValue());
        assertEquals("GraphCollectionComposer", union.getRunProvenance().getProducer());
        assertEquals("first quote", union.getEvidenceRecords().get(0).getQuote());
        assertEquals("contradictory quote", union.getEvidenceRecords().get(1).getQuote());
        assertEquals("doc-a", union.getEvidenceRecords().get(0).getDocumentId());
        assertEquals("doc-b", union.getEvidenceRecords().get(1).getDocumentId());
        assertFalse(union.getNodes().get(0).getEntityID().equals(union.getNodes().get(2).getEntityID()));
        assertFalse(union.getEdges().get(0).getEdgeId().equals(union.getEdges().get(1).getEdgeId()));
        var mappingA = composed.sources().get(0);
        var mappingB = composed.sources().get(1);
        assertEquals(mappingA.nodeIds().get("x"), union.getEdges().get(0).getStartID());
        assertEquals(mappingB.nodeIds().get("x"), union.getEdges().get(1).getStartID());
        assertEquals(mappingA.evidenceIds().get("ev"), union.getEdges().get(0)
            .getRelationship().getSemantics().getEvidenceIds().get(0));
        assertEquals(mappingB.evidenceIds().get("ev"), union.getEdges().get(1)
            .getRelationship().getSemantics().getEvidenceIds().get(0));
        assertFalse(union.getEdges().get(0).getRelationship().getSemantics().getHierarchyScopeId()
            .equals(union.getEdges().get(1).getRelationship().getSemantics().getHierarchyScopeId()));
        assertTrue(GraphSemanticValidator.validate(union, GraphSemanticValidator.Mode.STRICT_TYPED)
            .isValid());
        assertEquals(2, GraphHierarchyIndex.build(union).hierarchies().size());
        assertEquals("x", a.getEdges().get(0).getStartID());
        assertEquals("ev", b.getEvidenceRecords().get(0).getEvidenceId());
        assertEquals("taxonomy", a.getEdges().get(0).getRelationship().getSemantics().getHierarchyScopeId());
    }

    @Test
    void isolatedNodesAndParallelAssertionsAreNeverCollapsed() {
        GraphDirectedCollection a = source("d", "support");
        a.getEdges().add(GraphAnalysisProjectionTest.typed("claim2", "x", "y", "SUBCLASS_OF",
            GraphAssertionOrigin.USER_CURATED, "taxonomy"));
        a.getNodes().addAll(GraphAnalysisProjectionTest.graph("isolated").getNodes());
        var result = GraphCollectionComposer.compose("hybrid",
            List.of(new GraphCollectionComposer.Input("A", a)));
        assertEquals(3, result.graph().getNodes().size());
        assertEquals(2, result.graph().getEdges().size());
        assertEquals(2, GraphAnalysisProjection.build(result.graph(),
            GraphViewSpecification.builder().predicateIds("SUBCLASS_OF").build()).links().size());
        assertTrue(result.sources().get(0).nodeIds().containsKey("isolated"));
        assertEquals(2, result.sources().get(0).edgeIds().size());
    }

    @Test
    void duplicateNamespacesIdsAndConflictingCatalogsFailExplicitly() {
        GraphDirectedCollection a = source("d", "support");
        assertThrows(IllegalArgumentException.class,
            () -> GraphCollectionComposer.compose("g", List.of(
                new GraphCollectionComposer.Input("A", a),
                new GraphCollectionComposer.Input("A", source("e", "support")))));
        a.getNodes().add(GraphAnalysisProjectionTest.graph("x").getNodes().get(0));
        assertThrows(IllegalArgumentException.class,
            () -> GraphCollectionComposer.compose("g", List.of(new GraphCollectionComposer.Input("A", a))));
        a.getNodes().remove(a.getNodes().size() - 1);
        GraphDirectedCollection b = source("e", "support");
        GraphPredicateDefinition first = custom("HAS_RELATION", GraphPredicateFamily.DOMAIN_ASSERTION);
        GraphPredicateDefinition second = custom("HAS_RELATION", GraphPredicateFamily.TAXONOMY);
        a.setPredicateDefinitions(List.of(first)); b.setPredicateDefinitions(List.of(second));
        assertThrows(IllegalArgumentException.class,
            () -> GraphCollectionComposer.compose("g", List.of(
                new GraphCollectionComposer.Input("A", a),
                new GraphCollectionComposer.Input("B", b))));
        assertThrows(CancellationException.class,
            () -> GraphCollectionComposer.compose("g", List.of(new GraphCollectionComposer.Input("A", a)),
                () -> true));
    }

    @Test
    void legacyEdgesGetOnlyPositionalFallbackAndDoNotBecomeFacts() {
        GraphDirectedCollection legacy = GraphAnalysisProjectionTest.graph("x", "y");
        legacy.setSchemaVersion(null);
        legacy.getEdges().add(GraphAnalysisProjectionTest.legacy("x", "y", "UNKNOWN_REL"));
        var result = GraphCollectionComposer.compose("hybrid",
            List.of(new GraphCollectionComposer.Input("old", legacy)));
        GraphDirectedCollection output = result.graph();
        assertNull(output.getSchemaVersion());
        assertTrue(output.getEdges().get(0).getEdgeId().contains("legacy-index:0"));
        assertNull(output.getEdges().get(0).getRelationship().getSemantics());
        assertNull(output.getEvidenceRecords());
        assertEquals(1, result.sources().get(0).edgeIds().size());
        assertTrue(GraphSemanticValidator.validate(output,
            GraphSemanticValidator.Mode.LEGACY_PERMISSIVE).isValid());
    }

    @Test
    void sourceRunMethodsMetricsAndParametersRemainTraceable() {
        GraphDirectedCollection source = source("d", "support");
        GraphRunProvenance run = new GraphRunProvenance();
        run.setRunId("umap-fit-42");
        run.setProducer("Trinity UMAP");
        run.setMethod("UMAP_FUZZY_GRAPH");
        run.setSourceCollectionId("collection-3");
        run.setParameters(new HashMap<>(Map.of("metric", "euclidean", "nNeighbors", 22)));
        run.setSourceDocumentIds(List.of("d"));
        source.setRunProvenance(run);
        GraphDirectedCollection result = GraphCollectionComposer.compose("g",
            List.of(new GraphCollectionComposer.Input("umap", source))).graph();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) result
            .getRunProvenance().getParameters().get("sourceGraphs");
        assertEquals("umap-fit-42", entries.get(0).get("runId"));
        assertEquals("UMAP_FUZZY_GRAPH", entries.get(0).get("method"));
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) entries.get(0).get("parameters");
        assertEquals("euclidean", params.get("metric"));
        assertEquals(22, params.get("nNeighbors"));
        run.getParameters().put("metric", "changed");
        assertEquals("euclidean", params.get("metric"));
    }

    @Test
    void jsonPropertyListsAndMapsAreCopiedWithoutMutatingSources() {
        GraphDirectedCollection a = source("doc", "quote");
        a.getNodes().get(0).getProperties().put("nested", new HashMap<>(Map.of(
            "tokens", new java.util.ArrayList<>(List.of("first")))));
        GraphDirectedCollection result = GraphCollectionComposer.compose("g",
            List.of(new GraphCollectionComposer.Input("A", a))).graph();
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) result.getNodes().get(0)
            .getProperties().get("nested");
        @SuppressWarnings("unchecked")
        List<String> tokens = (List<String>) nested.get("tokens");
        tokens.add("second");
        @SuppressWarnings("unchecked")
        Map<String, Object> original = (Map<String, Object>) a.getNodes().get(0)
            .getProperties().get("nested");
        assertEquals(List.of("first"), original.get("tokens"));
    }

    @Test
    void qualifiedIdsRoundTripEvenWithDelimiterCharacters() {
        String encoded = GraphCollectionComposer.qualified("a:b::c", "x:y:12#z");
        assertEquals("a:b::c", GraphCollectionComposer.decode(encoded).namespace());
        assertEquals("x:y:12#z", GraphCollectionComposer.decode(encoded).localId());
        assertThrows(IllegalArgumentException.class, () -> GraphCollectionComposer.decode("a:b"));
        assertThrows(IllegalArgumentException.class, () -> GraphCollectionComposer.decode("-2:a1:z"));
    }

    @Test
    void compositionSurvivesJacksonSerialization() throws Exception {
        GraphDirectedCollection a = source("doc-a", "first");
        GraphDirectedCollection b = source("doc-b", "second");
        var output = GraphCollectionComposer.compose("hybrid",
            List.of(new GraphCollectionComposer.Input("a", a),
                new GraphCollectionComposer.Input("b", b)));
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        GraphDirectedCollection restored = mapper.readValue(
            mapper.writeValueAsString(output.graph()), GraphDirectedCollection.class);
        assertEquals(4, restored.getNodes().size());
        assertEquals(2, restored.getEdges().size());
        assertEquals(2, restored.getEvidenceRecords().size());
        assertEquals("a", GraphCollectionComposer.decode(restored.getNodes().get(0).getEntityID()).namespace());
        assertTrue(GraphSemanticValidator.validate(restored, GraphSemanticValidator.Mode.STRICT_TYPED)
            .isValid());
    }

    private static GraphPredicateDefinition custom(String id, GraphPredicateFamily family) {
        GraphPredicateDefinition d = new GraphPredicateDefinition();
        d.setPredicateId(id);
        d.setFamily(family);
        return d;
    }

    private static GraphDirectedCollection source(String document, String quote) {
        GraphDirectedCollection graph = GraphAnalysisProjectionTest.graph("x", "y");
        graph.setGraphId("source:graph");
        GraphEvidence ev = new GraphEvidence();
        ev.setEvidenceId("ev"); ev.setDocumentId(document);
        ev.setDocumentRevision("rev1"); ev.setPassageId("p1");
        ev.setQuote(quote);
        graph.setEvidenceRecords(List.of(ev));
        graph.getEdges().add(GraphAnalysisProjectionTest.typed("assertion", "x", "y",
            "SUBCLASS_OF", GraphAssertionOrigin.SOURCE_ASSERTED, "taxonomy", "ev"));
        return graph;
    }
}
