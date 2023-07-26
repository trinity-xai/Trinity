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
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextEmbeddingCollection extends MessageData {

    public static final String TYPESTRING = "TextEmbeddingCollection";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {   "type": "TextEmbeddingCollection",
        "label" : "chatGPT",
        "score" : 0.5,
        "text_embeddings": [
            ...boat load of TextEmbeddingSet objects
        ]
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private List<TextEmbeddingSet> text_embeddings;
    private String type;
    private String label;
    private Double score;
    //</editor-fold>

    public TextEmbeddingCollection() {
        type = TYPESTRING;
        text_embeddings = new ArrayList<>();
    }

    public static boolean isTextEmbeddingCollection(String messageBody) {
        return messageBody.contains("type")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("text_embeddings");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

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
     * @return the text_embeddings
     */
    public List<TextEmbeddingSet> getText_embeddings() {
        return text_embeddings;
    }

    /**
     * @param text_embeddings the text_embeddings to set
     */
    public void setText_embeddings(List<TextEmbeddingSet> text_embeddings) {
        this.text_embeddings = text_embeddings;
    }
    //</editor-fold>

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the score
     */
    public Double getScore() {
        return score;
    }

    /**
     * @param score the score to set
     */
    public void setScore(Double score) {
        this.score = score;
    }
}
