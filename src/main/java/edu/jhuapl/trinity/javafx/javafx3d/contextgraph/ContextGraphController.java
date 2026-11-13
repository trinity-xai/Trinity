package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphFeatureCollectionAdapter;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.utils.graph.GraphLayoutParams;
import edu.jhuapl.trinity.utils.graph.ForceFrLayout3D;
import edu.jhuapl.trinity.utils.graph.GraphTopologyLayouts3D;
import edu.jhuapl.trinity.utils.graph.GraphNetworkLayouts3D;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.concurrent.Task;
import javafx.geometry.Point3D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.List;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.GraphEvent;

import edu.jhuapl.trinity.javafx.renderers.Graph3DRenderer;
import edu.jhuapl.trinity.utils.graph.GraphStyleParams;
import edu.jhuapl.trinity.utils.Utils;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.image.WritableImage;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import javafx.scene.text.Font;


/**
 * Owns the 3D graph visual subsystem hosted by the Context Graph view.
 *
 * The controller owns graph rendering and style state independently of other views.
 *
 * @author Sean Phillips
 */
public final class ContextGraphController {

    private final Scene scene;
    private final Group graphLayer = new Group();
    // These groups stay attached to the SubScene for the lifetime of the viewer.
    private final Group nodeLayer = new Group();
    private final Group edgeLayer = new Group();
    /** Derived summaries, entirely separate from original GraphEdge 3D objects. */
    private final Group aggregateEdgeLayer = new Group();
    /** Virtual community glyphs and optional local drill-down connections. */
    private final Group communityRepresentativeLayer = new Group();
    private final Group communityExpansionEdgeLayer = new Group();
    private boolean diagnosticDiffuseMaterials;
    private final Map<Color, PhongMaterial> diffuseMaterials = new HashMap<>();

    public boolean isDiagnosticDiffuseMaterials() { return diagnosticDiffuseMaterials; }

    /** Diagnostic only: compare the ordinary diffuse pipeline with RGBA emissive textures. */
    public void setDiagnosticDiffuseMaterials(boolean enabled) {
        if (diagnosticDiffuseMaterials == enabled) return;
        diagnosticDiffuseMaterials = enabled;
        applyNodeMaterials(styleParams);
        applyEdgeMaterials(styleParams);
        applyAggregateMaterials();
    }

    private PhongMaterial displayMaterial(Color color, double opacity) {
        if (!diagnosticDiffuseMaterials) return emissiveMaterial(color, opacity);
        // Deliberately opaque for the rendering A/B test.
        Color opaque = new Color(color.getRed(), color.getGreen(), color.getBlue(), 1.0);
        return diffuseMaterials.computeIfAbsent(opaque, c -> {
            PhongMaterial material = new PhongMaterial(c);
            material.setSpecularColor(c);
            return material;
        });
    }

    private static String category(List<String> labels) {
        if (labels == null || labels.isEmpty()) return "Uncategorized";
        // Match GraphFeatureCollectionAdapter's FeatureVector.label mapping.
        String joined = String.join(", ", labels).trim();
        return joined.isEmpty() ? "Uncategorized" : joined;
    }

    private static Color categoryColor(String label, Color fallback) {
        FactorLabel factor = FactorLabel.getFactorLabel(label);
        if (factor != null && factor.getColor() != null) return factor.getColor();
        if ("Uncategorized".equals(label)) return fallback;
        // Stable local fallback when the label has not been registered globally.
        return Color.hsb(Math.floorMod(label.hashCode(), 360), 0.72, 0.94);
    }

    private Color nodeColor(GraphNode node, GraphStyleParams style) {
        Color fallback = style.nodeColor == null ? Color.CYAN : style.nodeColor;
        if (revealedEntityCommunity != null
            && revealedCommunityMembers.contains(node.getEntityID()))
            return Color.rgb(255, 227, 126);
        if (communityColorDiagnostic && diagnosticMembership.containsKey(node.getEntityID()))
            return diagnosticCommunityColor(diagnosticMembership.get(node.getEntityID()));
        if (!style.colorByLabel) return fallback;
        return categoryColor(category(node.getLabels()), fallback);
    }

    private Color edgeColor(GraphEdge edge, GraphStyleParams style) {
        Color fallback = style.edgeColor == null ? Color.CORNFLOWERBLUE : style.edgeColor;
        if (communityColorDiagnostic && !diagnosticMembership.isEmpty()) {
            Integer a = diagnosticMembership.get(edge.getStartID());
            Integer b = diagnosticMembership.get(edge.getEndID());
            return a != null && a.equals(b)
                ? diagnosticCommunityColor(a) : Color.rgb(95, 104, 119);
        }
        if (!style.colorByLabel) return fallback;
        return categoryColor(edge.getRelationship() == null ? "Uncategorized"
            : category(edge.getRelationship().getLabels()), fallback);
    }

    /** Register graph categories in Trinity's existing shared label legend. */
    private void registerGraphLabels(GraphDirectedCollection graph) {
        Set<String> labels = new LinkedHashSet<>();
        for (GraphNode node : graph.getNodes()) labels.add(category(node.getLabels()));
        for (GraphEdge edge : graph.getEdges()) {
            labels.add(edge.getRelationship() == null ? "Uncategorized"
                : category(edge.getRelationship().getLabels()));
        }
        List<FactorLabel> missing = new ArrayList<>();
        for (String label : labels) {
            if (FactorLabel.getFactorLabel(label) == null) {
                Color fallback = "Uncategorized".equals(label)
                    ? (styleParams.nodeColor == null ? Color.CYAN : styleParams.nodeColor)
                    : Color.hsb(Math.floorMod(label.hashCode(), 360), 0.72, 0.94);
                missing.add(new FactorLabel(label, fallback));
            }
        }
        if (!missing.isEmpty()) FactorLabel.addAllFactorLabels(missing);
    }

    private boolean nodeLabelVisible(GraphNode node) {
        return FactorLabel.visibilityByLabel(category(node.getLabels()));
    }

    private boolean edgeLabelVisible(GraphEdge edge) {
        String label = edge.getRelationship() == null ? "Uncategorized"
            : category(edge.getRelationship().getLabels());
        if (!FactorLabel.visibilityByLabel(label)) return false;
        GraphNode start = topology.node(edge.getStartID());
        GraphNode end = topology.node(edge.getEndID());
        return start != null && end != null
            && nodeLabelVisible(start) && nodeLabelVisible(end);
    }

    private void applyLabelVisibility() {
        if (currentGraph == null) return;
        for (GraphNode node : currentGraph.getNodes()) {
            ContextGraphNode3D view = nodeViews.get(node.getEntityID());
            if (view != null) view.setVisible(nodeAllowed(node));
        }
        for (Map.Entry<GraphEdge, ContextGraphEdge3D> entry : edgeViews.entrySet()) {
            entry.getValue().setVisible(edgeAllowed(entry.getKey()));
        }
        refreshAggregateVisibility();
    }

    // Graph exploration state is independent of category styling and scene geometry.
    private ContextGraphNeighborhoodIndex topology = new ContextGraphNeighborhoodIndex(null);
    private ContextGraphEntitySearchIndex entitySearchIndex = new ContextGraphEntitySearchIndex(null);
    // All three selectors are display-only: they never delete or rewrite graph edges.
    private ContextGraphEdgeDisplayMode edgeDisplayMode = ContextGraphEdgeDisplayMode.ALL;
    private boolean edgeDisplayModeCustomized;
    private Task<ContextGraphCommunityEdgeAggregation> aggregationTask;
    private AnimationTimer aggregateAttachmentTimer;
    private long aggregationGeneration;
    private final Map<Integer, ContextGraphCommunityEdgeAggregation> aggregationCache = new HashMap<>();
    private ContextGraphCommunityEdgeAggregation aggregation;
    private final Map<ContextGraphCommunityEdgeBundle, ContextGraphEdge3D>
        aggregateViews = new LinkedHashMap<>();
    private final Map<ContextGraphEdge3D, ContextGraphCommunityEdgeBundle>
        pickedAggregates = new java.util.IdentityHashMap<>();
    private final Set<ContextGraphCommunityEdgeBundle> aggregateValidPositions = new HashSet<>();
    private final Set<GraphEdge> revealedAggregateEdges =
        Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private ContextGraphCommunityEdgeBundle selectedAggregate;
    private ContextGraphCommunityCatalog communityCatalog;
    private final Map<ContextGraphCommunityRef, ContextGraphNode3D> communityViews = new LinkedHashMap<>();
    private final Map<ContextGraphNode3D, ContextGraphCommunityRef> pickedCommunities =
        new java.util.IdentityHashMap<>();
    private final Map<ContextGraphCommunityChildConnection, ContextGraphEdge3D> childConnectionViews =
        new LinkedHashMap<>();
    private final Set<ContextGraphCommunityChildConnection> childValidPositions = new HashSet<>();
    private final List<ContextGraphCommunityRef> expansionPath = new ArrayList<>();
    private final Set<GraphEdge> revealedCommunityEdges =
        Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private Set<String> revealedCommunityMembers = Set.of();
    /** Source endpoints disclosed explicitly by selecting one derived relationship bundle. */
    private Set<String> revealedAggregateEndpoints = Set.of();
    private ContextGraphCommunityRef revealedEntityCommunity;
    private ContextGraphCommunityRef selectedCommunity;
    private long expansionGeneration;
    private Task<Map<Integer, ContextGraphCommunityEdgeAggregation>> expansionTask;
    private Set<GraphEdge> backboneEdges = Set.of();
    private boolean backboneReady;
    private boolean backboneFailed;
    private long backboneGeneration;
    private ContextGraphCommunityParams communityParams = ContextGraphCommunityParams.defaults();
    private ContextGraphCommunityHierarchy communityHierarchy;
    private Task<ContextGraphCommunityLayoutResult> communityTask;
    private final Map<ContextGraphCommunityParams, ContextGraphCommunityHierarchy> hierarchyCache =
        new LinkedHashMap<>();
    private final Map<ContextGraphCommunityParams,
        Map<ContextGraphHyperbolicPackingParams, ContextGraphCommunityLayoutResult>> layoutCache =
            new LinkedHashMap<>();
    private ContextGraphHyperbolicPackingParams packingParams =
        ContextGraphHyperbolicPackingParams.defaults();
    private ContextGraphSpatialDiagnostics spatialDiagnostics;
    private String spatialComparison = "";
    private String partitionComparison = "";
    private boolean communityColorDiagnostic;
    private int communityDiagnosticLevel = -1;
    private Map<String, Integer> diagnosticMembership = Map.of();
    private AnimationTimer communityMaterialTimer;
    private long communityMaterialGeneration;
    private String communityStatus = "No graph loaded";

    public ContextGraphCommunityParams getCommunityParams() { return communityParams; }
    public ContextGraphCommunityHierarchy getCommunityHierarchy() { return communityHierarchy; }
    public ContextGraphHyperbolicPackingParams getPackingParams() { return packingParams; }
    public boolean isCommunityColorDiagnostic() { return communityColorDiagnostic; }
    public ContextGraphCommunityRef resolveCommunity(Node picked) {
        return picked instanceof ContextGraphNode3D view ? pickedCommunities.get(view) : null;
    }
    public ContextGraphCommunityRef getSelectedCommunity() { return selectedCommunity; }

    /** The visible drill-down branch (or leaf entity disclosure), not all computed communities. */
    public boolean isCommunityExpanded(ContextGraphCommunityRef ref) {
        return ref != null && (expansionPath.contains(ref) || ref.equals(revealedEntityCommunity));
    }

    public String getCommunityExpansionActionLabel(ContextGraphCommunityRef ref) {
        if (ref == null) return "Expand Community";
        if (isCommunityExpanded(ref)) return ref.level() == 0
            ? "Hide Original Entities" : "Collapse Community";
        return ref.level() == 0 ? "Reveal Original Entities" : "Expand Community";
    }
    public ContextGraphNode3D getSelectedCommunityView() { return communityViews.get(selectedCommunity); }
    public String getSelectedCommunityTitle() {
        ContextGraphCommunityDescriptor descriptor = communityCatalog == null ? null
            : communityCatalog.descriptor(selectedCommunity);
        return descriptor == null ? "Community" : descriptor.title() + " (" + descriptor.memberCount() + ")";
    }
    public String getSelectedCommunityDetails() {
        return communityInspection(selectedCommunity);
    }
    public ContextGraphCommunityEdgeBundle getSelectedAggregate() { return selectedAggregate; }
    public ContextGraphEdge3D getSelectedAggregateView() { return findAggregateView(selectedAggregate); }
    public String getSelectedAggregateTitle() {
        if (selectedAggregate == null || aggregation == null || communityCatalog == null)
            return "Derived community connection";
        ContextGraphCommunityPair pair = selectedAggregate.pair();
        int level = aggregateViews.containsKey(selectedAggregate) ? aggregation.levelIndex()
            : childConnectionViews.keySet().stream()
                .filter(key -> key.bundle() == selectedAggregate)
                .mapToInt(ContextGraphCommunityChildConnection::level).findFirst().orElse(aggregation.levelIndex());
        ContextGraphCommunityDescriptor a = communityCatalog.descriptor(new ContextGraphCommunityRef(level, pair.first()));
        ContextGraphCommunityDescriptor b = communityCatalog.descriptor(new ContextGraphCommunityRef(level, pair.second()));
        return (a == null ? "Community " + (pair.first() + 1) : a.title()) + " / "
            + (b == null ? "Community " + (pair.second() + 1) : b.title());
    }
    public String getSelectedAggregateDetails() {
        return selectedAggregate == null ? "" : selectedAggregate.summary()
            + "\nCurrently passing filters: " + filteredAggregateCount(selectedAggregate)
            + "\nThis is a derived summary of original edges; it is not a source assertion."
            + "\nClick the connection again to hide disclosed original edges.";
    }
    public ContextGraphCommunityEdgeBundle resolveAggregateEdge(Node picked) {
        return picked instanceof ContextGraphEdge3D view ? pickedAggregates.get(view) : null;
    }
    public String getAggregationSummary() {
        if (edgeDisplayMode != ContextGraphEdgeDisplayMode.COMMUNITY_AGGREGATE) return "";
        if (aggregation == null) return "Preparing community relationship summaries...";
        int visible = 0, represented = 0;
        for (ContextGraphCommunityEdgeBundle connection : aggregation.connections()) {
            int count = filteredAggregateCount(connection);
            if (count > 0) { visible++; represented += count; }
        }
        String result = visible + " derived community connections representing " + represented
            + " original inter-community edges (level " + (aggregation.levelIndex() + 1) + ")."
            + " All original entities remain visible and selectable."
            + " Click a summarized connection to reveal its original edges; click again to hide."
            + " Intra-community relationships remain in the source graph but are not drawn"
            + " as inter-community connections.";
        if (selectedAggregate != null)
            result += "\n" + selectedAggregate.summary() + " | currently passing filters: "
                + filteredAggregateCount(selectedAggregate);
        return result;
    }
    public int getCommunityDiagnosticLevel() { return communityDiagnosticLevel; }

    public String getCommunityStatus() {
        if (communityTask != null || communityHierarchy == null
            || communityStatus.startsWith("Community hierarchy failed")) return communityStatus;
        return communityHierarchy.summary()
            + (spatialDiagnostics == null ? "" : "\n\n" + spatialDiagnostics.summary())
            + spatialComparison + partitionComparison;
    }

    /** Recompute geometry using the cached community hierarchy, not the detector. */
    public void setPackingParams(ContextGraphHyperbolicPackingParams updated) {
        if (updated == null || updated.equals(packingParams)) return;
        packingParams = updated;
        if (currentGraph != null) prepareBackbone(currentGraph, false);
        else fireOnRoot(new GraphEvent(GraphEvent.GRAPH_COMMUNITY_HIERARCHY_CHANGED));
    }

    /** Diagnostic overrides rendering colors only; category labels/visibility stay intact. */
    public void setCommunityColorDiagnostic(boolean enabled) {
        if (communityColorDiagnostic == enabled) return;
        communityColorDiagnostic = enabled;
        scheduleCommunityMaterialRefresh();
        fireOnRoot(new GraphEvent(GraphEvent.GRAPH_COMMUNITY_HIERARCHY_CHANGED));
    }

    public void setCommunityDiagnosticLevel(int level) {
        if (communityHierarchy == null || level < 0 || level >= communityHierarchy.levels().size()) return;
        if (communityDiagnosticLevel == level) return;
        communityDiagnosticLevel = level;
        refreshDiagnosticMembership();
        scheduleCommunityMaterialRefresh();
        if (edgeDisplayMode == ContextGraphEdgeDisplayMode.COMMUNITY_AGGREGATE)
            requestAggregateEdges();
        fireOnRoot(new GraphEvent(GraphEvent.GRAPH_COMMUNITY_HIERARCHY_CHANGED));
    }

    private Color diagnosticCommunityColor(int group) {
        double hue = (group * 137.50776405003785 + Math.max(0, communityDiagnosticLevel) * 23.0) % 360.0;
        return Color.hsb(hue, 0.70, 0.96);
    }

    private void refreshDiagnosticMembership() {
        if (communityHierarchy == null || communityHierarchy.levels().isEmpty()) {
            diagnosticMembership = Map.of();
            communityDiagnosticLevel = -1;
            return;
        }
        if (communityDiagnosticLevel < 0 || communityDiagnosticLevel >= communityHierarchy.levels().size())
            communityDiagnosticLevel = communityHierarchy.levels().size() - 1;
        ContextGraphCommunityLevel level = communityHierarchy.levels().get(communityDiagnosticLevel);
        Map<String, Integer> membership = new HashMap<>(communityHierarchy.entityIds().size() * 2);
        for (int i = 0; i < communityHierarchy.entityIds().size(); i++)
            membership.put(communityHierarchy.entityIds().get(i), level.communityOf(i));
        diagnosticMembership = membership;
    }

    /** Material updates are pulse-bounded; geometry/selection remains untouched. */
    private void scheduleCommunityMaterialRefresh() {
        final long generation = ++communityMaterialGeneration;
        if (communityMaterialTimer != null) {
            communityMaterialTimer.stop();
            communityMaterialTimer = null;
        }
        if (currentGraph == null) return;
        List<GraphNode> nodes = new ArrayList<>(currentGraph.getNodes());
        List<GraphEdge> edges = new ArrayList<>(currentGraph.getEdges());
        communityMaterialTimer = new AnimationTimer() {
            private int nextNode;
            private int nextEdge;
            @Override public void handle(long now) {
                if (generation != communityMaterialGeneration) {
                    stop();
                    return;
                }
                int nodeEnd = Math.min(nodes.size(), nextNode + 750);
                for (; nextNode < nodeEnd; nextNode++) {
                    GraphNode model = nodes.get(nextNode);
                    ContextGraphNode3D view = nodeViews.get(model.getEntityID());
                    if (view != null) {
                        view.setPhongMaterial(displayMaterial(nodeColor(model, styleParams), styleParams.nodeOpacity));
                        applyNodeExploration(model, view);
                    }
                }
                int edgeEnd = Math.min(edges.size(), nextEdge + 1000);
                for (; nextEdge < edgeEnd; nextEdge++) {
                    GraphEdge model = edges.get(nextEdge);
                    ContextGraphEdge3D view = edgeViews.get(model);
                    if (view != null) {
                        view.setBaseMaterial(displayMaterial(edgeColor(model, styleParams), styleParams.edgeOpacity));
                        applyEdgeExploration(model, view);
                    }
                }
                if (nextNode == nodes.size() && nextEdge == edges.size()) {
                    stop();
                    if (communityMaterialTimer == this) communityMaterialTimer = null;
                }
            }
        };
        communityMaterialTimer.start();
    }

    /** A changed setting recomputes asynchronously; unchanged values do not restart tasks. */
    public void setCommunityParams(ContextGraphCommunityParams params) {
        if (params == null || params.equals(communityParams)) return;
        communityParams = params;
        if (currentGraph != null) prepareBackbone(currentGraph, false);
        else fireOnRoot(new GraphEvent(GraphEvent.GRAPH_COMMUNITY_HIERARCHY_CHANGED));
    }

    /** Explicitly bypass cache, retaining the current settings and original graph. */
    public void recomputeCommunityHierarchy() {
        if (currentGraph != null) prepareBackbone(currentGraph, true);
    }
    private Map<String, Integer> relationshipTypeCounts = Map.of();
    private final Set<String> hiddenRelationshipTypes = new HashSet<>();
    private Set<GraphEdge> focusedCrossLinks = Set.of();
    private int focusedCrossLinkLimit = 100;
    private ContextGraphNeighborhoodIndex.Neighborhood neighborhood =
        ContextGraphNeighborhoodIndex.Neighborhood.empty();
    private boolean neighborhoodHighlightEnabled = true;
    private boolean dimOutsideNeighborhood;
    private boolean isolateSelection;
    private int neighborhoodDepth = 1;
    private static final Color NEIGHBOR_NODE_COLOR = Color.rgb(112, 238, 177);
    private static final Color DIM_NODE_COLOR = Color.rgb(45, 51, 64);
    private static final Color NEIGHBOR_EDGE_COLOR = Color.rgb(255, 225, 115);
    private static final Color DIM_EDGE_COLOR = Color.rgb(52, 54, 65);

    private boolean graphVisible = true;
    private GraphDirectedCollection currentGraph;
    private final Graph3DRenderer.Params graphParams = new Graph3DRenderer.Params()
        .withNodeRadius(1.0)
        .withNodeDivisions(16)
        .withEdgeWidth(0.5f)
        .withPositionScalar(1.0);
    // Context Graph defaults to categorical coloring without changing defaults for other views.
    private GraphStyleParams styleParams = defaultContextGraphStyle();

    private static GraphStyleParams defaultContextGraphStyle() {
        GraphStyleParams defaults = new GraphStyleParams();
        defaults.colorByLabel = true;
        defaults.edgeOpacity = 0.5;
        return defaults;
    }
    private boolean eventHandlersInstalled;
    private final Map<Color, PhongMaterial> emissiveMaterials = new HashMap<>();
    private double coordinateScale = 1.0;
    private static final double HYPERBOLIC_DISPLAY_RADIUS = 600.0;
    private ContextGraphProjectionMode projectionMode = ContextGraphProjectionMode.EUCLIDEAN;
    // Computed exclusively from GraphDirectedCollection nodes and edges.
    private Map<String, double[]> hyperbolicCoordinates = Map.of();
    private Map<String, double[]> projectedDisplayPositions = Map.of();
    private double[] hyperbolicFocus = {0.0, 0.0, 0.0};
    private long projectionGeneration;
    private AnimationTimer projectionUpdateTimer;

    // Coordinates are independent of the GraphNode's source vector.
    private final Map<String, double[]> layoutPositions = new HashMap<>();
    private final Map<String, ContextGraphNode3D> nodeViews = new HashMap<>();
    private final Map<GraphEdge, ContextGraphEdge3D> edgeViews = new java.util.IdentityHashMap<>();


    private String selectedNodeId;
    private GraphEdge selectedEdge;
    private final Map<ContextGraphNode3D, GraphNode> pickedNodes = new java.util.IdentityHashMap<>();
    private final Map<ContextGraphEdge3D, GraphEdge> pickedEdges = new java.util.IdentityHashMap<>();

    public GraphNode resolveNode(Node picked) {
        return picked instanceof ContextGraphNode3D node ? pickedNodes.get(node) : null;
    }

    public GraphEdge resolveEdge(Node picked) {
        return picked instanceof ContextGraphEdge3D edge ? pickedEdges.get(edge) : null;
    }

    /** Search graph entities without iterating through 3D scene nodes. */
    public List<ContextGraphEntitySearchResult> findEntities(String query, int maximum) {
        return entitySearchIndex.find(query, maximum);
    }

    /**
     * Select one entity explicitly. Unlike clicking a selected shape, repeated searches
     * must not toggle the selection off. Other selected entities are cleared.
     */
    public boolean selectNodeById(String entityId) {
        GraphNode model = topology.node(entityId);
        if (model == null) return false;
        clearAggregateSelection();
        if (selectedNodeId != null && !selectedNodeId.equals(entityId)) {
            ContextGraphNode3D old = nodeViews.get(selectedNodeId);
            if (old != null) old.unselect();
        }
        if (selectedEdge != null) {
            ContextGraphEdge3D old = edgeViews.get(selectedEdge);
            if (old != null) old.unselect();
            selectedEdge = null;
        }
        selectedNodeId = entityId;
        ContextGraphNode3D view = nodeViews.get(entityId);
        if (view != null) view.select();
        refreshNeighborhood();
        return true;
    }

    /** A selected model may be hidden by the user's FactorLabel filters. */
    public boolean isNodeVisibleByLabel(String entityId) {
        GraphNode node = topology.node(entityId);
        return node != null && nodeLabelVisible(node);
    }

    /** Relationship labels are treated as whole category names, like FactorLabel styling. */
    private static String relationshipType(GraphEdge edge) {
        return ContextGraphEdgeDisclosure.relationshipType(edge);
    }

    public ContextGraphEdgeDisplayMode getEdgeDisplayMode() { return edgeDisplayMode; }
    public boolean isBackboneReady() { return backboneReady; }
    public boolean isBackboneFailed() { return backboneFailed; }
    public int getBackboneEdgeCount() { return backboneEdges.size(); }
    public int getFocusedCrossLinkLimit() { return focusedCrossLinkLimit; }
    public Map<String, Integer> getRelationshipTypeCounts() { return relationshipTypeCounts; }
    public boolean isRelationshipTypeEnabled(String type) { return !hiddenRelationshipTypes.contains(type); }

    public void setEdgeDisplayMode(ContextGraphEdgeDisplayMode mode) {
        if (mode == null) return;
        edgeDisplayModeCustomized = true;
        if (edgeDisplayMode == mode) return;
        edgeDisplayMode = mode;
        clearAggregateSelection();
        if (mode == ContextGraphEdgeDisplayMode.COMMUNITY_AGGREGATE) requestAggregateEdges();
        else clearCommunityRepresentatives(false);
        // Edge display mode never changes source-entity visibility.
        refreshSourceNodeVisibility();
        updateAggregateLayerVisibility();
        applyEdgeVisibilityOnly();
        notifyExplorationState();
    }

    public void setFocusedCrossLinkLimit(int maximum) {
        if (maximum < 0 || maximum > 10000) {
            throw new IllegalArgumentException("Focused cross-link limit must be between 0 and 10000");
        }
        if (focusedCrossLinkLimit == maximum) return;
        focusedCrossLinkLimit = maximum;
        refreshFocusedCrossLinks();
        applyEdgeVisibilityOnly();
        notifyExplorationState();
    }

    public void setRelationshipTypeEnabled(String type, boolean enabled) {
        if (!relationshipTypeCounts.containsKey(type)) return;
        boolean changed = enabled ? hiddenRelationshipTypes.remove(type)
            : hiddenRelationshipTypes.add(type);
        if (!changed) return;
        refreshFocusedCrossLinks();
        applyEdgeVisibilityOnly();
        notifyExplorationState();
    }

    public void setAllRelationshipTypesEnabled(boolean enabled) {
        hiddenRelationshipTypes.clear();
        if (!enabled) hiddenRelationshipTypes.addAll(relationshipTypeCounts.keySet());
        refreshFocusedCrossLinks();
        applyEdgeVisibilityOnly();
        notifyExplorationState();
    }

    public int getVisibleRelationshipEdgeCount() {
        if (currentGraph == null) return 0;
        int count = 0;
        for (GraphEdge edge : currentGraph.getEdges()) if (edgeAllowed(edge)) count++;
        return count;
    }

    private void buildRelationshipTypeCounts(GraphDirectedCollection graph) {
        Map<String, Integer> counts = new TreeMap<>();
        for (GraphEdge edge : graph.getEdges()) {
            counts.merge(relationshipType(edge), 1, Integer::sum);
        }
        relationshipTypeCounts = Collections.unmodifiableMap(new LinkedHashMap<>(counts));
        hiddenRelationshipTypes.clear();
    }

    /** Show selected-entity cross-links on top of the exact layout backbone. */
    private void refreshFocusedCrossLinks() {
        focusedCrossLinks = ContextGraphEdgeDisclosure.focused(currentGraph, backboneReady,
            backboneEdges, selectedNodeId, selectedEdge, focusedCrossLinkLimit,
            hiddenRelationshipTypes);
    }

    private boolean edgeShownByStructure(GraphEdge edge) {
        if (edgeDisplayMode == ContextGraphEdgeDisplayMode.COMMUNITY_AGGREGATE)
            return revealedAggregateEdges.contains(edge)
                || revealedCommunityEdges.contains(edge) || edge == selectedEdge
                || (selectedNodeId != null && (selectedNodeId.equals(edge.getStartID())
                    || selectedNodeId.equals(edge.getEndID())));
        return ContextGraphEdgeDisclosure.shown(edgeDisplayMode, edge, backboneReady,
            backboneEdges, focusedCrossLinks);
    }

    /** Only touches already-attached FX edge views; progressive attachments use edgeAllowed. */
    private void applyEdgeVisibilityOnly() {
        for (Map.Entry<GraphEdge, ContextGraphEdge3D> entry : edgeViews.entrySet()) {
            entry.getValue().setVisible(edgeAllowed(entry.getKey()));
        }
        refreshAggregateVisibility();
    }

    /** All changes to the derived aggregate scene are FX-thread operations. */
    private void invalidateAggregation() {
        clearCommunityRepresentatives(true);
        ++aggregationGeneration;
        if (aggregationTask != null) { aggregationTask.cancel(true); aggregationTask = null; }
        if (aggregateAttachmentTimer != null) {
            aggregateAttachmentTimer.stop();
            aggregateAttachmentTimer = null;
        }
        aggregationCache.clear();
        aggregation = null;
        clearAggregateSelection();
        aggregateViews.clear();
        pickedAggregates.clear();
        aggregateValidPositions.clear();
        aggregateEdgeLayer.getChildren().clear();
    }

    private void clearAggregateSelection() {
        if (selectedAggregate != null) {
            ContextGraphEdge3D previous = findAggregateView(selectedAggregate);
            if (previous != null) previous.unselect();
        }
        selectedAggregate = null;
        revealedAggregateEdges.clear();
        if (!revealedAggregateEndpoints.isEmpty()) {
            revealedAggregateEndpoints = Set.of();
            refreshSourceNodeVisibility();
        }
    }

    /** Click a derived connection to disclose the original GraphEdge objects it represents. */
    public void toggleAggregateSelection(ContextGraphCommunityEdgeBundle connection) {
        if (connection == null || findAggregateView(connection) == null) return;
        boolean wasSelected = selectedAggregate == connection;
        clearSelections();
        if (!wasSelected) {
            selectedAggregate = connection;
            revealedAggregateEdges.addAll(connection.originals());
            Set<String> endpoints = new HashSet<>();
            for (GraphEdge edge : connection.originals()) {
                endpoints.add(edge.getStartID());
                endpoints.add(edge.getEndID());
            }
            revealedAggregateEndpoints = Set.copyOf(endpoints);
            refreshSourceNodeVisibility();
            findAggregateView(connection).select();
            report(connection.summary() + " | original edges revealed; click again to hide");
        }
        applyEdgeVisibilityOnly();
        notifyExplorationState();
    }

    private void updateAggregateLayerVisibility() {
        boolean visible = edgesDisplayed
            && edgeDisplayMode == ContextGraphEdgeDisplayMode.COMMUNITY_AGGREGATE;
        aggregateEdgeLayer.setVisible(visible);
        // Community centroids are no longer substitutes for original source entities.
        // Preserve the internal layers for compatibility with old derived state, but
        // do not show them when only relationships have been summarized.
        communityExpansionEdgeLayer.setVisible(false);
        communityRepresentativeLayer.setVisible(false);
    }

    /** Heavy aggregation is off-FX, keyed by actual hierarchy and level. */
    private void requestAggregateEdges() {
        if (edgeDisplayMode != ContextGraphEdgeDisplayMode.COMMUNITY_AGGREGATE
            || currentGraph == null || communityHierarchy == null
            || communityHierarchy.levels().isEmpty()) return;
        final int level = Math.max(0, Math.min(communityHierarchy.levels().size() - 1,
            communityDiagnosticLevel));
        final GraphDirectedCollection graph = currentGraph;
        final ContextGraphCommunityHierarchy hierarchy = communityHierarchy;
        final long generation = ++aggregationGeneration;
        if (aggregationTask != null) { aggregationTask.cancel(true); aggregationTask = null; }
        if (aggregateAttachmentTimer != null) {
            aggregateAttachmentTimer.stop();
            aggregateAttachmentTimer = null;
        }
        aggregation = null;
        clearAggregateSelection();
        clearCommunityRepresentatives(false);
        refreshSourceNodeVisibility();
        aggregateViews.clear();
        pickedAggregates.clear();
        aggregateValidPositions.clear();
        aggregateEdgeLayer.getChildren().clear();
        ContextGraphCommunityEdgeAggregation cached = aggregationCache.get(level);
        if (cached != null) {
            displayAggregates(graph, hierarchy, generation, cached);
            return;
        }
        final ContextGraphCommunityCatalog[] catalog = new ContextGraphCommunityCatalog[1];
        final boolean needsCatalog = communityCatalog == null;
        Task<ContextGraphCommunityEdgeAggregation> task = new Task<>() {
            @Override protected ContextGraphCommunityEdgeAggregation call() {
                if (needsCatalog)
                    catalog[0] = ContextGraphCommunityCatalog.build(graph, hierarchy);
                return ContextGraphCommunityEdgeAggregation.compute(graph, hierarchy, level);
            }
        };
        aggregationTask = task;
        task.setOnSucceeded(e -> {
            if (generation != aggregationGeneration || graph != currentGraph
                || hierarchy != communityHierarchy || task.isCancelled()) return;
            aggregationTask = null;
            ContextGraphCommunityEdgeAggregation result = task.getValue();
            if (result == null) return;
            aggregationCache.put(level, result);
            if (catalog[0] != null) communityCatalog = catalog[0];
            displayAggregates(graph, hierarchy, generation, result);
        });
        task.setOnFailed(e -> {
            if (generation != aggregationGeneration || graph != currentGraph) return;
            aggregationTask = null;
            report("Community connection aggregation failed: " + task.getException());
            notifyExplorationState();
        });
        Thread worker = new Thread(task, "context-graph-community-edge-aggregation");
        worker.setDaemon(true);
        worker.start();
    }

    private void displayAggregates(GraphDirectedCollection graph,
                                   ContextGraphCommunityHierarchy hierarchy, long generation,
                                   ContextGraphCommunityEdgeAggregation result) {
        if (generation != aggregationGeneration || graph != currentGraph
            || hierarchy != communityHierarchy
            || edgeDisplayMode != ContextGraphEdgeDisplayMode.COMMUNITY_AGGREGATE) return;
        aggregation = result;
        Point3D[] centroids = aggregateCentroids(result.levelIndex());
        List<ContextGraphCommunityEdgeBundle> connections = result.connections();
        // Attach only derived relationships; the original entity shapes remain visible.
        // No derived community centroids are constructed or attached in this mode.
        aggregateAttachmentTimer = new AnimationTimer() {
            private int next;
            @Override public void handle(long now) {
                if (generation != aggregationGeneration || graph != currentGraph
                    || hierarchy != communityHierarchy
                    || edgeDisplayMode != ContextGraphEdgeDisplayMode.COMMUNITY_AGGREGATE) {
                    stop();
                    if (aggregateAttachmentTimer == this) aggregateAttachmentTimer = null;
                    return;
                }
                int limit = Math.min(connections.size(), next + edgeBatchSize);
                List<ContextGraphEdge3D> added = new ArrayList<>(limit - next);
                for (; next < limit; next++) {
                    ContextGraphCommunityEdgeBundle connection = connections.get(next);
                    ContextGraphEdge3D view = new ContextGraphEdge3D(0.5f, 5, 1);
                    aggregateViews.put(connection, view);
                    pickedAggregates.put(view, connection);
                    view.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED,
                        e -> report(connection.summary()));
                    ContextGraphCommunityPair pair = connection.pair();
                    Point3D a = centroids[pair.first()], b = centroids[pair.second()];
                    boolean valid = a != null && b != null && a.distance(b) >= 1.0e-6;
                    if (valid) {
                        view.setEndpoints(a, b);
                        aggregateValidPositions.add(connection);
                    }
                    int filteredCount = filteredAggregateCount(connection);
                    applyAggregateStyle(view, filteredCount);
                    view.setVisible(valid && filteredCount > 0);
                    added.add(view);
                }
                aggregateEdgeLayer.getChildren().addAll(added);
                if (next >= connections.size()) {
                    stop();
                    if (aggregateAttachmentTimer == this) aggregateAttachmentTimer = null;
                    notifyExplorationState();
                }
            }
        };
        updateAggregateLayerVisibility();
        notifyExplorationState();
        aggregateAttachmentTimer.start();
    }

    private int filteredAggregateCount(ContextGraphCommunityEdgeBundle connection) {
        int count = 0;
        for (GraphEdge edge : connection.originals()) {
            if (!hiddenRelationshipTypes.contains(relationshipType(edge)) && edgeLabelVisible(edge)) count++;
        }
        return count;
    }

    private void refreshAggregateVisibility() {
        if (aggregation == null) return;
        for (Map.Entry<ContextGraphCommunityEdgeBundle, ContextGraphEdge3D> entry
            : aggregateViews.entrySet()) {
            int filteredCount = filteredAggregateCount(entry.getKey());
            applyAggregateStyle(entry.getValue(), filteredCount);
            entry.getValue().setVisible(aggregateValidPositions.contains(entry.getKey())
                && filteredCount > 0);
        }
        for (Map.Entry<ContextGraphCommunityChildConnection, ContextGraphEdge3D> entry
            : childConnectionViews.entrySet()) {
            int represented = filteredAggregateCount(entry.getKey().bundle());
            applyAggregateStyle(entry.getValue(), represented);
            entry.getValue().setVisible(childValidPositions.contains(entry.getKey())
                && represented > 0);
        }
    }

    private void applyAggregateStyle(ContextGraphEdge3D view, int representedCount) {
        // Derived connections use neutral styling, distinct from original category edges.
        view.setBaseMaterial(displayMaterial(Color.rgb(155, 186, 218), styleParams.edgeOpacity));
        double radius = Math.max(0.001, styleParams.edgeWidth)
            * Math.min(3.5, 0.85 + 0.38 * Math.log1p(representedCount));
        if (Math.abs(view.getRadius() - radius) > 0.0001) view.setRadius((float) radius);
    }

    private void applyAggregateMaterials() {
        for (Map.Entry<ContextGraphCommunityEdgeBundle, ContextGraphEdge3D> entry
            : aggregateViews.entrySet()) {
            applyAggregateStyle(entry.getValue(), filteredAggregateCount(entry.getKey()));
        }
        for (Map.Entry<ContextGraphCommunityChildConnection, ContextGraphEdge3D> entry
            : childConnectionViews.entrySet())
            applyAggregateStyle(entry.getValue(), filteredAggregateCount(entry.getKey().bundle()));
    }

    /** Centroids use currently displayed coordinates, including hyperbolic focus. */
    private Point3D[] aggregateCentroids(int levelIndex) {
        ContextGraphCommunityLevel level = communityHierarchy.levels().get(levelIndex);
        double[][] sums = new double[level.count()][3];
        int[] counts = new int[level.count()];
        List<String> ids = communityHierarchy.entityIds();
        for (int i = 0; i < ids.size(); i++) {
            GraphNode node = topology.node(ids.get(i));
            if (node == null) continue;
            double[] position = scaledPosition(node);
            int group = level.communityOf(i);
            for (int k = 0; k < 3; k++) sums[group][k] += position[k];
            counts[group]++;
        }
        Point3D[] centroids = new Point3D[counts.length];
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > 0) centroids[i] = new Point3D(
                sums[i][0] / counts[i], sums[i][1] / counts[i], sums[i][2] / counts[i]);
        }
        return centroids;
    }

    private void updateAggregateEndpoints() {
        if (aggregation == null || communityHierarchy == null) return;
        Point3D[] centroids = aggregateCentroids(aggregation.levelIndex());
        aggregateValidPositions.clear();
        for (Map.Entry<ContextGraphCommunityEdgeBundle, ContextGraphEdge3D> entry
            : aggregateViews.entrySet()) {
            ContextGraphCommunityPair pair = entry.getKey().pair();
            Point3D from = centroids[pair.first()], to = centroids[pair.second()];
            if (from == null || to == null || from.distance(to) < 1.0e-6) continue;
            entry.getValue().setEndpoints(from, to);
            aggregateValidPositions.add(entry.getKey());
        }
        updateCommunityRepresentativePositions();
        refreshAggregateVisibility();
    }

    /** The selected community is a derived identity, never a GraphNode. */
    public void selectCommunity(ContextGraphCommunityRef ref) {
        ContextGraphNode3D view = communityViews.get(ref);
        if (view == null) return;
        clearCommunitySelection();
        if (selectedNodeId != null) {
            ContextGraphNode3D previous = nodeViews.get(selectedNodeId);
            if (previous != null) previous.unselect();
        }
        if (selectedEdge != null) {
            ContextGraphEdge3D previous = edgeViews.get(selectedEdge);
            if (previous != null) previous.unselect();
        }
        selectedNodeId = null;
        selectedEdge = null;
        clearAggregateSelection();
        selectedCommunity = ref;
        view.select();
        refreshNeighborhood();
        report(communityInspection(ref));
    }

    private void clearCommunitySelection() {
        if (selectedCommunity != null) {
            ContextGraphNode3D view = communityViews.get(selectedCommunity);
            if (view != null) view.unselect();
        }
        selectedCommunity = null;
    }

    public String communityInspection(ContextGraphCommunityRef ref) {
        if (communityCatalog == null) return "No community descriptions available";
        ContextGraphCommunityDescriptor descriptor = communityCatalog.descriptor(ref);
        if (descriptor == null) return "Community not available";
        StringBuilder details = new StringBuilder(descriptor.details());
        ContextGraphCommunityEdgeAggregation related = aggregationCache.get(ref.level());
        if (related != null) {
            List<ContextGraphCommunityEdgeBundle> neighbors = new ArrayList<>();
            for (ContextGraphCommunityEdgeBundle connection : related.connections()) {
                if (connection.pair().first() == ref.id() || connection.pair().second() == ref.id())
                    neighbors.add(connection);
            }
            neighbors.sort(java.util.Comparator.comparingInt(
                ContextGraphCommunityEdgeBundle::originalCount).reversed());
            details.append("\n\nNeighboring communities: ").append(neighbors.size());
            for (int i = 0; i < Math.min(6, neighbors.size()); i++) {
                ContextGraphCommunityEdgeBundle connection = neighbors.get(i);
                int neighborId = connection.pair().first() == ref.id()
                    ? connection.pair().second() : connection.pair().first();
                ContextGraphCommunityDescriptor neighbor = communityCatalog.descriptor(
                    new ContextGraphCommunityRef(ref.level(), neighborId));
                details.append("\n  ").append(neighbor == null ? "Community " + (neighborId + 1)
                    : neighbor.title() + " [" + (neighborId + 1) + "]")
                    .append(": ").append(connection.originalCount())
                    .append(" original edges ").append(connection.typeCounts());
            }
        }
        details.append("\n\nState: ").append(isCommunityExpanded(ref) ? "EXPANDED" : "COLLAPSED");
        details.append("\nSingle-click: inspect. Double-click: expand or collapse")
            .append(". At the finest community level, double-click reveals original members and internal edges.");
        if (ref.equals(revealedEntityCommunity))
            details.append("\nRevealed original entities: ").append(revealedCommunityMembers.size())
                .append(", internal edges: ").append(revealedCommunityEdges.size());
        return details.toString();
    }

    /** Expand a single branch at each level, retaining all coarse community representatives. */
    public void toggleCommunityExpansion(ContextGraphCommunityRef ref) {
        if (communityCatalog == null || aggregation == null
            || communityCatalog.descriptor(ref) == null || !communityViews.containsKey(ref)) return;
        if (ref.level() == 0) {
            boolean collapse = ref.equals(revealedEntityCommunity);
            revealedEntityCommunity = collapse ? null : ref;
            revealedCommunityMembers = collapse ? Set.of() : communityCatalog.members(ref);
            revealedCommunityEdges.clear();
            if (!collapse) {
                for (GraphEdge edge : currentGraph.getEdges()) {
                    if (revealedCommunityMembers.contains(edge.getStartID())
                        && revealedCommunityMembers.contains(edge.getEndID()))
                        revealedCommunityEdges.add(edge);
                }
            }
            scheduleCommunityMaterialRefresh();
            refreshSourceNodeVisibility();
            applyEdgeVisibilityOnly();
            notifyExplorationState();
            report(collapse ? "Collapsed original entities for community " + (ref.id() + 1)
                : "Revealed " + revealedCommunityMembers.size() + " original entities and "
                    + revealedCommunityEdges.size() + " original internal edges for community "
                    + (ref.id() + 1));
            return;
        }
        int previous = expansionPath.indexOf(ref);
        if (previous >= 0) {
            expansionPath.subList(previous, expansionPath.size()).clear();
        } else if (ref.level() == aggregation.levelIndex()) {
            expansionPath.clear();
            expansionPath.add(ref);
        } else if (!expansionPath.isEmpty()) {
            ContextGraphCommunityRef parent = expansionPath.get(expansionPath.size() - 1);
            ContextGraphCommunityDescriptor parentDescriptor = communityCatalog.descriptor(parent);
            if (parentDescriptor == null || !ContextGraphCommunityDrilldown
                .visibleChildren(parent, communityCatalog::descriptor).contains(ref)) return;
            expansionPath.add(ref);
        } else return;
        revealedEntityCommunity = null;
        revealedCommunityMembers = Set.of();
        revealedCommunityEdges.clear();
        scheduleCommunityMaterialRefresh();
        refreshSourceNodeVisibility();
        applyEdgeVisibilityOnly();
        report((previous >= 0 ? "Collapsed" : "Expanded") + " derived community "
            + (ref.id() + 1) + " at level " + (ref.level() + 1)
            + "; original entities remain hidden until a leaf is opened");
        requestExpandedCommunities();
    }

    private ContextGraphEdge3D findAggregateView(ContextGraphCommunityEdgeBundle connection) {
        ContextGraphEdge3D view = aggregateViews.get(connection);
        if (view != null) return view;
        for (Map.Entry<ContextGraphCommunityChildConnection, ContextGraphEdge3D> entry
            : childConnectionViews.entrySet())
            if (entry.getKey().bundle() == connection) return entry.getValue();
        return null;
    }

    private void clearCommunityRepresentatives(boolean dropCatalog) {
        boolean hadOriginalMemberHighlight = revealedEntityCommunity != null;
        ++expansionGeneration;
        if (expansionTask != null) { expansionTask.cancel(true); expansionTask = null; }
        if (expansionAttachmentTimer != null) {
            expansionAttachmentTimer.stop();
            expansionAttachmentTimer = null;
        }
        clearCommunitySelection();
        expansionPath.clear();
        revealedEntityCommunity = null;
        revealedCommunityMembers = Set.of();
        revealedCommunityEdges.clear();
        communityViews.clear();
        pickedCommunities.clear();
        for (ContextGraphEdge3D child : childConnectionViews.values())
            pickedAggregates.remove(child);
        childConnectionViews.clear();
        childValidPositions.clear();
        communityRepresentativeLayer.getChildren().clear();
        communityExpansionEdgeLayer.getChildren().clear();
        if (dropCatalog) communityCatalog = null;
        if (hadOriginalMemberHighlight && !dropCatalog && currentGraph != null)
            scheduleCommunityMaterialRefresh();
        refreshSourceNodeVisibility();
    }

    private AnimationTimer expansionAttachmentTimer;

    private void attachCommunityRepresentative(ContextGraphCommunityDescriptor descriptor,
                                               Point3D center) {
        ContextGraphCommunityRef ref = descriptor.ref();
        if (communityViews.containsKey(ref)) return;
        double radius = Math.min(9.0, 2.5 + 0.75 * Math.log1p(descriptor.memberCount()));
        Color color = Color.hsb((ref.id() * 137.50776405003785
            + ref.level() * 23.0) % 360.0, 0.68, 0.98);
        ContextGraphNode3D glyph = new ContextGraphNode3D(
            displayMaterial(color, 1.0), radius, 12, true);
        glyph.setTranslateX(center.getX());
        glyph.setTranslateY(center.getY());
        glyph.setTranslateZ(center.getZ());
        communityViews.put(ref, glyph);
        pickedCommunities.put(glyph, ref);
        glyph.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED,
            e -> report("Derived community " + (ref.id() + 1) + " | " + descriptor.title()
                + " | " + descriptor.memberCount() + " original entities"));
        javafx.scene.control.Tooltip.install(glyph, new javafx.scene.control.Tooltip(
            descriptor.title() + " — " + descriptor.memberCount()
                + " original entities\nClick to inspect; double-click to expand/collapse"));
        if (ref.equals(selectedCommunity)) glyph.select();
        communityRepresentativeLayer.getChildren().add(glyph);
    }

    /** Do not run graph aggregation on the FX thread during drill-down. */
    private void requestExpandedCommunities() {
        long generation = ++expansionGeneration;
        if (expansionTask != null) { expansionTask.cancel(true); expansionTask = null; }
        if (expansionAttachmentTimer != null) { expansionAttachmentTimer.stop(); expansionAttachmentTimer = null; }
        for (Map.Entry<ContextGraphCommunityRef, ContextGraphNode3D> entry
            : new ArrayList<>(communityViews.entrySet())) {
            if (aggregation == null || entry.getKey().level() == aggregation.levelIndex()) continue;
            communityViews.remove(entry.getKey());
            pickedCommunities.remove(entry.getValue());
            communityRepresentativeLayer.getChildren().remove(entry.getValue());
        }
        if (selectedAggregate != null && findAggregateView(selectedAggregate) != aggregateViews.get(selectedAggregate))
            clearAggregateSelection();
        for (ContextGraphEdge3D child : childConnectionViews.values())
            pickedAggregates.remove(child);
        childConnectionViews.clear();
        childValidPositions.clear();
        communityExpansionEdgeLayer.getChildren().clear();
        if (expansionPath.isEmpty()) {
            if (selectedCommunity != null && !communityViews.containsKey(selectedCommunity))
                clearCommunitySelection();
            notifyExplorationState();
            return;
        }
        GraphDirectedCollection graph = currentGraph;
        ContextGraphCommunityHierarchy hierarchy = communityHierarchy;
        ContextGraphCommunityCatalog catalog = communityCatalog;
        if (catalog == null) return;
        List<ContextGraphCommunityRef> path = List.copyOf(expansionPath);
        Map<Integer, ContextGraphCommunityEdgeAggregation> already = new HashMap<>(aggregationCache);
        Task<Map<Integer, ContextGraphCommunityEdgeAggregation>> task = new Task<>() {
            @Override protected Map<Integer, ContextGraphCommunityEdgeAggregation> call() {
                for (ContextGraphCommunityRef parent : path) {
                    if (isCancelled()) throw new java.util.concurrent.CancellationException();
                    List<ContextGraphCommunityRef> visibleChildren = ContextGraphCommunityDrilldown
                        .visibleChildren(parent, catalog::descriptor);
                    if (visibleChildren.isEmpty()) continue;
                    int childLevel = visibleChildren.get(0).level();
                    if (!already.containsKey(childLevel)) already.put(childLevel,
                        ContextGraphCommunityEdgeAggregation.compute(graph, hierarchy, childLevel));
                }
                return already;
            }
        };
        expansionTask = task;
        task.setOnSucceeded(e -> {
            if (generation != expansionGeneration || graph != currentGraph
                || hierarchy != communityHierarchy || task.isCancelled()) return;
            expansionTask = null;
            Map<Integer, ContextGraphCommunityEdgeAggregation> computed = task.getValue();
            aggregationCache.putAll(computed);
            attachExpandedCommunities(generation, path, computed);
        });
        task.setOnFailed(e -> {
            if (generation == expansionGeneration) {
                expansionTask = null;
                report("Community expansion failed: " + task.getException());
            }
        });
        Thread worker = new Thread(task, "context-graph-community-expansion");
        worker.setDaemon(true);
        worker.start();
    }

    private void attachExpandedCommunities(long generation, List<ContextGraphCommunityRef> path,
                 Map<Integer, ContextGraphCommunityEdgeAggregation> computed) {
        List<ContextGraphCommunityDescriptor> children = new ArrayList<>();
        List<ContextGraphCommunityChildConnection> links = new ArrayList<>();
        Map<Integer, Point3D[]> centroids = new HashMap<>();
        for (ContextGraphCommunityRef parent : path) {
            ContextGraphCommunityDescriptor descriptor = communityCatalog.descriptor(parent);
            if (descriptor == null) continue;
            List<ContextGraphCommunityRef> visibleChildren = ContextGraphCommunityDrilldown
                .visibleChildren(parent, communityCatalog::descriptor);
            if (visibleChildren.isEmpty()) continue;
            int childLevel = visibleChildren.get(0).level();
            centroids.computeIfAbsent(childLevel, this::aggregateCentroids);
            Set<Integer> siblingIds = new HashSet<>();
            for (ContextGraphCommunityRef childRef : visibleChildren) {
                siblingIds.add(childRef.id());
                ContextGraphCommunityDescriptor child = communityCatalog.descriptor(childRef);
                if (child != null) children.add(child);
            }
            ContextGraphCommunityEdgeAggregation childAggregation = computed.get(childLevel);
            if (childAggregation == null) continue;
            for (ContextGraphCommunityEdgeBundle bundle : childAggregation.connections()) {
                if (siblingIds.contains(bundle.pair().first())
                    && siblingIds.contains(bundle.pair().second()))
                    links.add(new ContextGraphCommunityChildConnection(childLevel, bundle));
            }
        }
        expansionAttachmentTimer = new AnimationTimer() {
            private int nextChild;
            private int nextLink;
            @Override public void handle(long now) {
                if (generation != expansionGeneration || edgeDisplayMode
                    != ContextGraphEdgeDisplayMode.COMMUNITY_AGGREGATE) {
                    stop();
                    if (expansionAttachmentTimer == this) expansionAttachmentTimer = null;
                    return;
                }
                int childLimit = Math.min(children.size(), nextChild + edgeBatchSize);
                for (; nextChild < childLimit; nextChild++) {
                    ContextGraphCommunityDescriptor child = children.get(nextChild);
                    Point3D center = centroids.get(child.ref().level())[child.ref().id()];
                    if (center != null) attachCommunityRepresentative(child, center);
                }
                int linkLimit = Math.min(links.size(), nextLink + edgeBatchSize);
                for (; nextLink < linkLimit; nextLink++) {
                    ContextGraphCommunityChildConnection key = links.get(nextLink);
                    Point3D[] positions = centroids.get(key.level());
                    Point3D from = positions[key.bundle().pair().first()];
                    Point3D to = positions[key.bundle().pair().second()];
                    if (from == null || to == null || from.distance(to) < 1.0e-6) continue;
                    ContextGraphEdge3D view = new ContextGraphEdge3D(0.5f, 5, 1);
                    view.setEndpoints(from, to);
                    view.setBaseMaterial(displayMaterial(Color.rgb(107, 149, 178), styleParams.edgeOpacity));
                    applyAggregateStyle(view, filteredAggregateCount(key.bundle()));
                    view.setVisible(filteredAggregateCount(key.bundle()) > 0);
                    view.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED,
                        e -> report(key.bundle().summary()));
                    childConnectionViews.put(key, view);
                    childValidPositions.add(key);
                    pickedAggregates.put(view, key.bundle());
                    communityExpansionEdgeLayer.getChildren().add(view);
                }
                if (nextChild >= children.size() && nextLink >= links.size()) {
                    stop();
                    if (expansionAttachmentTimer == this) expansionAttachmentTimer = null;
                    notifyExplorationState();
                }
            }
        };
        expansionAttachmentTimer.start();
    }

    private void updateCommunityRepresentativePositions() {
        if (communityHierarchy == null || communityViews.isEmpty()) return;
        Map<Integer, Point3D[]> positions = new HashMap<>();
        for (Map.Entry<ContextGraphCommunityRef, ContextGraphNode3D> entry : communityViews.entrySet()) {
            ContextGraphCommunityRef ref = entry.getKey();
            Point3D point = positions.computeIfAbsent(ref.level(), this::aggregateCentroids)[ref.id()];
            if (point == null) continue;
            ContextGraphNode3D view = entry.getValue();
            view.setTranslateX(point.getX());
            view.setTranslateY(point.getY());
            view.setTranslateZ(point.getZ());
        }
        childValidPositions.clear();
        for (Map.Entry<ContextGraphCommunityChildConnection, ContextGraphEdge3D> entry
            : childConnectionViews.entrySet()) {
            ContextGraphCommunityChildConnection key = entry.getKey();
            Point3D[] coords = positions.computeIfAbsent(key.level(), this::aggregateCentroids);
            Point3D from = coords[key.bundle().pair().first()];
            Point3D to = coords[key.bundle().pair().second()];
            if (from == null || to == null || from.distance(to) < 1.0e-6) {
                entry.getValue().setVisible(false);
            } else {
                entry.getValue().setEndpoints(from, to);
                childValidPositions.add(key);
            }
        }
    }

    private void prepareBackbone(GraphDirectedCollection graph) {
        prepareBackbone(graph, false);
    }

    /** One worker produces communities AND graph-backed topology geometry. */
    private void prepareBackbone(GraphDirectedCollection graph, boolean force) {
        final long generation = ++backboneGeneration;
        // Any projection animation based on prior coordinates must not continue
        // while the hierarchy and layout are being replaced.
        ++projectionGeneration;
        if (projectionUpdateTimer != null) {
            projectionUpdateTimer.stop();
            projectionUpdateTimer = null;
        }
        if (communityTask != null) {
            communityTask.cancel(true);
            communityTask = null;
        }
        final ContextGraphCommunityParams params = communityParams;
        final ContextGraphHyperbolicPackingParams packing = packingParams;
        if (force) {
            hierarchyCache.remove(params);
            layoutCache.remove(params);
        }
        ContextGraphCommunityHierarchy cachedHierarchy = hierarchyCache.get(params);
        communityStatus = cachedHierarchy == null
            ? "Computing semantic hierarchy and spatial layout..."
            : "Reusing semantic hierarchy; computing spatial layout...";
        Map<ContextGraphHyperbolicPackingParams, ContextGraphCommunityLayoutResult> variants =
            layoutCache.get(params);
        if (!force && variants != null) {
            ContextGraphCommunityLayoutResult cached = variants.get(packing);
            if (cached != null) {
                applyCommunityAnalysis(graph, generation, cached);
                return;
            }
        }
        Task<ContextGraphCommunityLayoutResult> task = new Task<>() {
            @Override protected ContextGraphCommunityLayoutResult call() {
                ContextGraphCommunityHierarchy hierarchy = cachedHierarchy == null
                    ? ContextGraphCommunityDetection.detect(graph, params) : cachedHierarchy;
                if (isCancelled()) return null;
                ContextGraphCommunitySpatialLayout.Result packed =
                    ContextGraphCommunitySpatialLayout.compute(graph, hierarchy, packing);
                if (isCancelled()) return null;
                return new ContextGraphCommunityLayoutResult(hierarchy, packed.layout(),
                    packed.diagnostics());
            }
        };
        communityTask = task;
        fireOnRoot(new GraphEvent(GraphEvent.GRAPH_COMMUNITY_HIERARCHY_CHANGED));
        task.setOnSucceeded(event -> {
            if (generation != backboneGeneration || graph != currentGraph || task.isCancelled()) return;
            communityTask = null;
            ContextGraphCommunityLayoutResult result = task.getValue();
            if (result == null) return;
            hierarchyCache.put(params, result.hierarchy());
            layoutCache.computeIfAbsent(params, ignored -> new LinkedHashMap<>())
                .put(packing, result);
            // Bound cached configurations only; never limit the graph size.
            Map<ContextGraphHyperbolicPackingParams, ContextGraphCommunityLayoutResult> cachedLayouts =
                layoutCache.get(params);
            if (cachedLayouts.size() > 4)
                cachedLayouts.remove(cachedLayouts.keySet().iterator().next());
            if (hierarchyCache.size() > 6) {
                ContextGraphCommunityParams eldest = hierarchyCache.keySet().iterator().next();
                hierarchyCache.remove(eldest);
                layoutCache.remove(eldest);
            }
            applyCommunityAnalysis(graph, generation, result);
        });
        task.setOnFailed(event -> {
            if (generation != backboneGeneration || graph != currentGraph) return;
            communityTask = null;
            backboneFailed = !backboneReady;
            communityStatus = "Community hierarchy failed: " + task.getException();
            fireOnRoot(new GraphEvent(GraphEvent.GRAPH_COMMUNITY_HIERARCHY_CHANGED));
            report(communityStatus);
            notifyExplorationState();
            if (projectionMode.isHyperbolic() && hyperbolicCoordinates.isEmpty()) {
                projectionMode = ContextGraphProjectionMode.EUCLIDEAN;
                applyDefaultEdgeModeForProjection(projectionMode);
                fireOnRoot(new GraphEvent(GraphEvent.GRAPH_PROJECTION_FAILED, projectionMode));
            }
        });
        Thread worker = new Thread(task, "context-graph-community-layout");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyCommunityAnalysis(GraphDirectedCollection graph, long generation,
                                        ContextGraphCommunityLayoutResult result) {
        if (generation != backboneGeneration || graph != currentGraph) return;
        ContextGraphHyperbolicLayout.LayoutResult layout = result.layout();
        if (spatialDiagnostics != null && result.diagnostics() != spatialDiagnostics)
            spatialComparison = result.diagnostics().comparison(spatialDiagnostics);
        else spatialComparison = "";
        spatialDiagnostics = result.diagnostics();
        if (communityHierarchy != null && communityHierarchy != result.hierarchy())
            partitionComparison = ContextGraphCommunityPartitionComparison.compare(
                communityHierarchy, result.hierarchy());
        boolean hierarchyChanged = communityHierarchy != result.hierarchy();
        if (hierarchyChanged) invalidateAggregation();
        communityHierarchy = result.hierarchy();
        refreshDiagnosticMembership();
        if (edgeDisplayMode == ContextGraphEdgeDisplayMode.COMMUNITY_AGGREGATE
            && (hierarchyChanged || aggregation == null)) requestAggregateEdges();
        if (communityColorDiagnostic) scheduleCommunityMaterialRefresh();
        communityStatus = "Ready";
        hyperbolicCoordinates = layout.positions();
        backboneEdges = layout.backboneEdges();
        backboneReady = true;
        backboneFailed = false;
        refreshFocusedCrossLinks();
        applyEdgeVisibilityOnly();
        notifyExplorationState();
        fireOnRoot(new GraphEvent(GraphEvent.GRAPH_COMMUNITY_HIERARCHY_CHANGED));
        if (projectionMode.isHyperbolic()) scheduleProjectionReposition();
    }

    private void applyDefaultEdgeModeForProjection(ContextGraphProjectionMode mode) {
        if (!edgeDisplayModeCustomized) {
            edgeDisplayMode = mode.isHyperbolic()
                ? ContextGraphEdgeDisplayMode.BACKBONE_WITH_FOCUS
                : ContextGraphEdgeDisplayMode.ALL;
        }
        updateAggregateLayerVisibility();
        refreshSourceNodeVisibility();
        applyEdgeVisibilityOnly();
        notifyExplorationState();
    }

    public String getSelectedNodeId() { return selectedNodeId; }
    public GraphEdge getSelectedEdge() { return selectedEdge; }
    public GraphNode getSelectedNode() { return topology.node(selectedNodeId); }
    public ContextGraphNode3D getSelectedNodeView() { return nodeViews.get(selectedNodeId); }
    public ContextGraphEdge3D getSelectedEdgeView() { return edgeViews.get(selectedEdge); }
    public boolean isNeighborhoodHighlightEnabled() { return neighborhoodHighlightEnabled; }
    public boolean isDimOutsideNeighborhood() { return dimOutsideNeighborhood; }

    public void setDimOutsideNeighborhood(boolean dimmed) {
        dimOutsideNeighborhood = dimmed;
        applyExplorationState();
        notifyExplorationState();
    }
    public boolean isSelectionIsolated() { return isolateSelection; }
    public int getNeighborhoodDepth() { return neighborhoodDepth; }
    public int getNeighborhoodNodeCount() { return neighborhood.nodeCount(); }
    public int getNeighborhoodEdgeCount() { return neighborhood.edgeCount(); }

    public void setNeighborhoodHighlightEnabled(boolean enabled) {
        neighborhoodHighlightEnabled = enabled;
        applyExplorationState();
        notifyExplorationState();
    }

    public void setNeighborhoodDepth(int depth) {
        if (depth < 1) throw new IllegalArgumentException("Neighborhood depth must be >= 1");
        if (neighborhoodDepth == depth) return;
        neighborhoodDepth = depth;
        refreshNeighborhood();
        notifyExplorationState();
    }

    public void setSelectionIsolated(boolean isolated) {
        if (isolateSelection == isolated) return;
        isolateSelection = isolated;
        applyExplorationState();
        notifyExplorationState();
    }

    /** Restores the complete category-filtered view; selection is preserved. */
    public void showAll() {
        isolateSelection = false;
        neighborhoodHighlightEnabled = false;
        applyExplorationState();
        notifyExplorationState();
    }

    public List<GraphNode> getSelectedEntityNodes() {
        List<GraphNode> result = new ArrayList<>();
        GraphNode node = topology.node(selectedNodeId);
        if (node != null) result.add(node);
        if (selectedEdge != null) {
            GraphNode from = topology.node(selectedEdge.getStartID());
            GraphNode to = topology.node(selectedEdge.getEndID());
            if (from != null && !result.contains(from)) result.add(from);
            if (to != null && !result.contains(to)) result.add(to);
        }
        return result;
    }

    public List<GraphNode> getNeighborhoodNodes() {
        List<GraphNode> result = new ArrayList<>();
        for (String id : neighborhood.nodeIds()) {
            GraphNode node = topology.node(id);
            if (node != null && nodeLabelVisible(node)) result.add(node);
        }
        return result;
    }

    private void notifyExplorationState() {
        fireOnRoot(new GraphEvent(GraphEvent.GRAPH_EXPLORATION_STATE_CHANGED));
    }

    private void refreshNeighborhood() {
        Set<String> roots = new LinkedHashSet<>();
        if (selectedNodeId != null) roots.add(selectedNodeId);
        if (selectedEdge != null) {
            roots.add(selectedEdge.getStartID());
            roots.add(selectedEdge.getEndID());
        }
        neighborhood = topology.traverse(roots, neighborhoodDepth);
        refreshFocusedCrossLinks();
        applyExplorationState();
    }

    private boolean nodeAllowed(GraphNode node) {
        // Original-node visibility is independent of how edges are presented.
        // FactorLabel and explicit neighborhood isolation remain authoritative.
        return ContextGraphCommunityDisclosurePolicy.originalVisible(
            nodeLabelVisible(node), isolateSelection, neighborhood.isEmpty(),
            neighborhood.containsNode(node.getEntityID()));
    }

    /** Updates only existing node visibility, without reconstructing shapes or materials. */
    private void refreshSourceNodeVisibility() {
        if (currentGraph == null || nodeViews.isEmpty()) return;
        for (GraphNode node : currentGraph.getNodes()) {
            ContextGraphNode3D view = nodeViews.get(node.getEntityID());
            if (view != null) view.setVisible(nodeAllowed(node));
        }
    }

    private boolean edgeAllowed(GraphEdge edge) {
        return edgeLabelVisible(edge)
            && !hiddenRelationshipTypes.contains(relationshipType(edge))
            && edgeShownByStructure(edge)
            && (!isolateSelection || neighborhood.isEmpty() || neighborhood.containsEdge(edge));
    }

    private void applyNodeExploration(GraphNode model, ContextGraphNode3D view) {
        boolean active = neighborhoodHighlightEnabled && !neighborhood.isEmpty();
        if (!active) {
            view.setNeighborhoodState(false, false, null, null);
        } else {
            boolean highlighted = neighborhood.containsNode(model.getEntityID());
            view.setNeighborhoodState(highlighted, dimOutsideNeighborhood && !highlighted,
                displayMaterial(NEIGHBOR_NODE_COLOR, styleParams.nodeOpacity),
                dimOutsideNeighborhood ? displayMaterial(DIM_NODE_COLOR, styleParams.nodeOpacity) : null);
        }
        view.setVisible(nodeAllowed(model));
    }

    private void applyEdgeExploration(GraphEdge model, ContextGraphEdge3D view) {
        boolean active = neighborhoodHighlightEnabled && !neighborhood.isEmpty();
        if (!active) {
            view.setNeighborhoodState(false, false, null, null);
        } else {
            boolean highlighted = neighborhood.containsEdge(model);
            view.setNeighborhoodState(highlighted, dimOutsideNeighborhood && !highlighted,
                displayMaterial(NEIGHBOR_EDGE_COLOR, styleParams.edgeOpacity),
                dimOutsideNeighborhood ? displayMaterial(DIM_EDGE_COLOR, styleParams.edgeOpacity) : null);
        }
        view.setVisible(edgeAllowed(model));
    }

    private void applyExplorationState() {
        if (currentGraph == null) return;
        for (GraphNode node : currentGraph.getNodes()) {
            ContextGraphNode3D view = nodeViews.get(node.getEntityID());
            if (view != null) applyNodeExploration(node, view);
        }
        for (Map.Entry<GraphEdge, ContextGraphEdge3D> entry : edgeViews.entrySet()) {
            applyEdgeExploration(entry.getKey(), entry.getValue());
        }
        refreshAggregateVisibility();
    }

    public void toggleNodeSelection(GraphNode node) {
        clearCommunitySelection();
        clearAggregateSelection();
        String id = node.getEntityID();
        if (selectedNodeId != null) {
            ContextGraphNode3D old = nodeViews.get(selectedNodeId);
            if (old != null) old.unselect();
        }
        selectedNodeId = java.util.Objects.equals(selectedNodeId, id) ? null : id;
        if (selectedNodeId != null) {
            ContextGraphNode3D view = nodeViews.get(selectedNodeId);
            if (view != null) view.select();
        }
        refreshNeighborhood();
    }

    public void toggleEdgeSelection(GraphEdge edge) {
        clearCommunitySelection();
        clearAggregateSelection();
        if (selectedEdge != null) {
            ContextGraphEdge3D old = edgeViews.get(selectedEdge);
            if (old != null) old.unselect();
        }
        selectedEdge = selectedEdge == edge ? null : edge;
        if (selectedEdge != null) {
            ContextGraphEdge3D view = edgeViews.get(selectedEdge);
            if (view != null) view.select();
        }
        refreshNeighborhood();
    }

    public void clearSelections() {
        clearCommunitySelection();
        clearAggregateSelection();
        if (selectedNodeId != null) {
            ContextGraphNode3D view = nodeViews.get(selectedNodeId);
            if (view != null) view.unselect();
        }
        if (selectedEdge != null) {
            ContextGraphEdge3D view = edgeViews.get(selectedEdge);
            if (view != null) view.unselect();
        }
        selectedNodeId = null;
        selectedEdge = null;
        refreshNeighborhood();
    }

    private void registerNode(GraphNode model, ContextGraphNode3D view) {
        pickedNodes.put(view, model);
        view.setMouseTransparent(false);
        view.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED,
            e -> fireOnRoot(new GraphEvent(GraphEvent.GRAPH_NODE_HOVER, model)));
    }

    private void registerEdge(GraphEdge model, ContextGraphEdge3D view) {
        pickedEdges.put(view, model);
        view.setMouseTransparent(false);
        view.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_ENTERED,
            e -> fireOnRoot(new GraphEvent(GraphEvent.GRAPH_EDGE_HOVER, model)));
    }

    private GraphLayoutParams layoutParams = new GraphLayoutParams();
    private long layoutGeneration;
    private volatile long renderGeneration;
    private int edgeBatchSize = 100;

    public int getEdgeBatchSize() { return edgeBatchSize; }

    /** Changes the number of edges attached on the next animation pulse. FX thread only. */
    public void setEdgeBatchSize(int value) {
        if (value < 1 || value > 1000) throw new IllegalArgumentException("Edge batch size must be between 1 and 1000");
        edgeBatchSize = value;
    }
    private AnimationTimer edgeAttachmentTimer;

    public double getCoordinateScale() { return coordinateScale; }

    public void setCoordinateScale(double value) {
        if (!Double.isFinite(value) || value <= 0 || value == coordinateScale) return;
        coordinateScale = value;
        if (projectionMode.isHyperbolic()) scheduleProjectionReposition();
        else {
            updateNodePositions();
            updateEdgeEndpoints();
        }
    }

    public void finishCoordinateScale() {
        if (!projectionMode.isHyperbolic()) updateEdgeEndpoints();
    }

    private double[] sourcePosition(GraphNode node) {
        double[] position = layoutPositions.get(node.getEntityID());
        if (position != null) return position;
        if (node.getVector() == null || node.getVector().size() < 3) return new double[]{0,0,0};
        return new double[]{node.getVector().get(0), node.getVector().get(1), node.getVector().get(2)};
    }

    private double[] scaledPosition(GraphNode node) {
        if (projectionMode.isHyperbolic()) {
            double[] projected = projectedDisplayPositions.get(node.getEntityID());
            if (projected != null) return projected;
        }
        double[] p = sourcePosition(node);
        return new double[]{p[0]*coordinateScale, p[1]*coordinateScale, p[2]*coordinateScale};
    }

    public ContextGraphProjectionMode getProjectionMode() { return projectionMode; }

    public double getHyperbolicDisplayRadius() { return HYPERBOLIC_DISPLAY_RADIUS * coordinateScale; }

    /** Switch projection without modifying GraphNode.vector or the active Euclidean layout. */
    public void setProjectionMode(ContextGraphProjectionMode requested) {
        if (requested == null || requested == projectionMode) return;
        if (currentGraph == null) {
            fireOnRoot(new GraphEvent(GraphEvent.GRAPH_PROJECTION_FAILED, projectionMode));
            return;
        }
        final long generation = ++projectionGeneration;
        if (projectionUpdateTimer != null) {
            projectionUpdateTimer.stop();
            projectionUpdateTimer = null;
        }
        if (!requested.isHyperbolic()) {
            projectionMode = requested;
            applyDefaultEdgeModeForProjection(requested);
            scheduleProjectionReposition();
            return;
        }
        if (backboneReady && !hyperbolicCoordinates.isEmpty()) {
            hyperbolicFocus = new double[]{0.0, 0.0, 0.0};
            projectionMode = requested;
            applyDefaultEdgeModeForProjection(requested);
            scheduleProjectionReposition();
            return;
        }
        // The graph load/parameter worker already computes both hierarchy and coordinates.
        // Remember the requested mode and apply it when that single worker completes.
        projectionMode = requested;
        hyperbolicFocus = new double[]{0.0, 0.0, 0.0};
        applyDefaultEdgeModeForProjection(requested);
    }

    /** Hyperbolic isometry: recenter on the selected semantic entity. */
    public boolean focusHyperbolicSelection() {
        if (!projectionMode.isHyperbolic()) return false;
        double[] point = hyperbolicCoordinates.get(selectedNodeId);
        if (point == null && selectedEdge != null) {
            double[] a = hyperbolicCoordinates.get(selectedEdge.getStartID());
            double[] b = hyperbolicCoordinates.get(selectedEdge.getEndID());
            if (a != null && b != null) point = ContextGraphHyperbolicMath.midpoint(a, b);
        }
        if (point == null) return false;
        setHyperbolicFocus(point);
        return true;
    }

    public boolean focusHyperbolicNeighborhood() {
        if (!projectionMode.isHyperbolic()) return false;
        List<GraphNode> nodes = getNeighborhoodNodes();
        if (nodes.isEmpty()) return focusHyperbolicSelection();
        double[] center = new double[3];
        int count = 0;
        for (GraphNode node : nodes) {
            double[] p = hyperbolicCoordinates.get(node.getEntityID());
            if (p == null) continue;
            for (int k = 0; k < 3; k++) center[k] += p[k];
            count++;
        }
        if (count == 0) return false;
        for (int k = 0; k < 3; k++) center[k] /= count;
        setHyperbolicFocus(center);
        return true;
    }

    public void resetHyperbolicFocus() {
        if (!projectionMode.isHyperbolic()) return;
        setHyperbolicFocus(new double[]{0.0, 0.0, 0.0});
    }

    private void setHyperbolicFocus(double[] point) {
        hyperbolicFocus = ContextGraphHyperbolicMath.validate(point);
        scheduleProjectionReposition();
    }

    /** Source chart distance, invariant under hyperbolic focus transformations. */
    public double hyperbolicDistance(String sourceId, String targetId) {
        if (!projectionMode.isHyperbolic()) return Double.NaN;
        double[] a = hyperbolicCoordinates.get(sourceId);
        double[] b = hyperbolicCoordinates.get(targetId);
        return a == null || b == null ? Double.NaN : ContextGraphHyperbolicMath.distance(a, b);
    }

    /** Calculate transformed coordinates off FX, then move existing meshes in bounded pulses. */
    private void scheduleProjectionReposition() {
        if (currentGraph == null) return;
        final long generation = ++projectionGeneration;
        if (projectionUpdateTimer != null) {
            projectionUpdateTimer.stop();
            projectionUpdateTimer = null;
        }
        final GraphDirectedCollection graph = currentGraph;
        final ContextGraphProjectionMode mode = projectionMode;
        final Map<String, double[]> embedded = hyperbolicCoordinates;
        final double[] focus = hyperbolicFocus.clone();
        final Map<String, double[]> ordinary = new HashMap<>(layoutPositions);
        final double scale = coordinateScale;
        Task<Map<String, double[]>> task = new Task<>() {
            @Override protected Map<String, double[]> call() {
                Map<String, double[]> result = new HashMap<>();
                for (GraphNode node : graph.getNodes()) {
                    double[] p;
                    if (mode.isHyperbolic()) {
                        double[] original = embedded.get(node.getEntityID());
                        if (original == null) continue;
                        double[] centered = ContextGraphHyperbolicMath.recenter(original, focus);
                        double radius = HYPERBOLIC_DISPLAY_RADIUS * scale;
                        p = new double[]{centered[0] * radius, centered[1] * radius, centered[2] * radius};
                    } else {
                        double[] original = ordinary.get(node.getEntityID());
                        if (original == null) {
                            original = node.getVector() == null || node.getVector().size() < 3
                                ? new double[]{0.0, 0.0, 0.0}
                                : new double[]{node.getVector().get(0), node.getVector().get(1), node.getVector().get(2)};
                        }
                        p = new double[]{original[0] * scale, original[1] * scale, original[2] * scale};
                    }
                    result.put(node.getEntityID(), p);
                }
                return result;
            }
        };
        task.setOnSucceeded(e -> {
            if (generation != projectionGeneration || graph != currentGraph) return;
            projectedDisplayPositions = task.getValue();
            List<GraphNode> nodes = new ArrayList<>(graph.getNodes());
            List<GraphEdge> edges = new ArrayList<>(graph.getEdges());
            projectionUpdateTimer = new AnimationTimer() {
                private int nodeIndex;
                private int edgeIndex;
                @Override public void handle(long now) {
                    if (generation != projectionGeneration || graph != currentGraph) {
                        stop();
                        if (projectionUpdateTimer == this) projectionUpdateTimer = null;
                        return;
                    }
                    int nodeLimit = Math.min(nodes.size(), nodeIndex + 800);
                    for (; nodeIndex < nodeLimit; nodeIndex++) {
                        GraphNode model = nodes.get(nodeIndex);
                        ContextGraphNode3D view = nodeViews.get(model.getEntityID());
                        double[] p = projectedDisplayPositions.get(model.getEntityID());
                        if (view == null || p == null) continue;
                        view.setTranslateX(p[0]); view.setTranslateY(p[1]); view.setTranslateZ(p[2]);
                    }
                    int edgeLimit = Math.min(edges.size(), edgeIndex + 1000);
                    for (; edgeIndex < edgeLimit; edgeIndex++) {
                        GraphEdge model = edges.get(edgeIndex);
                        ContextGraphEdge3D view = edgeViews.get(model);
                        if (view == null) continue;
                        double[] a = projectedDisplayPositions.get(model.getStartID());
                        double[] b = projectedDisplayPositions.get(model.getEndID());
                        if (a == null || b == null) continue;
                        view.setEndpoints(new Point3D(a[0], a[1], a[2]),
                            new Point3D(b[0], b[1], b[2]));
                        view.setVisible(edgeAllowed(model));
                    }
                    if (nodeIndex == nodes.size() && edgeIndex == edges.size()) {
                        stop();
                        if (projectionUpdateTimer == this) projectionUpdateTimer = null;
                        updateAggregateEndpoints();
                        fireOnRoot(new GraphEvent(GraphEvent.GRAPH_PROJECTION_APPLIED, projectionMode));
                    }
                }
            };
            projectionUpdateTimer.start();
        });
        task.setOnFailed(e -> {
            if (generation == projectionGeneration) report("Projection update failed: " + task.getException());
        });
        Thread worker = new Thread(task, "context-graph-projection-update");
        worker.setDaemon(true);
        worker.start();
    }

    private void updateNodePositions() {
        if (currentGraph == null) return;
        for (GraphNode node : currentGraph.getNodes()) {
            ContextGraphNode3D sphere = nodeViews.get(node.getEntityID());
            if (sphere == null) continue;
            double[] p = scaledPosition(node);
            sphere.setTranslateX(p[0]);
            sphere.setTranslateY(p[1]);
            sphere.setTranslateZ(p[2]);
        }
    }

    private void updateEdgeEndpoints() {
        if (currentGraph == null) return;
        for (Map.Entry<GraphEdge, ContextGraphEdge3D> entry : edgeViews.entrySet()) {
            GraphEdge edge = entry.getKey();
            GraphNode a = topology.node(edge.getStartID());
            GraphNode b = topology.node(edge.getEndID());
            if (a == null || b == null) continue;
            double[] ap=scaledPosition(a), bp=scaledPosition(b);
            entry.getValue().setEndpoints(new Point3D(ap[0],ap[1],ap[2]),
                new Point3D(bp[0],bp[1],bp[2]));
            entry.getValue().setVisible(edgeAllowed(edge));
        }
        updateAggregateEndpoints();
    }

    private void refreshEdges() {
        long startTime = System.nanoTime();
        if (currentGraph == null || edgeLayer == null) return;
        edgeLayer.getChildren().clear();
        edgeViews.clear();
        pickedEdges.clear();
        for (GraphEdge edge : currentGraph.getEdges()) {
            GraphNode a = topology.node(edge.getStartID());
            GraphNode b = topology.node(edge.getEndID());
            if (a == null || b == null) continue;
            double[] ap = scaledPosition(a);
            double[] bp = scaledPosition(b);
            ContextGraphEdge3D edgeView = new ContextGraphEdge3D(
                (float) Math.max(0.001, styleParams.edgeWidth), 6, 1);
            edgeView.setEndpoints(new Point3D(ap[0], ap[1], ap[2]),
                new Point3D(bp[0], bp[1], bp[2]));
            edgeViews.put(edge, edgeView);
            registerEdge(edge, edgeView);
            applyEdgeExploration(edge, edgeView);
            if (selectedEdge == edge) edgeView.select();
            edgeLayer.getChildren().add(edgeView);
        }
        System.out.printf("[CG-TIMING] edge construction + attachment %.3f ms edges=%d%n", (System.nanoTime()-startTime)/1e6, edgeLayer.getChildren().size());
        applyEdgeMaterials(styleParams);
        applyExplorationState();
        System.out.print("ContextGraph refreshEdges: ");
        Utils.printTotalTime(startTime);
    }

    private void applyLayout(GraphLayoutParams params) {
        if (currentGraph == null || currentGraph.getNodes().isEmpty()) return;
        layoutParams = params;
        final GraphDirectedCollection graph = currentGraph;
        final List<GraphNode> nodes = new ArrayList<>(graph.getNodes());
        final long generation = ++layoutGeneration;
        final int n = nodes.size();
        final double[][] weights = new double[n][n];
        final Map<String,Integer> ids = new HashMap<>();
        for (int i=0;i<n;i++) ids.put(nodes.get(i).getEntityID(),i);
        final List<int[]> topologyEdges = new ArrayList<>();
        for (GraphEdge edge:graph.getEdges()) {
            Integer a=ids.get(edge.getStartID()), b=ids.get(edge.getEndID());
            if(a!=null && b!=null) { weights[a][b]=1; weights[b][a]=1; topologyEdges.add(new int[]{a,b}); }
        }
        Thread worker = new Thread(() -> {
            double[][] positions = new double[n][3];
            if (params.kind == GraphLayoutParams.LayoutKind.FORCE_FR) {
                positions = new ForceFrLayout3D().layout(n, null, null, weights, params);
            } else if (params.kind == GraphLayoutParams.LayoutKind.FORCE_ATLAS_3D
                || params.kind == GraphLayoutParams.LayoutKind.KAMADA_KAWAI_3D
                || params.kind == GraphLayoutParams.LayoutKind.STRESS_MAJOR_3D
                || params.kind == GraphLayoutParams.LayoutKind.SPECTRAL_3D) {
                positions = GraphNetworkLayouts3D.layout(n, topologyEdges, params);
            } else if (params.kind == GraphLayoutParams.LayoutKind.RADIAL_TREE_3D
                || params.kind == GraphLayoutParams.LayoutKind.LAYERED_HIERARCHY_3D
                || params.kind == GraphLayoutParams.LayoutKind.HYPERBOLIC_HIERARCHY_3D) {
                positions = GraphTopologyLayouts3D.layout(n, topologyEdges, params);
            } else {
                for(int i=0;i<n;i++) {
                    double angle=2*Math.PI*i/n;
                    if(params.kind == GraphLayoutParams.LayoutKind.CIRCLE_XY) {
                        positions[i]=new double[]{params.radius*Math.cos(angle),params.radius*Math.sin(angle),0};
                    } else if(params.kind == GraphLayoutParams.LayoutKind.CIRCLE_XZ) {
                        positions[i]=new double[]{params.radius*Math.cos(angle),0,params.radius*Math.sin(angle)};
                    } else {
                        double y=1-2*(i+0.5)/n;
                        double r=Math.sqrt(1-y*y);
                        double theta=i*Math.PI*(3-Math.sqrt(5));
                        positions[i]=new double[]{params.radius*r*Math.cos(theta),params.radius*y,params.radius*r*Math.sin(theta)};
                    }
                }
            }
            final double[][] result=positions;
            Platform.runLater(() -> {
                if(generation!=layoutGeneration || graph!=currentGraph) return;
                layoutPositions.clear();
                for(int i=0;i<n;i++) layoutPositions.put(nodes.get(i).getEntityID(),result[i]);
                if (!projectionMode.isHyperbolic()) {
                    updateNodePositions();
                    updateEdgeEndpoints();
                }
                report("Applied graph layout: " + params.kind);
            });
        }, "context-graph-layout");
        worker.setDaemon(true);
        worker.start();
    }

    private void report(String message) {
        if(scene.getRoot()!=null) scene.getRoot().fireEvent(new CommandTerminalEvent(
            message, new Font("Consolas", 16), Color.ALICEBLUE));
    }

    public ContextGraphController(Scene scene) {
        this.scene = scene;
        graphLayer.setVisible(graphVisible);
        graphLayer.getChildren().addAll(edgeLayer, aggregateEdgeLayer,
            communityExpansionEdgeLayer, nodeLayer, communityRepresentativeLayer);
        updateAggregateLayerVisibility();
    }


    /**
     * Synchronizes graph-owned state back to the existing graph controls.
     */
    public void syncVisibilityGui() {
        fireOnRoot(new GraphEvent(GraphEvent.SET_GRAPH_VISIBILITY_GUI, graphVisible));
    }

    private boolean nodesDisplayed = true;
    private boolean edgesDisplayed = true;

    public boolean isNodesDisplayed() { return nodesDisplayed; }
    public boolean isEdgesDisplayed() { return edgesDisplayed; }

    /** Detach or reattach the existing layer without recreating geometry. */
    public void setNodesDisplayed(boolean displayed) {
        if (nodesDisplayed == displayed) return;
        nodesDisplayed = displayed;
        long start = System.nanoTime();
        if (displayed) {
            if (!graphLayer.getChildren().contains(nodeLayer)) graphLayer.getChildren().add(nodeLayer);
        } else {
            graphLayer.getChildren().remove(nodeLayer);
        }
        System.out.printf("[CG-TIMING] nodes %s layer switch %.3f ms objects=%d%n",
            displayed ? "attached" : "detached", (System.nanoTime()-start)/1e6, nodeLayer.getChildren().size());
    }

    public void setEdgesDisplayed(boolean displayed) {
        if (edgesDisplayed == displayed) return;
        edgesDisplayed = displayed;
        long start = System.nanoTime();
        if (displayed) {
            if (!graphLayer.getChildren().contains(edgeLayer)) graphLayer.getChildren().add(0, edgeLayer);
        } else {
            graphLayer.getChildren().remove(edgeLayer);
        }
        updateAggregateLayerVisibility();
        System.out.printf("[CG-TIMING] edges %s layer switch %.3f ms objects=%d%n",
            displayed ? "attached" : "detached", (System.nanoTime()-start)/1e6, edgeLayer.getChildren().size());
    }

    public Group getGraphLayer() {
        return graphLayer;
    }

    public boolean isGraphVisible() {
        return graphVisible;
    }

    public GraphDirectedCollection getCurrentGraph() {
        return currentGraph;
    }

    public GraphStyleParams getStyleParams() {
        return styleParams;
    }

    /**
     * Installs GraphEvent handlers once. Kept separate from construction so the host
     * pane controls when the graph subsystem becomes event-active.
     */
    public void installEventHandlers() {
        if (scene == null || eventHandlersInstalled) return;
        eventHandlersInstalled = true;

        scene.addEventHandler(GraphEvent.GRAPH_REBUILD_PARAMS, e -> {
            if (e.object instanceof GraphLayoutParams p) applyLayout(p);
        });
        scene.addEventHandler(GraphEvent.GRAPH_RESET_PARAMS, e -> {
            layoutParams = new GraphLayoutParams();
            layoutPositions.clear();
            if (!projectionMode.isHyperbolic()) {
                updateNodePositions();
                updateEdgeEndpoints();
            }
        });
        scene.addEventHandler(GraphEvent.GRAPH_STYLE_PARAMS_CHANGED, e -> {
            if (!(e.object instanceof GraphStyleParams p)) return;

            styleParams.colorByLabel = p.colorByLabel;
            styleParams.nodeColor = p.nodeColor;
            styleParams.nodeRadius = p.nodeRadius;
            styleParams.nodeOpacity = clamp01(p.nodeOpacity);
            styleParams.edgeColor = p.edgeColor;
            styleParams.edgeWidth = p.edgeWidth;
            styleParams.edgeOpacity = clamp01(p.edgeOpacity);

            applyGraphStyle(styleParams, true);
        });

        scene.addEventHandler(GraphEvent.GRAPH_STYLE_RESET_DEFAULTS, e -> {
            communityColorDiagnostic = false;
            styleParams = defaultContextGraphStyle();
            fireOnRoot(new GraphEvent(GraphEvent.GRAPH_COMMUNITY_HIERARCHY_CHANGED));

            graphParams.withNodeRadius(styleParams.nodeRadius)
                .withEdgeWidth((float) styleParams.edgeWidth);

            if (currentGraph != null) {
                rebuildGraph();
            }
            applyGraphStyle(styleParams, false);
            fireOnRoot(new GraphEvent(GraphEvent.SET_STYLE_GUI, styleParams));
        });


        scene.addEventHandler(HyperspaceEvent.UPDATED_FACTOR_LABEL, e -> refreshLabelMaterials());
        scene.addEventHandler(HyperspaceEvent.ADDED_FACTOR_LABEL, e -> refreshLabelMaterials());
        scene.addEventHandler(HyperspaceEvent.ADDEDALL_FACTOR_LABELS, e -> refreshLabelMaterials());
        scene.addEventHandler(HyperspaceEvent.UPDATEDALL_FACTOR_LABELS, e -> refreshLabelMaterials());
        scene.addEventHandler(HyperspaceEvent.REMOVED_FACTOR_LABEL, e -> refreshLabelMaterials());
        scene.addEventHandler(HyperspaceEvent.CLEARED_FACTOR_LABELS, e -> refreshLabelMaterials());

        scene.addEventHandler(GraphEvent.GRAPH_VISIBILITY_CHANGED, e -> {
            if (!(e.object instanceof Boolean visible)) return;
            setGraphVisible(visible);
        });
    }

    private GraphFeatureCollectionAdapter featureAdapter;

    public GraphFeatureCollectionAdapter getFeatureAdapter() { return featureAdapter; }

    /** Loads a graph without routing through a generic callback. */
    public void loadGraph(GraphDirectedCollection graph) {
        if (graph == null) return;
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> loadGraph(graph));
            return;
        }
        if (currentGraph != graph) {
            selectedNodeId = null;
            selectedEdge = null;
        }
        ContextGraphProjectionMode restoreMode = projectionMode;
        if (communityTask != null) {
            communityTask.cancel(true);
            communityTask = null;
        }
        ++communityMaterialGeneration;
        if (communityMaterialTimer != null) {
            communityMaterialTimer.stop();
            communityMaterialTimer = null;
        }
        if (communityTask != null) {
            communityTask.cancel(true);
            communityTask = null;
        }
        invalidateAggregation();
        ++backboneGeneration;
        hierarchyCache.clear();
        layoutCache.clear();
        spatialDiagnostics = null;
        spatialComparison = "";
        partitionComparison = "";
        communityHierarchy = null;
        diagnosticMembership = Map.of();
        communityDiagnosticLevel = -1;
        communityStatus = "Computing semantic hierarchy and spatial layout...";
        ++projectionGeneration;
        if (projectionUpdateTimer != null) {
            projectionUpdateTimer.stop();
            projectionUpdateTimer = null;
        }
        projectionMode = ContextGraphProjectionMode.EUCLIDEAN;
        hyperbolicCoordinates = Map.of();
        backboneEdges = Set.of();
        backboneReady = false;
        backboneFailed = false;
        focusedCrossLinks = Set.of();
        projectedDisplayPositions = Map.of();
        hyperbolicFocus = new double[]{0.0, 0.0, 0.0};
        currentGraph = graph;
        topology = new ContextGraphNeighborhoodIndex(graph);
        entitySearchIndex = new ContextGraphEntitySearchIndex(graph);
        buildRelationshipTypeCounts(graph);
        registerGraphLabels(graph);
        refreshNeighborhood();
        ++layoutGeneration;
        layoutPositions.clear();
        rebuildGraph();
        prepareBackbone(graph);
        if (restoreMode.isHyperbolic()) setProjectionMode(restoreMode);
        else applyDefaultEdgeModeForProjection(restoreMode);
        fireOnRoot(new GraphEvent(GraphEvent.SET_STYLE_GUI, styleParams));
        fireOnRoot(new GraphEvent(GraphEvent.SET_GRAPH_VISIBILITY_GUI, graphVisible));
    }

    /** Clear only this Context Graph view. Invalidate all asynchronous graph work. */
    public void clearData() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::clearData);
            return;
        }
        ++renderGeneration;
        ++projectionGeneration;
        ++backboneGeneration;
        ++communityMaterialGeneration;
        if (communityMaterialTimer != null) {
            communityMaterialTimer.stop();
            communityMaterialTimer = null;
        }
        if (communityTask != null) {
            communityTask.cancel(true);
            communityTask = null;
        }
        invalidateAggregation();
        hierarchyCache.clear();
        layoutCache.clear();
        spatialDiagnostics = null;
        spatialComparison = "";
        partitionComparison = "";
        communityHierarchy = null;
        communityColorDiagnostic = false;
        diagnosticMembership = Map.of();
        communityDiagnosticLevel = -1;
        packingParams = ContextGraphHyperbolicPackingParams.defaults();
        communityParams = ContextGraphCommunityParams.defaults();
        communityStatus = "No graph loaded";
        ++layoutGeneration;
        if (edgeAttachmentTimer != null) {
            edgeAttachmentTimer.stop();
            edgeAttachmentTimer = null;
        }
        if (projectionUpdateTimer != null) {
            projectionUpdateTimer.stop();
            projectionUpdateTimer = null;
        }
        nodeLayer.getChildren().clear();
        edgeLayer.getChildren().clear();
        nodeViews.clear();
        edgeViews.clear();
        pickedNodes.clear();
        pickedEdges.clear();
        currentGraph = null;
        featureAdapter = null;
        topology = new ContextGraphNeighborhoodIndex(null);
        entitySearchIndex = new ContextGraphEntitySearchIndex(null);
        neighborhood = ContextGraphNeighborhoodIndex.Neighborhood.empty();
        selectedNodeId = null;
        selectedEdge = null;
        layoutPositions.clear();
        projectedDisplayPositions = Map.of();
        hyperbolicCoordinates = Map.of();
        hyperbolicFocus = new double[]{0.0, 0.0, 0.0};
        backboneEdges = Set.of();
        backboneReady = false;
        backboneFailed = false;
        focusedCrossLinks = Set.of();
        relationshipTypeCounts = Map.of();
        hiddenRelationshipTypes.clear();
        projectionMode = ContextGraphProjectionMode.EUCLIDEAN;
        edgeDisplayMode = ContextGraphEdgeDisplayMode.ALL;
        edgeDisplayModeCustomized = false;
        updateAggregateLayerVisibility();
        fireOnRoot(new GraphEvent(GraphEvent.GRAPH_DATA_CLEARED));
        fireOnRoot(new GraphEvent(GraphEvent.GRAPH_COMMUNITY_HIERARCHY_CHANGED));
        fireOnRoot(new GraphEvent(GraphEvent.GRAPH_PROJECTION_APPLIED, projectionMode));
        notifyExplorationState();
        report("Context Graph data cleared");
    }

    private void setGraphVisible(boolean visible) {
        graphVisible = visible;
        graphLayer.setVisible(visible);
    }

    /** Prepare detached geometry on a worker Task; attach edges across FX pulses. */
    private void rebuildGraph() {
        // Only the layer reset below requires FX-thread ownership; the Task
        // itself and all geometry preparation are independent of FX scheduling.
        final long generation = ++renderGeneration;
        if (edgeAttachmentTimer != null) {
            edgeAttachmentTimer.stop();
            edgeAttachmentTimer = null;
        }
        nodeLayer.getChildren().clear();
        edgeLayer.getChildren().clear();
        nodeViews.clear();
        edgeViews.clear();
        pickedNodes.clear();
        pickedEdges.clear();
        final GraphDirectedCollection graph = currentGraph;
        if (graph == null) {
            selectedNodeId = null;
            selectedEdge = null;
            return;
        }
        if (selectedNodeId != null && graph.findNodeById(selectedNodeId).isEmpty()) selectedNodeId = null;
        if (selectedEdge != null && graph.getEdges().stream().noneMatch(edge -> edge == selectedEdge)) selectedEdge = null;
        final double scale = coordinateScale;
        final double nodeRadius = styleParams.nodeRadius;
        final double edgeRadius = Math.max(0.001, styleParams.edgeWidth);
        final int nodeDivisions = graphParams.nodeDivisions;
        final PhongMaterial nodeMaterial = displayMaterial(
            styleParams.nodeColor == null ? Color.CYAN : styleParams.nodeColor,
            styleParams.nodeOpacity);
        final PhongMaterial edgeMaterial = displayMaterial(
            styleParams.edgeColor == null ? Color.CORNFLOWERBLUE : styleParams.edgeColor,
            styleParams.edgeOpacity);
        final long started = System.nanoTime();
        final Map<String, double[]> layoutSnapshot = new HashMap<>(layoutPositions);
        final Map<String, double[]> displaySnapshot = projectionMode.isHyperbolic()
            ? new HashMap<>(projectedDisplayPositions) : Map.of();
        // Snapshot ordinary graph data on the worker, not the FX thread.
        // This Task is started directly; it is not rescheduled onto FX.
        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                long prepareStart = System.nanoTime();
                final List<GraphNode> graphNodes = new ArrayList<>(graph.getNodes());
                final List<GraphEdge> graphEdges = new ArrayList<>(graph.getEdges());
                final Map<String, double[]> coordinates = new HashMap<>();
                for (GraphNode node : graphNodes) {
                    if (isCancelled() || generation != renderGeneration) return null;
                    double[] pos = layoutSnapshot.get(node.getEntityID());
                    double[] projected = displaySnapshot.get(node.getEntityID());
                    if (pos == null) {
                        if (node.getVector() == null || node.getVector().size() < 3) {
                            pos = new double[]{0, 0, 0};
                        } else {
                            pos = new double[]{node.getVector().get(0),
                                node.getVector().get(1), node.getVector().get(2)};
                        }
                    }
                    coordinates.put(node.getEntityID(), projected != null ? projected.clone() : new double[]{
                        pos[0] * scale, pos[1] * scale, pos[2] * scale});
                }
                for (GraphNode node : graphNodes) {
                    if (isCancelled() || generation != renderGeneration) return null;
                    double[] pos = coordinates.get(node.getEntityID());
                    ContextGraphNode3D sphere = new ContextGraphNode3D(nodeMaterial, nodeRadius,
                        nodeDivisions, true);
                    sphere.setExpansionScale(1.10);

                    sphere.setTranslateX(pos[0]);
                    sphere.setTranslateY(pos[1]);
                    sphere.setTranslateZ(pos[2]);
                    Platform.runLater(() -> {
                        if (generation != renderGeneration) return;
                        nodeViews.put(node.getEntityID(), sphere);
                        double[] latest = scaledPosition(node);
                        sphere.setTranslateX(latest[0]);
                        sphere.setTranslateY(latest[1]);
                        sphere.setTranslateZ(latest[2]);
                        registerNode(node, sphere);
                        sphere.setPhongMaterial(displayMaterial(nodeColor(node, styleParams), styleParams.nodeOpacity));
                        applyNodeExploration(node, sphere);
                        if (java.util.Objects.equals(selectedNodeId, node.getEntityID())) sphere.select();
                        nodeLayer.getChildren().add(sphere);
                    });
                }
                long edgeStart = System.nanoTime();
                final List<ContextGraphEdge3D> preparedEdges = new ArrayList<>();
                final List<GraphEdge> preparedEdgeKeys = new ArrayList<>();
                for (GraphEdge edge : graphEdges) {
                    if (isCancelled() || generation != renderGeneration) return null;
                    double[] a = coordinates.get(edge.getStartID());
                    double[] b = coordinates.get(edge.getEndID());
                    if (a == null || b == null) continue;
                    ContextGraphEdge3D view = new ContextGraphEdge3D((float) edgeRadius, 3, 1);
                    view.setEndpoints(new Point3D(a[0], a[1], a[2]),
                        new Point3D(b[0], b[1], b[2]));
                    view.setBaseMaterial(edgeMaterial);
                    preparedEdges.add(view);
                    preparedEdgeKeys.add(edge);
                }
                System.out.printf("[CG-TIMING] background geometry preparation %.3f ms; edge phase %.3f ms%n",
                    (System.nanoTime() - prepareStart) / 1e6,
                    (System.nanoTime() - edgeStart) / 1e6);
                Platform.runLater(() -> {
                    if (generation != renderGeneration) return;
                    final long progressiveStart = System.nanoTime();
                    edgeAttachmentTimer = new AnimationTimer() {
                        private int nextIndex;
                        private int batchCount;
                        private long previousPulse = -1L;
                        private double maxPulseGapMs;
                        private double maxAttachMs;

                        @Override public void handle(long now) {
                            if (generation != renderGeneration) {
                                stop();
                                if (edgeAttachmentTimer == this) edgeAttachmentTimer = null;
                                return;
                            }
                            if (previousPulse >= 0) {
                                double gapMs = (now - previousPulse) / 1e6;
                                maxPulseGapMs = Math.max(maxPulseGapMs, gapMs);
                                if (gapMs >= 250.0) {
                                    System.out.printf("[CG-TIMING] progressive FX pulse gap %.3f ms edges=%d/%d%n",
                                        gapMs, nextIndex, preparedEdges.size());
                                }
                            }
                            previousPulse = now;
                            int end = Math.min(nextIndex + edgeBatchSize, preparedEdges.size());
                            long attachStart = System.nanoTime();
                            if (end > nextIndex) {
                                // Honor style changes made while progressive loading is active.
                                float activeWidth = (float) Math.max(0.001, styleParams.edgeWidth);
                                for (int i = nextIndex; i < end; i++) {
                                    ContextGraphEdge3D pending = preparedEdges.get(i);
                                    pending.setBaseMaterial(displayMaterial(
                                        edgeColor(preparedEdgeKeys.get(i), styleParams), styleParams.edgeOpacity));
                                    if (pending.getRadius() != activeWidth) pending.setRadius(activeWidth);
                                }
                                edgeLayer.getChildren().addAll(
                                    preparedEdges.subList(nextIndex, end));
                                for (int i = nextIndex; i < end; i++) {
                                    GraphEdge model = preparedEdgeKeys.get(i);
                                    ContextGraphEdge3D view = preparedEdges.get(i);
                                    GraphNode start = topology.node(model.getStartID());
                                    GraphNode graphEnd = topology.node(model.getEndID());
                                    if (start != null && graphEnd != null) {
                                        double[] ap = scaledPosition(start);
                                        double[] bp = scaledPosition(graphEnd);
                                        view.setEndpoints(new Point3D(ap[0], ap[1], ap[2]),
                                            new Point3D(bp[0], bp[1], bp[2]));
                                    }
                                    edgeViews.put(model, view);
                                    registerEdge(model, view);
                                    applyEdgeExploration(model, view);
                                    if (selectedEdge == model) view.select();
                                }
                            }
                            double attachMs = (System.nanoTime() - attachStart) / 1e6;
                            maxAttachMs = Math.max(maxAttachMs, attachMs);
                            nextIndex = end;
                            batchCount++;
                            if (nextIndex == preparedEdges.size()) {
                                stop();
                                if (edgeAttachmentTimer == this) edgeAttachmentTimer = null;
                                System.out.printf("[CG-TIMING] progressive edges complete %.3f ms batches=%d batchSize=%d maxAttach=%.3f ms maxPulseGap=%.3f ms nodes=%d edges=%d%n",
                                    (System.nanoTime() - progressiveStart) / 1e6,
                                    batchCount, edgeBatchSize, maxAttachMs, maxPulseGapMs,
                                    nodeLayer.getChildren().size(), edgeLayer.getChildren().size());
                                System.out.printf("[CG-TIMING] all FX attachments completed %.3f ms nodes=%d edges=%d%n",
                                    (System.nanoTime() - started) / 1e6,
                                    nodeLayer.getChildren().size(), edgeLayer.getChildren().size());
                                report("Rendered 3D graph: nodes=" + nodeViews.size()
                                    + ", edges=" + edgeViews.size());
                            }
                        }
                    };
                    edgeAttachmentTimer.start();
                });
                return null;
            }
        };
        Thread worker = new Thread(task, "context-graph-geometry");
        worker.setDaemon(true);
        worker.start();
    }

    /** Styles only the indexed graph objects. No scene-graph recursion. */
    private void applyGraphStyle(GraphStyleParams style, boolean rebuildIfNeeded) {
        if (style == null) return;
        long startTime = System.nanoTime();
        boolean edgeWidthChanged = Math.abs(style.edgeWidth - graphParams.edgeWidth) > 1.0e-6;
        graphParams.withNodeRadius(style.nodeRadius);
        if (edgeWidthChanged && rebuildIfNeeded && currentGraph != null) {
            graphParams.withEdgeWidth((float) style.edgeWidth);
            for (ContextGraphEdge3D edge : edgeViews.values()) {
                edge.setRadius((float) Math.max(0.001, style.edgeWidth));
            }
        }
        applyNodeMaterials(style);
        applyEdgeMaterials(style);
        applyAggregateMaterials();
        applyExplorationState();
        System.out.print("ContextGraph applyGraphStyle: ");
        Utils.printTotalTime(startTime);
    }

    private void refreshLabelMaterials() {
        if (styleParams.colorByLabel) {
            applyNodeMaterials(styleParams);
            applyEdgeMaterials(styleParams);
        }
        applyExplorationState();
        notifyExplorationState();
    }

    private void applyNodeMaterials(GraphStyleParams style) {
        if (currentGraph == null) return;
        for (GraphNode node : currentGraph.getNodes()) {
            ContextGraphNode3D sphere = nodeViews.get(node.getEntityID());
            if (sphere == null) continue;
            if (sphere.getRadius() != style.nodeRadius) sphere.setSphereRadius(style.nodeRadius);
            sphere.setPhongMaterial(displayMaterial(nodeColor(node, style), style.nodeOpacity));
        }
    }

    private void applyEdgeMaterials(GraphStyleParams style) {
        for (Map.Entry<GraphEdge, ContextGraphEdge3D> entry : edgeViews.entrySet()) {
            entry.getValue().setBaseMaterial(displayMaterial(
                edgeColor(entry.getKey(), style), style.edgeOpacity));
        }
    }

    /** Current scaled coordinates, including any active graph layout. */
    public double[] getDisplayedPosition(GraphNode node) {
        return scaledPosition(node);
    }

    private PhongMaterial emissiveMaterial(Color color, double opacity) {
        Color rgba = new Color(color.getRed(), color.getGreen(), color.getBlue(), clamp01(opacity));
        return emissiveMaterials.computeIfAbsent(rgba, key -> {
            WritableImage image = new WritableImage(1, 1);
            image.getPixelWriter().setColor(0, 0, key);
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseColor(null);
            material.setDiffuseMap(null);
            material.setSelfIlluminationMap(image);
            return material;
        });
    }

    private void fireOnRoot(Event event) {
        if (scene != null && scene.getRoot() != null) {
            scene.getRoot().fireEvent(event);
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
