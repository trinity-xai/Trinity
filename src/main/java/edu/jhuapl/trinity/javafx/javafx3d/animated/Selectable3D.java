package edu.jhuapl.trinity.javafx.javafx3d.animated;

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

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * just a cheesy way to have a instanceof selectable Shape3D.
 *
 * @author Sean Phillips
 */
public interface Selectable3D {
    public static PhongMaterial DEFAULT_SELECTED_MATERIAL = new PhongMaterial(
        Color.ALICEBLUE.deriveColor(1, 1, 1, 0.5));
    public PhongMaterial previousMaterial = null;

    public void select();

    public void unselect();

    public boolean isSelected();

}
