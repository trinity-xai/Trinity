/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils;

import com.github.sarxos.webcam.Webcam;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

/**
 * Utilities used to manipulate attached camera.
 *
 * @author Sean Phillips
 */
public enum WebCamUtils {
    INSTANCE;
    private static final Logger LOG = LoggerFactory.getLogger(WebCamUtils.class);
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
            LOG.info("Attempting to initialize camera...");
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
