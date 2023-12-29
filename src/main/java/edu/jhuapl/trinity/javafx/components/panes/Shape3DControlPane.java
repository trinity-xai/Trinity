package edu.jhuapl.trinity.javafx.components.panes;

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

import edu.jhuapl.trinity.data.Manifold;
import edu.jhuapl.trinity.javafx.components.listviews.PointListItem;
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * @author Sean Phillips
 */
public class Shape3DControlPane extends LitPathPane {
    BorderPane bp;
    private Slider scaleSlider;
    private Slider rotateXSlider;
    private Slider rotateYSlider;
    private Slider rotateZSlider;
    private Label scaleLabel;
    private Label rotateXLabel;
    private Label rotateYLabel;
    private Label rotateZLabel;
    private ListView<PointListItem> pointListView;

    /**
     * Format for floating coordinate label
     */
    private NumberFormat format = new DecimalFormat("0.00");
    private Manifold3D manifold3D = null;

    private static BorderPane createContent() {
        BorderPane bpOilSpill = new BorderPane();
        return bpOilSpill;
    }

    public Shape3DControlPane(Scene scene, Pane parent) {
        super(scene, parent, 600, 400, createContent(),
            "Geometry Controls", "", 300.0, 400.0);
        this.scene = scene;

        bp = (BorderPane) this.contentPane;
        scaleLabel = new Label("Scale: ");
        scaleSlider = new Slider(0.25, 2, 1.0);
        scaleSlider.setShowTickMarks(true);
        scaleSlider.setMajorTickUnit(0.25);
        scaleSlider.setSnapToTicks(true);
        scaleLabel.setText("Scale " + format.format(scaleSlider.getValue()));
        scaleSlider.valueProperty().addListener((ov, t, t1) -> {
            scaleLabel.setText("Scale " + format.format(scaleSlider.getValue()));
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_SET_SCALE, t.doubleValue(), manifold3D.getManifold()));
        });

        rotateXLabel = new Label("Rotate X: ");
        rotateXSlider = new Slider(-180, 180, 0);
        rotateXSlider.setMajorTickUnit(10);
        rotateXSlider.setShowTickMarks(true);
        rotateXSlider.setSnapToTicks(true);
        rotateXLabel.setText("Rotate X: " + format.format(rotateXSlider.getValue()));
        rotateXSlider.valueProperty().addListener((ov, t, t1) -> {
            rotateXLabel.setText("Rotate X: " + format.format(rotateXSlider.getValue()));
            double[] ypr = new double[]{
                rotateYSlider.getValue(), rotateXSlider.getValue(), rotateZSlider.getValue()
            };
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_SET_YAWPITCHROLL, ypr, manifold3D.getManifold()));
        });

        rotateYLabel = new Label("Rotate Y: ");
        rotateYSlider = new Slider(-180, 180, 0);
        rotateYSlider.setMajorTickUnit(10);
        rotateYSlider.setShowTickMarks(true);
        rotateYSlider.setSnapToTicks(true);
        rotateYLabel.setText("Rotate Y: " + format.format(rotateYSlider.getValue()));
        rotateYSlider.valueProperty().addListener((ov, t, t1) -> {
            rotateYLabel.setText("Rotate Y: " + format.format(rotateYSlider.getValue()));
            double[] ypr = new double[]{
                rotateYSlider.getValue(), rotateXSlider.getValue(), rotateZSlider.getValue()
            };
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_SET_YAWPITCHROLL, ypr, manifold3D.getManifold()));
        });

        rotateZLabel = new Label("Rotate Z: ");
        rotateZSlider = new Slider(-180, 180, 0);
        rotateZSlider.setMajorTickUnit(10);
        rotateZSlider.setShowTickMarks(true);
        rotateZSlider.setSnapToTicks(true);
        rotateZLabel.setText("Rotate Z: " + format.format(rotateZSlider.getValue()));
        rotateZSlider.valueProperty().addListener((ov, t, t1) -> {
            rotateZLabel.setText("Rotate Z: " + format.format(rotateZSlider.getValue()));
            double[] ypr = new double[]{
                rotateYSlider.getValue(), rotateXSlider.getValue(), rotateZSlider.getValue()
            };
            scene.getRoot().fireEvent(new ManifoldEvent(
                ManifoldEvent.MANIFOLD_SET_YAWPITCHROLL, ypr, manifold3D.getManifold()));
        });

        Button refresh = new Button("Refresh Hull");
        CheckBox auto = new CheckBox("Auto");
        auto.setSelected(true);
        refresh.setOnAction(e -> updateManifold());
        auto.selectedProperty().addListener(cl -> {
            if (auto.isSelected())
                updateManifold();
        });

        VBox controlsVBox = new VBox(10, new Label("Controls"),
            new HBox(5, refresh, auto),
            scaleLabel, scaleSlider,
            rotateXLabel, rotateXSlider,
            rotateYLabel, rotateYSlider,
            rotateZLabel, rotateZSlider);
        controlsVBox.setPadding(new Insets(10));
        bp.setRight(controlsVBox);

        pointListView = new ListView<>();
        ImageView iv = ResourceUtils.loadIcon("point3D", 200);
        VBox placeholder = new VBox(10, iv, new Label("No Shape Point3Ds Acquired"));
        placeholder.setAlignment(Pos.CENTER);
        pointListView.setPlaceholder(placeholder);
        VBox pointVBOX = new VBox(10, new Label("Points"), pointListView);
        pointVBOX.setPrefWidth(350);
        bp.setLeft(pointVBOX);
        scene.addEventHandler(ManifoldEvent.TOGGLE_HULL_POINT, e -> {
            if (auto.isSelected()) {
                Manifold eventManifold = (Manifold) e.object1;
                Manifold manifold = manifold3D.getManifold();
                if (null != eventManifold && null != manifold
                    && eventManifold == manifold) {
                    updateManifold();
                }
            }
        });
        scene.addEventHandler(ManifoldEvent.SELECT_PROJECTION_POINT3D, e -> {
            Point3D p3D = (Point3D) e.object1;
//            org.fxyz3d.geometry.Point3D fxyzP3D = new org.fxyz3d.geometry.Point3D(
//                p3D.getX(), p3D.getY(), p3D.getZ());
            int shortestIndex = 0;
            Double shortestDistance = null;
            for (int i = 0; i < pointListView.getItems().size(); i++) {
                PointListItem item = pointListView.getItems().get(i);
                double currentDistance = p3D.distance(item.getPoint3D().getX(),
                    item.getPoint3D().getY(), item.getPoint3D().getZ());
                if (null == shortestDistance || currentDistance < shortestDistance) {
                    shortestDistance = currentDistance;
                    shortestIndex = i;
                }
            }
            pointListView.getSelectionModel().select(shortestIndex);
            pointListView.scrollTo(shortestIndex);
        });

    }

    private void updateManifold() {
        System.out.println("Refreshing Manifold...");
//        getScene().getRoot().fireEvent(new ManifoldEvent(
//            ManifoldEvent.UPDATE_MANIFOLD_POINTS,
//                this.manifold, includeCheckBox.isSelected()));
        //build list of points from listview
        List<org.fxyz3d.geometry.Point3D> points = new ArrayList<>();
        pointListView.getItems().forEach(item -> {
            if (item.isSelected())
                points.add(item.getPoint3D());
        });
        manifold3D.refreshMesh(points, false, true, false);
    }

    public void setShape3D(Manifold3D manifold3D) {
        this.manifold3D = manifold3D;
        Manifold manifold = manifold3D.getManifold();
        if (null != manifold) {
            pointListView.getItems().clear();
            List<PointListItem> pointListItems = this.manifold3D
                .getOriginalPoint3DList().stream()
                //.map(fxyzPoint3DTofxPoint3D)
                .map((p) -> new PointListItem(manifold, p, true))
                .collect(toList());
            pointListView.getItems().addAll(pointListItems);
        }
    }
}
