package edu.jhuapl.trinity.javafx.javafx3d;

import edu.jhuapl.trinity.data.CoordinateSet;
import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.FeatureLayer;
import edu.jhuapl.trinity.data.messages.xai.FeatureVector;
import edu.jhuapl.trinity.javafx.components.ColorMap;
import edu.jhuapl.trinity.javafx.components.panes.ShadowPane;
import edu.jhuapl.trinity.javafx.events.ColorMapEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.events.ShadowEvent;
import edu.jhuapl.trinity.utils.DataUtils;
import edu.jhuapl.trinity.utils.Utils;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import org.fxyz3d.scene.CubeWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class ShadowCubeWorld extends CubeWorld {

    public static enum PROJECTION_TYPE {FIXED_ORTHOGRAPHIC, ROTATING_PERSPECTIVE}

    public ShadowPane x1ShadowPane;
    public ShadowPane x2ShadowPane;
    public ShadowPane y1ShadowPane;
    public ShadowPane y2ShadowPane;
    public ShadowPane z1ShadowPane;
    public ShadowPane z2ShadowPane;
    Crosshair3D crosshair3D;
    public List<FeatureVector> featureVectors;
    public boolean meanCentered = true;
    public boolean autoScaling = true;
    public List<Double> meanVector = new ArrayList<>();
    public double maxAbsValue = 1.0;
    public double meanCenteredMaxAbsValue = 1.0;

    public static double DEFAULT_POINT_SIZE = 10.0;
    public static Color DEFAULT_COLOR = Color.ALICEBLUE;
    public int xFactorIndex = 0;
    public int yFactorIndex = 1;
    public int zFactorIndex = 2;
    public Double xShift = 0.0;
    public Double yShift = 0.0;
    public Double zShift = 0.0;
    public double point3dSize = DEFAULT_POINT_SIZE;
    public Scene activeScene;
    public double pointSizeScaling = 0.5; //default assumes pointsize as diameter
    public double pointScale = 1.0;
    public double scatterBuffScaling = 1.0;
    public double minDomain = -1.0;
    public double maxDomain = 1.0;
    public boolean overrideDomainTransform = false;
    public long hyperspaceRefreshRate = 500; //milliseconds
    public double pointOpacity = 0.5;
    public boolean renderingEnabled = true;
    public boolean showNearsidePoints = false; //declutters view
    public double panelRectangleOpacity = 0.01;
    PROJECTION_TYPE projectionType = ShadowCubeWorld.PROJECTION_TYPE.FIXED_ORTHOGRAPHIC;
    Affine projectionAffine = new Affine();
    public boolean dirty = false;
    public boolean reflectY = true;
    public HyperspaceEvent.COLOR_MODE colorMode = HyperspaceEvent.COLOR_MODE.COLOR_BY_LABEL;
    public ColorMapEvent.COLOR_MAP colorMap = ColorMapEvent.COLOR_MAP.ONE_COLOR_SPECTRUM;
    AnimationTimer animationTimer;
    double domainWidth;
    double minX;
    double rangeX;
    double minY;
    double rangeY;
    double minZ;
    double rangeZ;

    public ShadowCubeWorld(double size, double spacing, boolean selfLight, List<FeatureVector> featureVectors) {
        super(size, spacing, selfLight);
        this.featureVectors = featureVectors;
        crosshair3D = new Crosshair3D(Point3D.ZERO, size, 10.0f);
        getChildren().add(crosshair3D);

        x1AxisRectangle.setVisible(false);
        setFillOpacity(x1AxisRectangle, panelRectangleOpacity);
        x1ShadowPane = new ShadowPane(false, false, x1AxisRectangle);
        x1ShadowPane.setOnMouseClicked(eh -> mouseDrawHandler(eh));
        getChildren().add(x1ShadowPane);

        x2AxisRectangle.setVisible(false);
        setFillOpacity(x2AxisRectangle, panelRectangleOpacity);
        x2ShadowPane = new ShadowPane(false, false, x2AxisRectangle);
        x2ShadowPane.setOnMouseClicked(eh -> mouseDrawHandler(eh));
        getChildren().add(x2ShadowPane);

        y1AxisRectangle.setVisible(false);
        setFillOpacity(y1AxisRectangle, panelRectangleOpacity);
        y1ShadowPane = new ShadowPane(false, false, y1AxisRectangle);
        y1ShadowPane.setOnMouseClicked(eh -> mouseDrawHandler(eh));
        getChildren().add(y1ShadowPane);

        y2AxisRectangle.setVisible(false);
        setFillOpacity(y2AxisRectangle, panelRectangleOpacity);
        y2ShadowPane = new ShadowPane(false, false, y2AxisRectangle);
        y2ShadowPane.setOnMouseClicked(eh -> mouseDrawHandler(eh));
        getChildren().add(y2ShadowPane);

        z1AxisRectangle.setVisible(false);
        setFillOpacity(z1AxisRectangle, panelRectangleOpacity);
        z1ShadowPane = new ShadowPane(false, false, z1AxisRectangle);
        z1ShadowPane.setOnMouseClicked(eh -> mouseDrawHandler(eh));
        getChildren().add(z1ShadowPane);

        z2AxisRectangle.setVisible(false);
        setFillOpacity(z2AxisRectangle, panelRectangleOpacity);
        z2ShadowPane = new ShadowPane(false, false, z2AxisRectangle);
        z2ShadowPane.setOnMouseClicked(eh -> mouseDrawHandler(eh));
        getChildren().add(z2ShadowPane);

        //hide cube control points by default
        getChildren().filtered((Node t) -> t instanceof Sphere)
            .forEach(s -> s.setVisible(false));
        //hide cube frame by default
        showXAxesGroup(false);
        showYAxesGroup(false);
        showZAxesGroup(false);

        x1ShadowPane.setDepthTest(DepthTest.ENABLE);
        x2ShadowPane.setDepthTest(DepthTest.ENABLE);
        y1ShadowPane.setDepthTest(DepthTest.ENABLE);
        y2ShadowPane.setDepthTest(DepthTest.ENABLE);
        z1ShadowPane.setDepthTest(DepthTest.ENABLE);
        z2ShadowPane.setDepthTest(DepthTest.ENABLE);

        animationTimer = new AnimationTimer() {
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_MILLI = 1_000_000;

            @Override
            public void handle(long now) {
                sleepNs = hyperspaceRefreshRate * NANOS_IN_MILLI;
                if ((now - prevTime) < sleepNs) return;
                prevTime = now;
                if (dirty) {
                    redraw(true);
                    dirty = false;
                }
            }
        };
        animationTimer.start();
    }

    public void setScene(Scene scene) {
        activeScene = scene;
        if (null != activeScene) {
            activeScene.addEventHandler(ShadowEvent.FIXED_ORHOGRAPHIC_PROJECTION, e -> {
                if ((boolean) e.object) {
                    projectionType = PROJECTION_TYPE.FIXED_ORTHOGRAPHIC;
                    Platform.runLater(() -> redraw(true));
                }
            });
            activeScene.addEventHandler(ShadowEvent.ROTATING_PERSPECTIVE_PROJECTION, e -> {
                if ((boolean) e.object) {
                    projectionType = PROJECTION_TYPE.ROTATING_PERSPECTIVE;
                    Platform.runLater(() -> redraw(true));
                }
            });
            activeScene.addEventHandler(HyperspaceEvent.SCALING_AUTO_NORMALIZE, e -> {
                autoScaling = (boolean) e.object;
                Platform.runLater(() -> redraw(true));
            });
            activeScene.addEventHandler(HyperspaceEvent.SCALING_MANUAL_BOUNDS, e -> {
                autoScaling = !(boolean) e.object;
                Platform.runLater(() -> redraw(true));
            });
            activeScene.addEventHandler(HyperspaceEvent.SCALING_MEAN_CENTERED, e -> {
                meanCentered = (boolean) e.object;
                Platform.runLater(() -> redraw(true));
            });

            //color mode events
            scene.addEventHandler(HyperspaceEvent.COLOR_BY_LABEL, e -> {
                colorMode = HyperspaceEvent.COLOR_MODE.COLOR_BY_LABEL;
                redraw(true);
            });
            scene.addEventHandler(HyperspaceEvent.COLOR_BY_LAYER, e -> {
                colorMode = HyperspaceEvent.COLOR_MODE.COLOR_BY_LAYER;
                redraw(true);
            });
            scene.addEventHandler(HyperspaceEvent.COLOR_BY_GRADIENT, e -> {
                colorMode = HyperspaceEvent.COLOR_MODE.COLOR_BY_GRADIENT;
                redraw(true);
            });
            scene.addEventHandler(HyperspaceEvent.COLOR_BY_SCORE, e -> {
                colorMode = HyperspaceEvent.COLOR_MODE.COLOR_BY_SCORE;
                redraw(true);
            });
            scene.addEventHandler(HyperspaceEvent.COLOR_BY_PFA, e -> {
                colorMode = HyperspaceEvent.COLOR_MODE.COLOR_BY_PFA;
                redraw(true);
            });
            //Color Map Events
            scene.addEventHandler(ColorMapEvent.ONE_COLOR_SPECTRUM, e -> {
                colorMap = ColorMapEvent.COLOR_MAP.ONE_COLOR_SPECTRUM;
                redraw(true);
            });
            scene.addEventHandler(ColorMapEvent.TWO_COLOR_SPECTRUM, e -> {
                colorMap = ColorMapEvent.COLOR_MAP.TWO_COLOR_SPECTRUM;
                redraw(true);
            });
            scene.addEventHandler(ColorMapEvent.HSB_WHEEL_SPECTRUM, e -> {
                colorMap = ColorMapEvent.COLOR_MAP.HSB_WHEEL_SPECTRUM;
                redraw(true);
            });
            scene.addEventHandler(ColorMapEvent.PRESET_COLOR_PALETTE, e -> {
                colorMap = ColorMapEvent.COLOR_MAP.PRESET_COLOR_PALETTE;
                redraw(true);
            });
            scene.addEventHandler(ColorMapEvent.COLOR_DOMAIN_CHANGE, e -> redraw(true));

            //Label Events
            activeScene.addEventHandler(HyperspaceEvent.UPDATEDALL_FACTOR_LABELS, e -> redraw(true));
            activeScene.addEventHandler(HyperspaceEvent.ADDEDALL_FACTOR_LABELS, e -> redraw(true));
            activeScene.addEventHandler(HyperspaceEvent.ADDED_FACTOR_LABEL, e -> redraw(true));
            activeScene.addEventHandler(HyperspaceEvent.UPDATED_FACTOR_LABEL, e -> redraw(true));
            activeScene.addEventHandler(HyperspaceEvent.REMOVED_FACTOR_LABEL, e -> redraw(true));
            activeScene.addEventHandler(HyperspaceEvent.CLEARED_FACTOR_LABELS, e -> redraw(true));
            //Layer events
            activeScene.addEventHandler(HyperspaceEvent.ADDEDALL_FEATURE_LAYER, e -> redraw(true));
            activeScene.addEventHandler(HyperspaceEvent.ADDED_FEATURE_LAYER, e -> redraw(true));
            activeScene.addEventHandler(HyperspaceEvent.UPDATED_FEATURE_LAYER, e -> redraw(true));
            activeScene.addEventHandler(HyperspaceEvent.REMOVED_FEATURE_LAYER, e -> redraw(true));
            activeScene.addEventHandler(HyperspaceEvent.CLEARED_FEATURE_LAYERS, e -> redraw(true));

            //point and factor events
            activeScene.addEventHandler(HyperspaceEvent.POINT3D_SIZE_GUI, e -> {
                point3dSize = (double) e.object;
                redraw(true);
            });
            activeScene.addEventHandler(HyperspaceEvent.POINT3D_SIZE_KEYPRESS, e -> {
                point3dSize = (double) e.object;
                redraw(true);
            });
            //scaling events
            activeScene.addEventHandler(HyperspaceEvent.POINT_SCALE_GUI, e -> {
                pointScale = (double) e.object;
                redraw(true);
            });
            activeScene.addEventHandler(HyperspaceEvent.POINT_SCALE_KEYPRESS, e -> {
                pointScale = (double) e.object;
                redraw(true);
            });
            activeScene.addEventHandler(HyperspaceEvent.SCATTERBUFF_SCALING_GUI, e -> {
                scatterBuffScaling = (double) e.object;
                redraw(true);
            });
            activeScene.addEventHandler(HyperspaceEvent.SCATTERBUFF_SCALING_KEYPRESS, e -> {
                scatterBuffScaling = (double) e.object;
                redraw(true);
            });
            activeScene.addEventHandler(HyperspaceEvent.FACTOR_COORDINATES_GUI, e -> {
                CoordinateSet coords = (CoordinateSet) e.object;
                xFactorIndex = coords.coordinateIndices.get(0);
                yFactorIndex = coords.coordinateIndices.get(1);
                zFactorIndex = coords.coordinateIndices.get(2);
                redraw(true);
            });
            activeScene.addEventHandler(HyperspaceEvent.FACTOR_COORDINATES_KEYPRESS, e -> {
                CoordinateSet coords = (CoordinateSet) e.object;
                xFactorIndex = coords.coordinateIndices.get(0);
                yFactorIndex = coords.coordinateIndices.get(1);
                zFactorIndex = coords.coordinateIndices.get(2);
                redraw(true);
            });
            //Visual aspect events
            activeScene.addEventHandler(ShadowEvent.SET_DOMAIN_MINIMUM, e -> {
                minDomain = (double) e.object;
                redraw(true);
            });
            activeScene.addEventHandler(ShadowEvent.SET_DOMAIN_MAXIMUM, e -> {
                maxDomain = (double) e.object;
                redraw(true);
            });
            activeScene.addEventHandler(ShadowEvent.SET_POINT_SCALING, e -> {
                pointSizeScaling = (double) e.object;
                redraw(true);
            });
            activeScene.addEventHandler(ShadowEvent.SET_POINT_OPACITY, e -> {
                pointOpacity = (double) e.object;
                redraw(true);
            });
            activeScene.addEventHandler(ShadowEvent.OVERRIDE_DOMAIN_TRANSFORM, e -> {
                overrideDomainTransform = (boolean) e.object;
                redraw(true);
            });
            activeScene.addEventHandler(ShadowEvent.SET_CUBE_VISIBLE, e -> {
                setVisible((boolean) e.object);
            });

            activeScene.addEventHandler(ShadowEvent.SET_CUBEWALLS_VISIBLE, e -> {
                boolean show = (boolean) e.object;
                showX1Panel(show);
                showX2Panel(show);
                showY1Panel(show);
                showY2Panel(show);
                showZ1Panel(show);
                showZ2Panel(show);
            });
            activeScene.addEventHandler(ShadowEvent.SHOW_AXES_LABELS, e -> {
                crosshair3D.setVisible((boolean) e.object);
            });
            activeScene.addEventHandler(ShadowEvent.SHOW_NEARSIDE_POINTS, e -> {
                showNearsidePoints = (boolean) e.object;
                x1ShadowPane.showNearsidePoints.set(showNearsidePoints);
                x2ShadowPane.showNearsidePoints.set(showNearsidePoints);
                y1ShadowPane.showNearsidePoints.set(showNearsidePoints);
                y2ShadowPane.showNearsidePoints.set(showNearsidePoints);
                z1ShadowPane.showNearsidePoints.set(showNearsidePoints);
                z2ShadowPane.showNearsidePoints.set(showNearsidePoints);
            });

            activeScene.addEventHandler(ShadowEvent.SET_CONTROLPOINTS_VISIBLE, e -> {
                boolean show = (boolean) e.object;
                getChildren().filtered((Node t) -> t instanceof Sphere)
                    .forEach(s -> s.setVisible(show));
            });
            activeScene.addEventHandler(ShadowEvent.ENABLE_CUBE_PROJECTIONS, e -> {
                renderingEnabled = (boolean) e.object;
            });
            activeScene.addEventHandler(ShadowEvent.SET_GRIDLINES_VISIBLE, e -> {
                boolean show = (boolean) e.object;
                showAllGridLines(show);
            });
            activeScene.addEventHandler(ShadowEvent.SET_FRAME_VISIBLE, e -> {
                boolean show = (boolean) e.object;
                showXAxesGroup(show);
                showYAxesGroup(show);
                showZAxesGroup(show);
            });
            activeScene.addEventHandler(ShadowEvent.SET_PANEL_OPACITY, e -> {
                panelRectangleOpacity = (double) e.object;
                setFillOpacity(x1AxisRectangle, panelRectangleOpacity);
                setFillOpacity(x2AxisRectangle, panelRectangleOpacity);
                setFillOpacity(y1AxisRectangle, panelRectangleOpacity);
                setFillOpacity(y2AxisRectangle, panelRectangleOpacity);
                setFillOpacity(z1AxisRectangle, panelRectangleOpacity);
                setFillOpacity(z2AxisRectangle, panelRectangleOpacity);
            });
            activeScene.addEventHandler(HyperspaceEvent.REFRESH_RATE_GUI, e ->
                hyperspaceRefreshRate = (long) e.object);
        }
    }

    public Timeline animateOut(double milliseconds, double distance) {
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().addAll(new KeyFrame(Duration.millis(milliseconds), // Frame End
                new KeyValue(x1AxisRectangle.translateZProperty(), -distance, Interpolator.EASE_OUT),
                new KeyValue(x2AxisRectangle.translateZProperty(), distance, Interpolator.EASE_OUT),
                new KeyValue(y1AxisRectangle.translateXProperty(), -distance, Interpolator.EASE_OUT),
                new KeyValue(y2AxisRectangle.translateXProperty(), distance, Interpolator.EASE_OUT),
                new KeyValue(z1AxisRectangle.translateYProperty(), distance, Interpolator.EASE_OUT),
                new KeyValue(z2AxisRectangle.translateYProperty(), -distance, Interpolator.EASE_OUT)),
            new KeyFrame(Duration.millis(milliseconds * 1.5), // Frame End
                new KeyValue(xy1GridLinesGroup.translateZProperty(), -distance, Interpolator.EASE_OUT),
                new KeyValue(xx1GridLinesGroup.translateZProperty(), -distance, Interpolator.EASE_OUT),
                new KeyValue(xy2GridLinesGroup.translateZProperty(), distance, Interpolator.EASE_OUT),
                new KeyValue(xx2GridLinesGroup.translateZProperty(), distance, Interpolator.EASE_OUT),

                new KeyValue(yy1GridLinesGroup.translateXProperty(), -distance, Interpolator.EASE_OUT),
                new KeyValue(yx1GridLinesGroup.translateXProperty(), -distance, Interpolator.EASE_OUT),
                new KeyValue(yy2GridLinesGroup.translateXProperty(), distance, Interpolator.EASE_OUT),
                new KeyValue(yx2GridLinesGroup.translateXProperty(), distance, Interpolator.EASE_OUT),

                new KeyValue(zy1GridLinesGroup.translateYProperty(), distance, Interpolator.EASE_OUT),
                new KeyValue(zx1GridLinesGroup.translateYProperty(), distance, Interpolator.EASE_OUT),
                new KeyValue(zy2GridLinesGroup.translateYProperty(), -distance, Interpolator.EASE_OUT),
                new KeyValue(zx2GridLinesGroup.translateYProperty(), -distance, Interpolator.EASE_OUT)));
        timeline.playFromStart();
        return timeline;
    }

    public Timeline animateIn(long milliseconds, double size) {
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().addAll(new KeyFrame(Duration.millis(milliseconds * 1.5), // Frame End
                new KeyValue(x1AxisRectangle.translateZProperty(), -size / 2.0, Interpolator.EASE_OUT),
                new KeyValue(x2AxisRectangle.translateZProperty(), size / 2.0, Interpolator.EASE_OUT),
                new KeyValue(y1AxisRectangle.translateXProperty(), -size, Interpolator.EASE_OUT),
                new KeyValue(y2AxisRectangle.translateXProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(z1AxisRectangle.translateYProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(z2AxisRectangle.translateYProperty(), -size, Interpolator.EASE_OUT)),
            new KeyFrame(Duration.millis(milliseconds), // Frame End
                new KeyValue(xy1GridLinesGroup.translateZProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(xx1GridLinesGroup.translateZProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(xy2GridLinesGroup.translateZProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(xx2GridLinesGroup.translateZProperty(), 0, Interpolator.EASE_OUT),

                new KeyValue(yy1GridLinesGroup.translateXProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(yx1GridLinesGroup.translateXProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(yy2GridLinesGroup.translateXProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(yx2GridLinesGroup.translateXProperty(), 0, Interpolator.EASE_OUT),

                new KeyValue(zy1GridLinesGroup.translateYProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(zx1GridLinesGroup.translateYProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(zy2GridLinesGroup.translateYProperty(), 0, Interpolator.EASE_OUT),
                new KeyValue(zx2GridLinesGroup.translateYProperty(), 0, Interpolator.EASE_OUT)));
        timeline.playFromStart();
        return timeline;
    }

    public void resetPanels(double size) {
        x1AxisRectangle.setTranslateX(-size / 2);
        x1AxisRectangle.setTranslateY(-size / 2);
        x1AxisRectangle.setTranslateZ(-size / 2);
        x2AxisRectangle.setTranslateX(-size / 2);
        x2AxisRectangle.setTranslateY(-size / 2);
        x2AxisRectangle.setTranslateZ(size / 2);

        y2AxisRectangle.setTranslateY(-size / 2);
        y2AxisRectangle.setRotationAxis(Rotate.Y_AXIS);
        y2AxisRectangle.setRotate(90); //89.9
        y1AxisRectangle.setTranslateX(-size);
        y1AxisRectangle.setTranslateY(-size / 2);
        y1AxisRectangle.setRotationAxis(Rotate.Y_AXIS);
        y1AxisRectangle.setRotate(90); //89.9

        z1AxisRectangle.setTranslateX(-size / 2);
        z1AxisRectangle.setRotationAxis(Rotate.X_AXIS);
        z1AxisRectangle.setRotate(90); //89.9
        z2AxisRectangle.setTranslateX(-size / 2);
        z2AxisRectangle.setTranslateY(-size);
        z2AxisRectangle.setRotationAxis(Rotate.X_AXIS);
        z2AxisRectangle.setRotate(90); //89.9
    }

    public void clearAll() {
        Platform.runLater(() -> x1ShadowPane.clearAll());
        Platform.runLater(() -> x2ShadowPane.clearAll());
        Platform.runLater(() -> y1ShadowPane.clearAll());
        Platform.runLater(() -> y2ShadowPane.clearAll());
        Platform.runLater(() -> z1ShadowPane.clearAll());
        Platform.runLater(() -> z2ShadowPane.clearAll());
    }

    public void clearNow() {
        x1ShadowPane.clearAll();
        x2ShadowPane.clearAll();
        y1ShadowPane.clearAll();
        y2ShadowPane.clearAll();
        z1ShadowPane.clearAll();
        z2ShadowPane.clearAll();
    }

    private boolean visibilityByLabel(String label) {
        FactorLabel fl = FactorLabel.getFactorLabel(label);
        if (null == fl)
            return true;
        return fl.getVisible();
    }

    private boolean visibilityByLayer(int layer) {
        FeatureLayer fl = FeatureLayer.getFeatureLayer(layer);
        if (null == fl)
            return true;
        return fl.getVisible();
    }

    public void rotateByEuler(double yaw, double pitch, double roll) {
        //    Sy1, Cy = math.sin(yaw), math.cos(yaw)
        double sinYaw = Math.sin(yaw);
        double cosYaw = Math.cos(yaw);
        //    Sp, Cp = math.sin(pitch), math.cos(pitch)
        double sinPitch = Math.sin(pitch);
        double cosPitch = Math.cos(pitch);
        //    Sr, Cr = math.sin(roll), math.cos(roll)
        double sinRoll = Math.sin(roll);
        double cosRoll = Math.cos(roll);
        //    R_ypr = np.array([[Cy*Cp, -Sy*Cr + Cy*Sp*Sr, Sy*Sr + Cy*Sp*Cr],
        //                      [Sy*Cp, Cy*Cr + Sy*Sp*Sr, -Cy*Sr + Sy*Sp*Cr],
        //                      [-Sp, Cp*Sr, Cp*Cr]])

        //d - the X coordinate scaling element
        //d1 - the XY coordinate element
        //d2 - the XZ coordinate element
        //d3 - the X coordinate translation element
        //d4 - the YX coordinate element
        //d5 - the Y coordinate scaling element
        //d6 - the YZ coordinate element
        //d7 - the Y coordinate translation element
        //d8 - the ZX coordinate element
        //d9 - the ZY coordinate element
        //d10 - the Z coordinate scaling element
        //d11 - the Z coordinate translation element
        projectionAffine.setToIdentity();
        projectionAffine.setToTransform(
            1.0, cosYaw * cosPitch, -sinYaw * cosRoll + cosYaw * sinPitch * sinRoll, sinYaw * sinRoll + cosYaw * sinPitch * cosRoll,
            sinYaw * cosPitch, 1.0, cosYaw * cosRoll + sinYaw * sinPitch * sinRoll, -cosYaw * sinRoll + sinYaw * sinPitch * cosRoll,
            -sinPitch, cosPitch * sinRoll, 1.0, cosPitch * cosRoll
        );
    }

    public void matrixRotate(double roll, double pitch, double yaw) {
        double A11 = Math.cos(roll) * Math.cos(yaw);
        double A12 = Math.cos(pitch) * Math.sin(roll) + Math.cos(roll) * Math.sin(pitch) * Math.sin(yaw);
        double A13 = Math.sin(roll) * Math.sin(pitch) - Math.cos(roll) * Math.cos(pitch) * Math.sin(yaw);
        double A21 = -Math.cos(yaw) * Math.sin(roll);
        double A22 = Math.cos(roll) * Math.cos(pitch) - Math.sin(roll) * Math.sin(pitch) * Math.sin(yaw);
        double A23 = Math.cos(roll) * Math.sin(pitch) + Math.cos(pitch) * Math.sin(roll) * Math.sin(yaw);
        double A31 = Math.sin(yaw);
        double A32 = -Math.cos(yaw) * Math.sin(pitch);
        double A33 = Math.cos(pitch) * Math.cos(yaw);

        double d = Math.acos((A11 + A22 + A33 - 1d) / 2d);
        if (d != 0d) {
            double den = 2d * Math.sin(d);
            Point3D p = new Point3D((A32 - A23) / den, (A13 - A31) / den, (A21 - A12) / den);
            Rotate r = new Rotate(Math.toDegrees(d), p);
            projectionAffine.setToTransform(r.createConcatenation(projectionAffine));
        }
    }

    public void addRotation(double angle, Point3D axis) {
        Rotate r = new Rotate(angle, axis);
        projectionAffine.setToTransform(r.createConcatenation(projectionAffine));
    }

    public void resetProjectionAffine() {
        projectionAffine = new Affine();
        redraw(true);
    }

    public void redraw(boolean clearFirst) {
        //bust out early to save CPU
        if (!renderingEnabled || featureVectors.isEmpty()) return;
        int currentMax = featureVectors.get(0).getData().size() - 1;
        if (currentMax < 2) return;
        //Index safety checks
        if (xFactorIndex > currentMax) {
            xFactorIndex = currentMax;
        }
        if (yFactorIndex > currentMax) {
            yFactorIndex = currentMax;
        }
        if (zFactorIndex > currentMax) {
            zFactorIndex = currentMax;
        }

        if (clearFirst)
            clearNow();
//@DEBUG SMP useful for debugging translations
//        Platform.runLater(()-> {
//        x1ShadowPane.setFill(Color.RED);
//        y1ShadowPane.setFill(Color.WHITE);
//        z1ShadowPane.setFill(Color.BLUE);
//        });
        //Check flag to see if we are mean centering
        if (meanCentered && !meanVector.isEmpty()) {
            xShift = meanVector.get(xFactorIndex);
            yShift = meanVector.get(yFactorIndex);
            zShift = meanVector.get(zFactorIndex);
        } else {
            xShift = 0.0;
            yShift = 0.0;
            zShift = 0.0;
        }
        //formula bounds
        domainWidth = overrideDomainTransform ? maxDomain - minDomain : 2 * scatterBuffScaling;
        minX = overrideDomainTransform ? minDomain : 0 - scatterBuffScaling;
        rangeX = x1ShadowPane.getCanvas().getWidth();
        minY = overrideDomainTransform ? minDomain : 0 - scatterBuffScaling;
        rangeY = x1ShadowPane.getCanvas().getHeight();
        minZ = overrideDomainTransform ? minDomain : 0 - scatterBuffScaling;
        rangeZ = x1ShadowPane.getCanvas().getWidth();
        //Check flag to see if we are auto normalizing
        if (autoScaling) {
            domainWidth = meanCentered ? 2 * meanCenteredMaxAbsValue : 2 * maxAbsValue;
            minX = meanCentered ? -meanCenteredMaxAbsValue : -maxAbsValue;
            minY = meanCentered ? -meanCenteredMaxAbsValue : -maxAbsValue;
            minZ = meanCentered ? -meanCenteredMaxAbsValue : -maxAbsValue;
        }
        double totalSize = point3dSize * pointSizeScaling;
        //pure CPU way using blocking Canvas draws
        FeatureVector[] fvArray = featureVectors.toArray(FeatureVector[]::new);
//        //@DEBUG SMP what are the mins and maxs
//        System.out.println(toString()+" trying to redraw...");
//        double miniX = fvArray[0].getData().get(xFactorIndex);
//        double maxiX = miniX;
//        double miniY = fvArray[1].getData().get(yFactorIndex);
//        double maxiY = miniY;
//        double miniZ = fvArray[2].getData().get(zFactorIndex);
//        double maxiZ = miniZ;
        for (FeatureVector fv : fvArray) {
            if (null != fv) {
//                //@DEBUG SMP
//                if(fv.getData().get(xFactorIndex)<miniX)
//                    miniX = fv.getData().get(xFactorIndex);
//                if(fv.getData().get(xFactorIndex)>maxiX)
//                    maxiX = fv.getData().get(xFactorIndex);
//
//                if(fv.getData().get(yFactorIndex)<miniY)
//                    miniY = fv.getData().get(yFactorIndex);
//                if(fv.getData().get(yFactorIndex)>maxiY)
//                    maxiY = fv.getData().get(yFactorIndex);
//
//                if(fv.getData().get(zFactorIndex)<miniZ)
//                    miniZ = fv.getData().get(zFactorIndex);
//                if(fv.getData().get(zFactorIndex)>maxiZ)
//                    maxiZ = fv.getData().get(zFactorIndex);

                if (visibilityByLabel(fv.getLabel())
                    && visibilityByLayer(fv.getLayer())) {
                    drawToPanes(fv, totalSize, minX, minY, minZ,
                        rangeX, rangeY, rangeZ, domainWidth);
                }
            }
        }
//        System.out.println("Mins/Maxs: "
//            +  miniX + ", " + maxiX + " "
//            +  miniY + ", " + maxiY + " "
//            +  miniZ + ", " + maxiZ
//        );
        copyCanvas(x1ShadowPane, x2ShadowPane);
        copyCanvas(y1ShadowPane, y2ShadowPane);
        copyCanvas(z1ShadowPane, z2ShadowPane);
        dirty = false;
    }

    private Color getColorByMode(FeatureVector fv, double minX, double minY, double minZ, double domainRange) {
        switch (colorMode) {
            case COLOR_BY_LAYER -> {
                return FeatureLayer.getColorByIndex(fv.getLayer());
            }
            case COLOR_BY_GRADIENT -> {
                double xCoord = fv.getData().get(xFactorIndex);// - xShift;
                double yCoord = fv.getData().get(yFactorIndex);// - yShift;
                double zCoord = fv.getData().get(zFactorIndex);// - zShift;
                return Color.color(
                    Utils.clamp(0, DataUtils.normalize(xCoord, minX, minX + domainRange), 1),
                    Utils.clamp(0, DataUtils.normalize(yCoord, minY, minY + domainRange), 1),
                    Utils.clamp(0, DataUtils.normalize(zCoord, minZ, minZ + domainRange), 1),
                    1.0).deriveColor(1, 100, 100, 1); //full saturation/brightness/opacity
            }
            case COLOR_BY_SCORE -> {
                if (null != colorMap) switch (colorMap) {
                    case HSB_WHEEL_SPECTRUM -> {
                        return Color.hsb(
                            DataUtils.normalize(fv.getScore(),
                                ColorMap.domainMin1, ColorMap.domainMax1) * 360.0,
                            1, 1);
                    }
                    case ONE_COLOR_SPECTRUM -> {
                        return ColorMap.getInterpolatedColor(fv.getScore(),
                                ColorMap.domainMin1, ColorMap.domainMax1,
                                ColorMap.singleColorSpectrum)
                            .deriveColor(1, 100, 100, 1); //full saturation/brightness/opacity;
                    }
                    case TWO_COLOR_SPECTRUM -> {
                        return ColorMap.twoColorInterpolation(
                                ColorMap.twoColorSpectrum1, ColorMap.twoColorSpectrum2,
                                ColorMap.domainMin1, ColorMap.domainMax1,
                                fv.getScore())
                            .deriveColor(1, 100, 100, 1); //full saturation/brightness/opacity;
                    }
                    case PRESET_COLOR_PALETTE -> {
                        return ColorMap.currentMap.get(
                                DataUtils.normalize(fv.getScore(),
                                    ColorMap.domainMin1, ColorMap.domainMax1))
                            .deriveColor(1, 100, 100, 1); //full saturation/brightness/opacity;
                    }
                    default -> {
                        return Color.ALICEBLUE;
                    }
                }
                return Color.ALICEBLUE;
            }
            case COLOR_BY_PFA -> {
                if (null != colorMap) switch (colorMap) {
                    case HSB_WHEEL_SPECTRUM -> {
                        return Color.hsb(
                            DataUtils.normalize(fv.getPfa(),
                                ColorMap.domainMin2, ColorMap.domainMax2) * 360.0,
                            1, 1);
                    }
                    case ONE_COLOR_SPECTRUM -> {
                        return ColorMap.getInterpolatedColor(fv.getPfa(),
                                ColorMap.domainMin2, ColorMap.domainMax2,
                                ColorMap.singleColorSpectrum)
                            .deriveColor(1, 100, 100, 1); //full saturation/brightness/opacity;
                    }
                    case TWO_COLOR_SPECTRUM -> {
                        return ColorMap.twoColorInterpolation(
                                ColorMap.twoColorSpectrum1, ColorMap.twoColorSpectrum2,
                                ColorMap.domainMin2, ColorMap.domainMax2,
                                fv.getPfa())
                            .deriveColor(1, 100, 100, 1); //full saturation/brightness/opacity    ;
                    }
                    case PRESET_COLOR_PALETTE -> {
                        return ColorMap.currentMap.get(
                            DataUtils.normalize(fv.getPfa(),
                                ColorMap.domainMin2, ColorMap.domainMax2)
                        );
                    }
                    default -> {
                        return Color.ALICEBLUE;
                    }
                }
                return Color.ALICEBLUE;
            }
            case COLOR_BY_LABEL -> {
                return FactorLabel.getColorByLabel(fv.getLabel());
            }
            default -> {
                return Color.ALICEBLUE;
            }
        }
    }

    private void drawToPanes(FeatureVector fv, double totalSize, double minX, double minY, double minZ,
                             double rangeX, double rangeY, double rangeZ, double domainWidth) {
        double x, y, zminus, zplus;
        Point3D affinePoint3D = null;
        double xCoord = fv.getData().get(xFactorIndex) - xShift;
        double yCoord = fv.getData().get(yFactorIndex) - yShift;
        double zCoord = fv.getData().get(zFactorIndex) - zShift;
        Color pointColor = getColorByMode(fv, minX, minY, minZ, domainWidth);
        //Should we reflect the Y values so positive is up?
        double reflect = reflectY ? -1 : 1;
        //Straight coordinate transform
        if (projectionType == PROJECTION_TYPE.FIXED_ORTHOGRAPHIC) {
            x = ((pointScale * xCoord - minX) * rangeX) / domainWidth;
            y = ((pointScale * reflect * yCoord - minY) * rangeY) / domainWidth;
            zminus = ((pointScale * -zCoord - minZ) * rangeZ) / domainWidth;
            zplus = ((pointScale * zCoord - minZ) * rangeZ) / domainWidth;
        } else { //first apply rotations
            affinePoint3D = projectionAffine.transform(
                xCoord, yCoord, zCoord);
            x = ((pointScale * affinePoint3D.getX() - minX) * rangeX) / domainWidth;
            y = ((pointScale * reflect * affinePoint3D.getY() - minY) * rangeY) / domainWidth;
            zminus = ((pointScale * -affinePoint3D.getZ() - minZ) * rangeZ) / domainWidth;
            zplus = ((pointScale * affinePoint3D.getZ() - minZ) * rangeZ) / domainWidth;
        }

        //draw circles at the transformed coordinate combinations
        x1ShadowPane.drawPoint(x, y, totalSize, pointColor);
        y1ShadowPane.drawPoint(zminus, y, totalSize, pointColor);
        z1ShadowPane.drawPoint(x, zplus, totalSize, pointColor);
    }

    private void copyCanvas(ShadowPane src, ShadowPane dst) {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = src.getCanvas().snapshot(params, null);
        dst.getCanvas().getGraphicsContext2D().drawImage(image, 0, 0);
    }

    private void mouseDrawHandler(MouseEvent me) {
        if (me.getSource() instanceof ShadowPane shadowPane) {
            if (me.isControlDown()) {
                Platform.runLater(() -> {

                    GraphicsContext c = shadowPane.getCanvas().getGraphicsContext2D();
                    if (me.getButton() == MouseButton.MIDDLE) {
                        c.setFill(Color.ALICEBLUE);
                        c.fillRect(0, 0,
                            shadowPane.getCanvas().getWidth(),
                            shadowPane.getCanvas().getHeight());
                    } else if (me.getButton() == MouseButton.SECONDARY) {
                        c.clearRect(0, 0,
                            shadowPane.getCanvas().getWidth(),
                            shadowPane.getCanvas().getHeight());
                    } else {
                        c.setFill(Color.SKYBLUE.deriveColor(1, 1, 1, 0.75));
                        c.fillOval(me.getX() - 5, me.getY() - 5, 10, 10);
                    }
                });
            }
        }

    }

    private void setFillOpacity(Rectangle rectangle, double opacity) {
        Color color = (Color) rectangle.getFill();
        Platform.runLater(() -> {
            rectangle.setFill(color.deriveColor(1, 1, 1, opacity));
        });
    }

    public void setDirty(boolean isDirty) {
        dirty = isDirty;
    }

    public void showAllGridLines(boolean show) {
        showXX1GridLinesGroup(show);
        showXX2GridLinesGroup(show);
        showXY1GridLinesGroup(show);
        showXY2GridLinesGroup(show);

        showYX1GridLinesGroup(show);
        showYX2GridLinesGroup(show);
        showYY1GridLinesGroup(show);
        showYY2GridLinesGroup(show);

        showZX1GridLinesGroup(show);
        showZX2GridLinesGroup(show);
        showZY1GridLinesGroup(show);
        showZY2GridLinesGroup(show);
    }

    public void showDataAndCrosshairsOnly(boolean show) {
        showX1Panel(show);
        showX2Panel(show);
        showY1Panel(show);
        showY2Panel(show);
        showZ1Panel(show);
        showZ2Panel(show);
        x1ShadowPane.showNearsidePoints.set(showNearsidePoints);
        x2ShadowPane.showNearsidePoints.set(showNearsidePoints);
        y1ShadowPane.showNearsidePoints.set(showNearsidePoints);
        y2ShadowPane.showNearsidePoints.set(showNearsidePoints);
        z1ShadowPane.showNearsidePoints.set(showNearsidePoints);
        z2ShadowPane.showNearsidePoints.set(showNearsidePoints);
        getChildren().filtered((Node t) -> t instanceof Sphere)
            .forEach(s -> s.setVisible(show));
        renderingEnabled = show;
        showAllGridLines(show);
        showXAxesGroup(show);
        showYAxesGroup(show);
        showZAxesGroup(show);
    }
}
