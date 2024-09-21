package edu.jhuapl.trinity.javafx.javafx3d;

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

import edu.jhuapl.trinity.data.messages.ChannelFrame;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.HyperspaceEvent;
import edu.jhuapl.trinity.javafx.javafx3d.animated.AnimatedBox;
import edu.jhuapl.trinity.javafx.javafx3d.animated.AnimatedSphere;
import edu.jhuapl.trinity.javafx.javafx3d.animated.AnimatedStack;
import edu.jhuapl.trinity.javafx.javafx3d.animated.AnimatedTetrahedron;
import edu.jhuapl.trinity.javafx.javafx3d.animated.Opticon;
import edu.jhuapl.trinity.javafx.javafx3d.animated.TessellationMesh;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.MessageUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import edu.jhuapl.trinity.utils.WebCamUtils;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Shape3D;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.util.Duration;
import org.fxyz3d.utils.CameraTransformer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javafx.animation.Animation.INDEFINITE;

/**
 * @author Sean Phillips
 */
public class RetroWavePane extends StackPane {

    public static double DEFAULT_INTRO_DISTANCE = -60000.0;
    public static double DEFAULT_ZOOM_TIME_MS = 500.0;
    public PerspectiveCamera camera;
    public CameraTransformer cameraTransform = new CameraTransformer();
    public XFormGroup dataXForm = new XFormGroup();
    private double cameraRX = -1.4;
    private double moonRY = 180;
    private double cameraDistance = -2750;
    private final double sceneWidth = 4000;
    private final double sceneHeight = 4000;

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    public Group sceneRoot = new Group();
    public Group extrasGroup = new Group();
    public Group debugGroup = new Group();
    public SubScene subScene;

    public double point3dSize = 10.0; //size of 3d tetrahedra
    public double pointScale = 1.0; //scales parameter value in transform
    public double scatterBuffScaling = 1.0; //scales domain range in transform
    public long hypersurfaceRefreshRate = 100; //milliseconds
    public int queueLimit = 20000;

    public Color sceneColor = Color.BLACK;
    boolean isDirty = false;
    boolean computeRandos = false;
    boolean animated = false;
    boolean driving = false;
    double deltaZ = 0;
    boolean rawMeshRender = true;
    //allows 2D labels to track their 3D counterparts
    HashMap<Shape3D, Label> shape3DToLabel = new HashMap<>();
    private Random rando = new Random();
    AnimatedSphere retroMoonSphere;
    AnimatedSphere retroSunSphere;
    List<Opticon> opticonList = new ArrayList<>();
    public AnimatedBox retroHighway;
    public AnimatedTetrahedron pyramid;
    AmbientLight ambientLight;
    PointLight pointLight;
    double sunTranslateY = 700;
    private HyperSurfacePlotMesh surfPlot, horizonSurfPlot, horizonSurfPlotTriangles;
    private int xWidth = 200;
    private int zWidth = 200;
    private float yScale = 1.5f;
    private float horizonYScale = -yScale * 10f;
    private float surfScale = 25;
    private float highwayWidth = 200;
    int buildingCount = 30;
    int minStack = 2;
    int maxStack = 6;
    double cityArea = 2000.0;
    float cityBuildingDepth = 150;
    List<Image> tops = null;
    List<Image> tiles = null;

    Function<Vert3D, Number> vert3DLookup = p -> vertToHeight(p);
    Function<Vert3D, Number> horizonLookup = p -> horizonHeight(p);

    List<List<Double>> dataGrid = new ArrayList<>();
    List<List<Double>> horizonGrid = new ArrayList<>();

    // initial rotation
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotateZ = new Rotate(0, Rotate.Z_AXIS);

    private Group nodeGroup = new Group();
    private Group labelGroup = new Group();
    private Group cityGroup = new Group();

    BorderPane bp;
    public Scene scene;
    Timeline celestialTimeline;
    boolean cameraMovementEnabled = false;
    boolean recycling = false;

    int vert;
    org.fxyz3d.geometry.Point3D vertP3D;
    float[] verts = new float[]{0, 0, 0};

    TessellationMesh tessellationMeshView;
    boolean webcamEnabled = false;

    public RetroWavePane(Scene scene, boolean enableWebcam) {
        this.scene = scene;
        this.webcamEnabled = enableWebcam;
        if (enableWebcam) {
            System.out.println("Initializing Surveillance system... ");
            try {
                WebCamUtils.initialize();
                System.out.println("Camera system ONLINE!");
            } catch (Exception ex) {
                System.out.println("Camera system unreachable!");
            }
        }
        setBackground(Background.EMPTY);
        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        subScene.setFill(Color.TRANSPARENT);

        //attach our custom rotation transforms so we can update the labels dynamically
        nodeGroup.getTransforms().addAll(rotateX, rotateY, rotateZ);

        //add our labels to the group that will be added to the StackPane
        labelGroup.getChildren().addAll();
        labelGroup.setManaged(false);

        camera = new PerspectiveCamera(true);

        //setup camera transform for rotational support
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        cameraTransform.rx.setAngle(cameraRX);
        camera.setTranslateZ(cameraDistance);
        debugGroup.setVisible(false);
        //Add 3D subscene stuff to 3D scene root object
        sceneRoot.getChildren().addAll(cameraTransform,
            cityGroup, nodeGroup, extrasGroup, debugGroup, dataXForm);

        subScene.setCamera(camera);
        //add a Point Light for better viewing of the grid coordinate system
        pointLight = new PointLight(Color.WHITE);
        cameraTransform.getChildren().add(pointLight);
        pointLight.setTranslateX(camera.getTranslateX());
        pointLight.setTranslateY(camera.getTranslateY());
        pointLight.setTranslateZ(camera.getTranslateZ() + 500.0);

        //light for everything else
        ambientLight = new AmbientLight(Color.WHITE);
        sceneRoot.getChildren().add(ambientLight);

        //Some camera controls...
        subScene.setOnMouseEntered(event -> subScene.requestFocus());
        setOnMouseEntered(event -> subScene.requestFocus());
        scene.addEventHandler(HyperspaceEvent.REFRESH_RATE_GUI, e -> hypersurfaceRefreshRate = (long) e.object);

        subScene.setOnKeyPressed(event -> {
            //What key did the user press?
            KeyCode keycode = event.getCode();

            if (keycode == KeyCode.L && event.isAltDown()) {
                Opticon opticon = opticonList.get(rando.nextInt(opticonList.size()));
                if (!opticon.isScanning()) {
                    opticonScan(opticon);
                }
            }
            if (keycode == KeyCode.M && event.isAltDown()) {
                cameraMovementEnabled = true;
                moonShot(5000, false);
            }

            if ((keycode == KeyCode.NUMPAD0 && event.isControlDown())
                || (keycode == KeyCode.DIGIT0 && event.isControlDown())) {
                resetView(1000, false);
            } else if ((keycode == KeyCode.NUMPAD0 && event.isShiftDown())
                || (keycode == KeyCode.DIGIT0 && event.isShiftDown())) {
                resetView(0, true);
            }
            double change = 10.0;
            //Add shift modifier to simulate "Running Speed"
            if (event.isShiftDown()) {
                change = 100.0;
            } else {
                change = 10.0;
            }
            if (cameraMovementEnabled) {
                //Zoom controls
                if (keycode == KeyCode.W) {
                    camera.setTranslateZ(camera.getTranslateZ() + change);
                }
                if (keycode == KeyCode.S) {
                    camera.setTranslateZ(camera.getTranslateZ() - change);
                }
                if (keycode == KeyCode.PLUS && event.isShortcutDown()) {
                    camera.setTranslateZ(camera.getTranslateZ() + change);
                }
                if (keycode == KeyCode.MINUS && event.isShortcutDown()) {
                    camera.setTranslateZ(camera.getTranslateZ() - change);
                }
                //Strafe controls
                if (keycode == KeyCode.A) {
                    camera.setTranslateX(camera.getTranslateX() - change);
                }
                if (keycode == KeyCode.D) {
                    camera.setTranslateX(camera.getTranslateX() + change);
                }
                //Strafe controls
                if (keycode == KeyCode.E) {
                    camera.setTranslateY(camera.getTranslateY() - change);
                }
                if (keycode == KeyCode.C) {
                    camera.setTranslateY(camera.getTranslateY() + change);
                }
            }
            updateLabels();
        });

        subScene.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });

        subScene.setOnZoom(e -> {
            if (cameraMovementEnabled) {
                double zoom = e.getZoomFactor();
                if (zoom > 1) {
                    camera.setTranslateZ(camera.getTranslateZ() + 50.0);
                } else {
                    camera.setTranslateZ(camera.getTranslateZ() - 50.0);
                }
                updateLabels();
                //@TODO update surface callouts            radialOverlayPane.updateCallouts(subScene);
                e.consume();
            }
        });
        subScene.setOnScroll((ScrollEvent event) -> {
            if (cameraMovementEnabled) {
                double modifier = 5.0;
                double modifierFactor = 0.1;

                if (event.isControlDown()) {
                    modifier = 1;
                }
                if (event.isShiftDown()) {
                    modifier = 10.0;
                }
                double z = camera.getTranslateZ();
                double newZ = z + event.getDeltaY() * modifierFactor * modifier;
                camera.setTranslateZ(newZ);
                updateLabels();
                //@TODO update surface callouts            radialOverlayPane.updateCallouts(subScene);
                event.consume();
            }
        });
        //Start Tracking mouse movements only when a button is pressed
        subScene.setOnMouseDragged((MouseEvent me) -> {
            if (cameraMovementEnabled)
                mouseDragCamera(me);
        });

        bp = new BorderPane(subScene);
        getChildren().clear();
        getChildren().addAll(bp, labelGroup);

        //load empty surface
        loadSurf3D();
        loadBodies();
        loadHorizon();
        loadCity(buildingCount, minStack, maxStack, cityArea);
        loadOpticons();

        this.scene.addEventHandler(HyperspaceEvent.HYPERSPACE_BACKGROUND_COLOR, e -> {
            Color color = (Color) e.object;
            subScene.setFill(color);
        });

        AnimationTimer surfUpdateAnimationTimer = new AnimationTimer() {
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_MILLI = 1_000_000;

            @Override
            public void handle(long now) {
                sleepNs = hypersurfaceRefreshRate * NANOS_IN_MILLI;
                if ((now - prevTime) < sleepNs) {
                    return;
                }
                prevTime = now;
                if (isVisible()) {
                    if (computeRandos) {
                        generateRandos(xWidth, zWidth, yScale);
                    }
                    if (animated || isDirty) {
                        updateTheMesh();
                    }
                }
            }
        };
        surfUpdateAnimationTimer.start();
        celestialTimeline = new Timeline(
            //15 seconds to descend, the heavens are empty
            new KeyFrame(Duration.seconds(15.0), new KeyValue(retroSunSphere.translateYProperty(), sunTranslateY)),
            new KeyFrame(Duration.seconds(15.0), new KeyValue(retroMoonSphere.translateYProperty(), sunTranslateY)),
            //Ultraviolet dusk
            new KeyFrame(Duration.seconds(15.0), new KeyValue(ambientLight.colorProperty(), Color.DARKVIOLET)),
            new KeyFrame(Duration.seconds(15.0), new KeyValue(horizonSurfPlot.diffuseColorProperty(), Color.CYAN)),
            //periapsis for 15 more seconds
            new KeyFrame(Duration.seconds(30.0), new KeyValue(retroMoonSphere.translateYProperty(), -sunTranslateY)),
            new KeyFrame(Duration.seconds(30.0), new KeyValue(retroSunSphere.translateYProperty(), sunTranslateY)),
            //Night Prowler Black
            new KeyFrame(Duration.seconds(30.0), new KeyValue(ambientLight.colorProperty(), Color.BLACK)),
            //Night Prowler Black
            new KeyFrame(Duration.seconds(30.0), new KeyValue(subScene.fillProperty(), Color.BLACK)),
            //Can't See
            new KeyFrame(Duration.seconds(30.0), new KeyValue(horizonSurfPlot.diffuseColorProperty(), Color.CYAN.deriveColor(1, 1, 1, 0.0))),
            //Can see
            new KeyFrame(Duration.seconds(45.0), new KeyValue(retroMoonSphere.translateYProperty(), sunTranslateY)),
            new KeyFrame(Duration.seconds(45.0), new KeyValue(horizonSurfPlot.diffuseColorProperty(), Color.CYAN)),
            //ascend for 15 seconds
            new KeyFrame(Duration.seconds(45.0), new KeyValue(retroSunSphere.translateYProperty(), -sunTranslateY)),
            //Day Stalker Blue
            new KeyFrame(Duration.seconds(45.0), new KeyValue(ambientLight.colorProperty(), Color.CYAN)),
            //apoapsis for 5 seconds
            new KeyFrame(Duration.seconds(60.0), new KeyValue(retroSunSphere.translateYProperty(), -sunTranslateY)),
            new KeyFrame(Duration.seconds(60.0), new KeyValue(retroMoonSphere.translateYProperty(), sunTranslateY)),
            //Color me blood orange
            new KeyFrame(Duration.seconds(60.0), new KeyValue(ambientLight.colorProperty(), Color.ORANGE.darker())),
            //Night Prowler Black
            new KeyFrame(Duration.seconds(60.0), new KeyValue(subScene.fillProperty(), Color.TRANSPARENT))
        );
        celestialTimeline.setAutoReverse(true);
        celestialTimeline.setCycleCount(INDEFINITE);
        celestialTimeline.setDelay(Duration.seconds(5));
        celestialTimeline.play();

        AnimationTimer opticonAnimationTimer = new AnimationTimer() {
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_SECOND = 1_000_000_000;

            @Override
            public void handle(long now) {
                sleepNs = 10 * NANOS_IN_SECOND;
                if ((now - prevTime) < sleepNs) {
                    return;
                }
                prevTime = now;

                double totalWidth = xWidth * surfScale;
                double totalDepth = zWidth * surfScale;
                double xTranslate = -totalWidth / 2.0;
                double zTranslate = -totalDepth / 2.0;
                Opticon opticon = opticonList.get(rando.nextInt(opticonList.size()));
                if (!opticon.isScanning()) {
                    Point3D shiftedP3D = new Point3D(
                        rando.nextDouble() * totalWidth + xTranslate,
                        -75,
                        rando.nextDouble() * totalDepth + zTranslate);
                    opticon.search(shiftedP3D, Duration.seconds(5));
                }
            }
        };
        opticonAnimationTimer.start();

        AnimationTimer outrunTimer = new AnimationTimer() {
            long sleepNs = 0;
            long prevTime = 0;
            long NANOS_IN_MILLISECOND = 1_000_000;
            double stripDelta = cityBuildingDepth + (cityBuildingDepth * 0.2);

            @Override
            public void handle(long now) {
                sleepNs = 33 * NANOS_IN_MILLISECOND;
                if ((now - prevTime) < sleepNs) {
                    return;
                }
                prevTime = now;

                if (driving) {
                    double horizon = (zWidth * surfScale) / 2.0;
                    //What direction are we going?
                    if (deltaZ >= 0) { //forward
                        //have we begun recycling buildings?
                        if (!recycling) {
                            recycling = cityGroup.getChildren()
                                .filtered(node -> node.getTranslateZ() < cameraDistance)
                                .size() > 0;
                        }
                        cityGroup.getChildren()
                            .filtered(node -> node.getTranslateZ() < cameraDistance)
                            .forEach(n -> n.setTranslateZ((zWidth * surfScale) / 2.0));
                    } else { //backward
                        //cull
                        cityGroup.getChildren()
                            .filtered(node -> node.getTranslateZ() > horizon)
                            .forEach(n -> ambientLight.getScope().remove(n));
                        recycling = cityGroup.getChildren()
                            .removeIf(node -> node.getTranslateZ() > horizon);
                        if (cityGroup.getChildren().isEmpty()) {
                            //reset
                            stripDelta = cityBuildingDepth;
                        }
                    }

                    //translate
                    stripDelta -= deltaZ;
                    if (stripDelta <= 0) {
                        //if we've started recycling buildings, don't recreate them
                        if (!recycling) {
                            if (!tiles.isEmpty() && !tops.isEmpty()) {
                                //generate new strip
                                List<AnimatedStack> strip = genCityStrip(
                                    buildingCount, minStack, maxStack,
                                    (zWidth * surfScale) / 2.0, cityArea);
                                cityGroup.getChildren().addAll(strip);
                            }
                        }
                        //reset
                        stripDelta = cityBuildingDepth;
                    }
                    //make new
                    for (Node node : cityGroup.getChildren()) {
                        node.setTranslateZ(node.getTranslateZ() - deltaZ);
                    }
                }
            }
        };
        outrunTimer.start();

        this.visibleProperty().addListener(cl -> {
            if (this.isVisible()) {
                surfUpdateAnimationTimer.start();
                celestialTimeline.play();
                opticonAnimationTimer.start();
                outrunTimer.start();
            } else {
                surfUpdateAnimationTimer.stop();
                celestialTimeline.pause();
                opticonAnimationTimer.stop();
                outrunTimer.stop();
            }
        });
        retroHighway.setRateOfChange(0.0);
        retroHighway.enableCycle(true);

        Button regenerateHorizon = new Button("Regenerate Horizon");
        regenerateHorizon.setOnAction(e -> regHorizon());
        Button regenerateCity = new Button("Regenerate City");
        regenerateCity.setOnAction(e -> loadCity(buildingCount, minStack, maxStack, cityArea));

        ToggleButton outRun = new ToggleButton("Out Run");
        outRun.setOnAction(e -> driving = outRun.isSelected());
        Spinner txSpinner = new Spinner(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(-500, 500, 0, 1));
        txSpinner.setEditable(true);
        txSpinner.valueProperty().addListener(e -> {
            deltaZ = ((Integer) txSpinner.getValue()).doubleValue();
        });

        HBox hbox = new HBox(10, regenerateHorizon, regenerateCity, outRun, txSpinner);
        StackPane.setAlignment(hbox, Pos.CENTER_LEFT);
        hbox.setPickOnBounds(false);
        getChildren().add(hbox);
        retroHighway.addEventHandler(ScrollEvent.SCROLL, event -> {
            driving = true;
            Integer currentValue = (Integer) txSpinner.getValue();
            //subtract because y deltas are positive down on the screen but
            //we want the affect to be such that scrolling up is positive
            int newValue = currentValue - Double.valueOf(event.getDeltaY()).intValue();
            txSpinner.getValueFactory().setValue(newValue);
            event.consume();
        });
        retroHighway.addEventHandler(ZoomEvent.ZOOM, event -> {
            driving = event.getZoomFactor() > 1;
            if (!driving)
                retroHighway.setRateOfChange(0.0);
            retroHighway.enableCycle(driving);
            event.consume();
        });
    }

    private void updateTheMesh() {
        if (rawMeshRender) {
            surfPlot.updateMeshRaw(xWidth, zWidth, surfScale, yScale, surfScale);
        } else {
            surfPlot.updateMeshSmooth(xWidth, zWidth);
        }
    }

    public void resetView(double milliseconds, boolean rightNow) {
        if (!rightNow) {
            JavaFX3DUtils.transitionCameraTo(milliseconds, camera, cameraTransform,
                0, 0, cameraDistance, cameraRX, 0.0, 0.0);
        } else {
            dataXForm.reset();
        }
    }

    private void moonShot(double milliseconds, boolean rightNow) {
        if (!rightNow) {
            Timeline timeline = new Timeline();
            Timeline creditsTimeline = JavaFX3DUtils.creditsReel(nodeGroup,
                new org.fxyz3d.geometry.Point3D(0, -175, 900));
            creditsTimeline.setOnFinished(ct -> {
                nodeGroup.getScene().getRoot().fireEvent(
                    new CommandTerminalEvent("Thanks!!!",
                        new Font("Consolas", 50), Color.GREEN));
                resetView(1000, false);
            });
            timeline.getKeyFrames().addAll(new KeyFrame[]{
                new KeyFrame(Duration.millis(milliseconds * 0.333), new KeyValue[]{
                    new KeyValue(cameraTransform.rx.angleProperty(), 0, Interpolator.EASE_IN),
                    new KeyValue(cameraTransform.ry.angleProperty(), 0, Interpolator.EASE_IN),
                    new KeyValue(cameraTransform.rz.angleProperty(), 0, Interpolator.EASE_IN),
                    new KeyValue(camera.translateXProperty(), 0, Interpolator.EASE_IN),
                    new KeyValue(camera.translateYProperty(), 0, Interpolator.EASE_IN),
                    new KeyValue(camera.translateZProperty(), 0, Interpolator.EASE_IN)
                })
                , new KeyFrame(Duration.millis(milliseconds), new KeyValue[]{
                new KeyValue(cameraTransform.ry.angleProperty(), 180, Interpolator.EASE_OUT),
                new KeyValue(camera.translateYProperty(), -sunTranslateY / 3.0, Interpolator.EASE_OUT),
                new KeyValue(camera.translateZProperty(), -1200, Interpolator.EASE_OUT)
            })
                , new KeyFrame(Duration.millis(milliseconds * 1.25), kv -> {
                creditsTimeline.playFromStart();
            })
            });
            timeline.playFromStart();
        } else {
            dataXForm.reset();
        }
    }

    public void intro(double milliseconds) {
        camera.setTranslateZ(DEFAULT_INTRO_DISTANCE);
        JavaFX3DUtils.zoomTransition(milliseconds, camera, cameraDistance);
    }

    public void outtro(double milliseconds) {
        JavaFX3DUtils.zoomTransition(milliseconds, camera, DEFAULT_INTRO_DISTANCE);
    }

    private void mouseDragCamera(MouseEvent me) {
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);
        double modifier = 1.0;
        double modifierFactor = 0.1;  //@TODO SMP connect to sensitivity property

        if (me.isControlDown()) {
            modifier = 0.1;
        }
        if (me.isShiftDown()) {
            modifier = 25.0;
        }
        if (me.isPrimaryButtonDown()) {
            if (me.isAltDown()) { //roll
                cameraTransform.rz.setAngle(((cameraTransform.rz.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
            } else {
                cameraTransform.ry.setAngle(((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
                cameraTransform.rx.setAngle(
                    ((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            }
        } else if (me.isMiddleButtonDown()) {
            cameraTransform.t.setX(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3); // -
            cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3); // -
        }
        updateLabels();
//@TODO update surface callouts        radialOverlayPane.updateCallouts(subScene);
    }

    private void updateLabels() {
        shape3DToLabel.forEach((node, label) -> {
            Point3D coordinates = node.localToScene(Point3D.ZERO, true);
            //@DEBUG SMP  useful debugging print
            //System.out.println("subSceneToScene Coordinates: " + coordinates.toString());
            //Clipping Logic
            //if coordinates are outside of the scene it could
            //stretch the screen so don't transform them
            double x = coordinates.getX();
            double y = coordinates.getY();
            //is it left of the view?
            if (x < 0) {
                x = 0;
            }
            //is it right of the view?
            if ((x + label.getWidth() + 5) > subScene.getWidth()) {
                x = subScene.getWidth() - (label.getWidth() + 5);
            }
            //is it above the view?
            if (y < 0) {
                y = 0;
            }
            //is it below the view
            if ((y + label.getHeight()) > subScene.getHeight()) {
                y = subScene.getHeight() - (label.getHeight() + 5);
            }
            //@DEBUG SMP  useful debugging print
            //System.out.println("clipping Coordinates: " + x + ", " + y);
            //update the local transform of the label.
            label.getTransforms().setAll(new Translate(x, y));
        });
    }

    private double generateHorizon(int xWidth, int zWidth, float yScale, double prescaleHeightCap, int numberOfSpikes, int spikeSize, int zSkip) {
        if (null == horizonGrid) {
            horizonGrid = new ArrayList<>(zWidth);
        } else {
            horizonGrid.clear();
        }
        double max = 0;
        List<Double> xList;
        for (int z = 0; z < zWidth; z++) {
            ChannelFrame frame = MessageUtils.buildSpikeyChannelFrame(xWidth, prescaleHeightCap, numberOfSpikes, spikeSize);
            xList = new ArrayList<>(xWidth);
            xList.addAll(frame.getChannelData());
            for (int x = 0; x < xList.size(); x++) {
                xList.set(x, xList.get(x) * yScale);
                //Add a sharp peak
                if (rando.nextDouble() > 0.8)
                    xList.set(x, xList.get(x) - rando.nextDouble() * 5);
                if (Math.abs(xList.get(x)) > max)
                    max = Math.abs(xList.get(x));
            }
            horizonGrid.add(xList);
            for (int skip = 0; skip < zSkip; skip++) {
                z++;
                horizonGrid.add(xList);
            }
        }
        return max;
    }

    private void generateRandos(int xWidth, int zWidth, float yScale) {
        if (null == dataGrid) {
            dataGrid = new ArrayList<>(zWidth);
        } else {
            dataGrid.clear();
        }
        List<Double> xList;
        for (int z = 0; z < zWidth; z++) {
            xList = new ArrayList<>(xWidth);
            for (int x = 0; x < xWidth; x++) {
                xList.add(rando.nextDouble() * yScale);
            }
            dataGrid.add(xList);
        }
    }

    private Number vertToHeight(Vert3D p) {
        if (null != dataGrid) {
            if (rawMeshRender) {
                return lookupPoint(p);
            } else {
                return findBlerpHeight(p);
            }
        } else {
            return 0.0;
        }
    }

    private Number horizonHeight(Vert3D p) {
        if (null != horizonGrid) {
            //hacky bounds check
            if (p.yIndex >= horizonGrid.size() || p.xIndex >= horizonGrid.get(0).size()) {
                return 0.0;
            }
            return horizonGrid.get(p.yIndex).get(p.xIndex);
        } else {
            return 0.0;
        }
    }

    private Number lookupPoint(Vert3D p) {
        //hacky bounds check
        if (p.yIndex >= dataGrid.size()
            || p.xIndex >= dataGrid.get(0).size()) {
            return 0.0;
        }
        return dataGrid.get(p.yIndex).get(p.xIndex);
    }

    private Number findBlerpHeight(Vert3D p) {
        int x1Index = p.xIndex <= 0 ? 0 : p.xIndex - 1;
        if (x1Index >= dataGrid.get(0).size() - 1) {
            x1Index = dataGrid.get(0).size() - 1;
        }

        int x2Index = p.xIndex >= dataGrid.get(0).size() - 1
            ? dataGrid.get(0).size() - 1 : p.xIndex + 1;

        int y1Index = p.yIndex <= 0 ? 0 : p.yIndex - 1;
        if (y1Index >= dataGrid.size() - 1) {
            y1Index = dataGrid.size() - 1;
        }
        int y2Index = p.yIndex >= dataGrid.size() - 1
            ? dataGrid.size() - 1 : p.yIndex + 1;
        //System.out.println("x1,x2,y1,y2:" + x1Index + ", " + x2Index + ", " + y1Index + ", " + y2Index);

        double c11 = dataGrid.get(y1Index).get(x1Index) * yScale;
        double c21 = dataGrid.get(y1Index).get(x2Index) * yScale;
        double c12 = dataGrid.get(y2Index).get(x1Index) * yScale;
        double c22 = dataGrid.get(y2Index).get(x2Index) * yScale;
        //System.out.println("x1,x2,y1,y2:" + x1Index + ", " + x2Index + ", " + y1Index + ", " + y2Index);

        return quickBlerp(c11, c21, c12, c22, p.getX(), p.getY());
    }

    private Number quickBlerp(double f1, double f2, double f3, double f4, double x, double y) {
        double xratio = x - Math.floor(x);
        double yratio = y - Math.floor(y);
        double f12 = f1 + (f2 - f1) * xratio;
        double f34 = f3 + (f4 - f3) * xratio;
        return f12 + (f34 - f12) * yratio;
    }

    private void opticonScan(Opticon opticon) {
        if (null != tessellationMeshView) {
            tessellationMeshView.enableMatrix(false);
            nodeGroup.getChildren().remove(tessellationMeshView);
        }
        Image image = null;
        if (webcamEnabled) {
            try {
                image = WebCamUtils.takePicture();
                System.out.println("got your little soul...");
                tessellationMeshView = new TessellationMesh(image,
                    Color.GREEN, 1f, 100.0f, 2, false);
                ambientLight.getScope().add(tessellationMeshView);
                tessellationMeshView.setOnMouseClicked(click -> {
                    if (click.getClickCount() == 1 && click.isControlDown())
                        tessellationMeshView.enableMatrix(!tessellationMeshView.matrixEnabled);
                    if (click.getClickCount() > 1)
                        tessellationMeshView.setVisible(false);
                });
            } catch (Exception ex) {
                System.out.println("Unable to capture image.");
            }
        }
        final int top = (int) snappedTopInset();
        final int right = (int) snappedRightInset();
        final int bottom = (int) snappedBottomInset();
        final int left = (int) snappedLeftInset();
        final double w = getWidth() - left - right;
        final double h = getHeight() - top - bottom;
        opticon.scanMode(this, camera.getTranslateX(), camera.getTranslateY() - 75,
            camera.getTranslateZ() + 250, w, h);
        if (null != tessellationMeshView) {
            nodeGroup.getChildren().add(tessellationMeshView);
            tessellationMeshView.setRotationAxis(Rotate.X_AXIS);
            tessellationMeshView.setRotate(-90);
            tessellationMeshView.setTranslateZ(camera.getTranslateZ() + 150);
            if (null != image) {
                tessellationMeshView.setTranslateX(-image.getWidth() / 4.0);
                tessellationMeshView.setTranslateY(-image.getHeight() / 4.0);
            }
            pointLight.getScope().add(tessellationMeshView);
            pointLight.setTranslateZ(camera.getTranslateZ());
            Timeline scanTimeline = new Timeline(
                //Synch with Opticon laser sweep that starts at 3.5 seconds into timeline
                new KeyFrame(Duration.seconds(3.25), kv ->
                    tessellationMeshView.animateTessellation(15, 2)),
                new KeyFrame(Duration.seconds(9), kv ->
                    tessellationMeshView.enableMatrix(true)),
                new KeyFrame(Duration.seconds(9.0),
                    new KeyValue(tessellationMeshView.scaleXProperty(), 1.0)),
                new KeyFrame(Duration.seconds(9.0),
                    new KeyValue(tessellationMeshView.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.seconds(9.0),
                    new KeyValue(tessellationMeshView.scaleZProperty(), 1.0)),
                new KeyFrame(Duration.seconds(10.0),
                    new KeyValue(tessellationMeshView.scaleXProperty(), 0.5)),
                new KeyFrame(Duration.seconds(10.0),
                    new KeyValue(tessellationMeshView.scaleYProperty(), 0.5)),
                new KeyFrame(Duration.seconds(10.0),
                    new KeyValue(tessellationMeshView.scaleZProperty(), 0.5))
            );

            scanTimeline.setCycleCount(1);
            scanTimeline.playFromStart();
        }
    }

    private void loadOpticons() {
        double totalWidth = xWidth * surfScale;
        double totalDepth = zWidth * surfScale;
        double xTranslate = -totalWidth / 2.0;
        double zTranslate = -totalDepth / 2.0;
        for (int i = 0; i < 6; i++) {
            Opticon opticon = new Opticon(Color.RED, 20);
            opticon.scannerLight.getScope().clear();
            opticon.setTranslateX(rando.nextDouble() * totalWidth + xTranslate);
            opticon.setTranslateY(-75);
            opticon.setTranslateZ(rando.nextDouble() * totalDepth + zTranslate);
            opticonList.add(opticon);
            opticon.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                opticonScan(opticon);
            });
            // D&D starts
            opticon.setOnDragDetected((MouseEvent event) -> {
                opticon.setMouseTransparent(true);
                opticon.setCursor(Cursor.MOVE);
                opticon.startFullDrag();
                opticon.isPicking = true;
            });

            // D&D ends
            opticon.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
                opticon.setMouseTransparent(false);
                opticon.setCursor(Cursor.DEFAULT);
                opticon.isPicking = false;
                opticon.startled(5, 0.25, 4);
            });
        }
        nodeGroup.getChildren().addAll(opticonList);

        horizonSurfPlotTriangles.addEventHandler(MouseDragEvent.MOUSE_DRAG_OVER, event -> {
            Point3D coords = event.getPickResult().getIntersectedPoint();
            coords = horizonSurfPlotTriangles.localToParent(coords);
            for (Opticon opticon : opticonList) {
                if (opticon.isPicking) {
                    opticon.setTranslateX(coords.getX());
                    //opticon.setTranslateY(coords.getY());
                    opticon.setTranslateZ(coords.getZ());
                }
            }
        });

        retroHighway.addEventHandler(MouseDragEvent.MOUSE_DRAG_OVER, event -> {
            Point3D coords = event.getPickResult().getIntersectedPoint();
            coords = retroHighway.localToParent(coords);
            for (Opticon opticon : opticonList) {
                if (opticon.isPicking) {
                    opticon.setTranslateX(coords.getX());
                    opticon.setTranslateZ(coords.getZ());
                }
            }
        });

        surfPlot.addEventHandler(MouseDragEvent.MOUSE_DRAG_OVER, event -> {
            Point3D coords = event.getPickResult().getIntersectedPoint();
            coords = surfPlot.localToParent(coords);
            for (Opticon opticon : opticonList) {
                if (opticon.isPicking) {
                    opticon.setTranslateX(coords.getX());
                    opticon.setTranslateZ(coords.getZ());
                }
            }
        });

        surfPlot.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            PickResult n = e.getPickResult();
            Point3D p3D = n.getIntersectedPoint();
            double xT = -(xWidth * surfScale) / 2.0;
            double zT = -(zWidth * surfScale) / 2.0;
            Point3D shiftedP3D = new Point3D(p3D.getX() + xT, p3D.getY() - 100, p3D.getZ() + zT);
            //Pick random opticon to search
            opticonList.get(rando.nextInt(opticonList.size())).search(shiftedP3D, Duration.seconds(5));
            e.consume();
        });
    }

    private List<AnimatedStack> genCityStrip(int buildingCount, int stackMin, int stackMax, double tz, double area) {
        List<AnimatedStack> strip = new ArrayList<>();
        for (int i = 0; i < buildingCount; i++) {
            int count = rando.nextInt(stackMax + 1);
            if (count < stackMin)
                count = stackMin;
            AnimatedStack stack = new AnimatedStack(
                tops.get(rando.nextInt(tops.size())),
                tiles.get(rando.nextInt(tiles.size())),
                count,
                100f, 100f, cityBuildingDepth);
            stack.setTranslateY(-50);
            //offset off the highway (no buildings in the road
            double tx = (rando.nextDouble() * area) + highwayWidth * 1.5;
            if (rando.nextBoolean())
                tx *= -1;
            stack.setTranslateX(tx);
            stack.setTranslateZ(tz);
            ambientLight.getScope().add(stack);
            strip.add(stack);
        }
        return strip;
    }

    private void loadCity(int buildingCount, int stackMin, int stackMax, double area) {
        cityGroup.getChildren().clear();
        recycling = false;
        try {
            if (null == tiles || tiles.isEmpty())
                tiles = JavaFX3DUtils.getTiles();
            if (null == tops || tops.isEmpty())
                tops = JavaFX3DUtils.getTops();
            List<AnimatedStack> strip = genCityStrip(buildingCount, stackMin, stackMax, (zWidth * surfScale) / 2.0, area);
            cityGroup.getChildren().addAll(strip);
        } catch (URISyntaxException | IOException ex) {
            Logger.getLogger(RetroWavePane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void loadBodies() {
        pyramid = new AnimatedTetrahedron(300);
        try {
            Image specImage = ResourceUtils.load3DTextureImage("neoncyanpyramid-black");
            Image pyrDiffuseImage = ResourceUtils.load3DTextureImage("darkcyanpyramid");
            pyramid.setMaterial(new PhongMaterial(Color.WHITE, pyrDiffuseImage,
                specImage, specImage, null));
        } catch (IOException ex) {
            Logger.getLogger(RetroWavePane.class.getName()).log(Level.SEVERE, null, ex);
            pyramid.setMaterial(new PhongMaterial(Color.CYAN));
        }
        pyramid.setRotationAxis(Rotate.Z_AXIS);
        pyramid.setRotate(30);
        pyramid.setTranslateY(-50);
        pyramid.setTranslateZ((zWidth * surfScale) / 2.0);
        pyramid.setOnMouseClicked(e -> {
            if (e.getClickCount() > 1)
                loadCity(buildingCount, minStack, maxStack, cityArea);
        });
        ambientLight.getScope().add(pyramid);
        nodeGroup.getChildren().add(pyramid);

        retroHighway = new AnimatedBox(highwayWidth, 10, zWidth * surfScale);
        PhongMaterial highMat;
        Image highDiffuseImage;
        try {
            highDiffuseImage = ResourceUtils.load3DTextureImage("retrowavehighway");
            highMat = new PhongMaterial(Color.WHITE, highDiffuseImage, highDiffuseImage, highDiffuseImage, null);
        } catch (IOException ex) {
            Logger.getLogger(RetroWavePane.class.getName()).log(Level.SEVERE, null, ex);
            highMat = new PhongMaterial(Color.CYAN.deriveColor(1, 1, 1, 0.333));
        }
        retroHighway.setMaterial(highMat);
        retroHighway.setTranslateY(2);
        ambientLight.getScope().add(retroHighway);
        nodeGroup.getChildren().add(retroHighway);

        PhongMaterial sunMat;
        Image sunDiffuseImage;
        try {
            Image specBumpImage = ResourceUtils.load3DTextureImage("milkywaygalaxy");
            sunDiffuseImage = ResourceUtils.load3DTextureImage("retrowaveSun5");
            sunMat = new PhongMaterial(Color.YELLOW, sunDiffuseImage, specBumpImage, specBumpImage, sunDiffuseImage);
        } catch (IOException ex) {
            Logger.getLogger(RetroWavePane.class.getName()).log(Level.SEVERE, null, ex);
            sunMat = new PhongMaterial(Color.ORANGE);
        }
        retroSunSphere = new AnimatedSphere(sunMat, 400, 256, true);
        retroSunSphere.setTranslateZ((surfScale * zWidth) + 600);
        retroSunSphere.setTranslateY(-sunTranslateY);
        //HIDDEN FEATURE EASTER EGG!!
        retroSunSphere.setOnMouseClicked(e -> {
            System.out.println("Touched the Sun...");
            if (e.getClickCount() > 1) {
                resetView(1000, false);
                cameraMovementEnabled = false;
            } else {
                cameraMovementEnabled = true;
            }
            e.consume();
        });
        nodeGroup.getChildren().add(retroSunSphere);
        PhongMaterial mat;
        try {
            Image diffuseImage = ResourceUtils.load3DTextureImage("moonslices");
            Image specBumpImage = ResourceUtils.load3DTextureImage("vaporwave");
            mat = new PhongMaterial(Color.CYAN, diffuseImage, specBumpImage, specBumpImage, null);
        } catch (IOException ex) {
            Logger.getLogger(RetroWavePane.class.getName()).log(Level.SEVERE, null, ex);
            mat = new PhongMaterial(Color.CYAN);
        }
        retroMoonSphere = new AnimatedSphere(mat, 200.0, 64, true);
        retroMoonSphere.setMaterial(mat);
        retroMoonSphere.setTranslateY(sunTranslateY);
        retroMoonSphere.setTranslateZ((surfScale * zWidth) + 600);
        //HIDDEN FEATURE EASTER EGG!!
        retroMoonSphere.setOnMouseClicked(e -> {
            System.out.println("Moon shot...");
            if (e.getClickCount() > 1) {
                resetView(1000, false);
                cameraMovementEnabled = false;
            } else {
                moonShot(5000, false);
                cameraMovementEnabled = true;
            }
            e.consume();
        });


        PhongMaterial moonMat = new PhongMaterial(Color.CYAN.brighter().brighter());
        AnimatedSphere retroMoonMesh = new AnimatedSphere(moonMat, 165.0, 16, true);
        retroMoonMesh.setDrawMode(DrawMode.LINE);
        //retroMoonMesh.setCullFace(CullFace.FRONT);
        retroMoonMesh.setTranslateY(-sunTranslateY / 2.0);
        retroMoonMesh.translateXProperty().bind(retroMoonSphere.translateXProperty());
        retroMoonMesh.translateYProperty().bind(retroMoonSphere.translateYProperty());
        retroMoonMesh.translateZProperty().bind(retroMoonSphere.translateZProperty().subtract(25));

        PointLight moonLight = new PointLight(Color.SLATEBLUE.brighter());
        moonLight.getScope().addAll(retroMoonMesh, retroMoonSphere);
        moonLight.setTranslateY(-sunTranslateY / 2.0);
        moonLight.translateYProperty().bind(retroMoonSphere.translateYProperty());
        moonLight.translateZProperty().bind(retroMoonSphere.translateZProperty().subtract(25));
        nodeGroup.getChildren().addAll(moonLight, retroMoonMesh, retroMoonSphere);
        //@TODO SMP Add to animation timeline
    }

    private void regHorizon() {
        float horizonSurfScale = surfScale;// * 2f;
        int horizonZWidth = 9;
        int numberOfSpikes = 3;
        int zSkip = 1;
        int spikeSize = 30;
        int horizonXWidth = xWidth; // / 3;
        double prescaleCap = 15.0;
        double maxHeight = generateHorizon(horizonXWidth, horizonZWidth, horizonYScale, prescaleCap, numberOfSpikes, spikeSize, zSkip);
        horizonSurfPlot.updateMeshRaw(horizonXWidth, horizonZWidth, horizonSurfScale, horizonYScale, horizonSurfScale);
        horizonSurfPlot.setTranslateY(maxHeight * horizonYScale);
        horizonSurfPlotTriangles.updateMeshRaw(horizonXWidth, horizonZWidth, horizonSurfScale, horizonYScale, horizonSurfScale);
        horizonSurfPlotTriangles.setTranslateY(maxHeight * horizonYScale);
    }

    private void loadHorizon() {
        System.out.println("Rendering 3D Horizon...");
        float horizonSurfScale = surfScale;// * 2f;
        int horizonZWidth = 9;
        int numberOfSpikes = 3;
        int zSkip = 1;
        int spikeSize = 30;
        int horizonXWidth = xWidth; // / 3;
        double prescaleCap = 15.0;
        double maxHeight = generateHorizon(horizonXWidth, horizonZWidth, horizonYScale, prescaleCap, numberOfSpikes, spikeSize, zSkip);
        horizonSurfPlot = new HyperSurfacePlotMesh(horizonXWidth, horizonZWidth,
            64, 64, horizonYScale, horizonSurfScale, horizonLookup);
        horizonSurfPlot.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() > 1) {
                regHorizon();
            }
        });
        PhongMaterial material = new PhongMaterial(Color.CYAN.darker().darker());

        ambientLight.getScope().add(horizonSurfPlot);
        Glow glow = new Glow(0.8);
        horizonSurfPlot.setEffect(glow);
        horizonSurfPlot.setDrawMode(DrawMode.FILL);
        horizonSurfPlot.setCullFace(CullFace.NONE);
        horizonSurfPlot.setMaterial(material);
        nodeGroup.getChildren().add(horizonSurfPlot);
        horizonSurfPlot.setRotationAxis(Rotate.X_AXIS);
        horizonSurfPlot.setRotate(180.0);
        horizonSurfPlot.setTranslateX(-(horizonXWidth * horizonSurfScale) / 2.0);
        horizonSurfPlot.setTranslateY(maxHeight * horizonYScale);
        horizonSurfPlot.setTranslateZ((zWidth * surfScale) / 2.0);

        horizonSurfPlotTriangles = new HyperSurfacePlotMesh(horizonXWidth, horizonZWidth,
            64, 64, horizonYScale, horizonSurfScale, horizonLookup);
        horizonSurfPlotTriangles.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() > 1) {
                regHorizon();
            }
        });
        horizonSurfPlotTriangles.addEventHandler(ZoomEvent.ZOOM, event -> {
            double zf = event.getZoomFactor();
            horizonSurfPlot.scaleHeight(Double.valueOf(zf).floatValue());
            horizonSurfPlotTriangles.scaleHeight(Double.valueOf(zf).floatValue());
            float maxY = horizonSurfPlot.getMaxY();
            horizonSurfPlot.setTranslateY(-maxY);
            horizonSurfPlotTriangles.setTranslateY(-maxY);
            event.consume();
        });
        PhongMaterial material2 = new PhongMaterial(Color.CYAN.brighter());
        AmbientLight fixedambientLight = new AmbientLight(Color.WHITE);
        fixedambientLight.getScope().add(horizonSurfPlotTriangles);
        sceneRoot.getChildren().addAll(fixedambientLight, horizonSurfPlotTriangles);
        horizonSurfPlotTriangles.setEffect(glow);
        horizonSurfPlotTriangles.setDrawMode(DrawMode.LINE);
        horizonSurfPlotTriangles.setCullFace(CullFace.NONE);
        horizonSurfPlotTriangles.setMaterial(material2);
        horizonSurfPlotTriangles.setRotationAxis(Rotate.X_AXIS);
        horizonSurfPlotTriangles.setRotate(180.0);

        horizonSurfPlotTriangles.setTranslateX(-(horizonXWidth * horizonSurfScale) / 2.0);
        horizonSurfPlotTriangles.setTranslateY(maxHeight * horizonYScale);
        horizonSurfPlotTriangles.setTranslateZ((zWidth * surfScale) / 2.0);

        double height = 20;
        Box glowLineBox = new Box(horizonXWidth * horizonSurfScale, height, 5);
        glowLineBox.setMaterial(new PhongMaterial(Color.TOMATO.brighter().deriveColor(1, 1, 1, 0.1)));
        glowLineBox.setDrawMode(DrawMode.FILL);
        glowLineBox.setEffect(glow);
        glowLineBox.setTranslateZ((zWidth * surfScale) / 2.0);
        glowLineBox.setTranslateY(height / 3.0);
        nodeGroup.getChildren().add(glowLineBox);
    }

    private void loadSurf3D() {
        System.out.println("Rendering Surf3D Mesh...");
        generateRandos(xWidth, zWidth, yScale);
        surfPlot = new HyperSurfacePlotMesh(xWidth, zWidth,
            64, 64, yScale, surfScale, vert3DLookup);
        surfPlot.setCullFace(CullFace.NONE);
        PhongMaterial material;
        try {
            Image diffuseImage = ResourceUtils.load3DTextureImage("vaporwave");
            material = new PhongMaterial(Color.NAVY, null, diffuseImage, null, null);
        } catch (IOException ex) {
            Logger.getLogger(RetroWavePane.class.getName()).log(Level.SEVERE, null, ex);
            material = new PhongMaterial(Color.CYAN);
            surfPlot.setDrawMode(DrawMode.LINE);
            surfPlot.setCullFace(CullFace.NONE);
        }
        surfPlot.setMaterial(material);
        surfPlot.setTranslateX(-(xWidth * surfScale) / 2.0);
        surfPlot.setTranslateZ(-(zWidth * surfScale) / 2.0);
        surfPlot.setDrawMode(DrawMode.LINE);

        surfPlot.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            vert = e.getPickResult().getIntersectedFace() / 2;
            vert = vert + (vert / zWidth); //remove accumulating offset
            vertP3D = surfPlot.getVert(vert);
            verts[0] = vertP3D.x;
            verts[1] = vertP3D.y -= 5;
            verts[2] = vertP3D.z;
            surfPlot.setVert(vert, verts);
            e.consume();
        });
        ambientLight.getScope().add(surfPlot);
        nodeGroup.getChildren().add(surfPlot);
    }

    public void clearAll() {
        shape3DToLabel.clear();
    }

    public void animateHide() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(0.1), new KeyValue(opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.2), e -> outtro(1000)),
            new KeyFrame(Duration.seconds(2.0), new KeyValue(opacityProperty(), 0.0)),
            new KeyFrame(Duration.seconds(2.0), e -> setVisible(false))
        );
        timeline.play();
    }

    public void animateShow() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(0.1), e
                -> camera.setTranslateZ(DEFAULT_INTRO_DISTANCE)),
            new KeyFrame(Duration.seconds(0.1), new KeyValue(opacityProperty(), 0.0)),
            new KeyFrame(Duration.seconds(0.3), e -> setVisible(true)),
            new KeyFrame(Duration.seconds(0.3), new KeyValue(opacityProperty(), 1.0)),
            new KeyFrame(Duration.seconds(0.6), e -> intro(1000))
        );
        timeline.play();
    }
}
