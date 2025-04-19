package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class GraphEvent extends Event {

    public Object object;
    public Object object2;

    public static final EventType<GraphEvent> EXPORT_GRAPH_COLLECTION = new EventType(ANY, "EXPORT_GRAPH_COLLECTION");
    public static final EventType<GraphEvent> NEW_GRAPHDIRECTED_COLLECTION = new EventType(ANY, "NEW_GRAPHDIRECTED_COLLECTION");
    public static final EventType<GraphEvent> UPDATE_GRAPH_COMPONENTS = new EventType(ANY, "UPDATE_GRAPH_COMPONENTS");

    public GraphEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public GraphEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object = arg1;
    }

    public GraphEvent(EventType<? extends Event> arg0, Object arg1, Object arg2) {
        this(arg0);
        object = arg1;
        object2 = arg2;
    }

    public GraphEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
