package edu.jhuapl.trinity.data.messages.xai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CyberReportIO {
    private CyberReportIO() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // --- Public convenience overloads ---

    /**
     * Read from a File and return a flattened list of reports.
     */
    public static List<CyberReport> readReports(File file) throws IOException {
        JsonNode root = MAPPER.readTree(file);
        return toReports(root);
    }

    /**
     * Read from a Path and return a flattened list of reports.
     */
    public static List<CyberReport> readReports(Path path) throws IOException {
        JsonNode root = MAPPER.readTree(path.toFile());
        return toReports(root);
    }

    /**
     * Read from a Reader and return a flattened list of reports.
     */
    public static List<CyberReport> readReports(Reader in) throws IOException {
        JsonNode root = MAPPER.readTree(in);
        return toReports(root);
    }

    /**
     * Read from a JSON string and return a flattened list of reports.
     */
    public static List<CyberReport> readReports(String json) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        return toReports(root);
    }

    // --- Core shape detection + flattening ---
    private static List<CyberReport> toReports(JsonNode root) throws IOException {
        if (root == null || root.isNull()) return List.of();

        // 1) Single object -> [CyberReport]
        if (root.isObject()) {
            CyberReport one = MAPPER.treeToValue(root, CyberReport.class);
            return List.of(one);
        }

        // 2) Array -> could be [object, ...] OR [[object,...], [object,...], ...]
        if (root.isArray()) {
            List<CyberReport> out = new ArrayList<>();

            // Fast path: top-level array of objects
            if (allObjects(root)) {
                out.addAll(MAPPER.convertValue(root, new TypeReference<List<CyberReport>>() {
                }));
                return out;
            }

            // Matrix path: top-level array of arrays
            for (JsonNode group : root) {
                if (group == null || group.isNull()) continue;

                if (group.isObject()) {
                    // Mixed arrays are tolerated: append objects directly
                    out.add(MAPPER.treeToValue(group, CyberReport.class));
                } else if (group.isArray()) {
                    // Nested array: must be objects
                    for (JsonNode item : group) {
                        if (!item.isObject()) {
                            throw new IOException("Unsupported JSON shape: nested array contains non-object element.");
                        }
                        out.add(MAPPER.treeToValue(item, CyberReport.class));
                    }
                } else {
                    throw new IOException("Unsupported JSON shape: array contains non-object, non-array element.");
                }
            }
            return out;
        }

        throw new IOException("Unsupported JSON shape: expected object, array, or array-of-arrays.");
    }

    private static boolean allObjects(JsonNode arr) {
        for (JsonNode n : arr) {
            if (!n.isObject()) return false;
        }
        return true;
    }
}
