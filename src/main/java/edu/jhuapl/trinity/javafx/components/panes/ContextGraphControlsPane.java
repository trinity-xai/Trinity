package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.GraphControlsView;
import edu.jhuapl.trinity.javafx.components.GraphStyleControlsView;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import javafx.scene.control.Button;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphController;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraph3DPane;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphProjectionMode;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphEdgeDisplayMode;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphEntitySearchResult;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.Locale;
import java.util.List;

/** Dedicated floating controls for the Context Graph view. */
public final class ContextGraphControlsPane extends LitPathPane {
    public static final String CONTROLS_LAYOUT_VERSION = "CG-TABS-M4B-REORG-FIX-2026-11-12";

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
                graphPane.refreshPoincareBoundary();
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

        CheckBox highlightNeighborhood = new CheckBox("Highlight selected neighborhood");
        highlightNeighborhood.setSelected(graphController.isNeighborhoodHighlightEnabled());
        highlightNeighborhood.setOnAction(e ->
            graphController.setNeighborhoodHighlightEnabled(highlightNeighborhood.isSelected()));

        CheckBox dimOutsideNeighborhood = new CheckBox("Dim non-neighborhood entities");
        dimOutsideNeighborhood.setSelected(graphController.isDimOutsideNeighborhood());
        dimOutsideNeighborhood.setOnAction(e ->
            graphController.setDimOutsideNeighborhood(dimOutsideNeighborhood.isSelected()));

        Spinner<Integer> neighborhoodDepth = new Spinner<>(1, 10000, graphController.getNeighborhoodDepth(), 1);
        neighborhoodDepth.setEditable(true);
        neighborhoodDepth.setPrefWidth(130);
        neighborhoodDepth.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) graphController.setNeighborhoodDepth(newValue);
        });
        installCommitOnFocusLost(neighborhoodDepth);

        CheckBox isolateSelection = new CheckBox("Isolate selected neighborhood");
        isolateSelection.setSelected(graphController.isSelectionIsolated());
        isolateSelection.setOnAction(e -> graphController.setSelectionIsolated(isolateSelection.isSelected()));

        CheckBox selectionCallouts = new CheckBox("Animated selection callouts");
        selectionCallouts.setSelected(graphPane.isSelectionCalloutsEnabled());
        selectionCallouts.setOnAction(e -> graphPane.setSelectionCalloutsEnabled(selectionCallouts.isSelected()));

        Button focusSelection = new Button("Focus Selection");
        focusSelection.setOnAction(e -> graphPane.focusSelection());
        Button focusNeighborhood = new Button("Focus Neighborhood");
        focusNeighborhood.setOnAction(e -> graphPane.focusNeighborhood());
        Button showAll = new Button("Show All (Keep Label/Edge Filters)");
        showAll.setOnAction(e -> graphController.showAll());

        Label neighborhoodSummary = new Label();
        Runnable updateSummary = () -> neighborhoodSummary.setText(
            "Neighborhood: " + graphController.getNeighborhoodNodeCount() + " nodes / "
                + graphController.getNeighborhoodEdgeCount() + " edges");
        updateSummary.run();
        scene.addEventHandler(GraphEvent.GRAPH_NODE_CLICK, e -> updateSummary.run());
        scene.addEventHandler(GraphEvent.GRAPH_EDGE_CLICK, e -> updateSummary.run());
        scene.addEventHandler(GraphEvent.NEW_GRAPHDIRECTED_COLLECTION, e ->
            javafx.application.Platform.runLater(updateSummary));
        neighborhoodDepth.valueProperty().addListener((obs, oldValue, newValue) -> updateSummary.run());
        // Search operates on indexed GraphNode metadata, not rendered shape objects.
        TextField entityQuery = new TextField();
        entityQuery.setPromptText("Name, alias, ATT&CK ID, entity ID...");
        ListView<ContextGraphEntitySearchResult> entityResults = new ListView<>();
        entityResults.setPrefHeight(155);
        entityResults.setMinHeight(100);
        Label searchSummary = new Label("Type to find an entity");
        CheckBox focusFoundEntity = new CheckBox("Focus on selected entity");
        focusFoundEntity.setSelected(true);
        Button selectFoundEntity = new Button("Select Entity");
        selectFoundEntity.setDisable(true);
        Runnable updateSearch = () -> {
            List<ContextGraphEntitySearchResult> matches =
                graphController.findEntities(entityQuery.getText(), 50);
            entityResults.getItems().setAll(matches);
            searchSummary.setText(entityQuery.getText() == null || entityQuery.getText().isBlank()
                ? "Type to find an entity"
                : matches.isEmpty() ? "No matching entities"
                : matches.size() == 50 ? "Showing first 50 matches (refine your search)"
                : matches.size() + " matching entities");
        };
        entityQuery.textProperty().addListener((obs, oldText, newText) -> updateSearch.run());
        entityResults.getSelectionModel().selectedItemProperty().addListener((obs, oldResult, result) ->
            selectFoundEntity.setDisable(result == null));
        Runnable selectMatch = () -> {
            ContextGraphEntitySearchResult chosen = entityResults.getSelectionModel().getSelectedItem();
            if (chosen == null) return;
            if (graphPane.selectEntity(chosen.entityId(), focusFoundEntity.isSelected())) {
                searchSummary.setText("Selected: " + chosen.displayName()
                    + (graphController.isNodeVisibleByLabel(chosen.entityId())
                        ? "" : " (hidden by category filter)"));
            }
        };
        selectFoundEntity.setOnAction(e -> selectMatch.run());
        entityQuery.setOnAction(e -> {
            if (entityResults.getSelectionModel().getSelectedItem() == null
                    && !entityResults.getItems().isEmpty()) {
                entityResults.getSelectionModel().selectFirst();
            }
            selectMatch.run();
        });
        entityResults.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == javafx.scene.input.MouseButton.PRIMARY) selectMatch.run();
        });
        scene.addEventHandler(GraphEvent.NEW_GRAPHDIRECTED_COLLECTION, e ->
            javafx.application.Platform.runLater(() -> {
                entityQuery.clear();
                updateSearch.run();
            }));
        VBox searchPanel = new VBox(7, new Label("Find and Select Entity"),
            entityQuery, searchSummary, entityResults,
            new HBox(8, selectFoundEntity, focusFoundEntity));
        // Visual edge disclosure is independent of source graph adjacency and traversal.
        ComboBox<ContextGraphEdgeDisplayMode> edgeDisclosureMode = new ComboBox<>();
        edgeDisclosureMode.getItems().addAll(ContextGraphEdgeDisplayMode.values());
        edgeDisclosureMode.setValue(graphController.getEdgeDisplayMode());
        edgeDisclosureMode.setMaxWidth(Double.MAX_VALUE);
        edgeDisclosureMode.setOnAction(e -> {
            if (edgeDisclosureMode.getValue() != graphController.getEdgeDisplayMode()) {
                graphController.setEdgeDisplayMode(edgeDisclosureMode.getValue());
            }
        });
        Spinner<Integer> crossLinkLimit = new Spinner<>(0, 10000,
            graphController.getFocusedCrossLinkLimit(), 25);
        crossLinkLimit.setEditable(true);
        crossLinkLimit.setPrefWidth(130);
        crossLinkLimit.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) graphController.setFocusedCrossLinkLimit(newValue);
        });
        installCommitOnFocusLost(crossLinkLimit);
        Label edgeVisibilitySummary = new Label();
        edgeVisibilitySummary.setWrapText(true);
        Runnable updateEdgeSummary = () -> {
            edgeDisclosureMode.setValue(graphController.getEdgeDisplayMode());
            crossLinkLimit.setDisable(
                graphController.getEdgeDisplayMode() != ContextGraphEdgeDisplayMode.BACKBONE_WITH_FOCUS);
            edgeVisibilitySummary.setText("Relationships passing filters: "
                + graphController.getVisibleRelationshipEdgeCount() + " / "
                + (graphController.getCurrentGraph() == null ? 0
                    : graphController.getCurrentGraph().getEdges().size())
                + " | backbone: " + (graphController.isBackboneReady()
                    ? graphController.getBackboneEdgeCount()
                    : graphController.isBackboneFailed() ? "unavailable" : "computing..."));
        };
        VBox relationshipCheckboxes = new VBox(5);
        Runnable rebuildRelationshipCheckboxes = () -> {
            relationshipCheckboxes.getChildren().clear();
            for (var entry : graphController.getRelationshipTypeCounts().entrySet()) {
                String relationship = entry.getKey();
                CheckBox check = new CheckBox(relationship + " (" + entry.getValue() + ")");
                check.setSelected(graphController.isRelationshipTypeEnabled(relationship));
                check.setOnAction(e -> graphController.setRelationshipTypeEnabled(
                    relationship, check.isSelected()));
                relationshipCheckboxes.getChildren().add(check);
            }
        };
        Button showAllRelationships = new Button("All types");
        showAllRelationships.setOnAction(e -> {
            graphController.setAllRelationshipTypesEnabled(true);
            rebuildRelationshipCheckboxes.run();
        });
        Button hideAllRelationships = new Button("None");
        hideAllRelationships.setOnAction(e -> {
            graphController.setAllRelationshipTypesEnabled(false);
            rebuildRelationshipCheckboxes.run();
        });
        ScrollPane relationshipScroll = new ScrollPane(relationshipCheckboxes);
        relationshipScroll.setFitToWidth(true);
        relationshipScroll.setPrefHeight(175);
        relationshipScroll.setMinHeight(100);
        relationshipScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox edgeDisclosurePanel = new VBox(7,
            new Label("Edge disclosure / hierarchy"), edgeDisclosureMode,
            new Label("Maximum focused cross-links"), crossLinkLimit,
            edgeVisibilitySummary, new Label("Visible relationship types"),
            new HBox(8, showAllRelationships, hideAllRelationships), relationshipScroll);
        rebuildRelationshipCheckboxes.run();
        updateEdgeSummary.run();
        scene.addEventHandler(GraphEvent.GRAPH_EXPLORATION_STATE_CHANGED, e -> updateEdgeSummary.run());
        scene.addEventHandler(GraphEvent.GRAPH_NODE_CLICK, e -> updateEdgeSummary.run());
        scene.addEventHandler(GraphEvent.GRAPH_EDGE_CLICK, e -> updateEdgeSummary.run());
        scene.addEventHandler(GraphEvent.NEW_GRAPHDIRECTED_COLLECTION, e ->
            javafx.application.Platform.runLater(() -> {
                rebuildRelationshipCheckboxes.run();
                updateEdgeSummary.run();
            }));

        VBox explorationContent = new VBox(12,
            highlightNeighborhood, dimOutsideNeighborhood,
            new Label("Neighborhood depth (graph hops)"), neighborhoodDepth,
            isolateSelection, selectionCallouts, neighborhoodSummary,
            focusSelection, focusNeighborhood, showAll);
        explorationContent.setPadding(new Insets(12));
        ScrollPane explorationScroll = new ScrollPane(explorationContent);
        explorationScroll.setFitToWidth(true);
        explorationScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scene.addEventHandler(GraphEvent.GRAPH_EXPLORATION_STATE_CHANGED, e -> {
            highlightNeighborhood.setSelected(graphController.isNeighborhoodHighlightEnabled());
            dimOutsideNeighborhood.setSelected(graphController.isDimOutsideNeighborhood());
            isolateSelection.setSelected(graphController.isSelectionIsolated());
            updateSummary.run();
        });

        ComboBox<ContextGraphProjectionMode> projection = new ComboBox<>();
        projection.getItems().addAll(ContextGraphProjectionMode.values());
        projection.setValue(graphController.getProjectionMode());
        projection.setMaxWidth(Double.MAX_VALUE);
        projection.setOnAction(e -> {
            if (projection.getValue() != graphController.getProjectionMode())
                graphController.setProjectionMode(projection.getValue());
        });
        Button recenter = new Button("Reset Hyperbolic Focus");
        recenter.setOnAction(e -> graphController.resetHyperbolicFocus());
        Button focusHyperbolic = new Button("Recenter on Selection");
        focusHyperbolic.setOnAction(e -> graphPane.focusSelection());
        Label projectionInfo = new Label("Hyperbolic positions are computed by Trinity from actual graph links.\n"
            + "No imported layout coordinates or hierarchy annotations are used.\n"
            + "All semantic cross-links remain available in the graph.\n"
            + "Curved geodesic edges are not yet enabled.");
        projectionInfo.setWrapText(true);
        Label distanceInfo = new Label();
        Runnable updateDistance = () -> {
            var edge = graphController.getSelectedEdge();
            double d = edge == null ? Double.NaN
                : graphController.hyperbolicDistance(edge.getStartID(), edge.getEndID());
            distanceInfo.setText(Double.isFinite(d)
                ? String.format(Locale.ROOT, "Selected edge hyperbolic distance: %.4f", d)
                : "Select an edge to inspect hyperbolic distance");
        };
        updateDistance.run();
        scene.addEventHandler(GraphEvent.GRAPH_PROJECTION_APPLIED, e -> {
            projection.setValue(graphController.getProjectionMode());
            updateDistance.run();
        });
        scene.addEventHandler(GraphEvent.GRAPH_PROJECTION_FAILED, e ->
            projection.setValue(graphController.getProjectionMode()));
        scene.addEventHandler(GraphEvent.GRAPH_EDGE_CLICK, e -> updateDistance.run());
        scene.addEventHandler(GraphEvent.NEW_GRAPHDIRECTED_COLLECTION, e ->
            javafx.application.Platform.runLater(() -> {
                projection.setValue(graphController.getProjectionMode());
                updateDistance.run();
            }));
        CheckBox showPoincareBoundary = new CheckBox("Show Poincare ball boundary");
        showPoincareBoundary.setSelected(graphPane.isPoincareBoundaryVisible());
        showPoincareBoundary.setOnAction(e ->
            graphPane.setPoincareBoundaryVisible(showPoincareBoundary.isSelected()));

        VBox projectionContent = new VBox(12, new Label("Coordinate projection"), projection,
            showPoincareBoundary, focusHyperbolic, recenter, distanceInfo, projectionInfo,
            new Separator(), new Label("Graph layout (Euclidean projection)"),
            new Label("These layouts do not replace Trinity's computed Poincare hierarchy."),
            new GraphControlsView(scene));
        projectionContent.setPadding(new Insets(12));
        for (javafx.scene.Node item : projectionContent.getChildren()) {
            if (item instanceof Label label) label.setWrapText(true);
        }
        ScrollPane projectionScroll = new ScrollPane(projectionContent);
        projectionScroll.setFitToWidth(true);
        projectionScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        styleContent.getChildren().addAll(new Separator(), new Label("Display"),
            showNodes, showEdges,
            new Label("Edges per rendering pulse"), edgeBatchSize,
            new Label("Fit Graph Percentage"), fitPercentage);
        styleContent.setPadding(new Insets(12));
        ScrollPane styleScroll = new ScrollPane(styleContent);
        styleScroll.setFitToWidth(true);
        styleScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        searchPanel.setPadding(new Insets(12));
        ScrollPane searchScroll = new ScrollPane(searchPanel);
        searchScroll.setFitToWidth(true);
        searchScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        edgeDisclosurePanel.setPadding(new Insets(12));
        relationshipScroll.setPrefHeight(310);
        ScrollPane relationshipsScroll = new ScrollPane(edgeDisclosurePanel);
        relationshipsScroll.setFitToWidth(true);
        relationshipsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        TabPane tabs = new TabPane(
            new Tab("Projection", projectionScroll),
            new Tab("Graph Style", styleScroll),
            new Tab("Exploration", explorationScroll),
            new Tab("Search", searchScroll),
            new Tab("Relationships", relationshipsScroll));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        // Identify the actual loaded controls class when diagnosing stale build artifacts.
        System.out.println("[CG-CONTROLS] " + CONTROLS_LAYOUT_VERSION + " tabs="
            + String.join(", ", tabs.getTabs().stream().map(Tab::getText).toList()));
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
