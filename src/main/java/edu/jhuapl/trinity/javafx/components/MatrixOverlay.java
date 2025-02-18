/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.javafx.components.panes.CanvasOverlayPane;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 */
public class MatrixOverlay {
    private static final Logger LOG = LoggerFactory.getLogger(MatrixOverlay.class);
    CanvasOverlayPane canvasOverlayPane;
    MatrixEffect neo;
    Canvas canvas;
    public Runnable matrixOn;
    public Runnable matrixOff;
    public static final BooleanProperty on = new SimpleBooleanProperty(true);

    public MatrixOverlay(Scene scene, Pane desktopPaneParent) {
        addMatrixEffect(scene, desktopPaneParent);
    }

    /**
     * This will add a shortcut + N to the scene to toggle and display and hide
     * the matrix effect on the stackpane.
     *
     * @param scene
     * @param desktopPaneParent A stackpane to resize the canvas overlay pane (child).
     */
    public void addMatrixEffect(Scene scene, Pane desktopPaneParent) {
        // attach hotkey Alt N
        // stuff to run
        canvas = new Canvas();
        neo = new MatrixEffect(canvas);
        // canvas overlay only works on StackPanes desktopPane's Parent (borderpane center).
        canvasOverlayPane = new CanvasOverlayPane(canvas, false, false);
        desktopPaneParent.getChildren().add(canvasOverlayPane);

        matrixOn = () -> {
            LOG.info("matrix on");
            canvasOverlayPane.setVisible(true);
            neo.start();
            canvasOverlayPane.toFront();
        };
        matrixOff = () -> {
            LOG.info("matrix off");
            neo.stop();
            canvasOverlayPane.setVisible(false);
            canvasOverlayPane.toBack();
        };
        // when hotkey is a toggle. N for Neo
        attachToggle(scene, KeyCode.N, KeyCombination.ALT_DOWN, matrixOn, matrixOff);
//        canvas.setOnSwipeUp(e -> {
//            on.set(false);
//            matrixOff.run();
//            //e.consume(); // <-- stops passing the event to next node
//        });
//        canvas.setOnSwipeDown(e -> {
//            on.set(true);
//            matrixOn.run();
//            e.consume(); // <-- stops passing the event to next node
//        });

    }

    /**
     * Adds an event handler to listen for key combos global to the scene.
     * Two actions are assigned doWork and undoWork.
     * This is assigned a key combo as a toggle.
     *
     * @param scene
     * @param keyCode
     * @param modifier
     * @param doWork
     * @param undoWork
     * @return
     */
    public static EventHandler<KeyEvent> attachToggle(Scene scene, KeyCode keyCode,
                                                      KeyCombination.Modifier modifier, Runnable doWork, Runnable undoWork) {

//        final BooleanProperty on = new SimpleBooleanProperty(true);
        KeyCombination keyComb = new KeyCodeCombination(keyCode, modifier);
        EventHandler<KeyEvent> eventHandler = (keyEvent -> {
            if (App.isMatrixEnabled())
                if (keyComb.match(keyEvent)) {
                    if (on.get()) {
                        doWork.run();
                    } else {
                        undoWork.run();
                    }
                    on.set(!on.get());
                    keyEvent.consume(); // <-- stops passing the event to next node
                }
        });

        scene.addEventFilter(KeyEvent.KEY_PRESSED, eventHandler);
        return eventHandler;
    }
}
