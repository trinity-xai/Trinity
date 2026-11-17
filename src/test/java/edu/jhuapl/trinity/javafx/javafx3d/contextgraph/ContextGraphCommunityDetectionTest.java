package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Pure topology tests; no JavaFX initialization or external graph services. */
public class ContextGraphCommunityDetectionTest {
    private static ContextGraphCommunityParams settings(double resolution, int maximumLevels, int target) {
        return new ContextGraphCommunityParams(ContextGraphCommunityParams.Algorithm.LOUVAIN,
            resolution, maximumLevels, target, 0.08, 0.65, 1.0);
    }

    @Test
    public void nestedPartitionsAreValidAndStableUnderInputShuffling() {
        GraphDirectedCollection graph = ringOfCliques(64, 3);
        ContextGraphCommunityHierarchy first = ContextGraphCommunityDetection.detect(graph,
            settings(1.0, 0, 0));
        System.out.println("Controlled multilevel graph counts="
            + first.levels().stream().map(ContextGraphCommunityLevel::count).toList());
        check(first.levels().size() >= 2, "Expected multiple coarsening levels");
        check(first.levels().get(0).count() < graph.getNodes().size(), "No initial merging");
        for (int level = 0; level < first.levels().size(); level++) {
            ContextGraphCommunityLevel current = first.levels().get(level);
            if (level > 0) check(current.modularity() > first.levels().get(level - 1).modularity(),
                "Modularity did not improve after coarsening");
            int sum = current.clusters().stream().mapToInt(ContextGraphCommunityCluster::memberCount).sum();
            check(sum == graph.getNodes().size(), "Community coverage mismatch");
            if (level > 0) {
                check(current.count() < first.levels().get(level - 1).count(), "No coarsening");
                for (int i = 0; i < first.entityIds().size(); i++) {
                    int child = first.levels().get(level - 1).communityOf(i);
                    int parent = current.communityOf(i);
                    check(first.levels().get(level - 1).clusters().get(child).parentId() == parent,
                        "Inconsistent parent membership");
                    check(current.clusters().get(parent).childIds().contains(child),
                        "Missing reverse child membership");
                }
            }
        }
        Collections.shuffle(graph.getNodes(), new Random(17));
        Collections.shuffle(graph.getEdges(), new Random(29));
        ContextGraphCommunityHierarchy shuffled = ContextGraphCommunityDetection.detect(graph,
            settings(1.0, 0, 0));
        check(first.entityIds().equals(shuffled.entityIds()), "Non-deterministic entity index");
        check(first.levels().size() == shuffled.levels().size(), "Non-deterministic level count");
        for (int i = 0; i < first.levels().size(); i++) {
            check(java.util.Arrays.equals(first.levels().get(i).membership(),
                shuffled.levels().get(i).membership()), "Non-deterministic memberships");
        }
    }

    @Test
    public void disconnectedAndIsolatedNodesRemainFullyCovered() {
        GraphDirectedCollection graph = new GraphDirectedCollection();
        for (int i = 0; i < 7; i++) addNode(graph, "N" + i, null);
        addEdge(graph, "N0", "N1", "SEMANTIC");
        addEdge(graph, "N2", "N3", "SEMANTIC");
        addEdge(graph, "N3", "N4", "SEMANTIC");
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            settings(1.0, 0, 0));
        check(hierarchy.components() == 4, "Wrong component count");
        for (ContextGraphCommunityLevel level : hierarchy.levels()) {
            check(level.clusters().stream().mapToInt(ContextGraphCommunityCluster::memberCount).sum() == 7,
                "Lost original graph nodes");
            int[] m = level.membership();
            check(m[5] != m[6], "Separate isolated components incorrectly merged");
        }
    }

    @Test
    public void resolutionMaximumLevelsAndSizePreferenceHaveMeaningfulEffects() {
        GraphDirectedCollection graph = ringOfCliques(64, 3);
        ContextGraphCommunityHierarchy low = ContextGraphCommunityDetection.detect(graph,
            settings(0.35, 0, 0));
        ContextGraphCommunityHierarchy high = ContextGraphCommunityDetection.detect(graph,
            settings(2.5, 0, 0));
        check(low.levels().get(low.selectedLayoutLevel()).count()
            <= high.levels().get(high.selectedLayoutLevel()).count(),
            "Higher gamma should not merge more on this controlled graph");
        ContextGraphCommunityHierarchy capped = ContextGraphCommunityDetection.detect(graph,
            settings(1.0, 2, 0));
        check(capped.levels().size() <= 2, "Maximum levels ignored");
        ContextGraphCommunityHierarchy preferred = ContextGraphCommunityDetection.detect(graph,
            settings(1.0, 0, 100));
        check(preferred.levels().size() >= 2, "The size-preference test needs nested partitions");
        check(preferred.selectedLayoutLevel() == preferred.levels().size() - 1,
            "Largest target should prefer the coarsest available partition");
    }

    @Test
    public void legacyComparisonAndSourceAttributionDoNotModifyGraph() {
        GraphDirectedCollection graph = ringOfCliques(6, 5);
        for (int i = 0; i < graph.getNodes().size(); i++)
            graph.getNodes().get(i).getProperties().put("dataset", i % 3 == 0 ? "A" : "B");
        int edgesBefore = graph.getEdges().size();
        ContextGraphCommunityParams legacy = new ContextGraphCommunityParams(
            ContextGraphCommunityParams.Algorithm.LEGACY, 1.0, 0, 0, 0.08, 0.65, 1.0);
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph, legacy);
        check(hierarchy.levels().size() == 1, "Legacy must return one level");
        check(hierarchy.levels().get(0).sourceAttributedNodes() == graph.getNodes().size(),
            "Incorrect source attribution coverage");
        check(hierarchy.levels().get(0).crossSourceClusters() > 0,
            "Expected cross-source communities");
        check(graph.getEdges().size() == edgesBefore, "Graph edges were modified");
        check(graph.getNodes().get(0).getProperties().get("dataset") != null,
            "Graph properties were modified");
    }

    @Test
    public void computedLayoutPreservesActualBackboneEdgesAndBallValidity() {
        GraphDirectedCollection graph = ringOfCliques(8, 5);
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            settings(1.0, 0, 0));
        ContextGraphHyperbolicLayout.LayoutResult layout =
            ContextGraphHyperbolicLayout.computedLayout(graph, hierarchy);
        check(layout.positions().size() == graph.getNodes().size(), "Missing layout positions");
        check(layout.backboneEdges().size() == graph.getNodes().size() - 1,
            "Incorrect spanning backbone size");
        Set<GraphEdge> originals = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        originals.addAll(graph.getEdges());
        for (GraphEdge edge : layout.backboneEdges())
            check(originals.contains(edge), "Non-original backbone edge");
        for (double[] point : layout.positions().values()) {
            double r2 = point[0] * point[0] + point[1] * point[1] + point[2] * point[2];
            check(Double.isFinite(r2) && r2 < 1.0, "Invalid Poincare coordinate");
        }
    }

    @Test
    public void relationshipWeightsAndCancellationAreApplied() {
        GraphDirectedCollection graph = new GraphDirectedCollection();
        addNode(graph, "A", null);
        addNode(graph, "B", null);
        addNode(graph, "C", null);
        addEdge(graph, "A", "B", "DATASET_SECTION");
        addEdge(graph, "B", "C", "USES");
        ContextGraphCommunityHierarchy ordinary = ContextGraphCommunityDetection.detect(graph,
            settings(1.0, 0, 0));
        check(Math.abs(ordinary.totalEdgeWeight() - 1.08) < 1e-8,
            "Administrative link weight was not downweighted");
        ContextGraphCommunityParams altered = new ContextGraphCommunityParams(
            ContextGraphCommunityParams.Algorithm.LOUVAIN, 1.0, 0, 0, 0.5, 0.65, 2.0);
        ContextGraphCommunityHierarchy heavier = ContextGraphCommunityDetection.detect(graph, altered);
        check(Math.abs(heavier.totalEdgeWeight() - 2.5) < 1e-8,
            "User-configured relationship weights were ignored");
        Thread.currentThread().interrupt();
        try {
            boolean cancelled = false;
            try {
                ContextGraphCommunityDetection.detect(graph, settings(1.0, 0, 0));
            } catch (java.util.concurrent.CancellationException expected) {
                cancelled = true;
            }
            check(cancelled, "Interrupted worker did not stop");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void partitionComparisonDetectsMatchingAndDifferentGroupings() {
        int[] a = {0, 0, 1, 1, 2, 2};
        int[] sameRelabeled = {9, 9, 7, 7, 4, 4};
        int[] different = {0, 1, 0, 1, 0, 1};
        check(Math.abs(ContextGraphCommunityPartitionComparison.adjustedRandIndex(a, sameRelabeled)
            - 1.0) < 1e-12, "ARI should recognize identical partitions up to label renaming");
        check(ContextGraphCommunityPartitionComparison.adjustedRandIndex(a, different) < 0.5,
            "ARI did not detect distinct partition membership");
    }

    @Test
    public void semanticPackingRespondsToAngularAndRadialControls() {
        GraphDirectedCollection graph = ringOfCliques(64, 3);
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            settings(1.0, 0, 0));
        ContextGraphHyperbolicPackingParams defaults = ContextGraphHyperbolicPackingParams.defaults();
        ContextGraphHyperbolicPackingParams tight = new ContextGraphHyperbolicPackingParams(
            ContextGraphHyperbolicPackingParams.Placement.SEMANTIC_HIERARCHY, defaults.allocation(), 20.0, 0.0, 0.0,
            ContextGraphHyperbolicPackingParams.RadialProfile.UNIFORM, 0.20, List.of());
        ContextGraphHyperbolicPackingParams expanded = new ContextGraphHyperbolicPackingParams(
            ContextGraphHyperbolicPackingParams.Placement.SEMANTIC_HIERARCHY, defaults.allocation(), 150.0, 25.0, 1.0,
            ContextGraphHyperbolicPackingParams.RadialProfile.UNIFORM, 1.60, List.of());
        var a = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, tight);
        var b = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, expanded);
        check(a.layout().positions().size() == graph.getNodes().size(), "Packing lost entities");
        check(b.diagnostics().maxChartRadius() > a.diagnostics().maxChartRadius() + 0.10,
            "Radial spacing did not change chart radius significantly");
        check(meanDisplacement(a.layout().positions(), b.layout().positions()) > 0.10,
            "Extreme angular/radial controls did not move entities");
        check(b.diagnostics().nearBoundaryCount() == 0,
            "Large step must not force points onto the Poincare boundary");
        check(b.diagnostics().packedLayers() == hierarchy.levels().size() + 1,
            "The hierarchy levels are not participating in spatial packing");
        System.out.printf("[PACKING] tight maxRadius=%.5f expanded=%.5f mean displacement=%.5f%n",
            a.diagnostics().maxChartRadius(), b.diagnostics().maxChartRadius(),
            meanDisplacement(a.layout().positions(), b.layout().positions()));
    }

    @Test
    public void semanticPackingRespondsToAngularOnlyAndCustomDepthProfiles() {
        GraphDirectedCollection graph = ringOfCliques(64, 3);
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            settings(1.0, 0, 0));
        ContextGraphHyperbolicPackingParams defaults = ContextGraphHyperbolicPackingParams.defaults();
        var wide = new ContextGraphHyperbolicPackingParams(
            ContextGraphHyperbolicPackingParams.Placement.SEMANTIC_HIERARCHY,
            defaults.allocation(), 150.0, 25.0, 1.0, defaults.radialProfile(),
            defaults.radialStep(), List.of());
        var narrow = new ContextGraphHyperbolicPackingParams(
            ContextGraphHyperbolicPackingParams.Placement.SEMANTIC_HIERARCHY,
            defaults.allocation(), 20.0, 0.0, 0.0, defaults.radialProfile(),
            defaults.radialStep(), List.of());
        var fixed = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, wide);
        var changed = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, narrow);
        check(meanDisplacement(fixed.layout().positions(), changed.layout().positions()) > 0.01,
            "Angular spread should independently affect geometry");
        var custom = new ContextGraphHyperbolicPackingParams(
            ContextGraphHyperbolicPackingParams.Placement.SEMANTIC_HIERARCHY,
            defaults.allocation(), wide.angularSpreadDegrees(), wide.siblingGapDegrees(),
            wide.massInfluence(), ContextGraphHyperbolicPackingParams.RadialProfile.CUSTOM,
            wide.radialStep(), List.of(0.25, 0.40, 0.50, 0.35));
        var customized = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, custom);
        check(meanDisplacement(fixed.layout().positions(), customized.layout().positions()) > 0.02,
            "Custom per-layer radial steps should independently affect geometry");
    }

    @Test
    public void semanticPackingIsDeterministicAndKeepsActualBackboneEdges() {
        GraphDirectedCollection graph = ringOfCliques(48, 4);
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            settings(1.0, 0, 0));
        var defaults = ContextGraphHyperbolicPackingParams.defaults();
        var params = new ContextGraphHyperbolicPackingParams(
            ContextGraphHyperbolicPackingParams.Placement.SEMANTIC_HIERARCHY,
            defaults.allocation(), defaults.angularSpreadDegrees(), defaults.siblingGapDegrees(),
            defaults.massInfluence(), defaults.radialProfile(), defaults.radialStep(), List.of());
        var initial = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, params);
        Set<GraphEdge> originals = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        originals.addAll(graph.getEdges());
        for (GraphEdge edge : initial.layout().backboneEdges())
            check(originals.contains(edge), "Packed backbone manufactured a graph edge");
        Collections.shuffle(graph.getNodes(), new Random(13));
        Collections.shuffle(graph.getEdges(), new Random(17));
        var shuffled = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, params);
        check(meanDisplacement(initial.layout().positions(), shuffled.layout().positions()) < 1e-11,
            "Semantic packing depends on input list ordering");
        var structural = new ContextGraphHyperbolicPackingParams(
            ContextGraphHyperbolicPackingParams.Placement.STRUCTURAL_BACKBONE,
            params.allocation(), params.angularSpreadDegrees(), params.siblingGapDegrees(),
            params.massInfluence(), params.radialProfile(), params.radialStep(), List.of());
        var old = ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, structural);
        check(old.layout().backboneEdges().size() == shuffled.layout().backboneEdges().size(),
            "Structural comparison lost original spanning edges");
        check(meanDisplacement(old.layout().positions(), shuffled.layout().positions()) > 0.01,
            "Semantic and structural layouts did not differ");
    }

    @Test
    public void classic4BAIsDefaultAndIndependentOfMultilevelSettings() {
        GraphDirectedCollection graph = ringOfCliques(24, 4);
        check(ContextGraphHyperbolicPackingParams.defaults().placement()
            == ContextGraphHyperbolicPackingParams.Placement.STRUCTURAL_BACKBONE,
            "4B-A classic must be the default Poincare layout");
        var original = ContextGraphHyperbolicLayout.computedLayout(graph);
        var fine = ContextGraphCommunityDetection.detect(graph, settings(2.5, 2, 0));
        var coarse = ContextGraphCommunityDetection.detect(graph, settings(0.35, 0, 100));
        var first = ContextGraphCommunitySpatialLayout.compute(graph, fine,
            ContextGraphHyperbolicPackingParams.defaults()).layout();
        var second = ContextGraphCommunitySpatialLayout.compute(graph, coarse,
            ContextGraphHyperbolicPackingParams.defaults()).layout();
        check(first.positions().size() == graph.getNodes().size(),
            "Classic layout lost entities");
        for (String id : original.positions().keySet()) {
            check(java.util.Arrays.equals(original.positions().get(id), first.positions().get(id)),
                "Classic default does not reproduce 4B-A positions for " + id);
            check(java.util.Arrays.equals(first.positions().get(id), second.positions().get(id)),
                "Multilevel tuning altered protected 4B-A positions for " + id);
        }
        check(original.communities().equals(first.communities()),
            "Original single-level community memberships not preserved");
        check(original.communityCount() == first.communityCount(),
            "Original single-level community count not preserved");
        check(original.backboneEdges().equals(first.backboneEdges()),
            "Original source-backed spanning edges not preserved");
    }

    private static double meanDisplacement(Map<String, double[]> a, Map<String, double[]> b) {
        double total = 0;
        for (String id : a.keySet()) {
            double[] x = a.get(id), y = b.get(id);
            double dx = x[0] - y[0], dy = x[1] - y[1], dz = x[2] - y[2];
            total += Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        return total / Math.max(1, a.size());
    }

    private static GraphDirectedCollection ringOfCliques(int cliques, int size) {
        GraphDirectedCollection graph = new GraphDirectedCollection();
        for (int c = 0; c < cliques; c++) {
            for (int v = 0; v < size; v++) addNode(graph, String.format("%04d", c * size + v), null);
        }
        for (int c = 0; c < cliques; c++) {
            for (int i = 0; i < size; i++) {
                for (int j = i + 1; j < size; j++)
                    addEdge(graph, String.format("%04d", c * size + i),
                        String.format("%04d", c * size + j), "USES");
            }
            addEdge(graph, String.format("%04d", c * size),
                String.format("%04d", ((c + 1) % cliques) * size), "USES");
        }
        return graph;
    }

    private static void addNode(GraphDirectedCollection graph, String id, String source) {
        GraphNode node = new GraphNode();
        node.setEntityID(id);
        if (source != null) node.getProperties().put("dataset", source);
        graph.getNodes().add(node);
    }

    private static void addEdge(GraphDirectedCollection graph, String from, String to, String label) {
        GraphEdge edge = new GraphEdge();
        edge.setStartID(from);
        edge.setEndID(to);
        GraphRelationship relationship = new GraphRelationship();
        relationship.getLabels().add(label);
        edge.setRelationship(relationship);
        graph.getEdges().add(edge);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        ContextGraphCommunityDetectionTest tests = new ContextGraphCommunityDetectionTest();
        long started = System.nanoTime();
        tests.nestedPartitionsAreValidAndStableUnderInputShuffling();
        tests.disconnectedAndIsolatedNodesRemainFullyCovered();
        tests.resolutionMaximumLevelsAndSizePreferenceHaveMeaningfulEffects();
        tests.legacyComparisonAndSourceAttributionDoNotModifyGraph();
        tests.computedLayoutPreservesActualBackboneEdgesAndBallValidity();
        tests.relationshipWeightsAndCancellationAreApplied();
        tests.partitionComparisonDetectsMatchingAndDifferentGroupings();
        tests.semanticPackingRespondsToAngularAndRadialControls();
        tests.semanticPackingRespondsToAngularOnlyAndCustomDepthProfiles();
        tests.semanticPackingIsDeterministicAndKeepsActualBackboneEdges();
        tests.classic4BAIsDefaultAndIndependentOfMultilevelSettings();
        System.out.printf("ContextGraphCommunityDetectionTest: 11 checks PASSED (%.2f ms)%n",
            (System.nanoTime() - started) / 1_000_000.0);
    }
}
