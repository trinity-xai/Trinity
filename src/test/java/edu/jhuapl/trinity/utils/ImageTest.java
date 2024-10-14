/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sean Phillips
 */
public class ImageTest {
    private static final Logger LOG = LoggerFactory.getLogger(ImageTest.class);

    public ImageTest() {
    }

    /**
     * Test of doCommonsPca method, of class AnalysisUtils.
     */
    //@Test
    public void scrapeFlags() throws MalformedURLException, IOException {
        LOG.info("scrapeFlags");
        DecimalFormat df = new DecimalFormat("##");
        df.setDecimalSeparatorAlwaysShown(false);
        df.setMaximumFractionDigits(0);
        df.setMinimumIntegerDigits(2);
        df.setMaximumIntegerDigits(2);
        for (int i = 0; i < 51; i++) {
            String imageName = "UNST01" + df.format(i);
            URL url = new URL("http://www.flags.net/images/largeflags/" + imageName + ".GIF");
            BufferedImage image = ImageIO.read(url);
            ImageIO.write(image, "png", new File("c:/dev/trinity/imagery/" + imageName + ".PNG"));

        }

        assertTrue(true);
    }

}
