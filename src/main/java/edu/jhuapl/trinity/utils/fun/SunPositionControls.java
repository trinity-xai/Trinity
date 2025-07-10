package edu.jhuapl.trinity.utils.fun;

import edu.jhuapl.trinity.javafx.events.EffectEvent;
import static edu.jhuapl.trinity.javafx.events.EffectEvent.SUN_POSITION_ARCHEIGHT;
import static edu.jhuapl.trinity.javafx.events.EffectEvent.SUN_POSITION_ARCWIDTH;
import static edu.jhuapl.trinity.javafx.events.EffectEvent.SUN_POSITION_VELOCITY;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;

/**
 *
 * @author Sean Phillips
 */
public class SunPositionControls extends VBox {
    private final DoubleProperty arcWidth = new SimpleDoubleProperty(0.5);
    private final DoubleProperty arcHeight = new SimpleDoubleProperty(0.3);
    private final DoubleProperty velocityHz = new SimpleDoubleProperty(0.1); // Cycles/sec
    
    public SunPositionControls() {
        setSpacing(10);
        setPadding(new Insets(10));
        VBox widthSlider = createSliderBox("Arc Width", SUN_POSITION_ARCWIDTH, arcWidth, 0.1, 1);
        VBox heightSlider = createSliderBox("Arc Height", SUN_POSITION_ARCHEIGHT, arcHeight, 0.1, 1);
        VBox velocitySlider = createSliderBox("Velocity (Hz)", SUN_POSITION_VELOCITY, velocityHz, 0.01, 1);

        ToggleButton startStopBtn = new ToggleButton("Enable Animation");
        startStopBtn.setOnAction(e -> {
            getScene().getRoot().fireEvent(new EffectEvent(
                EffectEvent.SUN_POSITION_ANIMATING, startStopBtn.isSelected()));
        });
        getChildren().addAll(
            new Label("Sun Arc Animation"),
            widthSlider,
            heightSlider,
            velocitySlider,
            startStopBtn
        );        
    }
    private VBox createSliderBox(String name, EventType<EffectEvent> eventType, DoubleProperty boundValue, double min, double max) {
        Label label = new Label(name);
        Slider slider = new Slider(min, max, boundValue.get());
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMinorTickCount(1);
        slider.setMajorTickUnit((max - min) / 10);
        slider.setBlockIncrement((max - min) / 20.0);
        slider.valueProperty().bindBidirectional(boundValue);
        slider.valueProperty().addListener(e-> {
            slider.getScene().getRoot().fireEvent(
                new EffectEvent(eventType, slider.getValue()));
        });
        VBox container = new VBox(2, label, slider);
        container.setPadding(new Insets(4, 0, 4, 0));
        return container;
    }        
}