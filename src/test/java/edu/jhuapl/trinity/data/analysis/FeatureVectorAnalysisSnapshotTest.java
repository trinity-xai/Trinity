package edu.jhuapl.trinity.data.analysis;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FeatureVectorAnalysisSnapshotTest {
    @Test
    void detachedOrderedSnapshotCannotBeMutatedThroughOriginalVectors() {
        FeatureVector one = new FeatureVector(List.of(1.0, 2.0), "A");
        one.setEntityId("chunk-one");
        one.getMetaData().put("document", "reportA");
        FeatureVector two = new FeatureVector(List.of(3.0, 4.0), "B");
        two.setEntityId("chunk-two");
        var snapshot = FeatureVectorAnalysisSnapshot.fromVectors("doc", List.of(one, two));
        one.getData().set(0, 99.0);
        one.getMetaData().put("document", "mutated");
        assertEquals(List.of("chunk-one", "chunk-two"), snapshot.inputRowIds());
        assertEquals(1.0, snapshot.rows().get(0).data().get(0));
        assertEquals("reportA", snapshot.rows().get(0).metadata().get("document"));
        assertEquals(1, snapshot.rowById().get("chunk-two"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.rows().get(0).data().add(1.0));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.rows().get(0).metadata().put("x", "y"));
        double[][] matrix = snapshot.toDenseMatrix();
        matrix[0][0] = 333;
        assertEquals(1.0, snapshot.toDenseMatrix()[0][0]);
    }

    @Test
    void duplicateEntityIdsAreRejected() {
        FeatureVector a = new FeatureVector(); a.setEntityId("same");
        FeatureVector b = new FeatureVector(); b.setEntityId("same");
        assertThrows(IllegalArgumentException.class,
            () -> FeatureVectorAnalysisSnapshot.fromVectors("duplicate", List.of(a, b)));
    }
}
