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


import edu.jhuapl.trinity.data.audio.FFT;
import edu.jhuapl.trinity.utils.DataUtils;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javafx.application.Application;
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
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

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
    int binSize = 1024;
    int sampleSize = binSize / 2 + 1;
    float[][] samples; 
    float[][] reals; 
    float[][] imagines; 
    float[][] spectrums; 
    float[][] shifted; 
    float[][] inverses; 
    Spinner<Integer> polarScaleSpinner;
    
    @Override
    public void init() {
        try {
            baseImage = ResourceUtils.load3DTextureImage("carl-b-mono");
            baseImageView = new ImageView(baseImage);
            baseImagePR = baseImage.getPixelReader();
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
        
        inverseFFTCanvas = new Canvas(imageSize, imageSize);
        inverseFFTGC = inverseFFTCanvas.getGraphicsContext2D();
        inverseFFTGC.setFill(fillColor);
        inverseFFTGC.fillRect(0, 0, imageSize, imageSize);

        polarCanvas = new Canvas(imageSize, imageSize);
        polarGC = polarCanvas.getGraphicsContext2D();
        polarGC.setFill(fillColor);
        polarGC.fillRect(0, 0, imageSize, imageSize);
        
        plotPane = new GridPane(2,2);
        plotPane.setHgap(2);
        plotPane.setVgap(2);
        plotPane.addRow(0, baseImageView, inverseFFTCanvas);
        plotPane.addRow(1, imageFFTCanvas, polarCanvas); 
        
        
        Button runFFTButton = new Button("Execute FFT");
        runFFTButton.setOnAction(e -> {
            spaTofreq(baseImage);
        });
        polarScaleSpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 10, 1));
        polarScaleSpinner.valueProperty().addListener(c -> {
            if(null != reals){
                int height = Double.valueOf(baseImage.getHeight()).intValue();
                int width = Double.valueOf(baseImage.getWidth()).intValue();
                
                polarPlot(polarScaleSpinner.getValue(), height, width, reals, imagines);
            }
        });
        controlsBox = new VBox(20, 
            runFFTButton,
            new Label("Polar scale"),
            polarScaleSpinner
        );
    }

    @Override
    public void start(Stage stage) {
        BorderPane borderPane = new BorderPane(plotPane);
        borderPane.setLeft(controlsBox);
        Scene scene = new Scene(borderPane);

        stage.setTitle("Image FFT Test");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        System.exit(0);
    }
    public void spaTofreq(final Image image) {
        //convert rgb image to gray image
        //img1=rgb2gray(image)
        WritableImage img1 = JavaFX3DUtils.convertToGreyScale(image);
      
        int height = Double.valueOf(img1.getHeight()).intValue();
        int width = Double.valueOf(img1.getWidth()).intValue();
//        //normalize the matrix of values
//        img2=double(mat2gray(img1))

        //fft of the image

        binSize = 1024;
        sampleSize = binSize / 2 + 1;
        FFT fft = new FFT(binSize, 1);
        samples = new float[binSize][binSize];
        reals = new float[height][sampleSize];
        imagines = new float[height][sampleSize];
        spectrums = new float[height][sampleSize];
        shifted = new float[height][sampleSize];
        inverses = new float[height][sampleSize];
        
        
        PixelReader pixelReader = img1.getPixelReader();
        Color       colorFromMonoChromeImage;
        double      brightness, opacity;
        for (int y = 0 ; y < height ; y++) {
            for (int x = 0 ; x < width ; x++) {
                colorFromMonoChromeImage = pixelReader.getColor(x, y);
                opacity               = colorFromMonoChromeImage.getOpacity();
                brightness               = colorFromMonoChromeImage.getBrightness();
                samples[y][x] = Double.valueOf(brightness).floatValue();
            }
        }        

        System.out.println("samples extracted.");
        //do the fft
        for (int y = 0 ; y < height ; y++) {
            fft.forward(samples[y]);
            System.arraycopy(fft.getSpectrum(), 0, spectrums[y], 0, spectrums[y].length);
            System.arraycopy(fft.getRealPart(), 0, reals[y], 0, reals[y].length);
            System.arraycopy(fft.getImaginaryPart(), 0, imagines[y], 0, imagines[y].length);
            
            //copies to simulate fftshift
            System.arraycopy(fft.getRealPart(), 0, shifted[y], reals[y].length/2, reals[y].length/2);
            System.arraycopy(fft.getRealPart(), reals[y].length/2, shifted[y], 0, reals[y].length/2);
        }        

        System.out.println("FFTs done.");
        
//        //center shift the fourier of the transformed image
//        imgF=fftshift(fft2(img2))
        
//        //take a log of the image matrix
//        I=log(1+abs(imgF))
        PixelWriter pw = imageFFTGC.getPixelWriter();
        imageFFTGC.clearRect(0, 0, imageFFTCanvas.getWidth(), imageFFTCanvas.getHeight());

        double normalAbsFloat, normalAmp = 0; 
        double logVal = 0;
        
        DoubleSummaryStatistics stats = Arrays
          .stream(spectrums).parallel()
          .flatMapToDouble(t -> IntStream.range(0, t.length).mapToDouble(i -> t[i]))
          .summaryStatistics();        
        
        System.out.println("Max: " + stats.getMax() + " Min: " + stats.getMin());
        baseImagePR = baseImage.getPixelReader();
        double amplitude;
        for (int y = 0 ; y < height ; y++) {
            for (int x = 0 ; x < width ; x++) {
//                normalAbsFloat = DataUtils.normalize(Math.abs(shifted[y][x]), -stats.getMax(), stats.getMax());
                logVal = Math.log(Math.abs(spectrums[y][x]));
                pw.setColor(x, y, Color.gray(Utils.clamp(0, logVal, 1)));
            }
        }        
        System.out.println("log of fourier transformed image.");        

        polarPlot(polarScaleSpinner.getValue(), height, width, reals, imagines);
        System.out.println("Polar Plot done."); 
                
        //Inverse Fourier Transform of image
//        imginv=abs(ifft2(imgF))
        PixelWriter inversePW = inverseFFTGC.getPixelWriter();
        inverseFFTGC.clearRect(0, 0, inverseFFTCanvas.getWidth(), inverseFFTCanvas.getHeight());

        for (int y = 0 ; y < height ; y++) {
            fft.forward(samples[y]);
            fft.inverse(inverses[y]);
            for (int x = 0 ; x < inverses[y].length ; x++) {
                inversePW.setColor(x, y, Color.gray(Utils.clamp(0.0, inverses[y][x], 1.0)));
            }
        }        
        System.out.println("Inverses done.");        
    }

    public void polarPlot(double scale, int height, int width, float [][]reals, float[][]imagines) {
        PixelWriter polarPW = polarGC.getPixelWriter();
        polarGC.clearRect(0, 0, polarCanvas.getWidth(), polarCanvas.getHeight());

        double magnitude,phaseAngle;
        int xCoord, yCoord;
        int shiftX = Double.valueOf(polarCanvas.getWidth()/2.0).intValue();
        int shiftY = Double.valueOf(polarCanvas.getHeight()/2.0).intValue();
        for (int y = 0 ; y < height ; y++) {
            for (int x = 0 ; x < width ; x++) {
                //magnitude/radius = amplitude component
                magnitude = reals[y][x] * scale;
                //angle = phase component
                //atan2(imag(X),real(X))*180/pi; %phase information
                phaseAngle = Math.atan2(imagines[y][x],reals[y][x])*180.0/Math.PI; //%phase information
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
