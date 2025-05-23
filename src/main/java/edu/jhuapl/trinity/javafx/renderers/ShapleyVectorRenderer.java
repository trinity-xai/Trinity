package edu.jhuapl.trinity.javafx.renderers;

import edu.jhuapl.trinity.data.messages.xai.ShapleyCollection;
import edu.jhuapl.trinity.data.messages.xai.ShapleyVector;

/**
 * @author Sean Phillips
 */
public interface ShapleyVectorRenderer {
    public void addShapleyCollection(ShapleyCollection shapleyCollection);

    public void addShapleyVector(ShapleyVector shapleyVector);

    public void clearShapleyVectors();

    public void refresh();
}
