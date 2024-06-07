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
public class HitEvent extends Event {

    public Object object1 = null;
    public Object object2 = null;

    public static final EventType<HitEvent> RAY_INTERSECTS_BOX = new EventType(ANY, "RAY_INTERSECTS_BOX");
    public static final EventType<HitEvent> PROJECTILE_HIT_BOX = new EventType(ANY, "PROJECTILE_HIT_BOX");
    public static final EventType<HitEvent> PROJECTILE_HIT_BRICK = new EventType(ANY, "PROJECTILE_HIT_BRICK");
    public static final EventType<HitEvent> PROJECTILE_HIT_SHIELD = new EventType(ANY, "PROJECTILE_HIT_SHIELD");
    public static final EventType<HitEvent> PROJECTILE_HIT_CHARACTER = new EventType(ANY, "PROJECTILE_HIT_CHARACTER");
    public static final EventType<HitEvent> SHAPE_HIT_BOX = new EventType(ANY, "SHAPE_HIT_BOX");
    public static final EventType<HitEvent> PROJECTILE_HIT_SHAPE = new EventType(ANY, "PROJECTILE_HIT_SHAPE");

    public HitEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public HitEvent(EventType<? extends Event> arg0, Object arg1, Object arg2) {
        this(arg0);
        object1 = arg1;
        object2 = arg2;
    }

    public HitEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        object1 = arg0;
    }
}
