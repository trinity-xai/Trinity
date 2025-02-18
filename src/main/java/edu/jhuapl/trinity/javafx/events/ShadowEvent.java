/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class ShadowEvent extends Event {

    public Object object;

    public static final EventType<ShadowEvent> FIXED_ORHOGRAPHIC_PROJECTION = new EventType(ANY, "FIXED_TRANSFORM_PROJECTION");
    public static final EventType<ShadowEvent> ROTATING_PERSPECTIVE_PROJECTION = new EventType(ANY, "ROTATING_PERSPECTIVE_PROJECTION");
    public static final EventType<ShadowEvent> SHOW_AXES_LABELS = new EventType(ANY, "SHOW_AXES_LABELS");
    public static final EventType<ShadowEvent> OVERRIDE_DOMAIN_TRANSFORM = new EventType(ANY, "OVERRIDE_DOMAIN_TRANSFORM");
    public static final EventType<ShadowEvent> OVERRIDE_XFORM = new EventType(ANY, "OVERRIDE_XFORM");
    public static final EventType<ShadowEvent> SET_PANEL_OPACITY = new EventType(ANY, "SET_PANEL_OPACITY");
    public static final EventType<ShadowEvent> SHOW_NEARSIDE_POINTS = new EventType(ANY, "SHOW_NEARSIDE_POINTS");
    public static final EventType<ShadowEvent> ENABLE_CUBE_PROJECTIONS = new EventType(ANY, "ENABLE_CUBE_PROJECTIONS");
    public static final EventType<ShadowEvent> SET_GRIDLINES_VISIBLE = new EventType(ANY, "SET_GRIDLINES_VISIBLE");
    public static final EventType<ShadowEvent> SET_FRAME_VISIBLE = new EventType(ANY, "SET_FRAME_VISIBLE");
    public static final EventType<ShadowEvent> SET_CONTROLPOINTS_VISIBLE = new EventType(ANY, "SET_CONTROLPOINTS_VISIBLE");
    public static final EventType<ShadowEvent> SET_CUBEWALLS_VISIBLE = new EventType(ANY, "SET_CUBEWALLS_VISIBLE");
    public static final EventType<ShadowEvent> SET_CUBE_VISIBLE = new EventType(ANY, "SET_CUBE_VISIBLE");
    public static final EventType<ShadowEvent> SET_POINT_SCALING = new EventType(ANY, "SET_POINT_SCALING");
    public static final EventType<ShadowEvent> SET_DOMAIN_MINIMUM = new EventType(ANY, "SET_DOMAIN_MINIMUM");
    public static final EventType<ShadowEvent> SET_DOMAIN_MAXIMUM = new EventType(ANY, "SET_DOMAIN_MAXIMUM");
    public static final EventType<ShadowEvent> SET_POINT_OPACITY = new EventType(ANY, "SET_POINT_OPACITY");

    public ShadowEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public ShadowEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object = arg1;
    }

    public ShadowEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
