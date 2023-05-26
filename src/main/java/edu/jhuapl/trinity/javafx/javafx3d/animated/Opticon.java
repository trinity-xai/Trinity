package edu.jhuapl.trinity.javafx.javafx3d.animated;

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

import edu.jhuapl.trinity.javafx.components.Crosshair;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.javafx3d.RetroWavePane;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
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

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javafx.animation.Animation.INDEFINITE;

/**
 * @author Sean Phillips
 */
public class Opticon extends Group {
    public static double CONE_MESH_ROTATE = -5;
    public static double SCANMODE_X_ANGLE = 30.0;
    public static double SEARCH_DESTROY_X_ANGLE = 0.0;
    //public static double SCANMODE_ANGLE = 30.0;
    public Planetoid mainBody;
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

    public Opticon(Color lightColor, double radius) {
        pointer = new Box(1, 1, radius);
        pointer.setMaterial(new PhongMaterial(Color.TOMATO));
        pointer.setTranslateZ(radius);
        scanColor = lightColor;
        scanConeColor = lightColor.deriveColor(1, 1, 1, 0.1);
        PhongMaterial mat;
        try {
            Image diffuseImage = ResourceUtils.load3DTextureImage("top");
            Image specBumpImage = ResourceUtils.load3DTextureImage("inverseRetroline");
            mat = new PhongMaterial(Color.BLUE, diffuseImage, specBumpImage, specBumpImage, diffuseImage);
        } catch (IOException ex) {
            Logger.getLogger(RetroWavePane.class.getName()).log(Level.SEVERE, null, ex);
            mat = new PhongMaterial(Color.BLUE);
        }
        mainBody = new Planetoid(mat, radius, 32);
        mainBody.setMaterial(mat);
        mainBody.setRotationAxis(Rotate.X_AXIS);
        mainBody.setRotate(SCANMODE_X_ANGLE);

        scannerLight = new SpotLight(scanColor);
        scannerLight.setDirection(new Point3D(0, 1, 0));
        scannerLight.setInnerAngle(120);
        scannerLight.setOuterAngle(30);
        scannerLight.setFalloff(-0.4);
        scannerLight.setTranslateZ(-radius - 2);

        scannerConeMesh = new ConeMesh(8, 2 * radius, 5 * radius);
        scannerConeMesh.setCullFace(CullFace.NONE);
        scannerConeOutlineMesh = new ConeMesh(8, 2 * radius, 5 * radius);
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
            Logger.getLogger(Opticon.class.getName()).log(Level.SEVERE, null, ex);
            scannerConeMesh.setTextureModeNone(scanConeColor);
            scannerConeMesh.setDrawMode(DrawMode.LINE);
        }

        scannerConeMesh.setRotationAxis(Rotate.X_AXIS);
        scannerConeMesh.setRotate(CONE_MESH_ROTATE);
        scannerConeMesh.setTranslateZ(-radius);

        scannerConeOutlineMesh.setRotationAxis(Rotate.X_AXIS);
        scannerConeOutlineMesh.setRotate(CONE_MESH_ROTATE);
        scannerConeOutlineMesh.setTranslateZ(-radius);

        getChildren().addAll(mainBody, pointer, scannerConeMesh, scannerConeOutlineMesh, scannerLight);

        Sphere debugSphere = new Sphere(1);
        debugSphere.setMaterial(new PhongMaterial(Color.WHITE));
        debugSphere.setTranslateZ(-radius);
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

    }

    public void setCycle(double cycleSeconds, double fps) {
        KeyValue start = new KeyValue(keyCycle, 0, Interpolator.LINEAR);
        KeyValue end = new KeyValue(keyCycle, fps * cycleSeconds, Interpolator.LINEAR);
        KeyFrame kf = new KeyFrame(Duration.seconds(cycleSeconds), start, end);
//        KeyFrame cycleFinished = new KeyFrame(Duration.seconds(cycleSeconds), e->{
//
//        });
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
        //@TODO SMP lookup math is wrong
        //lookAt(true, true, currentP3D, searchLocation);

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
//            ,new KeyFrame(Duration.seconds(3.5), kv -> scannerConeMesh.setRotationAxis(Rotate.Y_AXIS))
//            ,new KeyFrame(Duration.seconds(3.5), kv -> scannerMeshTimeline.play())
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
        //@TODO SMP lookup math is wrong
        //lookAt(true, true, currentP3D, searchLocation);

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

    public void lookAt(boolean flipX, boolean flipY, javafx.geometry.Point3D currentPosition, javafx.geometry.Point3D lookAtPos) {
        //Create direction vector
        javafx.geometry.Point3D lookDirection = lookAtPos.subtract(currentPosition.getX(), currentPosition.getY(), currentPosition.getZ());
        lookDirection = lookDirection.normalize();
        //calculate rotation angles
        double xRotation = Math.toDegrees(Math.asin(-lookDirection.getY()));
        if (flipX)
            xRotation *= -1;
        double yRotation = Math.toDegrees(Math.atan2(lookDirection.getZ(), lookDirection.getX()));
        if (flipY)
            yRotation *= -1;
        //make rotation transforms using pivot point of 0,0,0
        Rotate ry = new Rotate(yRotation, 0, 0, 0, Rotate.Y_AXIS);
        Rotate rx = new Rotate(xRotation, 0, 0, 0, Rotate.X_AXIS);
        getTransforms().setAll(ry, rx); //rotate this pig
    }

    public void startled(double intensity, double seconds, int cycles) {
        double originalRotate = getRotate();
        Random rando = new Random();
        double x = getTranslateX() + rando.nextDouble() * intensity;
        double y = getTranslateY() + rando.nextDouble() * intensity;
        double z = getTranslateZ() + rando.nextDouble() * intensity;

        double third = seconds * 0.333;
        double twothird = seconds * 0.666;
        double end = seconds;
        double originalRadius = scannerConeMesh.radiusProperty().get();
        double originalHeight = scannerConeMesh.heightProperty().get();
        int originalDivisions = scannerConeMesh.divisionsProperty().get();

//            ,new KeyFrame(Duration.seconds(third), new KeyValue(scannerConeMesh.radiusProperty(),
//                originalRadius*2))
//            ,new KeyFrame(Duration.seconds(third), new KeyValue(scannerConeMesh.heightProperty(),
//                originalHeight*0.111))
//            ,new KeyFrame(Duration.seconds(third), new KeyValue(scannerConeMesh.divisionsProperty(),
//                originalDivisions*5))

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
        setMouseTransparent(scanning);
    }
}
