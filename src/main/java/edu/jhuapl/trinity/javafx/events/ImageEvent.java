/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class ImageEvent extends Event {

    public Object object;

    public static final EventType<ImageEvent> NEW_TEXTURE_SURFACE = new EventType(ANY, "NEW_TEXTURE_SURFACE");
    public static final EventType<ImageEvent> NEW_VECTORMASK_COLLECTION = new EventType(ANY, "NEW_VECTORMASK_COLLECTION");
    public static final EventType<ImageEvent> NEW_COCO_ANNOTATION = new EventType(ANY, "NEW_COCO_ANNOTATION");

    public ImageEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public ImageEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object = arg1;
    }

    public ImageEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
