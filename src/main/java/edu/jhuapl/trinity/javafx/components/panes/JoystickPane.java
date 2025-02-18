/* Copyright (C) 2021 - 2024 Sean Phillips */

package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.components.Joystick;
import edu.jhuapl.trinity.javafx.components.LockState;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * @author Sean Phillips
 */
public class JoystickPane extends LitPathPane {

    public Joystick joystick;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    public double mouseDeltaX;
    public double mouseDeltaY;

    public SimpleDoubleProperty angleproperty = new SimpleDoubleProperty(0.0);
    public SimpleDoubleProperty valueproperty = new SimpleDoubleProperty(0.0);
    public Button fireButton, thrustButton;
    BorderPane bpOilSpillNeverForget;
    HBox topHBox;
    HBox bottomButtonHBox;

    private static BorderPane createContent() {
        //make transparent so it doesn't interfere with subnode transparency effects
        Background transBack = new Background(new BackgroundFill(
            Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
        Joystick joystick = new Joystick();
        //joystick.setStickyMode(true);
        joystick.setStepSize(5);
        //joystick.setStepButtonsVisible(false);
        joystick.setDurationMillis(1000);
        joystick.setLockState(LockState.UNLOCKED);

        BorderPane ctRoot = new BorderPane();
        ctRoot.setCenter(joystick);
        ctRoot.setBackground(transBack);

        return ctRoot;
    }

    public JoystickPane(Scene scene, Pane parent) {
        super(scene, parent, 600, 730, createContent(), "Joystick Controls ", "", 200.0, 300.0);
        this.scene = scene;
        // must be set to prevent user from resizing too small.
        setMinWidth(300);
        setMinHeight(200);
        bpOilSpillNeverForget = (BorderPane) this.contentPane;
        joystick = (Joystick) bpOilSpillNeverForget.getCenter();
        angleproperty.bind(joystick.angle);
        valueproperty.bind(joystick.value);
        angleproperty.addListener(c -> updateTransforms());
        valueproperty.addListener(c -> updateTransforms());

        fireButton = new Button("FIRE!!");
        fireButton.setPrefWidth(150);
        thrustButton = new Button("THRUST!!");
        thrustButton.setPrefWidth(150);

        bottomButtonHBox = new HBox(50, fireButton, thrustButton);
        bottomButtonHBox.setAlignment(Pos.CENTER);
        bpOilSpillNeverForget.setBottom(bottomButtonHBox);

        RadioButton unlocked = new RadioButton("Unlocked");
        unlocked.setTextFill(Color.WHITE);
        unlocked.setSelected(true);
        unlocked.setOnAction(e -> joystick.setLockState(LockState.UNLOCKED));
        RadioButton lockedX = new RadioButton("x-axis locked");
        lockedX.setTextFill(Color.WHITE);
        lockedX.setOnAction(e -> joystick.setLockState(LockState.X_LOCKED));
        lockedX.setSelected(false);
        RadioButton lockedY = new RadioButton("y-axis locked");
        lockedY.setTextFill(Color.WHITE);
        lockedY.setOnAction(e -> joystick.setLockState(LockState.Y_LOCKED));
        lockedY.setSelected(false);

        ToggleGroup lockStateGroup = new ToggleGroup();
        lockStateGroup.getToggles().setAll(unlocked, lockedX, lockedY);

        CheckBox stepButtonsVisible = new CheckBox("Step buttons");
        stepButtonsVisible.setTextFill(Color.WHITE);
        stepButtonsVisible.setSelected(true);
        stepButtonsVisible.setOnAction(e -> joystick.setStepButtonsVisible(stepButtonsVisible.isSelected()));

        CheckBox stickyMode = new CheckBox("Sticky mode");
        stickyMode.setTextFill(Color.WHITE);
        stickyMode.setOnAction(e -> joystick.setStickyMode(stickyMode.isSelected()));

        topHBox = new HBox(15, unlocked, lockedX, lockedY,
            stepButtonsVisible, stickyMode);
        topHBox.setAlignment(Pos.BOTTOM_CENTER);
        mainTitleArea.getChildren().add(topHBox);
        topHBox.setTranslateY(30); //not sure why I have to do this...
        topHBox.prefWidthProperty().bind(mainTitleArea.widthProperty().subtract(10));
    }

    public void toggleTopControls(boolean add) {
        if (add)
            if (mainTitleArea.getChildren().contains(topHBox))
                mainTitleArea.getChildren().add(topHBox);
            else
                mainTitleArea.getChildren().remove(topHBox);
    }

    public void toggleBottomControls(boolean add) {
        if (add)
            bpOilSpillNeverForget.setBottom(bottomButtonHBox);
        else
            bpOilSpillNeverForget.setBottom(null);
    }

    private void updateTransforms() {
        double scalar = 1000.0;

        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = scalar * valueproperty.get() * Math.cos(Math.toRadians(angleproperty.getValue()));
        mousePosY = scalar * valueproperty.get() * Math.sin(Math.toRadians(angleproperty.getValue()));
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);
    }
}
