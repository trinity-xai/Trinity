package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class HyperdriveEvent extends Event {

    public Object object1;
    public Object object2;

    public static final EventType<HyperdriveEvent> NEW_BATCH_IMAGELOAD = new EventType(ANY, "NEW_BATCH_IMAGELOAD");
    public static final EventType<HyperdriveEvent> NEW_BATCH_TEXTLOAD = new EventType(ANY, "NEW_BATCH_TEXTLOAD");

    public HyperdriveEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public HyperdriveEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object1 = arg1;
    }

    public HyperdriveEvent(EventType<? extends Event> arg0, Object arg1, Object arg2) {
        this(arg0);
        object1 = arg1;
        object2 = arg2;
    }
}
