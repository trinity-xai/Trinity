package edu.jhuapl.trinity.javafx.services;

import edu.jhuapl.trinity.data.files.FeatureCollectionFile;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
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
import javafx.scene.Node;
import javafx.scene.Scene;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * In-memory implementation; keeps a map of named collections and exposes a sampled/filtered "displayed" list.
 */
public class FeatureVectorManagerServiceImpl implements FeatureVectorManagerService {

    private final ObservableList<String> collectionNames = FXCollections.observableArrayList();
    private final Map<String, List<FeatureVector>> collections = new LinkedHashMap<>();
    private final StringProperty activeCollectionName = new SimpleStringProperty();
    private final ObservableList<FeatureVector> displayedVectors = FXCollections.observableArrayList();
    private final ObjectProperty<SamplingMode> samplingMode = new SimpleObjectProperty<>(SamplingMode.ALL);
    private final StringProperty textFilter = new SimpleStringProperty("");

    // we store the target as Node or Scene to be able to call fireEvent
    private Node eventNode;
    private Scene eventScene;

    public FeatureVectorManagerServiceImpl(InMemoryFeatureVectorRepository ignoredRepo) {
        // keep displayedVectors in sync whenever active name, sampling mode, or filter changes
        activeCollectionName.addListener((obs, o, n) -> refreshDisplayedFromActive());
        samplingMode.addListener((obs, o, n) -> refreshDisplayedFromActive());
        textFilter.addListener((obs, o, n) -> refreshDisplayedFromActive());
    }

    @Override
    public ObservableList<String> getCollectionNames() {
        return collectionNames;
    }

    @Override
    public StringProperty activeCollectionNameProperty() {
        return activeCollectionName;
    }

    @Override
    public ObservableList<FeatureVector> getDisplayedVectors() {
        return displayedVectors;
    }

    @Override
    public ObjectProperty<SamplingMode> samplingModeProperty() {
        return samplingMode;
    }

    @Override
    public StringProperty textFilterProperty() {
        return textFilter;
    }

    @Override
    public void addCollection(String proposedName, List<FeatureVector> vectors) {
        if (vectors == null) vectors = List.of();
        final String clean = FeatureVectorUtils.cleanName(proposedName);
        final String name = uniquify(clean);
        final List<FeatureVector> payload = FeatureVectorUtils.copyVectors(vectors);

        runFx(() -> {
            collections.put(name, payload);
            if (!collectionNames.contains(name)) collectionNames.add(name);
            activeCollectionName.set(name);
            refreshDisplayedFromActive();
        });
    }

    @Override
    public void appendVectorsToActive(List<FeatureVector> vectors) {
        if (vectors == null || vectors.isEmpty()) return;
        runFx(() -> {
            String name = ensureActiveCollection();
            collections.computeIfAbsent(name, k -> new ArrayList<>()).addAll(FeatureVectorUtils.copyVectors(vectors));
            refreshDisplayedFromActive();
        });
    }

    @Override
    public void replaceActiveVectors(List<FeatureVector> vectors) {
        runFx(() -> {
            String name = ensureActiveCollection();
            collections.put(name, FeatureVectorUtils.copyVectors(vectors == null ? List.of() : vectors));
            refreshDisplayedFromActive();
        });
    }

    @Override
    public void renameCollection(String oldName, String newName) {
        if (oldName == null || newName == null) return;
        final String cleanNew = uniquify(FeatureVectorUtils.cleanName(newName));
        runFx(() -> {
            List<FeatureVector> existing = collections.remove(oldName);
            if (existing == null) return;
            collections.put(cleanNew, existing);
            int idx = collectionNames.indexOf(oldName);
            if (idx >= 0) collectionNames.set(idx, cleanNew);
            if (Objects.equals(activeCollectionName.get(), oldName)) {
                activeCollectionName.set(cleanNew);
            }
            refreshDisplayedFromActive();
        });
    }

    @Override
    public String duplicateCollection(String sourceName, String proposedName) {
        if (sourceName == null) return null;
        List<FeatureVector> src = collections.get(sourceName);
        if (src == null) return null;
        final String newName = uniquify(FeatureVectorUtils.cleanName(
            (proposedName == null || proposedName.isBlank()) ? ("Copy of " + sourceName) : proposedName));
        final List<FeatureVector> payload = FeatureVectorUtils.copyVectors(src);
        runFx(() -> {
            collections.put(newName, payload);
            collectionNames.add(newName);
            activeCollectionName.set(newName);
            refreshDisplayedFromActive();
        });
        return newName;
    }

    @Override
    public void deleteCollection(String name) {
        if (name == null) return;
        runFx(() -> {
            collections.remove(name);
            collectionNames.remove(name);
            if (Objects.equals(activeCollectionName.get(), name)) {
                if (!collectionNames.isEmpty()) {
                    activeCollectionName.set(collectionNames.get(0));
                } else {
                    activeCollectionName.set(null);
                    displayedVectors.clear();
                }
            } else {
                refreshDisplayedFromActive();
            }
        });
    }

    @Override
    public void mergeInto(String targetName, String sourceName, boolean dedupByEntityId) {
        if (targetName == null || sourceName == null) return;
        runFx(() -> {
            List<FeatureVector> tgt = collections.computeIfAbsent(targetName, k -> new ArrayList<>());
            List<FeatureVector> src = collections.getOrDefault(sourceName, List.of());
            if (src.isEmpty()) return;

            if (dedupByEntityId) {
                Set<String> existingIds = tgt.stream()
                    .map(FeatureVector::getEntityId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
                for (FeatureVector fv : src) {
                    String id = fv.getEntityId();
                    if (id == null || !existingIds.contains(id)) {
                        tgt.add(FeatureVectorUtils.cloneVector(fv));
                        if (id != null) existingIds.add(id);
                    }
                }
            } else {
                tgt.addAll(FeatureVectorUtils.copyVectors(src));
            }
            refreshDisplayedFromActive();
        });
    }

    @Override
    public void exportCollection(String name, File file, ExportFormat format) throws Exception {
        if (name == null || file == null || format == null) return;
        List<FeatureVector> src = collections.getOrDefault(name, List.of());
        if (format == ExportFormat.JSON) {
            FeatureCollection fc = new FeatureCollection();
            fc.setFeatures(FeatureVectorUtils.copyVectors(src));
            FeatureCollectionFile out = new FeatureCollectionFile(file.getAbsolutePath(), false);
            out.featureCollection = fc;
            out.writeContent();
        } else { // CSV
            FeatureVectorUtils.writeCsv(file, src);
        }
    }

    @Override
    public void removeFromActive(List<FeatureVector> toRemove) {
        if (toRemove == null || toRemove.isEmpty()) return;
        runFx(() -> {
            String name = activeCollectionName.get();
            if (name == null) return;
            List<FeatureVector> list = collections.get(name);
            if (list == null) return;
            Set<String> ids = toRemove.stream()
                .map(FeatureVector::getEntityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            if (!ids.isEmpty()) {
                list.removeIf(fv -> fv.getEntityId() != null && ids.contains(fv.getEntityId()));
            } else {
                list.removeAll(toRemove);
            }
            refreshDisplayedFromActive();
        });
    }

    @Override
    public void copyToCollection(List<FeatureVector> toCopy, String targetCollection) {
        if (toCopy == null || toCopy.isEmpty() || targetCollection == null) return;
        runFx(() -> {
            String target = (collectionNames.contains(targetCollection))
                ? targetCollection
                : uniquify(targetCollection);
            collections.computeIfAbsent(target, k -> {
                if (!collectionNames.contains(target)) collectionNames.add(target);
                return new ArrayList<>();
            }).addAll(FeatureVectorUtils.copyVectors(toCopy));
            refreshDisplayedFromActive();
        });
    }

    @Override
    public void bulkSetLabelInActive(List<FeatureVector> targets, String newLabel) {
        if (targets == null || targets.isEmpty()) return;
        runFx(() -> {
            String name = activeCollectionName.get();
            if (name == null) return;
            List<FeatureVector> list = collections.get(name);
            if (list == null) return;
            Set<String> ids = targets.stream().map(FeatureVector::getEntityId).filter(Objects::nonNull).collect(Collectors.toSet());
            for (FeatureVector fv : list) {
                if ((fv.getEntityId() != null && ids.contains(fv.getEntityId())) || targets.contains(fv)) {
                    fv.setLabel(newLabel);
                }
            }
            refreshDisplayedFromActive();
        });
    }

    @Override
    public void bulkEditMetadataInActive(List<FeatureVector> targets, Map<String, String> kv) {
        if (targets == null || targets.isEmpty() || kv == null || kv.isEmpty()) return;
        runFx(() -> {
            String name = activeCollectionName.get();
            if (name == null) return;
            List<FeatureVector> list = collections.get(name);
            if (list == null) return;
            Set<String> ids = targets.stream().map(FeatureVector::getEntityId).filter(Objects::nonNull).collect(Collectors.toSet());
            for (FeatureVector fv : list) {
                if ((fv.getEntityId() != null && ids.contains(fv.getEntityId())) || targets.contains(fv)) {
                    if (fv.getMetaData() != null) {
                        kv.forEach((k, v) -> fv.getMetaData().put(k, v));
                    }
                }
            }
            refreshDisplayedFromActive();
        });
    }

    @Override
    public void applyActiveToWorkspace(boolean replace) {
        runFx(() -> {
            String name = activeCollectionName.get();
            List<FeatureVector> active = (name == null) ? List.of()
                : collections.getOrDefault(name, List.of());
            if (active == null || active.isEmpty()) return;

            var fc = new FeatureCollection();
            fc.setFeatures(active);

            var evt = new FeatureVectorEvent(
                FeatureVectorEvent.NEW_FEATURE_COLLECTION,
                fc
            );
            evt.object2 = MANAGER_APPLY_TAG;   // guard against re-mirroring
            evt.clearExisting = replace;       // replace vs. append

            if (eventNode != null) eventNode.fireEvent(evt);
            else if (eventScene != null) eventScene.getRoot().fireEvent(evt);
        });
    }

    @Override
    public void applyAllToWorkspace(boolean replace) {
        runFx(() -> {
            List<FeatureVector> all = new ArrayList<>();
            collections.values().forEach(all::addAll);
            var fc = new FeatureCollection();
            fc.setFeatures(all);

            var evt = new FeatureVectorEvent(
                FeatureVectorEvent.NEW_FEATURE_COLLECTION,
                fc
            );
            evt.object2 = MANAGER_APPLY_TAG;   // guard against re-mirroring
            evt.clearExisting = replace;       // replace vs. append

            if (eventNode != null) eventNode.fireEvent(evt);
            else if (eventScene != null) eventScene.getRoot().fireEvent(evt);
        });
    }

    @Override
    public void setEventTarget(EventTarget target) {
        if (target instanceof Node n) {
            this.eventNode = n;
            this.eventScene = null;
        } else if (target instanceof Scene s) {
            this.eventScene = s;
            this.eventNode = null;
        } else {
            this.eventNode = null;
            this.eventScene = null;
        }
    }

    // ---------- internals ----------
    private void refreshDisplayedFromActive() {
        String name = activeCollectionName.get();
        List<FeatureVector> src = (name == null) ? List.of() : collections.getOrDefault(name, List.of());

        // normalize the query once
        String q = edu.jhuapl.trinity.javafx.services.FeatureVectorUtils.normalize(textFilter.get());

        // filter by label/text/metadata if query present
        List<FeatureVector> filtered = (q == null || q.isEmpty())
            ? src
            : src.stream()
            .filter(fv -> edu.jhuapl.trinity.javafx.services.FeatureVectorUtils.matchesTextFilter(fv, q))
            .collect(java.util.stream.Collectors.toList());

        // then apply sampling
        List<FeatureVector> sampled =
            edu.jhuapl.trinity.javafx.services.FeatureVectorUtils.applySampling(filtered, samplingMode.get());

        displayedVectors.setAll(sampled);
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

    private String ensureActiveCollection() {
        String name = activeCollectionName.get();
        if (name == null) {
            name = uniquify("Collection");
            collections.put(name, new ArrayList<>());
            collectionNames.add(name);
            activeCollectionName.set(name);
        }
        return name;
    }

    private static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}
