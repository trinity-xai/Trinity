/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

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
     * "start": {
     * "identity": 12525037,
     * "labels": [
     * "Document"
     * ],
     * "properties": {
     * "name": "7630a867296188c0566d0a73",
     * "numberHashes": 276
     * }
     * }
     */
    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private long identity;
    private ArrayList<String> labels;
    private HashMap<String, Object> properties;

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
