package edu.jhuapl.trinity.utils.fun;

/*-
 * #%L
 * trinity-2023.10.07
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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.FloatMap;
import javafx.util.Duration;

/**
 *
 * @author Birdasaur
 */
public enum GlitchUtils {
    INSTANCE;
    
    public static void gitchNode(Node node, Duration duration, Duration delay, int cycleCount) {
        int width = 220;
        int height = 100;

        FloatMap floatMap = new FloatMap();
        floatMap.setWidth(width);
        floatMap.setHeight(height);

        for (int i = 0; i < width; i++) {
            double v = (Math.sin(i / 20.0 * Math.PI) - 0.5) / 40.0;
            for (int j = 0; j < height; j++) {
                floatMap.setSamples(i, j, 0.0f, (float) v);
            }
        }        
        DisplacementMap map = new DisplacementMap(floatMap);
            //, width, width, width, width)

        Timeline t = new Timeline(
            new KeyFrame(Duration.millis(0), e -> node.setEffect(map)),
            new KeyFrame(duration, e->node.setEffect(null)),
            new KeyFrame(duration.add(delay), e->noOp())
        );
        t.setCycleCount(cycleCount);
        t.setAutoReverse(false);
        t.play();
    }
    private static void noOp(){
        
    }
}
