package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import java.util.List;

/** A selectable graph entity match, independent of any JavaFX scene node. */
public record ContextGraphEntitySearchResult(
        String entityId,
        String displayName,
        String category,
        String externalId,
        long identity,
        List<String> aliases) {

    public ContextGraphEntitySearchResult {
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    @Override
    public String toString() {
        String identifier = externalId == null || externalId.isBlank() ? "" : "  (" + externalId + ")";
        return displayName + identifier + "  [" + category + "]";
    }
}
