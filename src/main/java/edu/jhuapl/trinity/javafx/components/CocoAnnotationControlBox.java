package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.data.coco.CocoAnnotation;
import edu.jhuapl.trinity.data.coco.CocoCategory;
import edu.jhuapl.trinity.data.coco.CocoImage;
import edu.jhuapl.trinity.data.coco.CocoObject;
import edu.jhuapl.trinity.utils.ResourceUtils;
import java.io.File;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.TitledPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * @author Sean Phillips
 */
public class CocoAnnotationControlBox extends VBox {
    private static final Logger LOG = LoggerFactory.getLogger(CocoAnnotationControlBox.class);
    public static double DEFAULT_PREF_WIDTH = 320;
    public static double DEFAULT_TITLEDPANE_WIDTH = 300;
    public static double DEFAULT_LISTVIEW_HEIGHT = 200;
    ListView<String> imageListView;
    ListView<String> categoriesListView;
    ListView<String> annotationsListView;
    Text imageText;
    Text categoryText;
    Text annotationsText;
    TitledPane imagesTP;
    TitledPane categoriesTP;
    TitledPane annotationsTP;
    CocoObject cocoObject = null;
    
    public CocoAnnotationControlBox() {
        imagesTP = new TitledPane();
        imagesTP.setText("Images");
        imagesTP.setExpanded(true);
        imagesTP.setPrefWidth(DEFAULT_TITLEDPANE_WIDTH);

        imageListView = new ListView();
        ImageView imageListIV = ResourceUtils.loadIcon("waitingforimage", 64);
        VBox imagesPlaceholder = new VBox(10, imageListIV, new Label("I should COCO..."));
        imagesPlaceholder.setAlignment(Pos.CENTER);
        imageListView.setPlaceholder(imagesPlaceholder);
        imageListView.setPrefHeight(DEFAULT_LISTVIEW_HEIGHT);
        imageText = new Text("No Category Selected.");
//        imageText.setWrappingWidth(50); //something smallish just to initialize
        imageText.setFont(new Font("Consolas", 16));
        imageText.setStroke(Color.ALICEBLUE);
        imageText.setFill(Color.ALICEBLUE);
        imagesTP.setContent(new VBox(5, imageListView, imageText));        
//        imageText.wrappingWidthProperty().bind(imageVBox.widthProperty().subtract(10));
        
        categoriesTP = new TitledPane();
        categoriesTP.setText("Categories");
        categoriesTP.setExpanded(false);
        categoriesTP.setPrefWidth(DEFAULT_TITLEDPANE_WIDTH);
        categoriesListView = new ListView();
        ImageView categoriesListIV = ResourceUtils.loadIcon("waitingforimage", 64);
        VBox categoriesPlaceholder = new VBox(10, categoriesListIV, new Label("I should COCO..."));
        categoriesPlaceholder.setAlignment(Pos.CENTER);
        categoriesListView.setPlaceholder(categoriesPlaceholder);
        categoriesListView.setPrefHeight(DEFAULT_LISTVIEW_HEIGHT);
        
        categoryText = new Text("No Category Selected.");
//        categoryText.setWrappingWidth(50); //something smallish just to initialize
//        categoryText.wrappingWidthProperty().bind(categoriesTP.widthProperty().subtract(10));
        categoryText.setFont(new Font("Consolas", 16));
        categoryText.setStroke(Color.ALICEBLUE);
        categoryText.setFill(Color.WHITE);
        categoriesTP.setContent(new VBox(5, categoriesListView, categoryText));        
        
        annotationsTP = new TitledPane();
        annotationsTP.setText("Annotations");
        annotationsTP.setExpanded(false);
        annotationsTP.setPrefWidth(DEFAULT_TITLEDPANE_WIDTH);
        annotationsListView = new ListView();
        ImageView annotationsListIV = ResourceUtils.loadIcon("waitingforimage", 64);
        VBox annotationsPlaceholder = new VBox(10, annotationsListIV, new Label("I should COCO..."));
        annotationsPlaceholder.setAlignment(Pos.CENTER);
        annotationsListView.setPlaceholder(annotationsPlaceholder);
        annotationsListView.setPrefHeight(DEFAULT_LISTVIEW_HEIGHT);

        annotationsText = new Text("No Annotation Selected.");
//        annotationsText.setWrappingWidth(50); //something smallish just to initialize
//        annotationsText.wrappingWidthProperty().bind(annotationsTP.widthProperty().subtract(10));
        annotationsText.setFont(new Font("Consolas", 16));
        annotationsText.setStroke(Color.WHITE);
        annotationsText.setFill(Color.ALICEBLUE);
        
        annotationsTP.setContent(new VBox(5, annotationsListView, annotationsText));        
        
        setSpacing(10);
        setPrefWidth(DEFAULT_PREF_WIDTH);
        getChildren().addAll( 
            imagesTP, 
            categoriesTP, 
            annotationsTP
        );
        
        addListeners();
    }
    private void addListeners() {
        imageListView.getSelectionModel().selectedIndexProperty().addListener(cl -> {
            int index = imageListView.getSelectionModel().getSelectedIndex();
            if(index < cocoObject.getImages().size()) {
                CocoImage image = cocoObject.getImages().get(index);
                StringBuilder sb = new StringBuilder();
                sb.append(image.getFile_name()).append("\n")
                    .append("Image ID: " ).append(" : ").append(image.getId()).append("\n")
                    .append("Width: " ).append(" : ").append(image.getWidth()).append("\n")
                    .append("Height: " ).append(" : ").append(image.getHeight()).append("\n");
                imageText.setText(sb.toString());

            }
        });
        
    }
    public void populateControls(CocoObject cocoObject) {
        this.cocoObject = cocoObject;
        //clear existing information
        imageListView.getItems().clear();
        categoriesListView.getItems().clear();
        annotationsListView.getItems().clear();

        //populate image items
        for(CocoImage img : cocoObject.getImages()){
            File file = new File(img.getFile_name());
            imageListView.getItems().add(file.getName());
        }
        //populate category items
        for(CocoCategory cat : cocoObject.getCategories()){
            categoriesListView.getItems().add(cat.getName());
        }
        //populate annotation items
        for(CocoAnnotation ann : cocoObject.getAnnotations()){
            annotationsListView.getItems().add(
                "ID: " + ann.getId() + " ImageID: " + ann.getImage_id());
        }
    }    
}
