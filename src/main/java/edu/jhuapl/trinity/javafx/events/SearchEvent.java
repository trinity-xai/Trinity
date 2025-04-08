/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class SearchEvent extends Event {

    public Object eventObject;

    public static final EventType<SearchEvent> FILTER_BY_PROBABILITY = new EventType(ANY, "FILTER_BY_PROBABILITY");
    public static final EventType<SearchEvent> FILTER_BY_SCORE = new EventType(ANY, "FILTER_BY_SCORE");
    public static final EventType<SearchEvent> FILTER_BY_TERM = new EventType(ANY, "FILTER_BY_TERM");
    public static final EventType<SearchEvent> CLEAR_ALL_FILTERS = new EventType(ANY, "CLEAR_ALL_FILTERS");
    public static final EventType<SearchEvent> FIND_BY_QUERY = new EventType(ANY, "FIND_BY_QUERY");
    public static final EventType<SearchEvent> QUERY_EMBEDDINGS_RESPONSE = new EventType(ANY, "QUERY_EMBEDDINGS_RESPONSE");

    public SearchEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public SearchEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        eventObject = arg1;
    }

    public SearchEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        eventObject = arg0;
    }
}
