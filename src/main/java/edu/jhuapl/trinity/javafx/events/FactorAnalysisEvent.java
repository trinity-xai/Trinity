/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class FactorAnalysisEvent extends Event {

    public Object object;
    public static final EventType<FactorAnalysisEvent> SCALE_UPDATED = new EventType(ANY, "SCALE_UPDATED");
    public static final EventType<FactorAnalysisEvent> XFACTOR_SELECTION = new EventType(ANY, "XFACTOR_SELECTION");
    public static final EventType<FactorAnalysisEvent> YFACTOR_SELECTION = new EventType(ANY, "YFACTOR_SELECTION");
    public static final EventType<FactorAnalysisEvent> ZFACTOR_SELECTION = new EventType(ANY, "ZFACTOR_SELECTION");
    public static final EventType<FactorAnalysisEvent> SURFACE_XFACTOR_VECTOR = new EventType(ANY, "SURFACE_XFACTOR_VECTOR");
    public static final EventType<FactorAnalysisEvent> SURFACE_ZFACTOR_VECTOR = new EventType(ANY, "SURFACE_ZFACTOR_VECTOR");

    public FactorAnalysisEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public FactorAnalysisEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object = arg1;
    }

    public FactorAnalysisEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
