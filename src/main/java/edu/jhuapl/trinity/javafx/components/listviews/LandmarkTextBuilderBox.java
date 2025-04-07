package edu.jhuapl.trinity.javafx.components.listviews;

import edu.jhuapl.trinity.utils.ResourceUtils;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * @author Sean Phillips
 */
public class LandmarkTextBuilderBox extends VBox {

    public static double LISTVIEW_PREF_HEIGHT = 150;
    public static double BUTTON_PREF_WIDTH = 125;

    Background transFillBack = new Background(new BackgroundFill(
        Color.ALICEBLUE.deriveColor(1, 1, 1, 0.1), CornerRadii.EMPTY, Insets.EMPTY));

    protected ListView<LandmarkTextListItem> landmarksListView;

    public LandmarkTextBuilderBox() {
        TextField entryTextField = new TextField();
        entryTextField.setPrefWidth(200);
        Label entryLabel = new Label("Enter New Label Choice");
        
        Button addButton = new Button("Add");
        addButton.setPrefWidth(BUTTON_PREF_WIDTH);
        addButton.setOnAction(e-> {
            if(!entryTextField.getText().isBlank()){
                landmarksListView.getItems().add(
                    new LandmarkTextListItem(entryTextField.getText())
                );
                entryTextField.clear();
            }
        });
        Button clearButton = new Button("Clear");
        clearButton.setPrefWidth(BUTTON_PREF_WIDTH);
        clearButton.setOnAction(e-> landmarksListView.getItems().clear());
        HBox buttonHBox = new HBox(10, entryLabel, addButton, clearButton);
        buttonHBox.setAlignment(Pos.CENTER);
        
        landmarksListView = new ListView<>();
        landmarksListView.setPrefHeight(LISTVIEW_PREF_HEIGHT);
        landmarksListView.setEditable(true);
        ImageView iv = ResourceUtils.loadIcon("console", 128);
        VBox placeholder = new VBox(10, iv, new Label("No Text Landmarks Acquired"));
        placeholder.setAlignment(Pos.CENTER);
        landmarksListView.setPlaceholder(placeholder);
        
        setSpacing(10);
        setPadding(new Insets(5, 0, 0, 0));
        getChildren().addAll(
            buttonHBox, 
            entryTextField,
            landmarksListView
        );
        setBackground(transFillBack);
    }
    public void setChoices(List<String> choices){
        landmarksListView.getItems().setAll(choices.stream()
            .map(c -> new LandmarkTextListItem(c))
            .toList());
    }
    
    public List<String> getChoices(){
        List<String> choices = new ArrayList<>();
        choices.addAll(
            landmarksListView.getItems().stream()
                .map(LandmarkListItem::getFeatureVectorLabel)
                .toList());
        return choices;
    }
    public List<LandmarkTextListItem> getItems() {
        return landmarksListView.getItems();
    }
}
