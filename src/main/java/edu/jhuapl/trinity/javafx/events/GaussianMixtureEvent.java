package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class GaussianMixtureEvent extends Event {

    public Object object;
    public static final EventType<GaussianMixtureEvent> NEW_GAUSSIAN_COLLECTION = new EventType(ANY, "NEW_GAUSSIAN_COLLECTION");
    public static final EventType<GaussianMixtureEvent> NEW_GAUSSIAN_MIXTURE = new EventType(ANY, "NEW_GAUSSIAN_MIXTURE");
    public static final EventType<GaussianMixtureEvent> LOCATE_GAUSSIAN_MIXTURE = new EventType(ANY, "LOCATE_GAUSSIAN_MIXTURE");

    public GaussianMixtureEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public GaussianMixtureEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object = arg1;
    }

    public GaussianMixtureEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
