package edu.jhuapl.trinity.javafx.components.panes;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;

/**
 * @author Sean Phillips
 */
public class ManifoldControlPane extends LitPathPane {

    public static String CONTROLLER = "/edu/jhuapl/trinity/fxml/ManifoldControl.fxml";

    public ManifoldControlPane(Scene scene, Pane parent) {
        this(scene, parent, CONTROLLER);
    }

    public ManifoldControlPane(Scene scene, Pane parent, String controller) {
        super(scene, parent, 500, 600, createContent(controller), "Manifolds ", "", 200.0, 300.0);
    }
}
