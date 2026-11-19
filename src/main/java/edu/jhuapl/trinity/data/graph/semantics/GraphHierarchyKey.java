package edu.jhuapl.trinity.data.graph.semantics;

import java.util.Comparator;

/**
 * Each predicate and hierarchy namespace is indexed independently. A null
 * namespace means explicitly unscoped; it is not a synthetic global taxonomy.
 */
public record GraphHierarchyKey(String hierarchyScopeId, String predicateId)
        implements Comparable<GraphHierarchyKey> {
    public GraphHierarchyKey {
        if (predicateId == null || predicateId.isBlank()) {
            throw new IllegalArgumentException("predicateId must be nonblank");
        }
        if (hierarchyScopeId != null && hierarchyScopeId.isBlank()) {
            throw new IllegalArgumentException("hierarchyScopeId must be null or nonblank");
        }
    }

    @Override
    public int compareTo(GraphHierarchyKey other) {
        int byScope = Comparator.nullsFirst(String::compareTo)
            .compare(hierarchyScopeId, other.hierarchyScopeId);
        return byScope != 0 ? byScope : predicateId.compareTo(other.predicateId);
    }
}
