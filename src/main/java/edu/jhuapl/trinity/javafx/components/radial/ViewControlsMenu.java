package edu.jhuapl.trinity.javafx.components.radial;

/*-
 * #%L
 * trinity
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

import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Projections3DPane;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Shadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import lit.litfx.controls.menus.LitRadialMenuItem;


/**
 * @author Sean Phillips
 */
public class ViewControlsMenu extends RadialEntity {
    //momma view this will control and attach to
    Projections3DPane projections3DPane;

    //defaults
    public static double IMAGE_FIT_HEIGHT = 64;
    public static double IMAGE_FIT_WIDTH = 64;
    public static double ITEM_SIZE = 72;
    public static double INNER_RADIUS = 60.0;
    public static double ITEM_FIT_WIDTH = 64.0;
    public static double MENU_RADIUS = 175.0;
    public static double OFFSET = 15.0;
    public static double INITIAL_ANGLE = -20.0;
    public static double STROKE_WIDTH = 2.5;

    static Color bgLg1Color = Color.DARKCYAN.deriveColor(1, 1, 1, 0.2);
    static Color bgLg2Color = Color.LIGHTBLUE.deriveColor(1, 1, 1, 0.5);
    static Color bgMoLg1Color = Color.LIGHTSKYBLUE.deriveColor(1, 1, 1, 0.3);
    static Color bgMoLg2Color = Color.DARKBLUE.deriveColor(1, 1, 1, 0.6);
    static Color strokeColor = Color.ALICEBLUE;
    static Color strokeMouseOnColor = Color.YELLOW;
    static Color outlineColor = Color.GREEN;
    static Color outlineMouseOnColor = Color.LIGHTGREEN;

    static LinearGradient background = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0, bgLg1Color), new Stop(0.8, bgLg2Color));
    static LinearGradient backgroundMouseOn = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0, bgMoLg1Color), new Stop(0.8, bgMoLg2Color));
    ImageView iv;

    public ViewControlsMenu(Projections3DPane projections3DPane) {
        this(INITIAL_ANGLE, ITEM_SIZE, MENU_RADIUS, OFFSET, projections3DPane);
    }

    public ViewControlsMenu(double initialAngle, double itemSize, double menuSize, double offset, Projections3DPane projections3DPane) {
        super("view", IMAGE_FIT_WIDTH);
        this.projections3DPane = projections3DPane;
        this.scene = projections3DPane.scene;
        setScene(scene);
        buildMenu();

        setMenuItemSize(itemSize);
        setInnerRadius(INNER_RADIUS);
        setGraphicsFitWidth(ITEM_FIT_WIDTH);
        setRadius(menuSize);
        setOffset(offset);
        setInitialAngle(initialAngle);
        setStrokeWidth(STROKE_WIDTH);

        setStrokeWidth(STROKE_WIDTH);
        setOutlineStrokeWidth(STROKE_WIDTH);
        setOutlineStrokeFill(outlineColor);
        setOutlineStrokeMouseOnFill(outlineMouseOnColor);
        setHideMenuOnItemClick(true);

        setEmitterColors(Color.CYAN.deriveColor(1, 1, 1, 0.5),
            Color.CYAN.deriveColor(1, 1, 1, 0.15));
        setShowEmitter(false);
        setManaged(false);

        iv = ResourceUtils.loadIcon("camera", ITEM_FIT_WIDTH);
        if (null != iv) {
            iv.setSmooth(true);
            iv.setPreserveRatio(true);
            iv.setFitWidth(IMAGE_FIT_WIDTH);
            iv.setFitHeight(IMAGE_FIT_HEIGHT);
            setCenterGraphic(iv);
            getCenterGraphic().setTranslateX(-iv.getFitWidth() / 2.0);
            getCenterGraphic().setTranslateY(-IMAGE_FIT_HEIGHT / 2.0);
        }
        getCenterGroup().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.isControlDown() && event.getClickCount() > 1) {
                hideRadialMenu();
                setVisible(false);
                event.consume();
            }
        });
        getCenterGroup().addEventHandler(SwipeEvent.SWIPE_RIGHT, event -> {
            hideRadialMenu();
            event.consume();
        });
        addEventHandler(RotateEvent.ROTATE, event -> {
            setInitialAngle(getInitialAngle() - event.getAngle());
            event.consume();
        });
        itemGroup.visibleProperty().addListener(cl -> {
            if (itemGroup.isVisible()) {
                orbitingCircle.setRadius(getRadius() + ORBITING_CIRCLE_OFFSET);
            } else {
                orbitingCircle.setRadius(getInnerRadius() + ORBITING_CIRCLE_OFFSET);
            }
        });
    }

    public void resetMenuColors() {
        setBackgroundFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, bgLg1Color), new Stop(0.8, bgLg2Color)));
        setBackgroundMouseOnFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, bgMoLg1Color), new Stop(0.8, bgMoLg2Color)));
        setStrokeColor(strokeColor);
        setStrokeMouseOnColor(strokeMouseOnColor);
        setOutlineStrokeFill(outlineColor);
        setOutlineStrokeMouseOnFill(outlineMouseOnColor);
    }

    public void resetMenuSizes() {
        setMenuItemSize(ITEM_SIZE);
        setGraphicsFitWidth(ITEM_FIT_WIDTH);
        setRadius(MENU_RADIUS);
        setOffset(OFFSET);
        setInitialAngle(INITIAL_ANGLE);
        setStrokeWidth(STROKE_WIDTH);
    }


    public void buildMenu() {
        Glow glow = new Glow(0.5);
        Shadow shadow = new Shadow(BlurType.GAUSSIAN, Color.ALICEBLUE, 50);
        setOutlineEffect(shadow);

        ImageView freeCam = ResourceUtils.loadIcon("freecam", ITEM_FIT_WIDTH);
        freeCam.setEffect(glow);

        ImageView cockpit = ResourceUtils.loadIcon("fps", ITEM_FIT_WIDTH);
        cockpit.setEffect(glow);

        ImageView aftThruster = ResourceUtils.loadIcon("aft", ITEM_FIT_WIDTH);
        aftThruster.setEffect(glow);

        ImageView touchCountrols = ResourceUtils.loadIcon("touch", ITEM_FIT_WIDTH);
        touchCountrols.setEffect(glow);
        
        ImageView keyboard = ResourceUtils.loadIcon("keyboard", ITEM_FIT_WIDTH);
        keyboard.setEffect(glow);

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE , "Free Cam", freeCam, e -> {
            System.out.println("Free Camera View...");
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.FREE_CAMERA_MODE));
        }));

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE, "Cockpit", cockpit, e -> {
            System.out.println("FPS Cockpit View...");
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.FPS_CAMERA_MODE));
        }));

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE, "Aft Thruster", aftThruster, e -> {
            System.out.println("Over the Shoulder Aft Thruster View...");
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOULDER_CAMERA_MODE));        
        }));
        
        LitRadialMenuItem touchMenuItem = new LitRadialMenuItem(ITEM_SIZE*2.0, "Touch Controls", touchCountrols, e -> {
            System.out.println("Toggling Touch controls...");
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_JOYSTICK_CONTROLS));
        });
        addMenuItem(touchMenuItem);

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE, "Keyboard/Mouse", keyboard, e -> {
            System.out.println("Switching to Keyboard/Mouse...");
        }));
        
        
    }
}
