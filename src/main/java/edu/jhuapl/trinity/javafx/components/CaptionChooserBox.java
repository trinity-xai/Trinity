package edu.jhuapl.trinity.javafx.components;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * @author Sean Phillips
 */
public class CaptionChooserBox extends VBox {

    public static double LISTVIEW_PREF_HEIGHT = 150;
    public static double BUTTON_PREF_WIDTH = 125;

    Background transBack = new Background(new BackgroundFill(
        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));

    Background transFillBack = new Background(new BackgroundFill(
        Color.ALICEBLUE.deriveColor(1, 1, 1, 0.222), CornerRadii.EMPTY, Insets.EMPTY));

    ListView<String> choicesListView;

    public CaptionChooserBox() {
        TextField entryTextField = new TextField();
        entryTextField.setPrefWidth(200);
        Label entryLabel = new Label("Enter New Caption Choice");
        
        Button addButton = new Button("Add");
        addButton.setPrefWidth(BUTTON_PREF_WIDTH);
        addButton.setOnAction(e-> {
            if(!entryTextField.getText().isBlank()){
                choicesListView.getItems().add(entryTextField.getText());
            }
        });
        Button clearButton = new Button("Clear");
        clearButton.setPrefWidth(BUTTON_PREF_WIDTH);
        clearButton.setOnAction(e-> choicesListView.getItems().clear());
        HBox buttonHBox = new HBox(10, entryLabel, addButton, clearButton);
        buttonHBox.setAlignment(Pos.CENTER);
        
        choicesListView = new ListView<>();
        choicesListView.setPrefHeight(LISTVIEW_PREF_HEIGHT);
        choicesListView.setEditable(true);
        
        
        setSpacing(10);
        setPadding(new Insets(5, 0, 0, 0));
        getChildren().addAll(
            buttonHBox, 
            entryTextField,
            choicesListView
        );
        setBackground(transFillBack);
    }
    public void setChoices(List<String> choices){
        choicesListView.getItems().setAll(choices);
    }
    
    public List<String> getChoices(){
        List<String> choices = new ArrayList<>();
        choices.addAll(choicesListView.getItems());
        return choices;
    }
}
