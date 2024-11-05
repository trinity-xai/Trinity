/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.javafx3D;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SpotLight;
import javafx.scene.SubScene;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;
import org.fxyz3d.geometry.MathUtils;
import org.fxyz3d.utils.CameraTransformer;

import java.util.ArrayList;
import java.util.List;

public class SpotLightTest extends Application {
    protected Box box;
    PerspectiveCamera camera = new PerspectiveCamera(true);
    public Group sceneRoot = new Group();
    public SubScene subScene;
    public CameraTransformer cameraTransform = new CameraTransformer();
    private double cameraDistance = -4000;
    private final double sceneWidth = 10000;
    private final double sceneHeight = 4000;

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    @Override
    public void start(Stage primaryStage) throws Exception {

        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        //Start Tracking mouse movements only when a button is pressed
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
        StackPane stackPane = new StackPane(subScene);
        subScene.widthProperty().bind(stackPane.widthProperty());
        subScene.heightProperty().bind(stackPane.heightProperty());
        subScene.setFill(Color.LIGHTGREY);

        camera = new PerspectiveCamera(true);
        //setup camera transform for rotational support
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
        cameraTransform.ry.setAngle(-45.0);
        cameraTransform.rx.setAngle(-10.0);
        subScene.setCamera(camera);

        //create multiple spot lights
        List<SpotLight> lights = new ArrayList<>();
        lights.add(new SpotLight(Color.WHITE));
        lights.add(new SpotLight(Color.GREEN));
        lights.add(new SpotLight(Color.RED));
        for (int i = 0; i < lights.size(); i++) {
            SpotLight spotLight = lights.get(i);
            spotLight.setDirection(new Point3D(0, 1, 0));
//            spotLight.setInnerAngle(120);
//            spotLight.setOuterAngle(30);
//            spotLight.setFalloff(-0.4);
            spotLight.setInnerAngle(39);
            spotLight.setOuterAngle(40);
            spotLight.setFalloff(0.01);
            spotLight.setTranslateZ(2000 * i);
            spotLight.setTranslateY(-200);
        }
        SpotLight spotLight1 = lights.get(0);
        SpotLight spotLight2 = lights.get(1);
        SpotLight spotLight3 = lights.get(2);
        // DEBUG CONTROLS //////////////////
        var sliderX = createSlider(-15, 15, spotLight2.directionProperty().get().getX());
        var sliderY = createSlider(-15, 15, spotLight2.directionProperty().get().getY());
        var sliderZ = createSlider(-15, 15, spotLight2.directionProperty().get().getZ());
        spotLight2.directionProperty().bind(Bindings.createObjectBinding(
            () -> new Point3D(sliderX.getValue(), sliderY.getValue(), sliderZ.getValue()),
            sliderX.valueProperty(), sliderY.valueProperty(), sliderZ.valueProperty()));
        var dirX = createSliderControl("dir x", sliderX);
        var dirY = createSliderControl("dir y", sliderY);
        var dirZ = createSliderControl("dir z", sliderZ);

        var inner = createSliderControl("inner angle", spotLight2.innerAngleProperty(), 0, 180, spotLight2.getInnerAngle());
        var outer = createSliderControl("outer angle", spotLight2.outerAngleProperty(), 0, 180, spotLight2.getOuterAngle());
        var falloff = createSliderControl("falloff", spotLight2.falloffProperty(), -10, 10, spotLight2.getFalloff());

        ColorPicker spotLight1Picker = new ColorPicker(spotLight1.getColor());
        spotLight1.colorProperty().bind(spotLight1Picker.valueProperty());

        ColorPicker spotLight2Picker = new ColorPicker(spotLight2.getColor());
        spotLight2.colorProperty().bind(spotLight2Picker.valueProperty());

        ColorPicker spotLight3Picker = new ColorPicker(spotLight3.getColor());
        spotLight3.colorProperty().bind(spotLight3Picker.valueProperty());
        
        ColorPicker surfaceColorPicker = new ColorPicker(Color.CYAN);
        surfaceColorPicker.valueProperty().addListener(cl -> {
            PhongMaterial phong = (PhongMaterial) box.getMaterial();
            phong.setDiffuseColor(surfaceColorPicker.getValue());
        });

        VBox vbox = new VBox(10, 
            dirX, dirY, dirZ, inner, outer, falloff,
            new VBox(5, new Label("Spot Light 1 Color"), spotLight1Picker),
            new VBox(5, new Label("Spot Light 2 Color"), spotLight2Picker),
            new VBox(5, new Label("Spot Light 3 Color"), spotLight3Picker),
            new VBox(5, new Label("Surface Color"), surfaceColorPicker)
                
        );
        ScrollPane scrollPane = new ScrollPane(vbox);
        StackPane.setAlignment(vbox, Pos.BOTTOM_LEFT);
        scrollPane.setPickOnBounds(false);
        scrollPane.setTranslateY(50);
        scrollPane.setMaxSize(400, 800);
        StackPane.setAlignment(scrollPane, Pos.TOP_LEFT);

        box = new Box(sceneWidth, 50, sceneWidth);
        box.setDrawMode(DrawMode.FILL);
        box.setCullFace(CullFace.BACK);
        box.setMaterial(new PhongMaterial(Color.CYAN));

        sceneRoot.getChildren().addAll(cameraTransform, box);
        sceneRoot.getChildren().addAll(lights);

        BorderPane bpOilSpill = new BorderPane(subScene);
        stackPane.getChildren().clear();
        stackPane.getChildren().addAll(bpOilSpill, scrollPane);
        stackPane.setPadding(new Insets(10));
        stackPane.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255), CornerRadii.EMPTY, Insets.EMPTY)));
        Scene scene = new Scene(stackPane, 1000, 1000);
        scene.setOnMouseEntered(event -> subScene.requestFocus());

        primaryStage.setTitle("Multiple SpotLight Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox createSliderControl(String name, DoubleProperty property, double min, double max, double start) {
        var slider = createSlider(min, max, start);
        property.bind(slider.valueProperty());
        return createSliderControl(name, slider);
    }

    private HBox createSliderControl(String name, Slider slider) {
        var tf = createTextField(slider);
        return new HBox(5, new Label(name), slider, tf);
    }

    private TextField createTextField(Slider slider) {
        var tf = new TextField();
        tf.textProperty().bindBidirectional(slider.valueProperty(), new NumberStringConverter());
        tf.setMaxWidth(50);
        return tf;
    }

    private Slider createSlider(double min, double max, double start) {
        var slider = new Slider(min, max, start);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        return slider;
    }

    private void mouseDragCamera(MouseEvent me) {
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);
        double modifier = 1.0;
        double modifierFactor = 0.1;

        if (me.isControlDown()) {
            modifier = 0.1;
        }
        if (me.isShiftDown()) {
            modifier = 10.0;
        }
        if (me.isPrimaryButtonDown()) {
            if (me.isAltDown()) { //roll
                cameraTransform.rz.setAngle(((cameraTransform.rz.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
            } else {
                cameraTransform.ry.setAngle(((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
                cameraTransform.rx.setAngle(
                    MathUtils.clamp(-60,
                        (((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180),
                        60)); // -
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
