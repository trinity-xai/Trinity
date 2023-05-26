package edu.jhuapl.trinity.javafx.events;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
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
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class ShadowEvent extends Event {

    public Object object;

    public static final EventType<ShadowEvent> FIXED_ORHOGRAPHIC_PROJECTION = new EventType(ANY, "FIXED_TRANSFORM_PROJECTION");
    public static final EventType<ShadowEvent> ROTATING_PERSPECTIVE_PROJECTION = new EventType(ANY, "ROTATING_PERSPECTIVE_PROJECTION");
    public static final EventType<ShadowEvent> SHOW_AXES_LABELS = new EventType(ANY, "SHOW_AXES_LABELS");
    public static final EventType<ShadowEvent> OVERRIDE_DOMAIN_TRANSFORM = new EventType(ANY, "OVERRIDE_DOMAIN_TRANSFORM");
    public static final EventType<ShadowEvent> SET_PANEL_OPACITY = new EventType(ANY, "SET_PANEL_OPACITY");
    public static final EventType<ShadowEvent> SHOW_NEARSIDE_POINTS = new EventType(ANY, "SHOW_NEARSIDE_POINTS");
    public static final EventType<ShadowEvent> ENABLE_CUBE_PROJECTIONS = new EventType(ANY, "ENABLE_CUBE_PROJECTIONS");
    public static final EventType<ShadowEvent> SET_GRIDLINES_VISIBLE = new EventType(ANY, "SET_GRIDLINES_VISIBLE");
    public static final EventType<ShadowEvent> SET_FRAME_VISIBLE = new EventType(ANY, "SET_FRAME_VISIBLE");
    public static final EventType<ShadowEvent> SET_CONTROLPOINTS_VISIBLE = new EventType(ANY, "SET_CONTROLPOINTS_VISIBLE");
    public static final EventType<ShadowEvent> SET_CUBEWALLS_VISIBLE = new EventType(ANY, "SET_CUBEWALLS_VISIBLE");
    public static final EventType<ShadowEvent> SET_CUBE_VISIBLE = new EventType(ANY, "SET_CUBE_VISIBLE");
    public static final EventType<ShadowEvent> SET_POINT_SCALING = new EventType(ANY, "SET_POINT_SCALING");
    public static final EventType<ShadowEvent> SET_DOMAIN_MINIMUM = new EventType(ANY, "SET_DOMAIN_MINIMUM");
    public static final EventType<ShadowEvent> SET_DOMAIN_MAXIMUM = new EventType(ANY, "SET_DOMAIN_MAXIMUM");
    public static final EventType<ShadowEvent> SET_POINT_OPACITY = new EventType(ANY, "SET_POINT_OPACITY");

    public ShadowEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public ShadowEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object = arg1;
    }

    public ShadowEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
