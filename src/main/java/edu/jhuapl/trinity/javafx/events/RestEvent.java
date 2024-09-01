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
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * @author Sean Phillips
 */
public class RestEvent extends Event {

    public Object object;

    public static final EventType<RestEvent> START_RESTSERVER_THREAD = new EventType(ANY, "START_RESTSERVER_THREAD");
    public static final EventType<RestEvent> TERMINATE_RESTSERVER_THREAD = new EventType(ANY, "TERMINATE_RESTSERVER_THREAD");
    public static final EventType<RestEvent> START_RESTSERVER_PROCESSING = new EventType(ANY, "START_RESTSERVER_PROCESSING");
    public static final EventType<RestEvent> STOP_RESTSERVER_PROCESSING = new EventType(ANY, "STOP_RESTSERVER_PROCESSING");


    public RestEvent(EventType<? extends Event> arg0, Object t) {
        super(arg0);
        object = t;
    }

    public RestEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
