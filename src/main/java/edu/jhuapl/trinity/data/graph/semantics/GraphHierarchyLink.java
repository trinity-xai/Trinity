package edu.jhuapl.trinity.data.graph.semantics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable reference to one original hierarchy assertion. Multiple links may
 * have identical endpoints; the edge index identifies a legacy edge without an
 * edgeId, but is deliberately not a persistent identity.
 */
public record GraphHierarchyLink(String edgeId, int edgeIndex,
                                 String sourceStartId, String sourceEndId,
                                 String childId, String parentId, boolean fromLegacyLabels,
                                 GraphAssertionOrigin origin, List<String> evidenceIds) {
    public GraphHierarchyLink {
        if (edgeIndex < 0 || childId == null || parentId == null
            || sourceStartId == null || sourceEndId == null) {
            throw new IllegalArgumentException("Invalid hierarchy link");
        }
        // Invalid/null references remain visible for semantic validation rather
        // than making index construction fail with List.copyOf NPE.
        evidenceIds = evidenceIds == null ? List.of()
            : Collections.unmodifiableList(new ArrayList<>(evidenceIds));
    }
}
