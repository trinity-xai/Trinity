package edu.jhuapl.trinity.javafx.components;

import edu.jhuapl.trinity.javafx.events.FactorAnalysisEvent;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * @author Sean Phillips
 */
public class FactorControlBox extends VBox {
    Background transBack = new Background(new BackgroundFill(
        Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));

    Background transFillBack = new Background(new BackgroundFill(
        Color.ALICEBLUE.deriveColor(1, 1, 1, 0.222), CornerRadii.EMPTY, Insets.EMPTY));

    private final CanvasLineChart xFactorChart;
    private final CanvasLineChart zFactorChart;

    public FactorControlBox(double width, double height) {
        setPrefSize(width, height);

        xFactorChart = new CanvasLineChart();
        xFactorChart.setHoverPrefix("Factor Vector");
        zFactorChart = new CanvasLineChart();
        zFactorChart.setHoverPrefix("Dimension Over Time");

        xFactorChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        zFactorChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox.setVgrow(xFactorChart, Priority.ALWAYS);
        VBox.setVgrow(zFactorChart, Priority.ALWAYS);

        setBackground(Background.EMPTY);
        Label xFactorLabel = new Label("X Axis (Feature Vector)");
        Label zFactorLabel = new Label("Z Axis (Time)");

        ChoiceBox<String> xFactorChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(
            "Factor-0", "Factor-1", "Factor-2", "Factor-3",
            "Factor-4", "Factor-5", "Factor-6", "Factor-7"));
        xFactorChoiceBox.getSelectionModel().select(0); //X0 is the default
        xFactorChoiceBox.getSelectionModel().selectedIndexProperty().addListener(cl -> {
            xFactorChoiceBox.getScene().getRoot().fireEvent(
                new FactorAnalysisEvent(FactorAnalysisEvent.XFACTOR_SELECTION,
                    xFactorChoiceBox.getSelectionModel().getSelectedIndex()));
        });

        ChoiceBox<String> zFactorChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(
            "Factor-0", "Factor-1", "Factor-2", "Factor-3",
            "Factor-4", "Factor-5", "Factor-6", "Factor-7"));
        zFactorChoiceBox.getSelectionModel().select(2); //X2 is the default
        zFactorChoiceBox.getSelectionModel().selectedIndexProperty().addListener(cl -> {
            zFactorChoiceBox.getScene().getRoot().fireEvent(
                new FactorAnalysisEvent(FactorAnalysisEvent.ZFACTOR_SELECTION,
                    zFactorChoiceBox.getSelectionModel().getSelectedIndex()));
        });

        setSpacing(10);
        getChildren().addAll(
            new HBox(10, xFactorLabel, xFactorChoiceBox),
            xFactorChart,
            new HBox(10, zFactorLabel, zFactorChoiceBox),
            zFactorChart
        );
    }

    public void setXFactorVector(Double[] newData) {
        xFactorChart.setData(newData);
    }

    public void setZFactorVector(Double[] newData) {
        zFactorChart.setData(newData);
    }
}
