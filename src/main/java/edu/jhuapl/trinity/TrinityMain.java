/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity;

import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class exists because of the current state of JavaFX in Java11+.
 * Please see https://stackoverflow.com/questions/52569724/javafx-11-create-a-jar-file-with-gradle/52571719#52571719
 * for solution origin.
 */
public class TrinityMain {
    private static final Logger LOG = LoggerFactory.getLogger(TrinityMain.class);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        LOG.info("Entering Trinity main...");
        Application.launch(App.class, args);
    }
}
