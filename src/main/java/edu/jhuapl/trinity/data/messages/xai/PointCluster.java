/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.messages.xai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PointCluster extends MessageData {

    public static final String TYPESTRING = "point_cluster";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    '{
        "messageType": "point_cluster",
        "clusterName": "some_optional name",
        "clusterId": 1,
        "map": [3, 7, 9, 14, 19, 27, 330, 3000, 9001 ],
        "data": [[-0.4232191175986961, -0.5031443592838056, 0.30497221256679125],
            [0.4157550505277444, -0.46174460409303325, -0.12950797668733202],
            [0.6323170694189965, 0.20112482321095512, -0.0770502704073328],
            [-0.018055872253030292, 0.08990142832758276, 0.14425006537440124],
            [-0.30635162612534406, -0.04588245408916634, 0.37569343542885386],
            [-0.15806087484089912, 0.0673835604499377, 0.17998157972463474],
            [-0.01611924502337739, -0.24604972532815875, -0.01560032825534631],
            [0.3946917081600188, -0.12442754943149016, -0.18011471923351433],
            [-0.04629288711207645, 0.5376945006956948, 0.18828509670172092]
        ]
    }
     */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String clusterName;
    private Integer clusterId;
    private List<Integer> map;
    private List<List<Double>> data; //optional, the original data
    private List<Double> mean; //optional, the mean vector or centroid
    private List<List<Double>> covariance; //optional, the covariance of the
    //cluster typically derived from some fitting convergence process

    //</editor-fold>

    public PointCluster() {
        messageType = TYPESTRING;
        map = new ArrayList<>();
        data = new ArrayList<>();
        mean = new ArrayList<>();
        covariance = new ArrayList<>();
    }

    public static boolean isPointCluster(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("map");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the clusterName
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * @param clusterName the clusterName to set
     */
    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    /**
     * @return the clusterId
     */
    public Integer getClusterId() {
        return clusterId;
    }

    /**
     * @param clusterId the clusterId to set
     */
    public void setClusterId(Integer clusterId) {
        this.clusterId = clusterId;
    }

    /**
     * @return the map
     */
    public List<Integer> getMap() {
        return map;
    }

    /**
     * @param map the map to set
     */
    public void setMap(List<Integer> map) {
        this.map = map;
    }

    /**
     * @return the data
     */
    public List<List<Double>> getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(List<List<Double>> data) {
        this.data = data;
    }

    /**
     * @return the mean
     */
    public List<Double> getMean() {
        return mean;
    }

    /**
     * @param mean the mean to set
     */
    public void setMean(List<Double> mean) {
        this.mean = mean;
    }

    /**
     * @return the covariance
     */
    public List<List<Double>> getCovariance() {
        return covariance;
    }

    /**
     * @param covariance the covariance to set
     */
    public void setCovariance(List<List<Double>> covariance) {
        this.covariance = covariance;
    }

    //</editor-fold>
}
