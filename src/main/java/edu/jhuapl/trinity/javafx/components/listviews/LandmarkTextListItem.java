package edu.jhuapl.trinity.javafx.components.listviews;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * @author Sean Phillips
 */
public class LandmarkTextListItem extends LandmarkListItem {
    public static double PREF_TEXTFIELD_WIDTH = 250;
    private Label fileLabel;
    private TextField labelTextField;

    public LandmarkTextListItem(String landmarkLabel) {
        super(landmarkLabel);

        labelTextField = new TextField();
        labelTextField.setEditable(true);
        labelTextField.setPrefWidth(PREF_TEXTFIELD_WIDTH);
        labelTextField.setOnAction(e -> getFeatureVector().setLabel(labelTextField.getText()));
        labelTextField.textProperty().addListener(e -> getFeatureVector().setLabel(labelTextField.getText()));
        labelTextField.setText(landmarkLabel);
        getChildren().addAll(labelTextField);
        setSpacing(20);
        setPrefHeight(32);
    }

    @Override
    public void setFeatureVectorLabel(String text) {
        super.setFeatureVectorLabel(text);
        labelTextField.setText(text);
    }

    public void setLabelWidth(double width) {
        fileLabel.setPrefWidth(width);
    }
}
