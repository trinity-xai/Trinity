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
