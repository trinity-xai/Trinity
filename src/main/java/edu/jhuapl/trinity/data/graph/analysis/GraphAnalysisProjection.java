package edu.jhuapl.trinity.data.graph.analysis;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import edu.jhuapl.trinity.data.graph.semantics.GraphAssertionOrigin;
import edu.jhuapl.trinity.data.graph.semantics.GraphPredicateDefinition;
import edu.jhuapl.trinity.data.graph.semantics.GraphPredicateFamily;
import edu.jhuapl.trinity.data.graph.semantics.GraphRelationshipSemantics;
import edu.jhuapl.trinity.data.graph.semantics.GraphSemanticCatalog;

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
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Read-only, detached weighted topology descriptors for ONE original graph.
 * Source GraphNode and GraphEdge objects are never modified. Explicit methods
 * expose references to original objects for picking/disclosure or for adapting
 * older layout algorithms; those references are NOT immutable or a deep copy.
 *
 * All filtered links (including directed, parallel and source-supported claims)
 * remain individually addressable. adjacency() is a deliberately UNDIRECTED
 * aggregate for community analysis, never a taxonomy or evidence query index.
 */
public final class GraphAnalysisProjection {
    public record Node(String entityId, String nodeKind, String sourceKey) { }
    public record Link(String edgeId, int sourceEdgeIndex, String startId, String endId,
                       String predicateId, GraphPredicateFamily family, GraphAssertionOrigin origin,
                       String hierarchyScopeId, boolean undirected, boolean interpretedLegacyLabel,
                       List<String> evidenceIds, double weight) {
        public Link { evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds); }
    }

    private record Selected(Link link, GraphEdge original) { }

    private static final Comparator<Selected> LINK_ORDER = Comparator
        .comparing((Selected s) -> s.link.startId())
        .thenComparing(s -> s.link.endId())
        .thenComparing(s -> s.link.predicateId(), Comparator.nullsLast(String::compareTo))
        .thenComparingDouble(s -> s.link.weight())
        .thenComparing(s -> s.link.edgeId(), Comparator.nullsLast(String::compareTo))
        .thenComparingInt(s -> s.link.sourceEdgeIndex());

    private final GraphDirectedCollection sourceGraph;
    private final GraphViewSpecification specification;
    private final List<Node> nodes;
    private final List<Link> links;
    private final List<GraphNode> originalNodes;
    private final List<GraphEdge> originalEdges;
    private final Map<String, Map<String, Double>> adjacency;
    private final int sourceNodeCount;
    private final int sourceEdgeCount;

    private GraphAnalysisProjection(GraphDirectedCollection sourceGraph,
                                    GraphViewSpecification specification,
                                    List<Node> nodes, List<Link> links,
                                    List<GraphNode> originalNodes, List<GraphEdge> originalEdges,
                                    Map<String, Map<String, Double>> adjacency,
                                    int sourceNodeCount, int sourceEdgeCount) {
        this.sourceGraph = sourceGraph;
        this.specification = specification;
        this.nodes = List.copyOf(nodes);
        this.links = List.copyOf(links);
        this.originalNodes = List.copyOf(originalNodes);
        this.originalEdges = List.copyOf(originalEdges);
        this.adjacency = adjacency;
        this.sourceNodeCount = sourceNodeCount;
        this.sourceEdgeCount = sourceEdgeCount;
    }

    public static GraphAnalysisProjection build(GraphDirectedCollection graph,
                                                GraphViewSpecification specification) {
        return build(graph, specification, () -> false);
    }

    public static GraphAnalysisProjection build(GraphDirectedCollection graph,
                                                GraphViewSpecification specification,
                                                BooleanSupplier cancelled) {
        if (graph == null || specification == null)
            throw new IllegalArgumentException("Source graph and view specification are required");
        checkCancelled(cancelled);
        List<GraphNode> graphNodes = graph.getNodes();
        List<GraphEdge> graphEdges = graph.getEdges();
        if (graphNodes == null || graphEdges == null)
            throw new IllegalArgumentException("Graph must have node and edge lists");
        GraphSemanticCatalog catalog = GraphSemanticCatalog.forGraph(graph);

        Map<String, GraphNode> eligible = new TreeMap<>();
        Set<String> allIds = new HashSet<>();
        for (int i = 0; i < graphNodes.size(); i++) {
            if ((i & 255) == 0) checkCancelled(cancelled);
            GraphNode node = graphNodes.get(i);
            if (node == null || blank(node.getEntityID()) || !allIds.add(node.getEntityID()))
                throw new IllegalArgumentException("Invalid or duplicate source node ID at index " + i);
            if (eligibleKind(node.getNodeKind(), specification))
                eligible.put(node.getEntityID(), node);
        }

        List<Selected> selected = new ArrayList<>();
        Set<String> incident = new HashSet<>();
        for (int i = 0; i < graphEdges.size(); i++) {
            if ((i & 255) == 0) checkCancelled(cancelled);
            GraphEdge edge = graphEdges.get(i);
            if (edge == null) throw new IllegalArgumentException("Null source edge at index " + i);
            String start = edge.getStartID(), end = edge.getEndID();
            if (!allIds.contains(start) || !allIds.contains(end))
                throw new IllegalArgumentException("Unresolved source edge endpoints at index " + i);
            if (!eligible.containsKey(start) || !eligible.containsKey(end)) continue;

            GraphRelationship relationship = edge.getRelationship();
            GraphRelationshipSemantics semantics = relationship == null ? null : relationship.getSemantics();
            if (semantics == null && !specification.includeKnownLegacyLabels()
                && !specification.includeUnclassified()) continue;
            GraphSemanticCatalog.Resolution resolved = catalog.resolve(relationship);
            boolean legacy = semantics == null;
            String predicate = resolved.predicateId();
            GraphPredicateDefinition definition = resolved.definition();
            GraphPredicateFamily family = definition == null ? GraphPredicateFamily.UNCLASSIFIED
                : definition.getFamily();
            GraphAssertionOrigin origin = resolved.origin();
            if (legacy && !specification.includeKnownLegacyLabels()) {
                predicate = null;
                family = GraphPredicateFamily.UNCLASSIFIED;
                origin = null;
                definition = null;
            }
            if (family == GraphPredicateFamily.UNCLASSIFIED && !specification.includeUnclassified()) continue;
            if (!specification.predicateIds().isEmpty()
                && (predicate == null || !specification.predicateIds().contains(predicate))) continue;
            if (!specification.families().isEmpty() && !specification.families().contains(family)) continue;
            if (!specification.origins().isEmpty() && !specification.origins().contains(origin)) continue;
            String scope = semantics == null ? null : semantics.getHierarchyScopeId();
            if (specification.scopeFilterEnabled() && !specification.hierarchyScopeIds().contains(scope))
                continue;

            double weight = specification.weight(predicate, family, origin);
            if ("UMAP_FUZZY_ASSOCIATION".equals(predicate)) {
                Object raw = relationship.getProperties() == null ? null
                    : relationship.getProperties().get("membership");
                if (!(raw instanceof Number number) || !Double.isFinite(number.doubleValue())
                    || number.doubleValue() < 0 || number.doubleValue() > 1)
                    throw new IllegalArgumentException("Invalid UMAP membership on source edge " + i);
                double membership = number.doubleValue();
                if (membership < specification.minimumUmapMembership()) continue;
                if (specification.useUmapMembership()) weight *= membership;
            }
            if (weight == 0.0) continue; // zero membership contributes no link
            if (!Double.isFinite(weight) || weight < 0)
                throw new IllegalArgumentException("Invalid effective analysis weight on edge " + i);
            List<String> evidence = semantics == null ? List.of() : semantics.getEvidenceIds();
            boolean undirected = definition != null && Boolean.TRUE.equals(definition.getSymmetric());
            if (!undirected && relationship != null && relationship.getProperties() != null)
                undirected = Boolean.TRUE.equals(relationship.getProperties().get("undirected"));
            Link link = new Link(edge.getEdgeId(), i, start, end, predicate, family, origin, scope,
                undirected, legacy, evidence, weight);
            selected.add(new Selected(link, edge));
            incident.add(start);
            incident.add(end);
        }
        selected.sort(LINK_ORDER);
        checkCancelled(cancelled);

        List<Node> nodes = new ArrayList<>();
        List<GraphNode> originalNodes = new ArrayList<>();
        for (GraphNode node : eligible.values()) {
            if (!specification.retainIsolatedNodes() && !incident.contains(node.getEntityID())) continue;
            originalNodes.add(node);
            nodes.add(new Node(node.getEntityID(), node.getNodeKind(), sourceKey(node)));
        }
        List<Link> links = new ArrayList<>(selected.size());
        List<GraphEdge> originals = new ArrayList<>(selected.size());
        for (Selected s : selected) {
            links.add(s.link());
            originals.add(s.original());
        }
        return new GraphAnalysisProjection(graph, specification, nodes, links,
            originalNodes, originals, buildAdjacency(nodes, links, cancelled),
            graphNodes.size(), graphEdges.size());
    }

    public GraphViewSpecification specification() { return specification; }
    public List<Node> nodes() { return nodes; }
    public List<Link> links() { return links; }
    public Map<String, Map<String, Double>> adjacency() { return adjacency; }
    public int sourceNodeCount() { return sourceNodeCount; }
    public int sourceEdgeCount() { return sourceEdgeCount; }
    public GraphDirectedCollection sourceGraph() { return sourceGraph; }

    /** Actual GraphEdge references, preserving scene pick identity. Do not mutate them. */
    public List<GraphEdge> selectedOriginalEdges() { return originalEdges; }

    /**
     * A NEW graph container, containing ORIGINAL model object references.
     * For existing layout adapters only; it is not a deep copy or an immutable
     * graph and must not be used for source editing or as canonical storage.
     */
    public GraphDirectedCollection toSourceEdgeView() {
        GraphDirectedCollection view = new GraphDirectedCollection();
        view.setGraphId(sourceGraph.getGraphId());
        view.setSchemaVersion(sourceGraph.getSchemaVersion());
        view.setRunProvenance(sourceGraph.getRunProvenance());
        view.setPredicateDefinitions(sourceGraph.getPredicateDefinitions());
        view.setEvidenceRecords(sourceGraph.getEvidenceRecords());
        view.setDefaultNodeColor(sourceGraph.getDefaultNodeColor());
        view.setDefaultEdgeColor(sourceGraph.getDefaultEdgeColor());
        view.setNodes(new ArrayList<>(originalNodes));
        view.setEdges(new ArrayList<>(originalEdges));
        return view;
    }

    private static boolean eligibleKind(String kind, GraphViewSpecification spec) {
        if (spec.nodeKinds().isEmpty()) return true;
        if (blank(kind)) return spec.includeUnspecifiedNodeKinds();
        return spec.nodeKinds().contains(kind.trim().toUpperCase(java.util.Locale.ROOT));
    }

    private static Map<String, Map<String, Double>> buildAdjacency(List<Node> nodes,
                                                                   List<Link> links,
                                                                   BooleanSupplier cancelled) {
        Map<String, Map<String, Double>> neighbors = new TreeMap<>();
        for (Node node : nodes) neighbors.put(node.entityId(), new TreeMap<>());
        // Weighted links have a stable ordering before accumulation; parallel
        // assertions are still individually retained in links().
        for (int i = 0; i < links.size(); i++) {
            if ((i & 255) == 0) checkCancelled(cancelled);
            Link link = links.get(i);
            if (link.startId().equals(link.endId())) continue;
            neighbors.get(link.startId()).merge(link.endId(), link.weight(), Double::sum);
            neighbors.get(link.endId()).merge(link.startId(), link.weight(), Double::sum);
        }
        Map<String, Map<String, Double>> snapshot = new LinkedHashMap<>();
        for (var entry : neighbors.entrySet()) {
            for (double weight : entry.getValue().values()) {
                if (!Double.isFinite(weight))
                    throw new IllegalArgumentException("Weighted adjacency overflow");
            }
            snapshot.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(snapshot);
    }

    private static String sourceKey(GraphNode node) {
        if (node.getProperties() == null) return null;
        for (String key : List.of("sourceDataset", "source_dataset", "dataset", "source")) {
            Object value = node.getProperties().get(key);
            if (value instanceof String text && !text.isBlank()) return text.trim();
        }
        return null;
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void checkCancelled(BooleanSupplier cancelled) {
        if ((cancelled != null && cancelled.getAsBoolean()) || Thread.currentThread().isInterrupted())
            throw new CancellationException("Graph analysis projection cancelled");
    }
}
