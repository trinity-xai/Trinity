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

import edu.jhuapl.trinity.App;
import edu.jhuapl.trinity.data.Trajectory;
import edu.jhuapl.trinity.javafx.events.HoverTrajectory3DEvent;
import edu.jhuapl.trinity.javafx.events.SelectTrajectory3DEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.shapes.composites.PolyLine3D;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Sean Phillips
 */
public class Trajectory3D extends PolyLine3D {
    public int trialNumber;
    public int dayNumber;
    public Trajectory trajectory;
    public float width, highlightWidth;
    public Color color;
    public PhongMaterial highlightMaterial = new PhongMaterial(Color.ALICEBLUE);
    private PhongMaterial baseMaterial;
    private PolyLine3D highlightPoly;
    private PolyLine3D basePoly;

    public Trajectory3D(int trialNumber, int dayNumber, Trajectory trajectory,
                        List<Point3D> points, float width, Color color) {
        super(points, width, color, LineType.TRIANGLE);
        this.trialNumber = trialNumber;
        this.dayNumber = dayNumber;
        this.width = width;
        this.color = color;
        this.highlightWidth = 2 * width;
        this.trajectory = trajectory;

        highlightPoly = new PolyLine3D(this.points, highlightWidth, Color.ALICEBLUE, LineType.TRIANGLE);
        basePoly = new PolyLine3D(this.points, this.width, this.color, LineType.TRIANGLE);
        baseMaterial = basePoly.material;

        setOnMouseEntered(me -> {
            meshView.setMaterial(highlightMaterial);
            meshView.setMesh(highlightPoly.meshView.getMesh());
            HoverTrajectory3DEvent event = new HoverTrajectory3DEvent(this);
            event.mouseEvent = me;
            App.getAppScene().getRoot().fireEvent(event);
            fireEvent(event);
            setOnMouseExited(mouseExit -> {
                removeEventHandler(MouseEvent.MOUSE_EXITED, getOnMouseExited());
                meshView.setMaterial(basePoly.material);
                meshView.setMesh(basePoly.meshView.getMesh());
            });
        });
        setOnMouseClicked(me -> {
            SelectTrajectory3DEvent event = new SelectTrajectory3DEvent(this);
            event.mouseEvent = me;
            fireEvent(event);
        });
    }

    public List<Point3D> getPolyLinePoints() {
        return basePoly.points;
    }

    private static Function<List<double[]>, List<Point3D>> statesToPoints = (states) -> {
        List<Point3D> newList = new ArrayList<>();
        for(double [] state : states) {
            newList.add(new Point3D(state[0], state[1], state[2]));
        }
        return newList;
    };

}
