package edu.jhuapl.trinity.javafx.javafx3d;

import edu.jhuapl.trinity.javafx.javafx3d.animated.BillBoard;
import edu.jhuapl.trinity.javafx.javafx3d.animated.BillboardNode.BillboardMode;
import edu.jhuapl.trinity.javafx.javafx3d.particle.AgingParticle;
import edu.jhuapl.trinity.javafx.javafx3d.particle.Particle;
import edu.jhuapl.trinity.javafx.javafx3d.particle.SimpleParticleSystem;
import edu.jhuapl.trinity.javafx.javafx3d.particle.Smoke;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import org.fxyz3d.scene.CameraView;
import org.fxyz3d.utils.CameraTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParticleTest extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(ParticleTest.class);
    Box box;
    Sphere xSphere;
    Sphere ySphere;
    Sphere zSphere;
    ImageView pyramidImageView;
    Smoke smoke;
    SimpleParticleSystem particleSystem;

    protected CameraView cameraView;
    PerspectiveCamera camera = new PerspectiveCamera(true);
    BillBoard billboard;
    public Group sceneRoot = new Group();
    public SubScene subScene;
    public CameraTransformer cameraTransform = new CameraTransformer();
    private double cameraDistance = -2000;
    private final double sceneWidth = 10000;
    private final double sceneHeight = 4000;
    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    //**************************************************************************
    private final BooleanProperty useCameraView = new SimpleBooleanProperty(this, "CameraView Enabled", true);
    private final BooleanProperty active = new SimpleBooleanProperty(this, "Billboarding Active"); //Flag for toggling behavior
    private final ObjectProperty<BillboardMode> mode = new SimpleObjectProperty<BillboardMode>(this, "BillBoard Mode", BillboardMode.SPHERICAL) {
        @Override
        protected void invalidated() {
            if (billboard != null) {
                billboard.setBillboardMode(getValue());
                LOG.info("mode changed");
            }
        }

    };

    @Override
    public void start(Stage primaryStage) throws Exception {
        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        subScene.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> subScene.requestFocus());
        subScene.setOnKeyPressed(event -> {
            //What key did the user press?
            KeyCode keycode = event.getCode();
            double change = 10.0;
            //Add shift modifier to simulate "Running Speed"
            if (event.isShiftDown()) {
                change = 100.0;
            }
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
        });

        //Start Tracking mouse movements only when a button is pressed
        subScene.setOnMousePressed((MouseEvent me) -> {
            if (me.isSynthesized())
                LOG.info("isSynthesized");
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
        subScene.setOnMouseDragged((MouseEvent me) -> mouseDragCamera(me));
        subScene.setOnScroll((ScrollEvent event) -> {
            double modifier = 50.0;
            double modifierFactor = 0.1;

            if (event.isControlDown()) {
                modifier = 1;
            }
            if (event.isShiftDown()) {
                modifier = 100.0;
            }
            double z = camera.getTranslateZ();
            double newZ = z + event.getDeltaY() * modifierFactor * modifier;
            camera.setTranslateZ(newZ);
        });
        StackPane subSceneStackPane = new StackPane(subScene);
        subScene.widthProperty().bind(subSceneStackPane.widthProperty());
        subScene.heightProperty().bind(subSceneStackPane.heightProperty());
        subScene.setFill(Color.TRANSPARENT);

        Image image = ResourceUtils.load3DTextureImage("retrowaveSun5");

        billboard = new BillBoard(camera, image);
        billboard.setBillboardMode(mode.get());
        billboard.activeProperty().bind(active);
        active.set(true);

        //setup camera transform for rotational support
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
        subScene.setCamera(camera);

        box = new Box(20, 20, 20);
        box.setMaterial(new PhongMaterial(Color.LIGHTGRAY));
        box.setDrawMode(DrawMode.LINE);

        xSphere = new Sphere(10);
        xSphere.setTranslateX(100);
        xSphere.setMaterial(new PhongMaterial(Color.RED));
        ySphere = new Sphere(10);
        ySphere.setTranslateY(-100);
        ySphere.setMaterial(new PhongMaterial(Color.GREEN));
        zSphere = new Sphere(10);
        zSphere.setTranslateZ(100);
        zSphere.setMaterial(new PhongMaterial(Color.BLUE));


        pyramidImageView = new ImageView(ResourceUtils.load3DTextureImage("carl-b"));
        pyramidImageView.setFitWidth(100);
        pyramidImageView.setSmooth(true);
        pyramidImageView.setPreserveRatio(true);
        pyramidImageView.setDepthTest(DepthTest.ENABLE);
        pyramidImageView.setTranslateZ(-500);

        sceneRoot.getChildren().addAll(cameraTransform,
            box, xSphere, ySphere, zSphere, pyramidImageView);
        BorderPane bpOilSpill = new BorderPane(subSceneStackPane);

        smoke = new Smoke();
        particleSystem = new SimpleParticleSystem(smoke, sceneRoot, 30);

        CheckBox particleTimerCheckBox = new CheckBox("Enable Particle Timer");
        particleTimerCheckBox.setSelected(false);
        particleTimerCheckBox.selectedProperty().addListener(cl -> {
            particleSystem.setEnableParticleTimer(particleTimerCheckBox.isSelected());
        });
        CheckBox spawnParticlesCheckBox = new CheckBox("Enable Spawning Particles");
        spawnParticlesCheckBox.setSelected(false);
        spawnParticlesCheckBox.selectedProperty().addListener(cl -> {
            particleSystem.setRunning(spawnParticlesCheckBox.isSelected());
        });

        CheckBox activeCheckBox = new CheckBox("BillBoard Active");
        activeCheckBox.setSelected(true);
        active.bind(activeCheckBox.selectedProperty());
        ToggleGroup tg = new ToggleGroup();
        RadioButton sphericalRadioButton = new RadioButton("Spherical Mode");
        sphericalRadioButton.setSelected(true);
        sphericalRadioButton.setToggleGroup(tg);
        RadioButton cylinderRadioButton = new RadioButton("Cylinder Mode");
        cylinderRadioButton.setToggleGroup(tg);
        tg.selectedToggleProperty().addListener(cl -> {
            if (sphericalRadioButton.isSelected())
                mode.set(BillboardMode.SPHERICAL);
            else
                mode.set(BillboardMode.CYLINDRICAL);
        });

        CheckBox depthTestCheckBox = new CheckBox("Enable DepthTest");
        depthTestCheckBox.setSelected(true);
        depthTestCheckBox.selectedProperty().addListener(e -> {
            DepthTest dt = depthTestCheckBox.isSelected() ? DepthTest.ENABLE : DepthTest.DISABLE;
            for (Object object : particleSystem.getParticleArray()) {
                ((Particle) object).getNode().setDepthTest(dt);
            }
        });

        ColorPicker sceneFillPicker = new ColorPicker(Color.BLACK);
        subScene.fillProperty().bind(sceneFillPicker.valueProperty());

        Spinner<Double> gravitySpinner = new Spinner(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(-1.0, 1.0, -0.01, 0.005));
//        gravitySpinner.setEditable(true);
        //whenever the spinner value is changed...
        gravitySpinner.valueProperty().addListener(e -> {
            smoke.gravity = gravitySpinner.getValue().floatValue();
            for (Object object : particleSystem.getParticleArray()) {
                AgingParticle p = (AgingParticle) object;
                p.gravity = gravitySpinner.getValue().floatValue();
            }
        });

        VBox vbox = new VBox(10,
            particleTimerCheckBox,
            spawnParticlesCheckBox,
            depthTestCheckBox,
            activeCheckBox,
            new VBox(5, new Label("Scene Fill"), sceneFillPicker),
            new VBox(5, new Label("Gravity"), gravitySpinner)
        );
        ScrollPane scrollPane = new ScrollPane(vbox);
        StackPane.setAlignment(vbox, Pos.BOTTOM_LEFT);
        scrollPane.setPickOnBounds(false);
        scrollPane.setTranslateY(50);
        scrollPane.setMaxSize(300, 300);

        bpOilSpill.setLeft(scrollPane);
        Scene scene = new Scene(bpOilSpill, 1000, 1000, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnMouseEntered(event -> subScene.requestFocus());

        primaryStage.setTitle("Particle Test");
        primaryStage.setScene(scene);
        primaryStage.show();
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}
