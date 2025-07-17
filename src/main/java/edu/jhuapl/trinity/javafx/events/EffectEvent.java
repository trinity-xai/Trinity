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
    public static final EventType<EffectEvent> RADIAL_GRID_ROTATION = new EventType(ANY, "RADIAL_GRID_ROTATION");
    public static final EventType<EffectEvent> SUN_POSITION_ARCWIDTH = new EventType(ANY, "SUN_POSITION_ARCWIDTH");
    public static final EventType<EffectEvent> SUN_POSITION_ARCHEIGHT = new EventType(ANY, "SUN_POSITION_ARCHEIGHT");
    public static final EventType<EffectEvent> SUN_POSITION_VELOCITY = new EventType(ANY, "SUN_POSITION_VELOCITY");
    public static final EventType<EffectEvent> SUN_POSITION_ANIMATING = new EventType(ANY, "SUN_POSITION_ANIMATING");
    public static final EventType<EffectEvent> SUN_ARTIFACT_ENABLED = new EventType(ANY, "SUN_ARTIFACT_ENABLED");
    public static final EventType<EffectEvent> LENSFLARE_ARTIFACT_ENABLED = new EventType(ANY, "LENSFLARE_ARTIFACT_ENABLED");
    public static final EventType<EffectEvent> SUN_POSITION_PATHMODE = new EventType(ANY, "SUN_POSITION_PATHMODE");
    public static final EventType<EffectEvent> PLANETARY_STYLE_CHANGE = new EventType(ANY, "PLANETARY_STYLE_CHANGE");
    public static final EventType<EffectEvent> NEW_PLANETARY_DISC = new EventType<>(ANY, "NEW_PLANETARY_DISC");
    public static final EventType<EffectEvent> ENABLE_GLITCH_EFFECT = new EventType<>(ANY, "ENABLE_GLITCH_EFFECT");
    public static final EventType<EffectEvent> ENABLE_PIXELATE_EFFECT = new EventType<>(ANY, "ENABLE_PIXELATE_EFFECT");
    public static final EventType<EffectEvent> ENABLE_VHSSCANLINE_EFFECT = new EventType<>(ANY, "ENABLE_VHSSCANLINE_EFFECT");

    
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
