/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.javafx.components.panes.JoystickPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class JoystickPaneTest extends Application {
    StackPane centerStack;
    Pane pathPane;
    JoystickPane joystickPane;
    ContextMenu cm = new ContextMenu();

    @Override
    public void start(Stage stage) {
        pathPane = new Pane(); //transparent layer that just holds floating panes
        pathPane.setPickOnBounds(false);
        
        centerStack = new StackPane(pathPane);
        centerStack.setBackground(Background.EMPTY);
        BorderPane bpOilSpill = new BorderPane(centerStack);
        bpOilSpill.setBackground(Background.EMPTY);

        CheckMenuItem toggleJoystickMenuItem = new CheckMenuItem("Enable Joystick Pane");
        toggleJoystickMenuItem.setOnAction(e -> {
            if(toggleJoystickMenuItem.isSelected()) {
                pathPane.getChildren().add(joystickPane);
                joystickPane.slideInPane();
            } else
                pathPane.getChildren().remove(joystickPane);
        });
        CheckMenuItem toggleTopControlsMenuItem = new CheckMenuItem("Joystick Controls");
        toggleTopControlsMenuItem.setOnAction(e -> {
            joystickPane.toggleTopControls(toggleTopControlsMenuItem.isSelected());
        });
        CheckMenuItem toggleButtonsMenuItem = new CheckMenuItem("Enable Fire and Thrust Buttons");
        toggleButtonsMenuItem.setOnAction(e -> {
            joystickPane.toggleBottomControls(toggleButtonsMenuItem.isSelected());
        });

        cm.getItems().addAll(
            toggleJoystickMenuItem,
            toggleTopControlsMenuItem,
            toggleButtonsMenuItem
        );
        cm.setAutoFix(true);
        cm.setAutoHide(true);
        cm.setHideOnEscape(true);

        centerStack.setOnMouseClicked((MouseEvent e) -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (null != cm)
                    if (!cm.isShowing())
                        cm.show(centerStack.getParent(), e.getScreenX(), e.getScreenY());
                    else
                        cm.hide();
                e.consume();
            }
        });

        Scene scene = new Scene(bpOilSpill, Color.BLACK);
        joystickPane = new JoystickPane(scene, centerStack);
        joystickPane.fireButton.setOnAction(e -> System.out.println("fire"));
        joystickPane.thrustButton.setOnAction(e -> System.out.println("thrust"));
        joystickPane.angleproperty.subscribe(c -> {
//            projectileSystem.playerShip.mouseDragged(null,
//                joystickPane.mouseDeltaX,
//                joystickPane.mouseDeltaY);
        });

        joystickPane.valueproperty.subscribe(c -> {
//            projectileSystem.playerShip.mouseDragged(null,
//                joystickPane.mouseDeltaX,
//                joystickPane.mouseDeltaY);
        });
        //Make everything pretty
        String CSS = this.getClass().getResource("/edu/jhuapl/trinity/css/styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        CSS = this.getClass().getResource("/edu/jhuapl/trinity/css/covalent.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        stage.setTitle("Joystick tester");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
