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

import edu.jhuapl.trinity.javafx.javafx3d.Hypersurface3DPane;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;

/**
 * @author Sean Phillips
 */
public class SurfaceControlPane extends LitPathPane {
    public static double ICON_FIT_HEIGHT = 64;
    public static double ICON_FIT_WIDTH = 64;
    BorderPane bp;
    Hypersurface3DPane hypersurface;
    Spinner xWidthSpinner, zWidthSpinner;
    
    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public SurfaceControlPane(Scene scene, Pane parent, Hypersurface3DPane hypersurface) {
        super(scene, parent, 400, 600, createContent(), "Hypersurface Controls ", "", 200.0, 300.0);
        this.scene = scene;
        this.hypersurface = hypersurface;
        bp = (BorderPane) this.contentPane;
        buildControls();
    }
    private void buildControls() {
        Spinner yScaleSpinner = new Spinner(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(
                0.0, 100.0, hypersurface.yScale, 1.00));
        yScaleSpinner.setEditable(true);
        //whenever the spinner value is changed...
        yScaleSpinner.valueProperty().addListener(e -> {
            hypersurface.yScale = ((Double) yScaleSpinner.getValue()).floatValue();
            hypersurface.surfPlot.setFunctionScale(hypersurface.yScale);
            hypersurface.updateTheMesh();
        });
        yScaleSpinner.setPrefWidth(125);
        Spinner surfScaleSpinner = new Spinner(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(
                0.0, 100.0, hypersurface.surfScale, 1.0));
        surfScaleSpinner.setEditable(true);
        //whenever the spinner value is changed...
        surfScaleSpinner.valueProperty().addListener(e -> {
            hypersurface.surfScale = ((Double) surfScaleSpinner.getValue()).floatValue();
            hypersurface.surfPlot.setRangeX(hypersurface.xWidth * hypersurface.surfScale);
            hypersurface.surfPlot.setRangeY(hypersurface.zWidth * hypersurface.surfScale);
            hypersurface.updateTheMesh();
            hypersurface.surfPlot.setTranslateX(-(hypersurface.xWidth * hypersurface.surfScale) / 2.0);
            hypersurface.surfPlot.setTranslateZ(-(hypersurface.zWidth * hypersurface.surfScale) / 2.0);
        });
        surfScaleSpinner.setPrefWidth(125);
//        Spinner divisionsSpinner = new Spinner(
//            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 512, 64, 4));
//        divisionsSpinner.setEditable(true);
//        //whenever the spinner value is changed...
//        divisionsSpinner.valueProperty().addListener(e -> {
//            surfPlot.setDivisionsX((int) divisionsSpinner.getValue());
//            surfPlot.setDivisionsY((int) divisionsSpinner.getValue());
//            updateTheMesh();
//        });
//        divisionsSpinner.setPrefWidth(125);

        xWidthSpinner = new Spinner(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 4000, 200, 4));
        xWidthSpinner.setEditable(true);
        //whenever the spinner value is changed...
        xWidthSpinner.valueProperty().addListener(e -> {
            hypersurface.xWidth = ((int) xWidthSpinner.getValue());
            hypersurface.updateTheMesh();
            hypersurface.surfPlot.setTranslateX(-(hypersurface.xWidth * hypersurface.surfScale) / 2.0);
            hypersurface.surfPlot.setTranslateZ(-(hypersurface.zWidth * hypersurface.surfScale) / 2.0);
        });
        xWidthSpinner.setPrefWidth(125);
        zWidthSpinner = new Spinner(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 4000, 200, 10));
        zWidthSpinner.setEditable(true);
        //whenever the spinner value is changed...
        zWidthSpinner.valueProperty().addListener(e -> {
            hypersurface.zWidth = ((int) zWidthSpinner.getValue());
            hypersurface.updateTheMesh();
            hypersurface.surfPlot.setTranslateX(-(hypersurface.xWidth * hypersurface.surfScale) / 2.0);
            hypersurface.surfPlot.setTranslateZ(-(hypersurface.zWidth * hypersurface.surfScale) / 2.0);
        });
        zWidthSpinner.setPrefWidth(125);
        ToggleGroup meshTypeToggle = new ToggleGroup();
        RadioButton surfaceRadioButton = new RadioButton("Surface Projection");
        surfaceRadioButton.setSelected(true);
        surfaceRadioButton.setToggleGroup(meshTypeToggle);
        RadioButton cylinderRadioButton = new RadioButton("Cylindrical");
        cylinderRadioButton.setToggleGroup(meshTypeToggle);
        meshTypeToggle.selectedToggleProperty().addListener(cl -> {
            hypersurface.surfaceRender = surfaceRadioButton.isSelected();
            hypersurface.updateTheMesh();
        });
        HBox meshTypeHBox = new HBox(10, surfaceRadioButton, cylinderRadioButton);

        ToggleGroup drawModeToggle = new ToggleGroup();
        RadioButton drawModeLine = new RadioButton("Line");
        drawModeLine.setSelected(true);
        drawModeLine.setToggleGroup(drawModeToggle);
        RadioButton drawModeFill = new RadioButton("Fill");
        drawModeFill.setToggleGroup(drawModeToggle);
        drawModeToggle.selectedToggleProperty().addListener(cl -> {
            if (drawModeLine.isSelected()) {
                hypersurface.surfPlot.setDrawMode(DrawMode.LINE);
            } else {
                hypersurface.surfPlot.setDrawMode(DrawMode.FILL);
            }
        });
        HBox drawModeHBox = new HBox(10, drawModeLine, drawModeFill);

        ToggleGroup cullFaceToggle = new ToggleGroup();
        RadioButton cullFaceFront = new RadioButton("Front");
        cullFaceFront.setToggleGroup(cullFaceToggle);
        RadioButton cullFaceBack = new RadioButton("Back");
        cullFaceBack.setToggleGroup(cullFaceToggle);
        RadioButton cullFaceNone = new RadioButton("None");
        cullFaceNone.setSelected(true);
        cullFaceNone.setToggleGroup(cullFaceToggle);
        cullFaceToggle.selectedToggleProperty().addListener(cl -> {
            if (cullFaceFront.isSelected()) {
                hypersurface.surfPlot.setCullFace(CullFace.FRONT);
            } else if (cullFaceBack.isSelected()) {
                hypersurface.surfPlot.setCullFace(CullFace.BACK);
            } else {
                hypersurface.surfPlot.setCullFace(CullFace.NONE);
            }
        });
        HBox cullFaceHBox = new HBox(10, cullFaceFront, cullFaceBack, cullFaceNone);

        //add a Point Light for better viewing of the grid coordinate system
        hypersurface.pointLight.getScope().addAll(hypersurface.surfPlot);
        hypersurface.sceneRoot.getChildren().add(hypersurface.pointLight);
        hypersurface.pointLight.translateXProperty().bind(hypersurface.camera.translateXProperty());
        hypersurface.pointLight.translateYProperty().bind(hypersurface.camera.translateYProperty());
        hypersurface.pointLight.translateZProperty().bind(hypersurface.camera.translateZProperty().add(500));

        hypersurface.ambientLight.getScope().addAll(hypersurface.surfPlot);
        hypersurface.sceneRoot.getChildren().add(hypersurface.ambientLight);

        ColorPicker lightPicker = new ColorPicker(Color.WHITE);
        hypersurface.ambientLight.colorProperty().bind(lightPicker.valueProperty());

        ColorPicker specPicker = new ColorPicker(Color.CYAN);
        specPicker.setOnAction(e -> {
            ((PhongMaterial)hypersurface.surfPlot.getMaterial()).setSpecularColor(specPicker.getValue());
        });
        
        CheckBox enableAmbient = new CheckBox("Enable Ambient Light");
        enableAmbient.setSelected(true);
        enableAmbient.setOnAction(e -> {
            if (enableAmbient.isSelected()) {
                lightPicker.setDisable(false);
                hypersurface.ambientLight.getScope().addAll(hypersurface.surfPlot);
            } else {
                lightPicker.setDisable(true);
                hypersurface.ambientLight.getScope().clear();
            }
        });

        CheckBox enablePoint = new CheckBox("Enable Point Light");
        enablePoint.setSelected(true);
        enablePoint.setOnAction(e -> {
            if (enablePoint.isSelected()) {
                specPicker.setDisable(false);
                hypersurface.pointLight.getScope().addAll(hypersurface.surfPlot);
            } else {
                specPicker.setDisable(true);
                hypersurface.pointLight.getScope().clear();
            }
        });

        Label divLabel = new Label("Divisions");
        divLabel.setPrefWidth(125);
        Label xWidthLabel = new Label("Usable X Width");
        xWidthLabel.setPrefWidth(125);
        Label zWidthLabel = new Label("Usable Z Length");
        zWidthLabel.setPrefWidth(125);
        Label yScaleLabel = new Label("Y Scale");
        yScaleLabel.setPrefWidth(125);
        Label surfScaleLabel = new Label("Surface Range Scale");
        surfScaleLabel.setPrefWidth(125);

        VBox vbox = new VBox(10,
            new HBox(10, xWidthLabel, xWidthSpinner),
            new HBox(10, zWidthLabel, zWidthSpinner),
            new HBox(10, yScaleLabel, yScaleSpinner),
            new HBox(10, surfScaleLabel, surfScaleSpinner),
            new Label("Draw Mode"),
            meshTypeHBox,
            drawModeHBox,
            new Label("Cull Face"),
            cullFaceHBox,
            new Label("Ambient Light Color"),
            enableAmbient,
            lightPicker,
//            new Label("Diffuse Color"),
//            diffusePicker,
            new Label("Specular Color"),
            enablePoint,
            specPicker
        );
        StackPane.setAlignment(vbox, Pos.BOTTOM_LEFT);
        vbox.setPickOnBounds(false);
        getChildren().add(vbox);        
    }
}
