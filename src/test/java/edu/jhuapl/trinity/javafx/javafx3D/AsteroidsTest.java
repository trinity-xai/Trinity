/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.javafx3d;

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.data.messages.ManifoldData;
import edu.jhuapl.trinity.data.messages.P3D;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.javafx3d.AsteroidFieldPane;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import edu.jhuapl.trinity.javafx.renderers.ManifoldRenderer;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.ParallelTransition;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.fxyz3d.utils.CameraTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.SpotLight;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.util.converter.NumberStringConverter;

public class AsteroidsTest extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(AsteroidsTest.class);
    private final double sceneWidth = 2000;
    private final double sceneHeight = 2000;
    AsteroidFieldPane asteroidsPane;
    Scene scene;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        //stackPane.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255), CornerRadii.EMPTY, Insets.EMPTY)));
        BorderPane bpOilSpillNeverForget = new BorderPane();
        scene = new Scene(bpOilSpillNeverForget, 1000, 1000);
        asteroidsPane = new AsteroidFieldPane(scene);
        bpOilSpillNeverForget.setCenter(asteroidsPane);
        scene.addEventHandler(DragEvent.DRAG_OVER, event -> {
            if (ResourceUtils.canDragOver(event))
                event.acceptTransferModes(TransferMode.COPY);
            else
                event.consume();
        });
        scene.addEventHandler(DragEvent.DRAG_DROPPED, event -> {
            ResourceUtils.onDragDropped(event, scene);
        });

        scene.getRoot().addEventHandler(ManifoldEvent.NEW_MANIFOLD_DATA, e-> {
            handleNewManifoldData(e);
        });

        SpotLight spotLight = new SpotLight(Color.GREEN);
        asteroidsPane.sceneRoot.getChildren().addAll(spotLight);

        spotLight.setDirection(new Point3D(0, 0, 1));
//            spotLight.setInnerAngle(120);
//            spotLight.setOuterAngle(30);
//            spotLight.setFalloff(-0.4);
        spotLight.setInnerAngle(39);
        spotLight.setOuterAngle(40);
        spotLight.setFalloff(0.01);
        spotLight.setTranslateZ(-2000);
//        spotLight.setTranslateY(-200);

        // DEBUG CONTROLS //////////////////
        var sliderX = createSlider(-15, 15, spotLight.directionProperty().get().getX());
        var sliderY = createSlider(-15, 15, spotLight.directionProperty().get().getY());
        var sliderZ = createSlider(-15, 15, spotLight.directionProperty().get().getZ());
        spotLight.directionProperty().bind(Bindings.createObjectBinding(
            () -> new Point3D(sliderX.getValue(), sliderY.getValue(), sliderZ.getValue()),
            sliderX.valueProperty(), sliderY.valueProperty(), sliderZ.valueProperty()));
        var dirX = createSliderControl("dir x", sliderX);
        var dirY = createSliderControl("dir y", sliderY);
        var dirZ = createSliderControl("dir z", sliderZ);

        var sliderXpos = createSlider(-2000, 2000, spotLight.translateXProperty().get());
        var sliderYpos = createSlider(-2000, 2000, spotLight.translateYProperty().get());
        var sliderZpos = createSlider(-2000, 2000, spotLight.translateZProperty().get());
        spotLight.translateXProperty().bind(sliderXpos.valueProperty());
        spotLight.translateYProperty().bind(sliderYpos.valueProperty());
        spotLight.translateZProperty().bind(sliderZpos.valueProperty());
        var posX = createSliderControl("pos x", sliderXpos);
        var posY = createSliderControl("pos y", sliderYpos);
        var posZ = createSliderControl("pos z", sliderZpos);
        
        
        var inner = createSliderControl("inner angle", spotLight.innerAngleProperty(), 0, 180, spotLight.getInnerAngle());
        var outer = createSliderControl("outer angle", spotLight.outerAngleProperty(), 0, 180, spotLight.getOuterAngle());
        var falloff = createSliderControl("falloff", spotLight.falloffProperty(), -10, 10, spotLight.getFalloff());

        ColorPicker spotLight1Picker = new ColorPicker(spotLight.getColor());
        spotLight.colorProperty().bind(spotLight1Picker.valueProperty());

        ColorPicker surfaceColorPicker = new ColorPicker(Color.CYAN);
        surfaceColorPicker.valueProperty().addListener(cl -> {
            PhongMaterial phong = (PhongMaterial) asteroidsPane.centralBody.getMaterial();
            phong.setDiffuseColor(surfaceColorPicker.getValue());
        });

        CheckBox ambientLightCheckBox = new CheckBox("Enable Ambient Light");
        ambientLightCheckBox.setSelected(true);
        asteroidsPane.ambientLight.disableProperty().bind(
            ambientLightCheckBox.selectedProperty().not());
  
        ColorPicker ambientColorPicker = new ColorPicker(Color.WHITE);
        asteroidsPane.ambientLight.colorProperty().bind(ambientColorPicker.valueProperty());
        ambientColorPicker.disableProperty().bind(ambientLightCheckBox.selectedProperty().not());

        CheckBox pointLightCheckBox = new CheckBox("Enable Point Light");
        pointLightCheckBox.setSelected(true);
        asteroidsPane.pointLight.disableProperty().bind(
            pointLightCheckBox.selectedProperty().not());
  
        ColorPicker pointColorPicker = new ColorPicker(Color.WHITE);
        asteroidsPane.pointLight.colorProperty().bind(pointColorPicker.valueProperty());
        pointColorPicker.disableProperty().bind(pointLightCheckBox.selectedProperty().not());

        
        VBox vbox = new VBox(10, 
            dirX, dirY, dirZ, inner, outer, falloff,
            new VBox(5, new Label("Spot Light Color"), spotLight1Picker),
            posX, posY, posZ,
//            new VBox(5, new Label("Spot Light 3 Color"), spotLight3Picker),
            new VBox(5, new Label("Surface Diffuse Color"), surfaceColorPicker),
            ambientLightCheckBox,
            new VBox(5, new Label("Ambient Light Color"), ambientColorPicker),
            pointLightCheckBox,
            new VBox(5, new Label("Point Light Color"), pointColorPicker)
        );
        ScrollPane scrollPane = new ScrollPane(vbox);
        StackPane.setAlignment(vbox, Pos.BOTTOM_LEFT);
        scrollPane.setPickOnBounds(false);
        scrollPane.setTranslateY(50);
        scrollPane.setMaxSize(400, 800);
        StackPane.setAlignment(scrollPane, Pos.TOP_LEFT);
        bpOilSpillNeverForget.setLeft(scrollPane);
        
        primaryStage.setTitle("Asteroid Field");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void handleNewManifoldData(ManifoldEvent event) {
//        Platform.runLater(() -> {
//            App.getAppScene().getRoot().fireEvent(
//                new CommandTerminalEvent("Loading Manifold Data...",
//                    new Font("Consolas", 20), Color.GREEN));
//        });
//        System.out.println("Loading Manifold Data...");
        ManifoldData md = (ManifoldData) event.object1;
        //convert deserialized points to Fxyz3D point3ds
        List<org.fxyz3d.geometry.Point3D> points = md.getPoints().stream()
            .map(P3D.p3DToFxyzPoint3D)
            .collect(toList());
        ArrayList<Point3D> fxpoints = points.stream()
            .map(JavaFX3DUtils.toFX)
            .collect(Collectors.toCollection(ArrayList::new));
        Manifold manifold = new Manifold(fxpoints, "dudelabel", "dudename", Color.CYAN);
        Manifold3D manifold3D = new Manifold3D(points, true, true, false, null);
        //Add this Manifold data object to the global tracker
        Manifold.addManifold(manifold);
        //update the manifold to manifold3D mapping
        Manifold.globalManifoldToManifold3DMap.put(manifold, manifold3D);

//        for (ManifoldRenderer renderer : manifoldRenderers) {
//            renderer.addManifold(manifold, manifold3D);
//        }
        asteroidsPane.addManifold(manifold, manifold3D);

        Platform.runLater(() -> {
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD3D_OBJECT_GENERATED, manifold, manifold3D));
        });
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

}
