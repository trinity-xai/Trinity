package edu.jhuapl.trinity.javafx.components.radial;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.javafx.events.EffectEvent;
import edu.jhuapl.trinity.javafx.events.RadialEntityEvent;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;
import lit.litfx.controls.menus.LitRadialContainerMenuItem;
import lit.litfx.controls.menus.LitRadialMenu;
import lit.litfx.controls.menus.LitRadialMenuItem;
import lit.litfx.core.components.BandEmitter;
import lit.litfx.core.components.CircleQuadBandCreator;
import lit.litfx.core.components.targeting.SpinningReticule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class RadialEntity extends LitRadialMenu {
    private static final Logger LOG = LoggerFactory.getLogger(RadialEntity.class);
    public static double ORBITING_CIRCLE_OFFSET = 20;
    public static double ITEM_SIZE = 180.0; //degrees
    public static double INNER_RADIUS = 150.0;
    public static double ITEM_FIT_WIDTH = 200.0;
    public static double CENTER_FIT_WIDTH = 600.0;
    public static double MENU_RADIUS = 200.0;
    public static double OFFSET = 5.0;
    public static double INITIAL_ANGLE = 0.0;
    public static double STROKE_WIDTH = 0.1;
    public static double MIN_OUTLINE_STROKE_WIDTH = 0.1;
    public static double MAX_OUTLINE_STROKE_WIDTH = 2.0;

    public static double SUBMENU_ITEM_SIZE = 30.0; //degrees
    public static double SHADOW_RADIUS = 100.0;
    public static double SHADOW_WIDTH = 100.0;
    public static double SHADOW_HEIGHT = 100.0;
    public static double SHADOW_SPREAD = 0.5;

    public static Paint DEFAULT_FILL = Color.TRANSPARENT;
    public static Paint DEFAULT_MOUSEON_FILL = Color.CYAN.deriveColor(1, 1, 1, 0.2);

    public static Paint DEFAULT_STROKE = Color.DARKCYAN.deriveColor(1, 1, 1, 0.1);
    public static Paint DEFAULT_MOUSEON_STROKE = Color.CYAN.deriveColor(1, 1, 1, 0.5);

    public static Paint DEFAULT_OUTLINE_STROKE = Color.CYAN.deriveColor(1, 1, 1, 0.8);
    public static Paint DEFAULT_MOUSEON_OUTLINE_STROKE = Color.YELLOW.deriveColor(1, 1, 1, 0.8);

    public static String DEFAULT_IMAGE_CHIP = "defaultchip";
    public static String DEFAULT_IMAGE = "defaultimage";

    //Velocity of 1.0 will cause no change in band
    public double BAND_INWARD_VELOCITY = 0.8;
    public double BAND_OUTWARD_VELOCITY = 1.1;

    public String entityName;
    public List<LitRadialContainerMenuItem> subItems;
    public SimpleBooleanProperty emittingBands = new SimpleBooleanProperty(false);
    private boolean emittingEnabled = true;
    public BandEmitter centerBe;
    public Scene scene;
    public Glow glow;
    public DropShadow shadow;
    public Timeline glowTimeline;
    public SpinningReticule reticule;
    public RotateTransition livingRotate;
    public Circle orbitingCircle;

    private double mousePosX, mousePosY, mouseOldX, mouseOldY, mouseDeltaX, mouseDeltaY;

    public RadialEntity(String entityName, double centerFitWidth) {
        super(INITIAL_ANGLE, INNER_RADIUS, MENU_RADIUS, OFFSET,
            DEFAULT_FILL, DEFAULT_MOUSEON_FILL, DEFAULT_STROKE, DEFAULT_MOUSEON_STROKE,
            false, LitRadialMenu.CenterVisibility.ALWAYS,
            ResourceUtils.loadIcon(DEFAULT_IMAGE, centerFitWidth));
        this.entityName = entityName;
        getCenterGraphic().setTranslateX(-centerFitWidth / 2.0);
        getCenterGraphic().setTranslateY(-centerFitWidth / 2.0);

        glow = new Glow(1.0);
        getCenterGraphic().setEffect(glow);
        setStrokeWidth(STROKE_WIDTH);
        setOutlineStrokeWidth(MIN_OUTLINE_STROKE_WIDTH);
        setOutlineStrokeFill(DEFAULT_OUTLINE_STROKE);
        setOutlineStrokeMouseOnFill(DEFAULT_MOUSEON_STROKE);
        setOutlineEffect(glow);

        subItems = new ArrayList<>();
        createBandEmitter();

        hideRadialMenu();
        mouseOnProperty.addListener(c -> {
            if (mouseOnProperty.get()) {
                if (null != glowTimeline) {
                    glowTimeline.pause();
                }
                glow.setLevel(1.0);
                setOutlineStrokeWidth(MAX_OUTLINE_STROKE_WIDTH);
            } else {
                if (null != glowTimeline) {
                    glowTimeline.play();
                }
                setOutlineStrokeWidth(MIN_OUTLINE_STROKE_WIDTH);
            }
        });
        setHideMenuOnItemClick(false);

        centerGroup.setOnMousePressed(me -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
        centerGroup.setOnMouseDragged(me -> {
            mouseOldX = mousePosX;
            mouseOldY = mousePosY;
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseDeltaX = (mousePosX - mouseOldX);
            mouseDeltaY = (mousePosY - mouseOldY);
            setTranslateX(getTranslateX() + mouseDeltaX);
            setTranslateY(getTranslateY() + mouseDeltaY);
        });
        shadow = new DropShadow(BlurType.GAUSSIAN,
            Color.CYAN.deriveColor(1, 1, 1, 0.8), SHADOW_RADIUS, SHADOW_SPREAD,
            0, 0);
        shadow.setWidth(SHADOW_WIDTH);
        shadow.setHeight(SHADOW_HEIGHT);
        setEffect(shadow);
        createGlowAnimation();

        reticule = new SpinningReticule(getRadius() + 20, 3,
            0.666, 0.85, 1.0, 0.25,
            25, 50, 75);
        reticule.setCenterRadius(5);
        reticule.setVisible(false);
        reticule.c1.setStroke(Color.ALICEBLUE.deriveColor(1, 1, 1, 0.5));
        reticule.c1.setStrokeLineJoin(StrokeLineJoin.ROUND);
        reticule.c1.setFill(Color.TRANSPARENT);

        reticule.c2.setStroke(Color.DEEPSKYBLUE.deriveColor(1, 1, 1, 0.333));
        reticule.c2.setStrokeLineJoin(StrokeLineJoin.ROUND);
        reticule.c2.setFill(Color.TRANSPARENT);

        reticule.c3.setStroke(Color.CYAN.deriveColor(1, 1, 1, 0.85));
        reticule.c3.setStrokeLineJoin(StrokeLineJoin.ROUND);
        reticule.c3.setFill(Color.TRANSPARENT);

        reticule.setManaged(false);
        getChildren().add(reticule);

        orbitingCircle = dashedCircle(getRadius() + ORBITING_CIRCLE_OFFSET,
            1.5, getRadius(), Color.CYAN.deriveColor(1, 1, 1, 0.5));
        orbitingCircle.setManaged(false);
        getChildren().add(orbitingCircle);

        livingRotate = reticule.getCircleRotate(orbitingCircle, 360, 20.0);
        livingRotate.setRate(1);
        livingRotate.setCycleCount(Animation.INDEFINITE);
        livingRotate.play();
    }

    public void itemReticuleAnimation() {
        itemReticuleAnimation(90, 180, 360, 0.5, 1.0, 2.0);
    }

    public SequentialTransition itemReticuleLivingSpin(
        int c1Angle, int c2Angle, int c3Angle,
        double c1Seconds, double c2Seconds, double c3Seconds) {
        //Make reticule visible but completely translucent so it can fade in
        Platform.runLater(() -> {
            orbitingCircle.setVisible(false);
            reticule.setOpacity(0.0);
            reticule.setVisible(true);
        });
        FadeTransition fadeInTransition = new FadeTransition(Duration.seconds(0.250), reticule);
        fadeInTransition.setToValue(1.0);
        ParallelTransition parallelTransition = new ParallelTransition(
            reticule.getCircleRotate(reticule.c1, c1Angle, c1Seconds),
            reticule.getCircleRotate(reticule.c2, c2Angle, c2Seconds),
            reticule.getCircleRotate(reticule.c3, c3Angle, c3Seconds)
        );
        SequentialTransition sequential = new SequentialTransition();
        sequential.getChildren().addAll(fadeInTransition, parallelTransition);
        sequential.setAutoReverse(true);
        sequential.setCycleCount(Animation.INDEFINITE);
        sequential.play();
        return sequential;
    }

    public void itemReticuleAnimation(
        int c1Angle, int c2Angle, int c3Angle,
        double c1Seconds, double c2Seconds, double c3Seconds) {
        //Make reticule visible but completely translucent so it can fade in
        Platform.runLater(() -> {
            orbitingCircle.setVisible(false);
            reticule.setOpacity(0.0);
            reticule.setVisible(true);
        });
        FadeTransition fadeInTransition = new FadeTransition(Duration.seconds(0.250), reticule);
        fadeInTransition.setToValue(1.0);
        ParallelTransition parallelTransition = new ParallelTransition(
            reticule.getCircleRotate(reticule.c1, c1Angle, c1Seconds),
            reticule.getCircleRotate(reticule.c2, c2Angle, c2Seconds),
            reticule.getCircleRotate(reticule.c3, c3Angle, c3Seconds)
        );

        FadeTransition fadeOutTransition = new FadeTransition(Duration.seconds(0.250), reticule);
        fadeOutTransition.setFromValue(1.0);
        fadeOutTransition.setToValue(0.0);

        SequentialTransition sequential = new SequentialTransition();
        //sequential.getChildren().addAll(attackerST, fadeTransition, pathTransition, parallelTransition);
        sequential.getChildren().addAll(fadeInTransition, parallelTransition, fadeOutTransition);
        sequential.setOnFinished(e -> {
            orbitingCircle.setVisible(true);
            showRadialMenu();
        });
        sequential.play();
    }

    private Circle dashedCircle(double radius, double strokeWidth,
                                double dashSpacing, Color strokeColor) {
        Glow glow = new Glow(1);
        Circle c = new Circle(radius, Color.TRANSPARENT);
        c.setEffect(glow);
        c.setStroke(strokeColor);
        c.setStrokeWidth(strokeWidth);
        c.setStrokeLineJoin(StrokeLineJoin.MITER);
        c.setStrokeMiterLimit(50);
        c.getStrokeDashArray().addAll(dashSpacing);
        c.setMouseTransparent(true);
        return c;
    }

    public void setScene(Scene daScene) {
        scene = daScene;
        scene.addEventHandler(EffectEvent.ENABLE_EMITTERS, e -> {
            emittingEnabled = (boolean) e.object;
        });
        scene.addEventHandler(EffectEvent.START_EMITTING, e -> {
            if (emittingEnabled && e.stringId.contentEquals(entityName))
                emittingBands.set(true);
        });
        scene.addEventHandler(EffectEvent.STOP_EMITTING, e -> {
            if (emittingEnabled && e.stringId.contentEquals(entityName))
                emittingBands.set(false);
        });

        scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_MENU_ITEM, e -> handleRadialEntityEvent(e));
        scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_INNER_RADIUS, e -> handleRadialEntityEvent(e));
        scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_ITEM_FIT_WIDTH, e -> handleRadialEntityEvent(e));
        scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_MENU_SIZE, e -> handleRadialEntityEvent(e));
        scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_OFFSET, e -> handleRadialEntityEvent(e));
        scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_INITIAL_ANGLE, e -> handleRadialEntityEvent(e));
        scene.addEventHandler(RadialEntityEvent.RADIAL_ENTITY_STROKE_WIDTH, e -> handleRadialEntityEvent(e));

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
            subItems.forEach(item -> {
                item.getItems().forEach((t) -> {
                    Node node = t.getGraphic();
                    if (node instanceof ImageView iv) {
                        iv.setFitWidth(event.newValue);
                    }
                });
            });
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

    public void setEmitterColors(Paint strokePaint, Paint fillPaint) {
        centerBe.setCustomStroke(strokePaint);
        centerBe.setCustomFill(fillPaint);
    }

    public void setShowEmitter(boolean show) {
        emittingBands.set(show);
    }

    private void createBandEmitter() {
        centerBe = new BandEmitter(30, BAND_OUTWARD_VELOCITY, INNER_RADIUS, 5.0);
        centerBe.setPathThickness(0.5);
        centerBe.setManaged(false);
        centerBe.setGeneratorCenterX(getTranslateX());
        centerBe.setGeneratorCenterY(getTranslateY());
        centerBe.setTimeToLiveSeconds(5);
        translateXProperty().addListener((o, t, t1) -> {
            centerBe.setGeneratorCenterX(getTranslateX());
        });
        translateYProperty().addListener((o, t, t1) -> {
            centerBe.setGeneratorCenterY(getTranslateY());
        });
        centerBe.setMouseTransparent(true);
        setEmitterColors(Color.RED.deriveColor(1, 1, 1, 0.666), Color.RED.deriveColor(1, 1, 1, 0.111));
        Task animationTask = new Task() {
            @Override
            protected Void call() throws Exception {
                while (!this.isCancelled() && !this.isDone()) {
                    if (emittingBands.get() && emittingEnabled)
                        updateBands();
                    Thread.sleep(1500);
                }
                return null;
            }
        };
        Thread animationThread = new Thread(animationTask);
        animationThread.setDaemon(true);
        animationThread.start();
    }

    private void updateBands() {
        Platform.runLater(() -> {
            //CircleQuadBandCreator specific fields
            CircleQuadBandCreator cqbc = (CircleQuadBandCreator) centerBe.getQuadBandCreator();
            cqbc.setInitialRadius(getInnerRadius() / 3.0);
            if (getMouseOnProperty().get())
                cqbc.setVelocity(BAND_INWARD_VELOCITY);
            else
                cqbc.setVelocity(BAND_OUTWARD_VELOCITY);
            cqbc.setVelocity(BAND_OUTWARD_VELOCITY);
            centerBe.createQuadBand();
        });
    }

    private void createGlowAnimation() {

        glowTimeline = new Timeline(
            new KeyFrame(Duration.seconds(0.0), new KeyValue(glow.levelProperty(), 0.0)),
            new KeyFrame(Duration.seconds(0.0), new KeyValue(outlineStrokeWidth, 0.0)),
//            new KeyFrame(Duration.seconds(0.0), new KeyValue(shadow.radiusProperty(), 1)),
//            new KeyFrame(Duration.seconds(0.0), new KeyValue(shadow.spreadProperty(), 1)),
            new KeyFrame(Duration.seconds(1.5), new KeyValue(glow.levelProperty(), 0.8)),
            new KeyFrame(Duration.seconds(1.5), new KeyValue(outlineStrokeWidth, 0.444)),

            new KeyFrame(Duration.seconds(2.0), new KeyValue(glow.levelProperty(), 1.0)),
            new KeyFrame(Duration.seconds(2.0), new KeyValue(outlineStrokeWidth, 0.5))
//            new KeyFrame(Duration.seconds(2.0), new KeyValue(shadow.spreadProperty(), SHADOW_SPREAD))
        );
        glowTimeline.setAutoReverse(true);
        glowTimeline.setCycleCount(Animation.INDEFINITE);
        glowTimeline.play();
    }

    public int getTotalItems() {
        return items.size();
    }

    public void resizeItemsToFit() {

        double degreesPerItem = 360.0 / items.size();
        if (degreesPerItem > 180.0) {
            LOG.info("Capping item size to 180 degrees.");
            setMenuItemSize(180.0);
        } else {
            setMenuItemSize(degreesPerItem);
        }

        double scaling = 2.0;
        double pixelsPerItem = (getRadius() * scaling) / items.size();
        items.stream().forEach(item -> {
            Node node = item.getGraphic();
            if (node instanceof ImageView iv) {
                iv.setPreserveRatio(true);
                iv.setFitWidth(pixelsPerItem);
            }
        });
    }

    public void setCenterFitWidth(double fitWidth) {
        Node centerNode = getCenterGraphic();
        if (centerNode instanceof ImageView civ) {
            civ.setFitWidth(fitWidth);
            civ.setTranslateX(-fitWidth / 2.0);
            civ.setTranslateY(-fitWidth / 2.0);
        }
    }

    public void clearAllItems() {
        for (int i = items.size() - 1; i >= 0; i--) {
            removeMenuItem(i);
        }
    }

    public LitRadialMenuItem addItem(String name, ImageView imageView, boolean resize, boolean show) {
        LitRadialMenuItem submenuitem = new LitRadialMenuItem(
            RadialEntity.ITEM_SIZE,
            name + "-" + getTotalItems(),
            imageView
        );
        addMenuItem(submenuitem);
        if (resize)
            resizeItemsToFit();
        if (show)
            showRadialMenu();
        return submenuitem;
    }

    public void addItem(String name) {
        ImageView imageView = ResourceUtils.loadIcon(RadialEntity.DEFAULT_IMAGE_CHIP,
            RadialEntity.ITEM_FIT_WIDTH);
        addItem(name, imageView, true, true);
    }

    public void addSubItem(String subItemName, List<LitRadialMenuItem> systemItems) {
        LitRadialContainerMenuItem subItem = new LitRadialContainerMenuItem(
            ITEM_SIZE, subItemName, ResourceUtils.loadIcon(DEFAULT_IMAGE_CHIP, ITEM_FIT_WIDTH));
        addMenuItem(subItem);
        subItems.add(subItem);
    }
}
