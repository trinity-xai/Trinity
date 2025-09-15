package edu.jhuapl.trinity.javafx.services;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventTarget;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import javafx.scene.Node;

/**
 * In-memory implementation; keeps a map of named collections and exposes a sampled "displayed" list.
 */
public class FeatureVectorManagerServiceImpl implements FeatureVectorManagerService {

    private final ObservableList<String> collectionNames = FXCollections.observableArrayList();
    private final Map<String, List<FeatureVector>> collections = new LinkedHashMap<>();
    private final StringProperty activeCollectionName = new SimpleStringProperty();
    private final ObservableList<FeatureVector> displayedVectors = FXCollections.observableArrayList();
    private final ObjectProperty<SamplingMode> samplingMode = new SimpleObjectProperty<>(SamplingMode.ALL);
    private Node eventTarget;

    public FeatureVectorManagerServiceImpl(InMemoryFeatureVectorRepository ignoredRepo) {
        // repo placeholder kept for drop-in compatibility; not used in this in-memory impl.

        // keep displayedVectors in sync whenever the active name or sampling mode changes
        activeCollectionName.addListener((obs, o, n) -> refreshDisplayedFromActive());
        samplingMode.addListener((obs, o, n) -> refreshDisplayedFromActive());
    }

    @Override public ObservableList<String> getCollectionNames() { return collectionNames; }
    @Override public StringProperty activeCollectionNameProperty() { return activeCollectionName; }
    @Override public ObservableList<FeatureVector> getDisplayedVectors() { return displayedVectors; }
    @Override public ObjectProperty<SamplingMode> samplingModeProperty() { return samplingMode; }

    @Override
    public void addCollection(String proposedName, List<FeatureVector> vectors) {
        if (vectors == null) vectors = List.of();
        final String clean = cleanName(proposedName);
        final String name = uniquify(clean);
        final List<FeatureVector> payload = List.copyOf(vectors); // defensive

        runFx(() -> {
            collections.put(name, new ArrayList<>(payload));
            if (!collectionNames.contains(name)) collectionNames.add(name);
            // Select new collection by default
            activeCollectionName.set(name);
            refreshDisplayedFromActive();
        });
    }

    @Override
    public void appendVectorsToActive(List<FeatureVector> vectors) {
        if (vectors == null || vectors.isEmpty()) return;
        runFx(() -> {
            String name = activeCollectionName.get();
            if (name == null) {
                // No collection yet: create a default one
                name = uniquify("Collection");
                collections.put(name, new ArrayList<>());
                collectionNames.add(name);
                activeCollectionName.set(name);
            }
            collections.computeIfAbsent(name, k -> new ArrayList<>()).addAll(vectors);
            refreshDisplayedFromActive();
        });
    }

    @Override
    public void replaceActiveVectors(List<FeatureVector> vectors) {
        runFx(() -> {
            String name = activeCollectionName.get();
            if (name == null) {
                name = uniquify("Collection");
                collectionNames.add(name);
                activeCollectionName.set(name);
            }
            collections.put(name, new ArrayList<>(vectors == null ? List.of() : vectors));
            refreshDisplayedFromActive();
        });
    }

    @Override
    public void applyActiveToWorkspace() {
        runFx(() -> {
            String name = activeCollectionName.get();
            List<FeatureVector> active = (name == null) ? List.of()
                : Collections.unmodifiableList(collections.getOrDefault(name, List.of()));
            if (eventTarget != null) {
                FeatureVectorEvent evt = new FeatureVectorEvent(FeatureVectorEvent.APPLY_ACTIVE_FEATUREVECTORS, active);
                eventTarget.fireEvent(evt);
            }
        });
    }

@Override
public void setEventTarget(EventTarget target) {
    if (target instanceof Node n) {
        this.eventTarget = n;
    } else {
        this.eventTarget = null;
    }
}

    // ---------- internals ----------

    private void refreshDisplayedFromActive() {
        String name = activeCollectionName.get();
        List<FeatureVector> src = (name == null) ? List.of() : collections.getOrDefault(name, List.of());
        List<FeatureVector> sampled = sample(src, samplingMode.get());
        displayedVectors.setAll(sampled);
    }

    private static List<FeatureVector> sample(List<FeatureVector> src, SamplingMode mode) {
        if (src == null || src.isEmpty()) return List.of();
        int n = src.size();
        switch (mode) {
            case ALL -> { return src; }
            case HEAD_1000 -> { return src.subList(0, Math.min(1000, n)); }
            case TAIL_1000 -> {
                if (n <= 1000) return src;
                return src.subList(n - 1000, n);
            }
            case RANDOM_1000 -> {
                if (n <= 1000) return src;
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                // reservoir-like quick sample
                ArrayList<FeatureVector> copy = new ArrayList<>(src);
                for (int i = 0; i < 1000; i++) {
                    int j = i + rng.nextInt(n - i);
                    Collections.swap(copy, i, j);
                }
                return copy.subList(0, 1000);
            }
            default -> { return src; }
        }
    }

    private static String cleanName(String s) {
        if (s == null) return "";
        return s.trim();
    }

    private String uniquify(String base) {
        String b = (base == null || base.isBlank()) ? "Collection" : base.trim();
        String name = b;
        int i = 2;
        while (collectionNames.contains(name)) {
            name = b + "-" + i++;
        }
        return name;
    }

    private static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}
