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

import edu.jhuapl.trinity.data.Trial;
import eu.hansolo.fx.charts.Category;
import eu.hansolo.fx.charts.ChartType;
import eu.hansolo.fx.charts.YChart;
import eu.hansolo.fx.charts.YPane;
import eu.hansolo.fx.charts.data.ValueChartItem;
import eu.hansolo.fx.charts.series.YSeries;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class TrialRadarChart extends VBox {

    Label trajectoryLabel;
    private static final int ELEMENTS = 30;
    //    private static final ChartType CHART_TYPE = ChartType.SMOOTH_RADAR_POLYGON;
    private static final ChartType CHART_TYPE = ChartType.RADAR_SECTOR;

    private YSeries<ValueChartItem> series1;
    private YChart<ValueChartItem> chart;
    public double REFLECTION_SIZE = 0.25;
    public double WIDTH = 175;
    public double LABELHEIGHT = 25;
    //    public double HEIGHT = WIDTH + (WIDTH * REFLECTION_SIZE);
    public double HEIGHT = WIDTH - LABELHEIGHT;
    public double RADIUS_H = WIDTH / 2;
    public double BACK = WIDTH / 10;
    public PerspectiveTransform transform = new PerspectiveTransform();
    private Rectangle clip = new Rectangle();
    Color TRANSALICEBLUE = new Color(0.9411765f, 0.972549f, 1.0f, 0.75f);
    Color TRANSDARKSLATEGRAY = new Color(0.18431373f, 0.30980393f, 0.30980393f, 0.25f);
    Color TRANSSKYBLUE = new Color(0.5294118f, 0.80784315f, 0.92156863f, 0.5f);
    DropShadow glow = new DropShadow();

    public TrialRadarChart(Trial trial, int index, double initialWidth, double initialHeight) {
        trajectoryLabel = new Label(trial.trialId);
        makeRadarPlot(trial, index);
        setSpacing(10);
        WIDTH = initialWidth;
        HEIGHT = initialHeight;
        updateCalcs();
        setEffect(transform);
//        setClip(clip);
//        clip.setWidth(WIDTH);
//        clip.setHeight(HEIGHT);
//        clip.setArcWidth(20);
//        clip.setArcHeight(20);
        setMaxWidth(WIDTH);
        setMaxHeight(HEIGHT);
        getChildren().addAll(trajectoryLabel, chart);
    }

    private void makeRadarPlot(Trial trial, int index) {

        List<ValueChartItem> item1 = new ArrayList<>(trial.factorTimeSeries.size());
        for (int i = 0; i < trial.factorTimeSeries.size(); i++) {
            ValueChartItem dataPoint;
            dataPoint = new ValueChartItem(
                trial.factorTimeSeries.get(index).get(i) * 100, "P" + i);
            item1.add(dataPoint);
        }
//        series1 = new YSeries(item1, CHART_TYPE, new RadialGradient(0, 0, 0, 0, 1, true,
        series1 = new YSeries(item1, ChartType.SMOOTH_RADAR_POLYGON, new RadialGradient(0, 0, 0, 0, 1, true,
            CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.rgb(0, 0, 255, 0.5)),
            new Stop(0.5, Color.rgb(255, 255, 0, 0.5)),
            new Stop(1.0, Color.rgb(255, 0, 0, 0.75))),
            Color.TRANSPARENT);
        //series1.setWithWrapping(true);
        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < trial.factorTimeSeries.size(); i++) {
            categories.add(new Category("P" + i));
        }
        YPane ypane = new YPane(categories, series1);
        chart = new YChart(ypane);
        chart.setPrefSize(175, 175);

    }

    /**
     * Angle Property
     */
    public final DoubleProperty angle = new SimpleDoubleProperty(45) {
        @Override
        protected void invalidated() {
            // when angle changes calculate new transform
            updateCalcs();
            double lx = (RADIUS_H - Math.sin(Math.toRadians(angle.get())) * RADIUS_H - 1);
            double rx = (RADIUS_H + Math.sin(Math.toRadians(angle.get())) * RADIUS_H + 1);
            double uly = (-Math.cos(Math.toRadians(angle.get())) * BACK);
            double ury = -uly;
            transform.setUlx(lx);
            transform.setUly(uly);
            transform.setUrx(rx);
            transform.setUry(ury);
            transform.setLrx(rx);
            transform.setLry(HEIGHT + uly);
            transform.setLlx(lx);
            transform.setLly(HEIGHT + ury);
        }
    };

    public final double getAngle() {
        return angle.getValue();
    }

    public final void setAngle(double value) {
        angle.setValue(value);
    }

    public final DoubleProperty angleModel() {
        return angle;
    }

    private void updateCalcs() {
//        HEIGHT = WIDTH + (WIDTH * REFLECTION_SIZE);
        HEIGHT = HEIGHT;

        RADIUS_H = WIDTH / 2;
        BACK = WIDTH / 10;
    }
}
