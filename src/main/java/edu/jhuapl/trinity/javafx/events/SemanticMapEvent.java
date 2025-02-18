/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class SemanticMapEvent extends Event {

    public Object object;
    public static final EventType<SemanticMapEvent> NEW_SEMANTICMAP_COLLECTION = new EventType(ANY, "NEW_SEMANTICMAP_COLLECTION");
    public static final EventType<SemanticMapEvent> NEW_SEMANTIC_MAP = new EventType(ANY, "NEW_SEMANTIC_MAP");
//    public static final EventType<SemanticMapEvent> LOCATE_FEATURE_VECTOR = new EventType(ANY, "LOCATE_FEATURE_VECTOR");

    public SemanticMapEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public SemanticMapEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object = arg1;
    }

    public SemanticMapEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
