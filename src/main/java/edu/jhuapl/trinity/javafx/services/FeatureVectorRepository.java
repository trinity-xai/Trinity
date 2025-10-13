package edu.jhuapl.trinity.javafx.services;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import javafx.collections.ObservableList;

import java.util.List;

public interface FeatureVectorRepository {
    ObservableList<String> getCollectionNames();

    void put(String name, List<FeatureVector> vectors);

    void remove(String name);

    boolean contains(String name);

    List<FeatureVector> get(String name);

    void clear();
}
