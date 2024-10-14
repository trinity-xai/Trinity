/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class HitEvent extends Event {

    public Object object1 = null;
    public Object object2 = null;

    public static final EventType<HitEvent> TRACKING_PROJECTILE_EVENTS = new EventType(ANY, "TRACKING_PROJECTILE_EVENTS");
    public static final EventType<HitEvent> RAY_INTERSECTS_BOX = new EventType(ANY, "RAY_INTERSECTS_BOX");
    public static final EventType<HitEvent> PROJECTILE_HIT_BOX = new EventType(ANY, "PROJECTILE_HIT_BOX");
    public static final EventType<HitEvent> PROJECTILE_HIT_BRICK = new EventType(ANY, "PROJECTILE_HIT_BRICK");
    public static final EventType<HitEvent> PROJECTILE_HIT_SHIELD = new EventType(ANY, "PROJECTILE_HIT_SHIELD");
    public static final EventType<HitEvent> PROJECTILE_HIT_CHARACTER = new EventType(ANY, "PROJECTILE_HIT_CHARACTER");
    public static final EventType<HitEvent> SHAPE_HIT_BOX = new EventType(ANY, "SHAPE_HIT_BOX");
    public static final EventType<HitEvent> PROJECTILE_HIT_SHAPE = new EventType(ANY, "PROJECTILE_HIT_SHAPE");

    public HitEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public HitEvent(EventType<? extends Event> arg0, Object arg1, Object arg2) {
        this(arg0);
        object1 = arg1;
        object2 = arg2;
    }

    public HitEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object1 = arg0;
    }
}
