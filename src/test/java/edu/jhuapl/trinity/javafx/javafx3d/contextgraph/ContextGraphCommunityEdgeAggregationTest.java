package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Community edge summaries are derived metadata, not new source GraphEdge objects. */
public class ContextGraphCommunityEdgeAggregationTest {
    @Test
    public void countsTypesAndOriginalIdentityAreConservedAtEveryLevel() {
        GraphDirectedCollection graph = testGraph();
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            ContextGraphCommunityParams.defaults());
        Set<GraphEdge> identity = Collections.newSetFromMap(new IdentityHashMap<>());
        identity.addAll(graph.getEdges());
        for (int level = 0; level < hierarchy.levels().size(); level++) {
            ContextGraphCommunityEdgeAggregation summary =
                ContextGraphCommunityEdgeAggregation.compute(graph, hierarchy, level);
            int represented = 0, semantic = 0, structural = 0, admin = 0;
            Set<GraphEdge> found = Collections.newSetFromMap(new IdentityHashMap<>());
            for (ContextGraphCommunityEdgeBundle connection : summary.connections()) {
                check(connection.pair().first() < connection.pair().second(), "Pair not canonical");
                check(connection.originalCount() == connection.semanticCount()
                    + connection.structuralCount() + connection.administrativeCount(),
                    "Classification counts do not add up");
                check(connection.originalCount() == connection.typeCounts().values().stream()
                    .mapToInt(Integer::intValue).sum(), "Relationship counts do not add up");
                for (GraphEdge edge : connection.originals()) {
                    check(identity.contains(edge), "New artificial source relationship was created");
                    check(found.add(edge), "Source relationship counted twice");
                }
                represented += connection.originalCount();
                semantic += connection.semanticCount();
                structural += connection.structuralCount();
                admin += connection.administrativeCount();
            }
            check(represented + summary.intraCommunityEdges() + summary.unmappedEdges()
                == graph.getEdges().size(), "Lost original relationships");
            check(semantic + structural + admin == represented, "Incorrect category totals");
        }
    }

    @Test
    public void summariesDoNotDependOnInputOrder() {
        GraphDirectedCollection graph = testGraph();
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            ContextGraphCommunityParams.defaults());
        int coarsest = hierarchy.levels().size() - 1;
        var first = ContextGraphCommunityEdgeAggregation.compute(graph, hierarchy, coarsest);
        Collections.shuffle(graph.getNodes(), new Random(31));
        Collections.shuffle(graph.getEdges(), new Random(32));
        var next = ContextGraphCommunityEdgeAggregation.compute(graph, hierarchy, coarsest);
        check(first.connections().size() == next.connections().size(), "Different number of bundles");
        for (int i = 0; i < first.connections().size(); i++) {
            var a = first.connections().get(i);
            var b = next.connections().get(i);
            check(a.pair().equals(b.pair()), "Pair changed with ordering");
            check(a.typeCounts().equals(b.typeCounts()), "Types changed with ordering");
            check(a.originalCount() == b.originalCount(), "Count changed with ordering");
        }
    }

    @Test
    public void cancellationIsObserved() {
        GraphDirectedCollection graph = testGraph();
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            ContextGraphCommunityParams.defaults());
        Thread.currentThread().interrupt();
        try {
            boolean cancelled = false;
            try {
                ContextGraphCommunityEdgeAggregation.compute(graph, hierarchy, 0);
            } catch (java.util.concurrent.CancellationException expected) {
                cancelled = true;
            }
            check(cancelled, "Interrupted aggregation continued");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void aggregationDoesNotChangeProtectedClassicGeometry() {
        GraphDirectedCollection graph = testGraph();
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            ContextGraphCommunityParams.defaults());
        var settings = ContextGraphHyperbolicPackingParams.defaults();
        check(settings.placement() == ContextGraphHyperbolicPackingParams.Placement.STRUCTURAL_BACKBONE,
            "Classic must remain default");
        Map<String, double[]> before = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy,
            settings).layout().positions();
        ContextGraphCommunityEdgeAggregation.compute(graph, hierarchy, hierarchy.levels().size() - 1);
        Map<String, double[]> after = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy,
            settings).layout().positions();
        for (String id : before.keySet())
            check(java.util.Arrays.equals(before.get(id), after.get(id)),
                "Classic geometry was modified by edge aggregation");
    }

    private static GraphDirectedCollection testGraph() {
        GraphDirectedCollection graph = new GraphDirectedCollection();
        for (int i = 0; i < 36; i++) {
            GraphNode node = new GraphNode();
            node.setEntityID(String.format("N%02d", i));
            graph.getNodes().add(node);
        }
        for (int g = 0; g < 9; g++) {
            for (int i = 0; i < 4; i++) {
                for (int j = i + 1; j < 4; j++) {
                    add(graph, g * 4 + i, g * 4 + j, "USES");
                }
            }
            add(graph, g * 4, ((g + 1) % 9) * 4, "USES");
            add(graph, g * 4 + 1, ((g + 2) % 9) * 4, "HAS_SECTION");
            add(graph, g * 4 + 2, ((g + 3) % 9) * 4, "DATASET_SECTION");
        }
        return graph;
    }

    private static void add(GraphDirectedCollection graph, int a, int b, String type) {
        GraphRelationship relationship = new GraphRelationship();
        relationship.getLabels().add(type);
        GraphEdge edge = new GraphEdge();
        edge.setStartID(String.format("N%02d", a));
        edge.setEndID(String.format("N%02d", b));
        edge.setRelationship(relationship);
        graph.getEdges().add(edge);
    }

    private static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        var tests = new ContextGraphCommunityEdgeAggregationTest();
        tests.countsTypesAndOriginalIdentityAreConservedAtEveryLevel();
        tests.summariesDoNotDependOnInputOrder();
        tests.cancellationIsObserved();
        tests.aggregationDoesNotChangeProtectedClassicGeometry();
        System.out.println("ContextGraphCommunityEdgeAggregationTest: 4 checks PASSED");
    }
}
