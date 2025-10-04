package edu.jhuapl.trinity.javafx.events;

import edu.jhuapl.trinity.utils.graph.GraphStyleParams;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.paint.Color;

/**
 * @author Sean Phillips
 */
public class GraphEvent extends Event {

    public Object object;
    public Object object2;

    public static final EventType<GraphEvent> EXPORT_GRAPH_COLLECTION = new EventType(ANY, "EXPORT_GRAPH_COLLECTION");
    public static final EventType<GraphEvent> NEW_GRAPHDIRECTED_COLLECTION = new EventType(ANY, "NEW_GRAPHDIRECTED_COLLECTION");
    public static final EventType<GraphEvent> UPDATE_GRAPH_COMPONENTS = new EventType(ANY, "UPDATE_GRAPH_COMPONENTS");
    public static final EventType<GraphEvent> GRAPH_PARAMS_CHANGED = new EventType<>(ANY, "GRAPH_PARAMS_CHANGED");
    public static final EventType<GraphEvent> GRAPH_REBUILD_PARAMS = new EventType<>(ANY, "GRAPH_REBUILD_PARAMS");
    public static final EventType<GraphEvent> GRAPH_RESET_PARAMS = new EventType<>(ANY, "GRAPH_RESET_PARAMS");
    // Graph style (runtime + reset)
    public static final EventType<GraphEvent> GRAPH_STYLE_PARAMS_CHANGED = new EventType<>(ANY, "GRAPH_STYLE_PARAMS_CHANGED");
    public static final EventType<GraphEvent> GRAPH_STYLE_RESET_DEFAULTS = new EventType<>(ANY, "GRAPH_STYLE_RESET_DEFAULTS");
    // GUI sync (fine-grained) — model → UI
    public static final EventType<GraphEvent> SET_NODE_COLOR_GUI = new EventType<>(ANY, "SET_NODE_COLOR_GUI");
    public static final EventType<GraphEvent> SET_NODE_RADIUS_GUI = new EventType<>(ANY, "SET_NODE_RADIUS_GUI");
    public static final EventType<GraphEvent> SET_NODE_OPACITY_GUI = new EventType<>(ANY, "SET_NODE_OPACITY_GUI");
    public static final EventType<GraphEvent> SET_EDGE_COLOR_GUI = new EventType<>(ANY, "SET_EDGE_COLOR_GUI");
    public static final EventType<GraphEvent> SET_EDGE_WIDTH_GUI = new EventType<>(ANY, "SET_EDGE_WIDTH_GUI");
    public static final EventType<GraphEvent> SET_EDGE_OPACITY_GUI = new EventType<>(ANY, "SET_EDGE_OPACITY_GUI");
    // Optional: coarse-grained hydrate — model → UI
    public static final EventType<GraphEvent> SET_STYLE_GUI = new EventType<>(ANY, "SET_STYLE_GUI");
    // Interaction / inspect
    public static final EventType<GraphEvent> GRAPH_NODE_HOVER = new EventType<>(ANY, "GRAPH_NODE_HOVER");
    public static final EventType<GraphEvent> GRAPH_NODE_CLICK = new EventType<>(ANY, "GRAPH_NODE_CLICK");
    public static final EventType<GraphEvent> GRAPH_EDGE_HOVER = new EventType<>(ANY, "GRAPH_EDGE_HOVER");
    public static final EventType<GraphEvent> GRAPH_EDGE_CLICK = new EventType<>(ANY, "GRAPH_EDGE_CLICK");
    // Graph overlay visibility
    public static final EventType<GraphEvent> GRAPH_VISIBILITY_CHANGED = new EventType<>(ANY, "GRAPH_VISIBILITY_CHANGED");
    public static final EventType<GraphEvent> SET_GRAPH_VISIBILITY_GUI = new EventType<>(ANY, "SET_GRAPH_VISIBILITY_GUI");

    public GraphEvent(EventType<? extends Event> arg0) { super(arg0); }
    public GraphEvent(EventType<? extends Event> arg0, Object arg1) { this(arg0); object = arg1; }
    public GraphEvent(EventType<? extends Event> arg0, Object arg1, Object arg2) { this(arg0); object = arg1; object2 = arg2; }
    public GraphEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) { super(arg0, arg1, arg2); object = arg0; }

    // Convenience factory methods (optional, mirrors HypersurfaceEvent style)
    public static GraphEvent styleParamsChanged(GraphStyleParams p){ return new GraphEvent(GRAPH_STYLE_PARAMS_CHANGED, GraphStyleParams.copyOf(p)); }
    public static GraphEvent resetStyleDefaults(){ return new GraphEvent(GRAPH_STYLE_RESET_DEFAULTS, null); }
    public static GraphEvent setNodeColorGUI(Color c){ return new GraphEvent(SET_NODE_COLOR_GUI, c); }
    public static GraphEvent setNodeRadiusGUI(double r){ return new GraphEvent(SET_NODE_RADIUS_GUI, r); }
    public static GraphEvent setNodeOpacityGUI(double o){ return new GraphEvent(SET_NODE_OPACITY_GUI, o); }
    public static GraphEvent setEdgeColorGUI(Color c){ return new GraphEvent(SET_EDGE_COLOR_GUI, c); }
    public static GraphEvent setEdgeWidthGUI(double w){ return new GraphEvent(SET_EDGE_WIDTH_GUI, w); }
    public static GraphEvent setEdgeOpacityGUI(double o){ return new GraphEvent(SET_EDGE_OPACITY_GUI, o); }
    public static GraphEvent setStyleGUI(GraphStyleParams p){ return new GraphEvent(SET_STYLE_GUI, GraphStyleParams.copyOf(p)); }
}
