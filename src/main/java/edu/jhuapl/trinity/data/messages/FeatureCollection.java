package edu.jhuapl.trinity.data.messages;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureCollection extends MessageData {

    public static final String TYPESTRING = "FeatureCollection";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {   "type": "FeatureCollection",
        "dimensionLabels": ["d1label", "d2label", "etc etc" ],
        "features": [
            ...boat load of FeatureVector objects
        ]
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private ArrayList<String> dimensionLabels;
    private List<FeatureVector> features;
    private String type;
    //</editor-fold>

    public FeatureCollection() {
        type = TYPESTRING;
        this.features = new ArrayList<>();
    }

    public static boolean isFeatureCollection(String messageBody) {
        return messageBody.contains("type")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("features");
    }

    public float[][] convertFeaturesToFloatArray() {
        int vectorCount = features.size();
        int vectorWidth = features.get(0).getData().size();
        float[][] data = new float[vectorCount][vectorWidth];
        for (int featureVectorIndex = 0; featureVectorIndex < vectorCount; featureVectorIndex++) {
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                data[featureVectorIndex][vectorIndex] = features.get(featureVectorIndex).getData().get(vectorIndex).floatValue();
            }
        }
        return data;
    }

    public double[][] convertFeaturesToArray() {
        int vectorCount = features.size();
        int vectorWidth = features.get(0).getData().size();
        double[][] data = new double[vectorCount][vectorWidth];
        for (int featureVectorIndex = 0; featureVectorIndex < vectorCount; featureVectorIndex++) {
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                data[featureVectorIndex][vectorIndex] = features.get(featureVectorIndex).getData().get(vectorIndex);
            }
        }
        return data;
    }

    public static FeatureCollection fromData(List<List<Double>> data) {
        FeatureCollection fcf = new FeatureCollection();
        int vectorCount = data.size();
        List<FeatureVector> featureVectors = new ArrayList<>(vectorCount);
        //super slow and ugly conversion from lists to feature collection
        for (int featureVectorIndex = 0; featureVectorIndex < vectorCount; featureVectorIndex++) {
            FeatureVector fv = new FeatureVector();
            fv.setData(data.get(featureVectorIndex));
            featureVectors.add(fv);
        }
        fcf.setFeatures(featureVectors);
        return fcf;
    }

    public static FeatureCollection fromData(double[][] data, int width, double scaling) {
        FeatureCollection fcf = new FeatureCollection();
        int vectorCount = data.length;
        int vectorWidth = data[0].length;
        if (width < vectorWidth) //truncate width if the base data is wider than the parameter
            vectorWidth = width;
        List<FeatureVector> featureVectors = new ArrayList<>(vectorCount);
        //super slow and ugly conversion from arrays to lists
        for (int featureVectorIndex = 0; featureVectorIndex < vectorCount; featureVectorIndex++) {
            FeatureVector fv = new FeatureVector();
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                fv.getData().add(data[featureVectorIndex][vectorIndex]
                    * scaling); //add projection scaling
            }
            featureVectors.add(fv);
        }
        fcf.setFeatures(featureVectors);
        return fcf;
    }

    public static FeatureCollection fromData(double[][] data) {
        FeatureCollection fcf = new FeatureCollection();
        int vectorCount = data.length;
        int vectorWidth = data[0].length;
        List<FeatureVector> featureVectors = new ArrayList<>(vectorCount);
        //super slow and ugly conversion from arrays to lists
        for (int featureVectorIndex = 0; featureVectorIndex < vectorCount; featureVectorIndex++) {
            FeatureVector fv = new FeatureVector();
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                fv.getData().add(data[featureVectorIndex][vectorIndex]);
            }
            featureVectors.add(fv);
        }
        fcf.setFeatures(featureVectors);
        return fcf;
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the features
     */
    public List<FeatureVector> getFeatures() {
        return features;
    }

    /**
     * @param features the features to set
     */
    public void setFeatures(List<FeatureVector> features) {
        this.features = features;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
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
