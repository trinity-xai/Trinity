/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShapleyVector extends MessageData {

    public static final String TYPESTRING = "shapley_vector";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    @TBD SMP
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String entityId;
    private List<Double> data;
    private String label;
    private int xCoordinate;
    private int yCoordinate;
    private HashMap<String, String> metaData;
    //</editor-fold>

    public ShapleyVector() {
        this.messageType = TYPESTRING;
        this.data = new ArrayList<>();
        this.metaData = new HashMap<>();
    }

    public static boolean isShapleyVector(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING);
    }

    public static Function<ShapleyVector, String> mapDataToString = (state) -> {
        return state.getData().toString();
    };

    public static Function<ShapleyVector, double[]> mapToStateArray = (state) -> {
        double[] states = new double[state.data.size()];
        for (int i = 0; i < states.length; i++) {
            states[i] = state.data.get(i);
        }
        return states;
    };

    public static ShapleyVector fromData(double[] data, int width, double scaling) {
        ShapleyVector fv = new ShapleyVector();
        for (int vectorIndex = 0; vectorIndex < width; vectorIndex++) {
            fv.getData().add(data[vectorIndex] * scaling);
        }
        return fv;
    }

    public static ShapleyVector EMPTY_SHAPLEY_VECTOR(String label, int dataSize) {
        ShapleyVector fv = new ShapleyVector();
        fv.setLabel(label);
        ArrayList<Double> newData = new ArrayList<>(dataSize);
        for (int i = 0; i < dataSize; i++)
            newData.add(0.0);
        fv.setData(newData);
        return fv;
    }

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

    public double minDataValue() {
        return getData().stream().min(Double::compare).get();
    }

    public double maxDataValue() {
        return getData().stream().max(Double::compare).get();
    }

    public double totalDataWidth() {
        return Math.abs(maxDataValue() - minDataValue());
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the entityId
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * @param entityId the entityId to set
     */
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

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
     * @return the xCoordinate
     */
    public int getxCoordinate() {
        return xCoordinate;
    }

    /**
     * @param xCoordinate the xCoordinate to set
     */
    public void setxCoordinate(int xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    /**
     * @return the yCoordinate
     */
    public int getyCoordinate() {
        return yCoordinate;
    }

    /**
     * @param yCoordinate the yCoordinate to set
     */
    public void setyCoordinate(int yCoordinate) {
        this.yCoordinate = yCoordinate;
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
