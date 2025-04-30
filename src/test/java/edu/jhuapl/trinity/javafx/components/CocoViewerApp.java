package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.css.StyleResourceProvider;
import edu.jhuapl.trinity.data.coco.CocoObject;
import edu.jhuapl.trinity.data.files.CocoAnnotationFile;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
/**
 * @author Sean Phillips
 */
public class CocoViewerApp extends Application {

    private Color fillColor = Color.ALICEBLUE.deriveColor(1, 1, 1, 0.1);
    private double canvasSize = 500;
    private Image baseImage;
    private ImageView baseImageView;
    private Canvas heatMapCanvas;
    private Pane annotationPane;
    private GraphicsContext canvasGC;

    CocoObject cocoObject;
    Rectangle selectionRectangle;
    CocoAnnotationControlBox controls;
    //make transparent so it doesn't interfere with subnode transparency effects
    Background transBack = new Background(new BackgroundFill(
        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
    
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;

    @Override
    public void init() {
        heatMapCanvas = new Canvas(canvasSize, canvasSize);
        heatMapCanvas.setCursor(Cursor.CROSSHAIR);
        canvasGC = heatMapCanvas.getGraphicsContext2D();
        canvasGC.setFill(fillColor);
        canvasGC.fillRect(0, 0, canvasSize, canvasSize);
        
        try {
            baseImage = ResourceUtils.load3DTextureImage("carl-b-portrait");
        } catch (IOException ex) {
            Logger.getLogger(CocoViewerApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        baseImageView = new ImageView(baseImage);
        baseImageView.setPreserveRatio(true);
        baseImageView.setSmooth(true);
        
        annotationPane = new Pane();
        //Setup selection rectangle and event handling
        selectionRectangle = new Rectangle(1, 1,
            Color.CYAN.deriveColor(1, 1, 1, 0.5));
        selectionRectangle.setManaged(false);
        selectionRectangle.setMouseTransparent(true);
        selectionRectangle.setVisible(false);
        heatMapCanvas.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
            selectionRectangle.setWidth(1);
            selectionRectangle.setHeight(1);
            selectionRectangle.setX(mousePosX);
            selectionRectangle.setY(mousePosY);
            selectionRectangle.setVisible(true);
        });
        //Start Tracking mouse movements only when a button is pressed
        heatMapCanvas.setOnMouseDragged((MouseEvent me) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            selectionRectangle.setWidth(mousePosX - selectionRectangle.getX());
            selectionRectangle.setHeight(mousePosY - selectionRectangle.getY());
        });
        heatMapCanvas.setOnMouseReleased((MouseEvent me) -> {
            //transform from scene to local
            Point2D startLocal = heatMapCanvas.sceneToLocal(selectionRectangle.getX(), selectionRectangle.getY());
            Point2D endLocal = startLocal.add(selectionRectangle.getWidth(), selectionRectangle.getHeight());
            //update masks
            int startY = Double.valueOf(startLocal.getY()).intValue();
            int endY = Double.valueOf(endLocal.getY()).intValue();
            int startX = Double.valueOf(startLocal.getX()).intValue();
            int endX = Double.valueOf(endLocal.getX()).intValue();

            selectionRectangle.setVisible(false);
            selectionRectangle.setWidth(1);
            selectionRectangle.setHeight(1);

        });
        controls = new CocoAnnotationControlBox();
    }

    @Override
    public void start(Stage stage) {
        StackPane centerStackPane = new StackPane(
            baseImageView, 
            heatMapCanvas, 
            annotationPane);
        centerStackPane.setBackground(transBack);
        centerStackPane.setMinSize(canvasSize, canvasSize);

        baseImageView.fitWidthProperty().bind(centerStackPane.widthProperty());
        centerStackPane.widthProperty().addListener(cl-> {
            heatMapCanvas.setWidth(centerStackPane.getWidth());
            annotationPane.setMinWidth(centerStackPane.getWidth());
            annotationPane.setPrefWidth(centerStackPane.getWidth());
            annotationPane.setMaxWidth(centerStackPane.getWidth());
        });
        centerStackPane.heightProperty().addListener(cl-> {
            heatMapCanvas.setHeight(centerStackPane.getHeight());
            annotationPane.setMinHeight(centerStackPane.getHeight());
            annotationPane.setPrefHeight(centerStackPane.getHeight());
            annotationPane.setMaxHeight(centerStackPane.getHeight());
        });

        BorderPane borderPane = new BorderPane(centerStackPane);
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
                        populateControls();
                    }
                } catch (Exception ex) {
                    Logger.getLogger(CocoViewerApp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        ScrollPane sp = new ScrollPane(controls);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setPannable(true);
        
        borderPane.setLeft(sp);
        borderPane.getChildren().add(selectionRectangle); // a little hacky but...
        borderPane.getStyleClass().add("trinity-pane");
        
        Scene scene = new Scene(borderPane, Color.BLACK);

        //Make everything pretty
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);

        stage.setTitle("COCO Annotation Test");
        stage.setScene(scene);
        stage.show();
    }
    public void populateControls() {
        
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
