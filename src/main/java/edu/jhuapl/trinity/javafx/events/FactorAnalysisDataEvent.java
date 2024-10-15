/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import edu.jhuapl.trinity.data.FactorAnalysisState;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class FactorAnalysisDataEvent extends Event {

    public FactorAnalysisState factorAnalysisState;
    public static final EventType<FactorAnalysisDataEvent> NEW_FACTORANALYSIS_STATE = new EventType(ANY, "NEW_FACTORANALYSIS_STATE");

    public FactorAnalysisDataEvent(FactorAnalysisState t) {
        this(NEW_FACTORANALYSIS_STATE);
        factorAnalysisState = t;
    }

    public FactorAnalysisDataEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public FactorAnalysisDataEvent(FactorAnalysisState arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        factorAnalysisState = arg0;
    }
}
