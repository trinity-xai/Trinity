package edu.jhuapl.trinity.javafx.services;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventTarget;
import javafx.scene.Node;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeatureVectorManagerServiceImpl implements FeatureVectorManagerService {

    private final FeatureVectorRepository repo;
    private final ObservableList<FeatureVector> displayed = FXCollections.observableArrayList();

    private final StringProperty activeCollection = new SimpleStringProperty("");
    private final ObjectProperty<SamplingMode> samplingMode = new SimpleObjectProperty<>(SamplingMode.ALL);

    private final ReadOnlyIntegerWrapper totalCount = new ReadOnlyIntegerWrapper(0);
    private final ReadOnlyIntegerWrapper displayedCount = new ReadOnlyIntegerWrapper(0);

    private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper("Ready.");
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(-1);

    private final ExecutorService executor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()/2));

    // Where to fire events (typically scene.getRoot())
    private EventTarget eventTarget;

    public FeatureVectorManagerServiceImpl(FeatureVectorRepository repo) {
        this.repo = Objects.requireNonNull(repo, "repo");
        activeCollection.addListener((obs,o,n) -> recomputeDisplayed());
        samplingMode.addListener((obs,o,n) -> recomputeDisplayed());
    }

    public void setEventTarget(EventTarget target) { this.eventTarget = target; }

    @Override public ObservableList<String> getCollectionNames() { return repo.getCollectionNames(); }
    @Override public StringProperty activeCollectionNameProperty() { return activeCollection; }
    @Override public ObservableList<FeatureVector> getDisplayedVectors() { return displayed; }
    @Override public ObjectProperty<SamplingMode> samplingModeProperty() { return samplingMode; }

    @Override public ReadOnlyIntegerProperty totalVectorCountProperty() { return totalCount.getReadOnlyProperty(); }
    @Override public ReadOnlyIntegerProperty displayedCountProperty() { return displayedCount.getReadOnlyProperty(); }

    @Override public ReadOnlyStringProperty statusProperty() { return status.getReadOnlyProperty(); }
    @Override public ReadOnlyDoubleProperty progressProperty() { return progress.getReadOnlyProperty(); }

    @Override
    public void addCollection(String name, List<FeatureVector> vectors) {
        if (name == null || name.isBlank()) return;
        repo.put(name, vectors);
        if (activeCollection.get().isBlank()) activeCollection.set(name);
        recomputeDisplayed();
        setStatus("Added collection '" + name + "' (" + (vectors==null?0:vectors.size()) + " vectors).");
    }

    @Override
    public void removeCollection(String name) {
        repo.remove(name);
        if (name.equals(activeCollection.get())) {
            activeCollection.set(repo.getCollectionNames().isEmpty() ? "" :
                repo.getCollectionNames().get(0));
        }
        recomputeDisplayed();
        setStatus("Removed collection '" + name + "'.");
    }

    @Override
    public void renameCollection(String oldName, String newName) {
        if (!repo.contains(oldName) || newName == null || newName.isBlank()) return;
        List<FeatureVector> v = new ArrayList<>(repo.get(oldName));
        repo.remove(oldName);
        repo.put(newName, v);
        if (oldName.equals(activeCollection.get())) activeCollection.set(newName);
        setStatus("Renamed collection '" + oldName + "' → '" + newName + "'.");
    }

    @Override
    public void appendVectorsToActive(List<FeatureVector> vectors) {
        if (vectors == null || vectors.isEmpty()) return;
        String name = activeCollection.get();
        if (name == null || name.isBlank()) {
            name = "Collection-" + (repo.getCollectionNames().size() + 1);
            repo.put(name, new ArrayList<>());
            activeCollection.set(name);
        }
        repo.get(name).addAll(vectors);
        recomputeDisplayed();
        setStatus("Appended " + vectors.size() + " vectors to '" + name + "'.");
    }

    @Override
    public void replaceActiveVectors(List<FeatureVector> vectors) {
        String name = activeCollection.get();
        if (name == null || name.isBlank()) {
            name = "Collection-" + (repo.getCollectionNames().size() + 1);
            repo.put(name, new ArrayList<>());
            activeCollection.set(name);
        }
        List<FeatureVector> dest = repo.get(name);
        dest.clear();
        if (vectors != null) dest.addAll(vectors);
        recomputeDisplayed();
        setStatus("Replaced active collection with " + dest.size() + " vectors.");
    }

    @Override
    public void importJsonAsync(String collectionName, Path jsonPath) {
        Task<List<FeatureVector>> task = new Task<>() {
            @Override protected List<FeatureVector> call() throws Exception {
                updateMessage("Importing JSON: " + jsonPath.getFileName());
                updateProgress(-1, 1);
                Thread.sleep(200);
                return List.of();
            }
        };
        task.messageProperty().addListener((obs,o,n) -> setStatus(n));
        bindProgress(task);
        task.setOnSucceeded(e -> { addCollection(collectionName, task.getValue()); clearProgress(); });
        task.setOnFailed(e -> { setStatus("Import failed: " + task.getException()); clearProgress(); });
        executor.submit(task);
    }

    @Override
    public void importCsvAsync(String collectionName, Path csvPath, int expectedDim) {
        Task<List<FeatureVector>> task = new Task<>() {
            @Override protected List<FeatureVector> call() throws Exception {
                updateMessage("Importing CSV: " + csvPath.getFileName());
                updateProgress(-1, 1);
                Thread.sleep(200);
                return List.of();
            }
        };
        task.messageProperty().addListener((obs,o,n) -> setStatus(n));
        bindProgress(task);
        task.setOnSucceeded(e -> { addCollection(collectionName, task.getValue()); clearProgress(); });
        task.setOnFailed(e -> { setStatus("Import failed: " + task.getException()); clearProgress(); });
        executor.submit(task);
    }

    @Override
    public void exportCsvAsync(Path csvPath, List<FeatureVector> source) {
        if (source == null) source = getActive();
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                updateMessage("Exporting CSV: " + csvPath.getFileName());
                updateProgress(-1, 1);
                Thread.sleep(200);
                return null;
            }
        };
        task.messageProperty().addListener((obs,o,n) -> setStatus(n));
        bindProgress(task);
        task.setOnSucceeded(e -> { setStatus("Export complete: " + csvPath.getFileName()); clearProgress(); });
        task.setOnFailed(e -> { setStatus("Export failed: " + task.getException()); clearProgress(); });
        executor.submit(task);
    }

@Override
public void applyActiveToWorkspace() {
    if (eventTarget == null) {
        setStatus("No event target set; cannot apply to workspace.");
        return;
    }
    List<FeatureVector> flat = new ArrayList<>(getActive());
    Platform.runLater(() -> {
        FeatureVectorEvent ev = new FeatureVectorEvent(
            FeatureVectorEvent.APPLY_ACTIVE_FEATUREVECTORS,
            flat
        );
        ev.clearExisting = false; // or true if you want to reset consumers
        if (eventTarget instanceof Node node) {
            node.fireEvent(ev);
        }
    });
    setStatus("Applied " + flat.size() + " vectors to workspace.");
}

    private List<FeatureVector> getActive() {
        String name = activeCollection.get();
        return (name == null || name.isBlank()) ? List.of() : repo.get(name);
    }

    private void recomputeDisplayed() {
        List<FeatureVector> active = getActive();
        totalCount.set(active.size());
        List<FeatureVector> sampled = DisplayListSampler.sample(active, samplingMode.get());
        Platform.runLater(() -> {
            displayed.setAll(sampled);
            displayedCount.set(displayed.size());
        });
    }

    private void setStatus(String s) { Platform.runLater(() -> status.set(s == null ? "" : s)); }

    private void bindProgress(Task<?> t) {
        t.progressProperty().addListener((obs,o,n) -> {
            double val = (n == null) ? -1 : n.doubleValue();
            Platform.runLater(() -> progress.set(val));
        });
    }

    private void clearProgress() { Platform.runLater(() -> progress.set(-1)); }

    @Override public void dispose() { executor.shutdownNow(); }
}
