package edu.jhuapl.trinity.javafx.components.radial;

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

import edu.jhuapl.trinity.javafx.renderers.Renderer;
import javafx.animation.ScaleTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * anchor displayed around a point.
 *
 * @author Sean Phillips
 */
public class Anchor extends Group implements LayerableObject {
    public LocalPoint localPoint;
    /**
     * The Circle Node that represents the Anchor
     */
    private Circle circle;
    /**
     * floating coordinate label
     */
    private Label label;
    /**
     * Format for floating coordinate label
     */
    private NumberFormat format = new DecimalFormat("0.00");

    /**
     * Properties bound to the CenterX and CenterY of the Circle Node
     */
    public DoubleProperty x, y;
    /**
     * Controls animation of circle size when mouse interaction occurs
     */
    private ScaleTransition scaleTransition;
    /**
     * Rate of animation of circle size
     */
    private Duration scalingDuration = Duration.millis(30);
    /**
     * Controls coloration of both stroke and fill.
     */
    public SimpleObjectProperty<Color> color;

    Boolean selected = true;

    Anchor me;
    private StringConverter<Double> converter = new DoubleStringConverter();

    public static double DEFAULT_CIRCLE_RADIUS = 10.0;

    /**
     * @param color The color property to bind the circle's stroke and fill to.
     * @param x     The initial X property to bind with in Local Coordinates
     * @param y     The initial Y property to bind with in Local Coordinates
     */
    public Anchor(Color color, double x, double y) {
        getStyleClass().add("anchor");
        this.x = new SimpleDoubleProperty(x);
        this.y = new SimpleDoubleProperty(y);
        this.color = new SimpleObjectProperty<>(color);

        circle = new Circle(this.x.get(), this.y.get(), DEFAULT_CIRCLE_RADIUS);
        circle.setStroke(this.color.get());
        circle.strokeProperty().bind(this.color);
        circle.setFill(this.color.get().deriveColor(1, 1, 1, 0.5));
        SimpleObjectProperty<Color> fillColor = new SimpleObjectProperty<>(this.color.get().deriveColor(1, 1, 1, 0.5));
        this.color.addListener((obs, oV, nV) -> fillColor.set(this.color.get().deriveColor(1, 1, 1, 0.5)));
        circle.fillProperty().bind(fillColor);
        circle.setStrokeWidth(2);
        circle.setStrokeType(StrokeType.OUTSIDE);

        //allows the label and other things to stay congruent with the circle
        this.x.bind(circle.centerXProperty());
        this.y.bind(circle.centerYProperty());

        //nice little animation whenever the mouse enters the Circle Node
        scaleTransition = new ScaleTransition(scalingDuration, getCircle());
        scaleTransition.setToX(1.5f);
        scaleTransition.setToY(1.5f);
        scaleTransition.setCycleCount(1);

        label = new Label();
        label.setMouseTransparent(true);
        label.setText(computeLabelString());
        label.layoutXProperty().bind(this.x);
        label.layoutYProperty().bind(this.y);

        getChildren().addAll(circle, label);
        //This is sorta important... by default we do NOT want JavaFX to try to
        //manage our layouts because the intent is to do coordinate transforms
        //between grid layers, heat maps and objects ourselves.  This is necessary
        //Because we will be mixing Event mapped objects with Canvas objects etc.
        this.setManaged(false);
        me = this;
    }

    public void showLabel(boolean show) {
        getLabel().setVisible(show);
    }

    public String computeLabelString() {
        return "(" + format.format(x.get()) + ", " +
            format.format(y.get()) + ")";
    }

    public Point2D toPoint2D() {
        return new Point2D(localPoint.x, localPoint.y);
    }

    @Override
    public void setEnableDrag(boolean enableDrag) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setLocation(double x, double y) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getCenterX() {
        return getCircle().getCenterX();
    }

    @Override
    public double getCenterY() {
        return getCircle().getCenterY();
    }

    @Override
    public Boolean isSelected() {
        return this.selected;
    }

    @Override
    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    @Override
    public void selectedDrag(MouseEvent me) {
        //This action is handled at SegmentedPolygon.
    }

    @Override
    public void selectedPress(MouseEvent event) {
        //This action is handled at SegmentedPolygon.
    }

    @Override
    public void selectedRelease(MouseEvent event) {
        //This action is handled at SegmentedPolygon.
    }


    /**
     * Anchor currently does not use a separate Renderer class because it is a
     * Node object.
     */
    @Override
    public Renderer getRenderer() {
        return new Renderer() {
            @Override
            public Node renderNode() {
                return me;
            }

            @Override
            public LocalPoint getLocation() {
                return localPoint;
            }
        };
    }

    /**
     * @return the circle
     */
    public Circle getCircle() {
        return circle;
    }

    /**
     * @param circle the circle to set
     */
    public void setCircle(Circle circle) {
        this.circle = circle;
    }

    /**
     * @return the label
     */
    public Label getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(Label label) {
        this.label = label;
    }
}
