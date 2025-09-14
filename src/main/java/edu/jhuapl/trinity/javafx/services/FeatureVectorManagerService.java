package edu.jhuapl.trinity.javafx.services;

import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import javafx.beans.property.*;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.List;

public interface FeatureVectorManagerService {

    enum SamplingMode { ALL, HEAD_1000, TAIL_1000, RANDOM_1000 }

    // Collections
    ObservableList<String> getCollectionNames();
    StringProperty activeCollectionNameProperty();

    // Displayed vectors
    ObservableList<FeatureVector> getDisplayedVectors();
    ObjectProperty<SamplingMode> samplingModeProperty();

    // Counts
    ReadOnlyIntegerProperty totalVectorCountProperty();
    ReadOnlyIntegerProperty displayedCountProperty();

    // Mutations
    void addCollection(String name, List<FeatureVector> vectors);
    void removeCollection(String name);
    void renameCollection(String oldName, String newName);
    void appendVectorsToActive(List<FeatureVector> vectors);
    void replaceActiveVectors(List<FeatureVector> vectors);

    // Import / Export (background)
    void importJsonAsync(String collectionName, Path jsonPath);
    void importCsvAsync(String collectionName, Path csvPath, int expectedDim);
    void exportCsvAsync(Path csvPath, List<FeatureVector> source);

    // Status / progress
    ReadOnlyStringProperty statusProperty();
    ReadOnlyDoubleProperty progressProperty();

    // Integration back to app (fires FeatureVectorEvent subtype)
    void applyActiveToWorkspace();

    // Lifecycle
    void dispose();
    /**
     * Derive a collection name from an import hint (e.g. filename) or fall back to a generic name.
     * @param hint may be a String path, filename, or null
     * @param fc   the FeatureCollection being imported
     * @return a suitable collection name
     */
    public static String deriveCollectionName(Object hint, FeatureCollection fc) {
        String name = null;
        if (hint instanceof String pathOrHint) {
            int slash = Math.max(pathOrHint.lastIndexOf('/'), pathOrHint.lastIndexOf('\\'));
            name = (slash >= 0) ? pathOrHint.substring(slash + 1) : pathOrHint;
            int dot = name.lastIndexOf('.');
            if (dot > 0) name = name.substring(0, dot);
        }
        if (name == null || name.isBlank()) {
            name = "FeatureCollection-" + System.currentTimeMillis();
        }
        return name;
    }    
}
