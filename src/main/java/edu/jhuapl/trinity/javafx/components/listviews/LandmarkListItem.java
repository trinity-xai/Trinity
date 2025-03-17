package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
    private Label fileLabel;
    private Label dimensionsLabel;
    private TextField labelTextField;
    private FeatureVector featureVector = null;

    public LandmarkListItem(String landmarkLabel) {
        this.landmarkLabel = landmarkLabel;
        landmarkID = atomicID.getAndIncrement();
        featureVector = FeatureVector.EMPTY_FEATURE_VECTOR("", 3);
        featureVector.setLabel(landmarkLabel);        
        
        labelTextField = new TextField();
        labelTextField.setEditable(true);
        labelTextField.setPrefWidth(PREF_DIMLABEL_WIDTH);
        labelTextField.setOnAction(e -> featureVector.setLabel(labelTextField.getText()));
        labelTextField.textProperty().addListener(e -> featureVector.setLabel(labelTextField.getText()));
        labelTextField.setText(landmarkLabel);
        dimensionsLabel = new Label(format.format(0));

        getChildren().addAll(labelTextField, dimensionsLabel);
        setSpacing(20);
        setPrefHeight(32);
//        featureVector.setImageURL(file.getAbsolutePath());
//        Tooltip.install(this, new Tooltip(file.getAbsolutePath()));
    }
//    public void reloadImage(boolean renderIcon) {
//        if(renderIcon) {
//            try {
//                imageView = new ImageView(ResourceUtils.loadImageFile(file));
//            } catch (IOException ex) {
//                Logger.getLogger(ImageFileListItem.class.getName()).log(Level.SEVERE, null, ex);
//                imageView = new ImageView(DEFAULT_ICON);
//            }
//        } else {
//            imageView = new ImageView(DEFAULT_ICON);
//        }
//    }
    public void setEmbeddings(List<Double> data) {
        featureVector.getData().clear();
        featureVector.getData().addAll(data);
        dimensionsLabel.setText(format.format(data.size()));
    }
    public void setFeatureVectorLabel(String text) {
        labelTextField.setText(text);
        featureVector.setLabel(text);
    }
    public String getFeatureVectorLabel() {
        return labelTextField.getText();
    }
    public void setLabelWidth(double width) {
        fileLabel.setPrefWidth(width);
    }    
//    public Image getCurrentImage() {
//        return imageView.getImage();
//    }
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
//    public static Function<File, LandmarkListItem> itemFromFile = file -> {
//        return new LandmarkListItem(file);
//    };     
//    public static Function<File, LandmarkListItem> itemNoRenderFromFile = file -> {
//        return new LandmarkListItem(file, false);
//    };     
}
