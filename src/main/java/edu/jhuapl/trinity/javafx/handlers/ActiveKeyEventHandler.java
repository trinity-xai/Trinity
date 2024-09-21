package edu.jhuapl.trinity.javafx.handlers;

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

import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

/**
 * @author Sean Phillips
 */
public class ActiveKeyEventHandler implements EventHandler<KeyEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(ActiveKeyEventHandler.class);
    static HashSet<String> currentlyActiveKeys;

    public ActiveKeyEventHandler() {
        currentlyActiveKeys = new HashSet<>();
    }

    @Override
    public void handle(KeyEvent event) {
        if (event.getEventType().equals(KeyEvent.KEY_PRESSED)) {
            currentlyActiveKeys.add(event.getCode().toString());
            if (event.getCode() == KeyCode.CONTROL) {
                LOG.info("Control Key is pressed.");
            }
        }
        if (event.getEventType().equals(KeyEvent.KEY_RELEASED)) {
            currentlyActiveKeys.remove(event.getCode().toString());
            if (event.getCode() == KeyCode.CONTROL) {
                LOG.info("Control Key is released.");
            }
        }

    }

    public static boolean isPressed(KeyCode keyCode) {
        return currentlyActiveKeys.contains(keyCode.toString());
    }
}
