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
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextEmbeddingSet extends MessageData {

    public static final String TYPESTRING = "text_embedding_set";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    '{
        "text" : "some long glob of written text in ASCII with spaces and stuff",
        "masked" : ["A set of <TOK> tokenized phrases <TOK> from the text source",
            "There are multiple <TOK> versions of each phrase", "<TOK> see?? <TOK>"],
        "parsed" : ["A set of tokenized phrases from the text source",
            "There are multiple versions of each phrase", " see?? "],
        "text_embedding": [-0.4232191175986961, -0.5031443592838056, 0.30497221256679125,
            0.4157550505277444, -0.46174460409303325, -0.12950797668733202,
            0.6323170694189965, 0.20112482321095512, -0.0770502704073328,
            -0.018055872253030292, 0.08990142832758276, 0.14425006537440124,
            -0.30635162612534406, -0.04588245408916634, 0.37569343542885386,
            0.08211227109965555, -0.24560538018977932, -0.17276225757375335,
            0.1307694902388548, 0.4419011504572875, -0.5020043352557712
        ],
        "embeddings": [[-0.4232191175986961, -0.5031443592838056, 0.30497221256679125],
            [-0.4232191175986961, -0.5031443592838056, 0.30497221256679125],
            [-0.4232191175986961, -0.5031443592838056, 0.30497221256679125],
            [-0.4232191175986961, -0.5031443592838056, 0.30497221256679125],
            [-0.4232191175986961, -0.5031443592838056, 0.30497221256679125],
            [-0.4232191175986961, -0.5031443592838056, 0.30497221256679125]],
//        "score": -2.753245759396493,
//        "label": "human",
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String text;
    private List<String> masked;
    private List<String> parsed;
    private List<Double> text_embedding;
    private List<List<Double>> embeddings;
    private String label;
    private Double score;
    private HashMap<String, String> metaData;
    //</editor-fold>

    public TextEmbeddingSet() {
        messageType = TYPESTRING;
        masked = new ArrayList<>();
        parsed = new ArrayList<>();
        text_embedding = new ArrayList<>();
        embeddings = new ArrayList<>();
        metaData = new HashMap<>();
    }

    public static boolean isTextEmbeddingSet(String messageBody) {
        return messageBody.contains("text_embedding")
            && messageBody.contains("embeddings");
    }

    public boolean metaContainsTerm(String term) {
        for (Entry<String, String> entry : getMetaData().entrySet()) {
            if (entry.getKey().contains(term) || entry.getValue().contains(term))
                return true;
        }
        return false;
    }

    public String metadataAsString(String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : getMetaData().entrySet()) {
            sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append(delimiter);
        }
        return sb.toString();
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the masked
     */
    public List<String> getMasked() {
        return masked;
    }

    /**
     * @param masked the masked to set
     */
    public void setMasked(List<String> masked) {
        this.masked = masked;
    }

    /**
     * @return the parsed
     */
    public List<String> getParsed() {
        return parsed;
    }

    /**
     * @param parsed the parsed to set
     */
    public void setParsed(List<String> parsed) {
        this.parsed = parsed;
    }

    /**
     * @return the text_embedding
     */
    public List<Double> getText_embedding() {
        return text_embedding;
    }

    /**
     * @param text_embedding the text_embedding to set
     */
    public void setText_embedding(List<Double> text_embedding) {
        this.text_embedding = text_embedding;
    }

    /**
     * @return the embeddings
     */
    public List<List<Double>> getEmbeddings() {
        return embeddings;
    }

    /**
     * @param embeddings the embeddings to set
     */
    public void setEmbeddings(List<List<Double>> embeddings) {
        this.embeddings = embeddings;
    }

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

    /**
     * @return the metaData
     */
    public HashMap<String, String> getMetaData() {
        return metaData;
    }

    /**
     * @param metaData the metaData to set
     */
    public void setMetaData(HashMap<String, String> metaData) {
        this.metaData = metaData;
    }

    //</editor-fold>    
}
