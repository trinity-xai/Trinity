package edu.jhuapl.trinity.data.graph.semantics;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Data-layer tests: do not initialize or modify a JavaFX scene. */
class GraphHierarchyIndexTest {

    @Test
    void multiParentTaxonomyTraversesEveryActualParentAndDistinctNodePath() {
        GraphDirectedCollection graph = graph("tech", "a", "b", "root", "inst");
        graph.getEdges().add(edge("e1", "tech", "b", "SUBCLASS_OF", "tax:security"));
        graph.getEdges().add(edge("e2", "tech", "a", "SUBCLASS_OF", "tax:security"));
        graph.getEdges().add(edge("e3", "a", "root", "SUBCLASS_OF", "tax:security"));
        graph.getEdges().add(edge("e4", "b", "root", "SUBCLASS_OF", "tax:security"));
        graph.getEdges().add(edge("e5", "inst", "tech", "INSTANCE_OF", "tax:security"));
        GraphHierarchyIndex index = GraphHierarchyIndex.build(graph);
        GraphHierarchy h = index.hierarchy("tax:security", "SUBCLASS_OF");
        assertEquals(List.of("a", "b"), h.immediateParents("tech"));
        assertEquals(List.of("tech"), h.immediateChildren("a"));
        assertEquals(List.of("a", "b", "root"), h.ancestors("tech"));
        assertEquals(List.of("a", "b", "tech"), h.descendants("root"));
        assertEquals(List.of("tech", "a", "root"), h.shortestPathToAncestor("tech", "root"));
        assertEquals(List.of(List.of("tech", "a", "root"), List.of("tech", "b", "root")),
            h.pathsToAncestor("tech", "root", 5, 6));
        assertEquals(1, h.pathsToAncestor("tech", "root", 1, 6).size());
        assertEquals(List.of(), h.pathsToAncestor("tech", "root", 5, 1));
        assertNull(index.hierarchy("tax:security", "INSTANCE_OF"));
        assertFalse(h.containsCycles());
        assertTrue(GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED)
            .isValid());
    }

    @Test
    void overlappingTaxonomiesDoNotPolluteEachOtherOrJoinOtherPredicates() {
        GraphDirectedCollection graph = graph("x", "a", "b", "container");
        graph.getEdges().add(edge("tA", "x", "a", "SUBCLASS_OF", "taxonomy:A"));
        graph.getEdges().add(edge("tB", "x", "b", "SUBCLASS_OF", "taxonomy:B"));
        graph.getEdges().add(edge("part", "x", "container", "PART_OF", "taxonomy:A"));
        GraphHierarchyIndex index = GraphHierarchyIndex.build(graph);
        assertEquals(List.of("a"), index.hierarchy("taxonomy:A", "SUBCLASS_OF")
            .immediateParents("x"));
        assertEquals(List.of("b"), index.hierarchy("taxonomy:B", "SUBCLASS_OF")
            .immediateParents("x"));
        assertEquals(List.of("container"), index.hierarchy("taxonomy:A", "PART_OF")
            .immediateParents("x"));
        assertEquals(2, index.inScope("taxonomy:A").size());
        assertEquals(3, index.keys().size());
    }

    @Test
    void containsReversesDirectionAndStaysSeparateFromPartOf() {
        GraphDirectedCollection graph = graph("doc", "section", "passage");
        graph.getEdges().add(edge("c1", "doc", "section", "CONTAINS", "document:1"));
        graph.getEdges().add(edge("c2", "section", "passage", "CONTAINS", "document:1"));
        graph.getEdges().add(edge("p1", "passage", "doc", "PART_OF", "document:1"));
        GraphHierarchyIndex index = GraphHierarchyIndex.build(graph);
        GraphHierarchy contains = index.hierarchy("document:1", "CONTAINS");
        assertEquals(GraphHierarchyOrientation.SUBJECT_IS_PARENT, contains.orientation());
        assertEquals("doc", contains.parentLinks("section").get(0).sourceStartId());
        assertEquals("section", contains.parentLinks("section").get(0).sourceEndId());
        assertEquals(List.of("section"), contains.immediateParents("passage"));
        assertEquals(List.of("section", "doc"), contains.ancestors("passage"));
        assertEquals(List.of("section", "passage"), contains.descendants("doc"));
        assertEquals(List.of("passage", "section", "doc"),
            contains.shortestPathToAncestor("passage", "doc"));
        assertEquals(List.of("doc"), index.hierarchy("document:1", "PART_OF")
            .immediateParents("passage"));
        assertTrue(GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED)
            .isValid());
    }

    @Test
    void parallelAssertionsRetainIndependentIdsAndDefensiveEvidenceCopies() {
        GraphDirectedCollection graph = graph("child", "parent");
        GraphEdge first = edge("claim:1", "child", "parent", "SUBCLASS_OF", "tax");
        first.getRelationship().getSemantics().setEvidenceIds(new ArrayList<>(List.of("ev:a")));
        GraphEdge second = edge("claim:2", "child", "parent", "SUBCLASS_OF", "tax");
        second.getRelationship().getSemantics().setEvidenceIds(new ArrayList<>(List.of("ev:b")));
        graph.getEdges().addAll(List.of(first, second));
        GraphHierarchy hierarchy = GraphHierarchyIndex.build(graph).hierarchy("tax", "SUBCLASS_OF");
        assertEquals(List.of("parent"), hierarchy.immediateParents("child"));
        assertEquals(List.of("claim:1", "claim:2"), hierarchy.linksBetween("child", "parent")
            .stream().map(GraphHierarchyLink::edgeId).toList());
        assertEquals(List.of("ev:a"), hierarchy.linksBetween("child", "parent")
            .get(0).evidenceIds());
        first.setEndID("child");
        first.getRelationship().getSemantics().getEvidenceIds().set(0, "changed");
        assertEquals(List.of("parent"), hierarchy.immediateParents("child"));
        assertEquals(List.of("ev:a"), hierarchy.linksBetween("child", "parent")
            .get(0).evidenceIds());
        assertThrows(UnsupportedOperationException.class,
            () -> hierarchy.parentsByNode().clear());
        assertThrows(UnsupportedOperationException.class,
            () -> hierarchy.immediateParents("child").add("unwanted"));
        assertThrows(UnsupportedOperationException.class,
            () -> hierarchy.linksBetween("child", "parent").get(0).evidenceIds().clear());
    }

    @Test
    void cycleDiagnosticsAreScopedAndOnlyRequiredDagsFailValidation() {
        GraphDirectedCollection graph = graph("a", "b", "c", "x", "y");
        graph.getEdges().add(edge("s1", "a", "b", "SUBCLASS_OF", "tax:A"));
        graph.getEdges().add(edge("s2", "b", "a", "SUBCLASS_OF", "tax:A"));
        graph.getEdges().add(edge("s3", "c", "c", "SUBCLASS_OF", "tax:A"));
        graph.getEdges().add(edge("s4", "a", "b", "SUBCLASS_OF", "tax:B"));
        graph.getEdges().add(edge("p1", "x", "y", "PART_OF", "parts"));
        graph.getEdges().add(edge("p2", "y", "x", "PART_OF", "parts"));
        GraphHierarchyIndex index = GraphHierarchyIndex.build(graph);
        assertEquals(List.of(List.of("a", "b"), List.of("c")),
            index.hierarchy("tax:A", "SUBCLASS_OF").cycleComponents());
        assertFalse(index.hierarchy("tax:B", "SUBCLASS_OF").containsCycles());
        assertEquals(List.of(List.of("x", "y")),
            index.hierarchy("parts", "PART_OF").cycleComponents());
        var result = GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED);
        assertEquals(2L, result.issues().stream()
            .filter(issue -> issue.code().equals("HIERARCHY_CYCLE")).count());
        assertTrue(result.issues().stream().filter(issue -> issue.code().equals("HIERARCHY_CYCLE"))
            .allMatch(issue -> issue.location().contains("tax:A")));
    }

    @Test
    void customDirectionalSingleParentConstraintIsCheckedPerScope() {
        GraphDirectedCollection graph = graph("a", "b", "child");
        GraphPredicateDefinition definition = new GraphPredicateDefinition();
        definition.setPredicateId("HAS_CHILD");
        definition.setFamily(GraphPredicateFamily.SOURCE_STRUCTURE);
        definition.setHierarchy(true);
        definition.setHierarchyOrientation(GraphHierarchyOrientation.SUBJECT_IS_PARENT);
        definition.setAcyclic(true);
        definition.setAllowsMultipleParents(false);
        graph.setPredicateDefinitions(List.of(definition));
        graph.getEdges().add(edge("a1", "a", "child", "HAS_CHILD", "scope1"));
        graph.getEdges().add(edge("a2", "b", "child", "HAS_CHILD", "scope1"));
        graph.getEdges().add(edge("a3", "b", "child", "HAS_CHILD", "scope2"));
        graph.getEdges().add(edge("a4", "b", "child", "HAS_CHILD", "scope2"));
        GraphHierarchyIndex index = GraphHierarchyIndex.build(graph);
        assertEquals(List.of("a", "b"), index.hierarchy("scope1", "HAS_CHILD")
            .immediateParents("child"));
        assertEquals(List.of("b"), index.hierarchy("scope2", "HAS_CHILD")
            .immediateParents("child"));
        var result = GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED);
        assertEquals(1L, result.issues().stream()
            .filter(issue -> issue.code().equals("HIERARCHY_MULTIPLE_PARENTS")).count());
        assertFalse(result.isValid());
        definition.setHierarchyOrientation(null);
        var missing = GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED);
        assertTrue(missing.issues().stream()
            .anyMatch(issue -> issue.code().equals("HIERARCHY_ORIENTATION")));
        assertThrows(IllegalArgumentException.class, () -> GraphHierarchyIndex.build(graph));
    }

    @Test
    void optionalLegacyCompatibilityDoesNotInventEvidenceOrIndexUnknownLabels() {
        GraphDirectedCollection graph = graph("a", "b", "c");
        graph.getEdges().add(legacy("a", "b", "SUBCLASS_OF"));
        graph.getEdges().add(legacy("a", "c", "SOME_CUSTOM_RELATIONSHIP"));
        graph.getEdges().add(legacy("a", "c", "IS_A"));
        GraphHierarchyIndex typed = GraphHierarchyIndex.build(graph);
        assertTrue(typed.hierarchies().isEmpty());
        GraphHierarchyIndex legacy = GraphHierarchyIndex.build(graph, true);
        GraphHierarchy hierarchy = legacy.hierarchy(null, "SUBCLASS_OF");
        assertEquals(List.of("b"), hierarchy.immediateParents("a"));
        assertTrue(hierarchy.parentLinks("a").get(0).fromLegacyLabels());
        assertNull(hierarchy.parentLinks("a").get(0).origin());
        assertEquals(List.of(), hierarchy.parentLinks("a").get(0).evidenceIds());
        assertNull(legacy.hierarchy(null, "IS_A"));
        assertNull(legacy.hierarchy(null, "SOME_CUSTOM_RELATIONSHIP"));
        assertTrue(GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.LEGACY_PERMISSIVE)
            .isValid());
    }

    @Test
    void traversalIsBoundedCycleSafeAndCancellable() {
        GraphDirectedCollection graph = graph("a", "b", "c", "d");
        graph.getEdges().add(edge("ab", "a", "b", "PART_OF", "scope"));
        graph.getEdges().add(edge("bc", "b", "c", "PART_OF", "scope"));
        graph.getEdges().add(edge("ca", "c", "a", "PART_OF", "scope"));
        graph.getEdges().add(edge("cd", "c", "d", "PART_OF", "scope"));
        GraphHierarchy hierarchy = GraphHierarchyIndex.build(graph).hierarchy("scope", "PART_OF");
        assertEquals(List.of("b", "c"), hierarchy.ancestors("a", 2, () -> false));
        assertEquals(List.of("a", "b", "c", "d"),
            hierarchy.shortestPathToAncestor("a", "d"));
        assertEquals(List.of(List.of("a", "b", "c", "d")),
            hierarchy.pathsToAncestor("a", "d", 3, 5));
        assertTrue(hierarchy.pathsToAncestor("a", "d", 3, 2).isEmpty());
        assertThrows(IllegalStateException.class,
            () -> hierarchy.pathsToAncestor("a", "d", 3, 5, 1, () -> false));
        assertThrows(CancellationException.class,
            () -> GraphHierarchyIndex.build(graph, false, () -> true));
        assertThrows(CancellationException.class,
            () -> hierarchy.ancestors("a", 5, () -> true));
        AtomicBoolean cancelled = new AtomicBoolean(true);
        assertThrows(CancellationException.class,
            () -> hierarchy.pathsToAncestor("a", "d", 3, 5, cancelled::get));
    }

    @Test
    void cyclicUnconstrainedRelationsAreValidAndOrderingIsIndependentOfSourceEdgeOrder() {
        GraphDirectedCollection graph = graph("x", "z", "a", "b");
        graph.getEdges().add(edge("z1", "x", "z", "PART_OF", "scope"));
        graph.getEdges().add(edge("a1", "x", "a", "PART_OF", "scope"));
        graph.getEdges().add(edge("a2", "x", "a", "PART_OF", "scope"));
        graph.getEdges().add(edge("loop", "z", "x", "PART_OF", "scope"));
        GraphHierarchy first = GraphHierarchyIndex.build(graph).hierarchy("scope", "PART_OF");
        assertEquals(List.of("a", "z"), first.immediateParents("x"));
        assertEquals(List.of("a1", "a2"), first.linksBetween("x", "a")
            .stream().map(GraphHierarchyLink::edgeId).toList());
        assertTrue(first.containsCycles());
        assertTrue(GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED)
            .isValid());
        java.util.Collections.reverse(graph.getEdges());
        GraphHierarchy second = GraphHierarchyIndex.build(graph).hierarchy("scope", "PART_OF");
        assertEquals(first.immediateParents("x"), second.immediateParents("x"));
        assertEquals(first.cycleComponents(), second.cycleComponents());
        assertEquals(first.linksBetween("x", "a").stream().map(GraphHierarchyLink::edgeId).toList(),
            second.linksBetween("x", "a").stream().map(GraphHierarchyLink::edgeId).toList());
    }

    @Test
    void customOrientationSurvivesJacksonGraphRoundTrip() throws Exception {
        GraphDirectedCollection graph = graph("parent", "child");
        GraphPredicateDefinition def = new GraphPredicateDefinition();
        def.setPredicateId("HAS_DESCENDANT");
        def.setFamily(GraphPredicateFamily.SOURCE_STRUCTURE);
        def.setHierarchy(true);
        def.setHierarchyOrientation(GraphHierarchyOrientation.SUBJECT_IS_PARENT);
        def.setAcyclic(true);
        def.setAllowsMultipleParents(true);
        graph.setPredicateDefinitions(List.of(def));
        graph.getEdges().add(edge("descendant:1", "parent", "child",
            "HAS_DESCENDANT", "source:doc"));
        ObjectMapper mapper = new ObjectMapper();
        GraphDirectedCollection restored = mapper.readValue(mapper.writeValueAsString(graph),
            GraphDirectedCollection.class);
        assertEquals(GraphHierarchyOrientation.SUBJECT_IS_PARENT,
            restored.getPredicateDefinitions().get(0).getHierarchyOrientation());
        GraphHierarchy hierarchy = GraphHierarchyIndex.build(restored)
            .hierarchy("source:doc", "HAS_DESCENDANT");
        assertEquals(List.of("parent"), hierarchy.immediateParents("child"));
        assertTrue(GraphSemanticValidator.validate(restored,
            GraphSemanticValidator.Mode.STRICT_TYPED).isValid());
    }

    @Test
    void deepHierarchyIsIterativeAndDoesNotOverflowTheStack() {
        final int n = 10_000;
        GraphDirectedCollection graph = new GraphDirectedCollection();
        graph.setSchemaVersion(2);
        for (int i = 0; i < n; i++) graph.getNodes().add(node("node:" + i));
        for (int i = 0; i < n - 1; i++) {
            graph.getEdges().add(edge("e:" + i, "node:" + i, "node:" + (i + 1),
                "SUBCLASS_OF", "large"));
        }
        GraphHierarchy h = GraphHierarchyIndex.build(graph).hierarchy("large", "SUBCLASS_OF");
        assertEquals(n, h.nodeCount());
        assertFalse(h.containsCycles());
        assertEquals(n - 1, h.ancestors("node:0").size());
        assertEquals(n, h.shortestPathToAncestor("node:0", "node:9999").size());
    }

    private static GraphDirectedCollection graph(String... ids) {
        GraphDirectedCollection graph = new GraphDirectedCollection();
        graph.setSchemaVersion(2);
        for (String id : ids) graph.getNodes().add(node(id));
        return graph;
    }

    private static GraphNode node(String id) {
        GraphNode node = new GraphNode();
        node.setEntityID(id);
        return node;
    }

    private static GraphEdge edge(String edgeId, String start, String end,
                                  String predicate, String scope) {
        GraphRelationshipSemantics semantics = new GraphRelationshipSemantics();
        semantics.setPredicateId(predicate);
        semantics.setHierarchyScopeId(scope);
        semantics.setOrigin(GraphAssertionOrigin.USER_CURATED);
        GraphRelationship relationship = new GraphRelationship();
        relationship.getLabels().add(predicate);
        relationship.setSemantics(semantics);
        GraphEdge edge = new GraphEdge();
        edge.setEdgeId(edgeId);
        edge.setStartID(start);
        edge.setEndID(end);
        edge.setRelationship(relationship);
        return edge;
    }

    private static GraphEdge legacy(String start, String end, String label) {
        GraphEdge edge = new GraphEdge();
        edge.setStartID(start);
        edge.setEndID(end);
        GraphRelationship relationship = new GraphRelationship();
        relationship.getLabels().add(label);
        edge.setRelationship(relationship);
        return edge;
    }
}
