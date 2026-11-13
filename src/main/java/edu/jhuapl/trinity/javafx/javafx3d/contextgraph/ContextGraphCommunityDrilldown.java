package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Chooses the next *visible* community subdivision. Louvain sometimes creates
 * consecutive one-child levels (for example 29 -> 32 coarse CTI communities).
 * Displaying those levels adds a second glyph at the same centroid and makes
 * the Expand button appear inert. Skip only such degenerate levels; the exact
 * intermediate hierarchy and original graph memberships remain unchanged.
 */
public final class ContextGraphCommunityDrilldown {
    private ContextGraphCommunityDrilldown() { }

    public static List<ContextGraphCommunityRef> visibleChildren(
        ContextGraphCommunityRef parent,
        Function<ContextGraphCommunityRef, ContextGraphCommunityDescriptor> descriptorLookup) {
        if (parent == null || parent.level() == 0) return List.of();
        ContextGraphCommunityRef current = parent;
        while (current.level() > 0) {
            ContextGraphCommunityDescriptor descriptor = descriptorLookup.apply(current);
            if (descriptor == null || descriptor.childIds().isEmpty()) return List.of();
            int childLevel = current.level() - 1;
            List<ContextGraphCommunityRef> children = new ArrayList<>(descriptor.childIds().size());
            for (int id : descriptor.childIds())
                children.add(new ContextGraphCommunityRef(childLevel, id));
            if (children.size() > 1 || childLevel == 0) return List.copyOf(children);
            current = children.get(0);
        }
        return List.of();
    }
}
