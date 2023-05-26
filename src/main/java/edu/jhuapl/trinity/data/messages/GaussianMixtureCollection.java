package edu.jhuapl.trinity.data.messages;

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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GaussianMixtureCollection {

    public static final String TYPESTRING = "GaussianMixtureCollection";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {   "type": "GaussianMixtureCollection",
        "mixtures": [
            ...boat load of FeatureVector objects
        ]
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private List<GaussianMixture> mixtures;
    private String type;
    private List<String> axesLabels; //optional field
    //</editor-fold>

    public GaussianMixtureCollection() {
        type = TYPESTRING;
        mixtures = new ArrayList<>();
        axesLabels = new ArrayList<>();
    }

    public static boolean isGaussianMixture(String messageBody) {
        return messageBody.contains("type")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("mixtures");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the mixtures
     */
    public List<GaussianMixture> getMixtures() {
        return mixtures;
    }

    /**
     * @param mixtures the mixtures to set
     */
    public void setMixtures(List<GaussianMixture> mixtures) {
        this.mixtures = mixtures;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the axesLabels
     */
    public List<String> getAxesLabels() {
        return axesLabels;
    }

    /**
     * @param axesLabels the axesLabels to set
     */
    public void setAxesLabels(List<String> axesLabels) {
        this.axesLabels = axesLabels;
    }
    //</editor-fold>
}
