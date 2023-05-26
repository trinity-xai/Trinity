package edu.jhuapl.trinity.utils;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
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

    public ImageTest() {
    }

    /**
     * Test of doCommonsPca method, of class AnalysisUtils.
     */
    //@Test
    public void scrapeFlags() throws MalformedURLException, IOException {
        System.out.println("scrapeFlags");
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
