package edu.jhuapl.trinity.data.graph.semantics;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;

/**
 * Builds an immutable hierarchy index from a GraphDirectedCollection snapshot.
 * Never changes the graph, relationship labels, provenance, or layout tree.
 * Each (scope, predicate) gets its own complete multi-parent adjacency index.
 *
 * By default, only explicitly typed edges participate. Callers may opt into
 * read-only interpretation of recognized legacy labels, without inventing
 * origin, evidence, hierarchy scope, or ontology membership for unknown labels.
 *
 * This is an analytical snapshot, NOT a live index. Validate first and rebuild
 * when source graph data changes. Building and traversing can run off the FX
 * application thread; callers may supply a cancellation flag or interrupt.
 */
public final class GraphHierarchyIndex {
    private final Map<GraphHierarchyKey, GraphHierarchy> hierarchies;

    private GraphHierarchyIndex(Map<GraphHierarchyKey, GraphHierarchy> hierarchies) {
        this.hierarchies = Collections.unmodifiableMap(new LinkedHashMap<>(hierarchies));
    }

    public static GraphHierarchyIndex build(GraphDirectedCollection graph) {
        return build(graph, false, () -> false);
    }

    public static GraphHierarchyIndex build(GraphDirectedCollection graph,
                                            boolean includeKnownLegacyLabels) {
        return build(graph, includeKnownLegacyLabels, () -> false);
    }

    public static GraphHierarchyIndex build(GraphDirectedCollection graph,
                                            boolean includeKnownLegacyLabels,
                                            BooleanSupplier cancelled) {
        if (graph == null) throw new IllegalArgumentException("graph is required");
        return build(graph, GraphSemanticCatalog.forGraph(graph),
            includeKnownLegacyLabels, cancelled);
    }

    // Package-private overload lets the validator reuse its already checked catalog.
    static GraphHierarchyIndex build(GraphDirectedCollection graph,
                                     GraphSemanticCatalog catalog,
                                     boolean includeKnownLegacyLabels,
                                     BooleanSupplier cancelled) {
        if (graph == null || catalog == null) {
            throw new IllegalArgumentException("graph and catalog are required");
        }
        GraphHierarchy.checkCancelled(cancelled);
        Set<String> nodeIds = new HashSet<>();
        if (graph.getNodes() != null) {
            for (GraphNode node : graph.getNodes()) {
                if (node != null && !blank(node.getEntityID())) nodeIds.add(node.getEntityID());
            }
        }
        Map<GraphHierarchyKey, MutableHierarchy> byKey = new TreeMap<>();
        List<GraphEdge> sourceEdges = graph.getEdges();
        int count = sourceEdges == null ? 0 : sourceEdges.size();
        for (int i = 0; i < count; i++) {
            if ((i & 255) == 0) GraphHierarchy.checkCancelled(cancelled);
            GraphEdge edge = sourceEdges.get(i);
            if (edge == null || blank(edge.getStartID()) || blank(edge.getEndID())
                || !nodeIds.contains(edge.getStartID()) || !nodeIds.contains(edge.getEndID())) {
                continue; // Structural validator reports malformed endpoints.
            }
            GraphRelationship relationship = edge.getRelationship();
            if (relationship == null || (!includeKnownLegacyLabels
                && relationship.getSemantics() == null)) continue;
            GraphSemanticCatalog.Resolution resolution = catalog.resolve(relationship);
            GraphPredicateDefinition definition = resolution.definition();
            if (definition == null || !Boolean.TRUE.equals(definition.getHierarchy())) continue;
            GraphHierarchyOrientation orientation = definition.getHierarchyOrientation();
            if (orientation == null) {
                throw new IllegalArgumentException("Hierarchy predicate "
                    + definition.getPredicateId() + " needs an explicit hierarchyOrientation");
            }
            GraphRelationshipSemantics semantics = relationship.getSemantics();
            String scopeId = semantics == null ? null : semantics.getHierarchyScopeId();
            if (scopeId != null && scopeId.isBlank()) {
                throw new IllegalArgumentException("Blank hierarchyScopeId on edge " + i);
            }
            String child = orientation == GraphHierarchyOrientation.SUBJECT_IS_CHILD
                ? edge.getStartID() : edge.getEndID();
            String parent = orientation == GraphHierarchyOrientation.SUBJECT_IS_CHILD
                ? edge.getEndID() : edge.getStartID();
            GraphHierarchyKey key = new GraphHierarchyKey(scopeId, definition.getPredicateId());
            GraphHierarchyLink link = new GraphHierarchyLink(edge.getEdgeId(), i,
                edge.getStartID(), edge.getEndID(), child, parent, resolution.fromLegacyLabels(), resolution.origin(),
                semantics == null ? null : semantics.getEvidenceIds());
            byKey.computeIfAbsent(key, ignored -> new MutableHierarchy(definition)).add(link);
        }
        Map<GraphHierarchyKey, GraphHierarchy> result = new LinkedHashMap<>();
        for (var entry : byKey.entrySet()) {
            GraphHierarchy.checkCancelled(cancelled);
            result.put(entry.getKey(), entry.getValue().freeze(entry.getKey(), cancelled));
        }
        GraphHierarchy.checkCancelled(cancelled);
        return new GraphHierarchyIndex(result);
    }

    public Map<GraphHierarchyKey, GraphHierarchy> hierarchies() { return hierarchies; }
    public Set<GraphHierarchyKey> keys() { return hierarchies.keySet(); }
    public GraphHierarchy hierarchy(String hierarchyScopeId, String predicateId) {
        if (blank(predicateId) || (hierarchyScopeId != null && hierarchyScopeId.isBlank())) {
            return null;
        }
        return hierarchies.get(new GraphHierarchyKey(hierarchyScopeId, predicateId));
    }

    public List<GraphHierarchy> inScope(String hierarchyScopeId) {
        return hierarchies.entrySet().stream()
            .filter(entry -> java.util.Objects.equals(hierarchyScopeId,
                entry.getKey().hierarchyScopeId()))
            .map(Map.Entry::getValue).toList();
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private static final class MutableHierarchy {
        private static final Comparator<GraphHierarchyLink> LINK_ORDER =
            Comparator.comparing(GraphHierarchyLink::childId)
                .thenComparing(GraphHierarchyLink::parentId)
                .thenComparing(GraphHierarchyLink::edgeId,
                    Comparator.nullsLast(String::compareTo))
                .thenComparingInt(GraphHierarchyLink::edgeIndex);
        private final GraphPredicateDefinition definition;
        private final Map<String, Set<String>> parents = new HashMap<>();
        private final Map<String, Set<String>> children = new HashMap<>();
        private final List<GraphHierarchyLink> links = new ArrayList<>();

        private MutableHierarchy(GraphPredicateDefinition definition) {
            this.definition = definition;
        }

        void add(GraphHierarchyLink link) {
            parents.computeIfAbsent(link.childId(), ignored -> new TreeSet<>()).add(link.parentId());
            parents.computeIfAbsent(link.parentId(), ignored -> new TreeSet<>());
            children.computeIfAbsent(link.parentId(), ignored -> new TreeSet<>()).add(link.childId());
            children.computeIfAbsent(link.childId(), ignored -> new TreeSet<>());
            links.add(link);
        }

        GraphHierarchy freeze(GraphHierarchyKey key, BooleanSupplier cancelled) {
            GraphHierarchy.checkCancelled(cancelled);
            links.sort(LINK_ORDER);
            Map<String, List<String>> immutableParents = new TreeMap<>();
            Map<String, List<String>> immutableChildren = new TreeMap<>();
            for (var entry : parents.entrySet()) {
                immutableParents.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            for (var entry : children.entrySet()) {
                immutableChildren.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            Map<String, List<GraphHierarchyLink>> parentLinks = new HashMap<>();
            Map<String, List<GraphHierarchyLink>> childLinks = new HashMap<>();
            for (GraphHierarchyLink link : links) {
                parentLinks.computeIfAbsent(link.childId(), ignored -> new ArrayList<>()).add(link);
                childLinks.computeIfAbsent(link.parentId(), ignored -> new ArrayList<>()).add(link);
            }
            return new GraphHierarchy(key, definition, immutableParents,
                immutableChildren, parentLinks, childLinks, links, cancelled);
        }
    }
}
