package edu.jhuapl.trinity.data.graph.semantics;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Explicit, read-only validation. File imports and legacy rendering are NOT
 * forced through this validator. Strict mode is for newly generated typed v2
 * graphs. Phase 2.3B additionally checks typed hierarchy orientation, scoped
 * cycles (only when acyclicity is required), and single-parent constraints.
 */
public final class GraphSemanticValidator {
    public enum Mode { LEGACY_PERMISSIVE, STRICT_TYPED }
    public enum Severity { ERROR, WARNING }
    public record Issue(Severity severity, String code, String location, String message) { }
    public record Result(List<Issue> issues) {
        public Result { issues = List.copyOf(issues); }
        public boolean isValid() { return issues.stream().noneMatch(i -> i.severity() == Severity.ERROR); }
        public long errorCount() { return issues.stream().filter(i -> i.severity() == Severity.ERROR).count(); }
    }

    private GraphSemanticValidator() { }

    public static Result validate(GraphDirectedCollection graph, Mode mode) {
        List<Issue> issues = new ArrayList<>();
        if (graph == null) {
            error(issues, "GRAPH_NULL", "graph", "Graph is null");
            return new Result(issues);
        }
        if (mode == null) throw new IllegalArgumentException("Validation mode required");
        boolean strict = mode == Mode.STRICT_TYPED;
        if (strict && (graph.getSchemaVersion() == null || graph.getSchemaVersion() < 2)) {
            error(issues, "SCHEMA_VERSION", "graph.schemaVersion",
                "Strict typed graphs require schemaVersion >= 2");
        }
        GraphSemanticCatalog catalog = new GraphSemanticCatalog();
        boolean catalogValidForIndex = true;
        if (graph.getPredicateDefinitions() != null) {
            int i = 0;
            for (GraphPredicateDefinition definition : graph.getPredicateDefinitions()) {
                try {
                    catalog.register(definition);
                } catch (IllegalArgumentException ex) {
                    catalogValidForIndex = false;
                    error(issues, "PREDICATE_DEFINITION", "graph.predicateDefinitions[" + i + "]", ex.getMessage());
                }
                if (definition != null && Boolean.TRUE.equals(definition.getHierarchy())) {
                    if (definition.getHierarchyOrientation() == null) {
                        if (strict) error(issues, "HIERARCHY_ORIENTATION",
                            "graph.predicateDefinitions[" + i + "]",
                            "Indexed hierarchy predicate needs an explicit hierarchyOrientation");
                        catalogValidForIndex = false;
                    }
                    if (Boolean.TRUE.equals(definition.getSymmetric()) && strict) {
                        error(issues, "SYMMETRIC_HIERARCHY", "graph.predicateDefinitions[" + i + "]",
                            "Hierarchies must declare a directional parent/child orientation");
                    }
                }
                i++;
            }
        }

        Map<String, GraphNode> nodes = new HashMap<>();
        if (graph.getNodes() == null) {
            error(issues, "NODES_MISSING", "graph.nodes", "Node list is missing");
        } else {
            for (int i = 0; i < graph.getNodes().size(); i++) {
                GraphNode node = graph.getNodes().get(i);
                String path = "nodes[" + i + "]";
                if (node == null || blank(node.getEntityID())) {
                    error(issues, "NODE_ID", path, "Node has no nonblank entityID");
                } else if (nodes.putIfAbsent(node.getEntityID(), node) != null) {
                    error(issues, "DUPLICATE_NODE_ID", path, "Repeated entityID " + node.getEntityID());
                }
            }
        }

        Set<String> evidenceIds = new HashSet<>();
        if (graph.getEvidenceRecords() != null) {
            for (int i = 0; i < graph.getEvidenceRecords().size(); i++) {
                GraphEvidence evidence = graph.getEvidenceRecords().get(i);
                String path = "evidenceRecords[" + i + "]";
                if (evidence == null || blank(evidence.getEvidenceId())) {
                    error(issues, "EVIDENCE_ID", path, "Evidence record needs evidenceId");
                    continue;
                }
                if (!evidenceIds.add(evidence.getEvidenceId())) {
                    error(issues, "DUPLICATE_EVIDENCE_ID", path,
                        "Repeated evidenceId " + evidence.getEvidenceId());
                }
                if (strict && (blank(evidence.getDocumentId())
                    || blank(evidence.getDocumentRevision()) || blank(evidence.getPassageId()))) {
                    error(issues, "EVIDENCE_SOURCE", path,
                        "Typed passage evidence requires documentId, documentRevision and passageId");
                }
                if (evidence.getPageNumber() != null && evidence.getPageNumber() < 1) {
                    error(issues, "EVIDENCE_PAGE", path, "pageNumber must be 1-based");
                }
                Long start = evidence.getStartOffset();
                Long end = evidence.getEndOffset();
                if ((start == null) != (end == null)) {
                    error(issues, "EVIDENCE_SPAN", path,
                        "startOffset and exclusive endOffset must both be supplied");
                } else if (start != null) {
                    if (start < 0 || end <= start) {
                        error(issues, "EVIDENCE_SPAN", path, "Offsets must form a nonempty half-open span");
                    }
                    if (blank(evidence.getSpanCoordinateSystem())) {
                        error(issues, "EVIDENCE_COORDINATES", path,
                            "A span requires an explicit spanCoordinateSystem");
                    }
                }
                if (evidence.getExtractionScore() != null
                    && !Double.isFinite(evidence.getExtractionScore())) {
                    error(issues, "EVIDENCE_SCORE", path, "extractionScore must be finite");
                }
            }
        }

        Set<String> edgeIds = new HashSet<>();
        if (graph.getEdges() == null) {
            error(issues, "EDGES_MISSING", "graph.edges", "Edge list is missing");
        } else {
            for (int i = 0; i < graph.getEdges().size(); i++) {
                GraphEdge edge = graph.getEdges().get(i);
                String path = "edges[" + i + "]";
                if (edge == null) {
                    error(issues, "EDGE_NULL", path, "Null edge");
                    continue;
                }
                if (blank(edge.getStartID()) || !nodes.containsKey(edge.getStartID())) {
                    error(issues, "EDGE_START", path, "Edge startID does not resolve to a node");
                }
                if (blank(edge.getEndID()) || !nodes.containsKey(edge.getEndID())) {
                    error(issues, "EDGE_END", path, "Edge endID does not resolve to a node");
                }
                if (blank(edge.getEdgeId())) {
                    if (strict) error(issues, "EDGE_ID", path, "Typed edge requires stable edgeId");
                } else if (!edgeIds.add(edge.getEdgeId())) {
                    error(issues, "DUPLICATE_EDGE_ID", path, "Repeated edgeId " + edge.getEdgeId());
                }
                GraphRelationship relationship = edge.getRelationship();
                if (relationship == null) {
                    if (strict) error(issues, "RELATIONSHIP_NULL", path,
                        "Typed edge requires relationship semantics");
                    continue;
                }
                GraphRelationshipSemantics semantics = relationship.getSemantics();
                if (!strict && semantics == null) continue;
                if (strict && semantics == null) {
                    error(issues, "SEMANTICS_MISSING", path,
                        "Typed edge requires a semantic envelope");
                    continue;
                }
                if (semantics != null) {
                    validateSemantics(semantics, catalog, nodes, edge, evidenceIds,
                        path, strict, issues);
                }
            }
        }
        if (strict && catalogValidForIndex && issues.stream()
            .noneMatch(i -> "HIERARCHY_SCOPE_ID".equals(i.code()))) {
            // Index only explicitly typed edges. Legacy labels are not inferred
            // as asserted knowledge during schema-v2 validation.
            GraphHierarchyIndex index = GraphHierarchyIndex.build(graph, catalog, false, () -> false);
            for (GraphHierarchy hierarchy : index.hierarchies().values()) {
                String location = "hierarchies[" + hierarchy.key().hierarchyScopeId()
                    + "," + hierarchy.key().predicateId() + "]";
                if (hierarchy.acyclicRequired()) {
                    for (List<String> cycle : hierarchy.cycleComponents()) {
                        error(issues, "HIERARCHY_CYCLE", location,
                            "Directed cycle in required acyclic hierarchy: " + cycle);
                    }
                }
                if (!hierarchy.multipleParentsAllowed()) {
                    for (var entry : hierarchy.parentsByNode().entrySet()) {
                        if (entry.getValue().size() > 1) {
                            error(issues, "HIERARCHY_MULTIPLE_PARENTS", location,
                                "Node " + entry.getKey() + " has multiple distinct parents: "
                                    + entry.getValue());
                        }
                    }
                }
            }
        }
        return new Result(issues);
    }

    private static void validateSemantics(GraphRelationshipSemantics semantics,
            GraphSemanticCatalog catalog, Map<String, GraphNode> nodes,
            GraphEdge edge, Set<String> evidenceIds, String path, boolean strict,
            List<Issue> issues) {
        String predicateId = semantics.getPredicateId();
        if (blank(predicateId)) {
            error(issues, "PREDICATE_ID", path, "Semantic envelope needs predicateId");
            return;
        }
        GraphPredicateDefinition definition = catalog.definition(predicateId);
        if (definition == null) {
            issue(issues, strict ? Severity.ERROR : Severity.WARNING, "UNKNOWN_PREDICATE", path,
                "Predicate " + predicateId + " has no catalog definition");
        } else {
            if (strict && !predicateId.equals(definition.getPredicateId())) {
                error(issues, "NONCANONICAL_PREDICATE", path,
                    "Use canonical predicateId " + definition.getPredicateId());
            }
            checkKind(definition.getAllowedSubjectNodeKinds(), nodes.get(edge.getStartID()),
                "subject", predicateId, path, issues);
            checkKind(definition.getAllowedObjectNodeKinds(), nodes.get(edge.getEndID()),
                "object", predicateId, path, issues);
        }
        if (strict && semantics.getHierarchyScopeId() != null
            && semantics.getHierarchyScopeId().isBlank()) {
            error(issues, "HIERARCHY_SCOPE_ID", path,
                "hierarchyScopeId must be null or nonblank");
        }
        if (strict && semantics.getOrigin() == null) {
            error(issues, "ORIGIN_MISSING", path, "Typed assertion needs an explicit origin");
        }
        boolean umapFuzzy = definition != null
            && "UMAP_FUZZY_ASSOCIATION".equals(definition.getPredicateId());
        if (umapFuzzy && semantics.getOrigin() != GraphAssertionOrigin.ALGORITHM_DERIVED) {
            error(issues, "UMAP_ORIGIN", path, "UMAP fuzzy links are algorithm-derived, not source assertions");
        }
        if (strict && semantics.getOrigin() == GraphAssertionOrigin.SOURCE_ASSERTED
            && (semantics.getEvidenceIds() == null || semantics.getEvidenceIds().isEmpty())) {
            error(issues, "SOURCE_EVIDENCE", path, "Source assertions require evidence references");
        }
        if (semantics.getEvidenceIds() != null) {
            Set<String> seen = new HashSet<>();
            for (String evidenceId : semantics.getEvidenceIds()) {
                if (blank(evidenceId) || !evidenceIds.contains(evidenceId)) {
                    issue(issues, strict ? Severity.ERROR : Severity.WARNING, "UNRESOLVED_EVIDENCE", path,
                        "Unresolved evidenceId: " + evidenceId);
                } else if (!seen.add(evidenceId)) {
                    issue(issues, strict ? Severity.ERROR : Severity.WARNING, "DUPLICATE_EVIDENCE_REF",
                        path, "Repeated evidence reference: " + evidenceId);
                }
            }
        }
        if (umapFuzzy && strict) {
            Object membership = edge.getRelationship().getProperties() == null ? null
                : edge.getRelationship().getProperties().get("membership");
            if (!(membership instanceof Number number) || !Double.isFinite(number.doubleValue())
                || number.doubleValue() < 0 || number.doubleValue() > 1) {
                error(issues, "UMAP_MEMBERSHIP", path, "Fuzzy membership must be numeric in [0, 1]");
            }
        }
    }

    private static void checkKind(List<String> allowed, GraphNode node, String role,
            String predicateId, String path, List<Issue> issues) {
        if (allowed == null || allowed.isEmpty() || node == null || blank(node.getNodeKind())) return;
        String actual = node.getNodeKind().trim().toUpperCase(Locale.ROOT);
        boolean matches = allowed.stream().anyMatch(kind -> kind != null
            && actual.equals(kind.trim().toUpperCase(Locale.ROOT)));
        if (!matches) error(issues, "ENDPOINT_KIND", path,
            predicateId + " " + role + " nodeKind " + node.getNodeKind() + " is not allowed");
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void error(List<Issue> issues, String code, String path, String message) {
        issue(issues, Severity.ERROR, code, path, message);
    }
    private static void issue(List<Issue> issues, Severity severity,
                              String code, String path, String message) {
        issues.add(new Issue(severity, code, path, message));
    }
}
