/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.renderers;

import edu.jhuapl.trinity.data.messages.FeatureCollection;
import edu.jhuapl.trinity.data.messages.SemanticMap;
import edu.jhuapl.trinity.data.messages.SemanticMapCollection;

/**
 * @author Sean Phillips
 */
public interface SemanticMapRenderer {
    //This method is cheating harder than Shakira's ex...
    public void setFeatureCollection(FeatureCollection featureCollection);

    public void addSemanticMapCollection(SemanticMapCollection semanticMapCollection);

    public void addSemanticMap(SemanticMap semanticMap);

    public SemanticMap getSemanticMap(long id);

    public void locateSemanticMap(SemanticMap semanticMap);

    public void clearSemanticMaps();
}
