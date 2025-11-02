package edu.jhuapl.trinity.javafx.javafx3d.animated;

import edu.jhuapl.trinity.javafx.components.Crosshair;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.utils.DataUtils;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.SpotLight;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;
import org.fxyz3d.shapes.primitives.ConeMesh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;

import static javafx.animation.Animation.INDEFINITE;

/**
 * @author Sean Phillips
 */
public class Opticon extends Group {
    private static final Logger LOG = LoggerFactory.getLogger(Opticon.class);
    public static double CONE_MESH_ROTATE = -5;
    public static double SCANMODE_X_ANGLE = 30.0;
    public static double SEARCH_DESTROY_X_ANGLE = 0.0;
    public AnimatedSphere mainBody;
    Box pointer;
    public SpotLight scannerLight;
    public ConeMesh scannerConeMesh;
    public ConeMesh scannerConeOutlineMesh;
    private TriangleMesh scannerTriangleMesh;

    Timeline searchTimeline, scanTimeline;
    Color scanColor = Color.CYAN;
    Color scanConeColor = Color.CYAN.deriveColor(1, 1, 1, 0.1);
    Color destroyColor = Color.RED;
    Color destroyConeColor = Color.RED.deriveColor(1, 1, 1, 0.1);

    Point3D searchLocation = Point3D.ZERO;
    Point3D viewDirection = Point3D.ZERO;

    Duration SEARCH_DURATION = Duration.seconds(15.0);
    Timeline scannerMeshTimeline;
    Crosshair laserCrosshair;
    private boolean scanning = false;
    public double searchDurationSeconds = 2.0;
    public long wakeupTimeSeconds = 10;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    Rotate rotateX, rotateY;

    public volatile boolean isPicking = false;
    public Point3D vecIni, vecPos;
    public double distance;

    public double DEFAULT_RATEOFCHANGE = 0.1;
    private double rateOfChange = DEFAULT_RATEOFCHANGE;
    public IntegerProperty keyCycle = new SimpleIntegerProperty();
    Timeline tm;
    float[] uvCoords = {
        0f, 0f,
        0.25f, 0.5f,
        0.5f, 0f,
//        0.5f, 1f,
//        0.75f, 0.5f,
//        1f, 0f
    };
    boolean animatingScannerTexture = false;
    public SimpleBooleanProperty orbitingProperty = new SimpleBooleanProperty(false);
    private AnimationTimer orbitAnimationTimer;
    private double totalSceneWidth = 1000;
    private double totalSceneHeight = 1000;
    private double totalSceneDepth = 1000;
    private double scannerBaseRadius = 20;

    public Opticon(Color lightColor, double scannerBaseRadius) {
        this.scannerBaseRadius = scannerBaseRadius;
        pointer = new Box(1, 1, scannerBaseRadius);
        pointer.setMaterial(new PhongMaterial(Color.TOMATO));
        pointer.setTranslateZ(scannerBaseRadius);
        scanColor = lightColor;
        scanConeColor = lightColor.deriveColor(1, 1, 1, 0.1);
        PhongMaterial mat;
        try {
            Image diffuseImage = ResourceUtils.load3DTextureImage("top");
            Image specBumpImage = ResourceUtils.load3DTextureImage("droidBumpNormalMap");
            Image specularImage = ResourceUtils.load3DTextureImage("inverseRetroline");
            mat = new PhongMaterial(Color.BLUE, diffuseImage, specularImage, specBumpImage, diffuseImage);
        } catch (IOException ex) {
            LOG.error(null, ex);
            mat = new PhongMaterial(Color.BLUE);
        }
        mainBody = new AnimatedSphere(mat, scannerBaseRadius, 32, true);
        mainBody.setMaterial(mat);
        mainBody.setRotationAxis(Rotate.X_AXIS);
        mainBody.setRotate(SCANMODE_X_ANGLE);

        scannerLight = new SpotLight(scanColor);
        scannerLight.setDirection(new Point3D(0, 1, 0));
        scannerLight.setInnerAngle(120);
        scannerLight.setOuterAngle(30);
        scannerLight.setFalloff(-0.4);
        scannerLight.setTranslateZ(-scannerBaseRadius - 2);

        scannerConeMesh = new ConeMesh(8, 2 * scannerBaseRadius, 5 * scannerBaseRadius);
        scannerConeMesh.setCullFace(CullFace.NONE);
        scannerConeOutlineMesh = new ConeMesh(8, 2 * scannerBaseRadius, 5 * scannerBaseRadius);
        scannerConeOutlineMesh.setCullFace(CullFace.NONE);
        scannerTriangleMesh = (TriangleMesh) scannerConeMesh.getMesh();

        try {
            Image image = ResourceUtils.load3DTextureImage("neoncyanpyramid-transparent");
            PhongMaterial coneMat = new PhongMaterial(scanConeColor, image, null, null, null);
            scannerConeMesh.setMaterial(coneMat);
            scannerConeMesh.setDrawMode(DrawMode.FILL);
            scannerTriangleMesh.getPoints();
            scannerTriangleMesh.getTexCoords().clear();
            scannerTriangleMesh.getTexCoords().setAll(uvCoords);

            PhongMaterial outlineMat = new PhongMaterial(scanConeColor, null, null, null, null);
            scannerConeOutlineMesh.setMaterial(outlineMat);
            scannerConeOutlineMesh.setDrawMode(DrawMode.LINE);

        } catch (IOException ex) {
            LOG.error(null, ex);
            scannerConeMesh.setTextureModeNone(scanConeColor);
            scannerConeMesh.setDrawMode(DrawMode.LINE);
        }

        scannerConeMesh.setRotationAxis(Rotate.X_AXIS);
        scannerConeMesh.setRotate(CONE_MESH_ROTATE);
        scannerConeMesh.setTranslateZ(-scannerBaseRadius);

        scannerConeOutlineMesh.setRotationAxis(Rotate.X_AXIS);
        scannerConeOutlineMesh.setRotate(CONE_MESH_ROTATE);
        scannerConeOutlineMesh.setTranslateZ(-scannerBaseRadius);

        getChildren().addAll(mainBody, pointer, scannerConeMesh, scannerConeOutlineMesh, scannerLight);

        Sphere debugSphere = new Sphere(1);
        debugSphere.setMaterial(new PhongMaterial(Color.WHITE));
        debugSphere.setTranslateZ(-scannerBaseRadius);
        scannerLight.getScope().add(debugSphere);
        getChildren().addAll(debugSphere);

        scannerConeMesh.setRotationAxis(Rotate.Y_AXIS);
        scannerConeOutlineMesh.rotationAxisProperty().bind(scannerConeMesh.rotationAxisProperty());
        scannerConeOutlineMesh.rotateProperty().bind(scannerConeMesh.rotateProperty());

        scannerMeshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(5.0), new KeyValue(scannerConeMesh.rotateProperty(), 180)),
            new KeyFrame(Duration.seconds(7.5), new KeyValue(scannerConeMesh.rotateProperty(), 359.9)),
            new KeyFrame(Duration.seconds(7.6), new KeyValue(scannerConeMesh.rotateProperty(), 0)),
            new KeyFrame(Duration.seconds(10.0), new KeyValue(scannerConeMesh.rotateProperty(), 180)),
            new KeyFrame(Duration.seconds(15.0), new KeyValue(scannerConeMesh.rotateProperty(), 359.9))
        );
        scannerMeshTimeline.setCycleCount(INDEFINITE);
        Random rando = new Random();
        scannerMeshTimeline.setDelay(Duration.seconds(rando.nextDouble() * 4));
        scannerMeshTimeline.play();

        rotateY = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
        rotateX = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
        setCycle(20, 30);
        keyCycle.addListener(e -> {
            float add = keyCycle.getValue() / 30000f;
            //i=0;i+=2 is right to left
            //i=1;i+=2 is bottom to top
            for (int i = 1; i < uvCoords.length; i += 2) {
                uvCoords[i] -= rateOfChange;
            }
            scannerTriangleMesh.getTexCoords().set(0, uvCoords, 0, uvCoords.length);
        });
        scannerConeMesh.setOnScroll(e -> {
            setRateOfChange(rateOfChange - e.getDeltaY() / 1000f);
        });
        scannerConeMesh.setOnMouseClicked(eh -> {
            animatingScannerTexture = !animatingScannerTexture;
            enableCycle(animatingScannerTexture);
        });
        scannerConeOutlineMesh.setMouseTransparent(true);
        scannerConeOutlineMesh.radiusProperty().bind(scannerConeMesh.radiusProperty());
        scannerConeOutlineMesh.divisionsProperty().bind(scannerConeMesh.divisionsProperty());
        scannerConeOutlineMesh.heightProperty().bind(scannerConeMesh.heightProperty());

        orbitAnimationTimer = new AnimationTimer() {
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_SECOND = 1_000_000_000;
            Random rando = new Random();

            @Override
            public void handle(long now) {
                //wake up and change position time
//                sleepNs = 10 * NANOS_IN_SECOND;
                sleepNs = wakeupTimeSeconds * NANOS_IN_SECOND;

                if ((now - prevTime) < sleepNs) {
                    return;
                }
                prevTime = now;
                if (!orbitingProperty.get())
                    return;

//                double yTranslate = -getTotalSceneWidth() -
//                    rando.nextDouble() * 200;
                double yTranslate = DataUtils.randomSign() *
                    rando.nextDouble() * getTotalSceneHeight() * 0.9;
                double xTranslate = DataUtils.randomSign() *
                    rando.nextDouble() * getTotalSceneWidth() * 0.9;
                double zTranslate = DataUtils.randomSign() *
                    rando.nextDouble() * getTotalSceneDepth() * 0.9;

                Point3D shiftedP3D = new Point3D(
                    xTranslate, yTranslate, zTranslate);

                mainBody.setAnimateOnHover(true);
//                updateScannerSize(getScannerBaseRadius() +
//                    rando.nextDouble() * 200);
                //make sure the duration is less than the wakeup time above
                search(shiftedP3D, Duration.seconds(searchDurationSeconds));
            }
        };
    }

    public void fireData(Group parent, Point3D destination, double milliseconds, Color dataColor) {
        Point3D sceneToLocalPoint = this.sceneToLocal(destination);
        Sphere dataSphere = new Sphere(10);
        dataSphere.setMaterial(new PhongMaterial(dataColor));

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().addAll(new KeyFrame(Duration.millis(milliseconds), // Frame End
            new KeyValue(dataSphere.translateXProperty(), sceneToLocalPoint.getX(), Interpolator.EASE_OUT),
            new KeyValue(dataSphere.translateYProperty(), sceneToLocalPoint.getY(), Interpolator.EASE_OUT),
            new KeyValue(dataSphere.translateZProperty(), sceneToLocalPoint.getZ(), Interpolator.EASE_OUT)));
        timeline.setOnFinished(e -> {
            parent.getChildren().remove(dataSphere);
        });
        parent.getChildren().add(dataSphere);
        timeline.playFromStart();
    }

    public void fireData(Point3D destination, double milliseconds, Color dataColor) {
        fireData(this, destination, milliseconds, dataColor);
    }

    public void updateScannerSize(double radius) {
        scannerConeMesh.setHeight(2 * getScannerBaseRadius());
        scannerConeMesh.setRadius(5 * getScannerBaseRadius());
    }

    public void enableOrbiting(boolean enabled) {
        orbitingProperty.set(enabled);
        if (enabled)
            orbitAnimationTimer.start();
        else
            orbitAnimationTimer.stop();
    }

    public void setCycle(double cycleSeconds, double fps) {
        KeyValue start = new KeyValue(keyCycle, 0, Interpolator.LINEAR);
        KeyValue end = new KeyValue(keyCycle, fps * cycleSeconds, Interpolator.LINEAR);
        KeyFrame kf = new KeyFrame(Duration.seconds(cycleSeconds), start, end);
        tm = new Timeline(kf);
        tm.setCycleCount(INDEFINITE);
    }

    public void enableCycle(boolean enable) {
        if (enable)
            tm.play();
        else
            tm.stop();
    }

    /**
     * @return the rateOfChange
     */
    public synchronized double getRateOfChange() {
        return rateOfChange;
    }

    /**
     * @param rateOfChange the rateOfChange to set
     */
    public synchronized void setRateOfChange(double rateOfChange) {
        this.rateOfChange = rateOfChange;
    }

    private void mouseDrag(MouseEvent me) {
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);
        double modifier = 1.0;
        double modifierFactor = 0.1;  //@TODO SMP connect to sensitivity property

        if (me.isPrimaryButtonDown()) {
            rotateY.setAngle(((rotateY.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
            rotateX.setAngle(
                ((rotateX.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
        }
    }

    public void scanMode(Pane parentPane,
                         double xPosition, double yPosition, double zDistance,
                         double width, double height) {
        setScanning(true);
        final Point3D currentP3D = new Point3D(getTranslateX(), getTranslateY(), getTranslateZ());
        final double originalRadius = scannerConeMesh.radiusProperty().get();
        //@TODO SMP need a way to animate the lookAt so the rotation isn't jarring
        //@TODO SMP also need to have the option to flatten one of the planes of rotation
        //JavaFX3DUtils.lookAt(this, currentP3D, searchLocation, false);

        if (null != scanTimeline) {
            scanTimeline.stop();
        }

        scanTimeline = new Timeline(
            new KeyFrame(Duration.seconds(3), new KeyValue(translateZProperty(), zDistance))
            , new KeyFrame(Duration.seconds(3), new KeyValue(translateXProperty(), xPosition))
            , new KeyFrame(Duration.seconds(3), new KeyValue(translateYProperty(), yPosition))

            , new KeyFrame(Duration.seconds(3), kv -> scannerMeshTimeline.pause())
            , new KeyFrame(Duration.seconds(3.1), kv -> scannerConeMesh.setRotationAxis(Rotate.X_AXIS))
            , new KeyFrame(Duration.seconds(3.1), kv -> scannerConeMesh.setRotate(-90))
            , new KeyFrame(Duration.seconds(3.1), new KeyValue(scannerConeMesh.radiusProperty(), originalRadius / 4))
            , new KeyFrame(Duration.seconds(3.5), kv -> laserSweep(parentPane, 4.0, width, height))
            , new KeyFrame(Duration.seconds(4), new KeyValue(scannerConeMesh.radiusProperty(), height / 8))
            , new KeyFrame(Duration.seconds(5), new KeyValue(scannerConeMesh.radiusProperty(), height / 8))
            , new KeyFrame(Duration.seconds(7.9), new KeyValue(scannerConeMesh.radiusProperty(), 5))
            , new KeyFrame(Duration.seconds(8), new KeyValue(translateZProperty(), zDistance))
            , new KeyFrame(Duration.seconds(8), new KeyValue(translateXProperty(), xPosition))
            , new KeyFrame(Duration.seconds(8), new KeyValue(translateYProperty(), yPosition))

            , new KeyFrame(Duration.seconds(8.1), kv -> scannerConeMesh.setRotationAxis(Rotate.X_AXIS))
            , new KeyFrame(Duration.seconds(8.1), kv -> scannerConeMesh.setRotate(CONE_MESH_ROTATE))
            , new KeyFrame(Duration.seconds(8.5), kv -> scannerConeMesh.setRotationAxis(Rotate.Y_AXIS))
            , new KeyFrame(Duration.seconds(8.5), kv -> scannerMeshTimeline.play())

            , new KeyFrame(Duration.seconds(12), new KeyValue(scannerConeMesh.radiusProperty(), originalRadius))
            , new KeyFrame(Duration.seconds(12), new KeyValue(translateXProperty(), currentP3D.getX()))
            , new KeyFrame(Duration.seconds(12), new KeyValue(translateYProperty(), currentP3D.getY()))
            , new KeyFrame(Duration.seconds(12), new KeyValue(translateZProperty(), currentP3D.getZ()))
        );
        scanTimeline.setCycleCount(1);
        scanTimeline.setOnFinished(e -> {
            setScanning(false);
            scannerConeMesh.setVisible(true);
        });
        scanTimeline.play();
    }

    private void laserSweep(Pane parent, double timeSeconds, double width, double height) {
        laserCrosshair = new Crosshair(parent);
        laserCrosshair.setManaged(false);
        parent.getChildren().add(laserCrosshair);
        laserCrosshair.mouseEnabled = false;

        Color startColor = Color.RED.deriveColor(1, 1, 1, 0.8);
        Color throbColor = Color.CYAN.deriveColor(1, 1, 1, 0.8);

        laserCrosshair.topStrokePaint.set(startColor);
        laserCrosshair.bottomStrokePaint.set(startColor);
        laserCrosshair.rightStrokePaint.set(startColor);
        laserCrosshair.leftStrokePaint.set(startColor);

        laserCrosshair.leftHorizontalLine.setStrokeWidth(3);
        laserCrosshair.rightHorizontalLine.setStrokeWidth(3);
        laserCrosshair.bottomVerticalLine.setStrokeWidth(3);
        laserCrosshair.topVerticalLine.setStrokeWidth(3);

        Duration duration = Duration.seconds(timeSeconds);

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(0.1), e -> laserCrosshair.setVisible(true)),
            new KeyFrame(Duration.seconds(0.2), e -> laserCrosshair.setCenter(0, 0)),
            new KeyFrame(Duration.seconds(timeSeconds * 0.25),
                new KeyValue(laserCrosshair.leftStrokePaint, throbColor)),
            new KeyFrame(Duration.seconds(timeSeconds * 0.5),
                new KeyValue(laserCrosshair.leftStrokePaint, startColor)),
            new KeyFrame(Duration.seconds(timeSeconds * 0.75),
                new KeyValue(laserCrosshair.leftStrokePaint, throbColor)),
            new KeyFrame(Duration.seconds(timeSeconds),
                new KeyValue(laserCrosshair.leftStrokePaint, startColor)),

            new KeyFrame(Duration.seconds(timeSeconds * 0.25),
                new KeyValue(laserCrosshair.rightStrokePaint, throbColor)),
            new KeyFrame(Duration.seconds(timeSeconds * 0.5),
                new KeyValue(laserCrosshair.rightStrokePaint, startColor)),
            new KeyFrame(Duration.seconds(timeSeconds * 0.75),
                new KeyValue(laserCrosshair.rightStrokePaint, throbColor)),
            new KeyFrame(Duration.seconds(timeSeconds),
                new KeyValue(laserCrosshair.rightStrokePaint, startColor)),

            new KeyFrame(Duration.seconds(timeSeconds * 0.25),
                new KeyValue(laserCrosshair.topStrokePaint, throbColor)),
            new KeyFrame(Duration.seconds(timeSeconds * 0.5),
                new KeyValue(laserCrosshair.topStrokePaint, startColor)),
            new KeyFrame(Duration.seconds(timeSeconds * 0.75),
                new KeyValue(laserCrosshair.topStrokePaint, throbColor)),
            new KeyFrame(Duration.seconds(timeSeconds),
                new KeyValue(laserCrosshair.topStrokePaint, startColor)),

            new KeyFrame(Duration.seconds(timeSeconds * 0.25),
                new KeyValue(laserCrosshair.bottomStrokePaint, throbColor)),
            new KeyFrame(Duration.seconds(timeSeconds * 0.5),
                new KeyValue(laserCrosshair.bottomStrokePaint, startColor)),
            new KeyFrame(Duration.seconds(timeSeconds * 0.75),
                new KeyValue(laserCrosshair.bottomStrokePaint, throbColor)),
            new KeyFrame(Duration.seconds(timeSeconds),
                new KeyValue(laserCrosshair.bottomStrokePaint, startColor)),

            new KeyFrame(duration, new KeyValue(laserCrosshair.leftHorizontalLine.startXProperty(), width)),
            new KeyFrame(duration, new KeyValue(laserCrosshair.leftHorizontalLine.startYProperty(), height)),
            new KeyFrame(duration, new KeyValue(laserCrosshair.leftHorizontalLine.endYProperty(), height)),

            new KeyFrame(duration, new KeyValue(laserCrosshair.topVerticalLine.startXProperty(), width)),
            new KeyFrame(duration, new KeyValue(laserCrosshair.topVerticalLine.endXProperty(), width)),
            new KeyFrame(duration, new KeyValue(laserCrosshair.topVerticalLine.startYProperty(), height)),

            new KeyFrame(duration, new KeyValue(laserCrosshair.rightHorizontalLine.startXProperty(), width)),
            new KeyFrame(duration, new KeyValue(laserCrosshair.rightHorizontalLine.startYProperty(), height)),
            new KeyFrame(duration, new KeyValue(laserCrosshair.rightHorizontalLine.endYProperty(), height)),

            new KeyFrame(duration, new KeyValue(laserCrosshair.bottomVerticalLine.startXProperty(), width)),
            new KeyFrame(duration, new KeyValue(laserCrosshair.bottomVerticalLine.endXProperty(), width)),
            new KeyFrame(duration, new KeyValue(laserCrosshair.bottomVerticalLine.startYProperty(), height))

        );
        timeline.play();
        timeline.setOnFinished(e -> {
            laserCrosshair.getScene().getRoot().fireEvent(new CommandTerminalEvent(
                "User Scan Complete.", new Font("Consolas", 30), Color.GREEN));
            laserCrosshair.setVisible(false);
            parent.getChildren().remove(laserCrosshair);
        });
    }

    public void search(Point3D searchLocation, Duration searchDuration) {
        this.searchLocation = searchLocation;
        Point3D currentP3D = new Point3D(getTranslateX(), getTranslateY(), getTranslateZ());
        //@TODO SMP need a way to animate the lookAt so the rotation isn't jarring
        JavaFX3DUtils.lookAt(this, searchLocation, currentP3D, false, true);

        if (null != searchTimeline) {
            searchTimeline.stop();
        }
        double originalRadius = scannerConeMesh.radiusProperty().get();
        double originalHeight = scannerConeMesh.heightProperty().get();

        searchTimeline = new Timeline(
            new KeyFrame(Duration.seconds(0.3), new KeyValue(scannerConeMesh.radiusProperty(), 1))
            , new KeyFrame(Duration.seconds(0.3), new KeyValue(scannerConeMesh.heightProperty(), 2))

            , new KeyFrame(searchDuration, new KeyValue(translateXProperty(), this.searchLocation.getX()))
            , new KeyFrame(searchDuration, new KeyValue(translateYProperty(), this.searchLocation.getY()))
            , new KeyFrame(searchDuration, new KeyValue(translateZProperty(), this.searchLocation.getZ()))

            , new KeyFrame(searchDuration, new KeyValue(scannerConeMesh.radiusProperty(), 1))
            , new KeyFrame(searchDuration, new KeyValue(scannerConeMesh.heightProperty(), 2))

            , new KeyFrame(searchDuration, new KeyValue(scannerConeMesh.heightProperty(), 2))
            , new KeyFrame(searchDuration.add(Duration.seconds(0.5)), new KeyValue(scannerConeMesh.heightProperty(), originalHeight))

            , new KeyFrame(searchDuration.add(Duration.seconds(0.5)), new KeyValue(scannerConeMesh.radiusProperty(), 1))
            , new KeyFrame(searchDuration.add(Duration.seconds(1)), new KeyValue(scannerConeMesh.radiusProperty(), originalRadius))

        );

        searchTimeline.setCycleCount(1);
        searchTimeline.setDelay(Duration.seconds(1));
        searchTimeline.play();
    }

    public void startled(double intensity, double seconds, int cycles) {
        double originalRotate = getRotate();
        Random rando = new Random();
        double x = getTranslateX() + rando.nextDouble() * intensity;
        double y = getTranslateY() + rando.nextDouble() * intensity;
        double z = getTranslateZ() + rando.nextDouble() * intensity;

        double third = seconds * 0.333;
        double twothird = seconds * 0.666;

        scannerConeMesh.setRadius(2);
        scannerConeMesh.setHeight(2);

        Timeline startledTimeline = new Timeline(
            new KeyFrame(Duration.seconds(third), new KeyValue(translateXProperty(), x))
            , new KeyFrame(Duration.seconds(third), new KeyValue(translateYProperty(), y))
            , new KeyFrame(Duration.seconds(third), new KeyValue(translateZProperty(), z))

            , new KeyFrame(Duration.seconds(twothird), new KeyValue(translateXProperty(),
            getTranslateX() + rando.nextDouble() * intensity))
            , new KeyFrame(Duration.seconds(twothird), new KeyValue(translateYProperty(),
            getTranslateY() + rando.nextDouble() * intensity))
            , new KeyFrame(Duration.seconds(twothird), new KeyValue(translateZProperty(),
            getTranslateZ() + rando.nextDouble() * intensity))

            , new KeyFrame(Duration.seconds(seconds), new KeyValue(translateXProperty(),
            getTranslateX() + rando.nextDouble() * intensity))
            , new KeyFrame(Duration.seconds(seconds), new KeyValue(translateYProperty(),
            getTranslateY() + rando.nextDouble() * intensity))
            , new KeyFrame(Duration.seconds(seconds), new KeyValue(translateZProperty(),
            getTranslateZ() + rando.nextDouble() * intensity))

        );
        startledTimeline.setAutoReverse(true);
        startledTimeline.setCycleCount(cycles);
        startledTimeline.setOnFinished(eh -> setRotate(originalRotate));
        startledTimeline.play();
    }

    /**
     * @return the scanning
     */
    public boolean isScanning() {
        return scanning;
    }

    /**
     * @param scanning the scanning to set
     */
    public void setScanning(boolean scanning) {
        this.scanning = scanning;
        scannerConeMesh.setVisible(scanning);
        scannerConeOutlineMesh.setVisible(scanning);
        if (scanning)
            scannerMeshTimeline.playFromStart();
        else
            scannerMeshTimeline.stop();
        setMouseTransparent(scanning);
    }

    /**
     * @return the totalSceneWidth
     */
    public double getTotalSceneWidth() {
        return totalSceneWidth;
    }

    /**
     * @param totalSceneWidth the totalSceneWidth to set
     */
    public void setTotalSceneWidth(double totalSceneWidth) {
        this.totalSceneWidth = totalSceneWidth;
    }

    /**
     * @return the totalSceneHeight
     */
    public double getTotalSceneHeight() {
        return totalSceneHeight;
    }

    /**
     * @param totalSceneHeight the totalSceneHeight to set
     */
    public void setTotalSceneHeight(double totalSceneHeight) {
        this.totalSceneHeight = totalSceneHeight;
    }

    /**
     * @return the totalSceneDepth
     */
    public double getTotalSceneDepth() {
        return totalSceneDepth;
    }

    /**
     * @param totalSceneDepth the totalSceneDepth to set
     */
    public void setTotalSceneDepth(double totalSceneDepth) {
        this.totalSceneDepth = totalSceneDepth;
    }

    /**
     * @return the scannerBaseRadius
     */
    public double getScannerBaseRadius() {
        return scannerBaseRadius;
    }

    /**
     * @param scannerBaseRadius the scannerBaseRadius to set
     */
    public void setScannerBaseRadius(double scannerBaseRadius) {
        this.scannerBaseRadius = scannerBaseRadius;
    }
}
