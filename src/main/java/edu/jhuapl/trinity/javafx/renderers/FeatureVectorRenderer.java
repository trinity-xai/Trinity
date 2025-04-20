package edu.jhuapl.trinity.javafx.renderers;

import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * @author Sean Phillips
 */
public interface FeatureVectorRenderer {
    public void addFeatureCollection(FeatureCollection featureCollection, boolean clearQueue);

    public void addFeatureVector(FeatureVector featureVector);

    public List<FeatureVector> getAllFeatureVectors();

    public void locateFeatureVector(FeatureVector featureVector);

    public void clearFeatureVectors();

    public void setVisibleByIndex(int i, boolean b);

    public void setColorByIndex(int i, Color color);

    public void setColorByID(String iGotID, Color color);

    public void refresh(boolean forceNodeUpdate);

    public void setSpheroidAnchor(boolean animate, int index);

    public void setDimensionLabels(List<String> labelStrings);
}
