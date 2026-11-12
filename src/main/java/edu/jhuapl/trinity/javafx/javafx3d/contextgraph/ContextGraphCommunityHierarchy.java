package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/** Immutable topology-derived hierarchy. Community IDs are scoped to each level. */
public final class ContextGraphCommunityHierarchy {
    private final ContextGraphCommunityParams params;
    private final List<String> entityIds;
    private final List<ContextGraphCommunityLevel> levels;
    private final int sourceEdgeCount;
    private final int semanticEdges;
    private final int structuralEdges;
    private final int administrativeEdges;
    private final int components;
    private final double totalEdgeWeight;
    private final int layoutLevel;
    private final long elapsedNanos;

    ContextGraphCommunityHierarchy(ContextGraphCommunityParams params, List<String> entityIds,
                                   List<ContextGraphCommunityLevel> levels,
                                   int sourceEdgeCount, int semanticEdges, int structuralEdges,
                                   int administrativeEdges, int components, double totalEdgeWeight,
                                   int layoutLevel, long elapsedNanos) {
        this.params = params;
        this.entityIds = List.copyOf(entityIds);
        this.levels = List.copyOf(levels);
        this.sourceEdgeCount = sourceEdgeCount;
        this.semanticEdges = semanticEdges;
        this.structuralEdges = structuralEdges;
        this.administrativeEdges = administrativeEdges;
        this.components = components;
        this.totalEdgeWeight = totalEdgeWeight;
        this.layoutLevel = layoutLevel;
        this.elapsedNanos = elapsedNanos;
    }

    public ContextGraphCommunityParams params() { return params; }
    public List<String> entityIds() { return entityIds; }
    public List<ContextGraphCommunityLevel> levels() { return levels; }
    public int selectedLayoutLevel() { return layoutLevel; }
    public int components() { return components; }
    public int sourceEdgeCount() { return sourceEdgeCount; }
    public int semanticEdges() { return semanticEdges; }
    public int structuralEdges() { return structuralEdges; }
    public int administrativeEdges() { return administrativeEdges; }
    public double totalEdgeWeight() { return totalEdgeWeight; }
    public long elapsedNanos() { return elapsedNanos; }

    public Map<String, Integer> layoutMembership() {
        if (levels.isEmpty()) return Map.of();
        ContextGraphCommunityLevel level = levels.get(layoutLevel);
        Map<String, Integer> mapping = new LinkedHashMap<>();
        for (int i = 0; i < entityIds.size(); i++)
            mapping.put(entityIds.get(i), level.communityOf(i));
        return Map.copyOf(mapping);
    }

    public String summary() {
        if (levels.isEmpty()) return "No community hierarchy (empty graph)";
        StringBuilder out = new StringBuilder();
        out.append(params.effectiveAlgorithm()).append(" | ").append(entityIds.size())
            .append(" nodes, ").append(sourceEdgeCount).append(" edges, ")
            .append(components).append(" components\n");
        out.append("Weighted links: ").append(semanticEdges).append(" semantic, ")
            .append(structuralEdges).append(" structural, ")
            .append(administrativeEdges).append(" administrative; total weight ")
            .append(String.format(Locale.ROOT, "%.2f", totalEdgeWeight)).append("\n");
        out.append("Nested levels: ").append(levels.size()).append("; layout uses level ")
            .append(layoutLevel + 1).append("\n");
        for (ContextGraphCommunityLevel level : levels) {
            int min = Integer.MAX_VALUE, max = 0;
            for (ContextGraphCommunityCluster cluster : level.clusters()) {
                min = Math.min(min, cluster.memberCount());
                max = Math.max(max, cluster.memberCount());
            }
            out.append("Level ").append(level.index() + 1).append(": ")
                .append(level.count()).append(" groups [").append(min).append("-")
                .append(max).append("], mean ")
                .append(String.format(Locale.ROOT, "%.1f", (double) entityIds.size() / level.count()))
                .append(", coverage=")
                .append(level.clusters().stream().mapToInt(ContextGraphCommunityCluster::memberCount).sum())
                .append("/").append(entityIds.size())
                .append(", Q=").append(String.format(Locale.ROOT, "%.4f", level.modularity()));
            if (level.sourceAttributedNodes() > 0)
                out.append(", cross-source groups=").append(level.crossSourceClusters())
                    .append(" (attributed nodes: ").append(level.sourceAttributedNodes()).append(")");
            else out.append(", cross-source: unavailable (no source attribute)");
            out.append('\n');
        }
        return out.append(String.format(Locale.ROOT, "Detection %.1f ms", elapsedNanos / 1_000_000.0)).toString();
    }
}
