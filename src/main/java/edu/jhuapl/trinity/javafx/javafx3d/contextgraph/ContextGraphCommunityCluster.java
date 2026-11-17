package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.List;

/** A derived semantic group, not an entity or assertion in the source graph. */
public record ContextGraphCommunityCluster(int level, int id, int parentId,
                                           List<Integer> childIds, int memberCount,
                                           String representativeEntityId) {
    public ContextGraphCommunityCluster {
        childIds = List.copyOf(childIds);
    }
}
