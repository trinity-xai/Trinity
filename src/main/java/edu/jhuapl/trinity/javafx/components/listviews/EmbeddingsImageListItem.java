package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.utils.ResourceUtils;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.converter.IntegerStringConverter;

/**
 * @author Sean Phillips
 */
public class EmbeddingsImageListItem extends HBox {
    public static Image DEFAULT_ICON = ResourceUtils.loadIconFile("noimage");
    public static double PREF_DIMLABEL_WIDTH = 75;
    public static double PREF_FILELABEL_WIDTH = 250;
    public static AtomicInteger atomicID = new AtomicInteger();
    public static NumberFormat format = new DecimalFormat("0000");
        
    public int imageID;
    private ImageView imageView;
    private Label fileLabel;
    private File file;
    public boolean renderIcon;
    private Label diminsionsLabel;
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

        diminsionsLabel = new Label(format.format(0));
        diminsionsLabel.setPrefWidth(PREF_DIMLABEL_WIDTH);

        getChildren().addAll(imageView, fileLabel, diminsionsLabel, labelTextField);
        setSpacing(20);
        setPrefHeight(32);
        featureVector = FeatureVector.EMPTY_FEATURE_VECTOR("", 3);
        Tooltip.install(this, new Tooltip(file.getAbsolutePath()));
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
        diminsionsLabel.setText(format.format(data.size()));
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
    public void addExplanation(String explanation) {
        featureVector.getMetaData().put("explanation", explanation);
    }
    public void addDescription(String description) {
        featureVector.setText(description);
    }
    
    public static Function<File, EmbeddingsImageListItem> itemFromFile = file -> {
        return new EmbeddingsImageListItem(file);
    };     
}
