/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.components.panes;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;

/**
 * @author Sean Phillips
 */
public class ConfigurationPane extends LitPathPane {
    public static String CONTROLLER = "/edu/jhuapl/trinity/fxml/Configuration.fxml";

    public ConfigurationPane(Scene scene, Pane parent) {
        this(scene, parent, CONTROLLER);
    }

    public ConfigurationPane(Scene scene, Pane parent, String controller) {
        super(scene, parent, 500, 600, createContent(controller), "Configuration ", "", 200.0, 300.0);
    }
}
