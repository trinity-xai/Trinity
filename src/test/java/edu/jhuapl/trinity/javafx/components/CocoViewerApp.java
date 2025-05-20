package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.data.coco.CocoObject;
import edu.jhuapl.trinity.data.files.CocoAnnotationFile;
import edu.jhuapl.trinity.javafx.components.annotations.BBoxOverlay;
import edu.jhuapl.trinity.javafx.components.annotations.CocoAnnotationControlBox;
import edu.jhuapl.trinity.javafx.components.annotations.CocoAnnotationPane;
import edu.jhuapl.trinity.javafx.components.annotations.SegmentationOverlay;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Phillips
 */
public class CocoViewerApp extends Application {

    CocoAnnotationPane cocoAnnotationPane;
    TextField basePathTextField;
    CocoObject cocoObject;
    CocoAnnotationControlBox controls;
    //make transparent so it doesn't interfere with subnode transparency effects
    Background transBack = new Background(new BackgroundFill(
        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));

    @Override
    public void init() {
        cocoAnnotationPane = new CocoAnnotationPane();
        controls = new CocoAnnotationControlBox();
        basePathTextField = new TextField("");
        controls.imageBasePathProperty.bind(basePathTextField.textProperty());
    }

    @Override
    public void start(Stage stage) {
        BorderPane borderPane = new BorderPane(cocoAnnotationPane);
        borderPane.setBackground(transBack);
        borderPane.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        borderPane.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                final File file = db.getFiles().get(0);
                try {
                    if (CocoAnnotationFile.isCocoAnnotationFile(file)) {
                        System.out.println("Detected CocoAnnotation File...");
                        loadCocoFile(file);
                        controls.populateControls(cocoObject);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(CocoViewerApp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        ScrollPane scrollPane = new ScrollPane(controls);
        // hide scrollpane scrollbars
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPadding(Insets.EMPTY);
        scrollPane.setPannable(true);

        controls.heightProperty().addListener(cl -> {
            scrollPane.setVvalue(scrollPane.getVmax());
        });


        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = new File(basePathTextField.getText());
            if (f.isDirectory())
                dc.setInitialDirectory(f);
            dc.setTitle("Browse to imagery base path...");
            File dir = dc.showDialog(null);
            if (null != dir && dir.isDirectory()) {
                basePathTextField.setText(dir.getPath());
            }
        });
        HBox basePathHBox = new HBox(10, browseButton, basePathTextField);
        basePathHBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(basePathTextField, Priority.ALWAYS);
        basePathTextField.setPrefHeight(40);
        VBox basePathVBox = new VBox(5,
            new Label("Imagery Base Path"), basePathHBox);
        basePathVBox.setPadding(new Insets(5));
        borderPane.setTop(basePathVBox);
        borderPane.setLeft(scrollPane);
        borderPane.getStyleClass().add("trinity-pane");

        Scene scene = new Scene(borderPane, Color.BLACK);

        scene.addEventHandler(ImageEvent.CLEAR_COCO_ANNOTATIONS, e -> {
            cocoAnnotationPane.clearAnnotations();
        });
        scene.addEventHandler(ImageEvent.SELECT_COCO_BBOX, e -> {
            BBoxOverlay bbox = (BBoxOverlay) e.object;
            if (null != bbox) {
                cocoAnnotationPane.addCocoBBox(bbox);
//                transformOverlays();
            }
        });
        scene.addEventHandler(ImageEvent.SELECT_COCO_SEGMENTATION, e -> {
            SegmentationOverlay seg = (SegmentationOverlay) e.object;
            if (null != seg) {
                cocoAnnotationPane.addSegmentationOverlay(seg);
//                transformOverlays();
            }
        });

        scene.addEventHandler(ImageEvent.SELECT_COCO_IMAGE, e -> {
            Image image = (Image) e.object;
            if (null != image) {
                cocoAnnotationPane.setBaseImage(image);
            }
        });
        //Make everything pretty
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);

        stage.setTitle("COCO Annotation Test");
        stage.setScene(scene);
        stage.show();
    }


    public void loadCocoFile(File file) {
        try {
            CocoAnnotationFile cocoFile = new CocoAnnotationFile(file.getPath(), true);
            cocoObject = cocoFile.cocoObject;
            System.out.println("Total Annotations in object: " + cocoFile.cocoObject.getAnnotations().size());
        } catch (IOException ex) {
            Logger.getLogger(CocoViewerApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
