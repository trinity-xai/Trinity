package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphNode;
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

/** Immutable, deterministic human-readable summaries, indexed by hierarchy level and ID. */
public final class ContextGraphCommunityCatalog {
    private final List<List<ContextGraphCommunityDescriptor>> levels;
    private final ContextGraphCommunityHierarchy hierarchy;

    private ContextGraphCommunityCatalog(ContextGraphCommunityHierarchy hierarchy,
                                         List<List<ContextGraphCommunityDescriptor>> levels) {
        this.hierarchy = hierarchy;
        this.levels = List.copyOf(levels);
    }

    public ContextGraphCommunityDescriptor descriptor(ContextGraphCommunityRef ref) {
        if (ref == null || ref.level() >= levels.size() || ref.id() >= levels.get(ref.level()).size())
            return null;
        return levels.get(ref.level()).get(ref.id());
    }

    public List<ContextGraphCommunityDescriptor> atLevel(int level) {
        return levels.get(level);
    }

    public Set<String> members(ContextGraphCommunityRef ref) {
        if (descriptor(ref) == null) return Set.of();
        ContextGraphCommunityLevel partition = hierarchy.levels().get(ref.level());
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < hierarchy.entityIds().size(); i++)
            if (partition.communityOf(i) == ref.id()) ids.add(hierarchy.entityIds().get(i));
        return Collections.unmodifiableSet(ids);
    }

    public static ContextGraphCommunityCatalog build(GraphDirectedCollection graph,
                                                    ContextGraphCommunityHierarchy hierarchy) {
        if (graph == null || hierarchy == null) throw new IllegalArgumentException("Graph and hierarchy required");
        Map<String, GraphNode> byId = new HashMap<>();
        for (GraphNode node : graph.getNodes()) byId.put(node.getEntityID(), node);
        List<List<ContextGraphCommunityDescriptor>> descriptions = new ArrayList<>();
        List<String> ids = hierarchy.entityIds();
        for (ContextGraphCommunityLevel level : hierarchy.levels()) {
            if (Thread.currentThread().isInterrupted())
                throw new CancellationException("Community catalog cancelled");
            List<Map<String, Integer>> labelCounts = new ArrayList<>();
            List<List<String>> exampleNames = new ArrayList<>();
            for (int i = 0; i < level.count(); i++) {
                labelCounts.add(new TreeMap<>());
                exampleNames.add(new ArrayList<>());
            }
            for (int i = 0; i < ids.size(); i++) {
                if ((i & 255) == 0 && Thread.currentThread().isInterrupted())
                    throw new CancellationException("Community catalog cancelled");
                GraphNode node = byId.get(ids.get(i));
                if (node == null) continue;
                int group = level.communityOf(i);
                List<String> labels = node.getLabels();
                String label = labels == null || labels.isEmpty() || labels.get(0) == null
                    || labels.get(0).isBlank() ? "Uncategorized" : labels.get(0);
                labelCounts.get(group).merge(label, 1, Integer::sum);
                Object name = node.getProperties() == null ? null : node.getProperties().get("name");
                if (name == null && node.getProperties() != null) name = node.getProperties().get("title");
                if (name != null && !name.toString().isBlank()) exampleNames.get(group).add(name.toString());
            }
            List<ContextGraphCommunityDescriptor> descriptionsAtLevel = new ArrayList<>();
            for (ContextGraphCommunityCluster cluster : level.clusters()) {
                Map<String, Integer> labels = labelCounts.get(cluster.id());
                List<Map.Entry<String, Integer>> ranked = new ArrayList<>(labels.entrySet());
                ranked.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                    .reversed().thenComparing(Map.Entry::getKey));
                String title;
                if (ranked.isEmpty()) title = "Community " + (cluster.id() + 1);
                else {
                    title = ranked.get(0).getKey();
                    if (ranked.size() > 1 && ranked.get(1).getValue() >= ranked.get(0).getValue() / 3.0)
                        title += " / " + ranked.get(1).getKey();
                }
                List<String> examples = exampleNames.get(cluster.id());
                examples.sort(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()));
                List<String> distinct = new ArrayList<>();
                String previous = null;
                for (String value : examples) {
                    if (!value.equals(previous)) distinct.add(value);
                    previous = value;
                    if (distinct.size() == 5) break;
                }
                descriptionsAtLevel.add(new ContextGraphCommunityDescriptor(
                    new ContextGraphCommunityRef(level.index(), cluster.id()), cluster.parentId(),
                    cluster.childIds(), cluster.memberCount(), title,
                    new LinkedHashMap<>(labels), distinct));
            }
            descriptions.add(List.copyOf(descriptionsAtLevel));
        }
        return new ContextGraphCommunityCatalog(hierarchy, descriptions);
    }
}
