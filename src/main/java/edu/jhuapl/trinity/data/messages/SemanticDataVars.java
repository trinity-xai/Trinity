/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.data.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticDataVars extends MessageData {
    public static final String TYPESTRING = "semantic_data_vars";

    private SemanticReconstructionMap neural_timeseries;
    private SemanticReconstructionMap semantic_target;
    private SemanticReconstructionMap prediction;
    private ReconstructionProjectionMap prediction_r;
    private ReconstructionProjectionMap prediction_p;

    public SemanticDataVars() {
        this.messageType = TYPESTRING;
    }

    /**
     * @return the neural_timeseries
     */
    public SemanticReconstructionMap getNeural_timeseries() {
        return neural_timeseries;
    }

    /**
     * @param neural_timeseries the neural_timeseries to set
     */
    public void setNeural_timeseries(SemanticReconstructionMap neural_timeseries) {
        this.neural_timeseries = neural_timeseries;
    }

    /**
     * @return the semantic_target
     */
    public SemanticReconstructionMap getSemantic_target() {
        return semantic_target;
    }

    /**
     * @param semantic_target the semantic_target to set
     */
    public void setSemantic_target(SemanticReconstructionMap semantic_target) {
        this.semantic_target = semantic_target;
    }

    /**
     * @return the prediction
     */
    public SemanticReconstructionMap getPrediction() {
        return prediction;
    }

    /**
     * @param prediction the prediction to set
     */
    public void setPrediction(SemanticReconstructionMap prediction) {
        this.prediction = prediction;
    }

    /**
     * @return the prediction_r
     */
    public ReconstructionProjectionMap getPrediction_r() {
        return prediction_r;
    }

    /**
     * @param prediction_r the prediction_r to set
     */
    public void setPrediction_r(ReconstructionProjectionMap prediction_r) {
        this.prediction_r = prediction_r;
    }

    /**
     * @return the prediction_p
     */
    public ReconstructionProjectionMap getPrediction_p() {
        return prediction_p;
    }

    /**
     * @param prediction_p the prediction_p to set
     */
    public void setPrediction_p(ReconstructionProjectionMap prediction_p) {
        this.prediction_p = prediction_p;
    }
}
