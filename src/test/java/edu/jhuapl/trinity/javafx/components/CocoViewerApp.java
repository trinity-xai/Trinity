package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.data.files.CocoAnnotationFile;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
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
    private double canvasSize = 1000;
    private VBox controlsBox;
    private Image baseImage;
    private ImageView baseImageView;
    private Canvas heatMapCanvas;
    private Pane annotationPane;
    private GraphicsContext canvasGC;

    Rectangle selectionRectangle;
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
        Button clearButton = new Button("Clear All");
        clearButton.setPrefWidth(150);
        clearButton.setOnAction(e -> {

            Platform.runLater(()->{
                canvasGC.setFill(Color.ALICEBLUE);
                canvasGC.fillRect(0, 0, heatMapCanvas.getWidth(), heatMapCanvas.getHeight());         
            });                
        });

        controlsBox = new VBox(10,
            clearButton
        );
    }

    @Override
    public void start(Stage stage) {
        StackPane centerStackPane = new StackPane(
            baseImageView, 
            heatMapCanvas, 
            annotationPane);
        centerStackPane.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
        centerStackPane.setMinSize(canvasSize, canvasSize);
        
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
//                    if (SaturnFile.isSaturnFile(file)) {
//                        System.out.println("Detected Saturn File...");
//                        loadSaturnFile(file);
                        long startTime = System.nanoTime();

                        System.out.println("Saturn Measurements conversion took: " + Utils.totalTimeString(startTime));
//                    }
                } catch (Exception ex) {
                    Logger.getLogger(CocoViewerApp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        
        borderPane.setLeft(controlsBox);
        borderPane.getChildren().add(selectionRectangle); // a little hacky but...
        Scene scene = new Scene(borderPane);

        stage.setTitle("Saturn Test");
        stage.setScene(scene);
        stage.show();
    }
    public void drawCurrent() {
        Task task = new Task() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(()->{
                    canvasGC.setFill(Color.ALICEBLUE);
                    canvasGC.fillRect(0, 0, heatMapCanvas.getWidth(), heatMapCanvas.getHeight());         
                });
                System.out.println("Starting render...");
                long startTime = System.nanoTime();
                Utils.printTotalTime(startTime);
                System.out.println("Finished Rendering.");
                return null;
            }
        };
        Thread thread = new Thread(task);
//        task.setOnSucceeded(e->renderingProperty.set(false));
//        task.setOnFailed(e->renderingProperty.set(false));
//        task.setOnCancelled(e->renderingProperty.set(false));
//        renderingProperty.set(true);
        thread.start();
    }
    public void loadCocoFile(File file) {
        try {
            CocoAnnotationFile cocoFile = new CocoAnnotationFile(file.getPath(), true);

            System.out.println("Total Measurements: " + cocoFile.cocoObject.getAnnotations().size());
//            System.out.println("Shutter==1 Count: " + 
//                saturnFile.shots.stream().filter(s -> s.isShutter()).count());
        } catch (IOException ex) {
            Logger.getLogger(CocoViewerApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
