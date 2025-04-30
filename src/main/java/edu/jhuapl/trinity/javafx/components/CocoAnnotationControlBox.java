package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
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
    public static double DEFAULT_TITLEDPANE_WIDTH = 300;
    ListView<String> imageListView;
    ListView<String> categoriesListView;
    ListView<String> annotationsListView;
    Text imageText;
    Text categoryText;
    Text annotationsText;
    TitledPane categoriesTP;
    TitledPane annotationsTP;
    
    public CocoAnnotationControlBox() {
        imageListView = new ListView();
        ImageView imageListIV = ResourceUtils.loadIcon("waitingforimage", 64);
        VBox imagesPlaceholder = new VBox(10, imageListIV, new Label("I should COCO..."));
        imagesPlaceholder.setAlignment(Pos.CENTER);
        imageListView.setPlaceholder(imagesPlaceholder);

        imageText = new Text("No Category Selected.");
//        imageText.setWrappingWidth(50); //something smallish just to initialize
        imageText.setFont(new Font("Consolas", 16));
        imageText.setStroke(Color.ALICEBLUE);
        imageText.setFill(Color.ALICEBLUE);
        VBox imageVBox = new VBox(5, imageListView, imageText);        
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

        annotationsText = new Text("No Annotation Selected.");
//        annotationsText.setWrappingWidth(50); //something smallish just to initialize
//        annotationsText.wrappingWidthProperty().bind(annotationsTP.widthProperty().subtract(10));
        annotationsText.setFont(new Font("Consolas", 16));
        annotationsText.setStroke(Color.WHITE);
        annotationsText.setFill(Color.ALICEBLUE);
        
        annotationsTP.setContent(new VBox(5, annotationsListView, annotationsText));        
        
        setSpacing(10);
        getChildren().addAll( 
            imageVBox, 
            new Separator(Orientation.HORIZONTAL), 
            categoriesTP, 
            annotationsTP
        );
    }
}
