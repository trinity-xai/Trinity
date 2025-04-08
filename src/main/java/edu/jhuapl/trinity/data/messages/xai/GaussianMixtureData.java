/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.data.messages.xai;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class GaussianMixtureData {

    //    {
//        "proportion": 0.19285854697227478,
//        "mean": [list of floats of length numFeatures],
//        "covariance": [list of list of floats of length numFeatures]
//    }
    private double proportion;
    private List<Double> mean;
    private List<List<Double>> covariance;
    private List<Double> ellipsoidDiagonal;

    public GaussianMixtureData() {
        mean = new ArrayList<>();
        covariance = new ArrayList<>();
        ellipsoidDiagonal = new ArrayList<>();
    }

//    /**
//     * Automatically indexes into data
//     * @param covarianceMode determines how to index into covariance list
//     * @param index index of the covariance value
//     * @return covariance value
//     */
//    public double getCovByIndex(String covarianceMode, int index) {
//        if(covarianceMode.contentEquals("full"))
//            return covariance.get(0).get(index);
//        else //diag assumes a single list of doubles
//            return covariance.get(0).get(index);
//    }

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

    /**
     * @return the proportion
     */
    public double getProportion() {
        return proportion;
    }

    /**
     * @param proportion the proportion to set
     */
    public void setProportion(double proportion) {
        this.proportion = proportion;
    }

    /**
     * @return the ellipsoidDiagonal
     */
    public List<Double> getEllipsoidDiagonal() {
        return ellipsoidDiagonal;
    }

    /**
     * @param ellipsoidDiagonal the ellipsoidDiagonal to set
     */
    public void setEllipsoidDiagonal(List<Double> ellipsoidDiagonal) {
        this.ellipsoidDiagonal = ellipsoidDiagonal;
    }

}
