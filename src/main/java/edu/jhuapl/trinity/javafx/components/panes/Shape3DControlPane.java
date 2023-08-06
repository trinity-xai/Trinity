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

import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * @author Sean Phillips
 */
public class Shape3DControlPane extends LitPathPane {
    BorderPane bp;
    private Slider scaleSlider;
    private Slider rotateXSlider;
    private Slider rotateYSlider;
    private Slider rotateZSlider;
    private Label scaleLabel;
    private Label rotateXLabel;
    private Label rotateYLabel;
    private Label rotateZLabel;
    /**
     * Format for floating coordinate label
     */
    private NumberFormat format = new DecimalFormat("0.00");

    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public Shape3DControlPane(Scene scene, Pane parent) {
        super(scene, parent, 400, 400, createContent(), 
        "Geometry Controls", "", 200.0, 300.0);
        this.scene = scene;

        bp = (BorderPane) this.contentPane;

        scaleLabel.setText("Scale " + format.format(scaleSlider.getValue()));
        scaleSlider.valueProperty().addListener((ov, t, t1) -> {
        scaleLabel.setText("Scale " + format.format(scaleSlider.getValue()));
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_SET_SCALE, t.doubleValue()));
        });

        rotateXLabel.setText("Rotate X: " + format.format(rotateXSlider.getValue()));
        rotateXSlider.valueProperty().addListener((ov, t, t1) -> {
            rotateXLabel.setText("Rotate X: " + format.format(rotateXSlider.getValue()));
            double[] ypr = new double[]{
                rotateYSlider.getValue(), rotateXSlider.getValue(), rotateZSlider.getValue()
            };
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_SET_YAWPITCHROLL, ypr));
        });

        rotateYLabel.setText("Rotate Y: " + format.format(rotateYSlider.getValue()));
        rotateYSlider.valueProperty().addListener((ov, t, t1) -> {
            rotateYLabel.setText("Rotate Y: " + format.format(rotateYSlider.getValue()));
            double[] ypr = new double[]{
                rotateYSlider.getValue(), rotateXSlider.getValue(), rotateZSlider.getValue()
            };
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_SET_YAWPITCHROLL, ypr));
        });

        rotateZLabel.setText("Rotate Z: " + format.format(rotateZSlider.getValue()));
        rotateZSlider.valueProperty().addListener((ov, t, t1) -> {
            rotateZLabel.setText("Rotate Z: " + format.format(rotateZSlider.getValue()));
            double[] ypr = new double[]{
                rotateYSlider.getValue(), rotateXSlider.getValue(), rotateZSlider.getValue()
            };
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_SET_YAWPITCHROLL, ypr));
        });
        
        VBox centerVBox = new VBox(10, scaleLabel, scaleSlider,
            rotateXLabel, rotateXSlider, rotateYLabel, rotateYSlider,
            rotateZLabel, rotateZSlider);
        bp.setCenter(centerVBox);
        
        
    }
}
