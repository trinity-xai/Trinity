/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class FullscreenEvent extends Event {

    public boolean setFullscreen;
    public static final EventType<FullscreenEvent> SET_FULLSCREEN = new EventType(ANY, "SET_FULLSCREEN");

    public FullscreenEvent(boolean setFullscreen) {
        this(SET_FULLSCREEN);
        this.setFullscreen = setFullscreen;
    }

    public FullscreenEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public FullscreenEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        setFullscreen = (boolean) arg0;
    }
}
