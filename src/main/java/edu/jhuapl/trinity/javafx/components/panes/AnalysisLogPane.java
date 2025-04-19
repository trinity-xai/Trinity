package edu.jhuapl.trinity.javafx.components.panes;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;

/**
 * @author Sean Phillips
 */
public class AnalysisLogPane extends LitPathPane {

    public static String CONTROLLER = "/edu/jhuapl/trinity/fxml/AnalysisLog.fxml";

    public AnalysisLogPane(Scene scene, Pane parent) {
        this(scene, parent, CONTROLLER);
    }

    public AnalysisLogPane(Scene scene, Pane parent, String controller) {
        super(scene, parent, 500, 500, createContent(controller), "Analysis ", "Log", 200.0, 300.0);
    }
}
