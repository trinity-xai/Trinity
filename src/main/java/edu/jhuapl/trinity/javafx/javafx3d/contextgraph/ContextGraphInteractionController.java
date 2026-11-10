package edu.jhuapl.trinity.javafx.javafx3d.contextgraph;

import edu.jhuapl.trinity.data.graph.GraphEdge;
import edu.jhuapl.trinity.data.graph.GraphNode;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Context Graph event reporting. Actual picking/selection lives in the pane,
 * navigation stays hover-driven in NavigatorPane, and graph events remain
 * available to other views. No synthetic FactorAnalysisEvent adjacency rows.
 */
public final class ContextGraphInteractionController {
    private final Scene scene;
    private boolean installed;

    public ContextGraphInteractionController(Scene scene) {
        this.scene = scene;
    }

    public void installEventHandlers() {
        if (installed || scene == null) return;
        installed = true;
        scene.addEventHandler(GraphEvent.GRAPH_NODE_CLICK, e -> {
            if (!(e.object instanceof GraphNode node)) return;
            if (!(e.object2 instanceof Boolean selected)) return;
            report("Graph node " + (selected ? "selected: " : "deselected: ") + node.getEntityID());
        });
        scene.addEventHandler(GraphEvent.GRAPH_EDGE_CLICK, e -> {
            if (!(e.object instanceof GraphEdge edge)) return;
            if (!(e.object2 instanceof Boolean selected)) return;
            report("Graph edge " + (selected ? "selected: " : "deselected: ")
                + edge.getStartID() + " -> " + edge.getEndID());
        });
        scene.addEventHandler(GraphEvent.GRAPH_EDGE_HOVER, e -> {
            if (!(e.object instanceof GraphEdge edge)) return;
            report("Edge hover: " + edge.getStartID() + " -> " + edge.getEndID());
        });
    }

    private void report(String message) {
        if (scene.getRoot() != null) {
            scene.getRoot().fireEvent(new CommandTerminalEvent(
                message, new Font("Consolas", 16), Color.ALICEBLUE));
        }
    }
}
