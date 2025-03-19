package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

/**
 * @author Sean Phillips
 */
public class EmbeddingsImageListItem extends HBox {
    public static Image DEFAULT_ICON = ResourceUtils.loadIconFile("noimage");
    public static double PREF_DIMLABEL_WIDTH = 150;
    public static double PREF_FILELABEL_WIDTH = 250;
    public static AtomicInteger atomicID = new AtomicInteger();
    public static NumberFormat format = new DecimalFormat("0000");
        
    public int imageID;
    private ImageView imageView;
    private Label fileLabel;
    private File file;
    public boolean renderIcon;
    private Label dimensionsLabel;
    private TextField labelTextField;
    private FeatureVector featureVector = null;

    public EmbeddingsImageListItem(File file) {
        this(file, true);
    }
    public EmbeddingsImageListItem(File file, boolean renderIcon) {
        imageID = atomicID.getAndIncrement();
        this.file = file;
        this.renderIcon = renderIcon;
        fileLabel = new Label(file.getName());
        fileLabel.setPrefWidth(PREF_FILELABEL_WIDTH);
        reloadImage(renderIcon);
        imageView.setFitWidth(32);
        imageView.setFitHeight(32);
        
        labelTextField = new TextField();
        labelTextField.setEditable(true);
        labelTextField.setPrefWidth(PREF_DIMLABEL_WIDTH);
        labelTextField.setOnAction(e -> featureVector.setLabel(labelTextField.getText()));
        labelTextField.textProperty().addListener(e -> featureVector.setLabel(labelTextField.getText()));

        dimensionsLabel = new Label(format.format(0));

        getChildren().addAll(imageView, fileLabel, dimensionsLabel, labelTextField);
        setSpacing(20);
        setPrefHeight(32);
        featureVector = FeatureVector.EMPTY_FEATURE_VECTOR("", 3);
        featureVector.setImageURL(file.getAbsolutePath());
        Tooltip.install(this, new Tooltip(file.getAbsolutePath()));
        
        imageView.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() > 1) {
                getScene().getRoot().fireEvent(new FeatureVectorEvent(
                    FeatureVectorEvent.SELECT_FEATURE_VECTOR, featureVector));
                getScene().getRoot().fireEvent(
                    new FeatureVectorEvent(FeatureVectorEvent.LOCATE_FEATURE_VECTOR, featureVector));
            }
        });
        setOnMouseClicked(e -> {
            if (e.getClickCount() > 1) {
                getScene().getRoot().fireEvent(new FeatureVectorEvent(
                    FeatureVectorEvent.SELECT_FEATURE_VECTOR, featureVector));
                getScene().getRoot().fireEvent(
                    new FeatureVectorEvent(FeatureVectorEvent.LOCATE_FEATURE_VECTOR, featureVector));
            }
        });        
        
    }
    public void reloadImage(boolean renderIcon) {
        if(renderIcon) {
            try {
                imageView = new ImageView(ResourceUtils.loadImageFile(file));
            } catch (IOException ex) {
                Logger.getLogger(ImageFileListItem.class.getName()).log(Level.SEVERE, null, ex);
                imageView = new ImageView(DEFAULT_ICON);
            }
        } else {
            imageView = new ImageView(DEFAULT_ICON);
        }
    }
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
    public Image getCurrentImage() {
        return imageView.getImage();
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
    public String getFeatureVectorEntityID() {
        return featureVector.getEntityId();
    }
    public void setFeatureVectorEntityID(String entityID) {
        featureVector.setEntityId(entityID);
    }
    public static Function<File, EmbeddingsImageListItem> itemFromFile = file -> {
        return new EmbeddingsImageListItem(file);
    };     
    public static Function<File, EmbeddingsImageListItem> itemNoRenderFromFile = file -> {
        return new EmbeddingsImageListItem(file, false);
    };     
}
