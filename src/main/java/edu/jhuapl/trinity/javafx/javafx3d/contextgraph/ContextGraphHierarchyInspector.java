package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.data.graph.GraphRelationship;
import edu.jhuapl.trinity.data.graph.semantics.GraphEvidence;
import edu.jhuapl.trinity.data.graph.semantics.GraphHierarchy;
import edu.jhuapl.trinity.data.graph.semantics.GraphHierarchyIndex;
import edu.jhuapl.trinity.data.graph.semantics.GraphHierarchyKey;
import edu.jhuapl.trinity.data.graph.semantics.GraphRelationshipSemantics;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An opt-in inspection dialog for typed, directional hierarchies and original
 * evidence references. It never transforms graph topology or layout positions.
 * Indexes are prepared on a worker and reused until a different graph loads.
 */
final class ContextGraphHierarchyInspector {
    private final ContextGraph3DPane view;
    private GraphDirectedCollection indexedGraph;
    private IndexedData indexedData;
    private Task<IndexedData> pending;
    private Dialog<Void> openDialog;

    ContextGraphHierarchyInspector(ContextGraph3DPane view) {
        this.view = view;
    }

    void invalidate() {
        if (pending != null) pending.cancel();
        pending = null;
        indexedGraph = null;
        indexedData = null;
        if (openDialog != null) openDialog.close();
        openDialog = null;
    }

    void show() {
        GraphDirectedCollection graph = view.getGraphController().getCurrentGraph();
        String nodeId = view.getGraphController().getSelectedNodeId();
        GraphEdge edge = view.getGraphController().getSelectedEdge();
        if (graph == null || (nodeId == null && edge == null)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Select a graph node or edge before opening hierarchy inspection.", ButtonType.OK);
            alert.setHeaderText("No graph selection");
            alert.show();
            return;
        }
        if (openDialog != null) openDialog.close();
        if (pending != null) pending.cancel();
        DialogContents dialog = new DialogContents(graph, nodeId, edge);
        openDialog = dialog.window;
        dialog.window.setOnHidden(e -> {
            if (openDialog == dialog.window) openDialog = null;
            if (pending != null) pending.cancel();
        });
        dialog.window.show();
        if (indexedGraph == graph && indexedData != null) {
            dialog.loaded(indexedData);
        } else {
            Task<IndexedData> worker = new Task<>() {
                @Override protected IndexedData call() {
                    return IndexedData.build(graph, this::isCancelled);
                }
            };
            pending = worker;
            worker.setOnSucceeded(e -> {
                if (pending != worker || view.getGraphController().getCurrentGraph() != graph) return;
                pending = null;
                indexedGraph = graph;
                indexedData = worker.getValue();
                if (dialog.window.isShowing()) dialog.loaded(indexedData);
            });
            worker.setOnFailed(e -> {
                if (pending != worker) return;
                pending = null;
                if (dialog.window.isShowing()) {
                    dialog.details.setText("Unable to index typed relationships:\n"
                        + worker.getException());
                }
            });
            Thread thread = new Thread(worker, "context-graph-hierarchy-inspection");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private record RelatedNode(String relation, String id, String title) {
        @Override public String toString() { return relation + "  " + title + "  [" + id + "]"; }
    }

    private static final class IndexedData {
        final GraphHierarchyIndex hierarchyIndex;
        final Map<String, GraphNode> nodes;
        final Map<String, GraphEvidence> evidenceById;
        final Map<String, List<GraphEvidence>> evidenceByPassage;
        final Map<String, List<GraphEdge>> incidentEdges;

        private IndexedData(GraphHierarchyIndex hierarchyIndex,
                            Map<String, GraphNode> nodes,
                            Map<String, GraphEvidence> evidenceById,
                            Map<String, List<GraphEvidence>> evidenceByPassage,
                            Map<String, List<GraphEdge>> incidentEdges) {
            this.hierarchyIndex = hierarchyIndex;
            this.nodes = nodes;
            this.evidenceById = evidenceById;
            this.evidenceByPassage = evidenceByPassage;
            this.incidentEdges = incidentEdges;
        }

        static IndexedData build(GraphDirectedCollection graph,
                                 java.util.function.BooleanSupplier cancelled) {
            GraphHierarchyIndex hierarchy = GraphHierarchyIndex.build(graph, true, cancelled);
            Map<String, GraphNode> nodes = new LinkedHashMap<>();
            if (graph.getNodes() != null) for (GraphNode node : graph.getNodes()) {
                if (cancelled.getAsBoolean()) throw new java.util.concurrent.CancellationException();
                if (node != null) nodes.put(node.getEntityID(), node);
            }
            Map<String, GraphEvidence> byId = new HashMap<>();
            Map<String, List<GraphEvidence>> byPassage = new HashMap<>();
            if (graph.getEvidenceRecords() != null) {
                for (GraphEvidence evidence : graph.getEvidenceRecords()) {
                    if (cancelled.getAsBoolean()) throw new java.util.concurrent.CancellationException();
                    if (evidence == null) continue;
                    byId.put(evidence.getEvidenceId(), evidence);
                    if (evidence.getPassageId() != null) {
                        byPassage.computeIfAbsent(evidence.getPassageId(), ignored -> new ArrayList<>())
                            .add(evidence);
                    }
                }
            }
            Map<String, List<GraphEdge>> incident = new HashMap<>();
            if (graph.getEdges() != null) for (GraphEdge edge : graph.getEdges()) {
                if (cancelled.getAsBoolean()) throw new java.util.concurrent.CancellationException();
                if (edge == null) continue;
                incident.computeIfAbsent(edge.getStartID(), ignored -> new ArrayList<>()).add(edge);
                if (!Objects.equals(edge.getStartID(), edge.getEndID())) {
                    incident.computeIfAbsent(edge.getEndID(), ignored -> new ArrayList<>()).add(edge);
                }
            }
            return new IndexedData(hierarchy, nodes, byId, byPassage, incident);
        }
    }

    private final class DialogContents {
        final Dialog<Void> window = new Dialog<>();
        final Label heading = new Label("Indexing graph relationships...");
        final TextArea details = new TextArea("Building typed hierarchy and evidence indexes...");
        final ChoiceBox<GraphHierarchyKey> hierarchyChoice = new ChoiceBox<>();
        final ListView<RelatedNode> relations = new ListView<>();
        final Button selectFocus = new Button("Select and Focus");
        final Button refresh = new Button("Inspect Current Selection");
        final GraphDirectedCollection graph;
        GraphEdge edge;
        String nodeId;
        IndexedData data;

        DialogContents(GraphDirectedCollection graph, String nodeId, GraphEdge edge) {
            this.graph = graph;
            this.nodeId = nodeId;
            this.edge = edge;
            window.setTitle("Context Graph - Hierarchy and Evidence");
            window.initOwner(view.getScene().getWindow());
            window.initModality(Modality.NONE);
            window.setResizable(true);
            window.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            heading.setWrapText(true);
            details.setEditable(false);
            details.setWrapText(true);
            details.setPrefRowCount(16);
            relations.setPrefHeight(175);
            hierarchyChoice.setMaxWidth(Double.MAX_VALUE);
            hierarchyChoice.setConverter(new StringConverter<>() {
                @Override public String toString(GraphHierarchyKey key) {
                    if (key == null) return "No typed hierarchies";
                    String scope = key.hierarchyScopeId() == null ? "unscoped" : key.hierarchyScopeId();
                    if (scope.length() > 68) scope = scope.substring(0, 68) + "...";
                    return key.predicateId() + "  |  " + scope;
                }
                @Override public GraphHierarchyKey fromString(String value) { return null; }
            });
            selectFocus.setDisable(true);
            selectFocus.setOnAction(e -> {
                RelatedNode target = relations.getSelectionModel().getSelectedItem();
                if (target != null && view.getGraphController().getCurrentGraph() == graph
                    && view.selectEntity(target.id(), true)) {
                    DialogContents.this.nodeId = target.id();
                    DialogContents.this.edge = null;
                    chooseHierarchyForNode();
                    update();
                }
            });
            relations.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) ->
                selectFocus.setDisable(selected == null));
            refresh.setOnAction(e -> {
                if (view.getGraphController().getCurrentGraph() != graph) return;
                DialogContents.this.nodeId = view.getGraphController().getSelectedNodeId();
                DialogContents.this.edge = view.getGraphController().getSelectedEdge();
                chooseHierarchyForNode();
                update();
            });
            hierarchyChoice.getSelectionModel().selectedItemProperty().addListener((obs, old, next) -> update());
            VBox body = new VBox(8,
                heading, new Label("Hierarchy scope / predicate"), hierarchyChoice,
                details, new Label("Navigate direct relationships"), relations,
                new HBox(8, selectFocus, refresh),
                new Label("Offsets refer to extracted UTF-16 text, not original PDF byte/glyph coordinates."));
            body.setPadding(new Insets(12));
            VBox.setVgrow(details, Priority.ALWAYS);
            VBox.setVgrow(relations, Priority.ALWAYS);
            window.getDialogPane().setContent(body);
            window.getDialogPane().setPrefSize(720, 730);
        }

        void loaded(IndexedData indexed) {
            data = indexed;
            hierarchyChoice.getItems().setAll(data.hierarchyIndex.keys());
            chooseHierarchyForNode();
            update();
        }

        void chooseHierarchyForNode() {
            if (data == null || nodeId == null) return;
            GraphHierarchyKey current = hierarchyChoice.getValue();
            if (current != null) {
                GraphHierarchy existing = data.hierarchyIndex.hierarchies().get(current);
                if (existing != null && existing.containsNode(nodeId)) return;
            }
            data.hierarchyIndex.keys().stream()
                .filter(key -> data.hierarchyIndex.hierarchies().get(key).containsNode(nodeId))
                .findFirst().ifPresent(key -> hierarchyChoice.setValue(key));
        }

        void update() {
            if (data == null || view.getGraphController().getCurrentGraph() != graph) return;
            relations.getItems().clear();
            StringBuilder text = new StringBuilder();
            GraphHierarchyKey chosen = hierarchyChoice.getValue();
            GraphHierarchy hierarchy = chosen == null ? null
                : data.hierarchyIndex.hierarchies().get(chosen);
            if (nodeId != null) {
                GraphNode node = data.nodes.get(nodeId);
                if (node == null) return;
                heading.setText(title(node) + "  (" + node.getNodeKind() + ")");
                add(text, "Entity ID", nodeId);
                add(text, "Node kind", node.getNodeKind());
                appendProperties(text, node.getProperties());
                if (hierarchy != null && hierarchy.containsNode(nodeId)) {
                    List<String> parents = hierarchy.immediateParents(nodeId);
                    List<String> children = hierarchy.immediateChildren(nodeId);
                    List<String> ancestors = hierarchy.ancestors(nodeId);
                    List<String> descendants = hierarchy.descendants(nodeId);
                    text.append("\nHierarchy: ").append(hierarchy.key()).append('\n');
                    add(text, "Direct parents", parents.size());
                    add(text, "Direct children", children.size());
                    add(text, "Ancestors", ancestors.size());
                    add(text, "Descendants", descendants.size());
                    add(text, "Cycles", hierarchy.cycleComponents().size());
                    for (String id : parents) addRelation("Parent", id);
                    for (String id : children) addRelation("Child", id);
                    appendIds(text, "Ancestor IDs", ancestors, 40);
                    appendIds(text, "Descendant IDs", descendants, 40);
                } else {
                    text.append("\nThis entity is not in the selected hierarchy.\n");
                }
                Map<String, GraphEvidence> supporting = new LinkedHashMap<>();
                for (GraphEvidence ref : data.evidenceByPassage.getOrDefault(nodeId, List.of())) {
                    supporting.put(ref.getEvidenceId(), ref);
                }
                for (GraphEdge incident : data.incidentEdges.getOrDefault(nodeId, List.of())) {
                    collectEvidence(supporting, incident);
                }
                appendEvidence(text, supporting.values().stream().toList());
            } else if (edge != null) {
                heading.setText("Selected relationship");
                add(text, "Edge ID", edge.getEdgeId());
                add(text, "Source", edge.getStartID());
                add(text, "Target", edge.getEndID());
                GraphRelationship relation = edge.getRelationship();
                if (relation != null) {
                    add(text, "Labels", relation.getLabels());
                    add(text, "Predicate", relation.getSemantics() == null
                        ? "Unspecified" : relation.getSemantics().getPredicateId());
                    add(text, "Origin", relation.getSemantics() == null
                        ? "Unspecified" : relation.getSemantics().getOrigin());
                    add(text, "Hierarchy scope", relation.getSemantics() == null
                        ? "Unspecified" : relation.getSemantics().getHierarchyScopeId());
                    appendProperties(text, relation.getProperties());
                }
                addRelation("Source", edge.getStartID());
                addRelation("Target", edge.getEndID());
                Map<String, GraphEvidence> supporting = new LinkedHashMap<>();
                collectEvidence(supporting, edge);
                appendEvidence(text, supporting.values().stream().toList());
            } else {
                heading.setText("No selection");
                text.append("Choose a node or edge in the graph, then Inspect Current Selection.\n");
            }
            details.setText(text.toString());
            details.positionCaret(0);
        }

        void collectEvidence(Map<String, GraphEvidence> supporting, GraphEdge edge) {
            if (edge.getRelationship() == null) return;
            GraphRelationshipSemantics semantics = edge.getRelationship().getSemantics();
            if (semantics == null || semantics.getEvidenceIds() == null) return;
            for (String id : semantics.getEvidenceIds()) {
                GraphEvidence evidence = data.evidenceById.get(id);
                if (evidence != null) supporting.put(id, evidence);
            }
        }

        void addRelation(String relation, String id) {
            GraphNode node = data.nodes.get(id);
            relations.getItems().add(new RelatedNode(relation, id,
                node == null ? id : title(node)));
        }
    }

    private static String title(GraphNode node) {
        if (node.getProperties() != null) {
            Object name = node.getProperties().get("name");
            if (name != null && !name.toString().isBlank()) return name.toString();
        }
        return node.getEntityID();
    }

    private static void appendProperties(StringBuilder out, Map<String, Object> props) {
        if (props == null || props.isEmpty()) return;
        out.append("\nSource metadata\n");
        props.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            if (!"text".equals(entry.getKey())) add(out, entry.getKey(), entry.getValue());
        });
        Object text = props.get("text");
        if (text != null) add(out, "Passage text", text);
    }

    private static void appendIds(StringBuilder out, String label, List<String> ids, int limit) {
        if (ids.isEmpty()) return;
        out.append(label).append(" (first ").append(Math.min(limit, ids.size()))
            .append(" of ").append(ids.size()).append("):\n");
        for (int i = 0; i < Math.min(limit, ids.size()); i++) {
            out.append("  ").append(ids.get(i)).append('\n');
        }
    }

    private static void appendEvidence(StringBuilder out, List<GraphEvidence> evidence) {
        out.append("\nSupporting evidence records: ").append(evidence.size()).append('\n');
        for (int i = 0; i < Math.min(evidence.size(), 30); i++) {
            GraphEvidence item = evidence.get(i);
            add(out, "Evidence", item.getEvidenceId());
            add(out, "Document", item.getDocumentId());
            add(out, "Revision", item.getDocumentRevision());
            add(out, "Passage", item.getPassageId());
            if (item.getPageNumber() != null) add(out, "PDF page", item.getPageNumber());
            if (item.getSectionId() != null) add(out, "Section", item.getSectionId());
            if (item.getStartOffset() != null) {
                add(out, "UTF-16 source span", "[" + item.getStartOffset() + ", "
                    + item.getEndOffset() + ") " + item.getSpanCoordinateSystem());
            }
            if (item.getQuote() != null) add(out, "Exact extracted quote", item.getQuote());
            add(out, "Extraction method", item.getExtractionMethod());
            add(out, "Review status", item.getReviewStatus());
            out.append('\n');
        }
        if (evidence.size() > 30) out.append("Additional evidence records omitted from this display.\n");
    }

    private static void add(StringBuilder out, String key, Object value) {
        if (value != null) out.append(key).append(": ").append(value).append('\n');
    }
}
