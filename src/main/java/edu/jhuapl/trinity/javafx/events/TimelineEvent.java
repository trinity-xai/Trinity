/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Luis C. Puche
 */
public class TimelineEvent extends Event {

    public static final EventType<TimelineEvent> TIMELINE_SET_SAMPLEFRAMERATE = new EventType(ANY, "TIMELINE_SET_SAMPLEFRAMERATE");
    public static final EventType<TimelineEvent> TIMELINE_SAMPLE_INDEX = new EventType(ANY, "TIMELINE_SAMPLE_INDEX");
    public static final EventType<TimelineEvent> TIMELINE_STEP_BACKWARD = new EventType(ANY, "TIMELINE_STEP_BACKWARD");
    public static final EventType<TimelineEvent> TIMELINE_STEP_FORWARD = new EventType(ANY, "TIMELINE_STEP_FORWARD");
    public static final EventType<TimelineEvent> TIMELINE_SET_VISIBLE = new EventType(ANY, "TIMELINE_SET_VISIBLE");
    public static final EventType<TimelineEvent> TIMELINE_CLEAR_ITEMS = new EventType(ANY, "TIMELINE_CLEAR_ITEMS");
    public static final EventType<TimelineEvent> TIMELINE_ADD_ITEMS = new EventType(ANY, "TIMELINE_ADD_ITEMS");
    public static final EventType<TimelineEvent> TIMELINE_DISPLAY_DURATION = new EventType(ANY, "TIMELINE_DISPLAY_DURATION");
    public static final EventType<TimelineEvent> TIMELINE_INCREASE_PROPRATE = new EventType(ANY, "TIMELINE_INCREASE_PROPRATE");
    public static final EventType<TimelineEvent> TIMELINE_DECREASE_PROPRATE = new EventType(ANY, "TIMELINE_DECREASE_PROPRATE");
    public static final EventType<TimelineEvent> TIMELINE_GET_CURRENT_PROP_RATE = new EventType(ANY, "TIMELINE_GET_CURRENT_PROP_RATE");
    public static final EventType<TimelineEvent> TIMELINE_SET_ANIMATIONDURATION = new EventType(ANY, "TIMELINE_SET_ANIMATIONDURATION");
    public static final EventType<TimelineEvent> TIMELINE_SET_LOOP = new EventType(ANY, "TIMELINE_SET_LOOP");
    public static final EventType<TimelineEvent> TIMELINE_RESTART = new EventType(ANY, "TIMELINE_RESTART");
    public static final EventType<TimelineEvent> TIMELINE_PAUSE = new EventType(ANY, "TIMELINE_PAUSE");
    public static final EventType<TimelineEvent> TIMELINE_PLAY = new EventType(ANY, "TIMELINE_PLAY");
    public static final EventType<TimelineEvent> TIMELINE_PLAY_BACKWARDS = new EventType(ANY, "TIMELINE_PLAY_BACKWARDS");
    public static final EventType<TimelineEvent> TIMELINE_UPDATE_ANIMATIONTIME = new EventType(ANY, "TIMELINE_UPDATE_ANIMATIONTIME");
    public static final EventType<TimelineEvent> TIMELINE_SET_INITIALTIME = new EventType(ANY, "TIMELINE_SET_INITIALTIME");

    public Object object = null;

    public TimelineEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }

    public TimelineEvent(EventType<? extends Event> arg0, Object object) {
        this(arg0);
        this.object = object;
    }
}
