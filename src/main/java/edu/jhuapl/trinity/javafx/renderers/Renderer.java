package edu.jhuapl.trinity.javafx.renderers;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
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

import edu.jhuapl.trinity.javafx.components.radial.LocalPoint;
import javafx.scene.Node;
import javafx.scene.image.Image;

import java.net.URL;

/**
 * Provides enforcement for a class to create the JavaFX Node and provide the
 * location relative to canvas.
 *
 * @author Sean Phillips
 */
public abstract class Renderer {
    /**
     * JavaFX Node object that can be displayed in a Scene.
     */
    public Node renderedNode;

    /**
     * the anchor location local to Canvas/Local layer
     */
    public LocalPoint anchor;

    /**
     * Provides a JavaFX Node object that can be displayed in a Scene.
     *
     * @return Node the object to be added to the scenegraph
     */
    public abstract Node renderNode();

    /**
     * Get the anchor location of the rendered Node. This is typically in local
     * "domain" coordinates.
     *
     * @return LocalPoint the anchor location. Z axis will be ignored in 2D views.
     */
    public abstract LocalPoint getLocation();

    /**
     * Forces inheriting class to to reinstantiate the underlying GUI Node
     */
    public final void resetRenderedNode() {
        renderedNode = null;
        renderNode();
    }

    /**
     * Utility that attempts to find a texture file
     *
     * @param name String name of the file. Typically the same string as the
     *             class of the object to be rendered.  (ie... sensor.png)
     * @return Image Image object of file. returns null if file could not be
     * found.
     */
    public static Image findTexture(String name) {
        //Get texture file by name
        URL url = Renderer.class.getResource(name + ".png");
        if (null == url) {
            //try to use generic image
            url = Renderer.class.getResource("entity.png");
        }
        if (null != url)
            return new Image(url.toExternalForm());
        else
            return null;
    }
}
