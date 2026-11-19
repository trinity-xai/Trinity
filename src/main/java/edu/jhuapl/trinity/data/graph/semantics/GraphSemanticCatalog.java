package edu.jhuapl.trinity.data.graph.semantics;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Built-in and graph-specific predicate definitions. Legacy labels are read
 * without rewriting the source graph or inventing provenance/evidence.
 * Unknown explicit predicates remain intact for later registration.
 */
public final class GraphSemanticCatalog {
    private final Map<String, GraphPredicateDefinition> definitions = new LinkedHashMap<>();

    public GraphSemanticCatalog() {
        builtin("SUBCLASS_OF", GraphPredicateFamily.TAXONOMY, "subclass", "superclass", true, true, true);
        builtin("INSTANCE_OF", GraphPredicateFamily.CLASSIFICATION, "instance", "class", false, false, false);
        // IS_A is intentionally not an alias: a legacy IS_A label does not say
        // whether its subject is an instance or a narrower class.
        builtin("IS_A", GraphPredicateFamily.CLASSIFICATION, "subject", "classification", false, false, false);
        builtin("PART_OF", GraphPredicateFamily.PART_WHOLE, "part", "whole", true, false, false);
        GraphPredicateDefinition contains = builtin("CONTAINS", GraphPredicateFamily.SOURCE_STRUCTURE,
            "container", "contained", true, true, false);
        contains.setHierarchyOrientation(GraphHierarchyOrientation.SUBJECT_IS_PARENT);
        builtin("MEMBER_OF", GraphPredicateFamily.MEMBERSHIP, "member", "group", true, false, false);
        builtin("MENTIONED_IN", GraphPredicateFamily.SOURCE_REFERENCE, "mention", "source", false, false, false);
        builtin("DERIVED_FROM", GraphPredicateFamily.PROVENANCE, "derived", "source", false, false, false);
        GraphPredicateDefinition umap = builtin("UMAP_FUZZY_ASSOCIATION",
            GraphPredicateFamily.DERIVED_ASSOCIATION, "member", "neighbor", false, false, false);
        umap.setSymmetric(true);
    }

    private GraphPredicateDefinition builtin(String id, GraphPredicateFamily family,
                                             String subject, String object,
                                             boolean hierarchy, boolean acyclic,
                                             boolean transitive) {
        GraphPredicateDefinition def = new GraphPredicateDefinition();
        def.setPredicateId(id);
        def.setFamily(family);
        def.setSubjectRole(subject);
        def.setObjectRole(object);
        def.setSymmetric(false);
        def.setHierarchy(hierarchy);
        def.setAcyclic(acyclic);
        if (hierarchy) {
            def.setAllowsMultipleParents(true);
            def.setHierarchyOrientation(GraphHierarchyOrientation.SUBJECT_IS_CHILD);
        }
        def.setTransitive(transitive);
        definitions.put(id, def);
        return def;
    }

    /** Registers a custom definition. Built-in IDs and duplicates cannot be redefined. */
    public void register(GraphPredicateDefinition definition) {
        if (definition == null || blank(definition.getPredicateId())) {
            throw new IllegalArgumentException("Predicate definition needs a nonblank predicateId");
        }
        String id = definition.getPredicateId();
        if (definitions.containsKey(id) || BUILTIN_IDS.containsKey(normalizeLegacyLabel(id))) {
            throw new IllegalArgumentException("Duplicate or built-in predicate definition: " + id);
        }
        if (definition.getFamily() == null) {
            throw new IllegalArgumentException("Predicate " + id + " needs a family");
        }
        definitions.put(id, definition);
    }

    public static GraphSemanticCatalog forGraph(GraphDirectedCollection graph) {
        GraphSemanticCatalog catalog = new GraphSemanticCatalog();
        if (graph != null && graph.getPredicateDefinitions() != null) {
            for (GraphPredicateDefinition definition : graph.getPredicateDefinitions()) {
                catalog.register(definition);
            }
        }
        return catalog;
    }

    public GraphPredicateDefinition definition(String predicateId) {
        if (blank(predicateId)) return null;
        GraphPredicateDefinition exact = definitions.get(predicateId);
        if (exact != null) return exact;
        // Normalize only built-in spellings. Custom IDs retain exact identity.
        String normalized = normalizeLegacyLabel(predicateId);
        GraphPredicateDefinition normalizedDefinition = definitions.get(normalized);
        return normalizedDefinition != null && BUILTIN_IDS.containsKey(normalized)
            ? normalizedDefinition : null;
    }

    public Map<String, GraphPredicateDefinition> definitions() {
        return Collections.unmodifiableMap(definitions);
    }

    public Resolution resolve(GraphRelationship relationship) {
        if (relationship == null) return new Resolution(null, null, null, false, null);
        GraphRelationshipSemantics semantics = relationship.getSemantics();
        if (semantics != null) {
            String explicitId = semantics.getPredicateId();
            return new Resolution(explicitId, definition(explicitId), semantics.getOrigin(),
                false, null);
        }
        List<String> labels = relationship.getLabels();
        String unknown = null;
        if (labels != null) {
            for (String label : labels) {
                if (blank(label)) continue;
                if (unknown == null) unknown = label;
                String normalized = normalizeLegacyLabel(label);
                // Do not infer domain assertions or custom types from arbitrary
                // legacy display labels. Only documented built-ins are recognized.
                GraphPredicateDefinition def = BUILTIN_IDS.get(normalized);
                if (def != null) {
                    GraphAssertionOrigin origin = normalized.equals("UMAP_FUZZY_ASSOCIATION")
                        ? GraphAssertionOrigin.ALGORITHM_DERIVED : null;
                    return new Resolution(def.getPredicateId(), definition(def.getPredicateId()),
                        origin, true, null);
                }
            }
        }
        return new Resolution(null, null, null, true, unknown);
    }

    /** Keeps actual semantics separate from labels inferred solely for read-only compatibility. */
    public record Resolution(String predicateId, GraphPredicateDefinition definition,
                             GraphAssertionOrigin origin, boolean fromLegacyLabels,
                             String unclassifiedLegacyLabel) {
        public boolean isRecognized() { return definition != null; }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static String normalizeLegacyLabel(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    // A separate built-in snapshot prevents graph-custom definitions from being
    // mistaken for asserted ontology facts based only on old display labels.
    private static final Map<String, GraphPredicateDefinition> BUILTIN_IDS =
        Map.copyOf(new GraphSemanticCatalog().definitions);
}
