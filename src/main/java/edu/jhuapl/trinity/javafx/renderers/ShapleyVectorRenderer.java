/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.renderers;

import edu.jhuapl.trinity.data.messages.ShapleyCollection;
import edu.jhuapl.trinity.data.messages.ShapleyVector;

/**
 * @author Sean Phillips
 */
public interface ShapleyVectorRenderer {
    public void addShapleyCollection(ShapleyCollection shapleyCollection);

    public void addShapleyVector(ShapleyVector shapleyVector);

    public void clearShapleyVectors();

    public void refresh();
}
