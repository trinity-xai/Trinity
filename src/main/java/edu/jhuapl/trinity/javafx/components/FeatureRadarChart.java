/* Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC */

package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.utils.ResourceUtils;
import eu.hansolo.fx.charts.Category;
import eu.hansolo.fx.charts.ChartType;
import eu.hansolo.fx.charts.Symbol;
import eu.hansolo.fx.charts.YChart;
import eu.hansolo.fx.charts.YPane;
import eu.hansolo.fx.charts.data.ValueChartItem;
import eu.hansolo.fx.charts.series.YSeries;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Sean Phillips
 */
public class FeatureRadarChart extends VBox {

    Label featureLabel;
    private static Integer SCALAR_MAG = 500;
    private YSeries<ValueChartItem> series;
    private YSeries<ValueChartItem> stackedSeries;
    private YChart<ValueChartItem> chart;
    private YPane ypane;
    private List<String> labels = new ArrayList<>();
    private Spinner<Integer> scalarSpinner;
    private FeatureVector baseFeatureVector;
    private FeatureVector stackedFeatureVector;
    public double initialWidth;
    public double initialHeight;
    public SimpleBooleanProperty stacking = new SimpleBooleanProperty(false);
    public SimpleBooleanProperty positiveShifting = new SimpleBooleanProperty(false);

    public FeatureRadarChart(FeatureVector featureVector, double initialWidth, double initialHeight) {
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
        featureLabel = new Label(featureVector.getLabel());
        featureLabel.setFont(new Font("consolas", 14));
        featureLabel.setMinWidth(150);
        ImageView stackImageView = ResourceUtils.loadIcon("stack", 50);
        ToggleButton stackingToggleButton = new ToggleButton(
            "Stacking Mode", stackImageView);

        stacking.bind(stackingToggleButton.selectedProperty());
        stacking.addListener(cl -> {
            stackedFeatureVector = FeatureVector.EMPTY_FEATURE_VECTOR("", baseFeatureVector.getData().size());
            stackRadarPlot(stackedFeatureVector);
        });
        scalarSpinner = new Spinner<>();
        scalarSpinner.setEditable(true);
        scalarSpinner.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, SCALAR_MAG, 50, 5));
        scalarSpinner.valueProperty().addListener(c -> scaleRadarPlot());
        VBox spinnerVBox = new VBox(2, new Label("Scaling"), scalarSpinner);
        spinnerVBox.setPrefWidth(100);

        CheckBox positiveShiftCheckBox = new CheckBox("Positive Shift");
        positiveShifting.bind(positiveShiftCheckBox.selectedProperty());
        positiveShifting.addListener(c -> scaleRadarPlot()); //cheap way to recompute plot
        HBox topHBox = new HBox(10,
            featureLabel, positiveShiftCheckBox, spinnerVBox, stackingToggleButton);
        topHBox.setAlignment(Pos.CENTER);
        topHBox.setPadding(new Insets(5));
        Background transBack = new Background(new BackgroundFill(
            Color.CYAN.deriveColor(1, 1, 1, 0.111),
            new CornerRadii(10, false), Insets.EMPTY));
        topHBox.setBackground(transBack);

        makeRadarPlot(featureVector);
        setSpacing(5);
        ChoiceBox<ChartType> choiceBox = new ChoiceBox<>(
            FXCollections.observableList(Arrays.asList(ChartType.values())));
        choiceBox.getSelectionModel().select(ChartType.RADAR_SECTOR);
        choiceBox.setOnAction(e -> {
            series.setChartType(choiceBox.getValue());
            chart.refresh();
        });
        getChildren().addAll(topHBox, chart, choiceBox);
    }

    public void updateLabel(int index, String label) {
        if (index > labels.size()) return;
        labels.remove(index);
        labels.add(index, label);
        refreshCategories();
    }

    public void removeLabel(int index) {
        if (index > labels.size()) return;
        labels.remove(index);
        labels.add(index, "P" + index);
        refreshCategories();
    }

    public void setLabels(List<String> labels) {
        this.labels.clear();
        this.labels.addAll(labels);
        refreshCategories();
    }

    public void clearLabels() {
        int currentSize = labels.size();
        labels.clear();
        for (int i = 0; i < currentSize; i++) {
            labels.add("P" + i);
        }
        refreshCategories();
    }

    private void refreshCategories() {
        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < this.labels.size(); i++) {
            categories.add(new Category(this.labels.get(i)));
        }
        ypane.setCategories(categories);
    }

    public List<String> getLabels() {
        return labels;
    }

    private void scaleRadarPlot() {
        if (null == baseFeatureVector) return;
        double value;
        for (int i = 0; i < series.getItems().size(); i++) {
            value = baseFeatureVector.getData().get(i) * scalarSpinner.getValue();
            if (positiveShifting.get())
                value = transformPositive(baseFeatureVector.getData().get(i), baseFeatureVector) * scalarSpinner.getValue();
            series.getItems().get(i).setValue(value);

            if (null != stackedFeatureVector) {
                value = stackedFeatureVector.getData().get(i) * scalarSpinner.getValue();
                if (positiveShifting.get())
                    value = transformPositive(stackedFeatureVector.getData().get(i), stackedFeatureVector) * scalarSpinner.getValue();
                stackedSeries.getItems().get(i).setValue(value);
            }
        }
        chart.refresh();
    }

    public void stackRadarPlot(FeatureVector featureVector) {
        stackedFeatureVector = featureVector;
        List<ValueChartItem> items = new ArrayList<>(featureVector.getData().size());
        double value;
        for (int i = 0; i < featureVector.getData().size(); i++) {
            String label = "P " + i;
            if (null != labels && i < labels.size())
                label = labels.get(i);
            value = featureVector.getData().get(i) * scalarSpinner.getValue();
            if (positiveShifting.get())
                value = transformPositive(featureVector.getData().get(i), featureVector) * scalarSpinner.getValue();
            ValueChartItem dataPoint = new ValueChartItem(value, label);
            dataPoint.setSymbol(Symbol.CIRCLE);
            dataPoint.setStroke(Color.SKYBLUE);
            dataPoint.setFill(Color.BLUE);
            items.add(dataPoint);
        }
        stackedSeries.setItems(items);
        stackedSeries.setVisible(true);
        chart.refresh();
    }

    public void updateRadarPlot(FeatureVector featureVector) {
        baseFeatureVector = featureVector;
        //Hide stacked series
        stackedFeatureVector = FeatureVector.EMPTY_FEATURE_VECTOR("", baseFeatureVector.getData().size());
        stackRadarPlot(stackedFeatureVector);
        stackedSeries.setVisible(false);
        //update base series
        List<ValueChartItem> items = new ArrayList<>(baseFeatureVector.getData().size());
        double value;
        for (int i = 0; i < baseFeatureVector.getData().size(); i++) {
            String label = "P " + i;
            if (null != labels && i < labels.size())
                label = labels.get(i);
            value = featureVector.getData().get(i) * scalarSpinner.getValue();
            if (positiveShifting.get())
                value = transformPositive(featureVector.getData().get(i), featureVector) * scalarSpinner.getValue();
            ValueChartItem dataPoint = new ValueChartItem(value, label);
            dataPoint.setSymbol(Symbol.CIRCLE);
            dataPoint.setStroke(Color.SKYBLUE);
            dataPoint.setFill(Color.BLUE);
            items.add(dataPoint);
        }
        series.setItems(items);
        series.setTextFill(Color.BLUE);
        featureLabel.setText(baseFeatureVector.getLabel());
        chart.refresh();
    }

    private double transformPositive(double value, FeatureVector fv) {
        return (((value - fv.getMin()) * 1) / fv.getWidth());
    }

    private void makeRadarPlot(FeatureVector featureVector) {
        List<ValueChartItem> items = new ArrayList<>(featureVector.getData().size());
        double value;
        for (int i = 0; i < featureVector.getData().size(); i++) {
            String label = "P " + i;
            if (null != labels && i < labels.size())
                label = labels.get(i);
            value = featureVector.getData().get(i) * scalarSpinner.getValue();
            if (positiveShifting.get())
                value = transformPositive(featureVector.getData().get(i), featureVector) * scalarSpinner.getValue();
            ValueChartItem dataPoint = new ValueChartItem(value, label);
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

        stackedSeries = new YSeries(items, ChartType.RADAR_SECTOR, "Features",
            new RadialGradient(0, 0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(0, 0, 255, 0.25)),
                new Stop(1.0, Color.rgb(0, 255, 0, 0.5))),
            Color.SKYBLUE, Symbol.CIRCLE);
        stackedSeries.setTextFill(Color.BLUE);
        stackedSeries.setAnimated(true);
        stackedSeries.setAnimationDuration(100);
        stackedSeries.setVisible(false);
        List<Category> categories = new ArrayList<>();
        for (int i = 0; i < featureVector.getData().size(); i++) {
            categories.add(new Category("P" + i));
        }
        ypane = new YPane(categories, series, stackedSeries);
        ypane.setCategoryColor(Color.SKYBLUE);
        chart = new YChart(ypane);
        chart.setPrefSize(initialWidth, initialHeight);
        chart.refresh();
        featureLabel.setText(featureVector.getLabel());
    }

    //     private void updateCalcs() {
////        HEIGHT = WIDTH + (WIDTH * REFLECTION_SIZE);
//        HEIGHT = HEIGHT;
//
//        RADIUS_H = WIDTH / 2;
//        BACK = WIDTH / 10;
//    }
//     /**
//     * Angle Property
//     */
    public final DoubleProperty angle = new SimpleDoubleProperty(0) {
        @Override
        protected void invalidated() {
            // when angle changes calculate new transform
//            updateCalcs();
//            double lx = (RADIUS_H - Math.sin(Math.toRadians(angle.get())) * RADIUS_H - 1);
//            double rx = (RADIUS_H + Math.sin(Math.toRadians(angle.get())) * RADIUS_H + 1);
//            double uly = (-Math.cos(Math.toRadians(angle.get())) * BACK);
//            double ury = -uly;
//            transform.setUlx(lx);
//            transform.setUly(uly);
//            transform.setUrx(rx);
//            transform.setUry(ury);
//            transform.setLrx(rx);
//            transform.setLry(HEIGHT + uly);
//            transform.setLlx(lx);
//            transform.setLly(HEIGHT + ury);
        }
    };
//
//    public final double getAngle() {
//        return angle.getValue();
//    }
//
//    public final void setAngle(double value) {
//        angle.setValue(value);
//    }
//
//    public final DoubleProperty angleModel() {
//        return angle;
//    }
}
