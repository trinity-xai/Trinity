package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * @author Sean Phillips
 */
public class EmbeddingsImageListItem extends HBox {
    public static double PREF_LABEL_WIDTH = 200;
    private Label descriptionLabel;
    private Label labelLabel;
    private TextField labelTextField;
    private FeatureVector featureVector;

    public EmbeddingsImageListItem(FeatureVector featureVector) {
        this.featureVector = featureVector;
        descriptionLabel = new Label("Dimensions: " + featureVector.getData().size());
        descriptionLabel.setPrefWidth(PREF_LABEL_WIDTH);
        labelLabel = new Label("Label");

        labelTextField = new TextField();
        labelTextField.setEditable(true);
        labelTextField.setPrefWidth(PREF_LABEL_WIDTH);
        labelTextField.setOnAction(e -> featureVector.setLabel(labelTextField.getText()));
        labelTextField.textProperty().addListener(e -> featureVector.setLabel(labelTextField.getText()));
        getChildren().addAll(descriptionLabel, labelLabel, labelTextField);
        setSpacing(20);
        setPrefHeight(32);
    }
    public void setFeatureVectorLabel(String text) {
        labelTextField.setText(text);
        featureVector.setLabel(text);
    }
    public String getFeatureVectorLabel() {
        return labelTextField.getText();
    }
}
