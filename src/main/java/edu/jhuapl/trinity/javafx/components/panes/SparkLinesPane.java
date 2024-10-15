/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.panes;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;

/**
 * @author Sean Phillips
 */
public class SparkLinesPane extends LitPathPane {
    //    Scene scene;
    public static String CONTROLLER = "/edu/jhuapl/trinity/fxml/SparkLines.fxml";

    public SparkLinesPane(Scene scene, Pane parent) {
        this(scene, parent, CONTROLLER);
    }

    public SparkLinesPane(Scene scene, Pane parent, String controller) {
        super(scene, parent, 500, 600, createContent(controller), "Manifolds ", "", 200.0, 300.0);
    }

//    private static BorderPane createContent() {
//        //make transparent so it doesn't interfere with subnode transparency effects
//        Background transBack = new Background(new BackgroundFill(
//            Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
//        FXMLLoader loader = new FXMLLoader(SparkLinesPane.class.getResource("/edu/jhuapl/neo/fxml/ChartRecorder.fxml"));
//        loader.setLocation(SparkLinesPane.class.getResource("/edu/jhuapl/neo/fxml/ChartRecorder.fxml"));
//        BorderPane sgRoot;
//        try {
//            AnchorPane chartRecorderAnchorPane = loader.load();
//            sgRoot = new BorderPane(chartRecorderAnchorPane);
//            sgRoot.setBackground(transBack);
//        } catch (IOException ex) {
//            sgRoot = new BorderPane(new Text("Unable to load Chart Recorder."));
//            Logger.getLogger(SparkLinesPane.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        return sgRoot;
//    }
//    public SparkLinesPane(Scene scene, Pane parent) {
//        super(scene, parent, 1600, 500, createContent(), "Chart Recorder ", "", 200.0, 300.0);
//        this.scene = scene;
//        // must be set to prevent user from resizing too small.
//        setMinWidth(300);
//        setMinHeight(200);
//        this.scene.getRoot().addEventHandler(CovalentPaneEvent.COVALENT_PANE_CLOSE, e -> {
//            if(e.pathPane == this)
//                parent.getChildren().remove(this);
//        });
//        this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> this.toFront());
//    }
}
