package edu.jhuapl.trinity.javafx.components.panes;

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

import javafx.scene.Scene;
import javafx.scene.layout.Pane;

/**
 * @author Sean Phillips
 */
public class ManifoldControlPane extends LitPathPane {

    public static String CONTROLLER = "/edu/jhuapl/trinity/fxml/ManifoldControl.fxml";
    
    public ManifoldControlPane(Scene scene, Pane parent) {
        this(scene, parent, CONTROLLER);
    }
    public ManifoldControlPane(Scene scene, Pane parent, String controller) {
        super(scene, parent, 500, 600, createContent(controller), "Manifolds ", "", 200.0, 300.0);
    }
}
