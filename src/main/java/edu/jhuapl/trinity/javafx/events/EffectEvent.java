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
public class EffectEvent extends Event {

    public static final EventType<EffectEvent> START_SCAN_EFFECT = new EventType(ANY, "START_SCAN_EFFECT");
    public static final EventType<EffectEvent> STOP_SCAN_EFFECT = new EventType(ANY, "STOP_SCAN_EFFECT");
    public static final EventType<EffectEvent> ENABLE_EMITTERS = new EventType(ANY, "ENABLE_EMITTERS");
    public static final EventType<EffectEvent> START_EMITTING = new EventType(ANY, "START_EMITTING");
    public static final EventType<EffectEvent> STOP_EMITTING = new EventType(ANY, "STOP_EMITTING");


    public String stringId = null;
    public Object object = null;

    public EffectEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public EffectEvent(EventType<? extends Event> arg0, String id) {
        this(arg0);
        this.stringId = id;
    }

    public EffectEvent(EventType<? extends Event> arg0, Object object) {
        this(arg0);
        this.object = object;
    }
}
