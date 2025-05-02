package edu.jhuapl.trinity.javafx.components.annotations;

import edu.jhuapl.trinity.data.coco.CocoAnnotation;
import edu.jhuapl.trinity.data.coco.CocoCategory;
import edu.jhuapl.trinity.data.coco.CocoImage;
import edu.jhuapl.trinity.data.coco.CocoObject;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import java.io.File;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
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
    Text annotationText;
    TitledPane imagesTP;
    TitledPane categoriesTP;
    TitledPane annotationsTP;
    CocoObject cocoObject = null;
    public SimpleStringProperty imageBasePathProperty = new SimpleStringProperty(".");
    
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
        Button clearAnnotations = new Button("Clear Annotations");
        clearAnnotations.setOnAction(e -> {
            clearAnnotations.getScene().getRoot().fireEvent(
                new ImageEvent(ImageEvent.CLEAR_COCO_ANNOTATIONS)); 
        });
                
        annotationsListView = new ListView();
        ImageView annotationsListIV = ResourceUtils.loadIcon("waitingforimage", 64);
        VBox annotationsPlaceholder = new VBox(10, annotationsListIV, new Label("I should COCO..."));
        annotationsPlaceholder.setAlignment(Pos.CENTER);
        annotationsListView.setPlaceholder(annotationsPlaceholder);
        annotationsListView.setPrefHeight(DEFAULT_LISTVIEW_HEIGHT);

        annotationText = new Text("No Annotation Selected.");
//        annotationsText.setWrappingWidth(50); //something smallish just to initialize
//        annotationsText.wrappingWidthProperty().bind(annotationsTP.widthProperty().subtract(10));
        annotationText.setFont(new Font("Consolas", 16));
        annotationText.setStroke(Color.WHITE);
        annotationText.setFill(Color.ALICEBLUE);
        
        annotationsTP.setContent(new VBox(5, clearAnnotations, annotationsListView, annotationText));        
        
        setSpacing(10);
        setPrefWidth(DEFAULT_PREF_WIDTH);
        getChildren().addAll( 
            imagesTP, 
            annotationsTP,
            categoriesTP
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
        imageListView.addEventHandler(MouseEvent.MOUSE_CLICKED, e-> {
            if(e.getClickCount() > 1){
                int index = imageListView.getSelectionModel().getSelectedIndex();
                if(index < cocoObject.getImages().size()) {
                    CocoImage image = cocoObject.getImages().get(index);
                    String resolvedPath = imageBasePathProperty.get();
                    //if we have a full path.. use it... otherwise just the filename
                    String imageFile = 
                        (null != image.getPath() && !image.getPath().isBlank()) ?
                        image.getPath() : image.getFile_name();
                    resolvedPath += imageFile;
                    try {
                        File resolvedFile = new File(resolvedPath);
                        if(resolvedFile.isFile()) {
                            Image img = new Image(resolvedFile.toURI().toURL().toExternalForm());
                            imageListView.getScene().getRoot().fireEvent(
                                new ImageEvent(ImageEvent.SELECT_COCO_IMAGE, img)); 
                        } else
                            LOG.error("Failed to load " + resolvedPath);
                    } catch(Exception ex) {
                        LOG.error("Failed to load " + resolvedPath);
                    }
                }
            }
        });
        categoriesListView.getSelectionModel().selectedIndexProperty().addListener(cl -> {
            int index = categoriesListView.getSelectionModel().getSelectedIndex();
            if(index < cocoObject.getCategories().size()) {
                CocoCategory cat = cocoObject.getCategories().get(index);
                StringBuilder sb = new StringBuilder();
                sb.append("Category ID: " ).append(" : ").append(cat.getId()).append("\n")
                    .append("Name: " ).append(" : ").append(cat.getName()).append("\n")
                    .append("Supercategory: " ).append(" : ").append(cat.getSupercategory()).append("\n");
                categoryText.setText(sb.toString());
            }
        });

        annotationsListView.getSelectionModel().selectedIndexProperty().addListener(cl -> {
            int index = annotationsListView.getSelectionModel().getSelectedIndex();
            if(index < cocoObject.getAnnotations().size()) {
                CocoAnnotation ann = cocoObject.getAnnotations().get(index);
                StringBuilder sb = new StringBuilder();
                sb.append("Annotation ID: " ).append(" : ").append(ann.getId()).append("\n")
                    .append("Image ID: " ).append(" : ").append(ann.getImage_id()).append("\n")
                    .append("Category ID: " ).append(" : ").append(ann.getCategory_id()).append("\n")
                    .append("BBox: " ).append(" : ").append(ann.bboxToString()).append("\n")
                    .append("Segmentations: " ).append(" : ").append(ann.getSegmentation().size()).append("\n");
                annotationText.setText(sb.toString());

                if(ann.isBBoxValid()) {
                    BBoxOverlay bbox = new BBoxOverlay(ann.getBbox(), String.valueOf(ann.getId()));
                    imageListView.getScene().getRoot().fireEvent(
                        new ImageEvent(ImageEvent.SELECT_COCO_BBOX, bbox)); 
                }
//                if(ann.isSegmentationValid()) {
//                    SegmentationOverlay seg = new SegmentationOverlay(
//                        new BBox(ann.getBbox()), String.valueOf(ann.getId()));
//                    imageListView.getScene().getRoot().fireEvent(
//                        new ImageEvent(ImageEvent.NEW_COCO_BBOX, bbox)); 
//                }
                
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
