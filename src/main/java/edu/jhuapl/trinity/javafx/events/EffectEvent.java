package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class EffectEvent extends Event {

    public static final EventType<EffectEvent> START_DIGITAL_RAIN = new EventType(ANY, "START_DIGITAL_RAIN");
    public static final EventType<EffectEvent> STOP_DIGITAL_RAIN = new EventType(ANY, "STOP_DIGITAL_RAIN");
    public static final EventType<EffectEvent> START_SCAN_EFFECT = new EventType(ANY, "START_SCAN_EFFECT");
    public static final EventType<EffectEvent> STOP_SCAN_EFFECT = new EventType(ANY, "STOP_SCAN_EFFECT");
    public static final EventType<EffectEvent> ENABLE_EMITTERS = new EventType(ANY, "ENABLE_EMITTERS");
    public static final EventType<EffectEvent> START_EMITTING = new EventType(ANY, "START_EMITTING");
    public static final EventType<EffectEvent> STOP_EMITTING = new EventType(ANY, "STOP_EMITTING");
    public static final EventType<EffectEvent> ENABLE_EMPTY_VISION = new EventType(ANY, "ENABLE_EMPTY_VISION");
    public static final EventType<EffectEvent> OPTICON_USER_ATTENTION = new EventType(ANY, "OPTICON_USER_ATTENTION");
    public static final EventType<EffectEvent> OPTICON_LASER_SWEEP = new EventType(ANY, "OPTICON_LASER_SWEEP");
    public static final EventType<EffectEvent> OPTICON_ENABLE_ORBITING = new EventType(ANY, "OPTICON_ENABLE_ORBITING");

    public String stringId = null;
    public Object object = null;

    public EffectEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public EffectEvent(EventType<? extends Event> arg0, String id) {
        this(arg0);
        this.stringId = id;
    }

    public EffectEvent(EventType<? extends Event> arg0, Object object) {
        this(arg0);
        this.object = object;
    }
}
