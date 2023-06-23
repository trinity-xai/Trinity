package edu.jhuapl.trinity.javafx.components.radial;

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

import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;

import java.util.ArrayList;

/**
 * @author Sean Phillips
 */
public class AnimatedFillCircle extends Circle {
    public Color fillStartColor = Color.CYAN;
    public Color fillEndColor = Color.CYAN;
    public LinearGradient lg;
    public Stop stop1, stop2, stop3;
    public SimpleDoubleProperty percentComplete = new SimpleDoubleProperty(0.0);

    public AnimatedFillCircle(double radius, double strokeWidth, Double... dashedArray) {
        updateComplete();
        setStroke(fillStartColor);
        setStrokeWidth(strokeWidth);
        setEffect(new Glow(0.9));
        setRadius(radius);
        getStrokeDashArray().setAll(dashedArray);
        percentComplete.addListener(l -> updateComplete());
    }

    public void setPercentComplete(double complete) {
        percentComplete.set(complete);
    }

    private void updateComplete() {
        stop1 = new Stop(0, fillStartColor);
        stop2 = new Stop(percentComplete.get(), Color.TRANSPARENT);
        stop3 = new Stop(1, fillEndColor);
        ArrayList<Stop> stops = new ArrayList<>();
        stops.add(stop1);
        stops.add(stop2);
        stops.add(stop3);
        lg = new LinearGradient(
            0.5, 1.0, 0.5, 0.0, true, CycleMethod.NO_CYCLE, stops);
        setFill(lg);
    }
}
