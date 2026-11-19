package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import edu.jhuapl.trinity.data.graph.analysis.GraphAnalysisProjection;
import edu.jhuapl.trinity.data.graph.analysis.GraphViewSpecification;
import edu.jhuapl.trinity.data.graph.semantics.GraphAssertionOrigin;
import edu.jhuapl.trinity.data.graph.semantics.GraphRelationshipSemantics;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in relationship selection affects actual communities, not just visible edges. */
public class ContextGraphTypedAnalysisTest {
    @Test
    void projectedCommunitiesUseSelectedLinksAndMembershipStrength() {
        GraphDirectedCollection graph = sample();
        GraphViewSpecification fuzzyOnly = GraphViewSpecification.builder()
            .predicateIds("UMAP_FUZZY_ASSOCIATION")
            .origins(GraphAssertionOrigin.ALGORITHM_DERIVED)
            .build();
        GraphAnalysisProjection projection = GraphAnalysisProjection.build(graph, fuzzyOnly);
        assertEquals(2, projection.links().size());
        assertEquals(6, projection.nodes().size());
        ContextGraphCommunityHierarchy selected = ContextGraphCommunityDetection.detect(
            projection, ContextGraphCommunityParams.defaults());
        assertEquals(2, selected.sourceEdgeCount());
        assertEquals(0.9, selected.totalEdgeWeight());
        assertEquals(4, selected.components()); // 6 nodes / 2 independent selected links
        assertEquals(6, selected.entityIds().size());
        assertEquals(5, graph.getEdges().size()); // no filtering of the original source
        ContextGraphCommunityHierarchy unfiltered = ContextGraphCommunityDetection.detect(
            graph, ContextGraphCommunityParams.defaults());
        assertEquals(5, unfiltered.sourceEdgeCount());
        assertTrue(unfiltered.totalEdgeWeight() > selected.totalEdgeWeight());
    }

    @Test
    void classicPoincareCoordinatesAndSourceEdgeIdentityRemainProtected() {
        GraphDirectedCollection graph = sample();
        GraphAnalysisProjection projection = GraphAnalysisProjection.build(graph,
            GraphViewSpecification.builder().predicateIds("UMAP_FUZZY_ASSOCIATION").build());
        ContextGraphCommunityHierarchy selected = ContextGraphCommunityDetection.detect(
            projection, ContextGraphCommunityParams.defaults());
        Map<String, double[]> original = ContextGraphHyperbolicLayout.originalStructuralLayout(graph).positions();
        ContextGraphCommunitySpatialLayout.Result classic = ContextGraphCommunitySpatialLayout.compute(
            projection, selected, ContextGraphHyperbolicPackingParams.defaults());
        assertEquals(original.keySet(), classic.layout().positions().keySet());
        for (String id : original.keySet()) {
            double[] actual = classic.layout().positions().get(id);
            double[] expected = original.get(id);
            for (int axis = 0; axis < 3; axis++) assertEquals(expected[axis], actual[axis]);
        }
        assertTrue(graph.getEdges().containsAll(classic.layout().backboneEdges()));
        var relationshipAware = new ContextGraphHyperbolicPackingParams(
            ContextGraphHyperbolicPackingParams.Placement.RELATIONSHIP_AWARE,
            ContextGraphHyperbolicPackingParams.AngularAllocation.WEIGHTED,
            110, 5, 0.75, ContextGraphHyperbolicPackingParams.RadialProfile.AUTO,
            0.85, List.of());
        ContextGraphCommunitySpatialLayout.Result derived = ContextGraphCommunitySpatialLayout.compute(
            projection, selected, relationshipAware);
        assertEquals(6, derived.layout().positions().size());
        assertTrue(projection.selectedOriginalEdges().containsAll(derived.layout().backboneEdges()));
        assertEquals(5, graph.getEdges().size());
    }

    @Test
    void legacyWeightingIsExplicitlyProtected() {
        GraphDirectedCollection graph = sample();
        GraphAnalysisProjection projection = GraphAnalysisProjection.build(graph,
            GraphViewSpecification.builder().predicateIds("SUBCLASS_OF").build());
        ContextGraphCommunityParams legacy = new ContextGraphCommunityParams(
            ContextGraphCommunityParams.Algorithm.LEGACY, 1, 0, 0, 0.08, 0.65, 1);
        assertThrows(IllegalArgumentException.class,
            () -> ContextGraphCommunityDetection.detect(projection, legacy));
        assertEquals(1, ContextGraphCommunityDetection.detect(graph, legacy).levels().size());
    }

    private static GraphDirectedCollection sample() {
        GraphDirectedCollection graph = new GraphDirectedCollection();
        graph.setGraphId("analysis-sample");
        for (String id : List.of("a", "b", "c", "d", "e", "f")) {
            GraphNode node = new GraphNode();
            node.setEntityID(id);
            node.setVector(new ArrayList<>(List.of(1.0, 2.0, 3.0)));
            graph.getNodes().add(node);
        }
        graph.getEdges().add(edge("a", "b", "fuzzy-1", "UMAP_FUZZY_ASSOCIATION",
            GraphAssertionOrigin.ALGORITHM_DERIVED, 0.8));
        graph.getEdges().add(edge("c", "d", "fuzzy-2", "UMAP_FUZZY_ASSOCIATION",
            GraphAssertionOrigin.ALGORITHM_DERIVED, 0.1));
        graph.getEdges().add(edge("b", "c", "tax-1", "SUBCLASS_OF",
            GraphAssertionOrigin.USER_CURATED, Double.NaN));
        graph.getEdges().add(edge("d", "e", "tax-2", "SUBCLASS_OF",
            GraphAssertionOrigin.USER_CURATED, Double.NaN));
        graph.getEdges().add(edge("e", "f", "tax-3", "SUBCLASS_OF",
            GraphAssertionOrigin.USER_CURATED, Double.NaN));
        return graph;
    }

    private static GraphEdge edge(String a, String b, String id, String predicate,
                                  GraphAssertionOrigin origin, double membership) {
        GraphEdge edge = new GraphEdge();
        edge.setStartID(a); edge.setEndID(b); edge.setEdgeId(id);
        GraphRelationship relationship = new GraphRelationship();
        relationship.getLabels().add(predicate);
        GraphRelationshipSemantics semantics = new GraphRelationshipSemantics();
        semantics.setPredicateId(predicate); semantics.setOrigin(origin);
        relationship.setSemantics(semantics);
        if (Double.isFinite(membership)) relationship.getProperties().put("membership", membership);
        edge.setRelationship(relationship);
        return edge;
    }
}
