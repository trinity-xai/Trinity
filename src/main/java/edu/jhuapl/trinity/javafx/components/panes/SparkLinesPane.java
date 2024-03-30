package edu.jhuapl.trinity.javafx.components.panes;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2020 - 2024 Johns Hopkins University Applied Physics Laboratory
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

import javafx.scene.Scene;
import javafx.scene.layout.Pane;

/**
 *
 * @author phillsm1
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
