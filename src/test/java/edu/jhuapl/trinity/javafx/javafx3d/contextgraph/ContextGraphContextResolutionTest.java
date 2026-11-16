package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Source-only regression checks: full-node geometry, new placement methods, context history. */
public class ContextGraphContextResolutionTest {
    @Test
    public void exploreBackGlobalRetainsIdentityAndFiltersOnlySummaryBundles() {
        ContextGraphContextNavigation nav = new ContextGraphContextNavigation();
        var ab = new ContextGraphCommunityEdgeBundle(new ContextGraphCommunityPair(1, 2),
            List.of(), Map.of(), 0, 0, 0);
        var cd = new ContextGraphCommunityEdgeBundle(new ContextGraphCommunityPair(3, 4),
            List.of(), Map.of(), 0, 0, 0);
        check(nav.includes(ab, null), "Global must include all aggregate relationships");
        check(nav.explore("APT29", 3, 4) == 2, "Explore should move finer by one level");
        check(nav.anchorEntityId().equals("APT29"), "Anchor identity must remain original");
        check(nav.includes(ab, 1), "Focused bundle should remain visible");
        check(!nav.includes(cd, 1), "Unrelated bundle should be outside context");
        check(nav.explore("Technique X", 2, 4) == 1, "Second drill-down failed");
        var previous = nav.back();
        check(previous.level() == 2 && previous.anchorEntityId().equals("APT29"),
            "Back did not restore prior entity + resolution");
        var global = nav.back();
        check(global.level() == 3 && global.anchorEntityId() == null,
            "Back did not restore global context");
        check(nav.includes(cd, null), "Global restoration should expose all summaries");
        nav.explore("APT29", 3, 4);
        nav.reset();
        check(nav.anchorEntityId() == null && !nav.canGoBack(), "Global reset retained stale history");
    }

    @Test
    public void alternativePlacementsAreDeterministicAndPreserveBackbone() {
        GraphDirectedCollection graph = ringOfCliques(48, 4);
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            ContextGraphCommunityParams.defaults());
        var defaults = ContextGraphHyperbolicPackingParams.defaults();
        var classic = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, defaults).layout();
        Set<GraphEdge> original = Collections.newSetFromMap(new IdentityHashMap<>());
        original.addAll(graph.getEdges());
        for (var placement : List.of(ContextGraphHyperbolicPackingParams.Placement.SPECTRAL,
                                       ContextGraphHyperbolicPackingParams.Placement.GRAPH_DISTANCE_RADIAL)) {
            var params = new ContextGraphHyperbolicPackingParams(placement, defaults.allocation(),
                defaults.angularSpreadDegrees(), defaults.siblingGapDegrees(),
                defaults.massInfluence(), defaults.radialProfile(), defaults.radialStep(),
                defaults.customSteps());
            var first = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, params).layout();
            check(first.positions().size() == graph.getNodes().size(), "Lost source entities: " + placement);
            for (GraphEdge edge : first.backboneEdges())
                check(original.contains(edge), "Invented graph edge: " + placement);
            for (double[] coords : first.positions().values())
                check(ContextGraphHyperbolicMath.insideBall(coords), "Invalid hyperbolic position");
            Collections.shuffle(graph.getNodes(), new Random(781));
            Collections.shuffle(graph.getEdges(), new Random(882));
            var second = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, params).layout();
            int changedFromClassic = 0;
            for (String id : first.positions().keySet()) {
                check(Arrays.equals(first.positions().get(id), second.positions().get(id)),
                    "Non-deterministic result: " + placement + " / " + id);
                if (!Arrays.equals(first.positions().get(id), classic.positions().get(id)))
                    changedFromClassic++;
            }
            check(changedFromClassic > 0, "Alternative placement never changed geometry");
        }
        var restored = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, defaults).layout();
        for (String id : classic.positions().keySet())
            check(Arrays.equals(classic.positions().get(id), restored.positions().get(id)),
                "Classic projection changed");
    }

    private static GraphDirectedCollection ringOfCliques(int groups, int groupSize) {
        GraphDirectedCollection graph = new GraphDirectedCollection();
        GraphRelationship rel = new GraphRelationship();
        rel.getLabels().add("USES");
        for (int i = 0; i < groups * groupSize; i++) {
            GraphNode node = new GraphNode();
            node.setEntityID(String.format("%05d", i));
            graph.getNodes().add(node);
        }
        for (int c = 0; c < groups; c++) {
            for (int a = 0; a < groupSize; a++)
                for (int b = a + 1; b < groupSize; b++)
                    add(graph, c * groupSize + a, c * groupSize + b, rel);
            add(graph, c * groupSize, ((c + 1) % groups) * groupSize, rel);
        }
        return graph;
    }
    private static void add(GraphDirectedCollection graph, int a, int b, GraphRelationship rel) {
        GraphEdge edge = new GraphEdge();
        edge.setStartID(String.format("%05d", a));
        edge.setEndID(String.format("%05d", b));
        edge.setRelationship(rel);
        graph.getEdges().add(edge);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var tests = new ContextGraphContextResolutionTest();
        tests.exploreBackGlobalRetainsIdentityAndFiltersOnlySummaryBundles();
        tests.alternativePlacementsAreDeterministicAndPreserveBackbone();
        System.out.println("ContextGraphContextResolutionTest: 2 checks PASSED");
    }
}
