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
    public Object object2;
    
    public static final EventType<RestEvent> START_RESTSERVER_THREAD = new EventType(ANY, "START_RESTSERVER_THREAD");
    public static final EventType<RestEvent> TERMINATE_RESTSERVER_THREAD = new EventType(ANY, "TERMINATE_RESTSERVER_THREAD");
    public static final EventType<RestEvent> START_RESTSERVER_PROCESSING = new EventType(ANY, "START_RESTSERVER_PROCESSING");
    public static final EventType<RestEvent> STOP_RESTSERVER_PROCESSING = new EventType(ANY, "STOP_RESTSERVER_PROCESSING");

    public static final EventType<RestEvent> NEW_EMBEDDINGS_IMAGE = new EventType(ANY, "NEW_EMBEDDINGS_IMAGE");
    public static final EventType<RestEvent> NEW_CHAT_COMPLETION = new EventType(ANY, "NEW_CHAT_COMPLETION");
    public static final EventType<RestEvent> ERROR_EMBEDDINGS_IMAGE = new EventType(ANY, "ERROR_EMBEDDINGS_IMAGE");
    public static final EventType<RestEvent> ERROR_CHAT_COMPLETIONS = new EventType(ANY, "ERROR_CHAT_COMPLETIONS");
    public static final EventType<RestEvent> EMBEDDING_MODELS_ALIVE = new EventType(ANY, "EMBEDDING_MODELS_ALIVE");
    public static final EventType<RestEvent> CHAT_MODELS_ALIVE = new EventType(ANY, "CHAT_MODELS_ALIVE");

    public RestEvent(EventType<? extends Event> arg0, Object t) {
        super(arg0);
        object = t;
    }
    public RestEvent(EventType<? extends Event> arg0, Object object1, Object object2) {
        super(arg0);
        object = object1;
        this.object2 = object2;
    }

    public RestEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
