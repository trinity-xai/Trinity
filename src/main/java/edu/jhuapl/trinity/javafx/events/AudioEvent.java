/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class AudioEvent extends Event {

    public static final EventType<AudioEvent> NEW_AUDIO_FILE = new EventType(ANY, "NEW_AUDIO_FILE");
    public Object object = null;

    public AudioEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public AudioEvent(EventType<? extends Event> arg0, Object object) {
        this(arg0);
        this.object = object;
    }
}
