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
 * @author Luis C. Puche
 */
public class ApplicationEvent extends Event {

    public Object object = null;

    public static final EventType<ApplicationEvent> SET_IMAGERY_BASEPATH = new EventType(ANY, "SET_IMAGERY_BASEPATH");
    public static final EventType<ApplicationEvent> SHOW_ABOUT = new EventType(ANY, "SHOW_ABOUT");
    public static final EventType<ApplicationEvent> SHUTDOWN = new EventType(ANY, "SHUTDOWN");
    public static final EventType<ApplicationEvent> RESTORE_PANES = new EventType(ANY, "RESTORE_PANES");
    public static final EventType<ApplicationEvent> SHOW_DATA = new EventType(ANY, "SHOW_DATA");
    public static final EventType<ApplicationEvent> SHOW_TEXT_CONSOLE = new EventType(ANY, "SHOW_TEXT_CONSOLE");
    public static final EventType<ApplicationEvent> SHOW_WAVEFORM_PANE = new EventType(ANY, "SHOW_WAVEFORM_PANE");
    public static final EventType<ApplicationEvent> SHOW_HYPERSPACE = new EventType(ANY, "SHOW_HYPERSPACE");
    public static final EventType<ApplicationEvent> SHOW_HYPERSURFACE = new EventType(ANY, "SHOW_HYPERSURFACE");
    public static final EventType<ApplicationEvent> SHOW_PROJECTIONS = new EventType(ANY, "SHOW_PROJECTIONS");
    public static final EventType<ApplicationEvent> SHOW_BUSY_INDICATOR = new EventType(ANY, "SHOW_BUSY_INDICATOR");
    public static final EventType<ApplicationEvent> HIDE_BUSY_INDICATOR = new EventType(ANY, "HIDE_BUSY_INDICATOR");
    public static final EventType<ApplicationEvent> UPDATE_BUSY_INDICATOR = new EventType(ANY, "PROGRESS_BUSY_INDICATOR");
    public static final EventType<ApplicationEvent> SHOW_SHAPE3D_CONTROLS = new EventType(ANY, "SHOW_SHAPE3D_CONTROLS");

    public ApplicationEvent(EventType<? extends Event> eventType, Object object) {
        this(eventType);
        this.object = object;
    }

    public ApplicationEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }
}
