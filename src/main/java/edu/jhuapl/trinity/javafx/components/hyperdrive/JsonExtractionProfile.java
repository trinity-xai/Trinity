package edu.jhuapl.trinity.javafx.components.hyperdrive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reusable, Jackson-serializable mapping from JSON paths to embedding input and provenance. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonExtractionProfile {
    private String name = "JSON Mapping";
    private List<FieldMapping> fields = new ArrayList<>();

    public JsonExtractionProfile() { }
    public String getName() { return name; }
    public void setName(String name) { this.name = name == null ? "JSON Mapping" : name; }
    public List<FieldMapping> getFields() { return fields; }
    public void setFields(List<FieldMapping> fields) {
        this.fields = fields == null ? new ArrayList<>() : new ArrayList<>(fields);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FieldMapping {
        private String path;
        private boolean embedding;
        private boolean label;
        private boolean metadata;

        public FieldMapping() { }
        public FieldMapping(String path, boolean embedding, boolean label, boolean metadata) {
            this.path = path;
            this.embedding = embedding;
            this.label = label;
            this.metadata = metadata;
        }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public boolean isEmbedding() { return embedding; }
        public void setEmbedding(boolean embedding) { this.embedding = embedding; }
        public boolean isLabel() { return label; }
        public void setLabel(boolean label) { this.label = label; }
        public boolean isMetadata() { return metadata; }
        public void setMetadata(boolean metadata) { this.metadata = metadata; }
    }

    /** Take a stable snapshot before processing a background import. */
    public JsonExtractionProfile copy() {
        JsonExtractionProfile snapshot = new JsonExtractionProfile();
        snapshot.setName(name);
        for (FieldMapping field : fields) {
            snapshot.fields.add(new FieldMapping(field.path, field.embedding, field.label, field.metadata));
        }
        return snapshot;
    }

    public boolean hasEmbeddingFields() {
        return fields.stream().anyMatch(f -> f.embedding && f.path != null && !f.path.isBlank());
    }

    public record Extracted(String text, String label, Map<String, String> metadata) { }

    /** The preview and importer share this identical extraction implementation. */
    public Extracted extract(JsonNode record, String fallbackLabel) {
        StringBuilder text = new StringBuilder();
        String labelValue = fallbackLabel;
        Map<String, String> metadata = new LinkedHashMap<>();
        for (FieldMapping mapping : fields) {
            if (mapping.path == null || mapping.path.isBlank()) continue;
            List<String> values = valuesAt(record, mapping.path);
            if (values.isEmpty()) continue;
            if (mapping.embedding) {
                String display = displayName(mapping.path);
                for (String value : values) {
                    if (!value.isBlank()) text.append(display).append(": ").append(value).append('\n');
                }
            }
            if (mapping.label && !values.get(0).isBlank()) labelValue = values.get(0);
            if (mapping.metadata) metadata.put(mapping.path, String.join("; ", values));
        }
        return new Extracted(text.toString().strip(), labelValue, metadata);
    }

    private static String displayName(String path) {
        String leaf = path.substring(path.lastIndexOf('.') + 1).replace("[]", "");
        return leaf.replace('_', ' ').replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    public static List<String> valuesAt(JsonNode node, String path) {
        List<String> output = new ArrayList<>();
        if (node == null || path == null || path.isBlank()) return output;
        visitValues(node, path.split("\\."), 0, output);
        return output;
    }

    private static void visitValues(JsonNode node, String[] components, int i, List<String> output) {
        if (node == null || node.isNull()) return;
        if (i == components.length) {
            if (node.isArray()) {
                for (JsonNode part : node) visitValues(part, components, i, output);
            } else if (node.isValueNode()) {
                String value = node.asText();
                if (!value.isBlank()) output.add(value);
            }
            return;
        }
        String part = components[i];
        if (part.endsWith("[]")) {
            JsonNode array = node.get(part.substring(0, part.length() - 2));
            if (array != null && array.isArray()) for (JsonNode element : array) visitValues(element, components, i + 1, output);
        } else if (node.isArray()) {
            for (JsonNode element : node) visitValues(element, components, i, output);
        } else {
            visitValues(node.get(part), components, i + 1, output);
        }
    }

    /** Inspect a bounded sample of records and discover scalar field paths, including arrays. */
    public static List<String> discoverPaths(JsonNode root) {
        Set<String> paths = new LinkedHashSet<>();
        if (root == null) return List.of();
        if (root.isArray()) {
            for (int i = 0; i < Math.min(40, root.size()) && paths.size() < 300; i++) {
                traverse(root.get(i), "", 0, paths);
            }
        } else traverse(root, "", 0, paths);
        return List.copyOf(paths);
    }

    private static void traverse(JsonNode node, String path, int depth, Set<String> paths) {
        if (node == null || node.isNull() || depth > 16 || paths.size() >= 300) return;
        if (node.isObject()) {
            node.properties().forEach(p -> traverse(p.getValue(), path.isEmpty() ? p.getKey() : path + "." + p.getKey(), depth + 1, paths));
        } else if (node.isArray()) {
            for (int i = 0; i < Math.min(20, node.size()); i++) traverse(node.get(i), path + "[]", depth + 1, paths);
        } else if (!path.isBlank()) paths.add(path);
    }
}
