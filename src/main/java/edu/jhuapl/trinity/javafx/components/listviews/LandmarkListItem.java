package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * @author Sean Phillips
 */
public class LandmarkListItem extends HBox {
    public static double PREF_DIMLABEL_WIDTH = 150;
    public static NumberFormat format = new DecimalFormat("0000");
    public static AtomicInteger atomicID = new AtomicInteger();
    public int landmarkID;    
    private String landmarkLabel;
    private Label dimensionsLabel;
    private FeatureVector featureVector = null;

    public LandmarkListItem(String landmarkLabel) {
        this.landmarkLabel = landmarkLabel;
        landmarkID = atomicID.getAndIncrement();
        featureVector = FeatureVector.EMPTY_FEATURE_VECTOR("", 3);
        featureVector.setLabel(landmarkLabel);        
        
        dimensionsLabel = new Label(format.format(0));
        getChildren().addAll(dimensionsLabel);
        setSpacing(20);
        setPrefHeight(32);
    }

    public void setEmbeddings(List<Double> data) {
        featureVector.getData().clear();
        featureVector.getData().addAll(data);
        dimensionsLabel.setText(format.format(data.size()));
    }
    public void setFeatureVectorLabel(String text) {
        featureVector.setLabel(text);
    }
    public String getFeatureVectorLabel() {
        return featureVector.getLabel();
    }

    public void addMetaData(String key, String value){
        featureVector.getMetaData().put(key, value);
    }
    public void addExplanation(String explanation) {
        addMetaData("explanation", explanation);
    }
    public void addDescription(String description) {
        featureVector.setText(description);
    }
    public FeatureVector getFeatureVector(){
        return featureVector;
    }
}