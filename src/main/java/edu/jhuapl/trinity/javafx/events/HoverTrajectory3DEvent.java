/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.input.MouseEvent;

/**
 * @author Sean Phillips
 */
public class HoverTrajectory3DEvent extends Event {

    public Object eventObject;
    public MouseEvent mouseEvent;
    public static final EventType<HoverTrajectory3DEvent> HOVER_TRAJECTORY_3D = new EventType(ANY, "HOVER_TRAJECTORY_3D");

    public HoverTrajectory3DEvent(Object t) {
        this(HOVER_TRAJECTORY_3D);
        eventObject = t;
    }

    public HoverTrajectory3DEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public HoverTrajectory3DEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        eventObject = arg0;
    }
}
