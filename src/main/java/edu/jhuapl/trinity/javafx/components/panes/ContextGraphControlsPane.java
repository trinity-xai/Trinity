package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.GraphControlsView;
import edu.jhuapl.trinity.javafx.components.GraphStyleControlsView;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphController;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TabPane;

import javafx.scene.control.Slider;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/** Dedicated floating controls for the Context Graph view. */
public final class ContextGraphControlsPane extends LitPathPane {
    public ContextGraphControlsPane(Scene scene, Pane parent, ContextGraphController graphController) {
        super(scene, parent, 420, 620, new BorderPane(), "Context Graph Controls", "", 200.0, 300.0);
        setPickOnBounds(false);
        setFocusTraversable(false);
        Slider coordinateScale = new Slider(0.1, 20.0, graphController.getCoordinateScale());
        coordinateScale.setShowTickLabels(true);
        coordinateScale.setShowTickMarks(true);
        coordinateScale.valueProperty().addListener((obs, oldValue, newValue) -> graphController.setCoordinateScale(newValue.doubleValue()));
        coordinateScale.valueChangingProperty().addListener((obs, was, now) -> { if (!now) graphController.finishCoordinateScale(); });
        coordinateScale.setOnMouseReleased(e -> graphController.finishCoordinateScale());
        VBox styleContent = new VBox(10, new Label("Coordinate Scale (positions only)"),
            coordinateScale, new GraphStyleControlsView(scene));
        styleContent.setPadding(new Insets(6));
        CheckBox showNodes = new CheckBox("Show Nodes");
        showNodes.setSelected(graphController.isNodesDisplayed());
        showNodes.setOnAction(e -> graphController.setNodesDisplayed(showNodes.isSelected()));
        CheckBox showEdges = new CheckBox("Show Edges");
        showEdges.setSelected(graphController.isEdgesDisplayed());
        showEdges.setOnAction(e -> graphController.setEdgesDisplayed(showEdges.isSelected()));
        VBox displayContent = new VBox(12, showNodes, showEdges);
        displayContent.setPadding(new Insets(12));
        TabPane tabs = new TabPane(
            new Tab("Graph Layout", new GraphControlsView(scene)),
            new Tab("Graph Style", styleContent),
            new Tab("Display", displayContent));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        ((BorderPane) contentPane).setCenter(tabs);
    }
}
