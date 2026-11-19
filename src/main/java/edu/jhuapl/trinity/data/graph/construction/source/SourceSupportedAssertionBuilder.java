package edu.jhuapl.trinity.data.graph.construction.source;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import edu.jhuapl.trinity.data.graph.semantics.GraphAssertionOrigin;
import edu.jhuapl.trinity.data.graph.semantics.GraphPredicateDefinition;
import edu.jhuapl.trinity.data.graph.semantics.GraphRelationshipSemantics;
import edu.jhuapl.trinity.data.graph.semantics.GraphRunProvenance;
import edu.jhuapl.trinity.data.graph.semantics.GraphSemanticCatalog;
import edu.jhuapl.trinity.data.graph.semantics.GraphSemanticValidator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Materializes explicitly supplied, evidence-linked semantic statements over
 * an imported source hierarchy. This is NOT a language model extractor or an
 * entity resolution engine: input entities, predicates, hierarchy memberships,
 * canonical IDs and supporting passages must be supplied by a separate source
 * extraction/review workflow. Contradictory and parallel assertions remain
 * independent; subclass parents are never collapsed into a single tree.
 */
public final class SourceSupportedAssertionBuilder {
    private SourceSupportedAssertionBuilder() { }

    /** An already identified semantic entity; nodeKind is e.g. ENTITY, CLASS, EVENT. */
    public record Entity(String entityId, String name, String nodeKind) {
        public Entity {
            if (blank(entityId) || blank(name) || blank(nodeKind)) {
                throw new IllegalArgumentException("Semantic entity ID, name and kind are required");
            }
        }
    }

    /**
     * One independent statement. passageIds are references into the exact
     * imported source revision and may include multiple corroborating chunks.
     * SOURCE_ASSERTED statements require at least one supporting passage.
     */
    public record Assertion(String assertionId, String subjectId, String predicateId,
                            String objectId, String hierarchyScopeId,
                            GraphAssertionOrigin origin, List<String> passageIds) {
        public Assertion {
            if (blank(assertionId) || blank(subjectId) || blank(predicateId)
                    || blank(objectId) || origin == null) {
                throw new IllegalArgumentException("Assertion needs ID, endpoints, predicate and origin");
            }
            if (hierarchyScopeId != null && hierarchyScopeId.isBlank()) {
                throw new IllegalArgumentException("Blank hierarchy scope is invalid");
            }
            passageIds = List.copyOf(Objects.requireNonNull(passageIds, "passageIds"));
            if (passageIds.stream().anyMatch(SourceSupportedAssertionBuilder::blank)) {
                throw new IllegalArgumentException("Blank passage evidence ID");
            }
            if (origin != GraphAssertionOrigin.SOURCE_ASSERTED
                    && origin != GraphAssertionOrigin.USER_CURATED) {
                throw new IllegalArgumentException("Semantic assertion must be source-asserted or user-curated");
            }
            if (origin == GraphAssertionOrigin.SOURCE_ASSERTED && passageIds.isEmpty()) {
                throw new IllegalArgumentException("Source-asserted statements require passage evidence");
            }
        }
    }

    public static GraphDirectedCollection build(SourceDocumentSnapshot source,
             List<Entity> entities, List<Assertion> assertions,
             List<GraphPredicateDefinition> customDefinitions) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(entities, "entities");
        Objects.requireNonNull(assertions, "assertions");
        Objects.requireNonNull(customDefinitions, "customDefinitions");
        GraphDirectedCollection graph = SourceDocumentHierarchyBuilder.build(source);
        graph.setPredicateDefinitions(new ArrayList<>(customDefinitions));
        GraphSemanticCatalog catalog = GraphSemanticCatalog.forGraph(graph);
        Map<String, GraphNode> nodeById = new LinkedHashMap<>();
        for (GraphNode node : graph.getNodes()) nodeById.put(node.getEntityID(), node);
        long nodeIdentity = graph.getNodes().size() + 1L;
        for (Entity entity : entities) {
            if (nodeById.containsKey(entity.entityId())) {
                throw new IllegalArgumentException("Duplicate source/semantic node ID: " + entity.entityId());
            }
            GraphNode node = new GraphNode();
            node.setIdentity(nodeIdentity++);
            node.setEntityID(entity.entityId());
            node.setNodeKind(entity.nodeKind());
            node.getLabels().add(entity.nodeKind());
            node.getProperties().put("name", entity.name());
            graph.getNodes().add(node);
            nodeById.put(entity.entityId(), node);
        }

        Map<String, String> passageEvidence = new HashMap<>();
        graph.getEvidenceRecords().forEach(e -> passageEvidence.put(e.getPassageId(), e.getEvidenceId()));
        Set<String> edgeIds = new HashSet<>();
        graph.getEdges().forEach(e -> edgeIds.add(e.getEdgeId()));
        long relationshipIdentity = graph.getEdges().size() + 1L;
        for (Assertion assertion : assertions) {
            if (!nodeById.containsKey(assertion.subjectId()) || !nodeById.containsKey(assertion.objectId())) {
                throw new IllegalArgumentException("Unknown source/semantic assertion endpoint: " + assertion.assertionId());
            }
            if (!edgeIds.add(assertion.assertionId())) {
                throw new IllegalArgumentException("Duplicate assertion ID: " + assertion.assertionId());
            }
            GraphPredicateDefinition def = catalog.definition(assertion.predicateId());
            if (def == null || !def.getPredicateId().equals(assertion.predicateId())) {
                throw new IllegalArgumentException("Unregistered canonical predicate: " + assertion.predicateId());
            }
            if (Boolean.TRUE.equals(def.getHierarchy()) && assertion.hierarchyScopeId() == null) {
                throw new IllegalArgumentException("Semantic hierarchies require an explicit namespace");
            }
            Set<String> uniqueEvidence = new java.util.LinkedHashSet<>();
            for (String passageId : assertion.passageIds()) {
                String evidenceId = passageEvidence.get(passageId);
                if (evidenceId == null) {
                    throw new IllegalArgumentException("Passage evidence not found in this revision: " + passageId);
                }
                uniqueEvidence.add(evidenceId);
            }
            GraphEdge edge = new GraphEdge();
            edge.setEdgeId(assertion.assertionId());
            edge.setStartID(assertion.subjectId());
            edge.setEndID(assertion.objectId());
            GraphRelationship relationship = new GraphRelationship();
            relationship.setIdentity(relationshipIdentity++);
            relationship.getLabels().add(assertion.predicateId());
            GraphRelationshipSemantics semantics = new GraphRelationshipSemantics();
            semantics.setPredicateId(assertion.predicateId());
            semantics.setHierarchyScopeId(assertion.hierarchyScopeId());
            semantics.setOrigin(assertion.origin());
            if (!uniqueEvidence.isEmpty()) semantics.setEvidenceIds(List.copyOf(uniqueEvidence));
            relationship.setSemantics(semantics);
            edge.setRelationship(relationship);
            graph.getEdges().add(edge);
        }
        GraphRunProvenance run = graph.getRunProvenance();
        run.setMethod("SOURCE_STRUCTURE_WITH_EXPLICIT_ASSERTIONS");
        run.setRunId(SourceDocumentIdentity.structuralId("run", source.documentId(),
            source.documentRevision(), "SOURCE_STRUCTURE_WITH_EXPLICIT_ASSERTIONS",
            assertions.stream().map(Assertion::assertionId).sorted()
                .collect(java.util.stream.Collectors.joining("|"))));
        Map<String, Object> params = new LinkedHashMap<>(run.getParameters());
        params.put("explicitAssertions", assertions.size());
        params.put("explicitEntities", entities.size());
        run.setParameters(params);
        var validation = GraphSemanticValidator.validate(graph,
            GraphSemanticValidator.Mode.STRICT_TYPED);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("Invalid supported source assertions: " + validation.issues());
        }
        return graph;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
