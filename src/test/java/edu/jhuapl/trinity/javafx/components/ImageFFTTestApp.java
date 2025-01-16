/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */
package edu.jhuapl.trinity.javafx.components;

/*
 * Copyright (c) 2020 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.tambapps.fft4j.FastFourier2d;
import com.tambapps.fft4j.Signal2d;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import static edu.jhuapl.trinity.utils.Utils.clamp;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ImageFFTTestApp extends Application {

    private Color fillColor = Color.SLATEGREY;
    private double imageSize = 512;
    private VBox controlsBox;
    private GridPane plotPane;
    private Image baseImage;
    private PixelReader baseImagePR;
    private ImageView baseImageView;
    private Canvas imageFFTCanvas, inverseFFTCanvas, polarCanvas;
    private GraphicsContext imageFFTGC, inverseFFTGC, polarGC;

    Signal2d greySignal2d;
    Signal2d redChannelSignal2d;
    Signal2d greenChannelSignal2d;
    Signal2d blueChannelSignal2d;
    Signal2d shiftedGreySignal2d;
    Signal2d shiftedRedChannelSignal2d;
    Signal2d shiftedGreenChannelSignal2d;
    Signal2d shiftedBlueChannelSignal2d;    
    double[][] masks;
    Spinner<Integer> polarScaleSpinner;
    Spinner<Double> transparencySpinner;
    Rectangle selectionRectangle;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    
    @Override
    public void init() {
        try {
            baseImage = ResourceUtils.load3DTextureImage("carl-b-mono");
            baseImageView = new ImageView(baseImage);
            baseImagePR = baseImage.getPixelReader();
            int height = Double.valueOf(baseImage.getHeight()).intValue();
            int width = Double.valueOf(baseImage.getWidth()).intValue();            
            masks = new double[height][width];
            clearMask();
        } catch (IOException ex) {
            Logger.getLogger(ImageFFTTestApp.class.getName()).log(Level.SEVERE, null, ex);
            baseImageView = new ImageView();
        }
        baseImageView.setFitWidth(imageSize);
        baseImageView.setPreserveRatio(true);
        baseImageView.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event)) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        baseImageView.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                final File file = db.getFiles().get(0);
                if (JavaFX3DUtils.isTextureFile(file)) {
                    try {
                        baseImage = new Image(file.toURI().toURL().toExternalForm());
                        baseImageView.setImage(baseImage);
                        baseImagePR = baseImage.getPixelReader();
                        int height = Double.valueOf(baseImage.getHeight()).intValue();
                        int width = Double.valueOf(baseImage.getWidth()).intValue();            
                        masks = new double[height][width];
                        clearMask();
                    } catch (MalformedURLException ex) {
                        Logger.getLogger(ImageFFTTestApp.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });

        imageFFTCanvas = new Canvas(imageSize, imageSize);
        imageFFTGC = imageFFTCanvas.getGraphicsContext2D();
        imageFFTGC.setFill(fillColor);
        imageFFTGC.fillRect(0, 0, imageSize, imageSize);
        imageFFTCanvas.setCursor(Cursor.CROSSHAIR);
        //Setup selection rectangle and event handling
        selectionRectangle = new Rectangle(1, 1,
            Color.CYAN.deriveColor(1, 1, 1, 0.5));
        selectionRectangle.setManaged(false);
        selectionRectangle.setMouseTransparent(true);

        selectionRectangle.setVisible(false);
        imageFFTCanvas.setOnMousePressed((MouseEvent me) -> {
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
        imageFFTCanvas.setOnMouseDragged((MouseEvent me) -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            selectionRectangle.setWidth(
                mousePosX - selectionRectangle.getX());
            selectionRectangle.setHeight(
                mousePosY - selectionRectangle.getY());
        });
        imageFFTCanvas.setOnMouseReleased((MouseEvent me) -> {
            //transform from scene to local
            Point2D startLocal = imageFFTCanvas.sceneToLocal(selectionRectangle.getX(), selectionRectangle.getY());
            Point2D endLocal = startLocal.add(selectionRectangle.getWidth(), selectionRectangle.getHeight());
            //update masks
            int startY = Double.valueOf(startLocal.getY()).intValue();
            int endY = Double.valueOf(endLocal.getY()).intValue();
            int startX = Double.valueOf(startLocal.getX()).intValue();
            int endX = Double.valueOf(endLocal.getX()).intValue();
            PixelWriter pw = imageFFTGC.getPixelWriter();
            for(int y=startY;y<endY;y++){
                for(int x=startX;x<endX;x++){
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
            for(int y=startY;y<endY;y++){
                for(int x=startX;x<endX;x++){
                    redShiftCopy.setAt(y, x, masks[y][x],masks[y][x]);
                    greenShiftCopy.setAt(y, x, masks[y][x],masks[y][x]);
                    blueShiftCopy.setAt(y, x, masks[y][x],masks[y][x]);
                }
            }            
            //invert fftshift
            Signal2d inverseShiftedRedChannelSignal2d = fftShift2d_P(redShiftCopy, true);
            Signal2d inverseShiftedGreenChannelSignal2d = fftShift2d_P(greenShiftCopy, true);
            Signal2d inverseShiftedBlueChannelSignal2d = fftShift2d_P(blueShiftCopy, true);              
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

            selectionRectangle.setVisible(false);
            selectionRectangle.setWidth(1);
            selectionRectangle.setHeight(1);
            
        });
        
        inverseFFTCanvas = new Canvas(imageSize, imageSize);
        inverseFFTGC = inverseFFTCanvas.getGraphicsContext2D();
        inverseFFTGC.setFill(fillColor);
        inverseFFTGC.fillRect(0, 0, imageSize, imageSize);

        polarCanvas = new Canvas(imageSize, imageSize);
        polarGC = polarCanvas.getGraphicsContext2D();
        polarGC.setFill(fillColor);
        polarGC.fillRect(0, 0, imageSize, imageSize);

        plotPane = new GridPane(2, 2);
        plotPane.setHgap(2);
        plotPane.setVgap(2);
        plotPane.addRow(0, baseImageView, inverseFFTCanvas);
        plotPane.addRow(1, imageFFTCanvas, polarCanvas);
        
        Button runFFTButton = new Button("Execute FFT");
        runFFTButton.setOnAction(e -> {
            spaToFreq2D(baseImage);
        });
        
        polarScaleSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 10, 1));
//        polarScaleSpinner.valueProperty().addListener(c -> {
//            if (null != reals) {
//                int height = Double.valueOf(baseImage.getHeight()).intValue();
//                int width = Double.valueOf(baseImage.getWidth()).intValue();
//
//                polarPlot(polarScaleSpinner.getValue(), height, width, reals, imagines);
//            }
//        });
        transparencySpinner = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1, 0.666, 0.1));


        Button clearMaskButton = new Button("Clear Mask");
        clearMaskButton.setOnAction(e -> {
            clearMask();
        });
        
        controlsBox = new VBox(10,
            runFFTButton,
            new Label("Polar scale"),
            polarScaleSpinner,
            new Label("Transparency"),
            transparencySpinner, 
            clearMaskButton
        );
    }

    @Override
    public void start(Stage stage) {
        BorderPane borderPane = new BorderPane(plotPane);
        borderPane.setLeft(controlsBox);
        borderPane.getChildren().add(selectionRectangle); // a little hacky but...
        Scene scene = new Scene(borderPane);

        stage.setTitle("Image FFT Test");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        System.exit(0);
    }
    private void clearMask() {
        Arrays.stream(masks).forEach(row->Arrays.fill(row,1.0));
    }
    private Signal2d fftShift2d_P(Signal2d inputSignal2d, boolean inverse) { 

        int totalColumns = inputSignal2d.getN();
        int totalRows = inputSignal2d.getM();
        int middleColumn = totalColumns / 2;
        int columnRange = totalColumns - middleColumn;
        int middleRow = totalRows / 2;
        int rowRange = totalRows - middleRow;

        if(inverse) {
            //doIt(rx, mx, ry, my);
            middleColumn = totalColumns - middleColumn;
            columnRange = totalColumns / 2;
            middleRow = totalRows - middleRow;
            rowRange = totalRows / 2;
        }
//        else
//            doIt(mx, rx, my, ry);        
        
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
        
        greySignal2d = new Signal2d(height, width);
        redChannelSignal2d = new Signal2d(height, width);
        greenChannelSignal2d = new Signal2d(height, width);
        blueChannelSignal2d = new Signal2d(height, width);
        //do the red channel
        PixelReader pixelReader = image.getPixelReader();
        Color color = Color.BLACK;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                color = pixelReader.getColor(x, y);
                greySignal2d.setReAt(y, x, color.grayscale().getBrightness());
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
        System.out.print("FFT on Greyscale... ");
        long startTime = System.nanoTime();
        transformer2D.transform(greySignal2d);
        Utils.printTotalTime(startTime);
        transformer2D.transform(redChannelSignal2d);
        transformer2D.transform(greenChannelSignal2d);
        transformer2D.transform(blueChannelSignal2d);

        //fshift = np.fft.fftshift(f_image)
        System.out.print("FFTShift on Greyscale... ");
        startTime = System.nanoTime();
        shiftedGreySignal2d = fftShift2d_P(greySignal2d, false);
        Utils.printTotalTime(startTime);
        shiftedRedChannelSignal2d = fftShift2d_P(redChannelSignal2d, false);
        shiftedGreenChannelSignal2d = fftShift2d_P(greenChannelSignal2d, false);
        shiftedBlueChannelSignal2d = fftShift2d_P(blueChannelSignal2d, false);

        //magnitude_spectrum = 20 * np.log(np.abs(fshift))
        double redlogVal, greenlogVal, bluelogVal = 0;
        DoubleSummaryStatistics stats = Arrays
                .stream(greySignal2d.getRe())
                .summaryStatistics();
        System.out.println("Max: " + stats.getMax() + " Min: " + stats.getMin());

        baseImagePR = baseImage.getPixelReader();
        PixelWriter pw = imageFFTGC.getPixelWriter();
        imageFFTGC.setFill(Color.BLACK);
        imageFFTGC.fillRect(0, 0, imageFFTCanvas.getWidth(), imageFFTCanvas.getHeight());
        int height = Double.valueOf(image.getHeight()).intValue();
        int width = Double.valueOf(image.getWidth()).intValue();
        double opacity = transparencySpinner.getValue();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                redlogVal = Math.log10(Math.abs(shiftedRedChannelSignal2d.getReAt(y, x))/ polarScaleSpinner.getValue());
                greenlogVal = Math.log10(Math.abs(shiftedGreenChannelSignal2d.getReAt(y, x))/ polarScaleSpinner.getValue());
                bluelogVal = Math.log10(Math.abs(shiftedBlueChannelSignal2d.getReAt(y, x))/ polarScaleSpinner.getValue());

                pw.setColor(x, y, Color.color(
                    Utils.clamp(0,redlogVal,1), 
                    Utils.clamp(0,greenlogVal,1),
                    Utils.clamp(0,bluelogVal,1), 
                    opacity)
                );
                //This is good and correct for grayscale
                //                pw.setColor(x, y, Color.gray(Utils.clamp(0, 
                //                    Math.log10(Math.abs(shiftedGreySignal2d.getReAt(y, x))) / polarScaleSpinner.getValue(),
                //                    1)));
            }
        }
        System.out.println("log of fourier transformed greyscale image done.");

        System.out.println("Polar Plotting...");
        polarGC.clearRect(0, 0, polarCanvas.getWidth(), polarCanvas.getHeight());
        polarPlot2D(polarScaleSpinner.getValue(), height, width, shiftedRedChannelSignal2d);
        polarPlot2D(polarScaleSpinner.getValue(), height, width, shiftedGreenChannelSignal2d);
        polarPlot2D(polarScaleSpinner.getValue(), height, width, shiftedBlueChannelSignal2d);

        startTime = System.nanoTime();
        System.out.print("Inverse on Red Channel... ");
        transformer2D.inverse(redChannelSignal2d);
        Utils.printTotalTime(startTime);
        transformer2D.inverse(greenChannelSignal2d);
        transformer2D.inverse(blueChannelSignal2d);

        plotInverseFFT(height,width, redChannelSignal2d, greenChannelSignal2d, blueChannelSignal2d);

        // don't forget to shut it down as it uses an executor service
        transformer2D.shutdown();
    }
    public void plotInverseFFT(int height, int width, Signal2d redChannel, Signal2d greenChannel, Signal2d blueChannel) {
        PixelWriter pw = inverseFFTGC.getPixelWriter();
        PixelReader pr = baseImage.getPixelReader();
        Color color = Color.BLACK;
        Color originalColor = Color.BLACK;
        double level = 1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
//                color = new Color(
//                        clamp(0, redChannel.getReAt(y, x), 1),
//                        clamp(0, greenChannel.getReAt(y, x), 1),
//                        clamp(0, blueChannel.getReAt(y, x), 1),
//                        1);//masks[y][x]);
                originalColor = pr.getColor(x, y);
                level = clamp(0, (redChannel.getReAt(y, x) + greenChannel.getReAt(y, x)
                    + blueChannel.getReAt(y, x))/1.0, 1);
                pw.setColor(x, y, originalColor.deriveColor(1, 1, level, 1));
            }
        }        
    }
    public void polarPlot2D(double scale, int height, int width, Signal2d signal2D) {
        PixelWriter polarPW = polarGC.getPixelWriter();
        double magnitude, phaseAngle;
        int xCoord, yCoord;
        int shiftX = Double.valueOf(polarCanvas.getWidth() / 2.0).intValue();
        int shiftY = Double.valueOf(polarCanvas.getHeight() / 2.0).intValue();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                //magnitude/radius = amplitude component
                magnitude = signal2D.getReAt(y, x) * scale;
                //angle = phase component
                //atan2(imag(X),real(X))*180/pi; %phase information
                phaseAngle = Math.atan2(signal2D.getImAt(y, x), signal2D.getReAt(y, x))
                        * 180.0 / Math.PI; //%phase information
//Compute Polar Coordinates based on magnitude and phase                
//return new Vector(magnitude * Math.cos(flippedAngle),
//                magnitude * Math.sin(flippedAngle));

                xCoord = Long.valueOf(Math.round(magnitude
                        * Math.cos(phaseAngle))).intValue()
                        + shiftX;
                yCoord = Long.valueOf(Math.round(magnitude
                        * Math.sin(phaseAngle))).intValue()
                        + shiftY;
                polarPW.setColor(xCoord, yCoord, baseImagePR.getColor(x, y));
            }
        }
    }

    public void polarPlot(double scale, int height, int width, float[][] reals, float[][] imagines) {
        PixelWriter polarPW = polarGC.getPixelWriter();
        polarGC.clearRect(0, 0, polarCanvas.getWidth(), polarCanvas.getHeight());

        double magnitude, phaseAngle;
        int xCoord, yCoord;
        int shiftX = Double.valueOf(polarCanvas.getWidth() / 2.0).intValue();
        int shiftY = Double.valueOf(polarCanvas.getHeight() / 2.0).intValue();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                //magnitude/radius = amplitude component
                magnitude = reals[y][x] * scale;
                //angle = phase component
                //atan2(imag(X),real(X))*180/pi; %phase information
                phaseAngle = Math.atan2(imagines[y][x], reals[y][x]) * 180.0 / Math.PI; //%phase information
//Compute Polar Coordinates based on magnitude and phase                
//return new Vector(magnitude * Math.cos(flippedAngle),
//                magnitude * Math.sin(flippedAngle));

                xCoord = Long.valueOf(Math.round(magnitude
                        * Math.cos(phaseAngle))).intValue()
                        + shiftX;
                yCoord = Long.valueOf(Math.round(magnitude
                        * Math.sin(phaseAngle))).intValue()
                        + shiftY;
                polarPW.setColor(xCoord, yCoord, baseImagePR.getColor(x, y));
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
