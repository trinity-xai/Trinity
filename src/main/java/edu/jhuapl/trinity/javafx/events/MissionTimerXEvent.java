/*
 * Copyright (c) 2021 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import edu.jhuapl.trinity.javafx.components.timeline.Item;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

public class MissionTimerXEvent extends Event {
    public static final EventType<MissionTimerXEvent> ALL = new EventType<>("ALL");
    public static final EventType<MissionTimerXEvent> TRIGGERED = new EventType<>(ALL, "TRIGGERED");
    public static final EventType<MissionTimerXEvent> NEW_ITEM_INDEX = new EventType<>(ALL, "NEW_ITEM_INDEX");

    private final Item item;
    private Integer index = null;

    // ******************** Constructors **************************************
    public MissionTimerXEvent(final Item item, final EventType<? extends Event> type) {
        super(type);
        this.item = item;
    }

    public MissionTimerXEvent(final Item item, final Object src, final EventTarget target, final EventType<? extends Event> type) {
        super(src, target, type);
        this.item = item;
    }

    public MissionTimerXEvent(final Item item, Integer index, final EventType<? extends Event> type) {
        super(type);
        this.item = item;
        this.index = index;
    }

    // ******************** Methods *******************************************
    public Item getItem() {
        return item;
    }

    public int getIndex() {
        return index;
    }
}
