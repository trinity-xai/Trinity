package edu.jhuapl.trinity.javafx.javafx3d.animated;

import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.utils.Utils;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * @author Sean Phillips
 */
public class RadialGridControlBox extends VBox {
    private static final double CONTROL_PREF_WIDTH = 100;
    private static final int NUM_CIRCLES = 5;
    private static final int NUM_RADIAL_LINES = 20;
    private static final double MAX_RADIUS = 1000;
    private static final double LINE_RADIUS = 0.5;
    private static final double CIRCLE_SEGMENTS = 60;
    RadialGrid radialGrid;
    Spinner<Integer> numCirclesSpinner;
    Spinner<Integer> numLinesSpinner;
    Spinner<Double> maxRadiusSpinner;
    Spinner<Double> lineRadiusSpinner;
    Spinner<Double> circleSegmentsSpinner;

    public RadialGridControlBox(RadialGrid radialGrid) {
        this.radialGrid = radialGrid;
        ToggleButton animateToggle = new ToggleButton("Animation");
        animateToggle.setPrefWidth(CONTROL_PREF_WIDTH * 2);
        animateToggle.setOnAction(e ->
            radialGrid.setEnableAnimation(animateToggle.isSelected()));
        ToggleButton pulseToggle = new ToggleButton("Pulse");
        pulseToggle.setPrefWidth(CONTROL_PREF_WIDTH * 2);
        pulseToggle.setOnAction(e ->
            radialGrid.setEnablePulsation(pulseToggle.isSelected()));
        ToggleButton rotateToggle = new ToggleButton("Rotate");
        rotateToggle.setPrefWidth(CONTROL_PREF_WIDTH * 2);
        rotateToggle.setOnAction(e -> {
            radialGrid.setEnableRotation(rotateToggle.isSelected());
            radialGrid.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.CAMERA_ORBIT_MODE, 
                    rotateToggle.isSelected()));
        });

        numCirclesSpinner = new Spinner(2, 200, NUM_CIRCLES, 1);
        numCirclesSpinner.valueProperty().addListener(e -> regenerate());
        numCirclesSpinner.setPrefWidth(CONTROL_PREF_WIDTH);

        numLinesSpinner = new Spinner(2, 200, NUM_RADIAL_LINES, 1);
        numLinesSpinner.valueProperty().addListener(e -> regenerate());
        numLinesSpinner.setPrefWidth(CONTROL_PREF_WIDTH);

        maxRadiusSpinner = new Spinner(1, 10000, MAX_RADIUS, 100);
        maxRadiusSpinner.valueProperty().addListener(e -> regenerate());
        maxRadiusSpinner.setPrefWidth(CONTROL_PREF_WIDTH);

        lineRadiusSpinner = new Spinner(0.1, 100, LINE_RADIUS, 0.1);
        lineRadiusSpinner.valueProperty().addListener(e -> {
            this.radialGrid.setBaseRadius(lineRadiusSpinner.getValue());
            regenerate();
        });
        lineRadiusSpinner.setPrefWidth(CONTROL_PREF_WIDTH);

        circleSegmentsSpinner = new Spinner(1, 360, CIRCLE_SEGMENTS, 1);
        circleSegmentsSpinner.valueProperty().addListener(e -> regenerate());
        circleSegmentsSpinner.setPrefWidth(CONTROL_PREF_WIDTH);

        Button generateButton = new Button("Regenerate");
        generateButton.setPrefWidth(CONTROL_PREF_WIDTH * 2);
        generateButton.setOnAction(e -> regenerate());

        ColorPicker diffuseColorPicker = new ColorPicker(Color.DEEPSKYBLUE);
        diffuseColorPicker.valueProperty().bindBidirectional(radialGrid.gridMaterial.diffuseColorProperty());
        ColorPicker specularColorPicker = new ColorPicker(Color.LIGHTCYAN);
        specularColorPicker.valueProperty().bindBidirectional(radialGrid.gridMaterial.specularColorProperty());

        getChildren().addAll(
            animateToggle,
            pulseToggle,
            rotateToggle,
            new Label("Number of Circles"),
            numCirclesSpinner,
            new Label("Number of Lines"),
            numLinesSpinner,
            new Label("Max Radius"),
            maxRadiusSpinner,
            new Label("Line Radius"),
            lineRadiusSpinner,
            new Label("Circle Segment Arc"),
            circleSegmentsSpinner,
            generateButton,
            new Label("Diffuse Color"),
            diffuseColorPicker,
            new Label("Specular Color"),
            specularColorPicker
        );

        setSpacing(10);
    }

    private void regenerate() {
        long startTime = System.nanoTime();
        radialGrid.regenerate(numCirclesSpinner.getValue(),
            numLinesSpinner.getValue(), maxRadiusSpinner.getValue(),
            lineRadiusSpinner.getValue(), circleSegmentsSpinner.getValue());
        Utils.printTotalTime(startTime);
    }
}
