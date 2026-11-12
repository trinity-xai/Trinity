package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Computes a three-dimensional Poincare-ball hierarchy FROM GRAPH TOPOLOGY ONLY.
 * GraphNode.vector, properties.poincare, properties.layoutParent, and other external
 * position/layout metadata are deliberately never inspected.
 *
 * A minimum-cost spanning forest prefers directed containment/classification
 * relationships where they exist; every remaining vertex is reached through
 * real graph edges. Non-tree cross-links are retained by GraphDirectedCollection.
 * The layout is a navigable hierarchy scaffold, not an optimized embedding or
 * a claim that spatial proximity proves semantic similarity.
 */
public final class ContextGraphHyperbolicLayout {
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
    private static final int STRUCTURAL_FORWARD = 10;
    private static final int STRUCTURAL_REVERSE = 35;
    private static final int ASSOCIATION_FORWARD = 48;
    private static final int ASSOCIATION_REVERSE = 72;
    private static final int SEMANTIC_COST = 110;
    private static final int MAX_ROOT_HEIGHT_ROUNDS = 12;
    private static final double MAX_POINCARE_NORM = 0.985;

    private ContextGraphHyperbolicLayout() { }

    private record Arc(int to, int cost, GraphEdge edge) { }
    private record Candidate(int vertex, long cost, int hops) { }
    private record OrientedLink(int parent, int child) { }

    /** Existing position-only API retained for any layout callers. */
    public static Map<String, double[]> computed(GraphDirectedCollection graph) {
        return computedLayout(graph).positions();
    }

    /**
     * Computed coordinates and EXACT GraphEdge objects selected as tree edges.
     * The graph model and edge identities are never copied or modified.
     */
    public record LayoutResult(Map<String, double[]> positions, Set<GraphEdge> backboneEdges,
                               Map<String, Integer> communities, int communityCount) { }

    /**
     * Original 4B-A Poincare layout: original single-level weighted detector,
     * spanning hierarchy and spherical-sector allocation. This is the default
     * and is intentionally independent of the optional multilevel detector.
     */
    public static LayoutResult computedLayout(GraphDirectedCollection graph) {
        return originalStructuralLayout(graph);
    }

    public static LayoutResult originalStructuralLayout(GraphDirectedCollection graph) {
        return computeInternal(graph, null, true);
    }

    /** Reuse a computed multilevel hierarchy for the experimental layout. */
    public static LayoutResult computedLayout(GraphDirectedCollection graph,
                                               ContextGraphCommunityHierarchy hierarchy) {
        return computeInternal(graph, hierarchy, false);
    }

    private static LayoutResult computeInternal(GraphDirectedCollection graph,
            ContextGraphCommunityHierarchy hierarchy, boolean originalDetector) {
        if (graph == null || graph.getNodes() == null) {
            throw new IllegalArgumentException("Graph and graph nodes are required");
        }
        List<GraphNode> nodes = new ArrayList<>(graph.getNodes());
        nodes.sort(Comparator.comparing(GraphNode::getEntityID));
        int n = nodes.size();
        if (n == 0) return new LayoutResult(Map.of(), Set.of(), Map.of(), 0);

        Map<String, Integer> index = new HashMap<>(n * 2);
        for (int i = 0; i < n; i++) {
            String id = nodes.get(i).getEntityID();
            if (id == null || id.isBlank() || index.putIfAbsent(id, i) != null) {
                throw new IllegalArgumentException("Every graph entity must have a unique nonblank ID");
            }
        }
        int[] community;
        int communityCount;
        if (originalDetector) {
            ContextGraphSemanticCommunities.Result semantic =
                ContextGraphSemanticCommunities.detect(nodes, graph.getEdges(), index);
            community = semantic.membership();
            communityCount = semantic.count();
        } else {
            if (hierarchy == null || !hierarchy.entityIds().equals(
                nodes.stream().map(GraphNode::getEntityID).toList())) {
                throw new IllegalArgumentException("Hierarchy does not match graph entity identities");
            }
            ContextGraphCommunityLevel activeLevel =
                hierarchy.levels().get(hierarchy.selectedLayoutLevel());
            community = activeLevel.membership();
            communityCount = activeLevel.count();
        }
        List<List<Arc>> adjacency = new ArrayList<>(n);
        for (int i = 0; i < n; i++) adjacency.add(new ArrayList<>());
        List<OrientedLink> structuralLinks = new ArrayList<>();
        int[] structuralIncoming = new int[n];
        int[] structuralOutgoing = new int[n];
        int[] degree = new int[n];
        int[] union = new int[n];
        for (int i = 0; i < n; i++) union[i] = i;

        if (graph.getEdges() != null) {
            for (GraphEdge edge : graph.getEdges()) {
                checkCancelled();
                if (edge == null) continue;
                Integer start = index.get(edge.getStartID());
                Integer end = index.get(edge.getEndID());
                if (start == null || end == null || start.equals(end)) continue;
                merge(union, start, end);
                degree[start]++;
                degree[end]++;
                // Direction is taken only from the actual relationship label.
                // No source position or imported hierarchy annotations are used.
                int orientation = orientation(edge.getRelationship());
                if (orientation == 0) {
                    adjacency.get(start).add(new Arc(end, SEMANTIC_COST, edge));
                    adjacency.get(end).add(new Arc(start, SEMANTIC_COST, edge));
                } else {
                    int sourceParent = orientation > 0 ? start : end;
                    int targetChild = orientation > 0 ? end : start;
                    int forward = isAssociation(edge.getRelationship())
                        ? ASSOCIATION_FORWARD : STRUCTURAL_FORWARD;
                    int reverse = isAssociation(edge.getRelationship())
                        ? ASSOCIATION_REVERSE : STRUCTURAL_REVERSE;
                    adjacency.get(sourceParent).add(new Arc(targetChild, forward, edge));
                    adjacency.get(targetChild).add(new Arc(sourceParent, reverse, edge));
                    if (!isAssociation(edge.getRelationship())) {
                        structuralLinks.add(new OrientedLink(sourceParent, targetChild));
                        structuralIncoming[targetChild]++;
                        structuralOutgoing[sourceParent]++;
                    }
                }
            }
        }
        for (List<Arc> neighbors : adjacency) {
            neighbors.sort(Comparator.comparingInt(Arc::cost).thenComparingInt(Arc::to)
                .thenComparingLong(a -> a.edge().getRelationship() == null ? 0L
                    : a.edge().getRelationship().getIdentity())
                .thenComparing(a -> a.edge().getRelationship() == null
                    || a.edge().getRelationship().getLabels() == null ? ""
                    : String.join(",", a.edge().getRelationship().getLabels())));
        }

        // A small, bounded longest-downstream-path heuristic favors genuine
        // hierarchy ancestors over high-degree semantic hubs. No recursion.
        int[] structuralHeight = new int[n];
        for (int pass = 0; pass < MAX_ROOT_HEIGHT_ROUNDS; pass++) {
            int[] previous = structuralHeight.clone();
            for (OrientedLink link : structuralLinks) {
                structuralHeight[link.parent] = Math.max(structuralHeight[link.parent],
                    1 + previous[link.child]);
            }
        }

        Map<Integer, List<Integer>> components = new HashMap<>();
        for (int i = 0; i < n; i++) {
            components.computeIfAbsent(find(union, i), k -> new ArrayList<>()).add(i);
        }
        List<Integer> roots = new ArrayList<>();
        for (List<Integer> component : components.values()) {
            component.sort(Comparator
                .comparingInt((Integer i) -> structuralIncoming[i] == 0 ? 0 : 1)
                .thenComparingInt((Integer i) -> -structuralHeight[i])
                .thenComparingInt((Integer i) -> -structuralOutgoing[i])
                .thenComparingInt((Integer i) -> -degree[i])
                .thenComparingInt(i -> i));
            roots.add(component.get(0));
        }
        roots.sort(Integer::compareTo);

        int[] parent = new int[n];
        GraphEdge[] parentEdges = new GraphEdge[n];
        int[] hops = new int[n];
        long[] best = new long[n];
        Arrays.fill(parent, -1);
        Arrays.fill(hops, Integer.MAX_VALUE);
        Arrays.fill(best, Long.MAX_VALUE);
        PriorityQueue<Candidate> queue = new PriorityQueue<>(Comparator
            .comparingLong(Candidate::cost)
            .thenComparingInt(Candidate::hops)
            .thenComparingInt(Candidate::vertex));
        for (int root : roots) {
            best[root] = 0;
            hops[root] = 0;
            queue.add(new Candidate(root, 0, 0));
        }
        while (!queue.isEmpty()) {
            checkCancelled();
            Candidate next = queue.poll();
            if (next.cost != best[next.vertex] || next.hops != hops[next.vertex]) continue;
            for (Arc arc : adjacency.get(next.vertex)) {
                long cost = next.cost + arc.cost;
                int hop = next.hops + 1;
                if (cost < best[arc.to] || (cost == best[arc.to] && hop < hops[arc.to])) {
                    best[arc.to] = cost;
                    hops[arc.to] = hop;
                    parent[arc.to] = next.vertex;
                    parentEdges[arc.to] = arc.edge();
                    queue.add(new Candidate(arc.to, cost, hop));
                }
            }
        }

        List<List<Integer>> children = new ArrayList<>(n);
        for (int i = 0; i < n; i++) children.add(new ArrayList<>());
        for (int i = 0; i < n; i++) {
            if (parent[i] >= 0) children.get(parent[i]).add(i);
        }
        List<Integer> breadthOrder = new ArrayList<>(n);
        ArrayDeque<Integer> traversal = new ArrayDeque<>(roots);
        while (!traversal.isEmpty()) {
            int current = traversal.removeFirst();
            breadthOrder.add(current);
            for (int child : children.get(current)) traversal.addLast(child);
        }
        if (breadthOrder.size() != n) {
            throw new IllegalStateException("Graph topology did not produce a complete spanning forest");
        }
        int[] subtreeSize = new int[n];
        Arrays.fill(subtreeSize, 1);
        for (int i = breadthOrder.size() - 1; i >= 0; i--) {
            int current = breadthOrder.get(i);
            if (parent[current] >= 0) subtreeSize[parent[current]] += subtreeSize[current];
        }
        for (List<Integer> branch : children) {
            branch.sort(Comparator.comparingInt((Integer i) -> -subtreeSize[i]).thenComparingInt(i -> i));
        }

        // Determine each subtree's dominant semantic community bottom-up.
        // Small-to-large merging avoids quadratic cost in chains and star graphs.
        List<Map<Integer, Integer>> frequencies = new ArrayList<>(n);
        for (int i = 0; i < n; i++) frequencies.add(null);
        int[] subtreeCommunity = new int[n];
        for (int t = breadthOrder.size() - 1; t >= 0; t--) {
            int current = breadthOrder.get(t);
            Map<Integer, Integer> counts = new HashMap<>();
            counts.put(community[current], 1);
            for (int child : children.get(current)) {
                Map<Integer, Integer> childCounts = frequencies.get(child);
                if (childCounts.size() > counts.size()) {
                    Map<Integer, Integer> swap = counts;
                    counts = childCounts;
                    childCounts = swap;
                }
                for (Map.Entry<Integer, Integer> entry : childCounts.entrySet()) {
                    counts.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
                frequencies.set(child, null);
            }
            int dominant = community[current];
            int largest = -1;
            for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
                if (entry.getValue() > largest || (entry.getValue() == largest
                    && entry.getKey() < dominant)) {
                    largest = entry.getValue();
                    dominant = entry.getKey();
                }
            }
            subtreeCommunity[current] = dominant;
            frequencies.set(current, counts);
        }

        int maxDepth = 0;
        for (int hop : hops) maxDepth = Math.max(maxDepth, hop);
        double step = Math.min(1.45, 5.4 / Math.max(1, maxDepth));
        double[][] positions = new double[n][3];
        double[][] axes = new double[n][3];
        for (int i = 0; i < roots.size(); i++) {
            int root = roots.get(i);
            axes[root] = fibonacciDirection(i, roots.size());
            positions[root] = roots.size() == 1
                ? new double[]{0, 0, 0} : multiply(axes[root], 0.12);
        }
        for (int current : breadthOrder) {
            checkCancelled();
            List<Integer> branch = children.get(current);
            if (branch.isEmpty()) continue;
            double[] axis = axes[current];
            double[] auxiliary = Math.abs(axis[1]) < 0.9
                ? new double[]{0, 1, 0} : new double[]{1, 0, 0};
            double[] u = normalize(cross(axis, auxiliary));
            double[] v = normalize(cross(axis, u));

            // Group actual spanning children by their dominant semantic community.
            // This changes spatial sectors, never the source graph or tree edges.
            Map<Integer, List<Integer>> groups = new TreeMap<>();
            for (int child : branch) {
                groups.computeIfAbsent(subtreeCommunity[child], ignored -> new ArrayList<>())
                    .add(child);
            }
            Map<Integer, Long> sectorMass = new HashMap<>();
            for (Map.Entry<Integer, List<Integer>> group : groups.entrySet()) {
                long mass = 0;
                for (int child : group.getValue()) mass += subtreeSize[child];
                sectorMass.put(group.getKey(), mass);
            }
            List<Map.Entry<Integer, List<Integer>>> sectors = new ArrayList<>(groups.entrySet());
            sectors.sort(Comparator.comparingLong((Map.Entry<Integer, List<Integer>> e) ->
                -sectorMass.get(e.getKey())).thenComparingInt(Map.Entry::getKey));
            int sectorIndex = 0;
            for (Map.Entry<Integer, List<Integer>> sector : sectors) {
                List<Integer> members = sector.getValue();
                double[] groupAxis = parent[current] == -1 && roots.size() == 1
                    ? fibonacciDirection(sectorIndex, sectors.size())
                    : capDirection(axis, u, v, sectorIndex, sectors.size(), 1.48);
                double[] auxiliary2 = Math.abs(groupAxis[1]) < 0.9
                    ? new double[]{0, 1, 0} : new double[]{1, 0, 0};
                double[] groupU = normalize(cross(groupAxis, auxiliary2));
                double[] groupV = normalize(cross(groupAxis, groupU));
                for (int j = 0; j < members.size(); j++) {
                    int child = members.get(j);
                    double[] direction = members.size() == 1 ? groupAxis
                        : capDirection(groupAxis, groupU, groupV, j, members.size(),
                            Math.min(0.72, 0.25 + 0.12 * Math.log1p(members.size())));
                    double branching = 0.90 + 0.045 * Math.log1p(branch.size())
                        + 0.025 * Math.log1p(subtreeSize[child]);
                    double localNorm = Math.tanh(0.5 * step * branching);
                    double[] result = ContextGraphHyperbolicMath.add(
                        positions[current], multiply(direction, localNorm));
                    double norm = Math.sqrt(ContextGraphHyperbolicMath.dot(result, result));
                    if (norm > MAX_POINCARE_NORM)
                        result = multiply(result, MAX_POINCARE_NORM / norm);
                    positions[child] = result;
                    // Retain this branch's own orientation. Using the radial
                    // origin-to-position axis was causing the overlapping fans.
                    axes[child] = direction;
                }
                sectorIndex++;
            }
        }

        Map<String, double[]> output = new LinkedHashMap<>(n * 2);
        for (int i = 0; i < n; i++) output.put(nodes.get(i).getEntityID(), positions[i]);
        Set<GraphEdge> backbone = Collections.newSetFromMap(new IdentityHashMap<>());
        for (GraphEdge edge : parentEdges) if (edge != null) backbone.add(edge);
        Map<String, Integer> communityOutput = new LinkedHashMap<>(n * 2);
        for (int i = 0; i < n; i++) communityOutput.put(nodes.get(i).getEntityID(), community[i]);
        return new LayoutResult(Collections.unmodifiableMap(output),
            Collections.unmodifiableSet(backbone), Collections.unmodifiableMap(communityOutput),
            communityCount);
    }

    private static void checkCancelled() {
        if (Thread.currentThread().isInterrupted())
            throw new java.util.concurrent.CancellationException("Topology layout cancelled");
    }

    /** 1: source is parent; -1: target is parent; 0: not a hierarchy relationship. */
    private static int orientation(GraphRelationship relationship) {
        if (relationship == null || relationship.getLabels() == null) return 0;
        for (String raw : relationship.getLabels()) {
            if (raw == null) continue;
            String label = raw.toUpperCase(java.util.Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
            switch (label) {
                case "SUBTECHNIQUE_OF", "SUBCLASS_OF", "CHILD_OF", "PART_OF",
                    "IS_PART_OF", "BELONGS_TO", "MEMBER_OF" -> { return -1; }
                case "HAS_TACTIC", "CLASSIFIES_TECHNIQUE", "HAS_ANALYTIC",
                    "DATASET_SECTION", "CATALOG_VENDOR", "LISTS_PRODUCT",
                    "HAS_CWE_INDEX", "AFFECTED_BY_CVE", "HAS_CHILD",
                    "PARENT_OF", "CONTAINS", "HAS_SECTION", "HAS_PARAGRAPH",
                    "HAS_SENTENCE", "HAS_DOCUMENT" -> { return 1; }
                default -> {
                    if (label.startsWith("INDEXES_")) return 1;
                }
            }
        }
        return 0;
    }

    private static boolean isAssociation(GraphRelationship relationship) {
        return relationship != null && relationship.getLabels() != null
            && relationship.getLabels().stream().anyMatch(s ->
                s != null && "AFFECTED_BY_CVE".equalsIgnoreCase(s));
    }

    private static int find(int[] parents, int i) {
        int current = i;
        while (parents[current] != current) current = parents[current];
        while (parents[i] != i) {
            int next = parents[i];
            parents[i] = current;
            i = next;
        }
        return current;
    }

    private static void merge(int[] parents, int a, int b) {
        int x = find(parents, a);
        int y = find(parents, b);
        if (x != y) parents[Math.max(x, y)] = Math.min(x, y);
    }

    /** Evenly sample a spherical cap around a local branch axis. */
    private static double[] capDirection(double[] axis, double[] u, double[] v,
                                         int index, int count, double aperture) {
        double fraction = (index + 0.5) / Math.max(1.0, count);
        double theta = Math.acos(1.0 - fraction * (1.0 - Math.cos(aperture)));
        double azimuth = GOLDEN_ANGLE * index;
        double longitude = Math.cos(theta);
        double lateralU = Math.sin(theta) * Math.cos(azimuth);
        double lateralV = Math.sin(theta) * Math.sin(azimuth);
        return normalize(new double[]{axis[0] * longitude + u[0] * lateralU + v[0] * lateralV,
            axis[1] * longitude + u[1] * lateralU + v[1] * lateralV,
            axis[2] * longitude + u[2] * lateralU + v[2] * lateralV});
    }

    private static double[] fibonacciDirection(int index, int count) {
        double y = 1.0 - 2.0 * (index + 0.5) / Math.max(1, count);
        double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
        double azimuth = GOLDEN_ANGLE * index;
        return new double[]{Math.cos(azimuth) * r, y, Math.sin(azimuth) * r};
    }

    private static double[] multiply(double[] a, double f) {
        return new double[]{a[0] * f, a[1] * f, a[2] * f};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalize(double[] a) {
        double length = Math.sqrt(ContextGraphHyperbolicMath.dot(a, a));
        return length < 1.0e-12 ? new double[]{0, 0, 1} : multiply(a, 1.0 / length);
    }
}
