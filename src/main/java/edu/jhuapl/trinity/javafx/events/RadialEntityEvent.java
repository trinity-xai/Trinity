/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class RadialEntityEvent extends Event {

    public static final EventType<RadialEntityEvent> RADIAL_ENTITY_MENU_ITEM = new EventType(ANY, "RADIAL_ENTITY_MENU_ITEM");
    public static final EventType<RadialEntityEvent> RADIAL_ENTITY_INNER_RADIUS = new EventType(ANY, "RADIAL_ENTITY_INNER_RADIUS");
    public static final EventType<RadialEntityEvent> RADIAL_ENTITY_ITEM_FIT_WIDTH = new EventType(ANY, "RADIAL_ENTITY_ITEM_FIT_WIDTH");
    public static final EventType<RadialEntityEvent> RADIAL_ENTITY_MENU_SIZE = new EventType(ANY, "RADIAL_ENTITY_MENU_SIZE");
    public static final EventType<RadialEntityEvent> RADIAL_ENTITY_OFFSET = new EventType(ANY, "RADIAL_ENTITY_OFFSET");
    public static final EventType<RadialEntityEvent> RADIAL_ENTITY_INITIAL_ANGLE = new EventType(ANY, "RADIAL_ENTITY_INITIAL_ANGLE");
    public static final EventType<RadialEntityEvent> RADIAL_ENTITY_STROKE_WIDTH = new EventType(ANY, "RADIAL_ENTITY_STROKE_WIDTH");

    public double newValue;
    public double oldValue;

    public RadialEntityEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public RadialEntityEvent(EventType<? extends Event> arg0, double oldValue, double newValue) {
        this(arg0);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
}
