package edu.jhuapl.trinity.javafx.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.input.MouseEvent;

/**
 * @author Sean Phillips
 */
public class SelectTrajectory3DEvent extends Event {

    public Object eventObject;
    public MouseEvent mouseEvent;
    public static final EventType<SelectTrajectory3DEvent> SELECT_TRAJECTORY_3D = new EventType(ANY, "SELECT_TRAJECTORY_3D");

    public SelectTrajectory3DEvent(Object t) {
        this(SELECT_TRAJECTORY_3D);
        eventObject = t;
    }

    public SelectTrajectory3DEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public SelectTrajectory3DEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        eventObject = arg0;
    }
}
