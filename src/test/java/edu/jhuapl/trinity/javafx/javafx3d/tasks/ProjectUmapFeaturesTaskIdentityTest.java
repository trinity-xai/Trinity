package edu.jhuapl.trinity.javafx.javafx3d.tasks;

import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProjectUmapFeaturesTaskIdentityTest {
    @Test
    void projectionPreservesEntityIdAndDoesNotAliasSourceMetadata() {
        FeatureVector first = new FeatureVector(List.of(1.0, 2.0), "source");
        first.setEntityId("passage-id");
        first.getMetaData().put("source_file_path", "report.pdf");
        FeatureCollection inputs = new FeatureCollection();
        inputs.setFeatures(List.of(first));
        FeatureCollection outputs = UmapFeatureIdentityMapper.mapProjectionRows(
            inputs, new double[][]{{0.5, -0.3, 1.1}});
        FeatureVector derived = outputs.getFeatures().get(0);
        assertEquals("passage-id", derived.getEntityId());
        assertEquals("passage-id", derived.getMetaData().get("uuid"));
        assertEquals("report.pdf", derived.getMetaData().get("source_file_path"));
        derived.getMetaData().put("source_file_path", "changed");
        assertEquals("report.pdf", first.getMetaData().get("source_file_path"));
        assertEquals(List.of(0.5, -0.3, 1.1), derived.getData());
    }
}
