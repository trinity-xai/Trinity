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
/**
 * @author Sean Phillips
 */

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.Observable;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;

/**
 * @author Sean Phillips
 */
public class DataCarousel extends Region {

    private static final Duration DURATION = Duration.millis(500);
    private static final Interpolator INTERPOLATOR = Interpolator.EASE_BOTH;
    public double SPACING = 150;
    public double LEFT_OFFSET = -SPACING;
    public double RIGHT_OFFSET = SPACING;
    public double SCALE_SMALL = 0.7;
    public double SCALE_BIG = 2.0;
    private boolean scaledBig = false;
    private ArrayList<TrialRadarChart> radarCharts;

    private Group centered = new Group();
    private Group left = new Group();
    private Group center = new Group();
    private Group right = new Group();
    private int centerIndex = 0;
    public SimpleIntegerProperty centeredIndexProp = new SimpleIntegerProperty(centerIndex);
    private Timeline timeline;
    private ScrollBar scrollBar = new ScrollBar();
    private boolean localChange = false;
    private Rectangle clip = new Rectangle();

    public double defaultItemWidth = 200;
    public double defaultItemHeight = 200;
    public double leftAngle = 30;
    public double centerAngle = 90; //90 degrees equals a flat facing forward node
    public double rightAngle = 140;

    public DataCarousel(ArrayList<TrialRadarChart> charts, double itemWidth, double itemHeight) {
        defaultItemWidth = itemWidth;
        defaultItemHeight = itemHeight;
        // set clip
        setClip(clip);

        // create items
        radarCharts = new ArrayList<>();

        for (int i = 0; i < charts.size(); i++) {
            addChart(charts.get(i));
        }
        // setup scroll bar
        scrollBar.setMax(radarCharts.size());
        scrollBar.setVisibleAmount(1);
        scrollBar.setUnitIncrement(1);
        scrollBar.setBlockIncrement(1);
        scrollBar.valueProperty().addListener((Observable ov) -> {
            if (!localChange && (int) scrollBar.getValue() < radarCharts.size()) {
                shiftToCenter(radarCharts.get((int) scrollBar.getValue()));
            }
        });
        if (radarCharts.size() > 1) {
            scrollBar.setVisible(true);
        } else {
            scrollBar.setVisible(false);
        }
        setOnSwipeLeft((SwipeEvent t) -> {
            left();
            t.consume();
        });
        setOnSwipeRight((SwipeEvent t) -> {
            right();
            t.consume();
        });
        setOnZoom((ZoomEvent event) -> {
            setScaleX(getScaleX() * event.getZoomFactor());
            setScaleY(getScaleY() * event.getZoomFactor());
            event.consume();
        });
        // Add all content
        centered.getChildren().addAll(left, right, center);
        getChildren().addAll(centered, scrollBar);
        update();
    }

    public void left() {
        shift(1);
        localChange = true;
        scrollBar.setValue(centerIndex);
        localChange = false;
    }

    public void right() {
        shift(-1);
        localChange = true;
        scrollBar.setValue(centerIndex);
        localChange = false;
    }

    public void clearAll() {
        centerIndex = 0;
        radarCharts.clear();
        scrollBar.setMax(0);
        localChange = true;
        scrollBar.setValue(0);
        localChange = false;
        left.getChildren().clear();
        center.getChildren().clear();
        right.getChildren().clear();
    }

    public void clearCharts() {
        radarCharts.clear();
        scrollBar.setMax(1);
        update();
    }

    public int addChart(TrialRadarChart chart) {
        radarCharts.add(chart);
        // setup scroll bar
        final double index = radarCharts.size();
        scrollBar.setMax(index);
        chart.setOnMouseClicked((MouseEvent me) -> {
            localChange = true;
            scrollBar.setValue(index);
            localChange = false;
            shiftToCenter(chart);
        });
        update();
        return radarCharts.size();
    }

    @Override
    protected void layoutChildren() {
        // update clip to our size
        clip.setWidth(getWidth());
        clip.setHeight(getHeight());
        // keep centered centered
        centered.setLayoutY((getHeight() - (radarCharts.isEmpty() ? defaultItemHeight : radarCharts.get(0).getHeight())) / 2);
        centered.setLayoutX((getWidth() - (radarCharts.isEmpty() ? defaultItemWidth : radarCharts.get(0).getWidth())) / 2);

        // position scroll bar at bottom
        scrollBar.setLayoutX(10);
        scrollBar.setLayoutY(getHeight() - 20);
        scrollBar.resize(getWidth() - 20, 15);
    }

    public void toggleCenter() {
        if (scaledBig) {
            center.setScaleX(SCALE_BIG);
            center.setScaleY(SCALE_BIG);
        } else {
            center.setScaleX(1.0);
            center.setScaleY(1.0);
            update();
        }
        scaledBig = !scaledBig;
    }

    public void update() {
        left.getChildren().clear();
        center.getChildren().clear();
        right.getChildren().clear();
        if (radarCharts.size() > 1) {
            scrollBar.setVisible(true);
        } else {
            scrollBar.setVisible(false);
        }
        //Break out early if there are no nodes to render
        if (radarCharts.isEmpty()) {
            return;
        }
        // move items to new homes in groups
        for (int i = 0; i < centerIndex; i++) {
            left.getChildren().add(radarCharts.get(i));
        }
        if (!radarCharts.isEmpty()) {
            center.getChildren().add(radarCharts.get(centerIndex));
            centeredIndexProp.setValue(centerIndex);
        }
        for (int i = radarCharts.size() - 1; i > centerIndex; i--) {
            right.getChildren().add(radarCharts.get(i));
        }
        // stop old timeline if there is one running
        if (timeline != null) {
            timeline.stop();
        }
        // create timeline to animate to new positions
        timeline = new Timeline();
        // add keyframes for left items
        final ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
        for (int i = 0; i < left.getChildren().size(); i++) {
            TrialRadarChart it = radarCharts.get(i);
            double newX = -left.getChildren().size() * SPACING + SPACING * i + LEFT_OFFSET;
            keyFrames.add(new KeyFrame(DURATION,
                new KeyValue(it.translateXProperty(), newX, INTERPOLATOR),
                new KeyValue(it.scaleXProperty(), SCALE_SMALL, INTERPOLATOR),
                new KeyValue(it.scaleYProperty(), SCALE_SMALL, INTERPOLATOR),
                new KeyValue(it.angle, leftAngle, INTERPOLATOR)));
        }
        // add keyframe for center item
        if (!radarCharts.isEmpty()) {
            TrialRadarChart centerChart = radarCharts.get(centerIndex);
            keyFrames.add(new KeyFrame(DURATION,
                new KeyValue(centerChart.translateXProperty(), 0, INTERPOLATOR),
                new KeyValue(centerChart.scaleXProperty(), 1.0, INTERPOLATOR),
                new KeyValue(centerChart.scaleYProperty(), 1.0, INTERPOLATOR),
                new KeyValue(centerChart.angle, centerAngle, INTERPOLATOR)));
            // add keyframes for right items
            for (int i = 0; i < right.getChildren().size(); i++) {
                final TrialRadarChart it = radarCharts.get(radarCharts.size() - i - 1);
                final double newX = right.getChildren().size()
                    * SPACING - SPACING * i + RIGHT_OFFSET;
                keyFrames.add(new KeyFrame(DURATION,
                    new KeyValue(it.translateXProperty(), newX, INTERPOLATOR),
                    new KeyValue(it.scaleXProperty(), SCALE_SMALL, INTERPOLATOR),
                    new KeyValue(it.scaleYProperty(), SCALE_SMALL, INTERPOLATOR),
                    new KeyValue(it.angle, rightAngle, INTERPOLATOR)));
            }
        }
        timeline.play();             // play animation
    }

    private void shiftToCenter(TrialRadarChart chart) {
        for (int i = 0; i < left.getChildren().size(); i++) {
            if (left.getChildren().get(i) == chart) {
                int shiftAmount = left.getChildren().size() - i;
                shift(shiftAmount);
                return;
            }
        }
        if (center.getChildren().get(0) == chart) {
            return;
        }
        for (int i = 0; i < right.getChildren().size(); i++) {
            if (right.getChildren().get(i) == chart) {
                int shiftAmount = -(right.getChildren().size() - i);
                shift(shiftAmount);
                return;
            }
        }
    }

    public void shiftTo(int newIndex) {
        if (newIndex < 0 || newIndex > radarCharts.size()) {
            return;
        }
        localChange = true;
        scrollBar.setValue(newIndex);
        localChange = false;
        shiftToCenter(radarCharts.get(newIndex));
    }

    public void shift(int shiftAmount) {
        if (centerIndex <= 0 && shiftAmount > 0) {
            return;
        }
        if (centerIndex >= radarCharts.size() - 1 && shiftAmount < 0) {
            return;
        }
        centerIndex -= shiftAmount;
        update();
    }
}
