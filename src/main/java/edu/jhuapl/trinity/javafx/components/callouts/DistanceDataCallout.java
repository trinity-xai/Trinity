package edu.jhuapl.trinity.javafx.components.callouts;

/*-
 * #%L
 * trinity-2023.09.01
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
import edu.jhuapl.trinity.javafx.javafx3d.Manifold3D;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Shape3D;
import org.fxyz3d.geometry.Point3D;

/**
 *
 * @author phillsm1
 */
public class DistanceDataCallout extends VBox {
    /**
     * 
     * @param shape3D 3D anchor node to use for 2D transforms
     * @param manifold The manifold to associate with this data
     * @param distancePoint3D The point in space this data is measured from
     * @param subScene the 3D subscene that holds all this BS
     * @return Custom Callout using Builder pattern
     */
    public static Callout createByManifold3D(Shape3D shape3D, Manifold manifold, Point3D distancePoint3D, SubScene subScene) {
        Manifold3D manifold3D = Manifold.globalManifoldToManifold3DMap.get(manifold);
        DistanceDataCallout mainTitleVBox = new DistanceDataCallout(manifold3D, distancePoint3D, subScene);
        Point2D p2D = JavaFX3DUtils.getTransformedP2D(shape3D, subScene, Callout.DEFAULT_HEAD_RADIUS + 5);
        Callout infoCallout = CalloutBuilder.create()
            .headPoint(p2D.getX(), p2D.getY())
            .leaderLineToPoint(p2D.getX() - 100, p2D.getY() - 150)
            .endLeaderLineRight()
            .mainTitle(manifold.getLabel() + " Manifold", mainTitleVBox)
            .pause(10)
            .build();

        infoCallout.setOnMouseClicked(e -> {
            if (e.getClickCount() > 1) {
                infoCallout.hide();
            }
        });

        infoCallout.setOnZoom(e -> {
            if (e.getZoomFactor() < 1)
                infoCallout.hide(); //pinch hides it
        });

        infoCallout.setPickOnBounds(false);
        infoCallout.setManaged(false);
       
        return infoCallout;
    }

    public DistanceDataCallout(Manifold3D manifold3D, Point3D point3D, SubScene subScene) {
        NumberFormat doubleFormat = new DecimalFormat("0.0000");
        TitledPane manifoldTP = new TitledPane();

        GridPane manifoldGridPane = new GridPane();
        manifoldGridPane.setPadding(new Insets(1));
        manifoldGridPane.setHgap(5);
        manifoldGridPane.addRow(0, new Label("Label"),
            new Label("Value"));

        manifoldGridPane.addRow(1, new Label("Mean Centroid"),
            new Label("Coord"));
        manifoldGridPane.addRow(2, new Label("X"),
            new Label(doubleFormat.format(point3D.x)));
        manifoldGridPane.addRow(3, new Label("Y"),
            new Label(doubleFormat.format(point3D.y)));
        manifoldGridPane.addRow(4, new Label("Z"),
            new Label(doubleFormat.format(point3D.z)));

        manifoldGridPane.addRow(1, new Label("Hit Box Size"),
            new Label("Dimension"));
        manifoldGridPane.addRow(2, new Label("Width"),
            new Label(doubleFormat.format(point3D.x)));
        manifoldGridPane.addRow(3, new Label("Height"),
            new Label(doubleFormat.format(point3D.y)));
        manifoldGridPane.addRow(4, new Label("Depth"),
            new Label(doubleFormat.format(point3D.z)));

        VBox manifoldVBox = new VBox(5, manifoldGridPane);
        manifoldTP.setContent(manifoldVBox);
        manifoldTP.setText("Manifold Details");
        manifoldTP.setExpanded(false);        
        
        TitledPane measurementsTP = new TitledPane();
        GridPane measurementsGridPane = new GridPane();
        measurementsGridPane.setPadding(new Insets(-1));
        measurementsGridPane.setHgap(5);
        measurementsGridPane.addRow(0, new Label("Start Point"),
            new Label("Value"));
        measurementsGridPane.addRow(1, new Label("X"),
            new Label(doubleFormat.format(point3D.x)));
        measurementsGridPane.addRow(2, new Label("Y"),
            new Label(doubleFormat.format(point3D.y)));
        measurementsGridPane.addRow(3, new Label("Z"),
            new Label(doubleFormat.format(point3D.z)));
        measurementsGridPane.addRow(4, new Label("Inside Hit Box"),
            new Label("TODO Boolean"));
        measurementsGridPane.addRow(5, new Label("Inside Hull"),
            new Label("TODO Boolean"));
        measurementsGridPane.addRow(6, new Label("Hull Distance"),
            new Label("TODO Number"));

        VBox measurementsVBox = new VBox(5, measurementsGridPane);
        measurementsTP.setContent(measurementsVBox);        
        measurementsTP.setText("Measurements");
        measurementsTP.setExpanded(false);

        getChildren().addAll(manifoldTP, measurementsTP);
        setSpacing(3);
//        setPrefWidth(250);
//        setPrefHeight(100);

    }

}
