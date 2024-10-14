/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class TerrainEvent extends Event {

    public Object object;

    public static final EventType<TerrainEvent> NEW_TERRAIN_TEXTFILE = new EventType(ANY, "NEW_TERRAIN_TEXTFILE");
    public static final EventType<TerrainEvent> NEW_FIREAREA_TEXTFILE = new EventType(ANY, "NEW_FIREAREA_TEXTFILE");

    public TerrainEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public TerrainEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object = arg1;
    }

    public TerrainEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
