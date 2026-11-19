package edu.jhuapl.trinity.javafx.components.hyperdrive;

import edu.jhuapl.trinity.data.graph.construction.source.SourceDocumentSnapshot;
import edu.jhuapl.trinity.data.graph.construction.source.SourceDocumentHierarchyBuilder;
import edu.jhuapl.trinity.data.graph.semantics.GraphHierarchyIndex;
import edu.jhuapl.trinity.data.graph.semantics.GraphSemanticValidator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SourceDocumentParsingTest {
    private static TextChunkParser.Options plain(int size, boolean lines) {
        return new TextChunkParser.Options(false, false, false, 0, lines, size);
    }

    @Test
    void sameSourceReimportKeepsDocumentRevisionAndPassageIdentity() throws Exception {
        File file = new File("relocations/example.txt");
        var a = TextChunkParser.parseTextDocument(file, "hello\nworld", plain(50, true));
        var b = TextChunkParser.parseTextDocument(file, "hello\nworld", plain(50, true));
        assertEquals(a.source().documentId(), b.source().documentId());
        assertEquals(a.source().documentRevision(), b.source().documentRevision());
        assertEquals(a.chunks(), b.chunks());
        assertEquals(a.source().passages(), b.source().passages());
        var changed = TextChunkParser.parseTextDocument(file, "hello\nworld!", plain(50, true));
        assertEquals(a.source().documentId(), changed.source().documentId());
        assertFalse(a.source().documentRevision().equals(changed.source().documentRevision()));
        assertFalse(a.source().passages().get(0).passageId().equals(changed.source().passages().get(0).passageId()));
    }

    @Test
    void actualTextFileRevisionHashesTheSameBytesThatWereParsed() throws Exception {
        Path file = Files.createTempFile("trinity-document-", ".txt");
        try {
            String content = "First line\nSecond line \uD83D\uDE00\n";
            Files.writeString(file, content);
            var parsed = TextChunkParser.parseDocument(file.toFile(), plain(2048, true));
            assertEquals(content, parsed.source().canonicalText());
            assertEquals(edu.jhuapl.trinity.data.graph.construction.source.SourceDocumentIdentity
                .revision(Files.readAllBytes(file)), parsed.source().documentRevision());
            assertEquals(2, parsed.chunks().size());
            assertEquals("passage:", parsed.chunks().get(0).metadata()
                .get("source_passage_id").substring(0, 8));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void lineChunkOffsetsUseExactUtf16PositionsAndSkipBlankLines() throws Exception {
        String text = "alpha\r\n\r\nbeta\n\uD83D\uDE00suffix";
        var doc = TextChunkParser.parseTextDocument(new File("example.md"), text, plain(4, true));
        assertTrue(doc.source().pages().isEmpty());
        for (SourceDocumentSnapshot.Passage p : doc.source().passages()) {
            assertTrue(p.hasExactSpan());
            assertEquals(p.text(), text.substring(p.startOffset(), p.endOffset()));
            assertEquals(SourceDocumentSnapshot.SPAN_COORDINATE_SYSTEM,
                p.metadata().get("source_span_coordinate_system"));
            assertNull(p.pageNumber());
        }
        assertEquals("1", doc.chunks().get(0).metadata().get("source_line"));
        assertEquals("3", doc.chunks().stream().filter(c -> c.text().contains("beta"))
            .findFirst().orElseThrow().metadata().get("source_line"));
        assertFalse(doc.chunks().stream().anyMatch(c -> c.text().equals("\uD83D") || c.text().equals("\uDE00")));
    }

    @Test
    void markdownHeadingHierarchyIsTypedAndValidationSafe() throws Exception {
        String text = "# Scope\nfirst\n## Details\nsecond\n# Other\nthird\n";
        var parsed = TextChunkParser.parseTextDocument(new File("sample.md"), text, plain(100, true));
        var graph = SourceDocumentHierarchyBuilder.build(parsed.source());
        var report = GraphSemanticValidator.validate(graph, GraphSemanticValidator.Mode.STRICT_TYPED);
        assertTrue(report.isValid(), report.issues().toString());
        long sectionCount = graph.getNodes().stream().filter(n -> "SECTION".equals(n.getNodeKind())).count();
        assertEquals(3L, sectionCount);
        var hierarchy = GraphHierarchyIndex.build(graph)
            .hierarchy("source:" + parsed.source().documentId(), "CONTAINS");
        assertTrue(hierarchy.cycleComponents().isEmpty());
        var child = parsed.source().passages().stream().filter(p -> p.text().equals("second"))
            .findFirst().orElseThrow();
        var parents = hierarchy.immediateParents(child.passageId());
        assertEquals(1, parents.size());
        String details = graph.getNodes().stream()
            .filter(n -> "Details".equals(n.getProperties().get("name")))
            .findFirst().orElseThrow().getEntityID();
        assertEquals(details, parents.get(0));
        assertTrue(hierarchy.ancestors(child.passageId()).contains(parsed.source().documentId()));
        assertEquals(graph.getEvidenceRecords().size(), parsed.source().passages().size());
        for (var evidence : graph.getEvidenceRecords()) {
            assertEquals(text.substring(evidence.getStartOffset().intValue(),
                evidence.getEndOffset().intValue()), evidence.getQuote());
        }
    }

    @Test
    void unmappableTransformedCsvDoesNotFabricateOffsets() throws Exception {
        String csv = "key,description\n1,alpha\n2,beta\n";
        var options = new TextChunkParser.Options(false, true, false, 0, false, 512);
        var parsed = TextChunkParser.parseTextDocument(new File("items.csv"), csv, options);
        assertEquals(2, parsed.source().passages().size());
        for (var p : parsed.source().passages()) {
            assertFalse(p.hasExactSpan());
            assertNull(p.metadata().get("source_span_coordinate_system"));
            assertNull(p.pageNumber());
        }
        var graph = SourceDocumentHierarchyBuilder.build(parsed.source());
        assertTrue(GraphSemanticValidator.validate(graph,
            GraphSemanticValidator.Mode.STRICT_TYPED).isValid());
        assertTrue(graph.getEvidenceRecords().stream().allMatch(e -> e.getStartOffset() == null
            && e.getEndOffset() == null && e.getQuote() == null));
    }

    @Test
    void twoPagePdfKeepsExactPageExtractedOffsetsAndGraphContainment() throws Exception {
        Path file = Files.createTempFile("trinity-source-hierarchy-", ".pdf");
        try {
            try (PDDocument pdf = new PDDocument()) {
                for (String phrase : List.of("first page source", "second page source")) {
                    PDPage page = new PDPage();
                    pdf.addPage(page);
                    try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                        stream.beginText();
                        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        stream.newLineAtOffset(45, 700);
                        stream.showText(phrase);
                        stream.endText();
                    }
                }
                pdf.save(file.toFile());
            }
            var parsed = TextChunkParser.parseDocument(file.toFile(), plain(2048, false));
            assertEquals(2, parsed.source().pages().size());
            assertEquals(2, parsed.source().passages().size());
            assertEquals("1", parsed.chunks().get(0).metadata().get("source_page_number"));
            assertEquals("2", parsed.chunks().get(1).metadata().get("source_page_number"));
            for (var passage : parsed.source().passages()) {
                assertTrue(passage.hasExactSpan());
                assertEquals(passage.text(), parsed.source().canonicalText().substring(
                    passage.startOffset(), passage.endOffset()));
            }
            var graph = SourceDocumentHierarchyBuilder.build(parsed.source());
            assertEquals(5, graph.getNodes().size()); // document + two pages + two passages
            var index = GraphHierarchyIndex.build(graph)
                .hierarchy("source:" + parsed.source().documentId(), "CONTAINS");
            for (var passage : parsed.source().passages()) {
                assertEquals(1, index.immediateParents(passage.passageId()).size());
                assertEquals(2, index.ancestors(passage.passageId()).size());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
