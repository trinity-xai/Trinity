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
package edu.jhuapl.trinity.javafx.handlers;

import edu.jhuapl.trinity.data.Trial;
import edu.jhuapl.trinity.javafx.components.ColorMap;
import edu.jhuapl.trinity.javafx.events.NeuralEvent;
import edu.jhuapl.trinity.javafx.renderers.NeuralRenderer;
import javafx.event.EventHandler;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class NeuralEventHandler implements EventHandler<NeuralEvent> {

    List<NeuralRenderer> renderers;
    ColorMap colorMap;
    int colorIndex = 0;
    int colorCount = 0;

    public NeuralEventHandler() {
        renderers = new ArrayList<>();
        colorMap = ColorMap.jet();
        colorCount = colorMap.getFixedColorCount();
    }

    public void addNeuralRenderer(NeuralRenderer renderer) {
        renderers.add(renderer);
    }

    public void handleNewTrial(NeuralEvent event) {
        Trial trial = (Trial) event.object;
        for (NeuralRenderer renderer : renderers) {
            renderer.addTrial(trial, Color.WHITE);
        }
    }

    public void handleTrialList(NeuralEvent event) {
        ArrayList<Trial> trialList = (ArrayList<Trial>) event.object;
        for (NeuralRenderer renderer : renderers) {
            double size = Double.valueOf(trialList.size());
            for (int i = 0; i < trialList.size(); i++) {
                Trial trial = trialList.get(i);
                double colorTerp = Double.valueOf(i) / size;
                Color color = colorMap.get(colorTerp);
                renderer.addTrial(trial, color);
            }
        }
    }

    @Override
    public void handle(NeuralEvent event) {
        if (event.getEventType().equals(NeuralEvent.NEW_NEURAL_TRIAL))
            handleNewTrial(event);
        else if (event.getEventType().equals(NeuralEvent.NEURAL_TRIAL_LIST))
            handleTrialList(event);

    }
}
