package edu.jhuapl.trinity.data.graph.construction.source;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.semantics.GraphAssertionOrigin;
import edu.jhuapl.trinity.data.graph.semantics.GraphHierarchyIndex;
import edu.jhuapl.trinity.data.graph.semantics.GraphPredicateDefinition;
import edu.jhuapl.trinity.data.graph.semantics.GraphPredicateFamily;
import edu.jhuapl.trinity.data.graph.semantics.GraphSemanticValidator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SourceSupportedAssertionBuilderTest {
    private static SourceDocumentSnapshot source() {
        String id = SourceDocumentIdentity.documentId("logical-source://document-one");
        String text = "A technique belongs to two categories.\nSecond source confirms both.";
        String revision = SourceDocumentIdentity.revision(text);
        int firstEnd = text.indexOf('\n');
        var a = new SourceDocumentSnapshot.Passage(
            SourceDocumentIdentity.passageId(id, revision, 0, text.substring(0, firstEnd)),
            0, text.substring(0, firstEnd), "d", Map.of(), 0, firstEnd, null);
        var b = new SourceDocumentSnapshot.Passage(
            SourceDocumentIdentity.passageId(id, revision, 1, text.substring(firstEnd + 1)),
            1, text.substring(firstEnd + 1), "d", Map.of(), firstEnd + 1, text.length(), null);
        return new SourceDocumentSnapshot(id, revision, "source.txt", null,
            "TEXT_UTF8", text, List.of(), List.of(a, b), Map.of());
    }

    private static SourceSupportedAssertionBuilder.Assertion assertion(String id,
            String parent, String scope, String... passageIds) {
        return new SourceSupportedAssertionBuilder.Assertion(id, "tech", "SUBCLASS_OF", parent,
            scope, GraphAssertionOrigin.SOURCE_ASSERTED, List.of(passageIds));
    }

    private static List<SourceSupportedAssertionBuilder.Entity> entities() {
        return List.of(new SourceSupportedAssertionBuilder.Entity("tech", "Technique", "CLASS"),
            new SourceSupportedAssertionBuilder.Entity("parent-A", "Category A", "CLASS"),
            new SourceSupportedAssertionBuilder.Entity("parent-B", "Category B", "CLASS"));
    }

    @Test
    void supportedMultipleParentsRemainInSameNamedTaxonomyWithIndependentEvidence() {
        var source = source();
        var assertions = List.of(
            assertion("assertion-a", "parent-A", "taxonomy:security",
                source.passages().get(0).passageId(), source.passages().get(1).passageId()),
            assertion("assertion-b", "parent-B", "taxonomy:security",
                source.passages().get(0).passageId()));
        GraphDirectedCollection graph = SourceSupportedAssertionBuilder.build(source, entities(),
            assertions, List.of());
        assertTrue(GraphSemanticValidator.validate(graph,
            GraphSemanticValidator.Mode.STRICT_TYPED).isValid());
        var hierarchy = GraphHierarchyIndex.build(graph).hierarchy("taxonomy:security", "SUBCLASS_OF");
        assertEquals(List.of("parent-A", "parent-B"), hierarchy.immediateParents("tech"));
        var other = GraphHierarchyIndex.build(graph).hierarchy("source:" + source.documentId(), "CONTAINS");
        assertEquals(2, other.immediateChildren(source.documentId()).size());
        var a = graph.getEdges().stream().filter(edge -> "assertion-a".equals(edge.getEdgeId()))
            .findFirst().orElseThrow();
        assertEquals(2, a.getRelationship().getSemantics().getEvidenceIds().size());
        assertEquals(GraphAssertionOrigin.SOURCE_ASSERTED, a.getRelationship().getSemantics().getOrigin());
    }

    @Test
    void scopesDoNotMergeAndParallelClaimsRetainDifferentAssertions() {
        var source = source();
        String passage = source.passages().get(0).passageId();
        var graph = SourceSupportedAssertionBuilder.build(source, entities(), List.of(
            assertion("claim-1", "parent-A", "taxonomy:one", passage),
            assertion("claim-2", "parent-A", "taxonomy:two", passage),
            assertion("claim-3", "parent-A", "taxonomy:one", source.passages().get(1).passageId())),
            List.of());
        var index = GraphHierarchyIndex.build(graph);
        assertEquals(2, index.hierarchy("taxonomy:one", "SUBCLASS_OF").links().size());
        assertEquals(1, index.hierarchy("taxonomy:two", "SUBCLASS_OF").links().size());
        assertEquals(2, index.hierarchy("taxonomy:one", "SUBCLASS_OF")
            .linksBetween("tech", "parent-A").size());
    }

    @Test
    void rejectsUnknownPassageEvidenceAndImplicitOntologyScope() {
        var source = source();
        assertThrows(IllegalArgumentException.class, () -> SourceSupportedAssertionBuilder.build(
            source, entities(), List.of(assertion("unknown", "parent-A", "taxonomy:one", "missing")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> SourceSupportedAssertionBuilder.build(
            source, entities(), List.of(assertion("no-scope", "parent-A", null,
                source.passages().get(0).passageId())), List.of()));
    }

    @Test
    void customDomainPredicateRequiresExplicitDefinitionAndSourceSupport() {
        var source = source();
        var extracted = new SourceSupportedAssertionBuilder.Assertion("domain-1", "tech", "USED_IN",
            "parent-A", null, GraphAssertionOrigin.SOURCE_ASSERTED,
            List.of(source.passages().get(0).passageId()));
        assertThrows(IllegalArgumentException.class, () -> SourceSupportedAssertionBuilder.build(
            source, entities(), List.of(extracted), List.of()));
        GraphPredicateDefinition definition = new GraphPredicateDefinition();
        definition.setPredicateId("USED_IN");
        definition.setFamily(GraphPredicateFamily.DOMAIN_ASSERTION);
        definition.setSubjectRole("method");
        definition.setObjectRole("operation");
        var graph = SourceSupportedAssertionBuilder.build(source, entities(),
            List.of(extracted), List.of(definition));
        assertEquals("USED_IN", graph.getEdges().get(graph.getEdges().size()-1)
            .getRelationship().getSemantics().getPredicateId());
        assertTrue(GraphSemanticValidator.validate(graph,
            GraphSemanticValidator.Mode.STRICT_TYPED).isValid());
    }

    @Test
    void manualPdfPageSnapshotPreservesPageContainmentWithoutGuessingHeadings() {
        String docId = SourceDocumentIdentity.documentId("pdf://two-pages");
        String text = "first page\nsecond page\n";
        String rev = SourceDocumentIdentity.revision(text);
        int boundary = "first page\n".length();
        var one = new SourceDocumentSnapshot.Passage(
            SourceDocumentIdentity.passageId(docId, rev, 0, text.substring(0, boundary)),
            0, text.substring(0, boundary), "pdf", Map.of(), 0, boundary, 1);
        var two = new SourceDocumentSnapshot.Passage(
            SourceDocumentIdentity.passageId(docId, rev, 1, text.substring(boundary)),
            1, text.substring(boundary), "pdf", Map.of(), boundary, text.length(), 2);
        var source = new SourceDocumentSnapshot(docId, rev, "scanned.pdf", null,
            "PDFBOX_PER_PAGE_SORT_BY_POSITION", text,
            List.of(new SourceDocumentSnapshot.PageSpan(1, 0, boundary),
                new SourceDocumentSnapshot.PageSpan(2, boundary, text.length())),
            List.of(one, two), Map.of());
        var graph = SourceDocumentHierarchyBuilder.build(source);
        assertEquals(5, graph.getNodes().size());
        assertEquals(4, graph.getEdges().size());
        var hierarchy = GraphHierarchyIndex.build(graph)
            .hierarchy("source:" + docId, "CONTAINS");
        assertEquals(2, hierarchy.ancestors(two.passageId()).size());
        assertEquals(Integer.valueOf(2), graph.getEvidenceRecords().get(1).getPageNumber());
        assertTrue(graph.getNodes().stream().noneMatch(n -> "SECTION".equals(n.getNodeKind())));
        assertTrue(GraphSemanticValidator.validate(graph,
            GraphSemanticValidator.Mode.STRICT_TYPED).isValid());
    }

    @Test
    void sourceAssertionCannotPretendToHaveEvidenceWhenNoneExists() {
        assertThrows(IllegalArgumentException.class, () -> new SourceSupportedAssertionBuilder.Assertion(
            "unsupported", "tech", "SUBCLASS_OF", "parent-A", "tax:one",
            GraphAssertionOrigin.SOURCE_ASSERTED, List.of()));
        assertThrows(IllegalArgumentException.class, () -> new SourceSupportedAssertionBuilder.Assertion(
            "derived", "tech", "SUBCLASS_OF", "parent-A", "tax:one",
            GraphAssertionOrigin.ALGORITHM_DERIVED, List.of("p")));
    }
}
