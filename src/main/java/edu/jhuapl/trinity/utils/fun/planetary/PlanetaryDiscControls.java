package edu.jhuapl.trinity.utils.fun.planetary;

import edu.jhuapl.trinity.utils.fun.planetary.PlanetaryEffectFactory.PlanetStyle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import java.util.function.Consumer;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Separator;
import javafx.scene.paint.Color;
/**
 *
 * @author Sean Phillips
 */
public class PlanetaryDiscControls extends VBox {

    private final ObjectProperty<PlanetStyle> selectedStyle = new SimpleObjectProperty<>(PlanetStyle.RETROWAVE);
    private final DoubleProperty discRadius = new SimpleDoubleProperty(200);
    private final DoubleProperty verticalOffset = new SimpleDoubleProperty(0);
    private final DoubleProperty horizontalOffset = new SimpleDoubleProperty(0);
    private final BooleanProperty discVisible = new SimpleBooleanProperty(false);
    private final BooleanProperty debugOccluder = new SimpleBooleanProperty(false);
    private final DoubleProperty scatteringBlurRadius = new SimpleDoubleProperty(20);
    private final BooleanProperty scatteringEnabled = new SimpleBooleanProperty(true);
    private final ObjectProperty<Color> scatteringColor = new SimpleObjectProperty<>(Color.CYAN);
    private final DoubleProperty shadowIntensity = new SimpleDoubleProperty(0.5);

    public PlanetaryDiscControls(
        Consumer<Double> radiusChanged,
        Consumer<Double> verticalOffsetChanged,
        Consumer<Double> horizontalOffsetChanged,
        Consumer<Boolean> visibleChanged,
        Consumer<Boolean> debugChanged,
        Consumer<Boolean> scatteringToggled,
        Consumer<Color> scatteringColorChanged,
        Consumer<Double> shadowChanged,
        Consumer<Double> blurRadiusChanged
    ) {
        setSpacing(10);
        setPadding(new Insets(10));

        // Radius slider
        VBox radiusBox = createSlider("Disc Radius", 100, 1000, discRadius, radiusChanged);

        // Vertical offset
        VBox vOffsetBox = createSlider("Vertical Offset", -1000, 1000, verticalOffset, verticalOffsetChanged);

        // Horizontal offset
        VBox hOffsetBox = createSlider("Horizontal Offset", -2000, 2000, horizontalOffset, horizontalOffsetChanged);

        // Visibility checkbox
        CheckBox visibleBox = new CheckBox("Visible");
        visibleBox.setSelected(discVisible.get());
        visibleBox.selectedProperty().bindBidirectional(discVisible);
        discVisible.addListener((obs, old, val) -> visibleChanged.accept(val));

        // Debug checkbox
        CheckBox debugBox = new CheckBox("Show Occluder Shape");
        debugBox.setSelected(debugOccluder.get());
        debugBox.selectedProperty().bindBidirectional(debugOccluder);
        debugOccluder.addListener((obs, old, val) -> debugChanged.accept(val));

        // Atmospheric scattering toggle
        CheckBox scatterToggle = new CheckBox("Enable Atmospheric Scattering");
        scatterToggle.setSelected(scatteringEnabled.get());
        scatterToggle.selectedProperty().bindBidirectional(scatteringEnabled);
        scatteringEnabled.addListener((obs, old, val) -> scatteringToggled.accept(val));

        // Scattering color picker
        ColorPicker scatterColorPicker = new ColorPicker(scatteringColor.get());
        scatteringColor.bind(scatterColorPicker.valueProperty());
        scatteringColor.addListener((obs, old, val) -> scatteringColorChanged.accept(val));

        // Shadow intensity & blur radius sliders
        VBox shadowSlider = createSlider("Shadow Intensity", 0.0, 1.0, shadowIntensity, shadowChanged);
        VBox blurSlider = createSlider("Scattering Blur Radius", 0, 100, scatteringBlurRadius, blurRadiusChanged);

        getChildren().addAll(
            new Label("Planetary Disc Controls"),
            radiusBox,
            vOffsetBox,
            hOffsetBox,
            visibleBox,
            debugBox,
            new Separator(),
            scatterToggle,
            new HBox(10, new Label("Scattering Color:"), scatterColorPicker),
            blurSlider,
            shadowSlider
        );
    }

    private VBox createSlider(String labelText, double min, double max, DoubleProperty prop, Consumer<Double> onChange) {
        Label label = new Label(labelText);
        Slider slider = new Slider(min, max, prop.get());
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setSnapToTicks(true);
        slider.setBlockIncrement((max - min) / 20);
        slider.valueProperty().bindBidirectional(prop);
        slider.valueProperty().addListener((obs, old, val) -> onChange.accept(val.doubleValue()));
        return new VBox(2, label, slider);
    }

    // Exposed properties
    public ObjectProperty<PlanetStyle> selectedStyleProperty() { return selectedStyle; }
    public DoubleProperty discRadiusProperty() { return discRadius; }
    public DoubleProperty verticalOffsetProperty() { return verticalOffset; }
    public DoubleProperty horizontalOffsetProperty() { return horizontalOffset; }
    public BooleanProperty discVisibleProperty() { return discVisible; }
    public BooleanProperty debugOccluderProperty() { return debugOccluder; }
    public DoubleProperty scatteringBlurRadiusProperty() { return scatteringBlurRadius; }
    public BooleanProperty scatteringEnabledProperty() { return scatteringEnabled; }
    public ObjectProperty<Color> scatteringColorProperty() { return scatteringColor; }
    public DoubleProperty shadowIntensityProperty() { return shadowIntensity; }
}
