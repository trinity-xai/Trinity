/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.data.messages.xai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LabelConfig extends MessageData {

    public static final String TYPESTRING = "label_config";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    '{
        "messageType": "label_config",
        "labels": {
            "hotdog":"#FF00FF88", //RGBA color code
            "hamburger":"#55FF00FF",
            etc...
        },
        "clearAll" : "false" //nuclear flag to remove all existing labels
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private ArrayList<String> dimensionLabels;
    private HashMap<String, String> wildcards;
    private HashMap<String, String> labels;
    private Boolean clearAll;
    //</editor-fold>

    public LabelConfig() {
        messageType = TYPESTRING;
        dimensionLabels = new ArrayList<>();
        wildcards = new HashMap<>();
        labels = new HashMap<>();
    }

    public static boolean isMatch(String testString, String wildcardPattern) {
        Pattern regex = Pattern.compile(wildcardPattern);
        return regex.matcher(testString).matches();
    }

    public static boolean isLabelConfig(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING);
    }


    public boolean labelsContainsTerm(String term) {
        for (Entry<String, String> entry : labels.entrySet()) {
            if (entry.getKey().contains(term) || entry.getValue().contains(term))
                return true;
        }
        return false;
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the labels
     */
    public HashMap<String, String> getLabels() {
        return labels;
    }

    /**
     * @param labels the labels to set
     */
    public void setLabels(HashMap<String, String> labels) {
        this.labels = labels;
    }

    /**
     * @return the wildcards
     */
    public HashMap<String, String> getWildcards() {
        return wildcards;
    }

    /**
     * @param wildcards the wildcards to set
     */
    public void setWildcards(HashMap<String, String> wildcards) {
        this.wildcards = wildcards;
    }

    /**
     * @return the clearAll
     */
    public Boolean isClearAll() {
        return clearAll;
    }

    /**
     * @param clearAll the clearAll to set
     */
    public void setClearAll(Boolean clearAll) {
        this.clearAll = clearAll;
    }
    //</editor-fold>

    /**
     * @return the dimensionLabels
     */
    public ArrayList<String> getDimensionLabels() {
        return dimensionLabels;
    }

    /**
     * @param dimensionLabels the dimensionLabels to set
     */
    public void setDimensionLabels(ArrayList<String> dimensionLabels) {
        this.dimensionLabels = dimensionLabels;
    }
}
