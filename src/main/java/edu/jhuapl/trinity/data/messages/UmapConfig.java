/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;


/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UmapConfig extends MessageData {

    public static final String TYPESTRING = "umap_config";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    '{
        "messageType": "manifold_data",
        "repulsionStrength":0.5,
        "minDist": 0.1,
        "spread": 0.1,
        "opMixRatio":0.9,
        "numberComponents":3,
        "numberEpochs":200,
        "numberNearestNeighbours":15,
        "negativeSampleRate":10,
        "localConnectivity":10,
        "metric":"Euclidean",
        "verbose":"true"
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">

    private Float repulsionStrength;
    private Float minDist;
    private Float spread;
    private Float opMixRatio;
    private Integer numberComponents;
    private Integer numberEpochs;
    private Integer numberNearestNeighbours;
    private Integer negativeSampleRate;
    private Integer localConnectivity;
    private Double threshold;
    private Float targetWeight;
    private String metric;
    private Boolean verbose;

    //</editor-fold>

    public UmapConfig() {
        messageType = TYPESTRING;

    }

    public static boolean isUmapConfig(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("numberComponents")
            && messageBody.contains("numberNearestNeighbours");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the repulsionStrength
     */
    public Float getRepulsionStrength() {
        return repulsionStrength;
    }

    /**
     * @param repulsionStrength the repulsionStrength to set
     */
    public void setRepulsionStrength(Float repulsionStrength) {
        this.repulsionStrength = repulsionStrength;
    }

    /**
     * @return the minDist
     */
    public Float getMinDist() {
        return minDist;
    }

    /**
     * @param minDist the minDist to set
     */
    public void setMinDist(Float minDist) {
        this.minDist = minDist;
    }

    /**
     * @return the spread
     */
    public Float getSpread() {
        return spread;
    }

    /**
     * @param spread the spread to set
     */
    public void setSpread(Float spread) {
        this.spread = spread;
    }

    /**
     * @return the opMixRatio
     */
    public Float getOpMixRatio() {
        return opMixRatio;
    }

    /**
     * @param opMixRatio the opMixRatio to set
     */
    public void setOpMixRatio(Float opMixRatio) {
        this.opMixRatio = opMixRatio;
    }

    /**
     * @return the numberComponents
     */
    public Integer getNumberComponents() {
        return numberComponents;
    }

    /**
     * @param numberComponents the numberComponents to set
     */
    public void setNumberComponents(Integer numberComponents) {
        this.numberComponents = numberComponents;
    }

    /**
     * @return the numberEpochs
     */
    public Integer getNumberEpochs() {
        return numberEpochs;
    }

    /**
     * @param numberEpochs the numberEpochs to set
     */
    public void setNumberEpochs(Integer numberEpochs) {
        this.numberEpochs = numberEpochs;
    }

    /**
     * @return the numberNearestNeighbours
     */
    public Integer getNumberNearestNeighbours() {
        return numberNearestNeighbours;
    }

    /**
     * @param numberNearestNeighbours the numberNearestNeighbours to set
     */
    public void setNumberNearestNeighbours(Integer numberNearestNeighbours) {
        this.numberNearestNeighbours = numberNearestNeighbours;
    }

    /**
     * @return the negativeSampleRate
     */
    public Integer getNegativeSampleRate() {
        return negativeSampleRate;
    }

    /**
     * @param negativeSampleRate the negativeSampleRate to set
     */
    public void setNegativeSampleRate(Integer negativeSampleRate) {
        this.negativeSampleRate = negativeSampleRate;
    }

    /**
     * @return the localConnectivity
     */
    public Integer getLocalConnectivity() {
        return localConnectivity;
    }

    /**
     * @param localConnectivity the localConnectivity to set
     */
    public void setLocalConnectivity(Integer localConnectivity) {
        this.localConnectivity = localConnectivity;
    }

    /**
     * @return the metric
     */
    public String getMetric() {
        return metric;
    }

    /**
     * @param metric the metric to set
     */
    public void setMetric(String metric) {
        this.metric = metric;
    }

    /**
     * @return the verbose
     */
    public Boolean getVerbose() {
        return verbose;
    }

    /**
     * @param verbose the verbose to set
     */
    public void setVerbose(Boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * @return the threshold
     */
    public Double getThreshold() {
        return threshold;
    }

    /**
     * @param threshold the threshold to set
     */
    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    /**
     * @return the targetWeight
     */
    public Float getTargetWeight() {
        return targetWeight;
    }

    /**
     * @param targetWeight the targetWeight to set
     */
    public void setTargetWeight(Float targetWeight) {
        this.targetWeight = targetWeight;
    }

    //</editor-fold>
}
