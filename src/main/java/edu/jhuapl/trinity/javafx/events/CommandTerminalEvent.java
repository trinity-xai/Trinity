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
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * @author Sean Phillips
 */
public class CommandTerminalEvent extends Event {

    public String text;
    public Font font;
    public Color color;
    public Node node;
    public long timeMS;
    public static final EventType<CommandTerminalEvent> FOLLOWUP = new EventType(ANY, "FOLLOWUP");
    public static final EventType<CommandTerminalEvent> NOTIFICATION = new EventType(ANY, "NOTIFICATION");
    public static final EventType<CommandTerminalEvent> ALERT = new EventType(ANY, "ALERT");
    public static final EventType<CommandTerminalEvent> FADE_OUT = new EventType(ANY, "FADE_OUT");

    public CommandTerminalEvent(String text) {
        this(FOLLOWUP);
        this.text = text;
    }

    public CommandTerminalEvent(String text, Font font, Color color) {
        this(NOTIFICATION);
        this.text = text;
        this.font = font;
        this.color = color;
    }

    public CommandTerminalEvent(String text, Font font, Color color, Node node) {
        this(ALERT);
        this.text = text;
        this.font = font;
        this.color = color;
        this.node = node;
    }

    public CommandTerminalEvent(long fadeOutMS) {
        this(FADE_OUT);
        this.timeMS = fadeOutMS;
    }

    public CommandTerminalEvent(EventType<? extends Event> arg0) {
        super(arg0);
    }
}
