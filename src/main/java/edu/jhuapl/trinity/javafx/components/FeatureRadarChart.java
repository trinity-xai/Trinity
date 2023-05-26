package edu.jhuapl.trinity.javafx.components;

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

import edu.jhuapl.trinity.data.messages.FeatureVector;
import eu.hansolo.fx.charts.Category;
import eu.hansolo.fx.charts.ChartType;
import eu.hansolo.fx.charts.Symbol;
import eu.hansolo.fx.charts.YChart;
import eu.hansolo.fx.charts.YPane;
import eu.hansolo.fx.charts.data.ValueChartItem;
import eu.hansolo.fx.charts.series.YSeries;
import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class FeatureRadarChart extends VBox {

    Label trajectoryLabel;
    private static double SCALAR_MAG = 500.0;
    private YSeries<ValueChartItem> series;
    private YChart<ValueChartItem> chart;
    private YPane ypane;
    private List<String> labels = null;

    public double initialWidth;
    public double initialHeight;

    public FeatureRadarChart(FeatureVector featureVector, double initialWidth, double initialHeight) {
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
        trajectoryLabel = new Label(featureVector.getLabel());
        makeRadarPlot(featureVector);
        setSpacing(5);
        ChoiceBox<ChartType> choiceBox = new ChoiceBox<>(
            FXCollections.observableList(Arrays.asList(ChartType.values())));
        choiceBox.getSelectionModel().select(ChartType.SMOOTH_RADAR_POLYGON);
        choiceBox.setOnAction(e -> {
            series.setChartType(choiceBox.getValue());
            chart.refresh();
        });
        getChildren().addAll(trajectoryLabel, chart, choiceBox);
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            categories.add(new Category(labels.get(i)));
        }
        ypane.setCategories(categories);
    }

    public void updateRadarPlot(FeatureVector featureVector) {
        List<ValueChartItem> items = new ArrayList<>(featureVector.getData().size());
        for (int i = 0; i < featureVector.getData().size(); i++) {
            String label = "P " + i;
            if (null != labels && i < labels.size())
                label = labels.get(i);
            ValueChartItem dataPoint = new ValueChartItem(
                featureVector.getData().get(i) * SCALAR_MAG, label);
            dataPoint.setSymbol(Symbol.CIRCLE);
            dataPoint.setStroke(Color.SKYBLUE);
            dataPoint.setFill(Color.BLUE);
            items.add(dataPoint);
        }
        series.setItems(items);
        series.setTextFill(Color.BLUE);
        trajectoryLabel.setText(featureVector.getLabel());
        chart.refresh();
    }

    private void makeRadarPlot(FeatureVector featureVector) {
        List<ValueChartItem> items = new ArrayList<>(featureVector.getData().size());
        for (int i = 0; i < featureVector.getData().size(); i++) {
            String label = "P " + i;
            if (null != labels && i < labels.size())
                label = labels.get(i);
            ValueChartItem dataPoint = new ValueChartItem(
                featureVector.getData().get(i) * SCALAR_MAG, label);
            dataPoint.setSymbol(Symbol.CIRCLE);
            dataPoint.setStroke(Color.RED);
            dataPoint.setFill(Color.BLUE);
            items.add(dataPoint);
        }
        series = new YSeries(items, ChartType.RADAR_SECTOR, "Features",
            new RadialGradient(0, 0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(0, 0, 255, 0.5)),
                new Stop(0.5, Color.rgb(255, 255, 0, 0.5)),
                new Stop(1.0, Color.rgb(255, 0, 0, 0.75))),
            Color.SKYBLUE, Symbol.CIRCLE);
        series.setTextFill(Color.BLUE);
        series.setAnimated(true);
        series.setAnimationDuration(100);

        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < featureVector.getData().size(); i++) {
            categories.add(new Category("P" + i));
        }
        ypane = new YPane(categories, series);
        ypane.setCategoryColor(Color.SKYBLUE);
        chart = new YChart(ypane);
        chart.setPrefSize(initialWidth, initialHeight);
        chart.refresh();
        trajectoryLabel.setText(featureVector.getLabel());
    }
}
