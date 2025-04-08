/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import edu.jhuapl.trinity.data.messages.bci.ChannelFrame;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class ChannelFrameDataEvent extends Event {

    public ChannelFrame channelFrame;
    public static final EventType<ChannelFrameDataEvent> NEW_CHANNEL_FRAME = new EventType(ANY, "NEW_CHANNEL_FRAME");

    public ChannelFrameDataEvent(ChannelFrame t) {
        this(NEW_CHANNEL_FRAME);
        channelFrame = t;
    }

    public ChannelFrameDataEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public ChannelFrameDataEvent(ChannelFrame arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        channelFrame = arg0;
    }
}
