/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphNode {
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /**
     * 
     * "identity": 12525037,
     * "entityId": "some-arbitrary-name-or-UUID,
     * "vector": [ 123.22, 100.6, 430.22 ],
     * "labels": [
     * "Document"
     * ],
     * "properties": {
     * "name": "7630a867296188c0566d0a73",
     * "numberHashes": 276
     * }
     * 
     */
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private long identity;
    private String entityId;
    private ArrayList<Double> vector; //required but optional length
    private ArrayList<String> labels; //optional
    private String color; //optional overrides default color 
    //ex #FF0000FF == fully opaque Red in RGBA HEX form
    private HashMap<String, Object> properties; //optional

    //</editor-fold>
    public GraphNode() {
        this.labels = new ArrayList<>();
        this.properties = new HashMap<>();
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the identity
     */
    public long getIdentity() {
        return identity;
    }

    /**
     * @param identity the identity to set
     */
    public void setIdentity(long identity) {
        this.identity = identity;
    }

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
     * @return the vector
     */
    public ArrayList<Double> getVector() {
        return vector;
    }

    /**
     * @param vector the vector to set
     */
    public void setVector(ArrayList<Double> vector) {
        this.vector = vector;
    }

    /**
     * @return the labels
     */
    public ArrayList<String> getLabels() {
        return labels;
    }

    /**
     * @param labels the labels to set
     */
    public void setLabels(ArrayList<String> labels) {
        this.labels = labels;
    }

    /**
     * @return the color
     */
    public String getColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * @return the properties
     */
    public HashMap<String, Object> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(HashMap<String, Object> properties) {
        this.properties = properties;
    }
    //</editor-fold>
}
