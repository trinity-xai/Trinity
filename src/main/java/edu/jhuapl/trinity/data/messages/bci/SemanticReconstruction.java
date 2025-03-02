/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.data.messages.bci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticReconstruction extends MessageData {

    public static final String TYPESTRING = "semantic_reconstruction";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        "reconstruction": {
            "dims": {
                "semantic_feature": 12,
                "time": 1522,
                "neural_feature": 836
            },
            "coords": { },
            "attrs": {},
            "data_vars": {
                "neural_timeseries": {
                    "dims": [
                        "time",
                        "neural_feature"
                    ],
                    "attrs": {},
                    "data": [
                        [
                            0.022663313111536354,
                            -0.0005209651381807893,
                            0.017817566867876167,
            //etc etc etc
            "coords": {
                "semantic_feature": {
                },
                "time": {
                },
                "NN": {
                }
                //etc etc etc
            },
            "name": "human218"
        }
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    //public List<SemanticReconstructionMap> coords;
    private Map<String, Double> dims;
    private SemanticDataVars data_vars;
    private ReconstructionAttributes attrs;

    //</editor-fold>

    public SemanticReconstruction() {
        messageType = TYPESTRING;
        dims = new HashMap<>();
        //coords = new ArrayList<>();
    }

    public static boolean isSemanticReconstruction(String messageBody) {
        return messageBody.contains("dims") && messageBody.contains("attrs")
            && messageBody.contains("data_vars") && messageBody.contains("coords");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the data_vars
     */
    public SemanticDataVars getData_vars() {
        return data_vars;
    }

    /**
     * @param data_vars the data_vars to set
     */
    public void setData_vars(SemanticDataVars data_vars) {
        this.data_vars = data_vars;
    }

    /**
     * @return the dims
     */
    public Map<String, Double> getDims() {
        return dims;
    }

    /**
     * @param dims the dims to set
     */
    public void setDims(Map<String, Double> dims) {
        this.dims = dims;
    }

    /**
     * @return the attrs
     */
    public ReconstructionAttributes getAttrs() {
        return attrs;
    }

    /**
     * @param attrs the attrs to set
     */
    public void setAttrs(ReconstructionAttributes attrs) {
        this.attrs = attrs;
    }
    //</editor-fold>
}
