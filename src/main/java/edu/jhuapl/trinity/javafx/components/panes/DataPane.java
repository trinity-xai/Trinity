/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.components.panes;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;

/**
 * @author Sean Phillips
 */
public class DataPane extends LitPathPane {
    public static String CONTROLLER = "/edu/jhuapl/trinity/fxml/Data.fxml";

    public DataPane(Scene scene, Pane parent) {
        this(scene, parent, CONTROLLER);
    }

    public DataPane(Scene scene, Pane parent, String controller) {
        super(scene, parent, 400, 200, createContent(controller), "Data ", "ZeroMQ", 200.0, 300.0);
    }
}
