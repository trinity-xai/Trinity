package edu.jhuapl.trinity.javafx;

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
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.FloatMap;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

public class DisplacementTestApp extends Application {
    boolean animating = false;
    int width = 400;
    int height = 400;

    float xOffset = 0.0f;
    float yOffset = 0.0f;
    float xScale = 0.0f;
    float yScale = 0.0f;

    float xIncrement = 0.01f;
    float yIncrement = 0.01f;
    float sXIncrement = 0.01f;
    float sYIncrement = 0.01f;

    Spinner<Double> band0Spinner = new Spinner<>();
    Spinner<Double> band1Spinner = new Spinner<>();

    Spinner<Double> topUlxSpinner = new Spinner<>();
    Spinner<Double> topUlySpinner = new Spinner<>();
    Spinner<Double> topUrxSpinner = new Spinner<>();
    Spinner<Double> topUrySpinner = new Spinner<>();

    Spinner<Double> topLlxSpinner = new Spinner<>();
    Spinner<Double> topLlySpinner = new Spinner<>();
    Spinner<Double> topLrxSpinner = new Spinner<>();
    Spinner<Double> topLrySpinner = new Spinner<>();


    SimpleBooleanProperty wrapProp = new SimpleBooleanProperty(true);
    SimpleBooleanProperty canvasProp = new SimpleBooleanProperty(false);
    SimpleBooleanProperty topGridProp = new SimpleBooleanProperty(false);
    SimpleBooleanProperty bottomGridProp = new SimpleBooleanProperty(false);

    PerspectiveTransform topPT = new PerspectiveTransform();
    PerspectiveTransform bottomPT = new PerspectiveTransform();
    ImageView topGridImageView;
    ImageView bottomGridImageView;


    @Override
    public void start(Stage stage) throws IOException {
        band0Spinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(
                -1.0, 1.0, 0, 0.005));
        band0Spinner.valueProperty().addListener(c -> updateValues());

        band1Spinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(
                -1.0, 1.0, 0, 0.005));
        band1Spinner.valueProperty().addListener(c -> updateValues());

        //Upper Left
        topUlxSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(
                -1.0, 1.0, 0, 0.01));
        topUlxSpinner.valueProperty().addListener(c -> updateValues());
        topUlySpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(
                -1.0, 1.0, 0, 0.01));
        topUlySpinner.valueProperty().addListener(c -> updateValues());

        //Upper Right
        topUrxSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(
                0.0, 2.0, 1, 0.01));
        topUrxSpinner.valueProperty().addListener(c -> updateValues());
        topUrySpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(
                -1.0, 1.0, 0, 0.01));
        topUrySpinner.valueProperty().addListener(c -> updateValues());

        //Bottom Left
        topLlxSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(
                -1.0, 1.0, 0, 0.01));
        topLlxSpinner.valueProperty().addListener(c -> updateValues());
        topLlySpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(
                0, 2.0, 1, 0.01));
        topLlySpinner.valueProperty().addListener(c -> updateValues());

        //Bottom Right
        topLrxSpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(
                0.0, 2.0, 1, 0.01));
        topLrxSpinner.valueProperty().addListener(c -> updateValues());
        topLrySpinner.setValueFactory(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(
                0.0, 2.0, 1, 0.01));
        topLrySpinner.valueProperty().addListener(c -> updateValues());


        CheckBox wrapCheckBox = new CheckBox("Wrap");
        wrapProp.bind(wrapCheckBox.selectedProperty());
        wrapCheckBox.setSelected(true);
        CheckBox topGridCheckBox = new CheckBox("Top Grid");
        topGridProp.bind(topGridCheckBox.selectedProperty());
        CheckBox bottomGridCheckBox = new CheckBox("Bottom Grid");
        bottomGridProp.bind(bottomGridCheckBox.selectedProperty());
        CheckBox canvasCheckBox = new CheckBox("Canvas");
        canvasProp.bind(canvasCheckBox.selectedProperty());

        VBox controlsVBox = new VBox(5,
            wrapCheckBox,
//            topGridCheckBox,
//            bottomGridCheckBox,
//            canvasCheckBox,
            new Label("Band 0"),
            band0Spinner,
            new Label("Band 1"),
            band1Spinner,
            new Label("Top Grid ULX"),
            topUlxSpinner,
            new Label("Top Grid ULY"),
            topUlySpinner,
            new Label("Top Grid URX"),
            topUrxSpinner,
            new Label("Top Grid URY"),
            topUrySpinner,
            new Label("Top Grid LLX"),
            topLlxSpinner,
            new Label("Top Grid LLY"),
            topLlySpinner,
            new Label("Top Grid LRX"),
            topLrxSpinner,
            new Label("Top Grid LRY"),
            topLrySpinner

        );
        Canvas canvas = new Canvas(width, height);
        topGridImageView = new ImageView(ResourceUtils.load3DTextureImage("green-matte-trans-grid"));
        topGridImageView.setFitWidth(width);
        topGridImageView.setFitHeight(height);
        bottomGridImageView = new ImageView(ResourceUtils.load3DTextureImage("green-matte-trans-grid"));
        bottomGridImageView.setFitWidth(width);
        bottomGridImageView.setFitHeight(height);

        StackPane stackPane = new StackPane(topGridImageView, bottomGridImageView, canvas);

        Background imageBack = new Background(new BackgroundImage(ResourceUtils.load3DTextureImage("1500_blackgrid"),
            BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT));
        stackPane.setBackground(imageBack);
        BorderPane borderPane = new BorderPane(stackPane);
        borderPane.setLeft(controlsVBox);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.CYAN);
        gc.fillOval(175, 175, 50, 50);
        // Draw a green rectangle at the center with alpha 0.6
        gc.setFill(Color.GREEN.deriveColor(1, 1, 1, 0.6));
        gc.fillRect(190, 190, 20, 20);

        //effect for canvas
        FloatMap floatMap = new FloatMap();
        floatMap.setWidth(width);
        floatMap.setHeight(height);

        DisplacementMap displacementMap = new DisplacementMap();
        displacementMap.setMapData(floatMap);
        displacementMap.setWrap(true);
        displacementMap.wrapProperty().bind(wrapProp);

        canvas.setEffect(displacementMap);

        //perspective transforms
        topPT.setInput(displacementMap);
        topPT.setUlx(0);
        topPT.setUly(0);
        topPT.setUrx(topGridImageView.getFitWidth());
        topPT.setUry(0);

        topPT.setLrx(topGridImageView.getFitWidth());
        topPT.setLry(topGridImageView.getFitHeight());
        topPT.setLlx(0);
        topPT.setLly(topGridImageView.getFitHeight());
        topGridImageView.setEffect(topPT);

        bottomPT.setInput(displacementMap);
        bottomPT.setUlx(0);
        bottomPT.setUly(0);
        bottomPT.setUrx(bottomGridImageView.getFitWidth());
        bottomPT.setUry(0);

        bottomPT.setLrx(bottomGridImageView.getFitWidth());
        bottomPT.setLry(bottomGridImageView.getFitHeight());
        bottomPT.setLlx(0);
        bottomPT.setLly(bottomGridImageView.getFitHeight());
        bottomGridImageView.setEffect(topPT);
        bottomGridImageView.setEffect(bottomPT);

        Scene scene = new Scene(borderPane);
        scene.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> animating = !animating);

        stage.setScene(scene);
        stage.show();

        AnimationTimer cameraTimer = new AnimationTimer() {
            long last = 0;

            @Override
            public void handle(long now) {
                if ((now - last) > 30_000_000) {
                    if (animating) {
                        xOffset += xIncrement;
                        yOffset += yIncrement;
                        for (int i = 0; i < width; i++)
                            for (int j = 0; j < height; j++)
                                floatMap.setSamples(i, j, xOffset, yOffset);
                    }
                    last = now;
                }
            }
        };
        updateValues();
        cameraTimer.start();
    }

    public void updateValues() {
        //FloatMap Bands for DisplacementMap effect
//        0 = X offset distance
//        1 = Y offset distance
        xIncrement = band0Spinner.getValue().floatValue();
        yIncrement = band1Spinner.getValue().floatValue();

//the four corners of the imageview transform
        topPT.setUlx(topGridImageView.getFitWidth() * topUlxSpinner.getValue().floatValue());
        topPT.setUly(topGridImageView.getFitHeight() * topUlySpinner.getValue().floatValue());

        topPT.setUrx(topGridImageView.getFitWidth() * topUrxSpinner.getValue().floatValue());
        topPT.setUry(topGridImageView.getFitHeight() * topUrySpinner.getValue().floatValue());

        topPT.setLrx(topGridImageView.getFitWidth() * topLrxSpinner.getValue().floatValue());
        topPT.setLry(topGridImageView.getFitHeight() * topLrySpinner.getValue().floatValue());

        topPT.setLlx(topGridImageView.getFitHeight() * topLlxSpinner.getValue().floatValue());
        topPT.setLly(topGridImageView.getFitHeight() * topLlySpinner.getValue().floatValue());
//reflect it (negate Y's) for the bottom imageview
        bottomPT.setUlx(bottomGridImageView.getFitWidth() * topUlxSpinner.getValue().floatValue());
        bottomPT.setUly(bottomGridImageView.getFitHeight() * -topUlySpinner.getValue().floatValue());

        bottomPT.setUrx(bottomGridImageView.getFitWidth() * topUrxSpinner.getValue().floatValue());
        bottomPT.setUry(bottomGridImageView.getFitHeight() * -topUrySpinner.getValue().floatValue());

        bottomPT.setLrx(bottomGridImageView.getFitWidth() * topLrxSpinner.getValue().floatValue());
        bottomPT.setLry(bottomGridImageView.getFitHeight() * -topLrySpinner.getValue().floatValue());

        bottomPT.setLlx(bottomGridImageView.getFitHeight() * topLlxSpinner.getValue().floatValue());
        bottomPT.setLly(bottomGridImageView.getFitHeight() * -topLlySpinner.getValue().floatValue());


    }

    public static void main(String[] args) {
        launch(args);
    }
}
