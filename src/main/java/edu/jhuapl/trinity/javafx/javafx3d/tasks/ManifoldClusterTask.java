package edu.jhuapl.trinity.javafx.javafx3d.tasks;

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

import edu.jhuapl.trinity.data.FactorLabel;
import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.components.radial.ProgressStatus;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.PerspectiveCamera;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.stage.StageStyle;
import org.fxyz3d.geometry.Point3D;

/**
 * @author Sean Phillips
 */
public class ManifoldClusterTask extends Task {
    Scene scene;
    PerspectiveCamera camera;
    HashMap<Sphere, FeatureVector> currentMap;
    Rectangle rectangle;
    boolean filterByLabel = false;
    String filterLabel = "";
    Color diffuseColor = Color.CYAN;
    String manifoldName = "Selected Manifold";
    private boolean cancelledByUser = false;
    static AtomicInteger ai = new AtomicInteger(0);
    
    public ManifoldClusterTask(Scene scene, PerspectiveCamera camera, 
        HashMap<Sphere, FeatureVector> currentMap, Rectangle rectangle) {
        this.scene = scene;
        this.camera = camera;
        this.currentMap = currentMap;
        this.rectangle = new Rectangle(rectangle.getX(), rectangle.getY(), 
            rectangle.getWidth(), rectangle.getHeight());
        setOnSucceeded(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
        setOnFailed(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
        setOnCancelled(e -> {
            Platform.runLater(() -> {
                scene.getRoot().fireEvent(
                    new ApplicationEvent(ApplicationEvent.HIDE_BUSY_INDICATOR));
            });
        });
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Customizations",
            ButtonType.APPLY, ButtonType.CANCEL);
        alert.setHeaderText("Customize Selection");
        alert.setGraphic(ResourceUtils.loadIcon("alert", 75));
        alert.initStyle(StageStyle.TRANSPARENT);
        DialogPane dialogPane = alert.getDialogPane();
        Label nameLabel = new Label("Name");
        nameLabel.setMinWidth(100);
        TextField labelTextField = new TextField("Selected Manifold " + ai.getAndIncrement());
        labelTextField.setPrefWidth(200);
        HBox textFieldHBox = new HBox(10, nameLabel, labelTextField);
        textFieldHBox.setAlignment(Pos.CENTER_LEFT);
        CheckBox filterCheckBox = new CheckBox("");
        Label filterCheckBoxLabel = new Label("Filter");
        filterCheckBoxLabel.setMinWidth(100);
        filterCheckBoxLabel.setGraphicTextGap(32);
        filterCheckBoxLabel.setGraphic(filterCheckBox);
        filterCheckBoxLabel.setContentDisplay(ContentDisplay.RIGHT);

        ChoiceBox labelChoiceBox = new ChoiceBox();
        labelChoiceBox.getItems().clear();
        labelChoiceBox.getItems().add("ALL");
        labelChoiceBox.getItems().addAll(
            FactorLabel.getFactorLabels().stream()
                .map(f -> f.getLabel()).sorted().toList());
        labelChoiceBox.getSelectionModel().selectFirst();
        labelChoiceBox.setPrefWidth(200);
        HBox filterHBox = new HBox(10, filterCheckBoxLabel, labelChoiceBox);
        filterHBox.setAlignment(Pos.CENTER_LEFT);
        labelChoiceBox.disableProperty().bind(filterCheckBox.selectedProperty().not());
        
        ColorPicker colorPicker = new ColorPicker(Color.CYAN);
        colorPicker.setPrefWidth(200);
        Label colorLabel = new Label("Color");
        colorLabel.setMinWidth(100);
        HBox colorHBox = new HBox(10, colorLabel, colorPicker);
        colorHBox.setAlignment(Pos.CENTER_LEFT);

        VBox contentVBox = new VBox(2, textFieldHBox, 
             filterHBox, colorHBox);
        
        dialogPane.setContent(contentVBox);
        dialogPane.setBackground(Background.EMPTY);
        dialogPane.getScene().setFill(Color.TRANSPARENT);
        String DIALOGCSS = ResourceUtils.class.getResource("/edu/jhuapl/trinity/css/dialogstyles.css").toExternalForm();
        dialogPane.getStylesheets().add(DIALOGCSS);
        Optional<ButtonType> optBT = alert.showAndWait();
        if (optBT.get().equals(ButtonType.APPLY)) {
            filterByLabel = filterCheckBox.isSelected();
            filterLabel = (String)labelChoiceBox.getSelectionModel().getSelectedItem();
            diffuseColor = colorPicker.getValue();
            manifoldName = labelTextField.getText();
        } else {
            cancelledByUser = true;
        }
    }

    @Override
    protected Void call() throws Exception {
        if(isCancelled()) return null;
        Platform.runLater(() -> {
            ProgressStatus ps = new ProgressStatus("Converting Cluster to Manifold...", -1);
            scene.getRoot().fireEvent(
                new ApplicationEvent(ApplicationEvent.SHOW_BUSY_INDICATOR, ps));
        });
        List<Sphere> spheres = currentMap.keySet().stream().toList();
        List<Integer> indices = 
            JavaFX3DUtils.pickIndicesByBox(camera, spheres, 
            new Point2D(rectangle.getX(), rectangle.getY()), 
            new Point2D(rectangle.getX()+rectangle.getWidth(), 
                    rectangle.getY()+rectangle.getHeight())
        );
        if(indices.size()>4) {
            if(filterByLabel) {
                
            }
            ArrayList<javafx.geometry.Point3D> manPoints = new ArrayList<>();
            List<Point3D> labelMatchedPoints = new ArrayList<>();
            for(int index : indices) {
                javafx.geometry.Point3D p3D = JavaFX3DUtils.mapShape3DToPoint3D.apply(spheres.get(index));
                manPoints.add(p3D);
                FeatureVector fv = currentMap.get(spheres.get(index));
                if(!filterByLabel || (null != fv && filterLabel.contentEquals(fv.getLabel())))
                    labelMatchedPoints.add(JavaFX3DUtils.toFXYZ3D.apply(p3D));
            }
            Manifold manifold = new Manifold(manPoints, manifoldName, manifoldName, diffuseColor);
            //Create the 3D manifold shape
            Manifold3D manifold3D = new Manifold3D(
                labelMatchedPoints, true, true, true, null
            );
            manifold3D.quickhullMeshView.setCullFace(CullFace.FRONT);
            manifold3D.setManifold(manifold);
            ((PhongMaterial)manifold3D.quickhullMeshView.getMaterial()).setDiffuseColor(diffuseColor);
            
            manifold3D.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
                scene.getRoot().fireEvent(
                    new ManifoldEvent(ManifoldEvent.MANIFOLD_3D_SELECTED, manifold));
            });
            //Add this Manifold data object to the global tracker
            Manifold.addManifold(manifold);
            //update the manifold to manifold3D mapping
            Manifold.globalManifoldToManifold3DMap.put(manifold, manifold3D);
            //announce to the world of the new manifold and its shape
            //System.out.println("Manifold3D generation complete for " + manifoldName);
            Platform.runLater(()-> {
                scene.getRoot().fireEvent(
                    new CommandTerminalEvent("Manifold3D generation complete for " + manifoldName,
                    new Font("Consolas", 20), Color.GREEN));
                scene.getRoot().fireEvent(new ManifoldEvent(
                    ManifoldEvent.NEW_MANIFOLD_CLUSTER, manifold, manifold3D));

                scene.getRoot().fireEvent(new ManifoldEvent(
                    ManifoldEvent.MANIFOLD3D_OBJECT_GENERATED, manifold, manifold3D));
            });
        } else {
            Platform.runLater(()-> {            
                scene.getRoot().fireEvent(
                    new CommandTerminalEvent("Insufficient selected points for 3D Hull.",
                    new Font("Consolas", 20), Color.YELLOW));
            });
        }
        return null;
    }

    /**
     * @return the cancelledByUser
     */
    public boolean isCancelledByUser() {
        return cancelledByUser;
    }

    /**
     * @param cancelledByUser the cancelledByUser to set
     */
    public void setCancelledByUser(boolean cancelledByUser) {
        this.cancelledByUser = cancelledByUser;
    }
}
