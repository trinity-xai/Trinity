/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import edu.jhuapl.trinity.messages.ZeroMQSubscriberConfig;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class ZeroMQEvent extends Event {

    public ZeroMQSubscriberConfig subscriberConfig;

    public static final EventType<ZeroMQEvent> ZEROMQ_ESTABLISH_CONNECTION = new EventType(ANY, "ZEROMQ_ESTABLISH_CONNECTION");
    public static final EventType<ZeroMQEvent> ZEROMQ_TERMINATE_CONNECTION = new EventType(ANY, "ZEROMQ_TERMINATE_CONNECTION");
    public static final EventType<ZeroMQEvent> ZEROMQ_START_PROCESSING = new EventType(ANY, "ZEROMQ_START_PROCESSING");
    public static final EventType<ZeroMQEvent> ZEROMQ_STOP_PROCESSING = new EventType(ANY, "ZEROMQ_STOP_PROCESSING");


    public ZeroMQEvent(EventType<? extends Event> arg0, ZeroMQSubscriberConfig t) {
        super(arg0);
        subscriberConfig = t;
    }

    public ZeroMQEvent(ZeroMQSubscriberConfig arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        subscriberConfig = arg0;
    }
}
