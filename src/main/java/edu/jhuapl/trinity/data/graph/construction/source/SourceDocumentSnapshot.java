package edu.jhuapl.trinity.data.graph.construction.source;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Detached, immutable representation of one imported source revision. Offsets are
 * half-open Java UTF-16 code-unit offsets in canonicalText, not PDF byte offsets,
 * glyph positions, or offsets into a pre-extraction PDF stream.
 *
 * Page spans describe exactly the PDFBox per-page text concatenation used to
 * create canonicalText. For non-PDF sources, pages is empty; callers must not
 * infer page numbers. Transformed JSON/CSV passages may have no source span.
 */
public record SourceDocumentSnapshot(
        String documentId,
        String documentRevision,
        String sourceName,
        String sourcePath,
        String extractionMethod,
        String canonicalText,
        List<PageSpan> pages,
        List<Passage> passages,
        Map<String, String> parameters) {

    public static final String SPAN_COORDINATE_SYSTEM = "TRINITY_EXTRACTED_TEXT_UTF16_V1";

    public record PageSpan(int pageNumber, int startOffset, int endOffset) {
        public PageSpan {
            if (pageNumber < 1 || startOffset < 0 || endOffset < startOffset) {
                throw new IllegalArgumentException("Invalid page span");
            }
        }
    }

    /** A source-aligned chunk, or a derived CSV/JSON embedding chunk without a raw span. */
    public record Passage(
            String passageId,
            int chunkIndex,
            String text,
            String label,
            Map<String, String> metadata,
            Integer startOffset,
            Integer endOffset,
            Integer pageNumber) {
        public Passage {
            if (passageId == null || passageId.isBlank() || chunkIndex < 0
                    || text == null || text.isBlank()) {
                throw new IllegalArgumentException("Invalid source passage");
            }
            metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
            if ((startOffset == null) != (endOffset == null)
                || (startOffset != null && (startOffset < 0 || endOffset <= startOffset))) {
                throw new IllegalArgumentException("Passage must have an exact nonempty span or none");
            }
            if (pageNumber != null && pageNumber < 1) {
                throw new IllegalArgumentException("Invalid passage page");
            }
        }

        public boolean hasExactSpan() { return startOffset != null; }
    }

    public SourceDocumentSnapshot {
        if (documentId == null || documentId.isBlank()
                || documentRevision == null || documentRevision.isBlank()
                || sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("Document identity, revision and source name are required");
        }
        canonicalText = Objects.requireNonNull(canonicalText, "canonicalText");
        pages = List.copyOf(Objects.requireNonNull(pages, "pages"));
        passages = List.copyOf(Objects.requireNonNull(passages, "passages"));
        parameters = Map.copyOf(Objects.requireNonNull(parameters, "parameters"));

        int previousEnd = 0;
        for (PageSpan page : pages) {
            if (page.startOffset() != previousEnd || page.endOffset() > canonicalText.length()) {
                throw new IllegalArgumentException("PDF page spans must exactly partition canonical text");
            }
            previousEnd = page.endOffset();
        }
        if (!pages.isEmpty() && previousEnd != canonicalText.length()) {
            throw new IllegalArgumentException("PDF page spans do not cover canonical text");
        }
        for (int i = 0; i < passages.size(); i++) {
            Passage passage = passages.get(i);
            if (passage.chunkIndex() != i) {
                throw new IllegalArgumentException("Passages must be ordered and consecutively indexed");
            }
            if (passage.hasExactSpan()) {
                if (passage.endOffset() > canonicalText.length()
                        || !canonicalText.substring(passage.startOffset(), passage.endOffset())
                            .equals(passage.text())) {
                    throw new IllegalArgumentException("Passage span is not an exact canonical-text match");
                }
                if (passage.pageNumber() != null) {
                    int pageNumber = passage.pageNumber();
                    PageSpan containing = pages.stream()
                        .filter(p -> p.pageNumber() == pageNumber).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Passage page does not exist"));
                    if (passage.startOffset() < containing.startOffset()
                            || passage.endOffset() > containing.endOffset()) {
                        throw new IllegalArgumentException("Passage crosses a PDF page boundary");
                    }
                }
            }
        }
    }
}
