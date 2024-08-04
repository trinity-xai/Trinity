package edu.jhuapl.trinity.javafx.events;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2024 Sean Phillips
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
public class ShapleyEvent extends Event {

    public Object object;
    public Object object2;

    public static final EventType<ShapleyEvent> EXPORT_SHAPLEY_COLLECTION = new EventType(ANY, "EXPORT_SHAPLEY_COLLECTION");
    public static final EventType<ShapleyEvent> NEW_SHAPLEY_COLLECTION = new EventType(ANY, "NEW_SHAPLEY_COLLECTION");
    public static final EventType<ShapleyEvent> NEW_SHAPLEY_VECTOR = new EventType(ANY, "NEW_SHAPLEY_VECTOR");

    public ShapleyEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public ShapleyEvent(EventType<? extends Event> arg0, Object arg1) {
        this(arg0);
        object = arg1;
    }

    public ShapleyEvent(EventType<? extends Event> arg0, Object arg1, Object arg2) {
        this(arg0);
        object = arg1;
        object2 = arg2;
    }

    public ShapleyEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object = arg0;
    }
}
