/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.data.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.DoubleStream;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeatureVector extends MessageData {

    public static final String TYPESTRING = "feature_vector";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
    '{
        "messageType": "feature_vector",
        "topic": "some_topic",
        "messageId": 0,
        "data": [-0.4232191175986961, -0.5031443592838056, 0.30497221256679125,
            0.4157550505277444, -0.46174460409303325, -0.12950797668733202,
            0.6323170694189965, 0.20112482321095512, -0.0770502704073328,
            -0.018055872253030292, 0.08990142832758276, 0.14425006537440124,
            -0.30635162612534406, -0.04588245408916634, 0.37569343542885386,
            -0.15806087484089912, 0.0673835604499377, 0.17998157972463474,
            -0.01611924502337739, -0.24604972532815875, -0.01560032825534631,
            0.3946917081600188, -0.12442754943149016, -0.18011471923351433,
            -0.04629288711207645, 0.5376945006956948, 0.18828509670172092,
            -0.3326629890669504, -0.0020758551490591298, 0.20795446895704697,
            0.17734780464694122, -0.3287723687849336, -0.2575243260361731,
            0.2656010566497462, 0.1351256172194629, -0.21505064172654048,
            -0.49758121872237776, 0.052445275025069085, 0.463676957756242,
            -0.02580167997317252, -0.29729273004911727, 0.02267132636472527,
            -0.021781132983331605, 0.2855062868586593, -0.11389146262348109,
            -0.4338320677142379, 0.14545007041168245, 0.34325194689681915
        ],
        "score": -2.753245759396493,
        "pfa": 0.0008605957637858228,
        "label": "no_object",
        "bbox": [0.0, 0, 16.0, 0],
        "imageId": 7952,
        "frameId": 0,
        "imageURL": "/media/images/video_10_frame_666.jpg",
        "mediaURL": "/media/localrecordings/video_10_segment_8.wav",
        "layer": 3
    }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private String entityId;
    private List<Double> data;
    private String label;
    private List<Double> bbox; //[0.0, 0, 16.0, 0]
    private long imageId;
    private long frameId;
    private String imageURL;
    private String mediaURL;
    private double score;
    private double pfa;
    private int layer;
    private String text = "";
    private HashMap<String, String> metaData;
    //</editor-fold>

    public FeatureVector() {
        this.messageType = TYPESTRING;
        this.data = new ArrayList<>();
        this.metaData = new HashMap<>();
    }

    public boolean isBBoxValid() {
        return null != getBbox() && !getBbox().isEmpty() && getBbox().size() > 3
            && getBbox().get(2) > 0.0 && getBbox().get(3) > 0.0;
    }

    public double getMin() {
        return getData().stream().min(Double::compare).get();
    }

    public double getMax() {
        return getData().stream().max(Double::compare).get();
    }

    public double getWidth() {
        return Math.abs(getMax() - getMin());
    }

    public static boolean isFeatureVector(String messageBody) {
        return messageBody.contains("messageType")
            && messageBody.contains(TYPESTRING);
    }

    public static double getMaxAbsValue(List<FeatureVector> featureVectors) {
        return featureVectors.parallelStream().flatMapToDouble((t) -> {
            return t.getData().stream().mapToDouble((value) -> {
                return Math.abs(value);
            });
        }).max().getAsDouble();
    }

    public static double getMeanCenteredMaxAbsValue(List<FeatureVector> featureVectors, List<Double> meanVector) {
        int dataSize = featureVectors.get(0).getData().size();
        Double maxMax = null;
        double currentMax;
        for (int dataIndex = 0; dataIndex < dataSize; dataIndex++) {
            int index = dataIndex;
            currentMax = featureVectors.stream()
                .mapMultiToDouble((t, u) -> {
                    if (null != t)
                        u.accept(Math.abs(t.getData().get(index) - meanVector.get(index)));
                })
                .max().getAsDouble();
            //System.out.println("CurrentMax: " + currentMax + " meanVector.get(index): " + meanVector.get(index));
            if (null == maxMax || currentMax > maxMax)
                maxMax = currentMax;
        }

        return maxMax;
    }

    public static List<Double> getMeanVector(List<FeatureVector> featureVectors) {
        int dataSize = featureVectors.get(0).getData().size();
        List<Double> meanVector = new ArrayList<>(dataSize);

        for (int dataIndex = 0; dataIndex < dataSize; dataIndex++) {
            int index = dataIndex;
            meanVector.add(index, featureVectors.stream()
                .mapMultiToDouble((t, u) -> {
                    if (null != t)
                        u.accept(t.getData().get(index));
                })
                .average().getAsDouble());
        }
        return meanVector;
    }

    public static void updateMeanVector(List<Double> meanVector, FeatureVector featureVector) {
        int dataSize = meanVector.size();

        for (int i = 0; i < dataSize; i++) {
            meanVector.set(i, (meanVector.get(i) + featureVector.getData().get(i)) / 2.0);
        }
    }

    private double getGlobalMean(List<FeatureVector> featureVectors) {
        return featureVectors.stream().flatMapToDouble(new Function<FeatureVector, DoubleStream>() {
            @Override
            public DoubleStream apply(FeatureVector t) {
                return t.getData().stream().mapToDouble(Double::doubleValue);
            }
        }).average().getAsDouble();
    }

    public static Function<FeatureVector, String> mapDataToString = (state) -> {
        return state.getData().toString();
    };

    public static Function<FeatureVector, double[]> mapToStateArray = (state) -> {
        double[] states = new double[state.data.size()];
        for (int i = 0; i < states.length; i++) {
            states[i] = state.data.get(i);
        }
        return states;
    };

    public static FeatureVector fromData(double[] data, int width, double scaling) {
        FeatureVector fv = new FeatureVector();
        for (int vectorIndex = 0; vectorIndex < width; vectorIndex++) {
            fv.getData().add(data[vectorIndex] * scaling);
        }
        return fv;
    }

    public static FeatureVector EMPTY_FEATURE_VECTOR(String label, int dataSize) {
        FeatureVector fv = new FeatureVector();
        fv.setLabel(label);
        ArrayList<Double> newData = new ArrayList<>(dataSize);
        for (int i = 0; i < dataSize; i++)
            newData.add(0.0);
        fv.setData(newData);
        ArrayList<Double> bboxList = new ArrayList<>();
        bboxList.add(0.0);
        bboxList.add(0.0);
        bboxList.add(0.0);
        bboxList.add(0.0);
        fv.setBbox(bboxList);
        return fv;
    }

    public boolean metaContainsTerm(String term) {
        for (Entry<String, String> entry : metaData.entrySet()) {
            if (entry.getKey().contains(term) || entry.getValue().contains(term))
                return true;
        }
        return false;
    }

    public String metadataAsString(String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : getMetaData().entrySet()) {
            sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append(delimiter);
        }
        return sb.toString();
    }
    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the entityId
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * @param entityId the entityId to set
     */
    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    /**
     * @return the data
     */
    public List<Double> getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(List<Double> data) {
        this.data = data;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the bbox
     */
    public List<Double> getBbox() {
        return bbox;
    }

    /**
     * @param bbox the bbox to set
     */
    public void setBbox(List<Double> bbox) {
        this.bbox = bbox;
    }

    /**
     * @return the imageId
     */
    public long getImageId() {
        return imageId;
    }

    /**
     * @param imageId the imageId to set
     */
    public void setImageId(long imageId) {
        this.imageId = imageId;
    }

    /**
     * @return the frameId
     */
    public long getFrameId() {
        return frameId;
    }

    /**
     * @param frameId the frameId to set
     */
    public void setFrameId(long frameId) {
        this.frameId = frameId;
    }

    /**
     * @return the imageURL
     */
    public String getImageURL() {
        return imageURL;
    }

    /**
     * @param imageURL the imageURL to set
     */
    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    /**
     * @return the score
     */
    public double getScore() {
        return score;
    }

    /**
     * @param score the score to set
     */
    public void setScore(double score) {
        this.score = score;
    }

    /**
     * @return the pfa
     */
    public double getPfa() {
        return pfa;
    }

    /**
     * @param pfa the pfa to set
     */
    public void setPfa(double pfa) {
        this.pfa = pfa;
    }

    /**
     * @return the layer
     */
    public int getLayer() {
        return layer;
    }

    /**
     * @param layer the layer to set
     */
    public void setLayer(int layer) {
        this.layer = layer;
    }

    /**
     * @return the metaData
     */
    public HashMap<String, String> getMetaData() {
        return metaData;
    }

    /**
     * @param metaData the metaData to set
     */
    public void setMetaData(HashMap<String, String> metaData) {
        this.metaData = metaData;
    }
    //</editor-fold>

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the mediaURL
     */
    public String getMediaURL() {
        return mediaURL;
    }

    /**
     * @param mediaURL the mediaURL to set
     */
    public void setMediaURL(String mediaURL) {
        this.mediaURL = mediaURL;
    }
}
