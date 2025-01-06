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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class ImageFFTTestApp extends Application {
    private double imageSize = 512;
    private HBox plotBox, controlsBox;
    private Image baseImage;
    private ImageView baseImageView;
    private Canvas imageFFTCanvas, inverseFFTCanvas;
    private GraphicsContext imageFFTGC, inverseFFTGC;
    

    @Override
    public void init() {
        try {
            baseImage = ResourceUtils.load3DTextureImage("carl-b-mono");
            baseImageView = new ImageView(baseImage);

        } catch (IOException ex) {
            Logger.getLogger(ImageFFTTestApp.class.getName()).log(Level.SEVERE, null, ex);
            baseImageView = new ImageView();
        }
        baseImageView.setFitWidth(imageSize);
        baseImageView.setPreserveRatio(true);

        imageFFTCanvas = new Canvas(imageSize, imageSize);
        imageFFTGC = imageFFTCanvas.getGraphicsContext2D();
        imageFFTGC.setFill(Color.ALICEBLUE);
        imageFFTGC.fillRect(0, 0, imageSize, imageSize);
        
        inverseFFTCanvas = new Canvas(imageSize, imageSize);
        inverseFFTGC = inverseFFTCanvas.getGraphicsContext2D();
        inverseFFTGC.setFill(Color.ALICEBLUE);
        inverseFFTGC.fillRect(0, 0, imageSize, imageSize);
        
        plotBox = new HBox(20, baseImageView, imageFFTCanvas, inverseFFTCanvas); 
        
        
        Button runFFTButton = new Button("Execute FFT");
        runFFTButton.setOnAction(e -> {
            spaTofreq(baseImage);
        });
        controlsBox = new HBox(20, runFFTButton);
    }

    @Override
    public void start(Stage stage) {
        BorderPane borderPane = new BorderPane(plotBox);
        borderPane.setBottom(controlsBox);
        Scene scene = new Scene(borderPane);

        stage.setTitle("Image FFT Test");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        System.exit(0);
    }
    public void spaTofreq(Image image) {
        //convert rgb image to gray image
        //img1=rgb2gray(image)
        WritableImage img1 = JavaFX3DUtils.convertToGreyScale(image);
      
        int height = Double.valueOf(img1.getHeight()).intValue();
        int width = Double.valueOf(img1.getWidth()).intValue();
//        //normalize the matrix of values
//        img2=double(mat2gray(img1))

        //fft of the image

        int binSize = 512;
        int sampleSize = binSize / 2 + 1;
        FFT fft = new FFT(binSize, 44100);
        float[][] samples = new float[binSize][binSize];
        float[][] reals = new float[height][sampleSize];
        float[][] spectrums = new float[height][sampleSize];
        float[][] logs = new float[height][sampleSize];
        
        
        PixelReader pixelReader = img1.getPixelReader();
        Color       colorFromMonoChromeImage;
        double      brightness, opacity;
        for (int y = 0 ; y < height ; y++) {
            for (int x = 0 ; x < width ; x++) {
                colorFromMonoChromeImage = pixelReader.getColor(x, y);
                opacity               = colorFromMonoChromeImage.getOpacity();
                brightness               = colorFromMonoChromeImage.getBrightness();
                samples[y][x] = Double.valueOf(opacity).floatValue();
            }
        }        

        System.out.println("samples extracted.");

        for (int y = 0 ; y < height ; y++) {
            fft.forward(samples[y]);
            System.arraycopy(fft.getSpectrum(), 0, spectrums[y], 0, spectrums[y].length);
            System.arraycopy(fft.getRealPart(), 0, reals[y], 0, reals[y].length);
        }        

        System.out.println("FFTs done.");
        
//        //center shift the fourier of the transformed image
//        imgF=fftshift(fft2(img2))

//        //take a log of the image matrix
//        I=log(1+abs(imgF))
        PixelWriter pw = imageFFTGC.getPixelWriter();
        double normalAbsFloat = 0; 
        double logVal = 0;
        for (int y = 0 ; y < height ; y++) {
            for (int x = 0 ; x < reals[y].length ; x++) {
                normalAbsFloat = DataUtils.normalize(Math.abs(reals[y][x]), 0, 512);
                logVal = Math.log(1 + normalAbsFloat);
//                System.out.print(logVal + " ");
                pw.setColor(x, y, Color.gray(logVal));
            }
        }        

        System.out.println("log of fourier transformed image.");        
        
        
//        //plot original image
//        imshow(image)
//        
//        //fourier transform of image
//        imshow(I)
//               
//        //Frequency bar graph of image
//        bar(I)
//                
//        //Inverse Fourier Transform of image
//        imginv=abs(ifft2(imgF))
//        imshow(imginv)        
    }

    public static void main(String[] args) {
        launch(args);
    }
}
