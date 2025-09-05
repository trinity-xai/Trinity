package edu.jhuapl.trinity.data.messages.xai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;

import java.util.ArrayList;
import java.util.Arrays;
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
    private String type;
    private ArrayList<String> dimensionLabels;
    private List<FeatureVector> features;

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

    public static FeatureCollection merge(List<FeatureCollection> collections) {
        FeatureCollection fcf = new FeatureCollection();
        List<FeatureVector> featureVectors = new ArrayList<>();
        for(FeatureCollection fc : collections) {
            featureVectors.addAll(fc.getFeatures());
        }
        fcf.setFeatures(featureVectors);
        return fcf;
    }
    
    public float[][] convertFeaturesToFloatArray() {
        int vectorCount = features.size();
        int vectorWidth = features.get(0).getData().size();
        float[][] data = new float[vectorCount][vectorWidth];
        for (int featureVectorIndex = 0; featureVectorIndex < vectorCount; featureVectorIndex++) {
            Arrays.fill(data[featureVectorIndex], 0);
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                if (features.get(featureVectorIndex).getData().size() > vectorIndex)
                    data[featureVectorIndex][vectorIndex] =
                        features.get(featureVectorIndex).getData().get(vectorIndex).floatValue();
                else
                    break;
            }
        }
        return data;
    }

    public double[][] convertFeaturesToArray() {
        int vectorCount = features.size();
        int vectorWidth = features.get(0).getData().size();
        double[][] data = new double[vectorCount][vectorWidth];
        for (int featureVectorIndex = 0; featureVectorIndex < vectorCount; featureVectorIndex++) {
            Arrays.fill(data[featureVectorIndex], 0);
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                if (features.get(featureVectorIndex).getData().size() > vectorIndex)
                    data[featureVectorIndex][vectorIndex] =
                        features.get(featureVectorIndex).getData().get(vectorIndex);
                else
                    break;
            }
        }
        return data;
    }

    public static double[][] toData(List<FeatureVector> featureVectors) {
        double[][] data = new double[featureVectors.size()][featureVectors.get(0).getData().size()];
        for (int i = 0; i < featureVectors.size(); i++) {
            data[i] = FeatureVector.mapToStateArray.apply(featureVectors.get(i));
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
    //</editor-fold>    
}