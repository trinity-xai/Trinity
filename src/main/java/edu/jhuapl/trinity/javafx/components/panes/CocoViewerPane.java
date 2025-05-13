package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.data.coco.CocoObject;
import edu.jhuapl.trinity.data.files.CocoAnnotationFile;
import edu.jhuapl.trinity.javafx.components.annotations.BBoxOverlay;
import edu.jhuapl.trinity.javafx.components.annotations.CocoAnnotationControlBox;
import edu.jhuapl.trinity.javafx.components.annotations.CocoAnnotationPane;
import edu.jhuapl.trinity.javafx.components.annotations.SegmentationOverlay;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

/**
 * @author Sean Phillips
 */
public class CocoViewerPane extends LitPathPane {
    private static final Logger LOG = LoggerFactory.getLogger(CocoViewerPane.class);
    public static int PANE_WIDTH = 600;
    public static int PANE_HEIGHT = 600;
    public BorderPane borderPane;
    public StackPane centerStack;
    ScrollPane scrollPane;
    CocoAnnotationPane cocoAnnotationPane;
    TextField basePathTextField;
    CocoObject cocoObject;
    CocoAnnotationControlBox controls;
    //make transparent so it doesn't interfere with subnode transparency effects
    Background transBack = new Background(new BackgroundFill(
        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));

    
    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public CocoViewerPane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, PANE_HEIGHT, createContent(),
            "COCO Viewer", "", 300.0, 400.0);
        setBackground(Background.EMPTY);
        this.scene = scene;
        borderPane = (BorderPane) this.contentPane;
        centerStack = new StackPane();
        centerStack.setAlignment(Pos.CENTER);
        borderPane.setCenter(centerStack);
        
        cocoAnnotationPane = new CocoAnnotationPane();
        controls = new CocoAnnotationControlBox();
        scrollPane = new ScrollPane(controls);
        // hide scrollpane scrollbars
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPadding(Insets.EMPTY); 
        scrollPane.setPannable(true);
        
        controls.heightProperty().addListener(cl-> {
            scrollPane.setVvalue(scrollPane.getVmax());
        });        
        borderPane.setLeft(scrollPane);
        basePathTextField = new TextField("");
        controls.imageBasePathProperty.bind(basePathTextField.textProperty());        
        Button browseButton = new Button("Browse");
        browseButton.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            File f = new File(basePathTextField.getText());
            if(f.isDirectory())
                dc.setInitialDirectory(f);
            dc.setTitle("Browse to imagery base path...");
            File dir = dc.showDialog(null);
            if(null != dir && dir.isDirectory()) {
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

        scrollPane = new ScrollPane(cocoAnnotationPane);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(512, 512);
        centerStack.getChildren().add(scrollPane);

        scene.addEventHandler(ImageEvent.CLEAR_COCO_ANNOTATIONS, e-> {
            cocoAnnotationPane.clearAnnotations();
        });
        scene.addEventHandler(ImageEvent.SELECT_COCO_BBOX, e-> {
            BBoxOverlay bbox = (BBoxOverlay) e.object;
            if(null != bbox) {
                cocoAnnotationPane.addCocoBBox(bbox);
//                transformOverlays();
            }
        });
        scene.addEventHandler(ImageEvent.SELECT_COCO_SEGMENTATION, e-> {
            SegmentationOverlay seg = (SegmentationOverlay) e.object;
            if(null != seg) {
                cocoAnnotationPane.addSegmentationOverlay(seg);
//                transformOverlays();
            }
        });
        
        scene.addEventHandler(ImageEvent.SELECT_COCO_IMAGE, e-> {
            Image image = (Image) e.object;
            if(null != image) {
                cocoAnnotationPane.setBaseImage(image);
            }
        });
        
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
                        //System.out.println("Detected CocoAnnotation File...");
                        loadCocoFile(file);
                    }
                } catch (Exception ex) {
                    LOG.error("Could not load COCO File: " + file.getPath());
                }
            }
        });   
    }
    public void loadCocoObject(CocoObject coco) {
        cocoObject = coco;
        //System.out.println("Total Annotations in object: " + cocoFile.cocoObject.getAnnotations().size());
        controls.populateControls(cocoObject);
    }
    public void loadCocoFile(File file) {
        try {
            CocoAnnotationFile cocoFile = new CocoAnnotationFile(file.getPath(), true);
            loadCocoObject(cocoFile.cocoObject);
        } catch (IOException ex) {
            LOG.error("Could not load COCO File: " + file.getPath());
        }
    }
}
