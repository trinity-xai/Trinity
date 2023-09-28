package edu.jhuapl.trinity.javafx.components;

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
import edu.jhuapl.trinity.javafx.events.ManifoldEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.fxyz3d.geometry.Point3D;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * @author Sean Phillips
 */
public class PointListItem extends HBox {
    public static double PREF_LABEL_WIDTH = 100;
    private CheckBox includeCheckBox;
    private Label label;
    private Manifold manifold;
    private Point3D point3D;
    /**
     * Format for floating coordinate label
     */
    private NumberFormat format = new DecimalFormat("0.00000");

    public PointListItem(Manifold manifold, Point3D point3D, boolean included) {
        this.manifold = manifold;
        this.point3D = point3D;
        label = new Label(formattedString());
//        label.setPrefWidth(PREF_LABEL_WIDTH);
        includeCheckBox = new CheckBox("");
        includeCheckBox.setSelected(included);

        getChildren().addAll(includeCheckBox, label);
        setSpacing(5);
        includeCheckBox.selectedProperty().addListener(cl -> {
            if (null != includeCheckBox.getScene()) {
                getScene().getRoot().fireEvent(new ManifoldEvent(
                    ManifoldEvent.TOGGLE_HULL_POINT,
                    this.manifold, point3D));
            }
        });
    }

    private String formattedString() {
        return format.format(getPoint3D().getX()) + ", "
            + format.format(getPoint3D().getY()) + ", "
            + format.format(getPoint3D().getZ());
    }

    public boolean isSelected() {
        return includeCheckBox.isSelected();
    }

    /**
     * @return the point3D
     */
    public Point3D getPoint3D() {
        return point3D;
    }

    /**
     * @param point3D the point3D to set
     */
    public void setPoint3D(Point3D point3D) {
        this.point3D = point3D;
    }
}
