package edu.jhuapl.trinity.javafx.components.hyperdrive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhuapl.trinity.data.graph.construction.source.SourceDocumentIdentity;
import edu.jhuapl.trinity.data.graph.construction.source.SourceDocumentSnapshot;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharacterCodingException;
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

    /** A source extraction and the exact chunks submitted for embedding. */
    public record ParsedDocument(SourceDocumentSnapshot source, List<ParsedChunk> chunks) {
        public ParsedDocument {
            if (source == null || chunks == null) throw new IllegalArgumentException("Missing parsed source");
            chunks = List.copyOf(chunks);
        }
    }

    /** Existing Hyperdrive entry point, now with stable passage provenance. */
    public static List<ParsedChunk> parse(File file, Options options) throws IOException {
        return parseDocument(file, options).chunks();
    }

    /**
     * Extract a source with stable identities. PDFBox text is extracted one page
     * at a time and concatenated in page order; a passage never crosses a PDF
     * page. Offsets reference this exact canonical extracted string, not PDF
     * bytes or glyph geometry. Does not create JavaFX nodes.
     */
    public static ParsedDocument parseDocument(File file, Options options) throws IOException {
        return parseDocument(file, options, null);
    }

    /** Specify an enduring external logical source key to survive file relocation. */
    public static ParsedDocument parseDocument(File file, Options options,
                                               String logicalSourceKey) throws IOException {
        if (file == null || !file.isFile() || !file.canRead()) {
            throw new IOException("File is not readable: " + file);
        }
        // Every file revision is computed from the same bytes supplied to its
        // extractor. PDF uses a temporary disk snapshot to avoid retaining a
        // potentially large scanned PDF in the Java heap.
        String documentId = logicalSourceKey == null
            ? SourceDocumentIdentity.documentId(file)
            : SourceDocumentIdentity.documentId(logicalSourceKey);
        if (file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            java.nio.file.Path snapshotFile = Files.createTempFile("trinity-pdf-source-", ".pdf");
            try {
                Files.copy(file.toPath(), snapshotFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                String revision = SourceDocumentIdentity.revision(snapshotFile.toFile());
                try (PDDocument pdf = Loader.loadPDF(snapshotFile.toFile())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setSortByPosition(true);
                    StringBuilder source = new StringBuilder();
                    List<SourceDocumentSnapshot.PageSpan> pages = new ArrayList<>();
                    for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
                        stripper.setStartPage(page);
                        stripper.setEndPage(page);
                        int start = source.length();
                        source.append(stripper.getText(pdf));
                        pages.add(new SourceDocumentSnapshot.PageSpan(page, start, source.length()));
                    }
                    return parseDocumentText(file, source.toString(), options, documentId, revision,
                        pages, "PDFBOX_PER_PAGE_SORT_BY_POSITION");
                }
            } finally {
                Files.deleteIfExists(snapshotFile);
            }
        }
        byte[] rawBytes = Files.readAllBytes(file.toPath());
        String revision = SourceDocumentIdentity.revision(rawBytes);
        String utf8;
        try {
            utf8 = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(rawBytes)).toString();
        } catch (CharacterCodingException malformed) {
            throw new IOException("Source contains invalid UTF-8: " + file, malformed);
        }
        return parseDocumentText(file, utf8, options,
            documentId, revision, List.of(), "TEXT_UTF8");
    }

    /** Existing deterministic test API; supplied text is the canonical source. */
    public static List<ParsedChunk> parseText(File file, String source, Options options) throws IOException {
        return parseTextDocument(file, source, options).chunks();
    }

    /** Test-friendly path with an exact canonical text and explicit provenance. */
    public static ParsedDocument parseTextDocument(File file, String source,
                                                    Options options) throws IOException {
        if (file == null) throw new IllegalArgumentException("file is required");
        return parseDocumentText(file, source == null ? "" : source, options,
            SourceDocumentIdentity.documentId(file),
            SourceDocumentIdentity.revision(source == null ? "" : source),
            List.of(), "TEXT_UTF8");
    }

    private static ParsedDocument parseDocumentText(File file, String source, Options options,
            String documentId, String revision, List<SourceDocumentSnapshot.PageSpan> pages,
            String extractionMethod) throws IOException {
        if (options == null) throw new IllegalArgumentException("options are required");
        String name = file.getName();
        List<ParsedChunk> parsed = new ArrayList<>();
        if (!source.isBlank()) {
            if (!pages.isEmpty()) {
                // PDF contents are ALWAYS plaintext even if JSON/CSV settings are on.
                for (SourceDocumentSnapshot.PageSpan page : pages) {
                    String pageText = source.substring(page.startOffset(), page.endOffset());
                    List<ParsedChunk> pageChunks = options.breakOnNewlines()
                        ? parseLines(name, pageText, options.chunkSize(), page.startOffset())
                        : splitText(name, pageText, options.chunkSize(), Map.of(), page.startOffset());
                    for (ParsedChunk chunk : pageChunks) {
                        Map<String, String> meta = new LinkedHashMap<>(chunk.metadata());
                        meta.put("source_page_number", Integer.toString(page.pageNumber()));
                        parsed.add(new ParsedChunk(chunk.text(), chunk.label(), meta));
                    }
                }
            } else {
                String trimmed = source.strip();
                if (options.csvExpansion() && name.toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
                    parsed = parseCsv(name, source, options);
                } else if (options.jsonProcessing() && (trimmed.startsWith("{") || trimmed.startsWith("["))) {
                    parsed = parseJson(name, trimmed, options.jsonProfile());
                } else if (options.breakOnNewlines()) {
                    parsed = parseLines(name, source, options.chunkSize(), 0);
                } else {
                    parsed = splitText(name, source, options.chunkSize(), Map.of(), 0);
                }
            }
        }
        boolean transformed = pages.isEmpty() && isTransformed(name, options, source);
        List<ParsedChunk> indexed = new ArrayList<>(parsed.size());
        List<SourceDocumentSnapshot.Passage> passages = new ArrayList<>(parsed.size());
        for (int i = 0; i < parsed.size(); i++) {
            ParsedChunk chunk = parsed.get(i);
            Map<String, String> meta = new LinkedHashMap<>(chunk.metadata());
            // Reserved provenance fields must come from OUR extraction, not an input JSON object.
            String startText = chunk.metadata().get("source_start_offset");
            String endText = chunk.metadata().get("source_end_offset");
            String pageText = chunk.metadata().get("source_page_number");
            boolean exact = startText != null && endText != null &&
                (pages.isEmpty() ? !transformed : pageText != null);
            Integer start = exact ? Integer.parseInt(startText) : null;
            Integer end = exact ? Integer.parseInt(endText) : null;
            Integer page = exact && pageText != null ? Integer.parseInt(pageText) : null;
            meta.remove("source_start_offset");
            meta.remove("source_end_offset");
            meta.remove("source_page_number");
            meta.remove("source_span_coordinate_system");
            meta.remove("source_document_id");
            meta.remove("source_document_revision");
            meta.remove("source_passage_id");
            meta.put("source_chunk_index", Integer.toString(i));
            meta.put("source_chunk_count", Integer.toString(parsed.size()));
            meta.put("source_document_id", documentId);
            meta.put("source_document_revision", revision);
            String passageId = SourceDocumentIdentity.passageId(documentId, revision, i, chunk.text());
            meta.put("source_passage_id", passageId);
            if (start != null) {
                meta.put("source_start_offset", start.toString());
                meta.put("source_end_offset", end.toString());
                meta.put("source_span_coordinate_system", SourceDocumentSnapshot.SPAN_COORDINATE_SYSTEM);
            }
            if (page != null) meta.put("source_page_number", page.toString());
            ParsedChunk indexedChunk = new ParsedChunk(chunk.text(), chunk.label(), meta);
            indexed.add(indexedChunk);
            passages.add(new SourceDocumentSnapshot.Passage(passageId, i, chunk.text(),
                chunk.label(), meta, start, end, page));
        }
        Map<String, String> parameters = Map.of(
            "chunkSize", Integer.toString(options.chunkSize()),
            "breakOnNewlines", Boolean.toString(options.breakOnNewlines()),
            "jsonProcessing", Boolean.toString(options.jsonProcessing()),
            "csvExpansion", Boolean.toString(options.csvExpansion()));
        SourceDocumentSnapshot snapshot = new SourceDocumentSnapshot(documentId, revision,
            name, file.toPath().toAbsolutePath().normalize().toString(),
            extractionMethod, source, pages, passages, parameters);
        return new ParsedDocument(snapshot, indexed);
    }

    private static boolean isTransformed(String name, Options options, String source) {
        String trimmed = source.strip();
        return (options.csvExpansion() && name.toLowerCase(java.util.Locale.ROOT).endsWith(".csv"))
            || (options.jsonProcessing() && (trimmed.startsWith("{") || trimmed.startsWith("[")));
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

    private static List<ParsedChunk> parseLines(String name, String source, int size,
                                                int baseOffset) {
        List<ParsedChunk> result = new ArrayList<>();
        int lineNumber = 1;
        int start = 0;
        for (int i = 0; i <= source.length(); i++) {
            if (i != source.length() && source.charAt(i) != '\n' && source.charAt(i) != '\r') {
                continue;
            }
            if (i > start && !source.substring(start, i).isBlank()) {
                result.addAll(splitText(name, source.substring(start, i), size,
                    Map.of("source_line", Integer.toString(lineNumber)), baseOffset + start));
            }
            if (i < source.length() && source.charAt(i) == '\r' && i + 1 < source.length()
                    && source.charAt(i + 1) == '\n') i++;
            start = i + 1;
            lineNumber++;
        }
        return result;
    }

    private static List<ParsedChunk> splitText(String name, String source, int size,
                                                Map<String, String> metadata, int baseOffset) {
        List<ParsedChunk> result = new ArrayList<>();
        for (int start = 0; start < source.length();) {
            int end = Math.min(source.length(), start + size);
            if (end < source.length() && end > start
                    && Character.isHighSurrogate(source.charAt(end - 1))
                    && Character.isLowSurrogate(source.charAt(end))) end--;
            if (end == start) end = Math.min(source.length(), start + 2);
            String chunk = source.substring(start, end);
            if (!chunk.isBlank()) {
                Map<String, String> meta = new LinkedHashMap<>(metadata);
                meta.put("source_start_offset", Integer.toString(baseOffset + start));
                meta.put("source_end_offset", Integer.toString(baseOffset + end));
                result.add(new ParsedChunk(chunk, name, meta));
            }
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
