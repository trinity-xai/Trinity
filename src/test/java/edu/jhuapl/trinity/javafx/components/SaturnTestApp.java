package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.data.SaturnShot;
import edu.jhuapl.trinity.data.files.SaturnFile;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

/**
 * @author Sean Phillips
 */
public class SaturnTestApp extends Application {

    private Color fillColor = Color.SLATEGREY;
    private double canvasSize = 500;
    private VBox controlsBox;
    private ScrollPane canvasScrollPane;
    private Canvas saturnLayerCanvas;
    private GraphicsContext canvasGC;

    TextField dataPointsTextField;
    Rectangle selectionRectangle;
    List<SaturnShot> saturnMeasurements;
    SimpleBooleanProperty hasDataProperty = new SimpleBooleanProperty(false);
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;

    @Override
    public void init() {
        saturnLayerCanvas = new Canvas(canvasSize, canvasSize);
        saturnLayerCanvas.setCursor(Cursor.CROSSHAIR);
        canvasGC = saturnLayerCanvas.getGraphicsContext2D();
        canvasGC.setFill(fillColor);
        canvasGC.fillRect(0, 0, canvasSize, canvasSize);
        
        canvasScrollPane = new ScrollPane(saturnLayerCanvas);
        
        canvasScrollPane.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        canvasScrollPane.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                final File file = db.getFiles().get(0);
                try {
                    if (SaturnFile.isSaturnFile(file)) {
                        System.out.println("Detected Saturn File...");
                        loadSaturnFile(file);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SaturnTestApp.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });

        //Setup selection rectangle and event handling
        selectionRectangle = new Rectangle(1, 1,
            Color.CYAN.deriveColor(1, 1, 1, 0.5));
        selectionRectangle.setManaged(false);
        selectionRectangle.setMouseTransparent(true);
        selectionRectangle.setVisible(false);
        saturnLayerCanvas.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
            saturnLayerCanvas.setWidth(1);
            saturnLayerCanvas.setHeight(1);
            selectionRectangle.setX(mousePosX);
            selectionRectangle.setY(mousePosY);
            saturnLayerCanvas.setVisible(true);
        });
        //Start Tracking mouse movements only when a button is pressed
        saturnLayerCanvas.setOnMouseDragged((MouseEvent me) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            selectionRectangle.setWidth(mousePosX - selectionRectangle.getX());
            selectionRectangle.setHeight(mousePosY - selectionRectangle.getY());
        });
        saturnLayerCanvas.setOnMouseReleased((MouseEvent me) -> {
            //transform from scene to local
            Point2D startLocal = saturnLayerCanvas.sceneToLocal(selectionRectangle.getX(), selectionRectangle.getY());
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
        saturnMeasurements = new ArrayList();
        dataPointsTextField = new TextField();
        dataPointsTextField.setEditable(false);
        dataPointsTextField.setPrefWidth(150);
        Button drawButton = new Button("Draw Measurements");
        drawButton.setPrefWidth(150);
        drawButton.disableProperty().bind(hasDataProperty.not());
        drawButton.setOnAction(e -> {
            drawCurrent();
        });
        Button clearButton = new Button("Clear Measurements");
        clearButton.setPrefWidth(150);
        clearButton.setOnAction(e -> {
            saturnMeasurements.clear();
            dataPointsTextField.clear();
            hasDataProperty.set(false);
        });

        controlsBox = new VBox(10,
            new Label("Current Measurements"),
            dataPointsTextField,
            drawButton,
            clearButton
        );
    }

    @Override
    public void start(Stage stage) {
        BorderPane borderPane = new BorderPane(canvasScrollPane);
        borderPane.setLeft(controlsBox);
        borderPane.getChildren().add(selectionRectangle); // a little hacky but...
        Scene scene = new Scene(borderPane);

        stage.setTitle("Saturn Test");
        stage.setScene(scene);
        stage.show();
    }
    public void drawCurrent() {
        
    }
    public void loadSaturnFile(File file) {
        try {
            SaturnFile saturnFile = new SaturnFile(file.getPath(), Boolean.FALSE);
            saturnMeasurements = saturnFile.parseContent();
            dataPointsTextField.setText(String.valueOf(saturnMeasurements.size()));
            hasDataProperty.set(true);
//            System.out.println("Total Measurements: " + saturnFile.shots.size());
//            System.out.println("Shutter==1 Count: " + 
//                saturnFile.shots.stream().filter(s -> s.isShutter()).count());
            
        } catch (IOException ex) {
            Logger.getLogger(SaturnTestApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

//    public void plotCanvas(int height, int width, Signal2d redChannel, Signal2d greenChannel, Signal2d blueChannel) {
//        PixelWriter pw = inverseFFTGC.getPixelWriter();
//        PixelReader pr = baseImage.getPixelReader();
//        Color originalColor = Color.BLACK;
//        double level = 1;
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                //Neat way to colorize but too dark
//                //color = new Color(
//                //        clamp(0, redChannel.getReAt(y, x), 1),
//                //        clamp(0, greenChannel.getReAt(y, x), 1),
//                //        clamp(0, blueChannel.getReAt(y, x), 1),
//                //        1);//masks[y][x]);
//                originalColor = pr.getColor(x, y);
//                level = clamp(0, (redChannel.getReAt(y, x) + greenChannel.getReAt(y, x)
//                    + blueChannel.getReAt(y, x)) / 1.0, 1);
//                pw.setColor(x, y, originalColor.deriveColor(1, 1, level, 1));
//            }
//        }
//    }

    public static void main(String[] args) {
        launch(args);
    }
}
