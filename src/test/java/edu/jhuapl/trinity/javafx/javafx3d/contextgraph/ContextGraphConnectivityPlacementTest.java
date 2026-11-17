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

/** Geometry-only correctness and topology-awareness tests; no JavaFX toolkit required. */
public class ContextGraphConnectivityPlacementTest {
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    @Test
    public void connectivityReducesWeightedAngularDistance() {
        GraphDirectedCollection graph = ringOfCliques(64, 4);
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            ContextGraphCommunityParams.defaults());
        int top = hierarchy.levels().size() - 1;
        ContextGraphCommunityConnectivity connectivity = ContextGraphCommunityConnectivity.from(graph, hierarchy);
        List<ContextGraphCommunityCluster> coarse = new ArrayList<>(hierarchy.levels().get(top).clusters());
        coarse.sort(java.util.Comparator.comparingInt(ContextGraphCommunityCluster::memberCount)
            .reversed().thenComparingInt(ContextGraphCommunityCluster::id));
        List<Integer> groupIds = new ArrayList<>();
        double[][] initial = new double[coarse.size()][3];
        for (int i = 0; i < coarse.size(); i++) {
            groupIds.add(coarse.get(i).id());
            double y = 1 - 2.0 * (i + 0.5) / coarse.size();
            double r = Math.sqrt(1 - y * y);
            initial[i] = new double[]{Math.cos(GOLDEN_ANGLE * i) * r, y,
                Math.sin(GOLDEN_ANGLE * i) * r};
        }
        double[][] optimized = connectivity.arrange(top, groupIds, initial, null, Math.PI);
        double before = ContextGraphCommunityConnectivity.weightedAngularDistance(
            initial, groupIds, connectivity.linksAt(top));
        double after = ContextGraphCommunityConnectivity.weightedAngularDistance(
            optimized, groupIds, connectivity.linksAt(top));
        check(after < before * 0.93, "Connectivity optimization failed to shorten linked angles: "
            + before + " -> " + after);
        System.out.printf("[RELATIONSHIP] top groups=%d weighted mean link angle %.4f -> %.4f rad%n",
            coarse.size(), before, after);
    }

    @Test
    public void layoutIsDeterministicAndPreservesOriginalGraphEdges() {
        GraphDirectedCollection graph = ringOfCliques(48, 4);
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            ContextGraphCommunityParams.defaults());
        var settings = relationshipParams();
        var first = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, settings);
        Set<GraphEdge> originals = Collections.newSetFromMap(new IdentityHashMap<>());
        originals.addAll(graph.getEdges());
        for (GraphEdge edge : first.layout().backboneEdges())
            check(originals.contains(edge), "An invented backbone relationship was emitted");
        check(first.layout().positions().size() == graph.getNodes().size(), "Missing original entities");
        for (double[] p : first.layout().positions().values())
            check(ContextGraphHyperbolicMath.insideBall(p), "Invalid Poincare coordinate");
        Collections.shuffle(graph.getNodes(), new Random(123));
        Collections.shuffle(graph.getEdges(), new Random(456));
        var second = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, settings);
        for (Map.Entry<String, double[]> entry : first.layout().positions().entrySet()) {
            double[] next = second.layout().positions().get(entry.getKey());
            check(java.util.Arrays.equals(entry.getValue(), next),
                "Coordinate changed with input order for " + entry.getKey());
        }
    }

    @Test
    public void classicIsProtectedAndModeSwitchChangesOnlyExperimentalGeometry() {
        GraphDirectedCollection graph = ringOfCliques(32, 5);
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            ContextGraphCommunityParams.defaults());
        check(ContextGraphHyperbolicPackingParams.defaults().placement()
            == ContextGraphHyperbolicPackingParams.Placement.STRUCTURAL_BACKBONE,
            "Protected Classic must remain default");
        var classicBefore = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy,
            ContextGraphHyperbolicPackingParams.defaults()).layout();
        var aware = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, relationshipParams());
        var classicAfter = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy,
            ContextGraphHyperbolicPackingParams.defaults()).layout();
        int moved = 0;
        for (String id : classicBefore.positions().keySet()) {
            check(java.util.Arrays.equals(classicBefore.positions().get(id), classicAfter.positions().get(id)),
                "Classic changed after running connectivity placement");
            if (!java.util.Arrays.equals(classicBefore.positions().get(id), aware.layout().positions().get(id)))
                moved++;
        }
        check(moved > 0, "Relationship-aware geometry never changed positions");
        check(classicBefore.backboneEdges().equals(classicAfter.backboneEdges()),
            "Classic backed edge identities changed");
    }

    @Test
    public void cancelledComputationDoesNotContinue() {
        GraphDirectedCollection graph = ringOfCliques(16, 4);
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            ContextGraphCommunityParams.defaults());
        Thread.currentThread().interrupt();
        try {
            boolean caught = false;
            try {
                ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, relationshipParams());
            } catch (java.util.concurrent.CancellationException expected) {
                caught = true;
            }
            check(caught, "Interrupted placement continued despite cancellation");
        } finally {
            Thread.interrupted();
        }
    }

    private static ContextGraphHyperbolicPackingParams relationshipParams() {
        var d = ContextGraphHyperbolicPackingParams.defaults();
        return new ContextGraphHyperbolicPackingParams(
            ContextGraphHyperbolicPackingParams.Placement.RELATIONSHIP_AWARE, d.allocation(),
            d.angularSpreadDegrees(), d.siblingGapDegrees(), d.massInfluence(),
            d.radialProfile(), d.radialStep(), d.customSteps());
    }

    private static GraphDirectedCollection ringOfCliques(int count, int cliqueSize) {
        GraphDirectedCollection graph = new GraphDirectedCollection();
        GraphRelationship related = new GraphRelationship();
        related.getLabels().add("USES");
        for (int c = 0; c < count; c++) {
            for (int i = 0; i < cliqueSize; i++) {
                GraphNode node = new GraphNode();
                node.setEntityID(String.format("%05d", c * cliqueSize + i));
                graph.getNodes().add(node);
            }
        }
        for (int c = 0; c < count; c++) {
            for (int i = 0; i < cliqueSize; i++) {
                for (int j = i + 1; j < cliqueSize; j++)
                    addEdge(graph, c * cliqueSize + i, c * cliqueSize + j, related);
            }
            addEdge(graph, c * cliqueSize, ((c + 1) % count) * cliqueSize, related);
        }
        return graph;
    }

    private static void addEdge(GraphDirectedCollection graph, int a, int b,
                                GraphRelationship label) {
        GraphEdge edge = new GraphEdge();
        edge.setStartID(String.format("%05d", a));
        edge.setEndID(String.format("%05d", b));
        edge.setRelationship(label);
        graph.getEdges().add(edge);
    }

    private static void check(boolean okay, String message) {
        if (!okay) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        var test = new ContextGraphConnectivityPlacementTest();
        test.connectivityReducesWeightedAngularDistance();
        test.layoutIsDeterministicAndPreservesOriginalGraphEdges();
        test.classicIsProtectedAndModeSwitchChangesOnlyExperimentalGeometry();
        test.cancelledComputationDoesNotContinue();
        System.out.println("ContextGraphConnectivityPlacementTest: 4 checks PASSED");
    }
}
