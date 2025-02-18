/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.data.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShapleyCollection extends MessageData {

    public static final String TYPESTRING = "ShapleyCollection";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    {   "type": "ShapleyCollection",
        "values": [
            ...boat load of ShapleyVector objects
        ]
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private ArrayList<String> dimensionLabels;
    private List<ShapleyVector> shapleyValues;
    private String sourceInput;
    //</editor-fold>

    public ShapleyCollection() {
        this.messageType = TYPESTRING;
        this.shapleyValues = new ArrayList<>();

    }

    public static boolean isShapleyCollection(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("sourceInput")
            && messageBody.contains("values");
    }

    public float[][] convertValuesToFloatArray() {
        int vectorCount = shapleyValues.size();
        int vectorWidth = shapleyValues.get(0).getData().size();
        float[][] data = new float[vectorCount][vectorWidth];
        for (int shapleyVectorIndex = 0; shapleyVectorIndex < vectorCount; shapleyVectorIndex++) {
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                data[shapleyVectorIndex][vectorIndex] = shapleyValues.get(shapleyVectorIndex).getData().get(vectorIndex).floatValue();
            }
        }
        return data;
    }

    public double[][] convertValuesToArray() {
        int vectorCount = shapleyValues.size();
        int vectorWidth = shapleyValues.get(0).getData().size();
        double[][] data = new double[vectorCount][vectorWidth];
        for (int shapleyVectorIndex = 0; shapleyVectorIndex < vectorCount; shapleyVectorIndex++) {
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                data[shapleyVectorIndex][vectorIndex] = shapleyValues.get(shapleyVectorIndex).getData().get(vectorIndex);
            }
        }
        return data;
    }

    public static double[][] toData(List<ShapleyVector> shapleVectors) {
        double[][] data = new double[shapleVectors.size()][shapleVectors.get(0).getData().size()];
        for (int i = 0; i < shapleVectors.size(); i++) {
            data[i] = ShapleyVector.mapToStateArray.apply(shapleVectors.get(i));
        }
        return data;
    }

    public static ShapleyCollection fromData(List<List<Double>> data) {
        ShapleyCollection scf = new ShapleyCollection();
        int vectorCount = data.size();
        List<ShapleyVector> shapleyVectors = new ArrayList<>(vectorCount);
        //super slow and ugly conversion from lists to feature collection
        for (int shapleyVectorIndex = 0; shapleyVectorIndex < vectorCount; shapleyVectorIndex++) {
            ShapleyVector sv = new ShapleyVector();
            sv.setData(data.get(shapleyVectorIndex));
            shapleyVectors.add(sv);
        }
        scf.setValues(shapleyVectors);
        return scf;
    }

    public static ShapleyCollection fromData(double[][] data, int width, double scaling) {
        ShapleyCollection sc = new ShapleyCollection();
        int vectorCount = data.length;
        int vectorWidth = data[0].length;
        if (width < vectorWidth) //truncate width if the base data is wider than the parameter
            vectorWidth = width;
        List<ShapleyVector> shapleyVectors = new ArrayList<>(vectorCount);
        //super slow and ugly conversion from arrays to lists
        for (int shapleyVectorIndex = 0; shapleyVectorIndex < vectorCount; shapleyVectorIndex++) {
            ShapleyVector sv = new ShapleyVector();
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                sv.getData().add(data[shapleyVectorIndex][vectorIndex]
                    * scaling); //add projection scaling
            }
            shapleyVectors.add(sv);
        }
        sc.setValues(shapleyVectors);
        return sc;
    }

    public static ShapleyCollection fromData(double[][] data) {
        ShapleyCollection sc = new ShapleyCollection();
        int vectorCount = data.length;
        int vectorWidth = data[0].length;
        List<ShapleyVector> shapleyVectors = new ArrayList<>(vectorCount);
        //super slow and ugly conversion from arrays to lists
        for (int shapleyVectorIndex = 0; shapleyVectorIndex < vectorCount; shapleyVectorIndex++) {
            ShapleyVector sv = new ShapleyVector();
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                sv.getData().add(data[shapleyVectorIndex][vectorIndex]);
            }
            shapleyVectors.add(sv);
        }
        sc.setValues(shapleyVectors);
        return sc;
    }

    public static ShapleyCollection fakeCollection(int rows, int columns) {
        ShapleyCollection sc = new ShapleyCollection();
        sc.setSourceInput("OnyxHappyFace.png");
        Random rando = new Random();
        List<ShapleyVector> shapleyVectors = new ArrayList<>(rows);
        //Assumes Image input mapping values to pixel grid
        for (int shapleyVectorIndex = 0; shapleyVectorIndex < rows; shapleyVectorIndex++) {
            for (int vectorIndex = 0; vectorIndex < columns; vectorIndex++) {
                ShapleyVector sv = new ShapleyVector();
                sv.setxCoordinate(vectorIndex); //columns are x position
                sv.setyCoordinate(shapleyVectorIndex); //rows are y position
                sv.getData().add(rando.nextGaussian());
                sv.setMetaData(null);
                shapleyVectors.add(sv);
            }
        }
        sc.setValues(shapleyVectors);
        return sc;

    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the features
     */
    public List<ShapleyVector> getValues() {
        return shapleyValues;
    }

    /**
     * @param values the features to set
     */
    public void setValues(List<ShapleyVector> values) {
        this.shapleyValues = values;
    }

    /**
     * @return the sourceInput
     */
    public String getSourceInput() {
        return sourceInput;
    }

    /**
     * @param sourceInput the sourceInput to set
     */
    public void setSourceInput(String sourceInput) {
        this.sourceInput = sourceInput;
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
