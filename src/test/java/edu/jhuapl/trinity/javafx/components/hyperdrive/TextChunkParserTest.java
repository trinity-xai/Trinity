package edu.jhuapl.trinity.javafx.components.hyperdrive;

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

class TextChunkParserTest {
    private static final File TEXT = new File("sample.txt");
    private static final File JSON = new File("sample.json");
    private static final File CSV = new File("sample.csv");

    private static TextChunkParser.Options plain(int size) {
        return new TextChunkParser.Options(false, false, false, 0, false, size);
    }

    @Test
    void exactMultipleHasNoEmptyChunkAndKeepsProvenance() throws Exception {
        var chunks = TextChunkParser.parseText(TEXT, "abcdefgh", plain(4));
        assertEquals(List.of("abcd", "efgh"), chunks.stream().map(TextChunkParser.ParsedChunk::text).toList());
        assertEquals("0", chunks.get(0).metadata().get("source_chunk_index"));
        assertEquals("2", chunks.get(0).metadata().get("source_chunk_count"));
    }

    @Test
    void noBlankChunksAreEmitted() throws Exception {
        assertTrue(TextChunkParser.parseText(TEXT, "  \t  ", plain(4)).isEmpty());
        assertEquals(2, TextChunkParser.parseText(TEXT, "a\r\n\r\nb\n", new TextChunkParser.Options(false, false, false, 0, true, 20)).size());
    }

    @Test
    void jsonObjectTextMatchesEmbeddingSource() throws Exception {
        var chunks = TextChunkParser.parseText(JSON,
            "{\"name\":\"doc 1\",\"text\":\"actual content\",\"topic\":\"network\"}",
            new TextChunkParser.Options(true, false, false, 0, false, 20));
        assertEquals(1, chunks.size());
        assertEquals("actual content", chunks.get(0).text());
        assertEquals("doc 1", chunks.get(0).label());
        assertEquals("\"network\"", chunks.get(0).metadata().get("topic"));
    }

    @Test
    void jsonArrayCreatesIndexedObjects() throws Exception {
        var chunks = TextChunkParser.parseText(JSON, "[{\"text\":\"first\"},{\"text\":\"second\"}]",
            new TextChunkParser.Options(true, false, false, 0, false, 20));
        assertEquals(List.of("first", "second"), chunks.stream().map(TextChunkParser.ParsedChunk::text).toList());
        assertEquals("1", chunks.get(1).metadata().get("source_json_index"));
    }

    @Test
    void csvHandlesQuotedCommasMultilineFieldsAndEscapedQuotes() throws Exception {
        String content = "id,description,category\r\n"
            + "1,\"high, unusual traffic\",warning\r\n"
            + "2,\"first line\nsecond \"\"quoted\"\" line\",info\r\n";
        var chunks = TextChunkParser.parseText(CSV, content,
            new TextChunkParser.Options(false, true, true, 0, false, 16));
        assertEquals(2, chunks.size());
        assertEquals("1", chunks.get(0).label());
        assertTrue(chunks.get(0).text().contains("description = high, unusual traffic"));
        assertTrue(chunks.get(1).text().contains("first line\nsecond \"quoted\" line"));
        assertEquals("3", chunks.get(1).metadata().get("source_csv_record"));
    }

    @Test
    void malformedCsvFailsExplicitly() {
        assertThrows(java.io.IOException.class,
            () -> TextChunkParser.parseCsvRecords("heading\n\"unclosed"));
    }

    @Test
    void smallPdfIsExtractedInsteadOfReadAsBinary() throws Exception {
        Path file = Files.createTempFile("trinity-hyperdrive-pdf-", ".pdf");
        try {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(40, 700);
                    stream.showText("PDF text preserved");
                    stream.endText();
                }
                document.save(file.toFile());
            }
            var chunks = TextChunkParser.parse(file.toFile(), plain(2048));
            assertEquals(1, chunks.size());
            assertTrue(chunks.get(0).text().contains("PDF text preserved"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
