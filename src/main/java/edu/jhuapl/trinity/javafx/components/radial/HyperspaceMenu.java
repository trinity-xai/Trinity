/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.components.radial;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.javafx.components.panes.RadarPlotPane;
import edu.jhuapl.trinity.javafx.components.panes.SearchPane;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Hyperspace3DPane;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.Glow;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Shadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import lit.litfx.controls.covalent.PathPane;
import lit.litfx.controls.menus.LitRadialMenuItem;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * @author Sean Phillips
 */
public class HyperspaceMenu extends RadialEntity {
    //momma view this will control and attach to
    Hyperspace3DPane hyperspace3DPane;

    //Various special GUI views
    RadarPlotPane radarPlotPane;
    SearchPane searchPane;
    //defaults
    public static double IMAGE_FIT_HEIGHT = 64;
    public static double IMAGE_FIT_WIDTH = 64;
    public static double ITEM_SIZE = 36;
    public static double INNER_RADIUS = 70.0;
    public static double ITEM_FIT_WIDTH = 64.0;
    public static double MENU_RADIUS = 216.0;
    public static double OFFSET = 10.0;
    public static double INITIAL_ANGLE = 0.0;
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

    public HyperspaceMenu(Hyperspace3DPane hyperspace3DPane) {
        this(INITIAL_ANGLE, ITEM_SIZE, MENU_RADIUS, OFFSET, hyperspace3DPane);
    }

    public HyperspaceMenu(double initialAngle, double itemSize, double menuSize, double offset, Hyperspace3DPane hyperspace3DPane) {
        super("Hyperspace Menu", IMAGE_FIT_WIDTH);
        this.hyperspace3DPane = hyperspace3DPane;
        this.scene = hyperspace3DPane.scene;
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

        iv = ResourceUtils.loadIcon("hyperspace", ITEM_FIT_WIDTH);
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

    public void buildMenu() {
        Glow glow = new Glow(0.5);
        Shadow shadow = new Shadow(BlurType.GAUSSIAN, Color.ALICEBLUE, 50);
        setOutlineEffect(shadow);

        ImageView radar = ResourceUtils.loadIcon("radar", ITEM_FIT_WIDTH);
        radar.setEffect(glow);

        ImageView search = ResourceUtils.loadIcon("search", ITEM_FIT_WIDTH);
        search.setEffect(glow);

        ImageView filter = ResourceUtils.loadIcon("filter", ITEM_FIT_WIDTH);
        filter.setEffect(glow);

        ImageView copy = ResourceUtils.loadIcon("copy", ITEM_FIT_WIDTH);
        copy.setEffect(glow);

        ImageView save = ResourceUtils.loadIcon("save", ITEM_FIT_WIDTH);
        save.setEffect(glow);

        ImageView refresh = ResourceUtils.loadIcon("refresh", ITEM_FIT_WIDTH);
        refresh.setEffect(glow);

        ImageView clear = ResourceUtils.loadIcon("clear", ITEM_FIT_WIDTH);
        clear.setEffect(glow);

        ImageView camera = ResourceUtils.loadIcon("camera", ITEM_FIT_WIDTH);
        camera.setEffect(glow);

        ImageView callouts = ResourceUtils.loadIcon("callouts", ITEM_FIT_WIDTH);
        callouts.setEffect(glow);

        ImageView navigator = ResourceUtils.loadIcon("navigator", ITEM_FIT_WIDTH);
        navigator.setEffect(glow);

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Metadata Search", search, e -> {
            Pane pathPane = App.getAppPathPaneStack();
            if (null == searchPane) {
                searchPane = new SearchPane(scene, pathPane);
                searchPane.visibleProperty().bind(hyperspace3DPane.visibleProperty());
            }
            if (!pathPane.getChildren().contains(searchPane)) {
                pathPane.getChildren().add(searchPane);
                slideInPane(searchPane);
            } else {
                searchPane.show();
            }
            searchPane.showSearch();
        }));

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Metadata Filter", filter, e -> {
            Pane pathPane = App.getAppPathPaneStack();
            if (null == searchPane) {
                searchPane = new SearchPane(scene, pathPane);
                searchPane.visibleProperty().bind(hyperspace3DPane.visibleProperty());
            }
            if (!pathPane.getChildren().contains(searchPane)) {
                pathPane.getChildren().add(searchPane);
                slideInPane(searchPane);
            } else {
                searchPane.show();
            }
            searchPane.showFilters();
        }));

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Parameter RADAR", radar, e -> {
            Pane pathPane = App.getAppPathPaneStack();
            if (null == radarPlotPane) {
                radarPlotPane = new RadarPlotPane(scene, pathPane);
//                radarPlotPane.visibleProperty().bind(hyperspace3DPane.visibleProperty());
            }
            if (!pathPane.getChildren().contains(radarPlotPane)) {
                pathPane.getChildren().add(radarPlotPane);
                slideInPane(radarPlotPane);
            } else {
                radarPlotPane.show();
            }
        }));

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Copy Snapshot", copy, e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putImage(hyperspace3DPane.snapshot(new SnapshotParameters(), null));
            clipboard.setContent(content);
        }));
        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Save as Image", save, e -> {
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save scene as...");
            fileChooser.setInitialFileName("trinity_hyperspace.png");
            fileChooser.setInitialDirectory(Paths.get(".").toFile());
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                setVisible(false);
                WritableImage image = hyperspace3DPane.snapshot(new SnapshotParameters(), null);
                setVisible(true);
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                } catch (IOException ioe) {
                    // TODO: handle exception here
                }
            }
        }));
        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Refresh Render", refresh, e -> {
            hyperspace3DPane.updateAll();
        }));

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Clear Data", clear, e -> {
            hyperspace3DPane.clearAll();
            hyperspace3DPane.updateView(true);
            hyperspace3DPane.cubeWorld.clearAll();
        }));
        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Reset Camera View", camera, e -> {
            hyperspace3DPane.resetView(1000, false);
        }));
        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Clear Callouts", callouts, e -> {
            hyperspace3DPane.clearCallouts();
        }));

        addMenuItem(new LitRadialMenuItem(ITEM_SIZE * 0.5, "Content Navigator", navigator, e -> {
            hyperspace3DPane.getScene().getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_NAVIGATOR_PANE));
        }));
    }
}
