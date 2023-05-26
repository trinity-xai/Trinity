package edu.jhuapl.trinity.javafx.components.panes;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
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

import edu.jhuapl.trinity.javafx.events.VisibilityEvent;
import edu.jhuapl.trinity.javafx.events.VisibilityObject;
import javafx.animation.FadeTransition;
import javafx.scene.Cursor;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * @author Sean Phillips
 */
public class FactorAnalysisVisibilityPane extends StackPane {
    double controlSize = 15;
    double BORDER = 2.0;
    public double width;
    public double height;
    private double panePosX;
    private double paneBoxPosY;
    private double paneBoxOldX;
    private double paneBoxOldY;
    private double paneBoxDeltaX;
    private double paneBoxDeltaY;

    private FadeTransition ft = new FadeTransition(Duration.seconds(10), this);
    BorderPane bp;
    CheckBox day1CheckBox;
    CheckBox day8CheckBox;
    CheckBox day19CheckBox;
    CheckBox day36CheckBox;
    CheckBox day43amCheckBox;
    CheckBox day43pmCheckBox;

    CheckBox restGestureCheckBox;
    CheckBox openCheckBox;
    CheckBox pinchCheckBox;

    public FactorAnalysisVisibilityPane(double width, double height) {

        this.width = width;
        this.height = height;
        ft.setAutoReverse(false);
        ft.setCycleCount(0);
        setOnMouseEntered(e -> {
            requestFocus();
            ft.stop();
            setOpacity(0.9);
        });
        setOnMouseExited(e -> {
            ft.stop();
            ft.setToValue(0.2);
            ft.setDelay(Duration.seconds(10));
            ft.playFromStart();
        });
        setOnMousePressed(me -> {
            setCursor(Cursor.MOVE);
            panePosX = me.getSceneX();
            paneBoxPosY = me.getSceneY();
            paneBoxOldX = me.getSceneX();
            paneBoxOldY = me.getSceneY();
            me.consume();
        });
        setOnMouseDragged(me -> {
            setCursor(Cursor.MOVE);
            paneBoxOldX = panePosX;
            paneBoxOldY = paneBoxPosY;
            panePosX = me.getSceneX();
            paneBoxPosY = me.getSceneY();
            paneBoxDeltaX = (panePosX - paneBoxOldX);
            paneBoxDeltaY = (paneBoxPosY - paneBoxOldY);
            setTranslateX(getTranslateX() + paneBoxDeltaX);
            setTranslateY(getTranslateY() + paneBoxDeltaY);
            me.consume();
        });
        setOnMouseReleased(me -> setCursor(Cursor.DEFAULT));

        setPrefSize(width, height + controlSize);
        setMaxSize(width, height + controlSize);
//        Color ALICEBLUETRANS = new Color(0.9411765f, 0.972549f, 1.0f, 0.3);
//        setBorder(new Border(
//            new BorderStroke(ALICEBLUETRANS,
//                             BorderStrokeStyle.SOLID,
//                             new CornerRadii(25,25,0,0,false),
//                             new BorderWidths(3)
//                             )));

        setBackground(Background.EMPTY);

        day1CheckBox = new CheckBox("Day 1 Trials");
        day1CheckBox.setSelected(true);
        day1CheckBox.selectedProperty().addListener(e -> updateVisibility());

        day8CheckBox = new CheckBox("Day 8 Trials");
        day8CheckBox.setSelected(true);
        day8CheckBox.selectedProperty().addListener(e -> updateVisibility());

        day19CheckBox = new CheckBox("Day 19 Trials");
        day19CheckBox.setSelected(true);
        day19CheckBox.selectedProperty().addListener(e -> updateVisibility());

        day36CheckBox = new CheckBox("Day 36 Trials");
        day36CheckBox.setSelected(true);
        day36CheckBox.selectedProperty().addListener(e -> updateVisibility());

        day43amCheckBox = new CheckBox("Day 43am Trials");
        day43amCheckBox.setSelected(true);
        day43amCheckBox.selectedProperty().addListener(e -> updateVisibility());

        day43pmCheckBox = new CheckBox("Day 43pm Trials");
        day43pmCheckBox.setSelected(true);
        day43pmCheckBox.selectedProperty().addListener(e -> updateVisibility());

        restGestureCheckBox = new CheckBox("Rest Gestures");
        restGestureCheckBox.setSelected(true);
        restGestureCheckBox.selectedProperty().addListener(e -> updateVisibility());

        openCheckBox = new CheckBox("Open Gestures");
        openCheckBox.setSelected(true);
        openCheckBox.selectedProperty().addListener(e -> updateVisibility());

        pinchCheckBox = new CheckBox("Pinch Gestures");
        pinchCheckBox.setSelected(true);
        pinchCheckBox.selectedProperty().addListener(e -> updateVisibility());

        VBox vbox = new VBox(10,
            new VBox(10, day1CheckBox, day8CheckBox, day19CheckBox,
                day36CheckBox, day43amCheckBox, day43pmCheckBox),
            new VBox(10, restGestureCheckBox, openCheckBox, pinchCheckBox)
        );
        bp = new BorderPane(vbox);
        bp.setMinSize(width, height);
        getChildren().addAll(bp); //add everything to the Pane
    }

    private void updateVisibility() {
        var vo = new VisibilityObject();
        vo.day1Group = day1CheckBox.isSelected();
        vo.day8Group = day8CheckBox.isSelected();
        vo.day19Group = day19CheckBox.isSelected();
        vo.day36Group = day36CheckBox.isSelected();
        vo.day43amGroup = day43amCheckBox.isSelected();
        vo.day43pmGroup = day43pmCheckBox.isSelected();

        vo.restGestureGroup = restGestureCheckBox.isSelected();
        vo.pinchGestureGroup = pinchCheckBox.isSelected();
        vo.openGestureGroup = openCheckBox.isSelected();
        bp.getScene().getRoot().fireEvent(new VisibilityEvent(
            VisibilityEvent.VISIBILITY_CHANGED, vo));
    }

}
