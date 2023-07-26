package edu.jhuapl.trinity.data.messages;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticData extends MessageData {

    public static final String TYPESTRING = "semantic_data";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        "noun": {
            "dims": ["noun"],
            "attrs": {},
            "data": ["bear", "cat", "cow", "dog", "horse", "arm", "eye", "foot", "hand", "leg", "apartment", "barn", "church", "house", "igloo", "arch", "chimney", "closet", "door", "window", "coat", "dress", "pants", "shirt", "skirt", "bed", "chair", "desk", "dresser", "table", "ant", "bee", "beetle", "butterfly", "fly", "bottle", "cup", "glass", "knife", "spoon", "bell", "key", "refrigerator", "telephone", "watch", "chisel", "hammer", "pliers", "saw", "screwdriver", "carrot", "celery", "corn", "lettuce", "tomato", "airplane", "bicycle", "car", "train", "truck"]
        }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private List<String> dims;
    private Map<String, Double> attrs;
    private List<String> data;

    //</editor-fold>

    public SemanticData() {
        this.messageType = TYPESTRING;
        this.dims = new ArrayList<>();
        attrs = new HashMap<>();
        this.data = new ArrayList<>();
    }

    public static boolean isFeatureVector(String messageBody) {
        return messageBody.contains("dims")
            && messageBody.contains("attrs") && messageBody.contains("data");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the dims
     */
    public List<String> getDims() {
        return dims;
    }

    /**
     * @param dims the dims to set
     */
    public void setDims(List<String> dims) {
        this.dims = dims;
    }

    /**
     * @return the attrs
     */
    public Map<String, Double> getAttrs() {
        return attrs;
    }

    /**
     * @param attrs the attrs to set
     */
    public void setAttrs(Map<String, Double> attrs) {
        this.attrs = attrs;
    }

    /**
     * @return the data
     */
    public List<String> getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(List<String> data) {
        this.data = data;
    }
    //</editor-fold>
}
