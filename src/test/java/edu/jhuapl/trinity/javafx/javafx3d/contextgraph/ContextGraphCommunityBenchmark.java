package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import java.util.ArrayList;
import java.util.Locale;

/** Optional executable 10,000-node/50,000-edge analysis benchmark; not part of UI loading. */
public final class ContextGraphCommunityBenchmark {
    private ContextGraphCommunityBenchmark() { }

    public static void main(String[] args) {
        int groups = 500, groupSize = 20;
        GraphDirectedCollection graph = new GraphDirectedCollection();
        for (int i = 0; i < groups * groupSize; i++) {
            GraphNode node = new GraphNode();
            node.setEntityID(String.format(Locale.ROOT, "%05d", i));
            graph.getNodes().add(node);
        }
        GraphRelationship semantic = new GraphRelationship();
        semantic.getLabels().add("ASSOCIATED_WITH");
        for (int group = 0; group < groups; group++) {
            for (int i = 0; i < groupSize; i++) {
                int first = group * groupSize + i;
                for (int offset = 1; offset <= 4; offset++) {
                    addEdge(graph, first, group * groupSize + (i + offset) % groupSize, semantic);
                }
                addEdge(graph, first, ((group + 1) % groups) * groupSize + i, semantic);
            }
        }
        int nodes = graph.getNodes().size(), edges = graph.getEdges().size();
        if (nodes != 10_000 || edges != 50_000) throw new AssertionError("Wrong benchmark size");
        long start = System.nanoTime();
        ContextGraphCommunityHierarchy hierarchy = ContextGraphCommunityDetection.detect(graph,
            ContextGraphCommunityParams.defaults());
        long analysisTime = System.nanoTime() - start;
        System.out.println(hierarchy.summary());
        System.out.printf(Locale.ROOT, "Synthetic community detection %.1f ms%n",
            analysisTime / 1e6);
        for (ContextGraphCommunityLevel level : hierarchy.levels()) {
            if (level.clusters().stream().mapToInt(ContextGraphCommunityCluster::memberCount).sum() != nodes)
                throw new AssertionError("Lost nodes at level " + level.index());
        }
        long catalogStart = System.nanoTime();
        ContextGraphCommunityCatalog catalog = ContextGraphCommunityCatalog.build(graph, hierarchy);
        for (int levelIndex = 0; levelIndex < hierarchy.levels().size(); levelIndex++) {
            int total = catalog.atLevel(levelIndex).stream()
                .mapToInt(ContextGraphCommunityDescriptor::memberCount).sum();
            if (total != nodes) throw new AssertionError("Catalog lost source members");
        }
        System.out.printf(Locale.ROOT, "Community representative catalog %.1f ms, "
            + "coarsest representatives=%d%n", (System.nanoTime() - catalogStart) / 1e6,
            catalog.atLevel(hierarchy.levels().size() - 1).size());
        long layoutStart = System.nanoTime();
        ContextGraphHyperbolicLayout.LayoutResult layout =
            ContextGraphHyperbolicLayout.computedLayout(graph, hierarchy);
        System.out.printf(Locale.ROOT, "Synthetic hyperbolic layout %.1f ms, points=%d, backbone=%d%n",
            (System.nanoTime() - layoutStart) / 1e6,
            layout.positions().size(), layout.backboneEdges().size());
        if (layout.positions().size() != nodes || layout.backboneEdges().size() != nodes - 1)
            throw new AssertionError("Invalid topology layout");
        long packingStart = System.nanoTime();
        ContextGraphCommunitySpatialLayout.Result packing =
            ContextGraphCommunitySpatialLayout.compute(graph, hierarchy,
                ContextGraphHyperbolicPackingParams.defaults());
        System.out.printf(Locale.ROOT, "Semantic packed layout %.1f ms%n",
            (System.nanoTime() - packingStart) / 1e6);
        System.out.println(packing.diagnostics().summary());
        if (packing.layout().positions().size() != nodes
            || packing.layout().backboneEdges().size() != nodes - 1
            || packing.diagnostics().nearBoundaryCount() != 0)
            throw new AssertionError("Invalid semantic packing");
        for (int level = 0; level < hierarchy.levels().size(); level++) {
            long aggregateStart = System.nanoTime();
            ContextGraphCommunityEdgeAggregation aggregation =
                ContextGraphCommunityEdgeAggregation.compute(graph, hierarchy, level);
            long visibleEdges = aggregation.connections().stream()
                .mapToLong(ContextGraphCommunityEdgeBundle::originalCount).sum();
            if (visibleEdges + aggregation.intraCommunityEdges() + aggregation.unmappedEdges() != edges)
                throw new AssertionError("Aggregation lost original graph edges");
            System.out.printf(Locale.ROOT,
                "Community level %d: %d derived connections representing %d original edges; "
                    + "intra=%d; aggregate %.1f ms%n",
                level + 1, aggregation.connections().size(), visibleEdges,
                aggregation.intraCommunityEdges(), (System.nanoTime() - aggregateStart) / 1e6);
        }
    }

    private static void addEdge(GraphDirectedCollection graph, int first, int second,
                                GraphRelationship relationship) {
        GraphEdge edge = new GraphEdge();
        edge.setStartID(String.format(Locale.ROOT, "%05d", first));
        edge.setEndID(String.format(Locale.ROOT, "%05d", second));
        edge.setRelationship(relationship);
        graph.getEdges().add(edge);
    }
}
