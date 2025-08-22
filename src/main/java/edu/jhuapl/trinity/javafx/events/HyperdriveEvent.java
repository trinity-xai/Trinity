package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class HyperdriveEvent extends Event {

    public Object object1;
    public Object object2;

    public static final EventType<HyperdriveEvent> SET_CHUNK_SIZE = new EventType(ANY, "SET_CHUNK_SIZE");
    public static final EventType<HyperdriveEvent> BREAK_ON_NEWLINES = new EventType(ANY, "BREAK_ON_NEWLINES");
    public static final EventType<HyperdriveEvent> ENABLE_JSON_PROCESSING = new EventType(ANY, "ENABLE_JSON_PROCESSING");
    public static final EventType<HyperdriveEvent> ENABLE_CSV_EXPANSION = new EventType(ANY, "ENABLE_CSV_EXPANSION");
    public static final EventType<HyperdriveEvent> AUTOLABEL_FROM_CSVCOLUMN = new EventType(ANY, "AUTOLABEL_FROM_CSVCOLUMN");
    public static final EventType<HyperdriveEvent> SET_CSV_DEFAULTLABELCOLUMN = new EventType(ANY, "SET_CSV_DEFAULTLABELCOLUMN");
    public static final EventType<HyperdriveEvent> NEW_BATCH_IMAGELOAD = new EventType(ANY, "NEW_BATCH_IMAGELOAD");
    public static final EventType<HyperdriveEvent> NEW_BATCH_TEXTLOAD = new EventType(ANY, "NEW_BATCH_TEXTLOAD");

    public HyperdriveEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public HyperdriveEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object1 = arg1;
    }

    public HyperdriveEvent(EventType<? extends Event> arg0, Object arg1, Object arg2) {
        this(arg0);
        object1 = arg1;
        object2 = arg2;
    }
}
