package edu.jhuapl.trinity.utils;

import com.github.sarxos.webcam.Webcam;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

/**
 * Utilities used to manipulate attached camera.
 *
 * @author Sean Phillips
 */
public enum WebCamUtils {
    INSTANCE;
    public static Webcam webCam = null;

    public static void initialize() throws Exception {
        webCam = Webcam.getDefault(10, TimeUnit.SECONDS);
        webCam.open(true);
        Dimension d = webCam.getViewSizes()[2];
        webCam.close();
        webCam.setViewSize(d);
        webCam.open(true);
    }

    public static Image takePicture() throws Exception {
        if (null == webCam) {
            System.out.println("Attempting to initialize camera...");
            initialize();
        }
        BufferedImage img = null;
        WritableImage image = null;
        if ((img = webCam.getImage()) != null) {
            image = new WritableImage(img.getWidth(), img.getHeight());
            SwingFXUtils.toFXImage(img, image);
        } else
            image = new WritableImage(100, 100);

        return image;
    }
}
