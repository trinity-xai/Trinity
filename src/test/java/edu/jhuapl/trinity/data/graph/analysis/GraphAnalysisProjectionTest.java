package edu.jhuapl.trinity.data.graph.analysis;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import edu.jhuapl.trinity.data.graph.semantics.GraphAssertionOrigin;
import edu.jhuapl.trinity.data.graph.semantics.GraphPredicateFamily;
import edu.jhuapl.trinity.data.graph.semantics.GraphRelationshipSemantics;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.*;

/** Pure Java topology contract tests; no JavaFX application is required. */
public class GraphAnalysisProjectionTest {
    @Test
    void filtersByPredicateFamilyOriginAndNamedHierarchyBeforeAnalyzing() {
        GraphDirectedCollection graph = graph("child", "parent", "parent2", "passage", "other");
        GraphEdge a = typed("tax-a", "child", "parent", "SUBCLASS_OF",
            GraphAssertionOrigin.SOURCE_ASSERTED, "taxonomy-a", "ev-a");
        GraphEdge b = typed("tax-b", "child", "parent2", "SUBCLASS_OF",
            GraphAssertionOrigin.USER_CURATED, "taxonomy-b");
        GraphEdge c = typed("mention", "child", "passage", "MENTIONED_IN",
            GraphAssertionOrigin.SOURCE_ASSERTED, null, "ev-c");
        graph.getEdges().addAll(List.of(a, b, c));

        GraphViewSpecification spec = GraphViewSpecification.builder()
            .families(GraphPredicateFamily.TAXONOMY)
            .predicateIds("SUBCLASS_OF")
            .origins(GraphAssertionOrigin.SOURCE_ASSERTED)
            .hierarchyScopes("taxonomy-a").build();
        GraphAnalysisProjection view = GraphAnalysisProjection.build(graph, spec);
        assertEquals(5, view.nodes().size()); // all original entities retained independently
        assertEquals(1, view.links().size());
        assertEquals("tax-a", view.links().get(0).edgeId());
        assertEquals(List.of("ev-a"), view.links().get(0).evidenceIds());
        assertEquals(GraphPredicateFamily.TAXONOMY, view.links().get(0).family());
        assertEquals(0.0, view.adjacency().get("child").getOrDefault("parent2", 0.0).doubleValue());
        assertEquals(1.0, view.adjacency().get("child").get("parent").doubleValue());
        assertEquals(3, graph.getEdges().size());
        assertTrue(graph.getEdges().contains(b));
        assertEquals(3, view.sourceEdgeCount());
        assertSame(a, view.selectedOriginalEdges().get(0));
        assertSame(a, view.toSourceEdgeView().getEdges().get(0));
        assertEquals(5, view.toSourceEdgeView().getNodes().size());
    }

    @Test
    void membershipWeightsRespectThresholdAndNeverBecomeAssertions() {
        GraphDirectedCollection graph = graph("a", "b", "c", "d");
        GraphEdge strong = legacy("a", "b", "UMAP_FUZZY_ASSOCIATION");
        strong.getRelationship().getProperties().put("membership", 0.9);
        GraphEdge weak = legacy("b", "c", "UMAP_FUZZY_ASSOCIATION");
        weak.getRelationship().getProperties().put("membership", 0.2);
        GraphEdge mid = typed("umap-mid", "c", "d", "UMAP_FUZZY_ASSOCIATION",
            GraphAssertionOrigin.ALGORITHM_DERIVED, null);
        mid.getRelationship().getProperties().put("membership", 0.6);
        graph.getEdges().addAll(List.of(strong, weak, mid));
        GraphViewSpecification spec = GraphViewSpecification.builder()
            .predicateIds("UMAP_FUZZY_ASSOCIATION")
            .origins(GraphAssertionOrigin.ALGORITHM_DERIVED)
            .includeKnownLegacyLabels(true)
            .minimumUmapMembership(0.25)
            .predicateWeight("UMAP_FUZZY_ASSOCIATION", 2.0).build();
        GraphAnalysisProjection view = GraphAnalysisProjection.build(graph, spec);
        assertEquals(2, view.links().size());
        assertEquals(1.8, view.adjacency().get("a").get("b").doubleValue());
        assertEquals(1.2, view.adjacency().get("c").get("d").doubleValue());
        assertEquals(GraphAssertionOrigin.ALGORITHM_DERIVED, view.links().get(0).origin());
        assertTrue(view.links().get(0).interpretedLegacyLabel());
        assertEquals(List.of(), view.links().get(0).evidenceIds());
        assertEquals(3, graph.getEdges().size());
        GraphAnalysisProjection fixed = GraphAnalysisProjection.build(graph,
            GraphViewSpecification.builder().predicateIds("UMAP_FUZZY_ASSOCIATION")
                .includeKnownLegacyLabels(true).useUmapMembership(false).build());
        assertEquals(3, fixed.links().size());
        assertEquals(1.0, fixed.adjacency().get("b").get("c").doubleValue());
        strong.getRelationship().getProperties().put("membership", "fake");
        assertThrows(IllegalArgumentException.class,
            () -> GraphAnalysisProjection.build(graph, spec));
    }

    @Test
    void parallelAssertionsAndOriginalEdgeReferencesSurviveUndirectedAggregation() {
        GraphDirectedCollection graph = graph("entity-a", "entity-b", "isolated");
        GraphEdge first = typed("claim-1", "entity-a", "entity-b", "SUBCLASS_OF",
            GraphAssertionOrigin.SOURCE_ASSERTED, "tax", "passage-1");
        GraphEdge second = typed("claim-2", "entity-a", "entity-b", "SUBCLASS_OF",
            GraphAssertionOrigin.SOURCE_ASSERTED, "tax", "passage-2");
        graph.getEdges().addAll(List.of(first, second));
        GraphAnalysisProjection view = GraphAnalysisProjection.build(graph,
            GraphViewSpecification.builder().predicateIds("SUBCLASS_OF")
                .predicateWeight("SUBCLASS_OF", 3.0).build());
        assertEquals(2, view.links().size());
        assertEquals(6.0, view.adjacency().get("entity-a").get("entity-b").doubleValue());
        assertEquals(6.0, view.adjacency().get("entity-b").get("entity-a").doubleValue());
        assertEquals("claim-1", view.links().get(0).edgeId());
        assertEquals("claim-2", view.links().get(1).edgeId());
        assertEquals(List.of("passage-2"), view.links().get(1).evidenceIds());
        assertEquals(3, view.nodes().size());
        assertTrue(view.adjacency().get("isolated").isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> view.links().clear());
        assertThrows(UnsupportedOperationException.class,
            () -> view.adjacency().get("entity-a").put("x", 9.0));
        first.getRelationship().getSemantics().getEvidenceIds().clear();
        assertEquals(List.of("passage-1"), view.links().get(0).evidenceIds());
        assertSame(first, view.selectedOriginalEdges().get(0));
    }

    @Test
    void explicitRoleFilteringAndUnregisteredPredicatesAreNotInvented() {
        GraphDirectedCollection graph = graph("a", "b", "c");
        graph.getNodes().get(0).setNodeKind("ENTITY");
        graph.getNodes().get(1).setNodeKind("ENTITY");
        graph.getNodes().get(2).setNodeKind("PASSAGE");
        graph.getEdges().add(typed("custom", "a", "b", "MAKES_CLAIM",
            GraphAssertionOrigin.USER_CURATED, null));
        graph.getEdges().add(legacy("b", "c", "UNKNOWN_LEGACY"));
        GraphAnalysisProjection defaultView = GraphAnalysisProjection.build(graph,
            GraphViewSpecification.builder().build());
        assertTrue(defaultView.links().isEmpty());
        GraphAnalysisProjection custom = GraphAnalysisProjection.build(graph,
            GraphViewSpecification.builder().includeUnclassified(true)
                .predicateIds("MAKES_CLAIM").nodeKinds("ENTITY")
                .retainIsolatedNodes(false).build());
        assertEquals(2, custom.nodes().size());
        assertEquals(1, custom.links().size());
        assertEquals("MAKES_CLAIM", custom.links().get(0).predicateId());
        assertEquals(GraphPredicateFamily.UNCLASSIFIED, custom.links().get(0).family());
        GraphAnalysisProjection legacy = GraphAnalysisProjection.build(graph,
            GraphViewSpecification.allRelationships());
        assertEquals(2, legacy.links().size());
        assertEquals(GraphPredicateFamily.UNCLASSIFIED, legacy.links().get(1).family());
        assertNull(legacy.links().get(1).predicateId());
    }

    @Test
    void deterministicAdjacencyCancellationAndComparison() {
        GraphDirectedCollection graph = graph("a", "b", "c");
        for (int i = 0; i < 30; i++) {
            GraphEdge edge = typed("e" + i, "a", i % 2 == 0 ? "b" : "c", "SUBCLASS_OF",
                GraphAssertionOrigin.USER_CURATED, "scope");
            graph.getEdges().add(edge);
        }
        GraphViewSpecification spec = GraphViewSpecification.builder()
            .predicateIds("SUBCLASS_OF").build();
        GraphAnalysisProjection before = GraphAnalysisProjection.build(graph, spec);
        Collections.shuffle(graph.getEdges(), new java.util.Random(23));
        GraphAnalysisProjection after = GraphAnalysisProjection.build(graph, spec);
        assertEquals(before.adjacency(), after.adjacency());
        assertEquals(before.links().stream().map(GraphAnalysisProjection.Link::edgeId).toList(),
            after.links().stream().map(GraphAnalysisProjection.Link::edgeId).toList());
        assertEquals(30, GraphAnalysisComparison.compare(before, after).commonEdges());
        assertEquals(1.0, GraphAnalysisComparison.compare(before, after).edgeJaccard());
        assertEquals(30, GraphAnalysisComparison.compare(before, before).commonEdges());
        assertEquals(1.0, GraphAnalysisComparison.compare(before, before).edgeJaccard());
        assertThrows(CancellationException.class,
            () -> GraphAnalysisProjection.build(graph, spec, () -> true));
        assertThrows(IllegalArgumentException.class,
            () -> GraphAnalysisComparison.compare(before, GraphAnalysisProjection.build(
                graph("a", "b"), spec)));
    }

    static GraphDirectedCollection graph(String... ids) {
        GraphDirectedCollection graph = new GraphDirectedCollection();
        graph.setSchemaVersion(2);
        for (String id : ids) {
            GraphNode node = new GraphNode();
            node.setEntityID(id);
            node.getLabels().add("Entity");
            graph.getNodes().add(node);
        }
        return graph;
    }

    static GraphEdge typed(String id, String start, String end, String predicate,
                           GraphAssertionOrigin origin, String scope, String... evidence) {
        GraphEdge edge = new GraphEdge();
        edge.setStartID(start); edge.setEndID(end); edge.setEdgeId(id);
        GraphRelationship relationship = new GraphRelationship();
        relationship.getLabels().add(predicate);
        GraphRelationshipSemantics semantics = new GraphRelationshipSemantics();
        semantics.setPredicateId(predicate); semantics.setOrigin(origin);
        semantics.setHierarchyScopeId(scope);
        semantics.setEvidenceIds(new ArrayList<>(List.of(evidence)));
        relationship.setSemantics(semantics);
        edge.setRelationship(relationship);
        return edge;
    }

    static GraphEdge legacy(String start, String end, String label) {
        GraphEdge edge = new GraphEdge();
        edge.setStartID(start); edge.setEndID(end);
        GraphRelationship relationship = new GraphRelationship();
        relationship.getLabels().add(label);
        edge.setRelationship(relationship);
        return edge;
    }
}
