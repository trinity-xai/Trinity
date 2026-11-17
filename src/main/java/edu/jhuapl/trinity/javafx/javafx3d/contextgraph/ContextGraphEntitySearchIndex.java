package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fast, deterministic lookup by entity name, aliases, IDs, and category.
 * The search index contains model data only: no scene objects or userData.
 */
public final class ContextGraphEntitySearchIndex {
    private final List<ContextGraphEntitySearchResult> entries;
    private final Map<String, String> searchableText;

    public ContextGraphEntitySearchIndex(GraphDirectedCollection graph) {
        List<ContextGraphEntitySearchResult> found = new ArrayList<>();
        Map<String, String> text = new HashMap<>();
        if (graph != null && graph.getNodes() != null) {
            for (GraphNode node : graph.getNodes()) {
                if (node == null || node.getEntityID() == null || node.getEntityID().isBlank()) continue;
                Map<String, Object> properties = node.getProperties();
                String name = property(properties, "name");
                if (name.isBlank()) name = property(properties, "displayName");
                if (name.isBlank()) name = property(properties, "title");
                if (name.isBlank()) name = node.getEntityID();
                String category = node.getLabels() == null || node.getLabels().isEmpty()
                    ? "Uncategorized" : String.join(", ", node.getLabels());
                String externalId = property(properties, "externalId");
                List<String> aliases = new ArrayList<>();
                addAliases(aliases, properties == null ? null : properties.get("aliases"));
                addAliases(aliases, properties == null ? null : properties.get("alias"));
                addAliases(aliases, properties == null ? null : properties.get("alternativeNames"));
                addAliases(aliases, properties == null ? null : properties.get("alternative_names"));
                ContextGraphEntitySearchResult result = new ContextGraphEntitySearchResult(
                    node.getEntityID(), name, category, externalId, node.getIdentity(), aliases);
                found.add(result);
                text.put(result.entityId(), normalize(name + " " + result.entityId() + " "
                    + externalId + " " + node.getIdentity() + " " + category + " "
                    + String.join(" ", aliases)));
            }
        }
        found.sort(Comparator.comparing(ContextGraphEntitySearchResult::entityId));
        entries = List.copyOf(found);
        searchableText = Map.copyOf(text);
    }

    /** Empty queries return no results. Results are ranked and bounded for large graphs. */
    public List<ContextGraphEntitySearchResult> find(String query, int maximum) {
        if (query == null || query.isBlank() || maximum <= 0) return List.of();
        String needle = normalize(query.trim());
        String[] words = needle.split("\\s+");
        List<ContextGraphEntitySearchResult> matches = new ArrayList<>();
        for (ContextGraphEntitySearchResult result : entries) {
            String corpus = searchableText.get(result.entityId());
            boolean all = true;
            for (String word : words) {
                if (!corpus.contains(word)) { all = false; break; }
            }
            if (all) matches.add(result);
        }
        matches.sort(Comparator
            .comparingInt((ContextGraphEntitySearchResult result) -> relevance(result, needle))
            .thenComparing(ContextGraphEntitySearchResult::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(ContextGraphEntitySearchResult::entityId));
        return List.copyOf(matches.subList(0, Math.min(maximum, matches.size())));
    }

    private static int relevance(ContextGraphEntitySearchResult entity, String needle) {
        String name = normalize(entity.displayName());
        String id = normalize(entity.entityId());
        String external = normalize(entity.externalId());
        if (name.equals(needle)) return 0;
        if (id.equals(needle) || external.equals(needle)
                || Long.toString(entity.identity()).equals(needle)) return 1;
        for (String alias : entity.aliases()) if (normalize(alias).equals(needle)) return 2;
        if (name.startsWith(needle)) return 3;
        for (String alias : entity.aliases()) if (normalize(alias).startsWith(needle)) return 4;
        if (id.startsWith(needle) || external.startsWith(needle)) return 5;
        if (name.contains(needle)) return 6;
        return 7;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String property(Map<String, Object> properties, String key) {
        if (properties == null) return "";
        Object value = properties.get(key);
        return value instanceof String s ? s.trim() : "";
    }

    private static void addAliases(List<String> target, Object value) {
        if (value instanceof String s && !s.isBlank()) target.add(s);
        else if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item instanceof String s && !s.isBlank()) target.add(s);
            }
        }
    }
}
