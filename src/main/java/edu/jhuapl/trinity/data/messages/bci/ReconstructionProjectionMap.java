/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.data.messages.bci;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReconstructionProjectionMap extends MessageData {

    public static final String TYPESTRING = "reconstruction_projection_map";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        "prediction_r": {
            "dims": [
                "semantic_feature"
            ],
            "attrs": {},
            "data": [
                0.01110963205879954,
                -0.06802699131024609,
                -0.11685241367718269,
                0.013962425388517035,
                0.089110939824971,
                -0.022600049038886662,
                -0.06640164503658286,
                0.05758633558137826,
                -0.10430922323291593,
                -0.008708889484009228,
                0.018643856648294037,
                0.08696546761054849
            ]
        }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private List<String> dims;
    private Map<String, Object> attrs;
    private List<Double> data;
    //</editor-fold>

    public ReconstructionProjectionMap() {
        this.messageType = TYPESTRING;
        this.dims = new ArrayList<>();
        attrs = new HashMap<>();
    }

    public static boolean isSemanticReconstructionMap(String messageBody) {
        return messageBody.contains("dims") && messageBody.contains("attrs")
            && messageBody.contains("data");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the data
     */
    public List<Double> getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(List<Double> data) {
        this.data = data;
    }

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
    public Map<String, Object> getAttrs() {
        return attrs;
    }

    /**
     * @param attrs the attrs to set
     */
    public void setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
    }
    //</editor-fold>
}
