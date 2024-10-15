/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.handlers;

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
