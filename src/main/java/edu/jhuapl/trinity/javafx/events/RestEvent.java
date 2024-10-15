/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class RestEvent extends Event {

    public Object object;

    public static final EventType<RestEvent> START_RESTSERVER_THREAD = new EventType(ANY, "START_RESTSERVER_THREAD");
    public static final EventType<RestEvent> TERMINATE_RESTSERVER_THREAD = new EventType(ANY, "TERMINATE_RESTSERVER_THREAD");
    public static final EventType<RestEvent> START_RESTSERVER_PROCESSING = new EventType(ANY, "START_RESTSERVER_PROCESSING");
    public static final EventType<RestEvent> STOP_RESTSERVER_PROCESSING = new EventType(ANY, "STOP_RESTSERVER_PROCESSING");


    public RestEvent(EventType<? extends Event> arg0, Object t) {
        super(arg0);
        object = t;
    }

    public RestEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
