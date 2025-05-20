package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.data.SaturnShot;
import edu.jhuapl.trinity.data.files.SaturnFile;
import edu.jhuapl.trinity.data.messages.xai.FeatureCollection;
import edu.jhuapl.trinity.utils.DataUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 */
public class SaturnTestApp extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(SaturnTestApp.class);

    private Color fillColor = Color.ALICEBLUE;
    private double canvasSize = 1000;
    private VBox controlsBox;
    private Canvas saturnLayerCanvas;
    private GraphicsContext canvasGC;

    TextField dataPointsTextField;
    Rectangle selectionRectangle;
    List<SaturnShot> saturnMeasurements;
    SimpleBooleanProperty hasDataProperty = new SimpleBooleanProperty(false);
    SimpleBooleanProperty renderingProperty = new SimpleBooleanProperty(false);
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
            selectionRectangle.setWidth(1);
            selectionRectangle.setHeight(1);
            selectionRectangle.setX(mousePosX);
            selectionRectangle.setY(mousePosY);
            selectionRectangle.setVisible(true);
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
        drawButton.disableProperty().bind(hasDataProperty.not().and(renderingProperty));
        drawButton.setOnAction(e -> {
            drawCurrent();
        });
        Button clearButton = new Button("Clear Measurements");
        clearButton.setPrefWidth(150);
        clearButton.setOnAction(e -> {
            saturnMeasurements.clear();
            dataPointsTextField.clear();
            hasDataProperty.set(false);
            renderingProperty.set(false);
            Platform.runLater(() -> {
                canvasGC.setFill(Color.ALICEBLUE);
                canvasGC.fillRect(0, 0, saturnLayerCanvas.getWidth(), saturnLayerCanvas.getHeight());
            });
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
        Pane centerPane = new Pane(saturnLayerCanvas);
        centerPane.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
        centerPane.setMinSize(canvasSize, canvasSize);

        centerPane.widthProperty().addListener(cl -> {
            saturnLayerCanvas.setWidth(centerPane.getWidth());
        });
        centerPane.heightProperty().addListener(cl -> {
            saturnLayerCanvas.setHeight(centerPane.getHeight());
        });

        BorderPane borderPane = new BorderPane(centerPane);

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
                    if (SaturnFile.isSaturnFile(file)) {
                        LOG.info("Detected Saturn File...");
                        loadSaturnFile(file);
                        long startTime = System.nanoTime();
                        FeatureCollection fc = DataUtils.convertSaturnMeasurements(saturnMeasurements);
                        LOG.info("Saturn Measurements conversion took: " + Utils.totalTimeString(startTime));
                    }
                } catch (IOException ex) {
                    LOG.error("Error while processing dropped Saturn file: " + ex.getMessage());
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
                Platform.runLater(() -> {
                    canvasGC.setFill(Color.ALICEBLUE);
                    canvasGC.fillRect(0, 0, saturnLayerCanvas.getWidth(), saturnLayerCanvas.getHeight());
                });
                //find some numbers
                DoubleSummaryStatistics x_mmStats = saturnMeasurements.stream()
                    .mapToDouble(SaturnShot::getX_mm)
                    .summaryStatistics();
                //@DEBUG SMP
                //System.out.println("X_MM Stats: " + x_mmStats.toString());

                DoubleSummaryStatistics y_mmStats = saturnMeasurements.stream()
                    .mapToDouble(SaturnShot::getY_mm)
                    .summaryStatistics();
                //@DEBUG SMP
                //System.out.println("Y_MM Stats: " + y_mmStats.toString());

                DoubleSummaryStatistics powerStats = saturnMeasurements.stream()
                    .mapToDouble(SaturnShot::getPower)
                    .summaryStatistics();
                //@DEBUG SMP
                //System.out.println("Power Stats: " + powerStats.toString());

                DoubleSummaryStatistics pd0Stats = saturnMeasurements.stream()
                    .mapToDouble(SaturnShot::getPd_0)
                    .summaryStatistics();
                //@DEBUG SMP
                //System.out.println("pd0Stats: " + pd0Stats.toString());

                LOG.info("normalizing values...");
                long startTime = System.nanoTime();
                saturnMeasurements.parallelStream().forEach(s -> {
                    s.normalizedValue = DataUtils.normalize(s.getPd_0(), pd0Stats.getMin(), pd0Stats.getMax());
                    s.code = Color.color(s.normalizedValue, 0, 0, 0.666).toString();
                    s.normalizedX = saturnLayerCanvas.getWidth() * DataUtils.normalize(s.getX_mm(), x_mmStats.getMin(), x_mmStats.getMax());
                    s.normalizedY = saturnLayerCanvas.getHeight() * DataUtils.normalize(s.getY_mm(), y_mmStats.getMin(), y_mmStats.getMax());
                });
                Utils.logTotalTime(startTime);

                int chunkSize = 20000;
                int numberOfChunks = saturnMeasurements.size() / chunkSize;
                LOG.info("Starting Canvas render...");
                startTime = System.nanoTime();
                for (int i = 0; i < numberOfChunks; i++) {
                    if (!renderingProperty.get()) {
                        LOG.info("Rendering halted.");
                        return null;
                    }
                    int start = i * chunkSize;
                    Platform.runLater(() -> {
                        saturnMeasurements.subList(start, start + chunkSize).forEach(shot -> {
                            canvasGC.setFill(Color.valueOf(shot.code));
                            canvasGC.fillOval(shot.normalizedX, shot.normalizedY, 0.5, 0.5);
                        });
                    });
                    try {
                        Thread.sleep(17);
                    } catch (InterruptedException ex) {
                        LOG.error("Rendering loop sleep interrupted: " + ex.getMessage());
                    }
                }
                Utils.logTotalTime(startTime);
                LOG.info("Finished Rendering.");
                return null;
            }
        };
        Thread thread = new Thread(task);
        task.setOnSucceeded(e -> renderingProperty.set(false));
        task.setOnFailed(e -> renderingProperty.set(false));
        task.setOnCancelled(e -> renderingProperty.set(false));
        renderingProperty.set(true);
        thread.start();
    }

    public void loadSaturnFile(File file) {
        try {
            SaturnFile saturnFile = new SaturnFile(file.getPath(), Boolean.FALSE);
            saturnMeasurements = saturnFile.parseContent();
            dataPointsTextField.setText(String.valueOf(saturnMeasurements.size()));
            hasDataProperty.set(true);
            //@DEBUG SMP
//            System.out.println("Total Measurements: " + saturnFile.shots.size());
//            System.out.println("Shutter==1 Count: " +
//                saturnFile.shots.stream().filter(s -> s.isShutter()).count());
        } catch (IOException ex) {
            LOG.error("Error while trying to load Saturn file: " +  ex);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
