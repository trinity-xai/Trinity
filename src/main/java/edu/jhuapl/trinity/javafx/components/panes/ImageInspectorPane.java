/* Copyright (C) 2025 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.panes;

import com.tambapps.fft4j.FastFourier2d;
import com.tambapps.fft4j.Signal2d;
import edu.jhuapl.trinity.javafx.events.ImageEvent;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.logging.Level;

import static edu.jhuapl.trinity.utils.Utils.clamp;


/**
 * @author Sean Phillips
 */
public class ImageInspectorPane extends LitPathPane {
    private static final Logger LOG = LoggerFactory.getLogger(ImageInspectorPane.class);
    public static int PANE_WIDTH = 1600;
    public static int PANE_HEIGHT = 575;
    private double imageSize = 512;
    private Color fillColor = Color.SLATEGREY;
    public BorderPane borderPane;
    private TilePane tilePane;
    private BorderPane imageViewBorderPane;
    BorderPane imageFFTBorderPane;
    public StackPane centerStack;
    public Image baseImage;
    private PixelReader baseImagePR;
    public ImageView baseImageView;
    private Canvas imageFFTCanvas, inverseFFTCanvas;
    private GraphicsContext imageFFTGC, inverseFFTGC;
    Rectangle fftSelectionRectangle;
    ScrollPane scrollPane;
    public Rectangle selectionRectangle;
    private double mousePosX;
    private double mousePosY;
    Signal2d greySignal2d;
    Signal2d redChannelSignal2d;
    Signal2d greenChannelSignal2d;
    Signal2d blueChannelSignal2d;
    double[][] masks;
    Signal2d shiftedRedChannelSignal2d;
    Signal2d shiftedGreenChannelSignal2d;
    Signal2d shiftedBlueChannelSignal2d;
    Spinner<Double> transparencySpinner;
    Spinner<Integer> brightnessDownscalerSpinner;


    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public ImageInspectorPane(Scene scene, Pane parent) {
        super(scene, parent, PANE_WIDTH, PANE_HEIGHT, createContent(),
            "Image Inspector", "", 300.0, 400.0);
        setBackground(Background.EMPTY);
        this.scene = scene;
        baseImage = ResourceUtils.loadIconFile("waitingforimage");
        borderPane = (BorderPane) this.contentPane;
        tilePane = new TilePane();
        tilePane.setPrefColumns(3);
        tilePane.setHgap(10);
        tilePane.setAlignment(Pos.CENTER_LEFT);
        ScrollPane tileScrollPane = new ScrollPane(tilePane);
        tileScrollPane.setPannable(true);
        tileScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tileScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tileScrollPane.setFitToHeight(true);
        tileScrollPane.setFitToWidth(true);

        borderPane.setCenter(tileScrollPane);

        centerStack = new StackPane();
        centerStack.setAlignment(Pos.CENTER);
        imageViewBorderPane = new BorderPane(centerStack);

        baseImageView = new ImageView(baseImage);
        baseImageView.setPreserveRatio(true);
        scrollPane = new ScrollPane(baseImageView);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(512, 512);
        centerStack.getChildren().add(scrollPane);

        Button tessellateSelectionButton = new Button("Tessellate");
        tessellateSelectionButton.setOnAction(e -> {
            //select a subregion
            if (selectionRectangle.getWidth() > 1 && selectionRectangle.getHeight() > 1) {
                Point2D sceneP2D = selectionRectangle.localToScene(
                    selectionRectangle.getX(), selectionRectangle.getY());
                Point2D localP2D = baseImageView.sceneToLocal(sceneP2D);
                WritableImage wi = ResourceUtils.cropImage(baseImage,
                    localP2D.getX(), localP2D.getY(),
                    localP2D.getX() + selectionRectangle.getWidth(),
                    localP2D.getY() + selectionRectangle.getHeight());
                tessellateSelectionButton.getScene().getRoot().fireEvent(
                    new ImageEvent(ImageEvent.NEW_TEXTURE_SURFACE, wi));
            } else { //just use the whole dang thing
                tessellateSelectionButton.getScene().getRoot().fireEvent(
                    new ImageEvent(ImageEvent.NEW_TEXTURE_SURFACE, baseImage));
            }
        });
        Button runFFTButton = new Button("Execute FFT");
        runFFTButton.setOnAction(e -> {
            spaToFreq2D(baseImage);
        });
        HBox powerBottomHBox = new HBox(10, tessellateSelectionButton, runFFTButton);
        powerBottomHBox.setPadding(new Insets(10));
        powerBottomHBox.setAlignment(Pos.CENTER);
        imageViewBorderPane.setBottom(powerBottomHBox);

        //Setup selection rectangle and event handling
        selectionRectangle = new Rectangle(1, 1, Color.CYAN.deriveColor(1, 1, 1, 0.5));
        selectionRectangle.setManaged(false);
        selectionRectangle.setMouseTransparent(true);
        selectionRectangle.setVisible(false);
        imageViewBorderPane.getChildren().add(selectionRectangle); // a little hacky but...

        baseImageView.setOnMouseEntered(e -> baseImageView.setCursor(Cursor.CROSSHAIR));
        baseImageView.setOnMouseExited(e -> baseImageView.setCursor(Cursor.DEFAULT));

        baseImageView.setOnMousePressed((MouseEvent me) -> {
            if (me.isPrimaryButtonDown()) {
                me.consume();
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                selectionRectangle.setWidth(1);
                selectionRectangle.setHeight(1);
                Point2D localP2D = imageViewBorderPane.sceneToLocal(mousePosX, mousePosY);
                selectionRectangle.setX(localP2D.getX());
                selectionRectangle.setY(localP2D.getY());
                selectionRectangle.setVisible(true);
            }
        });
        //Start Tracking mouse movements only when a button is pressed
        baseImageView.setOnMouseDragged((MouseEvent me) -> {
            if (me.isPrimaryButtonDown()) {
                me.consume();
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
                Point2D localP2D = imageViewBorderPane.sceneToLocal(mousePosX, mousePosY);
                selectionRectangle.setWidth(
                    localP2D.getX() - selectionRectangle.getX());
                selectionRectangle.setHeight(
                    localP2D.getY() - selectionRectangle.getY());
            }
        });
        baseImageView.setOnMouseReleased((MouseEvent me) -> {
            if (me.isPrimaryButtonDown()) {
                me.consume();
            }
        });

        imageFFTCanvas = new Canvas(imageSize, imageSize);
        imageFFTGC = imageFFTCanvas.getGraphicsContext2D();
        imageFFTGC.setFill(fillColor);
        imageFFTGC.fillRect(0, 0, imageSize, imageSize);
        imageFFTCanvas.setCursor(Cursor.CROSSHAIR);
        //Setup selection rectangle and event handling
        fftSelectionRectangle = new Rectangle(1, 1,
            Color.CYAN.deriveColor(1, 1, 1, 0.5));
        fftSelectionRectangle.setManaged(false);
        fftSelectionRectangle.setMouseTransparent(true);
        fftSelectionRectangle.setVisible(false);
        imageFFTCanvas.setOnMousePressed((MouseEvent me) -> {
            if (me.isPrimaryButtonDown()) {
                me.consume();
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
//                Point2D localP2D = imageFFTCanvas.sceneToLocal(mousePosX, mousePosY);
                Point2D localP2D = imageFFTBorderPane.sceneToLocal(mousePosX, mousePosY);
                fftSelectionRectangle.setWidth(1);
                fftSelectionRectangle.setHeight(1);
                fftSelectionRectangle.setX(localP2D.getX());
                fftSelectionRectangle.setY(localP2D.getY());
                fftSelectionRectangle.setVisible(true);
            }
        });
        //Start Tracking mouse movements only when a button is pressed
        imageFFTCanvas.setOnMouseDragged((MouseEvent me) -> {
            if (me.isPrimaryButtonDown()) {
                me.consume();
                mousePosX = me.getSceneX();
                mousePosY = me.getSceneY();
//                Point2D localP2D = imageFFTCanvas.sceneToLocal(mousePosX, mousePosY);
                Point2D localP2D = imageFFTBorderPane.sceneToLocal(mousePosX, mousePosY);
                fftSelectionRectangle.setWidth(localP2D.getX() - fftSelectionRectangle.getX());
                fftSelectionRectangle.setHeight(localP2D.getY() - fftSelectionRectangle.getY());
            }
        });
        imageFFTCanvas.setOnMouseReleased((MouseEvent me) -> {
            me.consume();
            //update masks
            Point2D sceneStart = fftSelectionRectangle.localToScene(fftSelectionRectangle.getX(), fftSelectionRectangle.getY());
            Point2D localStart = imageFFTCanvas.sceneToLocal(sceneStart);
            Point2D sceneEnd = fftSelectionRectangle.localToScene(
                fftSelectionRectangle.getX() + fftSelectionRectangle.getHeight(),
                fftSelectionRectangle.getY() + fftSelectionRectangle.getHeight());
            Point2D localEnd = imageFFTCanvas.sceneToLocal(sceneEnd);

            int startY = Double.valueOf(localStart.getY()).intValue();
            int endY = Double.valueOf(localEnd.getY()).intValue();
            int startX = Double.valueOf(localStart.getX()).intValue();
            int endX = Double.valueOf(localEnd.getX()).intValue();

            PixelWriter pw = imageFFTGC.getPixelWriter();
            for (int y = startY; y < endY; y++) {
                for (int x = startX; x < endX; x++) {
                    masks[y][x] = 0.0;
                    pw.setColor(x, y, Color.BLACK.deriveColor(1, 1, 1, 0.5));
                }
            }

            //copy the current shifted FFT image
            //shiftedGreySignal2d = fftShift2d_P(greySignal2d, false);
            Signal2d redShiftCopy = new Signal2d(shiftedRedChannelSignal2d.getM(), shiftedRedChannelSignal2d.getN());
            shiftedRedChannelSignal2d.copyInto(redShiftCopy);
            Signal2d greenShiftCopy = new Signal2d(shiftedGreenChannelSignal2d.getM(), shiftedGreenChannelSignal2d.getN());
            shiftedRedChannelSignal2d.copyInto(greenShiftCopy);
            Signal2d blueShiftCopy = new Signal2d(shiftedBlueChannelSignal2d.getM(), shiftedBlueChannelSignal2d.getN());
            shiftedRedChannelSignal2d.copyInto(blueShiftCopy);
            //apply masks,
            for (int y = startY; y < endY; y++) {
                for (int x = startX; x < endX; x++) {
                    redShiftCopy.setAt(y, x, masks[y][x], masks[y][x]);
                    greenShiftCopy.setAt(y, x, masks[y][x], masks[y][x]);
                    blueShiftCopy.setAt(y, x, masks[y][x], masks[y][x]);
                }
            }
            //invert fftshift
            Signal2d inverseShiftedRedChannelSignal2d = fftShift2d(redShiftCopy, true);
            Signal2d inverseShiftedGreenChannelSignal2d = fftShift2d(greenShiftCopy, true);
            Signal2d inverseShiftedBlueChannelSignal2d = fftShift2d(blueShiftCopy, true);
            //do inverse fft
            int height = Double.valueOf(baseImage.getHeight()).intValue();
            int width = Double.valueOf(baseImage.getWidth()).intValue();
            FastFourier2d transformer2D = new FastFourier2d();
            transformer2D.inverse(inverseShiftedRedChannelSignal2d);
            transformer2D.inverse(inverseShiftedGreenChannelSignal2d);
            transformer2D.inverse(inverseShiftedBlueChannelSignal2d);
            transformer2D.shutdown();
            plotInverseFFT(height, width,
                inverseShiftedRedChannelSignal2d,
                inverseShiftedGreenChannelSignal2d,
                inverseShiftedBlueChannelSignal2d
            );

            fftSelectionRectangle.setVisible(false);
            fftSelectionRectangle.setWidth(1);
            fftSelectionRectangle.setHeight(1);
        });

        brightnessDownscalerSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 10, 1));
        brightnessDownscalerSpinner.setPrefWidth(100);
        transparencySpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.666, 0.1));
        transparencySpinner.setPrefWidth(100);
        Button clearMaskButton = new Button("Clear Mask");
        clearMaskButton.setOnAction(e -> {
            clearMask();
        });
        Button tessellateFFTButton = new Button("Tessellate FFT");
        tessellateFFTButton.setOnAction(e -> {
            SnapshotParameters snapshotParameters = new SnapshotParameters();
            snapshotParameters.setFill(Color.TRANSPARENT);
            Image image = imageFFTCanvas.snapshot(snapshotParameters, null);
            tessellateFFTButton.getScene().getRoot().fireEvent(
                new ImageEvent(ImageEvent.NEW_TEXTURE_SURFACE, image));
        });

        HBox controlsBox = new HBox(10,
            clearMaskButton,
            new VBox(5, new Label("Brightness Downscale"), brightnessDownscalerSpinner),
            new VBox(5, new Label("Alpha Channel"), transparencySpinner),
            tessellateFFTButton
        );
        controlsBox.setAlignment(Pos.CENTER);

        inverseFFTCanvas = new Canvas(imageSize, imageSize);
        inverseFFTGC = inverseFFTCanvas.getGraphicsContext2D();
        inverseFFTGC.setFill(fillColor);
        inverseFFTGC.fillRect(0, 0, imageSize, imageSize);

        ScrollPane imageFFTScrollPane = new ScrollPane(imageFFTCanvas);
        imageFFTScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        imageFFTScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        imageFFTScrollPane.setPannable(true);
        imageFFTScrollPane.setFitToHeight(true);
        imageFFTScrollPane.setFitToWidth(true);
        imageFFTScrollPane.setPrefSize(512, 512);
        imageFFTBorderPane = new BorderPane(imageFFTScrollPane);
        imageFFTBorderPane.setBottom(controlsBox);
        imageFFTBorderPane.getChildren().add(fftSelectionRectangle); // a little hacky but...

        ScrollPane inverseScrollPane = new ScrollPane(inverseFFTCanvas);
        inverseScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        inverseScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        inverseScrollPane.setPannable(true);
        inverseScrollPane.setFitToHeight(true);
        inverseScrollPane.setFitToWidth(true);
        inverseScrollPane.setPrefSize(512, 512);
        BorderPane inverseFFTBorderPane = new BorderPane(inverseScrollPane);
        Button tessellateInverseFFTButton = new Button("Tessellate Inverse");
        tessellateInverseFFTButton.setOnAction(e -> {
            SnapshotParameters snapshotParameters = new SnapshotParameters();
            snapshotParameters.setFill(Color.TRANSPARENT);
            Image image = inverseFFTCanvas.snapshot(snapshotParameters, null);
            tessellateInverseFFTButton.getScene().getRoot().fireEvent(
                new ImageEvent(ImageEvent.NEW_TEXTURE_SURFACE, image));
        });
        HBox inversePowerBottomHBox = new HBox(10, tessellateInverseFFTButton);
        inversePowerBottomHBox.setPadding(new Insets(10));
        inversePowerBottomHBox.setAlignment(Pos.CENTER);
        inverseFFTBorderPane.setBottom(inversePowerBottomHBox);

        tilePane.getChildren().addAll(
            imageViewBorderPane, imageFFTBorderPane, inverseFFTBorderPane);

        borderPane.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        borderPane.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            event.consume();
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                final File file = db.getFiles().get(0);
                if (JavaFX3DUtils.isTextureFile(file)) {
                    try {
                        setImage(new Image(file.toURI().toURL().toExternalForm()));
                    } catch (MalformedURLException ex) {
                        LOG.error(ex.getMessage());
                    }
                }
            }
        });
        scene.getRoot().addEventHandler(ImageEvent.NEW_IMAGE_INSPECTION, event -> {
            try {
                Image image = (Image)event.object;
                setImage(image);
            } catch (Exception ex) {
                LOG.error("dude...");
            }
        });
    }

    private void clearMask() {
        Arrays.stream(masks).forEach(row -> Arrays.fill(row, 1.0));
    }

    private Signal2d fftShift2d(Signal2d inputSignal2d, boolean inverse) {

        int totalColumns = inputSignal2d.getN();
        int totalRows = inputSignal2d.getM();
        int middleColumn = totalColumns / 2;
        int columnRange = totalColumns - middleColumn;
        int middleRow = totalRows / 2;
        int rowRange = totalRows - middleRow;

        if (inverse) {
            middleColumn = totalColumns - middleColumn;
            columnRange = totalColumns / 2;
            middleRow = totalRows - middleRow;
            rowRange = totalRows / 2;
        }

        Signal2d shiftedSignal2d = new Signal2d(inputSignal2d.getM(), inputSignal2d.getN());
        // down right corner move to up left corner
        for (int y = 0; y < rowRange; y++) {
            for (int x = 0; x < columnRange; x++) {
                shiftedSignal2d.setAt(x, y,
                    inputSignal2d.getReAt(x + middleColumn, y + middleRow),
                    inputSignal2d.getImAt(x + middleColumn, y + middleRow)
                );
            }
        }

        // down left corner move to up right corner
        for (int y = 0; y < rowRange; y++) {
            for (int x = columnRange; x < totalColumns; x++) {
                shiftedSignal2d.setAt(x, y,
                    inputSignal2d.getReAt(x - columnRange, y + middleRow),
                    inputSignal2d.getImAt(x - columnRange, y + middleRow)
                );
            }
        }

        // up right corner move to down left corner
        for (int y = rowRange; y < totalRows; y++) {
            for (int x = 0; x < columnRange; x++) {
                shiftedSignal2d.setAt(x, y,
                    inputSignal2d.getReAt(x + middleColumn, y - rowRange),
                    inputSignal2d.getImAt(x + middleColumn, y - rowRange)
                );
            }
        }

        // up left corner move to down right corner
        for (int y = rowRange; y < totalRows; y++) {
            for (int x = columnRange; x < totalColumns; x++) {
                shiftedSignal2d.setAt(x, y,
                    inputSignal2d.getReAt(x - columnRange, y - rowRange),
                    inputSignal2d.getImAt(x - columnRange, y - rowRange)
                );
            }
        }
        return shiftedSignal2d;
    }

    public void extractSignals(Image image) {
        int height = Double.valueOf(image.getHeight()).intValue();
        int width = Double.valueOf(image.getWidth()).intValue();

//        greySignal2d = new Signal2d(height, width);
        redChannelSignal2d = new Signal2d(height, width);
        greenChannelSignal2d = new Signal2d(height, width);
        blueChannelSignal2d = new Signal2d(height, width);
        //do the red channel
        PixelReader pixelReader = image.getPixelReader();
        Color color = Color.BLACK;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                color = pixelReader.getColor(x, y);
//                greySignal2d.setReAt(y, x, color.grayscale().getBrightness());
                redChannelSignal2d.setReAt(y, x, color.getRed());
                greenChannelSignal2d.setReAt(y, x, color.getGreen());
                blueChannelSignal2d.setReAt(y, x, color.getBlue());
            }
        }
    }

    public void spaToFreq2D(Image image) {
        extractSignals(image);
        System.out.println("Signal2D values extracted.");

        FastFourier2d transformer2D = new FastFourier2d();
//        System.out.print("FFT on Greyscale... ");
        long startTime = System.nanoTime();
//        transformer2D.transform(greySignal2d);
//        Utils.printTotalTime(startTime);
        System.out.print("FFT on Red Channel... ");
        transformer2D.transform(redChannelSignal2d);
        System.out.print("FFT on Green Channel... ");
        transformer2D.transform(greenChannelSignal2d);
        System.out.print("FFT on Blue Channel... ");
        transformer2D.transform(blueChannelSignal2d);

        //fshift = np.fft.fftshift(f_image)
//        System.out.print("FFTShift on Greyscale... ");
//        startTime = System.nanoTime();
//        shiftedGreySignal2d = fftShift2d(greySignal2d, false);
//        Utils.printTotalTime(startTime);
        System.out.print("FFT Shift on Red Channel... ");
        shiftedRedChannelSignal2d = fftShift2d(redChannelSignal2d, false);
        System.out.print("FFT Shift on Green Channel... ");
        shiftedGreenChannelSignal2d = fftShift2d(greenChannelSignal2d, false);
        System.out.print("FFT Shift on Blue Channel... ");
        shiftedBlueChannelSignal2d = fftShift2d(blueChannelSignal2d, false);

        //magnitude_spectrum = 20 * np.log(np.abs(fshift))
        System.out.print("Plotting FFT Canvas... ");
        double redlogVal, greenlogVal, bluelogVal = 0;
        baseImagePR = image.getPixelReader();
        PixelWriter pw = imageFFTGC.getPixelWriter();
        imageFFTGC.setFill(Color.BLACK);
        imageFFTGC.fillRect(0, 0, imageFFTCanvas.getWidth(), imageFFTCanvas.getHeight());
        int height = Double.valueOf(image.getHeight()).intValue();
        int width = Double.valueOf(image.getWidth()).intValue();
        double opacity = transparencySpinner.getValue();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                redlogVal = Math.log10(Math.abs(shiftedRedChannelSignal2d.getReAt(y, x)) / brightnessDownscalerSpinner.getValue());
                greenlogVal = Math.log10(Math.abs(shiftedGreenChannelSignal2d.getReAt(y, x)) / brightnessDownscalerSpinner.getValue());
                bluelogVal = Math.log10(Math.abs(shiftedBlueChannelSignal2d.getReAt(y, x)) / brightnessDownscalerSpinner.getValue());

                pw.setColor(x, y, Color.color(
                    Utils.clamp(0, redlogVal, 1),
                    Utils.clamp(0, greenlogVal, 1),
                    Utils.clamp(0, bluelogVal, 1),
                    opacity)
                );
                //This is good and correct for grayscale
                //                pw.setColor(x, y, Color.gray(Utils.clamp(0,
                //                    Math.log10(Math.abs(shiftedGreySignal2d.getReAt(y, x))) / polarScaleSpinner.getValue(),
                //                    1)));
            }
        }
        System.out.print("Inverse on Red Channel... ");
        transformer2D.inverse(redChannelSignal2d);
        System.out.print("Inverse on Red Channel... ");
        transformer2D.inverse(greenChannelSignal2d);
        System.out.print("Inverse on Red Channel... ");
        transformer2D.inverse(blueChannelSignal2d);

        System.out.print("Plotting Inverse FFT... ");
        plotInverseFFT(height, width, redChannelSignal2d, greenChannelSignal2d, blueChannelSignal2d);
        Utils.printTotalTime(startTime);

        // don't forget to shut it down as it uses an executor service
        transformer2D.shutdown();
    }

    public void plotInverseFFT(int height, int width, Signal2d redChannel, Signal2d greenChannel, Signal2d blueChannel) {
        PixelWriter pw = inverseFFTGC.getPixelWriter();
        PixelReader pr = baseImage.getPixelReader();
        Color originalColor = Color.BLACK;
        double level = 1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                //Neat way to colorize but too dark
                //color = new Color(
                //        clamp(0, redChannel.getReAt(y, x), 1),
                //        clamp(0, greenChannel.getReAt(y, x), 1),
                //        clamp(0, blueChannel.getReAt(y, x), 1),
                //        1);//masks[y][x]);
                originalColor = pr.getColor(x, y);
                level = clamp(0, (redChannel.getReAt(y, x) + greenChannel.getReAt(y, x)
                    + blueChannel.getReAt(y, x)) / 1.0, 1);
                pw.setColor(x, y, originalColor.deriveColor(1, 1, level, 1));
            }
        }
    }

    public void setImage(Image image) {
        this.baseImage = image;
        baseImageView.setImage(this.baseImage);
        scrollPane.setHvalue(0);
        scrollPane.setVvalue(0);
        baseImagePR = baseImage.getPixelReader();
        int height = Double.valueOf(baseImage.getHeight()).intValue();
        int width = Double.valueOf(baseImage.getWidth()).intValue();
        masks = new double[height][width];
        clearMask();
        imageFFTCanvas.setWidth(baseImage.getWidth());
        imageFFTCanvas.setHeight(baseImage.getHeight());
        inverseFFTCanvas.setWidth(baseImage.getWidth());
        inverseFFTCanvas.setHeight(baseImage.getHeight());
        imageSize = width;
    }
}
