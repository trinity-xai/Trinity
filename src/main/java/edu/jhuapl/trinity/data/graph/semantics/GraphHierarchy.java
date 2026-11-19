package edu.jhuapl.trinity.data.graph.semantics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/**
 * Immutable, predicate-specific and scope-specific view of the source graph.
 * All relationships are normalized child -> parent for navigation; original
 * GraphEdges and their directed endpoints are never changed. A hierarchy is
 * not necessarily a tree or DAG: cycles remain inspectable for diagnostics.
 *
 * Ancestor/descendant ordering is BFS (increasing hop count, then lexical
 * neighbor order). Simple paths are enumerated in lexical depth-first order,
 * with mandatory limits to prevent runaway output on dense polyhierarchies.
 */
public final class GraphHierarchy {
    private static final BooleanSupplier NEVER_CANCELLED = () -> false;
    private static final int DEFAULT_MAX_PATH_EXPANSIONS = 500_000;
    private final GraphHierarchyKey key;
    private final GraphHierarchyOrientation orientation;
    private final boolean acyclicRequired;
    private final boolean multipleParentsAllowed;
    private final Map<String, List<String>> parents;
    private final Map<String, List<String>> children;
    private final Map<String, List<GraphHierarchyLink>> parentLinks;
    private final Map<String, List<GraphHierarchyLink>> childLinks;
    private final List<GraphHierarchyLink> links;
    private final List<List<String>> cycles;

    GraphHierarchy(GraphHierarchyKey key, GraphPredicateDefinition definition,
                   Map<String, List<String>> parents, Map<String, List<String>> children,
                   Map<String, List<GraphHierarchyLink>> parentLinks,
                   Map<String, List<GraphHierarchyLink>> childLinks,
                   List<GraphHierarchyLink> links, BooleanSupplier cancelled) {
        this.key = key;
        this.orientation = definition.getHierarchyOrientation();
        this.acyclicRequired = Boolean.TRUE.equals(definition.getAcyclic());
        this.multipleParentsAllowed = !Boolean.FALSE.equals(definition.getAllowsMultipleParents());
        this.parents = frozen(parents);
        this.children = frozen(children);
        this.parentLinks = frozen(parentLinks);
        this.childLinks = frozen(childLinks);
        this.links = List.copyOf(links);
        this.cycles = cycleComponents(this.parents, this.children, cancelled);
    }

    private static <T> Map<String, List<T>> frozen(Map<String, List<T>> source) {
        Map<String, List<T>> copy = new LinkedHashMap<>();
        source.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(entry -> copy.put(entry.getKey(), List.copyOf(entry.getValue())));
        return Collections.unmodifiableMap(copy);
    }

    public GraphHierarchyKey key() { return key; }
    public GraphHierarchyOrientation orientation() { return orientation; }
    public boolean acyclicRequired() { return acyclicRequired; }
    public boolean multipleParentsAllowed() { return multipleParentsAllowed; }
    public int nodeCount() { return parents.size(); }
    public int edgeCount() { return links.size(); }
    public boolean containsNode(String nodeId) { return parents.containsKey(nodeId); }
    public Map<String, List<String>> parentsByNode() { return parents; }
    public Map<String, List<String>> childrenByNode() { return children; }
    public List<GraphHierarchyLink> links() { return links; }
    public List<List<String>> cycleComponents() { return cycles; }
    public boolean containsCycles() { return !cycles.isEmpty(); }

    public List<String> immediateParents(String childId) {
        return parents.getOrDefault(childId, List.of());
    }

    public List<String> immediateChildren(String parentId) {
        return children.getOrDefault(parentId, List.of());
    }

    public List<GraphHierarchyLink> parentLinks(String childId) {
        return parentLinks.getOrDefault(childId, List.of());
    }

    public List<GraphHierarchyLink> childLinks(String parentId) {
        return childLinks.getOrDefault(parentId, List.of());
    }

    /** Returns every independent assertion connecting the normalized endpoints. */
    public List<GraphHierarchyLink> linksBetween(String childId, String parentId) {
        if (childId == null || parentId == null) return List.of();
        return parentLinks(childId).stream().filter(link -> parentId.equals(link.parentId())).toList();
    }

    public List<String> roots() {
        return parents.keySet().stream().filter(n -> immediateParents(n).isEmpty()).toList();
    }

    public List<String> leaves() {
        return children.keySet().stream().filter(n -> immediateChildren(n).isEmpty()).toList();
    }

    public List<String> ancestors(String childId) {
        return ancestors(childId, Integer.MAX_VALUE, NEVER_CANCELLED);
    }

    public List<String> ancestors(String childId, int maxDepth, BooleanSupplier cancelled) {
        return reachable(childId, parents, maxDepth, cancelled);
    }

    public List<String> descendants(String parentId) {
        return descendants(parentId, Integer.MAX_VALUE, NEVER_CANCELLED);
    }

    public List<String> descendants(String parentId, int maxDepth, BooleanSupplier cancelled) {
        return reachable(parentId, children, maxDepth, cancelled);
    }

    private static List<String> reachable(String startId, Map<String, List<String>> adjacency,
                                          int maxDepth, BooleanSupplier cancelled) {
        if (maxDepth < 0) throw new IllegalArgumentException("maxDepth must be >= 0");
        checkCancelled(cancelled);
        if (startId == null || !adjacency.containsKey(startId) || maxDepth == 0) return List.of();
        record Visit(String id, int depth) { }
        ArrayDeque<Visit> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        List<String> result = new ArrayList<>();
        visited.add(startId);
        queue.addLast(new Visit(startId, 0));
        int work = 0;
        while (!queue.isEmpty()) {
            if ((++work & 255) == 0) checkCancelled(cancelled);
            Visit item = queue.removeFirst();
            if (item.depth() >= maxDepth) continue;
            for (String next : adjacency.getOrDefault(item.id(), List.of())) {
                if ((++work & 255) == 0) checkCancelled(cancelled);
                if (visited.add(next)) {
                    result.add(next);
                    queue.addLast(new Visit(next, item.depth() + 1));
                }
            }
        }
        checkCancelled(cancelled);
        return List.copyOf(result);
    }

    /** Shortest node path in normalized child -> parent direction, inclusive. */
    public List<String> shortestPathToAncestor(String childId, String ancestorId) {
        return shortestPathToAncestor(childId, ancestorId, NEVER_CANCELLED);
    }

    public List<String> shortestPathToAncestor(String childId, String ancestorId,
                                               BooleanSupplier cancelled) {
        checkCancelled(cancelled);
        if (childId == null || ancestorId == null || !containsNode(childId)
            || !containsNode(ancestorId)) return List.of();
        if (childId.equals(ancestorId)) return List.of(childId);
        ArrayDeque<String> queue = new ArrayDeque<>();
        Map<String, String> predecessor = new HashMap<>();
        predecessor.put(childId, null);
        queue.addLast(childId);
        int work = 0;
        while (!queue.isEmpty()) {
            if ((++work & 255) == 0) checkCancelled(cancelled);
            String current = queue.removeFirst();
            for (String next : immediateParents(current)) {
                if ((++work & 255) == 0) checkCancelled(cancelled);
                if (predecessor.containsKey(next)) continue;
                predecessor.put(next, current);
                if (next.equals(ancestorId)) {
                    ArrayList<String> path = new ArrayList<>();
                    String cursor = next;
                    while (cursor != null) {
                        path.add(cursor);
                        cursor = predecessor.get(cursor);
                    }
                    Collections.reverse(path);
                    checkCancelled(cancelled);
                    return List.copyOf(path);
                }
                queue.addLast(next);
            }
        }
        checkCancelled(cancelled);
        return List.of();
    }

    /**
     * Enumerates at most maxPaths simple node paths, up to maxDepth edges, in
     * child -> parent order. Parallel assertions do not duplicate node paths;
     * use linksBetween() to inspect each distinct source assertion. If more
     * than the exploration budget is required, throws IllegalStateException
     * instead of silently returning an incomplete set of paths.
     */
    public List<List<String>> pathsToAncestor(String childId, String ancestorId,
                                              int maxPaths, int maxDepth,
                                              BooleanSupplier cancelled) {
        return pathsToAncestor(childId, ancestorId, maxPaths, maxDepth,
            DEFAULT_MAX_PATH_EXPANSIONS, cancelled);
    }

    /** maxExploredStates caps work even when no requested path can be found. */
    public List<List<String>> pathsToAncestor(String childId, String ancestorId,
                                              int maxPaths, int maxDepth,
                                              int maxExploredStates,
                                              BooleanSupplier cancelled) {
        if (maxPaths < 1 || maxDepth < 0 || maxExploredStates < 1) {
            throw new IllegalArgumentException("maxPaths and maxExploredStates must be > 0; maxDepth >= 0");
        }
        checkCancelled(cancelled);
        if (childId == null || ancestorId == null || !containsNode(childId)
            || !containsNode(ancestorId)) return List.of();
        class Frame {
            final String node;
            int next;
            Frame(String node) { this.node = node; }
        }
        ArrayDeque<Frame> stack = new ArrayDeque<>();
        ArrayList<String> path = new ArrayList<>();
        Set<String> onPath = new HashSet<>();
        List<List<String>> found = new ArrayList<>();
        path.add(childId);
        onPath.add(childId);
        stack.push(new Frame(childId));
        int work = 0;
        while (!stack.isEmpty() && found.size() < maxPaths) {
            if (++work > maxExploredStates) {
                throw new IllegalStateException("Hierarchy path exploration budget exceeded: "
                    + maxExploredStates + " states");
            }
            if ((work & 255) == 0) checkCancelled(cancelled);
            Frame frame = stack.peek();
            if (frame.node.equals(ancestorId)) {
                found.add(List.copyOf(path));
            }
            List<String> nextNodes = immediateParents(frame.node);
            if (frame.node.equals(ancestorId) || path.size() - 1 >= maxDepth
                || frame.next >= nextNodes.size()) {
                stack.pop();
                onPath.remove(frame.node);
                path.remove(path.size() - 1);
            } else {
                String next = nextNodes.get(frame.next++);
                if (onPath.add(next)) {
                    path.add(next);
                    stack.push(new Frame(next));
                }
            }
        }
        checkCancelled(cancelled);
        return List.copyOf(found);
    }

    public List<List<String>> pathsToAncestor(String childId, String ancestorId,
                                              int maxPaths, int maxDepth) {
        return pathsToAncestor(childId, ancestorId, maxPaths, maxDepth, NEVER_CANCELLED);
    }

    /** Iterative Kosaraju SCCs: reports only cycles, including singleton self-loops. */
    private static List<List<String>> cycleComponents(Map<String, List<String>> parents,
                                                       Map<String, List<String>> children,
                                                       BooleanSupplier cancelled) {
        checkCancelled(cancelled);
        class Frame {
            final String id;
            int next;
            Frame(String id) { this.id = id; }
        }
        Set<String> visited = new HashSet<>();
        List<String> finish = new ArrayList<>(parents.size());
        ArrayDeque<Frame> stack = new ArrayDeque<>();
        int work = 0;
        for (String start : parents.keySet()) {
            if (!visited.add(start)) continue;
            stack.push(new Frame(start));
            while (!stack.isEmpty()) {
                if ((++work & 255) == 0) checkCancelled(cancelled);
                Frame top = stack.peek();
                List<String> nextNodes = parents.getOrDefault(top.id, List.of());
                if (top.next < nextNodes.size()) {
                    String next = nextNodes.get(top.next++);
                    if (visited.add(next)) stack.push(new Frame(next));
                } else {
                    finish.add(top.id);
                    stack.pop();
                }
            }
        }
        visited.clear();
        List<List<String>> cycles = new ArrayList<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        for (int i = finish.size() - 1; i >= 0; i--) {
            String start = finish.get(i);
            if (!visited.add(start)) continue;
            ArrayList<String> component = new ArrayList<>();
            queue.addLast(start);
            while (!queue.isEmpty()) {
                if ((++work & 255) == 0) checkCancelled(cancelled);
                String current = queue.removeLast();
                component.add(current);
                for (String next : children.getOrDefault(current, List.of())) {
                    if (visited.add(next)) queue.addLast(next);
                }
            }
            if (component.size() > 1 || parents.getOrDefault(start, List.of()).contains(start)) {
                component.sort(Comparator.naturalOrder());
                cycles.add(List.copyOf(component));
            }
        }
        cycles.sort(Comparator.comparing(c -> c.getFirst()));
        checkCancelled(cancelled);
        return List.copyOf(cycles);
    }

    static void checkCancelled(BooleanSupplier cancelled) {
        if (Thread.currentThread().isInterrupted()
            || (cancelled != null && cancelled.getAsBoolean())) {
            throw new CancellationException("Hierarchy analysis cancelled");
        }
    }
}
