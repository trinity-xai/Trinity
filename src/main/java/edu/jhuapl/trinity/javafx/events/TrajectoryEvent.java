package edu.jhuapl.trinity.javafx.events;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import javafx.event.Event;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class TrajectoryEvent extends Event {

    public Object eventObject;
    public Object eventObject2;
        
    public static final EventType<TrajectoryEvent> CLEAR_ALL_TRAJECTORIES = new EventType(ANY, "CLEAR_ALL_TRAJECTORIES");
    public static final EventType<TrajectoryEvent> TRAJECTORY_VISIBILITY_CHANGED = new EventType(ANY, "TRAJECTORY_VISIBILITY_CHANGED");
    public static final EventType<TrajectoryEvent> TRAJECTORY_COLOR_CHANGED = new EventType(ANY, "TRAJECTORY_COLOR_CHANGED");
    public static final EventType<TrajectoryEvent> NEW_TRAJECTORY_OBJECT = new EventType(ANY, "NEW_TRAJECTORY_OBJECT");
    public static final EventType<TrajectoryEvent> TRAJECTORY_OBJECT_SELECTED = new EventType(ANY, "TRAJECTORY_OBJECT_SELECTED");
    public static final EventType<TrajectoryEvent> SHOW_TRAJECTORY_TRACKER = new EventType(ANY, "SHOW_TRAJECTORY_TRACKER");
    public static final EventType<TrajectoryEvent> TIMELINE_SHOW_TRAJECTORY = new EventType(ANY, "TIMELINE_SHOW_TRAJECTORY");
    public static final EventType<TrajectoryEvent> TRAJECTORY_TAIL_SIZE = new EventType(ANY, "TRAJECTORY_TAIL_SIZE");
    public static final EventType<TrajectoryEvent> TIMELINE_SHOW_CALLOUT = new EventType(ANY, "TIMELINE_SHOW_CALLOUT");

    public TrajectoryEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public TrajectoryEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        this.eventObject = arg1;
    }
    public TrajectoryEvent(EventType<? extends Event> arg0, Object eventObject, Object eventObject2) {
        this(arg0);
        this.eventObject = eventObject;
        this.eventObject2 = eventObject2;
    }
}
