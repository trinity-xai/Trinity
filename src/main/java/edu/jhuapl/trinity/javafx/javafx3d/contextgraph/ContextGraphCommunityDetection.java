package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Entry point for producing immutable, typed semantic hierarchies from graph topology. */
public final class ContextGraphCommunityDetection {
    private ContextGraphCommunityDetection() { }

    public static ContextGraphCommunityHierarchy detect(GraphDirectedCollection graph,
                                                        ContextGraphCommunityParams params) {
        long started = System.nanoTime();
        boolean legacy = params.effectiveAlgorithm() == ContextGraphCommunityParams.Algorithm.LEGACY;
        ContextGraphWeightedTopology topology = ContextGraphWeightedTopology.from(graph,
            legacy ? ContextGraphCommunityParams.defaults() : params);
        List<int[]> partitions;
        if (legacy) {
            Map<String, Integer> indices = new HashMap<>();
            for (int i = 0; i < topology.entityIds.size(); i++)
                indices.put(topology.entityIds.get(i), i);
            ContextGraphSemanticCommunities.Result result = ContextGraphSemanticCommunities.detect(
                topology.nodes, graph.getEdges(), indices);
            int[] partition = result.membership().clone();
            ContextGraphLouvain.canonicalize(partition);
            partitions = partition.length == 0 ? List.of() : List.of(partition);
        } else {
            partitions = ContextGraphLouvain.run(topology.adjacency, params.resolution(),
                params.levelLimit());
        }
        List<ContextGraphCommunityLevel> levels = assemble(topology, partitions,
            legacy ? 1.0 : params.resolution());
        int layoutLevel = 0;
        if (params.targetCommunitySize() > 0) {
            double closest = Double.POSITIVE_INFINITY;
            for (int i = 0; i < levels.size(); i++) {
                double mean = (double) topology.nodes.size() / levels.get(i).count();
                double score = Math.abs(Math.log(mean / params.targetCommunitySize()));
                if (score < closest) {
                    closest = score;
                    layoutLevel = i;
                }
            }
        }
        return new ContextGraphCommunityHierarchy(params, topology.entityIds, levels,
            topology.inputEdgeCount, topology.semanticEdges, topology.structuralEdges,
            topology.administrativeEdges, topology.components, topology.totalEdgeWeight,
            layoutLevel, System.nanoTime() - started);
    }

    private static List<ContextGraphCommunityLevel> assemble(ContextGraphWeightedTopology topology,
                                                            List<int[]> partitions, double gamma) {
        List<ContextGraphCommunityLevel> result = new ArrayList<>();
        int n = topology.nodes.size();
        for (int levelIndex = 0; levelIndex < partitions.size(); levelIndex++) {
            if (Thread.currentThread().isInterrupted())
                throw new java.util.concurrent.CancellationException("Community analysis cancelled");
            int[] partition = partitions.get(levelIndex);
            int count = Arrays.stream(partition).max().orElse(-1) + 1;
            int[] populations = new int[count];
            int[] firstIndices = new int[count];
            Arrays.fill(firstIndices, -1);
            String[] representatives = new String[count];
            List<Set<String>> sources = new ArrayList<>(count);
            List<Set<Integer>> children = new ArrayList<>(count);
            for (int c = 0; c < count; c++) {
                sources.add(new HashSet<>());
                children.add(new HashSet<>());
            }
            int attributed = 0;
            for (int i = 0; i < n; i++) {
                int group = partition[i];
                populations[group]++;
                if (representatives[group] == null) {
                    representatives[group] = topology.entityIds.get(i);
                    firstIndices[group] = i;
                }
                if (topology.sourceKeys[i] != null) {
                    sources.get(group).add(topology.sourceKeys[i]);
                    attributed++;
                }
                if (levelIndex > 0) children.get(group).add(partitions.get(levelIndex - 1)[i]);
            }
            List<ContextGraphCommunityCluster> clusters = new ArrayList<>(count);
            int crossings = 0;
            for (int c = 0; c < count; c++) {
                if (sources.get(c).size() > 1) crossings++;
                int parent = levelIndex + 1 == partitions.size() ? -1
                    : partitions.get(levelIndex + 1)[firstIndices[c]];
                List<Integer> childIds = new ArrayList<>(children.get(c));
                childIds.sort(Integer::compareTo);
                clusters.add(new ContextGraphCommunityCluster(levelIndex, c, parent, childIds,
                    populations[c], representatives[c]));
            }
            result.add(new ContextGraphCommunityLevel(levelIndex, partition, clusters,
                ContextGraphLouvain.modularity(topology.adjacency, partition, gamma),
                crossings, attributed));
        }
        return result;
    }

}
