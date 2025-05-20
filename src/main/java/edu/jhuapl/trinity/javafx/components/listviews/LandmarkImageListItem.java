package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author Sean Phillips
 */
public class LandmarkImageListItem extends LandmarkListItem {
    public static Image DEFAULT_ICON = ResourceUtils.loadIconFile("noimage");
    private static final Logger LOG = LoggerFactory.getLogger(LandmarkImageListItem.class);
    public static double PREF_TEXTFIELD_WIDTH = 250;
    private ImageView imageView;
    private File file;
    private TextField labelTextField;

    public LandmarkImageListItem(File file) {
        super(file.getName());
        this.file = file;
        labelTextField = new TextField();
        labelTextField.setEditable(true);
        labelTextField.setPrefWidth(PREF_TEXTFIELD_WIDTH);
        labelTextField.setOnAction(e -> getFeatureVector().setLabel(labelTextField.getText()));
        labelTextField.textProperty().addListener(e -> getFeatureVector().setLabel(labelTextField.getText()));
        labelTextField.setText(file.getName());

        try {
            imageView = new ImageView(ResourceUtils.loadImageFile(file));
        } catch (IOException ex) {
            LOG.error(null, ex);
            imageView = new ImageView(DEFAULT_ICON);
        }
        imageView.setFitWidth(32);
        imageView.setFitHeight(32);

        getChildren().add(0, imageView);
        getChildren().add(labelTextField);
        setSpacing(20);
        setPrefHeight(32);
    }

    public Image getCurrentImage() {
        return imageView.getImage();
    }
}
