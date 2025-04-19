package edu.jhuapl.trinity.javafx.components.panes;

import edu.jhuapl.trinity.javafx.events.FactorAnalysisEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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

    public ObservableList<XYChart.Data<Double, Double>> xFactorVector;
    public XYChart.Series xFactorSeries;
    private LineChart xFactorChart;

    public ObservableList<XYChart.Data<Double, Double>> zFactorVector;
    public XYChart.Series zFactorSeries;
    private LineChart zFactorChart;

    public FactorControlBox(double width, double height) {
        setPrefSize(width, height);
        xFactorVector = FXCollections.observableArrayList();
        xFactorSeries = new XYChart.Series("Hyperdimensional Feature Vector", xFactorVector);
        this.xFactorChart = new LineChart(new NumberAxis(), new NumberAxis(), FXCollections.observableArrayList(xFactorSeries));

        zFactorVector = FXCollections.observableArrayList();
        zFactorSeries = new XYChart.Series("Dimension Over Time", zFactorVector);
        this.zFactorChart = new LineChart(new NumberAxis(), new NumberAxis(), FXCollections.observableArrayList(zFactorSeries));

        xFactorChart.setAnimated(false);
        zFactorChart.setAnimated(false);

        setBackground(Background.EMPTY);
        Label xFactorLabel = new Label("X Axis (Feature Vector)");
        Label zFactorLabel = new Label("Z Axis (Time)");

        ChoiceBox<String> xFactorChoiceBox = new ChoiceBox(FXCollections.observableArrayList(
            "Factor-0", "Factor-1", "Factor-2", "Factor-3",
            "Factor-4", "Factor-5", "Factor-6", "Factor-7"));
        xFactorChoiceBox.getSelectionModel().select(0); //X0 is the default
        xFactorChoiceBox.getSelectionModel().selectedIndexProperty().addListener(cl -> {
            xFactorChoiceBox.getScene().getRoot().fireEvent(
                new FactorAnalysisEvent(FactorAnalysisEvent.XFACTOR_SELECTION,
                    xFactorChoiceBox.getSelectionModel().getSelectedIndex()));
        });

        ChoiceBox<String> zFactorChoiceBox = new ChoiceBox(FXCollections.observableArrayList(
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

    public void setFactorVector(ObservableList<XYChart.Data<Double, Double>> vector, Double[] newData) {

        vector.clear();
        for (int i = 0; i < newData.length; i++) {
            XYChart.Data data = new XYChart.Data(i, newData[i], newData[i]);
            Tooltip.install(data.getNode(), new Tooltip("Factor Vector "
                + data.getXValue().toString() + "\n"
                + data.getYValue()));

            data.setNode(new HoverNode(i, newData[i]));
            vector.add(data);
        }
    }

    /**
     * a node which displays a value on hover, but is otherwise empty
     */
    class HoverNode extends StackPane {

        HoverNode(int index, Double value) {
            setPrefSize(15, 15);
            final Label label = new Label("Vector(" + index + ") = " + value);
            label.getStyleClass().addAll("default-color0", "chart-line-symbol", "chart-series-line");
            label.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
            label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);
            label.setTranslateY(-30); //Move label 25 pixels up
            Color ALICEBLUETRANS = new Color(0.9411765f, 0.972549f, 1.0f, 0.3);
            label.setBackground(new Background(new BackgroundFill(ALICEBLUETRANS, CornerRadii.EMPTY, Insets.EMPTY)));

            setOnMouseEntered(e -> {
                label.getStyleClass().add("onHover");
                getChildren().setAll(label);
                toFront();
            });
            setOnMouseExited(e -> {
                label.getStyleClass().remove("onHover");
                getChildren().clear();
            });
        }
    }
}
