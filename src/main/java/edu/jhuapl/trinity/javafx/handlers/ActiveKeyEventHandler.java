package edu.jhuapl.trinity.javafx.handlers;

/*-
 * #%L
 * trinity-2024.06.03
 * %%
 * Copyright (C) 2021 - 2024 The Johns Hopkins University Applied Physics Laboratory LLC
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

import java.util.HashSet;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 *
 * @author phillsm1
 */
public class ActiveKeyEventHandler implements EventHandler<KeyEvent> {
    static HashSet<String> currentlyActiveKeys;
    
    public ActiveKeyEventHandler() {
        currentlyActiveKeys = new HashSet<>();
    }

    @Override
    public void handle(KeyEvent event) {
        if (event.getEventType().equals(KeyEvent.KEY_PRESSED)) {
            currentlyActiveKeys.add(event.getCode().toString());
            if(event.getCode() == KeyCode.CONTROL) {
                System.out.println("Control Key is pressed.");
            }            
        }
        if (event.getEventType().equals(KeyEvent.KEY_RELEASED)) {
            currentlyActiveKeys.remove(event.getCode().toString());
            if(event.getCode() == KeyCode.CONTROL) {
                System.out.println("Control Key is released.");
            }        
        }

    }
    public static boolean isPressed(KeyCode keyCode) {
        return currentlyActiveKeys.contains(keyCode.toString());
    }
}
