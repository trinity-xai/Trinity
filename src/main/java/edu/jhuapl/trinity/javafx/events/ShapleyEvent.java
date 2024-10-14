/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class ShapleyEvent extends Event {

    public Object object;
    public Object object2;

    public static final EventType<ShapleyEvent> EXPORT_SHAPLEY_COLLECTION = new EventType(ANY, "EXPORT_SHAPLEY_COLLECTION");
    public static final EventType<ShapleyEvent> NEW_SHAPLEY_COLLECTION = new EventType(ANY, "NEW_SHAPLEY_COLLECTION");
    public static final EventType<ShapleyEvent> NEW_SHAPLEY_VECTOR = new EventType(ANY, "NEW_SHAPLEY_VECTOR");

    public ShapleyEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public ShapleyEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object = arg1;
    }

    public ShapleyEvent(EventType<? extends Event> arg0, Object arg1, Object arg2) {
        this(arg0);
        object = arg1;
        object2 = arg2;
    }

    public ShapleyEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
