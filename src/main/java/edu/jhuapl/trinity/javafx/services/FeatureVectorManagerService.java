package edu.jhuapl.trinity.javafx.services;

import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

/**
 * Central manager API for FeatureVectors grouped as named collections.
 */
public interface FeatureVectorManagerService {

    enum SamplingMode { ALL, HEAD_1000, TAIL_1000, RANDOM_1000 }

    /** Live list of collection names (do not replace the instance; mutate it). */
    ObservableList<String> getCollectionNames();

    /** Name of the currently active collection. */
    StringProperty activeCollectionNameProperty();

    /** Live list of vectors to display (sampling applied). */
    ObservableList<FeatureVector> getDisplayedVectors();

    /** Current sampling mode. */
    ObjectProperty<SamplingMode> samplingModeProperty();

    /** Add a new collection (or replace existing if same name), selecting it active. */
    void addCollection(String proposedName, java.util.List<FeatureVector> vectors);

    /** Append vectors to the currently active collection. */
    void appendVectorsToActive(java.util.List<FeatureVector> vectors);

    /** Replace the vectors in the currently active collection. */
    void replaceActiveVectors(java.util.List<FeatureVector> vectors);

    /** Fire APPLY_ACTIVE_FEATUREVECTORS back to the app (scene root). */
    void applyActiveToWorkspace();

    /** Optional: Where events should be fired (e.g., scene.getRoot()). */
    void setEventTarget(javafx.event.EventTarget target);

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
