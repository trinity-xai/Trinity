package edu.jhuapl.trinity.javafx.handlers;

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

import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Sean Phillips
 */
public class ExpandTouchGestureHandler implements EventHandler<TouchEvent> {

    ConcurrentHashMap<Integer, TouchPoint> activeTouchPoints;
    ConcurrentHashMap<Integer, Circle> touchPointNodes;
    Pane pane;
    AnimationTimer animationTimer;
    boolean isDirty = false;
    TouchPoint touchPoint;
    int maxTouchPoints;

    public ExpandTouchGestureHandler(Pane pane, int maxTouchPoints) {
        this.pane = pane;
        this.maxTouchPoints = maxTouchPoints;
        activeTouchPoints = new ConcurrentHashMap<>();
        touchPointNodes = new ConcurrentHashMap<>();
        for (int i = 1; i <= maxTouchPoints; i++) {
            Circle circle = new Circle(40, Color.SKYBLUE.deriveColor(1, 1, 1, 0.5));
            circle.setStroke(Color.ANTIQUEWHITE);
            circle.setManaged(false);
            circle.setTranslateX(-40);
            circle.setTranslateY(-40);
            this.pane.getChildren().add(circle);
            circle.setVisible(false);
            circle.setMouseTransparent(true);
            touchPointNodes.put(i, circle);
        }

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isDirty) {
                    activeTouchPoints.forEach((id, t) -> {
                        //System.out.print(id + " ");
                        if (id <= maxTouchPoints) {
                            touchPointNodes.get(id).relocate(t.getX(), t.getY());
                        }
                    });
                    //System.out.print("\n");
                    isDirty = false;
                }
            }

            ;
        };
        animationTimer.start();
    }

    private void deactivateById(int id) {
        activeTouchPoints.remove(id);
        Node node = touchPointNodes.get(id);
        if (null != node)
            node.setVisible(false);
    }

    @Override
    public void handle(TouchEvent event) {
        if (event.getEventType() == TouchEvent.TOUCH_MOVED) {
            isDirty = true;
            event.getTouchPoints().parallelStream().forEach(t -> {
                activeTouchPoints.put((Integer) t.getId(), t);
            });
        } else if (event.getEventType() == TouchEvent.TOUCH_PRESSED) {
            isDirty = true;
            touchPoint = event.getTouchPoint();
            if (touchPoint.getId() <= maxTouchPoints) {
                activeTouchPoints.put(touchPoint.getId(), touchPoint);
                touchPointNodes.get(touchPoint.getId()).setVisible(true);
            }
        } else if (event.getEventType() == TouchEvent.TOUCH_RELEASED) {
            deactivateById(event.getTouchPoint().getId());

            activeTouchPoints.forEach((id, t) -> {
                if (!event.getTouchPoints().contains(t)) {
                    activeTouchPoints.remove(id);
                }
            });
        }

////        if (event.getTouchCount() != 2) {
////            // Ignore if this is not a two-finger touch
////            return;
////        }
//
//        TouchPoint main = event.getTouchPoint();
//        TouchPoint other = event.getTouchPoints().get(1);
//        if (other.getId() == main.getId()) {
//            // Ignore if the second finger is in the ball and
//            // the first finger is anywhere else
//            return;
//        }
////
////        if (other.getState() != TouchPoint.State.PRESSED
////                || other.belongsTo(Ball.this)
////                || !(other.getTarget() instanceof Rectangle)) {
////            // Jump only if the second finger was just
////            // pressed in a rectangle
////            return;
////        }
////
////        // Jump now
////        setTranslateX(other.getSceneX() - touchx);
////        setTranslateY(other.getSceneY() - touchy);
////
//        // Grab the destination touch point, which is now inside
//        // the ball, so that jumping can continue without
//        // releasing the finger
//        other.grab();
//
//        // The original touch point is no longer of interest so
//        // call ungrab() to release the target
//        main.ungrab();

        event.consume();
    }

}
