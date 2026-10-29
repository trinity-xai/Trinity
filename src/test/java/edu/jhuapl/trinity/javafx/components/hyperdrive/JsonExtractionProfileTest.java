package edu.jhuapl.trinity.javafx.components.hyperdrive;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonExtractionProfileTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SOURCE = """
        [{
          "properties": {
            "definition": "The desired probability-of-kill of air-to-surface targets.",
            "datatype": "NUMERIC(4,2)",
            "note": "A separate table is used for varying target types."
          },
          "entities_containing_the_attribute": [{
            "properties": {"field": "DesiredPk"}, "entity_name": "Air_Command"
          }],
          "attribute_name": "A2S_Desired_Kill_Probability"
        }]
        """;

    private static JsonExtractionProfile profile() {
        JsonExtractionProfile p = new JsonExtractionProfile();
        p.setName("Attribute definitions");
        p.setFields(List.of(
            new JsonExtractionProfile.FieldMapping("attribute_name", true, true, false),
            new JsonExtractionProfile.FieldMapping("properties.definition", true, false, false),
            new JsonExtractionProfile.FieldMapping("properties.note", true, false, false),
            new JsonExtractionProfile.FieldMapping("properties.datatype", false, false, true),
            new JsonExtractionProfile.FieldMapping("entities_containing_the_attribute[].entity_name", false, false, true)
        ));
        return p;
    }

    @Test void discoversNestedFieldPathsAndArrayPaths() throws Exception {
        var paths = JsonExtractionProfile.discoverPaths(MAPPER.readTree(SOURCE));
        assertTrue(paths.contains("properties.definition"));
        assertTrue(paths.contains("entities_containing_the_attribute[].entity_name"));
        assertTrue(paths.contains("attribute_name"));
    }

    @Test void mappedImportPreservesOnlySelectedSemanticContent() throws Exception {
        var chunks = TextChunkParser.parseText(new File("attributes.json"), SOURCE,
            new TextChunkParser.Options(true, false, false, 0, false, 16384, profile()));
        assertEquals(1, chunks.size());
        var chunk = chunks.get(0);
        assertEquals("A2S_Desired_Kill_Probability", chunk.label());
        assertTrue(chunk.text().contains("desired probability-of-kill"));
        assertTrue(chunk.text().contains("separate table"));
        assertFalse(chunk.text().contains("entities_containing_the_attribute"));
        assertFalse(chunk.text().contains("NUMERIC"));
        assertEquals("NUMERIC(4,2)", chunk.metadata().get("properties.datatype"));
        assertEquals("Air_Command", chunk.metadata().get("entities_containing_the_attribute[].entity_name"));
        assertEquals("0", chunk.metadata().get("source_json_index"));
    }

    @Test void emptyMappingDoesNotFallbackToRawJson() {
        JsonExtractionProfile p = new JsonExtractionProfile();
        p.setFields(List.of(new JsonExtractionProfile.FieldMapping("missing.path", true, false, false)));
        assertThrows(java.io.IOException.class,
            () -> TextChunkParser.parseText(new File("attributes.json"), SOURCE,
                new TextChunkParser.Options(true, false, false, 0, false, 16384, p)));
    }

    @Test void profileJacksonRoundTripAndJobSnapshot() throws Exception {
        JsonExtractionProfile original = profile();
        JsonExtractionProfile copy = original.copy();
        original.getFields().get(0).setEmbedding(false);
        assertTrue(copy.getFields().get(0).isEmbedding());
        JsonExtractionProfile roundTrip = MAPPER.readValue(
            MAPPER.writeValueAsString(copy), JsonExtractionProfile.class);
        assertTrue(roundTrip.getFields().get(0).isEmbedding());
        assertEquals("Attribute definitions", roundTrip.getName());
    }
}
