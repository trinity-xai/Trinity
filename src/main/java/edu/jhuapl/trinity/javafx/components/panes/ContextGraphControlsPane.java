package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.GraphControlsView;
import edu.jhuapl.trinity.javafx.components.GraphStyleControlsView;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import javafx.scene.control.Button;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphController;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraph3DPane;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphProjectionMode;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphCommunityParams;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphCommunityHierarchy;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphHyperbolicPackingParams;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphEdgeDisplayMode;
import edu.jhuapl.trinity.javafx.javafx3d.contextgraph.ContextGraphEntitySearchResult;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
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
import javafx.stage.Screen;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

/** Dedicated floating controls for the Context Graph view. */
public final class ContextGraphControlsPane extends LitPathPane {
    public static final String CONTROLS_LAYOUT_VERSION = "CG-M4B2-SEMANTIC-PACKING-DIAGNOSTICS-2026-11-12";

    public ContextGraphControlsPane(Scene scene, Pane parent, ContextGraphController graphController,
                                    ContextGraph3DPane graphPane) {
        super(scene, parent, 700, 620, new BorderPane(), "Context Graph Controls", "", 200.0, 300.0);
        setPickOnBounds(false);
        setFocusTraversable(false);
        // The LitPathPane content must not be allowed to determine the floating pane's
        // initial dimensions. In particular, wrapped explanatory Labels can report a
        // very large natural preferred width before their viewport has been measured.
        // Clamp initial dimensions to both the Scene and the available display area;
        // the user can still resize the floating pane afterwards.
        double sceneWidth = scene.getWidth() > 0 ? scene.getWidth() : Double.MAX_VALUE;
        double sceneHeight = scene.getHeight() > 0 ? scene.getHeight() : Double.MAX_VALUE;
        Screen display = Screen.getPrimary();
        if (scene.getWindow() != null) {
            List<Screen> intersectingScreens = Screen.getScreensForRectangle(
                scene.getWindow().getX(), scene.getWindow().getY(),
                Math.max(1.0, sceneWidth == Double.MAX_VALUE ? 1.0 : sceneWidth),
                Math.max(1.0, sceneHeight == Double.MAX_VALUE ? 1.0 : sceneHeight));
            if (!intersectingScreens.isEmpty()) display = intersectingScreens.get(0);
        }
        double screenWidth = display.getVisualBounds().getWidth();
        double screenHeight = display.getVisualBounds().getHeight();
        setPrefSize(
            Math.max(getMinWidth(), Math.min(700.0, Math.min(sceneWidth, screenWidth) - 32.0)),
            Math.max(getMinHeight(), Math.min(620.0, Math.min(sceneHeight, screenHeight) - 32.0)));

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
        entityResults.setMinHeight(0);
        entityResults.setMaxHeight(Double.MAX_VALUE);
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
        scene.addEventHandler(GraphEvent.GRAPH_DATA_CLEARED, e -> {
            entityQuery.clear();
            entityResults.getItems().clear();
            searchSummary.setText("Type to find an entity");
        });
        VBox searchHeader = new VBox(7, new Label("Find and Select Entity"),
            entityQuery, searchSummary);
        BorderPane searchPanel = new BorderPane();
        searchPanel.setTop(searchHeader);
        searchPanel.setCenter(entityResults);
        searchPanel.setBottom(new HBox(8, selectFoundEntity, focusFoundEntity));
        BorderPane.setMargin(entityResults, new Insets(8, 0, 8, 0));
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
        edgeVisibilitySummary.setMinWidth(0);
        edgeVisibilitySummary.setMaxWidth(Double.MAX_VALUE);
        Runnable updateEdgeSummary = () -> {
            edgeDisclosureMode.setValue(graphController.getEdgeDisplayMode());
            crossLinkLimit.setDisable(
                graphController.getEdgeDisplayMode() != ContextGraphEdgeDisplayMode.BACKBONE_WITH_FOCUS);
            if (graphController.getEdgeDisplayMode() == ContextGraphEdgeDisplayMode.COMMUNITY_AGGREGATE) {
                edgeVisibilitySummary.setText(graphController.getAggregationSummary());
            } else {
                edgeVisibilitySummary.setText("Relationships passing filters: "
                    + graphController.getVisibleRelationshipEdgeCount() + " / "
                    + (graphController.getCurrentGraph() == null ? 0
                        : graphController.getCurrentGraph().getEdges().size())
                    + " | backbone: " + (graphController.isBackboneReady()
                        ? graphController.getBackboneEdgeCount()
                        : graphController.isBackboneFailed() ? "unavailable" : "computing..."));
            }
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
        relationshipScroll.setMinHeight(0);
        relationshipScroll.setMaxHeight(Double.MAX_VALUE);
        relationshipScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox edgeDisclosurePanel = new VBox(7,
            new Label("Relationship display (source nodes stay visible)"), edgeDisclosureMode,
            new Label("Maximum focused cross-links"), crossLinkLimit,
            edgeVisibilitySummary, new Label("Visible relationship types"),
            new HBox(8, showAllRelationships, hideAllRelationships));
        rebuildRelationshipCheckboxes.run();
        updateEdgeSummary.run();
        scene.addEventHandler(GraphEvent.GRAPH_DATA_CLEARED, e -> {
            rebuildRelationshipCheckboxes.run();
            updateEdgeSummary.run();
        });
        scene.addEventHandler(GraphEvent.GRAPH_EXPLORATION_STATE_CHANGED, e -> updateEdgeSummary.run());
        scene.addEventHandler(GraphEvent.GRAPH_COMMUNITY_HIERARCHY_CHANGED, e -> updateEdgeSummary.run());
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

        // A semantic hierarchy is computed from original topology, independent of source
        // containment and Euclidean coordinates. Changes apply only on Recompute.
        ContextGraphCommunityParams initial = graphController.getCommunityParams();
        ComboBox<ContextGraphCommunityParams.Algorithm> communityAlgorithm = new ComboBox<>();
        communityAlgorithm.getItems().addAll(ContextGraphCommunityParams.Algorithm.values());
        communityAlgorithm.setValue(initial.algorithm());
        sizeCommunityControl(communityAlgorithm);
        Spinner<Double> communityResolution = new Spinner<>(0.2, 3.0, initial.resolution(), 0.1);
        communityResolution.setEditable(true);
        sizeCommunityControl(communityResolution);
        installCommitOnFocusLost(communityResolution);
        ComboBox<String> maximumLevels = new ComboBox<>();
        maximumLevels.getItems().addAll("Auto", "2", "3", "4", "5");
        maximumLevels.setValue(initial.maximumLevels() == 0
            ? "Auto" : Integer.toString(initial.maximumLevels()));
        sizeCommunityControl(maximumLevels);
        ComboBox<String> targetCommunitySize = new ComboBox<>();
        targetCommunitySize.getItems().addAll("Auto", "10", "25", "50", "100", "250");
        targetCommunitySize.setValue(initial.targetCommunitySize() == 0
            ? "Auto" : Integer.toString(initial.targetCommunitySize()));
        sizeCommunityControl(targetCommunitySize);
        Spinner<Double> adminWeight = new Spinner<>(0.01, 10.0,
            initial.administrativeWeight(), 0.01);
        Spinner<Double> structuralWeight = new Spinner<>(0.01, 10.0,
            initial.structuralWeight(), 0.05);
        Spinner<Double> semanticWeight = new Spinner<>(0.01, 10.0,
            initial.semanticWeight(), 0.05);
        for (Spinner<Double> spinner : List.of(adminWeight, structuralWeight, semanticWeight)) {
            spinner.setEditable(true);
            sizeCommunityControl(spinner);
            installCommitOnFocusLost(spinner);
        }
        Runnable updateAlgorithmFields = () -> {
            boolean legacy = communityAlgorithm.getValue()
                == ContextGraphCommunityParams.Algorithm.LEGACY;
            communityResolution.setDisable(legacy);
            maximumLevels.setDisable(legacy);
            targetCommunitySize.setDisable(legacy);
            adminWeight.setDisable(legacy);
            structuralWeight.setDisable(legacy);
            semanticWeight.setDisable(legacy);
        };
        communityAlgorithm.setOnAction(e -> updateAlgorithmFields.run());
        updateAlgorithmFields.run();
        Button recomputeCommunities = new Button("Recompute Community Hierarchy");
        recomputeCommunities.setMinWidth(0);
        recomputeCommunities.setPrefWidth(260);
        recomputeCommunities.setMaxWidth(260);
        Label communityDiagnostics = new Label();
        communityDiagnostics.setWrapText(true);
        communityDiagnostics.setMinWidth(0);
        communityDiagnostics.setPrefWidth(0);
        communityDiagnostics.setMaxWidth(Double.MAX_VALUE);
        Runnable updateCommunities = () -> communityDiagnostics.setText(graphController.getCommunityStatus());
        updateCommunities.run();
        recomputeCommunities.setOnAction(e -> {
            ContextGraphCommunityParams revised = new ContextGraphCommunityParams(
                communityAlgorithm.getValue(), communityResolution.getValue(),
                "Auto".equals(maximumLevels.getValue()) ? 0
                    : Integer.parseInt(maximumLevels.getValue()),
                "Auto".equals(targetCommunitySize.getValue()) ? 0
                    : Integer.parseInt(targetCommunitySize.getValue()),
                adminWeight.getValue(), structuralWeight.getValue(), semanticWeight.getValue());
            if (revised.equals(graphController.getCommunityParams()))
                graphController.recomputeCommunityHierarchy();
            else graphController.setCommunityParams(revised);
        });
        scene.addEventHandler(GraphEvent.GRAPH_COMMUNITY_HIERARCHY_CHANGED,
            e -> updateCommunities.run());
        scene.addEventHandler(GraphEvent.GRAPH_DATA_CLEARED, e -> {
            ContextGraphCommunityParams defaults = graphController.getCommunityParams();
            communityAlgorithm.setValue(defaults.algorithm());
            communityResolution.getValueFactory().setValue(defaults.resolution());
            maximumLevels.setValue("Auto");
            targetCommunitySize.setValue("Auto");
            adminWeight.getValueFactory().setValue(defaults.administrativeWeight());
            structuralWeight.getValueFactory().setValue(defaults.structuralWeight());
            semanticWeight.getValueFactory().setValue(defaults.semanticWeight());
            updateAlgorithmFields.run();
            updateCommunities.run();
        });
        VBox communityPanel = new VBox(8, new Label("Community Hierarchy"),
            new Label("Community detector"), communityAlgorithm,
            new Label("Resolution γ (1.0 = standard weighted modularity)"), communityResolution,
            new Label("Maximum computed levels (not visible detail)"), maximumLevels,
            new Label("Preferred layout group size (not a hard constraint)"), targetCommunitySize,
            new Label("Relationship weights: administrative"), adminWeight,
            new Label("Structural / containment"), structuralWeight,
            new Label("Semantic / ordinary"), semanticWeight,
            new Label("Auto selects multilevel Louvain. Maximum Levels Auto stops on "
                + "convergence or after 32 levels. Legacy uses fixed historical weights "
                + "and returns a single partition. Target size selects the computed level "
                + "whose mean community size is closest; Auto uses the finest level."),
            recomputeCommunities, new Separator(), communityDiagnostics);
        ContextGraphHyperbolicPackingParams initialPacking = graphController.getPackingParams();
        ComboBox<ContextGraphHyperbolicPackingParams.Placement> placementMode = new ComboBox<>();
        placementMode.getItems().addAll(ContextGraphHyperbolicPackingParams.Placement.values());
        placementMode.setValue(initialPacking.placement());
        sizeCommunityControl(placementMode);
        ComboBox<ContextGraphHyperbolicPackingParams.AngularAllocation> angularAllocation = new ComboBox<>();
        angularAllocation.getItems().addAll(ContextGraphHyperbolicPackingParams.AngularAllocation.values());
        angularAllocation.setValue(initialPacking.allocation());
        sizeCommunityControl(angularAllocation);
        Spinner<Double> angularSpread = new Spinner<>(20.0, 150.0,
            initialPacking.angularSpreadDegrees(), 5.0);
        Spinner<Double> siblingGap = new Spinner<>(0.0, 25.0,
            initialPacking.siblingGapDegrees(), 1.0);
        Spinner<Double> massInfluence = new Spinner<>(0.0, 1.0,
            initialPacking.massInfluence(), 0.05);
        Spinner<Double> radialStep = new Spinner<>(0.15, 2.0,
            initialPacking.radialStep(), 0.05);
        for (Spinner<Double> spinner : List.of(angularSpread, siblingGap, massInfluence, radialStep)) {
            spinner.setEditable(true);
            sizeCommunityControl(spinner);
            installCommitOnFocusLost(spinner);
        }
        ComboBox<ContextGraphHyperbolicPackingParams.RadialProfile> radialProfile = new ComboBox<>();
        radialProfile.getItems().addAll(ContextGraphHyperbolicPackingParams.RadialProfile.values());
        radialProfile.setValue(initialPacking.radialProfile());
        sizeCommunityControl(radialProfile);
        TextField customSteps = new TextField("1.0, 0.85, 0.70, 0.55");
        customSteps.setPrefWidth(300);
        customSteps.setMaxWidth(300);
        customSteps.setPromptText("Hyperbolic steps: outer, intermediate, inner, entities...");
        if (!initialPacking.customSteps().isEmpty())
            customSteps.setText(initialPacking.customSteps().stream()
                .map(String::valueOf).collect(java.util.stream.Collectors.joining(", ")));
        Runnable updatePackingFields = () -> {
            boolean structural = placementMode.getValue()
                == ContextGraphHyperbolicPackingParams.Placement.STRUCTURAL_BACKBONE;
            angularAllocation.setDisable(structural);
            angularSpread.setDisable(structural);
            siblingGap.setDisable(structural);
            massInfluence.setDisable(structural);
            radialProfile.setDisable(structural);
            radialStep.setDisable(structural || radialProfile.getValue()
                == ContextGraphHyperbolicPackingParams.RadialProfile.CUSTOM);
            customSteps.setDisable(structural || radialProfile.getValue()
                != ContextGraphHyperbolicPackingParams.RadialProfile.CUSTOM);
        };
        radialProfile.setOnAction(e -> updatePackingFields.run());
        placementMode.setOnAction(e -> updatePackingFields.run());
        updatePackingFields.run();
        Label packingError = new Label();
        packingError.setWrapText(true);
        Button applyPacking = new Button("Apply Layout");
        applyPacking.setOnAction(e -> {
            try {
                List<Double> custom = new ArrayList<>();
                if (radialProfile.getValue() == ContextGraphHyperbolicPackingParams.RadialProfile.CUSTOM) {
                    for (String token : customSteps.getText().split(","))
                        custom.add(Double.parseDouble(token.trim()));
                }
                ContextGraphHyperbolicPackingParams updated =
                    new ContextGraphHyperbolicPackingParams(placementMode.getValue(),
                        angularAllocation.getValue(), angularSpread.getValue(),
                        siblingGap.getValue(), massInfluence.getValue(),
                        radialProfile.getValue(), radialStep.getValue(), custom);
                packingError.setText("");
                graphController.setPackingParams(updated);
            } catch (IllegalArgumentException invalid) {
                packingError.setText("Invalid packing settings: " + invalid.getMessage());
            }
        });

        CheckBox communityColors = new CheckBox("Community colors (diagnostic override)");
        communityColors.setSelected(graphController.isCommunityColorDiagnostic());
        communityColors.setOnAction(e ->
            graphController.setCommunityColorDiagnostic(communityColors.isSelected()));
        ComboBox<String> diagnosticLevel = new ComboBox<>();
        sizeCommunityControl(diagnosticLevel);
        boolean[] updatingLevel = {false};
        diagnosticLevel.setOnAction(e -> {
            if (!updatingLevel[0] && diagnosticLevel.getSelectionModel().getSelectedIndex() >= 0)
                graphController.setCommunityDiagnosticLevel(
                    diagnosticLevel.getSelectionModel().getSelectedIndex());
        });
        Runnable updateDiagnosticLevels = () -> {
            updatingLevel[0] = true;
            try {
                ContextGraphCommunityHierarchy hierarchy = graphController.getCommunityHierarchy();
                diagnosticLevel.getItems().clear();
                if (hierarchy != null) {
                    for (int i = 0; i < hierarchy.levels().size(); i++) {
                        diagnosticLevel.getItems().add("Level " + (i + 1) + ": "
                            + hierarchy.levels().get(i).count() + " communities");
                    }
                    int selected = graphController.getCommunityDiagnosticLevel();
                    if (selected >= 0 && selected < diagnosticLevel.getItems().size())
                        diagnosticLevel.getSelectionModel().select(selected);
                }
                diagnosticLevel.setDisable(diagnosticLevel.getItems().isEmpty());
                communityColors.setSelected(graphController.isCommunityColorDiagnostic());
            } finally {
                updatingLevel[0] = false;
            }
        };
        updateDiagnosticLevels.run();
        scene.addEventHandler(GraphEvent.GRAPH_COMMUNITY_HIERARCHY_CHANGED,
            e -> updateDiagnosticLevels.run());
        scene.addEventHandler(GraphEvent.GRAPH_DATA_CLEARED, e -> {
            ContextGraphHyperbolicPackingParams defaults = graphController.getPackingParams();
            placementMode.setValue(defaults.placement());
            angularAllocation.setValue(defaults.allocation());
            angularSpread.getValueFactory().setValue(defaults.angularSpreadDegrees());
            siblingGap.getValueFactory().setValue(defaults.siblingGapDegrees());
            massInfluence.getValueFactory().setValue(defaults.massInfluence());
            radialProfile.setValue(defaults.radialProfile());
            radialStep.getValueFactory().setValue(defaults.radialStep());
            customSteps.setText("1.0, 0.85, 0.70, 0.55");
            packingError.setText("");
            updatePackingFields.run();
            updateDiagnosticLevels.run();
        });
        communityPanel.getChildren().addAll(new Separator(),
            new Label("Community coloring (diagnostic, not FactorLabel)"),
            communityColors, new Label("Community level (colors / edge summaries)"), diagnosticLevel,
            new Label("Spatial layout: Classic preserves 4B-A; Multilevel uses spherical "
                + "hierarchy packing; Relationship-aware also uses weighted graph links."),
            new Label("Placement strategy"), placementMode,
            new Label("Angular allocation"), angularAllocation,
            new Label("Branch angular spread (degrees)"), angularSpread,
            new Label("Desired sibling gap (degrees, not guaranteed)"), siblingGap,
            new Label("Subtree mass influence (0=equal, 1=population)"), massInfluence,
            new Label("Radial spacing profile"), radialProfile,
            new Label("Global parent-child hyperbolic step"), radialStep,
            new Label("Custom hyperbolic steps per layer, comma-separated"), customSteps,
            new Label("Layers run from coarse communities to finer communities to entities. "
                + "Distances are hyperbolic, not JavaFX display scale; an automatic cumulative "
                + "distance budget prevents boundary saturation. Geometry updates reuse cached detection."),
            applyPacking, packingError);
        communityPanel.setPadding(new Insets(12));
        communityPanel.setMinWidth(0);
        communityPanel.setPrefWidth(0);
        // Wrap to the actual viewport, not the text's unwrapped preferred width.
        // This prevents lengthy help and diagnostics text from enlarging PathPane.
        for (javafx.scene.Node component : communityPanel.getChildren()) {
            if (component instanceof Label label) {
                label.setWrapText(true);
                label.setMinWidth(0);
                label.setPrefWidth(0);
                label.setMaxWidth(Double.MAX_VALUE);
            }
        }
        ScrollPane communityScroll = new ScrollPane(communityPanel);
        communityScroll.setFitToWidth(true);
        communityScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        communityScroll.setMinSize(0, 0);

        Button clearGraphData = new Button("Clear Data");
        clearGraphData.setOnAction(e -> graphPane.clearData());
        VBox projectionContent = new VBox(12, new Label("Coordinate projection"), projection,
            showPoincareBoundary, focusHyperbolic, recenter, clearGraphData, distanceInfo, projectionInfo,
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
        searchPanel.setMinHeight(0);

        edgeDisclosurePanel.setPadding(new Insets(12, 12, 8, 12));
        BorderPane relationshipsPanel = new BorderPane();
        relationshipsPanel.setTop(edgeDisclosurePanel);
        relationshipsPanel.setCenter(relationshipScroll);
        relationshipsPanel.setPadding(new Insets(0, 12, 12, 12));
        relationshipsPanel.setMinHeight(0);

        TabPane tabs = new TabPane(
            new Tab("Projection", projectionScroll),
            new Tab("Communities", communityScroll),
            new Tab("Graph Style", styleScroll),
            new Tab("Exploration", explorationScroll),
            new Tab("Search", searchPanel),
            new Tab("Relationships", relationshipsPanel));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setMinSize(0, 0);
        ((BorderPane) contentPane).setMinSize(0, 0);
        // Selection boxes in all six tabs must not propagate unbounded preferred widths
        // through the LitPathPane, including nested GraphControlsView controls.
        constrainSelectors(tabs);
        // Identify the actual loaded controls class when diagnosing stale build artifacts.
        System.out.println("[CG-CONTROLS] " + CONTROLS_LAYOUT_VERSION + " tabs="
            + String.join(", ", tabs.getTabs().stream().map(Tab::getText).toList()));
        ((BorderPane) contentPane).setCenter(tabs);
    }

    /** Bound ComboBoxes and ChoiceBoxes across all tabs, including embedded control views. */
    private static void constrainSelectors(Parent root) {
        ArrayDeque<Node> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            Node node = pending.removeFirst();
            if (node instanceof ComboBox<?> || node instanceof ChoiceBox<?>) {
                Control c = (Control) node;
                c.setMinWidth(0);
                c.setPrefWidth(240);
                c.setMaxWidth(240);
            } else if (node instanceof Spinner<?> spinner) {
                spinner.setMinWidth(0);
                double preferred = spinner.getPrefWidth();
                spinner.setPrefWidth(preferred > 0 ? Math.min(preferred, 240) : 180);
                spinner.setMaxWidth(240);
            }
            // Tab and ScrollPane content is not part of the Parent child list
            // until a JavaFX skin is installed. Visit it explicitly now.
            if (node instanceof TabPane tabs) {
                for (Tab tab : tabs.getTabs()) {
                    if (tab.getContent() != null) pending.addLast(tab.getContent());
                }
            }
            if (node instanceof ScrollPane scroll && scroll.getContent() != null)
                pending.addLast(scroll.getContent());
            if (node instanceof Parent parent)
                pending.addAll(parent.getChildrenUnmodifiable());
        }
    }

    /** Keep community selectors at readable, predictable widths without forcing the pane wider. */
    private static void sizeCommunityControl(javafx.scene.control.Control control) {
        control.setMinWidth(0);
        control.setPrefWidth(240);
        control.setMaxWidth(240);
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
