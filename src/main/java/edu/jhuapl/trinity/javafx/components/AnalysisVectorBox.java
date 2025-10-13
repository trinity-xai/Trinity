package edu.jhuapl.trinity.javafx.components;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.VBox;

/**
 * @author Sean Phillips
 */
public class AnalysisVectorBox extends VBox {
    public ObservableList<XYChart.Data<Double, Double>> analysisVector;
    public XYChart.Series series;
    private LineChart lineChart;
    Label vectorLabel;

    public AnalysisVectorBox(double width, double height) {
        setPrefSize(width, height);
        analysisVector = FXCollections.observableArrayList();
        series = new XYChart.Series("Hyperdimensional Vector", analysisVector);
        this.lineChart = new LineChart(new NumberAxis(), new NumberAxis(), FXCollections.observableArrayList(series));

        lineChart.setAnimated(false);
        lineChart.setLegendVisible(false);
        setBackground(Background.EMPTY);
        vectorLabel = new Label("Analys Vector");

        setSpacing(10);
        getChildren().addAll(vectorLabel, lineChart);
    }

    public void setAnalysisVector(String label, Double[] newData) {
        if (null != label) {
            vectorLabel.setText(label);
        }
        analysisVector.clear();
        for (int i = 0; i < newData.length; i++) {
            XYChart.Data data = new XYChart.Data(i, newData[i], newData[i]);
            Tooltip.install(data.getNode(), new Tooltip("Analysis Vector "
                + data.getXValue().toString() + "\n"
                + data.getYValue()));

            data.setNode(new HoverNode(i, newData[i]));
            analysisVector.add(data);
        }
    }
}
