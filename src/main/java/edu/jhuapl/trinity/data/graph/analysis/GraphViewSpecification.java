package edu.jhuapl.trinity.data.graph.analysis;

import edu.jhuapl.trinity.data.graph.semantics.GraphAssertionOrigin;
import edu.jhuapl.trinity.data.graph.semantics.GraphPredicateFamily;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, opt-in selection and weighting policy for a SOURCE graph.
 * All nonempty predicate/family/origin/node-kind filters intersect. A scoped
 * filter also intersects; null means an explicitly unscoped hierarchy.
 *
 * Nothing in this specification changes source edges, ontology assertions,
 * display visibility, or the default (legacy) Context Graph computations.
 */
public final class GraphViewSpecification {
    private final Set<String> predicateIds;
    private final Set<GraphPredicateFamily> families;
    private final Set<GraphAssertionOrigin> origins;
    private final Set<String> nodeKinds;
    private final Set<String> hierarchyScopeIds;
    private final boolean scopeFilterEnabled;
    private final boolean includeUnspecifiedNodeKinds;
    private final boolean retainIsolatedNodes;
    private final boolean includeKnownLegacyLabels;
    private final boolean includeUnclassified;
    private final boolean useUmapMembership;
    private final double minimumUmapMembership;
    private final double defaultWeight;
    private final Map<GraphPredicateFamily, Double> familyWeights;
    private final Map<String, Double> predicateWeights;
    private final Map<GraphAssertionOrigin, Double> originMultipliers;

    private GraphViewSpecification(Builder b) {
        predicateIds = Set.copyOf(b.predicateIds);
        families = Set.copyOf(b.families);
        origins = Set.copyOf(b.origins);
        nodeKinds = Set.copyOf(b.nodeKinds);
        // A scope filter can explicitly include a null (unscoped) hierarchy.
        hierarchyScopeIds = Collections.unmodifiableSet(new LinkedHashSet<>(b.hierarchyScopeIds));
        scopeFilterEnabled = b.scopeFilterEnabled;
        includeUnspecifiedNodeKinds = b.includeUnspecifiedNodeKinds;
        retainIsolatedNodes = b.retainIsolatedNodes;
        includeKnownLegacyLabels = b.includeKnownLegacyLabels;
        includeUnclassified = b.includeUnclassified;
        useUmapMembership = b.useUmapMembership;
        minimumUmapMembership = b.minimumUmapMembership;
        defaultWeight = b.defaultWeight;
        familyWeights = Map.copyOf(b.familyWeights);
        predicateWeights = Map.copyOf(b.predicateWeights);
        originMultipliers = Map.copyOf(b.originMultipliers);
    }

    public static Builder builder() { return new Builder(); }

    /** Explicitly includes known legacy labels and unknown/unclassified relationships. */
    public static GraphViewSpecification allRelationships() {
        return builder().includeKnownLegacyLabels(true).includeUnclassified(true).build();
    }

    /** The fuzzy-graph analysis path also accepts 2.2 legacy labeled UMAP edges. */
    public static GraphViewSpecification umapFuzzy() {
        return builder().predicateIds("UMAP_FUZZY_ASSOCIATION")
            .includeKnownLegacyLabels(true).build();
    }

    public Set<String> predicateIds() { return predicateIds; }
    public Set<GraphPredicateFamily> families() { return families; }
    public Set<GraphAssertionOrigin> origins() { return origins; }
    public Set<String> nodeKinds() { return nodeKinds; }
    public Set<String> hierarchyScopeIds() { return hierarchyScopeIds; }
    public boolean scopeFilterEnabled() { return scopeFilterEnabled; }
    public boolean includeUnspecifiedNodeKinds() { return includeUnspecifiedNodeKinds; }
    public boolean retainIsolatedNodes() { return retainIsolatedNodes; }
    public boolean includeKnownLegacyLabels() { return includeKnownLegacyLabels; }
    public boolean includeUnclassified() { return includeUnclassified; }
    public boolean useUmapMembership() { return useUmapMembership; }
    public double minimumUmapMembership() { return minimumUmapMembership; }
    public double defaultWeight() { return defaultWeight; }
    public Map<GraphPredicateFamily, Double> familyWeights() { return familyWeights; }
    public Map<String, Double> predicateWeights() { return predicateWeights; }
    public Map<GraphAssertionOrigin, Double> originMultipliers() { return originMultipliers; }

    /** Predicate override > family override > base; an origin multiplier is then applied. */
    public double weight(String predicateId, GraphPredicateFamily family, GraphAssertionOrigin origin) {
        double base = predicateId != null && predicateWeights.containsKey(predicateId)
            ? predicateWeights.get(predicateId) : familyWeights.getOrDefault(family, defaultWeight);
        return base * (origin == null ? 1.0 : originMultipliers.getOrDefault(origin, 1.0));
    }

    public static final class Builder {
        private final Set<String> predicateIds = new LinkedHashSet<>();
        private final Set<GraphPredicateFamily> families = new LinkedHashSet<>();
        private final Set<GraphAssertionOrigin> origins = new LinkedHashSet<>();
        private final Set<String> nodeKinds = new LinkedHashSet<>();
        private final Set<String> hierarchyScopeIds = new LinkedHashSet<>();
        private boolean scopeFilterEnabled;
        private boolean includeUnspecifiedNodeKinds;
        private boolean retainIsolatedNodes = true;
        private boolean includeKnownLegacyLabels;
        private boolean includeUnclassified;
        private boolean useUmapMembership = true;
        private double minimumUmapMembership;
        private double defaultWeight = 1.0;
        private final Map<GraphPredicateFamily, Double> familyWeights = new LinkedHashMap<>();
        private final Map<String, Double> predicateWeights = new LinkedHashMap<>();
        private final Map<GraphAssertionOrigin, Double> originMultipliers = new LinkedHashMap<>();
        private Builder() { }

        public Builder predicateIds(String... values) {
            for (String value : values) predicateIds.add(nonblank(value, "predicateId"));
            return this;
        }
        public Builder families(GraphPredicateFamily... values) {
            for (GraphPredicateFamily value : values) families.add(required(value, "family"));
            return this;
        }
        public Builder origins(GraphAssertionOrigin... values) {
            for (GraphAssertionOrigin value : values) origins.add(required(value, "origin"));
            return this;
        }
        public Builder nodeKinds(String... values) {
            for (String value : values) nodeKinds.add(nonblank(value, "nodeKind").trim().toUpperCase(java.util.Locale.ROOT));
            return this;
        }
        /** Limits matching to the given scopes. Pass null for unscoped relationships. */
        public Builder hierarchyScopes(String... values) {
            scopeFilterEnabled = true;
            for (String value : values) hierarchyScopeIds.add(value == null ? null : nonblank(value, "scope"));
            return this;
        }
        public Builder includeUnspecifiedNodeKinds(boolean value) { includeUnspecifiedNodeKinds = value; return this; }
        public Builder retainIsolatedNodes(boolean value) { retainIsolatedNodes = value; return this; }
        public Builder includeKnownLegacyLabels(boolean value) { includeKnownLegacyLabels = value; return this; }
        /** Unclassified remains unclassified; enabling this never invents a predicate or evidence. */
        public Builder includeUnclassified(boolean value) { includeUnclassified = value; return this; }
        public Builder useUmapMembership(boolean value) { useUmapMembership = value; return this; }
        public Builder minimumUmapMembership(double value) {
            if (!Double.isFinite(value) || value < 0 || value > 1)
                throw new IllegalArgumentException("UMAP membership threshold must be in [0, 1]");
            minimumUmapMembership = value;
            return this;
        }
        public Builder defaultWeight(double value) { defaultWeight = positive(value); return this; }
        public Builder familyWeight(GraphPredicateFamily family, double weight) {
            familyWeights.put(required(family, "family"), positive(weight)); return this;
        }
        public Builder predicateWeight(String id, double weight) {
            predicateWeights.put(nonblank(id, "predicateId"), positive(weight)); return this;
        }
        public Builder originMultiplier(GraphAssertionOrigin origin, double factor) {
            originMultipliers.put(required(origin, "origin"), positive(factor)); return this;
        }
        public GraphViewSpecification build() { return new GraphViewSpecification(this); }

        private static <T> T required(T value, String name) {
            if (value == null) throw new IllegalArgumentException(name + " is required");
            return value;
        }
        private static String nonblank(String value, String name) {
            if (value == null || value.isBlank())
                throw new IllegalArgumentException(name + " must be nonblank");
            return value;
        }
        private static double positive(double value) {
            if (!Double.isFinite(value) || value <= 0 || value > 1_000_000)
                throw new IllegalArgumentException("Weights/multipliers must be finite in (0, 1000000]");
            return value;
        }
    }
}
