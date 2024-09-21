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

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.javafx.components.panes.AboutPane;
import edu.jhuapl.trinity.javafx.components.panes.ConfigurationPane;
import edu.jhuapl.trinity.javafx.components.panes.DataPane;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.RadialEntityEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.Glow;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Shadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;
import lit.litfx.controls.covalent.PathPane;
import lit.litfx.controls.menus.LitRadialContainerMenuItem;
import lit.litfx.controls.menus.LitRadialMenu;
import lit.litfx.controls.menus.LitRadialMenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sean Phillips
 */
public class MainNavMenu extends LitRadialMenu {
    private static final Logger LOG = LoggerFactory.getLogger(MainNavMenu.class);

    Scene scene;

    //Various GUI views
    DataPane dataPane;
    ConfigurationPane configurationPane;
    AboutPane aboutPane;

    //defaults
    public static double ITEM_SIZE = 60.0;
    public static double INNER_RADIUS = 60.0;
    public static double ITEM_FIT_WIDTH = 85.0;
    public static double MENU_RADIUS = 215.0;
    public static double OFFSET = 9.0;
    public static double INITIAL_ANGLE = -120.0;
    public static double STROKE_WIDTH = 1.5;

    static Color bgLg1Color = Color.DARKCYAN.deriveColor(1, 1, 1, 0.2);
    static Color bgLg2Color = Color.LIGHTBLUE.deriveColor(1, 1, 1, 0.5);
    static Color bgMoLg1Color = Color.LIGHTSKYBLUE.deriveColor(1, 1, 1, 0.3);
    static Color bgMoLg2Color = Color.DARKBLUE.deriveColor(1, 1, 1, 0.6);
    static Color strokeColor = Color.ALICEBLUE;
    static Color strokeMouseOnColor = Color.CYAN;
    static Color outlineColor = Color.GREEN;
    static Color outlineMouseOnColor = Color.LIGHTGREEN;

    static LinearGradient background = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0, bgLg1Color), new Stop(0.8, bgLg2Color));
    static LinearGradient backgroundMouseOn = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
        new Stop(0, bgMoLg1Color), new Stop(0.8, bgMoLg2Color));
    static ImageView iv = ResourceUtils.loadIcon("trinity", 150);

    public MainNavMenu(Scene scene) {
        this(INITIAL_ANGLE, ITEM_SIZE, MENU_RADIUS, OFFSET, scene);
    }

    public MainNavMenu(double initialAngle, double itemSize, double menuSize, double offset, Scene scene) {
        super(initialAngle, itemSize, menuSize, offset,
            background, backgroundMouseOn, strokeColor, strokeMouseOnColor,
            false, LitRadialMenu.CenterVisibility.ALWAYS, iv);
        this.scene = scene;
        //System.out.println("MainNavMenu constructor...");
        buildMenu();

        //System.out.println("buildMenu() complete ");

        Glow glow = new Glow(0.5);
        iv.setEffect(glow);
        iv.setTranslateX(-ITEM_FIT_WIDTH / 2.0);
        iv.setTranslateY(-ITEM_FIT_WIDTH / 2.0);
        resetMenuSizes();
        resetMenuColors();

        setHideMenuOnItemClick(true);
        this.scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_MENU_ITEM, e -> handleRadialEntityEvent(e));
        this.scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_INNER_RADIUS, e -> handleRadialEntityEvent(e));
        this.scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_ITEM_FIT_WIDTH, e -> handleRadialEntityEvent(e));
        this.scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_MENU_SIZE, e -> handleRadialEntityEvent(e));
        this.scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_OFFSET, e -> handleRadialEntityEvent(e));
        this.scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_INITIAL_ANGLE, e -> handleRadialEntityEvent(e));
        this.scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_STROKE_WIDTH, e -> handleRadialEntityEvent(e));
        //@TODO SMP
//        setDefaultsFromConfig();
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

    public static void slideInPane(PathPane pane) {
        //https://stackoverflow.com/questions/48893282/javafx-apply-perspective-transformation-on-a-node-given-a-perspective-transform?noredirect=1&lq=1
        PerspectiveTransform pt = new PerspectiveTransform();
        pt.setUlx(pane.getWidth());
        pt.setUly(pane.getHeight() * 0.5);
        pt.setUrx(pane.getWidth() + 1.0);
        pt.setUry(pane.getHeight() * 0.5);

        pt.setLrx(pane.getWidth() + 1.0);
        pt.setLry(pane.getHeight() * 0.5);
        pt.setLlx(pane.getWidth());
        pt.setLly(pane.getHeight() * 0.5);
        pane.setEffect(pt);

        Duration showPointDuration = Duration.seconds(0.75);
        Duration midPointDuration = Duration.seconds(0.75);
        Duration endPointDuration = Duration.seconds(1.00);

        Timeline timeline = new Timeline(
            new KeyFrame(showPointDuration, e -> pane.show()),

            //animation to midpoint
            new KeyFrame(midPointDuration, new KeyValue(pt.ulxProperty(), pane.getWidth() * 0.75)),
            new KeyFrame(midPointDuration, new KeyValue(pt.ulyProperty(), 0.0)),
            new KeyFrame(midPointDuration, new KeyValue(pt.urxProperty(), pane.getWidth())),
            new KeyFrame(midPointDuration, new KeyValue(pt.uryProperty(), pane.getHeight() * 0.333)),

            new KeyFrame(midPointDuration, new KeyValue(pt.lrxProperty(), pane.getWidth())),
            new KeyFrame(midPointDuration, new KeyValue(pt.lryProperty(), pane.getHeight() * 0.666)),
            new KeyFrame(midPointDuration, new KeyValue(pt.llxProperty(), pane.getWidth() * 0.75)),
            new KeyFrame(midPointDuration, new KeyValue(pt.llyProperty(), pane.getHeight())),

            //animation to actual size
            new KeyFrame(endPointDuration, new KeyValue(pt.ulxProperty(), 0.0)),
            new KeyFrame(endPointDuration, new KeyValue(pt.ulyProperty(), 0.0)),
            new KeyFrame(endPointDuration, new KeyValue(pt.urxProperty(), pane.getWidth())),
            new KeyFrame(endPointDuration, new KeyValue(pt.uryProperty(), 0.0)),

            new KeyFrame(endPointDuration, new KeyValue(pt.lrxProperty(), pane.getWidth())),
            new KeyFrame(endPointDuration, new KeyValue(pt.lryProperty(), pane.getHeight())),
            new KeyFrame(endPointDuration, new KeyValue(pt.llxProperty(), 0.0)),
            new KeyFrame(endPointDuration, new KeyValue(pt.llyProperty(), pane.getHeight()))
        );
        timeline.play();
        timeline.setOnFinished(e -> {
            pane.setEffect(null);
        });
    }

    private void setDefaultsFromConfig() {
        ITEM_SIZE = Double.valueOf((String) App.getConfig().configProps.get("ITEM_SIZE"));
        INNER_RADIUS = Double.valueOf((String) App.getConfig().configProps.get("INNER_RADIUS"));
        ITEM_FIT_WIDTH = Double.valueOf((String) App.getConfig().configProps.get("ITEM_FIT_WIDTH"));
        MENU_RADIUS = Double.valueOf((String) App.getConfig().configProps.get("MENU_RADIUS"));
        OFFSET = Double.valueOf((String) App.getConfig().configProps.get("OFFSET"));
        INITIAL_ANGLE = Double.valueOf((String) App.getConfig().configProps.get("INITIAL_ANGLE"));
        STROKE_WIDTH = Double.valueOf((String) App.getConfig().configProps.get("STROKE_WIDTH"));
    }

    private void handleRadialEntityEvent(RadialEntityEvent event) {
        if (event.getEventType() == RadialEntityEvent.RADIAL_ENTITY_MENU_ITEM) {
            setMenuItemSize(event.newValue);
        } else if (event.getEventType() == RadialEntityEvent.RADIAL_ENTITY_INNER_RADIUS) {
            setInnerRadius(event.newValue);
        } else if (event.getEventType() == RadialEntityEvent.RADIAL_ENTITY_ITEM_FIT_WIDTH) {
            setGraphicsFitWidth(event.newValue);
            //submenu type support if ever needed
//            subnetItems.forEach(item -> {
//                item.getItems().forEach((t) -> {
//                    Node node = t.getGraphic();
//                    if(node instanceof ImageView) {
//                        ImageView iv = (ImageView)node;
//                        iv.setFitWidth(event.newValue);
//                    }
//                });
//            });
        } else if (event.getEventType() == RadialEntityEvent.RADIAL_ENTITY_MENU_SIZE) {
            setRadius(event.newValue);
        } else if (event.getEventType() == RadialEntityEvent.RADIAL_ENTITY_OFFSET) {
            setOffset(event.newValue);
        } else if (event.getEventType() == RadialEntityEvent.RADIAL_ENTITY_INITIAL_ANGLE) {
            setInitialAngle(event.newValue);
        } else if (event.getEventType() == RadialEntityEvent.RADIAL_ENTITY_STROKE_WIDTH) {
            setStrokeWidth(event.newValue);
        }
    }

    public void buildMenu() {
        setStrokeWidth(STROKE_WIDTH);
        setOutlineStrokeWidth(STROKE_WIDTH);
        setOutlineStrokeFill(outlineColor);
        setOutlineStrokeMouseOnFill(outlineMouseOnColor);
        Glow glow = new Glow(5.2);
        Shadow shadow = new Shadow(BlurType.GAUSSIAN, Color.ALICEBLUE, 50);
        setOutlineEffect(shadow);
        setHideMenuOnItemClick(false);

        //System.out.println("Loading first menu icon...");

        ImageView configuration = ResourceUtils.loadIcon("configuration", ITEM_FIT_WIDTH);
        //System.out.println("first menu icon loaded.");
        configuration.setEffect(glow);
        ImageView data = ResourceUtils.loadIcon("data", ITEM_FIT_WIDTH);
        data.setEffect(glow);
        ImageView hypersurface = ResourceUtils.loadIcon("hypersurface", ITEM_FIT_WIDTH);
        hypersurface.setEffect(glow);

        ImageView projections = ResourceUtils.loadIcon("graph", ITEM_FIT_WIDTH);
        projections.setEffect(glow);

        ImageView hyperspace = ResourceUtils.loadIcon("hyperspace", ITEM_FIT_WIDTH);
        hyperspace.setEffect(glow);

        ImageView system = ResourceUtils.loadIcon("system", ITEM_FIT_WIDTH);
        system.setEffect(glow);

        ImageView about = ResourceUtils.loadIcon("about", ITEM_FIT_WIDTH);
        about.setEffect(glow);
        ImageView restoreAll = ResourceUtils.loadIcon("restore", ITEM_FIT_WIDTH);
        restoreAll.setEffect(glow);
        ImageView shutdown = ResourceUtils.loadIcon("shutdown", ITEM_FIT_WIDTH);
        shutdown.setEffect(glow);

        //System.out.println("attempting submenus...");
        /* System submenus */

        try {
            LitRadialContainerMenuItem systemSubMenuItem = new LitRadialContainerMenuItem(ITEM_SIZE, "System", system);
            //System.out.println("LitRadialContainerMenuItem created...");

            systemSubMenuItem.addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "About", about, e -> {
                scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_ABOUT));
                Pane pathPane = App.getAppPathPaneStack();
                if (null == aboutPane)
                    aboutPane = new AboutPane(scene, pathPane);
                if (!pathPane.getChildren().contains(aboutPane))
                    pathPane.getChildren().add(aboutPane);
                slideInPane(aboutPane);
            }));
            //System.out.println("attempting restore item...");
            systemSubMenuItem.addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Restore Panes", restoreAll, e -> {
                scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.RESTORE_PANES));
            }));
            systemSubMenuItem.addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Shutdown", shutdown, e -> {
                scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHUTDOWN));
            }));
            addMenuItem(systemSubMenuItem);
        } catch (Exception ex) {
            LOG.error("Exception", ex);
        }


        //System.out.println("attempting Configuration item...");
        addMenuItem(new LitRadialMenuItem(ITEM_SIZE, "Configuration", configuration, e -> {
            Pane pathPane = App.getAppPathPaneStack();
            if (null == configurationPane)
                configurationPane = new ConfigurationPane(scene, pathPane);
            if (!pathPane.getChildren().contains(configurationPane))
                pathPane.getChildren().add(configurationPane);
            configurationPane.show();
            slideInPane(configurationPane);
        }));

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Hypersurface", hypersurface, e -> {
            scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_HYPERSURFACE));
        }));
        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Projections", projections, e -> {
            scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_PROJECTIONS));
        }));

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Hyperspace", hyperspace, e -> {
            scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_HYPERSPACE));
        }));
        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Data", data, e -> {
            scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_DATA));
            Pane pathPane = App.getAppPathPaneStack();
            if (null == dataPane)
                dataPane = new DataPane(scene, pathPane);
            if (!pathPane.getChildren().contains(dataPane))
                pathPane.getChildren().add(dataPane);
            slideInPane(dataPane);
        }));
    }
}
