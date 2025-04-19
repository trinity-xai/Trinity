package edu.jhuapl.trinity.data.messages.xai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.jhuapl.trinity.data.messages.MessageData;
import edu.jhuapl.trinity.utils.Utils;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class VectorMaskCollection extends MessageData {

    public static final String TYPESTRING = "VectorMaskCollection";

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private List<VectorMask> vectorMasks;
    //</editor-fold>

    public VectorMaskCollection() {
        this.messageType = TYPESTRING;
        this.vectorMasks = new ArrayList<>();

    }

    public static boolean isVectorMaskCollection(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING)
            && messageBody.contains("values");
    }

    public float[][] convertValuesToFloatArray() {
        int vectorCount = vectorMasks.size();
        int vectorWidth = vectorMasks.get(0).getData().size();
        float[][] data = new float[vectorCount][vectorWidth];
        for (int vectorMasksIndex = 0; vectorMasksIndex < vectorCount; vectorMasksIndex++) {
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                data[vectorMasksIndex][vectorIndex] = vectorMasks.get(vectorMasksIndex).getData().get(vectorIndex).floatValue();
            }
        }
        return data;
    }

    public double[][] convertValuesToArray() {
        int vectorCount = vectorMasks.size();
        int vectorWidth = vectorMasks.get(0).getData().size();
        double[][] data = new double[vectorCount][vectorWidth];
        for (int vectorMaskIndex = 0; vectorMaskIndex < vectorCount; vectorMaskIndex++) {
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                data[vectorMaskIndex][vectorIndex] = vectorMasks.get(vectorMaskIndex).getData().get(vectorIndex);
            }
        }
        return data;
    }

    public static double[][] toData(List<VectorMask> vectorMasks) {
        double[][] data = new double[vectorMasks.size()][vectorMasks.get(0).getData().size()];
        for (int i = 0; i < vectorMasks.size(); i++) {
            data[i] = VectorMask.mapToStateArray.apply(vectorMasks.get(i));
        }
        return data;
    }

    public static VectorMaskCollection fromData(List<List<Double>> data) {
        VectorMaskCollection scf = new VectorMaskCollection();
        int vectorCount = data.size();
        List<VectorMask> vectorMasks = new ArrayList<>(vectorCount);
        //super slow and ugly conversion from lists to feature collection
        for (int vectorMaskIndex = 0; vectorMaskIndex < vectorCount; vectorMaskIndex++) {
            VectorMask sv = new VectorMask();
            sv.setData(data.get(vectorMaskIndex));
            vectorMasks.add(sv);
        }
        scf.setValues(vectorMasks);
        return scf;
    }

//    public static VectorMaskCollection fromData(double[][] data, int width, double scaling) {
//        VectorMaskCollection sc = new VectorMaskCollection();
//        int vectorCount = data.length;
//        int vectorWidth = data[0].length;
//        if (width < vectorWidth) //truncate width if the base data is wider than the parameter
//            vectorWidth = width;
//        List<ShapleyVector> shapleyVectors = new ArrayList<>(vectorCount);
//        //super slow and ugly conversion from arrays to lists
//        for (int shapleyVectorIndex = 0; shapleyVectorIndex < vectorCount; shapleyVectorIndex++) {
//            ShapleyVector sv = new ShapleyVector();
//            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
//                sv.getData().add(data[shapleyVectorIndex][vectorIndex]
//                    * scaling); //add projection scaling
//            }
//            shapleyVectors.add(sv);
//        }
//        sc.setValues(shapleyVectors);
//        return sc;
//    }

    public static VectorMaskCollection fromData(double[][] data) {
        VectorMaskCollection sc = new VectorMaskCollection();
        int vectorCount = data.length;
        int vectorWidth = data[0].length;
        List<VectorMask> vectorMasks = new ArrayList<>(vectorCount);
        //super slow and ugly conversion from arrays to lists
        for (int vectorMasksIndex = 0; vectorMasksIndex < vectorCount; vectorMasksIndex++) {
            VectorMask sv = new VectorMask();
            for (int vectorIndex = 0; vectorIndex < vectorWidth; vectorIndex++) {
                sv.getData().add(data[vectorMasksIndex][vectorIndex]);
            }
            vectorMasks.add(sv);
        }
        sc.setValues(vectorMasks);
        return sc;
    }

    public static VectorMaskCollection fakeCollection(int rows, int columns, double scale) {
        VectorMaskCollection sc = new VectorMaskCollection();
        Random rando = new Random();
        List<VectorMask> vectorMasks = new ArrayList<>(rows);
        //Assumes Image input mapping values to pixel grid
        for (int shapleyVectorIndex = 0; shapleyVectorIndex < rows; shapleyVectorIndex++) {
            for (int vectorIndex = 0; vectorIndex < columns; vectorIndex++) {
                VectorMask sv = new VectorMask();
                sv.getData().add(rando.nextDouble() * scale);
                Color color = new Color(rando.nextDouble(), 1.0, 1.0, 1.0);
                sv.setColor("#" + Utils.convertColorToString(color));
                vectorMasks.add(sv);
            }
        }
        sc.setValues(vectorMasks);
        return sc;

    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the features
     */
    public List<VectorMask> getValues() {
        return vectorMasks;
    }

    /**
     * @param values the features to set
     */
    public void setValues(List<VectorMask> values) {
        this.vectorMasks = values;
    }

    //</editor-fold>

}
