package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;

/**
 * Derived, immutable undirected inter-community edge summaries. Every represented
 * relationship remains an ORIGINAL GraphEdge reference, never a new graph fact.
 * Intra-community edges are counted but not rendered as aggregate connections.
 */
public final class ContextGraphCommunityEdgeAggregation {
    private final int levelIndex;
    private final int communityCount;
    private final int intraCommunityEdges;
    private final int unmappedEdges;
    private final List<ContextGraphCommunityEdgeBundle> connections;

    private ContextGraphCommunityEdgeAggregation(int levelIndex, int communityCount,
                                                 int intraCommunityEdges, int unmappedEdges,
                                                 List<ContextGraphCommunityEdgeBundle> connections) {
        this.levelIndex = levelIndex;
        this.communityCount = communityCount;
        this.intraCommunityEdges = intraCommunityEdges;
        this.unmappedEdges = unmappedEdges;
        this.connections = List.copyOf(connections);
    }

    public int levelIndex() { return levelIndex; }
    public int communityCount() { return communityCount; }
    public int intraCommunityEdges() { return intraCommunityEdges; }
    public int unmappedEdges() { return unmappedEdges; }
    public List<ContextGraphCommunityEdgeBundle> connections() { return connections; }

    public static ContextGraphCommunityEdgeAggregation compute(GraphDirectedCollection graph,
                                                                ContextGraphCommunityHierarchy hierarchy,
                                                                int levelIndex) {
        if (graph == null || hierarchy == null || levelIndex < 0
            || levelIndex >= hierarchy.levels().size())
            throw new IllegalArgumentException("A graph and valid community level are required");
        ContextGraphCommunityLevel level = hierarchy.levels().get(levelIndex);
        int[] memberships = level.membership();
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < hierarchy.entityIds().size(); i++)
            index.put(hierarchy.entityIds().get(i), memberships[i]);
        Map<ContextGraphCommunityPair, List<GraphEdge>> grouped = new TreeMap<>();
        int intra = 0, unmapped = 0;
        for (GraphEdge edge : graph.getEdges()) {
            if (Thread.currentThread().isInterrupted())
                throw new CancellationException("Community edge aggregation cancelled");
            if (edge == null) continue;
            Integer from = index.get(edge.getStartID()), to = index.get(edge.getEndID());
            if (from == null || to == null) { unmapped++; continue; }
            if (from.equals(to)) { intra++; continue; }
            ContextGraphCommunityPair pair = new ContextGraphCommunityPair(Math.min(from, to), Math.max(from, to));
            grouped.computeIfAbsent(pair, ignored -> new ArrayList<>()).add(edge);
        }
        Comparator<GraphEdge> sourceOrder = Comparator.comparing(GraphEdge::getStartID)
            .thenComparing(GraphEdge::getEndID)
            .thenComparing(ContextGraphEdgeDisclosure::relationshipType)
            .thenComparingLong(edge -> edge.getRelationship() == null ? 0L
                : edge.getRelationship().getIdentity());
        List<ContextGraphCommunityEdgeBundle> connections = new ArrayList<>(grouped.size());
        for (Map.Entry<ContextGraphCommunityPair, List<GraphEdge>> entry : grouped.entrySet()) {
            if (Thread.currentThread().isInterrupted())
                throw new CancellationException("Community edge aggregation cancelled");
            List<GraphEdge> originals = entry.getValue();
            originals.sort(sourceOrder);
            Map<String, Integer> byType = new TreeMap<>();
            int semantic = 0, structural = 0, administrative = 0;
            for (GraphEdge edge : originals) {
                byType.merge(ContextGraphEdgeDisclosure.relationshipType(edge), 1, Integer::sum);
                int kind = ContextGraphWeightedTopology.relationshipType(edge.getRelationship());
                if (kind == 0) administrative++;
                else if (kind == 1) structural++;
                else semantic++;
            }
            connections.add(new ContextGraphCommunityEdgeBundle(entry.getKey(), originals,
                new LinkedHashMap<>(byType), semantic, structural, administrative));
        }
        return new ContextGraphCommunityEdgeAggregation(levelIndex, level.count(),
            intra, unmapped, connections);
    }
}
