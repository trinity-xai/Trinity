package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.List;

/** One complete partition of the ORIGINAL graph, ordered finest to coarsest. */
public final class ContextGraphCommunityLevel {
    private final int index;
    private final int[] membership;
    private final List<ContextGraphCommunityCluster> clusters;
    private final double modularity;
    private final int crossSourceClusters;
    private final int sourceAttributedNodes;

    ContextGraphCommunityLevel(int index, int[] membership,
                               List<ContextGraphCommunityCluster> clusters,
                               double modularity, int crossSourceClusters,
                               int sourceAttributedNodes) {
        this.index = index;
        this.membership = membership.clone();
        this.clusters = List.copyOf(clusters);
        this.modularity = modularity;
        this.crossSourceClusters = crossSourceClusters;
        this.sourceAttributedNodes = sourceAttributedNodes;
    }

    public int index() { return index; }
    public int[] membership() { return membership.clone(); }
    public int communityOf(int sortedOriginalNodeIndex) { return membership[sortedOriginalNodeIndex]; }
    public List<ContextGraphCommunityCluster> clusters() { return clusters; }
    public int count() { return clusters.size(); }
    public double modularity() { return modularity; }
    public int crossSourceClusters() { return crossSourceClusters; }
    public int sourceAttributedNodes() { return sourceAttributedNodes; }
}
