package edu.jhuapl.trinity.javafx.components;

/* Original Source code for Joystick License
 * Copyright (c) 2020 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import edu.jhuapl.trinity.App;
import java.util.List;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.LongProperty;
import javafx.beans.property.LongPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

/**
 * User: hansolo
 * Date: 31.05.20
 * Time: 08:18
 * Modified by Birdasaur
 */
public class Joystick extends Region {
    private static final double                    PREFERRED_WIDTH  = 500;
    private static final double                    PREFERRED_HEIGHT = 500;
    private static final double                    MINIMUM_WIDTH    = 50;
    private static final double                    MINIMUM_HEIGHT   = 50;
    private static final double                    MAXIMUM_WIDTH    = 1024;
    private static final double                    MAXIMUM_HEIGHT   = 1024;
    private static final double                    HALF_PI          = Math.PI / 2.0;
    private static final double                    MAX_STEP_SIZE    = 10;
    private              double                    size;
    private              double                    center;
    private              double                    width;
    private              double                    height;
    private              Canvas                    background;
    private              GraphicsContext           ctx;
    private              Circle                    touchIndicator;
    private              Circle                    touchPoint;
    private              Arc                       touchN;
    private              Arc                       touchNW;
    private              Arc                       touchW;
    private              Arc                       touchSW;
    private              Arc                       touchS;
    private              Arc                       touchSE;
    private              Arc                       touchE;
    private              Arc                       touchNE;
    private              Pane                      pane;
    private              LockState                 _lockState;
    private              ObjectProperty<LockState> lockState;
    private              boolean                   _stickyMode;
    private              BooleanProperty           stickyMode;
    private              boolean                   _animated;
    private              BooleanProperty           animated;
    private              long                      _durationMillis;
    private              LongProperty              durationMillis;
    private              double                    _stepSize;
    private              DoubleProperty            stepSize;
    private              boolean                   _stepButtonsVisible;
    private              BooleanProperty           stepButtonsVisible;
    private              Color                     _inactiveColor;
    private              ObjectProperty<Color>     inactiveColor;
    private              Color                     _activeColor;
    private              ObjectProperty<Color>     activeColor;
    private              Color                     _lockedColor;
    private              ObjectProperty<Color>     lockedColor;
    private              Color                     transclucentActiveColor;
    private              boolean                   _touched;
    private              BooleanProperty           touched;
    private              DoubleProperty            x;
    private              DoubleProperty            y;
    public              DoubleProperty            value;
public              DoubleProperty            angle;
    private              double                    offsetX;
    private              double                    offsetY;
    private              Timeline                  timeline;
    private              EventHandler<MouseEvent>  mouseHandler;
    private              EventHandler<TouchEvent>  touchHandler;

    private List<Color> buttonColors = null;
    private List<String> buttonStrings = null;
    private List<String> buttonText = null;
    private List<VectorAction> vectorActions;

    private boolean touchNLatch = false;
    private boolean touchNWLatch = false;
    private boolean touchWLatch = false;
    private boolean touchSWLatch = false;
    private boolean touchSLatch = false;
    private boolean touchSELatch = false;
    private boolean touchELatch = false;
    private boolean touchNELatch = false;
    
//    private Label touchNText;
//    private Label touchNWText;
//    private Label touchWText;
//    private Label touchSWText;
//    private Label touchSText;
//    private Label touchSEText;
//    private Label touchEText;
//    private Label touchNEText;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    public double mouseDeltaX;
    public double mouseDeltaY;
    
    public SimpleObjectProperty actionTaken = new SimpleObjectProperty(false);
    private boolean touchPointLatch = false;
    
    // ******************** Constructors **************************************
    public Joystick() {
        center                  = PREFERRED_WIDTH * 0.5;
        _lockState              = LockState.UNLOCKED;
        _stickyMode             = false;
        _animated               = true;
        _durationMillis         = 100;
        _stepSize               = 0.01;
        _stepButtonsVisible     = true;
        _inactiveColor          = Color.web("#506691");
        _activeColor            = Color.web("#CFF9FF");
        _lockedColor            = Color.web("#B36B6B");
        transclucentActiveColor = Color.color(_activeColor.getRed(), _activeColor.getGreen(), _activeColor.getBlue(), 0.25);
        _touched                = false;
        x                       = new DoublePropertyBase(0.0) {
            @Override protected void invalidated() {}
            @Override public Object getBean() { return Joystick.this; }
            @Override public String getName() { return "valueX"; }
        };
        y                       = new DoublePropertyBase(0.0) {
            @Override protected void invalidated() {}
            @Override public Object getBean() { return Joystick.this; }
            @Override public String getName() { return "valueY"; }
        };
        value                   = new DoublePropertyBase(0) {
            @Override protected void invalidated() { redraw(); }
            @Override public Object getBean() { return Joystick.this; }
            @Override public String getName() { return "value"; }
        };
        angle                   = new DoublePropertyBase(0) {
            @Override protected void invalidated() {
            }
            @Override public Object getBean() { return Joystick.this; }
            @Override public String getName() { return "angle"; }
        };
        offsetX                 = 0;
        offsetY                 = 0;
        timeline                = new Timeline();
        mouseHandler            = e -> {
            Object                          src  = e.getSource();
            EventType<? extends MouseEvent> type = e.getEventType();
            double x   = clamp(size * 0.15, size * 0.85, e.getX());
            double y   = clamp(size * 0.15, size * 0.85, e.getY());

            if (LockState.X_LOCKED == getLockState()) {
                y = center;
            } else if (LockState.Y_LOCKED == getLockState()) {
                x = center;
            }

            double dx  = x - center;
            double dy  = -(y - center);
            double rad = Math.atan2(dy, dx) + HALF_PI;
            double phi = Math.toDegrees(rad - Math.PI);
            if (phi < 0) { phi += 360.0; }
            double r    = Math.sqrt(dx * dx + dy * dy);
            double maxR = size * 0.35;
            if (r > maxR) {
                x = -Math.cos(rad + HALF_PI) * maxR + center;
                y = Math.sin(rad + HALF_PI) * maxR + center;
                r = maxR;
            }
            setX(-Math.cos(rad + HALF_PI));
            setY(-Math.sin(rad + HALF_PI));
            if (src.equals(touchPoint)) {
                if(!touchPointLatch) {
                    touchPointLatch = true; //let this be the only time we collect the start
                    Platform.runLater(() -> {
                        Scene scene = App.getAppScene();
                        //@TODO SMP fire event?
//                        scene.getRoot().fireEvent(
//                            new ScenarioEvent(ScenarioEvent.MULTIPLEX_NATIVE_START, System.currentTimeMillis())); 
                    });
                }
                setAngle(phi);
                if (MouseEvent.MOUSE_PRESSED.equals(type)) {
                    setTouched(true);
                    touchPoint.setFill(getActiveColor());
                    touchIndicator.setStroke(getActiveColor());
                } else if (MouseEvent.MOUSE_DRAGGED.equals(type)) {
                    touchPoint.setCenterX(x);
                    touchPoint.setCenterY(y);
                    setValue(r / maxR);
                    drawBackground();
                } else if (MouseEvent.MOUSE_RELEASED.equals(type)) {
                    setTouched(false);
                    touchPoint.setFill(Color.TRANSPARENT);
                    touchIndicator.setStroke(getInactiveColor());
                    reset();
                } else if (MouseEvent.MOUSE_CLICKED.equals(type)) {
                    if (isStickyMode() && e.getClickCount() == 2) {
                        touchPoint.setCenterX(0.5 * size);
                        touchPoint.setCenterY(0.5 * size);
                        value.set(0);
                        angle.set(0);
                    }
                }
            } else if (src.equals(touchN)) {
                if (MouseEvent.MOUSE_PRESSED.equals(type)) {
                    touchN.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX(), touchPoint.getCenterY() - getStepSize(), 0);
                } else if (MouseEvent.MOUSE_RELEASED.equals(type)) {
                    touchN.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchNW)) {
                if (MouseEvent.MOUSE_PRESSED.equals(type)) {
                    touchNW.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX() - getStepSize(), touchPoint.getCenterY() - getStepSize(), 45);
                } else if (MouseEvent.MOUSE_RELEASED.equals(type)) {
                    touchNW.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchW)) {
                if (MouseEvent.MOUSE_PRESSED.equals(type)) {
                    touchW.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX() - getStepSize(), touchPoint.getCenterY(), 90);
                } else if (MouseEvent.MOUSE_RELEASED.equals(type)) {
                    touchW.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchSW)) {
                if (MouseEvent.MOUSE_PRESSED.equals(type)) {
                    touchSW.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX() - getStepSize(), touchPoint.getCenterY() + getStepSize(), 135);
                } else if (MouseEvent.MOUSE_RELEASED.equals(type)) {
                    touchSW.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchS)) {
                if (MouseEvent.MOUSE_PRESSED.equals(type)) {
                    touchS.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX(), touchPoint.getCenterY() + getStepSize(), 180);
                } else if (MouseEvent.MOUSE_RELEASED.equals(type)) {
                    touchS.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchSE)) {
                if (MouseEvent.MOUSE_PRESSED.equals(type)) {
                    touchSE.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX() + getStepSize(), touchPoint.getCenterY() + getStepSize(), 225);
                } else if (MouseEvent.MOUSE_RELEASED.equals(type)) {
                    touchSE.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchE)) {
                if (MouseEvent.MOUSE_PRESSED.equals(type)) {
                    touchE.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX() + getStepSize(), touchPoint.getCenterY(), 270);
                } else if (MouseEvent.MOUSE_RELEASED.equals(type)) {
                    touchE.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchNE)) {
                if (MouseEvent.MOUSE_PRESSED.equals(type)) {
                    touchNE.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX() + getStepSize(), touchPoint.getCenterY() - getStepSize(), 315);
                } else if (MouseEvent.MOUSE_RELEASED.equals(type)) {
                    touchNE.setStroke(getInactiveColor());
                }
            }
        };
        touchHandler            = e -> {
            Object                          src  = e.getSource();
            EventType<? extends TouchEvent> type = e.getEventType();
            double x   = clamp(size * 0.15, size * 0.85, e.getTouchPoint().getX());
            double y   = clamp(size * 0.15, size * 0.85, e.getTouchPoint().getY());

            if (LockState.X_LOCKED == getLockState()) {
                y = center;
            } else if (LockState.Y_LOCKED == getLockState()) {
                x = center;
            }

            double dx  = x - center;
            double dy  = -(y - center);
            double rad = Math.atan2(dy, dx) + HALF_PI;
            double phi = Math.toDegrees(rad - Math.PI);
            if (phi < 0) { phi += 360.0; }
            setAngle(phi);
            double r    = Math.sqrt(dx * dx + dy * dy);
            double maxR = size * 0.35;
            if (r > maxR) {
                x = -Math.cos(rad + HALF_PI) * maxR + center;
                y = Math.sin(rad + HALF_PI) * maxR + center;
                r = maxR;
            }
            setX(-Math.cos(rad + HALF_PI));
            setY(-Math.sin(rad + HALF_PI));

            if (src.equals(touchPoint)) {
                if(!touchPointLatch) {
                    touchPointLatch = true; //let this be the only time we collect the start
                    Platform.runLater(() -> {
                        Scene scene = App.getAppScene();
                        //@TODO SMP Fire event
//                        scene.getRoot().fireEvent(
//                            new ScenarioEvent(ScenarioEvent.MULTIPLEX_NATIVE_START, System.currentTimeMillis())); 
                    });
                }                
                if (TouchEvent.TOUCH_PRESSED.equals(type)) {
                    setTouched(true);
                    if (isStickyMode() && e.getTouchCount() == 2) {
                        touchPoint.setCenterX(0.5 * size);
                        touchPoint.setCenterY(0.5 * size);
                        value.set(0);
                        angle.set(0);
                    }
                    touchPoint.setFill(getActiveColor());
                    touchIndicator.setStroke(getActiveColor());
                } else if (TouchEvent.TOUCH_MOVED.equals(type)) {
                    touchPoint.setCenterX(x);
                    touchPoint.setCenterY(y);
                    setValue(r / maxR);
                    drawBackground();
                } else if (TouchEvent.TOUCH_RELEASED.equals(type)) {
                    setTouched(false);
                    touchPoint.setFill(Color.TRANSPARENT);
                    touchIndicator.setStroke(getInactiveColor());
                    reset();
                }
            } else if (src.equals(touchN)) {
                if (TouchEvent.TOUCH_PRESSED.equals(type)) {
                    touchN.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX(), touchPoint.getCenterY() - getStepSize(), 0);
                } else if (TouchEvent.TOUCH_RELEASED.equals(type)) {
                    touchN.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchNW)) {
                if (TouchEvent.TOUCH_PRESSED.equals(type)) {
                    touchNW.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX() - getStepSize(), touchPoint.getCenterY() - getStepSize(), 45);
                } else if (TouchEvent.TOUCH_RELEASED.equals(type)) {
                    touchNW.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchW)) {
                if (TouchEvent.TOUCH_PRESSED.equals(type)) {
                    touchW.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX() - getStepSize(), touchPoint.getCenterY(), 90);
                } else if (TouchEvent.TOUCH_RELEASED.equals(type)) {
                    touchW.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchSW)) {
                if (TouchEvent.TOUCH_PRESSED.equals(type)) {
                    touchSW.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX() - getStepSize(), touchPoint.getCenterY() + getStepSize(), 135);
                } else if (TouchEvent.TOUCH_RELEASED.equals(type)) {
                    touchSW.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchS)) {
                if (TouchEvent.TOUCH_PRESSED.equals(type)) {
                    touchS.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX(), touchPoint.getCenterY() + getStepSize(), 180);
                } else if (TouchEvent.TOUCH_RELEASED.equals(type)) {
                    touchS.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchSE)) {
                if (TouchEvent.TOUCH_PRESSED.equals(type)) {
                    touchSE.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX() + getStepSize(), touchPoint.getCenterY() + getStepSize(), 225);
                } else if (TouchEvent.TOUCH_RELEASED.equals(type)) {
                    touchSE.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchE)) {
                if (TouchEvent.TOUCH_PRESSED.equals(type)) {
                    touchE.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX() + getStepSize(), touchPoint.getCenterY(), 270);
                } else if (TouchEvent.TOUCH_RELEASED.equals(type)) {
                    touchE.setStroke(getInactiveColor());
                }
            } else if (src.equals(touchNE)) {
                if (TouchEvent.TOUCH_PRESSED.equals(type)) {
                    touchNE.setStroke(getActiveColor());
                    setXY(touchPoint.getCenterX() + getStepSize(), touchPoint.getCenterY() - getStepSize(), 315);
                } else if (TouchEvent.TOUCH_RELEASED.equals(type)) {
                    touchNE.setStroke(getInactiveColor());
                }
            }
        };

        getStylesheets().add(Joystick.class.getResource("/edu/jhuapl/trinity/css/touchjoystick.css").toExternalForm());

        initGraphics();
        registerListeners();
        value.addListener(e -> {
            updateTransforms();
//            System.out.println(value.get());
        });
        angle.addListener(e -> updateTransforms());
    }
    private void updateTransforms() {
        double scalar = 100.0;
//        double modifier = 1.0;
//        double modifierFactor = 0.1;  //@TODO SMP connect to sensitivity property
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = scalar*value.get()*Math.cos(angle.getValue());
        mousePosY = scalar*value.get()*Math.sin(angle.getValue());
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);
//        double yChange = (((mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
//        double xChange = (((-mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
//        addRotation(yChange, Rotate.Y_AXIS);
//        addRotation(xChange, Rotate.X_AXIS);
        
    }
    private void testStatus() {
        System.out.println(value.get() + ", " + angle.get());
        if(null != vectorActions) {
            System.out.println("Testing value at " + value.get());
            if(value.get() >= 1) {
                Platform.runLater(()-> {
                    Scene scene = App.getAppScene();
                    VectorAction va = null;
                    System.out.println("Testing angle at " + angle.getValue().intValue());
                    boolean sendEvent = false;
                    switch(angle.getValue().intValue()) {
                        case 0: va = vectorActions.get(0); 
                            if(!touchNLatch)
                                sendEvent = true;
                            touchNLatch = true;
                            break;
                        case 45: va = vectorActions.get(1); 
                            if(!touchNWLatch)
                                sendEvent = true;
                            touchNWLatch = true;
                            break;
                        case 90: va = vectorActions.get(2); 
                            if(!touchWLatch)
                                sendEvent = true;
                            touchWLatch = true;
                            break;
                        case 135: va = vectorActions.get(3); 
                            if(!touchSWLatch)
                                sendEvent = true;
                            touchSWLatch = true;
                            break;
                        case 180: va = vectorActions.get(4); 
                            if(!touchSLatch)
                                sendEvent = true;
                            touchSLatch = true;
                            break;
                        case 225: va = vectorActions.get(5); 
                            if(!touchSELatch)
                                sendEvent = true;
                            touchSELatch = true;
                            break;
                        case 270: va = vectorActions.get(6); 
                            if(!touchELatch)
                                sendEvent = true;
                            touchELatch = true;
                            break;
                        case 315: va = vectorActions.get(7); 
                            if(!touchNELatch)
                                sendEvent = true;
                            touchNELatch = true;
                            break;
                    }
                    if(null != va) {
                        //@DEBUG SMP useful print
                        //System.out.println("Angle and Value REACHED!");    
                        if(sendEvent) {    
                            actionTaken.set(va);
                            //TODO SMP Fire event?
//                            scene.getRoot().fireEvent(
//                                new ScenarioEvent(ScenarioEvent.MULTIPLEX_NATIVE_END, System.currentTimeMillis())); 
                        }
                    }
                });
            }
        }
    }

    // ******************** Initialization ************************************
    private void initGraphics() {
        if (Double.compare(getPrefWidth(), 0.0) <= 0 || Double.compare(getPrefHeight(), 0.0) <= 0 || Double.compare(getWidth(), 0.0) <= 0 ||
            Double.compare(getHeight(), 0.0) <= 0) {
            if (getPrefWidth() > 0 && getPrefHeight() > 0) {
                setPrefSize(getPrefWidth(), getPrefHeight());
            } else {
                setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        getStyleClass().add("touch-joystick");

        background = new Canvas(0.7 * PREFERRED_WIDTH, 0.7 * PREFERRED_HEIGHT);
        background.setMouseTransparent(true);
        ctx        = background.getGraphicsContext2D();

        touchN  = createArc(0);
        touchNW = createArc(45);
        touchW  = createArc(90);
        touchSW = createArc(135);
        touchS  = createArc(180);
        touchSE = createArc(225);
        touchE  = createArc(270);
        touchNE = createArc(315);

        touchIndicator = new Circle();
        touchIndicator.setFill(Color.TRANSPARENT);
        touchIndicator.setStroke(getInactiveColor());
        touchIndicator.setMouseTransparent(true);

        touchPoint = new Circle();
        touchPoint.setFill(Color.TRANSPARENT);
        touchPoint.setStroke(getActiveColor());

//        touchNText = new Label("North");
//        touchNWText = new Label("North-West");
//        touchWText = new Label("West");
//        touchSWText = new Label("South-West");
//        touchSText = new Label("South");
//        touchSEText = new Label("South-East");
//        touchEText = new Label("East");
//        touchNEText = new Label("North-East");

        pane = new Pane(background, touchN, touchNW, touchW, touchSW, touchS, touchSE, touchE, touchNE, touchIndicator, touchPoint);
//        pane.getChildren().addAll(touchNText, touchNWText, touchWText, touchSWText, touchSText, touchSEText, touchEText, touchNEText  );
        getChildren().setAll(pane);
    }

    private void registerListeners() {
        widthProperty().addListener(o -> resize());
        heightProperty().addListener(o -> resize());
        touchPoint.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseHandler);
        touchPoint.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseHandler);
        touchPoint.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseHandler);
        touchPoint.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseHandler);
        touchPoint.addEventHandler(TouchEvent.TOUCH_PRESSED, touchHandler);
        touchPoint.addEventHandler(TouchEvent.TOUCH_MOVED, touchHandler);
        touchPoint.addEventHandler(TouchEvent.TOUCH_RELEASED, touchHandler);
        timeline.setOnFinished(e -> resetTouchButtons());
    }


    // ******************** Methods *******************************************
    @Override protected double computeMinWidth(final double HEIGHT) { return MINIMUM_WIDTH; }
    @Override protected double computeMinHeight(final double WIDTH) { return MINIMUM_HEIGHT; }
    @Override protected double computePrefWidth(final double HEIGHT) { return super.computePrefWidth(HEIGHT); }
    @Override protected double computePrefHeight(final double WIDTH) { return super.computePrefHeight(WIDTH); }
    @Override protected double computeMaxWidth(final double HEIGHT) { return MAXIMUM_WIDTH; }
    @Override protected double computeMaxHeight(final double WIDTH) { return MAXIMUM_HEIGHT; }


    public LockState getLockState() { return null == lockState ? _lockState : lockState.get(); }
    public void setLockState(final LockState lockState) {
        if (null == this.lockState) {
            _lockState = lockState;
            switch(lockState) {
                case X_LOCKED:
                    touchN.setDisable(true);
                    touchNW.setDisable(true);
                    touchW.setDisable(false);
                    touchSW.setDisable(true);
                    touchS.setDisable(true);
                    touchSE.setDisable(true);
                    touchE.setDisable(false);
                    touchNE.setDisable(true);
                    break;
                case Y_LOCKED:
                    touchN.setDisable(false);
                    touchNW.setDisable(true);
                    touchW.setDisable(true);
                    touchSW.setDisable(true);
                    touchS.setDisable(false);
                    touchSE.setDisable(true);
                    touchE.setDisable(true);
                    touchNE.setDisable(true);
                    break;
                case UNLOCKED:
                default:
                    touchN.setDisable(false);
                    touchNW.setDisable(false);
                    touchW.setDisable(false);
                    touchSW.setDisable(false);
                    touchS.setDisable(false);
                    touchSE.setDisable(false);
                    touchE.setDisable(false);
                    touchNE.setDisable(false);
                    break;
            }
            redraw();
        } else {
            this.lockState.set(lockState);
        }
    }
    public ObjectProperty<LockState> lockStateProperty() {
        if (null == lockState) {
            lockState = new ObjectPropertyBase<>(_lockState) {
                @Override protected void invalidated() {
                    switch(get()) {
                        case X_LOCKED:
                            touchN.setDisable(false);
                            touchNW.setDisable(true);
                            touchW.setDisable(true);
                            touchSW.setDisable(true);
                            touchS.setDisable(false);
                            touchSE.setDisable(true);
                            touchE.setDisable(true);
                            touchNE.setDisable(true);
                            break;
                        case Y_LOCKED:
                            touchN.setDisable(false);
                            touchNW.setDisable(false);
                            touchW.setDisable(false);
                            touchSW.setDisable(false);
                            touchS.setDisable(false);
                            touchSE.setDisable(false);
                            touchE.setDisable(false);
                            touchNE.setDisable(false);
                            break;
                        case UNLOCKED:
                        default:
                            touchN.setDisable(false);
                            touchNW.setDisable(false);
                            touchW.setDisable(false);
                            touchSW.setDisable(false);
                            touchS.setDisable(false);
                            touchSE.setDisable(false);
                            touchE.setDisable(false);
                            touchNE.setDisable(false);
                            break;
                    }
                    redraw();
                }
                @Override public Object getBean() { return Joystick.this; }
                @Override public String getName() { return "lockState"; }
            };
            _lockState = null;
        }
        return lockState;
    }
    
    public boolean isStickyMode() { return null == stickyMode ? _stickyMode : stickyMode.get(); }
    public void setStickyMode(final boolean stickyMode) {
        if (null == this.stickyMode) {
            _stickyMode = stickyMode;
        } else {
            this.stickyMode.set(stickyMode);
        }
    }
    public BooleanProperty stickyModeProperty() {
        if (null == stickyMode) {
            stickyMode = new BooleanPropertyBase(_stickyMode) {
                @Override public Object getBean() { return Joystick.this; }
                @Override public String getName() { return "stickyMode"; }
            };
        }
        return stickyMode;
    }
    
    public boolean isAnimated() { return null == animated ? _animated : animated.get(); }
    public void setAnimated(final boolean animated) {
        if (null == this.animated) {
            _animated = animated;
        } else {
            this.animated.set(animated);
        }
    }
    public BooleanProperty animatedProperty() {
        if (null == animated) {
            animated = new BooleanPropertyBase(_animated) {
                @Override public Object getBean() { return Joystick.this; }
                @Override public String getName() { return "animated"; }
            };
        }
        return animated;
    }

    public long getDurationMillis() { return null == durationMillis ? _durationMillis : durationMillis.get(); }
    public void setDurationMillis(final long durationMillis) {
        if (null == this.durationMillis) {
            _durationMillis = clamp(10, 1000, durationMillis);
        } else {
            this.durationMillis.set(durationMillis);
        }
    }
    public LongProperty durationMillisProperty() {
        if (null == durationMillis) {
            durationMillis = new LongPropertyBase(_durationMillis) {
                @Override protected void invalidated() { set(clamp(10, 1000, get())); }
                @Override public Object getBean() { return Joystick.this; }
                @Override public String getName() { return "durationMillis"; }
            };
        }
        return durationMillis;
    }

    public double getStepSize() { return null == stepSize ? _stepSize : stepSize.get(); }
    public void setStepSize(final double stepSize) {
        if (null == this.stepSize) {
            _stepSize = clamp(0.001, MAX_STEP_SIZE, stepSize);
        } else {
            this.stepSize.set(stepSize);
        }
    }
    public DoubleProperty stepSizeProperty() {
        if (null == stepSize) {
            stepSize = new DoublePropertyBase(_stepSize) {
                @Override protected void invalidated() { set(clamp(0.001, MAX_STEP_SIZE, get())); }
                @Override public Object getBean() { return Joystick.this; }
                @Override public String getName() { return "stepSizeX"; }
            };
        }
        return stepSize;
    }

    public boolean getStepButtonsVisible() { return null == stepButtonsVisible ? _stepButtonsVisible : stepButtonsVisible.get(); }
    public void setStepButtonsVisible(final boolean stepButtonsVisible) {
        if (null == this.stepButtonsVisible) {
            _stepButtonsVisible = stepButtonsVisible;
            touchN.setVisible(stepButtonsVisible);
            touchNW.setVisible(stepButtonsVisible);
            touchW.setVisible(stepButtonsVisible);
            touchSW.setVisible(stepButtonsVisible);
            touchS.setVisible(stepButtonsVisible);
            touchSE.setVisible(stepButtonsVisible);
            touchE.setVisible(stepButtonsVisible);
            touchNE.setVisible(stepButtonsVisible);
            redraw();
        } else {
            this.stepButtonsVisible.set(stepButtonsVisible);
        }
    }
    public BooleanProperty stepButtonsVisibleProperty() {
        if (null == stepButtonsVisible) {
            stepButtonsVisible = new BooleanPropertyBase(_stepButtonsVisible) {
                @Override protected void invalidated() {
                    touchN.setVisible(get());
                    touchNW.setVisible(get());
                    touchW.setVisible(get());
                    touchSW.setVisible(get());
                    touchS.setVisible(get());
                    touchSE.setVisible(get());
                    touchE.setVisible(get());
                    touchNE.setVisible(get());
                    redraw();
                }
                @Override public Object getBean() { return Joystick.this; }
                @Override public String getName() { return "stepButtonsVisible"; }
            };
        }
        return stepButtonsVisible;
    }
    
    public Color getInactiveColor() { return null == inactiveColor ? _inactiveColor : inactiveColor.get(); }
    public void setInactiveColor(final Color inactiveColor) {
        if (null == this.inactiveColor) {
            _inactiveColor = inactiveColor;
            redraw();
        } else {
            this.inactiveColor.set(inactiveColor);
        }
    }
    public ObjectProperty<Color> inactiveColorProperty() {
        if (null == inactiveColor) {
            inactiveColor = new ObjectPropertyBase<Color>(_inactiveColor) {
                @Override protected void invalidated() { redraw(); }
                @Override public Object getBean() { return Joystick.this; }
                @Override public String getName() { return "inactiveColor"; }
            };
            _inactiveColor = null;
        }
        return inactiveColor;
    }

    public Color getActiveColor() { return null == activeColor ? _activeColor : activeColor.get(); }
    public void setActiveColor(final Color activeColor) {
        if (null == this.activeColor) {
            _activeColor            = activeColor;
            transclucentActiveColor = Color.color(_activeColor.getRed(), _activeColor.getGreen(), _activeColor.getBlue(), 0.25);
            redraw();
        } else {
            this.activeColor.set(activeColor);
        }
    }
    public ObjectProperty<Color> activeColorProperty() {
        if (null == activeColor) {
            activeColor = new ObjectPropertyBase<Color>(_activeColor) {
                @Override protected void invalidated() {
                    transclucentActiveColor = Color.color(get().getRed(), get().getGreen(), get().getBlue(), 0.25);
                    redraw();
                }
                @Override public Object getBean() { return Joystick.this; }
                @Override public String getName() { return "activeColor"; }
            };
            _activeColor = null;
        }
        return activeColor;
    }

    public Color getLockedColor() { return null == lockedColor ? _lockedColor : lockedColor.get(); }
    public void setLockedColor(final Color lockedColor) {
        if (null == this.lockedColor) {
            _lockedColor = lockedColor;
            redraw();
        } else {
            this.lockedColor.set(lockedColor);
        }
    }
    public ObjectProperty<Color> lockedColorProperty() {
        if (null == lockedColor) {
            lockedColor = new ObjectPropertyBase<Color>(_lockedColor) {
                @Override protected void invalidated() { redraw(); }
                @Override public Object getBean() { return Joystick.this; }
                @Override public String getName() { return "lockedColor"; }
            };
            _lockedColor = null;
        }
        return lockedColor;
    }

    public boolean isTouched() { return null == touched ? _touched : touched.get(); }
    private void setTouched(final boolean touched) {
        if (null == this.touched) {
            _touched = touched;
            redraw();
        } else {
            this.touched.set(touched);
        }
    }
    public ReadOnlyBooleanProperty touchedProperty() {
        if (null == touched) {
            touched = new BooleanPropertyBase(_touched) {
                @Override protected void invalidated() { redraw(); }
                @Override public Object getBean() { return Joystick.this; }
                @Override public String getName() { return "touched"; }
            };
        }
        return touched;
    }

    public double getValue() { return value.get(); }
    private void setValue(final double value) { this.value.set(value); }
    public ReadOnlyDoubleProperty valueProperty() { return value; }

    public double getAngle() { return angle.get(); }
    private void setAngle(final double angle) { this.angle.set(angle); }
    public ReadOnlyDoubleProperty angleProperty() { return angle; }

    public double getX() { return x.get(); }
    private void setX(final double x) { this.x.set(x); }
    public ReadOnlyDoubleProperty xProperty() { return x; }

    public double getY() { return y.get(); }
    private void setY(final double y) { this.y.set(y); }
    public ReadOnlyDoubleProperty yProperty() { return y; }

    private void reset() {
        if (!isStickyMode()) {
            if (isAnimated()) {
                KeyValue kvX0 = new KeyValue(touchPoint.centerXProperty(), touchPoint.getCenterX(), Interpolator.EASE_OUT);
                KeyValue kvY0 = new KeyValue(touchPoint.centerYProperty(), touchPoint.getCenterY(), Interpolator.EASE_OUT);
                KeyValue kvV0 = new KeyValue(value, value.get(), Interpolator.EASE_OUT);
                KeyValue kvX1 = new KeyValue(touchPoint.centerXProperty(), 0.5 * size, Interpolator.EASE_OUT);
                KeyValue kvY1 = new KeyValue(touchPoint.centerYProperty(), 0.5 * size, Interpolator.EASE_OUT);
                KeyValue kvV1 = new KeyValue(value, 0, Interpolator.EASE_OUT);
                KeyFrame kf0  = new KeyFrame(Duration.ZERO, kvX0, kvY0, kvV0);
                KeyFrame kf1  = new KeyFrame(Duration.millis(getDurationMillis()), kvX1, kvY1, kvV1);
                timeline.getKeyFrames().setAll(kf0, kf1);
                timeline.play();
            } else {
                touchPoint.setCenterX(center);
                touchPoint.setCenterY(center);
                resetTouchButtons();
                value.set(0);
                angle.set(0);
            }
        }
    }

    private void resetTouchButtons() {
        Color inactiveColor = getInactiveColor();
        switch(getLockState()) {
            case X_LOCKED:
                touchN.setStroke(transclucentActiveColor);
                touchNW.setStroke(transclucentActiveColor);
                touchW.setStroke(inactiveColor);
                touchSW.setStroke(transclucentActiveColor);
                touchS.setStroke(transclucentActiveColor);
                touchSE.setStroke(transclucentActiveColor);
                touchE.setStroke(inactiveColor);
                touchNE.setStroke(transclucentActiveColor);
                break;
            case Y_LOCKED:
                touchN.setStroke(inactiveColor);
                touchNW.setStroke(transclucentActiveColor);
                touchW.setStroke(transclucentActiveColor);
                touchSW.setStroke(transclucentActiveColor);
                touchS.setStroke(inactiveColor);
                touchSE.setStroke(transclucentActiveColor);
                touchE.setStroke(transclucentActiveColor);
                touchNE.setStroke(transclucentActiveColor);
                break;
            case UNLOCKED:
            default:
                touchN.setStroke(inactiveColor);
                touchNW.setStroke(inactiveColor);
                touchW.setStroke(inactiveColor);
                touchSW.setStroke(inactiveColor);
                touchS.setStroke(inactiveColor);
                touchSE.setStroke(inactiveColor);
                touchE.setStroke(inactiveColor);
                touchNE.setStroke(inactiveColor);
                break;
        }
    }

    private void setXY(final double newX, final double newY, final double newAngle) {
        double x   = clamp(size * 0.15, size * 0.85, newX);
        double y   = clamp(size * 0.15, size * 0.85, newY);
        double dx  = x - center;
        double dy  = -(y - center);
        double rad = Math.atan2(dy, dx) + HALF_PI;
        double phi = Math.toDegrees(rad - Math.PI);
        if (phi < 0) { phi += 360.0; }
        setAngle(phi);
        double r    = Math.sqrt(dx * dx + dy * dy);
        double maxR = size * 0.35;
        if (r > maxR) {
            x = -Math.cos(rad + HALF_PI) * maxR + center;
            y = Math.sin(rad + HALF_PI) * maxR + center;
            r = maxR;
        }
        setX(-Math.cos(rad + HALF_PI));
        setY(-Math.sin(rad + HALF_PI));

        touchPoint.setCenterX(x);
        touchPoint.setCenterY(y);
        setValue(r / maxR);

        reset();
    }

    private Arc createArc(final double startAngle) {
        Arc arc  = new Arc(0.5 * size, 0.5 * size, 0.455 * size, 0.455 * size, startAngle + 90 - 18.5, 37);
        arc.setFill(Color.TRANSPARENT);
        arc.setStrokeLineCap(StrokeLineCap.BUTT);
        arc.addEventHandler(MouseEvent.MOUSE_PRESSED, mouseHandler);
        arc.addEventHandler(MouseEvent.MOUSE_RELEASED, mouseHandler);
        arc.addEventHandler(TouchEvent.TOUCH_PRESSED, touchHandler);
        arc.addEventHandler(TouchEvent.TOUCH_RELEASED, touchHandler);
        return arc;
    }

    private double clamp(final double min, final double max, final double value) {
        if (value < min) { return min; }
        if (value > max) { return max; }
        return value;
    }
    private long clamp(final long min, final long max, final long value) {
        if (value < min) { return min; }
        if (value > max) { return max; }
        return value;
    }

    
    // ******************** Resizing ******************************************
    private void resize() {
        width  = getWidth() - getInsets().getLeft() - getInsets().getRight();
        height = getHeight() - getInsets().getTop() - getInsets().getBottom();
        size   = width < height ? width : height;
        center = size * 0.5;

        if (width > 0 && height > 0) {
            pane.setMaxSize(size, size);
            pane.setPrefSize(size, size);
            pane.relocate((getWidth() - size) * 0.5, (getHeight() - size) * 0.5);

            background.setWidth(0.7 * size);
            background.setHeight(0.7 * size);
            background.relocate(0.15 *size, 0.15 * size);
            double extra = 1.2;
            resizeArc(touchN);
//            touchNText.setTranslateX(center + (center * Math.cos(Math.toRadians(90))));            
//            touchNText.setTranslateY(center + (-center * Math.sin(Math.toRadians(90))));            
            resizeArc(touchNW);
//            touchNWText.setTranslateX(center + (center*extra * Math.cos(Math.toRadians(135))));            
//            touchNWText.setTranslateY(center + (-center*extra * Math.sin(Math.toRadians(135))));            
            resizeArc(touchW);
//            touchWText.setTranslateX(center + (center*extra * Math.cos(Math.toRadians(180))));            
//            touchWText.setTranslateY(center + (-center*extra * Math.sin(Math.toRadians(180))));            
            resizeArc(touchSW);
//            touchSWText.setTranslateX(center + (center*extra * Math.cos(Math.toRadians(215))));            
//            touchSWText.setTranslateY(center + (-center*extra * Math.sin(Math.toRadians(215))));            
            resizeArc(touchS);
//            touchSText.setTranslateX(center + (center * Math.cos(Math.toRadians(260))));            
//            touchSText.setTranslateY(center + (-center * Math.sin(Math.toRadians(260))));            
            resizeArc(touchSE);
//            touchSEText.setTranslateX(center + (center * Math.cos(Math.toRadians(315))));            
//            touchSEText.setTranslateY(center + (-center * Math.sin(Math.toRadians(315))));            
            resizeArc(touchE);
//            touchEText.setTranslateX(center + (center * Math.cos(Math.toRadians(0))));            
//            touchEText.setTranslateY(center + (-center * Math.sin(Math.toRadians(0))));            
            resizeArc(touchNE);
//            touchNEText.setTranslateX(center + (center * Math.cos(Math.toRadians(45))));            
//            touchNEText.setTranslateY(center + (-center * Math.sin(Math.toRadians(45))));            

            touchIndicator.setRadius(0.4 * size);
            touchIndicator.setCenterX(center);
            touchIndicator.setCenterY(center);

            touchPoint.setRadius(0.05 * size);
            touchPoint.setCenterX(center + offsetX);
            touchPoint.setCenterY(center + offsetY);

            redraw();
        }
    }

    private void resizeArc(final Arc arc) {
        arc.setCenterX(center);
        arc.setCenterY(center);
        arc.setRadiusX(0.455 * size);
        arc.setRadiusY(0.455 * size);
        arc.setStrokeWidth(0.084 * size);
    }

    private void drawBackground() {
        double w = background.getWidth();
        double h = background.getHeight();
        ctx.clearRect(0, 0, background.getWidth(), background.getHeight());
        ctx.setFill(getInactiveColor());
        ctx.fillOval(0, 0, w, h);
        ctx.setFill(Color.TRANSPARENT);
        ctx.setStroke(LockState.X_LOCKED == getLockState() || LockState.Y_LOCKED == getLockState() ? getLockedColor() : transclucentActiveColor);
        ctx.strokeLine(0.15 * w, 0.15 * h, 0.85 * w, 0.85 * h);
        ctx.strokeLine(0.85 * w, 0.15 * h, 0.15 * w, 0.85 * h);
        ctx.setStroke(transclucentActiveColor);
        ctx.strokeOval(0.42857143 * w, 0.42857143 * h, 0.14285714 * w, 0.14285714 * h);
        ctx.setStroke(LockState.Y_LOCKED == getLockState() ? getLockedColor() : transclucentActiveColor);
        ctx.strokeLine(0, 0.5 * h, w, 0.5 *h);
        ctx.setStroke(LockState.X_LOCKED == getLockState() ? getLockedColor() : transclucentActiveColor);
        ctx.strokeLine(0.5 * w, 0, 0.5 * w, h);

        ctx.save();
        double value            = getValue();
        double chevronHalfWidth = 0.05 * w;
        double chevronHeight    = 0.04 * h;
        double center           = 0.5 * w;

        double offsetY          = center;
//        double offsetY          = h - chevronHeight * 0.25;

        double chevronStepY     = 1.22 * chevronHeight;
        ctx.translate(center, center);
        ctx.rotate(-getAngle());
        ctx.translate(-center, -center);
        ctx.setStroke(getActiveColor());
        ctx.setLineWidth(0.015 * h);
        ctx.setLineCap(StrokeLineCap.ROUND);
        ctx.setLineJoin(StrokeLineJoin.ROUND);
        int counter = 0;
        for (double i = 0.0 ; i < value - 0.1 ; i += 0.1) {
            ctx.strokeLine(center - chevronHalfWidth, offsetY - counter * chevronStepY, center, offsetY - (counter + 1) * chevronStepY);
            ctx.strokeLine(center, offsetY - (counter + 1) * chevronStepY, center + chevronHalfWidth, offsetY - counter * chevronStepY);
            counter += 1;
        }
        ctx.restore();
    }

    private void redraw() {
        drawBackground();
        Color activeColor   = getActiveColor();
        Color inactiveColor = getInactiveColor();
        switch(getLockState()) {
            case X_LOCKED:
                touchN.setStroke(transclucentActiveColor);
                touchNW.setStroke(transclucentActiveColor);
                touchW.setStroke(touchW.isHover() ? activeColor : inactiveColor);
                touchSW.setStroke(transclucentActiveColor);
                touchS.setStroke(transclucentActiveColor);
                touchSE.setStroke(transclucentActiveColor);
                touchE.setStroke(touchE.isHover() ? activeColor : inactiveColor);
                touchNE.setStroke(transclucentActiveColor);
                break;
            case Y_LOCKED:
                touchN.setStroke(touchN.isHover() ? activeColor : inactiveColor);
                touchNW.setStroke(transclucentActiveColor);
                touchW.setStroke(transclucentActiveColor);
                touchSW.setStroke(transclucentActiveColor);
                touchS.setStroke(touchS.isHover() ? activeColor : inactiveColor);
                touchSE.setStroke(transclucentActiveColor);
                touchE.setStroke(transclucentActiveColor);
                touchNE.setStroke(transclucentActiveColor);
                break;
            case UNLOCKED:
            default:
                touchN.setStroke(touchN.isHover() ? activeColor : inactiveColor);
                touchNW.setStroke(touchNW.isHover() ? activeColor : inactiveColor);
                touchW.setStroke(touchW.isHover() ? activeColor : inactiveColor);
                touchSW.setStroke(touchSW.isHover() ? activeColor : inactiveColor);
                touchS.setStroke(touchS.isHover() ? activeColor : inactiveColor);
                touchSE.setStroke(touchSE.isHover() ? activeColor : inactiveColor);
                touchE.setStroke(touchE.isHover() ? activeColor : inactiveColor);
                touchNE.setStroke(touchNE.isHover() ? activeColor : inactiveColor);
                break;
        }
        //Override default logic if colors and strings made available
        if(null != buttonColors) {
            touchN.setStroke(touchN.isHover() ? touchNLatch ? buttonColors.get(0) : activeColor : buttonColors.get(0));
            touchNW.setStroke(touchNW.isHover() ? touchNWLatch ? buttonColors.get(1) : activeColor : buttonColors.get(1));
            touchW.setStroke(touchW.isHover() ? touchWLatch ? buttonColors.get(2) : activeColor : buttonColors.get(2));
            touchSW.setStroke(touchSW.isHover() ? touchSWLatch ? buttonColors.get(3) : activeColor : buttonColors.get(3));
            touchS.setStroke(touchS.isHover() ? touchSLatch ? buttonColors.get(4) : activeColor : buttonColors.get(4));
            touchSE.setStroke(touchSE.isHover() ? touchSELatch ? buttonColors.get(5) : activeColor : buttonColors.get(5));
            touchE.setStroke(touchE.isHover() ? touchELatch ? buttonColors.get(6) : activeColor : buttonColors.get(6));
            touchNE.setStroke(touchNE.isHover() ? touchNELatch ? buttonColors.get(7) : activeColor : buttonColors.get(7));
        }
//        if(null != buttonStrings) {
//            touchNText.setText(buttonStrings.get(0));
//            touchNWText.setText(buttonStrings.get(1));
//            touchWText.setText(buttonStrings.get(2));
//            touchSWText.setText(buttonStrings.get(3));
//            touchSText.setText(buttonStrings.get(4));
//            touchSEText.setText(buttonStrings.get(5));
//            touchEText.setText(buttonStrings.get(6));
//            touchNEText.setText(buttonStrings.get(7));
//        }

        touchPoint.setFill(isTouched() ? activeColor : Color.TRANSPARENT);
        touchIndicator.setStroke(isTouched() ? activeColor : inactiveColor);
    }
    
    public void setupButtons(List<VectorAction> actions, List<Color> colorList, List<String> stringList, List<String> textList) {
        vectorActions = actions;
        buttonColors = colorList;
        buttonStrings = stringList;
        buttonText = textList;
    }
    public Circle getTouchPoint() {
        return touchPoint;
    }
}
