package edu.jhuapl.trinity.javafx.components.panes;

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

import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.components.FeatureRadarChart;
import edu.jhuapl.trinity.javafx.events.FeatureVectorEvent;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import lit.litfx.controls.covalent.PathPane;
import lit.litfx.controls.covalent.events.CovalentPaneEvent;

import java.util.List;
import java.util.Optional;

/**
 * @author Sean Phillips
 */
public class RadarPlotPane extends PathPane {
    Scene scene;
    FeatureRadarChart radarChart;

    private static BorderPane createContent() {
        FeatureRadarChart radarChart = new FeatureRadarChart(FeatureVector.EMPTY_FEATURE_VECTOR("Waiting...", 20), 500, 500);
        BorderPane bpOilSpill = new BorderPane(radarChart);
        return bpOilSpill;
    }

    public RadarPlotPane(Scene scene, Pane parent) {
        super(scene, parent, 500, 500, createContent(), "Parameters ", "RADAR", 200.0, 300.0);
        this.scene = scene;
        this.scene.addEventHandler(FeatureVectorEvent.SELECT_FEATURE_VECTOR, e -> {
            Platform.runLater(() -> {
                radarChart.setLabels((List<String>) e.object2);
                setFeatureVector((FeatureVector) e.object);
            });
        });
        Optional<Node> bpOpt = contentPane.getChildren().stream()
            .filter(node -> node instanceof FeatureRadarChart)
            .findFirst();
        if (bpOpt.isPresent()) {
            radarChart = (FeatureRadarChart) bpOpt.get();
        }
        // must be set to prevent user from resizing too small.
        setMinWidth(300);
        setMinHeight(300);
        this.scene.getRoot().addEventHandler(CovalentPaneEvent.COVALENT_PANE_CLOSE, e -> {
            if (e.pathPane == this)
                parent.getChildren().remove(this);
        });
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> this.toFront());
    }

    public void setFeatureVector(FeatureVector featureVector) {
        radarChart.updateRadarPlot(featureVector);
    }
}
