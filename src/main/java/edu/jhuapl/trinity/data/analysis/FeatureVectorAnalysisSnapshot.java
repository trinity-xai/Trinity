package edu.jhuapl.trinity.data.analysis;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, ordered input to UMAP, clustering and document-graph builders.
 * Row positions and entity IDs are paired exactly once at snapshot capture.
 * This is a detached copy, not a view of the manager's sampled/filtered UI.
 *
 * An entityId is a vector identity, not proof of stable document provenance;
 * document/passage IDs and extraction spans will be supplied by the ingestion layer.
 */
public final class FeatureVectorAnalysisSnapshot {

    public record Row(String entityId, String label, String text,
                      List<Double> data, Map<String, String> metadata,
                      String imageURL, String mediaURL, long imageId,
                      long frameId, List<Double> bbox) {
        public Row {
            if (entityId == null || entityId.isBlank()) {
                throw new IllegalArgumentException("Analysis rows require nonblank entity IDs");
            }
            data = Collections.unmodifiableList(new ArrayList<>(data == null ? List.of() : data));
            metadata = Collections.unmodifiableMap(new LinkedHashMap<>(
                metadata == null ? Map.of() : metadata));
            bbox = Collections.unmodifiableList(new ArrayList<>(bbox == null ? List.of() : bbox));
        }
    }

    private final String collectionName;
    private final List<Row> rows;
    private final List<String> inputRowIds;
    private final Map<String, Integer> rowById;

    private FeatureVectorAnalysisSnapshot(String collectionName, List<Row> rows) {
        this.collectionName = Objects.requireNonNull(collectionName, "collectionName");
        this.rows = List.copyOf(rows);
        List<String> ids = new ArrayList<>(rows.size());
        Map<String, Integer> positions = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            String id = rows.get(i).entityId();
            if (positions.putIfAbsent(id, i) != null) {
                throw new IllegalArgumentException("Duplicate FeatureVector entityId " + id
                    + " in collection " + collectionName + "; cannot map projection rows safely");
            }
            ids.add(id);
        }
        inputRowIds = List.copyOf(ids);
        rowById = Collections.unmodifiableMap(positions);
    }

    public static FeatureVectorAnalysisSnapshot fromVectors(String collectionName,
                                                              List<FeatureVector> vectors) {
        Objects.requireNonNull(vectors, "vectors");
        List<Row> rows = new ArrayList<>(vectors.size());
        for (FeatureVector fv : vectors) {
            if (fv == null) {
                throw new IllegalArgumentException("Null FeatureVector in " + collectionName);
            }
            rows.add(new Row(fv.getEntityId(), fv.getLabel(), fv.getText(),
                fv.getData(), fv.getMetaData(), fv.getImageURL(), fv.getMediaURL(),
                fv.getImageId(), fv.getFrameId(), fv.getBbox()));
        }
        return new FeatureVectorAnalysisSnapshot(collectionName, rows);
    }

    public String collectionName() { return collectionName; }
    public int size() { return rows.size(); }
    public List<Row> rows() { return rows; }
    public List<String> inputRowIds() { return inputRowIds; }
    public Map<String, Integer> rowById() { return rowById; }
    public Optional<Row> findById(String entityId) {
        Integer index = rowById.get(entityId);
        return index == null ? Optional.empty() : Optional.of(rows.get(index));
    }

    /** Fresh dense data matrix for computation; fails fast on missing/nonfinite/ragged data. */
    public double[][] toDenseMatrix() {
        if (rows.isEmpty()) return new double[0][];
        int dimensions = rows.get(0).data().size();
        if (dimensions == 0) throw new IllegalArgumentException("Empty feature dimension");
        double[][] matrix = new double[rows.size()][dimensions];
        for (int i = 0; i < rows.size(); i++) {
            List<Double> values = rows.get(i).data();
            if (values.size() != dimensions) {
                throw new IllegalArgumentException("Ragged data at row " + i);
            }
            for (int d = 0; d < dimensions; d++) {
                Double value = values.get(d);
                if (value == null || !Double.isFinite(value)) {
                    throw new IllegalArgumentException("Non-finite data at row " + i + " dimension " + d);
                }
                matrix[i][d] = value;
            }
        }
        return matrix;
    }
}
