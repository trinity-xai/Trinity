package edu.jhuapl.trinity.data;

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

//import com.fasterxml.jackson.annotation.JsonTypeInfo;

import javafx.scene.paint.Color;


/**
 * @author Sean Phillips
 */
//@JsonTypeInfo(
//    use = JsonTypeInfo.Id.MINIMAL_CLASS,
//    include = JsonTypeInfo.As.PROPERTY,
//    property = "@type")
public class GlyphAction {
    public Color color;
    public String label;
    public String name;

    public GlyphAction() {

    }

    public GlyphAction(String name, String label, Color colorObject) {
        this.name = name;
        this.label = label;
        color = colorObject;
    }
}
