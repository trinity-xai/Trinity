package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/** Projection-aware grid settings embedded directly inside the Projection tab. */
public final class ContextGraphSceneReferenceControls extends VBox {
    private static final double LABEL_WIDTH = 215.0;
    private static final double INPUT_WIDTH = 150.0;

    public ContextGraphSceneReferenceControls(ContextGraphReferenceGrid3D references,
                                              Runnable refreshCartesianBounds) {
        super(0);
        // No external padding: this TitledPane must align with Graph Layout Settings.
        setFillWidth(true);
        setMinWidth(0);
        setMaxWidth(Double.MAX_VALUE);

        Grid3D cartesian = references.getCartesianGrid();
        PoincareGrid3D poincare = references.getPoincareGrid();

        CheckBox visible = new CheckBox("Show Reference Grid");
        visible.selectedProperty().bindBidirectional(references.visibleProperty());

        ComboBox<Grid3D.Style> style = new ComboBox<>();
        style.getItems().setAll(Grid3D.Style.values());
        style.setValue(cartesian.getMeshStyle());
        style.valueProperty().bindBidirectional(cartesian.meshStyleProperty());
        style.disableProperty().bind(references.hyperbolicProperty());
        sizeInput(style);

        // Cartesian and Poincare grids use the same major/minor RGBA styling.
        cartesian.lineColorProperty().addListener((obs, oldColor, color) ->
            poincare.setMinorLineColor(color));
        cartesian.majorLineColorProperty().addListener((obs, oldColor, color) ->
            poincare.setMajorLineColor(color));
        ColorPicker minorColor = colorPicker(cartesian.lineColorProperty());
        ColorPicker majorColor = colorPicker(cartesian.majorLineColorProperty());
        ColorPicker checkA = colorPicker(cartesian.checkAProperty());
        ColorPicker checkB = colorPicker(cartesian.checkBProperty());

        Spinner<Double> width = doubleSpinner(cartesian.widthProperty(), 0.01, 1_000_000_000.0, 10.0);
        Spinner<Double> length = doubleSpinner(cartesian.lengthProperty(), 0.01, 1_000_000_000.0, 10.0);
        CheckBox autoFit = new CheckBox("Auto-fit grid to graph data");
        autoFit.selectedProperty().bindBidirectional(references.autoFitCartesianProperty());
        width.disableProperty().bind(autoFit.selectedProperty());
        length.disableProperty().bind(autoFit.selectedProperty());
        autoFit.setOnAction(e -> {
            if (autoFit.isSelected() && refreshCartesianBounds != null) refreshCartesianBounds.run();
        });

        VBox cartesianSettings = new VBox(6,
            new Label("Cartesian grid"), autoFit,
            labelled("Width", width),
            labelled("Length", length),
            labelled("X divisions", intSpinner(cartesian.xDivsProperty(), 1, 256, 1)),
            labelled("Z divisions", intSpinner(cartesian.zDivsProperty(), 1, 256, 1)),
            labelled("Major every", intSpinner(cartesian.majorEveryProperty(), 1, 1000, 1)),
            labelled("Checkerboard A", checkA),
            labelled("Checker A opacity (%)", opacitySpinner(cartesian.checkAProperty())),
            labelled("Checkerboard B", checkB),
            labelled("Checker B opacity (%)", opacitySpinner(cartesian.checkBProperty())));
        cartesianSettings.visibleProperty().bind(references.hyperbolicProperty().not());
        cartesianSettings.managedProperty().bind(cartesianSettings.visibleProperty());

        VBox hyperbolicSettings = new VBox(6,
            new Label("Poincare disk (XZ cross-section)"),
            labelled("Hyperbolic ring spacing", doubleSpinner(poincare.ringStepProperty(), 0.01, 100.0, 0.1)),
            labelled("Ring count", intSpinner(poincare.ringCountProperty(), 1, 64, 1)),
            labelled("Radial spokes", intSpinner(poincare.spokeCountProperty(), 0, 128, 1)),
            labelled("Major every", intSpinner(poincare.majorEveryProperty(), 1, 64, 1)));
        hyperbolicSettings.visibleProperty().bind(references.hyperbolicProperty());
        hyperbolicSettings.managedProperty().bind(hyperbolicSettings.visibleProperty());

        VBox advancedContent = new VBox(7,
            visible,
            labelled("Grid style (Cartesian)", style),
            labelled("Minor lines", minorColor),
            labelled("Minor line opacity (%)", opacitySpinner(cartesian.lineColorProperty())),
            labelled("Major lines", majorColor),
            labelled("Major line opacity (%)", opacitySpinner(cartesian.majorLineColorProperty())),
            cartesianSettings,
            hyperbolicSettings);
        advancedContent.setPadding(new Insets(8));
        advancedContent.setFillWidth(true);
        advancedContent.setMinWidth(0);
        advancedContent.setMaxWidth(Double.MAX_VALUE);

        TitledPane advanced = new TitledPane("Advanced Grid Settings", advancedContent);
        advanced.setExpanded(false);
        advanced.setMinWidth(0);
        advanced.setMaxWidth(Double.MAX_VALUE);
        getChildren().add(advanced);
    }

    private static ColorPicker colorPicker(ObjectProperty<Color> property) {
        ColorPicker picker = new ColorPicker(property.get());
        picker.valueProperty().bindBidirectional(property);
        sizeInput(picker);
        return picker;
    }

    private static Spinner<Integer> opacitySpinner(ObjectProperty<Color> property) {
        Color initial = property.get();
        Spinner<Integer> spinner = new Spinner<>(0, 100,
            initial == null ? 0 : (int) Math.round(initial.getOpacity() * 100), 1);
        spinner.setEditable(true);
        sizeInput(spinner);
        spinner.valueProperty().addListener((obs, oldValue, value) -> {
            if (value == null) return;
            Color current = property.get();
            if (current == null) return;
            double alpha = value / 100.0;
            if (Math.abs(current.getOpacity() - alpha) > 0.00001)
                property.set(new Color(current.getRed(), current.getGreen(), current.getBlue(), alpha));
        });
        property.addListener((obs, oldColor, color) -> {
            int percent = color == null ? 0 : (int) Math.round(color.getOpacity() * 100);
            if (spinner.getValue() == null || spinner.getValue() != percent)
                spinner.getValueFactory().setValue(percent);
        });
        installCommit(spinner);
        return spinner;
    }

    private static Spinner<Integer> intSpinner(IntegerProperty property, int min, int max, int step) {
        Spinner<Integer> spinner = new Spinner<>(min, max, Math.max(min, Math.min(max, property.get())), step);
        spinner.setEditable(true);
        sizeInput(spinner);
        spinner.valueProperty().addListener((obs, oldValue, value) -> {
            if (value != null && value != property.get()) property.set(value);
        });
        property.addListener((obs, oldValue, value) -> {
            int clamped = Math.max(min, Math.min(max, value.intValue()));
            if (spinner.getValue() == null || spinner.getValue() != clamped)
                spinner.getValueFactory().setValue(clamped);
        });
        installCommit(spinner);
        return spinner;
    }

    private static Spinner<Double> doubleSpinner(DoubleProperty property,
                                                  double min, double max, double step) {
        double initial = Math.max(min, Math.min(max, property.get()));
        Spinner<Double> spinner = new Spinner<>(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, initial, step));
        spinner.setEditable(true);
        sizeInput(spinner);
        spinner.valueProperty().addListener((obs, oldValue, value) -> {
            if (value != null && Double.isFinite(value) && Double.compare(property.get(), value) != 0)
                property.set(value);
        });
        property.addListener((obs, oldValue, value) -> {
            double next = value.doubleValue();
            if (Double.isFinite(next) && next >= min && next <= max &&
                (spinner.getValue() == null || Double.compare(spinner.getValue(), next) != 0))
                spinner.getValueFactory().setValue(next);
        });
        installCommit(spinner);
        return spinner;
    }

    /** Only commit valid typed values; restore the model value on invalid entry. */
    private static <T> void installCommit(Spinner<T> spinner) {
        Runnable commit = () -> {
            SpinnerValueFactory<T> factory = spinner.getValueFactory();
            try {
                T parsed = factory.getConverter().fromString(spinner.getEditor().getText());
                if (parsed == null) throw new IllegalArgumentException("Empty spinner value");
                if (parsed instanceof Double d && !Double.isFinite(d))
                    throw new IllegalArgumentException("Non-finite spinner value");
                if (factory instanceof SpinnerValueFactory.DoubleSpinnerValueFactory doubles &&
                    parsed instanceof Double d && (d < doubles.getMin() || d > doubles.getMax()))
                    throw new IllegalArgumentException("Out of range");
                if (factory instanceof SpinnerValueFactory.IntegerSpinnerValueFactory integers &&
                    parsed instanceof Integer i && (i < integers.getMin() || i > integers.getMax()))
                    throw new IllegalArgumentException("Out of range");
                factory.setValue(parsed);
            } catch (RuntimeException invalid) {
                // Retain the last accepted value when the editor cannot be parsed.
            }
            spinner.getEditor().setText(factory.getConverter().toString(factory.getValue()));
        };
        spinner.getEditor().setOnAction(e -> commit.run());
        spinner.getEditor().focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) commit.run();
        });
    }

    private static HBox labelled(String label, Node field) {
        Label title = new Label(label);
        title.setPrefWidth(LABEL_WIDTH);
        title.setMinWidth(0);
        title.setMaxWidth(LABEL_WIDTH);
        HBox row = new HBox(8, title, field);
        row.setFillHeight(true);
        row.setMinWidth(0);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private static void sizeInput(javafx.scene.control.Control control) {
        control.setMinWidth(0);
        control.setPrefWidth(INPUT_WIDTH);
        control.setMaxWidth(INPUT_WIDTH);
    }
}
