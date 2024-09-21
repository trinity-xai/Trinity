package edu.jhuapl.trinity.javafx.javafx3d.particle;

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

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Smoke extends DualTextureParticle {
    private static final Logger LOG = LoggerFactory.getLogger(Smoke.class);
    public static final String DEFAULT_TEXTURE1_NAME = "smoke2_16";
    public static final String DEFAULT_TEXTURE2_NAME = "smoke2_16";
    public static Image image1 = null;
    public static Image image2 = null;
    public static float DEFAULT_DYING_AGE = 10000;
    public static float DEFAULT_GRAVITY = -0.01f;
    public double expansionScale = 1.0;
    public double expansionRateOfChange = 0.1;

    static {
        try {
            image1 = ResourceUtils.load3DTextureImage(DEFAULT_TEXTURE1_NAME);
            image2 = ResourceUtils.load3DTextureImage(DEFAULT_TEXTURE2_NAME);
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
    }

    public Smoke() {
        super(image1, image2);
        gravity = DEFAULT_GRAVITY;
        reset();
    }

    @Override
    public boolean update(final double _time) {
        if (!super.update(_time)) {
            return false;
        }
//        texture1.setScaleX(texture1.getScaleX()+expansionRateOfChange);
//        texture1.setScaleY(texture1.getScaleY()+expansionRateOfChange);
//        texture2.setScaleX(texture2.getScaleX()+expansionRateOfChange);
//        texture2.setScaleY(texture2.getScaleY()+expansionRateOfChange);
        return true;
    }

    @Override
    public void reset() {
        expansionScale = 1.0;
//        texture1.setScaleX(expansionScale);
//        texture1.setScaleY(expansionScale);
//        texture2.setScaleX(expansionScale);
//        texture2.setScaleY(expansionScale);
        birthAge = -1;
        age = 0;
        dyingAge = DEFAULT_DYING_AGE + random.nextInt(100);
        sizeCounter = 0.0003f;
        rotation = .05f;
        rotationCounter = 0.01f;
        location.x = location.y = location.z = 0;
        //gravity = DEFAULT_GRAVITY;

        size = .07f;

        color[0] = 0.5f;
        color[1] = 0.5f;
        color[2] = 0.5f;
        color[3] = 0.5f;

        colorCounter[0] = -0.2f / (dyingAge);
        colorCounter[1] = -0.2f / (dyingAge);
        colorCounter[2] = -0.2f / (dyingAge);
        colorCounter[3] = -1.0f / (dyingAge);

        color2[0] = 0.0f;
        color2[1] = 0.0f;
        color2[2] = 0.0f;
        color2[3] = 1.0f;

        colorCounter2[0] = 0.5f / (dyingAge);
        colorCounter2[1] = 0.5f / (dyingAge);
        colorCounter2[2] = 0.5f / (dyingAge);
        colorCounter2[3] = -1.0f / (dyingAge);

        velocity.x = -.001f + (0.002f * random.nextFloat());
        velocity.y = -0.001f * random.nextFloat(); //always up
        velocity.z = -.001f + (0.002f * random.nextFloat());

        location.x = 0 - 0.1f + (0.2f) * random.nextFloat();
        location.y = 0 - 0.1f + (0.2f) * random.nextFloat();
        location.z = 0 - 0.1f + (0.2f) * random.nextFloat();
    }
}
