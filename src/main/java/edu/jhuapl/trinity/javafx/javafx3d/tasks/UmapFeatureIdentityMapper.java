package edu.jhuapl.trinity.javafx.javafx3d.tasks;

import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.services.FeatureVectorUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure helper for preserving original FeatureVector identity across projection results. */
public final class UmapFeatureIdentityMapper {
    private UmapFeatureIdentityMapper() { }
    /** Pure identity-preserving mapping, isolated for deterministic unit tests. */
    public static FeatureCollection mapProjectionRows(FeatureCollection originalFC, double[][] umapMatrix) {
        if (originalFC == null || originalFC.getFeatures() == null || umapMatrix == null) {
            throw new IllegalArgumentException("UMAP input/output cannot be null");
        }
        // Projection changes coordinates, not the identity of the input observation.
        // Preserve exact row order + entityId; never alias mutable source metadata.
        List<FeatureVector> originals = originalFC.getFeatures();
        if (umapMatrix.length != originals.size()) {
            throw new IllegalStateException("UMAP row count differs from input FeatureCollection");
        }
        List<FeatureVector> projected = new ArrayList<>(originals.size());
        Set<String> seenIds = new HashSet<>();
        for (int i = 0; i < originals.size(); i++) {
            FeatureVector source = originals.get(i);
            String id = source.getEntityId();
            if (id == null || id.isBlank() || !seenIds.add(id)) {
                throw new IllegalArgumentException("Missing or duplicate UMAP source entityId at row " + i);
            }
            FeatureVector mapped = FeatureVectorUtils.cloneVector(source);
            List<Double> coordinates = new ArrayList<>(umapMatrix[i].length);
            for (double v : umapMatrix[i]) coordinates.add(v);
            mapped.setData(coordinates);
            // The legacy FeatureVector setter does not synchronize metadata.uuid.
            // Normalize only in this derived copy; never mutate the source vector.
            if (mapped.getMetaData() != null) mapped.getMetaData().put("uuid", id);
            projected.add(mapped);
        }
        FeatureCollection projectedFC = new FeatureCollection();
        projectedFC.setFeatures(projected);
        return projectedFC;
    }

}
