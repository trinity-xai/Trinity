/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.data.messages;

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
public class SystemFeatures extends MessageData {

    public static final String TYPESTRING = "system_features";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        {
            "type": "system_features",
            "message": {
                "message_type": "x_y_z",
                "message_id": 0,
                "version": "x.x",
                "input": {
                    "data_type": "x_data",
                    "data_length": 32,
                    "data": []
                },
                "latent": {
                    "data_type": "y_data",
                    "data_length": 64,
                    "data": []
                },
                "output": {
                    "data_type": "z_data",
                    "data_length": 16,
                    "data": []
                }
            }
        }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String message_type;
    private Integer message_id;
    private List<Double> input;
    private List<Double> latent;
    private List<Double> output;
    private HashMap<String, String> metaData;
    //</editor-fold>

    public SystemFeatures() {
        messageType = TYPESTRING;
        input = new ArrayList<>();
        latent = new ArrayList<>();
        output = new ArrayList<>();
        metaData = new HashMap<>();
    }

    public static boolean isSystemFeatures(String messageBody) {
        return messageBody.contains("type")
            && messageBody.contains(TYPESTRING);
    }

    //    public static Function<SystemFeatures, String> mapDataToString = (state) -> {
//        return state.getData().toString();
//    };
//
//    public static Function<SystemFeatures, double []> mapToStateArray = (state) -> {
//        double [] states = new double [state.data.size()];
//        for(int i=0;i<states.length;i++) {
//            states[i] = state.data.get(i);
//        }
//        return states;
//    };
//    public static SystemFeatures EMPTY_FEATURE_VECTOR(String label, int dataSize) {
//        SystemFeatures fv = new SystemFeatures();
//        fv.setLabel(label);
//        ArrayList<Double> newData = new ArrayList<>(dataSize);
//        for(int i=0;i<dataSize;i++)
//            newData.add(0.0);
//        fv.setData(newData);
//        ArrayList<Double> bboxList = new ArrayList<>();
//        bboxList.add(0.0);
//        bboxList.add(0.0);
//        bboxList.add(0.0);
//        bboxList.add(0.0);
//        fv.setBbox(bboxList);
//        return fv;
//    }
    public boolean metaContainsTerm(String term) {
        for (Entry<String, String> entry : metaData.entrySet()) {
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
     * @return the message_type
     */
    public String getMessage_type() {
        return message_type;
    }

    /**
     * @param message_type the message_type to set
     */
    public void setMessage_type(String message_type) {
        this.message_type = message_type;
    }

    /**
     * @return the message_id
     */
    public Integer getMessage_id() {
        return message_id;
    }

    /**
     * @param message_id the message_id to set
     */
    public void setMessage_id(Integer message_id) {
        this.message_id = message_id;
    }

    /**
     * @return the input
     */
    public List<Double> getInput() {
        return input;
    }

    /**
     * @param input the input to set
     */
    public void setInput(List<Double> input) {
        this.input = input;
    }

    /**
     * @return the latent
     */
    public List<Double> getLatent() {
        return latent;
    }

    /**
     * @param latent the latent to set
     */
    public void setLatent(List<Double> latent) {
        this.latent = latent;
    }

    /**
     * @return the output
     */
    public List<Double> getOutput() {
        return output;
    }

    /**
     * @param output the output to set
     */
    public void setOutput(List<Double> output) {
        this.output = output;
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
