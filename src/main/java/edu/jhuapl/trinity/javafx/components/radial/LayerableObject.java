package edu.jhuapl.trinity.javafx.components.radial;

import edu.jhuapl.trinity.javafx.renderers.Renderer;
import javafx.scene.input.MouseEvent;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * Enforcing type checking across object types so that a Layer class can have
 * a generic type list of objects to hold.
 *
 * @author Sean Phillips
 */
//@JsonTypeInfo(
//    use = JsonTypeInfo.Id.MINIMAL_CLASS,
//    include = JsonTypeInfo.As.PROPERTY,
//    property = "@type")
public interface LayerableObject {
    /**
     * Returns a Renderer which can be used to draw this object on screen.
     *
     * @return The object that implements Renderer
     */
    public Renderer getRenderer();

    /**
     * Implementation will facilitate user interaction to relocate the object.
     * Must update local/global coordinates information appropriately.
     *
     * @param enableDrag boolean true to enable drag.
     */
    public void setEnableDrag(boolean enableDrag);

    /**
     * Implementation will allow a new object to be aligned and then located.
     *
     * @param x Location on X axis in the Domain dimensions
     * @param y Location on Y axis in the Domain dimensions
     */
    public void setLocation(double x, double y);

    /**
     * Implementation will provide an external entity to easily determine what the
     * Node specific center point X coordinate should be.  This allows the
     * underlying renderer to provide what it feels should be the logical
     * X coordinate
     *
     * @return The X coordinate IN PIXELS WITHIN THE PARENT of the logical center point
     */
    public double getCenterX();

    /**
     * Implementation will provide an external entity to easily determine what the
     * Node specific center point Y coordinate should be.  This allows the
     * underlying renderer to provide what it feels should be the logical
     * Y coordinate
     *
     * @return The Y coordinate IN PIXELS WITHIN THE PARENT of the logical center point
     */
    public double getCenterY();

    /**
     * Function to determine if the current object is selected or not.
     *
     * @return value, true if selected.
     */
    public Boolean isSelected();

    /**
     * Set the selection state.
     *
     * @param selected true or false if selected.
     */
    public void setSelected(Boolean selected);

    /**
     * Action to perform on drag when part of a selected group.
     *
     * @param event mouse event drag.
     */
    public void selectedDrag(MouseEvent event);

    /**
     * Action to perform on press when part of a selection group.
     *
     * @param event MouseEvent on press.
     */
    public void selectedPress(MouseEvent event);

    /**
     * Action to perform when mouse is released when part of selection group.
     *
     * @param event MouseEvent of release
     */
    public void selectedRelease(MouseEvent event);

}
