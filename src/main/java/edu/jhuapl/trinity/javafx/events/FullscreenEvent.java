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
public class FullscreenEvent extends Event {

    public boolean setFullscreen;
    public static final EventType<FullscreenEvent> SET_FULLSCREEN = new EventType(ANY, "SET_FULLSCREEN");

    public FullscreenEvent(boolean setFullscreen) {
        this(SET_FULLSCREEN);
        this.setFullscreen = setFullscreen;
    }

    public FullscreenEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }

    public FullscreenEvent(Object arg0, EventTarget arg1, EventType<? extends Event> arg2) {
        super(arg0, arg1, arg2);
        setFullscreen = (boolean) arg0;
    }
}
