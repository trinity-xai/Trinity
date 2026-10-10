package edu.jhuapl.trinity.javafx.javafx3d.hypersurface;

import edu.jhuapl.trinity.data.graph.GraphDirectedCollection;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.GraphEvent;
import edu.jhuapl.trinity.javafx.javafx3d.animated.AnimatedSphere;
import edu.jhuapl.trinity.javafx.javafx3d.animated.Tracer;
import edu.jhuapl.trinity.javafx.renderers.Graph3DRenderer;
import edu.jhuapl.trinity.utils.graph.GraphStyleParams;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.function.Consumer;

/**
 * Owns the temporary 3D graph visual subsystem hosted by the Hypersurface view.
 *
 * <p>The graph was originally embedded in Hypersurface as an experiment. This
 * controller deliberately avoids depending on {@link Hypersurface3DPane} so the
 * graph rendering/style logic can later migrate to a dedicated 3D graph pane and
 * SubScene with minimal restructuring. The optional graph consumer is the temporary
 * bridge used to keep Hypersurface-specific graph-to-surface interaction working.</p>
 *
 * @author Sean Phillips
 */
public final class HypersurfaceGraphController {

    private final Scene scene;
    private final Consumer<GraphDirectedCollection> graphConsumer;
    private final Group graphLayer = new Group();

    private boolean graphVisible = true;
    private GraphDirectedCollection currentGraph;
    private final Graph3DRenderer.Params graphParams = new Graph3DRenderer.Params()
        .withNodeRadius(20.0)
        .withEdgeWidth(8.0f)
        .withPositionScalar(1.0);
    private GraphStyleParams styleParams = new GraphStyleParams();
    private boolean eventHandlersInstalled;

    public HypersurfaceGraphController(Scene scene,
                                       Consumer<GraphDirectedCollection> graphConsumer) {
        this.scene = scene;
        this.graphConsumer = graphConsumer;
        graphLayer.setVisible(graphVisible);
    }


    /**
     * Synchronizes graph-owned state back to the existing graph controls.
     */
    public void syncVisibilityGui() {
        fireOnRoot(new GraphEvent(GraphEvent.SET_GRAPH_VISIBILITY_GUI, graphVisible));
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

        scene.addEventHandler(GraphEvent.GRAPH_STYLE_PARAMS_CHANGED, e -> {
            if (!(e.object instanceof GraphStyleParams p)) return;

            styleParams.nodeColor = p.nodeColor;
            styleParams.nodeRadius = p.nodeRadius;
            styleParams.nodeOpacity = clamp01(p.nodeOpacity);
            styleParams.edgeColor = p.edgeColor;
            styleParams.edgeWidth = p.edgeWidth;
            styleParams.edgeOpacity = clamp01(p.edgeOpacity);

            applyGraphStyle(styleParams, true);
        });

        scene.addEventHandler(GraphEvent.GRAPH_STYLE_RESET_DEFAULTS, e -> {
            styleParams = new GraphStyleParams();

            graphParams.withNodeRadius(styleParams.nodeRadius)
                .withEdgeWidth((float) styleParams.edgeWidth);

            if (currentGraph != null) {
                rebuildGraph();
            }
            applyGraphStyle(styleParams, false);
            fireOnRoot(new GraphEvent(GraphEvent.SET_STYLE_GUI, styleParams));
        });

        scene.addEventHandler(GraphEvent.NEW_GRAPHDIRECTED_COLLECTION, e -> {
            if (!(e.object instanceof GraphDirectedCollection graph)) return;
            setGraph(graph);
        });

        scene.addEventHandler(GraphEvent.GRAPH_VISIBILITY_CHANGED, e -> {
            if (!(e.object instanceof Boolean visible)) return;
            setGraphVisible(visible);
        });
    }

    private void setGraph(GraphDirectedCollection graph) {
        currentGraph = graph;
        if (graphConsumer != null) {
            graphConsumer.accept(graph);
        }

        rebuildGraph();
        applyGraphStyle(styleParams, false);

        fireOnRoot(new GraphEvent(GraphEvent.SET_STYLE_GUI, styleParams));
        fireOnRoot(new GraphEvent(GraphEvent.SET_GRAPH_VISIBILITY_GUI, graphVisible));
        scene.getRoot().fireEvent(new CommandTerminalEvent(
            "Rendered 3D graph: nodes=" + graph.getNodes().size()
                + ", edges=" + graph.getEdges().size(),
            new Font("Consolas", 18), Color.LIGHTGREEN));
    }

    private void setGraphVisible(boolean visible) {
        graphVisible = visible;
        graphLayer.setVisible(visible);
    }

    private void rebuildGraph() {
        graphLayer.getChildren().clear();
        if (currentGraph != null) {
            graphLayer.getChildren().add(
                Graph3DRenderer.buildGraphGroup(currentGraph, graphParams));
        }
    }

    /**
     * Applies the current graph style. Geometry is rebuilt only when edge width
     * changes because edge width is encoded into the generated edge geometry.
     */
    private void applyGraphStyle(GraphStyleParams style, boolean rebuildIfNeeded) {
        if (style == null) return;

        double currentEdgeWidth = graphParams.edgeWidth;
        boolean needRebuild = Math.abs(style.edgeWidth - currentEdgeWidth) > 1.0e-6;

        if (needRebuild && rebuildIfNeeded && currentGraph != null) {
            graphParams.withNodeRadius(style.nodeRadius)
                .withEdgeWidth((float) style.edgeWidth);
            rebuildGraph();
        }

        for (Node node : graphLayer.getChildren()) {
            applyGraphStyleRecursive(node, style);
        }
    }

    private void applyGraphStyleRecursive(Node node, GraphStyleParams style) {
        if (node instanceof AnimatedSphere sphere) {
            if (style.nodeColor != null) {
                sphere.setColor(new Color(
                    style.nodeColor.getRed(),
                    style.nodeColor.getGreen(),
                    style.nodeColor.getBlue(),
                    sphere.getPhongMaterial().getDiffuseColor() != null
                        ? sphere.getPhongMaterial().getDiffuseColor().getOpacity()
                        : 1.0));
            }
            sphere.setSphereRadius(style.nodeRadius);
            sphere.setMaterialOpacity(style.nodeOpacity);
        } else if (node instanceof Tracer tracer) {
            if (style.edgeColor != null) {
                tracer.setDiffuseColor(new Color(
                    style.edgeColor.getRed(),
                    style.edgeColor.getGreen(),
                    style.edgeColor.getBlue(),
                    1.0));
            }
            tracer.setOpacityAlpha(style.edgeOpacity);
        } else if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyGraphStyleRecursive(child, style);
            }
        }
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
