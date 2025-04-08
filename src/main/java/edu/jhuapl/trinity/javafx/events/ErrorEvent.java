package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */

public class ErrorEvent extends Event {

    public static final EventType<ErrorEvent> REST_ERROR = new EventType(ANY, "REST_ERROR");
    public String errorMessage;

    public ErrorEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }

    public ErrorEvent(EventType<? extends Event> arg0, String message) {
        this(arg0);
        this.errorMessage = message;
    }
}
