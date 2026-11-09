package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.FactorAnalysisEvent;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Graph-specific picking and analytical event routing, independent of Hypersurface. */
public final class ContextGraphInteractionController {
    private final Scene scene;
    private final ContextGraphController graphController;
    private boolean installed;
    private GraphDirectedCollection indexedGraph;
    private final Map<String, Integer> indices = new HashMap<>();
    private int[][] adjacency = new int[0][];

    public ContextGraphInteractionController(Scene scene,
            ContextGraphController graphController) {
        this.scene = scene;
        this.graphController = graphController;
    }

    public void installEventHandlers() {
        if (installed || scene == null) return;
        installed = true;
        scene.addEventHandler(GraphEvent.GRAPH_NODE_CLICK, e -> {
            if (e.object instanceof GraphNode node) {
                publishVector("Graph Adjacency Row (click): " + node.getEntityID(), adjacencyRow(node));
            }
        });
        scene.addEventHandler(GraphEvent.GRAPH_NODE_HOVER, e -> {
            if (e.object instanceof GraphNode node) {
                publishVector("Graph Adjacency Row (hover): " + node.getEntityID(), adjacencyRow(node));
            }
        });
        scene.addEventHandler(GraphEvent.GRAPH_EDGE_CLICK, e -> {
            if (e.object instanceof GraphEdge edge) {
                publishVector("Graph Edge Weight (click): " + edge.getStartID()
                    + " -> " + edge.getEndID(), new Double[]{1.0});
            }
        });
        scene.addEventHandler(GraphEvent.GRAPH_EDGE_HOVER, e -> {
            if (!(e.object instanceof GraphEdge edge)) return;
            GraphDirectedCollection graph = graphController.getCurrentGraph();
            Optional<GraphNode> start = graph == null ? Optional.empty()
                : graph.findNodeById(edge.getStartID());
            Optional<GraphNode> end = graph == null ? Optional.empty()
                : graph.findNodeById(edge.getEndID());
            scene.getRoot().fireEvent(new CommandTerminalEvent(
                "Edge hover: " + start.map(GraphNode::getEntityID).orElse("?")
                    + " -> " + end.map(GraphNode::getEntityID).orElse("?"),
                new Font("Consolas", 16), Color.ALICEBLUE));
        });
    }

    private void publishVector(String label, Double[] vector) {
        if (scene.getRoot() != null) {
            scene.getRoot().fireEvent(new FactorAnalysisEvent(
                FactorAnalysisEvent.ANALYSIS_DATA_VECTOR, label, vector));
        }
    }

    private Double[] adjacencyRow(GraphNode node) {
        GraphDirectedCollection graph = graphController.getCurrentGraph();
        if (graph == null || graph.getNodes() == null || node == null) return new Double[0];
        if (graph != indexedGraph) index(graph);
        Integer index = indices.get(node.getEntityID());
        if (index == null) return new Double[0];
        Double[] row = new Double[adjacency.length];
        Arrays.fill(row, 0.0);
        for (int neighbor : adjacency[index]) row[neighbor] = 1.0;
        return row;
    }

    private void index(GraphDirectedCollection graph) {
        indexedGraph = graph;
        indices.clear();
        List<GraphNode> nodes = graph.getNodes();
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i) != null && nodes.get(i).getEntityID() != null)
                indices.put(nodes.get(i).getEntityID(), i);
        }
        java.util.ArrayList<java.util.HashSet<Integer>> links = new java.util.ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) links.add(new java.util.HashSet<>());
        if (graph.getEdges() != null) for (GraphEdge edge : graph.getEdges()) {
            Integer from = indices.get(edge.getStartID());
            Integer to = indices.get(edge.getEndID());
            if (from != null && to != null) links.get(from).add(to);
        }
        adjacency = new int[nodes.size()][];
        for (int i = 0; i < nodes.size(); i++) {
            adjacency[i] = links.get(i).stream().mapToInt(Integer::intValue).toArray();
        }
    }
}
