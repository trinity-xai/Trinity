/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.components.panes;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;

/**
 * @author Sean Phillips
 */
public class AboutPane extends LitPathPane {
    public static String CONTROLLER = "/edu/jhuapl/trinity/fxml/About.fxml";

    public AboutPane(Scene scene, Pane parent) {
        this(scene, parent, CONTROLLER);
    }

    public AboutPane(Scene scene, Pane parent, String controller) {
        super(scene, parent, 600, 800, createContent(controller), "About ", "", 200.0, 300.0);
    }
}
