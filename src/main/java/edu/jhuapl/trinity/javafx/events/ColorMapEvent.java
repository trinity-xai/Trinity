/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class ColorMapEvent extends Event {

    public static final EventType<ColorMapEvent> ONE_COLOR_SPECTRUM = new EventType(ANY, "ONE_COLOR_SPECTRUM");
    public static final EventType<ColorMapEvent> TWO_COLOR_SPECTRUM = new EventType(ANY, "TWO_COLOR_SPECTRUM");
    public static final EventType<ColorMapEvent> HSB_WHEEL_SPECTRUM = new EventType(ANY, "HSB_WHEEL_SPECTRUM");
    public static final EventType<ColorMapEvent> PRESET_COLOR_PALETTE = new EventType(ANY, "PRESET_COLOR_PALETTE");
    public static final EventType<ColorMapEvent> COLOR_DOMAIN_CHANGE = new EventType(ANY, "COLOR_DOMAIN_CHANGE");

    public enum COLOR_MAP {
        ONE_COLOR_SPECTRUM, TWO_COLOR_SPECTRUM, HSB_WHEEL_SPECTRUM, PRESET_COLOR_PALETTE
    }

    public Object object1 = null;
    public Object object2 = null;

    public ColorMapEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public ColorMapEvent(EventType<? extends Event> arg0, Object object1) {
        this(arg0);
        this.object1 = object1;
    }

    public ColorMapEvent(EventType<? extends Event> arg0, Object object1, Object object2) {
        this(arg0);
        this.object1 = object1;
        this.object2 = object2;
    }
}
