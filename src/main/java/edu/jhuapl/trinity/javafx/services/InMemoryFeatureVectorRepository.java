package edu.jhuapl.trinity.javafx.services;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class InMemoryFeatureVectorRepository implements FeatureVectorRepository {
    private final Map<String, List<FeatureVector>> map = new LinkedHashMap<>();
    private final ObservableList<String> collectionNames = FXCollections.observableArrayList();

    @Override public ObservableList<String> getCollectionNames() { return collectionNames; }

    @Override public void put(String name, List<FeatureVector> vectors) {
        if (name == null) return;
        map.put(name, (vectors == null) ? new ArrayList<>() : new ArrayList<>(vectors));
        if (!collectionNames.contains(name)) collectionNames.add(name);
    }

    @Override public void remove(String name) {
        if (name == null) return;
        map.remove(name);
        collectionNames.remove(name);
    }

    @Override public boolean contains(String name) { return map.containsKey(name); }

    @Override public List<FeatureVector> get(String name) {
        return map.getOrDefault(name, Collections.emptyList());
    }

    @Override public void clear() {
        map.clear();
        collectionNames.clear();
    }
}
