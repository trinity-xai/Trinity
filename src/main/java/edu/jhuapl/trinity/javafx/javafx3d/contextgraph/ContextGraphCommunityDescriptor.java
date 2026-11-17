package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.List;
import java.util.Map;

/** Source-derived descriptive summary of a virtual community; never a source assertion. */
public record ContextGraphCommunityDescriptor(ContextGraphCommunityRef ref,
                                               int parentId, List<Integer> childIds,
                                               int memberCount, String title,
                                               Map<String, Integer> entityTypes,
                                               List<String> exampleEntities) {
    public ContextGraphCommunityDescriptor {
        childIds = List.copyOf(childIds);
        entityTypes = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(entityTypes));
        exampleEntities = List.copyOf(exampleEntities);
    }

    public String details() {
        StringBuilder text = new StringBuilder();
        text.append("Derived semantic community (not a source entity)\n")
            .append("Hierarchy level: ").append(ref.level() + 1).append('\n')
            .append("Community: ").append(ref.id() + 1).append('\n')
            .append("Original entities: ").append(memberCount).append('\n')
            .append("Dominant source labels: ").append(entityTypes).append('\n')
            .append("Example entity names: ").append(exampleEntities).append('\n')
            .append("Child communities: ").append(childIds.size()).append('\n');
        if (parentId >= 0) text.append("Parent community: ").append(parentId + 1).append('\n');
        text.append("The title is a descriptive label-frequency heuristic, not an inferred topic.");
        return text.toString();
    }
}
