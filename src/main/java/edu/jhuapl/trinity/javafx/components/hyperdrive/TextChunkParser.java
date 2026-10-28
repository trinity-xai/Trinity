package edu.jhuapl.trinity.javafx.components.hyperdrive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * File parsing and text chunk creation without JavaFX dependencies. Parse on a worker
 * thread, then create EmbeddingsTextListItems on the JavaFX Application Thread.
 */
public final class TextChunkParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TextChunkParser() { }

    public record Options(boolean jsonProcessing, boolean csvExpansion,
                          boolean labelFromCsvColumn, int csvLabelColumn,
                          boolean breakOnNewlines, int chunkSize, JsonExtractionProfile jsonProfile) {
        public Options(boolean jsonProcessing, boolean csvExpansion,
                       boolean labelFromCsvColumn, int csvLabelColumn,
                       boolean breakOnNewlines, int chunkSize) {
            this(jsonProcessing, csvExpansion, labelFromCsvColumn, csvLabelColumn, breakOnNewlines, chunkSize, null);
        }
        public Options {
            if (chunkSize < 1) throw new IllegalArgumentException("chunkSize must be positive");
            if (csvLabelColumn < 0) throw new IllegalArgumentException("csvLabelColumn must not be negative");
        }
    }

    /** The text value is exactly what the embedding server should receive. */
    public record ParsedChunk(String text, String label, Map<String, String> metadata) {
        public ParsedChunk {
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Empty embedding chunk");
            }
            metadata = Map.copyOf(metadata);
        }
    }

    public static String extractText(File file) throws IOException {
        if (file == null || !file.isFile() || !file.canRead()) {
            throw new IOException("File is not readable: " + file);
        }
        if (file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                return stripper.getText(document);
            }
        }
        return Files.readString(file.toPath());
    }

    public static List<ParsedChunk> parse(File file, Options options) throws IOException {
        return parseText(file, extractText(file), options);
    }

    /** Package-friendly deterministic entry point for tests of content and options. */
    public static List<ParsedChunk> parseText(File file, String source, Options options) throws IOException {
        if (source == null || source.isBlank()) return List.of();
        String name = file.getName();
        String trimmed = source.strip();
        List<ParsedChunk> parsed;
        if (options.csvExpansion() && name.toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
            parsed = parseCsv(name, source, options);
        } else if (options.jsonProcessing() && (trimmed.startsWith("{") || trimmed.startsWith("["))) {
            parsed = parseJson(name, trimmed, options.jsonProfile());
        } else if (options.breakOnNewlines()) {
            parsed = parseLines(name, source, options.chunkSize());
        } else {
            parsed = splitText(name, source, options.chunkSize(), Map.of());
        }
        // Stable, zero-based provenance for all supported formats.
        List<ParsedChunk> indexed = new ArrayList<>(parsed.size());
        for (int i = 0; i < parsed.size(); i++) {
            ParsedChunk chunk = parsed.get(i);
            Map<String, String> meta = new LinkedHashMap<>(chunk.metadata());
            meta.put("source_chunk_index", Integer.toString(i));
            meta.put("source_chunk_count", Integer.toString(parsed.size()));
            indexed.add(new ParsedChunk(chunk.text(), chunk.label(), meta));
        }
        return indexed;
    }

    private static List<ParsedChunk> parseJson(String name, String json, JsonExtractionProfile profile) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        List<ParsedChunk> output = new ArrayList<>();
        if (profile != null && !profile.hasEmbeddingFields()) {
            throw new IOException("JSON mapping profile has no embedding fields: " + profile.getName());
        }
        if (root.isArray()) {
            for (int i = 0; i < root.size(); i++) {
                if (profile == null) addJsonValue(output, root.get(i), name, i);
                else addMappedJsonValue(output, root.get(i), name, i, profile);
            }
        } else {
            if (profile == null) addJsonValue(output, root, name, 0);
            else addMappedJsonValue(output, root, name, 0, profile);
        }
        if (profile != null && output.isEmpty()) {
            throw new IOException("JSON mapping profile '" + profile.getName()
                + "' produced no embedding text for " + name);
        }
        if (profile != null && root.isArray() && output.size() < root.size()) {
            int skipped = root.size() - output.size();
            List<ParsedChunk> annotated = new ArrayList<>(output.size());
            for (ParsedChunk chunk : output) {
                Map<String, String> metadata = new LinkedHashMap<>(chunk.metadata());
                metadata.put("source_json_skipped_records", Integer.toString(skipped));
                annotated.add(new ParsedChunk(chunk.text(), chunk.label(), metadata));
            }
            return annotated;
        }
        return output;
    }

    private static void addMappedJsonValue(List<ParsedChunk> output, JsonNode record,
                                          String name, int index, JsonExtractionProfile profile) {
        if (record == null || record.isNull()) return;
        JsonExtractionProfile.Extracted extracted = profile.extract(record, name);
        if (extracted.text().isBlank()) return; // Never silently revert to raw JSON.
        Map<String, String> metadata = new LinkedHashMap<>(extracted.metadata());
        metadata.put("source_json_index", Integer.toString(index));
        metadata.put("json_mapping_profile", profile.getName());
        output.add(new ParsedChunk(extracted.text(), extracted.label(), metadata));
    }

    private static void addJsonValue(List<ParsedChunk> output, JsonNode value, String name, int index) {
        if (value == null || value.isNull()) return;
        String text = value.toPrettyString();
        String label = name;
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("source_json_index", Integer.toString(index));
        if (value.isObject()) {
            var fields = value.properties();
            for (var field : fields) {
                String key = field.getKey();
                JsonNode child = field.getValue();
                String lower = key.toLowerCase(java.util.Locale.ROOT);
                if (lower.equals("text") && child != null && !child.isNull()) {
                    String candidate = child.isTextual() ? child.asText() : child.toPrettyString();
                    if (!candidate.isBlank()) text = candidate;
                } else if ((lower.contains("name") || lower.contains("label")) && child != null) {
                    label = child.isTextual() ? child.asText() : child.toString();
                } else {
                    metadata.put(key, child == null ? "null" : child.toString());
                }
            }
        }
        if (!text.isBlank()) output.add(new ParsedChunk(text, label, metadata));
    }

    private static List<ParsedChunk> parseLines(String name, String source, int size) {
        List<ParsedChunk> result = new ArrayList<>();
        // Supports CRLF, LF and lone CR within one imported file.
        String[] lines = source.split("\\r\\n|\\n|\\r", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            result.addAll(splitText(name, lines[i], size, Map.of("source_line", Integer.toString(i + 1))));
        }
        return result;
    }

    private static List<ParsedChunk> splitText(String name, String source, int size, Map<String, String> metadata) {
        List<ParsedChunk> result = new ArrayList<>();
        for (int start = 0; start < source.length();) {
            int end = Math.min(source.length(), start + size);
            // Avoid slicing a surrogate pair across two embedding requests.
            if (end < source.length() && end > start
                    && Character.isHighSurrogate(source.charAt(end - 1))
                    && Character.isLowSurrogate(source.charAt(end))) end--;
            if (end == start) end = Math.min(source.length(), start + 2);
            String chunk = source.substring(start, end);
            if (!chunk.isBlank()) result.add(new ParsedChunk(chunk, name, metadata));
            start = end;
        }
        return result;
    }

    private static List<ParsedChunk> parseCsv(String name, String source, Options options) throws IOException {
        List<List<String>> records = parseCsvRecords(source);
        if (records.size() < 2) return List.of();
        List<String> labels = records.get(0);
        List<ParsedChunk> chunks = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < records.size(); rowIndex++) {
            List<String> record = records.get(rowIndex);
            if (record.stream().allMatch(String::isBlank)) continue;
            StringBuilder expanded = new StringBuilder();
            for (int col = 0; col < Math.min(labels.size(), record.size()); col++) {
                expanded.append(labels.get(col).trim()).append(" = ")
                    .append(record.get(col).trim()).append(System.lineSeparator());
            }
            if (expanded.isEmpty() || expanded.toString().isBlank()) continue;
            String label = name;
            if (options.labelFromCsvColumn() && options.csvLabelColumn() < record.size()) {
                label = record.get(options.csvLabelColumn());
            }
            chunks.add(new ParsedChunk(expanded.toString(), label,
                Map.of("source_csv_record", Integer.toString(rowIndex + 1))));
        }
        return chunks;
    }

    /** RFC-4180-style CSV field parsing, including escaped quotes and multiline fields. */
    public static List<List<String>> parseCsvRecords(String source) throws IOException {
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean afterQuote = false;
        boolean atFieldStart = true;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < source.length() && source.charAt(i + 1) == '"') {
                        field.append('"'); i++;
                    } else {
                        inQuotes = false;
                        afterQuote = true;
                    }
                } else field.append(c);
            } else if (c == ',' || c == '\n' || c == '\r') {
                fields.add(field.toString());
                field.setLength(0);
                atFieldStart = true;
                afterQuote = false;
                if (c != ',') {
                    if (c == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') i++;
                    records.add(fields);
                    fields = new ArrayList<>();
                }
            } else if (c == '"' && atFieldStart) {
                inQuotes = true;
                atFieldStart = false;
            } else {
                if (afterQuote && !Character.isWhitespace(c)) {
                    throw new IOException("Unexpected character after closing CSV quote at offset " + i);
                }
                if (!afterQuote) field.append(c);
                atFieldStart = false;
            }
        }
        if (inQuotes) throw new IOException("Unterminated quoted CSV field");
        if (!fields.isEmpty() || !atFieldStart || field.length() > 0 || source.endsWith(",")) {
            fields.add(field.toString());
            records.add(fields);
        }
        return records;
    }
}
