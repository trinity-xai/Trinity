package edu.jhuapl.trinity.javafx.services;

import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Central manager API for FeatureVectors grouped as named collections.
 */
public interface FeatureVectorManagerService {

    public static String MANAGER_APPLY_TAG = "FV_MANAGER_APPLY";

    enum SamplingMode { ALL, HEAD_1000, TAIL_1000, RANDOM_1000 }
    enum ExportFormat { JSON, CSV }

    /** Live list of collection names (do not replace the instance; mutate it). */
    ObservableList<String> getCollectionNames();

    /** Name of the currently active collection. */
    StringProperty activeCollectionNameProperty();

    /** Live list of vectors to display (sampling applied). */
    ObservableList<FeatureVector> getDisplayedVectors();

    /** Current sampling mode. */
    ObjectProperty<SamplingMode> samplingModeProperty();

    /** Add a new collection (or replace existing if same name), selecting it active. */
    void addCollection(String proposedName, List<FeatureVector> vectors);

    /** Append vectors to the currently active collection. */
    void appendVectorsToActive(List<FeatureVector> vectors);

    /** Replace the vectors in the currently active collection. */
    void replaceActiveVectors(List<FeatureVector> vectors);

    /** Rename a collection. */
    void renameCollection(String oldName, String newName);

    /** Duplicate a collection; returns new collection name. */
    String duplicateCollection(String sourceName, String proposedName);

    /** Delete a collection. */
    void deleteCollection(String name);

    /** Merge source collection into target. Optionally de-duplicate by entityId. */
    void mergeInto(String targetName, String sourceName, boolean dedupByEntityId);

    /** Export a collection to a file in the given format. */
    void exportCollection(String name, File file, ExportFormat format) throws Exception;

    /** Remove specific vectors from the active collection. */
    void removeFromActive(List<FeatureVector> toRemove);

    /** Copy specific vectors to a target collection (create if missing). */
    void copyToCollection(List<FeatureVector> toCopy, String targetCollection);

    /** Bulk set label on selected vectors in active collection. */
    void bulkSetLabelInActive(List<FeatureVector> targets, String newLabel);

    /** Bulk edit metadata (upsert keys) on selected vectors in active collection. */
    void bulkEditMetadataInActive(List<FeatureVector> targets, Map<String, String> kv);

    /** Fire APPLY_ACTIVE_FEATUREVECTORS back to the app (scene root). */
    void applyActiveToWorkspace(boolean replace);

    // Convenience default 
    default void applyActiveToWorkspace() {
        applyActiveToWorkspace(false);
    }
    /** Optional: Where events should be fired (e.g., scene.getRoot()). */
    void setEventTarget(EventTarget target);

    // ---------- Shared naming helper ----------
    /**
     * Derive a collection name from an import hint (e.g., file path) or fall back to a generic name.
     */
    static String deriveCollectionName(Object hint, FeatureCollection fc) {
        String name = null;
        if (hint instanceof String pathOrHint) {
            int slash = Math.max(pathOrHint.lastIndexOf('/'), pathOrHint.lastIndexOf('\\'));
            name = (slash >= 0) ? pathOrHint.substring(slash + 1) : pathOrHint;
            int dot = name.lastIndexOf('.');
            if (dot > 0) name = name.substring(0, dot);
        }
        if (name == null || name.trim().isEmpty()) {
            name = "FeatureCollection-" + System.currentTimeMillis();
        }
        return name.trim();
    }
}
