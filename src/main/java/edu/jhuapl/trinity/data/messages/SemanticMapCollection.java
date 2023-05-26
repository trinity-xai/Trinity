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

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticMapCollection extends MessageData {

    public static final String TYPESTRING = "SemanticMapCollection";

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private SemanticMap semantic_space;
    private SemanticReconstruction reconstruction;
    private String type;
    //</editor-fold>

    public SemanticMapCollection() {

    }

    public static boolean isSemanticMapCollection(String messageBody) {
        return messageBody.contains("semantic_space")
            && messageBody.contains("reconstruction");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the semantic_space
     */
    public SemanticMap getSemantic_space() {
        return semantic_space;
    }

    /**
     * @param semantic_space the semantic_space to set
     */
    public void setSemantic_space(SemanticMap semantic_space) {
        this.semantic_space = semantic_space;
    }

    /**
     * @return the reconstruction
     */
    public SemanticReconstruction getReconstruction() {
        return reconstruction;
    }

    /**
     * @param reconstruction the reconstruction to set
     */
    public void setReconstruction(SemanticReconstruction reconstruction) {
        this.reconstruction = reconstruction;
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
    //</editor-fold>
}
