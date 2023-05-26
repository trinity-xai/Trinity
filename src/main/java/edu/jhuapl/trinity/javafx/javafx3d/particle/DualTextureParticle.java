package edu.jhuapl.trinity.javafx.javafx3d.particle;

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

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.scene.DepthTest;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;

public abstract class DualTextureParticle extends AgingParticle {

    public static double DEFAULT_FIT_WIDTH = 16;
    public float[] color2 = new float[4]; // The Particle's Color
    public float[] colorCounter2 = new float[4]; // The Color Counter!
    public ImageView texture1;
    public ImageView texture2;

    public DualTextureParticle(String t1, String t2) {
        try {
            setTexture1(ResourceUtils.load3DTextureImage(t1));
            setTexture2(ResourceUtils.load3DTextureImage(t2));
        } catch (final IOException _e) {
            System.out.println("Unable to load texture");
        }
    }

    public DualTextureParticle(Image image1, Image image2) {
        setTexture1(image1);
        setTexture2(image2);
    }

    public final void setTexture1(Image image1) {
        texture1 = new ImageView(image1);
        texture1.setDepthTest(DepthTest.ENABLE);
        texture1.setPreserveRatio(true);
        texture1.setFitWidth(DEFAULT_FIT_WIDTH);
        texture1.visibleProperty().bind(activeProperty);

    }

    public final void setTexture2(Image image2) {
        texture2 = new ImageView(image2);
        texture2.setDepthTest(DepthTest.ENABLE);
        texture2.setPreserveRatio(true);
        texture2.setFitWidth(DEFAULT_FIT_WIDTH);
        texture2.visibleProperty().bind(activeProperty);
    }

    @Override
    public Node getNode() {
        return texture1;
    }
}
