package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class VisibilityEvent extends Event {

    public Object eventObject;
    public static final EventType<VisibilityEvent> VISIBILITY_CHANGED = new EventType(ANY, "VISIBILITY_CHANGED");

    public VisibilityEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public VisibilityEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        eventObject = arg1;
    }

    public VisibilityEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        eventObject = arg0;
    }

}
