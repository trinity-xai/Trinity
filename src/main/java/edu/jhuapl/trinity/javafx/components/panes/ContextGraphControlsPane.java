package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.GraphControlsView;
import edu.jhuapl.trinity.javafx.components.GraphStyleControlsView;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphController;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraph3DPane;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.Locale;

/** Dedicated floating controls for the Context Graph view. */
public final class ContextGraphControlsPane extends LitPathPane {
    public ContextGraphControlsPane(Scene scene, Pane parent, ContextGraphController graphController,
                                    ContextGraph3DPane graphPane) {
        super(scene, parent, 420, 620, new BorderPane(), "Context Graph Controls", "", 200.0, 300.0);
        setPickOnBounds(false);
        setFocusTraversable(false);

        Spinner<Double> coordinateScale = new Spinner<>();
        SpinnerValueFactory.DoubleSpinnerValueFactory coordinateValues =
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 100.0, graphController.getCoordinateScale(), 0.1);
        coordinateValues.setConverter(new StringConverter<>() {
            @Override public String toString(Double value) {
                return value == null ? "" : String.format(Locale.ROOT, "%.1f", value);
            }
            @Override public Double fromString(String text) {
                double value = Double.parseDouble(text.trim());
                if (!Double.isFinite(value) || value < 0.1 || value > 100.0) {
                    throw new IllegalArgumentException("Coordinate scale must be between 0.1 and 100.0");
                }
                return value;
            }
        });
        coordinateScale.setValueFactory(coordinateValues);
        coordinateScale.setEditable(true);
        coordinateScale.setPrefWidth(130);
        coordinateScale.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                graphController.setCoordinateScale(newValue);
                graphController.finishCoordinateScale();
            }
        });
        installCommitOnFocusLost(coordinateScale);

        VBox styleContent = new VBox(10, new Label("Coordinate Scale (positions only)"),
            coordinateScale, new GraphStyleControlsView(scene));
        styleContent.setPadding(new Insets(6));

        CheckBox showNodes = new CheckBox("Show Nodes");
        showNodes.setSelected(graphController.isNodesDisplayed());
        showNodes.setOnAction(e -> graphController.setNodesDisplayed(showNodes.isSelected()));
        CheckBox showEdges = new CheckBox("Show Edges");
        showEdges.setSelected(graphController.isEdgesDisplayed());
        showEdges.setOnAction(e -> graphController.setEdgesDisplayed(showEdges.isSelected()));

        Spinner<Integer> edgeBatchSize = new Spinner<>(1, 1000, graphController.getEdgeBatchSize(), 10);
        edgeBatchSize.setEditable(true);
        edgeBatchSize.setPrefWidth(130);
        edgeBatchSize.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) graphController.setEdgeBatchSize(newValue);
        });
        installCommitOnFocusLost(edgeBatchSize);

        Spinner<Double> fitPercentage = new Spinner<>(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(10.0, 100.0,
                graphPane.getFitGraphPercentage(), 1.0));
        fitPercentage.setEditable(true);
        fitPercentage.setPrefWidth(130);
        fitPercentage.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) graphPane.setFitGraphPercentage(newValue);
        });
        installCommitOnFocusLost(fitPercentage);

        VBox displayContent = new VBox(12, showNodes, showEdges,
            new Label("Edges per rendering pulse"), edgeBatchSize,
            new Label("Fit Graph Percentage"), fitPercentage);
        displayContent.setPadding(new Insets(12));
        TabPane tabs = new TabPane(
            new Tab("Graph Layout", new GraphControlsView(scene)),
            new Tab("Graph Style", styleContent),
            new Tab("Display", displayContent));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        ((BorderPane) contentPane).setCenter(tabs);
    }

    /** Commit typed spinner values on Enter or focus loss; revert invalid entries. */
    private static <T> void installCommitOnFocusLost(Spinner<T> spinner) {
        Runnable commit = () -> {
            SpinnerValueFactory<T> factory = spinner.getValueFactory();
            try {
                T parsed = factory.getConverter().fromString(spinner.getEditor().getText());
                factory.setValue(parsed);
            } catch (RuntimeException invalid) {
                spinner.getEditor().setText(factory.getConverter().toString(factory.getValue()));
            }
        };
        spinner.getEditor().setOnAction(e -> commit.run());
        spinner.getEditor().focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) commit.run();
        });
    }
}
