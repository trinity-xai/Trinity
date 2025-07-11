package edu.jhuapl.trinity.utils.fun.planetary;

import edu.jhuapl.trinity.utils.fun.planetary.PlanetaryEffectFactory.PlanetStyle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import java.util.function.Consumer;
/**
 *
 * @author Sean Phillips
 */
public class PlanetaryDiscControls extends VBox {

    private final ObjectProperty<PlanetStyle> selectedStyle = new SimpleObjectProperty<>(PlanetStyle.RETROWAVE);
    private final DoubleProperty discRadius = new SimpleDoubleProperty(200);
    private final DoubleProperty verticalOffset = new SimpleDoubleProperty(0);
    private final DoubleProperty horizontalOffset = new SimpleDoubleProperty(0);
    private final BooleanProperty discVisible = new SimpleBooleanProperty(true);
    private final BooleanProperty debugOccluder = new SimpleBooleanProperty(false);

    public PlanetaryDiscControls(
            Consumer<PlanetStyle> styleChanged,
            Consumer<Double> diameterChanged,
            Consumer<Double> verticalOffsetChanged,
            Consumer<Double> horizontalOffsetChanged,
            Consumer<Boolean> visibilityChanged,
            Consumer<Boolean> debugChanged
    ) {
        setSpacing(10);
        setPadding(new Insets(10));

        // Style dropdown
        ComboBox<PlanetStyle> styleBox = new ComboBox<>(FXCollections.observableArrayList(PlanetStyle.values()));
        styleBox.setValue(selectedStyle.get());
        styleBox.valueProperty().bindBidirectional(selectedStyle);
        selectedStyle.addListener((obs, old, val) -> styleChanged.accept(val));

        // Radius slider
        VBox radiusBox = createSlider("Disc Radius", 100, 1000, discRadius, diameterChanged);

        // Vertical offset slider
        VBox vOffsetBox = createSlider("Vertical Offset", -1000, 1000, verticalOffset, verticalOffsetChanged);

        // Horizontal offset slider
        VBox hOffsetBox = createSlider("Horizontal Offset", -1000, 1000, horizontalOffset, horizontalOffsetChanged);

        // Visibility checkbox
        CheckBox visibleBox = new CheckBox("Visible");
        visibleBox.setSelected(true);
        visibleBox.selectedProperty().bindBidirectional(discVisible);
        discVisible.addListener((obs, old, val) -> visibilityChanged.accept(val));

        // Debug checkbox
        CheckBox debugBox = new CheckBox("Show Occluder Shape");
        debugBox.setSelected(false);
        debugBox.selectedProperty().bindBidirectional(debugOccluder);
        debugOccluder.addListener((obs, old, val) -> debugChanged.accept(val));

        getChildren().addAll(
            new Label("Planetary Disc Controls"),
            new HBox(10, new Label("Style:"), styleBox),
            radiusBox,
            vOffsetBox,
            hOffsetBox,
            visibleBox,
            debugBox
        );
    }

    private VBox createSlider(String labelText, double min, double max, DoubleProperty prop, Consumer<Double> onChange) {
        Label label = new Label(labelText);
        Slider slider = new Slider(min, max, prop.get());
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
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
}
