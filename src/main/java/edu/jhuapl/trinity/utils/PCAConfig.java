/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.utils;

import edu.jhuapl.trinity.utils.AnalysisUtils.ANALYSIS_METHOD;
import edu.jhuapl.trinity.utils.AnalysisUtils.SOURCE;

/**
 * @author Sean Phillips
 */
public class PCAConfig {
    public SOURCE source;
    public ANALYSIS_METHOD method;
    public int pcaDimensions;
    public double scaling;
    public int startIndex;
    public int endIndex; //-1 means use max

    public PCAConfig(SOURCE source, ANALYSIS_METHOD method, int pcaDimensions, double scaling, int startIndex, int endIndex) {
        this.source = source;
        this.method = method;
        this.pcaDimensions = pcaDimensions;
        this.scaling = scaling;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }


}
