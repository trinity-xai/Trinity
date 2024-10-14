/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.renderers;

import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.FeatureVector;

import java.util.List;

/**
 * @author Sean Phillips
 */
public interface FeatureVectorRenderer {
    public void addFeatureCollection(FeatureCollection featureCollection);

    public void addFeatureVector(FeatureVector featureVector);

    public List<FeatureVector> getAllFeatureVectors();

    public void locateFeatureVector(FeatureVector featureVector);

    public void clearFeatureVectors();

    public void setVisibleByIndex(int i, boolean b);

    public void refresh();

    public void setSpheroidAnchor(boolean animate, int index);

    public void setDimensionLabels(List<String> labelStrings);
}
