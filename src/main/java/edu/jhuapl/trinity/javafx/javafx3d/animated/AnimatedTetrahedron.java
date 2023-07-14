package edu.jhuapl.trinity.javafx.javafx3d.animated;

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

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.shape.TriangleMesh;
import javafx.util.Duration;
import org.fxyz3d.shapes.primitives.TetrahedraMesh;

import static javafx.animation.Animation.INDEFINITE;

/**
 * @author Sean Phillips
 */
public class AnimatedTetrahedron extends TetrahedraMesh {
    public double DEFAULT_RATEOFCHANGE = 0.01;
    public double rateOfChange = DEFAULT_RATEOFCHANGE;
    public TriangleMesh triangleMesh;
    public IntegerProperty keyCycle = new SimpleIntegerProperty();
    Timeline tm;
    float[] uvCoords = {
        0f, 0f,
        0.25f, 0.5f,
        0.5f, 0f,
        0.5f, 1f,
        0.75f, 0.5f,
        1f, 0f
    };

    boolean animating = false;

    public AnimatedTetrahedron(double height) {
        super(height);
        triangleMesh = (TriangleMesh) this.getMesh();
        setCycle(20, 30);
        keyCycle.addListener(e -> {
            float add = keyCycle.getValue() / 30000f;
            //i=0;i+=2 is right to left
            //i=1;i+=2 is bottom to top
            for (int i = 1; i < uvCoords.length; i += 2) {
                uvCoords[i] -= rateOfChange;
            }
            triangleMesh.getTexCoords().set(0, uvCoords, 0, uvCoords.length);
        });
        setOnScroll(e -> {
            rateOfChange += e.getDeltaY() / 10000f;
        });
        setOnMouseClicked(e -> {
            if (e.isSecondaryButtonDown()) {
                rateOfChange = DEFAULT_RATEOFCHANGE;
            }
            if (e.getClickCount() > 1) {
                enableCycle(!animating);
            }
        });
        mesh.getPoints().clear();
        mesh.getTexCoords().clear();
        mesh.getFaces().clear();
        mesh.getFaceSmoothingGroups().clear();
        float hw = Double.valueOf(height).floatValue();
        mesh.getPoints().addAll(
            0f, 0f, 0.612372f * hw, // top -> center
            -0.288675f * hw, -0.5f * hw, -0.204124f * hw, // base -> top left
            -0.288675f * hw, 0.5f * hw, -0.204124f * hw, // base -> bottom left
            0.57735f * hw, 0f, -0.204124f * hw); // base -> right center

        // normalized textures
        // open from top vertex, and project to 2D
        // 0   2   5
        //   1   4
        //     3
        mesh.getTexCoords().addAll(
            0f, 0f,
            0.25f, 0.5f,
            0.5f, 0f,
            0.5f, 1f,
            0.75f, 0.5f,
            1f, 0f);

        mesh.getFaces().addAll(
            1, 0, 2, 2, 3, 1,
            2, 3, 1, 1, 0, 4,
            3, 2, 0, 4, 1, 1,
            0, 4, 3, 2, 2, 5);
    }

    public void setCycle(double cycleSeconds, double fps) {
        KeyValue start = new KeyValue(keyCycle, 0, Interpolator.LINEAR);
        KeyValue end = new KeyValue(keyCycle, fps * cycleSeconds, Interpolator.LINEAR);
        KeyFrame kf = new KeyFrame(Duration.seconds(cycleSeconds), start, end);
//        KeyFrame cycleFinished = new KeyFrame(Duration.seconds(cycleSeconds), e->{
//
//        });
        tm = new Timeline(kf);
        tm.setCycleCount(INDEFINITE);
    }

    public void enableCycle(boolean enable) {
        animating = enable;
        if (enable)
            tm.play();
        else
            tm.stop();
    }
}
