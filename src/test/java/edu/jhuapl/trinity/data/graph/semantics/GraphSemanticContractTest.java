package edu.jhuapl.trinity.data.graph.semantics;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Contract-only tests; no JavaFX toolkit required. */
class GraphSemanticContractTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void legacyJsonLoadsWithoutInventedSemanticsOrEvidence() throws Exception {
        String legacy = """
            {"type":"GraphDirectedCollection","graphId":"cti-legacy",
             "nodes":[{"entityID":"A","labels":["Threat Actor"]},
                      {"entityID":"B","labels":["Target"]}],
             "edges":[{"startID":"A","endID":"B",
                       "relationship":{"identity":7,"labels":["TARGETS"],
                                       "properties":{"source":"benchmark"}}}]}
            """;
        GraphDirectedCollection graph = MAPPER.readValue(legacy, GraphDirectedCollection.class);
        assertNull(graph.getSchemaVersion());
        assertNull(graph.getEvidenceRecords());
        assertEquals("A", graph.getNodes().get(0).getEntityID());
        assertEquals("B", graph.getEdges().get(0).getEndID());
        GraphRelationship rel = graph.getEdges().get(0).getRelationship();
        assertNull(rel.getSemantics());
        assertEquals(List.of("TARGETS"), rel.getLabels());
        var resolution = new GraphSemanticCatalog().resolve(rel);
        assertNull(resolution.origin());
        assertNull(resolution.predicateId());
        assertEquals("TARGETS", resolution.unclassifiedLegacyLabel());
        assertTrue(GraphSemanticValidator.validate(graph,
            GraphSemanticValidator.Mode.LEGACY_PERMISSIVE).isValid());
        GraphDirectedCollection copy = MAPPER.readValue(MAPPER.writeValueAsString(graph),
            GraphDirectedCollection.class);
        assertEquals("TARGETS", copy.getEdges().get(0).getRelationship().getLabels().get(0));
        assertNull(copy.getEdges().get(0).getEdgeId());
    }

    @Test
    void legacyUmapLabelIsDerivedButDoesNotClaimEvidenceOrMetric() {
        GraphRelationship rel = new GraphRelationship();
        rel.getLabels().add("UMAP_FUZZY_ASSOCIATION");
        rel.getProperties().put("membership", 0.72);
        rel.getProperties().put("derived", true);
        var resolution = new GraphSemanticCatalog().resolve(rel);
        assertEquals("UMAP_FUZZY_ASSOCIATION", resolution.predicateId());
        assertEquals(GraphPredicateFamily.DERIVED_ASSOCIATION, resolution.definition().getFamily());
        assertEquals(GraphAssertionOrigin.ALGORITHM_DERIVED, resolution.origin());
        assertNull(rel.getSemantics());
        assertEquals(0.72, rel.getProperties().get("membership"));
        assertNull(new GraphSemanticCatalog().resolve(new GraphRelationship()).origin());
    }

    @Test
    void typedRoundTripPreservesParallelAssertionsPolyhierarchyAndEvidence() throws Exception {
        GraphDirectedCollection graph = new GraphDirectedCollection();
        graph.setSchemaVersion(2);
        graph.setGraphId("knowledge:1");
        graph.getNodes().addAll(List.of(node("technique", "CLASS"),
            node("network", "CLASS"), node("credentials", "CLASS")));
        GraphRunProvenance provenance = new GraphRunProvenance();
        provenance.setRunId("run-001");
        provenance.setMethod("manual-review");
        graph.setRunProvenance(provenance);
        graph.getEdges().add(edge("subclass:1", "technique", "network", "SUBCLASS_OF",
            "taxonomy:techniques", GraphAssertionOrigin.USER_CURATED));
        graph.getEdges().add(edge("subclass:2", "technique", "credentials", "SUBCLASS_OF",
            "taxonomy:techniques", GraphAssertionOrigin.USER_CURATED));
        GraphPredicateDefinition custom = new GraphPredicateDefinition();
        custom.setPredicateId("RELATED_TO");
        custom.setFamily(GraphPredicateFamily.DOMAIN_ASSERTION);
        custom.setSubjectRole("subject");
        custom.setObjectRole("relatedEntity");
        graph.setPredicateDefinitions(List.of(custom));
        graph.setEvidenceRecords(List.of(evidence("e:passage:1", "p:1"),
            evidence("e:passage:2", "p:2")));
        GraphEdge claimA = edge("claim:a", "technique", "network", "RELATED_TO", null,
            GraphAssertionOrigin.SOURCE_ASSERTED);
        claimA.getRelationship().getSemantics().setEvidenceIds(List.of("e:passage:1", "e:passage:2"));
        GraphEdge claimB = edge("claim:b", "technique", "network", "RELATED_TO", null,
            GraphAssertionOrigin.SOURCE_ASSERTED);
        claimB.getRelationship().getSemantics().setEvidenceIds(List.of("e:passage:2"));
        graph.getEdges().add(claimA);
        graph.getEdges().add(claimB);

        var validated = GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED);
        assertTrue(validated.isValid(), validated.issues().toString());
        GraphDirectedCollection parsed = MAPPER.readValue(MAPPER.writeValueAsString(graph),
            GraphDirectedCollection.class);
        assertEquals(2, parsed.getSchemaVersion());
        assertEquals("run-001", parsed.getRunProvenance().getRunId());
        assertEquals(4, parsed.getEdges().size());
        assertEquals("subclass:1", parsed.getEdges().get(0).getEdgeId());
        assertEquals("subclass:2", parsed.getEdges().get(1).getEdgeId());
        assertEquals("taxonomy:techniques", parsed.getEdges().get(1).getRelationship()
            .getSemantics().getHierarchyScopeId());
        assertEquals(2, parsed.getEdges().get(2).getRelationship().getSemantics().getEvidenceIds().size());
        assertEquals("RELATED_TO", parsed.getPredicateDefinitions().get(0).getPredicateId());
        assertNull(parsed.getEvidenceRecords().get(0).getPageNumber());
        assertNull(parsed.getEvidenceRecords().get(0).getStartOffset());
        assertTrue(GraphSemanticValidator.validate(parsed, GraphSemanticValidator.Mode.STRICT_TYPED)
            .isValid());
    }

    @Test
    void classTaxonomyAndInstanceClassificationRemainDistinct() {
        GraphSemanticCatalog catalog = new GraphSemanticCatalog();
        assertEquals(GraphPredicateFamily.TAXONOMY,
            catalog.definition("SUBCLASS_OF").getFamily());
        assertEquals(GraphPredicateFamily.CLASSIFICATION,
            catalog.definition("INSTANCE_OF").getFamily());
        assertFalse(Boolean.TRUE.equals(catalog.definition("INSTANCE_OF").getTransitive()));
        assertFalse(Boolean.TRUE.equals(catalog.definition("IS_A").getTransitive()));
        assertTrue(Boolean.TRUE.equals(catalog.definition("SUBCLASS_OF").getTransitive()));
        assertThrows(IllegalArgumentException.class, () ->
            catalog.register(catalog.definition("SUBCLASS_OF")));
    }

    @Test
    void endpointDirectionAndDuplicateIdsAreValidatedWithoutCollapsingEdges() {
        GraphDirectedCollection graph = new GraphDirectedCollection();
        graph.setSchemaVersion(2);
        graph.getNodes().addAll(List.of(node("doc", "DOCUMENT"), node("passage", "PASSAGE")));
        GraphPredicateDefinition predicate = new GraphPredicateDefinition();
        predicate.setPredicateId("HAS_PASSAGE");
        predicate.setFamily(GraphPredicateFamily.SOURCE_STRUCTURE);
        predicate.setAllowedSubjectNodeKinds(List.of("DOCUMENT"));
        predicate.setAllowedObjectNodeKinds(List.of("PASSAGE"));
        graph.setPredicateDefinitions(List.of(predicate));
        GraphEdge correct = edge("edge:1", "doc", "passage", "HAS_PASSAGE", "document:A",
            GraphAssertionOrigin.STRUCTURAL_IMPORTED);
        GraphEdge wrongDirection = edge("edge:1", "passage", "doc", "HAS_PASSAGE", "document:A",
            GraphAssertionOrigin.STRUCTURAL_IMPORTED);
        graph.setEdges(new ArrayList<>(List.of(correct, wrongDirection)));
        var result = GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED);
        assertFalse(result.isValid());
        assertTrue(result.issues().stream().anyMatch(i -> i.code().equals("DUPLICATE_EDGE_ID")));
        assertTrue(result.issues().stream().anyMatch(i -> i.code().equals("ENDPOINT_KIND")));
        wrongDirection.setEdgeId("edge:2");
        wrongDirection.setStartID("doc");
        wrongDirection.setEndID("passage");
        assertTrue(GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED)
            .isValid());
    }

    @Test
    void missingEvidenceAndAmbiguousSpansAreRejectedButMissingPagesAreNotFabricated() {
        GraphDirectedCollection graph = new GraphDirectedCollection();
        graph.setSchemaVersion(2);
        graph.getNodes().addAll(List.of(node("a", null), node("b", null)));
        GraphPredicateDefinition predicate = new GraphPredicateDefinition();
        predicate.setPredicateId("REPORTS");
        predicate.setFamily(GraphPredicateFamily.DOMAIN_ASSERTION);
        graph.setPredicateDefinitions(List.of(predicate));
        GraphEdge edge = edge("reports:1", "a", "b", "REPORTS", null,
            GraphAssertionOrigin.SOURCE_ASSERTED);
        edge.getRelationship().getSemantics().setEvidenceIds(List.of("missing"));
        graph.getEdges().add(edge);
        var missing = GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED);
        assertFalse(missing.isValid());
        assertTrue(missing.issues().stream().anyMatch(i -> i.code().equals("UNRESOLVED_EVIDENCE")));
        GraphEvidence source = evidence("missing", "passage:1");
        source.setStartOffset(5L);
        source.setEndOffset(21L);
        graph.setEvidenceRecords(List.of(source));
        var coordinates = GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED);
        assertTrue(coordinates.issues().stream().anyMatch(i -> i.code().equals("EVIDENCE_COORDINATES")));
        source.setSpanCoordinateSystem("DOCUMENT_REVISION_CANONICAL_TEXT_UTF16");
        assertTrue(GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED)
            .isValid());
        assertNull(source.getPageNumber());
    }

    private static GraphNode node(String id, String kind) {
        GraphNode node = new GraphNode();
        node.setEntityID(id);
        node.setNodeKind(kind);
        return node;
    }

    private static GraphEdge edge(String id, String from, String to, String predicate,
                                  String scope, GraphAssertionOrigin origin) {
        GraphRelationshipSemantics semantics = new GraphRelationshipSemantics();
        semantics.setPredicateId(predicate);
        semantics.setHierarchyScopeId(scope);
        semantics.setOrigin(origin);
        GraphRelationship rel = new GraphRelationship();
        rel.getLabels().add(predicate);
        rel.setSemantics(semantics);
        GraphEdge edge = new GraphEdge();
        edge.setEdgeId(id);
        edge.setStartID(from);
        edge.setEndID(to);
        edge.setRelationship(rel);
        return edge;
    }

    private static GraphEvidence evidence(String id, String passageId) {
        GraphEvidence evidence = new GraphEvidence();
        evidence.setEvidenceId(id);
        evidence.setDocumentId("document:1");
        evidence.setDocumentRevision("sha256:example-revision");
        evidence.setPassageId(passageId);
        return evidence;
    }
}
