package edu.jhuapl.trinity.data.graph.analysis;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import edu.jhuapl.trinity.data.graph.semantics.GraphEvidence;
import edu.jhuapl.trinity.data.graph.semantics.GraphPredicateDefinition;
import edu.jhuapl.trinity.data.graph.semantics.GraphRelationshipSemantics;
import edu.jhuapl.trinity.data.graph.semantics.GraphRunProvenance;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Conservative, provenance-preserving UNION of independent source graphs.
 * Every source is explicitly namespaced; entities are NOT reconciled or inferred
 * to be identical. Parallel/contradictory edges and all evidence remain separate.
 * This is composition, not semantic extraction or ontology inheritance.
 */
public final class GraphCollectionComposer {
    public record Input(String namespace, GraphDirectedCollection graph) { }
    public record SourceMapping(String namespace, String sourceGraphId, String sourceRunId,
                                Map<String, String> nodeIds,
                                Map<String, String> edgeIds,
                                Map<String, String> evidenceIds,
                                Map<String, String> hierarchyScopeIds) {
        public SourceMapping {
            nodeIds = Map.copyOf(nodeIds);
            edgeIds = Map.copyOf(edgeIds);
            evidenceIds = Map.copyOf(evidenceIds);
            hierarchyScopeIds = Map.copyOf(hierarchyScopeIds);
        }
    }
    public record Result(GraphDirectedCollection graph, List<SourceMapping> sources) {
        public Result { sources = List.copyOf(sources); }
    }

    private GraphCollectionComposer() { }

    /**
     * All namespaces must be nonblank and distinct. Legacy edges without edgeId
     * get a clearly positional fallback ID (source index), which is not a
     * persistent cross-reimport assertion identity. A source with duplicate
     * local IDs is rejected; it is never silently deduplicated or merged.
     */
    public static Result compose(String outputGraphId, List<Input> inputs) {
        return compose(outputGraphId, inputs, () -> false);
    }

    public static Result compose(String outputGraphId, List<Input> inputs, BooleanSupplier cancelled) {
        if (blank(outputGraphId) || inputs == null || inputs.isEmpty())
            throw new IllegalArgumentException("Output graphId and at least one input are required");
        GraphDirectedCollection combined = new GraphDirectedCollection();
        combined.setGraphId(outputGraphId);
        List<SourceMapping> mappings = new ArrayList<>();
        Set<String> usedNamespaces = new HashSet<>();
        Map<String, GraphPredicateDefinition> customPredicates = new LinkedHashMap<>();
        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        List<GraphEvidence> evidence = new ArrayList<>();
        List<Map<String, Object>> inputRuns = new ArrayList<>();
        Set<String> documentIds = new LinkedHashSet<>();
        boolean typedVersion = true;

        for (Input input : inputs) {
            checkCancelled(cancelled);
            if (input == null || blank(input.namespace()) || input.graph() == null)
                throw new IllegalArgumentException("Every input needs a nonblank namespace and graph");
            String ns = input.namespace();
            if (!usedNamespaces.add(ns))
                throw new IllegalArgumentException("Repeated source namespace: " + ns);
            GraphDirectedCollection source = input.graph();
            if (source.getNodes() == null || source.getEdges() == null)
                throw new IllegalArgumentException("Missing source nodes or edges: " + ns);
            typedVersion &= source.getSchemaVersion() != null && source.getSchemaVersion() >= 2;
            Map<String, String> nodeIds = new LinkedHashMap<>();
            Map<String, String> edgeIds = new LinkedHashMap<>();
            Map<String, String> evidenceIds = new LinkedHashMap<>();
            Map<String, String> scopes = new LinkedHashMap<>();

            if (source.getPredicateDefinitions() != null) {
                for (GraphPredicateDefinition definition : source.getPredicateDefinitions()) {
                    if (definition == null || blank(definition.getPredicateId()))
                        throw new IllegalArgumentException("Invalid custom predicate in " + ns);
                    GraphPredicateDefinition prior = customPredicates.putIfAbsent(
                        definition.getPredicateId(), copy(definition));
                    if (prior != null && !samePredicate(prior, definition))
                        throw new IllegalArgumentException("Conflicting definition for predicate "
                            + definition.getPredicateId() + " in " + ns);
                }
            }
            for (int i = 0; i < source.getNodes().size(); i++) {
                if ((i & 255) == 0) checkCancelled(cancelled);
                GraphNode original = source.getNodes().get(i);
                if (original == null || blank(original.getEntityID()))
                    throw new IllegalArgumentException("Missing node ID in " + ns + " at " + i);
                String id = qualified(ns, original.getEntityID());
                if (nodeIds.putIfAbsent(original.getEntityID(), id) != null)
                    throw new IllegalArgumentException("Duplicate source node ID " + original.getEntityID()
                        + " in " + ns);
                GraphNode node = new GraphNode();
                node.setEntityID(id);
                node.setIdentity(original.getIdentity()); // local numeric identity is not a global key
                node.setNodeKind(original.getNodeKind());
                node.setColor(original.getColor() == null ? source.getDefaultNodeColor() : original.getColor());
                node.setLabels(original.getLabels() == null ? null : new ArrayList<>(original.getLabels()));
                node.setVector(original.getVector() == null ? null : new ArrayList<>(original.getVector()));
                node.setProperties(copyProperties(original.getProperties()));
                nodes.add(node);
            }
            if (source.getEvidenceRecords() != null) {
                for (GraphEvidence item : source.getEvidenceRecords()) {
                    checkCancelled(cancelled);
                    if (item == null || blank(item.getEvidenceId()))
                        throw new IllegalArgumentException("Missing evidence ID in " + ns);
                    String id = qualified(ns, item.getEvidenceId());
                    if (evidenceIds.putIfAbsent(item.getEvidenceId(), id) != null)
                        throw new IllegalArgumentException("Duplicate evidence ID in " + ns + ": "
                            + item.getEvidenceId());
                    GraphEvidence copied = copy(item);
                    copied.setEvidenceId(id);
                    evidence.add(copied);
                }
            }
            for (int i = 0; i < source.getEdges().size(); i++) {
                if ((i & 255) == 0) checkCancelled(cancelled);
                GraphEdge original = source.getEdges().get(i);
                if (original == null || !nodeIds.containsKey(original.getStartID())
                    || !nodeIds.containsKey(original.getEndID()))
                    throw new IllegalArgumentException("Unresolved edge endpoints in " + ns + " at " + i);
                String sourceEdgeKey = blank(original.getEdgeId())
                    ? "legacy-index:" + i : "id:" + original.getEdgeId();
                String qualifiedEdgeId = qualified(ns, sourceEdgeKey);
                if (edgeIds.putIfAbsent(sourceEdgeKey, qualifiedEdgeId) != null)
                    throw new IllegalArgumentException("Duplicate edge ID in " + ns + ": " + sourceEdgeKey);
                GraphEdge edge = new GraphEdge();
                edge.setStartID(nodeIds.get(original.getStartID()));
                edge.setEndID(nodeIds.get(original.getEndID()));
                edge.setEdgeId(qualifiedEdgeId);
                edge.setColor(original.getColor() == null ? source.getDefaultEdgeColor() : original.getColor());
                if (original.getRelationship() != null) {
                    GraphRelationship prior = original.getRelationship();
                    GraphRelationship relation = new GraphRelationship();
                    relation.setIdentity(prior.getIdentity());
                    relation.setLabels(prior.getLabels() == null ? null : new ArrayList<>(prior.getLabels()));
                    relation.setProperties(copyProperties(prior.getProperties()));
                    if (prior.getSemantics() != null) {
                        GraphRelationshipSemantics semantics = new GraphRelationshipSemantics();
                        GraphRelationshipSemantics originalSemantics = prior.getSemantics();
                        semantics.setPredicateId(originalSemantics.getPredicateId());
                        semantics.setOrigin(originalSemantics.getOrigin());
                        String scope = originalSemantics.getHierarchyScopeId();
                        if (scope != null) {
                            if (scope.isBlank()) throw new IllegalArgumentException("Blank scope in " + ns);
                            scopes.putIfAbsent(scope, qualified(ns, scope));
                            semantics.setHierarchyScopeId(scopes.get(scope));
                        }
                        if (originalSemantics.getEvidenceIds() != null) {
                            List<String> remapped = new ArrayList<>();
                            for (String evidenceId : originalSemantics.getEvidenceIds()) {
                                String ref = evidenceIds.get(evidenceId);
                                if (ref == null) throw new IllegalArgumentException(
                                    "Unresolved evidence " + evidenceId + " on " + sourceEdgeKey
                                        + " in " + ns);
                                remapped.add(ref);
                            }
                            semantics.setEvidenceIds(remapped);
                        }
                        relation.setSemantics(semantics);
                    }
                    edge.setRelationship(relation);
                }
                edges.add(edge);
            }
            String sourceRunId = source.getRunProvenance() == null
                ? null : source.getRunProvenance().getRunId();
            mappings.add(new SourceMapping(ns, source.getGraphId(), sourceRunId,
                nodeIds, edgeIds, evidenceIds, scopes));
            Map<String, Object> inputInfo = new LinkedHashMap<>();
            inputInfo.put("namespace", ns);
            if (source.getGraphId() != null) inputInfo.put("graphId", source.getGraphId());
            if (source.getSchemaVersion() != null) inputInfo.put("schemaVersion", source.getSchemaVersion());
            GraphRunProvenance inputRun = source.getRunProvenance();
            if (sourceRunId != null) inputInfo.put("runId", sourceRunId);
            if (inputRun != null) {
                if (inputRun.getProducer() != null) inputInfo.put("producer", inputRun.getProducer());
                if (inputRun.getMethod() != null) inputInfo.put("method", inputRun.getMethod());
                if (inputRun.getSourceCollectionId() != null)
                    inputInfo.put("sourceCollectionId", inputRun.getSourceCollectionId());
                if (inputRun.getParameters() != null)
                    inputInfo.put("parameters", copyJsonValue(inputRun.getParameters(),
                        new IdentityHashMap<>()));
                if (inputRun.getSourceDocumentIds() != null)
                    inputInfo.put("sourceDocumentIds", new ArrayList<>(inputRun.getSourceDocumentIds()));
            }
            inputRuns.add(inputInfo);
            if (source.getRunProvenance() != null
                && source.getRunProvenance().getSourceDocumentIds() != null)
                documentIds.addAll(source.getRunProvenance().getSourceDocumentIds());
        }
        checkCancelled(cancelled);
        combined.setNodes(nodes);
        combined.setEdges(edges);
        combined.setEvidenceRecords(evidence.isEmpty() ? null : evidence);
        combined.setPredicateDefinitions(customPredicates.isEmpty()
            ? null : new ArrayList<>(customPredicates.values()));
        if (typedVersion) combined.setSchemaVersion(2);
        GraphRunProvenance run = new GraphRunProvenance();
        run.setProducer("GraphCollectionComposer");
        run.setMethod("NAMESPACED_UNION_NO_RECONCILIATION");
        run.setParameters(Map.of("sourceGraphs", inputRuns));
        if (!documentIds.isEmpty()) run.setSourceDocumentIds(new ArrayList<>(documentIds));
        combined.setRunProvenance(run);
        return new Result(combined, mappings);
    }

    /** Deterministic length-prefixed qualification, unambiguous with arbitrary IDs. */
    public static String qualified(String namespace, String localId) {
        if (blank(namespace) || blank(localId))
            throw new IllegalArgumentException("Namespace and local ID must be nonblank");
        return namespace.length() + ":" + namespace + localId.length() + ":" + localId;
    }

    /** Recovers the exact source namespace and local ID after JSON roundtrip. */
    public record QualifiedId(String namespace, String localId) { }

    public static QualifiedId decode(String qualifiedId) {
        if (blank(qualifiedId)) throw new IllegalArgumentException("Qualified ID is required");
        int first = qualifiedId.indexOf(':');
        if (first <= 0) throw new IllegalArgumentException("Malformed qualified ID");
        try {
            int nsLength = Integer.parseInt(qualifiedId.substring(0, first));
            if (nsLength < 1 || first + 1L + nsLength >= qualifiedId.length())
                throw new IllegalArgumentException("Malformed qualified ID namespace");
            int namespaceEnd = first + 1 + nsLength;
            int lengthStart = namespaceEnd;
            int lengthEnd = qualifiedId.indexOf(':', lengthStart);
            if (lengthEnd <= lengthStart) throw new IllegalArgumentException("Malformed qualified ID local length");
            int localLength = Integer.parseInt(qualifiedId.substring(lengthStart, lengthEnd));
            if (localLength < 1 || lengthEnd + 1L + localLength != qualifiedId.length())
                throw new IllegalArgumentException("Malformed qualified ID local value");
            return new QualifiedId(qualifiedId.substring(first + 1, namespaceEnd),
                qualifiedId.substring(lengthEnd + 1));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Malformed qualified ID lengths", ex);
        }
    }

    private static GraphPredicateDefinition copy(GraphPredicateDefinition d) {
        GraphPredicateDefinition c = new GraphPredicateDefinition();
        c.setPredicateId(d.getPredicateId()); c.setFamily(d.getFamily());
        c.setSubjectRole(d.getSubjectRole()); c.setObjectRole(d.getObjectRole());
        c.setSymmetric(d.getSymmetric()); c.setHierarchy(d.getHierarchy());
        c.setHierarchyOrientation(d.getHierarchyOrientation()); c.setAcyclic(d.getAcyclic());
        c.setAllowsMultipleParents(d.getAllowsMultipleParents()); c.setTransitive(d.getTransitive());
        c.setAllowedSubjectNodeKinds(d.getAllowedSubjectNodeKinds() == null ? null
            : new ArrayList<>(d.getAllowedSubjectNodeKinds()));
        c.setAllowedObjectNodeKinds(d.getAllowedObjectNodeKinds() == null ? null
            : new ArrayList<>(d.getAllowedObjectNodeKinds()));
        return c;
    }

    private static boolean samePredicate(GraphPredicateDefinition a, GraphPredicateDefinition b) {
        return Objects.equals(a.getPredicateId(), b.getPredicateId())
            && Objects.equals(a.getFamily(), b.getFamily())
            && Objects.equals(a.getSubjectRole(), b.getSubjectRole())
            && Objects.equals(a.getObjectRole(), b.getObjectRole())
            && Objects.equals(a.getSymmetric(), b.getSymmetric())
            && Objects.equals(a.getHierarchy(), b.getHierarchy())
            && Objects.equals(a.getHierarchyOrientation(), b.getHierarchyOrientation())
            && Objects.equals(a.getAcyclic(), b.getAcyclic())
            && Objects.equals(a.getAllowsMultipleParents(), b.getAllowsMultipleParents())
            && Objects.equals(a.getTransitive(), b.getTransitive())
            && Objects.equals(a.getAllowedSubjectNodeKinds(), b.getAllowedSubjectNodeKinds())
            && Objects.equals(a.getAllowedObjectNodeKinds(), b.getAllowedObjectNodeKinds());
    }

    private static GraphEvidence copy(GraphEvidence e) {
        GraphEvidence c = new GraphEvidence();
        c.setEvidenceId(e.getEvidenceId()); c.setDocumentId(e.getDocumentId());
        c.setDocumentRevision(e.getDocumentRevision()); c.setPassageId(e.getPassageId());
        c.setPageNumber(e.getPageNumber()); c.setSectionId(e.getSectionId());
        c.setStartOffset(e.getStartOffset()); c.setEndOffset(e.getEndOffset());
        c.setSpanCoordinateSystem(e.getSpanCoordinateSystem()); c.setQuote(e.getQuote());
        c.setExtractionRunId(e.getExtractionRunId()); c.setExtractionMethod(e.getExtractionMethod());
        c.setExtractionScore(e.getExtractionScore()); c.setReviewStatus(e.getReviewStatus());
        return c;
    }

    private static HashMap<String, Object> copyProperties(Map<String, Object> source) {
        if (source == null) return null;
        HashMap<String, Object> copied = new HashMap<>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        for (var entry : source.entrySet())
            copied.put(entry.getKey(), copyJsonValue(entry.getValue(), visited));
        return copied;
    }

    /** Deep-copy JSON-compatible containers. Opaque property values are passed through. */
    private static Object copyJsonValue(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (value == null) return null;
        if (value instanceof Map<?, ?> map) {
            if (visited.put(value, true) != null)
                throw new IllegalArgumentException("Cyclic graph property map");
            Map<Object, Object> copy = new LinkedHashMap<>();
            for (var e : map.entrySet()) copy.put(e.getKey(), copyJsonValue(e.getValue(), visited));
            visited.remove(value);
            return copy;
        }
        if (value instanceof List<?> list) {
            if (visited.put(value, true) != null)
                throw new IllegalArgumentException("Cyclic graph property list");
            List<Object> copy = new ArrayList<>(list.size());
            for (Object element : list) copy.add(copyJsonValue(element, visited));
            visited.remove(value);
            return copy;
        }
        if (value.getClass().isArray()) {
            if (visited.put(value, true) != null)
                throw new IllegalArgumentException("Cyclic graph property array");
            int length = Array.getLength(value);
            Object copy = Array.newInstance(value.getClass().getComponentType(), length);
            for (int i = 0; i < length; i++)
                Array.set(copy, i, copyJsonValue(Array.get(value, i), visited));
            visited.remove(value);
            return copy;
        }
        return value;
    }

    private static boolean blank(String v) { return v == null || v.isBlank(); }
    private static void checkCancelled(BooleanSupplier cancelled) {
        if ((cancelled != null && cancelled.getAsBoolean()) || Thread.currentThread().isInterrupted())
            throw new CancellationException("Graph composition cancelled");
    }
}
